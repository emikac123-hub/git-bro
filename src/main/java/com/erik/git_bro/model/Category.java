package com.erik.git_bro.model;

/**
 * Represents the category of feedback provided by the code analysis.
 */
public enum Category {
    /**
     * A potential security vulnerability, such as SQL injection or a null pointer exception.
     */
    SECURITY,

    /**
     * A potential performance issue, such as an inefficient algorithm or a race condition.
     */
    PERFORMANCE,

    /**
     * A deviation from coding style guidelines or naming conventions.
     */
    STYLE,

    /**
     * A general piece of feedback that does not fit into other categories.
     */
    GENERAL,

    /**
     * Indicates that no feedback was generated, possibly due to an error or clean code.
     */
    NO_FEEDBACK
}
