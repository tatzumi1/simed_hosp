// src/main/java/com/PruebaSimed2/Main.java
package com.PruebaSimed2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.io.InputStream;
import java.net.URL;

@Log4j2
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ... código de conexión igual ...

        log.debug("Cargando ventana de login...");

        // SOLUCIÓN CORREGIDA:
        InputStream fxmlStream = getClass().getClassLoader().getResourceAsStream("views/Login.fxml");
        if (fxmlStream == null) {
            log.error("No se encontró views/Login.fxml");
            throw new RuntimeException("No se encontró views/Login.fxml");
        }

        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(fxmlStream);
        primaryStage.setTitle("Sistema SIMED - login");
        primaryStage.setScene(new Scene(root, 900, 550));
        primaryStage.setResizable(false);
        primaryStage.show();

        log.info("Aplicación iniciada correctamente");
    }

    private void showError(String mensaje) {
        log.error(mensaje);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Conexión");
        alert.setHeaderText("Error de conexión a la base de datos");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}