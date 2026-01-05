//src/main/java/com/PruebaSimed2/controllers/CambioPasswordController.java

package com.PruebaSimed2.controllers;

import com.PruebaSimed2.models.Usuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class CambioPasswordController {

    @FXML private PasswordField txtNuevaPassword;
    @FXML private PasswordField txtConfirmarPassword;
    @FXML private Label lblMensaje;

    private Usuario usuario;
    private AuthController authController = new AuthController();

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        System.out.println(" Usuario en cambio password: " + usuario.getUsername());
    }

    @FXML
    private void handleCambiarPassword() {
        String nuevaPassword = txtNuevaPassword.getText();
        String confirmarPassword = txtConfirmarPassword.getText();

        // Validaciones básicas para todos
        if (nuevaPassword.isEmpty() || confirmarPassword.isEmpty()) {
            mostrarMensaje(" Por favor complete ambos campos", "red");
            return;
        }

        if (nuevaPassword.length() < 8) {
            mostrarMensaje(" La contraseña debe tener al menos 8 caracteres", "red");
            return;
        }

        // Validaciones de seguridad para TODOS
        if (!nuevaPassword.matches(".*[A-Z].*")) {
            mostrarMensaje(" La contraseña debe contener al menos una mayúscula", "red");
            return;
        }

        if (!nuevaPassword.matches(".*[a-z].*")) {
            mostrarMensaje(" La contraseña debe contener al menos una minúscula", "red");
            return;
        }

        if (!nuevaPassword.matches(".*[0-9].*")) {
            mostrarMensaje(" La contraseña debe contener al menos un número", "red");
            return;
        }

        // Validaciones extras para admin
        if (usuario.getRol().equals("ADMIN")) {
            if (nuevaPassword.toLowerCase().contains("hospital")) {
                mostrarMensaje(" La contraseña no puede contener 'hospital'", "red");
                return;
            }

            if (nuevaPassword.length() < 10) {
                mostrarMensaje(" Para admin, la contraseña debe tener al menos 10 caracteres", "red");
                return;
            }
        }

        // No permitir contraseñas temporales
        if (nuevaPassword.equals("Temp123") || nuevaPassword.equals("hospital123")) {
            mostrarMensaje(" No puede usar las contraseñas temporales", "red");
            return;
        }

        if (!nuevaPassword.equals(confirmarPassword)) {
            mostrarMensaje(" Las contraseñas no coinciden", "red");
            return;
        }

        // Cambiar contraseña
        boolean exito = authController.cambiarPassword(usuario.getUsername(), nuevaPassword);

        if (exito) {
            mostrarMensaje(" Contraseña cambiada exitosamente. Redirigiendo...", "green");

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        abrirVentanaPrincipal();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            mostrarMensaje(" Error al cambiar la contraseña", "red");
        }
    }

    private void abrirVentanaPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mainWindow.fxml"));
            Parent root = loader.load();

            // Pasar el usuario al MainController
            MainController mainController = loader.getController();
            mainController.setUsuarioLogueado(usuario);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Sistema SIMED - Módulo de Atención Médica");
            stage.show();

            // Cerrar ventana actual
            Stage currentStage = (Stage) txtNuevaPassword.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            System.err.println(" Error abriendo ventana principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarMensaje(String mensaje, String color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: " + color + ";");
    }
}