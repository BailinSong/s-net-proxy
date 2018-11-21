<template>
    <div>
        <v-toolbar flat>
            <v-toolbar-title>
                <v-icon>link</v-icon>
            </v-toolbar-title>
            <v-divider
                    class="mx-2"
                    inset
                    vertical
            ></v-divider>
            <v-text-field
                    v-model="search"
                    append-icon="search"
                    label="Search"
                    single-line
                    hide-details
            ></v-text-field>
            <v-divider
                    class="mx-2"
                    inset
                    vertical
            ></v-divider>
            <v-btn flat icon @click="initialize">
                <v-icon>cached</v-icon>
            </v-btn>
            <v-dialog v-model="dialog" max-width="500px" :fullscreen="$vuetify.breakpoint.xsOnly">
                <v-btn slot="activator" color="primary" flat icon>
                    <v-icon>add</v-icon>
                </v-btn>

                <v-card>
                    <v-card-title>
                        <span class="headline">{{ formTitle }}</span>
                    </v-card-title>

                    <v-card-text>
                        <v-container grid-list-md>
                            <v-form ref="form">
                                <v-layout wrap>

                                    <v-flex xs12 sm6 md4>
                                        <v-text-field v-model="editedItem.id" label="ID" disabled
                                                      :rules="idRules"></v-text-field>

                                    </v-flex>
                                    <v-flex xs12 sm6 md4>
                                        <v-text-field v-model="editedItem.name" label="名称"></v-text-field>
                                    </v-flex>
                                    <v-flex xs12 sm6 md4>
                                        <v-select
                                                v-model="editedItem.protocol"
                                                :items="['tcp','http']"
                                                label="协议"
                                        ></v-select>
                                    </v-flex>
                                    <v-flex xs12 sm6 md4>
                                        <v-text-field v-model="editedItem.host" :rules="hostRules"
                                                      label="主机"></v-text-field>
                                    </v-flex>
                                    <v-flex xs12 sm6 md4>
                                        <v-text-field v-model="editedItem.port" :rules="portRules" type="number"
                                                      label="端口"></v-text-field>
                                    </v-flex>
                                    <v-flex xs12 sm6 md4>
                                        <v-text-field v-if="editedItem.protocol=='http'" v-model="editedItem.path"
                                                      label="路径"></v-text-field>
                                    </v-flex>
                                    <v-flex xs12 sm6 md4>
                                        <v-text-field v-model="editedItem.realHost" :rules="realHostRules"
                                                      label="真实主机"></v-text-field>
                                    </v-flex>
                                    <v-flex xs12 sm6 md4>
                                        <v-text-field v-model="editedItem.realPort" :rules="realPortRules" type="number"
                                                      label="真实端口"></v-text-field>
                                    </v-flex>
                                    <v-flex xs12 sm6 md4>
                                        <v-text-field v-if="editedItem.protocol=='http'" v-model="editedItem.realPath"
                                                      label="真实路径"></v-text-field>
                                    </v-flex>

                                </v-layout>
                            </v-form>
                        </v-container>
                    </v-card-text>

                    <v-card-actions>
                        <v-spacer></v-spacer>
                        <v-btn color="blue darken-1" flat @click.native="close">取消</v-btn>
                        <v-btn color="blue darken-1" flat @click.native="save">保存</v-btn>
                    </v-card-actions>
                </v-card>
            </v-dialog>
        </v-toolbar>

        <v-layout hidden-md-and-up>
            <v-flex>
                <v-list three-line>
                    <template v-for="(item, index) in this.desserts">
                        <div :hidden="!matching(item)">
                            <v-list-tile
                                    avatar
                            >
                                <v-list-tile-action style="width: 80px" class="text-uppercase">
                                    {{ item.name }}
                                    <v-chip small color="primary">{{item.id}}</v-chip>
                                    <v-chip small>{{ item.protocol }}</v-chip>
                                </v-list-tile-action>
                                <v-divider
                                        class="mx-2"
                                        inset
                                        vertical
                                ></v-divider>
                                <v-list-tile-content>
                                    <v-list-tile-title>{{ item.host }}:{{item.port}}<span v-if="item.protocol=='http'">1{{item.path}}</span>
                                    </v-list-tile-title>
                                    <v-list-tile-sub-title>{{ item.realHost }}:{{item.realPort}}<span
                                            v-if="item.protocol=='http'">2{{item.realPath}}</span>
                                    </v-list-tile-sub-title>
                                </v-list-tile-content>

                                <v-list-tile-action>
                                    <v-layout>
                                        <v-btn fab small @click="editItem(item)">
                                            <v-icon


                                            >
                                                edit
                                            </v-icon>
                                        </v-btn>
                                        <v-btn fab small @click="deleteItem(item)" color="primary">
                                            <v-icon

                                            >
                                                delete
                                            </v-icon>
                                        </v-btn>
                                    </v-layout>

                                </v-list-tile-action>

                            </v-list-tile>
                            <v-divider
                                    v-if="index + 1 < desserts.length"
                                    :key="index"
                            ></v-divider>
                        </div>
                    </template>
                </v-list>
            </v-flex>
        </v-layout>
        <v-layout hidden-sm-and-down>
            <v-flex>
                <v-data-table
                        :headers="headers"
                        :items="desserts"
                        hide-actions
                        :search="search"
                        class="elevation-1"
                        xs12 sm8 md4
                >
                    <template slot="items" slot-scope="props">
                        <td>{{ props.item.id }}</td>
                        <td>{{ props.item.name }}</td>
                        <td>{{ props.item.protocol }}</td>
                        <td>{{ props.item.host }}</td>
                        <td>{{ props.item.port }}</td>
                        <td>{{ props.item.path }}</td>
                        <td>{{ props.item.realHost }}</td>
                        <td>{{ props.item.realPort }}</td>
                        <td>{{ props.item.realPath }}</td>
                        <td class="justify-center layout px-0 text-xs-right">
                            <v-icon
                                    small
                                    class="mr-2"
                                    @click="editItem(props.item)"
                            >
                                edit
                            </v-icon>
                            <v-icon
                                    small
                                    @click="deleteItem(props.item)"
                            >
                                delete
                            </v-icon>
                        </td>
                    </template>
                    <template slot="no-data">
                        <span>点击<v-icon>cached</v-icon>重新加载数据</span>
                    </template>
                </v-data-table>
            </v-flex>
        </v-layout>

    </div>
