import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import './style.css'
import App from './App.vue'
import { router } from './router/index.js'

const app = createApp(App)

for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

// Pinia 必須在 router 之前 use（router guard 內會用到 store）
app.use(createPinia())
app.use(ElementPlus, { locale: zhTw })
app.use(router)
app.mount('#app')
