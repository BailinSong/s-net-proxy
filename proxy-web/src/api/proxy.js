import axios from 'axios'

const API_URL = 'http://45.78.43.163:1024';
// const API_URL = 'http://192.168.1.155:1024';
// const API_URL = 'http://35.221.215.170:1024';
// const API_URL='http://proxy-server:1024';
export default {


  getToken(user, password) {
    let timestamp = new Date().getTime();
    let token = btoa(user + ':' + password + ':' + timestamp);
    // axios.interceptors.request.use((request) => {
    //   request.headers['token'] = token;
    //   return request;
    // });
    return token;
  },
  checkIn(token, su, er) {
    axios.get(API_URL + "/user", {
      headers: {
        'token': token
      }
    })
      .then(res =>
        su(res['data']))
      .catch(e => er(e))
  },

  getRules(token , su,er){
    axios.get(API_URL + "/rules", {
      headers: {
        'token': token
      }
    })
      .then(res =>
        su(res['data']))
      .catch(e => er(e))
  }
  ,putRule(token,rule,su,er){
    axios.put(API_URL + "/rules", rule,{
      headers: {
        'token': token
      }
    }).then(res =>
      su(res['data']))
      .catch(e => er(e))
  }
    ,removeRule(token,rule,su,er){
        axios.delete(API_URL + "/rules/"+rule.id,{
            headers: {
                'token': token
            }
        }).then(res =>
            su(res['data']))
            .catch(e => er(e))
    }

}
