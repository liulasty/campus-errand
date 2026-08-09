// web/src/router/menu.js
// 从扁平路由（meta.menu.group/submenu）组装按角色过滤的侧边菜单，纯函数无依赖

const MENU_GROUPS = {
    workbench: { title: '工作台', icon: 's-home', roles: ['USER'] },
    adminPanel: { title: '平台管理中心', icon: 's-tools', roles: ['ADMIN'] }
}

const MENU_SUBMENUS = {
    myTasks: { title: '我的委托', icon: 's-order' },
    profile: { title: '个人中心', icon: 'user' },
    delegationAdmin: { title: '委托管理', icon: 's-order' },
    userAdmin: { title: '用户管理', icon: 'user' },
    bulletinAdmin: { title: '平台公告', icon: 'chat-dot-round' },
    noticeAdmin: { title: '消息管理', icon: 'bell' },
    systemConfig: { title: '系统配置', icon: 'setting' }
}

function toNode(record) {
    return {
        path: record.path,
        name: record.name,
        label: record.meta.title,
        icon: record.meta.icon,
        index: record.path
    }
}

function hasRole(record, userType) {
    return !record.meta.roles || record.meta.roles.includes(userType)
}

export function buildMenu(routes, userType) {
    const mainRoute = routes.find(r => r.path === '/main')
    if (!mainRoute || !mainRoute.children) {
        return []
    }
    const leaves = mainRoute.children.filter(c => c.meta && c.meta.menu)

    const menu = []
    // 顶层独立项（无 group，如数据驾驶舱）
    leaves.filter(c => !c.meta.menu.group).forEach(c => {
        if (hasRole(c, userType)) {
            menu.push(toNode(c))
        }
    })
    // 分组
    for (const [gKey, gDef] of Object.entries(MENU_GROUPS)) {
        if (gDef.roles && !gDef.roles.includes(userType)) {
            continue
        }
        const groupLeaves = leaves.filter(c => c.meta.menu.group === gKey)
        if (!groupLeaves.length) {
            continue
        }
        const groupNode = { path: gKey, name: '', label: gDef.title, icon: gDef.icon, index: gKey, children: [] }
        // 组内无子分组的直接项
        groupLeaves.filter(c => !c.meta.menu.submenu).forEach(c => {
            if (hasRole(c, userType)) {
                groupNode.children.push(toNode(c))
            }
        })
        // 子分组
        for (const [sk, sDef] of Object.entries(MENU_SUBMENUS)) {
            const subLeaves = groupLeaves.filter(c => c.meta.menu.submenu === sk)
            if (!subLeaves.length) {
                continue
            }
            const subNode = { path: sk, name: '', label: sDef.title, icon: sDef.icon, index: sk, children: [] }
            subLeaves.forEach(c => {
                if (hasRole(c, userType)) {
                    subNode.children.push(toNode(c))
                }
            })
            if (subNode.children.length) {
                groupNode.children.push(subNode)
            }
        }
        if (groupNode.children.length) {
            menu.push(groupNode)
        }
    }
    return menu
}
