package com.botmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Main JavaFX Application class for Discord Bot Manager
 *
 * @author valkarinc
 * @version 1.0.0
 */
public class DiscordBotManagerApp extends Application {

    private static final String APP_TITLE = "Discord Bot Manager";
    private static final double MIN_WIDTH = 1000;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) {
        try {

            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("/fxml/main.fxml")
            );

            Scene scene = new Scene(fxmlLoader.load(), MIN_WIDTH, MIN_HEIGHT);

            scene.getStylesheets().add(
                    getClass().getResource("/css/styles.css").toExternalForm()
            );

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);

            try (InputStream iconStream = getClass().getResourceAsStream("/images/bot-icon.png")) {
                if (iconStream != null) {
                    primaryStage.getIcons().add(new Image(iconStream));
                }
            } catch (IOException e) {
                System.out.println("Could not load application icon: " + e.getMessage());
            }

            primaryStage.setOnCloseRequest(event -> {
                // TODO: Stop all running bots before closing
                System.out.println("Application closing...");
                System.exit(0);
            });

            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Failed to load application: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}