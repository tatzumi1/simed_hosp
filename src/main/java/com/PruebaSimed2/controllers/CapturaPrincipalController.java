// src/main/java/com/PruebaSimed2/controllers/CapturaPrincipalController.java

package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.NotaMedicaVO;
import com.PruebaSimed2.utils.PDFGenerator;
import com.PruebaSimed2.models.InterconsultaVO;
import com.PruebaSimed2.utils.SesionUsuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.io.File;
import java.net.URL;

public class CapturaPrincipalController {

    // ==================== CAMPOS IZQUIERDA - DATOS EXISTENTES ====================
    @FXML private TextField txtFolio, txtFechaRegistro, txtHoraRegistro, txtTriage;
    @FXML private TextField txtNombre, txtEdad, txtSexo, txtMunicipio, txtEntidad;
    @FXML private TextField txtDerechohabiencia, txtReferencia, txtMedicoIngreso;
    @FXML private TextArea txtDomicilio, txtSintomas;
    @FXML private Button btnColorTriage;

    // ==================== CAMPOS DERECHA - NUEVA INFORMACIÓN ====================
    @FXML private TextField txtFechaAtencion, txtHoraAtencion, txtCedulaMedico;
    @FXML private ComboBox<String> cmbTipoUrgencia, cmbMotivoUrgencia, cmbTipoCama, cmbMedicoActual;

    // ==================== ESTADO DEL PACIENTE ====================
    @FXML private ToggleGroup tgEstadoPaciente;
    @FXML private RadioButton rbObservacion, rbAltaMedica;

    // ==================== NOTAS MÉDICAS ====================
    @FXML private Label lblContadorNotas, lblContadorInterconsultas;
    @FXML private TableView<NotaMedicaVO> tablaNotasMedicas;
    @FXML private TableView<InterconsultaVO> tablaInterconsultas;
    @FXML private Button btnVisualizarNotaMedica, btnEditarNotaMedica, btnImprimirNotaMedica;
    @FXML private Button btnVisualizarInterconsulta, btnEditarInterconsulta, btnImprimirInterconsulta;
    @FXML private Button btnNuevaNotaMedica, btnNuevaInterconsulta;
    @FXML private Button btnOtorgarPermiso, btnOtorgarPermisoInterconsulta;
    @FXML private Button btnGuardarGeneral;

    // MÉTODOS RESTANTES (visualizar, imprimir, etc.) - SE MANTIENEN SIMILARES

    @FXML private void imprimirNotaMedica() { /* implementación similar */ }
    @FXML private void imprimirInterconsulta() { /* implementación similar */ }




    private int folioPaciente;
    private String usuarioLogueado;
    private String rolUsuarioLogueado;
    private Map<String, String> coloresTriage = new HashMap<>();
    private boolean capturaGuardada = false;

    // Variables para datos
    private ObservableList<NotaMedicaVO> notasData = FXCollections.observableArrayList();
    private ObservableList<InterconsultaVO> interconsultasData = FXCollections.observableArrayList();
    private SesionUsuario sesion = SesionUsuario.getInstance();

    // ==================== MÉTODOS PRINCIPALES ====================

    public void setFolioPaciente(int folio) {
        this.folioPaciente = folio;
        cargarDatosPaciente(folio);
        cargarCombos();
        cargarContadores();
        configurarFechaHoraActual();
        iniciarRelojTiempoReal();
        cargarNotasDelPaciente();
        cargarInterconsultasDelPaciente();
        configurarColumnasTablas();
        configurarColumnasInterconsultas();
        verificarNotasTemporalesPropias();
        configurarVisibilidadSegunRol();
    }

    public void setUsuarioLogueado(String usuario, String rol) {
        this.usuarioLogueado = usuario;
        this.rolUsuarioLogueado = rol;

        if (sesion.getUsername() == null) {
            int usuarioId = obtenerIdUsuarioDesdeBD(usuario);
            sesion.inicializar(usuario, rol, usuarioId);
        }

        System.out.println("Usuario en captura: " + usuario + " - Rol: " + rol);
        System.out.println("Nombre médico en sesión: " + sesion.getNombreMedico());

        configurarVisibilidadSegunRol(); // NUEVA LÍNEA
    }

    @FXML
    public void initialize() {
        configurarColumnasTablas();
        configurarColumnasInterconsultas();
        btnNuevaNotaMedica.setStyle("-fx-background-color: #002366; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevaInterconsulta.setStyle("-fx-background-color: #002366; -fx-text-fill: white; -fx-font-weight: bold;");

        coloresTriage.put("Rojo", "#e74c3c");
        coloresTriage.put("Amarillo", "#f1c40f");
        coloresTriage.put("Verde", "#2ecc71");

        configurarToggleGroup();
        configurarEventos();
        configurarTablas();
    }

    private void configurarToggleGroup() {
        tgEstadoPaciente = new ToggleGroup();
        rbObservacion.setToggleGroup(tgEstadoPaciente);
        rbAltaMedica.setToggleGroup(tgEstadoPaciente);
    }

    private void configurarEventos() {
        // Combos médicos
        cmbMedicoActual.setOnAction(e -> actualizarCedulaMedico());

        // Botones de notas
        btnNuevaNotaMedica.setOnAction(e -> abrirNotaMedica());
        btnNuevaInterconsulta.setOnAction(e -> abrirInterconsulta());

        // Botones de acciones
        btnVisualizarNotaMedica.setOnAction(e -> visualizarNotaMedica());
        btnEditarNotaMedica.setOnAction(e -> editarNotaMedica());
        btnImprimirNotaMedica.setOnAction(e -> imprimirNotaMedica());
        btnOtorgarPermiso.setOnAction(e -> otorgarPermisoEdicion());

        btnVisualizarInterconsulta.setOnAction(e -> visualizarInterconsulta());
        btnEditarInterconsulta.setOnAction(e -> editarInterconsulta());
        btnImprimirInterconsulta.setOnAction(e -> imprimirInterconsulta());
        btnOtorgarPermisoInterconsulta.setOnAction(e -> otorgarPermisoInterconsulta());

        // Guardar
        btnGuardarGeneral.setOnAction(e -> guardarGeneral());
    }

    private void configurarTablas() {
        // Configurar tabla de notas médicas
        if (tablaNotasMedicas != null) {
            tablaNotasMedicas.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> onNotaSeleccionada()
            );
        }

        // Configurar tabla de interconsultas
        if (tablaInterconsultas != null) {
            tablaInterconsultas.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> onInterconsultaSeleccionada()
            );
        }
    }

    private void cargarDatosPaciente(int folio) {
        String sql = "SELECT u.*, " +
                "m.DESCRIP as NombreMunicipio, e.DESCRIP as NombreEntidad, " +
                "dh.Derechohabiencia as NombreDerechohabiencia " +
                "FROM tb_urgencias u " +
                "LEFT JOIN tblt_mpo m ON u.Municipio_resid = m.MPO AND u.Entidad_resid = m.EDO " +
                "LEFT JOIN tblt_entidad e ON u.Entidad_resid = e.EDO " +
                "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                "WHERE u.Folio = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folio);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // ========== INFORMACIÓN BÁSICA ==========
                txtFolio.setText(String.valueOf(rs.getInt("Folio")));
                txtFechaRegistro.setText(rs.getString("Fecha"));
                txtHoraRegistro.setText(rs.getString("Hora_registro"));

                // TRIAGE con color
                String triage = rs.getString("TRIAGE");
                txtTriage.setText(triage != null ? triage : "No especificado");
                aplicarColorTriage(triage);

                // Nombre completo
                String nombreCompleto = construirNombreCompleto(
                        rs.getString("A_paterno"),
                        rs.getString("A_materno"),
                        rs.getString("Nombre")
                );
                txtNombre.setText(nombreCompleto);

                // Edad
                txtEdad.setText(String.valueOf(rs.getInt("Edad")));

                // Sexo
                txtSexo.setText(obtenerDescripcionSexo(rs.getString("Sexo")));

                // Domicilio
                txtDomicilio.setText(rs.getString("Domicilio"));

                // ========== MUNICIPIO Y ENTIDAD ==========
                String municipioCode = rs.getString("Municipio_resid");
                String entidadCode = rs.getString("Entidad_resid");

                System.out.println(" Datos municipio/entidad:");
                System.out.println("   Municipio_resid: '" + municipioCode + "'");
                System.out.println("   Entidad_resid: '" + entidadCode + "'");

                // 1. PRIMERO usar el resultado del JOIN si encontró algo
                String municipio = rs.getString("NombreMunicipio");
                String entidad = rs.getString("NombreEntidad");

                System.out.println("   Resultado JOIN Municipio: '" + municipio + "'");
                System.out.println("   Resultado JOIN Entidad: '" + entidad + "'");

                // 2. SI el JOIN no funcionó, procesar nosotros
                if (municipio == null && municipioCode != null) {
                    municipio = obtenerNombreMunicipio(municipioCode, entidadCode);
                }

                if (entidad == null && entidadCode != null) {
                    entidad = obtenerNombreEntidad(entidadCode);
                }

                // Mostrar en UI
                txtMunicipio.setText(municipio != null ? municipio : "No especificado");
                txtEntidad.setText(entidad != null ? entidad : "No especificado");

                System.out.println(" Mostrando en UI:");
                System.out.println("   Municipio: " + txtMunicipio.getText());
                System.out.println("   Entidad: " + txtEntidad.getText());
                // ========== FIN MUNICIPIO/ENTIDAD ==========

                // Derechohabiencia
                String derechohabiencia = rs.getString("NombreDerechohabiencia");
                txtDerechohabiencia.setText(derechohabiencia != null ? derechohabiencia : "No especificado");

                // Información médica
                txtReferencia.setText(rs.getString("Referencia"));
                txtSintomas.setText(rs.getString("Sintomas"));
                txtMedicoIngreso.setText(rs.getString("Nom_med"));

                System.out.println(" Datos paciente cargados - Folio: " + folio);

            } else {
                System.out.println(" No se encontró paciente con folio: " + folio);
            }
        } catch (SQLException e) {
            System.err.println(" Error cargando paciente: " + e.getMessage());
            mostrarAlerta("Error", "No se pudieron cargar los datos del paciente", Alert.AlertType.ERROR);
        }
    }

