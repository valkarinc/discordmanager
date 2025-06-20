package com.botmanager.controller;

import com.botmanager.util.BotGenerator; // Import your BotGenerator
import com.botmanager.controller.MainController.Bot; // Import MainController.Bot

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class NewBotWizardController {

    // FXML for Wizard UI
    @FXML private AnchorPane wizardRootPane;
    @FXML private Label stepTitleLabel;
    @FXML private StackPane wizardContentStackPane;

    @FXML private VBox step1Pane;
    @FXML private TextField botDisplayNameField;
    @FXML private TextField projectNameField;
    @FXML private TextField groupIdField;
    @FXML private TextField botVersionField;
    @FXML private TextArea botDescriptionArea;

    @FXML private VBox step2Pane;
    @FXML private TextField mainClassNameField;
    @FXML private ComboBox<String> botTypeComboBox;
    @FXML private ComboBox<String> javaVersionComboBox;
    @FXML private PasswordField botTokenField;
    @FXML private CheckBox includeDiscordTokenEnv;
    @FXML private CheckBox includePrefixEnv;

    @FXML private VBox step3Pane;
    @FXML private TextField installLocationField;
    @FXML private Button browseLocationButton;
    @FXML private Label projectPreviewPathLabel;

    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button finishButton;
    @FXML private Button cancelButton;

    @FXML private Label errorLabel;

    private int currentStep = 1;
    private final int totalSteps = 3;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {

        botVersionField.setText("1.0.0");
        groupIdField.setText("com.mycompany.discordbot");

        mainClassNameField.setText("Main");
        botTypeComboBox.getSelectionModel().selectFirst();
        javaVersionComboBox.getSelectionModel().select("17"); // Default Java Version

        botDisplayNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal == null || oldVal.isEmpty() || projectNameField.getText().equalsIgnoreCase(cleanForProjectName(oldVal))) {
                projectNameField.setText(cleanForProjectName(newVal));
            }
            if (oldVal == null || oldVal.isEmpty() || mainClassNameField.getText().equalsIgnoreCase(cleanForClassName(oldVal))) {
                mainClassNameField.setText(cleanForClassName(newVal) + "Main");
            }
        });

        installLocationField.textProperty().addListener((obs, oldVal, newVal) -> updateProjectPreviewPath());
        projectNameField.textProperty().addListener((obs, oldVal, newVal) -> updateProjectPreviewPath());

        updateWizardUI();
        addValidationListeners();
    }

    private void addValidationListeners() {
        botDisplayNameField.textProperty().addListener((obs) -> validateCurrentStep());
        projectNameField.textProperty().addListener((obs) -> validateCurrentStep());
        groupIdField.textProperty().addListener((obs) -> validateCurrentStep());
        botVersionField.textProperty().addListener((obs) -> validateCurrentStep());
        botDescriptionArea.textProperty().addListener((obs) -> validateCurrentStep());

        mainClassNameField.textProperty().addListener((obs) -> validateCurrentStep());
        botTypeComboBox.valueProperty().addListener((obs) -> validateCurrentStep());
        javaVersionComboBox.valueProperty().addListener((obs) -> validateCurrentStep());

        installLocationField.textProperty().addListener((obs) -> validateCurrentStep());
    }

    private String cleanForProjectName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Allow only a-z, 0-9, spaces, hyphens
                .replaceAll("\\s+", "-")       // Replace spaces with hyphens
                .replaceAll("^-|-$", "");      // Remove leading/trailing hyphens
    }

    private String cleanForClassName(String name) {
        if (name == null) return "";
        String cleaned = name.replaceAll("[^a-zA-Z0-9\\s]", "");
        StringBuilder builder = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : cleaned.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private void updateProjectPreviewPath() {
        String location = installLocationField.getText();
        String projectName = projectNameField.getText();
        if (location != null && !location.trim().isEmpty() && projectName != null && !projectName.trim().isEmpty()) {
            projectPreviewPathLabel.setText(Paths.get(location, projectName).normalize().toString());
        } else {
            projectPreviewPathLabel.setText("[Please select an install location and enter a project name]");
        }
    }

    private void updateWizardUI() {
        step1Pane.setVisible(false);
        step1Pane.setManaged(false);
        step2Pane.setVisible(false);
        step2Pane.setManaged(false);
        step3Pane.setVisible(false);
        step3Pane.setManaged(false);

        backButton.setDisable(currentStep == 1);
        nextButton.setVisible(currentStep < totalSteps);
        nextButton.setManaged(currentStep < totalSteps);
        finishButton.setVisible(currentStep == totalSteps);
        finishButton.setManaged(currentStep == totalSteps);

        switch (currentStep) {
            case 1:
                step1Pane.setVisible(true);
                step1Pane.setManaged(true);
                stepTitleLabel.setText("Step 1: Bot Basic Information");
                break;
            case 2:
                step2Pane.setVisible(true);
                step2Pane.setManaged(true);
                stepTitleLabel.setText("Step 2: Bot Configuration & Dependencies");
                break;
            case 3:
                step3Pane.setVisible(true);
                step3Pane.setManaged(true);
                stepTitleLabel.setText("Step 3: Installation Location");
                break;
        }
        validateCurrentStep();
    }

    /**
     * Validates the input for the current step.
     * @return true if the current step's input is valid, false otherwise.
     */
    private boolean validateCurrentStep() {
        StringBuilder errorMessage = new StringBuilder();

        switch (currentStep) {
            case 1:
                if (botDisplayNameField.getText() == null || botDisplayNameField.getText().trim().isEmpty()) {
                    errorMessage.append("- Bot Display Name is required.\n");
                }
                if (projectNameField.getText() == null || projectNameField.getText().trim().isEmpty()) {
                    errorMessage.append("- Project Name (ArtifactId) is required.\n");
                } else if (!projectNameField.getText().matches("^[a-z0-9-]+$")) {
                    errorMessage.append("- Project Name must be lowercase alphanumeric with hyphens (no spaces or special chars).\n");
                }
                if (groupIdField.getText() == null || groupIdField.getText().trim().isEmpty()) {
                    errorMessage.append("- Group ID (Package) is required.\n");
                } else if (!groupIdField.getText().matches("^[a-zA-Z0-9.]+$")) {
                    errorMessage.append("- Group ID must be alphanumeric with dots (e.g., com.example.bot).\n");
                }
                break;
            case 2:
                if (mainClassNameField.getText() == null || mainClassNameField.getText().trim().isEmpty()) {
                    errorMessage.append("- Main Class Name is required.\n");
                } else if (!mainClassNameField.getText().matches("^[a-zA-Z_$][a-zA-Z0-9_$]*$")) {
                    errorMessage.append("- Main Class Name is not a valid Java identifier.\n");
                }
                if (botTypeComboBox.getSelectionModel().isEmpty()) {
                    errorMessage.append("- Bot Type/Framework is required.\n");
                }
                if (javaVersionComboBox.getSelectionModel().isEmpty()) {
                    errorMessage.append("- Java Version is required.\n");
                }
                break;
            case 3:
                if (installLocationField.getText() == null || installLocationField.getText().trim().isEmpty()) {
                    errorMessage.append("- Install Location is required.\n");
                } else {
                    Path chosenPath = Paths.get(installLocationField.getText());
                    Path projectPath = chosenPath.resolve(projectNameField.getText());
                    if (Files.exists(projectPath)) {
                        errorMessage.append("- Project directory '").append(projectPath.getFileName()).append("' already exists at this location.\n");
                    }
                }
                break;
        }

        boolean isValid = errorMessage.isEmpty();
        errorLabel.setText(errorMessage.toString());
        errorLabel.setVisible(!isValid);
        errorLabel.setManaged(!isValid);

        if (currentStep < totalSteps) {
            nextButton.setDisable(!isValid);
        } else {
            finishButton.setDisable(!isValid);
        }

        return isValid;
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (currentStep > 1) {
            currentStep--;
            updateWizardUI();
        }
    }

    @FXML
    private void handleNext(ActionEvent event) {
        if (validateCurrentStep()) {
            if (currentStep < totalSteps) {
                currentStep++;
                updateWizardUI();
            }
        }
    }

    @FXML
    private void handleFinish(ActionEvent event) {
        if (validateCurrentStep()) {

            BotCreationDetails details = new BotCreationDetails(
                    botDisplayNameField.getText().trim(),
                    projectNameField.getText().trim(),
                    groupIdField.getText().trim(),
                    botVersionField.getText().trim(),
                    botDescriptionArea.getText().trim(),
                    mainClassNameField.getText().trim(),
                    installLocationField.getText().trim(),
                    botTypeComboBox.getSelectionModel().getSelectedItem(),
                    javaVersionComboBox.getSelectionModel().getSelectedItem(),
                    botTokenField.getText().trim(),
                    includeDiscordTokenEnv.isSelected(),
                    includePrefixEnv.isSelected()
            );

            Stage stage = (Stage) wizardRootPane.getScene().getWindow();
            stage.close();

            if (mainController != null) {
                mainController.appendConsoleOutput("[INFO] Preparing to create bot project: " + details.getDisplayName());
                mainController.appendConsoleOutput("[INFO] This might take a moment...");
                new Thread(() -> {
                    try {
                        new BotGenerator().generateBotProject(details);

                        Platform.runLater(() -> {

                            Bot newBot = new Bot(
                                    UUID.randomUUID().toString(),
                                    details.getDisplayName(),
                                    details.getDescription(),
                                    details.getFullProjectPath().toString(),
                                    details.getVersion(),
                                    details.getMainClassName(),
                                    "INFO",
                                    false
                            );
                            newBot.setJvmArgs("");
                            newBot.setStartupDelayMs(0);

                            if (details.isIncludeDiscordTokenEnv()) {
                                // Important: We DO NOT store the token directly in the Bot object here for security.

                                newBot.getEnvVariables().add(new MainController.EnvVariable("DISCORD_TOKEN", ""));
                            }
                            if (details.isIncludePrefixEnv()) {
                                newBot.getEnvVariables().add(new MainController.EnvVariable("BOT_PREFIX", "!"));
                            }

                            mainController.addBotToManager(newBot);
                            mainController.appendConsoleOutput("[INFO] Bot project '" + details.getDisplayName() + "' created successfully!");
                        });

                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            mainController.showErrorAlert("Project Generation Error", "Failed to generate bot project: " + e.getMessage());
                            e.printStackTrace();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            mainController.showErrorAlert("Unexpected Error", "An unexpected error occurred during bot project generation: " + e.getMessage());
                            e.printStackTrace();
                        });
                    }
                }).start();
            }
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) wizardRootPane.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleBrowseLocation(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Bot Project Installation Directory");

        File initialDir = new File(System.getProperty("user.home"));
        if (installLocationField.getText() != null && !installLocationField.getText().isEmpty()) {
            File currentSelected = new File(installLocationField.getText());
            if (currentSelected.exists() && currentSelected.isDirectory()) {
                initialDir = currentSelected;
            }
        }
        directoryChooser.setInitialDirectory(initialDir);

        Stage stage = (Stage) browseLocationButton.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            installLocationField.setText(selectedDirectory.getAbsolutePath());
            updateProjectPreviewPath(); // Update preview immediately
            validateCurrentStep(); // Re-validate after changing location
        }
    }

    public static class BotCreationDetails {
        private final String displayName;
        private final String projectName;
        private final String groupId;
        private final String version;
        private final String description;
        private final String mainClassName;
        private final String installLocation;
        private final String botType;
        private final String javaVersion;
        private final String botToken; // Direct token (optional, for direct injection or specific config)
        private final boolean includeDiscordTokenEnv; // Whether to add DISCORD_TOKEN as env var
        private final boolean includePrefixEnv; // Whether to add BOT_PREFIX as env var


        public BotCreationDetails(String displayName, String projectName, String groupId, String version, String description,
                                  String mainClassName, String installLocation, String botType, String javaVersion,
                                  String botToken, boolean includeDiscordTokenEnv, boolean includePrefixEnv) {
            this.displayName = displayName;
            this.projectName = projectName;
            this.groupId = groupId;
            this.version = version;
            this.description = description;
            this.mainClassName = mainClassName;
            this.installLocation = installLocation;
            this.botType = botType;
            this.javaVersion = javaVersion;
            this.botToken = botToken;
            this.includeDiscordTokenEnv = includeDiscordTokenEnv;
            this.includePrefixEnv = includePrefixEnv;
        }

        // Getters
        public String getDisplayName() { return displayName; }
        public String getProjectName() { return projectName; }
        public String getGroupId() { return groupId; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public String getMainClassName() { return mainClassName; }
        public String getInstallLocation() { return installLocation; }
        public String getBotType() { return botType; }
        public String getJavaVersion() { return javaVersion; }
        public String getBotToken() { return botToken; }
        public boolean isIncludeDiscordTokenEnv() { return includeDiscordTokenEnv; }
        public boolean isIncludePrefixEnv() { return includePrefixEnv; }


        public Path getFullProjectPath() {
            return Paths.get(installLocation, projectName);
        }
    }
}