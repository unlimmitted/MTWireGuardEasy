<template>
  <div class="loginForm">
    <loginForm @tryMtConnect="tryMtConnect" :authErrors="authErrors"/>
  </div>
  <div class="settingsForm">
    <settings-form @sendMtSettings="sendSettings"/>
  </div>
  <div class="root">
    <div class="text-center">
      <div class="inner cover bigContainer">
        <clientForm @createPeer="addPeer"/>
        <div class="container">
          <clientsList @deletePeer="deletePeer" @changeRoutVpn="changeVpnRoute" :wgPeers="wgPeers"/>
        </div>
      </div>
    </div>
  </div>
  <error @deleteError="deleteError" :errors="errors"/>
</template>

<script>
import Error from "@/components/error.vue";
import LoginForm from "@/components/loginForm.vue";
import QrcodeVue from "qrcode.vue";
import clientForm from "@/components/clientForm.vue";
import clientsList from "@/components/clientsList.vue";
import SettingsForm from "@/components/settingsForm.vue";
import error from "@/components/error.vue";

export default {
  name: 'Main',
  components: {
    SettingsForm,
    LoginForm,
    clientsList,
    clientForm,
    QrcodeVue,
    Error,
  },
  data() {
    return {
      serverUrl: 'http://192.168.0.102:8000',
      errors: [],
      wgPeers: [],
      authErrors: ""
    }
  },
  created() {
    this.getPeers()
  },
  methods: {
    async tryMtConnect(body) {
      await this.sendRequest("POST",
          `${this.serverUrl}/api/v1/try-connect?host=${body.host}&username=${body.username}&password=${body.password}`)
      this.getPeers()
    },
    getPeers() {
      this.sendRequest("POST", `${this.serverUrl}/api/v1/peers`,
          {token: this.$cookies.get("session")})
    },
    sendSettings(settings) {
      this.sendRequest("POST", `${this.serverUrl}/api/v1/set-settings`, settings)
    },
    deleteError(index) {
      this.sendRequest("DELETE", `${this.serverUrl}/api/v1/del-error`,
          {
            id: index,
            token: this.$cookies.get("session")
          })
    },
    changeVpnRoute(routVpnProperty) {
      this.sendRequest('POST', `${this.serverUrl}/api/v1/change_vpn_rout`,
          {
            status: routVpnProperty.status,
            id: routVpnProperty.id,
            token: this.$cookies.get("session")
          })
    },
    addPeer(new_peer) {
      this.sendRequest('POST', `${this.serverUrl}/api/v1/add-peer`, new_peer)
    },
    deletePeer(clientID) {
      this.sendRequest('DELETE', `${this.serverUrl}/api/v1/del-peer`,
          {
            id: clientID,
            token: this.$cookies.get("session")
          })
    },

    async sendRequest(method, url, body = null) {
      return new Promise((resolve) => {
        const xhr = new XMLHttpRequest()

        xhr.open(method, url, true)
        xhr.responseType = 'json'
        xhr.setRequestHeader('Content-Type', 'application/json')
        xhr.setRequestHeader('charset', 'utf-8')

        xhr.onload = () => {

          const root = document.querySelector('.root')
          const settingsForm = document.querySelector('.settingsForm')
          const loginForm = document.querySelector('.loginForm')

          if (xhr.status === 401) {
            if (this.$cookies.get("session") !== null){
              this.authErrors = xhr.response["detail"]
            }
            loginForm.style.visibility = "visible"
            root.style.visibility = "hidden"
          }

          if (xhr.status > 401) {
            console.log(xhr.response)
          } else {
            resolve(xhr.response)

            this.wgPeers = xhr.response["wg_peers"]
            this.errors = xhr.response["errors"]

            if (xhr.response["access_token"] !== undefined) {
              this.$cookies.set("session", xhr.response["access_token"], 28800)
            }

            if (this.$cookies.get("session") !== null) {

              if (xhr.response["setting_status"] === true) {
                loginForm.style.visibility = 'hidden'
                root.style.visibility = 'visible'
                settingsForm.style.visibility = 'hidden'
                settingsForm.style.position = 'absolute'
              } else if (xhr.response["setting_status"] === false) {
                loginForm.style.visibility = 'hidden'
                root.style.visibility = 'hidden'
                settingsForm.style.visibility = 'visible'
              }

            }
          }
        }
        xhr.send(JSON.stringify(body))
      })
    }
  }
}
</script>

<style>

.container {
  margin-top: 20px;
}

.bigContainer {
  padding-left: 30%;
  padding-right: 30%;
}

.loginForm {
  visibility: hidden;
}

.root {
  visibility: hidden;
}

.settingsForm {
  visibility: hidden;
}

@media (max-width: 430px) and (max-height: 932px) {
  .bigContainer {
    padding-left: 2%;
    padding-right: 0;
  }

  .container {
    padding-right: 10px;
    padding-left: 0;
  }
}

</style>
