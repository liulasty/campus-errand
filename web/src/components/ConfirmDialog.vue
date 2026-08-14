<template>
    <transition name="ce-fade">
        <div v-if="visible" class="ce-confirm" @click.self="onCancel">
            <div class="ce-confirm__card" role="alertdialog" aria-modal="true">
                <span class="ce-confirm__icon" :class="'ce-confirm__icon--' + type">
                    <i :class="iconClass"></i>
                </span>
                <h3 class="ce-confirm__title">{{ title }}</h3>
                <p class="ce-confirm__message">{{ message }}</p>
                <div class="ce-confirm__actions">
                    <button type="button" class="ce-confirm__btn ce-confirm__btn--ghost" :disabled="loading"
                        @click="onCancel">{{ cancelText }}</button>
                    <button type="button" class="ce-confirm__btn" :class="'ce-confirm__btn--' + type" :disabled="loading"
                        @click="onConfirm">
                        <i v-if="loading" class="el-icon-loading"></i>
                        {{ loading ? loadingText : confirmText }}
                    </button>
                </div>
            </div>
        </div>
    </transition>
</template>

<script>
    export default {
        name: 'ConfirmDialog',
        props: {
            visible: { type: Boolean, default: false },
            title: { type: String, default: '确认操作' },
            message: { type: String, default: '' },
            confirmText: { type: String, default: '确认' },
            loadingText: { type: String, default: '处理中' },
            cancelText: { type: String, default: '取消' },
            loading: { type: Boolean, default: false },
            // danger / primary / warning
            type: { type: String, default: 'danger' }
        },
        computed: {
            iconClass() {
                if (this.type === 'danger') return 'el-icon-warning-outline';
                if (this.type === 'warning') return 'el-icon-question';
                return 'el-icon-check';
            }
        },
        methods: {
            onConfirm() {
                if (this.loading) return;
                this.$emit('confirm');
            },
            onCancel() {
                if (this.loading) return;
                this.$emit('cancel');
                this.$emit('update:visible', false);
            }
        }
    }
</script>

<style lang="less" scoped>
    .ce-confirm {
        position: fixed;
        inset: 0;
        z-index: 3000;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 24px;
        background: rgba(42, 58, 48, .46);
        -webkit-backdrop-filter: blur(3px);
        backdrop-filter: blur(3px);
    }

    .ce-confirm__card {
        width: 100%;
        max-width: 420px;
        padding: 34px 30px 26px;
        background: #fffcf5;
        border: 1px solid #e6ddc9;
        border-radius: 18px;
        box-shadow: 0 24px 60px -18px rgba(42, 58, 48, .45);
        text-align: center;
    }

    .ce-confirm__icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 56px;
        height: 56px;
        border-radius: 50%;
        margin-bottom: 18px;
        font-size: 28px;
        color: #fff;
    }

    .ce-confirm__icon--danger {
        background: #b4543a;
        box-shadow: 0 10px 24px -8px rgba(180, 84, 58, .55);
    }

    .ce-confirm__icon--primary {
        background: #2a3a30;
        box-shadow: 0 10px 24px -8px rgba(42, 58, 48, .5);
    }

    .ce-confirm__icon--warning {
        background: #b9892c;
        box-shadow: 0 10px 24px -8px rgba(185, 137, 44, .5);
    }

    .ce-confirm__title {
        margin: 0 0 12px;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 20px;
        font-weight: 700;
        letter-spacing: .02em;
        color: #2a3a30;
    }

    .ce-confirm__message {
        margin: 0;
        font-size: 13px;
        line-height: 1.8;
        color: #5f6b62;
        word-break: break-all;
    }

    .ce-confirm__actions {
        display: flex;
        justify-content: center;
        gap: 12px;
        margin-top: 26px;
    }

    .ce-confirm__btn {
        appearance: none;
        min-width: 110px;
        padding: 9px 20px;
        border-radius: 9px;
        border: 1px solid transparent;
        font-family: inherit;
        font-size: 13px;
        letter-spacing: .08em;
        cursor: pointer;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s, color .18s;
    }

    .ce-confirm__btn:disabled {
        cursor: not-allowed;
        opacity: .65;
    }

    .ce-confirm__btn--ghost {
        background: transparent;
        border-color: #e6ddc9;
        color: #5f6b62;
    }

    .ce-confirm__btn--ghost:hover {
        border-color: #b9892c;
        color: #96701f;
        background: rgba(255, 252, 245, .8);
    }

    .ce-confirm__btn--danger {
        background: #b4543a;
        color: #fdf6f2;
    }

    .ce-confirm__btn--danger:hover {
        background: #a0462f;
        transform: translateY(-1px);
        box-shadow: 0 6px 14px -6px rgba(180, 84, 58, .6);
    }

    .ce-confirm__btn--primary {
        background: #2a3a30;
        color: #f7f3ea;
    }

    .ce-confirm__btn--primary:hover {
        background: #33493c;
        transform: translateY(-1px);
        box-shadow: 0 6px 14px -6px rgba(42, 58, 48, .5);
    }

    .ce-confirm__btn--warning {
        background: #b9892c;
        color: #fdf9ec;
    }

    .ce-confirm__btn--warning:hover {
        background: #96701f;
        transform: translateY(-1px);
    }

    .ce-confirm__btn i {
        margin-right: 6px;
    }

    /* 入场动画 */
    .ce-fade-enter-active {
        transition: opacity .22s ease;
    }

    .ce-fade-enter-active .ce-confirm__card {
        transition: transform .22s ease, opacity .22s ease;
    }

    .ce-fade-enter {
        opacity: 0;
    }

    .ce-fade-enter .ce-confirm__card {
        opacity: 0;
        transform: translateY(14px) scale(.96);
    }

    .ce-fade-leave-active {
        transition: opacity .16s ease;
    }

    .ce-fade-leave-active .ce-confirm__card {
        transition: transform .16s ease, opacity .16s ease;
    }

    .ce-fade-leave-to {
        opacity: 0;
    }

    .ce-fade-leave-to .ce-confirm__card {
        opacity: 0;
        transform: translateY(10px) scale(.97);
    }
</style>
