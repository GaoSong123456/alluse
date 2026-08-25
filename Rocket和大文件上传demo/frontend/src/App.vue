<template>
  <div id="app">
    <el-container>
      <el-header class="header">
        <div class="title">🏢 企业级 Demo</div>
        <el-menu mode="horizontal" :default-active="activeMenu" router background-color="#001529"
                 text-color="#fff" active-text-color="#409EFF" class="menu">
          <el-menu-item index="/mq">RocketMQ 五大特性</el-menu-item>
          <el-menu-item index="/upload">大文件分片断点续传</el-menu-item>
        </el-menu>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App',
  computed: {
    activeMenu () {
      return this.$route.path
    }
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif; background: #f0f2f5; }
.header {
  display: flex;
  align-items: center;
  background: #001529;
  color: #fff;
  padding: 0 20px;
}
.header .title { font-size: 20px; font-weight: bold; margin-right: 40px; white-space: nowrap; }
.header .menu { border-bottom: none; flex: 1; background: transparent !important; }
.main { padding: 20px; }
</style>
