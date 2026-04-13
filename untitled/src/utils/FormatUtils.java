package src.utils;

import java.util.List;

public class FormatUtils {

    private static final String HORIZONTAL_LINE = "-";
    private static final String VERTICAL_LINE = "|";
    private static final String CROSS = "+";

    private FormatUtils() {
    }

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        int columnCount = headers.length;
        int[] columnWidths = new int[columnCount];

        for (int i = 0; i < columnCount; i++) {
            columnWidths[i] = headers[i].length();
        }

        if (rows != null) {
            for (String[] row : rows) {
                if (row != null) {
                    for (int i = 0; i < Math.min(row.length, columnCount); i++) {
                        if (row[i] != null) {
                            columnWidths[i] = Math.max(columnWidths[i], row[i].length());
                        }
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();

        String topLine = buildHorizontalLine(columnWidths);
        sb.append(topLine).append("\n");

        sb.append(buildHeaderRow(headers, columnWidths)).append("\n");

        sb.append(topLine).append("\n");

        if (rows != null) {
            for (String[] row : rows) {
                if (row != null) {
                    sb.append(buildDataRow(row, columnWidths)).append("\n");
                }
            }
        }

        sb.append(topLine);

        return sb.toString();
    }

    private static String buildHorizontalLine(int[] columnWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append(CROSS);
        for (int width : columnWidths) {
            sb.append(HORIZONTAL_LINE.repeat(width + 2));
            sb.append(CROSS);
        }
        return sb.toString();
    }

    private static String buildHeaderRow(String[] headers, int[] columnWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append(VERTICAL_LINE);
        for (int i = 0; i < headers.length; i++) {
            sb.append(" ").append(padRight(headers[i], columnWidths[i])).append(" ");
            sb.append(VERTICAL_LINE);
        }
        return sb.toString();
    }

    private static String buildDataRow(String[] row, int[] columnWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append(VERTICAL_LINE);
        for (int i = 0; i < columnWidths.length; i++) {
            String value = (i < row.length && row[i] != null) ? row[i] : "";
            sb.append(" ").append(padRight(value, columnWidths[i])).append(" ");
            sb.append(VERTICAL_LINE);
        }
        return sb.toString();
    }

    public static String formatBox(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int width = text.length() + 4;
        StringBuilder sb = new StringBuilder();

        sb.append(CROSS).append(HORIZONTAL_LINE.repeat(width)).append(CROSS).append("\n");
        sb.append(VERTICAL_LINE).append(" ").append(text).append(" ").append(VERTICAL_LINE).append("\n");
        sb.append(CROSS).append(HORIZONTAL_LINE.repeat(width)).append(CROSS);

        return sb.toString();
    }

    public static String formatHeader(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int width = text.length() + 4;
        StringBuilder sb = new StringBuilder();

        sb.append("\n").append(HORIZONTAL_LINE.repeat(width)).append("\n");
        sb.append("  ").append(text).append("\n");
        sb.append(HORIZONTAL_LINE.repeat(width));

        return sb.toString();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= 3) {
            return "...";
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        return text + " ".repeat(length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        return " ".repeat(length - text.length()) + text;
    }

    public static String formatList(List<String> items, String title) {
        if (items == null || items.isEmpty()) {
            return "No items";
        }

        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append(formatHeader(title)).append("\n");
        }

        for (int i = 0; i < items.size(); i++) {
            sb.append(String.format("  %d. %s%n", i + 1, items.get(i)));
        }

        return sb.toString();
    }

    public static String formatKeyValue(String key, String value) {
        return String.format("%-20s: %s", key, value);
    }

    public static String formatSeparator() {
        return HORIZONTAL_LINE.repeat(50);
    }
}