package com.manadoksli.controller.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance parser for {@link DataIntegrityViolationException} messages.
 *
 * <p>This parser converts cryptic database constraint violation messages into
 * user-friendly error messages. It uses pre-compiled regex patterns and optimized
 * parsing strategies to minimize overhead.
 *
 * <p><strong>Performance Optimizations:</strong>
 * <ul>
 *   <li>Pre-compiled regex patterns (compiled once at initialization)</li>
 *   <li>Early exit on first match (no unnecessary pattern matching)</li>
 *   <li>Cached root cause extraction</li>
 *   <li>Immutable parser list for thread safety without synchronization</li>
 *   <li>Efficient string operations in sanitize method</li>
 * </ul>
 *
 * <p><strong>Supported Violations:</strong>
 * <ul>
 *   <li>Unique key constraints</li>
 *   <li>Foreign key constraints</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * try {
 *     userRepository.save(user);
 * } catch (DataIntegrityViolationException ex) {
 *     String friendlyMessage = parser.parse(ex);
 *     // Returns: "email already exists: john@example.com"
 * }
 * }</pre>
 *
 * @author Silverlake Project Team
 * @version 1.0 (Performance Optimized)
 * @since 1.0
 */
@Component
public class DataIntegrityViolationMessageParser {

    private final List<IntegrityViolationParser> parsers;
    private final Pattern uniqueKeyPattern;
    private final Pattern foreignKeyPattern;
    private final Pattern notNullPattern;

    public DataIntegrityViolationMessageParser() {
        // Pre-compile patterns once for reuse
        this.uniqueKeyPattern = Pattern.compile(
            "Key \\((.*?)\\)=\\((.*?)\\) already exists",
            Pattern.CASE_INSENSITIVE
        );

        this.foreignKeyPattern = Pattern.compile(
            "Key \\((.*?)\\)=\\((.*?)\\) is not present in table",
            Pattern.CASE_INSENSITIVE
        );

        // Fixed pattern to match: null value in column "column_name" of relation "table_name" violates not-null constraint
        this.notNullPattern = Pattern.compile(
            "null value in column \"(.*?)\" of relation \"(.*?)\" violates not-null constraint",
            Pattern.CASE_INSENSITIVE
        );

        // Create immutable list - thread-safe without synchronization
        this.parsers = List.of(
            this::parseUniqueKeyViolation,
            this::parseForeignKeyViolation,
            this::parseNotNullViolation
            // Add more parsers here as needed
        );
    }

    /**
     * Parses a {@link DataIntegrityViolationException} into a user-friendly message.
     *
     * <p>This method extracts the root cause message and applies registered parsers
     * in sequence. The first parser that successfully matches returns immediately,
     * providing early exit optimization.
     *
     * <p><strong>Performance Characteristics:</strong>
     * <ul>
     *   <li>Time Complexity: O(n) where n is number of parsers, but typically O(1) due to early exit</li>
     *   <li>Space Complexity: O(1) - no intermediate collections created</li>
     *   <li>Thread-safe: All patterns and parser list are immutable</li>
     * </ul>
     *
     * @param ex the data integrity violation exception to parse
     * @return a user-friendly error message describing the constraint violation
     */
    public String parse(DataIntegrityViolationException ex) {
        // Cache root cause - avoid multiple getMostSpecificCause() calls
        Throwable root = ex.getMostSpecificCause();
        String message = root.getMessage();

        // Early exit on first match - no unnecessary parser invocations
        return parsers.stream()
            .map(parser -> parser.parse(message))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseGet(() -> sanitize(message));
    }

    /* ==================== PARSERS ==================== */

    /**
     * Parses unique key constraint violations.
     *
     * <p>Matches patterns like: "Key (email)=(john@example.com) already exists"
     * Produces output: "email already exists: john@example.com"
     *
     * @param message the raw database error message
     * @return an Optional containing the parsed message, or empty if no match
     */
    private Optional<String> parseUniqueKeyViolation(String message) {
        Matcher matcher = uniqueKeyPattern.matcher(message);
        if (matcher.find()) {
            return Optional.of(
                matcher.group(1) + " already exists: " + matcher.group(2)
            );
        }
        return Optional.empty();
    }

    /**
     * Parses foreign key constraint violations.
     *
     * <p>Matches patterns like: "Key (user_id)=(999) is not present in table"
     * Produces output: "user_id does not exist: 999"
     *
     * @param message the raw database error message
     * @return an Optional containing the parsed message, or empty if no match
     */
    private Optional<String> parseForeignKeyViolation(String message) {
        Matcher matcher = foreignKeyPattern.matcher(message);
        if (matcher.find()) {
            String column = matcher.group(1);
            String value = matcher.group(2);
            return Optional.of(column + " does not exist: " + value);
        }
        return Optional.empty();
    }

    /**
     * Parses not-null constraint violations.
     *
     * <p>Matches patterns like: "null value in column "usage_remaining" of relation "rewards" violates not-null constraint"
     * Produces output: "usage_remaining is required"
     *
     * @param message the raw database error message
     * @return an Optional containing the parsed message, or empty if no match
     */
    private Optional<String> parseNotNullViolation(String message) {
        Matcher matcher = notNullPattern.matcher(message);
        if (matcher.find()) {
            String column = matcher.group(1);
            String table = matcher.group(2);
            return Optional.of(column + " is required in table " + table);

        }
        return Optional.empty();
    }

    /* ==================== FALLBACK ==================== */

    /**
     * Sanitizes raw database error messages as fallback when no parser matches.
     *
     * @param message the raw database error message
     * @return a sanitized version of the message with common prefixes removed
     */
    private String sanitize(String message) {
        return message
            .replace("ERROR:", "")
            .replace("Detail:", "")
            .replace("\\n", " ")
            .trim();
    }

    /**
     * Functional interface for integrity violation parsers.
     */
    @FunctionalInterface
    public interface IntegrityViolationParser {
        /**
         * Attempts to parse a database error message.
         *
         * @param message the raw database error message
         * @return an Optional containing the parsed message if pattern matches,
         * or empty if this parser cannot handle the message
         */
        Optional<String> parse(String message);
    }
}
