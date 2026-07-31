<template>
    <el-menu :default-active="activeIndex" class="el-menu-vertical-demo" @open="handleOpen" @close="handleClose"
        :collapse="isCollapse" background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF"
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

        <el-submenu v-for="group in menuData" :key="group.index" :index="group.index">
            <template slot="title">
                <i :class="`el-icon-${group.icon}`"></i>
                <span slot="title">{{ group.label }}</span>
            </template>
            <template v-for="item in group.children">
                <el-menu-item v-if="!item.children" :key="item.index" :index="item.index" @click="clickMenu(item)">
                    <i :class="`el-icon-${item.icon}`"></i>
                    <span slot="title">{{ item.label }}</span>
                </el-menu-item>
                <el-submenu v-else :key="item.index" :index="item.index">
                    <template slot="title">
                        <i :class="`el-icon-${item.icon}`"></i>
                        <span slot="title">{{ item.label }}</span>
                    </template>
                    <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index"
                        @click="clickMenu(child)">
                        <i :class="`el-icon-${child.icon}`"></i>
                        <span slot="title">{{ child.label }}</span>
                    </el-menu-item>
                </el-submenu>
            </template>
        </el-submenu>
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
                color: #409EFF !important;
                border-left-color: #409EFF;

                i {
                    color: #409EFF;
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
    export default {
        watch: {
            '$route'(to, from) {
                this.updateMenuState(to.path);
            }
        },
        data() {
            return {
                isUniqueOpened: true,
                activeIndex: '1',
                openeds: [],
                menuData: [
                    {
                        label: '工作台',
                        icon: 's-home',
                        index: 'workbench',
                        children: [
                            { path: '/home', name: 'home', label: '数据驾驶舱', icon: 'data-line', index: 'w-1' },
                            { path: '/viewOnGoingList', name: 'viewOnGoingList', label: '委托大厅', icon: 'search', index: 'w-2' },
                            {
                                label: '我的委托',
                                icon: 's-order',
                                index: 'w-3',
                                children: [
                                    { path: '/myDelegationPublishList', name: 'myDelegationPublishList', label: '我发布的订单', icon: 'document-add', index: 'w-3-1' },
                                    { path: '/myDelegationAcceptList', name: 'myDelegationAcceptList', label: '我承接的订单', icon: 'document-checked', index: 'w-3-2' },
                                    { path: '/myDelegationProgress', name: 'myDelegationProgress', label: '履约进度', icon: 's-flag', index: 'w-3-3' }
                                ]
                            },
                            { path: '/messageCenter', name: 'messageCenter', label: '消息中心', icon: 'bell', index: 'w-4' },
                            {
                                label: '个人中心',
                                icon: 'user',
                                index: 'w-5',
                                children: [
                                    { path: '/myInfo', name: 'myInfo', label: '基础信息', icon: 'user-solid', index: 'w-5-1' },
                                    { path: '/creditProfile', name: 'creditProfile', label: '信用档案', icon: 'medal', index: 'w-5-2' }
                                ]
                            }
                        ]
                    },
                    {
                        label: '平台管理中心',
                        icon: 's-tools',
                        index: 'admin',
                        children: [
                            {
                                label: '订单管理',
                                icon: 's-order',
                                index: 'a-1',
                                children: [
                                    { path: '/publishedList', name: 'publishedList', label: '全部委托列表', icon: 'tickets', index: 'a-1-1' },
                                    { path: '/draftList', name: 'draftList', label: '草稿与审核', icon: 'document', index: 'a-1-2' },
                                    { path: '/auditList', name: 'auditList', label: '发布与接收', icon: 's-check', index: 'a-1-3' },
                                    { path: '/expireDelegationList', name: 'expireDelegationList', label: '未完成委托', icon: 'warning-outline', index: 'a-1-4' },
                                    { path: '/delegationUpdateRecords', name: 'delegationUpdateRecords', label: '履约记录查询', icon: 's-flag', index: 'a-1-5' }
                                ]
                            },
                            {
                                label: '用户管理',
                                icon: 'user',
                                index: 'a-2',
                                children: [
                                    { path: '/userList', name: 'userList', label: '用户列表', icon: 'user', index: 'a-2-1' }
                                ]
                            },
                            {
                                label: '内容管控',
                                icon: 'lock',
                                index: 'a-3',
                                children: [
                                    { path: '/systemBulletinList', name: 'systemBulletinList', label: '公告管理', icon: 'chat-dot-round', index: 'a-3-1' },
                                    { path: '/systemNoticeList', name: 'systemNoticeList', label: '系统通知', icon: 'chat-line-round', index: 'a-3-2' },
                                    { path: '/sensitiveWord', name: 'sensitiveWord', label: '敏感词配置', icon: 'lock', index: 'a-3-3' }
                                ]
                            },
                            {
                                label: '委托设置',
                                icon: 'menu',
                                index: 'a-4',
                                children: [
                                    { path: '/delegationType', name: 'delegationType', label: '委托类别', icon: 'menu', index: 'a-4-1' }
                                ]
                            },
                            {
                                label: '消息管理',
                                icon: 'bell',
                                index: 'a-5',
                                children: [
                                    { path: '/notificationReadStatus', name: 'notificationReadStatus', label: '消息通知', icon: 's-comment', index: 'a-5-1' },
                                    { path: '/notifications', name: 'notifications', label: '消息列表', icon: 'chat-dot-square', index: 'a-5-2' }
                                ]
                            }
                        ]
                    }
                ]
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
            userPermissions(type) {
                // 普通用户只保留「工作台」，管理员展示全部
                if (type !== 'ADMIN') {
                    this.menuData = this.menuData.filter(group => group.index !== 'admin');
                }
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
                    this.activeIndex = 'w-1';
                }
            }
        },
        mounted() {
            this.setUserInfo()
            this.userPermissions(this.$store.state.userInfo.userType)
            this.initAside()
        },
        computed: {
            isCollapse() {
                return this.$store.state.tab.isCollapse
            }
        }
    }
</script>
