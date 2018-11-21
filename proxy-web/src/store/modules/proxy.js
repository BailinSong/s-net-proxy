// import proxy from '../../api/proxy.js'
export default {
  state : {
    isLogin: false,
    user: '',
    password: ''
  },
  mutations:{
    checkIn (state,info){
      state.user=info.user;
      state.password=info.password;
      state.isLogin=true;
    },

    checkOut(state){
      state.user='';
      state.password='';
      state.isLogin=false;
    }
  }



}

