/********************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
  <div id="app">
    <div id="nav">
      <img src="@/assets/logo.svg" style="height: 50px; float: left; margin-right: 20px; "/>
      <router-link to="/">Managed Identity Wallets Dashboard</router-link>
      <!-- router-link to="/about">About</router-link-->
      <div class="clearfix"></div>
    </div>
    <router-view/>
  </div>
</template>

<style>
#app {
  font-family: "Segoe UI", "Segoe UI Web (West European)", "Segoe UI", -apple-system, BlinkMacSystemFont, Roboto, "Helvetica Neue", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: left;
  color: #2c3e50;
}

#nav {
  padding: 0px;
  margin-bottom: 10px;
}

#nav a {
  height: 50px;
  line-height: 50px;
  color: black;
  text-align: center;
  float: left;
  background: none;
}

#nav a.router-link-exact-active {
  border-bottom: solid 2px rgb(179,203, 45);
  padding-bottom: 2px;
  font-weight: normal;
  text-decoration: none;
}
.clearfix:after {
  content: "";
  display: table;
  clear: both;
}
</style>
