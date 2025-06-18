package com.botmanager.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main Controller for the Discord Bot Manager application
 * Handles all UI interactions and coordinates with backend services
 */
public class MainController implements Initializable {

    // Menu Items
    @FXML private MenuItem importBotMenuItem;
    @FXML private MenuItem exitMenuItem;
    @FXML private MenuItem refreshMenuItem;
    @FXML private CheckMenuItem darkModeMenuItem;
    @FXML private MenuItem aboutMenuItem;

    // Sidebar Components
    @FXML private ListView<String> botListView;
    @FXML private Button addBotButton;
    @FXML private Button startAllButton;
    @FXML private Button stopAllButton;

    // Bot Info Components
    @FXML private Label botNameLabel;
    @FXML private Label botDescriptionLabel;
    @FXML private Label botPathLabel;

    // Control Buttons
    @FXML private Button startBotButton;
    @FXML private Button stopBotButton;
    @FXML private Button restartBotButton;

    // Console Tab
    @FXML private TabPane configTabPane;
    @FXML private TextArea consoleOutputArea;
    @FXML private Button clearLogButton;
    @FXML private CheckBox autoScrollCheckBox;

    // Configuration Tab
    @FXML private TextField configBotNameField;
    @FXML private TextField configBotVersionField;
    @FXML private TextField configMainFileField;
    @FXML private TableView<EnvVariable> envVarsTable;
    @FXML private TableColumn<EnvVariable, String> envKeyColumn;
    @FXML private TableColumn<EnvVariable, String> envValueColumn;
    @FXML private Button addEnvVarButton;
    @FXML private Button removeEnvVarButton;
    @FXML private Button saveConfigButton;

    // Status Bar
    @FXML private Label statusLabel;
    @FXML private Label runningBotsLabel;

    // Internal state
    private Stage primaryStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing Discord Bot Manager...");

        // Initialize table columns
        setupTableColumns();

        // Setup bot list selection listener
        setupBotListListener();

        // Initialize UI state
        updateUIState();

        // Test data for UI verification
        addTestData();

