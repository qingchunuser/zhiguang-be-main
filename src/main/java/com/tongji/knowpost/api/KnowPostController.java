package com.tongji.knowpost.api;

import com.tongji.auth.token.JwtService;
import com.tongji.knowpost.api.dto.KnowPostContentConfirmRequest;
import com.tongji.knowpost.api.dto.KnowPostDraftCreateResponse;
import com.tongji.knowpost.api.dto.KnowPostPatchRequest;
import com.tongji.knowpost.api.dto.KnowPostTopPatchRequest;
import com.tongji.knowpost.api.dto.KnowPostVisibilityPatchRequest;
import com.tongji.knowpost.api.dto.FeedPageResponse;
import com.tongji.knowpost.service.KnowPostService;
import com.tongji.knowpost.service.KnowPostFeedService;
import com.tongji.knowpost.api.dto.KnowPostDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowposts")
@Validated
@RequiredArgsConstructor
public class KnowPostController {

    private final KnowPostService service;
    private final KnowPostFeedService feedService;
    private final JwtService jwtService;

    /**
     * 创建草稿，返回新 ID。默认类型为 image_text。
     */
    @PostMapping("/drafts")
    public KnowPostDraftCreateResponse createDraft(@AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        long id = service.createDraft(userId);
        return new KnowPostDraftCreateResponse(String.valueOf(id));
    }

    /**
     * 上传内容成功后回传确认，写入对象存储信息。
     *  发布流程中的 OSS 对象存储回传确认环节。
     */
    @PostMapping("/{id}/content/confirm")
    public ResponseEntity<Void> confirmContent(@PathVariable("id") long id,//知文ID
                                               @Valid @RequestBody KnowPostContentConfirmRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        /*@NotBlank String objectKey,   // OSS 对象键（文件路径）
          @NotBlank String etag,        // ETag（OSS 返回的文件标识）
          @NotNull Long size,           // 文件大小（字节）
          @NotBlank String sha256       // SHA256 哈希值*/
        service.confirmContent(userId, id, request.objectKey(), request.etag(), request.size(), request.sha256());
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新元数据（标题、标签、可见性、置顶、图片列表等）。
     * 📝 前端元素：
     *    - 标题输入框："输入内容标题"
     *    - 内容类型下拉框："图文"
     *    - 内容正文："写下你的知识内容..."
     *    - 知识摘要："填写内容摘要（50 字以内）"
     *    - 价格输入框："0"
     *    - 标签输入框："输入标签后按回车"
     *    - 免费分享开关：已开启
     *    - 可见范围开关：公开
     *
     * 🔌 前端调用：PATCH /api/v1/knowposts/{id}
     * 💾 后端方法：KnowPostController.patchMetadata()
     * 📦 请求体：
     *    {
     *      "title": "Java 入门教程",
     *      "tagId": 1,
     *      "tags": ["Java", "编程", "入门"],
     *      "imgUrls": ["https://...", "https://..."],
     *      "visible": "public",
     *      "isTop": false,
     *      "description": "本文介绍 Java 基础语法..."
     *    }
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> patchMetadata(@PathVariable("id") long id,
                                              @Valid @RequestBody KnowPostPatchRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        service.updateMetadata(userId, id, request.title(), request.tagId(), request.tags(), request.imgUrls(), request.visible(), request.isTop(), request.description());
        return ResponseEntity.noContent().build();
    }

    /**
     * 发布帖子（状态置为 published）。
     * 🔘 前端元素：橙色"发布"按钮
     * 🔌 前端调用：POST /api/v1/knowposts/{id}/publish
     * 💾 后端方法：KnowPostController.publish()
     * 📊 数据库变更：status = "draft" → "published"
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable("id") long id,
                                        @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        service.publish(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 设置置顶状态。
     */
    @PatchMapping("/{id}/top")
    public ResponseEntity<Void> patchTop(@PathVariable("id") long id,
                                         @Valid @RequestBody KnowPostTopPatchRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        service.updateTop(userId, id, request.isTop());
        return ResponseEntity.noContent().build();
    }

    /**
     * 设置可见性（权限）。
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> patchVisibility(@PathVariable("id") long id,
                                                @Valid @RequestBody KnowPostVisibilityPatchRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        service.updateVisibility(userId, id, request.visible());
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除知文（软删除）。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id,
                                       @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 首页 Feed（公开、已发布）分页查询；默认每页 20，最大 50。
     */
    @GetMapping("/feed")
    public FeedPageResponse feed(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size,
                                 @AuthenticationPrincipal Jwt jwt) {
        Long userId = (jwt == null) ? null : jwtService.extractUserId(jwt);
        /*page=1, size=20  → 返回第 1-20 条数据
          page=2, size=20  → 返回第 21-40 条数据
          page=3, size=20  → 返回第 41-60 条数据
          page=5, size=20  → 返回第 81-100 条数据*/
        return feedService.getPublicFeed(page, size, userId);
    }

    /**
     * 我的知文（当前用户已发布）分页查询；默认每页 20，最大 50。
     */
    @GetMapping("/mine")
    public FeedPageResponse mine(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size,
                                 @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return feedService.getMyPublished(userId, page, size);
    }

    /**
     * 知文详情（公开：published+public；非公开需作者本人）。
     */
    @GetMapping("/detail/{id}")
    public KnowPostDetailResponse detail(@PathVariable("id") long id,
                                         @AuthenticationPrincipal Jwt jwt) {
        Long userId = (jwt == null) ? null : jwtService.extractUserId(jwt);
        return service.getDetail(id, userId);
    }
}