// ========== MÉTODOS AUXILIARES ==========

    private String obtenerNombreMunicipio(String municipioCode, String entidadCode) {
        if (municipioCode == null || municipioCode.trim().isEmpty()) {
            return null;
        }

        municipioCode = municipioCode.trim();
        entidadCode = entidadCode != null ? entidadCode.trim() : "";

        System.out.println(" Procesando municipio: '" + municipioCode + "'");

        // CASO A: Si ya es texto (ej: "poza rica"), usarlo directamente
        if (!municipioCode.matches("\\d+")) {
            System.out.println("   Es texto, usando directamente: " + municipioCode);
            return municipioCode;
        }

        // CASO B: Si es número, buscar en tblt_mpo
        System.out.println("   Es número, buscando en tblt_mpo...");

        String sql = "SELECT DESCRIP FROM tblt_mpo WHERE MPO = ? AND EDO = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, municipioCode);
            pstmt.setString(2, entidadCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String nombre = rs.getString("DESCRIP");
                    System.out.println(" Municipio encontrado: " + nombre);
                    return nombre;
                } else {
                    System.out.println(" Municipio no encontrado en tblt_mpo");

                    // Intentar sin ceros a la izquierda
                    if (municipioCode.matches("0+\\d+")) {
                        String sinCeros = municipioCode.replaceFirst("^0+", "");
                        System.out.println("   Intentando sin ceros: '" + sinCeros + "'");

                        pstmt.setString(1, sinCeros);
                        ResultSet rs2 = pstmt.executeQuery();
                        if (rs2.next()) {
                            String nombre = rs2.getString("DESCRIP");
                            System.out.println(" Municipio encontrado (sin ceros): " + nombre);
                            return nombre;
                        }
                    }

                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo municipio: " + e.getMessage());
            return null;
        }
    }

    private String obtenerNombreEntidad(String entidadCode) {
        if (entidadCode == null || entidadCode.trim().isEmpty()) {
            return null;
        }

        entidadCode = entidadCode.trim();
        System.out.println("🔍 Procesando entidad: '" + entidadCode + "'");

        // CASO A: Si ya es texto, usarlo directamente
        if (!entidadCode.matches("\\d+")) {
            System.out.println("   Es texto, usando directamente: " + entidadCode);
            return entidadCode;
        }

        // CASO B: Si es número, buscar en tblt_entidad
        System.out.println("   Es número, buscando en tblt_entidad...");

        String sql = "SELECT DESCRIP FROM tblt_entidad WHERE EDO = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, entidadCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String nombre = rs.getString("DESCRIP");
                    System.out.println(" Entidad encontrada: " + nombre);
                    return nombre;
                } else {
                    System.out.println(" Entidad no encontrada en tblt_entidad");
                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo entidad: " + e.getMessage());
            return null;
        }
    }


    // Método actualizarUIDatosPaciente corregido
    private void actualizarUIDatosPaciente(Map<String, Object> datos) {
        try {
            // Información básica
            txtFolio.setText(String.valueOf(datos.get("folio")));
            txtFechaRegistro.setText((String) datos.get("fecha"));
            txtHoraRegistro.setText((String) datos.get("hora_registro"));

            // TRIAGE con color
            String triage = (String) datos.get("triage");
            txtTriage.setText(triage != null ? triage : "No especificado");
            aplicarColorTriage(triage);

            // Nombre completo
            String nombreCompleto = construirNombreCompleto(
                    (String) datos.get("a_paterno"),
                    (String) datos.get("a_materno"),
                    (String) datos.get("nombre")
            );
            txtNombre.setText(nombreCompleto);

            // Demás datos
            txtEdad.setText(String.valueOf(datos.get("edad")));
            txtSexo.setText(obtenerDescripcionSexo((String) datos.get("sexo")));
            txtDomicilio.setText((String) datos.get("domicilio"));

            // MUNICIPIO Y ENTIDAD (corregido)
            String municipio = (String) datos.get("municipio");
            String entidad = (String) datos.get("entidad");

            System.out.println(" Mostrando en UI:");
            System.out.println("   Municipio: " + municipio);
            System.out.println("   Entidad: " + entidad);

            txtMunicipio.setText(municipio != null ? municipio : "No especificado");
            txtEntidad.setText(entidad != null ? entidad : "No especificado");

            txtDerechohabiencia.setText((String) datos.get("derechohabiencia"));
            txtReferencia.setText((String) datos.get("referencia"));
            txtSintomas.setText((String) datos.get("sintomas"));
            txtMedicoIngreso.setText((String) datos.get("nom_med"));

            System.out.println(" Datos paciente cargados en UI - Folio: " + datos.get("folio"));

        } catch (Exception e) {
            System.err.println(" Error actualizando UI de paciente: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error de datos",
                    "Error al mostrar datos del paciente: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private String construirNombreCompleto(String aPaterno, String aMaterno, String nombre) {
        StringBuilder nombreCompleto = new StringBuilder();

        if (aPaterno != null && !aPaterno.trim().isEmpty()) {
            nombreCompleto.append(aPaterno).append(" ");
        }
        if (aMaterno != null && !aMaterno.trim().isEmpty()) {
            nombreCompleto.append(aMaterno).append(" ");
        }
        if (nombre != null && !nombre.trim().isEmpty()) {
            nombreCompleto.append(nombre);
        }

        return nombreCompleto.length() > 0 ? nombreCompleto.toString().trim() : "No especificado";
    }

    private String obtenerDescripcionSexo(String codigoSexo) {
        if ("1".equals(codigoSexo)) return "Masculino";
        if ("2".equals(codigoSexo)) return "Femenino";
        return "No especificado";
    }

    private void aplicarColorTriage(String triage) {
        if (triage != null) {
            String color = "";
            String texto = triage.toLowerCase();

            if (texto.contains("rojo")) color = "#e74c3c";
            else if (texto.contains("amarillo")) color = "#f1c40f";
            else if (texto.contains("verde")) color = "#2ecc71";
            else color = "#95a5a6";

            btnColorTriage.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10;");
        }
    }

    // ==================== MÉTODOS AUXILIARES MEJORADOS ====================

    /**
     *  OBTENER ID DE USUARIO DESDE BD - CORREGIDO
     */
    private int obtenerIdUsuarioDesdeBD(String username) {
        String sql = "SELECT id_usuario FROM tb_usuarios WHERE username = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id_usuario");
            }
        } catch (SQLException e) {
            System.err.println(" Error obteniendo ID de usuario: " + e.getMessage());
        }
        return 0; // Valor por defecto si no se encuentra
    }

    /**
     *  OBTENER NOMBRE COMPLETO DEL MÉDICO DESDE USUARIO - CORREGIDO
     */
    private String obtenerNombreMedicoDesdeUsuario(String username) {
        System.out.println(" BUSCANDO NOMBRE MÉDICO COMPLETO para usuario: " + username);

        // PRIMERO: Buscar por empleado_id en tb_usuarios
        String sql = "SELECT m.Med_nombre " +
                "FROM tb_medicos m " +
                "INNER JOIN tb_usuarios u ON m.Ced_prof = u.empleado_id " +
                "WHERE u.username = ? " +
                "UNION " +
                "SELECT nombre_completo FROM tb_usuarios WHERE username = ? " +
                "UNION " +
                "SELECT Med_nombre FROM tb_medicos WHERE Med_nombre LIKE CONCAT('%', ?, '%') " +
                "LIMIT 1";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setString(3, username);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String nombreMedico = rs.getString("Med_nombre");
                System.out.println(" NOMBRE MÉDICO ENCONTRADO: " + nombreMedico + " para usuario: " + username);
                return nombreMedico;
            } else {
                System.out.println(" NO se encontró médico completo para: " + username);
                // Fallback: buscar en tb_usuarios el nombre_completo
                return obtenerNombreCompletoDesdeUsuarios(username);
            }

        } catch (SQLException e) {
            System.err.println(" Error obteniendo nombre médico: " + e.getMessage());
            return obtenerNombreCompletoDesdeUsuarios(username);
        }
    }

    /**
     *  FALLBACK: Obtener nombre_completo desde tb_usuarios
     */
    private String obtenerNombreCompletoDesdeUsuarios(String username) {
        String sql = "SELECT nombre_completo FROM tb_usuarios WHERE username = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String nombreCompleto = rs.getString("nombre_completo");
                if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
                    System.out.println(" Nombre completo desde usuarios: " + nombreCompleto);
                    return nombreCompleto;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo nombre completo: " + e.getMessage());
        }

        System.out.println("Usando username como fallback: " + username);
        return username;
    }

    // de aaquiiii nooommmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm

    private void mostrarSeccionNuevaInformacion() {
        Platform.runLater(() -> {
            txtFechaAtencion.setVisible(true);
            txtHoraAtencion.setVisible(true);
            txtCedulaMedico.setVisible(true);
            cmbTipoUrgencia.setVisible(true);
            cmbMotivoUrgencia.setVisible(true);
            cmbTipoCama.setVisible(true);
            cmbMedicoActual.setVisible(true);
            rbObservacion.setVisible(true);
            rbAltaMedica.setVisible(true);
            btnGuardarGeneral.setVisible(true);

            Label labelFecha = (Label) txtFechaAtencion.getParent().getParent().getParent().lookup("Label[text='Fecha Atención:']");
            Label labelHora = (Label) txtHoraAtencion.getParent().getParent().getParent().lookup("Label[text='Hora Atención:']");
            Label labelTipoUrgencia = (Label) txtFechaAtencion.getParent().getParent().getParent().lookup("Label[text='Tipo de Urgencia:']");
            Label labelMotivo = (Label) txtFechaAtencion.getParent().getParent().getParent().lookup("Label[text='Motivo Urgencia:']");
            Label labelCama = (Label) txtFechaAtencion.getParent().getParent().getParent().lookup("Label[text='Tipo de Cama:']");
            Label labelMedico = (Label) txtFechaAtencion.getParent().getParent().getParent().lookup("Label[text='Médico Actual:']");
            Label labelEstado = (Label) txtFechaAtencion.getParent().getParent().getParent().getParent().getParent().lookup("Label[text='Indique si el paciente...']");

            if (labelFecha != null) labelFecha.setVisible(true);
            if (labelHora != null) labelHora.setVisible(true);
            if (labelTipoUrgencia != null) labelTipoUrgencia.setVisible(true);
            if (labelMotivo != null) labelMotivo.setVisible(true);
            if (labelCama != null) labelCama.setVisible(true);
            if (labelMedico != null) labelMedico.setVisible(true);
            if (labelEstado != null) labelEstado.setVisible(true);
        });
    }



    private void habilitarSeccionNuevaInformacion() {
        cmbTipoUrgencia.setDisable(false);
        cmbMotivoUrgencia.setDisable(false);
        cmbTipoCama.setDisable(false);
        cmbMedicoActual.setDisable(false);
        rbObservacion.setDisable(false);
        rbAltaMedica.setDisable(false);

        cmbTipoUrgencia.setStyle("");
        cmbMotivoUrgencia.setStyle("");
        cmbTipoCama.setStyle("");
        cmbMedicoActual.setStyle("");
    }

    @FXML
    private void abrirNotaMedica() {
        if (pacienteEgresado()) {
            mostrarAlerta("Error", "No se pueden crear notas para pacientes egresados", Alert.AlertType.WARNING);
            return;
        }

        if (!sesion.puedeCrearNuevaNota(folioPaciente)) {
            Integer idNotaExistente = sesion.obtenerNotaTemporalExistente(folioPaciente);
            if (idNotaExistente != null) {
                Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                alerta.setTitle("Nota Temporal Existente");
                alerta.setHeaderText("Ya tiene una nota temporal en este paciente");
                alerta.setContentText("¿Desea editar la nota temporal existente en lugar de crear una nueva?");
                Optional<ButtonType> resultado = alerta.showAndWait();
                if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                    cargarYEditarNotaExistente(idNotaExistente);
                    return;
                } else {
                    mostrarAlerta("Nota Temporal Existente", "Ya tiene una nota temporal en este paciente.\n\nPara crear una nueva nota, debe primero:\n1. Editar su nota temporal existente\n2. Guardarla como DEFINITIVA\n3. Luego podrá crear una nueva nota", Alert.AlertType.INFORMATION);
                    return;
                }
            }
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/nota_medica.fxml"));
            Parent root = loader.load();
            NotaMedicaController controller = loader.getController();
            controller.setFolioPaciente(folioPaciente);
            controller.setUsuarioLogueado(usuarioLogueado, rolUsuarioLogueado);

            if (!esMedicoInterconsulta() && capturaGuardada) {
                controller.setDatosCaptura(cmbTipoUrgencia.getValue(), cmbMotivoUrgencia.getValue(), cmbTipoCama.getValue(), cmbMedicoActual.getValue());
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Nueva Nota Médica - Folio: " + folioPaciente);
            stage.setOnHidden(e -> {
                Platform.runLater(() -> {
                    cargarNotasDelPaciente();
                    cargarContadores();
                });
            });
            stage.show();
        } catch (Exception e) {
            System.err.println("Error abriendo nota médica: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir la ventana de nota médica", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void abrirInterconsulta() {
        if (pacienteEgresado()) {
            mostrarAlerta("Error", "No se pueden crear interconsultas para pacientes egresados", Alert.AlertType.WARNING);
            return;
        }

        if (!sesion.puedeCrearNuevaInterconsulta(folioPaciente)) {
            mostrarAlerta("Interconsulta Temporal Existente", "Ya tiene una interconsulta temporal en este paciente.\n\nDebe editar o finalizar la interconsulta existente antes de crear una nueva.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/interconsulta.fxml"));
            Parent root = loader.load();
            InterconsultaController controller = loader.getController();
            controller.setFolioPaciente(folioPaciente);
            controller.setUsuarioLogueado(usuarioLogueado, rolUsuarioLogueado);

            if (!esMedicoInterconsulta() && capturaGuardada) {
                controller.setDatosCaptura(cmbTipoUrgencia.getValue(), cmbMotivoUrgencia.getValue(), cmbTipoCama.getValue(), cmbMedicoActual.getValue());
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Interconsulta - Folio: " + folioPaciente);
            stage.setOnHidden(e -> {
                Platform.runLater(() -> {
                    cargarInterconsultasDelPaciente();
                    cargarContadores();
                });
            });
            stage.show();
        } catch (Exception e) {
            System.err.println("Error abriendo interconsulta: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir la ventana de interconsulta", Alert.AlertType.ERROR);
        }
    }



//  de aqui pa rriba

    // ==================== CARGA DE COMBOS Y CONFIGURACIÓN ====================

    private void cargarCombos() {
        try (Connection conn = ConexionBD.conectar()) {
            // TIPOS DE URGENCIA
            cargarComboDesdeTabla(conn, "tblt_cveurg", "Descripcion", cmbTipoUrgencia);
            // MOTIVOS DE URGENCIA
            cargarComboDesdeTabla(conn, "tblt_cvemotatn", "Descripcion", cmbMotivoUrgencia);
            // TIPOS DE CAMA
            cargarComboDesdeTabla(conn, "tblt_cvecama", "Descripcion", cmbTipoCama);
            // MÉDICOS
            cargarComboDesdeTabla(conn, "tb_medicos", "Med_nombre", cmbMedicoActual);

            System.out.println(" Combos cargados correctamente");
        } catch (SQLException e) {
            System.err.println(" Error cargando combos: " + e.getMessage());
        }
    }

    private void cargarComboDesdeTabla(Connection conn, String tabla, String columna, ComboBox<String> combo) throws SQLException {
        String sql = "SELECT " + columna + " FROM " + tabla + " ORDER BY " + columna;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            combo.getItems().clear();
            while (rs.next()) {
                String valor = rs.getString(columna);
                if (valor != null && !valor.trim().isEmpty()) {
                    combo.getItems().add(valor);
                }
            }
        }
    }

    private void cargarContadores() {
        new Thread(() -> {
            try (Connection conn = ConexionBD.conectar()) {
                // CONTAR NOTAS (INCLUYENDO TEMPORALES)
                String sqlNotas = "SELECT COUNT(*) as total FROM tb_notas WHERE Folio = ? AND (Estado = 'DEFINITIVA' OR Estado = 'TEMPORAL')";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlNotas)) {
                    pstmt.setInt(1, folioPaciente);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        int total = rs.getInt("total");
                        Platform.runLater(() -> lblContadorNotas.setText(String.valueOf(total)));
                    }
                }

                // CONTAR INTERCONSULTAS (INCLUYENDO TEMPORALES)
                String sqlInter = "SELECT COUNT(*) as total FROM tb_inter WHERE Folio = ? AND (Estado = 'DEFINITIVA' OR Estado = 'TEMPORAL')";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInter)) {
                    pstmt.setInt(1, folioPaciente);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        int total = rs.getInt("total");
                        Platform.runLater(() -> lblContadorInterconsultas.setText(String.valueOf(total)));
                    }
                }
            } catch (SQLException e) {
                System.err.println(" Error cargando contadores: " + e.getMessage());
            }
        }).start();
    }

    private void configurarFechaHoraActual() {
        Date ahora = new Date();
        txtFechaAtencion.setText(new SimpleDateFormat("yyyy-MM-dd").format(ahora));
        txtHoraAtencion.setText(new SimpleDateFormat("HH:mm:ss").format(ahora));
    }

    private void iniciarRelojTiempoReal() {
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            Date ahora = new Date();
            txtFechaAtencion.setText(new SimpleDateFormat("yyyy-MM-dd").format(ahora));
            txtHoraAtencion.setText(new SimpleDateFormat("HH:mm:ss").format(ahora));
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }

    private void actualizarCedulaMedico() {
        String medicoSeleccionado = cmbMedicoActual.getValue();
        if (medicoSeleccionado != null) {
            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT Ced_prof FROM tb_medicos WHERE Med_nombre = ?")) {

                pstmt.setString(1, medicoSeleccionado);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    txtCedulaMedico.setText(rs.getString("Ced_prof"));
                }
            } catch (SQLException e) {
                System.err.println(" Error obteniendo cédula: " + e.getMessage());
            }
        }
    }

    // ==================== CONFIGURACIÓN DE TABLAS MEJORADA ====================

    private void configurarColumnasTablas() {
        if (tablaNotasMedicas != null) {
            tablaNotasMedicas.getColumns().clear();

            // COLUMNA NÚMERO DE NOTA
            TableColumn<NotaMedicaVO, Integer> colNumeroNota = new TableColumn<>("No. Nota");
            colNumeroNota.setCellValueFactory(new PropertyValueFactory<>("numeroNota"));
            colNumeroNota.setPrefWidth(80);

            // COLUMNA SÍNTOMAS
            TableColumn<NotaMedicaVO, String> colSintomas = new TableColumn<>("Síntomas");
            colSintomas.setCellFactory(tc -> crearCeldaConTooltip());
            colSintomas.setCellValueFactory(cellData -> {
                String sintomas = obtenerSintomasNota(cellData.getValue().getIdNota());
                return new SimpleStringProperty(sintomas);
            });
            colSintomas.setPrefWidth(220);

            // COLUMNA MÉDICO
            TableColumn<NotaMedicaVO, String> colMedico = new TableColumn<>("Médico");
            colMedico.setCellValueFactory(new PropertyValueFactory<>("medicoAutor"));
            colMedico.setPrefWidth(150);

            // COLUMNA FECHA/HORA
            TableColumn<NotaMedicaVO, String> colFecha = new TableColumn<>("Fecha/Hora");
            colFecha.setCellValueFactory(cellData -> {
                NotaMedicaVO nota = cellData.getValue();
                if (nota != null && nota.getFechaCreacion() != null) {
                    String fechaHora = nota.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    return new SimpleStringProperty(fechaHora);
                }
                return new SimpleStringProperty("N/A");
            });
            colFecha.setPrefWidth(120);

            // COLUMNA ESTADO
            TableColumn<NotaMedicaVO, String> colEstado = new TableColumn<>("Estado");
            colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
            colEstado.setPrefWidth(90);

            // COLUMNA ¿EDITABLE?
            TableColumn<NotaMedicaVO, String> colEditable = new TableColumn<>("¿Editable?");
            colEditable.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().isEditablePorMedico() ? "SÍ" : "NO"));
            colEditable.setPrefWidth(70);

            tablaNotasMedicas.getColumns().addAll(colNumeroNota, colSintomas, colMedico, colFecha, colEstado, colEditable);
        }
    }

    private TableCell<NotaMedicaVO, String> crearCeldaConTooltip() {
        return new TableCell<NotaMedicaVO, String>() {
            private final Tooltip tooltip = new Tooltip();
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setMaxWidth(200);
                label.setPrefWidth(200);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        mostrarSintomasCompletos(getItem());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    String preview = item.length() > 60 ? item.substring(0, 60) + "..." : item;
                    label.setText(preview);
                    setGraphic(label);
                    tooltip.setText(item);
                    setTooltip(tooltip);
                }
            }
        };
    }

    private void configurarColumnasInterconsultas() {
        if (tablaInterconsultas != null) {
            tablaInterconsultas.getColumns().clear();

            // COLUMNA NÚMERO INTERCONSULTA
            TableColumn<InterconsultaVO, Integer> colNumeroInter = new TableColumn<>("No. Interconsulta");
            colNumeroInter.setCellValueFactory(new PropertyValueFactory<>("numeroInterconsulta"));
            colNumeroInter.setPrefWidth(110);

            // COLUMNA SÍNTOMAS
            TableColumn<InterconsultaVO, String> colSintomasInter = new TableColumn<>("Síntomas");
            colSintomasInter.setCellFactory(tc -> crearCeldaConTooltipInterconsulta());
            //colSintomasInter.setCellFactory(tc -> crearCeldaConTooltip());
            colSintomasInter.setCellValueFactory(cellData -> {
                String sintomas = obtenerSintomasInterconsulta(cellData.getValue().getIdInterconsulta());
                return new SimpleStringProperty(sintomas);
            });
            colSintomasInter.setPrefWidth(220);

            // COLUMNA ESPECIALISTA
            TableColumn<InterconsultaVO, String> colEspecialista = new TableColumn<>("Especialista");
            colEspecialista.setCellValueFactory(new PropertyValueFactory<>("especialista"));
            colEspecialista.setPrefWidth(150);

            // COLUMNA FECHA/HORA
            TableColumn<InterconsultaVO, String> colFechaInter = new TableColumn<>("Fecha/Hora");
            colFechaInter.setCellValueFactory(cellData -> {
                InterconsultaVO interconsulta = cellData.getValue();
                if (interconsulta != null && interconsulta.getFechaCreacion() != null) {
                    String fechaHora = interconsulta.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    return new SimpleStringProperty(fechaHora);
                }
                return new SimpleStringProperty("N/A");
            });
            colFechaInter.setPrefWidth(120);

            // COLUMNA ESTADO
            TableColumn<InterconsultaVO, String> colEstadoInter = new TableColumn<>("Estado");
            colEstadoInter.setCellValueFactory(new PropertyValueFactory<>("estado"));
            colEstadoInter.setPrefWidth(90);

            tablaInterconsultas.getColumns().addAll(colNumeroInter, colSintomasInter, colEspecialista, colFechaInter, colEstadoInter);
        }
    }

    private TableCell<InterconsultaVO, String> crearCeldaConTooltipInterconsulta() {
        return new TableCell<InterconsultaVO, String>() {
            private final Tooltip tooltip = new Tooltip();
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setMaxWidth(200);
                label.setPrefWidth(200);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        mostrarSintomasCompletos(getItem());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    String preview = item.length() > 60 ? item.substring(0, 60) + "..." : item;
                    label.setText(preview);
                    setGraphic(label);
                    tooltip.setText(item);
                    setTooltip(tooltip);
                }
            }
        };
    }

    // ==================== MÉTODOS AUXILIARES PARA TABLAS ====================

    private String obtenerSintomasNota(int idNota) {
        String sql = "SELECT sintomas FROM tb_notas WHERE id_nota = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idNota);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String sintomas = rs.getString("sintomas");
                return sintomas != null ? sintomas : "Sin síntomas registrados";
            }
        } catch (SQLException e) {
            System.err.println(" Error obteniendo síntomas: " + e.getMessage());
        }
        return "Error al cargar";
    }

    private String obtenerSintomasInterconsulta(int idInterconsulta) {
        String sql = "SELECT sintomas FROM tb_inter WHERE id_inter = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idInterconsulta);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String sintomas = rs.getString("sintomas");
                return sintomas != null ? sintomas : "Sin síntomas registrados";
            }
        } catch (SQLException e) {
            System.err.println(" Error obteniendo síntomas de interconsulta: " + e.getMessage());
        }
        return "Error al cargar";
    }

    private void mostrarSintomasCompletos(String sintomas) {
        TextArea textArea = new TextArea(sintomas);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(500, 300);

        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        VBox vbox = new VBox(10, new Label("SÍNTOMAS COMPLETOS"), scrollPane);
        vbox.setPadding(new javafx.geometry.Insets(15));

        Stage stage = new Stage();
        stage.setTitle("Visualización de Síntomas");
        stage.setScene(new Scene(vbox, 550, 400));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }

