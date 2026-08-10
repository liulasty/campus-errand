<template>
    <div class="sensitive-config">
        <el-card shadow="hover">
            <div slot="header">
                <span><i class="el-icon-lock"></i> 敏感词配置（发布委托拦截）</span>
            </div>
            <div class="add-row">
                <el-input v-model="newWord" placeholder="输入敏感词，如：代考" clearable style="width: 280px;"
                    @keyup.enter.native="handleAdd"></el-input>
                <el-button type="primary" icon="el-icon-plus" @click="handleAdd">添加</el-button>
            </div>
            <el-table :data="list" v-loading="loading" stripe border style="margin-top: 15px;">
                <el-table-column prop="id" label="ID" width="80" align="center" />
                <el-table-column prop="word" label="敏感词" align="center" />
                <el-table-column prop="createTime" label="创建时间" align="center">
                    <template slot-scope="scope">
                        {{ formatDate(scope.row.createTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="120">
                    <template slot-scope="scope">
                        <el-button size="mini" type="danger" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-card shadow="hover" style="margin-top: 15px;">
            <div slot="header">
                <span><i class="el-icon-search"></i> 内容校验测试</span>
            </div>
            <el-input type="textarea" v-model="testText" :rows="3" placeholder="输入一段文字，检测是否命中敏感词"></el-input>
            <el-button type="primary" icon="el-icon-check" style="margin-top: 10px;" @click="handleCheck">校验</el-button>
            <div v-if="checkResult !== null" style="margin-top: 10px;">
                <el-tag v-if="checkResult.length === 0" type="success">未命中敏感词</el-tag>
                <el-tag v-else type="danger">命中敏感词：{{ checkResult.join('、') }}</el-tag>
            </div>
        </el-card>
    </div>
</template>
<script>
    import { listSensitiveWords, addSensitiveWord, deleteSensitiveWord, checkSensitiveText } from '@/api/'
    import { formatDateTime } from '@/utils/dateFormat'
    export default {
        name: 'SensitiveWordConfig',
        data() {
            return {
                list: [],
                loading: false,
                newWord: '',
                testText: '',
                checkResult: null
            }
        },
        created() {
            this.getList()
        },
        methods: {
            getList() {
                this.loading = true
                listSensitiveWords().then(res => {
                    if (res.data.code === 1) {
                        this.list = res.data.data
                    }
                    this.loading = false
                })
            },
            handleAdd() {
                const word = this.newWord.trim()
                if (!word) {
                    this.$message.warning('请输入敏感词')
                    return
                }
                addSensitiveWord(word).then(res => {
                    if (res.data.code === 1) {
                        this.$message.success('添加成功')
                        this.newWord = ''
                        this.getList()
                    } else {
                        this.$message.error(res.data.msg || '添加失败')
                    }
                }).catch(err => {
                    console.error('添加敏感词失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            handleDelete(row) {
                this.$confirm('确认删除敏感词「' + row.word + '」？', '提示', { type: 'warning' }).then(() => {
                    deleteSensitiveWord(row.id).then(res => {
                        if (res.data.code === 1) {
                            this.$message.success('删除成功')
                            this.getList()
                        } else {
                            this.$message.error(res.data.msg || '删除失败')
                        }
                    }).catch(err => {
                        console.error('删除敏感词失败：', err)
                        this.$message.error('请求异常，请稍后重试')
                    })
                }).catch(() => {})
            },
            handleCheck() {
                if (!this.testText) {
                    this.$message.warning('请输入要校验的文本')
                    return
                }
                checkSensitiveText(this.testText).then(res => {
                    if (res.data.code === 1) {
                        this.checkResult = res.data.data || []
                    } else {
                        this.$message.error(res.data.msg || '校验失败')
                    }
                }).catch(err => {
                    console.error('敏感词校验失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            formatDate(date) {
                return formatDateTime(date, false, '')
            }
        }
    }
</script>
<style scoped>
    .sensitive-config {
        padding: 10px;
    }

    .add-row {
        display: flex;
        align-items: center;
        gap: 10px;
    }
</style>
