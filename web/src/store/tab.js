export default {
    state: {
        isCollapse: false, //控制菜单的是否展开
        tabsList: [
            {
                path: '/home',
                name: 'home',
                label: '首页',
                icon: 's-home',
                url: 'Home/Home',
                index: '1'
            },
        ] //面包屑
    },
    mutations: {
        collapseMenu(state) {
            state.isCollapse = !state.isCollapse
        },
        //更新面包屑
        selectMenu(state, val) {
            // console.log(val, 'val')
            //判断添加数据是否为首页
            if (val.name !== 'home') {
                // 侧边菜单 item 自带 label；首页快捷入口等直接 $router.push 时 val 是路由对象（无 label），
                // 标题取 meta.title（与 router/menu.js toNode 同源），兜底用 name/path
                const label = val.label || (val.meta && val.meta.title) || val.name || val.path
                const item = Object.assign({}, val, { label })
                // 该方法通常用于查找满足特定条件的数组中第一个元素的索引，具体条件是 item.name 等于 val.name。
                const index = state.tabsList.findIndex(item => item.name === val.name)
                if (index === -1) {
                    state.tabsList.push(item)
                }
            }
        },

        closeTag(state, item) {
            console.log(item)
            const index = state.tabsList.findIndex(val => val.name == item.name)
            state.tabsList.splice(index, 1)

        }
    }
}