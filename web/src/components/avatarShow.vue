<template>
  <div class="avatar-show">
    <div class="cropper-area">
      <vue-cropper
        v-if="cropImg"
        ref="cropper"
        class="cropper"
        :img="cropImg"
        :output-type="'jpeg'"
        :auto-crop="true"
        :fixed="true"
        :fixed-number="[1, 1]"
        :center-box="true"
        :info="false"
        :can-move="true"
        :can-move-box="true"
        :can-scale="true"
        :auto-crop-width="240"
        :auto-crop-height="240"
        @real-time="onRealTime"
      />
      <el-empty v-else description="请选择一张图片" />
      <div v-if="cropImg" class="crop-tools">
        <el-button size="mini" @click="rotateLeft">左转</el-button>
        <el-button size="mini" @click="rotateRight">右转</el-button>
        <el-button size="mini" @click="zoomIn">放大</el-button>
        <el-button size="mini" @click="zoomOut">缩小</el-button>
      </div>
    </div>

    <div class="side-area">
      <div class="preview-wrap">
        <div class="preview-circle" :style="previewStyle"></div>
        <p class="preview-label">头像预览</p>
      </div>
      <div class="actions">
        <el-button type="primary" @click="handleUpload">{{ cropImg ? '重新选择' : '选择图片' }}</el-button>
        <el-button type="success" :disabled="!cropImg || uploading" :loading="uploading" @click="confirmUpload">
          确认上传
        </el-button>
      </div>
      <div class="tips">
        <p>支持 JPG / PNG / GIF 格式，大小不超过 5MB。</p>
        <p>拖动、缩放或旋转图片，裁剪框外区域将被裁掉。</p>
      </div>
    </div>

    <input ref="fileInput" type="file" accept="image/*" style="display: none;" @change="onFileChange" />
  </div>
</template>

<script>
import { VueCropper } from 'vue-cropper';
import { uploadAvatar } from '@/api';
import { SUCCESS_CODE } from '@/constants/http';

export default {
  name: 'avatarShow',
  components: { VueCropper },
  props: {
    initialSrc: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      imageSrc: this.initialSrc,
      cropImg: '',
      previewUrl: '',
      uploading: false,
    };
  },
  computed: {
    previewStyle() {
      const url = this.previewUrl || this.imageSrc;
      return {
        backgroundImage: url ? `url(${url})` : 'none',
      };
    },
  },
  methods: {
    onRealTime(data) {
      // vue-cropper 实时裁剪事件，payload.data.url 为裁剪结果 base64
      this.previewUrl = data.url || '';
    },
    handleUpload() {
      this.$refs.fileInput.click();
    },
    async onFileChange(event) {
      const file = event.target.files[0];
      event.target.value = ''; // 允许连续选择同一文件
      if (!file) return;
      if (!this.validateFileType(file)) return;
      if (!this.validateFileSize(file)) return;
      const dataUrl = await this.fileToDataUrl(file);
      this.cropImg = dataUrl;
    },
    fileToDataUrl(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
    },
    validateFileType(file) {
      if (!/\.(jpg|jpeg|png|gif)$/i.test(file.name)) {
        this.$message.error('只允许上传JPG、PNG、GIF格式的图片');
        return false;
      }
      return true;
    },
    validateFileSize(file) {
      if (file.size / (1024 * 1024) > 5) {
        this.$message.error('图片大小不能超过5MB');
        return false;
      }
      return true;
    },
    rotateLeft() {
      this.$refs.cropper.rotateLeft();
    },
    rotateRight() {
      this.$refs.cropper.rotateRight();
    },
    zoomIn() {
      this.$refs.cropper.changeScale(0.1);
    },
    zoomOut() {
      this.$refs.cropper.changeScale(-0.1);
    },
    confirmUpload() {
      if (!this.$refs.cropper) return;
      this.$refs.cropper.getCropBlob((blob) => {
        if (!blob) {
          this.$message.error('裁剪失败，请重试');
          return;
        }
        const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' });
        this.uploading = true;
        uploadAvatar(file)
          .then((result) => {
            if (result.data && result.data.code === SUCCESS_CODE && result.data.data) {
              const url = 'http://' + result.data.data;
              this.imageSrc = url;
              this.$store.commit('updatedAvatarSrc', url);
              this.$message.success('头像修改成功');
            } else {
              this.$message.error((result.data && result.data.msg) || '上传失败');
            }
          })
          .catch(() => {
            this.$message.error('网络错误，上传失败');
          })
          .finally(() => {
            this.uploading = false;
          });
      });
    },
  },
};
</script>

<style lang="less" scoped>
.avatar-show {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.cropper-area {
  flex: 1;
  min-width: 260px;

  .cropper {
    width: 300px;
    height: 300px;
    border-radius: 8px;
    overflow: hidden;
    background: #f5f7fa;
    border: 1px solid #ebeef5;
  }

  .crop-tools {
    margin-top: 12px;
    text-align: center;
  }
}

.side-area {
  width: 220px;

  .preview-wrap {
    text-align: center;

    .preview-circle {
      width: 96px;
      height: 96px;
      margin: 0 auto;
      border-radius: 50%;
      background-size: cover;
      background-position: center;
      background-repeat: no-repeat;
      border: 3px solid #fff;
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.12);
    }

    .preview-label {
      margin-top: 8px;
      font-size: 12px;
      color: #909399;
    }
  }

  .actions {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 16px;
  }

  .tips {
    margin-top: 16px;
    padding: 10px 12px;
    background: #f5f7fa;
    border-radius: 6px;

    p {
      margin-bottom: 4px;
      font-size: 12px;
      line-height: 1.5;
      color: #909399;
    }
  }
}

@media screen and (max-width: 600px) {
  .avatar-show {
    flex-direction: column;
  }

  .side-area {
    width: 100%;
  }
}
</style>
