# M3 公共内容审核与文件（MinIO）实施计划

## 一、调研结论（已核对源码）

### 已存在、直接复用的基础设施

| 类型 | 现状 |
|---|---|
| 实体 | `FileObject`（file_object，软删字段 `deletedAt` 非 `@TableLogic`，查询须手动 `isNull`）、`HealthContent`（content，`deleted` 列已 `@TableLogic`）、`ContentAttachment` |
| 枚举 | `FilePurpose`（TEMPLATE/ATTACHMENT/MESSAGE/AVATAR）、`ScanStatus`（PENDING/PASSED/FAILED）、`ContentType`（ARTICLE/VIDEO/GUIDE）、`AttachmentType`（COVER/VIDEO/IMAGE/DOC）、`ContentStatus` |
| DTO | `FilePresignRequest`（fileName/contentType/fileSize/purpose，含校验）、`FileCompleteRequest`（sha256，64 位十六进制正则）、`HealthContentSaveRequest`（含附件 `AttachmentPayload` 内嵌列表）、`ContentAuditRequest`（decision=APPROVE/REJECT/OFFLINE，comment）**全部就绪，model 层零改动** |
| Mapper | `FileObjectMapper`、`HealthContentMapper`、`ContentAttachmentMapper`、`ActionTemplateSampleMapper`、`ConsultationAttachmentMapper`、`ContentCollectionMapper` 均已在 web-app 模块（同进程 `com.rehab.**.mapper` 扫描） |
| MinIO | common 模块已引入 io.minio:8.5.14；`MinioConfig`（@ConditionalOnProperty 注册 MinioClient）、`MinioProperties`（@ConfigurationPropertiesScan 已扫 com.rehab.common）、`MinioFileService`（已实现 presignedGetUrl/presignedPutUrl）；dev 配置已启用 `http://192.168.10.102:9000` minioadmin/minioadmin 桶 `rehab`，健康探测 200 在线 |
| 错误码 | FILE_NOT_FOUND/FILE_URL_EXPIRED/FILE_IN_USE/FILE_TOO_LARGE(413)/UNSUPPORTED_MEDIA_TYPE(415)/FILE_HASH_MISMATCH/FILE_SCAN_FAILED(422)/STORAGE_UNAVAILABLE(424)/STATE_CONFLICT/VALIDATION_ERROR 全齐 |
| 前端 | request.ts（get/post/put/del，baseURL `/api/v1`）、admin 路由/菜单按 M2 模式扩展；无富文本依赖；vite 现仅代理 `/api` |

### ⚠️ 关键偏差：状态机以代码枚举为准

- 任务文档/接口文档文字为 `DRAFT→PENDING_AUDIT→PUBLISHED→OFFLINE/REJECTED`；
- 但 `ContentStatus` 枚举实际只有 **DRAFT / PENDING_REVIEW / PUBLISHED / OFFLINE**（沿用 M2「枚举码值以源码为准」原则，不改枚举、不加迁移）。
- 落地方案（与 DTO 注释"编辑草稿或被驳回内容"一致）：
  - **REJECT → 回到 DRAFT**，写 `reviewerId` + `reviewRemark`（驳回理由），内容可再次编辑送审；前端列表对"DRAFT 且有 reviewRemark"显示「已驳回」视觉标记。
  - **APPROVE 允许 DRAFT / PENDING_REVIEW → PUBLISHED**（管理员自建自审走 DRAFT；PENDING_REVIEW 预留给未来治疗师投稿，7.3 无投稿端点，不新增）。
  - **OFFLINE 仅 PUBLISHED → OFFLINE**；**PUT / DELETE 仅 DRAFT**；其他流转 409 STATE_CONFLICT。
  - REJECT 在 DRAFT 态也允许（管理端单角色场景下保证"驳回必填理由"链路可验收），状态不变但强制写理由。

### DDL 兜底点（DTO 宽松、数据库严格）

- `content.target_part_code` NOT NULL + 外键 rehab_part_dict：Service 校验非空且字典命中。
- `content.valid_from` NOT NULL 无默认：为空时创建/发布兜底当前时间；`valid_to < valid_from` 提前 400（DB 也有 CHECK）。
- `content_attachment.sort_no` NOT NULL：空值兜底 0；`uk_content_file(content_id,file_id)`。

## 二、文件与改动清单

### 后端（web-admin 为主，web-app 补患者只读端点）

新增（`backend/web/web-admin/src/main/java/com/rehab/admin/`）：

1. `service/file/FileUploadPolicy.java` — 文件用途白名单（业务常量，非环境配置）：
   - AVATAR：image/jpeg|png|webp，≤5MB，前缀 `avatar/`
   - ATTACHMENT：image/jpeg|png|gif|webp、video/mp4、application/pdf，≤100MB，前缀 `content/`
   - MESSAGE：image/jpeg|png、video/mp4，≤50MB，前缀 `message/`（预留）
   - TEMPLATE：video/mp4、video/webm，≤200MB，前缀 `action-demo/`
   - 提供 `validate(purpose, contentType, fileSize)`，超限 413 / 非白名单 415
