<template>
  <div class="upload-page">
    <el-alert type="success" :closable="false" show-icon
      title="大文件 Excel 分片上传 + 断点续传 — 支持断点续传, 刷新/中断后可从已上传分片继续" />

    <el-card class="card">
      <div slot="header">
        <span class="header-title">📄 选择 Excel 文件</span>
        <el-tag size="mini" type="info" style="margin-left:10px">分片大小: {{ (chunkSize / 1024 / 1024).toFixed(0) }}MB</el-tag>
      </div>

      <!-- 上传组件(自定义触发) -->
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="handleFileSelect" />
      <el-button type="primary" :disabled="uploading" @click="$refs.fileInput.click()">选择文件</el-button>
      <el-button type="warning" :disabled="!file || uploading" @click="startUpload">开始上传</el-button>
      <el-button type="danger" :disabled="!uploading" @click="pauseUpload">暂停</el-button>

      <div v-if="file" class="file-info">
        <p>文件名: <b>{{ file.name }}</b></p>
        <p>大小: <b>{{ formatSize(file.size) }}</b> | 分片数: <b>{{ totalChunks }}</b> | 文件标识(MD5): <code>{{ identifier }}</code></p>
        <p v-if="uploadedChunks.length">断点续传: 已存在 <b>{{ uploadedChunks.length }}</b> 个分片, 将跳过续传。</p>
      </div>

      <!-- 进度 -->
      <div v-if="file" class="progress-area">
        <el-progress :percentage="progress" :status="progress === 100 ? 'success' : undefined" :stroke-width="20" />
        <p class="progress-text">
          正在上传分片 <b>{{ currentChunk + 1 }}</b> / <b>{{ totalChunks }}</b>
          (已上传: {{ uploadedCount }} / {{ totalChunks }})
        </p>
      </div>

      <!-- 上传完成结果 -->
      <div v-if="mergeResult" class="result">
        <el-alert type="success" :closable="false" show-icon title="文件上传并合并成功！" />
        <p>
          访问/下载合并后的文件:
          <el-link type="primary" :href="downloadUrl" target="_blank">{{ downloadUrl }}</el-link>
        </p>
      </div>
    </el-card>

    <!-- 使用说明 -->
    <el-card class="card tip-card">
      <div slot="header">📌 断点续传原理 & 演示方式</div>
      <ul>
        <li><b>MD5 标识文件:</b> 前端用 spark-md5 计算文件 MD5, 作为文件唯一标识 identifier。</li>
        <li><b>分片切片:</b> 将大文件按 5MB 切割成多个 Blob 分片, 逐个上传。</li>
        <li><b>断点续传:</b> 上传前调用 <code>/api/upload/check</code> 查询已上传分片, 只上传缺失部分。</li>
        <li><b>合并:</b> 全部上传后调用 <code>/api/upload/merge</code>, 后端按序合并为完整 Excel。</li>
        <li><b>演示建议:</b> 上传较大 Excel(如 100MB+), 上传过程中暂停或刷新页面, 再重新选择同一文件即可从断点继续。</li>
      </ul>
    </el-card>
  </div>
</template>

<script>
import SparkMD5 from 'spark-md5'
import { uploadApi } from '../api/upload'

// 分片大小: 5MB
const CHUNK_SIZE = 5 * 1024 * 1024
// 并发上传数
const CONCURRENCY = 3

