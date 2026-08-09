<template>
  <div class="header-container">
    <div class="l-content">
      <el-button 
        @click="handleMenu" 
        class="menu-toggle-btn"
        icon="el-icon-s-fold" 
        size="small"
        circle>
      </el-button>
      <!-- 面包屑 -->
      <el-breadcrumb separator="/" class="custom-breadcrumb">
        <el-breadcrumb-item 
          v-for="item in tags" 
          :key="item.path" 
          :to="{ path: item.path }" 
          :id="generateUniqueId(item)"
          :class="generateItemClass(item)">
          {{ item.label }}
        </el-breadcrumb-item>
      </el-breadcrumb>

    </div>
    <div class="r-content">
      <div class="action-item">
        <el-tooltip content="全屏" placement="bottom">
            <i class="el-icon-full-screen action-icon"></i>
        </el-tooltip>
      </div>
      <div class="action-item">
          <el-badge :value="3" class="item">
            <i class="el-icon-bell action-icon" @click="handleNotice"></i>
          </el-badge>
      </div>
      <el-dropdown trigger="click" class="avatar-dropdown">
        <div class="avatar-wrapper">
          <img class="userIcon" :src="avatarSrc || defaultAvatar" alt="用户">
          <div class="avatar-info">
            <span class="avatar-name">{{ displayName }}</span>
            <span class="role-tag" :class="roleInfo.cls">{{ roleInfo.label }}</span>
          </div>
          <i class="el-icon-caret-bottom"></i>
        </div>
        <el-dropdown-menu slot="dropdown" class="user-dropdown">
          <div class="user-panel">
            <img class="panel-avatar" :src="avatarSrc || defaultAvatar" alt="用户">
            <div class="panel-meta">
              <div class="panel-name-row">
                <span class="panel-name">{{ displayName }}</span>
                <span class="role-tag" :class="roleInfo.cls">{{ roleInfo.label }}</span>
              </div>
              <div class="panel-row"><span class="panel-label">用户ID</span>{{ userId || '--' }}</div>
              <div class="panel-row"><span class="panel-label">实名状态</span>{{ authDesc || '未认证' }}</div>
              <div class="panel-row"><span class="panel-label">信用分</span>{{ creditDisplay }}</div>
            </div>
          </div>
          <el-dropdown-item @click.native="dialogAvatarVisible = true">
            <i class="el-icon-user"></i>
            修改头像
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="handleLogout">
            <i class="el-icon-switch-button"></i>
            退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>


      <el-dialog title="修改头像" :visible.sync="dialogAvatarVisible" @close="handleDialogClose" :width="dialogWidth"
        class="my-dialog" append-to-body>
        <avatarShowVue :initialSrc="avatarSrc" />
      </el-dialog>
      <el-dialog title="通知中心" :visible.sync="dialogNoticeVisible" @close="handleDialogClose" width="800px"
        class="my-dialog" top="10vh" append-to-body>
        <noticeVue :userId="userId" />
      </el-dialog>
    </div>
  </div>
</template>

