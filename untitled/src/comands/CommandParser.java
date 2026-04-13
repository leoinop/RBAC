package src.commands;

import src.system.RBACSystem;
import java.util.*;

public class CommandParser {
    private final Map<String, Command> commands;
    private final Map<String, String> commandDescriptions;

    public CommandParser() {
        this.commands = new HashMap<>();
        this.commandDescriptions = new HashMap<>();
    }

    public void registerCommand(String name, String description, Command command) {
        commands.put(name.toLowerCase(), command);
        commandDescriptions.put(name.toLowerCase(), description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        String cmd = commandName.toLowerCase();
        Command command = commands.get(cmd);

        if (command == null) {
            System.out.println("Unknown command: " + commandName);
            System.out.println("Type 'help' to see available commands");
            return;
        }

        try {
            command.execute(scanner, system);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }

    public void printHelp() {
        System.out.println("\n========== AVAILABLE COMMANDS ==========");

        List<String> sortedCommands = new ArrayList<>(commands.keySet());
        Collections.sort(sortedCommands);

        for (String cmd : sortedCommands) {
            String description = commandDescriptions.get(cmd);
            System.out.printf("  %-25s - %s%n", cmd, description);
        }

        System.out.println("========================================\n");
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0];

        executeCommand(commandName, scanner, system);
    }
}