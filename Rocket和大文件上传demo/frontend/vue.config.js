const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    port: 8081,
    proxy: {
      // 后端接口代理
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 文件访问代理(合并后的 Excel)
      '/files': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
