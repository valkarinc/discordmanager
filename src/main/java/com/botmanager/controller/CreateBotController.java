package com.botmanager.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Controller for the "Create New Discord Bot Project" dialog.
 * Handles user input for new bot details and triggers project generation.
 */
public class CreateBotController {

    @FXML private DialogPane createBotDialogPane;
    @FXML private TextField botDisplayNameField;
    @FXML private TextField projectNameField;
    @FXML private TextField groupIdField;
    @FXML private TextField botVersionField;
    @FXML private TextArea botDescriptionArea;
    @FXML private TextField mainClassNameField;
    @FXML private TextField installLocationField;
    @FXML private Button browseLocationButton;
    @FXML private ComboBox<String> botTypeComboBox;
    @FXML private Label errorLabel;

    // Button types defined in FXML, but we need to reference them
    // to configure validation later.
    private Button createButton;

    // Data to be returned by the dialog
    private BotCreationDetails resultDetails;

    // --- Initialization ---

    @FXML
    public void initialize() {
        // Set default values
        botVersionField.setText("1.0.0");
        groupIdField.setText("com.mycompany.discordbot");
        mainClassNameField.setText("Main");
        botTypeComboBox.getSelectionModel().selectFirst(); // Select JDA Basic by default

        // Add listeners for auto-filling project name and main class based on display name
        botDisplayNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal == null || oldVal.isEmpty() || projectNameField.getText().equalsIgnoreCase(cleanForProjectName(oldVal))) {
                projectNameField.setText(cleanForProjectName(newVal));
            }
            if (oldVal == null || oldVal.isEmpty() || mainClassNameField.getText().equalsIgnoreCase(cleanForClassName(oldVal))) {
                mainClassNameField.setText(cleanForClassName(newVal) + "Main"); // Suggest MyBotMain
            }
        });

        // Get access to the Create Bot button and add validation
        // This needs to be done after the dialog is shown, or via a specific method
        // called from the parent controller *after* the dialog is constructed.
        // For DialogPane, you can often do this:
        createBotDialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                createButton = (Button) createBotDialogPane.lookupButton(ButtonType.OK);
                if (createButton != null) {
                    createButton.addEventFilter(ActionEvent.ACTION, event -> {
                        if (!validateInput()) {
                            event.consume(); // Prevent dialog from closing
                        }
                    });
                }
            }
        });

        // Initial validation state
        validateInput();
    }

    /**
     * Cleans a string for use as a Maven artifactId (project name).
     * Converts to lowercase, replaces spaces with hyphens, removes special characters.
     */
    private String cleanForProjectName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Allow only a-z, 0-9, spaces, hyphens
                .replaceAll("\\s+", "-")       // Replace spaces with hyphens
                .replaceAll("^-|-$", "");      // Remove leading/trailing hyphens
    }

    /**
     * Cleans a string for use as a Java class name.
     * Removes non-alphanumeric characters, capitalizes first letter of words.
     */
    private String cleanForClassName(String name) {
        if (name == null) return "";
        String cleaned = name.replaceAll("[^a-zA-Z0-9\\s]", ""); // Keep only alphanumeric and spaces
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


    // --- Event Handlers ---

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
            validateInput(); // Re-validate after changing location
        }
    }

    // --- Validation and Data Retrieval ---

    /**
     * Validates user input before allowing bot creation.
     * Updates errorLabel and disables/enables the Create button.
     * @return true if input is valid, false otherwise.
     */
    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();

        // Validate Bot Display Name
        if (botDisplayNameField.getText() == null || botDisplayNameField.getText().trim().isEmpty()) {
            errorMessage.append("- Bot Display Name is required.\n");
            botDisplayNameField.setStyle("-fx-border-color: red;");
        } else {
            botDisplayNameField.setStyle("");
        }

        // Validate Project Name
        if (projectNameField.getText() == null || projectNameField.getText().trim().isEmpty()) {
            errorMessage.append("- Project Name (ArtifactId) is required.\n");
            projectNameField.setStyle("-fx-border-color: red;");
        } else if (!projectNameField.getText().matches("^[a-z0-9-]+$")) {
            errorMessage.append("- Project Name must be lowercase alphanumeric with hyphens.\n");
            projectNameField.setStyle("-fx-border-color: red;");
        } else {
            projectNameField.setStyle("");
        }

        // Validate Group ID
        if (groupIdField.getText() == null || groupIdField.getText().trim().isEmpty()) {
            errorMessage.append("- Group ID (Package) is required.\n");
            groupIdField.setStyle("-fx-border-color: red;");
        } else if (!groupIdField.getText().matches("^[a-zA-Z0-9.]+$")) {
            errorMessage.append("- Group ID must be alphanumeric with dots.\n");
            groupIdField.setStyle("-fx-border-color: red;");
        } else {
            groupIdField.setStyle("");
        }

        // Validate Main Class Name
        if (mainClassNameField.getText() == null || mainClassNameField.getText().trim().isEmpty()) {
            errorMessage.append("- Main Class Name is required.\n");
            mainClassNameField.setStyle("-fx-border-color: red;");
        } else if (!mainClassNameField.getText().matches("^[a-zA-Z_$][a-zA-Z0-9_$]*$")) { // Basic Java identifier regex
            errorMessage.append("- Main Class Name is not a valid Java identifier.\n");
            mainClassNameField.setStyle("-fx-border-color: red;");
        } else {
            mainClassNameField.setStyle("");
        }

        // Validate Install Location
        if (installLocationField.getText() == null || installLocationField.getText().trim().isEmpty()) {
            errorMessage.append("- Install Location is required.\n");
            installLocationField.setStyle("-fx-border-color: red;");
        } else {
            Path chosenPath = Paths.get(installLocationField.getText());
            Path projectPath = chosenPath.resolve(projectNameField.getText()); // Full path to new project dir
            if (Files.exists(projectPath)) {
                errorMessage.append("- Project directory '").append(projectPath.getFileName()).append("' already exists at this location.\n");
                installLocationField.setStyle("-fx-border-color: red;");
            } else {
                installLocationField.setStyle("");
            }
        }

        // Validate Bot Type
        if (botTypeComboBox.getSelectionModel().isEmpty()) {
            errorMessage.append("- Bot Type/Framework is required.\n");
            botTypeComboBox.setStyle("-fx-border-color: red;");
        } else {
            botTypeComboBox.setStyle("");
        }

        boolean isValid = errorMessage.isEmpty();
        errorLabel.setText(errorMessage.toString());
        errorLabel.setVisible(!isValid);
        errorLabel.setManaged(!isValid);

        // This assumes 'createButton' is accessible after the dialog is shown.
        // It's safer to configure this logic in the parent controller when showing the dialog.
        if (createButton != null) {
            createButton.setDisable(!isValid);
        }
        return isValid;
    }

    /**
     * Called when the dialog's result is processed.
     * Returns the collected bot creation details if input is valid.
     */
    public BotCreationDetails getResult() {
        if (validateInput()) {
            resultDetails = new BotCreationDetails(
                    botDisplayNameField.getText().trim(),
                    projectNameField.getText().trim(),
                    groupIdField.getText().trim(),
                    botVersionField.getText().trim(),
                    botDescriptionArea.getText().trim(),
                    mainClassNameField.getText().trim(),
                    installLocationField.getText().trim(),
                    botTypeComboBox.getSelectionModel().getSelectedItem()
            );
            return resultDetails;
        }
        return null;
    }

    // --- Helper Class for Dialog Result ---

    public static class BotCreationDetails {
        private final String displayName;
        private final String projectName; // artifactId
        private final String groupId;
        private final String version;
        private final String description;
        private final String mainClassName;
        private final String installLocation;
        private final String botType;

        public BotCreationDetails(String displayName, String projectName, String groupId, String version, String description, String mainClassName, String installLocation, String botType) {
            this.displayName = displayName;
            this.projectName = projectName;
            this.groupId = groupId;
            this.version = version;
            this.description = description;
            this.mainClassName = mainClassName;
            this.installLocation = installLocation;
            this.botType = botType;
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

        public Path getFullProjectPath() {
            return Paths.get(installLocation, projectName);
        }
    }
}