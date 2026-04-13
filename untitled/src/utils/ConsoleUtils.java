package src.utils;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    private ConsoleUtils() {
    }

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();

            if (required && (input == null || input.trim().isEmpty())) {
                System.out.println("Error: This field is required. Please try again.");
                continue;
            }

            return input != null ? input.trim() : "";
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message);
            try {
                int input = Integer.parseInt(scanner.nextLine());
                if (input >= min && input <= max) {
                    return input;
                } else {
                    System.out.println("Error: Please enter a number between " + min + " and " + max);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid number. Please enter a valid integer.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (yes/no): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("yes") || input.equals("y")) {
                return true;
            } else if (input.equals("no") || input.equals("n")) {
                return false;
            } else {
                System.out.println("Error: Please enter 'yes' or 'no'");
            }
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            System.out.println("Error: No options available");
            return null;
        }

        while (true) {
            System.out.println("\n" + message);
            System.out.println("----------------------------------------");
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, options.get(i).toString());
            }
            System.out.println("----------------------------------------");
            System.out.print("Choose option (1-" + options.size() + "): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice >= 1 && choice <= options.size()) {
                    return options.get(choice - 1);
                } else {
                    System.out.println("Error: Please enter a number between 1 and " + options.size());
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid input. Please enter a number.");
            }
        }
    }

    public static String promptDate(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (YYYY-MM-DD HH:MM:SS): ");
            String input = scanner.nextLine().trim();

            if (ValidationUtils.isValidDate(input)) {
                return input;
            } else {
                System.out.println("Error: Invalid date format. Use YYYY-MM-DD HH:MM:SS");
            }
        }
    }

    public static void printHeader(String title) {
        System.out.println("\n========================================");
        System.out.println("  " + title);
        System.out.println("========================================\n");
    }

    public static void printSuccess(String message) {
        System.out.println("[SUCCESS] " + message);
    }

    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void printTableRow(String... columns) {
        for (String col : columns) {
            System.out.printf("%-20s", col);
        }
        System.out.println();
    }

    public static void printSeparator() {
        System.out.println("----------------------------------------");
    }
}