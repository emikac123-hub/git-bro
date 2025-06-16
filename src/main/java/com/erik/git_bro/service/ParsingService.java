package com.erik.git_bro.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParsingService {
    private final String NULL_INPUT = "Input is null.";
    private final String MALFORMED_JSON = "Malformed JSON";

    public String filterAndExtractDiffLines(String diff) {
        StringBuilder filtered = new StringBuilder();
        String[] lines = diff.split("\n");

        for (String line : lines) {
            // Only keep lines starting with '+' or '-', excluding metadata
            if ((line.startsWith("+") || line.startsWith("-")) &&
                    !line.startsWith("+++") && !line.startsWith("---")) {
                filtered.append(line).append("\n");
            }
        }

        return this.extractAddedLinesOnly(filtered.toString());
    }

    public String cleanChunk(String chunk) {
        return chunk.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", ""); // Remove illegal control characters
    }

    public String extractAddedLinesOnly(String diff) {
        return Arrays.stream(diff.split("\n"))
                .filter(line -> line.startsWith("+") && !line.startsWith("+++"))
                .map(line -> line.substring(1)) // strip leading '+'
                .collect(Collectors.joining("\n"));
    }

    public String extractFilePathFromDiff(String diffContent) {
        Pattern pattern = Pattern.compile("^\\+\\+\\+ b/(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(diffContent);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    public List<String> splitDiffIntoChunks(String diff, int maxLinesPerChunk) {
        List<String> chunks = new ArrayList<>();
        String[] lines = diff.split("\n");
        StringBuilder chunk = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            chunk.append(line).append("\n");
            count++;
            if (count >= maxLinesPerChunk) {
                chunks.add(chunk.toString());
                chunk.setLength(0);
                count = 0;
            }
        }
        if (chunk.length() > 0) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    public String extractInput(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonString);

            JsonNode inputNode = rootNode.get("input");

            if (inputNode == null || inputNode.isNull()) {
                return NULL_INPUT;
            }

            return inputNode.asText();

        } catch (Exception e) {
            log.error("Error: Malformed JSON. ", e);
            return MALFORMED_JSON;
        }
    }

    public boolean isMalformedJson(final String output) {
        return MALFORMED_JSON.equals(output);
    }

    public boolean isNullJSONVal(final String output) {
        return NULL_INPUT.equals(output);
    }

}
