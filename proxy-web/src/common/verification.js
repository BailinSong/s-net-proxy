export default {
    isNum: function (v) {



        if (!v&&v!==0&&v!=='0') {

            return false
        }

        if (v === '') {

            return false
        }
        let reg = /^([0-9]|([1-9][0-9]*))$/
        return reg.test(v)
    },
    isInNum: function (v, n, m) {
        if(!this.isNum(v)){
            return false
        }
        let vn=new Number(v)
        if(n<=vn && m>=vn){
            return true
        }else{
            return false
        }

    },
    isIp: function (v) {
        let ip = v.split('.')
        if (ip.length !== 4) {
            return false
        }
        for (let ipInfo in ip) {
            if (!this.isInNum(ip[ipInfo], 0, 255)) {
                return false
            }
        }
        return true;
    },
    isDomain: function (v) {
        let reg = /[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+/
        return reg.test(v)
    },
    isPath: function (v) {
        let reg = /^[/]([A-Za-z0-9-~\/&?=%])+$/
        return reg.test(v)
    }


}


