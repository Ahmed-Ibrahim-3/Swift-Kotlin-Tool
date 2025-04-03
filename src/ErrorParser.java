import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing error location information from compiler messages.
 */
public class ErrorParser {
    // Pattern to match error locations like "script:2:1: error: cannot find 'foo' in scope"
    // or "/path/to/script.swift:2:1: error: ..."
    private static final Pattern ERROR_LOCATION_PATTERN =
            Pattern.compile("(?:.*?[/\\\\])?[^:]+:(\\d+):(\\d+):\\s*(?:error|warning)");

    /**
     * Represents a location in a source file.
     */
    public static class Location {
        private final int line;
        private final int column;
        private final String fullMatch;

        public Location(int line, int column, String fullMatch) {
            this.line = line;
            this.column = column;
            this.fullMatch = fullMatch;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public String getFullMatch() {
            return fullMatch;
        }
    }

    /**
     * Parses a line of text to extract error location information.
     *
     * @param line The line to parse
     * @return A Location object if the line contains location information, null otherwise
     */
    public static Location parseLocation(String line) {
        Matcher matcher = ERROR_LOCATION_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                int lineNumber = Integer.parseInt(matcher.group(1));
                int columnNumber = Integer.parseInt(matcher.group(2));
                return new Location(lineNumber, columnNumber, matcher.group(0));
            } catch (NumberFormatException e) {
                // Should not happen as the regex ensures we have digits
                return null;
            }
        }
        return null;
    }
}