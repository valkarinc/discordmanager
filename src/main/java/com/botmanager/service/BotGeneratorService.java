package com.botmanager.service;

import com.botmanager.controller.MainController.Bot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BotGeneratorService {


    private Consumer<String> logConsumer;

    public BotGeneratorService(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    private void logProgress(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
        System.out.println(message);
    }

    public CompletableFuture<Bot> generateBotProject(
            String botName, String description, String language, String framework,
            String outputPath, String botToken
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logProgress("Starting bot project generation for: " + botName);

                // 1. Validate inputs (more robust validation needed in real app)
                if (botName == null || botName.trim().isEmpty() ||
                        outputPath == null || outputPath.trim().isEmpty()) {
                    throw new IllegalArgumentException("Bot name and output path cannot be empty.");
                }

                Path projectDir = Paths.get(outputPath, sanitizeFolderName(botName));
                if (Files.exists(projectDir)) {
                    throw new IOException("Directory already exists: " + projectDir);
                }
                Files.createDirectories(projectDir);
                logProgress("Created project directory: " + projectDir.toAbsolutePath());

                String mainFile = "";

                // 2. Select/Generate Template & Populate Files
                switch (language.toLowerCase()) {
                    case "java":
                        if ("jda".equalsIgnoreCase(framework)) {
                            mainFile = generateJavaJDAProject(projectDir, botName, botToken);
                        } else {
                            throw new UnsupportedOperationException("Unsupported Java framework: " + framework);
                        }
                        break;
                    case "python":
                        if ("discord.py".equalsIgnoreCase(framework)) {
                            mainFile = generatePythonDiscordPyProject(projectDir, botName, botToken);
                        } else {
                            throw new UnsupportedOperationException("Unsupported Python framework: " + framework);
                        }
                        break;
                    // can add more languages/frameworks here
                    default:
                        throw new UnsupportedOperationException("Unsupported language: " + language);
                }

                logProgress("Generated project files.");

                // 3. Install Dependencies (Build Process)
                boolean buildSuccess = runBuildProcess(projectDir, language);
                if (!buildSuccess) {
                    throw new IOException("Build process failed for " + botName);
                }
                logProgress("Dependencies installed/project built successfully.");

                // 4. Create Bot object for the manager
                String botId = UUID.randomUUID().toString();
                Bot newBot = new Bot(botId, botName, description, projectDir.toAbsolutePath().toString(),
                        "1.0.0", mainFile, "INFO", false);
                logProgress("Bot project created and ready for management.");

                return newBot;

            } catch (Exception e) {
                logProgress("Error generating bot: " + e.getMessage());
                throw new RuntimeException("Bot generation failed: " + e.getMessage(), e);
            }
        });
    }

    private String sanitizeFolderName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
    }

    //template generation
    private String generateJavaJDAProject(Path projectDir, String botName, String botToken) throws IOException, InterruptedException {
        logProgress("Generating Java JDA project...");
        Path pomFile = projectDir.resolve("pom.xml");
        Path srcDir = projectDir.resolve("src").resolve("main").resolve("java").resolve("com").resolve("yourbot");
        Files.createDirectories(srcDir);

        // Basic pom.xml content (requires Maven for build)
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.yourbot</groupId>
                <artifactId>%s</artifactId>
                <version>1.0.0</version>
                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    <jda.version>5.0.0-beta.23</jda.version> </properties>
                <dependencies>
                    <dependency>
                        <groupId>net.dv8tion</groupId>
                        <artifactId>JDA</artifactId>
                        <version>${jda.version}</version>
                        <exclusions>
                            <exclusion>
                                <groupId>club.minnced</groupId>
                                <artifactId>opus-java</artifactId>
                            </exclusion>
                        </exclusions>
                    </dependency>
                    <dependency>
                        <groupId>ch.qos.logback</groupId>
                        <artifactId>logback-classic</artifactId>
                        <version>1.4.14</version> </dependency>
                </dependencies>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.11.0</version>
                            <configuration>
                                <source>${maven.compiler.source}</source>
                                <target>${maven.compiler.target}</target>
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
                                                <mainClass>com.yourbot.BotMain</mainClass>
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
            """.formatted(sanitizeFolderName(botName));
        Files.writeString(pomFile, pomContent);
        logProgress("pom.xml generated.");

        // Basic BotMain.java content
        String mainClassContent = """
            package com.yourbot;

            import net.dv8tion.jda.api.JDABuilder;
            import net.dv8tion.jda.api.OnlineStatus;
            import net.dv8tion.jda.api.entities.Activity;
            import net.dv8tion.jda.api.requests.GatewayIntent;
            import net.dv8tion.jda.api.utils.ChunkingFilter;
            import net.dv8tion.jda.api.utils.MemberCachePolicy;
            import net.dv8tion.jda.api.hooks.ListenerAdapter;
            import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

            import java.util.EnumSet;

            public class BotMain extends ListenerAdapter {
                public static void main(String[] args) throws Exception {
                    String token = System.getenv("DISCORD_TOKEN");
                    if (token == null || token.isEmpty()) {
                        token = "%s"; // Placeholder if not provided as env var
                        if (token.isEmpty()) {
                            System.err.println("ERROR: Discord Bot Token not found! Set DISCORD_TOKEN environment variable or replace placeholder in code.");
                            return;
                        }
                    }

                    JDABuilder builder = JDABuilder.createDefault(token);

                    // Set bot status and activity
                    builder.setStatus(OnlineStatus.ONLINE);
                    builder.setActivity(Activity.playing("Developing Discord Bots"));

                    // Configure intents (REQUIRED for many features)
                    builder.enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT, // REQUIRED for message content from v5
                        GatewayIntent.GUILD_MEMBERS // For member caching, enable if needed for commands related to members
                    );

                    // Recommended cache policies for performance
                    builder.setMemberCachePolicy(MemberCachePolicy.ALL); // Cache all members
                    builder.setChunkingFilter(ChunkingFilter.ALL); // Request all members during startup

                    // Add event listener
                    builder.addEventListeners(new BotMain());

                    // Build and login
                    builder.build();
                    System.out.println("JDA Bot '%s' is starting...");
                }

                @Override
                public void onMessageReceived(MessageReceivedEvent event) {
                    if (event.getAuthor().isBot()) return; // Ignore bots

                    String message = event.getMessage().getContentRaw();
                    if (message.equalsIgnoreCase("!ping")) {
                        event.getChannel().sendMessage("Pong!").queue();
                    } else if (message.equalsIgnoreCase("!hello")) {
                        event.getChannel().sendMessage("Hello, %s!".formatted(event.getAuthor().getName())).queue();
                    }
                }
            }
            """.formatted(botToken, botName); // Pass token and bot name to template
        Files.writeString(srcDir.resolve("BotMain.java"), mainClassContent);
        logProgress("BotMain.java generated.");

        return "com.yourbot.BotMain";
    }

    private String generatePythonDiscordPyProject(Path projectDir, String botName, String botToken) throws IOException, InterruptedException {
        logProgress("Generating Python discord.py project...");
        Path mainPy = projectDir.resolve("bot.py");
        Path reqsTxt = projectDir.resolve("requirements.txt");

        String mainPyContent = """
            import discord
            import os

            # Get bot token from environment variable or use placeholder
            TOKEN = os.getenv('DISCORD_TOKEN')
            if TOKEN is None:
                TOKEN = "%s" # Placeholder if not provided as env var
                if not TOKEN:
                    print("ERROR: Discord Bot Token not found! Set DISCORD_TOKEN environment variable or replace placeholder in code.")
                    exit()

            # Define intents for your bot (REQUIRED for many features)
            # discord.Intents.default() gives you a good set for most basic bots.
            # If you need specific events (like member joining/leaving, message content), enable them explicitly.
            intents = discord.Intents.default()
            intents.message_content = True  # Required for accessing message.content
            intents.members = True          # Required for member-related events/caching

            # Initialize the bot client
            client = discord.Client(intents=intents)

            @client.event
            async def on_ready():
                print(f'{client.user} has connected to Discord!')
                # You can set the bot's status and activity here
                await client.change_presence(activity=discord.Game(name="Managing Bots"))

            @client.event
            async def on_message(message):
                if message.author == client.user:
                    return # Ignore messages from self

                if message.content.startswith('!ping'):
                    await message.channel.send('Pong!')
                elif message.content.startswith('!hello'):
                    await message.channel.send(f'Hello, {message.author.display_name}!')

            # Run the bot
            client.run(TOKEN)
            """.formatted(botToken);
        Files.writeString(mainPy, mainPyContent);
        logProgress("bot.py generated.");

        String reqsContent = "discord.py\n"; // Only dependency
        Files.writeString(reqsTxt, reqsContent);
        logProgress("requirements.txt generated.");

        return "bot.py";
    }

    private boolean runBuildProcess(Path projectDir, String language) throws IOException, InterruptedException {
        ProcessBuilder processBuilder;
        switch (language.toLowerCase()) {
            case "java":

                logProgress("Running Maven build (mvn clean install)...");
                processBuilder = new ProcessBuilder("mvn", "clean", "install");
                break;
            case "python":
                logProgress("Running pip install -r requirements.txt...");
                processBuilder = new ProcessBuilder("pip", "install", "-r", "requirements.txt");
                break;
            case "nodejs":
                logProgress("Running npm install...");
                processBuilder = new ProcessBuilder("npm", "install");
                break;
            default:
                throw new UnsupportedOperationException("No build process defined for language: " + language);
        }

        processBuilder.directory(projectDir.toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logProgress("[BUILD] " + line);
            }
        }

        int exitCode = process.waitFor();
        logProgress("Build process exited with code: " + exitCode);
        return exitCode == 0;
    }
}