</template>

<script>

    import api from "../api/proxy"
    import ve from "../common/verification"

    export default {
        name: "Rules",
        data: () => ({
            search: '',
            dialog: false,
            headers: [
                {
                    text: 'ID',
                    align: 'left',
                    sortable: false,
                    value: 'id'
                },

                {text: '名称', align: 'left', value: 'name'},
                {text: '协议', align: 'left', value: 'protocol'},
                {text: '主机', align: 'left', value: 'host'},
                {text: '端口', align: 'left', value: 'port'},
                {text: '路径', align: 'left', value: 'path'},
                {text: '真实主机', align: 'left', value: 'realHost'},
                {text: '真实端口', align: 'left', value: 'realPort'},
                {text: '真实路径', align: 'left', value: 'realPath'},
                {text: '操作',value:'id'}

            ],
            desserts: [],
            editedIndex: -1,
            editedItem: {
                id: '',
                name: '',
                protocol: '',
                host: '',
                port: 0,
                path: '',
                realHost: '',
                realPort: 0,
                realPath: ''
            },
            defaultItem: {
                id: '0',
                name: 'New Rule',
                protocol: 'tcp',
                host: '0.0.0.0',
                port: 80,
                path: '',
                realHost: '127.0.0.1',
                realPort: 1,
                realPath: ''
            }
            , idRules: [
                v => ve.isInNum(v, 0, 65535) || '值必须在 0 - 65535 之间'
            ], hostRules: [
                v => ve.isIp(v) || '无效IP地址'
            ], portRules: [
                v => ve.isInNum(v, 80, 60000) || '值必须在 80 - 60000 之间'
            ], realHostRules: [
                v => !!v || '无效地址'
            ], realPortRules: [
                v => ve.isInNum(v, 1, 65535) || '值必须在 1 - 65535 之间'
            ]
        }),

        computed: {
            formTitle() {
                return this.isNew ? '新建规则' : '编辑规则'
            },
            isNew() {

                return this.editedIndex === -1;
            }
        },

        watch: {
            dialog(val) {
                val || this.close()
            }
        },

        created() {
            this.initialize()
        },

        methods: {
            updateDefautItemID:function(){


                for(let temp=0;temp<100;temp++){
                    let none=true
                    for( let i in this.desserts){
                        if(this.desserts[i].id===temp){
                            none=false
                            break
                        }
                    }
                    if(none){
                        this.defaultItem.id=temp;
                        break
                    }
                }

                this.editedItem = Object.assign({}, this.defaultItem)
                this.editedIndex = -1
            },
            initialize() {
                this.desserts.length = 0


                api.getRules(api.getToken(this.$store.state.proxy.user, this.$store.state.proxy.password),
                    da => {
                        if (da['state'] == '0') {
                            for (let key in da['data']) {
                                this.desserts.push(da['data'][key]);
                            }
                            this.rules = da['data']
                            this.updateDefautItemID()


                        } else {
                            this.$notify({
                                title: da['state'],
                                message: da['data'],
                                type: 'warning'
                            });
                        }
                    },
                    error => {
                        console.log(error);
                        this.$notify({
                            title: 'Error',
                            message: error,
                        })
                    }
                )

            },

            editItem(item) {
                this.editedIndex = this.desserts.indexOf(item)
                this.editedItem = Object.assign({}, item)
                this.dialog = true
            },

            deleteItem(item) {

                const index = this.desserts.indexOf(item)
                confirm('确定要删除此规则?') && api.removeRule(api.getToken(this.$store.state.proxy.user, this.$store.state.proxy.password), item,
                    da => {
                        if (da['state'] == '0') {
                            this.desserts.splice(index, 1)

                        } else {
                            confirm(da['state'] + ':' + da['data'])
                        }
                    }, error => {
                        confirm(error)

                    });
            },

            close() {
                this.dialog = false
                setTimeout(() => {
                    this.updateDefautItemID()
                }, 300)
            },

            save() {
                if (this.$refs.form.validate()) {
                    api.putRule(api.getToken(this.$store.state.proxy.user, this.$store.state.proxy.password), this.editedItem,
                        da => {
                            if (da['state'] == '0') {
                                if (this.editedIndex > -1) {
                                    Object.assign(this.desserts[this.editedIndex], this.editedItem)
                                } else {
                                    this.desserts.push(this.editedItem)
                                }
                                this.close()
                            } else {
                                confirm(da['state'] + ':' + da['data'])
                            }
                        }, error => {
                            confirm(error)

                        });
                }


            },
            matching: function (item) {

                if (this.search === '') {
                    return true
                } else {
                    for (let index in item) {


                        if (item[index] && ((item[index] + '').toUpperCase().indexOf(this.search.toUpperCase()) >= 0)) {
                            console.log(item[index])
                            return true;

                        }
                    }
                    return false
                }
            }
        }
    }
</script>

<style scoped>

</style>