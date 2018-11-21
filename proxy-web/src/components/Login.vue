<template>
        <v-container fluid fill-height>
            <v-layout align-center justify-center>
                <v-flex xs12 sm8 md4>
                    <v-card class="elevation-12">
                        <v-toolbar dark color="primary">
                            <v-toolbar-title>登入</v-toolbar-title>
                            <v-spacer></v-spacer>
                        </v-toolbar>
                        <v-card-text>
                            <v-form ref="form">
                                <v-text-field prepend-icon="person" name="login" label="用户" v-model="user"
                                              type="text" required :rules="userRules" ></v-text-field>
                                <v-text-field id="password" prepend-icon="lock" name="password" v-model="password"
                                              label="密钥" type="password" required :rules="passwordRules"
                                              :append-icon="show ? 'visibility_off' : 'visibility'"
                                              :type="show ? 'text' : 'password'"
                                              @click:append="show = !show"></v-text-field>
                            </v-form>
                        </v-card-text>
                        <v-card-actions>
                            <v-spacer></v-spacer>
                            <v-btn color="primary" @click="login">确认</v-btn>
                        </v-card-actions>
                    </v-card>
                </v-flex>
            </v-layout>
        </v-container>
</template>

<script>
    import api from "../api/proxy"

    export default {
        name: "Login",
        data() {
            return {
                user: "",
                password: "",
                show: false,
                userRules: [
                    v => !!v || '不能为空',
                    v => (v && v.length <= 20) || '长度不能超过20',
                    v => (v && v.length >= 4) || '长度不能少于4'
                ],
                passwordRules:[
                    v => !!v || '不能为空',
                    v => (v && v.length >= 4) || '长度不能少于4'
                ]
            }
        },
        methods: {
            login() {
                if (this.$refs.form.validate()) {
                    api.checkIn(api.getToken(this.user, this.password),
                        data => {
                            if (data['state'] == '0') {
                                this.$store.commit('checkIn', this);
                                this.user = this.password = '';
                            } else {
                                confirm(data['state'] + ':' + data['data'])
                            }
                        },
                        error => {

                            confirm(error)
                            // confirm('a'+error.response.status)
                            // confirm(error.response.data)

                        });
                }


            }
        }
    }
</script>

<style scoped>

</style>
