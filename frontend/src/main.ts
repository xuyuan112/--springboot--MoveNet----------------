import { createApp } from 'vue'
import type { Component } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

// Element Plus 全量样式 + 全局样式
import 'element-plus/dist/index.css'
import './styles/index.scss'

const app = createApp(App)

// 注意顺序：Pinia 需先于 Router 安装，路由守卫中才可以使用 store
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全量注册 Element Plus 图标为全局组件（模板中可直接使用 <Monitor /> 等）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component as Component)
}

app.mount('#app')
