import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/dashboard/Home.vue'
import Main from '../views/layout/Main.vue'
import pageOne from '../views/test/PageOne.vue'
import pageTwo from '../views/test/PageTwo.vue'
import pageThree from '../views/test/PageThree.vue'
import Login from '../views/auth/Login.vue'
import Landing from '../views/public/Landing.vue'
import MyInfo from '../views/user/MyInfo.vue'
import CreateDelegation from '@/views/user/CreateDelegation.vue'
import UserList from '@/views/admin/UserList.vue'
import RealNameAudit from '@/views/admin/RealNameAudit.vue'
import DraftList from '@/views/admin/DraftList.vue'
import AuditList from '@/views/admin/AuditList.vue'
import PublishedList from '@/views/admin/PublishedList.vue'
import DelegationUpdateRecords from '@/views/admin/DelegationUpdateRecords.vue'
import SystemBulletinList from '@/views/admin/SystemBulletinList.vue'
import SystemNoticeList from '@/views/admin/SystemNoticeList.vue'
import DelegationType from '@/views/admin/DelegationType.vue'
import ViewOnGoingList from '@/views/user/ViewOnGoingList.vue'
import MyDelegationAcceptList from '@/views/user/MyDelegationAcceptList.vue'
import MyDelegationPublishList from '@/views/user/MyDelegationPublishList.vue'
import ExpireDelegationList from '@/views/admin/ExpireDelegationList.vue'
import Notifications from '@/views/admin/Notifications.vue'
import NotificationReadStatus from '@/views/admin/NotificationReadStatus.vue'
import MessageCenter from '@/views/user/MessageCenter.vue'
import MyDelegationProgress from '@/views/user/MyDelegationProgress.vue'
import CreditProfile from '@/views/user/CreditProfile.vue'
import SensitiveWordConfig from '@/views/admin/SensitiveWordConfig.vue'

Vue.use(VueRouter)
const routes = [
    // 登录页面
    {
        path: '/login',
        name: 'login',
        component: Login
    },
    // 主要页面
    {
        path: '/main',
        component: Main,
        children: [
            {
                path: '',
                components: {
                    default: Home
                }
            },
            // 共享首页（两级角色可见）
            {
                path: '/home',
                name: 'home',
                component: Home,
                meta: { title: '数据驾驶舱', icon: 'data-line', roles: ['ADMIN', 'USER'] }
            },
            // 用户工作台（仅 USER）
            {
                path: '/workbench',
                meta: { title: '工作台', icon: 's-home', roles: ['USER'] },
                children: [
                    {
                        path: '/viewOnGoingList',
                        name: 'viewOnGoingList',
                        component: ViewOnGoingList,
                        meta: { title: '委托大厅', icon: 'search' }
                    },
                    {
                        path: '/myTasks',
                        meta: { title: '我的委托', icon: 's-order' },
                        children: [
                            {
                                path: '/createDelegation',
                                name: 'createDelegation',
                                component: CreateDelegation,
                                meta: { title: '发布委托', icon: 'edit-outline' }
                            },
                            {
                                path: '/myDelegationPublishList',
                                name: 'myDelegationPublishList',
                                component: MyDelegationPublishList,
                                meta: { title: '我发布的订单', icon: 'document-add' }
                            },
                            {
                                path: '/myDelegationAcceptList',
                                name: 'myDelegationAcceptList',
                                component: MyDelegationAcceptList,
                                meta: { title: '我承接的订单', icon: 'document-checked' }
                            }
                        ]
                    },
                    {
                        path: '/messageCenter',
                        name: 'messageCenter',
                        component: MessageCenter,
                        meta: { title: '消息中心', icon: 'bell' }
                    },
                    {
                        path: '/profile',
                        meta: { title: '个人中心', icon: 'user' },
                        children: [
                            {
                                path: '/myInfo',
                                name: 'myInfo',
                                component: MyInfo,
                                meta: { title: '基础信息', icon: 'user-solid' }
                            },
                            {
                                path: '/creditProfile',
                                name: 'creditProfile',
                                component: CreditProfile,
                                meta: { title: '信用档案', icon: 'medal' }
                            }
                        ]
                    },
                    // 详情页：不进菜单，但属于 USER 域（继承工作台 USER 角色，防 ADMIN 绕过）
                    {
                        path: '/myDelegationProgress',
                        name: 'myDelegationProgress',
                        component: MyDelegationProgress
                    }
                ]
            },
            // 平台管理中心（仅 ADMIN）
            {
                path: '/adminPanel',
                meta: { title: '平台管理中心', icon: 's-tools', roles: ['ADMIN'] },
                children: [
                    {
                        path: '/delegationAdmin',
                        meta: { title: '委托管理', icon: 's-order' },
                        children: [
                            {
                                path: '/publishedList',
                                name: 'publishedList',
                                component: PublishedList,
                                meta: { title: '全部委托', icon: 'tickets' }
                            },
                            {
                                path: '/auditList',
                                name: 'auditList',
                                component: AuditList,
                                meta: { title: '委托审核', icon: 's-check' }
                            },
                            {
                                path: '/expireDelegationList',
                                name: 'expireDelegationList',
                                component: ExpireDelegationList,
                                meta: { title: '未完成委托', icon: 'warning-outline' }
                            },
                            {
                                path: '/delegationUpdateRecords',
                                name: 'delegationUpdateRecords',
                                component: DelegationUpdateRecords,
                                meta: { title: '履约记录查询', icon: 's-flag' }
                            }
                        ]
                    },
                    {
                        path: '/userAdmin',
                        meta: { title: '用户管理', icon: 'user' },
                        children: [
                            {
                                path: '/userList',
                                name: 'userList',
                                component: UserList,
                                meta: { title: '用户列表', icon: 'user' }
                            },
                            {
                                path: '/admin/realNameAudit',
                                name: 'realNameAudit',
                                component: RealNameAudit,
                                meta: { title: '实名审核', icon: 'postcard' }
                            }
                        ]
                    },
                    {
                        path: '/bulletinAdmin',
                        meta: { title: '平台公告', icon: 'chat-dot-round' },
                        children: [
                            {
                                path: '/systemBulletinList',
                                name: 'systemBulletinList',
                                component: SystemBulletinList,
                                meta: { title: '公告管理', icon: 'chat-dot-round' }
                            }
                        ]
                    },
                    {
                        path: '/noticeAdmin',
                        meta: { title: '消息管理', icon: 'bell' },
                        children: [
                            {
                                path: '/notificationReadStatus',
                                name: 'notificationReadStatus',
                                component: NotificationReadStatus,
                                meta: { title: '消息管理', icon: 's-comment' }
                            }
                        ]
                    },
                    {
                        path: '/systemConfig',
                        meta: { title: '系统配置', icon: 'setting' },
                        children: [
                            {
                                path: '/delegationType',
                                name: 'delegationType',
                                component: DelegationType,
                                meta: { title: '委托分类配置', icon: 'menu' }
                            },
                            {
                                path: '/sensitiveWord',
                                name: 'sensitiveWord',
                                component: SensitiveWordConfig,
                                meta: { title: '敏感词管控', icon: 'lock' }
                            }
                        ]
                    }
                ]
            },
            // 未接入菜单的遗留路由（不进菜单，保持原可访问性；无角色限制）
            { path: '/draftList', name: 'draftList', component: DraftList },
            { path: '/systemNoticeList', name: 'systemNoticeList', component: SystemNoticeList },
            { path: '/notifications', name: 'notifications', component: Notifications },
            { path: '/page1', name: 'page1', component: pageOne },
            { path: '/page2', name: 'page2', component: pageTwo },
            { path: '/page3', name: 'page3', component: pageThree }
        ]
    },
    // 默认页面
    {
        path: '/',
        name: 'landing',
        component: Landing
    }
]