        System.out.println("Discord Bot Manager initialized successfully!");
    }

    /**
     * Setup table columns for environment variables
     */
    private void setupTableColumns() {
        envKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        envValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // Make columns editable
        envKeyColumn.setCellFactory(column -> new EditingCell());
        envValueColumn.setCellFactory(column -> new EditingCell());

        envVarsTable.setEditable(true);
    }

    /**
     * Setup listener for bot list selection changes
     */
    private void setupBotListListener() {
        botListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadBotDetails(newValue);
                    } else {
                        clearBotDetails();
                    }
                }
        );
    }

    /**
     * Load details for selected bot
     */
    private void loadBotDetails(String botName) {
        // TODO: Load actual bot details from configuration
        botNameLabel.setText(botName);
        botDescriptionLabel.setText("Sample bot description");
        botPathLabel.setText("/path/to/bot/project");

        // Enable control buttons
        startBotButton.setDisable(false);
        stopBotButton.setDisable(true); // Will be enabled when bot is running
        restartBotButton.setDisable(false);

        // Update configuration fields
        configBotNameField.setText(botName);
        configBotVersionField.setText("1.0.0");
        configMainFileField.setText("index.js");

        // Update status
        statusLabel.setText("Bot selected: " + botName);
    }

    /**
     * Clear bot details when no bot is selected
     */
    private void clearBotDetails() {
        botNameLabel.setText("Select a bot to view details");
        botDescriptionLabel.setText("");
        botPathLabel.setText("");

        // Disable control buttons
        startBotButton.setDisable(true);
        stopBotButton.setDisable(true);
        restartBotButton.setDisable(true);

        // Clear configuration fields
        configBotNameField.clear();
        configBotVersionField.clear();
        configMainFileField.clear();
        envVarsTable.getItems().clear();

        statusLabel.setText("No bot selected");
    }

    /**
     * Update UI state based on current application state
     */
    private void updateUIState() {
        // Update running bots count
        runningBotsLabel.setText("Running Bots: 0");
        statusLabel.setText("Ready");

        // Initially disable bot-specific controls
        startBotButton.setDisable(true);
        stopBotButton.setDisable(true);
        restartBotButton.setDisable(true);
    }

    /**
     * Add test data for UI verification
     */
    private void addTestData() {
        botListView.getItems().addAll(
                "Sample Bot 1",
                "Discord Music Bot",
                "Moderation Bot"
        );

        // Select first item for testing
        if (!botListView.getItems().isEmpty()) {
            botListView.getSelectionModel().selectFirst();
        }
    }

    // === EVENT HANDLERS ===

    /**
     * Handle import bot action
     */
    @FXML
    private void handleImportBot() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Bot Project Folder");

        File selectedDirectory = directoryChooser.showDialog(getStage());

        if (selectedDirectory != null) {
            System.out.println("Selected directory: " + selectedDirectory.getAbsolutePath());
            statusLabel.setText("Importing bot from: " + selectedDirectory.getName());

            // TODO: Implement actual bot import logic
            showInfoAlert("Import Bot", "Bot import functionality will be implemented next!");
        }
    }

    /**
     * Handle application exit
     */
    @FXML
    private void handleExit() {
        System.out.println("Exiting application...");
        System.exit(0);
    }

    /**
     * Handle refresh action
     */
    @FXML
    private void handleRefresh() {
        System.out.println("Refreshing bot list...");
        statusLabel.setText("Refreshing...");
        // TODO: Implement refresh logic
        statusLabel.setText("Refreshed");
    }

    /**
     * Handle dark mode toggle
     */
    @FXML
    private void handleToggleDarkMode() {
        boolean isDarkMode = darkModeMenuItem.isSelected();
        System.out.println("Dark mode: " + (isDarkMode ? "ON" : "OFF"));
        statusLabel.setText("Dark mode: " + (isDarkMode ? "Enabled" : "Disabled"));
        // TODO: Implement dark mode styling
    }

    /**
     * Handle about dialog
     */
    @FXML
    private void handleAbout() {
        showInfoAlert("About",
                "Discord Bot Manager v1.0.0\n\n" +
                        "A JavaFX application for managing Discord bots.\n" +
                        "Built with Java 17 and JavaFX.");
    }

    /**
     * Handle start bot action
     */
    @FXML
    private void handleStartBot() {
        String selectedBot = botListView.getSelectionModel().getSelectedItem();
        if (selectedBot != null) {
            System.out.println("Starting bot: " + selectedBot);
            consoleOutputArea.appendText("[INFO] Starting " + selectedBot + "...\n");
            consoleOutputArea.appendText("[INFO] Bot started successfully!\n");

            // Update button states
            startBotButton.setDisable(true);
            stopBotButton.setDisable(false);

            statusLabel.setText("Bot started: " + selectedBot);
            runningBotsLabel.setText("Running Bots: 1");

            // Auto-scroll console if enabled
            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
        }
    }

    /**
     * Handle stop bot action
     */
    @FXML
    private void handleStopBot() {
        String selectedBot = botListView.getSelectionModel().getSelectedItem();
        if (selectedBot != null) {
            System.out.println("Stopping bot: " + selectedBot);
            consoleOutputArea.appendText("[INFO] Stopping " + selectedBot + "...\n");
            consoleOutputArea.appendText("[INFO] Bot stopped.\n");

            // Update button states
            startBotButton.setDisable(false);
            stopBotButton.setDisable(true);

            statusLabel.setText("Bot stopped: " + selectedBot);
            runningBotsLabel.setText("Running Bots: 0");

            // Auto-scroll console if enabled
            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
        }
    }

    /**
     * Handle restart bot action
     */
    @FXML
    private void handleRestartBot() {
        String selectedBot = botListView.getSelectionModel().getSelectedItem();
        if (selectedBot != null) {
            System.out.println("Restarting bot: " + selectedBot);
            consoleOutputArea.appendText("[INFO] Restarting " + selectedBot + "...\n");
            consoleOutputArea.appendText("[INFO] Bot restarted successfully!\n");

            statusLabel.setText("Bot restarted: " + selectedBot);

            // Auto-scroll console if enabled
            if (autoScrollCheckBox.isSelected()) {
                consoleOutputArea.setScrollTop(Double.MAX_VALUE);
            }
        }
    }

    /**
     * Handle start all bots action
     */
    @FXML
    private void handleStartAll() {
        System.out.println("Starting all bots...");
        consoleOutputArea.appendText("[INFO] Starting all bots...\n");
        statusLabel.setText("Starting all bots...");
        // TODO: Implement start all logic
    }

    /**
     * Handle stop all bots action
     */
    @FXML
    private void handleStopAll() {
        System.out.println("Stopping all bots...");
        consoleOutputArea.appendText("[INFO] Stopping all bots...\n");
        statusLabel.setText("Stopping all bots...");
        // TODO: Implement stop all logic
    }

    /**
     * Handle clear log action
     */
    @FXML
    private void handleClearLog() {
        consoleOutputArea.clear();
        statusLabel.setText("Console cleared");
    }

    /**
     * Handle add environment variable action
     */
    @FXML
    private void handleAddEnvVar() {
        envVarsTable.getItems().add(new EnvVariable("NEW_VAR", "value"));
        statusLabel.setText("Environment variable added");
    }

    /**
     * Handle remove environment variable action
     */
    @FXML
    private void handleRemoveEnvVar() {
        EnvVariable selected = envVarsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            envVarsTable.getItems().remove(selected);
            statusLabel.setText("Environment variable removed");
        }
    }

    /**
     * Handle save configuration action
     */
    @FXML
    private void handleSaveConfig() {
        System.out.println("Saving configuration...");
        statusLabel.setText("Configuration saved");
        // TODO: Implement save configuration logic
    }

    // === UTILITY METHODS ===

    /**
     * Get the primary stage
     */
    private Stage getStage() {
        if (primaryStage == null && botListView != null) {
            primaryStage = (Stage) botListView.getScene().getWindow();
        }
        return primaryStage;
    }

    /**
     * Show information alert dialog
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // === INNER CLASSES ===

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

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    /**
     * Custom editable table cell
     */
    private static class EditingCell extends TableCell<EnvVariable, String> {
        private TextField textField;

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    commitEdit(textField.getText());
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }
}