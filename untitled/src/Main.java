package src;

import models.User;

public class Main {
    public static void main(String[] args) {
        testValidUser();
        testInvalidUsername();
    }

    private static void testValidUser() {
        System.out.println("Создание username");
        try {
            User user1 = User.validate("levon_666", "Levon Simonian", "lovonsada@gmail.com");
            System.out.println("Создан: " + user1.format());

        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testInvalidUsername() {
        System.out.println("Невалидные username");

        try {
            User.validate("le", "levon 123", "lovonsadan@gmail.com");
            System.out.println("Error: дефис");
        } catch (IllegalArgumentException e) {
            System.out.println("Символ дефис: " + e.getMessage());
        }

        System.out.println();
    }
}