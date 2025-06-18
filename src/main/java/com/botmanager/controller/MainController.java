package com.botmanager.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell; // For editable cells
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane; // Added for rootLayout
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.UUID; // To generate unique IDs for bots (placeholder)
import java.util.function.Predicate;

/**
 * Main Controller for the Discord Bot Manager application.
 * Handles all UI interactions and coordinates with backend services.
 *
 * @author Your Name (Upgraded by $10k Dev)
 * @version 1.1.0
 */
public class MainController implements Initializable {

    // --- FXML Injections (matched with main.fxml) ---

    // Root Layout
    @FXML private BorderPane rootLayout; // Added fx:id="rootLayout" in FXML

    // Menu Items
    @FXML private MenuBar mainMenuBar; // Added fx:id="mainMenuBar" in FXML
    @FXML private MenuItem importBotMenuItem;
    @FXML private MenuItem exitMenuItem;
    @FXML private MenuItem refreshMenuItem;
    @FXML private CheckMenuItem darkModeMenuItem;
    @FXML private MenuItem aboutMenuItem;

    // Sidebar Components
    @FXML private Button addBotButton;
    @FXML private TextField botSearchField; // New TextField
    @FXML private ListView<Bot> botListView; // Changed to ListView<Bot>
    @FXML private Button startAllButton;
    @FXML private Button stopAllButton;

    // Bot Info & Control Components (Right Panel)
    @FXML private ImageView botAvatar; // New ImageView
    @FXML private Label botNameLabel;
    @FXML private Label botDescriptionLabel;
    @FXML private Label botPathLabel;

    @FXML private MenuItem createNewBotMenuItem;
    @FXML private Button createNewBotButton;

    // Bot Control Buttons (Toolbar)
    @FXML private Button startBotButton;
    @FXML private Button stopBotButton;
    @FXML private Button restartBotButton;
    @FXML private Button editBotButton;   // New button
    @FXML private Button removeBotButton; // New button

    // Tab Pane
    @FXML private TabPane botDetailsTabPane; // Renamed from configTabPane
    @FXML private Tab activityMetricsTab; // New Tab

    // Console Output Tab
    @FXML private TextArea consoleOutputArea;
    @FXML private Button clearLogButton;
    @FXML private CheckBox autoScrollCheckBox;

    // Bot Settings Tab
    @FXML private TextField configBotNameField;
    @FXML private TextField configBotVersionField;
    @FXML private TextField configMainFileField;
    @FXML private TextField configProjectPathField; // New TextField
    @FXML private TextArea configDescriptionArea; // New TextArea

    @FXML private TableView<EnvVariable> envVarsTable;
    @FXML private TableColumn<EnvVariable, String> envKeyColumn;
    @FXML private TableColumn<EnvVariable, String> envValueColumn;
    @FXML private Button addEnvVarButton;
    @FXML private Button removeEnvVarButton;
    @FXML private Button saveConfigButton;

    @FXML private TextField jvmArgsField;    // New TextField
    @FXML private TextField startupDelayField; // New TextField

    // Status Bar
    @FXML private Label statusLabel;
    @FXML private Label runningBotsLabel;

    // --- Internal State ---
    private Stage primaryStage;
    private ObservableList<Bot> masterBotList = FXCollections.observableArrayList();
    private FilteredList<Bot> filteredBotList;
    private Bot currentlySelectedBot;
    private NewBotWizardController botWizardController;
    private Stage botWizardStage;

    // --- Initialization ---
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing Discord Bot Manager UI...");

        // Setup Bot List View
        setupBotListView();

        // Setup Environment Variables Table
        setupEnvVarsTable();

        // Setup Search Functionality
        setupBotSearch();

        // Setup bot list selection listener
        setupBotListListener();

        // Load default bot avatar
        loadDefaultBotAvatar();

        // Initialize UI state (disable detail pane elements)
        updateUIState(null); // No bot selected initially

        // Add test data for UI verification
        //addTestData();

        if (createNewBotMenuItem != null) {
            createNewBotMenuItem.setOnAction(this::handleCreateNewBot);
        }

        if (createNewBotButton != null) {
            createNewBotButton.setOnAction(this::handleCreateNewBot);
        }

