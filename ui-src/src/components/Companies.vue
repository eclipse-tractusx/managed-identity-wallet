<template>
  <div class="companies">
    <div class="drawer">
       <div class="selected">Administration</div>
    </div>
    <div class="list">
      <ul>
        <li v-for="company in companies" :key="company.bpn" @click="loadDetails(company.bpn)">
          <img src="@/assets/wallet.svg" style="margin-right: 15px; width:75px; float: left;"/><div style="float:left;">{{ company.bpn }}<br>{{ company.name }}<br>{{ company.createdAt.replace('T', ' ').substring(0, 19) }}</div>
        </li>
      </ul>
      <div class="json">
        <pre>{{ details }}</pre>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';

declare interface Company {
  bpn: string,
  name: string,
  createdAt: string,
  publicKey: string
}

declare interface WalletResult {
  did: string,
  createdAt: string
  publicKey: string
  vcs: string[]
}

declare interface CompaniesResult {
  bpn: string,
  name: string,
  wallet: WalletResult
}

export default Vue.extend({
  data() {
    return {
      companies: [] as Company[],
      details: ''
    };
  },
  mounted() {
    var c = this.companies;
    fetch('/ui/companies')
      .then(response => response.json() as Promise<CompaniesResult[]>)
      .then(data => {
        console.log(data)
        // replacing current list of companies
        c.length = 0
        for (let d of data) {
          const company : Company = {
            bpn: d.bpn,
            name: d.name,
            createdAt: d.wallet != null ? d.wallet.createdAt : '',
            publicKey : d.wallet != null ? d.wallet.publicKey : ''
          } 
          c.push(company)
        }
      })
      .catch(error => { console.log(error) });
  },
  methods: {
    loadDetails(bpn: string) {
      fetch('/ui/companies/' + bpn + '/full')
      .then(response => response.json())
      .then(data => {
        console.log(data)
        if (typeof data === 'string') {
          this.details = JSON.stringify(JSON.parse(data), undefined, 2)
        } else {
          this.details = JSON.stringify(data, undefined, 2)
        }
      })
      .catch(error => { console.log(error) });
    }
  }
});
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: table-row;
  margin: 0 10px;
}
a {
  color: #42b983;
}
.companies .drawer {
  float: left;
  width: 200px;
  padding: 10px;
}
.companies .list {
  background-color: rgb(245, 245, 245);
  float: left;
  width: calc(100% - 260px);
  padding: 0 10px 0 20px;
}
.selected {
  background-color: rgb(179, 203, 45);
  color: white;
  height: 42px;
  line-height: 42px;
  padding: 3px 0 3px 27px;
}
.json {
  background: #555555;
  color: white;
}
</style>