2. `service/AdminFileService.java` + `impl/AdminFileServiceImpl.java` — presign/complete/url/delete 四方法
3. `service/AdminContentService.java` + `impl/AdminContentServiceImpl.java` — 内容分页/详情/增改/审核/删除
4. `controller/file/AdminFileController.java`（4 端点）、`controller/content/AdminContentController.java`（6 端点）
5. VO：`vo/FilePresignVO.java`、`vo/FileUrlVO.java`、`vo/AdminContentVO.java`、`vo/AdminContentDetailVO.java`、`vo/ContentAttachmentVO.java`

新增（`backend/web/web-app/src/main/java/com/rehab/app/`）：

6. `controller/patient/PatientContentController.java` — GET `/patient/contents`、GET `/patient/contents/{id}`（仅 PUBLISHED 且当前在有效期；不可见 404；附件带预签名 URL）。**收藏 3 个端点不在 M3，随患者模块做。**
7. `service/PatientContentService.java` + impl、`vo/PatientContentVO.java`、`vo/PatientContentDetailVO.java`

修改：

8. `common/.../constant/RedisKeyConstants.java` 增加 `FILE_UPLOAD = "file:upload:"`
9. `MinioFileService` 增加一个 removeObject 透传方法（删除用；presignedPutUrl 已有）

### 前端（frontend/src）

新增：

10. `api/admin/file.ts` — presign/complete/getFileUrl/deleteFile + `toMinioProxyUrl()` 代理改写工具
11. `api/admin/content.ts` — 6 个内容接口 + 类型定义
12. `modules/admin/views/content/ContentListView.vue` — 筛选（状态/类型/关键词）、表格、状态标签（DRAFT 有驳回理由显示「已驳回」标记）、行内操作（详情/编辑/通过/驳回/下架/删除）、详情抽屉（正文 + 附件预览：el-image / video / PDF 新窗口）
13. `modules/admin/views/content/ContentFormDialog.vue` — 标题、类型、适用部位（字典下拉，复用 api/dict.ts）、目标人群、摘要、正文（el-input textarea，10000 字；**不引入富文本依赖**）、有效期范围、附件区
14. `modules/admin/views/content/AttachmentUploader.vue` — 选择文件后浏览器 `crypto.subtle.digest('SHA-256')` → presign → fetch PUT 直传（带 Content-Type 头，URL 走 `/minio` 代理）→ complete 拿 fileId → 按 COVER/VIDEO/IMAGE/DOC 分组挂入附件列表，显示上传进度与失败重试/移除

修改：

15. `router/index.ts` 增加 `/admin/contents`（meta.roles ADMIN）
16. `components/RehabLayout.vue` 管理端菜单加「内容管理」（Reading/Document 图标，位于动作库之后）
17. `vite.config.ts` 增加代理：`'/minio' → http://192.168.10.102:9000`，rewrite 去掉 `/minio`，changeOrigin true（规避浏览器直传 MinIO 的 CORS 问题；生产由 Nginx 承担同样反代，签名 Host 经 changeOrigin 保持一致）

## 三、核心实现要点

### 文件生命周期

- **presign**：白名单校验 → `uploadId=UUID(无横线)`，objectKey=`{前缀}{uploadId}{净化后的扩展名}` → Redis 存会话 JSON（fileName/contentType/fileSize/purpose/objectKey/userId），TTL=`minio.presign-expire-sec`（600s）→ 返回 `{uploadId, uploadUrl, method:"PUT", headers:{"Content-Type": contentType}, expiresInSec}`。首次调用懒检查桶：bucketExists 否则 makeBucket（私有桶）。
- **complete**：
  1. 取 Redis 会话，不存在/过期 → 404 FILE_URL_EXPIRED（一次性消费）；
  2. MinIO `statObject`：对象不存在 → 404 FILE_NOT_FOUND；实际 size ≠ 申报 → 400 VALIDATION_ERROR；对象 contentType ≠ 申报 → 415 UNSUPPORTED_MEDIA_TYPE；
  3. `getObject` 用 `DigestInputStream(SHA-256)` **流式**计算真实哈希（≤200MB 内网，不落盘不占堆），与申报 sha256 小写比对，不符 → 422 FILE_HASH_MISMATCH，同时删除 MinIO 脏对象与 Redis 会话；
  4. 全通过 → 插 file_object（`scanStatus=PASSED`：首版未接第三方杀毒，以白名单+哈希校验代替，代码注释说明），删除 Redis 会话，返回 fileId；
  5. MinIO 调用异常统一包装 424 STORAGE_UNAVAILABLE。
- **url**：查 file（deletedAt IS NULL，否则 404）→ presignedGetUrl，返回 `{url, urlExpiresAt, fileName, contentType}`。
- **delete**：查 file → 依次查 content_attachment / action_template_sample / consultation_attachment 引用，任一存在 → 409 FILE_IN_USE（冲突信息带引用来源）→ MinIO removeObject → 置 deletedAt=now（软删留痕，file_object 无 deleted 列以外的逻辑删机制）。

