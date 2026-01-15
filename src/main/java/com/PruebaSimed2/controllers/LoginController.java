// src/main/java/com/PruebaSimed2/controllers/LoginController.java
package com.PruebaSimed2.controllers;

import com.PruebaSimed2.models.Usuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LoginController {
    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblMensaje;

    private AuthController authController = new AuthController();

    @FXML
    public void initialize() {
        // Limpiar mensajes al iniciar
        lblMensaje.setText("");

        // Opcional: Auto-enfocar el campo usuario
        txtUsuario.requestFocus();
    }

    @FXML
    private void handleLogin() {
        log.debug(" Botón de login presionado!");
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();
        log.debug(" Usuario escrito: {}", usuario);
        log.debug(" Password escrito: {} caracteres", password.length());

        // Validar campos vacíos
        if (usuario.isEmpty() || password.isEmpty()) {
            log.debug(" Campos vacíos detectados");
            lblMensaje.setText(" Por favor complete todos los campos");
            lblMensaje.setStyle("-fx-text-fill: orange;");
            return;
        }

        log.debug(" Llamando a AuthController.login()...");

        // Intentar login
        Usuario usuarioLogueado = authController.login(usuario, password);

        if (usuarioLogueado != null) {
            //  LÍNEAS DE DEBUG AGREGADAS AQUÍ
            log.debug(" Login exitoso! ");
            log.debug(" Datos usuario: {}", usuarioLogueado.getUsername());
            log.debug(" Rol usuario: {}", usuarioLogueado.getRol());
            log.debug(" Primer login: {}", usuarioLogueado.isPrimerLogin());

            lblMensaje.setText(" ¡Login exitoso! Bienvenido " + usuario);
            lblMensaje.setStyle("-fx-text-fill: green;");

            if (usuarioLogueado.isPrimerLogin()) {
                // Redirigir a cambio de contraseña obligatorio
                redirigirACambioPassword(usuarioLogueado);
            } else {
                // Ir directamente al sistema
                redirigirAVentanaPrincipal(usuarioLogueado);
            }

        } else {
            log.debug(" Login falló en el controlador");
            lblMensaje.setText(" Usuario o contraseña incorrectos");
            lblMensaje.setStyle("-fx-text-fill: red;");

            // Limpiar campos
            txtPassword.clear();
            txtUsuario.requestFocus();
        }
    }

    private void redirigirACambioPassword(Usuario usuario) {
        try {
            //  CORREGIDO: usar usuario.getId() en lugar de usuario.getIdUsuario()
            com.PruebaSimed2.utils.SesionUsuario sesion = com.PruebaSimed2.utils.SesionUsuario.getInstance();
            sesion.inicializar(usuario.getUsername(), usuario.getRol(), usuario.getId());

            log.debug(" Sesión inicializada para cambio de password: {}", usuario.getUsername());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/cambioPassword.fxml"));
            Parent root = loader.load();

            // Pasar el usuario al controlador de cambio de password
            CambioPasswordController controller = loader.getController();
            controller.setUsuario(usuario);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 500, 400));
            stage.setTitle("Cambio de Contraseña Obligatorio");
            stage.setResizable(false);
            stage.show();

            // Cerrar ventana de login
            Stage currentStage = (Stage) txtUsuario.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            log.error(" Error abriendo ventana de cambio de password: {}", e.getMessage());
            log.error("StackTrace:", e);

            // Si falla, intentar abrir ventana principal como respaldo
            redirigirAVentanaPrincipal(usuario);
        }
    }

    private void redirigirAVentanaPrincipal(Usuario usuario) {
        try {
            //  CORREGIDO: usar usuario.getId() en lugar de usuario.getIdUsuario()
            com.PruebaSimed2.utils.SesionUsuario sesion = com.PruebaSimed2.utils.SesionUsuario.getInstance();
            sesion.inicializar(usuario.getUsername(), usuario.getRol(), usuario.getId());

            log.debug(" Sesión inicializada para: {} - Rol: {}", usuario.getUsername(), usuario.getRol());

            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("views/MainWindow.fxml"));
            Parent root = loader.load();

            // Pasar el usuario al MainController
            MainController mainController = loader.getController();
            mainController.setUsuarioLogueado(usuario);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 1000, 700);

            // Cargar estilos CSS
            scene.getStylesheets().add(getClass().getResource("/styles/mainStyles.css").toExternalForm());

            stage.setTitle("Sistema SIMED - Módulo de Atención Médica - Usuario: " + usuario.getUsername());
            stage.setScene(scene);
            stage.initStyle(javafx.stage.StageStyle.DECORATED);
            stage.show();

            // Cerrar ventana de login
            Stage currentStage = (Stage) txtUsuario.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            log.error(" Error abriendo ventana principal: {}", e.getMessage());
            log.error("StackTrace:", e);
            // Si falla, intentar abrir ventana principal como respaldo
            redirigirAVentanaPrincipal(usuario);
        }
    }

    // Método para manejar la tecla Enter
    @FXML
    private void handleEnter(javafx.scene.input.KeyEvent event) {
        if (event.getCode().toString().equals("ENTER")) {
            handleLogin();
        }
    }
}