package com.example.docs;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.tokenization.Tokenizer;
import dev.langchain4j.model.openai.tokenization.OpenAiTokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitterTest {

    @Test
    void splitRespectsSizeAndOverlap() {
        OpenAiTokenCountEstimator estimator = new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O_2024_11_20);
        DocumentSplitter splitter = DocumentSplitters.recursive(10, 2, estimator);

        Tokenizer tokenizer = OpenAiTokenizer.gpt4o();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("word").append(i).append(' ');
        }
        Document doc = Document.from(sb.toString());
        List<TextSegment> chunks = splitter.split(doc);
        assertTrue(chunks.size() > 1);

        for (TextSegment chunk : chunks) {
            int tokens = estimator.estimateTokenCountInText(chunk.text());
            assertTrue(tokens <= 10);
        }

        List<Integer> first = tokenizer.encode(chunks.get(0).text());
        List<Integer> second = tokenizer.encode(chunks.get(1).text());
        int overlap = 2;
        List<Integer> tail = first.subList(first.size() - overlap, first.size());
        List<Integer> head = second.subList(0, overlap);
        assertEquals(tail, head);
    }
}
