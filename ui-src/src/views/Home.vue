<template>
  <div class="home">
    <Wallets />
  </div>
</template>

<script>
// @ is an alias to /src
import Wallets from '@/components/Wallets.vue'

export default {
  name: 'Home',
  components: {
    Wallets
  }
}
</script>
