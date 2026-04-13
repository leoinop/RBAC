package src.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {
    }

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.compareTo(date2) > 0;
    }

    public static boolean isBeforeOrEqual(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.compareTo(date2) <= 0;
    }

    public static boolean isAfterOrEqual(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.compareTo(date2) >= 0;
    }

    public static String addDays(String date, int days) {
        if (date == null) {
            return getCurrentDate();
        }
        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            return localDate.plusDays(days).format(DATE_FORMATTER);
        } catch (Exception e) {
            return date;
        }
    }

    public static String addDaysToDateTime(String dateTime, int days) {
        if (dateTime == null) {
            return getCurrentDateTime();
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, DATETIME_FORMATTER);
            return localDateTime.plusDays(days).format(DATETIME_FORMATTER);
        } catch (Exception e) {
            return dateTime;
        }
    }

    public static String formatRelativeTime(String date) {
        if (date == null) {
            return "unknown";
        }

        try {
            LocalDateTime target = LocalDateTime.parse(date, DATETIME_FORMATTER);
            LocalDateTime now = LocalDateTime.now();

            if (target.isBefore(now)) {
                long days = ChronoUnit.DAYS.between(target, now);
                if (days == 0) {
                    long hours = ChronoUnit.HOURS.between(target, now);
                    if (hours == 0) {
                        long minutes = ChronoUnit.MINUTES.between(target, now);
                        return minutes + " minutes ago";
                    }
                    return hours + " hours ago";
                }
                return days + " days ago";
            } else {
                long days = ChronoUnit.DAYS.between(now, target);
                if (days == 0) {
                    long hours = ChronoUnit.HOURS.between(now, target);
                    if (hours == 0) {
                        long minutes = ChronoUnit.MINUTES.between(now, target);
                        return "in " + minutes + " minutes";
                    }
                    return "in " + hours + " hours";
                }
                return "in " + days + " days";
            }
        } catch (Exception e) {
            return date;
        }
    }

    public static String formatDate(String date) {
        if (date == null) {
            return "";
        }
        if (date.length() > 10) {
            return date.substring(0, 10);
        }
        return date;
    }

    public static boolean isValidDate(String date) {
        if (date == null) {
            return false;
        }
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidDateTime(String dateTime) {
        if (dateTime == null) {
            return false;
        }
        try {
            LocalDateTime.parse(dateTime, DATETIME_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static long daysBetween(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        try {
            LocalDate d1 = LocalDate.parse(date1, DATE_FORMATTER);
            LocalDate d2 = LocalDate.parse(date2, DATE_FORMATTER);
            return ChronoUnit.DAYS.between(d1, d2);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getExpirationWarning(String expiresAt) {
        if (expiresAt == null) {
            return "";
        }
        try {
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, DATETIME_FORMATTER);
            LocalDateTime now = LocalDateTime.now();
            long days = ChronoUnit.DAYS.between(now, expiry);

            if (days < 0) {
                return "EXPIRED";
            } else if (days == 0) {
                return "EXPIRES TODAY";
            } else if (days <= 3) {
                return "Expires in " + days + " days - WARNING!";
            } else if (days <= 7) {
                return "Expires in " + days + " days";
            }
            return "Expires in " + days + " days";
        } catch (Exception e) {
            return expiresAt;
        }
    }
}