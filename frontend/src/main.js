import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// --- 1. 引入 Element Plus (工具箱) ---
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css' // 引入 Element Plus 的 CSS 样式 (必须!)

// --- 2. 引入 Element Plus 图标 (图标包) ---
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// --- 3. 创建 App ---
const app = createApp(App)

// --- 4. 注册所有图标 ---
// (循环遍历图标包，把所有图标注册为全局组件)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// --- 5. 注册插件 ---
app.use(router)
app.use(ElementPlus) // 告诉 Vue 使用 Element Plus

// --- 6. 挂载 ---
app.mount('#app')