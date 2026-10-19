import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'

import App from './App.vue'
import router from './router'

// Vant 全量样式 + 全局样式
import 'vant/lib/index.css'
import './styles/index.scss'

const app = createApp(App)

// 注意顺序：Pinia 需先于 Router 安装，路由守卫中才可以使用 store
app.use(createPinia())
app.use(router)
app.use(Vant)

app.mount('#app')
