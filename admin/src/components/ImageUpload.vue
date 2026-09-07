<template>
  <div class="image-upload">
    <el-upload
      class="avatar-uploader"
      action="#"
      :show-file-list="false"
      :auto-upload="false"
      :on-change="handleChange"
      accept="image/*"
    >
      <img v-if="modelValue" :src="resolveImageUrl(modelValue)" class="upload-img" alt="" />
      <div v-else class="upload-placeholder">
        <el-icon><Plus /></el-icon>
        <span>上传图片</span>
      </div>
    </el-upload>
    <div class="upload-tip">本地选择后转为 Data URL，正式环境请接入 OSS 上传接口</div>
  </div>
</template>

<script setup lang="ts">
import type { UploadFile } from 'element-plus'
import { resolveImageUrl } from '@/utils/format'

const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

function handleChange(file: UploadFile) {
  const raw = file.raw
  if (!raw) return
  if (!raw.type.startsWith('image/')) return
  const reader = new FileReader()
  reader.onload = () => emit('update:modelValue', reader.result as string)
  reader.readAsDataURL(raw)
}
</script>

<style scoped lang="scss">
.image-upload {
  .upload-img,
  .upload-placeholder {
    width: 120px;
    height: 120px;
    border-radius: 8px;
    object-fit: cover;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    border: 1px dashed #d1d5db;
    background: #f9fafb;
    color: #9ca3af;
    cursor: pointer;
    font-size: 12px;
  }

  .upload-tip {
    font-size: 12px;
    color: #9ca3af;
    margin-top: 6px;
  }
}
</style>
