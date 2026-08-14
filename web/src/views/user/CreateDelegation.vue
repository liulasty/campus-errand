<template>
    <div class="dl-wrap">
        <header class="dl-wrap__header">
            <div class="dl-wrap__heading">
                <span class="dl-wrap__eyebrow">DELEGATION STUDIO</span>
                <h1 class="dl-wrap__title">发布委托</h1>
                <p class="dl-wrap__sub">填写委托信息，创建草稿，审核通过后即可发布</p>
            </div>
            <div class="dl-wrap__stamp">
                <strong class="dl-wrap__stamp-num">{{ draftCount }}</strong>
                <span class="dl-wrap__stamp-label">我的草稿</span>
            </div>
        </header>

        <nav v-if="tabPanes.length" class="dl-wrap__tabs">
            <button v-for="(pane, index) in tabPanes" :key="index" type="button"
                class="dl-wrap__tab" :class="{ 'is-active': activeTab === index }"
                @click="activeTab = index">{{ pane.label }}</button>
        </nav>

        <main class="dl-wrap__body">
            <div v-if="tabPanes.length && activePane.id != null" class="dl-wrap__inner">
                <div class="dl-wrap__intro">
                    <span class="dl-wrap__intro-mark"></span>
                    {{ activePane.DelegationType || '请填写以下信息发布新的委托任务' }}
                </div>
                <Delegation :DelegationType="activePane.label" :id="activePane.id"
                    :description="activePane.DelegationType" :tasks="tasks" :taskTypeOption="taskTypeOption"
                    @childEvent="getTaskDraft" />
            </div>
            <div v-else class="dl-wrap__loading" v-loading="true" element-loading-text="加载委托类型…" />
        </main>
    </div>
