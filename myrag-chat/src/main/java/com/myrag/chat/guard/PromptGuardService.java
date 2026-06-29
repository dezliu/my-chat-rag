package com.myrag.chat.guard;

import com.myrag.common.config.SecurityProperties;
import com.myrag.common.exception.MyragException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PromptGuardService {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|above|prior)\\s+instructions"),
            Pattern.compile("(?i)forget\\s+(everything|all|your)\\s+(instructions|rules|prompt)"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+"),
            Pattern.compile("(?i)new\\s+system\\s+prompt"),
            Pattern.compile("(?i)override\\s+(system|your)\\s+(prompt|instructions)"),
            Pattern.compile("(?i)disregard\\s+(all\\s+)?(previous|your)\\s+"),
            Pattern.compile("(?i)忽略(之前|上面|所有)(的)?(指令|规则|提示)"),
            Pattern.compile("(?i)你现在是"),
            Pattern.compile("(?i)忘记(之前|所有)(的)?(指令|规则)"),
            Pattern.compile("(?i)新的系统(提示|背景|角色)")
    );

    private final SecurityProperties securityProperties;

    public String sanitize(String userMessage, int maxLength) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new MyragException(400, "Message cannot be empty");
        }
        String trimmed = userMessage.trim();
        if (trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength);
        }
        if (securityProperties.isPromptInjectionEnabled()) {
            detectInjection(trimmed);
        }
        return trimmed;
    }

    private void detectInjection(String message) {
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                throw new MyragException(403, "检测到不安全的输入，请修改您的问题后重试");
            }
        }
    }
}
