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
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CambioPasswordController {

    @FXML
    private PasswordField txtNuevaPassword;
    @FXML
    private PasswordField txtConfirmarPassword;
    @FXML
    private Label lblMensaje;

    private Usuario usuario;
    private AuthController authController = new AuthController();

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        log.debug("Usuario en cambio password: {}", usuario.getUsername());
    }

    @FXML
    private void handleCambiarPassword() {
        String nuevaPassword = txtNuevaPassword.getText();
        String confirmarPassword = txtConfirmarPassword.getText();

        // Validaciones básicas para todos
        if (nuevaPassword.isEmpty() || confirmarPassword.isEmpty()) {
            mostrarMensaje(" Por favor complete ambos campos", "red");
            log.warn("Campos vacíos");
            return;
        }

        if (nuevaPassword.length() < 8) {
            mostrarMensaje(" La contraseña debe tener al menos 8 caracteres", "red");
            log.warn("Contraseña demasiado corta");
            return;
        }

        // Validaciones de seguridad para TODOS
        if (!nuevaPassword.matches(".*[A-Z].*")) {
            mostrarMensaje(" La contraseña debe contener al menos una mayúscula", "red");
            log.warn("Contraseña sin mayúscula");
            return;
        }

        if (!nuevaPassword.matches(".*[a-z].*")) {
            mostrarMensaje(" La contraseña debe contener al menos una minúscula", "red");
            log.warn("Contraseña sin minúscula");
            return;
        }

        if (!nuevaPassword.matches(".*[0-9].*")) {
            mostrarMensaje(" La contraseña debe contener al menos un número", "red");
            log.warn("Contraseña sin número");
            return;
        }

        // Validaciones extras para admin
        if (usuario.getRol().equals("ADMIN")) {
            if (nuevaPassword.toLowerCase().contains("hospital")) {
                mostrarMensaje(" La contraseña no puede contener 'hospital'", "red");
                log.warn("Contraseña con 'hospital' para admin");
                return;
            }

            if (nuevaPassword.length() < 10) {
                mostrarMensaje(" Para admin, la contraseña debe tener al menos 10 caracteres", "red");
                log.warn("Contraseña admin demasiado corta");
                return;
            }
        }

        // No permitir contraseñas temporales
        if (nuevaPassword.equals("Temp123") || nuevaPassword.equals("hospital123")) {
            mostrarMensaje(" No puede usar las contraseñas temporales", "red");
            log.warn("Contraseña temporal no permitida");
            return;
        }

        if (!nuevaPassword.equals(confirmarPassword)) {
            mostrarMensaje(" Las contraseñas no coinciden", "red");
            log.warn("Contraseñas no coinciden");
            return;
        }

        // Cambiar contraseña
        boolean exito = authController.cambiarPassword(usuario.getUsername(), nuevaPassword);

        if (exito) {
            mostrarMensaje(" Contraseña cambiada exitosamente. Redirigiendo...", "green");
            log.info("Contraseña cambiada exitosamente");

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        abrirVentanaPrincipal();
                    });
                } catch (InterruptedException e) {
                    log.error("Error al redirigir a la ventana principal", e);
                }
            }).start();
        } else {
            mostrarMensaje(" Error al cambiar la contraseña", "red");
            log.error("Error al cambiar la contraseña");
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
            log.error("Error abriendo ventana principal", e);
        }
    }

    private void mostrarMensaje(String mensaje, String color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: " + color + ";");
    }
}