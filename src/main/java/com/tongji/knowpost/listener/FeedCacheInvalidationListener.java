package com.tongji.knowpost.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tongji.counter.event.CounterEvent;
import com.tongji.knowpost.api.dto.FeedItemResponse;
import com.tongji.knowpost.model.KnowPost;
import com.tongji.knowpost.service.FeedCacheService;
import com.github.benmanes.caffeine.cache.Cache;
import com.tongji.knowpost.api.dto.FeedPageResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class FeedCacheInvalidationListener {

    private final FeedCacheService feedCacheService;
    private final Cache<String, FeedPageResponse> feedPublicCache;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final com.tongji.counter.service.UserCounterService userCounterService;
    private final com.tongji.knowpost.mapper.KnowPostMapper knowPostMapper;

    public FeedCacheInvalidationListener(FeedCacheService feedCacheService,
                                         @Qualifier("feedPublicCache") Cache<String, FeedPageResponse> feedPublicCache,
                                         StringRedisTemplate redis,
                                         ObjectMapper objectMapper,
                                         com.tongji.counter.service.UserCounterService userCounterService,
                                         com.tongji.knowpost.mapper.KnowPostMapper knowPostMapper) {
        this.feedCacheService = feedCacheService;
        this.feedPublicCache = feedPublicCache;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.userCounterService = userCounterService;
        this.knowPostMapper = knowPostMapper;
    }

    @EventListener
    public void onCounterChanged(CounterEvent event) {
        if (!"knowpost".equals(event.getEntityType())) return;
        String metric = event.getMetric();
        if ("like".equals(metric) || "fav".equals(metric)) {
            String eid = event.getEntityId();
            int delta = event.getDelta();
            try {
                KnowPost post = knowPostMapper.findById(Long.valueOf(eid));
                if (post != null && post.getCreatorId() != null) {
                    long owner = post.getCreatorId();//作品的作者
                    if ("like".equals(metric)) userCounterService.incrementLikesReceived(owner, delta);
                    if ("fav".equals(metric)) userCounterService.incrementFavsReceived(owner, delta);
                }
            } catch (Exception ignored) {
            }
            updateCountCache(eid, metric, delta);
            long hourSlot = System.currentTimeMillis() / 3600000L;
            Set<String> keys = new LinkedHashSet<>();
            Set<String> cur = redis.opsForSet().members("feed:public:index:" + eid + ":" + hourSlot);
            if (cur != null) keys.addAll(cur);
            Set<String> prev = redis.opsForSet().members("feed:public:index:" + eid + ":" + (hourSlot - 1));
            if (prev != null) keys.addAll(prev);
            if (keys == null || keys.isEmpty()) return;
            for (String key : keys) {
                FeedPageResponse local = feedPublicCache.getIfPresent(key);
                if (local != null) {
                    FeedPageResponse updatedLocal = adjustPageCounts(local, eid, metric, delta, true);
                    feedPublicCache.put(key, updatedLocal);
                }
                String cached = redis.opsForValue().get(key);
                if (cached != null) {
                    try {
                        FeedPageResponse resp = objectMapper.readValue(cached, FeedPageResponse.class);
                        FeedPageResponse updated = adjustPageCounts(resp, eid, metric, delta, false);
                        writePageJsonKeepingTtl(key, updated);
                    } catch (Exception ignored) {
                    }
                } else {
                    redis.opsForSet().remove("feed:public:index:" + eid + ":" + hourSlot, key);
                }
            }
        }
    }

    private void updateCountCache(String eid, String metric, int delta) {
        String cntKey = "feed:count:" + eid;
        String cntJson = redis.opsForValue().get(cntKey);
        Map<String, Long> cm = null;
        if (cntJson != null) {
            try {
                //转成Map类型
                cm = objectMapper.readValue(cntJson, new TypeReference<Map<String, Long>>() {
                });
            } catch (Exception ignored) {
            }
        }
        if (cm == null) cm = new LinkedHashMap<>();
        Long like = cm.getOrDefault("like", 0L);
        Long fav = cm.getOrDefault("fav", 0L);
        if ("like".equals(metric)) like = Math.max(0L, like + delta);
        if ("fav".equals(metric)) fav = Math.max(0L, fav + delta);
        cm.put("like", like);
        cm.put("fav", fav);
        try {
            String j = objectMapper.writeValueAsString(cm);
            Long ttl = redis.getExpire(cntKey);
            if (ttl != null && ttl > 0) redis.opsForValue().set(cntKey, j, java.time.Duration.ofSeconds(ttl));
            else redis.opsForValue().set(cntKey, j);
        } catch (Exception ignored) {
        }
    }

    private FeedPageResponse adjustPageCounts(FeedPageResponse page, String eid, String metric, int delta, boolean preserveUserFlags) {
        List<FeedItemResponse> items = new ArrayList<>(page.items().size());
        for (FeedItemResponse it : page.items()) {
            if (eid.equals(it.id())) {
                Long like = it.likeCount();
                Long fav = it.favoriteCount();
                if ("like".equals(metric)) like = Math.max(0L, (like == null ? 0L : like) + delta);
                if ("fav".equals(metric)) fav = Math.max(0L, (fav == null ? 0L : fav) + delta);
                Boolean liked = preserveUserFlags ? it.liked() : null;
                Boolean faved = preserveUserFlags ? it.faved() : null;
                it = new FeedItemResponse(it.id(), it.title(), it.description(), it.coverImage(), it.tags(), it.authorAvatar(), it.authorNickname(), it.tagJson(), like, fav, liked, faved, it.isTop());
            }
            items.add(it);
        }
        return new FeedPageResponse(items, page.page(), page.size(), page.hasMore());
    }

    private void writePageJsonKeepingTtl(String key, FeedPageResponse page) {
        try {
            String json = objectMapper.writeValueAsString(page);
            Long ttl = redis.getExpire(key);
            if (ttl != null && ttl > 0) {
                redis.opsForValue().set(key, json, Duration.ofSeconds(ttl));
            } else {
                redis.opsForValue().set(key, json);
            }
        } catch (Exception ignored) {
        }
    }
}
