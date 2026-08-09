<template>
  <div class="user-info-container">
    <el-card v-if="code !== 4 && code !== 0" shadow="hover" style="margin-bottom: 10px;">
      <el-alert v-if="code === 3" :title="'认证被驳回：' + (rejectReason || '材料不符')"
        type="error" :closable="false" show-icon />
      <el-alert v-else :title="authState === AUTH_STATUS.AUTHENTICATING ? '审核中，请等待管理员审核' : '完成 L1 实名认证后可发布委托、接单、打卡'"
        type="warning" :closable="false" show-icon />
    </el-card>

    <el-card shadow="hover" class="box-card" v-if="code != 4">
      <div slot="header" class="clearfix">
        <span><i class="el-icon-user"></i> 个人认证信息</span>
      </div>
      <div class="empty-state">
        <el-empty :description="this.msg"></el-empty>
        <div v-if="code != 2" class="action-btn">
          <el-button type="primary" size="medium" @click="applyForModification()">{{ button }}</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="hover" class="box-card" v-else>
      <div slot="header" class="clearfix">
        <span><i class="el-icon-user-solid"></i> 我的资料</span>
        <el-tag type="success" size="small" style="float: right;">已认证</el-tag>
      </div>
      <div style="margin-bottom: 10px;">
        <el-button type="info" size="small" disabled title="即将上线">L2 校园卡认证（即将上线）</el-button>
      </div>

      <div class="user-profile">
        <el-row :gutter="40">
            <el-col :span="8" class="profile-left">
                 <div class="avatar-container">
                    <el-avatar :size="100" :src="require('@/assets/avatar.jpg')" icon="el-icon-user-solid"></el-avatar>
                    <h3>{{ infoForm.name }}</h3>
                    <p>{{ infoForm.userRole }}</p>
                 </div>
            </el-col>
            <el-col :span="16">
                 <el-form :model="infoForm" ref="infoForm" label-position="right" label-width="100px" class="info-form">
                    <el-row :gutter="20">
                        <el-col :span="12">
                             <el-form-item label="姓名" prop="name">
                                <el-input v-model="infoForm.name" disabled prefix-icon="el-icon-user"></el-input>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="电话号码" prop="phoneNumber">
                                <el-input v-model="infoForm.phoneNumber" disabled prefix-icon="el-icon-mobile-phone"></el-input>
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <el-row :gutter="20">
                        <el-col :span="12">
                            <el-form-item label="QQ号" prop="qqNumber">
                                <el-input v-model="infoForm.qqNumber" disabled prefix-icon="el-icon-chat-round"></el-input>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                             <el-form-item label="认证身份" prop="userRole">
                                <el-input v-model="infoForm.userRole" disabled prefix-icon="el-icon-postcard"></el-input>
                            </el-form-item>
                        </el-col>
                    </el-row>
                    
                    <el-form-item label="认证时间" prop="certifiedTime">
                        <el-input :value="formatDate(infoForm.certifiedTime)" disabled prefix-icon="el-icon-date"></el-input>
                    </el-form-item>

                    <el-form-item class="form-actions">
                        <el-button type="primary" icon="el-icon-edit" @click="applyForModification()">申请修改</el-button>
                        <el-button type="danger" icon="el-icon-delete" @click="cancelAuthentication()">取消认证</el-button>
                        <el-button type="success" icon="el-icon-download" @click="myInfoExportExcel()">导出表格</el-button>
                    </el-form-item>
                </el-form>
            </el-col>
        </el-row>
      </div>
    </el-card>

    <el-dialog title="申请认证" :visible.sync="dialogUserInfo" width="500px" center append-to-body>
      <el-form :model="infoAddForm" label-width="80px" class="demo-infoUpdateForm">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="infoAddForm.name" placeholder="请输入真实姓名"></el-input>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="infoAddForm.phone" placeholder="请输入联系电话"></el-input>
        </el-form-item>
        <el-form-item label="QQ账号" prop="qq">
          <el-input v-model="infoAddForm.qq" placeholder="请输入QQ号码"></el-input>
        </el-form-item>
        <el-form-item label="认证照片" prop="img">
          <ImageUploader ref="imageSet"/>
        </el-form-item>

        <el-form-item label="认证角色" prop="role">
          <el-radio-group v-model="infoAddForm.role">
            <el-radio label="student">学生</el-radio>
            <el-radio label="teacher">教师</el-radio>
            <el-radio label="other">其他</el-radio>
          </el-radio-group>

        </el-form-item>
        <el-form-item label="身份标识" prop="identityNo">
          <el-input v-model="infoAddForm.identityNo"
            :placeholder="infoAddForm.role === 'student' ? '请输入学号' : infoAddForm.role === 'teacher' ? '请输入工号' : '请输入校内编号'"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogUserInfo = false">取 消</el-button>
        <el-button type="primary" @click="submitAnApplication()">提交申请</el-button>
      </div>

    </el-dialog>

  </div>
</template>
<script>
import {
  getUserInfo,
  submitCertificationInformation,
  deleteAuthenticationInformation,
  exportExcel
} from '@/api'
import ImageUploader from '@/components/ImageUploader.vue'
import {executeConfirmedRequest} from '@/utils/globalConfirmAction'
import {SUCCESS_CODE} from '@/constants/http'
import {AUTH_STATUS} from '@/constants/enums'

