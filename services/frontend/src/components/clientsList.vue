<template>
  <table class="table table-bordered myTable">
    <thead>
    <tr>
      <th scope="col">№</th>
      <th scope="col">Name</th>
      <th scope="col">IP address</th>
      <th scope="col">VPN</th>
      <th scope="col">QR</th>
      <th scope="col">Delete</th>
    </tr>
    </thead>
    <tbody>
    <tr v-for="(client, index) in wgPeers"
        :key="client.id">
      <th scope="row">{{ index + 1 }}</th>
      <td>{{ client.comment.split('\n')[0] }}</td>
      <td>{{ client["allowed-address"] }}</td>
      <td>
        <div v-if="client['double-vpn']">
          <div class="form-check form-switch">
            <input class="form-check-input" type="checkbox" @click="changeDoubleVpn(index)" :id="index" checked>
            <label class="form-check-label" :for="index"></label>
          </div>
        </div>
        <div v-else>
          <div class="form-check form-switch">
            <input class="form-check-input" @click="changeDoubleVpn(index)" type="checkbox" :id="index">
            <label class="form-check-label" :for="index"></label>
          </div>
        </div>
      </td>
      <td>
        <button :data-index="index"
                @click="showQR(index)" data-type="showQR" type="submit" id="showQR" class="btn btn-success">
          <bootstrap-q-r class="qrIcon"/>
        </button>
      </td>
      <td>
        <button :data-index="index"
                @click="deletePeer(index)" data-type="remove" type="submit" id="remove"
                class="btn btn-danger">&times;
        </button>
      </td>
    </tr>
    </tbody>
  </table>
  <div class="modal">
    <div class="modal__main">
      <h2 class="modal__title"></h2>
      <div class="modal__container">
        <qrcode-vue class="qrView" @click="closeQR()" :value="qrValue" :size="250" level="H"/>
      </div>
    </div>
  </div>
</template>

<script>
import QrcodeVue from 'qrcode.vue'
import BootstrapQR from "@/components/UI/bootstrapQR.vue";

export default {
  name: 'clientsList',
  props: ['wgPeers'],
  components: {
    BootstrapQR,
    QrcodeVue,
  },
  data() {
    return {
      qrValue: ``,
    }
  },
  methods: {
    deletePeer(index) {
      let clientID = this.wgPeers[index]["id"]
      this.$emit('deletePeer', clientID)
    },

    changeDoubleVpn(index) {
      let routVpnProperty = {
        id: this.wgPeers[index].id,
        status: this.wgPeers[index]['double-vpn']
      }
      this.$emit('changeRoutVpn', routVpnProperty)
    },
    closeQR() {
      const modalElm = document.querySelector('.modal')
      modalElm.style.display = 'none'
    },
    showQR(index) {
      const modalElm = document.querySelector('.modal')
      this.qrValue = `
      [Interface]
      PrivateKey = ${this.wgPeers[index].comment.split('\n')[1]}
      Address = ${this.wgPeers[index]["allowed-address"]}
      DNS = 1.1.1.1
      MTU = 1400
      [Peer]
      PublicKey = ${this.wgPeers[index]["server-public-key"]}
      AllowedIPs = 0.0.0.0/0, ::/0
      Endpoint = ${this.wgPeers[index]["endpoint-address"]}:${this.wgPeers[index]["endpoint-port"]}
      PersistentKeepalive = 0
      `
      modalElm.style.opacity = 1
      modalElm.style.transition = "opacity 300ms easy-in-out"
      modalElm.style.display = 'flex'
      modalElm.style.visibility = 'visible'
      modalElm.opacity = 1
    },
  },
}
</script>

<style scoped>
.myTable {
  background-color: white;
  border-radius: 10px;
  border: hidden
}

.qrView {
  cursor: pointer;
}

.modal {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
}

.modal__main {
  position: relative;
  max-width: 600px;
  background-color: white;
  margin: auto;
  border-radius: 16px;
  padding-left: 30px;
  padding-right: 30px;
  padding-bottom: 30px;

}

.modal__title {
  margin-bottom: 30px;
}

@media (max-width: 430px) and (max-height: 932px) {

  .modal__main {
    margin-top: 30%;
  }

  .myTable {
    margin-left: 2.5%;
    font-size: 13px;
    width: 100px;
  }

  .btn-success {
    padding-top: 0;
    padding-left: 7px;
    width: 27px;
    height: 27px;
  }

  .qrIcon {
    width: 12px;
    height: 22px;
  }

  .btn-danger {
    padding-left: 7px;
    padding-top: 0;
    width: 27px;
    height: 27px;
  }
}


</style>