export default {
  name: 'ExcelUpload',
  data () {
    return {
      chunkSize: CHUNK_SIZE,
      file: null,
      identifier: '',
      totalChunks: 0,
      uploadedChunks: [], // 已存在的分片(续传跳过)
      currentChunk: -1,
      uploading: false,
      paused: false,
      mergeResult: null,
      downloadUrl: '',
      progress: 0,
      uploadedCount: 0
    }
  },
  methods: {
    /**
     * 选择文件
     */
    handleFileSelect (e) {
      const file = e.target.files[0]
      if (!file) return
      // 简单校验是否 Excel
      if (!/\.(xlsx|xls)$/i.test(file.name)) {
        this.$message.error('请选择 Excel 文件(.xlsx / .xls)')
        return
      }
      this.file = file
      this.totalChunks = Math.ceil(file.size / CHUNK_SIZE)
      this.mergeResult = null
      this.downloadUrl = ''
      this.progress = 0
      this.identifier = ''
      this.uploadedChunks = []
      this.uploadedCount = 0
      this.$message.info('文件已选择, 正在计算 MD5...')
      this.calculateMD5(file)
    },

    /**
     * 计算文件 MD5 (用于断点续传标识)
     */
    calculateMD5 (file) {
      const fileReader = new FileReader()
      const spark = new SparkMD5.ArrayBuffer()
      const chunkSize = 10 * 1024 * 1024
      let current = 0
      const total = file.size

      const loadNext = () => {
        const slice = file.slice(current, current + chunkSize)
        fileReader.readAsArrayBuffer(slice)
      }
      fileReader.onload = (e) => {
        spark.append(e.target.result)
        current += chunkSize
        if (current < total) {
          loadNext()
        } else {
          this.identifier = spark.end()
          this.$message.success('文件 MD5 计算完成: ' + this.identifier)
          this.checkProgress()
        }
      }
      fileReader.onerror = () => {
        this.$message.error('读取文件失败, 无法计算 MD5')
      }
      loadNext()
    },

    /**
     * 查询已上传进度 (断点续传核心)
     */
    async checkProgress () {
      try {
        const res = await uploadApi.check({
          identifier: this.identifier,
          filename: this.file.name,
          totalSize: this.file.size,
          totalChunks: this.totalChunks,
          chunkSize: CHUNK_SIZE
        })
        this.uploadedChunks = res.data.uploadedChunks || []
        this.uploadedCount = this.uploadedChunks.length
        if (res.data.finished) {
          // 已全部上传, 直接合并
          this.$message.info('检测到文件已全部上传, 直接合并')
          await this.merge()
        } else if (this.uploadedChunks.length > 0) {
          this.$message.success(`检测到 ${this.uploadedChunks.length} 个已上传分片, 将续传`)
        }
      } catch (e) {
        this.$message.warning('查询进度失败(可能后端未启动), 将全量上传: ' + e.message)
      }
    },

    /**
     * 开始上传(并发控制)
     */
    async startUpload () {
      if (!this.file || !this.identifier) {
        this.$message.warning('请先选择文件并等待 MD5 计算完成')
        return
      }
      this.uploading = true
      this.paused = false
      this.mergeResult = null

      // 需要一个可中断的队列: 用标记是否暂停
      this.pauseFlag = false
      await this.uploadChunksConcurrent()
      if (this.pauseFlag) {
        this.uploading = false
        return
      }
      // 全部上传完成, 合并
      await this.merge()
      this.uploading = false
    },

    /**
     * 并发上传缺失分片
     */
    async uploadChunksConcurrent () {
      // 找出缺失分片
      const missing = []
      for (let i = 0; i < this.totalChunks; i++) {
        if (!this.uploadedChunks.includes(i)) {
          missing.push(i)
        }
      }
      if (missing.length === 0) {
        this.progress = 100
        return
      }

      let nextIndex = 0
      // 简单并发池
      const workers = []
      const runWorker = async () => {
        while (!this.pauseFlag) {
          const idx = nextIndex++
          if (idx >= missing.length) break
          const chunkNumber = missing[idx]
          this.currentChunk = chunkNumber
          await this.uploadSingleChunk(chunkNumber)
        }
      }
      const workerCount = Math.min(CONCURRENCY, missing.length)
      for (let i = 0; i < workerCount; i++) {
        workers.push(runWorker())
      }
      await Promise.all(workers)
      this.progress = Math.round((this.uploadedCount / this.totalChunks) * 100)
    },

    /**
     * 上传单个分片
     */
    async uploadSingleChunk (chunkNumber) {
      const start = chunkNumber * CHUNK_SIZE
      const end = Math.min(this.file.size, start + CHUNK_SIZE)
      const blob = this.file.slice(start, end)

      const formData = new FormData()
      formData.append('file', blob, this.file.name + '_' + chunkNumber)
      formData.append('identifier', this.identifier)
      formData.append('chunkNumber', chunkNumber)
      formData.append('totalChunks', this.totalChunks)
      formData.append('filename', this.file.name)
      formData.append('totalSize', this.file.size)
      formData.append('chunkSize', CHUNK_SIZE)

      let retry = 0
      const maxRetry = 3
      while (retry < maxRetry && !this.pauseFlag) {
        try {
          const res = await uploadApi.uploadChunk(formData)
          this.uploadedCount += 1
          if (!this.uploadedChunks.includes(chunkNumber)) {
            this.uploadedChunks.push(chunkNumber)
          }
          this.progress = Math.round((this.uploadedCount / this.totalChunks) * 100)
          return res
        } catch (e) {
          retry++
          if (retry >= maxRetry) {
            this.$message.error(`分片 ${chunkNumber + 1} 上传失败: ${e.message}`)
          }
        }
      }
    },

    /**
     * 暂停上传
     */
    pauseUpload () {
      this.pauseFlag = true
      this.paused = true
      this.uploading = false
      this.$message.warning('已暂停, 已上传分片已保存, 可重新选择同一文件续传')
    },

    /**
     * 合并分片
     */
    async merge () {
      try {
        const res = await uploadApi.merge({
          identifier: this.identifier,
          filename: this.file.name,
          totalChunks: this.totalChunks
        })
        this.mergeResult = res.data
        this.downloadUrl = res.data.downloadUrl
        this.progress = 100
        this.$message.success('文件合并成功!')
      } catch (e) {
        this.$message.error('合并失败: ' + e.message)
      }
    },

    /**
     * 格式化文件大小
     */
    formatSize (bytes) {
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
      if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(2) + ' MB'
      return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
    }
  }
}
</script>

<style scoped>
.card { margin-bottom: 20px; }
.header-title { font-weight: bold; font-size: 16px; }
.file-info { margin-top: 16px; line-height: 1.9; background: #f8f9fa; padding: 12px; border-radius: 4px; }
.file-info code { background: #f4f4f5; padding: 2px 6px; border-radius: 3px; color: #c7254e; }
.progress-area { margin-top: 20px; }
.progress-text { margin-top: 8px; color: #666; font-size: 14px; }
.result { margin-top: 20px; }
.result p { margin-top: 12px; line-height: 1.8; }
.tip-card ul { padding-left: 20px; line-height: 2; }
.tip-card code { background: #f4f4f5; padding: 2px 6px; border-radius: 3px; color: #c7254e; }
</style>