// 添加的方法
// 这段代码是为了解决重复点击导航时控制台出现报错的问题。具体来说，它重写了VueRouter的push方法。

// 在原有的代码中，VueRouter的push方法会返回一个Promise对象，当导航成功时，Promise会被resolve，当导航失败时，Promise会被reject。但是，如果在短时间内多次连续点击导航按钮，会导致多个导航请求同时发出，而这些请求会在前一个请求完成之前被取消，从而导致Promise被reject，控制台会出现报错。

// 为了解决这个问题，这段代码将VueRouter的push方法进行了重写。重写后的push方法在调用原有的VueRouter的push方法之前，先将其返回的Promise对象进行了catch处理，即使导航请求被取消，也会将错误捕获并返回一个空的Promise对象，从而避免了控制台报错。

// 最后，这段代码通过export default将重写后的router对象导出，以供其他模块使用。
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
    return originalPush.call(this, location).catch(err => err)
}

const router = new VueRouter({
    routes,
    mode: 'history',
    base: '/campus_entrustment/'
})

// 读取当前登录用户角色（刷新后 vuex 为空，以 localStorage 为准）
function getUserType() {
    try {
        const taskUser = JSON.parse(localStorage.getItem('TaskUser') || '{}')
        return taskUser.userType || ''
    } catch (e) {
        return ''
    }
}

// 添加全局前置导航守卫：token + 角色
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('jwtToken');
    const LOGIN_PATH = '/login';
    const LANDING_PATH = '/';
    const publicPaths = [LOGIN_PATH, LANDING_PATH];

    if (token) {
        // 已登录用户访问登录页 → 回首页
        if (to.path === LOGIN_PATH) {
            next('/home');
            return;
        }
        // 角色校验：目标路由链中存在角色要求且当前角色不满足 → 回首页
        const userType = getUserType();
        const hasRoleRestriction = to.matched.some(r => r.meta && r.meta.roles);
        const isAllowed = to.matched.some(r => r.meta && r.meta.roles && r.meta.roles.includes(userType));
        if (hasRoleRestriction && !isAllowed) {
            next('/home');
        } else {
            next();
        }
    } else {
        // 未登录
        if (publicPaths.includes(to.path)) {
            next();
        } else {
            next(LOGIN_PATH);
        }
    }
});

export default router


