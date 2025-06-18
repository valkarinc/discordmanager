package com.botmanager.controller;

import com.botmanager.util.CommandGenerator;
import com.botmanager.util.StringFormatter;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Main Controller for the Discord Bot Manager.
 * Handles all UI interactions and coordinates with backend services.
 *
 * @author valkarinc
 * @version 1.0.1
 */
public class MainController implements Initializable {

    @FXML private BorderPane rootLayout;

    // Menu Items
    @FXML private MenuBar mainMenuBar;
    @FXML private MenuItem importBotMenuItem;
    @FXML private MenuItem exitMenuItem;
    @FXML private MenuItem refreshMenuItem;
    @FXML private CheckMenuItem darkModeMenuItem;
    @FXML private CheckMenuItem draculaMenuItem;
    @FXML private MenuItem aboutMenuItem;

    // Sidebar Components
    @FXML private Button addBotButton;
    @FXML private TextField botSearchField;
    @FXML private ListView<Bot> botListView;
    @FXML private Button startAllButton;
    @FXML private Button stopAllButton;

    @FXML private ImageView botAvatar;
    @FXML private Label botNameLabel;
    @FXML private Label botDescriptionLabel;
    @FXML private Label botPathLabel;

    @FXML private MenuItem createNewBotMenuItem;
    @FXML private Button createNewBotButton;

    @FXML private Button startBotButton;
    @FXML private Button stopBotButton;
    @FXML private Button restartBotButton;
    @FXML private Button editBotButton;
    @FXML private Button removeBotButton;

    @FXML private TextField botCommandInput;
    @FXML private Button sendCommandButton;
    @FXML private Separator commandInputSeparator;

    // Tab Pane
    @FXML private TabPane botDetailsTabPane;
    @FXML private Tab activityMetricsTab;

    // Console Output Tab
    @FXML private TextArea consoleOutputArea;
    @FXML private Button clearLogButton;
    @FXML private CheckBox autoScrollCheckBox;

    // Bot Settings Tab
    @FXML private TextField configBotNameField;
    @FXML private TextField configBotVersionField;
    @FXML private TextField configMainFileField;
    @FXML private TextField configProjectPathField;
    @FXML private TextArea configDescriptionArea;

    @FXML private TableView<EnvVariable> envVarsTable;
    @FXML private TableColumn<EnvVariable, String> envKeyColumn;
    @FXML private TableColumn<EnvVariable, String> envValueColumn;
    @FXML private Button addEnvVarButton;
    @FXML private Button removeEnvVarButton;
    @FXML private Button saveConfigButton;

    @FXML private TextField jvmArgsField;
    @FXML private TextField startupDelayField;


    @FXML private Label statusLabel;
    @FXML private Label runningBotsLabel;

    private Stage primaryStage;
    private ObservableList<Bot> masterBotList = FXCollections.observableArrayList();
    private FilteredList<Bot> filteredBotList;
    private Bot currentlySelectedBot;
    private NewBotWizardController botWizardController;
    private Stage botWizardStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing Discord Bot Manager UI...");

        setupBotListView();

        setupEnvVarsTable();

        setupBotSearch();

        setupBotListListener();

        loadDefaultBotAvatar();

        updateUIState(null); // No bot selected initially

        // demo data
        //addTestData();

        if (createNewBotMenuItem != null) {
            createNewBotMenuItem.setOnAction(this::handleCreateNewBot);
        }

        if (createNewBotButton != null) {
            createNewBotButton.setOnAction(this::handleCreateNewBot);
        }

