package com.botmanager.util;

import com.botmanager.controller.NewBotWizardController.BotCreationDetails; // Import the updated BotCreationDetails

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class to generate a new Discord bot project based on a template.
 */
public class BotGenerator {

    /**
     * Generates a new bot project.
     * @param details The details for bot creation.
     * @throws IOException If there's an error creating directories or writing files.
     */
    public void generateBotProject(BotCreationDetails details) throws IOException {
        Path projectRoot = details.getFullProjectPath();

        // 1. Create project root directory
        Files.createDirectories(projectRoot);
        System.out.println("Created project directory: " + projectRoot);

        // Prepare template variables
        Map<String, String> templateVars = new HashMap<>();
        templateVars.put("BOT_DISPLAY_NAME", details.getDisplayName());
        templateVars.put("PROJECT_NAME", details.getProjectName());
        templateVars.put("GROUP_ID", details.getGroupId());
        templateVars.put("BOT_VERSION", details.getVersion());
        templateVars.put("BOT_DESCRIPTION", details.getDescription());
        templateVars.put("MAIN_CLASS_NAME", details.getMainClassName());
        templateVars.put("PACKAGE_PATH", details.getGroupId().replace('.', '/'));
        templateVars.put("JDA_VERSION", "5.0.0-beta.23"); // Example JDA version
        templateVars.put("SLF4J_VERSION", "2.0.7");      // Example SLF4J version (for logging)
        templateVars.put("JAVA_VERSION", details.getJavaVersion());
        templateVars.put("BOT_TOKEN", details.getBotToken()); // Pass token, but discourage hardcoding
        templateVars.put("INCLUDE_TOKEN_ENV", String.valueOf(details.isIncludeDiscordTokenEnv()));
        templateVars.put("INCLUDE_PREFIX_ENV", String.valueOf(details.isIncludePrefixEnv()));
        templateVars.put("BOT_TYPE", details.getBotType());


        // 2. Create basic Maven project structure
        Path srcMainJava = projectRoot.resolve("src/main/java").resolve(templateVars.get("PACKAGE_PATH"));
        Path srcMainResources = projectRoot.resolve("src/main/resources");
        Path srcTestJava = projectRoot.resolve("src/test/java").resolve(templateVars.get("PACKAGE_PATH"));

        Files.createDirectories(srcMainJava);
        Files.createDirectories(srcMainResources);
        Files.createDirectories(srcTestJava);
        System.out.println("Created basic Maven directories.");

        // 3. Generate pom.xml
        String pomContent = generatePomXml(templateVars);
        Files.writeString(projectRoot.resolve("pom.xml"), pomContent);
        System.out.println("Generated pom.xml");

        // 4. Generate Main Bot Class
        String mainClassContent = generateMainClass(templateVars); // Pass entire map
        Files.writeString(srcMainJava.resolve(details.getMainClassName() + ".java"), mainClassContent);
        System.out.println("Generated " + details.getMainClassName() + ".java");

        // 5. Generate logback.xml (basic logging config)
        String logbackContent = """
            <configuration>
                <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                    <encoder>
                        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
                    </encoder>
                </appender>
                <root level="INFO">
                    <appender-ref ref="STDOUT" />
                </root>
                <logger name="net.dv8tion" level="INFO"/>
                <logger name="discord4j" level="INFO"/>
                <logger name="reactor" level="INFO"/>
            </configuration>
            """;
        Files.writeString(srcMainResources.resolve("logback.xml"), logbackContent);
        System.out.println("Generated logback.xml");

        // 6. Generate .gitignore
        String gitignoreContent = """
            /target/
            .idea/
            .classpath
            .project
            .settings/
            *.iml
            *.ipr
            *.iws
            /.vscode/
            /bin/
            /out/
            /.DS_Store
            # Environment variables should not be committed
            .env
            """;
        Files.writeString(projectRoot.resolve(".gitignore"), gitignoreContent);
        System.out.println("Generated .gitignore");

        // 7. Generate a simple .env file for DISCORD_TOKEN
        if (details.isIncludeDiscordTokenEnv()) {
            String envContent = String.format("DISCORD_TOKEN=%s\n", details.getBotToken().isEmpty() ? "PASTE_YOUR_TOKEN_HERE" : details.getBotToken());
            if (details.isIncludePrefixEnv()) {
                envContent += "BOT_PREFIX=!\n";
            }
            Files.writeString(projectRoot.resolve(".env"), envContent);
            System.out.println("Generated .env file with token placeholder/value.");
        }
    }

