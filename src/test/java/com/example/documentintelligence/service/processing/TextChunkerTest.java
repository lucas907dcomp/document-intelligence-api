package com.example.documentintelligence.service.processing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    // Instantiate directly — no Spring context needed for pure unit test
    private final TextChunker chunker = new TextChunker(1000, 200);

    @Test
    void nullInputReturnsEmptyList() {
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void emptyStringReturnsEmptyList() {
        assertThat(chunker.chunk("")).isEmpty();
    }

    @Test
    void blankStringReturnsEmptyList() {
        assertThat(chunker.chunk("   ")).isEmpty();
    }

    @Test
    void shortTextProducesSingleChunk() {
        String text = "Hello, world!";
        List<String> chunks = chunker.chunk(text);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Hello, world!");
    }

    @Test
    void textShorterThanStepProducesSingleChunk() {
        // step = chunkSize - overlap = 800; text <= step → single chunk
        String text = "X".repeat(800);
        List<String> chunks = chunker.chunk(text);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(800);
    }

    @Test
    void longTextProducesMultipleChunksWithCorrectOverlap() {
        // 2600 distinct chars → step=800 → chunks at [0,1000), [800,1800), [1600,2600), [2400,2600)
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(100); // 2600 chars
        List<String> chunks = chunker.chunk(text);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(3);

        // Last 200 chars of chunk[0] must equal first 200 chars of chunk[1]
        String tail0 = chunks.get(0).substring(chunks.get(0).length() - 200);
        String head1 = chunks.get(1).substring(0, 200);
        assertThat(tail0).isEqualTo(head1);
    }

    @Test
    void lastChunkCanBeSmallerThanChunkSize() {
        // 1100 chars → step=800 → chunk[0]=1000, chunk[1]=300 (< 1000)
        String text = "Z".repeat(1100);
        List<String> chunks = chunker.chunk(text);
        assertThat(chunks.get(chunks.size() - 1).length()).isLessThan(1000);
    }

    @Test
    void chunkCountMatchesExpectedForKnownInput() {
        // 2600 chars, step=800: i=0,800,1600,2400 → 4 iterations
        String text = "A".repeat(2600);
        List<String> chunks = chunker.chunk(text);
        assertThat(chunks).hasSize(4);
    }
}
