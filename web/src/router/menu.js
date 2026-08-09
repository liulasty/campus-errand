// web/src/router/menu.js
// 从路由配置生成按角色过滤的侧边菜单（单一数据源），纯函数无依赖

function toNode(record) {
    return {
        path: record.path,
        name: record.name,
        label: record.meta.title,
        icon: record.meta.icon,
        index: record.path
    }
}

export function buildMenu(routes, userType) {
    const mainRoute = routes.find(r => r.path === '/main')
    if (!mainRoute || !mainRoute.children) {
        return []
    }
    const menu = []
    for (const group of mainRoute.children) {
        // 无标题（默认子路由/遗留路由/详情页）不进菜单
        if (!group.meta || !group.meta.title) {
            continue
        }
        // 分组角色过滤（子路由继承分组角色，无需逐层再判）
        if (group.meta.roles && !group.meta.roles.includes(userType)) {
            continue
        }
        const node = toNode(group)
        if (group.children && group.children.length) {
            node.children = group.children
                .filter(c => c.meta && c.meta.title)
                .map(c => {
                    const childNode = toNode(c)
                    if (c.children && c.children.length) {
                        childNode.children = c.children
                            .filter(cc => cc.meta && cc.meta.title)
                            .map(cc => toNode(cc))
                    }
                    return childNode
                })
        }
        menu.push(node)
    }
    return menu
}