    /**
     * Generates the pom.xml content based on template variables.
     */
    private String generatePomXml(Map<String, String> vars) {
        String dependencies = "";
        String jvmSourceTarget = vars.get("JAVA_VERSION"); // Use selected Java version

        switch (vars.get("BOT_TYPE")) {
            case "JDA (Java Discord API) Basic":
                dependencies = """
                        <dependency>
                            <groupId>net.dv8tion</groupId>
                            <artifactId>JDA</artifactId>
                            <version>%JDA_VERSION%</version>
                            <exclusions>
                                <exclusion>
                                    <groupId>ch.qos.logback</groupId>
                                    <artifactId>logback-classic</artifactId>
                                </exclusion>
                            </exclusions>
                        </dependency>
                        <dependency>
                            <groupId>org.slf4j</groupId>
                            <artifactId>slf4j-api</artifactId>
                            <version>%SLF4J_VERSION%</version>
                        </dependency>
                        <dependency>
                            <groupId>ch.qos.logback</groupId>
                            <artifactId>logback-classic</artifactId>
                            <version>%SLF4J_VERSION%</version>
                        </dependency>
                    """.replace("%JDA_VERSION%", vars.get("JDA_VERSION"))
                        .replace("%SLF4J_VERSION%", vars.get("SLF4J_VERSION"));
                break;
            case "Discord4J (Reactive Java Discord API) Basic":
                dependencies = """
                        <dependency>
                            <groupId>com.discord4j</groupId>
                            <artifactId>discord4j-core</artifactId>
                            <version>3.2.6</version>
                        </dependency>
                        <dependency>
                            <groupId>io.projectreactor</groupId>
                            <artifactId>reactor-core</artifactId>
                            <version>3.5.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.slf4j</groupId>
                            <artifactId>slf4j-api</artifactId>
                            <version>%SLF4J_VERSION%</version>
                        </dependency>
                        <dependency>
                            <groupId>ch.qos.logback</groupId>
                            <artifactId>logback-classic</artifactId>
                            <version>%SLF4J_VERSION%</version>
                        </dependency>
                    """.replace("%SLF4J_VERSION%", vars.get("SLF4J_VERSION"));
                break;
            case "Custom (Advanced)":
                // No specific Discord dependencies here, user will add manually
                break;
        }

        String pomTemplate = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>

                <groupId>%GROUP_ID%</groupId>
                <artifactId>%PROJECT_NAME%</artifactId>
                <version>%BOT_VERSION%</version>

                <name>%BOT_DISPLAY_NAME%</name>
                <description>%BOT_DESCRIPTION%</description>

                <properties>
                    <maven.compiler.source>%JVM_SOURCE_TARGET%</maven.compiler.source>
                    <maven.compiler.target>%JVM_SOURCE_TARGET%</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>

                <dependencies>
                    %DEPENDENCIES%
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-api</artifactId>
                        <version>5.9.2</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.11.0</version>
                            <configuration>
                                <source>%JVM_SOURCE_TARGET%</source>
                                <target>%JVM_SOURCE_TARGET%</target>
                            </configuration>
                        </plugin>

                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-shade-plugin</artifactId>
                            <version>3.4.1</version>
                            <executions>
                                <execution>
                                    <phase>package</phase>
                                    <goals>
                                        <goal>shade</goal>
                                    </goals>
                                    <configuration>
                                        <transformers>
                                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                                <mainClass>%GROUP_ID%.%MAIN_CLASS_NAME%</mainClass>
                                            </transformer>
                                        </transformers>
                                        <createDependencyReducedPom>false</createDependencyReducedPom>
                                    </configuration>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        String finalPom = pomTemplate
                .replace("%GROUP_ID%", vars.get("GROUP_ID"))
                .replace("%PROJECT_NAME%", vars.get("PROJECT_NAME"))
                .replace("%BOT_VERSION%", vars.get("BOT_VERSION"))
                .replace("%BOT_DISPLAY_NAME%", vars.get("BOT_DISPLAY_NAME"))
                .replace("%BOT_DESCRIPTION%", vars.get("BOT_DESCRIPTION"))
                .replace("%JVM_SOURCE_TARGET%", jvmSourceTarget)
                .replace("%DEPENDENCIES%", dependencies)
                .replace("%MAIN_CLASS_NAME%", vars.get("MAIN_CLASS_NAME"));

        return finalPom;
    }

