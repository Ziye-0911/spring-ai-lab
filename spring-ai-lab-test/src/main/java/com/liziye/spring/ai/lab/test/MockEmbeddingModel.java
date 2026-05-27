package com.liziye.spring.ai.lab.test;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock EmbeddingModel — 返回固定长度的占位向量。
 */
public class MockEmbeddingModel implements EmbeddingModel {

    private final int dimension;

    public MockEmbeddingModel() {
        this(1536);
    }

    public MockEmbeddingModel(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < request.getInstructions().size(); i++) {
            float[] vector = new float[dimension];
            embeddings.add(new Embedding(vector, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(String text) {
        return new float[dimension];
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public int dimensions() {
        return dimension;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            result.add(embed(texts.get(i)));
        }
        return result;
    }
}