<script>

  import avatarShowVue from './avatarShow.vue';
  import { mapState } from 'vuex';
  import { logout, uploadAvatar, fetchUserBasicInfo, getUserInfo } from '@/api';
  import { SUCCESS_CODE } from '@/constants/http';
  import noticeVue from './noticeList.vue';

  export default {
    components: {
      avatarShowVue,
      noticeVue,
    },
    data() {
      return {
        dialogAvatarVisible: false,
        imageUrl: '',
        initialImageSrc: '',
        avatarSrc: '',
        defaultAvatar: require('../assets/avatar.jpg'),
        userProfile: null,
        dialogWidth: '600px',
        dialogNoticeVisible: false,
        userId: '',
      }

    },
    watch: {
      '$route'(to, from) {
        // 在路由变化时触发，你可以在这里更新面包屑的样式
        // 比如根据当前路由信息 to 来设置面包屑样式
        this.updateBreadcrumbStyle(to)
      },
      showTooltip: function (newVal, oldVal) {
        // 在这里处理 showTooltip 值的变化
        console.log("newVal", newVal),
          console.log("old", oldVal)
      }
    },
    methods: {
      handleDialogClose() {
        console.log("userInfo.avatarSrc", this.$store.state.userInfo)
        this.avatarSrc = this.$store.state.userInfo.avatarSrc;
        console.log("this.avatarSrc", this.avatarSrc)
      },


      generateUniqueId(item) {
        // 生成一个基于路由项的唯一 ID，可以根据需要自定义生成逻辑
        return `breadcrumb-item-${item.path}`;
      },
      generateItemClass(item) {
        // 生成类名，例如 "special" 类名
        if (item.path === this.$route.path) {
          return 'current_bread';
        } else {
          if (this.$route.path === '/home' & item.path === '/') {
            return 'current_bread';
          }
          return '';
        }
      },
      async loadUserProfile(userId) {
        try {
          const res = await fetchUserBasicInfo(userId);
          if (res.data && res.data.code === SUCCESS_CODE && res.data.data) {
            const u = res.data.data;
            this.userProfile = { name: u.username || '', credit: u.creditScore, userRole: '' };
          }
        } catch (e) {
          console.warn('加载用户资料失败', e);
        }
        // users.Role 仅 USER/ADMIN，学生/教师来自 usersinfo.UserRole（小写 student/teacher，仅已实名用户存在）
        try {
          const r = await getUserInfo(userId);
          if (r.data && r.data.code === SUCCESS_CODE && r.data.data && r.data.data.userRole && this.userProfile) {
            this.userProfile.userRole = r.data.data.userRole;
          }
        } catch (e) {
          // 非实名/管理员无 usersinfo 记录，userRole 保持为空
        }
      },
      handleMenu() {
        this.$store.commit('collapseMenu')
      },
      updateBreadcrumbStyle(to) {
        // 根据 to 对象来更新面包屑样式，这里可以根据具体需求修改样式
        // 例如，你可以设置一个数据属性，然后在模板中使用这个属性来控制面包屑样式
        if (to.path === '/home' || to.path === '/') {
          console.log(to, '/home')
          const current_bread = document.getElementById('breadcrumb-item-' + to.path)

        } else {
          console.log(to, '其他')
          const current_bread = document.getElementById('breadcrumb-item-' + to.path)

        }
        this.$store.commit('selectMenu', to)
      },
      handleLogout() {
        let userId = this.$store.state.userInfo.userId
        // 删除JWT令牌
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('TaskUser');
        if (userId > 0) {
          logout().then((data) => {
            console.log("退出登录", data)


            // this.$store.commit('removeUser');
            this.$router.push('/login');
            // window.location.reload();

          })
        } else {
          this.$router.push('/login');
          // window.location.reload();

        }



      },
      handleNotice() {
        this.dialogNoticeVisible = true
      }
    },
    // computed 是一个对象，用于定义计算属性
    computed: {
      // ...mapState 意味着将 mapState 返回的所有属性都添加到 computed 对象中。
      //创建一个名为 tags 的计算属性，它将获取 Vuex 的状态中 state.tab.tabsList 的数据。
      ...mapState({
        tags: state => state.tab.tabsList
      }),
      roleInfo() {
        const role = this.$store.state.userInfo.userType ||
          (JSON.parse(localStorage.getItem('TaskUser') || '{}')).userType || '';
        if (role === 'ADMIN') {
          return { label: '管理员', cls: 'role-admin' };
        }
        const userRole = (this.userProfile && this.userProfile.userRole) || '';
        const map = {
          student: { label: '学生', cls: 'role-student' },
          teacher: { label: '教师', cls: 'role-teacher' },
        };
        return map[userRole] || { label: '普通用户', cls: 'role-default' };
      },
      displayName() {
        return (this.userProfile && this.userProfile.name) || '用户';
      },
      creditDisplay() {
        const c = this.userProfile ? this.userProfile.credit : null;
        return (c !== null && c !== undefined) ? String(c) : '--';
      },
      authDesc() {
        try {
          return (JSON.parse(localStorage.getItem('TaskUser') || '{}')).Authorization || '';
        } catch (e) {
          return '';
        }
      },
    },
    mounted() {
      let taskUser = {};
      try {
        taskUser = JSON.parse(localStorage.getItem('TaskUser') || '{}') || {};
      } catch (e) {
        taskUser = {};
      }
      this.userId = taskUser.userId || this.$store.state.userInfo.userId || '';
      // 修复：上传的头像在刷新后丢失（此前读取 localStorage 的代码被注释）
      this.avatarSrc = localStorage.getItem('avatarSrc') || '';
      if (this.userId) {
        this.loadUserProfile(this.userId);
      }
    }

  }
</script>

