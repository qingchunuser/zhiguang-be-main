package com.tongji.knowpost.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 内容上传确认请求。
 */
public record KnowPostContentConfirmRequest(
        @NotBlank String objectKey,   // OSS 对象键（文件路径）
        @NotBlank String etag,        // ETag（OSS 返回的文件标识）
        @NotNull Long size,           // 文件大小（字节）
        @NotBlank String sha256       // SHA256 哈希值
) {}
