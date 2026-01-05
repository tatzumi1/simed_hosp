// src/main/java/com/PruebaSimed2/Main.java
package com.PruebaSimed2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ... código de conexión igual ...

        System.out.println("Cargando ventana de login...");

        // SOLUCIÓN CORREGIDA:
        InputStream fxmlStream = getClass().getClassLoader().getResourceAsStream("views/Login.fxml");
        if (fxmlStream == null) {
            throw new RuntimeException("No se encontró views/Login.fxml");
        }

        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(fxmlStream);
        primaryStage.setTitle("Sistema SIMED - login");
        primaryStage.setScene(new Scene(root, 900, 550));
        primaryStage.setResizable(false);
        primaryStage.show();

        System.out.println("Aplicación iniciada correctamente");
    }

    private void showError(String mensaje) {
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