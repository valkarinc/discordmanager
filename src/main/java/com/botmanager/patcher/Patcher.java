package com.botmanager.patcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patcher extends Application {

    private static final String VERSION_INFO_URL = "https://raw.githubusercontent.com/valkarinc/discordmanager/main/latest_version.json";
    private static final String APP_JAR_FILENAME = "discord-bot-manager-1.0.0.jar";
    private static final String APP_JAR_PATH = "." + File.separator + "target" + File.separator + APP_JAR_FILENAME;
    private static final String LOCAL_VERSION_FILE = "." + File.separator + "version.txt";
    private static final String TEMP_NEW_JAR_FILENAME = APP_JAR_FILENAME + ".new";

    private Label statusLabel;
    private Label currentVersionLabel;
    private ProgressBar progressBar;
    private Button checkUpdateButton;
    private Button launchButton;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Discord Bot Manager Launcher");

        statusLabel = new Label("Ready.");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10px;");

        currentVersionLabel = new Label("Current App Version: Checking...");
        currentVersionLabel.setStyle("-fx-padding: 5px;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(250);
        progressBar.setVisible(false);

        checkUpdateButton = new Button("Check for Updates");
        checkUpdateButton.setOnAction(e -> checkUpdate());
        checkUpdateButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 150px; -fx-padding: 10px; -fx-background-radius: 5;");

        launchButton = new Button("Launch Discord Bot Manager");
        launchButton.setOnAction(e -> launchApplication());
        launchButton.setDisable(true);
        launchButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 200px; -fx-padding: 10px; -fx-background-radius: 5;");

        VBox controlsBox = new VBox(10, checkUpdateButton, launchButton);
        controlsBox.setAlignment(Pos.CENTER);
        controlsBox.setPadding(new Insets(10));

        VBox statusBox = new VBox(5, currentVersionLabel, statusLabel, progressBar);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(statusBox);
        root.setBottom(controlsBox);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f2f5;");

        Scene scene = new Scene(root, 400, 250);
        primaryStage.setScene(scene);
        primaryStage.show();

        Platform.runLater(this::performInitialChecks);
    }

    private void performInitialChecks() {
        Path tempNewJarPath = Paths.get(TEMP_NEW_JAR_FILENAME);
        Path appJarPath = Paths.get(APP_JAR_PATH);

        if (Files.exists(tempNewJarPath)) {
            statusLabel.setText("Applying pending update...");
            try {
                Files.createDirectories(appJarPath.getParent());
                Files.move(tempNewJarPath, appJarPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                statusLabel.setText("Update applied successfully! Reading version...");
                updateLocalVersionDisplay();
            } catch (IOException e) {
                statusLabel.setText("Error applying update: " + e.getMessage());
                System.err.println("Error applying update: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            updateLocalVersionDisplay();
        }

        if (Files.exists(appJarPath)) {
            launchButton.setDisable(false);
            statusLabel.setText("Ready to launch.");
        } else {
            statusLabel.setText("Discord Bot Manager not found. Please check for updates.");
        }
    }

    private void updateLocalVersionDisplay() {
        String localVersion = getLocalAppVersion();
        if (localVersion.isEmpty()) {
            currentVersionLabel.setText("Current App Version: N/A (Not Found)");
        } else {
            currentVersionLabel.setText("Current App Version: " + localVersion);
        }
    }

    private void checkUpdate() {
        checkUpdateButton.setDisable(true);
        launchButton.setDisable(true);
        statusLabel.setText("Checking for updates...");
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        Task<Optional<VersionInfo>> updateCheckTask = new Task<>() {
            @Override
            protected Optional<VersionInfo> call() throws Exception {
                String remoteVersionJson = fetchRemoteVersionInfo();
                return parseVersionInfo(remoteVersionJson);
            }
        };

        updateCheckTask.setOnSucceeded(event -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            progressBar.setProgress(0);

            Optional<VersionInfo> remoteInfoOpt = updateCheckTask.getValue();

            if (remoteInfoOpt.isEmpty()) {
                statusLabel.setText("Failed to get remote version info. Check network or URL.");
                System.err.println("Failed to get remote version info.");
                checkUpdateButton.setDisable(false);
                if (Files.exists(Paths.get(APP_JAR_PATH))) {
                    launchButton.setDisable(false);
                }
                return;
            }

            VersionInfo remoteInfo = remoteInfoOpt.get();
            String localVersion = getLocalAppVersion();

            if (isNewerVersion(remoteInfo.latestVersion, localVersion)) {
                statusLabel.setText("Update available! Downloading v" + remoteInfo.latestVersion + "...");
                downloadUpdate(remoteInfo);
            } else {
                statusLabel.setText("You are running the latest version: v" + localVersion);
                checkUpdateButton.setDisable(false);
                launchButton.setDisable(false);
            }
        });

        updateCheckTask.setOnFailed(event -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            progressBar.setProgress(0);

            statusLabel.setText("Error during update check: " + event.getSource().getException().getMessage());
            System.err.println("Error during update check: " + event.getSource().getException());
            event.getSource().getException().printStackTrace();
            checkUpdateButton.setDisable(false);
            if (Files.exists(Paths.get(APP_JAR_PATH))) {
                launchButton.setDisable(false);
            }
        });

        new Thread(updateCheckTask).start();
    }

    private void downloadUpdate(VersionInfo versionInfo) {
        String downloadUrl = getDownloadUrlForCurrentOS(versionInfo);

        if (downloadUrl == null || downloadUrl.isEmpty()) {
            statusLabel.setText("No download URL found for your operating system.");
            checkUpdateButton.setDisable(false);
            launchButton.setDisable(false);
            return;
        }

        statusLabel.setText("Downloading update...");
        progressBar.setVisible(true);
        progressBar.setProgress(0);

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Failed to download file. HTTP error code: " + conn.getResponseCode());
                }

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(TEMP_NEW_JAR_FILENAME)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytesRead = 0;
                    long fileSize = conn.getContentLengthLong();

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (fileSize > 0) {
                            updateProgress(totalBytesRead, fileSize);
                        }
                    }
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(downloadTask.progressProperty());

        downloadTask.setOnSucceeded(event -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            progressBar.setProgress(0);

            try {
                Files.write(Paths.get(LOCAL_VERSION_FILE), versionInfo.latestVersion.getBytes());
                statusLabel.setText("Update downloaded! Please restart the launcher to apply.");
                currentVersionLabel.setText("Current App Version: " + versionInfo.latestVersion + " (Update Pending)");
            } catch (IOException e) {
                statusLabel.setText("Update downloaded, but failed to write version file: " + e.getMessage());
                System.err.println("Failed to write local version file after download: " + e.getMessage());
            } finally {
                checkUpdateButton.setDisable(false);
                launchButton.setDisable(true);
            }
        });

        downloadTask.setOnFailed(event -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            progressBar.setProgress(0);

            statusLabel.setText("Error downloading update: " + event.getSource().getException().getMessage());
            System.err.println("Error downloading update: " + event.getSource().getException());
            event.getSource().getException().printStackTrace();
            checkUpdateButton.setDisable(false);
            if (Files.exists(Paths.get(APP_JAR_PATH))) {
                launchButton.setDisable(false);
            }
        });

        new Thread(downloadTask).start();
    }

    private String getLocalAppVersion() {
        try {
            Path versionFilePath = Paths.get(LOCAL_VERSION_FILE);
            if (Files.exists(versionFilePath)) {
                return Files.readAllLines(versionFilePath).get(0).trim();
            }
        } catch (IOException e) {
            System.err.println("Could not read local version file: " + e.getMessage());
        }
        return "";
    }

    private String fetchRemoteVersionInfo() throws IOException {
        URL url = new URL(VERSION_INFO_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                return content.toString();
            }
        } else {
            throw new IOException("Failed to fetch remote version info. HTTP Code: " + responseCode);
        }
    }

    private Optional<VersionInfo> parseVersionInfo(String jsonString) {
        try {
            Pattern versionPattern = Pattern.compile("\"latest_version\"\\s*:\\s*\"([^\"]+)\"");
            Pattern windowsUrlPattern = Pattern.compile("\"download_url_windows\"\\s*:\\s*\"([^\"]+)\"");
            Pattern linuxUrlPattern = Pattern.compile("\"download_url_linux\"\\s*:\\s*\"([^\"]+)\"");
            Pattern macosUrlPattern = Pattern.compile("\"download_url_macos\"\\s*:\\s*\"([^\"]+)\"");

            String latestVersion = extractValue(jsonString, versionPattern);
            String downloadUrlWindows = extractValue(jsonString, windowsUrlPattern);
            String downloadUrlLinux = extractValue(jsonString, linuxUrlPattern);
            String downloadUrlMacos = extractValue(jsonString, macosUrlPattern);

            if (latestVersion == null) {
                System.err.println("Could not find 'latest_version' in JSON: " + jsonString);
                return Optional.empty();
            }

            return Optional.of(new VersionInfo(
                    latestVersion,
                    downloadUrlWindows != null ? downloadUrlWindows : "",
                    downloadUrlLinux != null ? downloadUrlLinux : "",
                    downloadUrlMacos != null ? downloadUrlMacos : ""
            ));
        } catch (Exception e) {
            System.err.println("Error parsing version info JSON: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private String extractValue(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String getDownloadUrlForCurrentOS(VersionInfo versionInfo) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return versionInfo.downloadUrlWindows;
        } else if (osName.contains("mac")) {
            return versionInfo.downloadUrlMacos;
        } else if (osName.contains("linux")) {
            return versionInfo.downloadUrlLinux;
        }
        return null;
    }

    private boolean isNewerVersion(String remoteVersion, String localVersion) {
        if (localVersion == null || localVersion.isEmpty()) {
            return true;
        }

        String[] remoteParts = remoteVersion.split("\\.");
        String[] localParts = localVersion.split("\\.");

        int maxLength = Math.max(remoteParts.length, localParts.length);
        for (int i = 0; i < maxLength; i++) {
            int remotePart = (i < remoteParts.length) ? Integer.parseInt(remoteParts[i]) : 0;
            int localPart = (i < localParts.length) ? Integer.parseInt(localParts[i]) : 0;

            if (remotePart > localPart) {
                return true;
            }
            if (remotePart < localPart) {
                return false;
            }
        }
        return false;
    }

    private void launchApplication() {
        launchButton.setDisable(true);
        statusLabel.setText("Launching Discord Bot Manager...");

        try {
            Path appJarPath = Paths.get(APP_JAR_PATH);
            if (!Files.exists(appJarPath)) {
                statusLabel.setText("Error: Discord Bot Manager JAR not found at " + APP_JAR_PATH);
                launchButton.setDisable(false);
                return;
            }

            String javaCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "javaw" : "java";

            ProcessBuilder pb = new ProcessBuilder(javaCmd, "-jar", APP_JAR_PATH);
            pb.inheritIO();

            Process process = pb.start();
            statusLabel.setText("Discord Bot Manager launched.");
        } catch (IOException e) {
            statusLabel.setText("Failed to launch application: " + e.getMessage());
            System.err.println("Failed to launch application: " + e.getMessage());
            e.printStackTrace();
            launchButton.setDisable(false);
        }
    }

    private static class VersionInfo {
        String latestVersion;
        String downloadUrlWindows;
        String downloadUrlLinux;
        String downloadUrlMacos;

        public VersionInfo(String latestVersion, String downloadUrlWindows, String downloadUrlLinux, String downloadUrlMacos) {
            this.latestVersion = latestVersion;
            this.downloadUrlWindows = downloadUrlWindows;
            this.downloadUrlLinux = downloadUrlLinux;
            this.downloadUrlMacos = downloadUrlMacos;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}