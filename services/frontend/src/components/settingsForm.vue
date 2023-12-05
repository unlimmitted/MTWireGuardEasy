<template>
  <div class="sett">
    <div class="settingsWindow">
      <div class="settingsMain">
        <h2 class="settingsTitle">MikroTik Settings</h2>
        <div class="settingsContainer">
          <h2>Local WG settings</h2>
          <div class="row">
            <div class="col-md-6 mb-3">
              <label for="localWgNetwork">Local WG network</label>
              <input v-model="settings.localWgNetwork" type="text" class="form-control" id="localWgNetwork"
                     placeholder=""
                     required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="localWgEndpoint">Local WG endpoint</label>
              <input v-model="settings.localWgEndpoint" type="text" class="form-control" id="localWgEndpoint"
                     placeholder="" required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="localWgEndpointPort">Local WG endpoint port</label>
              <input v-model="settings.localWgEndpointPort" type="text"
                     class="form-control" id="localWgEndpointPort"
                     placeholder="" required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
          </div>
          <h2>External WG settings</h2>
          <div class="row">
            <div class="col-md-6 mb-3">
              <label for="ipAddress">IP Address</label>
              <input v-model="settings.ipAddress" type="text" class="form-control" id="ipAddress" placeholder=""
                     required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="allowedAddress">Allowed Address</label>
              <input v-model="settings.allowedAddress" type="text" class="form-control" id="allowedAddress" placeholder=""
                     required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="endpoint">Endpoint</label>
              <input v-model="settings.endpoint" type="text" class="form-control" id="endpoint" placeholder=""
                     required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="endpointPort">Endpoint port</label>
              <input v-model="settings.endpointPort" type="text" class="form-control" id="endpointPort" placeholder=""
                     required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="publicKey">Public Key</label>
              <input v-model="settings.publicKey" type="text" class="form-control" id="publicKey" placeholder=""
                     required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="privateKey">Private Key</label>
              <input v-model="settings.privateKey" type="text" class="form-control" id="privateKey" placeholder=""
                     required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="presharedKey">Preshared Key (if exists)</label>
              <input v-model="settings.presharedKey" type="text" class="form-control" id="presharedKey" placeholder=""
                     required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
          </div>
          <h2>Other MT settings</h2>
          <div class="row">
            <div class="col-md-6 mb-3">
              <label for="localNetwork">Local Network</label>
              <input v-model="settings.localNetwork" type="text" class="form-control" id="localNetwork"
                     placeholder="" required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <label for="wanInterfaceName">Wan Interface name</label>
              <input v-model="settings.wanInterfaceName" type="text" class="form-control" id="wanInterfaceName"
                     placeholder="" required="">
              <div class="invalid-feedback">
                Field is required.
              </div>
            </div>
          </div>
          <div class="checkBox">
            <div class="form-check form-switch">
              <input class="form-check-input" v-model="settings.portForward"
                     @click="this.settings.portForward = !this.settings.portForward" type="checkbox"
                     id="portForwardCheckBox">
              <label class="form-check-label" for="portForwardCheckBox">Click if you want the configurator to
                automatically forward ports (NAT LoopBack)</label>
            </div>
          </div>
          <div class="save-btn">
            <button @click="showModalWindow" type="button" class="btn btn-block btn-success">Go</button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="modalSettings">
    <div class="modalSettings__main">
      <h2 class="modalSettings__title">Confirm action</h2>
      <div class="modal__container">
        <p>After clicking the "Save" button, the VPN will automatically configure in accordance with the specified
          settings it is <strong>recommended to make a backup copy of your router before starting.</strong></p>
        <div class="btnModal">
          <button class="btn btn-success" @click="saveSettings">Save</button>
          <button class="btn btn-danger" @click="closeModalWindow">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import QrcodeVue from "qrcode.vue";

export default {
  components: {QrcodeVue},
  data() {
    return {
      settings: {
        "localWgNetwork": "",
        "localWgEndpoint": "",
        "localWgEndpointPort": "",
        "localNetwork": "",
        "ipAddress": "",
        "allowedAddress": "",
        "endpoint": "",
        "endpointPort": "",
        "publicKey": "",
        "privateKey": "",
        "presharedKey": "",
        "wanInterfaceName": "",
        "portForward": false,
        "token": this.$cookies.get("session")
      },
    }
  },
  methods: {
    showModalWindow() {
      const modalElm = document.querySelector('.modalSettings')
      modalElm.style.opacity = 1
      modalElm.style.transition = "opacity 300ms easy-in-out"
      modalElm.style.display = 'flex'
      modalElm.style.visibility = 'visible'
      modalElm.opacity = 1
    },
    closeModalWindow() {
      const modalElm = document.querySelector('.modalSettings')
      modalElm.style.display = 'none'
    },
    saveSettings() {
      this.closeModalWindow()
      this.$emit('sendMtSettings', this.settings)
    },
  }
}
</script>

<style scoped>
.checkBox {
  font-size: 13px;
  margin-bottom: 15px;
}

.btnModal {
  float: right;
}

.modalSettings {
  visibility: hidden;
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
}

.modalSettings__main {
  position: relative;
  max-width: 600px;
  background-color: white;
  margin: auto;
  border-radius: 16px;
  padding-left: 30px;
  padding-right: 30px;
  padding-bottom: 30px;

}

.modalSettings__title {
  padding-top: 20px;
  margin-bottom: 20px;
}

.settingsMain {
  max-width: 600px;
  background-color: white;
  position: relative;
  margin: auto;
  border-radius: 16px;
  padding: 30px;

}

.settingsTitle {
  padding-top: 10px;
  margin-bottom: 20px;
}

@media (max-width: 430px) and (max-height: 932px) {

  .save-btn {
    padding-bottom: 20px;
  }

  h2 {
    font-size: 25px;
  }

  .settingsTitle {
    padding: 10px;
    font-size: 35px;
  }

  .settingsContainer {
    padding-left: 15px;
    padding-right: 15px;
  }

  .settingsMain {
    font-size: 16px;
    width: 500px;
    padding: 0;
    position: relative;
    max-width: 370px;
  }
}


</style>