        System.out.println("Discord Bot Manager UI initialized.");
    }

    /**
     * Sets up the ListView for displaying bots.
     */
    private void setupBotListView() {
        // Set how bots are displayed in the ListView (e.g., by their name)
        botListView.setCellFactory(lv -> new ListCell<Bot>() {
            @Override
            protected void updateItem(Bot bot, boolean empty) {
                super.updateItem(bot, empty);
                setText(empty ? null : bot.getName());
                // You could add graphics here too:
                // if (bot != null && bot.getIcon() != null) {
                //     ImageView iconView = new ImageView(new Image(bot.getIcon()));
                //     iconView.setFitHeight(24);
                //     iconView.setFitWidth(24);
                //     setGraphic(iconView);
                // } else {
                //     setGraphic(null);
                // }
            }
        });

        filteredBotList = new FilteredList<>(masterBotList, p -> true); // Initially show all data
        SortedList<Bot> sortedData = new SortedList<>(filteredBotList);
        botListView.setItems(sortedData);
    }

    /**
     * Sets up the TableView for environment variables, including making cells editable.
     */
    private void setupEnvVarsTable() {
        envKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        envValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // Make columns editable
        envKeyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        envKeyColumn.setOnEditCommit(event -> {
            EnvVariable var = event.getRowValue();
            if (var != null) var.setKey(event.getNewValue());
        });

        envValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        envValueColumn.setOnEditCommit(event -> {
            EnvVariable var = event.getRowValue();
            if (var != null) var.setValue(event.getNewValue());
        });

        envVarsTable.setEditable(true); // Enable editing for the entire table
    }

    /**
     * Sets up the search functionality for the bot list.
     */
    private void setupBotSearch() {
        botSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredBotList.setPredicate(bot -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Display all bots if search field is empty
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return bot.getName().toLowerCase().contains(lowerCaseFilter) ||
                        bot.getDescription().toLowerCase().contains(lowerCaseFilter) ||
                        bot.getProjectPath().toLowerCase().contains(lowerCaseFilter);
            });
        });
    }

    /**
     * Sets up listener for bot list selection changes.
     */
    private void setupBotListListener() {
        botListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    currentlySelectedBot = newValue;
                    updateUIState(newValue);
                    if (newValue != null) {
                        loadBotDetails(newValue);
                    } else {
                        clearBotDetails();
                    }
                }
        );
    }

    /**
     * Loads a default bot avatar image.
     */
    private void loadDefaultBotAvatar() {
        // Add this null check
        if (botAvatar == null) {
            System.err.println("Error: botAvatar ImageView is null during loadDefaultBotAvatar call. Check FXML fx:id and injection.");
            return; // Exit to prevent NPE
        }

        try (InputStream iconStream = getClass().getResourceAsStream("/images/bot-icon.png")) {
            if (iconStream != null) {
                botAvatar.setImage(new Image(iconStream));
            } else {
                System.err.println("Default bot avatar not found at /images/bot-icon.png (or default_bot_avatar.png)");
            }
        } catch (Exception e) {
            System.err.println("Error loading default bot avatar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load details for the selected bot into the detail pane.
     */
    private void loadBotDetails(Bot bot) {
        if (bot == null) {
            clearBotDetails();
            return;
        }

        // Update Bot Header
        botNameLabel.setText(bot.getName());
        botDescriptionLabel.setText(bot.getDescription());
        botPathLabel.setText(bot.getProjectPath());
        // TODO: Update botAvatar.setImage(new Image(bot.getAvatarUrl())) if you have specific avatars

        // Update Configuration Fields
        configBotNameField.setText(bot.getName());
        configBotVersionField.setText(bot.getVersion());
        configMainFileField.setText(bot.getMainFile());
        configProjectPathField.setText(bot.getProjectPath());
        configDescriptionArea.setText(bot.getDescription());
        jvmArgsField.setText(bot.getJvmArgs());
        startupDelayField.setText(String.valueOf(bot.getStartupDelayMs()));

        // Update Environment Variables Table
        envVarsTable.setItems(FXCollections.observableArrayList(bot.getEnvVariables()));

        // Update console (for demo, just show some log)
        consoleOutputArea.appendText("[INFO] Loaded details for " + bot.getName() + ".\n");

        statusLabel.setText("Bot details loaded: " + bot.getName());
    }

    /**
     * Clears bot details when no bot is selected.
     */
    private void clearBotDetails() {
        botNameLabel.setText("Select a Bot");
        botDescriptionLabel.setText("No bot selected. Please choose a bot from the left sidebar to view its details and controls.");
        botPathLabel.setText("");
        loadDefaultBotAvatar(); // Reset avatar

        // Clear all fields
        configBotNameField.clear();
        configBotVersionField.clear();
        configMainFileField.clear();
        configProjectPathField.clear();
        configDescriptionArea.clear();
        envVarsTable.getItems().clear();
        jvmArgsField.clear();
        startupDelayField.clear();
        consoleOutputArea.clear(); // Clear console too

        statusLabel.setText("No bot selected");
    }

    /**
     * Updates the UI state (enable/disable controls) based on whether a bot is selected.
     */
    private void updateUIState(Bot selectedBot) {
        boolean isBotSelected = (selectedBot != null);

        // Control Buttons
        startBotButton.setDisable(!isBotSelected || selectedBot.isRunning());
        stopBotButton.setDisable(!isBotSelected || !selectedBot.isRunning());
        restartBotButton.setDisable(!isBotSelected);
        editBotButton.setDisable(!isBotSelected);
        removeBotButton.setDisable(!isBotSelected);

        // Configuration/Settings fields (editable only when bot selected and not running)
        boolean enableConfigEditing = isBotSelected && !selectedBot.isRunning(); // Or allow editing and require save
        configBotNameField.setEditable(enableConfigEditing);
        configBotVersionField.setEditable(enableConfigEditing);
        configMainFileField.setEditable(enableConfigEditing);
        configDescriptionArea.setEditable(enableConfigEditing);
        jvmArgsField.setEditable(enableConfigEditing);
        startupDelayField.setEditable(enableConfigEditing);
        envVarsTable.setEditable(enableConfigEditing); // Allow adding/removing always, but editing only when not running
        addEnvVarButton.setDisable(!isBotSelected);
        removeEnvVarButton.setDisable(!isBotSelected);
        saveConfigButton.setDisable(!isBotSelected);

        // TabPane visibility (might be controlled by selected bot)
        // For now, always visible, but tabs can be disabled or hidden based on context
        botDetailsTabPane.getSelectionModel().selectFirst(); // Default to console tab
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Adds sample bot data for demonstration purposes.
     */
    private void addTestData() {
        // Create sample bots
        Bot bot1 = new Bot("bot-1", "Sample Bot 1", "This is a basic Discord bot for general use.", "/users/chaz/bots/sample-bot-1", "1.0.0", "com.example.Main", "INFO", false);
        bot1.getEnvVariables().add(new EnvVariable("DISCORD_TOKEN", "YOUR_TOKEN_1"));
        bot1.getEnvVariables().add(new EnvVariable("PREFIX", "!"));

        Bot bot2 = new Bot("bot-2", "Discord Music Bot", "A feature-rich bot for playing music in voice channels.", "/users/chaz/bots/music-bot", "2.1.0", "org.musicbot.App", "DEBUG", true);
        bot2.setRunning(true); // Simulate running state
        bot2.getEnvVariables().add(new EnvVariable("SPOTIFY_API_KEY", "ABC123XYZ"));
        bot2.getEnvVariables().add(new EnvVariable("YOUTUBE_API_KEY", "DEF456UVW"));

        Bot bot3 = new Bot("bot-3", "Moderation Bot", "Automates moderation tasks and keeps your server safe.", "/users/chaz/bots/moderation-bot", "3.0.5", "net.modbot.Launcher", "WARN", false);

        masterBotList.addAll(bot1, bot2, bot3);

        // Select the first item to show details on startup
        if (!masterBotList.isEmpty()) {
            botListView.getSelectionModel().selectFirst();
        }

        // Update running bots count
        long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
        runningBotsLabel.setText("Active Bots: " + runningCount);
    }

    // --- Event Handlers ---

    @FXML
    private void handleImportBot() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Bot Project Folder");

        File selectedDirectory = directoryChooser.showDialog(getStage());

        if (selectedDirectory != null) {
            System.out.println("Selected directory for import: " + selectedDirectory.getAbsolutePath());
            statusLabel.setText("Importing bot from: " + selectedDirectory.getName() + "...");

            // TODO: In a real app, you'd parse the project (e.g., pom.xml, package.json)
            // to extract bot name, main file, etc.
            String botId = UUID.randomUUID().toString(); // Generate unique ID
            String botName = "New Imported Bot (" + selectedDirectory.getName() + ")";
            String botPath = selectedDirectory.getAbsolutePath();
            String botDescription = "Description for " + botName;
            String botVersion = "1.0.0";
            String mainFile = "Main.java"; // Placeholder

            Bot newBot = new Bot(botId, botName, botDescription, botPath, botVersion, mainFile, "INFO", false);
            masterBotList.add(newBot);
            botListView.getSelectionModel().select(newBot); // Select the newly imported bot

            showInfoAlert("Import Successful", "Bot project imported: " + botName);
            statusLabel.setText("Bot imported: " + botName);
        }
    }

    @FXML
    private void handleExit() {
        System.out.println("Exiting application...");
        // TODO: Ensure all running bots are stopped gracefully before exit
        System.exit(0);
    }

    @FXML
    private void handleRefresh() {
        System.out.println("Refreshing bot list and statuses...");
        statusLabel.setText("Refreshing...");
        // TODO: Implement actual refresh logic (re-scan directories, update running states)
        long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
        runningBotsLabel.setText("Active Bots: " + runningCount);
        statusLabel.setText("Refreshed data.");
    }

    @FXML
    private void handleToggleDarkMode() {
        boolean isDarkMode = darkModeMenuItem.isSelected();
        System.out.println("Dark mode: " + (isDarkMode ? "ON" : "OFF"));
        if (rootLayout != null) {
            ObservableList<String> stylesheets = rootLayout.getStylesheets();
            String darkModeCss = getClass().getResource("/css/dark-mode.css").toExternalForm();
            if (isDarkMode) {
                if (!stylesheets.contains(darkModeCss)) {
                    stylesheets.add(darkModeCss);
                }
            } else {
                stylesheets.remove(darkModeCss);
            }
        }
        statusLabel.setText("Dark mode: " + (isDarkMode ? "Enabled" : "Disabled"));
    }

    @FXML
    private void handleAbout() {
        showInfoAlert("About Discord Bot Manager",
                "Discord Bot Manager v1.1.0\n\n" +
                        "A powerful JavaFX application for seamless management of your Discord bots.\n" +
                        "Built with Java " + System.getProperty("java.version") + " and JavaFX.\n\n" +
                        "© 2025 Valkarin Studios All Rights Reserved.");
    }

    @FXML
    private void handleStartBot() {
        if (currentlySelectedBot != null) {
            if (currentlySelectedBot.isRunning()) {
                showAlert("Bot Already Running", currentlySelectedBot.getName() + " is already running.");
                return;
            }

            System.out.println("Attempting to start bot: " + currentlySelectedBot.getName());
            consoleOutputArea.appendText("[INFO] Starting " + currentlySelectedBot.getName() + "...\n");

            try {
                // 1. Determine the path to the bot's executable JAR file.
                //    Assumes Maven builds to 'target/' and JAR is named 'BotName-1.0-SNAPSHOT.jar'.
                //    YOU MIGHT NEED TO ADJUST THE JAR NAME IF YOUR BOT'S BUILD IS DIFFERENT.
                Path botJarPath = Paths.get(currentlySelectedBot.getProjectPath(), "target",
                        currentlySelectedBot.getName() + "-1.0-SNAPSHOT.jar");

                // Check if the JAR file actually exists
                if (!botJarPath.toFile().exists()) {
                    showAlert("JAR Not Found", "Bot JAR file not found at: " + botJarPath +
                            "\nPlease ensure the bot project is built (e.g., run 'mvn clean install' in its folder).");
                    currentlySelectedBot.setRunning(false); // Ensure status is not running
                    botListView.refresh();
                    updateUIState(currentlySelectedBot);
                    return;
                }

                // 2. Create a ProcessBuilder to run the Java command.
                ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", botJarPath.toString());
                // Set the working directory for the bot process to its project folder
                processBuilder.directory(new File(currentlySelectedBot.getProjectPath()));
                // Redirect error stream to output stream (optional, but often helpful)
                processBuilder.redirectErrorStream(true);

                // 3. Start the process.
                Process process = processBuilder.start();

                // 4. Store the Process object in the Bot object.
                currentlySelectedBot.setBotProcess(process); // You need to add this method to your Bot class
                currentlySelectedBot.setRunning(true);

                // 5. Read output from the bot process in a separate thread.
                //    This prevents the UI from freezing.
                new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String outputLine = line; // Need final for lambda
                            // Update UI on JavaFX Application Thread
                            Platform.runLater(() -> consoleOutputArea.appendText("[BOT] " + currentlySelectedBot.getName() + ": " + outputLine + "\n"));
                        }
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            consoleOutputArea.appendText("[ERROR] Reading output for " + currentlySelectedBot.getName() + ": " + e.getMessage() + "\n");
                        });
                    } finally {
                        // This block runs when the bot process finishes (crashes, stops, etc.)
                        Platform.runLater(() -> {
                            currentlySelectedBot.setRunning(false);
                            currentlySelectedBot.setBotProcess(null); // Clear the process reference
                            consoleOutputArea.appendText("[INFO] " + currentlySelectedBot.getName() + " process finished.\n");
                            updateUIState(currentlySelectedBot); // Update UI after bot stops
                        });
                    }
                }).start();


                consoleOutputArea.appendText("[INFO] " + currentlySelectedBot.getName() + " started successfully!\n");
                statusLabel.setText("Bot started: " + currentlySelectedBot.getName());

            } catch (IOException e) {
                showAlert("Error Starting Bot", "Could not start bot: " + e.getMessage());
                currentlySelectedBot.setRunning(false); // Set status back to stopped on error
                statusLabel.setText("Failed to start bot: " + currentlySelectedBot.getName());
                e.printStackTrace();
            }

            // Recalculate running bots
            long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
            runningBotsLabel.setText("Active Bots: " + runningCount);

            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
            updateUIState(currentlySelectedBot); // Update button states (disable Start, enable Stop)
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot from the list to start.");
        }
    }

    @FXML
    private void handleStopBot() {
        if (currentlySelectedBot != null) {
            System.out.println("Attempting to stop bot: " + currentlySelectedBot.getName());
            consoleOutputArea.appendText("[INFO] Stopping " + currentlySelectedBot.getName() + "...\n");
            // TODO: Implement actual bot shutdown command execution
            // Simulate shutdown success/failure
            boolean stopped = Math.random() > 0.2; // Simulate 80% success rate
            if (stopped) {
                currentlySelectedBot.setRunning(false);
                startBotButton.setDisable(false);
                stopBotButton.setDisable(true);
                consoleOutputArea.appendText("[INFO] " + currentlySelectedBot.getName() + " stopped.\n");
                statusLabel.setText("Bot stopped: " + currentlySelectedBot.getName());
            } else {
                consoleOutputArea.appendText("[ERROR] Failed to stop " + currentlySelectedBot.getName() + ".\n");
                statusLabel.setText("Failed to stop bot: " + currentlySelectedBot.getName());
            }

            // Recalculate running bots
            long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
            runningBotsLabel.setText("Active Bots: " + runningCount);

            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
            updateUIState(currentlySelectedBot); // Update button states
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot from the list to stop.");
        }
    }

    @FXML
    private void handleRestartBot() {
        if (currentlySelectedBot != null) {
            System.out.println("Attempting to restart bot: " + currentlySelectedBot.getName());
            consoleOutputArea.appendText("[INFO] Restarting " + currentlySelectedBot.getName() + "...\n");
            // TODO: Implement actual restart logic (stop, then start)
            handleStopBot(); // Simulate stop
            handleStartBot(); // Simulate start
            statusLabel.setText("Bot restarted: " + currentlySelectedBot.getName());
            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot from the list to restart.");
        }
    }

    @FXML
    private void handleEditBot() {
        if (currentlySelectedBot != null) {
            System.out.println("Editing settings for bot: " + currentlySelectedBot.getName());
            // Switch to the "Bot Settings" tab
            if (botDetailsTabPane != null && !botDetailsTabPane.getTabs().isEmpty()) {
                botDetailsTabPane.getSelectionModel().select(
                        botDetailsTabPane.getTabs().stream()
                                .filter(tab -> "Bot Settings".equals(tab.getText()))
                                .findFirst()
                                .orElse(null)
                );
            }
            statusLabel.setText("Editing settings for " + currentlySelectedBot.getName());
            // The `updateUIState` method should handle enabling/disabling fields
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot to edit its settings.");
        }
    }

    @FXML
    private void handleRemoveBot() {
        if (currentlySelectedBot != null) {
            // Capture the currently selected bot in a final local variable
            // This ensures its value is preserved even if 'this.currentlySelectedBot' changes later
            final Bot botToRemove = currentlySelectedBot;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Bot Removal");
            // Use the local variable here
            alert.setHeaderText("Remove " + botToRemove.getName() + "?");
            alert.setContentText("Are you sure you want to remove this bot? This action cannot be undone and will delete its configuration.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    System.out.println("Removing bot: " + botToRemove.getName()); // Use the local variable here too
                    // TODO: Implement actual deletion logic (delete config files, stop if running)

                    // Ensure the bot is stopped if it's running before removal
                    if (botToRemove.isRunning()) {
                        // This is where you'd call your backend stop logic for botToRemove
                        // For now, simulate:
                        consoleOutputArea.appendText("[INFO] Stopping " + botToRemove.getName() + " before removal...\n");
                        botToRemove.setRunning(false); // Update model
                        // You might need to wait for it to actually stop
                    }

                    masterBotList.remove(botToRemove); // Remove from your master list

                    // Only clear selection and details if the bot being removed *was* the currently selected one
                    // This prevents issues if selection changed during alert interaction
                    if (currentlySelectedBot == botToRemove) {
                        currentlySelectedBot = null; // Clear selection in controller state
                        botListView.getSelectionModel().clearSelection(); // Visually clear selection
                        clearBotDetails(); // Clear detail pane
                    }

                    updateUIState(null); // Reset UI to no-bot-selected state, ensuring buttons are correctly disabled

                    // Update running bots count
                    long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
                    runningBotsLabel.setText("Active Bots: " + runningCount);

                    showInfoAlert("Bot Removed", botToRemove.getName() + " has been successfully removed.");
                    statusLabel.setText("Bot removed.");
                }
            });
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot from the list to remove.");
        }
    }

    @FXML
    private void handleNewBotCreation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NewBotWizard.fxml"));
            AnchorPane wizardPane = loader.load();
            NewBotWizardController wizardController = loader.getController();

            // Pass the MainController instance to the wizard controller
            wizardController.setMainController(this); // Allows wizard to call back to MainController

            botWizardStage = new Stage();
            botWizardStage.setTitle("Create New Bot Project");
            botWizardStage.initModality(Modality.APPLICATION_MODAL); // Block main window
            botWizardStage.setScene(new Scene(wizardPane));
            botWizardStage.showAndWait();

        } catch (IOException e) {
            showErrorAlert("Error Loading Wizard", "Could not load the bot creation wizard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addBotToManager(Bot newBot) {
        if (newBot != null) {
            masterBotList.add(newBot);
            botListView.getSelectionModel().select(newBot);
            showInfoAlert("Bot Created!", "New bot '" + newBot.getName() + "' has been added to the manager.");
            long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
            runningBotsLabel.setText("Active Bots: " + runningCount);
        }
    }

    void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleStartAll() {
        System.out.println("Starting all bots...");
        consoleOutputArea.appendText("[INFO] Attempting to start all configured bots...\n");
        long startedCount = masterBotList.stream()
                .filter(bot -> !bot.isRunning())
                .peek(bot -> {
                    // Simulate starting each bot
                    boolean started = Math.random() > 0.3;
                    if (started) {
                        bot.setRunning(true);
                        consoleOutputArea.appendText("[INFO] Started: " + bot.getName() + "\n");
                    } else {
                        consoleOutputArea.appendText("[ERROR] Failed to start: " + bot.getName() + "\n");
                    }
                })
                .filter(Bot::isRunning)
                .count();

        long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
        runningBotsLabel.setText("Active Bots: " + runningCount);
        statusLabel.setText("Started " + startedCount + " bots.");
        updateUIState(currentlySelectedBot); // Update individual button states
        if (autoScrollCheckBox.isSelected()) consoleOutputArea.setScrollTop(Double.MAX_VALUE);
    }

    @FXML
    private void handleStopAll() {
        System.out.println("Stopping all bots...");
        consoleOutputArea.appendText("[INFO] Attempting to stop all running bots...\n");
        long stoppedCount = masterBotList.stream()
                .filter(Bot::isRunning)
                .peek(bot -> {
                    // Simulate stopping each bot
                    boolean stopped = Math.random() > 0.2;
                    if (stopped) {
                        bot.setRunning(false);
                        consoleOutputArea.appendText("[INFO] Stopped: " + bot.getName() + "\n");
                    } else {
                        consoleOutputArea.appendText("[ERROR] Failed to stop: " + bot.getName() + "\n");
                    }
                })
                .filter(bot -> !bot.isRunning()) // Count those that are now not running
                .count();

        long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
        runningBotsLabel.setText("Active Bots: " + runningCount);
        statusLabel.setText("Stopped " + stoppedCount + " bots.");
        updateUIState(currentlySelectedBot); // Update individual button states
        if (autoScrollCheckBox.isSelected()) consoleOutputArea.setScrollTop(Double.MAX_VALUE);
    }

    @FXML
    private void handleClearLog() {
        consoleOutputArea.clear();
        statusLabel.setText("Console cleared");
    }

    @FXML
    private void handleAddEnvVar() {
        if (currentlySelectedBot != null) {
            EnvVariable newVar = new EnvVariable("NEW_KEY", "new_value");
            currentlySelectedBot.getEnvVariables().add(newVar);
            envVarsTable.setItems(FXCollections.observableArrayList(currentlySelectedBot.getEnvVariables())); // Refresh TableView
            envVarsTable.getSelectionModel().select(newVar); // Select the new item
            envVarsTable.scrollTo(newVar); // Scroll to the new item
            envVarsTable.edit(envVarsTable.getItems().size() - 1, envKeyColumn); // Start editing
            statusLabel.setText("Environment variable added");
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot to add environment variables.");
        }
    }

    @FXML
    private void handleRemoveEnvVar() {
        if (currentlySelectedBot != null) {
            EnvVariable selected = envVarsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentlySelectedBot.getEnvVariables().remove(selected);
                envVarsTable.setItems(FXCollections.observableArrayList(currentlySelectedBot.getEnvVariables())); // Refresh TableView
                statusLabel.setText("Environment variable removed");
            } else {
                showWarningAlert("No Variable Selected", "Please select an environment variable to remove.");
            }
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot first.");
        }
    }

    @FXML
    private void handleSaveConfig() {
        if (currentlySelectedBot != null) {
            System.out.println("Saving configuration for: " + currentlySelectedBot.getName());

            // Update bot object from UI fields
            currentlySelectedBot.setName(configBotNameField.getText());
            currentlySelectedBot.setVersion(configBotVersionField.getText());
            currentlySelectedBot.setMainFile(configMainFileField.getText());
            currentlySelectedBot.setDescription(configDescriptionArea.getText());
            currentlySelectedBot.setJvmArgs(jvmArgsField.getText());
            try {
                currentlySelectedBot.setStartupDelayMs(Integer.parseInt(startupDelayField.getText()));
            } catch (NumberFormatException e) {
                showWarningAlert("Invalid Input", "Startup delay must be a valid number.");
                startupDelayField.setText(String.valueOf(currentlySelectedBot.getStartupDelayMs())); // Revert
                return;
            }
            // envVarsTable updates the Bot's list directly due to its ObservableList nature
            // and the `setOnEditCommit` handlers.

            // TODO: Persist the updated bot object to disk (e.g., JSON file)
            masterBotList.set(masterBotList.indexOf(currentlySelectedBot), currentlySelectedBot); // Update in master list to reflect changes in ListView

            // Force ListView to re-render selected item to show updated name, etc.
            botListView.refresh();
            // Or if names can change:
            // int selectedIndex = botListView.getSelectionModel().getSelectedIndex();
            // botListView.getItems().set(selectedIndex, currentlySelectedBot); // This will update the name displayed
            // botListView.getSelectionModel().select(selectedIndex);

            statusLabel.setText("Configuration saved for " + currentlySelectedBot.getName());
            showInfoAlert("Configuration Saved", "Bot configuration for " + currentlySelectedBot.getName() + " has been saved.");
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot to save its configuration.");
        }
    }

    // --- Utility Methods ---

    /**
     * Gets the primary stage of the application.
     * Lazy initializes it using any FXML node.
     * @return The primary Stage
     */
    private Stage getStage() {
        if (primaryStage == null && botListView != null) {
            primaryStage = (Stage) botListView.getScene().getWindow();
        } else if (primaryStage == null && rootLayout != null) {
            primaryStage = (Stage) rootLayout.getScene().getWindow();
        }
        return primaryStage;
    }

    public void appendConsoleOutput(String message) {
        // Run on JavaFX Application Thread
        Platform.runLater(() -> {
            consoleOutputArea.appendText(message + "\n");
            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    /**
     * Shows an information alert dialog.
     * @param title The title of the alert.
     * @param message The content message of the alert.
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a warning alert dialog.
     * @param title The title of the alert.
     * @param message The content message of the alert.
     */
    void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleCreateNewBot(ActionEvent event) {
        try {
            // Load the FXML for the wizard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NewBotWizard.fxml"));
            AnchorPane wizardRoot = loader.load(); // NewBotWizard.fxml uses AnchorPane as root
            NewBotWizardController wizardController = loader.getController();
            wizardController.setMainController(this); // Pass reference to MainController

            // Create a new Stage for the wizard window
            Stage wizardStage = new Stage();
            wizardStage.setTitle("Create New Discord Bot Project");
            wizardStage.initModality(Modality.APPLICATION_MODAL); // Blocks interaction with main window
            wizardStage.initOwner(getStage()); // Set parent window (MainController's stage)
            wizardStage.setScene(new Scene(wizardRoot));
            wizardStage.setResizable(false); // Wizards are often fixed size

            wizardStage.showAndWait(); // Show dialog and wait for it to be closed

            // The 'Finish' button in NewBotWizardController will call addBotToManager
            // so no need to process a result here.

        } catch (IOException e) {
            showErrorAlert("Error Loading Wizard", "Could not load the bot creation wizard: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // --- Inner Classes (Data Models) ---

    /**
     * Bot Model Class
     * Represents a Discord Bot managed by the application.
     */
    public static class Bot {
        private String id; // Unique identifier for the bot
        private String name;
        private String description;
        private String projectPath;
        private String version;
        private String mainFile; // e.g., "com.mybot.MainClass" or "index.js"
        private String logLevel; // INFO, DEBUG, ERROR etc.
        private boolean isRunning;
        private String jvmArgs; // For Java bots, JVM arguments
        private int startupDelayMs; // Delay before starting bot process
        private Process botProcess;

        // Using ObservableList for env variables so changes are automatically reflected if bound
        private ObservableList<EnvVariable> envVariables = FXCollections.observableArrayList();

        public Bot(String id, String name, String description, String projectPath, String version, String mainFile, String logLevel, boolean isRunning) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.projectPath = projectPath;
            this.version = version;
            this.mainFile = mainFile;
            this.logLevel = logLevel;
            this.isRunning = isRunning;
            this.jvmArgs = "";
            this.startupDelayMs = 0;
        }

        // --- Getters and Setters ---
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getProjectPath() { return projectPath; }
        public void setProjectPath(String projectPath) { this.projectPath = projectPath; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getMainFile() { return mainFile; }
        public void setMainFile(String mainFile) { this.mainFile = mainFile; }

        public String getLogLevel() { return logLevel; }
        public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

        public boolean isRunning() { return isRunning; }
        public void setRunning(boolean running) { this.isRunning = running; }

        public String getJvmArgs() { return jvmArgs; }
        public void setJvmArgs(String jvmArgs) { this.jvmArgs = jvmArgs; }

        public int getStartupDelayMs() { return startupDelayMs; }
        public void setStartupDelayMs(int startupDelayMs) { this.startupDelayMs = startupDelayMs; }

        public ObservableList<EnvVariable> getEnvVariables() { return envVariables; }
        public void setEnvVariables(ObservableList<EnvVariable> envVariables) { this.envVariables = envVariables; }

        @Override
        public String toString() {
            return name; // Used by ListView by default if no cell factory
        }

        public Process getBotProcess() { // <--- ADD THIS METHOD HERE
            return botProcess;
        }

        public void setBotProcess(Process botProcess) { // <--- ADD THIS METHOD HERE
            this.botProcess = botProcess;
        }
    }

    /**
     * Environment Variable model class
     */
    public static class EnvVariable {
        private String key;
        private String value;

        public EnvVariable(String key, String value) {
            this.key = key;
            this.value = value;
        }

        // Getters and Setters
        // Note: For TableView editing with PropertyValueFactory, these should ideally be JavaFX Properties
        // e.g., private StringProperty key = new SimpleStringProperty();
        // But for direct editing with TextFieldTableCell, simple getters/setters are often sufficient.
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}