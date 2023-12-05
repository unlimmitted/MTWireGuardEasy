<template>
  <div class="errors">
    <div class="error" v-for="(item, index) in errors" :key="index">
      <button type="button" class="close" aria-label="Close">
        <span @click="deleteError(index)" aria-hidden="true">&times;</span>
      </button>
      <h3 class="title">Error:</h3>
      <p>{{ item.error }}</p>
    </div>
  </div>
</template>

<script>

export default {
  name: "error",
  props: {
    errors: Object,
    index: Number
  },
  methods: {
    deleteError(index) {
      this.$emit('deleteError', index)
    }
  },
}
</script>

<style scoped>
.error {
  padding: 10px;
  border: solid;
  width: 330px;
  height: 120px;
  background: #DC143C;
  position: relative;
  margin: 15px;
  border-radius: 10px;
}

.errors {
  word-wrap: break-word;
  position: absolute;
  top: 15px;
  right: 15px;

}

@media (max-width: 430px) and (max-height: 932px) {
  .error {
    width: 180px;
    height: 76px;
  }
  .errors {
    font-size: 12px;
  }
  .title {
    font-size: 20px;
  }
}
</style>