// CONTINÚA EN LA TERCERA PARTE...


    // ==================== SISTEMA DE PERMISOS Y EDICIÓN CORREGIDO ====================

    private void onNotaSeleccionada() {
        configurarVisibilidadBotonesSegunNotaSeleccionada();
    }

    private void onInterconsultaSeleccionada() {
        configurarVisibilidadBotonesInterconsulta();
    }

    private void configurarVisibilidadBotonesSegunNotaSeleccionada() {
        if (tablaNotasMedicas == null || tablaNotasMedicas.getSelectionModel().getSelectedItem() == null) {
            btnEditarNotaMedica.setVisible(false);
            btnOtorgarPermiso.setVisible(false);
            return;
        }

        NotaMedicaVO notaSeleccionada = tablaNotasMedicas.getSelectionModel().getSelectedItem();

        boolean esAdmin = "ADMIN".equals(rolUsuarioLogueado);
        boolean esJefatura = "JEFATURA_URGENCIAS".equals(rolUsuarioLogueado);

        // ADMIN y JEFATURA siempre pueden ver ambos botones para CUALQUIER nota
        if (esAdmin || esJefatura) {
            btnEditarNotaMedica.setVisible(true);
            btnEditarNotaMedica.setDisable(false);
            btnOtorgarPermiso.setVisible(true);
            btnOtorgarPermiso.setDisable(false);

            if (esAdmin) {
                btnEditarNotaMedica.setText(" Editar (Admin)");
            } else {
                btnEditarNotaMedica.setText(" Editar (Jefatura)");
            }
            return;
        }

        // PARA MÉDICOS NORMALES
        boolean puedeEditar = sesion.puedeEditarNota(notaSeleccionada.getMedicoAutor());
        boolean esDueñoNota = notaSeleccionada.getMedicoAutor().equals(sesion.getNombreMedico());
        boolean notaEsTemporal = "TEMPORAL".equals(notaSeleccionada.getEstado());
        boolean tienePermiso = notaSeleccionada.isEditablePorMedico();

        // Mostrar botón de editar solo si:
        // 1. Es dueño de la nota Y es temporal
        // 2. O tiene permiso de edición otorgado
        boolean mostrarEditar = (esDueñoNota && notaEsTemporal) || tienePermiso;

        btnEditarNotaMedica.setVisible(mostrarEditar);
        btnEditarNotaMedica.setDisable(!mostrarEditar);

        // Médicos normales NO pueden otorgar permisos
        btnOtorgarPermiso.setVisible(false);
        btnOtorgarPermiso.setDisable(true);
    }

    private void configurarVisibilidadBotonesInterconsulta() {
        if (tablaInterconsultas == null || tablaInterconsultas.getSelectionModel().getSelectedItem() == null) {
            if (btnEditarInterconsulta != null) {
                btnEditarInterconsulta.setVisible(false);
                btnEditarInterconsulta.setDisable(true);
            }
            if (btnOtorgarPermisoInterconsulta != null) {
                btnOtorgarPermisoInterconsulta.setVisible(false);
                btnOtorgarPermisoInterconsulta.setDisable(true);
            }
            return;
        }

        InterconsultaVO interconsultaSeleccionada = tablaInterconsultas.getSelectionModel().getSelectedItem();

        boolean esAdmin = "ADMIN".equals(rolUsuarioLogueado);
        boolean esJefatura = "JEFATURA_URGENCIAS".equals(rolUsuarioLogueado);

        // ADMIN y JEFATURA siempre pueden ver
        if (esAdmin || esJefatura) {
            btnEditarInterconsulta.setVisible(true);
            btnEditarInterconsulta.setDisable(false);
            btnOtorgarPermisoInterconsulta.setVisible(true);
            btnOtorgarPermisoInterconsulta.setDisable(false);
            return;
        }

        // PARA ESPECIALISTAS NORMALES
        boolean esDueñoInterconsulta = interconsultaSeleccionada.getEspecialista().equals(sesion.getNombreMedico());
        boolean interconsultaEsTemporal = "TEMPORAL".equals(interconsultaSeleccionada.getEstado());
        boolean tienePermiso = interconsultaSeleccionada.isEditablePorMedico();

        // Mostrar botón de editar solo si:
        // 1. Es dueño de la interconsulta Y es temporal
        // 2. O tiene permiso de edición otorgado
        boolean mostrarEditar = (esDueñoInterconsulta && interconsultaEsTemporal) || tienePermiso;

        btnEditarInterconsulta.setVisible(mostrarEditar);
        btnEditarInterconsulta.setDisable(!mostrarEditar);
        btnOtorgarPermisoInterconsulta.setVisible(false);
    }

    private boolean otorgarPermisoInterconsultaEnBD(int idInterconsulta) {
        String sql = "UPDATE tb_inter SET " +
                "editable_por_medico = TRUE, " +
                "permiso_edicion_otorgado_por = ?, " +
                "fecha_permiso_edicion = NOW(), " +
                "fecha_edicion_realizada = NULL " +
                "WHERE id_inter = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuarioLogueado);
            pstmt.setInt(2, idInterconsulta);

            int filas = pstmt.executeUpdate();

            if (filas > 0) {
                System.out.println(" Permiso otorgado para interconsulta - ID: " + idInterconsulta);

                //  REGISTRAR EN HISTORIAL
                registrarEnHistorialPermisosInterconsulta(idInterconsulta, "INTERCONSULTA", "OTORGAR");
                return true;
            } else {
                System.out.println(" No se pudo otorgar permiso para interconsulta");
                return false;
            }

        } catch (SQLException e) {
            System.err.println(" Error otorgando permiso para interconsulta: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void editarNotaMedica() {
        if (tablaNotasMedicas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una nota primero", Alert.AlertType.WARNING);
            return;
        }

        NotaMedicaVO notaSeleccionada = tablaNotasMedicas.getSelectionModel().getSelectedItem();

        boolean esAdmin = "ADMIN".equals(rolUsuarioLogueado);
        boolean esJefatura = "JEFATURA_URGENCIAS".equals(rolUsuarioLogueado);

        // ADMIN y JEFATURA siempre pueden editar CUALQUIER nota
        boolean puedeEditar = esAdmin || esJefatura;

        // Si no es admin/jefatura, verificar permisos normales
        if (!puedeEditar) {
            puedeEditar = sesion.puedeEditarNota(notaSeleccionada.getMedicoAutor());
        }

        if (!puedeEditar) {
            mostrarAlerta("Sin permisos",
                    "No tiene permisos para editar esta nota.",
                    Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/nota_medica.fxml"));
            Parent root = loader.load();

            NotaMedicaController controller = loader.getController();
            controller.setFolioPaciente(folioPaciente);
            controller.setUsuarioLogueado(usuarioLogueado, rolUsuarioLogueado);

            // ✅ Llamar DIRECTAMENTE a setModoEdicion - YA EXISTE
            controller.setModoEdicion(notaSeleccionada);

            Stage stage = new Stage();
            stage.setTitle("Editar Nota Médica - Folio: " + folioPaciente);
            stage.setScene(new Scene(root, 700, 600));

            stage.setOnHidden(e -> {
                System.out.println("Actualizando lista después de edición...");
                forzarActualizacionCompleta();
            });

            stage.show();

        } catch (Exception e) {
            System.err.println("Error abriendo editor de nota: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir el editor", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void editarInterconsulta() {
        if (tablaInterconsultas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una interconsulta primero", Alert.AlertType.WARNING);
            return;
        }

        InterconsultaVO interconsultaSeleccionada = tablaInterconsultas.getSelectionModel().getSelectedItem();

        boolean esAdmin = "ADMIN".equals(rolUsuarioLogueado);
        boolean esJefatura = "JEFATURA_URGENCIAS".equals(rolUsuarioLogueado);

        // ADMIN y JEFATURA siempre pueden editar CUALQUIER interconsulta
        boolean puedeEditar = esAdmin || esJefatura;

        // Si no es admin/jefatura, verificar permisos normales
        if (!puedeEditar) {
            puedeEditar = sesion.puedeEditarInterconsulta(interconsultaSeleccionada.getEspecialista());
        }

        if (!puedeEditar) {
            mostrarAlerta("Sin permisos",
                    "No tiene permisos para editar esta interconsulta.",
                    Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/interconsulta.fxml"));
            Parent root = loader.load();

            InterconsultaController controller = loader.getController();
            controller.setFolioPaciente(folioPaciente);
            controller.setUsuarioLogueado(usuarioLogueado, rolUsuarioLogueado);

            // lamar DIRECTAMENTE a setModoEdicion -
            controller.setModoEdicion(interconsultaSeleccionada);

            Stage stage = new Stage();
            stage.setTitle("Editar Interconsulta - Folio: " + folioPaciente);
            stage.setScene(new Scene(root, 600, 450));

            stage.setOnHidden(e -> forzarActualizacionCompleta());
            stage.show();

        } catch (Exception e) {
            System.err.println("Error abriendo editor de interconsulta: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir el editor de interconsulta", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void guardarGeneral() {
        if (!validarCampos()) return;

        if (guardarCapturaCompleta()) {
            String estado = obtenerEstadoSeleccionado();

            if ("OBSERVACION".equals(estado)) {
                actualizarEstadoPaciente(2);
                mostrarAlerta("Éxito",
                        "Paciente movido a OBSERVACIÓN",
                        Alert.AlertType.INFORMATION);
                deshabilitarCamposNuevaInformacion();
                configurarBotonesSegunEstadoPaciente();
            } else if ("EGRESADO".equals(estado)) {
                abrirVentanaEgreso();
            }}}

    private boolean guardarCapturaCompleta() {
        Connection conn = null;
        try {
            conn = ConexionBD.conectar();
            conn.setAutoCommit(false);

            String tipoUrgencia = cmbTipoUrgencia.getValue();
            String motivoUrgencia = cmbMotivoUrgencia.getValue();
            String tipoCama = cmbTipoCama.getValue();
            String medico = cmbMedicoActual.getValue();

            String sql = "UPDATE tb_urgencias SET Tipo_urg = ?, Motivo_urg = ?, Tipo_cama = ?, Cve_med = ?, Nom_med = ?, Fecha_atencion = NOW(), Hora_atencion = CURTIME() WHERE Folio = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, obtenerClaveDesdeDescripcion("tblt_cveurg", "Descripcion", tipoUrgencia));
                pstmt.setInt(2, obtenerClaveDesdeDescripcion("tblt_cvemotatn", "Descripcion", motivoUrgencia));
                pstmt.setInt(3, obtenerClaveDesdeDescripcion("tblt_cvecama", "Descripcion", tipoCama));
                pstmt.setInt(4, obtenerClaveMedico(medico));
                pstmt.setString(5, medico);
                pstmt.setInt(6, folioPaciente);

                int filas = pstmt.executeUpdate();

                if (filas > 0) {
                    conn.commit();
                    deshabilitarCamposNuevaInformacion();
                    capturaGuardada = true;

                    // NUEVA LÍNEA: Actualizar botones según estado
                    configurarBotonesSegunEstadoPaciente();

                    System.out.println("Captura completa guardada - Folio: " + folioPaciente);
                    return true;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error guardando captura: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error en rollback: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error cerrando conexión: " + e.getMessage());
            }
        }
        return false;
    }

    private void verificarNotasTemporalesPropias() {
        try (Connection conn = ConexionBD.conectar()) {
            String nombreMedicoActual = sesion.getNombreMedico();

            // VERIFICAR NOTAS TEMPORALES PROPIAS
            String sqlNotas = "SELECT COUNT(*) as temp FROM tb_notas WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlNotas)) {
                pstmt.setInt(1, folioPaciente);
                pstmt.setString(2, nombreMedicoActual);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next() && rs.getInt("temp") > 0) {
                    cargarDatosNuevaInfoDesdeBD();
                    deshabilitarCamposNuevaInformacion();
                    habilitarBotonesNotas();
                    capturaGuardada = true;
                    System.out.println("Notas temporales PROPIAS detectadas - Usuario: " + nombreMedicoActual);
                }
            }

            // VERIFICAR INTERCONSULTAS TEMPORALES PROPIAS
            String sqlInter = "SELECT COUNT(*) as temp FROM tb_inter WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInter)) {
                pstmt.setInt(1, folioPaciente);
                pstmt.setString(2, nombreMedicoActual);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next() && rs.getInt("temp") > 0) {
                    if (!capturaGuardada) {
                        cargarDatosNuevaInfoDesdeBD();
                        deshabilitarCamposNuevaInformacion();
                        habilitarBotonesNotas();
                        capturaGuardada = true;
                    }
                    System.out.println(" Interconsultas temporales PROPIAS detectadas - Usuario: " + nombreMedicoActual);
                }
            }

        } catch (SQLException e) {
            System.err.println(" Error verificando notas temporales propias: " + e.getMessage());
        }
    }

    // ==================== MÉTODOS DE CARGA DESDE BD ====================

    private void cargarNotasDelPaciente() {
        String sql = "SELECT id_nota, Folio, Num_nota, Nota, Indicaciones, sintomas, signos_vitales, diagnostico, " +
                "Medico, Cedula, Fecha, Hora, Estado, estado_paciente, " +
                "editable_por_medico, permiso_edicion_otorgado_por, " +
                "fecha_permiso_edicion, rol_usuario_otorga, fecha_edicion_realizada " +
                "FROM tb_notas WHERE Folio = ? ORDER BY Num_nota DESC";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folioPaciente);
            ResultSet rs = pstmt.executeQuery();

            notasData.clear();

            while (rs.next()) {
                // CONVERTIR FECHA Y HORA CORRECTAMENTE
                LocalDateTime fechaHora = obtenerFechaHoraDesdeBD(rs.getString("Fecha"), rs.getString("Hora"));

                NotaMedicaVO nota = new NotaMedicaVO(
                        rs.getInt("id_nota"),
                        rs.getInt("Folio"),
                        rs.getInt("Num_nota"),
                        rs.getString("Nota"),
                        rs.getString("Medico"),
                        rs.getString("Cedula"),
                        fechaHora,
                        rs.getString("Estado"),
                        rs.getString("estado_paciente"),
                        rs.getBoolean("editable_por_medico"),
                        rs.getString("permiso_edicion_otorgado_por"),
                        rs.getTimestamp("fecha_permiso_edicion") != null ?
                                rs.getTimestamp("fecha_permiso_edicion").toLocalDateTime() : null
                );

                nota.setIndicaciones(rs.getString("Indicaciones"));
                notasData.add(nota);
            }

            Platform.runLater(() -> {
                tablaNotasMedicas.setItems(notasData);
                tablaNotasMedicas.refresh();
                System.out.println("si " + notasData.size() + " notas cargadas para folio: " + folioPaciente);
            });

        } catch (SQLException e) {
            System.err.println(" Error cargando notas: " + e.getMessage());
        }
    }

    private void cargarInterconsultasDelPaciente() {
        String sql = "SELECT id_inter, Folio, Num_inter, Nota, sintomas, signos_vitales, diagnostico, especialidad, " +
                "Medico, Cedula, Fecha, Hora, Estado, estado_paciente, " +
                "editable_por_medico, permiso_edicion_otorgado_por, " +
                "fecha_permiso_edicion, fecha_edicion_realizada " +
                "FROM tb_inter WHERE Folio = ? ORDER BY Num_inter DESC";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folioPaciente);
            ResultSet rs = pstmt.executeQuery();

            interconsultasData.clear();

            while (rs.next()) {
                LocalDateTime fechaHora = obtenerFechaHoraDesdeBD(rs.getString("Fecha"), rs.getString("Hora"));

                InterconsultaVO interconsulta = new InterconsultaVO(
                        rs.getInt("id_inter"),
                        rs.getInt("Folio"),
                        rs.getInt("Num_inter"),
                        rs.getString("Nota"),
                        rs.getString("Medico"),
                        rs.getString("Cedula"),
                        fechaHora,
                        rs.getString("Estado"),
                        rs.getString("estado_paciente"),
                        rs.getBoolean("editable_por_medico"),
                        rs.getString("permiso_edicion_otorgado_por"),
                        rs.getTimestamp("fecha_permiso_edicion") != null ?
                                rs.getTimestamp("fecha_permiso_edicion").toLocalDateTime() : null
                );

                interconsultasData.add(interconsulta);
            }

            Platform.runLater(() -> {
                if (tablaInterconsultas != null) {
                    tablaInterconsultas.setItems(interconsultasData);
                    tablaInterconsultas.refresh();
                }
                System.out.println("si " + interconsultasData.size() + " interconsultas cargadas para folio: " + folioPaciente);
            });

        } catch (SQLException e) {
            System.err.println(" Error cargando interconsultas: " + e.getMessage());
        }
    }

    private LocalDateTime obtenerFechaHoraDesdeBD(String fechaBD, String horaBD) {
        if (fechaBD == null) return null;

        try {
            // Formato: "2024-01-15 14:30:00"
            if (fechaBD.contains(" ")) {
                // Ya tiene fecha y hora juntas
                String fechaHora = fechaBD.replace(" ", "T");
                return LocalDateTime.parse(fechaHora);
            }

            // Si viene separado, combinar
            if (horaBD != null) {
                String fechaHora = fechaBD + "T" + horaBD;
                return LocalDateTime.parse(fechaHora);
            }

            // Solo fecha
            return LocalDate.parse(fechaBD).atStartOfDay();

        } catch (Exception e) {
            System.err.println("Error parseando fecha/hora: " + fechaBD + " " + horaBD);
            return null;
        }
    }

    private void cargarYEditarNotaExistente(int idNota) {
        try {
            NotaMedicaVO notaExistente = null;
            for (NotaMedicaVO nota : notasData) {
                if (nota.getIdNota() == idNota) {
                    notaExistente = nota;
                    break;
                }
            }

            if (notaExistente != null) {
                tablaNotasMedicas.getSelectionModel().select(notaExistente);
                editarNotaMedica();
            }
        } catch (Exception e) {
            System.err.println(" Error cargando nota existente: " + e.getMessage());
        }
    }

    private void cargarDatosNuevaInfoDesdeBD() {
        String sql = "SELECT Tipo_urg, Motivo_urg, Tipo_cama, Cve_med, Nom_med, Estado_pac FROM tb_urgencias WHERE Folio = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folioPaciente);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int estadoActual = rs.getInt("Estado_pac");

                // Si ya tiene datos guardados, cargarlos
                int tipoUrgenciaKey = rs.getInt("Tipo_urg");
                if (tipoUrgenciaKey > 0) {
                    String tipoUrgencia = obtenerDescripcionDesdeClave("tblt_cveurg", "Cve_urg", "Descripcion", tipoUrgenciaKey);
                    cmbTipoUrgencia.setValue(tipoUrgencia);
                }

                int motivoUrgenciaKey = rs.getInt("Motivo_urg");
                if (motivoUrgenciaKey > 0) {
                    String motivoUrgencia = obtenerDescripcionDesdeClave("tblt_cvemotatn", "Cve_motatn", "Descripcion", motivoUrgenciaKey);
                    cmbMotivoUrgencia.setValue(motivoUrgencia);
                }

                int tipoCamaKey = rs.getInt("Tipo_cama");
                if (tipoCamaKey > 0) {
                    String tipoCama = obtenerDescripcionDesdeClave("tblt_cvecama", "Cve_cama", "Descripcion", tipoCamaKey);
                    cmbTipoCama.setValue(tipoCama);
                }

                String medico = rs.getString("Nom_med");
                if (medico != null) {
                    cmbMedicoActual.setValue(medico);
                    actualizarCedulaMedico();
                }

                // Configurar RadioButtons según estado actual
                if (estadoActual == 2) { // Observación
                    rbObservacion.setSelected(true);
                } else if (estadoActual == 3) { // Egresado
                    rbAltaMedica.setSelected(true);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error cargando datos de Nueva Info: " + e.getMessage());
        }
    }

    private String obtenerDescripcionDesdeClave(String tabla, String columnaClave, String columnaDesc, int clave) {
        if (clave <= 0) return null;

        String sql = "SELECT " + columnaDesc + " FROM " + tabla + " WHERE " + columnaClave + " = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clave);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString(columnaDesc);
            }
        } catch (SQLException e) {
            System.err.println(" Error obteniendo descripción para clave " + clave + ": " + e.getMessage());
        }
        return null;
    }

    private void deshabilitarCamposNuevaInformacion() {
        cmbTipoUrgencia.setDisable(true);
        cmbMotivoUrgencia.setDisable(true);
        cmbTipoCama.setDisable(true);
        cmbMedicoActual.setDisable(true);
        rbObservacion.setDisable(true);
        rbAltaMedica.setDisable(true);

        String estiloGris = "-fx-opacity: 0.7; -fx-background-color: #f5f5f5; -fx-text-fill: #666666;";
        cmbTipoUrgencia.setStyle(estiloGris);
        cmbMotivoUrgencia.setStyle(estiloGris);
        cmbTipoCama.setStyle(estiloGris);
        cmbMedicoActual.setStyle(estiloGris);
    }

    private void habilitarBotonesNotas() {
        btnNuevaNotaMedica.setDisable(false);
        btnNuevaInterconsulta.setDisable(false);
        btnNuevaNotaMedica.setStyle("-fx-background-color: #002366; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevaInterconsulta.setStyle("-fx-background-color: #002366; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    private void forzarActualizacionCompleta() {
        cargarNotasDelPaciente();
        cargarInterconsultasDelPaciente();
        cargarContadores();

        if (tablaNotasMedicas != null) {
            tablaNotasMedicas.refresh();
        }
    }

    private String obtenerEstadoSeleccionado() {
        if (rbObservacion.isSelected()) return "OBSERVACION";
        if (rbAltaMedica.isSelected()) return "EGRESADO";
        return null;
    }

    private boolean validarCampos() {
        if (cmbTipoUrgencia.getValue() == null) {
            mostrarAlerta("Error", "Seleccione el tipo de urgencia", Alert.AlertType.ERROR);
            return false;
        }
        if (cmbMotivoUrgencia.getValue() == null) {
            mostrarAlerta("Error", "Seleccione el motivo de urgencia", Alert.AlertType.ERROR);
            return false;
        }
        if (cmbTipoCama.getValue() == null) {
            mostrarAlerta("Error", "Seleccione el tipo de cama", Alert.AlertType.ERROR);
            return false;
        }
        if (cmbMedicoActual.getValue() == null) {
            mostrarAlerta("Error", "Seleccione el médico actual", Alert.AlertType.ERROR);
            return false;
        }
        if (obtenerEstadoSeleccionado() == null) {
            mostrarAlerta("Error", "Seleccione el estado del paciente", Alert.AlertType.ERROR);
            return false;
        }
        return true;
    }

    private int obtenerClaveDesdeDescripcion(String tabla, String columnaDesc, String descripcion) {
        if (descripcion == null) return -1;

        String sql = "SELECT * FROM " + tabla + " WHERE " + columnaDesc + " = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, descripcion);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                switch(tabla) {
                    case "tblt_cveurg": return rs.getInt("Cve_urg");
                    case "tblt_cvemotatn": return rs.getInt("Cve_motatn");
                    case "tblt_cvecama": return rs.getInt("Cve_cama");
                    default: return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println(" Error obteniendo clave para " + descripcion + ": " + e.getMessage());
        }
        return -1;
    }

    private int obtenerClaveMedico(String nombreMedico) {
        if (nombreMedico == null) return -1;

        String sql = "SELECT Cve_med FROM tb_medicos WHERE Med_nombre = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombreMedico);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("Cve_med");
            }
        } catch (SQLException e) {
            System.err.println(" Error obteniendo clave médico: " + e.getMessage());
        }
        return -1;
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }


    // nuevo flujo 17,12,25

    private boolean esMedicoInterconsulta() {
        return "MEDICO_ESPECIALISTA".equals(rolUsuarioLogueado);
    }

    private int obtenerEstadoPacienteActual() {
        String sql = "SELECT Estado_pac FROM tb_urgencias WHERE Folio = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, folioPaciente);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("Estado_pac");
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo estado del paciente: " + e.getMessage());
        }
        return 1;
    }

    private boolean pacienteEnEspera() {
        return obtenerEstadoPacienteActual() == 1;
    }

    private boolean pacienteEnObservacion() {
        return obtenerEstadoPacienteActual() == 2;
    }

    private boolean pacienteEgresado() {
        return obtenerEstadoPacienteActual() == 3;
    }

    private boolean actualizarEstadoPaciente(int nuevoEstado) {
        String sql = "UPDATE tb_urgencias SET Estado_pac = ? WHERE Folio = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, nuevoEstado);
            pstmt.setInt(2, folioPaciente);
            int filas = pstmt.executeUpdate();
            return filas > 0;
        } catch (SQLException e) {
            System.err.println("Error actualizando estado del paciente: " + e.getMessage());
            return false;
        }
    }


    private void configurarVisibilidadSegunRol() {
        if (esMedicoInterconsulta()) {
            ocultarSeccionNuevaInformacion();
        } else if (pacienteEgresado()) {
            // PACIENTE EGRESADO: ocultar TODO excepto RadioButton para EGREESADO
            ocultarSeccionParaEgresado();
        } else if (pacienteEnObservacion()) {
            // PACIENTE EN OBSERVACIÓN: mostrar pero solo RadioButton
            mostrarSoloRadioButtonsParaEgreso();
        }

        configurarBotonesSegunEstadoPaciente();

        if (pacienteEnObservacion() && !esMedicoInterconsulta()) {
            cargarDatosNuevaInfoDesdeBD();
            deshabilitarCamposNuevaInformacion();
            // Pero habilitar RadioButton para egreso
            rbAltaMedica.setDisable(false);
            rbAltaMedica.setVisible(true);
        }
    }

    private void ocultarSeccionNuevaInformacion() {
        Platform.runLater(() -> {
            txtFechaAtencion.setVisible(false);
            txtHoraAtencion.setVisible(false);
            txtCedulaMedico.setVisible(false);
            cmbTipoUrgencia.setVisible(false);
            cmbMotivoUrgencia.setVisible(false);
            cmbTipoCama.setVisible(false);
            cmbMedicoActual.setVisible(false);
            rbObservacion.setVisible(false);
            rbAltaMedica.setVisible(false);
            btnGuardarGeneral.setVisible(false);
        });
    }

    private void configurarBotonesSegunEstadoPaciente() {
        boolean pacienteEgresado = pacienteEgresado();

        Platform.runLater(() -> {
            if (pacienteEgresado) {
                btnNuevaNotaMedica.setDisable(true);
                btnNuevaInterconsulta.setDisable(true);
                btnNuevaNotaMedica.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666;");
                btnNuevaInterconsulta.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666;");
            } else {
                btnNuevaNotaMedica.setDisable(false);
                btnNuevaInterconsulta.setDisable(false);
                btnNuevaNotaMedica.setStyle("-fx-background-color: #002366; -fx-text-fill: white; -fx-font-weight: bold;");
                btnNuevaInterconsulta.setStyle("-fx-background-color: #002366; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        });
    }





    @FXML
    private void visualizarNotaMedica() {
        if (tablaNotasMedicas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una nota primero", Alert.AlertType.WARNING);
            return;
        }

        NotaMedicaVO nota = tablaNotasMedicas.getSelectionModel().getSelectedItem();
        System.out.println(" Visualizando nota #" + nota.getNumeroNota());

        try {
            // Llamar al PDFGenerator NUEVO (solo necesita folio y número de nota)
            boolean exito = PDFGenerator.generarNotaMedicaPDF(
                    folioPaciente,          // int folioPaciente
                    nota.getNumeroNota()    // int numeroNota
            );

            if (exito) {
                System.out.println(" PDF generado correctamente");
            } else {
                mostrarAlerta("Error", "No se pudo generar el PDF", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            System.err.println(" Error visualizando nota: " + e.getMessage());
            mostrarAlerta("Error", "Error al generar PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void visualizarInterconsulta() {
        if (tablaInterconsultas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una interconsulta primero", Alert.AlertType.WARNING);
            return;
        }

        InterconsultaVO interconsulta = tablaInterconsultas.getSelectionModel().getSelectedItem();
        System.out.println("📄 Visualizando interconsulta #" + interconsulta.getNumeroInterconsulta());

        try {
            // Llamar al PDFGenerator NUEVO (solo necesita folio y número de interconsulta)
            boolean exito = PDFGenerator.generarInterconsultaPDF(
                    folioPaciente,                      // int folioPaciente
                    interconsulta.getNumeroInterconsulta() // int numeroInterconsulta
            );

            if (exito) {
                System.out.println(" PDF de interconsulta generado correctamente");
            } else {
                mostrarAlerta("Error", "No se pudo generar el PDF de interconsulta", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            System.err.println(" Error visualizando interconsulta: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "Error al generar PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    /**
     * OBTENER DATOS DEL ESPECIALISTA PARA EL PDF - VERSIÓN SIMPLIFICADA Y CORRECTA
     */
    private Map<String, String> obtenerDatosEspecialistaParaPDF(String nombreEspecialista) {
        Map<String, String> datos = new HashMap<>();

        // Valores por defecto
        datos.put("especialidad", "ESPECIALISTA");
        datos.put("universidad", "No especificada");
        datos.put("cedula", "");

        if (nombreEspecialista == null || nombreEspecialista.trim().isEmpty()) {
            return datos;
        }

        System.out.println("Buscando especialista en tb_medesp: " + nombreEspecialista);

        // Buscar EXACTAMENTE en tb_medesp
        String sql = "SELECT especialidad, universidad, Cedula FROM tb_medesp WHERE Nombre = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombreEspecialista.trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // OBTENER ESPECIALIDAD (DEBE ESTAR EN LA BD)
                String especialidad = rs.getString("especialidad");
                if (especialidad != null && !especialidad.trim().isEmpty()) {
                    datos.put("especialidad", especialidad);
                    System.out.println(" Especialidad encontrada en BD: " + especialidad);
                } else {
                    System.out.println(" Especialidad VACÍA en BD para: " + nombreEspecialista);
                }

                // OBTENER UNIVERSIDAD
                String universidad = rs.getString("universidad");
                if (universidad != null && !universidad.trim().isEmpty()) {
                    datos.put("universidad", universidad);
                    System.out.println(" Universidad encontrada: " + universidad);
                }

                // OBTENER CÉDULA
                String cedula = rs.getString("Cedula");
                if (cedula != null && !cedula.trim().isEmpty()) {
                    datos.put("cedula", cedula);
                    System.out.println(" Cédula encontrada: " + cedula);
                }

            } else {
                System.out.println(" Especialista NO ENCONTRADO en tb_medesp: " + nombreEspecialista);

                // Mostrar qué especialistas SÍ hay en la base de datos para depurar
                mostrarEspecialistasEnBD(conn);
            }

        } catch (SQLException e) {
            System.err.println(" Error SQL obteniendo datos de especialista: " + e.getMessage());
            e.printStackTrace();
        }

        return datos;
    }

    private Map<String, String> obtenerDatosMedicoFallback(String nombreMedico) {
        Map<String, String> datos = new HashMap<>();
        datos.put("especialidad", "URGENCIAS"); // Valor por defecto
        datos.put("universidad", "No especificada");

        if (nombreMedico == null) return datos;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT Med_nombre FROM tb_medicos WHERE Med_nombre LIKE ? LIMIT 1")) {

            pstmt.setString(1, "%" + nombreMedico + "%");
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println(" Médico encontrado en tb_medicos (fallback): " + rs.getString("Med_nombre"));
            }

        } catch (SQLException e) {
            System.err.println("Error en fallback médico: " + e.getMessage());
        }

        return datos;
    }

    private Map<String, String> obtenerDatosNotaCompleta(int idNota) {
        Map<String, String> datos = new HashMap<>();
        String sql = "SELECT sintomas, signos_vitales, diagnostico, Nota, Indicaciones, Cedula, Fecha, Hora FROM tb_notas WHERE id_nota = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idNota);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                datos.put("sintomas", rs.getString("sintomas"));
                datos.put("signosVitales", rs.getString("signos_vitales"));
                datos.put("diagnostico", rs.getString("diagnostico"));
                datos.put("nota", rs.getString("Nota"));
                datos.put("indicaciones", rs.getString("Indicaciones"));
                datos.put("cedula", rs.getString("Cedula"));
                datos.put("fecha", rs.getString("Fecha"));
                datos.put("hora", rs.getString("Hora"));
            }

        } catch (SQLException e) {
            System.err.println(" Error obteniendo datos de nota: " + e.getMessage());
        }

        // Valores por defecto
        datos.putIfAbsent("sintomas", "");
        datos.putIfAbsent("signosVitales", "");
        datos.putIfAbsent("diagnostico", "");
        datos.putIfAbsent("nota", "");
        datos.putIfAbsent("indicaciones", "");
        datos.putIfAbsent("cedula", "");
        datos.putIfAbsent("fecha", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        datos.putIfAbsent("hora", new SimpleDateFormat("HH:mm").format(new Date()));

        return datos;
    }


    private Map<String, String> obtenerDatosInterconsultaCompleta(int idInterconsulta) {
        Map<String, String> datos = new HashMap<>();
        String sql = "SELECT sintomas, signos_vitales, diagnostico, Nota, especialidad, Cedula, Fecha, Hora FROM tb_inter WHERE id_inter = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idInterconsulta);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                datos.put("sintomas", rs.getString("sintomas"));
                datos.put("signosVitales", rs.getString("signos_vitales"));
                datos.put("diagnostico", rs.getString("diagnostico"));
                datos.put("nota", rs.getString("Nota"));
                datos.put("especialidad", rs.getString("especialidad"));
                datos.put("cedula", rs.getString("Cedula"));
                datos.put("fecha", rs.getString("Fecha"));
                datos.put("hora", rs.getString("Hora"));
            }

        } catch (SQLException e) {
            System.err.println(" Error obteniendo datos de interconsulta: " + e.getMessage());
        }

        // Valores por defecto
        datos.putIfAbsent("sintomas", "");
        datos.putIfAbsent("signosVitales", "");
        datos.putIfAbsent("diagnostico", "");
        datos.putIfAbsent("nota", "");
        datos.putIfAbsent("especialidad", "");
        datos.putIfAbsent("cedula", "");
        datos.putIfAbsent("fecha", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        datos.putIfAbsent("hora", new SimpleDateFormat("HH:mm").format(new Date()));

        return datos;
    }


    private void abrirPDFReciente() {
        try {
            File directorio = new File("pdfs");
            if (directorio.exists()) {
                File[] archivos = directorio.listFiles((dir, name) ->
                        name.startsWith("Nota_Medica_Folio_" + folioPaciente + "_"));

                if (archivos != null && archivos.length > 0) {
                    Arrays.sort(archivos, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    java.awt.Desktop.getDesktop().open(archivos[0]);
                    System.out.println(" Abriendo PDF: " + archivos[0].getName());
                } else {
                    mostrarAlerta("Info", "No se encontró el PDF generado", Alert.AlertType.INFORMATION);
                }
            } else {
                mostrarAlerta("Info", "No se encontró la carpeta de PDFs", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            System.err.println(" Error abriendo PDF: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir el PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private void abrirPDFInterconsultaReciente() {
        try {
            File directorio = new File("pdfs");
            if (directorio.exists()) {
                File[] archivos = directorio.listFiles((dir, name) ->
                        name.startsWith("Interconsulta_Folio_" + folioPaciente + "_"));

                if (archivos != null && archivos.length > 0) {
                    Arrays.sort(archivos, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    java.awt.Desktop.getDesktop().open(archivos[0]);
                    System.out.println(" Abriendo PDF de interconsulta: " + archivos[0].getName());
                } else {
                    mostrarAlerta("Info", "No se encontró el PDF de interconsulta", Alert.AlertType.INFORMATION);
                }
            } else {
                mostrarAlerta("Info", "No se encontró la carpeta de PDFs", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            System.err.println(" Error abriendo PDF de interconsulta: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir el PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private Map<String, String> obtenerDatosPacienteCompletos() {
        Map<String, String> datos = new HashMap<>();
        String sql = "SELECT u.*, " +
                "m.DESCRIP as NombreMunicipio, e.DESCRIP as NombreEntidad, " +
                "dh.Derechohabiencia as NombreDerechohabiencia, " +
                "s.Descripcion as SexoDesc " +
                "FROM tb_urgencias u " +
                "LEFT JOIN tblt_mpo m ON u.Municipio_resid = m.MPO AND u.Entidad_resid = m.EDO " +
                "LEFT JOIN tblt_entidad e ON u.Entidad_resid = e.EDO " +
                "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                "LEFT JOIN tblt_cvesexo s ON u.Sexo = s.Cve_sexo " +
                "WHERE u.Folio = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folioPaciente);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Construir nombre completo
                String nombreCompleto = construirNombreCompleto(
                        rs.getString("A_paterno"),
                        rs.getString("A_materno"),
                        rs.getString("Nombre")
                );

                datos.put("nombre", nombreCompleto);
                datos.put("edad", String.valueOf(rs.getInt("Edad")));
                datos.put("sexo", rs.getString("SexoDesc"));
                datos.put("fechaNacimiento", rs.getString("F_nac"));
                datos.put("estadoCivil", rs.getString("Edo_civil"));
                datos.put("ocupacion", rs.getString("Ocupacion"));
                datos.put("domicilio", rs.getString("Domicilio"));
                datos.put("derechohabiencia", rs.getString("NombreDerechohabiencia"));
                datos.put("referencia", rs.getString("Referencia"));
                datos.put("expedienteClinico", rs.getString("Exp_clinico"));
            }
        } catch (SQLException e) {
            System.err.println(" Error obteniendo datos paciente: " + e.getMessage());
        }


        datos.putIfAbsent("nombre", "No especificado");
        datos.putIfAbsent("edad", "No especificado");
        datos.putIfAbsent("sexo", "No especificado");
        datos.putIfAbsent("fechaNacimiento", "No especificado");
        datos.putIfAbsent("estadoCivil", "No especificado");
        datos.putIfAbsent("ocupacion", "No especificado");
        datos.putIfAbsent("domicilio", "No especificado");
        datos.putIfAbsent("derechohabiencia", "No especificado");
        datos.putIfAbsent("referencia", "No especificado");
        datos.putIfAbsent("expedienteClinico", "No especificado");

        return datos;
    }


    @FXML
    private void otorgarPermisoInterconsulta() {
        if (tablaInterconsultas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una interconsulta primero", Alert.AlertType.WARNING);
            return;
        }

        InterconsultaVO interconsultaSeleccionada = tablaInterconsultas.getSelectionModel().getSelectedItem();

        //  VERIFICAR SI YA TIENE PERMISO
        if (interconsultaSeleccionada.isEditablePorMedico()) {
            mostrarAlerta("Permiso Ya Otorgado",
                    "Esta interconsulta YA tiene permiso de edición.\n\n" +
                            "Especialista: " + interconsultaSeleccionada.getEspecialista() +
                            "\nInterconsulta #: " + interconsultaSeleccionada.getNumeroInterconsulta() +
                            "\n\nEl especialista solo puede usar el permiso UNA vez.",
                    Alert.AlertType.WARNING);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Otorgar Permiso de Edición");
        confirmacion.setHeaderText("¿Otorgar permiso de edición al especialista?");
        confirmacion.setContentText("Especialista: " + interconsultaSeleccionada.getEspecialista() +
                "\nInterconsulta #: " + interconsultaSeleccionada.getNumeroInterconsulta() +
                "\nFolio: " + interconsultaSeleccionada.getFolioPaciente() +
                "\n\n¿El especialista podrá editar esta interconsulta? (Solo una vez)");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            if (otorgarPermisoInterconsultaEnBD(interconsultaSeleccionada.getIdInterconsulta())) {
                mostrarAlerta("Éxito", "Permiso de edición otorgado al especialista\n\n" +
                        "El especialista " + interconsultaSeleccionada.getEspecialista() +
                        " ahora puede editar esta interconsulta UNA sola vez", Alert.AlertType.INFORMATION);
                forzarActualizacionCompleta();
            } else {
                mostrarAlerta("Error", "No se pudo otorgar el permiso", Alert.AlertType.ERROR);
            }
        }
    }


    private void registrarEnHistorialPermisosInterconsulta(int idInterconsulta, String tipoNota, String accion) {
        String sql = "INSERT INTO tb_historial_permisos (id_nota, tipo_nota, folio_paciente, medico_autor, " +
                "accion, usuario_que_actua, rol_usuario, motivo, estado_paciente) " +
                "SELECT ?, ?, Folio, Medico, ?, ?, ?, 'Permiso de un solo uso', estado_paciente " +
                "FROM tb_inter WHERE id_inter = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idInterconsulta);
            pstmt.setString(2, tipoNota);
            pstmt.setString(3, accion);
            pstmt.setString(4, usuarioLogueado);
            pstmt.setString(5, rolUsuarioLogueado);
            pstmt.setInt(6, idInterconsulta);

            pstmt.executeUpdate();
            System.out.println(" Historial interconsulta registrado - " + accion + " - ID: " + idInterconsulta);

        } catch (SQLException e) {
            System.err.println(" Error registrando en historial de interconsulta: " + e.getMessage());
        }
    }

    private boolean otorgarPermisoEnBD(int idNota) {
        String sql = "UPDATE tb_notas SET " +
                "editable_por_medico = TRUE, " +
                "permiso_edicion_otorgado_por = ?, " +
                "fecha_permiso_edicion = NOW(), " +
                "fecha_edicion_realizada = NULL " +
                "WHERE id_nota = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuarioLogueado);
            pstmt.setInt(2, idNota);

            int filas = pstmt.executeUpdate();

            if (filas > 0) {
                System.out.println(" Permiso otorgado - ID Nota: " + idNota);

                //  REGISTRAR EN HISTORIAL
                registrarEnHistorialPermisos(idNota, "MEDICA", "OTORGAR");
                return true;
            } else {
                System.out.println(" No se pudo otorgar permiso");
                return false;
            }

        } catch (SQLException e) {
            System.err.println(" Error otorgando permiso: " + e.getMessage());
            return false;
        }
    }


    private void registrarEnHistorialPermisos(int idNota, String tipoNota, String accion) {
        String sql = "INSERT INTO tb_historial_permisos (id_nota, tipo_nota, folio_paciente, medico_autor, " +
                "accion, usuario_que_actua, rol_usuario, motivo, estado_paciente) " +
                "SELECT ?, ?, Folio, Medico, ?, ?, ?, 'Permiso de un solo uso', estado_paciente " +
                "FROM tb_notas WHERE id_nota = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idNota);
            pstmt.setString(2, tipoNota);
            pstmt.setString(3, accion);
            pstmt.setString(4, usuarioLogueado);
            pstmt.setString(5, rolUsuarioLogueado);
            pstmt.setInt(6, idNota);

            pstmt.executeUpdate();
            System.out.println(" Historial registrado - " + accion + " - ID Nota: " + idNota);

        } catch (SQLException e) {
            System.err.println(" Error registrando en historial: " + e.getMessage());
        }
    }

    @FXML
    private void otorgarPermisoEdicion() {
        if (tablaNotasMedicas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una nota primero", Alert.AlertType.WARNING);
            return;
        }

        NotaMedicaVO notaSeleccionada = tablaNotasMedicas.getSelectionModel().getSelectedItem();

        System.out.println(" VERIFICANDO NOTA PARA PERMISO:");
        System.out.println("   ID: " + notaSeleccionada.getIdNota());
        System.out.println("   Médico: " + notaSeleccionada.getMedicoAutor());
        System.out.println("   ¿Ya editable? " + notaSeleccionada.isEditablePorMedico());

        //  VERIFICAR SI YA TIENE PERMISO
        if (notaSeleccionada.isEditablePorMedico()) {
            mostrarAlerta("Permiso Ya Otorgado",
                    "Esta nota YA tiene permiso de edición.\n\n" +
                            "Médico: " + notaSeleccionada.getMedicoAutor() +
                            "\nNota #: " + notaSeleccionada.getNumeroNota() +
                            "\n\nEl médico solo puede usar el permiso UNA vez.",
                    Alert.AlertType.WARNING);
            return;
        }

        //  VERIFICAR QUE NO SE OTORGUE PERMISO A SÍ MISMO
        if (notaSeleccionada.getMedicoAutor().equals(sesion.getNombreMedico())) {
            mostrarAlerta("Error", "No puede otorgarse permiso a sí mismo", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Otorgar Permiso de Edición");
        confirmacion.setHeaderText("¿Otorgar permiso de edición al médico?");
        confirmacion.setContentText("Médico: " + notaSeleccionada.getMedicoAutor() +
                "\nNota #: " + notaSeleccionada.getNumeroNota() +
                "\nFolio: " + notaSeleccionada.getFolioPaciente() +
                "\n\n¿El médico podrá editar esta nota? (Solo una vez)");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            if (otorgarPermisoEnBD(notaSeleccionada.getIdNota())) {
                mostrarAlerta("Éxito", " Permiso de edición otorgado al médico\n\n" +
                                "Médico: " + notaSeleccionada.getMedicoAutor() +
                                "\nNota #: " + notaSeleccionada.getNumeroNota() +
                                "\n\nEl médico ahora puede editar esta nota UNA sola vez",
                        Alert.AlertType.INFORMATION);
                forzarActualizacionCompleta();
            } else {
                mostrarAlerta("Error", "No se pudo otorgar el permiso", Alert.AlertType.ERROR);
            }
        }
    }

    private void abrirVentanaEgreso() {
        try {
            System.out.println(" Abriendo ventana de egreso...");

            URL fxmlUrl = getClass().getResource("/views/egreso_paciente.fxml");
            if (fxmlUrl == null) {
                mostrarAlerta("Error", "No se encontró ventana de egreso", Alert.AlertType.ERROR);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            EgresoPacienteController controller = loader.getController();
            controller.setFolioPaciente(folioPaciente);
            controller.setUsuarioLogueado(usuarioLogueado, rolUsuarioLogueado);

            Stage egresoStage = new Stage();
            egresoStage.setScene(new Scene(root, 1000, 650));
            egresoStage.setTitle("Egreso Hospitalario - Folio: " + folioPaciente);

            // Cerrar ventana actual de captura
            Stage currentStage = (Stage) btnGuardarGeneral.getScene().getWindow();
            currentStage.close();

            // Mostrar ventana de egreso
            egresoStage.show();

            System.out.println(" Ventana de egreso abierta. El egreso se hará DENTRO de esa ventana.");

        } catch (Exception e) {
            System.err.println(" Error abriendo ventana de egreso: " + e.getMessage());
            mostrarAlerta("Error", "Error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Agregar en la clase
    @FXML private ToggleGroup mujerFertilGroup;

    // Método para obtener el valor seleccionado
    public String getMujerEdadFertil() {
        if (mujerFertilGroup.getSelectedToggle() != null) {
            return mujerFertilGroup.getSelectedToggle().getUserData().toString();
        }
        return null;
    }

    // Método para limpiar la selección
    public void limpiarMujerFertil() {
        mujerFertilGroup.selectToggle(null);
    }


    // cosas que se supone no se por qu estan aqui




    // MÉTODO AUXILIAR CORREGIDO: obtenerDatosPacienteParaPDF
    private Map<String, String> obtenerDatosPacienteParaPDF() {
        Map<String, String> datos = new HashMap<>();

        String sql = "SELECT " +
                "CONCAT(COALESCE(A_paterno, ''), ' ', COALESCE(A_materno, ''), ' ', COALESCE(Nombre, '')) as nombre_completo, " +
                "Edad, " +
                "s.Descripcion as sexo, " +
                "F_nac, " +
                "Edo_civil, " +
                "Ocupacion, " +
                "Domicilio, " +
                "dh.Derechohabiencia, " +
                "Referencia, " +
                "Exp_clinico, " +
                "CURP, " +
                "Telefono, " +
                "Municipio_resid, " +
                "Entidad_resid " +
                "FROM tb_urgencias u " +
                "LEFT JOIN tblt_cvesexo s ON u.Sexo = s.Cve_sexo " +
                "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                "WHERE u.Folio = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folioPaciente);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Nombre completo limpio
                String nombre = rs.getString("nombre_completo").trim();
                if (nombre.isEmpty()) nombre = "NO ESPECIFICADO";

                datos.put("nombre", nombre);
                datos.put("edad", rs.getString("Edad") != null ? rs.getString("Edad") : "No especificado");
                datos.put("sexo", rs.getString("sexo") != null ? rs.getString("sexo") : "No especificado");
                datos.put("fechaNacimiento", rs.getString("F_nac") != null ? rs.getString("F_nac") : "No especificado");
                datos.put("estadoCivil", rs.getString("Edo_civil") != null ? rs.getString("Edo_civil") : "No especificado");
                datos.put("ocupacion", rs.getString("Ocupacion") != null ? rs.getString("Ocupacion") : "No especificado");
                datos.put("domicilio", rs.getString("Domicilio") != null ? rs.getString("Domicilio") : "No especificado");
                datos.put("derechohabiencia", rs.getString("Derechohabiencia") != null ? rs.getString("Derechohabiencia") : "No especificado");
                datos.put("referencia", rs.getString("Referencia") != null ? rs.getString("Referencia") : "No especificado");
                datos.put("expedienteClinico", rs.getString("Exp_clinico") != null ? rs.getString("Exp_clinico") : "No especificado");
                datos.put("curp", rs.getString("CURP") != null ? rs.getString("CURP") : "No especificado");
                datos.put("telefono", rs.getString("Telefono") != null ? rs.getString("Telefono") : "No especificado");

                System.out.println("✅ Datos paciente obtenidos correctamente:");
                System.out.println("   Nombre: " + nombre);
                System.out.println("   CURP: " + datos.get("curp"));
                System.out.println("   Teléfono: " + datos.get("telefono"));
            } else {
                System.err.println("⚠️ No se encontró paciente con folio: " + folioPaciente);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error en obtenerDatosPacienteParaPDF: " + e.getMessage());
            e.printStackTrace();
        }

        return datos;
    }




    // MÉTODO AUXILIAR CORREGIDO: obtenerDatosNotaParaPDF
    private Map<String, String> obtenerDatosNotaParaPDF(int idNota) {
        Map<String, String> datos = new HashMap<>();

        String sql = "SELECT sintomas, signos_vitales, diagnostico, Nota, Indicaciones, " +
                "Cedula, Fecha, Hora, mujer_edad_fertil " +
                "FROM tb_notas WHERE id_nota = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idNota);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                datos.put("sintomas", rs.getString("sintomas"));
                datos.put("signosVitales", rs.getString("signos_vitales"));
                datos.put("diagnostico", rs.getString("diagnostico"));
                datos.put("nota", rs.getString("Nota"));
                datos.put("indicaciones", rs.getString("Indicaciones"));
                datos.put("cedula", rs.getString("Cedula"));
                datos.put("fecha", rs.getString("Fecha"));
                datos.put("hora", rs.getString("Hora"));
                datos.put("mujerEdadFertil", rs.getString("mujer_edad_fertil"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo datos de nota: " + e.getMessage());
        }

        // Valores por defecto
        datos.putIfAbsent("sintomas", "");
        datos.putIfAbsent("signosVitales", "");
        datos.putIfAbsent("diagnostico", "");
        datos.putIfAbsent("nota", "");
        datos.putIfAbsent("indicaciones", "");
        datos.putIfAbsent("cedula", "");
        datos.putIfAbsent("fecha", LocalDate.now().toString());
        datos.putIfAbsent("hora", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        datos.putIfAbsent("mujerEdadFertil", "NO");

        return datos;
    }
    /**
     * OBTENER DATOS COMPLETOS DE INTERCONSULTA PARA PDF
     */
    private Map<String, String> obtenerDatosInterconsultaParaPDF(int idInterconsulta) {
        Map<String, String> datos = new HashMap<>();

        String sql = "SELECT sintomas, signos_vitales, diagnostico, Nota, especialidad, " +
                "Cedula, Fecha, Hora, mujer_edad_fertil " +
                "FROM tb_inter WHERE id_inter = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idInterconsulta);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // IMPORTANTE: "Nota" son las indicaciones en interconsulta
                datos.put("sintomas", rs.getString("sintomas"));
                datos.put("signosVitales", rs.getString("signos_vitales"));
                datos.put("diagnostico", rs.getString("diagnostico"));
                datos.put("indicaciones", rs.getString("Nota")); // ← CORRECTO: Nota = indicaciones
                datos.put("especialidad", rs.getString("especialidad")); // ← Especialidad de tb_inter
                datos.put("cedula", rs.getString("Cedula"));
                datos.put("fecha", rs.getString("Fecha"));
                datos.put("hora", rs.getString("Hora"));
                datos.put("mujerEdadFertil", rs.getString("mujer_edad_fertil"));

                System.out.println("📋 Datos interconsulta obtenidos desde BD:");
                System.out.println("   Sintomas: " + (datos.get("sintomas") != null ? "Presente" : "Vacío"));
                System.out.println("   Signos vitales: " + (datos.get("signosVitales") != null ? "Presente" : "Vacío"));
                System.out.println("   Diagnóstico: " + (datos.get("diagnostico") != null ? "Presente" : "Vacío"));
                System.out.println("   Indicaciones (Nota): " + (datos.get("indicaciones") != null && !datos.get("indicaciones").isEmpty() ? "Presente" : "Vacío"));
                System.out.println("   Especialidad: " + datos.get("especialidad"));
            } else {
                System.err.println("❌ No se encontró interconsulta con ID: " + idInterconsulta);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error SQL obteniendo datos de interconsulta: " + e.getMessage());
            e.printStackTrace();
        }

        // Valores por defecto
        datos.putIfAbsent("sintomas", "");
        datos.putIfAbsent("signosVitales", "");
        datos.putIfAbsent("diagnostico", "");
        datos.putIfAbsent("indicaciones", ""); // Esto es lo que aparece como INDICACIONES en el PDF
        datos.putIfAbsent("especialidad", "");
        datos.putIfAbsent("cedula", "");
        datos.putIfAbsent("fecha", LocalDate.now().toString());
        datos.putIfAbsent("hora", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        datos.putIfAbsent("mujerEdadFertil", "NO");

        return datos;
    }

    // borarrrrrrrrrrr

    /**
     * MÉTODO DE DEPURACIÓN: Mostrar qué especialistas hay en la BD
     */
    private void mostrarEspecialistasEnBD(Connection conn) {
        try {
            System.out.println("📋 ESPECIALISTAS DISPONIBLES EN tb_medesp:");

            String sql = "SELECT Nombre, especialidad, universidad, Cedula FROM tb_medesp ORDER BY Nombre";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                int count = 0;
                while (rs.next()) {
                    System.out.println("   " + (++count) + ". " +
                            "Nombre: '" + rs.getString("Nombre") + "' - " +
                            "Especialidad: '" + rs.getString("especialidad") + "' - " +
                            "Universidad: '" + rs.getString("universidad") + "' - " +
                            "Cédula: '" + rs.getString("Cedula") + "'");
                }
                if (count == 0) {
                    System.out.println("   (Tabla tb_medesp está vacía)");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error mostrando especialistas: " + e.getMessage());
        }
    }


    // cosas de prueba borrar todo lo que est despues de estp



    private void ocultarSeccionParaEgresado() {
        Platform.runLater(() -> {
            // Ocultar todo excepto RadioButton para Egreso
            txtFechaAtencion.setVisible(false);
            txtHoraAtencion.setVisible(false);
            txtCedulaMedico.setVisible(false);
            cmbTipoUrgencia.setVisible(false);
            cmbMotivoUrgencia.setVisible(false);
            cmbTipoCama.setVisible(false);
            cmbMedicoActual.setVisible(false);
            rbObservacion.setVisible(false);
            btnGuardarGeneral.setVisible(false);

            // Solo mostrar RadioButton de Egreso (pero deshabilitado)
            rbAltaMedica.setVisible(true);
            rbAltaMedica.setDisable(true);
            rbAltaMedica.setSelected(false);
        });
    }

    private void mostrarSoloRadioButtonsParaEgreso() {
        Platform.runLater(() -> {
            // Ocultar todos los combos y campos
            txtFechaAtencion.setVisible(false);
            txtHoraAtencion.setVisible(false);
            txtCedulaMedico.setVisible(false);
            cmbTipoUrgencia.setVisible(false);
            cmbMotivoUrgencia.setVisible(false);
            cmbTipoCama.setVisible(false);
            cmbMedicoActual.setVisible(false);

            // Mostrar ambos RadioButtons
            rbObservacion.setVisible(true);
            rbObservacion.setDisable(true); // No puede cambiar de Observación
            rbObservacion.setSelected(true); // Ya está en observación

            rbAltaMedica.setVisible(true);
            rbAltaMedica.setDisable(false); // Puede seleccionar para egresar

            btnGuardarGeneral.setVisible(true);
        });
    }




}