### 内容服务

- 分页：status / contentType（query 参数名 `category` 对齐接口文档）/ keyword（title、summary 模糊），按 content_id 倒序；列表项带部位名称（批量查字典避免 N+1，沿用 M2 范式）。
- 详情：内容字段 + 附件列表，附件 VO 含 fileId/attachmentType/sortNo/fileName/contentType/fileSize/url/urlExpiresAt（逐条生成预签名）。
- create/update：校验部位、有效期；附件 fileId 必须存在、未删、PASSED；编辑时附件**全量替换**（同事务先物理删 content_attachment 旧关联再插入；file_object 本身不动）。
- audit：APPROVE 置 PUBLISHED + publishedAt=now + validFrom 空兜底；REJECT 强制 comment 非空（Service 二次校验防注解绕过）→ DRAFT + reviewerId + reviewRemark；OFFLINE → OFFLINE。全部 @OperationLog(module="CONTENT")，文件侧 module="FILE"，recordParam=true（审核理由必须入审计）。
- 患者列表：PUBLISHED + valid_from ≤ now ≤ (valid_to 或无界)，支持 partCode/category/keyword；详情不满足条件返回 404（防泄露未发布内容）。
- Redis 内容缓存首版**不做**（接口文档允许但非必须，避免一致性复杂度），在代码注释标注后续接入点 `cache:content:`。

## 四、实施顺序

1. 后端：RedisKeyConstants + MinioFileService.removeObject + FileUploadPolicy
2. AdminFileService/Impl + Controller（文件先行，内容依赖它）
3. AdminContentService/Impl + Controller + 5 个 VO
4. PatientContentService/Impl + Controller + 2 个患者 VO
5. `mvn package -DskipTests` 编译 + 重启后端
6. API 实测（curl.exe + 真实 MinIO，见验证节）
7. 前端：file.ts/content.ts → AttachmentUploader → ContentFormDialog → ContentListView → 路由/菜单/vite proxy
8. vue-tsc + vite build + 浏览器走查真实上传/审核/预览链路
9. 更新 docs/管理端模块开发任务文档.md M3 章节（状态机偏差说明、实际端点与页面）

## 五、验证方案

- 编译：后端 mvn package；前端 vue-tsc --noEmit、vite build；IDE 诊断零错误
- API 实测（curl.exe，PowerShell 5.1 沿用 M2 可靠方案：UTF-8 无 BOM 临时 body 文件 + `-w HTTPSTATUS:%{http_code}`）：
  1. presign 非法 MIME → 415；超大小 → 413；合法 → 200 拿 uploadUrl
  2. curl `-T` PUT 小文件到 MinIO（PDF/图片）→ complete 正确 sha256 → 200 fileId；错误 sha256 → 422（且脏对象被清理）
  3. getFileUrl → 200，curl 预签名 URL 可下载；重复 complete → 404
  4. 创建内容缺部位 → 400；草稿创建 → 200；非草稿编辑/删除 → 409
  5. REJECT 无理由 → 400；有理由 → DRAFT + reviewRemark 回显；APPROVE → PUBLISHED；OFFLINE 后患者列表不可见
  6. 被引用文件删除 → 409；解除引用（删草稿内容）后删除 → 200
  7. 患者端（无 token→401；PATIENT token）列表只见有效期内 PUBLISHED，直接 GET 草稿 id → 404
- 浏览器：登录 /admin/contents 走完整流程（上传图片→存为封面/附件→创建草稿→预览→通过→下架），DevPlatform 确认直传走 `/minio` 代理无 CORS 错误
- 审计：operation_log 表确认 CONTENT/FILE 写操作记录完整（含驳回理由）
- 权限回归：无 token 401、非 ADMIN 403

## 六、风险与处理

1. **MinIO CORS**：默认桶无 CORS 配置浏览器直传会被拦 → vite 同源代理 `/minio`（changeOrigin 保签名 Host 一致）；生产部署需 Nginx 同名反代，写入任务文档运维备注。
2. **大文件哈希下载校验成本**：白名单已按用途设上限（最大 200MB 仅 TEMPLATE，M3 内容附件最大 100MB），内网流式校验可接受；若后续接杀毒服务，scan_status 改由异步扫描回写。
3. **枚举与文档不一致**：不改枚举/不加 Flyway 迁移，REJECTED 语义以「DRAFT+reviewRemark」承载并在接口文档/任务文档注明。
4. **MinIO 中残留孤儿对象**：presign 后未 complete 的对象无法被业务引用；本次不做 GC（后续运维任务可按 file_object 对账清理），计划中注明。
5. **浏览器 SHA-256 内存占用**：100MB 级 file.arrayBuffer() 在演示环境可接受；超大文件分片哈希优化随 pose-engine 模板上传一起演进。
