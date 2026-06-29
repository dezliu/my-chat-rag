package com.myrag.common.util;

public final class ApiKeyMasker {

    private ApiKeyMasker() {
    }

    public static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "***";
        }
        return "sk-***" + apiKey.substring(apiKey.length() - 4);
    }
}
