package com.botmanager.util;

import com.botmanager.controller.MainController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to generate execution commands for bots.
 */
public final class CommandGenerator {

    private static final String TARGET_SUBDIRECTORY = "target";
    private static final String DEFAULT_JAR_FILENAME_PATTERN = "%s-1.0-SNAPSHOT.jar";

    private CommandGenerator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Generates the command list to start a Java-based bot using an executable JAR.
     *
     * @param bot The bot for which to generate the start command.
     *            Must not be null and must have a valid project path and name.
     * @return A list of strings representing the command and its arguments (e.g., ["java", "-jar", "path/to/bot.jar"]).
     *         Returns {@code null} if the bot's configuration is invalid, project path doesn't exist,
     *         or the bot's JAR file cannot be found.
     */
    public static List<String> generateJavaJarStartCommand(MainController.Bot bot) {
        if (bot == null) {
            System.err.println("CommandGenerator: Bot object is null.");
            return null;
        }
        if (bot.getProjectPath() == null || bot.getProjectPath().trim().isEmpty()) {
            System.err.println("CommandGenerator: Bot project path is null or empty for bot: " + bot.getName());
            return null;
        }
        if (bot.getName() == null || bot.getName().trim().isEmpty()) {
            System.err.println("CommandGenerator: Bot name is null or empty, cannot determine JAR name.");
            return null;
        }

        Path projectPathDir = Paths.get(bot.getProjectPath());
        if (!Files.isDirectory(projectPathDir)) {
            System.err.println("CommandGenerator: Bot project path does not exist or is not a directory: " + projectPathDir);
            return null;
        }

        String jarFileName = String.format(DEFAULT_JAR_FILENAME_PATTERN, bot.getName());
        Path botJarPath = projectPathDir.resolve(TARGET_SUBDIRECTORY).resolve(jarFileName);

        if (!Files.exists(botJarPath) || !Files.isRegularFile(botJarPath)) {
            System.err.println("CommandGenerator: Bot JAR file not found or is not a regular file at: " + botJarPath);
            return null;
        }

        List<String> command = new ArrayList<>();
        command.add("java");

        String jvmArgs = bot.getJvmArgs();
        if (jvmArgs != null && !jvmArgs.trim().isEmpty()) {
            String[] individualJvmArgs = jvmArgs.trim().split("\\s+");
            command.addAll(Arrays.asList(individualJvmArgs));
        }

        command.add("-jar");
        command.add(botJarPath.toString());

        System.out.println("CommandGenerator: Generated command: " + String.join(" ", command));
        return command;
    }

    /**
     * Helper method to get the expected path of the bot's JAR file.
     * Useful for constructing error messages in the UI.
     *
     * @param bot The bot.
     * @return The expected Path to the JAR file, or null if bot/path info is invalid.
     */
    public static Path getExpectedJarPath(MainController.Bot bot) {
        if (bot == null || bot.getProjectPath() == null || bot.getProjectPath().trim().isEmpty() ||
                bot.getName() == null || bot.getName().trim().isEmpty()) {
            return null;
        }
        String jarFileName = String.format(DEFAULT_JAR_FILENAME_PATTERN, bot.getName());
        return Paths.get(bot.getProjectPath(), TARGET_SUBDIRECTORY, jarFileName);
    }
}