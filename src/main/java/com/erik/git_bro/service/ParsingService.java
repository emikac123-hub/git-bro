package com.erik.git_bro.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class responsible for parsing and processing git diff content and
 * JSON inputs.
 * <p>
 * This class provides utilities to:
 * <ul>
 * <li>Filter and extract relevant diff lines (added/removed code lines).</li>
 * <li>Clean illegal control characters from diff chunks.</li>
 * <li>Extract file paths from diff metadata.</li>
 * <li>Split diffs into smaller chunks by line count.</li>
 * <li>Parse JSON input strings safely, returning special indicators for null or
 * malformed input.</li>
 * </ul>
 * <p>
 * Designed for integration with services that analyze or review code changes
 * and parse structured JSON payloads.
 * 
 * <p>
 * <b>Note:</b> This class uses {@code lombok} annotations for constructor
 * injection and logging.
 * 
 * @author Erik
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParsingService {

    /**
     * Constant message returned when JSON input is null.
     */
    private final String NULL_INPUT = "Input is null.";

    /**
     * Constant message returned when JSON input is malformed.
     */
    private final String MALFORMED_JSON = "Malformed JSON";

    /**
     * Filters the provided diff string and extracts only lines that represent
     * actual code changes.
     * <p>
     * Keeps lines starting with '+' or '-', excluding diff metadata lines like
     * '+++' or '---'.
     * Then extracts only the added lines (starting with '+') without the '+'
     * prefix.
     * 
     * @param diff the raw diff string to filter and process
     * @return a string containing only added lines from the diff, with '+' prefix
     *         removed
     */
    public String filterAndExtractDiffLines(String diffContent) {
        final var diff = this.cleanChunk(diffContent);
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

    /**
     * Cleans a diff chunk by removing illegal control characters except common
     * whitespace (carriage return, newline, tab).
     * 
     * @param chunk the diff chunk to clean
     * @return a cleaned string with illegal control characters removed
     */
    public String cleanChunk(String chunk) {
        // Remove ```json and ``` wrappers
        String cleaned = chunk.replaceAll("(?m)^```(json)?\\s*", "") // start code fence
                .replaceAll("(?m)^```\\s*", ""); // end code fence

        // Then remove any non-printable control characters (excluding \r, \n, and \t)
        return cleaned.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
    }

    /**
     * Extracts only the added lines from a diff string.
     * <p>
     * Lines starting with '+' are included, with the leading '+' removed.
     * Lines starting with '+++' (metadata) are excluded.
     * 
     * @param diff the diff string to process
     * @return string of added lines concatenated by newline characters
     */
    public String extractAddedLinesOnly(String diff) {
        return Arrays.stream(diff.split("\n"))
                .filter(line -> line.startsWith("+") && !line.startsWith("+++"))
                .map(line -> line.substring(1)) // strip leading '+'
                .collect(Collectors.joining("\n"));
    }

    /**
     * Extracts the file path from a diff header line.
     * <p>
     * Searches for lines matching the pattern "+++ b/<file_path>" and returns the
     * extracted file path.
     * If no match is found, returns "unknown".
     * 
     * @param diffContent the full diff content string
     * @return the extracted file path or "unknown" if not found
     */
    public String extractFilePathFromDiff(String diffContent) {
        Pattern pattern = Pattern.compile("^\\+\\+\\+ b/(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(diffContent);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    /**
     * Splits a diff string into smaller chunks, each containing up to
     * {@code maxLinesPerChunk} lines.
     * 
     * @param diff             the diff string to split
     * @param maxLinesPerChunk the maximum number of lines per chunk
     * @return a list of diff chunks as strings
     */
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

    /**
     * Extracts the value of the "input" field from a JSON string.
     * <p>
     * Returns special constant messages if the input is null or the JSON is
     * malformed.
     * 
     * @param jsonString the JSON string to parse
     * @return the text content of the "input" field, or a special message if null
     *         or malformed
     */
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

    /**
     * Checks if a given output string indicates malformed JSON input.
     * 
     * @param output the output string to check
     * @return true if the output equals the malformed JSON message, false otherwise
     */
    public boolean isMalformedJson(final String output) {
        return MALFORMED_JSON.equals(output);
    }

    /**
     * Checks if a given output string indicates a null JSON input.
     * 
     * @param output the output string to check
     * @return true if the output equals the null input message, false otherwise
     */
    public boolean isNullJSONVal(final String output) {
        return NULL_INPUT.equals(output);
    }

    public Set<Integer> extractCommentableLines(String patch) {
        Set<Integer> commentableLines = new HashSet<>();
        String[] lines = patch.split("\n");

        int newLineNum = -1;
        for (String line : lines) {
            if (line.startsWith("@@")) {
                Matcher matcher = Pattern.compile("\\+([0-9]+)").matcher(line);
                if (matcher.find()) {
                    newLineNum = Integer.parseInt(matcher.group(1)) - 1;
                }
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                commentableLines.add(++newLineNum);
            } else if (!line.startsWith("-")) {
                newLineNum++;
            }
        }

        return commentableLines;
    }

    public Integer calculatePositionInDiffHunk(String patch, int targetLine) {
        String[] lines = patch.split("\n");
        int position = -1; // position in patch (to be returned)
        int currentNewLine = -1; // tracks new file line number currently processed

        // The patch may contain multiple hunks, each starting with @@ line
        for (String line : lines) {
            if (line.startsWith("@@")) {
                // Extract starting line number of new file from hunk header
                Matcher matcher = Pattern.compile("\\+(\\d+)").matcher(line);
                if (matcher.find()) {
                    currentNewLine = Integer.parseInt(matcher.group(1)) - 1; // -1 because we'll increment before
                                                                             // checking line
                    position = -1; // reset position for new hunk
                }
            } else {
                position++; // increment position for every line in patch except hunk header
                char prefix = line.charAt(0);

                if (prefix == '+') {
                    currentNewLine++; // added line in new file
                    if (currentNewLine == targetLine) {
                        return position;
                    }
                } else if (prefix == ' ') {
                    currentNewLine++; // unchanged line in new file
                    if (currentNewLine == targetLine) {
                        return position;
                    }
                } else if (prefix == '-') {
                    // deleted line in old file; does not affect new file line count
                    // position still increments because it is in the patch,
                    // but we do NOT increment currentNewLine here
                }
            }
        }
        return 0; // target line not found in patch
    }

    public Integer extractLineNumberFromFeedback(String feedback) {
        Pattern pattern = Pattern.compile("Line (\\d+):");
        Matcher matcher = pattern.matcher(feedback);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null; // Or throw an exception, depending on desired behavior
    }
}