    /**
     * Generates the content for the main bot class based on the chosen bot type.
     */
    private String generateMainClass(Map<String, String> vars) {
        String mainClassTemplate = "";
        String packageName = vars.get("GROUP_ID");
        String mainClassName = vars.get("MAIN_CLASS_NAME");
        String botDisplayName = vars.get("BOT_DISPLAY_NAME");
        String botTokenSnippet = "System.getenv(\"DISCORD_TOKEN\")"; // Default to env var
        String botPrefixSnippet = "";

        if (vars.get("INCLUDE_PREFIX_ENV").equals("true")) {
            botPrefixSnippet = "private static final String BOT_PREFIX = System.getenv(\"BOT_PREFIX\"); // Or customize default";
        }

        switch (vars.get("BOT_TYPE")) {
            case "JDA (Java Discord API) Basic":
                mainClassTemplate = """
                    package %PACKAGE_NAME%;

                    import net.dv8tion.jda.api.JDABuilder;
                    import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
                    import net.dv8tion.jda.api.hooks.ListenerAdapter;
                    import net.dv8tion.jda.api.requests.GatewayIntent;
                    import org.slf4j.Logger;
                    import org.slf4j.LoggerFactory;

                    import javax.security.auth.login.LoginException;
                    import java.util.EnumSet;
                    import java.util.Objects; // Added for Objects.equals check

                    public class %MAIN_CLASS_NAME% extends ListenerAdapter {

                        private static final Logger logger = LoggerFactory.getLogger(%MAIN_CLASS_NAME%.class);
                        private static final String BOT_TOKEN = %BOT_TOKEN_SNIPPET%;
                        %BOT_PREFIX_SNIPPET%

                        public static void main(String[] args) throws LoginException, InterruptedException {
                            if (BOT_TOKEN == null || BOT_TOKEN.isEmpty() || BOT_TOKEN.equals("PASTE_YOUR_TOKEN_HERE")) {
                                logger.error("DISCORD_TOKEN environment variable is not set or is a placeholder!");
                                logger.error("Please set the 'DISCORD_TOKEN' environment variable with your bot's token.");
                                System.exit(1);
                            }

                            EnumSet<GatewayIntent> intents = EnumSet.of(
                                    GatewayIntent.GUILD_MESSAGES,
                                    GatewayIntent.MESSAGE_CONTENT,
                                    GatewayIntent.GUILD_MEMBERS
                            );

                            JDABuilder.createDefault(BOT_TOKEN, intents)
                                    .addEventListeners(new %MAIN_CLASS_NAME%())
                                    .build()
                                    .awaitReady();

                            logger.info("%BOT_DISPLAY_NAME% is online!");
                        }

                        @Override
                        public void onMessageReceived(MessageReceivedEvent event) {
                            if (event.getAuthor().isBot()) return;

                            String message = event.getMessage().getContentRaw();
                            logger.info("Received message from {}: {}", event.getAuthor().getAsTag(), message);

                            String effectivePrefix = %BOT_PREFIX_CHECK%; // Use default if env var not set

                            if (message.startsWith(effectivePrefix + "ping")) {
                                event.getChannel().sendMessage("Pong!").queue();
                                logger.info("Responded to !ping");
                            } else if (message.startsWith(effectivePrefix + "hello")) {
                                event.getChannel().sendMessage("Hello, " + event.getAuthor().getAsMention() + "!").queue();
                                logger.info("Responded to !hello");
                            }
                        }
                    }
                    """;
                String botPrefixCheckJDA = vars.get("INCLUDE_PREFIX_ENV").equals("true") ?
                        "Objects.requireNonNullElse(BOT_PREFIX, \"!\")" : "\"!\"";
                mainClassTemplate = mainClassTemplate.replace("%BOT_PREFIX_CHECK%", botPrefixCheckJDA);
                break;

            case "Discord4J (Reactive Java Discord API) Basic":
                mainClassTemplate = """
                    package %PACKAGE_NAME%;

                    import discord4j.core.DiscordClientBuilder;
                    import discord4j.core.GatewayDiscordClient;
                    import discord4j.core.event.domain.message.MessageCreateEvent;
                    import discord4j.core.object.entity.Message;
                    import org.slf4j.Logger;
                    import org.slf4j.LoggerFactory;
                    import reactor.core.publisher.Mono;

                    import java.util.Objects; // Added for Objects.requireNonNullElse

                    public class %MAIN_CLASS_NAME% {

                        private static final Logger logger = LoggerFactory.getLogger(%MAIN_CLASS_NAME%.class);
                        private static final String BOT_TOKEN = %BOT_TOKEN_SNIPPET%;
                        %BOT_PREFIX_SNIPPET%

                        public static void main(String[] args) {
                            if (BOT_TOKEN == null || BOT_TOKEN.isEmpty() || BOT_TOKEN.equals("PASTE_YOUR_TOKEN_HERE")) {
                                logger.error("DISCORD_TOKEN environment variable is not set or is a placeholder!");
                                logger.error("Please set the 'DISCORD_TOKEN' environment variable with your bot's token.");
                                System.exit(1);
                            }

                            Mono<Void> login = DiscordClientBuilder.create(BOT_TOKEN)
                                    .build()
                                    .login()
                                    .doOnSuccess(gateway -> logger.info("%BOT_DISPLAY_NAME% is online!"))
                                    .flatMap(gateway -> gateway.on(MessageCreateEvent.class)
                                            .flatMap(event -> processCommand(event, gateway))
                                            .then());

                            login.block();
                        }

                        private static Mono<Void> processCommand(MessageCreateEvent event, GatewayDiscordClient gateway) {
                            Message message = event.getMessage();
                            String content = message.getContent();

                            if (message.getAuthor().map(user -> user.isBot()).orElse(true)) return Mono.empty();

                            logger.info("Received message from {}: {}", message.getAuthor().get().getUsername(), content);

                            String effectivePrefix = %BOT_PREFIX_CHECK%; // Use default if env var not set

                            if (content.startsWith(effectivePrefix + "ping")) {
                                return message.getChannel()
                                        .flatMap(channel -> channel.createMessage("Pong!"))
                                        .doOnNext(m -> logger.info("Responded to !ping"))
                                        .then();
                            } else if (content.startsWith(effectivePrefix + "hello")) {
                                return message.getChannel()
                                        .flatMap(channel -> channel.createMessage("Hello, " + message.getAuthor().get().getMention() + "!"))
                                        .doOnNext(m -> logger.info("Responded to !hello"))
                                        .then();
                            }
                            return Mono.empty();
                        }
                    }
                    """;
                String botPrefixCheckD4J = vars.get("INCLUDE_PREFIX_ENV").equals("true") ?
                        "Objects.requireNonNullElse(BOT_PREFIX, \"!\")" : "\"!\"";
                mainClassTemplate = mainClassTemplate.replace("%BOT_PREFIX_CHECK%", botPrefixCheckD4J);
                break;

            case "Custom (Advanced)":
                mainClassTemplate = """
                    package %PACKAGE_NAME%;

                    import org.slf4j.Logger;
                    import org.slf4j.LoggerFactory;

                    public class %MAIN_CLASS_NAME% {
                        private static final Logger logger = LoggerFactory.getLogger(%MAIN_CLASS_NAME%.class);
                        private static final String BOT_TOKEN = %BOT_TOKEN_SNIPPET%;
                        %BOT_PREFIX_SNIPPET%

                        public static void main(String[] args) {
                            if (BOT_TOKEN == null || BOT_TOKEN.isEmpty() || BOT_TOKEN.equals("PASTE_YOUR_TOKEN_HERE")) {
                                logger.warn("BOT_TOKEN environment variable not set. For custom bots, ensure you load your token securely.");
                            }
                            logger.info("Starting %BOT_DISPLAY_NAME% (Custom Bot)...");
                            // TODO: Implement your custom bot logic here.
                            // The bot will stay alive as long as this main method is running.
                            // If it exits, the bot manager will consider it stopped.
                            while (!Thread.currentThread().isInterrupted()) {
                                try {
                                    Thread.sleep(5000); // Keep alive for demonstration
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    logger.info("%BOT_DISPLAY_NAME% interrupted and shutting down.");
                                }
                            }
                        }
                    }
                    """;
                break;

            default: // Fallback for safety
                mainClassTemplate = """
                    package %PACKAGE_NAME%;

                    import org.slf4j.Logger;
                    import org.slf4j.LoggerFactory;

                    public class %MAIN_CLASS_NAME% {
                        private static final Logger logger = LoggerFactory.getLogger(%MAIN_CLASS_NAME%.class);
                        public static void main(String[] args) {
                            logger.info("Starting %BOT_DISPLAY_NAME% - No specific bot type selected. Please implement your bot logic here.");
                        }
                    }
                    """;
                break;
        }

        // Handle the BOT_TOKEN_SNIPPET based on whether a token was provided in the wizard
        if (vars.get("BOT_TOKEN") != null && !vars.get("BOT_TOKEN").isEmpty()) {
            // If user provided a token, suggest hardcoding for testing (warn against for production)
            botTokenSnippet = "\"" + vars.get("BOT_TOKEN") + "\" /* WARNING: Hardcoding tokens is insecure! Use environment variables in production. */";
        } else if (vars.get("INCLUDE_TOKEN_ENV").equals("true")) {
            // Already set to System.getenv("DISCORD_TOKEN")
        } else {
            // No token provided and no env var requested
            botTokenSnippet = "\"PASTE_YOUR_TOKEN_HERE\"";
        }

        return mainClassTemplate
                .replace("%PACKAGE_NAME%", packageName)
                .replace("%MAIN_CLASS_NAME%", mainClassName)
                .replace("%BOT_DISPLAY_NAME%", botDisplayName)
                .replace("%BOT_TOKEN_SNIPPET%", botTokenSnippet)
                .replace("%BOT_PREFIX_SNIPPET%", botPrefixSnippet);
    }
}