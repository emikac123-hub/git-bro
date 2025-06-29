package com.erik.git_bro.service;

import com.erik.git_bro.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser
public class ParsingServiceTest {

    @Autowired
    private ParsingService parsingService;

    @Test
    public void testFilterAndExtractDiffLines() {
        String diff = "diff --git a/test.java b/test.java\n--- a/test.java\n+++ b/test.java\n@@ -1,1 +1,1 @@\n-public class Test { }\n+public class Test { public void newMethod() { } }";
        String expected = "public class Test { public void newMethod() { } }";
        assertEquals(expected, parsingService.filterAndExtractDiffLines(diff));
    }

    @Test
    public void testCleanChunk() {
        String chunk = "```json\n{\"key\": \"value\"}\n```";
        String expected = "{\"key\": \"value\"}";
        assertEquals(expected, parsingService.cleanChunk(chunk));
    }

    @Test
    public void testExtractAddedLinesOnly() {
        String diff = "+line1\n-line2\n+line3";
        String expected = "line1\nline3";
        assertEquals(expected, parsingService.extractAddedLinesOnly(diff));
    }

    @Test
    public void testExtractFilePathFromDiff() {
        String diff = "+++ b/path/to/file.java";
        String expected = "path/to/file.java";
        assertEquals(expected, parsingService.extractFilePathFromDiff(diff));
    }

    @Test
    public void testGetIssueCategory() {
        assertEquals(Category.SECURITY, parsingService.getIssueCategory("security issue"));
        assertEquals(Category.PERFORMANCE, parsingService.getIssueCategory("performance issue"));
        assertEquals(Category.STYLE, parsingService.getIssueCategory("style issue"));
        assertEquals(Category.GENERAL, parsingService.getIssueCategory("general issue"));
        assertEquals(Category.NO_FEEDBACK, parsingService.getIssueCategory(""));
    }
}
