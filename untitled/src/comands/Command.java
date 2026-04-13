package src.commands;

import java.util.Scanner;
import src.system.RBACSystem;

@FunctionalInterface
public interface Command {
    void execute(Scanner scanner, RBACSystem system);
}