export default {
  components: {ImageUploader},
  data() {
    return {
      AUTH_STATUS,
      userId: 0,
      code: 0,
      msg: '未认证',
      button: '申请认证',
      userOption: {
        '1': {
          msg: '未认证',
          button: '申请认证'
        },
        '2': {
          msg: '认证中,请等待',
          button: '查看'
        },
        '3': {
          msg: '认证失败',
          button: '重新认证'
        },
        '4': {
          msg: '已认证',
          button: '修改认证'
        }
      },
      dialogUpdateForm: false,
      dialogUserInfo: false,
      labelPosition: 'right',
      infoUpdateForm: {
        athleteId: 1,
      },
      infoForm: {
        name: '',
        age: '20',
        gender: '1',
        contact: '',
        userId: this.$store.state.userInfo.userId
      },
      infoAddForm: {
        name: '',
        phone: '',
        imgUrl: '',
        qq: '',
        role: 'student',
        identityNo: '',
      },
      rejectReason: '',
      playerInfo: {},

    }
  },
  computed: {
    authState() {
      const map = {1: '未认证', 2: '认证中', 3: '认证失败', 4: '认证通过'}
      return map[this.code] || '未认证'
    }
  },
  mounted() {
    this.getInfo()
  },
  methods: {
    getInfo() {
      this.userId = this.$store.state.userInfo.userId


      getUserInfo(this.userId).then((res) => {
        const {code, data} = res.data
        if (code === SUCCESS_CODE) {
          var info = data;
          this.rejectReason = info.rejectReason || '';
          if (info.authStatus && info.authStatus === AUTH_STATUS.AUTHENTICATING) {

            this.updateButton(2)

          } else if (info.authStatus && info.authStatus === AUTH_STATUS.AUTHENTICATED) {
            this.updateButton(4)
            this.infoForm = info
          } else {
            this.updateButton(3)

          }

        } else {
          this.updateButton(1)
        }
      }).catch(err => {
        console.error('获取认证信息失败：', err)
        this.$message.error('请求异常，请稍后重试')
      })
    },
    updateButton(id) {
      this.code = id;
      this.msg = this.userOption[id].msg
      this.button = this.userOption[id].button
    },
    applyForModification() {
      this.dialogUserInfo = true;
    },
    formatDate(date) {
      if (!date) return ''
      const d = new Date(Number(date))
      const pad = n => (n < 10 ? '0' + n : n)
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    async submitAnApplication() {
      if (!this.infoAddForm.name) { this.$message.warning('请填写姓名'); return }
      if (!this.infoAddForm.identityNo) { this.$message.warning('请填写身份标识'); return }
      const imgs = this.$refs.imageSet && this.$refs.imageSet.imageUrls
      if (!imgs || !imgs.length) { this.$message.warning('请上传身份照片'); return }

      await this.$refs.imageSet.uploadImages();
      this.infoAddForm.imgUrl = this.$refs.imageSet.imageUrls[0].ossUrl;
      // console.log("提交认证信息图片1", this.$refs.imageSet.imageUrls[0].ossUrl);
      // console.log("提交认证信息图片2", this.infoAddForm);

      this.infoAddForm.id = this.$store.state.userInfo.userId;
      console.log(this.infoAddForm);
      submitCertificationInformation(this.infoAddForm).then((response) => {
        if (response.data.code === SUCCESS_CODE) {
          this.dialogUserInfo = false;
          this.$message({
            message: response.data.msg,
            type: 'success'
          });
          this.updateButton(2)
        } else {
          this.$message({
            message: response.data.msg,
            type: 'error'
          });
        }

      }).catch(err => {
        console.error('提交认证失败：', err)
        this.$message.error('请求异常，请稍后重试')
      })
    },
    async cancelAuthentication() {
      await executeConfirmedRequest(deleteAuthenticationInformation, this.infoForm.userId, "是否确认取消用户认证？", "提示", "警告", "操作警告", "操作失败，请稍后重试", "操作已取消");
      this.getInfo();
    },

    async myInfoExportExcel() {
      console.log("导出execl")
      exportExcel().then((response) => {
        try {
          console.log("res:", response)
          const fileName = `运动员信息${Date.now()}.xlsx`;
          const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
          saveAs(blob, fileName);
          this.$message.success("导出成功");
        } catch (error) {
          this.$message.error("文件创建或保存失败，请稍后重试");
          console.error("文件创建或保存失败:", error);
        }
      })
    },

  }
}
</script>

<style lang="less" scoped>
.user-info-container {
  padding: 20px;
}

.box-card {
  min-height: 400px;
  
  .clearfix {
    display: flex;
    align-items: center;
    justify-content: space-between;
    
    span {
      font-size: 16px;
      font-weight: bold;
      
      i {
        margin-right: 8px;
        color: #409EFF;
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  
  .action-btn {
    margin-top: 20px;
  }
}

.user-profile {
  padding: 20px;
  
  .profile-left {
    display: flex;
    justify-content: center;
    border-right: 1px solid #ebeef5;
    
    .avatar-container {
      text-align: center;
      
      h3 {
        margin-top: 20px;
        font-size: 24px;
        color: #303133;
      }
      
      p {
        color: #909399;
        margin-top: 10px;
      }
    }
  }
  
  .info-form {
    padding-left: 20px;
    
    .form-actions {
      margin-top: 40px;
      text-align: right;
    }
  }
}
</style>