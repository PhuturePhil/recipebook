import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Design-System (Kopien aus workspace/snippets/, dort zuerst ändern) vor allen Komponenten-Styles
import './shell/design-tokens.css'
import './shell/akzent.css'
import './shell/app-shell.css'
import './style.css'
import './shell/app-icons.js'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/authStore'
import { installSessionGuard } from './services/sessionGuard'

const app = createApp(App)

app.use(createPinia())
app.use(router)

installSessionGuard({ authStore: useAuthStore(), router })

app.mount('#app')
