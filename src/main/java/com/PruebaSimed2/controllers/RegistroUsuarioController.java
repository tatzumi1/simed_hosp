//src/main/java/com/PruebaSimed2/controllers/RegistroUsuarioController.java


package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.Usuario;
import com.PruebaSimed2.utils.PasswordUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.sql.*;

@Log4j2
public class RegistroUsuarioController {

    @FXML
    private TextField txtNombreCompleto;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtUsername;
    @FXML
    private ComboBox<String> cbRol;
    @FXML
    private Label lblMensaje;
    @FXML
    private VBox vboxDatosMedico;
    @FXML
    private TextField txtCedulaProfesional;
    @FXML
    private VBox vboxUniversidad;
    @FXML
    private TextField txtUniversidad;
    @FXML
    private VBox vboxJefaturaUrgencias;
    @FXML
    private TextField txtCedulaJefatura;


    private Usuario usuarioAdmin;

    public void setUsuarioAdmin(Usuario usuario) {
        if (usuario != null) {
            this.usuarioAdmin = usuario;
            log.debug(" Admin registrando: {}", usuario.getUsername());
        } else {
            log.debug(" Usuario admin es NULL - usando admin por defecto");
            this.usuarioAdmin = new Usuario();
            this.usuarioAdmin.setUsername("admin_sistema");
            this.usuarioAdmin.setRol("ADMIN");
        }
    }

    @FXML
    public void initialize() {
        // Llenar combobox de roles
        cbRol.setItems(FXCollections.observableArrayList(
                "MEDICO_URGENCIAS",
                "MEDICO_ESPECIALISTA",
                "TRIAGE",
                "JEFATURA_URGENCIAS",
                "ADMINISTRATIVO",
                "TRABAJADOR_SOCIAL"
        ));

        cbRol.valueProperty().addListener((observable, oldValue, newValue) -> {
            boolean esMedicoUrgencias = newValue != null && newValue.equals("MEDICO_URGENCIAS");
            boolean esMedicoEspecialista = newValue != null && newValue.equals("MEDICO_ESPECIALISTA");
            boolean esJefaturaUrgencias = newValue != null && newValue.equals("JEFATURA_URGENCIAS");

            boolean esMedico = esMedicoUrgencias || esMedicoEspecialista;

            vboxDatosMedico.setVisible(esMedico);
            vboxDatosMedico.setManaged(esMedico);

            // NUEVO: Mostrar campo universidad solo para médico especialista
            vboxUniversidad.setVisible(esMedicoEspecialista);
            vboxUniversidad.setManaged(esMedicoEspecialista);

            // NUEVO: Mostrar campo cédula para jefatura de urgencias
            vboxJefaturaUrgencias.setVisible(esJefaturaUrgencias);
            vboxJefaturaUrgencias.setManaged(esJefaturaUrgencias);

            if (!esMedico && !esJefaturaUrgencias) {
                txtCedulaProfesional.clear();
                txtUniversidad.clear();
                txtCedulaJefatura.clear();
            }
        });

        lblMensaje.setText("Complete el formulario para registrar nuevo usuario");
    }

    private boolean registrarUsuarioCompleto(String nombreCompleto, String email, String username,
                                             String rol, String cedula, String universidad, String cedulaJefatura) {
        Connection conn = null;
        try {
            conn = ConexionBD.conectar();
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. PRIMERO registrar en tb_usuarios
            String sqlUsuario = "INSERT INTO tb_usuarios (username, password_hash, email, nombre_completo, rol, cedula_profesional, activo, primer_login) " +
                    "VALUES (?, ?, ?, ?, ?, ?, true, true)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                // Contraseña temporal: "Temp123"
                String passwordTemp = "Temp123";
                String hash = PasswordUtils.hashPassword(passwordTemp);

                pstmt.setString(1, username);
                pstmt.setString(2, hash);
                pstmt.setString(3, email);
                pstmt.setString(4, nombreCompleto);
                pstmt.setString(5, rol);

                // NUEVO: Guardar cédula en usuarios para jefatura y médicos
                if (rol.equals("JEFATURA_URGENCIAS")) {
                    pstmt.setString(6, cedulaJefatura);
                } else if (rol.equals("MEDICO_URGENCIAS") || rol.equals("MEDICO_ESPECIALISTA")) {
                    pstmt.setString(6, cedula);
                } else {
                    pstmt.setNull(6, java.sql.Types.VARCHAR);
                }

                pstmt.executeUpdate();

                // Obtener el ID generado (opcional, para auditoría)
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int idUsuario = generatedKeys.getInt(1);
                        log.debug("Usuario registrado con ID: {}", idUsuario);
                    }
                }

