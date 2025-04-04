import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorParser {
    private static final Pattern ERROR_LOCATION_PATTERN =
            Pattern.compile("(?:.*?[/\\\\])?[^:]+:(\\d+):(\\d+):\\s*(?:error|warning)");

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

     public static Location parseLocation(String line) {
        Matcher matcher = ERROR_LOCATION_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                int lineNumber = Integer.parseInt(matcher.group(1));
                int columnNumber = Integer.parseInt(matcher.group(2));
                return new Location(lineNumber, columnNumber, matcher.group(0));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}