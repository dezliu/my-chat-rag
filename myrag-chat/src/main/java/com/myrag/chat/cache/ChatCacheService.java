package com.myrag.chat.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myrag.common.config.ChatCacheProperties;
import com.myrag.common.constant.CacheKeyConstants;
import com.myrag.common.dto.CachedChatAnswer;
import com.myrag.common.dto.RagRouteDecision;
import com.myrag.rag.core.service.KbRevisionService;
import com.myrag.rag.core.service.SystemPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatCacheProperties cacheProperties;
    private final SystemPromptService systemPromptService;
    private final KbRevisionService kbRevisionService;

    public boolean isEnabledFor(String question) {
        return cacheProperties.isEnabled()
                && question != null
                && question.length() >= cacheProperties.getMinQuestionLength();
    }

    public String buildRedisKey(String question, RagRouteDecision route) {
        String normalized = normalize(question);
        int promptVersion = systemPromptService.getActiveVersion();
        List<String> kbIds = route.getKbIds() != null ? route.getKbIds() : List.of();
        String kbPart = route.isNeedRag() && !kbIds.isEmpty()
                ? kbIds.stream().sorted().collect(Collectors.joining(","))
                : "none";
        String revPart = route.isNeedRag() && !kbIds.isEmpty()
                ? kbRevisionService.revisionsFingerprint(kbIds)
                : "none";
        String raw = normalized + "|" + promptVersion + "|" + kbPart + "|" + revPart;
        return CacheKeyConstants.CHAT_ANSWER_PREFIX + sha256(raw);
    }

    public Optional<CachedChatAnswer> get(String redisKey) {
        try {
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, CachedChatAnswer.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize chat cache", e);
            return Optional.empty();
        }
    }

    public void put(String redisKey, CachedChatAnswer answer) {
        try {
            String json = objectMapper.writeValueAsString(answer);
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofHours(cacheProperties.getTtlHours()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize chat cache", e);
        }
    }

    public long clearAll() {
        Set<String> keys = redisTemplate.keys(CacheKeyConstants.CHAT_ANSWER_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted != null ? deleted : 0;
    }

    private String normalize(String question) {
        return question.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