                // 2. REGISTRAR EN TABLAS ESPECÍFICAS SEGÚN ROL
                boolean esMedicoUrgencias = rol.equals("MEDICO_URGENCIAS");
                boolean esMedicoEspecialista = rol.equals("MEDICO_ESPECIALISTA");
                boolean esJefaturaUrgencias = rol.equals("JEFATURA_URGENCIAS");

                if (esMedicoUrgencias) {
                    // Guardar en tb_medicos (SOLO médico urgencias)
                    int nuevaCveMed = generarNuevaCveMed(conn, "tb_medicos");
                    log.debug("DEBUG: Insertando en tb_medicos con ID: {}, Nombre: {}, Cédula: {}", nuevaCveMed, nombreCompleto, cedula);

                    String sqlMedico = "INSERT INTO tb_medicos (Cve_med, Med_nombre, Ced_prof) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmtMedico = conn.prepareStatement(sqlMedico)) {
                        pstmtMedico.setInt(1, nuevaCveMed);
                        pstmtMedico.setString(2, nombreCompleto); // Nombre completo para combobox
                        pstmtMedico.setString(3, cedula);
                        pstmtMedico.executeUpdate();
                        log.info("Registrado en tb_medicos: {}", nombreCompleto);
                    }
                } else if (esJefaturaUrgencias) {
                    // Guardar en tb_medicos como JEFATURA
                    int nuevaCveMed = generarNuevaCveMed(conn, "tb_medicos");
                    log.debug("DEBUG: Insertando JEFATURA en tb_medicos con ID: {}, Nombre: {}, Cédula: {}", nuevaCveMed, nombreCompleto, cedulaJefatura);

                    String sqlMedico = "INSERT INTO tb_medicos (Cve_med, Med_nombre, Ced_prof) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmtMedico = conn.prepareStatement(sqlMedico)) {
                        pstmtMedico.setInt(1, nuevaCveMed);
                        pstmtMedico.setString(2, nombreCompleto);
                        pstmtMedico.setString(3, cedulaJefatura);
                        pstmtMedico.executeUpdate();
                        log.info("Jefatura registrada en tb_medicos: {}", nombreCompleto);
                    }
                } else if (esMedicoEspecialista) {
                    // Guardar en tb_medesp con universidad
                    int nuevaCveMed = generarNuevaCveMed(conn, "tb_medesp");
                    log.debug("DEBUG: Insertando en tb_medesp con ID: {}, Nombre: {}, Cédula: {}, Universidad: {}", nuevaCveMed, nombreCompleto, cedula, universidad);

                    String sqlMedEsp = "INSERT INTO tb_medesp (Cve_med, Nombre, Cedula, universidad) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmtMedEsp = conn.prepareStatement(sqlMedEsp)) {
                        pstmtMedEsp.setInt(1, nuevaCveMed);
                        pstmtMedEsp.setString(2, nombreCompleto); // Nombre completo para combobox
                        pstmtMedEsp.setString(3, cedula);
                        pstmtMedEsp.setString(4, universidad);
                        pstmtMedEsp.executeUpdate();
                        log.info("Médico especialista registrado en tb_medesp: {} - Universidad: {}", nombreCompleto, universidad);
                    }
                }

                conn.commit(); // Confirmar toda la transacción

                // Registrar en auditoría
                String adminName = (usuarioAdmin != null && usuarioAdmin.getUsername() != null) ?
                        usuarioAdmin.getUsername() : "admin_sistema";
                registrarAuditoria("REGISTRO_USUARIO",
                        "Nuevo usuario: " + username + " - Nombre: " + nombreCompleto + " - Rol: " + rol + " por: " + adminName);

                return true;
            }

        } catch (SQLException e) {
            log.error("ERROR SQL registrando usuario: {}", e.getMessage(), e);
            if (conn != null) {
                try {
                    conn.rollback(); // Revertir en caso de error
                    log.error("Transacción revertida debido a error");
                } catch (SQLException ex) {
                    log.error("Error al revertir transacción: {}", ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error al cerrar conexión: {}", e.getMessage());
                }
            }
        }
        return false;
    }

    // Método adicional para verificar cédulas DUPLICADAS antes de insertar
    private boolean cedulaExiste(String cedula, String rol) {
        String sql = "";

        if (rol.equals("MEDICO_URGENCIAS") || rol.equals("JEFATURA_URGENCIAS")) {
            sql = "SELECT COUNT(*) as existe FROM tb_medicos WHERE Ced_prof = ?";
        } else if (rol.equals("MEDICO_ESPECIALISTA")) {
            sql = "SELECT COUNT(*) as existe FROM tb_medesp WHERE Cedula = ?";
        } else {
            return false; // No aplica para otros roles
        }

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cedula);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                boolean existe = rs.getInt("existe") > 0;
                if (existe) {
                    log.error("ADVERTENCIA: La cédula {} ya existe para rol {}", cedula, rol);
                }
                return existe;
            }

        } catch (SQLException e) {
            log.error("Error verificando cédula: {}", e.getMessage());
        }
        return false;
    }

    // Método para generar nueva Cve_med automáticamente BUSCANDO HUECOS
    private int generarNuevaCveMed(Connection conn, String tabla) throws SQLException {
        String sql = "";

        if (tabla.equals("tb_medicos")) {
            // BUSCAR PRIMER ID DISPONIBLE en tb_medicos (incluyendo huecos)
            sql = "SELECT MIN(t1.Cve_med + 1) as nuevo_id " +
                    "FROM tb_medicos t1 " +
                    "LEFT JOIN tb_medicos t2 ON t2.Cve_med = t1.Cve_med + 1 " +
                    "WHERE t2.Cve_med IS NULL " +
                    "UNION " +
                    "SELECT 1 as nuevo_id " +
                    "WHERE NOT EXISTS (SELECT 1 FROM tb_medicos WHERE Cve_med = 1) " +
                    "ORDER BY nuevo_id " +
                    "LIMIT 1";
        } else if (tabla.equals("tb_medesp")) {
            // BUSCAR PRIMER ID DISPONIBLE en tb_medesp (incluyendo huecos)
            sql = "SELECT MIN(t1.Cve_med + 1) as nuevo_id " +
                    "FROM tb_medesp t1 " +
                    "LEFT JOIN tb_medesp t2 ON t2.Cve_med = t1.Cve_med + 1 " +
                    "WHERE t2.Cve_med IS NULL " +
                    "UNION " +
                    "SELECT 1 as nuevo_id " +
                    "WHERE NOT EXISTS (SELECT 1 FROM tb_medesp WHERE Cve_med = 1) " +
                    "ORDER BY nuevo_id " +
                    "LIMIT 1";
        } else {
            // Para tablas que no son médicos (aunque no debería usarse)
            sql = "SELECT COALESCE(MAX(Cve_med), 0) + 1 as nuevo_id FROM " + tabla;
        }

        log.debug("DEBUG: Buscando ID disponible para tabla: {}", tabla);
        log.debug("DEBUG: SQL: {}", sql);

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int nuevoId = rs.getInt("nuevo_id");
                log.debug("DEBUG: ID encontrado: {} para tabla: {}", nuevoId, tabla);

                // VERIFICAR EXTRA: que realmente no exista
                String verificarSql = "SELECT COUNT(*) as existe FROM " + tabla + " WHERE Cve_med = ?";
                try (PreparedStatement verificarPstmt = conn.prepareStatement(verificarSql)) {
                    verificarPstmt.setInt(1, nuevoId);
                    try (ResultSet verificarRs = verificarPstmt.executeQuery()) {
                        if (verificarRs.next() && verificarRs.getInt("existe") > 0) {
                            log.error("ERROR: El ID {} ya existe en {}. Buscando siguiente...", nuevoId, tabla);
                            // Si por alguna rareza ya existe, buscar el siguiente
                            return generarSiguienteIdManual(conn, tabla, nuevoId);
                        }
                    }
                }

                return nuevoId;
            }

            log.debug("DEBUG: No se encontró ID, usando 1");
            return 1; // Si no hay registros, empezar en 1
        }
    }

    // Método auxiliar si falla la búsqueda automática
    private int generarSiguienteIdManual(Connection conn, String tabla, int idInicial) throws SQLException {
        int id = idInicial + 1;
        int maxIntentos = 1000; // Por seguridad

        for (int i = 0; i < maxIntentos; i++) {
            String verificarSql = "SELECT COUNT(*) as existe FROM " + tabla + " WHERE Cve_med = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(verificarSql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt("existe") == 0) {
                        log.debug("DEBUG: Encontrado ID manual: {}", id);
                        return id;
                    }
                }
            }
            id++;
        }

        // Si después de 1000 intentos no encuentra, usar máximo + 1
        String sqlMax = "SELECT COALESCE(MAX(Cve_med), 0) + 1 as max_id FROM " + tabla;
        try (PreparedStatement pstmt = conn.prepareStatement(sqlMax);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("max_id");
            }
        }

        return 1;
    }

    // nuevo

    private void registrarAuditoria(String accion, String detalles) {
        // Aquí puedes implementar tu sistema de auditoría
        log.debug(" AUDITORÍA - {}: {}", accion, detalles);
    }


    @FXML
    private void handleVolver() {
        Stage stage = (Stage) txtUsername.getScene().getWindow();
        stage.close();
    }

    private void limpiarFormulario() {
        txtNombreCompleto.clear();
        txtEmail.clear();
        txtUsername.clear();
        txtCedulaProfesional.clear(); // Este ya existe
        txtUniversidad.clear(); // NUEVO
        txtCedulaJefatura.clear(); // NUEVO
        cbRol.setValue(null);
        vboxDatosMedico.setVisible(false); // Este ya existe
        vboxUniversidad.setVisible(false); // NUEVO
        vboxJefaturaUrgencias.setVisible(false); // NUEVO
    }

    private void mostrarMensaje(String mensaje, String color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: " + color + ";");
    }

    @FXML
    private void handleRegistrarUsuario() {
        String nombreCompleto = txtNombreCompleto.getText().trim();
        String email = txtEmail.getText().trim();
        String username = txtUsername.getText().trim();
        String rol = cbRol.getValue();
        String cedula = txtCedulaProfesional.getText().trim();
        String universidad = txtUniversidad.getText().trim();
        String cedulaJefatura = txtCedulaJefatura.getText().trim();

        // Validaciones básicas
        if (nombreCompleto.isEmpty() || email.isEmpty() || username.isEmpty() || rol == null) {
            mostrarMensaje("Por favor complete todos los campos", "red");
            return;
        }

        // Validaciones específicas para médicos
        boolean esMedicoUrgencias = rol.equals("MEDICO_URGENCIAS");
        boolean esMedicoEspecialista = rol.equals("MEDICO_ESPECIALISTA");
        boolean esJefaturaUrgencias = rol.equals("JEFATURA_URGENCIAS");

        if ((esMedicoUrgencias || esMedicoEspecialista) && cedula.isEmpty()) {
            mostrarMensaje("La cédula profesional es obligatoria para médicos", "red");
            return;
        }

        // Validación: universidad obligatoria para médico especialista
        if (esMedicoEspecialista && universidad.isEmpty()) {
            mostrarMensaje("La universidad es obligatoria para médico especialista", "red");
            return;
        }

        // Validación: cédula obligatoria para jefatura de urgencias
        if (esJefaturaUrgencias && cedulaJefatura.isEmpty()) {
            mostrarMensaje("La cédula profesional es obligatoria para jefatura de urgencias", "red");
            return;
        }

        // NUEVO: Verificar si la cédula ya existe
        if (esMedicoUrgencias || esMedicoEspecialista || esJefaturaUrgencias) {
            String cedulaAVerificar = esJefaturaUrgencias ? cedulaJefatura : cedula;
            if (cedulaExiste(cedulaAVerificar, rol)) {
                mostrarMensaje("La cédula profesional " + cedulaAVerificar + " ya está registrada en el sistema", "red");
                return;
            }
        }

        // Registrar nuevo usuario
        if (registrarUsuarioCompleto(nombreCompleto, email, username, rol, cedula, universidad, cedulaJefatura)) {
            String mensajeExito = "Usuario registrado exitosamente!\n\n" +
                    "Datos del nuevo usuario:\n" +
                    "• Nombre: " + nombreCompleto + "\n" +
                    "• Usuario: " + username + "\n" +
                    "• Contraseña temporal: Temp123\n" +
                    "• Rol: " + rol + "\n";

            if (esMedicoUrgencias || esMedicoEspecialista) {
                mensajeExito += "• Cédula profesional: " + cedula + "\n";
            }
            if (esMedicoEspecialista) {
                mensajeExito += "• Universidad: " + universidad + "\n";
            }
            if (esJefaturaUrgencias) {
                mensajeExito += "• Cédula profesional: " + cedulaJefatura + "\n";
            }

            mensajeExito += "\nEl usuario deberá cambiar su contraseña en el primer login";
            mostrarMensaje(mensajeExito, "green");
            limpiarFormulario();
        } else {
            mostrarMensaje("Error al registrar el usuario", "red");
        }
    }
}