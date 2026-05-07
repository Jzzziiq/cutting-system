import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import permission from './directives/permission';
import './styles/main.css';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(permission);
app.mount('#app');
