<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createContent, updateContent } from '@/api/admin/content'
import type {
  AdminContentDetail,
  ContentSavePayload,
  ContentType
} from '@/api/admin/content'
import type { RehabPart } from '@/api/dict'
import { formatDateTime } from '@/utils/datetime'
import AttachmentUploader from './AttachmentUploader.vue'
import type { AttachmentItem } from './AttachmentUploader.vue'

interface Props {
  partOptions: RehabPart[]
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const reviewRemark = ref('')

const form = reactive({
  title: '',
  contentType: 'ARTICLE' as ContentType,
  targetPartCode: '',
  targetGroup: '',
  summary: '',
  contentBody: '',
  dateRange: [] as string[]
})

const attachments = ref<AttachmentItem[]>([])

const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题最多 200 个字符', trigger: 'blur' }
  ],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
  targetPartCode: [{ required: true, message: '请选择关联康复部位', trigger: 'change' }],
  contentBody: [
    { required: true, message: '请输入正文', trigger: 'blur' },
    { max: 10000, message: '正文最多 10000 个字符', trigger: 'blur' }
  ]
}

const dialogTitle = (): string => (editingId.value == null ? '新增公共内容' : '编辑公共内容')

const resetForm = (): void => {
  form.title = ''
  form.contentType = 'ARTICLE'
  form.targetPartCode = ''
  form.targetGroup = ''
  form.summary = ''
  form.contentBody = ''
  form.dateRange = []
  attachments.value = []
  reviewRemark.value = ''
}

const open = (detail?: AdminContentDetail): void => {
  editingId.value = detail ? detail.contentId : null
  resetForm()
  if (detail) {
    form.title = detail.title
    form.contentType = detail.contentType as ContentType
    form.targetPartCode = detail.targetPartCode ?? ''
    form.targetGroup = detail.targetGroup ?? ''
    form.summary = detail.summary ?? ''
    form.contentBody = detail.contentBody
    const from = formatDateTime(detail.validFrom)
    const to = formatDateTime(detail.validTo)
    form.dateRange = from && to ? [from, to] : []
    reviewRemark.value = detail.reviewRemark ?? ''
    attachments.value = (detail.attachments ?? []).map((a) => ({
      fileId: a.fileId,
      attachmentType: a.attachmentType as AttachmentItem['attachmentType'],
      sortNo: a.sortNo,
      fileName: a.fileName ?? `文件#${a.fileId}`,
      contentType: a.contentType ?? '',
      fileSize: a.fileSize ?? 0,
      url: a.url,
      status: 'done',
      progress: 100
    }))
  }
  visible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

defineExpose({ open })

const handleSubmit = async (): Promise<void> => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (attachments.value.some((a) => a.status === 'uploading')) {
    ElMessage.warning('附件仍在上传中，请等待上传完成或移除后再提交')
    return
  }
  if (attachments.value.some((a) => a.status === 'error')) {
    ElMessage.warning('存在上传失败的附件，请移除失败项或重新上传')
    return
  }
  if (form.dateRange.length === 1) {
    ElMessage.warning('请补全有效期的起止时间，或清空有效期范围')
    return
  }

  const payload: ContentSavePayload = {
    title: form.title,
    contentType: form.contentType,
    targetPartCode: form.targetPartCode,
    targetGroup: form.targetGroup || undefined,
    summary: form.summary || undefined,
    contentBody: form.contentBody,
    validFrom: form.dateRange[0] || undefined,
    validTo: form.dateRange[1] || undefined,
    attachments: attachments.value.map((a, idx) => ({
      fileId: a.fileId as number,
      attachmentType: a.attachmentType,
      sortNo: idx
    }))
  }

  submitting.value = true
  try {
    if (editingId.value == null) {
      await createContent(payload)
      ElMessage.success('内容草稿创建成功')
    } else {
      await updateContent(editingId.value, payload)
      ElMessage.success('内容更新成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    // 拦截器统一提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle()"
    width="760px"
    :close-on-click-modal="false"
    destroy-on-close
    top="6vh"
  >
    <el-alert
      v-if="reviewRemark"
      class="reject-alert"
      :closable="false"
      type="error"
      show-icon
      title="该内容曾被驳回"
      :description="reviewRemark"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入内容标题" clearable maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item label="内容类型" prop="contentType">
        <el-radio-group v-model="form.contentType">
          <el-radio value="ARTICLE">图文文章</el-radio>
          <el-radio value="VIDEO">视频</el-radio>
          <el-radio value="GUIDE">指南</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="康复部位" prop="targetPartCode">
        <el-select v-model="form.targetPartCode" placeholder="请选择关联康复部位" filterable style="width: 240px">
          <el-option v-for="item in partOptions" :key="item.code" :label="item.name" :value="item.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标人群">
        <el-input v-model="form.targetGroup" placeholder="如：术后 0-6 周患者（可选）" maxlength="200" />
      </el-form-item>
      <el-form-item label="摘要">
        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="2"
          placeholder="列表展示的摘要（可选，最多 500 字）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="有效期">
        <el-date-picker
          v-model="form.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="生效时间（留空发布时生效）"
          end-placeholder="失效时间（留空长期有效）"
          value-format="YYYY-MM-DD HH:mm:ss"
          format="YYYY-MM-DD HH:mm"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="正文" prop="contentBody">
        <el-input
          v-model="form.contentBody"
          type="textarea"
          :rows="9"
          placeholder="请输入正文内容（最多 10000 字）"
          maxlength="10000"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="附件">
        <AttachmentUploader v-model="attachments" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.reject-alert {
  margin-bottom: 14px;
}
</style>
