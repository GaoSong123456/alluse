import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/mq'
  },
  {
    path: '/mq',
    name: 'MqDemo',
    component: () => import('../views/MqDemo.vue'),
    meta: { title: 'RocketMQ 五大特性' }
  },
  {
    path: '/upload',
    name: 'ExcelUpload',
    component: () => import('../views/ExcelUpload.vue'),
    meta: { title: '大文件分片断点续传' }
  }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

export default router
