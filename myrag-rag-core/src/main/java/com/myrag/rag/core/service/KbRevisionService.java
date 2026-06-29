package com.myrag.rag.core.service;

import com.myrag.common.constant.CacheKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KbRevisionService {

    private final StringRedisTemplate redisTemplate;

    public long getRevision(String kbId) {
        String value = redisTemplate.opsForValue().get(CacheKeyConstants.KB_REV_PREFIX + kbId);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public String revisionsFingerprint(Collection<String> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return "none";
        }
        return kbIds.stream()
                .sorted()
                .map(id -> id + ":" + getRevision(id))
                .collect(Collectors.joining(","));
    }

    public void bumpRevision(String kbId) {
        redisTemplate.opsForValue().increment(CacheKeyConstants.KB_REV_PREFIX + kbId);
    }

    public void removeRevision(String kbId) {
        redisTemplate.delete(CacheKeyConstants.KB_REV_PREFIX + kbId);
    }
}
