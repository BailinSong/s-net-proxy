<template>
  <div app fixed>
    <v-toolbar app>
      <v-toolbar-title class="headline text-uppercase">
        <span>Net</span>
        <span class="font-weight-light">Proxy</span>
      </v-toolbar-title>
      <v-spacer></v-spacer>
    </v-toolbar>

    <v-content app>
      <Login></Login>
    </v-content>
  </div>
</template>

<script>
  import Login from '../components/Login'

  export default {
    components: {
        Login
    }
  }
</script>
