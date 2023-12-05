<template>
  <div class="lead">
    <div class="container myContainer">
      <h1 class="title">MT WireGuard Easy</h1>
      <input id="title" placeholder="Type peer name" type="text" v-model="peer.comment"
             class="form-control mr-sm-2 myInput">
      <button type="submit" id="create" @click="createPeer" class="btn btn-lg btn-block btn-primary">
        Create
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'clientForm',
  data() {
    return {
      peer: {
        comment: ''
      }
    }
  },
  methods: {
    createPeer() {
      this.$emit('createPeer', {comment: this.peer.comment, token: this.$cookies.get("session")})
      this.peer = {
        comment: ''
      }
    },
  }
}

</script>


<style scoped>
.title {
  color: white;
  text-align: left
}

.myInput {
  margin-bottom: 20px;
}
.myContainer {
  padding-top: 8%
}

@media (max-width: 430px) and (max-height: 932px){
  .myContainer {
    padding-top: 0;
  }
  .myInput{
    width: 377px;
  }

  .title {
    font-size: 40px;
  }

  .btn-block {
    width: 377px;
  }
  .lead {
    padding-left: 2.5%;
  }
}

</style>