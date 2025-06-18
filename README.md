# Discord Bot Manager

![Discord Bot Manager Screenshot](docs/screenshot.png) A powerful and intuitive desktop application built with JavaFX to streamline the management of your Discord bot projects. This manager allows you to create new bot projects from templates, import existing ones, control their lifecycle (start, stop, restart), view console output, and manage their configurations and environment variables – all from a user-friendly graphical interface.

## ✨ Features

* **Create New Bot Project (Wizard-driven):**
    * Walks you through the creation of a new Maven-based Java Discord bot project.
    * Choose between popular frameworks like **JDA** or **Discord4J**, or a custom template.
    * Configure basic bot details: display name, project name (Artifact ID), Group ID, version, and description.
    * Specify main class name and desired Java version for the project.
    * Optionally provide an initial Discord bot token (stored in a `.env` file for security, not hardcoded).
    * Choose an installation directory; the manager generates the complete project structure with `pom.xml`, main class, and logging configuration.
* **Import Existing Bot Projects:** Easily add pre-existing Discord bot projects to the manager.
* **Bot Lifecycle Management:**
    * **Start/Stop/Restart** individual bots with a single click.
    * **Start All/Stop All** bots for global control.
* **Detailed Bot Information & Configuration:**
    * View essential bot details: name, description, and project path.
    * Edit bot configurations dynamically, including name, version, main file, and description.
    * Manage **Environment Variables** (add, remove, edit) via an interactive table – crucial for secure token handling and flexible configuration.
    * Configure **JVM Arguments** and **Startup Delay** for advanced control over bot processes.
* **Real-time Console Output:** Monitor your bot's live output directly within the application.
    * Includes auto-scrolling and clear log functionalities.
* **Intuitive User Interface:**
    * Clean, modern design built with JavaFX.
    * **Dark Mode** toggle for comfortable viewing in different lighting conditions.
    * Search functionality for quickly finding bots in your list.
* **Extensible Architecture:** Designed for easy expansion with new bot types, features, and integrations.

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

* **Java Development Kit (JDK) 17 or higher:** The application is built with Java 17.
    * [Download JDK](https://www.oracle.com/java/technologies/downloads/) (e.g., Oracle JDK, Amazon Corretto, OpenJDK)
* **Apache Maven (Optional, but recommended for development):** Maven is used for building the project and is required for the generated bot projects.
    * [Download Maven](https://maven.apache.org/download.cgi)
* **An IDE (Integrated Development Environment):** IntelliJ IDEA Community Edition is highly recommended for JavaFX development.
    * [Download IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/)

### Installation and Running

1.  **Clone the Repository:**
    ```bash
    git clone [https://github.com/your-username/DiscordManager.git](https://github.com/your-username/DiscordManager.git) # Replace with your repo URL
    cd DiscordManager
    ```

2.  **Build and Run with Maven (Development):**
    Open the project in your IDE (e.g., IntelliJ IDEA). IntelliJ should automatically detect it as a Maven project.

    Alternatively, from your project's root directory in the terminal:
    ```bash
    mvn clean javafx:run
    ```
    This command will compile the project, download necessary dependencies, and run the application.

3.  **Build an Executable JAR (Production/Distribution):**
    To create a standalone executable `.jar` file (a "fat JAR" containing all dependencies), use the Maven Shade Plugin:
    ```bash
    mvn clean package
    ```
    After a successful build, you will find the executable JAR in the `target/` directory (e.g., `discord-bot-manager-1.0.0.jar`). You can then run it using:
    ```bash
    java -jar target/discord-bot-manager-1.0.0.jar
    ```

## 📋 Usage

### Creating a New Bot Project

1.  Click on `File -> Create New Bot...` in the menu bar, or the `New Bot` button in the sidebar.
2.  Follow the steps in the wizard:
    * **Step 1: Basic Information:** Provide display name, project details (ArtifactId, GroupId), version, and description.
    * **Step 2: Configuration & Dependencies:** Choose a main class name, bot type (JDA, Discord4J, Custom), Java version, and optionally provide an initial Discord bot token. Select if you want `DISCORD_TOKEN` and `BOT_PREFIX` as environment variables.
    * **Step 3: Installation Location:** Select the directory where you want your new bot project to be created.
3.  Click `Finish`. The manager will generate the project files and automatically add the new bot to your list.

### Importing an Existing Bot Project

1.  Click on `File -> Import Existing Bot Project...` in the menu bar, or the `Import Bot` button in the sidebar.
2.  Select the root directory of your existing Discord bot project.
3.  The manager will add it to your list. (Note: For imported bots, you may need to manually populate some configuration fields if the manager cannot auto-detect them from `pom.xml` or other project files).

### Managing Bots

* **Selecting a Bot:** Click on a bot in the `Imported Bots` list on the left sidebar to view its details and controls on the right panel.
* **Controlling a Bot:**
    * Use the `Start Bot`, `Stop Bot`, and `Restart Bot` buttons in the top-right section of the detail panel.
    * Use `Start All` and `Stop All` buttons in the left sidebar for global control.
* **Viewing Console Output:** The "Console Output" tab displays real-time logs from your running bot.
* **Editing Settings:**
    * Switch to the "Bot Settings" tab.
    * Modify general information, environment variables, JVM arguments, and startup delay.
    * Click `Save Changes` to apply modifications.
* **Removing a Bot:** Select a bot and click the `Remove Bot` button. This will remove its configuration from the manager. (Note: It currently does *not* delete the files from your file system.)

### Theming

* Toggle `View -> Dark Mode` to switch between light and dark themes.

## 📁 Project Structure
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/botmanager/
│   │   │       ├── controller/             # JavaFX FXML controllers (MainController, NewBotWizardController, etc.)
│   │   │       ├── service/                # Future backend services (e.g., bot process management)
│   │   │       └── util/                   # Utility classes (BotGenerator)
│   │   │           └── Launcher.java       # Main entry point for JavaFX
│   │   └── resources/
│   │       ├── css/                        # CSS stylesheets (styles.css, dark-mode.css)
│   │       ├── fxml/                       # FXML UI definitions (main.fxml, NewBotWizard.fxml, etc.)
│   │       ├── images/                     # Application icons and placeholder images
│   │       └── bot-config.json (or similar)# Placeholder for future bot configurations
│   └── test/
│       └── java/                           # Unit tests
├── pom.xml                                 # Maven project configuration for the manager application
├── docs/                                   # Documentation (e.g., screenshots)
└── README.md                               # This file

## 🛠️ Technologies Used

* **Java 17+**
* **JavaFX 17+**
* **Maven** (for dependency management and build automation)
* **JDA (Java Discord API)** / **Discord4J** (templates for generated bots)
* **SLF4J + Logback** (for logging in generated bots and the manager)

## 🤝 Contributing

Contributions are welcome! If you have suggestions, bug reports, or want to contribute code, please feel free to:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/your-feature`).
3.  Make your changes and commit them (`git commit -am 'Add new feature'`).
4.  Push to the branch (`git push origin feature/your-feature`).
5.  Create a new Pull Request.

## 📄 License

This project is licensed under the MIT License - see the `LICENSE` file for details.

---
**Disclaimer:** This application is a tool to help manage Discord bots. Ensure you adhere to Discord's Terms of Service and Developer Policy when creating and running bots.
