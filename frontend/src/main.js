import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/authStore'
import { installSessionGuard } from './services/sessionGuard'

const app = createApp(App)

app.use(createPinia())
app.use(router)

installSessionGuard({ authStore: useAuthStore(), router })

app.mount('#app')
