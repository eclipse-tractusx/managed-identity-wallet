/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the CatenaX (ng) GitHub Organisation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

<template>
  <div class="wallets">
    <div class="drawer">
       <div class="selected">Administration</div>
    </div>
    <div class="list">
      <ul>
        <li v-for="wallet in wallets" :key="wallet.bpn" @click="loadDetails(wallet.bpn)">
          <img src="@/assets/wallet.svg" style="margin-right: 15px; width:75px; float: left;"/><div style="float:left;">{{ wallet.bpn }}<br>{{ wallet.name }}<br>{{ wallet.createdAt.replace('T', ' ').substring(0, 19) }}</div>
        </li>
      </ul>
      <div class="json">
        <pre>{{ details }}</pre>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue'

declare interface Wallet {
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

export default defineComponent({
  data() {
    return {
      wallets: [] as Wallet[],
      details: '' as string,
    }
  },
  mounted() {
    var c = this.wallets;
    fetch('/ui/wallets')
      .then(response => response.json() as Promise<CompaniesResult[]>)
      .then(data => {
        console.log(data)
        // replacing current list of wallets
        c.length = 0
        for (let d of data) {
          const wallet : Wallet = {
            bpn: d.bpn,
            name: d.name,
            createdAt: d.wallet != null ? d.wallet.createdAt : '',
            publicKey : d.wallet != null ? d.wallet.publicKey : ''
          } 
          c.push(wallet)
        }
      })
      .catch(error => { console.log(error) });
  },
  methods: {
    loadDetails(bpn: string) {
      fetch('/ui/wallets/' + bpn + '/full')
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
.wallets .drawer {
  float: left;
  width: 200px;
  padding: 10px;
}
.wallets .list {
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
