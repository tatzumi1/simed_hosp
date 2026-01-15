//src/main/java/com/PruebaSimed2/controllers/GestionUsuariosController.java

package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import javafx.application.Platform;
import com.PruebaSimed2.models.Usuario;
import com.PruebaSimed2.utils.PasswordUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.util.Pair;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class GestionUsuariosController {


    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<String> cbFiltroRol;
    @FXML
    private ComboBox<String> cbFiltroEstado;
    @FXML
    private Label lblResultados;
    @FXML
    private Label lblContador;
    @FXML
    private TableView<UsuarioVO> tablaUsuarios;
    @FXML
    private TableColumn<UsuarioVO, String> colUsername;
    @FXML
    private TableColumn<UsuarioVO, String> colEmail;
    @FXML
    private TableColumn<UsuarioVO, String> colRol;
    @FXML
    private TableColumn<UsuarioVO, Boolean> colActivo;
    @FXML
    private TableColumn<UsuarioVO, String> colPrimerLogin;
    @FXML
    private TableColumn<UsuarioVO, String> colUltimoLogin;
    @FXML
    private TableColumn<UsuarioVO, String> colAcciones;
    @FXML
    private Label lblMensaje;

    private ObservableList<UsuarioVO> usuariosCompleta = FXCollections.observableArrayList();
    private ObservableList<UsuarioVO> usuariosFiltrada = FXCollections.observableArrayList();
    private Usuario usuarioAdmin;
    private ObservableList<UsuarioVO> usuariosData = FXCollections.observableArrayList();

    // Clase para representar usuarios en la tabla
    public static class UsuarioVO {
        private final SimpleStringProperty username;
        private final SimpleStringProperty email;
        private final SimpleStringProperty rol;
        private final SimpleBooleanProperty activo;
        private final SimpleStringProperty primerLogin;
        private final SimpleStringProperty ultimoLogin;
        private final int idUsuario;

        public UsuarioVO(int idUsuario, String username, String email, String rol,
                         boolean activo, boolean primerLogin, String ultimoLogin) {
            this.idUsuario = idUsuario;
            this.username = new SimpleStringProperty(username);
            this.email = new SimpleStringProperty(email);
            this.rol = new SimpleStringProperty(rol);
            this.activo = new SimpleBooleanProperty(activo);
            this.primerLogin = new SimpleStringProperty(primerLogin ? "SÍ" : "NO");
            this.ultimoLogin = new SimpleStringProperty(ultimoLogin != null ? ultimoLogin : "Nunca");
        }

        // Getters
        public String getUsername() {
            return username.get();
        }

        public String getEmail() {
            return email.get();
        }

        public String getRol() {
            return rol.get();
        }

        public boolean isActivo() {
            return activo.get();
        }

        public String getPrimerLogin() {
            return primerLogin.get();
        }

        public String getUltimoLogin() {
            return ultimoLogin.get();
        }

        public int getIdUsuario() {
            return idUsuario;
        }
    }


    public void setUsuarioAdmin(Usuario usuario) {
        log.debug("setUsuarioAdmin llamado");
        log.debug("Usuario: {}", usuario != null ? usuario.getUsername() : "NULL");
        log.debug("Rol: {}", usuario != null ? usuario.getRol() : "NULL");

        //  VALIDAR QUE SOLO ADMINS PUEDAN ACCEDER
        if (usuario == null) {
            log.debug("Usuario es NULL");
            mostrarMensaje(" Error: Usuario no identificado", "red");
            return;
        }


        if (usuario.getRol() == null || !usuario.getRol().toUpperCase().contains("ADMIN")) {
            log.debug("Usuario NO es admin: {}", usuario.getRol());
            mostrarMensaje(" Acceso denegado. Solo administradores pueden gestionar usuarios.", "red");

            // Cerrar ventana automáticamente si no es admin
            Platform.runLater(() -> {
                Stage stage = (Stage) lblMensaje.getScene().getWindow();
                stage.close();
            });
            return;
        }

        log.debug("Usuario ES admin, continuando...");
        this.usuarioAdmin = usuario;
        log.debug("Admin gestionando usuarios: {}", usuario.getUsername());
        cargarUsuarios();
    }


    @FXML
    public void initialize() {
        configurarColumnas();
        configurarAcciones();
    }

    private void configurarColumnas() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colPrimerLogin.setCellValueFactory(new PropertyValueFactory<>("primerLogin"));
        colUltimoLogin.setCellValueFactory(new PropertyValueFactory<>("ultimoLogin"));
    }

    private void configurarAcciones() {
        colAcciones.setCellFactory(new Callback<TableColumn<UsuarioVO, String>, TableCell<UsuarioVO, String>>() {
            @Override
            public TableCell<UsuarioVO, String> call(TableColumn<UsuarioVO, String> param) {
                return new TableCell<UsuarioVO, String>() {
                    private final Button btnResetear = new Button(" Resetear");
                    private final Button btnToggle = new Button(" Toggle");
                    private final Button btnCambiarRol = new Button("🔄 Rol");

                    {
                        // Estilos de los botones
                        btnResetear.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 10;");
                        btnToggle.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10;");
                        btnCambiarRol.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-size: 10;");

                        // Acciones
                        btnResetear.setOnAction(e -> {
                            UsuarioVO usuario = getTableView().getItems().get(getIndex());
                            resetearPassword(usuario);
                        });

                        btnToggle.setOnAction(e -> {
                            UsuarioVO usuario = getTableView().getItems().get(getIndex());
                            toggleActivo(usuario);
                        });

                        btnCambiarRol.setOnAction(e -> {
                            UsuarioVO usuario = getTableView().getItems().get(getIndex());
                            cambiarRol(usuario);
                        });
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            UsuarioVO usuario = getTableView().getItems().get(getIndex());
                            HBox botones = new HBox(5);

                            // Agregar botones base
                            botones.getChildren().addAll(btnResetear, btnToggle);

                            if (!usuario.getRol().equals("ADMIN")) {
                                botones.getChildren().add(btnCambiarRol);
                            }

                            setGraphic(botones);
                        }
                    }
                };
            }
        });
    }

    private void cargarUsuarios() {
        String sql = "SELECT id_usuario, username, email, rol, activo, primer_login, ultimo_login " +
                "FROM tb_usuarios ORDER BY rol, username";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            usuariosCompleta.clear();

            while (rs.next()) {
                String ultimoLogin = rs.getTimestamp("ultimo_login") != null ?
                        rs.getTimestamp("ultimo_login").toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null;

                UsuarioVO usuario = new UsuarioVO(
                        rs.getInt("id_usuario"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("rol"),
                        rs.getBoolean("activo"),
                        rs.getBoolean("primer_login"),
                        ultimoLogin
                );
                usuariosCompleta.add(usuario); // ← NUEVO: Agregar a lista completa
            }

            if (cbFiltroRol.getItems().isEmpty()) {
                inicializarBusqueda();
            }

            filtrarUsuarios();
            mostrarMensaje(" " + usuariosCompleta.size() + " usuarios cargados", "green");

        } catch (SQLException e) {
            log.error("Error cargando usuarios: {}", e.getMessage());
            mostrarMensaje(" Error al cargar usuarios: " + e.getMessage(), "red");
        }
    }

    private void resetearPassword(UsuarioVO usuario) {
        if (!confirmarAccion("¿Resetear contraseña de " + usuario.getUsername() + "?\n\n" +
                "La nueva contraseña temporal será: Temp123\n" +
                "El usuario deberá cambiarla en su próximo login.")) {
            return;
        }
        String tempPassword = "Temp123";
        String sql = "UPDATE tb_usuarios SET password_hash = ?, primer_login = true WHERE id_usuario = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String hash = PasswordUtils.hashPassword(tempPassword);
            pstmt.setString(1, hash);
            pstmt.setInt(2, usuario.getIdUsuario());
            int filas = pstmt.executeUpdate();
            if (filas > 0) {
                mostrarMensaje("  Contraseña reseteada para " + usuario.getUsername() +
                        "\nNueva contraseña temporal: Temp123" +
                        "\nEl usuario deberá cambiarla en su próximo login.", "green");
                cargarUsuarios();
            } else {
                mostrarMensaje("  Error: No se pudo resetear la contraseña", "red");
            }
        } catch (SQLException e) {
            log.error("Error reseteando password: {}", e.getMessage());
            mostrarMensaje("  Error al resetear contraseña: " + e.getMessage(), "red");
        }
    }

    private void toggleActivo(UsuarioVO usuario) {
        String accion = usuario.isActivo() ? "desactivar" : "activar";
        if (!confirmarAccion("¿" + accion.toUpperCase() + " a " + usuario.getUsername() + "?")) {
            return;
        }

        String sql = "UPDATE tb_usuarios SET activo = ? WHERE id_usuario = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, !usuario.isActivo());
            pstmt.setInt(2, usuario.getIdUsuario());

            int filas = pstmt.executeUpdate();

            if (filas > 0) {
                mostrarMensaje("Usuario " + usuario.getUsername() + " " +
                        (usuario.isActivo() ? "desactivado" : "activado"), "green");
                cargarUsuarios();
            }

        } catch (SQLException e) {
            log.error("Error cambiando estado: {}", e.getMessage());
            mostrarMensaje(" Error al cambiar estado", "red");
        }
    }

    @FXML
    private void handleRegistrarUsuario() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/registroUsuario.fxml"));
            Parent root = loader.load();

            RegistroUsuarioController controller = loader.getController();
            controller.setUsuarioAdmin(usuarioAdmin);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 600, 700));
            stage.setTitle("Registro de Nuevo Usuario");
            stage.show();

        } catch (Exception e) {
            log.error(" Error abriendo registro de usuario: {}", e.getMessage());
            log.error("StackTrace:", e);
        }
    }

    @FXML
    private void handleActualizarLista() {
        cargarUsuarios();
    }

    @FXML
    private void handleVolver() {
        Stage stage = (Stage) tablaUsuarios.getScene().getWindow();
        stage.close();
    }

    private boolean confirmarAccion(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar acción");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        return alert.showAndWait().get() == ButtonType.OK;
    }

    private void mostrarMensaje(String mensaje, String color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle("-fx-text-fill: " + color + ";");
    }

    // === INICIALIZAR BÚSQUEDA ===
    private void inicializarBusqueda() {
        // Llenar combobox de filtros
        cbFiltroRol.setItems(FXCollections.observableArrayList(
                "Todos los roles", "ADMIN", "MEDICO_URGENCIAS", "MEDICO_ESPECIALISTA",
                "TRIAGE", "JEFATURA_URGENCIAS", "ADMINISTRATIVO", "TRABAJADOR_SOCIAL"
        ));

        cbFiltroEstado.setItems(FXCollections.observableArrayList(
                "Todos los estados", "Activos", "Inactivos"
        ));

        // Listener para búsqueda en tiempo real
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrarUsuarios();
        });

        cbFiltroRol.valueProperty().addListener((observable, oldValue, newValue) -> {
            filtrarUsuarios();
        });

        cbFiltroEstado.valueProperty().addListener((observable, oldValue, newValue) -> {
            filtrarUsuarios();
        });
    }

    // === MÉTODO DE FILTRADO ===
    private void filtrarUsuarios() {
        String textoBusqueda = txtBuscar.getText().toLowerCase();
        String rolSeleccionado = cbFiltroRol.getValue();
        String estadoSeleccionado = cbFiltroEstado.getValue();

        usuariosFiltrada.clear();

        for (UsuarioVO usuario : usuariosCompleta) {
            boolean coincideBusqueda = textoBusqueda.isEmpty() ||
                    usuario.getUsername().toLowerCase().contains(textoBusqueda) ||
                    usuario.getEmail().toLowerCase().contains(textoBusqueda) ||
                    usuario.getRol().toLowerCase().contains(textoBusqueda);

            boolean coincideRol = rolSeleccionado == null ||
                    rolSeleccionado.equals("Todos los roles") ||
                    usuario.getRol().equals(rolSeleccionado);

            boolean coincideEstado = estadoSeleccionado == null ||
                    estadoSeleccionado.equals("Todos los estados") ||
                    (estadoSeleccionado.equals("Activos") && usuario.isActivo()) ||
                    (estadoSeleccionado.equals("Inactivos") && !usuario.isActivo());

            if (coincideBusqueda && coincideRol && coincideEstado) {
                usuariosFiltrada.add(usuario);
            }
        }

        tablaUsuarios.setItems(usuariosFiltrada);
        actualizarContadores();
    }

    private void actualizarContadores() {
        int total = usuariosCompleta.size();
        int mostrados = usuariosFiltrada.size();

        lblContador.setText(String.format("Total: %d usuarios | Mostrando: %d", total, mostrados));

        if (mostrados < total && !txtBuscar.getText().isEmpty()) {
            lblResultados.setText(String.format(" Se encontraron %d de %d usuarios", mostrados, total));
        } else {
            lblResultados.setText("");
        }
    }

    @FXML
    private void handleBuscar() {
        filtrarUsuarios();
    }

    @FXML
    private void handleLimpiarBusqueda() {
        txtBuscar.clear();
        cbFiltroRol.setValue(null);
        cbFiltroEstado.setValue(null);
        filtrarUsuarios();
    }

    @FXML
    private void handleEstadisticas() {
        // Mostrar estadísticas simples
        long activos = usuariosCompleta.stream().filter(UsuarioVO::isActivo).count();
        long admins = usuariosCompleta.stream().filter(u -> u.getRol().equals("ADMIN")).count();
        long medicos = usuariosCompleta.stream().filter(u -> u.getRol().contains("MEDICO")).count();

        String estadisticas = String.format(
                "ESTADÍSTICAS DE USUARIOS:\n\n" +
                        "• Total de usuarios: %d\n" +
                        "• Usuarios activos: %d\n" +
                        "• Administradores: %d\n" +
                        "• Médicos: %d\n" +
                        "• Inactivos: %d",
                usuariosCompleta.size(), activos, admins, medicos, (usuariosCompleta.size() - activos)
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Estadísticas de Usuarios");
        alert.setHeaderText("Resumen del Sistema");
        alert.setContentText(estadisticas);
        alert.showAndWait();
    }

    private void cambiarRol(UsuarioVO usuario) {

        if (usuario.getRol().equals("ADMIN")) {
            mostrarMensaje("  No se puede cambiar el rol de un usuario ADMIN", "red");
            return;
        }

        List<String> rolesPermitidos = Arrays.asList(
                "MEDICO_URGENCIAS", "MEDICO_ESPECIALISTA", "TRIAGE",
                "JEFATURA_URGENCIAS", "ADMINISTRATIVO", "TRABAJADOR_SOCIAL"
        );

        ChoiceDialog<String> dialog = new ChoiceDialog<>(usuario.getRol(), rolesPermitidos);
        dialog.setTitle("Cambiar Rol");
        dialog.setHeaderText("Cambiando rol para: " + usuario.getUsername());
        dialog.setContentText("Seleccione el nuevo rol:");

        Optional<String> resultado = dialog.showAndWait();

        if (resultado.isPresent() && !resultado.get().equals(usuario.getRol())) {
            String nuevoRol = resultado.get();

            if (nuevoRol.equals("MEDICO_URGENCIAS") || nuevoRol.equals("MEDICO_ESPECIALISTA") ||
                    nuevoRol.equals("JEFATURA_URGENCIAS")) {

                // Pedir datos adicionales
                Pair<String, String> datosMedico = pedirDatosMedico(nuevoRol);
                if (datosMedico == null) {
                    return;
                }

                String cedula = datosMedico.getKey();
                String universidad = datosMedico.getValue();

                if (confirmarAccion("¿Cambiar rol de " + usuario.getUsername() + " de '" +
                        usuario.getRol() + "' a '" + nuevoRol + "'?\n\n" +
                        "Cédula: " + cedula +
                        (nuevoRol.equals("MEDICO_ESPECIALISTA") ? "\nUniversidad: " + universidad : ""))) {

                    if (actualizarRolConDatos(usuario.getIdUsuario(), nuevoRol, cedula, universidad)) {
                        mostrarMensaje("Rol cambiado exitosamente para " + usuario.getUsername(), "green");
                        cargarUsuarios();
                    } else {
                        mostrarMensaje("Error al cambiar el rol", "red");
                    }
                }
            } else {
                // Para roles que no requieren datos adicionales
                if (confirmarAccion("¿Cambiar rol de " + usuario.getUsername() + " de '" +
                        usuario.getRol() + "' a '" + nuevoRol + "'?")) {

                    if (actualizarRolEnBD(usuario.getIdUsuario(), nuevoRol)) {
                        mostrarMensaje("  Rol cambiado exitosamente para " + usuario.getUsername(), "green");
                        cargarUsuarios();
                    } else {
                        mostrarMensaje("  Error al cambiar el rol", "red");
                    }
                }
            }
        }
    }


    // Método para pedir datos médicos adicionales
    private Pair<String, String> pedirDatosMedico(String rol) {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Datos Profesionales");
        dialog.setHeaderText("Complete los datos para el rol: " + rol);

        // Botones
        ButtonType btnConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

        // Crear campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtCedula = new TextField();
        txtCedula.setPromptText("Cédula profesional");

        TextField txtUniversidad = new TextField();
        txtUniversidad.setPromptText("Universidad");

        grid.add(new Label("Cédula Profesional:"), 0, 0);
        grid.add(txtCedula, 1, 0);

        // Solo mostrar universidad para médico especialista
        if (rol.equals("MEDICO_ESPECIALISTA")) {
            grid.add(new Label("Universidad:"), 0, 1);
            grid.add(txtUniversidad, 1, 1);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirmar) {
                if (txtCedula.getText().trim().isEmpty()) {
                    mostrarMensaje("La cédula profesional es obligatoria", "red");
                    return null;
                }
                if (rol.equals("MEDICO_ESPECIALISTA") && txtUniversidad.getText().trim().isEmpty()) {
                    mostrarMensaje("La universidad es obligatoria para médico especialista", "red");
                    return null;
                }
                return new Pair<>(txtCedula.getText().trim(),
                        rol.equals("MEDICO_ESPECIALISTA") ? txtUniversidad.getText().trim() : "");
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private boolean actualizarRolEnBD(int idUsuario, String nuevoRol) {
        //  Validación extra por seguridad
        if (nuevoRol.equals("ADMIN")) {
            mostrarMensaje("  Error: No se puede asignar rol ADMIN desde esta función", "red");
            return false;
        }

        String sql = "UPDATE tb_usuarios SET rol = ? WHERE id_usuario = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nuevoRol);
            pstmt.setInt(2, idUsuario);

            int filas = pstmt.executeUpdate();
            return filas > 0;

        } catch (SQLException e) {
            log.error("Error cambiando rol: {}", e.getMessage());
            return false;
        }
    }

    // Método para cambiar rol con datos médicos
    private boolean actualizarRolConDatos(int idUsuario, String nuevoRol, String cedula, String universidad) {
        Connection conn = null;
        try {
            conn = ConexionBD.conectar();
            conn.setAutoCommit(false);

            // 1. Actualizar rol y cédula en tb_usuarios
            String sqlUsuario = "UPDATE tb_usuarios SET rol = ?, cedula_profesional = ? WHERE id_usuario = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUsuario)) {
                pstmt.setString(1, nuevoRol);
                pstmt.setString(2, cedula);
                pstmt.setInt(3, idUsuario);
                pstmt.executeUpdate();
            }

            // 2. Eliminar de tablas médicas anteriores (limpieza)
            String sqlEliminarMedicos = "DELETE FROM tb_medicos WHERE Ced_prof = ?";
            String sqlEliminarMedEsp = "DELETE FROM tb_medesp WHERE Cedula = ?";

            try (PreparedStatement pstmt1 = conn.prepareStatement(sqlEliminarMedicos);
                 PreparedStatement pstmt2 = conn.prepareStatement(sqlEliminarMedEsp)) {
                pstmt1.setString(1, cedula);
                pstmt2.setString(1, cedula);
                pstmt1.executeUpdate();
                pstmt2.executeUpdate();
            }

            // 3. Agregar a la tabla correspondiente según el nuevo rol
            if (nuevoRol.equals("MEDICO_URGENCIAS") || nuevoRol.equals("JEFATURA_URGENCIAS")) {
                int nuevaCveMed = generarNuevaCveMed(conn, "tb_medicos");
                String sqlMedico = "INSERT INTO tb_medicos (Cve_med, Med_nombre, Ced_prof) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlMedico)) {
                    pstmt.setInt(1, nuevaCveMed);
                    pstmt.setString(2, obtenerNombreUsuario(conn, idUsuario));
                    pstmt.setString(3, cedula);
                    pstmt.executeUpdate();
                }
            } else if (nuevoRol.equals("MEDICO_ESPECIALISTA")) {
                int nuevaCveMed = generarNuevaCveMed(conn, "tb_medesp");
                String sqlMedEsp = "INSERT INTO tb_medesp (Cve_med, Nombre, Cedula, universidad) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlMedEsp)) {
                    pstmt.setInt(1, nuevaCveMed);
                    pstmt.setString(2, obtenerNombreUsuario(conn, idUsuario));
                    pstmt.setString(3, cedula);
                    pstmt.setString(4, universidad);
                    pstmt.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    log.error("Error al hacer rollback");
                }
            }
            log.error("Error cambiando rol con datos: {}", e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error al cerrar la conexión");
                }
            }
        }
    }

    // Método auxiliar para obtener nombre del usuario
    private String obtenerNombreUsuario(Connection conn, int idUsuario) throws SQLException {
        String sql = "SELECT nombre_completo FROM tb_usuarios WHERE id_usuario = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nombre_completo");
            }
        }
        return "";
    }

    // Método para generar nueva clave médica
    private int generarNuevaCveMed(Connection conn, String tabla) throws SQLException {
        String sql = "SELECT COALESCE(MAX(Cve_med), 0) + 1 as nueva_clave FROM " + tabla;
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("nueva_clave");
            }
            return 1;
        }
    }

    @FXML
    private void handleVerificarDatos() {
        String sql = "SELECT COUNT(*) as total, " +
                "SUM(CASE WHEN nombre_completo IS NOT NULL AND nombre_completo != '' THEN 1 ELSE 0 END) as con_nombre, " +
                "SUM(CASE WHEN username IS NOT NULL AND username != '' THEN 1 ELSE 0 END) as con_username " +
                "FROM tb_usuarios";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int total = rs.getInt("total");
                int conNombre = rs.getInt("con_nombre");
                int conUsername = rs.getInt("con_username");

                String mensaje = String.format(
                        "VERIFICACIÓN DE DATOS:\n\n" +
                                "• Total de usuarios: %d\n" +
                                "• Con nombre completo: %d\n" +
                                "• Con username: %d\n" +
                                "• Integridad de datos: %.1f%%",
                        total, conNombre, conUsername,
                        ((double) Math.min(conNombre, conUsername) / total) * 100
                );

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Verificación de Datos");
                alert.setHeaderText("Estado de la base de datos");
                alert.setContentText(mensaje);
                alert.showAndWait();
            }

        } catch (SQLException e) {
            mostrarMensaje(" Error al verificar datos: " + e.getMessage(), "red");
        }
    }

    // ===== MÉTODO PARA ABRIR LA VENTANA DE LOGOS
    @FXML
    private void abrirConfigurarLogos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/configurar_logos.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Configurar logos del encabezado PDF");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait(); // se cierra cuando el admin da "Guardar y cerrar"

        } catch (IOException e) {
            log.error("Error al cargar la ventana de logos: {}", e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("No se pudo abrir la ventana de logos.\nVerifica que el archivo configurar_logos.fxml esté en la carpeta resources/fxml/");
            alert.show();
        }
    }


}