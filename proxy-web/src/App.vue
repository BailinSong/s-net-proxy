<template>
    <v-app dark  v-if="!$store.state.proxy.isLogin">
        <main-login ></main-login>
    </v-app>
    <v-app dark v-else>
        <v-navigation-drawer
                v-model="drawer"
                clipped
                fixed
                app
        >
            <v-list dense>
                <v-list-tile @click="handleSelect('rules')">
                    <v-list-tile-action>
                        <v-icon>link</v-icon>
                    </v-list-tile-action>
                    <v-list-tile-content>
                        <v-list-tile-title>代理规则</v-list-tile-title>
                    </v-list-tile-content>
                </v-list-tile>
            </v-list>
        </v-navigation-drawer>
        <v-toolbar app fixed clipped-left>
            <v-toolbar-side-icon @click.stop="openNavigation"></v-toolbar-side-icon>
            <v-toolbar-title class="headline text-uppercase">
                <span>Net</span>
                <span class="font-weight-light">Proxy</span>
            </v-toolbar-title>
            <v-spacer></v-spacer>
            <v-btn flat icon @click="logOut"><v-icon>lock</v-icon></v-btn>
        </v-toolbar>
        <v-content>
            <router-view></router-view>
        </v-content>
        <v-footer app fixed>
            <span class="text-uppercase" > <span>Blue</span><span class="font-weight-light">Line</span> &copy; 2018</span>
        </v-footer>
    </v-app>
</template>

<script>

    import MainLogin from "./views/MainLogin";

    export default {
        name: 'App',
        components: {
            MainLogin
        },
        data: () => ({
            drawer: true
        }),
        props: {
            source: String
        },
        methods:{
            openNavigation:function(){
                this.drawer = !this.drawer;
            }
            ,logOut:function(){
                this.$store.commit('checkOut');
            }
            ,handleSelect(key) {
                this.$router.push({path: key})
            }
        }
    }
</script>
