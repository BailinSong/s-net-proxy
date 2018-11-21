import Vue from 'vue'
import Vuetify from 'vuetify'
import './plugins/vuetify'
import App from './App.vue'
import router from './router'
import store from './store/index'

Vue.config.productionTip = false

Vue.use(Vuetify,{
    theme:{
      primary:'#FF6F00'
    }
})

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
