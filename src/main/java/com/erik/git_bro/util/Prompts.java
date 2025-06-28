package com.erik.git_bro.util;

public class Prompts {

    public static String hasIssueBeenResolvedPrompt(String suggestionText, String updatedFileContent) {
        return String.format("""
                You previously suggested this issue:

                "%s"

                Here is the updated version of the file:

                %s

                Question: Does the current code resolve the problem described in the original suggestion?
                Answer strictly: "Yes" or "No".
                """, suggestionText.trim(), updatedFileContent.trim());
    }

    public static String getAnalysisPrompt(final String filename, final String diffContent) {
        return String.format(
                """
                        You are an expert senior Java software engineer specializing in backend services.
                        Review the following Git diff from file %s:

                        %s

                        Please carefully review the diff and identify any issues, including:
                        1. Bugs and correctness problems
                        2. Security issues
                        3. Style, naming, and formatting
                        4. Performance optimizations
                        5. Missing error handling or logging
                        6. Opportunities to improve readability and maintainability

                        For each issue found, output in JSON format with this structure:

                        {
                            "issues": [
                                {
                                "file": "src/main/java/com/erik/git_bro/service/ParsingService.java",
                                "line": 42,
                                "comment": "Possible null pointer dereference on this line."
                                },
                                ...
                            ],
                            "recommendation": "merge" | "do not merge"
                        }

                        * Use exact file name and line number from this diff.
                        * If unsure about line number, provide best guess based on context.
                        * Provide a code snippet to illustrate your point.
                        * If no issues are found, return: { "issues": [], "recommendation": "merge" }
                        * Do NOT include extra explanation or markdown — return pure JSON.


                                                                    """,
                filename, diffContent);
    }

    public static String getJavaDocPrompt(final String methodSource) {

        return String.format(
                """
                        Generate a professional JavaDoc comment for the following Java method:

                        %s

                        - Follow JavaDoc conventions
                        - Document parameters and return value
                        - Do not include extra explanations, only the JavaDoc block

                        """, methodSource);
    }
}
