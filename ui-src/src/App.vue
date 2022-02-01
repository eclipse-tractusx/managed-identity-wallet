<template>
  <div id="app">
    <div id="nav">
      <img src="@/assets/logo.svg" style="height: 50px; float: left; margin-right: 20px; "/>
      <router-link to="/">Custodian Dashboard</router-link>
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
