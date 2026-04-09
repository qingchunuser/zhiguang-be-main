package com.tongji.knowpost.api.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 帖子元数据更新请求（部分字段可选）。
 */
public record KnowPostPatchRequest(
        /*
        * 标题输入框："输入内容标题"
         *    - 内容类型下拉框："图文"
         *    - 内容正文："写下你的知识内容..."
         *    - 知识摘要："填写内容摘要（50 字以内）"
         *    - 价格输入框："0"
         *    - 标签输入框："输入标签后按回车"
         *    - 免费分享开关：已开启
         *    - 可见范围开关：公开*/
        String title,
        Long tagId,
        @Size(max = 20) List<String> tags,
        @Size(max = 20) List<String> imgUrls,
        String visible,
        Boolean isTop,
        String description
) {}