</template>
<script>
    import Delegation from '@/components/Delegation.vue';

    import { getTaskDraftById, getUserInfo } from '@/api/index';
    import { getTaskCategoriesUser } from '@/api/user';

    import { AUTH_STATUS, TASK_STATUS } from '@/constants/enums'

    export default {
        components: {
            Delegation
        },
        data() {
            return {
                AUTH_STATUS,
                TASK_STATUS,
                activeTab: 0,
                tabPanes: [],
                tasks: [],
                taskType: {},
                taskTypeOption: []
            }
        },
        computed: {
            activePane() {
                return this.tabPanes[this.activeTab] || this.tabPanes[0] || {};
            },
            draftCount() {
                return (this.tasks || []).filter(t => t.status === TASK_STATUS.DRAFT).length;
            }
        },
        methods: {
            setContext() {
                //获取类型
                getTaskCategoriesUser().then((data) => {
                    if (data.data.code === 1 && data.data.data.length > 0) {
                        this.tabPanes = data.data.data.map(category => ({
                            label: category.name,
                            id: category.id,
                            DelegationType: category.description,
                        }));
                        this.taskType = {};
                        this.taskTypeOption = data.data.data.map(category => {
                            this.taskType[`${category.id}`] = category.name;
                            return { label: category.name, value: category.id };
                        });
                        console.log("类型数组", this.taskTypeOption);
                    }
                }).catch(err => {
                    console.error('获取委托类型失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                });
            },
            //获取草稿
            getTaskDraft() {
                this.setContext();
                console.log("获取草稿");
                getTaskDraftById(this.$store.state.userInfo.userId).then((data) => {
                    const records = data.data.data || [];
                    records.forEach(element => {
                        element.type = this.taskType[`${element.type}`];
                    });
                    this.tasks = records;
                }).catch(err => {
                    console.error('获取草稿失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                });
            },
            //查询用户认证信息
            getUserInfoBefore() {
                console.log("获取用户信息");
                getUserInfo(this.$store.state.userInfo.userId).then((data) => {
                    if (data.data.code === 1) {
                        this.userInfo = data.data.data;
                        if (this.userInfo.authStatus != AUTH_STATUS.AUTHENTICATED) {
                            this.$message({
                                message: "请先完成认证",
                                type: 'error'
                            });
                            this.$router.push("/userInfo");
                        }
                    } else {
                        console.log("用户信息", data.data.msg);
                    }
                }).catch(err => {
                    console.error('获取用户信息失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                })
            },
        },
        mounted() {
            this.getUserInfoBefore();
            this.setContext();
            this.getTaskDraft();
        }
    }
</script>

<style lang="less" scoped>
    .dl-wrap {
        --paper: #f5f1e8;
        --card: #fffcf5;
        --ink: #2a3a30;
        --ink-soft: #5f6b62;
        --muted: #9aa198;
        --line: #e6ddc9;
        --line-strong: #d8ccb2;
        --brass: #b9892c;
        --brass-deep: #96701f;
        --sage: #5f8a6f;
        --terra: #b4543a;

        position: relative;
        min-height: calc(100vh - 160px);
        padding: 6px 4px 30px;
        border-radius: 14px;
        color: var(--ink);
        font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", sans-serif;
        background:
            radial-gradient(1100px 480px at 88% -8%, rgba(185, 137, 44, 0.14), transparent 60%),
            radial-gradient(900px 420px at -6% 0%, rgba(95, 138, 111, 0.12), transparent 55%),
            var(--paper);

        /* 颗粒层置于内容之下：避免 fixed 弹层被 z-index 困住导致整页卡死 */
        &::before {
            content: "";
            position: absolute;
            inset: 0;
            z-index: 0;
            pointer-events: none;
            opacity: .55;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.045'/%3E%3C/svg%3E");
        }
    }

    .dl-wrap__header {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        padding: 34px 40px 22px;
    }

    .dl-wrap__eyebrow {
        display: block;
        margin-bottom: 10px;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 11px;
        letter-spacing: .34em;
        text-transform: uppercase;
        color: var(--brass);
    }

    .dl-wrap__title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 34px;
        font-weight: 700;
        line-height: 1.12;
        letter-spacing: .02em;
    }

    .dl-wrap__sub {
        margin: 9px 0 0;
        font-size: 13px;
        letter-spacing: .08em;
        color: var(--muted);
    }

    .dl-wrap__stamp {
        text-align: right;
        padding-left: 18px;
        border-left: 2px solid var(--brass);
    }

    .dl-wrap__stamp-num {
        display: block;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 42px;
        font-weight: 700;
        line-height: 1;
        color: var(--ink);
    }

    .dl-wrap__stamp-label {
        display: block;
        margin-top: 7px;
        font-size: 12px;
        letter-spacing: .22em;
        color: var(--muted);
    }

    .dl-wrap__tabs {
        position: relative;
        z-index: 1;
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
        margin: 0 40px;
        border-bottom: 1px solid var(--line);
    }

    .dl-wrap__tab {
        appearance: none;
        border: 0;
        background: transparent;
        position: relative;
        padding: 13px 20px 15px;
        font-family: inherit;
        font-size: 15px;
        letter-spacing: .05em;
        color: var(--ink-soft);
        cursor: pointer;
        transition: color .25s;
    }

    .dl-wrap__tab::after {
        content: "";
        position: absolute;
        left: 20px;
        right: 20px;
        bottom: -1px;
        height: 2px;
        background: var(--brass);
        transform: scaleX(0);
        transform-origin: left;
        transition: transform .32s ease;
    }

    .dl-wrap__tab:hover {
        color: var(--ink);
    }

    .dl-wrap__tab.is-active {
        color: var(--ink);
        font-weight: 600;
    }

    .dl-wrap__tab.is-active::after {
        transform: scaleX(1);
    }

    .dl-wrap__body {
        position: relative;
        z-index: 1;
        padding: 20px 40px 10px;
    }

    .dl-wrap__intro {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 18px;
        padding: 14px 18px;
        background: rgba(255, 252, 245, .72);
        border: 1px solid var(--line);
        border-radius: 10px;
        font-size: 13px;
        letter-spacing: .04em;
        color: var(--ink-soft);
    }

    .dl-wrap__intro-mark {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--brass);
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .18);
        flex-shrink: 0;
    }

    .dl-wrap__loading {
        min-height: 260px;
    }
</style>
