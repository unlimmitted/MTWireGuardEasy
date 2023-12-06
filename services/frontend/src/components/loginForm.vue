<template>
  <div class="inner cover">
    <div class="text-center">
      <form class="form-signin">
        <img class="mb-4" src="/public/logo.png" alt="" width="230"
             height="100">
        <h1 class="h3 mb-3 font-weight-normal">Connect to MikroTik</h1>
        <div class="checkbox mb-3" v-if="authErrors !== '' ">
          <div class="authErrors" style="background-color: #DC143C; border-radius: 5px; padding: 3px">
            <p style="margin: auto"><b>{{ authErrors }}</b></p>
          </div>
        </div>
        <label for="inputHostname" class="sr-only">Hostname/IP</label>
        <input v-model="conInfo.host" id="inputHostname" class="form-control mb-3" placeholder="Hostname/IP"
               required="" autofocus="">
        <label for="inputLogin" class="sr-only">Login</label>
        <input v-model="conInfo.username" type="email" id="inputLogin" class="form-control mb-3" placeholder="Login"
               required="" autofocus="">
        <label for="inputPassword" class="sr-only">Password</label>
        <input v-model="conInfo.password" type="password" id="inputPassword" class="form-control mb-3"
               placeholder="Password" required="">
        <button class="btn btn-lg btn-primary btn-block"
                @click="tryMtConnect"
                type="button">Connect
        </button>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  props: ["authErrors"],
  data() {
    return {
      conInfo: {
        host: '',
        username: '',
        password: '',
      },
    }
  },
  methods: {
    tryMtConnect() {
      this.$emit("tryMtConnect", this.conInfo)
      this.conInfo = {host: '', username: '', password: '',}
    }
  }
}

</script>

<style scoped>
.form-signin {
  background-color: white;
  padding: 20px;
  border-radius: 15px;
}
.cover {
  top: 23%;
  position: fixed;
  padding-left: 40%;
  padding-right: 43%;
}

@media (max-width: 430px) and (max-height: 932px) {
  .cover {
    padding-left: 16.5%;
    padding-right: 0;
  }
}
</style>