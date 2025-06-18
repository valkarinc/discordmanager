# Discord Bot Manager: Simplified

The Discord Bot Manager is a powerful and user-friendly desktop application built with JavaFX to help you manage your Discord bot projects effortlessly.

## ✨ Features

* **Create New Bots:** A guided wizard helps you set up new Maven-based Java Discord bot projects.

    * Choose from templates like **JDA** or **Discord4J**.

    * Configure project details, Java version, and securely store your bot token.

* **Import Existing Bots:** Easily add your current bot projects to the manager.

* **Control Bots:**

    * Start, stop, or restart individual bots.

    * Start or stop all bots with a single click.

* **Manage Settings:**

    * View and edit bot details (name, description, path).

    * Securely manage **Environment Variables** for tokens and configurations.

    * Adjust **JVM Arguments** and **Startup Delay**.

* **Monitor Output:** See your bot's live console output directly within the application.

* **Intuitive UI:** Clean JavaFX design with a **Dark Mode** option and search functionality.

## 🚀 Getting Started

### Prerequisites

Make sure you have:

* **Java Development Kit (JDK) 17+**

* **Apache Maven** (optional, but recommended for development)

### Installation and Running

1. **Clone the Repository:**


git clone https://github.com/valkarinc/discordmanager.git
cd DiscordManager


2. **Build and Run (Development):**


mvn clean javafx:run


3. **Build Executable JAR (Production):**


mvn clean package
java -jar target/discord-bot-manager-1.0.0.jar


## 📋 Usage

* **Create a New Bot:** Go to `File -> Create New Bot...` and follow the wizard.

* **Import an Existing Bot:** Go to `File -> Import Existing Bot Project...` and select your bot's root directory.

* **Manage Bots:** Select a bot from the left sidebar to view its details and control its lifecycle (start, stop, restart) on the right panel.

* **Edit Settings:** Switch to the "Bot Settings" tab to modify configurations and environment variables. Remember to `Save Changes`.

* **Remove a Bot:** Select a bot and click `Remove Bot`. (Note: This only removes it from the manager, not your file system.)

* **Toggle Dark Mode:** `View -> Dark Mode`.

## 🛠️ Technologies Used

* Java 17+

* JavaFX 17+

* Maven

* JDA / Discord4J

* SLF4J + Logback

## 🤝 Contributing

Contributions are welcome! Please fork the repository, create a branch, commit your changes, and open a Pull Request.

## 📄 License

This project is licensed under the MIT License.

**Disclaimer:** Please ensure compliance with Discord's Terms of Service and Developer Policy when using this application
