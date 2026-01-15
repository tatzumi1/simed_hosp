//src/main/java/com/PruebaSimed2/controllers/VentanaInicialTriageController.java

package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.utils.SesionUsuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class VentanaInicialTriageController {

    @FXML
    private ComboBox<String> comboCapturista;
    @FXML
    private ComboBox<String> comboTurno;
    @FXML
    private Button btnIniciar, btnSalir;
    @FXML
    private Label lblInfo;

    private boolean esAdmin = false;

    @FXML
    public void initialize() {
        cargarTurnos();
        cargarCapturistas();
        btnIniciar.setOnAction(e -> iniciarCaptura());
        btnSalir.setOnAction(e -> cerrarVentana());
        comboCapturista.getItems().sort(String::compareToIgnoreCase);
    }

    // ─────────────────────────────────────────────
    // CARGAR TURNOS (tabla tblt_cveturno)
    // ─────────────────────────────────────────────
    private void cargarTurnos() {
        try (Connection c = ConexionBD.conectar();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT Turno FROM tblt_cveturno ORDER BY Turno")) {

            List<String> turnos = new ArrayList<>();
            while (rs.next()) {
                turnos.add(rs.getString("Turno"));
            }
            comboTurno.getItems().addAll(turnos);

        } catch (SQLException e) {
            mostrar("Error al cargar turnos: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void cargarCapturistas() {
        try (Connection c = ConexionBD.conectar();
             Statement s = c.createStatement()) {

            // DEBUG: Ver qué nombres completos tenemos
            log.debug("=== DEBUG: NOMBRES COMPLETOS DE CAPTURISTAS ===");
            String debugQuery = "SELECT nombre_completo, username, rol " +
                    "FROM tb_usuarios " +
                    "WHERE rol IN ('TRIAGE', 'JEFATURA_URGENCIAS', 'ADMINISTRATIVO') " +
                    "AND activo = TRUE " +
                    "AND nombre_completo IS NOT NULL";

            ResultSet debugRs = s.executeQuery(debugQuery);
            while (debugRs.next()) {
                log.debug("Nombre completo: " + debugRs.getString("nombre_completo") +
                        " - Username: " + debugRs.getString("username") +
                        " - Rol: " + debugRs.getString("rol"));
            }

            // CONSULTA PRINCIPAL - SOLO el nombre_completo
            String query = "SELECT nombre_completo " +
                    "FROM tb_usuarios " +
                    "WHERE activo = TRUE " +
                    "AND rol IN ('TRIAGE', 'JEFATURA_URGENCIAS', 'ADMINISTRATIVO') " +
                    "AND nombre_completo IS NOT NULL " +
                    "AND nombre_completo != '' " +  // Excluir vacíos
                    "ORDER BY nombre_completo";

            log.debug("=== CARGANDO CAPTURISTAS (SOLO NOMBRES COMPLETOS) ===");
            ResultSet rs = s.executeQuery(query);
            List<String> capturistas = new ArrayList<>();

            int count = 0;
            while (rs.next()) {
                String nombreCompleto = rs.getString("nombre_completo");

                // Solo agregar el nombre completo
                capturistas.add(nombreCompleto);
                log.debug(" Capturista " + (++count) + ": " + nombreCompleto);
            }

            comboCapturista.getItems().clear();
            comboCapturista.getItems().addAll(capturistas);

            log.debug(" Total de capturistas cargados: " + capturistas.size());

            // Si no hay capturistas, mostrar advertencia
            if (capturistas.isEmpty()) {
                log.warn(" ¡NO SE ENCONTRARON CAPTURISTAS! Revisa que los usuarios tengan nombre_completo.");
            }

        } catch (SQLException e) {
            log.error(" Error cargando capturistas: " + e.getMessage());
            mostrar("Error al cargar capturistas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String obtenerUsernamePorNombreCompleto(String nombreCompleto) {
        String sql = "SELECT username FROM tb_usuarios WHERE nombre_completo = ? AND activo = TRUE";

        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, nombreCompleto);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            log.error(" Error obteniendo username: " + e.getMessage());
        }
        return null;
    }

    private void iniciarCaptura() {
        String nombreCompleto = comboCapturista.getValue();
        String turno = comboTurno.getValue();

        if (nombreCompleto == null || turno == null || nombreCompleto.trim().isEmpty()) {
            mostrar("Debe seleccionar capturista y turno para continuar.", Alert.AlertType.WARNING);
            return;
        }

        String sql = """
                SELECT username, rol 
                FROM tb_usuarios 
                WHERE nombre_completo = ? AND activo = TRUE 
                LIMIT 1
                """;

        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, nombreCompleto);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                mostrar("Usuario no encontrado o inactivo.", Alert.AlertType.ERROR);
                return;
            }

            String username = rs.getString("username");
            String rol = rs.getString("rol");

            // INICIAR SESIÓN OFICIAL
            SesionUsuario.getInstance().inicializar(username, rol, -1);

            // AUDITORÍA
            registrarAuditoria(username, "Inicio de sesión en módulo TRIAGE - Turno: " + turno, "tb_urgencias");

            // INFO EN PANTALLA
            lblInfo.setText("Sesión iniciada: " + nombreCompleto + " | Turno: " + turno);

            // ABRIR VENTANA DE REGISTRO
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/VentanaRegistroTriage.fxml"));
            Stage stageActual = (Stage) btnIniciar.getScene().getWindow();
            Stage stageNueva = new Stage();
            stageNueva.setTitle("Registro de Pacientes - TRIAGE");
            stageNueva.setScene(new Scene(loader.load(), 1200, 800));
            stageNueva.setResizable(true);

            VentanaRegistroTriageController controller = loader.getController();
            controller.setTurno(turno);  // ← ¡IMPORTANTE! PASA EL TURNO SELECCIONADO

            stageNueva.show();
            stageActual.close();

        } catch (SQLException e) {
            mostrar("Error de base de datos: " + e.getMessage(), Alert.AlertType.ERROR);
            log.error("Error de base de datos: {}", e.getMessage(), e);
        } catch (Exception e) {
            mostrar("Error al abrir ventana de registro: " + e.getMessage(), Alert.AlertType.ERROR);
            log.error("Error al abrir ventana de registro: {}", e.getMessage(), e);
        }
    }

    private void registrarAuditoria(String usuario, String accion, String tablaAfectada) {
        String sql = "INSERT INTO tb_auditoria (username, accion, tabla_afectada, fecha_hora) " +
                "VALUES (?, ?, ?, NOW())";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ps.setString(2, accion);
            ps.setString(3, tablaAfectada);
            ps.executeUpdate();
            log.info(" Auditoría registrada: {} por {}", accion, usuario);

        } catch (SQLException e) {
            log.error(" Error al registrar auditoría: {}", e.getMessage());
        }
    }


    private void cerrarVentana() {
        Stage stage = (Stage) btnSalir.getScene().getWindow();
        stage.close();
    }

    private void mostrar(String mensaje, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, mensaje);
        a.showAndWait();
    }

    // Llamar esta función antes de mostrar la ventana para identificar tipo de usuario
    public void setAdmin(boolean admin) {
        this.esAdmin = admin;
    }
}