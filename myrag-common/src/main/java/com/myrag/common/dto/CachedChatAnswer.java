package com.myrag.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedChatAnswer {
    private String reply;
    private boolean usedRag;
    private List<String> ragKbIds;
}