        System.out.println("Discord Bot Manager UI initialized.");
    }

    private void setupBotListView() {

        botListView.setCellFactory(lv -> new ListCell<Bot>() {
            @Override
            protected void updateItem(Bot bot, boolean empty) {
                super.updateItem(bot, empty);
                setText(empty ? null : bot.getName());
                // @todo: add graphics here too:
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

        filteredBotList = new FilteredList<>(masterBotList, p -> true);
        SortedList<Bot> sortedData = new SortedList<>(filteredBotList);
        botListView.setItems(sortedData);
    }

    private void setupEnvVarsTable() {
        envKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        envValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

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

        envVarsTable.setEditable(true);
    }


    private void setupBotSearch() {
        botSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredBotList.setPredicate(bot -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return bot.getName().toLowerCase().contains(lowerCaseFilter) ||
                        bot.getDescription().toLowerCase().contains(lowerCaseFilter) ||
                        bot.getProjectPath().toLowerCase().contains(lowerCaseFilter);
            });
        });
    }

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

    //default img
    private void loadDefaultBotAvatar() {

        if (botAvatar == null) {
            System.err.println("Error: botAvatar ImageView is null during loadDefaultBotAvatar call. Check FXML fx:id and injection.");
            return;
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

    private void loadBotDetails(Bot bot) {
        if (bot == null) {
            clearBotDetails();
            return;
        }

        //bot header
        botNameLabel.setText(bot.getName());
        botDescriptionLabel.setText(bot.getDescription());
        botPathLabel.setText(bot.getProjectPath());
        // TODO: Update botAvatar.setImage(new Image(bot.getAvatarUrl()))

        configBotNameField.setText(bot.getName());
        configBotVersionField.setText(bot.getVersion());
        configMainFileField.setText(bot.getMainFile());
        configProjectPathField.setText(bot.getProjectPath());
        configDescriptionArea.setText(bot.getDescription());
        jvmArgsField.setText(bot.getJvmArgs());
        startupDelayField.setText(String.valueOf(bot.getStartupDelayMs()));

        envVarsTable.setItems(FXCollections.observableArrayList(bot.getEnvVariables()));

        consoleOutputArea.appendText("[INFO] Loaded details for " + bot.getName() + ".\n");

        statusLabel.setText("Bot details loaded: " + bot.getName());
    }

    private void clearBotDetails() {
        botNameLabel.setText("Select a Bot");
        botDescriptionLabel.setText("No bot selected. Please choose a bot from the left sidebar to view its details and controls.");
        botPathLabel.setText("");
        loadDefaultBotAvatar();

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

    private void updateUIState(Bot selectedBot) {
        boolean isBotSelected = (selectedBot != null);
        boolean isBotRunningAndProcessAvailable = isBotSelected && selectedBot.isRunning() && selectedBot.getBotProcess() != null && selectedBot.getBotProcess().isAlive();

        // Control Buttons
        startBotButton.setDisable(!isBotSelected || (selectedBot.isRunning()));
        stopBotButton.setDisable(!isBotRunningAndProcessAvailable);
        restartBotButton.setDisable(!isBotSelected);
        editBotButton.setDisable(!isBotSelected);
        removeBotButton.setDisable(!isBotSelected);

        // Configuration/Settings fields
        boolean enableConfigEditing = isBotSelected; // Allow editing if a bot is selected
        configBotNameField.setEditable(enableConfigEditing);
        configBotVersionField.setEditable(enableConfigEditing);
        configMainFileField.setEditable(enableConfigEditing);
        configProjectPathField.setEditable(false);
        configDescriptionArea.setEditable(enableConfigEditing);
        jvmArgsField.setEditable(enableConfigEditing);
        startupDelayField.setEditable(enableConfigEditing);
        envVarsTable.setEditable(enableConfigEditing);
        addEnvVarButton.setDisable(!isBotSelected);
        removeEnvVarButton.setDisable(!isBotSelected || envVarsTable.getSelectionModel().isEmpty());
        saveConfigButton.setDisable(!isBotSelected);

        // New Command Input UI
        if (botCommandInput != null) {
            botCommandInput.setDisable(!isBotRunningAndProcessAvailable);
        }
        if (sendCommandButton != null) {
            sendCommandButton.setDisable(!isBotRunningAndProcessAvailable);
        }
        if (commandInputSeparator != null) { // Show/hide separator with controls
            commandInputSeparator.setVisible(isBotSelected);
            commandInputSeparator.setManaged(isBotSelected);
        }

        if (isBotSelected) {
            botDetailsTabPane.setDisable(false);
            if (botDetailsTabPane.getSelectionModel().getSelectedItem() == null) {
                botDetailsTabPane.getSelectionModel().selectFirst();
            }
        } else {
            botDetailsTabPane.setDisable(true);
            if (commandInputSeparator != null) {
                commandInputSeparator.setVisible(false);
                commandInputSeparator.setManaged(false);
            }
        }
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void addTestData() {
        //keep this here
        Bot bot1 = new Bot("bot-1", "Sample Bot 1", "This is a basic Discord bot for general use.", "/users/chaz/bots/sample-bot-1", "1.0.0", "com.example.Main", "INFO", false);
        bot1.getEnvVariables().add(new EnvVariable("DISCORD_TOKEN", "YOUR_TOKEN_1"));
        bot1.getEnvVariables().add(new EnvVariable("PREFIX", "!"));

        Bot bot2 = new Bot("bot-2", "Discord Music Bot", "A feature-rich bot for playing music in voice channels.", "/users/chaz/bots/music-bot", "2.1.0", "org.musicbot.App", "DEBUG", true);
        bot2.setRunning(true); // Simulate running state
        bot2.getEnvVariables().add(new EnvVariable("SPOTIFY_API_KEY", "ABC123XYZ"));
        bot2.getEnvVariables().add(new EnvVariable("YOUTUBE_API_KEY", "DEF456UVW"));

        Bot bot3 = new Bot("bot-3", "Moderation Bot", "Automates moderation tasks and keeps your server safe.", "/users/chaz/bots/moderation-bot", "3.0.5", "net.modbot.Launcher", "WARN", false);

        masterBotList.addAll(bot1, bot2, bot3);

        if (!masterBotList.isEmpty()) {
            botListView.getSelectionModel().selectFirst();
        }

        long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
        runningBotsLabel.setText("Active Bots: " + runningCount);
    }


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
            botListView.getSelectionModel().select(newBot);

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
    private void handleToggleDraculaMode() {
        boolean isDraculaMode = draculaMenuItem.isSelected();
        System.out.println("Dracula mode: " + (isDraculaMode ? "ON" : "OFF"));
        if (rootLayout != null) {
            ObservableList<String> stylesheets = rootLayout.getStylesheets();
            String draculaCss = getClass().getResource("/css/dracula-style.css").toExternalForm();
            if (isDraculaMode) {
                if (!stylesheets.contains(draculaCss)) {
                    stylesheets.add(draculaCss);
                }
            } else {
                stylesheets.remove(draculaCss);
            }
            statusLabel.setText("Dracula mode: " + (isDraculaMode ? "Enabled" : "Disabled"));
        }
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

                List<String> commandParts = CommandGenerator.generateJavaJarStartCommand(currentlySelectedBot);

                if (commandParts == null) {
                    Path expectedJarPath = CommandGenerator.getExpectedJarPath(currentlySelectedBot);
                    String expectedPathMessage = (expectedJarPath != null) ?
                            "Expected at: " + expectedJarPath.toString() :
                            "Could not determine expected JAR path (check bot name and project path).";

                    showAlert("JAR Not Found or Command Generation Failed",
                            "Bot JAR file not found or command could not be generated for '" + currentlySelectedBot.getName() + "'.\n" +
                                    expectedPathMessage +
                                    "\nPlease ensure the bot project is built (e.g., 'mvn clean install') and the JAR name/location are correct according to conventions.");

                    currentlySelectedBot.setRunning(false);
                    updateUIState(currentlySelectedBot);
                    return;
                }

                ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
                processBuilder.directory(new File(currentlySelectedBot.getProjectPath()));
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                currentlySelectedBot.setBotProcess(process);
                currentlySelectedBot.setRunning(true);

                new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String outputLine = line;
                            Platform.runLater(() -> consoleOutputArea.appendText("[BOT] " + currentlySelectedBot.getName() + ": " + outputLine + "\n"));
                        }
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            consoleOutputArea.appendText("[ERROR] Reading output for " + currentlySelectedBot.getName() + ": " + e.getMessage() + "\n");
                        });
                    } finally {
                        Platform.runLater(() -> {
                            if (currentlySelectedBot != null) {
                                currentlySelectedBot.setRunning(false);
                                currentlySelectedBot.setBotProcess(null);
                                consoleOutputArea.appendText("[INFO] " + currentlySelectedBot.getName() + " process finished.\n");
                                updateUIState(currentlySelectedBot);
                            } else {
                                consoleOutputArea.appendText("[INFO] A bot process finished after the bot was deselected or removed.\n");
                            }
                        });
                    }
                }).start();

                consoleOutputArea.appendText("[INFO] " + currentlySelectedBot.getName() + " started successfully!\n");
                String formattedStatus = StringFormatter.formatBotStatus(currentlySelectedBot.getName(), currentlySelectedBot.isRunning());
                statusLabel.setText(formattedStatus);
                consoleOutputArea.appendText(StringFormatter.getGreeting("Bot Manager User") + "\n");

            } catch (IOException e) {
                showAlert("Error Starting Bot", "Could not start bot '" + currentlySelectedBot.getName() + "': " + e.getMessage());
                if (currentlySelectedBot != null) {
                    currentlySelectedBot.setRunning(false);
                    statusLabel.setText("Failed to start bot: " + currentlySelectedBot.getName());
                }
                e.printStackTrace();
            }

            long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
            runningBotsLabel.setText("Active Bots: " + runningCount);

            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
            updateUIState(currentlySelectedBot);
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot from the list to start.");
        }
    }

    @FXML
    private void handleStopBot() {
        if (currentlySelectedBot != null) {
            if (!currentlySelectedBot.isRunning()) {
                showAlert("Bot Not Running", currentlySelectedBot.getName() + " is not currently running.");
                return;
            }

            System.out.println("Attempting to stop bot: " + currentlySelectedBot.getName());
            consoleOutputArea.appendText("[INFO] Stopping " + currentlySelectedBot.getName() + "...\n");

            Process botProcess = currentlySelectedBot.getBotProcess();
            if (botProcess != null) {
                try {
                    botProcess.destroy();

                    boolean exited = botProcess.waitFor(5, TimeUnit.SECONDS);

                    if (exited) {
                        consoleOutputArea.appendText("[INFO] " + currentlySelectedBot.getName() + " stopped successfully.\n");
                    } else {
                        botProcess.destroyForcibly();
                        consoleOutputArea.appendText("[WARN] " + currentlySelectedBot.getName() + " did not respond; forced to stop.\n");
                    }

                    currentlySelectedBot.setRunning(false);
                    currentlySelectedBot.setBotProcess(null);
                    statusLabel.setText("Bot stopped: " + currentlySelectedBot.getName());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showAlert("Error Stopping Bot", "Stop operation interrupted for " + currentlySelectedBot.getName() + ": " + e.getMessage());
                    statusLabel.setText("Failed to stop bot: " + currentlySelectedBot.getName());
                    e.printStackTrace();
                } catch (Exception e) {
                    showAlert("Error Stopping Bot", "Could not stop bot: " + e.getMessage());
                    statusLabel.setText("Failed to stop bot: " + currentlySelectedBot.getName());
                    e.printStackTrace();
                }
            } else {
                consoleOutputArea.appendText("[WARN] No active process reference found for " + currentlySelectedBot.getName() + ". Assuming it's already stopped.\n");
                currentlySelectedBot.setRunning(false);
            }

            long runningCount = masterBotList.stream().filter(Bot::isRunning).count();
            runningBotsLabel.setText("Active Bots: " + runningCount);

            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
            updateUIState(currentlySelectedBot);
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

            final Bot botToRemove = currentlySelectedBot;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Bot Removal");
            alert.setHeaderText("Remove " + botToRemove.getName() + "?");
            alert.setContentText("Are you sure you want to remove this bot? This action cannot be undone and will delete its configuration.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    System.out.println("Removing bot: " + botToRemove.getName()); // Use the local variable here too
                    // TODO: Implement actual deletion logic (delete config files, stop if running)

                    if (botToRemove.isRunning()) {

                        // For now, simulate:
                        consoleOutputArea.appendText("[INFO] Stopping " + botToRemove.getName() + " before removal...\n");
                        botToRemove.setRunning(false); // Update model
                    }

                    masterBotList.remove(botToRemove);

                    if (currentlySelectedBot == botToRemove) {
                        currentlySelectedBot = null; // Clear selection in controller state
                        botListView.getSelectionModel().clearSelection(); // Visually clear selection
                        clearBotDetails(); // Clear detail pane
                    }

                    updateUIState(null);

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

            // TODO: Persist the updated bot object to disk (e.g., JSON file)
            masterBotList.set(masterBotList.indexOf(currentlySelectedBot), currentlySelectedBot); // Update in master list to reflect changes in ListView

            botListView.refresh();

            statusLabel.setText("Configuration saved for " + currentlySelectedBot.getName());
            showInfoAlert("Configuration Saved", "Bot configuration for " + currentlySelectedBot.getName() + " has been saved.");
        } else {
            showWarningAlert("No Bot Selected", "Please select a bot to save its configuration.");
        }
    }

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

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NewBotWizard.fxml"));
            AnchorPane wizardRoot = loader.load(); // NewBotWizard.fxml uses AnchorPane as root
            NewBotWizardController wizardController = loader.getController();
            wizardController.setMainController(this);

            Stage wizardStage = new Stage();
            wizardStage.setTitle("Create New Discord Bot Project");
            wizardStage.initModality(Modality.APPLICATION_MODAL);
            wizardStage.initOwner(getStage());
            wizardStage.setScene(new Scene(wizardRoot));
            wizardStage.setResizable(false);

            wizardStage.showAndWait();

        } catch (IOException e) {
            showErrorAlert("Error Loading Wizard", "Could not load the bot creation wizard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSendCommand() {
        if (currentlySelectedBot == null) {
            showWarningAlert("No Bot Selected", "Please select a bot to send a command to.");
            return;
        }
        if (!currentlySelectedBot.isRunning() || currentlySelectedBot.getBotProcess() == null) {
            showWarningAlert("Bot Not Running", currentlySelectedBot.getName() + " is not running or its process is unavailable. Start the bot first.");
            return;
        }

        String commandText = botCommandInput.getText();
        if (commandText == null || commandText.trim().isEmpty()) {
            showWarningAlert("Empty Command", "Please enter a command to send.");
            return;
        }

        consoleOutputArea.appendText("[UI_COMMAND] Attempting to send to " + currentlySelectedBot.getName() + ": " + commandText + "\n");

        Process botProcess = currentlySelectedBot.getBotProcess();
        if (botProcess.isAlive()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(botProcess.getOutputStream()))) {
                writer.write(commandText + System.lineSeparator()); // Send command with a newline
                writer.flush();
                consoleOutputArea.appendText("[SYSTEM] Command '" + commandText + "' sent to " + currentlySelectedBot.getName() + "'s input stream.\n");
                botCommandInput.clear();
            } catch (IOException e) {
                consoleOutputArea.appendText("[ERROR] Failed to send command to " + currentlySelectedBot.getName() + ": " + e.getMessage() + "\n");
                e.printStackTrace();
                showErrorAlert("Command Send Error", "Could not send command: " + e.getMessage());
            }
        } else {
            consoleOutputArea.appendText("[WARN] Bot process for " + currentlySelectedBot.getName() + " is not alive.\n");
            showWarningAlert("Bot Process Error", "The process for " + currentlySelectedBot.getName() + " is no longer running.");
        }

        if (autoScrollCheckBox.isSelected()) {
            consoleOutputArea.setScrollTop(Double.MAX_VALUE);
        }
    }

    public static class Bot {
        private String id;
        private String name;
        private String description;
        private String projectPath;
        private String version;
        private String mainFile;
        private String logLevel;
        private boolean isRunning;
        private String jvmArgs;
        private int startupDelayMs;
        private Process botProcess;

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
            return name;
        }

        public Process getBotProcess() {
            return botProcess;
        }

        public void setBotProcess(Process botProcess) {
            this.botProcess = botProcess;
        }
    }

    public static class EnvVariable {
        private String key;
        private String value;

        public EnvVariable(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}