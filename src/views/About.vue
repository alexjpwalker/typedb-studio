<template>
  <div class="about">
    <h1>This is an about page</h1>
      <div v-if="databases">
          <p v-for="database in databases">{{ database }}</p>
      </div>
  </div>
</template>

<script>
    import * as process from 'process';
import { GraknClient } from "grakn-client/rpc/GraknClient";

export default {
  name: 'About',
  data() {
      return {
          databases: null
      };
  },
    mounted() {
      console.log(process);
      let client = new GraknClient('localhost:1729');
      client.databases().all().then((databases) => {
          this.databases = databases;
      });
    },
};

</script>
