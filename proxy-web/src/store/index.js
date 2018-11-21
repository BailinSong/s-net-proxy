import Vue from 'vue'
import Vuex from 'vuex'

import proxy from './modules/proxy'
Vue.use(Vuex)
export default new Vuex.Store({
  modules:{
    proxy
  }
})
