import { createApp } from 'vue'
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

app.use(ElementPlus, { locale: zhTw })
app.use(router)
app.mount('#app')
