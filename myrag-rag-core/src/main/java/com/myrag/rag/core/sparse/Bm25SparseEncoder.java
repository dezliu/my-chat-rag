package com.myrag.rag.core.sparse;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * BM25-style sparse vector encoder for Qdrant hybrid search.
 * Uses HanLP for Chinese tokenization and MurmurHash for term indices.
 */
@Component
public class Bm25SparseEncoder {

    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private final Map<String, Integer> docFreq = new ConcurrentHashMap<>();
    private int totalDocs = 0;
    private double avgDocLength = 0;
    private final Map<String, Double> docLengthCache = new ConcurrentHashMap<>();

    public synchronized void indexDocument(String docId, String text) {
        List<String> tokens = tokenize(text);
        totalDocs++;
        double len = tokens.size();
        docLengthCache.put(docId, len);
        avgDocLength = docLengthCache.values().stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
        Set<String> unique = new HashSet<>(tokens);
        for (String token : unique) {
            docFreq.merge(token, 1, Integer::sum);
        }
    }

    public SparseVectorData encode(String text) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return new SparseVectorData(List.of(), List.of());
        }
        Map<Long, Float> indexToWeight = new HashMap<>();
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            tf.merge(token, 1, Integer::sum);
        }
        double docLen = tokens.size();
        for (Map.Entry<String, Integer> entry : tf.entrySet()) {
            String token = entry.getKey();
            int freq = entry.getValue();
            int df = docFreq.getOrDefault(token, 1);
            double idf = Math.log(1 + (totalDocs - df + 0.5) / (df + 0.5));
            double tfNorm = (freq * (K1 + 1)) / (freq + K1 * (1 - B + B * docLen / Math.max(avgDocLength, 1.0)));
            long index = hashToken(token);
            indexToWeight.merge(index, (float) (idf * tfNorm), Float::sum);
        }
        List<Long> indices = new ArrayList<>(indexToWeight.keySet());
        List<Float> values = indices.stream().map(indexToWeight::get).collect(Collectors.toList());
        return new SparseVectorData(indices, values);
    }

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<Term> terms = HanLP.segment(text);
        return terms.stream()
                .map(t -> t.word.toLowerCase().trim())
                .filter(w -> w.length() > 1)
                .collect(Collectors.toList());
    }

    private long hashToken(String token) {
        return Math.abs(token.hashCode()) & 0xFFFFFFFFL;
    }

    public record SparseVectorData(List<Long> indices, List<Float> values) {}
}
