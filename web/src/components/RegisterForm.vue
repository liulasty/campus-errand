<template>
  <div class="formBox register">
    <form action="#" class="form" @submit.prevent="$emit('register')">
      <h2 class="title">注 册</h2>
      <input type="text" placeholder="邮箱或5-9位用户名" class="input" v-model="registerInfo.username" />
      <input type="password" placeholder="密码" class="input" v-model="registerInfo.password" />
      <input type="email" placeholder="邮箱" class="input" v-model="registerInfo.email" />
      <button class="btn">注册</button>
    </form>
  </div>
</template>

<script>
export default {
  props: {
    registerInfo: {
      type: Object,
      required: true
    }
  }
};
</script>