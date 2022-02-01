<template>
  <div class="home">
    <Companies />
  </div>
</template>

<script>
// @ is an alias to /src
import Companies from '@/components/Companies.vue'

export default {
  name: 'Home',
  components: {
    Companies
  }
}
</script>
