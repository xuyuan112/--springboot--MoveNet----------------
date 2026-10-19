package com.rehab.admin.service;

import com.rehab.model.dto.FileCompleteRequest;
import com.rehab.model.dto.FilePresignRequest;
import com.rehab.admin.vo.FilePresignVO;
import com.rehab.admin.vo.FileUrlVO;

/**
 * 管理端 - MinIO 文件生命周期服务：预签名申请、完成确认、预览地址、引用校验删除。
 */
public interface AdminFileService {

    /**
     * 申请预签名直传地址（白名单校验后签发，会话信息存 Redis，TTL 与签名有效期一致）。
     */
    FilePresignVO presign(FilePresignRequest request);

    /**
     * 直传完成确认：校验对象存在、大小、MIME、SHA-256，通过后落库 file_object。
     *
     * @return 文件ID
     */
    Long complete(String uploadId, FileCompleteRequest request);

    /**
     * 获取文件短时预签名预览地址。
     */
    FileUrlVO getUrl(Long fileId);

    /**
     * 删除文件：被内容附件 / 模板样本 / 咨询消息引用时拒绝（409 FILE_IN_USE）。
     */
    void delete(Long fileId);
}