<style lang="less" scoped>
  .header-container {
    background-color: #ffffff;
    height: 60px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 24px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    position: relative;
    z-index: 1000;
    transition: all 0.3s ease;

    .l-content {
      display: flex;
      align-items: center;
      flex: 1;
      overflow: hidden;

      .menu-toggle-btn {
        margin-right: 20px;
        border: none;
        font-size: 20px;
        color: #606266;
        background: transparent;
        transition: all 0.3s;
        
        &:hover {
          color: var(--ce-primary);
          transform: scale(1.1);
          background-color: rgba(14, 124, 102, 0.1);
        }
      }

      .custom-breadcrumb {
        font-size: 14px;
        line-height: 60px;
        margin-left: 10px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;

        /deep/ .el-breadcrumb__item {
          .el-breadcrumb__inner {
            color: #909399;
            font-weight: 500;
            transition: color 0.3s;
            
            &.is-link:hover {
              color: var(--ce-primary);
              font-weight: 600;
            }
          }

          &:last-child .el-breadcrumb__inner {
             color: #303133;
             font-weight: 700;
          }
        }
      }
    }

    .r-content {
      display: flex;
      align-items: center;
      height: 100%;
      flex-shrink: 0;

      .action-item {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 12px;
        height: 100%;
        color: #606266;
        cursor: pointer;
        transition: all 0.3s;
        border-radius: 4px;

        &:hover {
          background: rgba(0, 0, 0, 0.04);
          color: var(--ce-primary);
        }
        
        .action-icon {
          font-size: 20px;
        }

        .item {
          display: flex;
          align-items: center;
          
          /deep/ .el-badge__content {
            border: none;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
          }
        }
      }

      .avatar-dropdown {
        margin-left: 20px;
        cursor: pointer;

        .avatar-wrapper {
          display: flex;
          align-items: center;
          padding: 4px;
          border-radius: 24px;
          transition: background 0.3s;

          &:hover {
            background: rgba(0, 0, 0, 0.04);
          }
          
          .userIcon {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            border: 2px solid #fff;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            transition: transform 0.3s;

            &:hover {
              transform: rotate(360deg);
            }
          }

          .avatar-info {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            margin-left: 10px;
            line-height: 1.2;

            .avatar-name {
              font-size: 14px;
              font-weight: 600;
              color: #303133;
              max-width: 120px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }

          .el-icon-caret-bottom {
            margin-left: 8px;
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }

  }

  .role-tag {
    display: inline-block;
    margin-top: 2px;
    padding: 0 6px;
    font-size: 11px;
    line-height: 18px;
    border-radius: 3px;

    &.role-admin { color: #b88230; background: #fdf6ec; border: 1px solid #f3d19e; }
    &.role-teacher { color: #2d8f6f; background: #e6f7f0; border: 1px solid #b7e3d0; }
    &.role-student { color: #3370ff; background: #ecf3ff; border: 1px solid #b9d0ff; }
    &.role-default { color: #909399; background: #f4f4f5; border: 1px solid #dcdce0; }
  }

  .user-panel {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    min-width: 220px;
    border-bottom: 1px solid #ebeef5;

    .panel-avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      object-fit: cover;
      margin-right: 12px;
      border: 2px solid #fff;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .panel-meta {
      flex: 1;
      min-width: 0;
    }

    .panel-name-row {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .panel-name {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }

    .panel-row {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;

      .panel-label {
        color: #c0c4cc;
        margin-right: 6px;
      }
    }
  }

  /* 响应式适配 */
  @media screen and (max-width: 768px) {
    .header-container {
      padding: 0 12px;

      .l-content {
        .custom-breadcrumb {
          display: none; // 移动端隐藏面包屑
        }
      }

      .r-content {
        .action-item {
          padding: 0 8px;
          
          .action-icon {
            font-size: 18px;
          }
        }

        .avatar-dropdown {
          margin-left: 10px;

          .avatar-wrapper {
            .userIcon {
              width: 32px;
              height: 32px;
            }

            .avatar-info {
              display: none;
            }
          }
        }
      }
    }
  }

  /* 覆盖Element UI 样式 */
  /deep/.el-dialog__header {
    border-bottom: 1px solid #ebeef5;
    padding: 20px;
  }
  
  /deep/.el-dialog__body {
    padding: 30px 20px;
  }
  
  /deep/.el-dialog {
    border-radius: 8px;
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  }
</style>