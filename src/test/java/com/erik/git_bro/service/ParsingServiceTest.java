package com.erik.git_bro.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParsingServiceTest {

    private ParsingService parsingService;

    @BeforeEach
    void setUp() {
        parsingService = new ParsingService();
    }

    @Test
    void filterAndExtractDiffLines_shouldReturnAddedLinesOnly() {
        String diff = """
                +Added line 1
                -Removed line 1
                +Added line 2
                --- metadata
                +++ b/file/path
                context line
                """;

        String result = parsingService.filterAndExtractDiffLines(diff);

        assertEquals("Added line 1\nAdded line 2", result.trim());
    }

    @Test
    void cleanChunk_shouldRemoveControlCharacters() {
        String dirtyChunk = "valid text \u0001\u0002 more text \u0007";

        String result = parsingService.cleanChunk(dirtyChunk);

        assertEquals("valid text  more text ", result);
    }

    @Test
    void extractAddedLinesOnly_shouldReturnOnlyAddedLines() {
        String diff = """
                +Added A
                +Added B
                --- metadata
                +++ b/path/to/file
                context
                """;

        String result = parsingService.extractAddedLinesOnly(diff);

        assertEquals("Added A\nAdded B", result.trim());
    }

    @Test
    void extractFilePathFromDiff_shouldReturnFilePath() {
        String diff = """
                --- a/old/file
                +++ b/src/main/java/com/example/MyFile.java
                + some change
                """;

        String result = parsingService.extractFilePathFromDiff(diff);

        assertEquals("src/main/java/com/example/MyFile.java", result);
    }

    @Test
    void extractFilePathFromDiff_shouldReturnUnknownIfNoPath() {
        String diff = """
                --- a/file
                --- random
                """;

        String result = parsingService.extractFilePathFromDiff(diff);

        assertEquals("unknown", result);
    }

    @Test
    void splitDiffIntoChunks_shouldSplitCorrectly() {
        String diff = """
                line 1
                line 2
                line 3
                line 4
                line 5
                """;

        List<String> chunks = parsingService.splitDiffIntoChunks(diff, 2);

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).contains("line 1"));
        assertTrue(chunks.get(0).contains("line 2"));
    }

    @Test
    void extractInput_shouldReturnInputValue() {
        String json = """
                {
                    "input": "my test input"
                }
                """;

        String result = parsingService.extractInput(json);

        assertEquals("my test input", result);
    }

    @Test
    void extractInput_shouldReturnNullInputMessage() {
        String json = """
                {
                    "input": null
                }
                """;

        String result = parsingService.extractInput(json);

        assertEquals("Input is null.", result);
        assertTrue(parsingService.isNullJSONVal(result));
    }

    @Test
    void extractInput_shouldReturnMalformedJsonMessage() {
        String malformedJson = "{ input: 'missing quotes }";

        String result = parsingService.extractInput(malformedJson);

        assertEquals("Malformed JSON", result);
        assertTrue(parsingService.isMalformedJson(result));
    }

}
