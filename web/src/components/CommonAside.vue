<template>
    <el-menu :default-active="activeIndex" class="el-menu-vertical-demo" @open="handleOpen" @close="handleClose"
        :collapse="isCollapse" background-color="#304156" text-color="#bfcbd9" active-text-color="var(--ce-primary)"
        :unique-opened="isUniqueOpened">
        <div class="sidebar-logo-container" :class="{'collapse': isCollapse}">
            <transition name="sidebarLogoFade">
                <router-link v-if="isCollapse" key="collapse" class="sidebar-logo-link" to="/">
                    <img src="../assets/logo.png" class="sidebar-logo">
                </router-link>
                <router-link v-else key="expand" class="sidebar-logo-link" to="/">
                    <img src="../assets/logo.png" class="sidebar-logo">
                    <h1 class="sidebar-title">校园委托平台</h1>
                </router-link>
            </transition>
        </div>

        <template v-for="item in menuData">
            <el-menu-item v-if="!item.children" :key="item.index" :index="item.index" @click="clickMenu(item)">
                <i :class="`el-icon-${item.icon}`"></i>
                <span slot="title">{{ item.label }}</span>
            </el-menu-item>
            <el-submenu v-else :key="item.index" :index="item.index">
                <template slot="title">
                    <i :class="`el-icon-${item.icon}`"></i>
                    <span slot="title">{{ item.label }}</span>
                </template>
                <template v-for="child in item.children">
                    <el-menu-item v-if="!child.children" :key="child.index" :index="child.index" @click="clickMenu(child)">
                        <i :class="`el-icon-${child.icon}`"></i>
                        <span slot="title">{{ child.label }}</span>
                    </el-menu-item>
                    <el-submenu v-else :key="child.index" :index="child.index">
                        <template slot="title">
                            <i :class="`el-icon-${child.icon}`"></i>
                            <span slot="title">{{ child.label }}</span>
                        </template>
                        <el-menu-item v-for="grand in child.children" :key="grand.index" :index="grand.index"
                            @click="clickMenu(grand)">
                            <i :class="`el-icon-${grand.icon}`"></i>
                            <span slot="title">{{ grand.label }}</span>
                        </el-menu-item>
                    </el-submenu>
                </template>
            </el-submenu>
        </template>
    </el-menu>
</template>

<style lang="less" scoped>
    .el-menu-vertical-demo:not(.el-menu--collapse) {
        width: 240px;
        min-height: 400px;
    }

    .el-menu {
        height: 100vh;
        border: none;
        background-color: #304156;
        box-shadow: 0 2px 6px rgba(0, 21, 41, 0.35);

        .el-menu-item {
            font-size: 14px;
            transition: all 0.3s;
            border-left: 3px solid transparent;

            &:hover {
                background-color: #263445 !important;
                color: #fff !important;
                i { color: #fff; }
            }

            i {
                color: #bfcbd9;
                margin-right: 12px;
                font-size: 18px;
                transition: color 0.3s;
            }

            &.is-active {
                background-color: #1f2d3d !important;
                color: var(--ce-primary) !important;
                border-left-color: var(--ce-primary);

                i {
                    color: var(--ce-primary);
                }
            }
        }

        .el-submenu {
             /deep/ .el-submenu__title {
                font-size: 14px;

                &:hover {
                    background-color: #263445 !important;
                }

                i {
                    color: #bfcbd9;
                    margin-right: 12px;
                    font-size: 18px;
                }
            }
        }
    }

    .sidebar-logo-container {
        position: relative;
        width: 100%;
        height: 60px;
        line-height: 60px;
        background: #2b2f3a;
        text-align: center;
        overflow: hidden;
        box-shadow: 0 1px 4px rgba(0,21,41,.08);

        & .sidebar-logo-link {
            height: 100%;
            width: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            text-decoration: none;

            & .sidebar-logo {
                width: 32px;
                height: 32px;
                vertical-align: middle;
                margin-right: 12px;
            }

            & .sidebar-title {
                display: inline-block;
                margin: 0;
                color: #fff;
                font-weight: 600;
                line-height: 50px;
                font-size: 18px;
                font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
                vertical-align: middle;
            }
        }

        &.collapse {
            .sidebar-logo {
                margin-right: 0px;
            }
        }
    }
</style>

<script>
    import { buildMenu } from '../router/menu'

    export default {
        watch: {
            '$route'(to, from) {
                this.updateMenuState(to.path);
            }
        },
        data() {
            return {
                isUniqueOpened: true,
                activeIndex: '/home',
                openeds: [],
                menuData: []
            };
        },
        methods: {
            handleOpen(key, keyPath) {
                console.log(key, keyPath);
            },
            handleClose(key, keyPath) {
                console.log(key, keyPath);
            },
            clickMenu(item) {
                console.log("点击菜单", item)
                if (this.$route.path !== item.path) {
                    this.$router.push(item.path)
                }
                this.$store.commit('selectMenu', item)
            },
            refreshPage() {
                this.$router.go(0);
            },
            setUserInfo() {
                const userInfo = localStorage.getItem('TaskUser')
                if (userInfo) {
                    this.$store.commit('loginUser', JSON.parse(userInfo))
                }
            },
            extractPathsAndIndices() {
                const result = [];
                const walk = (items) => {
                    items.forEach(item => {
                        if (item.children) {
                            walk(item.children);
                        } else {
                            result.push({ path: item.path, index: item.index });
                        }
                    });
                };
                walk(this.menuData);
                return result;
            },
            initAside() {
                const currentPath = this.$route.path;
                const walk = (items) => {
                    items.forEach(item => {
                        if (item.children) {
                            walk(item.children);
                        } else if (item.path === currentPath) {
                            this.$store.commit('selectMenu', item);
                            this.activeIndex = item.index;
                        }
                    });
                };
                walk(this.menuData);
            },
            updateMenuState(path) {
                const activeItem = this.extractPathsAndIndices().find(item => item.path === path);
                if (activeItem) {
                    this.activeIndex = activeItem.index;
                } else {
                    this.activeIndex = '/home';
                }
            }
        },
        mounted() {
            this.setUserInfo()
            this.menuData = buildMenu(this.$router.options.routes, this.$store.state.userInfo.userType)
            this.initAside()
        },
        computed: {
            isCollapse() {
                return this.$store.state.tab.isCollapse
            }
        }
    }
</script>
