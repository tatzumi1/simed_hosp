// src/main/java/com/PruebaSimed2/controllers/CapturaPrincipalController.java

package com.PruebaSimed2.controllers;

import com.PruebaSimed2.DTO.HistorialPermisos.InsertarPermisoDTO;
import com.PruebaSimed2.DTO.Urgencias.ActualizarCapturaPrincipalDTO;
import com.PruebaSimed2.database.*;
import com.PruebaSimed2.models.InterconsultaVO;
import com.PruebaSimed2.models.NotaMedicaVO;
import com.PruebaSimed2.utils.PDFGenerator;
import com.PruebaSimed2.utils.SesionUsuario;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.PruebaSimed2.utils.NameGenerator.generateName;
import static java.util.Arrays.asList;

@Log4j2
public class CapturaPrincipalController {

    // ==================== CAMPOS IZQUIERDA - DATOS EXISTENTES ====================
    @FXML
    private TextField txtFolio, txtFechaRegistro, txtHoraRegistro, txtTriage;
    @FXML
    private TextField txtNombre, txtEdad, txtSexo, txtMunicipio, txtEntidad;
    @FXML
    private TextField txtDerechohabiencia, txtReferencia, txtMedicoIngreso;
    @FXML
    private TextArea txtDomicilio, txtSintomas;
    @FXML
    private Button btnColorTriage;

    // ==================== CAMPOS DERECHA - NUEVA INFORMACIÓN ====================
    @FXML
    private TextField txtFechaAtencion, txtHoraAtencion, txtCedulaMedico;
    @FXML
    private ComboBox<String> cmbTipoUrgencia, cmbMotivoUrgencia, cmbTipoCama, cmbMedicoActual;

    @FXML
    private RadioButton rbObservacion, rbAltaMedica;

    // ==================== NOTAS MÉDICAS ====================
    @FXML
    private Label lblContadorNotas, lblContadorInterconsultas;
    @FXML
    private TableView<NotaMedicaVO> tablaNotasMedicas;
    @FXML
    private TableView<InterconsultaVO> tablaInterconsultas;
    @FXML
    private Button btnVisualizarNotaMedica, btnEditarNotaMedica, btnImprimirNotaMedica;
    @FXML
    private Button btnVisualizarInterconsulta, btnEditarInterconsulta, btnImprimirInterconsulta;
    @FXML
    private Button btnNuevaNotaMedica, btnNuevaInterconsulta;
    @FXML
    private Button btnOtorgarPermiso, btnOtorgarPermisoInterconsulta;
    @FXML
    private Button btnGuardarGeneral;

    @FXML
    private void imprimirNotaMedica() { /* implementación similar */ }

    @FXML
    private void imprimirInterconsulta() { /* implementación similar */ }


    private int folioPaciente;
    private String usuarioLogueado;
    private String rolUsuarioLogueado;
    private final Map<String, String> coloresTriage = new HashMap<>();
    private boolean capturaGuardada = false;

    // Variables para datos
    private final ObservableList<NotaMedicaVO> notasData = FXCollections.observableArrayList();
    private final ObservableList<InterconsultaVO> interconsultasData = FXCollections.observableArrayList();
    private final SesionUsuario sesion = SesionUsuario.getInstance();

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
        validarCamposRequeridosYActivarUI();
    }

    public void setUsuarioLogueado(String usuario, String rol) {
        this.usuarioLogueado = usuario;
        this.rolUsuarioLogueado = rol;

        if (sesion.getUsername() == null) {
            int usuarioId = obtenerIdUsuarioDesdeBD(usuario);
            sesion.inicializar(usuario, rol, usuarioId);
        }

        log.debug("Usuario en captura: {} - Rol: {}", usuario, rol);
        log.debug("Nombre médico en sesión: {}", sesion.getNombreMedico());

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
        // ==================== ESTADO DEL PACIENTE ====================
        ToggleGroup tgEstadoPaciente = new ToggleGroup();
        rbObservacion.setToggleGroup(tgEstadoPaciente);
        rbAltaMedica.setToggleGroup(tgEstadoPaciente);
    }

    private void configurarEventos() {
        // Listeners para habilitar UI
        cmbTipoUrgencia.valueProperty().addListener((obs, oldVal, newVal) -> validarCamposRequeridosYActivarUI());
        cmbMotivoUrgencia.valueProperty().addListener((obs, oldVal, newVal) -> validarCamposRequeridosYActivarUI());
        cmbTipoCama.valueProperty().addListener((obs, oldVal, newVal) -> validarCamposRequeridosYActivarUI());
        cmbMedicoActual.valueProperty().addListener((obs, oldVal, newVal) -> {
            actualizarCedulaMedico();
            validarCamposRequeridosYActivarUI();
        });
        rbObservacion.selectedProperty().addListener((obs, oldVal, newVal) -> validarCamposRequeridosYActivarUI());
        rbAltaMedica.selectedProperty().addListener((obs, oldVal, newVal) -> validarCamposRequeridosYActivarUI());

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
                    (observable, oldValue, newValue) -> configurarVisibilidadBotonesSegunNotaSeleccionada()
            );
        }

        // Configurar tabla de interconsultas
        if (tablaInterconsultas != null) {
            tablaInterconsultas.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> configurarVisibilidadBotonesInterconsulta()
            );
        }
    }

    private void cargarDatosPaciente(int folio) {
        try (Connection conn = ConexionBD.conectar()) {
            var ud = new UrgenciasData();
            var dto = ud.cardarDatosPaciente(folio, conn);

            txtFolio.setText(String.valueOf(dto.getFolio()));
            txtFechaRegistro.setText(dto.getFecha().toString());
            txtHoraRegistro.setText(dto.getHoraRegistro().toString());

            String triage = dto.getTriage();
            txtTriage.setText(triage != null ? triage : "No especificado");
            aplicarColorTriage(triage);

            String nombreCompleto = generateName(dto.getApellidoPaterno(), dto.getApellidoMaterno(), dto.getNombre());
            txtNombre.setText(nombreCompleto);
            txtEdad.setText(String.valueOf(dto.getEdad()));
            txtSexo.setText(obtenerDescripcionSexo(String.valueOf(dto.getSexo())));
            txtDomicilio.setText(dto.getDomicilio());
            String municipioCode = dto.getMunicipioResid();
            String entidadCode = dto.getEntidadResid();
            String municipio = dto.getNombreMunicipio();
            String entidad = dto.getNombreEntidad();
            if (municipio == null && municipioCode != null) {
                municipio = obtenerNombreMunicipio(municipioCode, entidadCode);
            }

            if (entidad == null && entidadCode != null) {
                entidad = obtenerNombreEntidad(entidadCode);
            }

            txtMunicipio.setText(municipio != null ? municipio : "No especificado");
            txtEntidad.setText(entidad != null ? entidad : "No especificado");
            String derechohabiencia = dto.getNombreDerechoHabiencia();
            txtDerechohabiencia.setText(derechohabiencia != null ? derechohabiencia : "No especificado");
            txtReferencia.setText(dto.getReferencia());
            txtSintomas.setText(dto.getSintomas());
            txtMedicoIngreso.setText(dto.getNombreMedico());
            log.info(" Datos paciente cargados - Folio: {}", folio);
        } catch (SQLException e) {
            log.error(" Error cargando paciente: {}", e.getMessage());
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

        log.debug(" Procesando municipio: '{}'", municipioCode);

        // CASO A: Si ya es texto (ej.: "poza rica"), usarlo directamente
        if (!municipioCode.matches("\\d+")) {
            log.debug("Es texto, usando directamente: {}", municipioCode);
            return municipioCode;
        }

        // CASO B: Si es número, buscar en tblt_mpo
        log.debug("Es número, buscando en tblt_mpo...");

        try (Connection conn = ConexionBD.conectar()) {
            var md = new MunicipioData();
            String nombre = md.obtenerDescripcionMunicipio(conn, municipioCode, entidadCode);

            if (nombre != null) {
                log.debug(" Municipio encontrado: {}", nombre);
                return nombre;
            } else {
                log.debug("Municipio no encontrado en tblt_mpo, intentando sin ceros a la izquierda...");
                if (municipioCode.matches("0+\\d+")) {
                    String sinCeros = municipioCode.replaceFirst("^0+", "");
                    log.debug("Intentando sin ceros: '{}'", sinCeros);
                    nombre = md.obtenerDescripcionMunicipio(conn, sinCeros, entidadCode);
                    if (nombre != null) {
                        log.debug("Municipio encontrado (sin ceros): {}", nombre);
                        return nombre;
                    }
                }
                log.warn("Municipio no encontrado en tblt_mpo ni sin ceros a la izquierda");
                return null;
            }
        } catch (SQLException e) {
            log.error("Error obteniendo municipio: {}", e.getMessage());
            return null;
        }
    }

    private String obtenerNombreEntidad(String entidadCode) {
        if (entidadCode == null || entidadCode.trim().isEmpty()) {
            return null;
        }

        entidadCode = entidadCode.trim();
        log.debug("Procesando entidad: '{}'", entidadCode);

        // CASO A: Si ya es texto, usarlo directamente
        if (!entidadCode.matches("\\d+")) {
            log.debug("   Es texto, usando directamente: {}", entidadCode);
            return entidadCode;
        }

        // CASO B: Si es número, buscar en tblt_entidad
        log.debug("   Es número, buscando en tblt_entidad...");

        try (Connection conn = ConexionBD.conectar()) {
            var ed = new EntidadData();
            return ed.obtenerEntidades(conn, entidadCode);
        } catch (SQLException e) {
            log.error("Error obteniendo entidad: {}", e.getMessage());
            return null;
        }
    }

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
            String nombreCompleto = generateName((String) datos.get("a_paterno"), (String) datos.get("a_materno"), (String) datos.get("nombre"));
            txtNombre.setText(nombreCompleto);

            // Demás datos
            txtEdad.setText(String.valueOf(datos.get("edad")));
            txtSexo.setText(obtenerDescripcionSexo((String) datos.get("sexo")));
            txtDomicilio.setText((String) datos.get("domicilio"));

            // MUNICIPIO Y ENTIDAD (corregido)
            String municipio = (String) datos.get("municipio");
            String entidad = (String) datos.get("entidad");

            log.debug(" Mostrando en UI:");
            log.debug("   Municipio: {}", municipio);
            log.debug("   Entidad: {}", entidad);

            txtMunicipio.setText(municipio != null ? municipio : "No especificado");
            txtEntidad.setText(entidad != null ? entidad : "No especificado");

            txtDerechohabiencia.setText((String) datos.get("derechohabiencia"));
            txtReferencia.setText((String) datos.get("referencia"));
            txtSintomas.setText((String) datos.get("sintomas"));
            txtMedicoIngreso.setText((String) datos.get("nom_med"));

            log.info(" Datos paciente cargados en UI - Folio: {}", datos.get("folio"));

        } catch (Exception e) {
            log.error(" Error actualizando UI de paciente: {}", e.getMessage());
            mostrarAlerta("Error de datos",
                    "Error al mostrar datos del paciente: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private String obtenerDescripcionSexo(String codigoSexo) {
        if ("1".equals(codigoSexo)) return "Masculino";
        if ("2".equals(codigoSexo)) return "Femenino";
        return "No especificado";
    }

    private void aplicarColorTriage(String triage) {
        if (triage != null) {
            String color;
            String texto = triage.toLowerCase();

            if (texto.contains("rojo")) color = "#e74c3c";
            else if (texto.contains("amarillo")) color = "#f1c40f";
            else if (texto.contains("naranja")) color = "#ff8000";
            else if (texto.contains("verde")) color = "#2ecc71";
            else color = "#95a5a6";

            btnColorTriage.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10;");
        }
    }

    private int obtenerIdUsuarioDesdeBD(String username) {
        try (Connection conn = ConexionBD.conectar()) {
            var ud = new UsuarioData();
            return ud.obtenerIdUsuarioPorUsername(conn, username);
        } catch (SQLException e) {
            log.error(" Error obteniendo ID de usuario: {}", e.getMessage());
        }
        return 0;
    }

    private String obtenerNombreMedicoDesdeUsuario(String username) {
        try (Connection conn = ConexionBD.conectar()) {
            var ud = new UsuarioData();
            return ud.obtenerNombreMedicoPorUsername(conn, username);
        } catch (SQLException e) {
            log.error(" Error obteniendo nombre médico: {}", e.getMessage());
            return null;
        }
    }

    @FXML
    private void abrirNotaMedica() {
        if (pacienteEgresado()) {
            mostrarAlerta("Error", "No se pueden crear notas para pacientes egresados", Alert.AlertType.WARNING);
            log.warn("Error: No se pueden crear notas para pacientes egresados");
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
                    log.debug("Nota temporal existente editada: {}", idNotaExistente);
                } else {
                    mostrarAlerta("Nota Temporal Existente", "Ya tiene una nota temporal en este paciente.\n\nPara crear una nueva nota, debe primero:\n1. Editar su nota temporal existente\n2. Guardarla como DEFINITIVA\n3. Luego podrá crear una nueva nota", Alert.AlertType.INFORMATION);
                    log.debug("Nota temporal existente cancelada: {}", idNotaExistente);
                }
                return;
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
            stage.setOnHidden(e -> Platform.runLater(() -> {
                cargarNotasDelPaciente();
                cargarContadores();
            }));
            stage.show();
        } catch (Exception e) {
            log.error("Error abriendo nota médica: {}", e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir la ventana de nota médica", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void abrirInterconsulta() {
        if (pacienteEgresado()) {
            mostrarAlerta("Error", "No se pueden crear interconsultas para pacientes egresados", Alert.AlertType.WARNING);
            log.warn("Error: No se pueden crear interconsultas para pacientes egresados");
            return;
        }

        if (!sesion.puedeCrearNuevaInterconsulta(folioPaciente)) {
            mostrarAlerta("Interconsulta Temporal Existente", "Ya tiene una interconsulta temporal en este paciente.\n\nDebe editar o finalizar la interconsulta existente antes de crear una nueva.", Alert.AlertType.WARNING);
            log.warn("Interconsulta Temporal Existente: No se puede crear interconsulta para paciente: {}", folioPaciente);
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
            stage.setOnHidden(e -> Platform.runLater(() -> {
                cargarInterconsultasDelPaciente();
                cargarContadores();
            }));
            stage.show();
        } catch (Exception e) {
            log.error("Error abriendo interconsulta: {}", e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir la ventana de interconsulta", Alert.AlertType.ERROR);
        }
    }

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
            log.debug("Combos cargados correctamente");
        } catch (SQLException e) {
            log.error("Error cargando combos: {}", e.getMessage());
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
                var nd = new NotasData();
                var id = new InterData();
                
                int conteoNotas = nd.obtenerConteoNotas(conn, folioPaciente);
                int conteoInterconsultas = id.obtenerConteoInterconsultas(conn, folioPaciente);
                
                Platform.runLater(() -> {
                    lblContadorNotas.setText(String.valueOf(conteoNotas));
                    lblContadorInterconsultas.setText(String.valueOf(conteoInterconsultas));
                });
            } catch (SQLException e) {
                log.error(" Error cargando contadores: {}", e.getMessage());
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
            try (Connection conn = ConexionBD.conectar()) {
                var md = new MedicoData();
                txtCedulaMedico.setText(md.obtenerCedulaPorNombre(conn, medicoSeleccionado));
            } catch (SQLException e) {
                log.error(" Error obteniendo cédula: {}", e.getMessage());
            }
        } else {
            log.warn("Medico no seleccionado");
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
            colSintomas.setCellFactory(tc -> crearCeldaGenericaConTooltip());
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
            TableColumn<NotaMedicaVO, String> colFecha = getNotaMedicaVOStringTableColumn();

            // COLUMNA ESTADO
            TableColumn<NotaMedicaVO, String> colEstado = new TableColumn<>("Estado");
            colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
            colEstado.setPrefWidth(90);

            // COLUMNA ¿EDITABLE?
            TableColumn<NotaMedicaVO, String> colEditable = new TableColumn<>("¿Editable?");
            colEditable.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().isEditablePorMedico() ? "SÍ" : "NO"));
            colEditable.setPrefWidth(70);

            tablaNotasMedicas.getColumns().addAll(asList(colNumeroNota, colSintomas, colMedico, colFecha, colEstado, colEditable));
        }
    }

    private static TableColumn<NotaMedicaVO, String> getNotaMedicaVOStringTableColumn() {
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
        return colFecha;
    }

    private <T> TableCell<T, String> crearCeldaGenericaConTooltip() {
        return new TableCell<>() {
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
            TableColumn<InterconsultaVO, String> colSintomasInter = getInterconsultaVOStringTableColumn();

            // COLUMNA ESPECIALISTA
            TableColumn<InterconsultaVO, String> colEspecialista = new TableColumn<>("Especialista");
            colEspecialista.setCellValueFactory(new PropertyValueFactory<>("especialista"));
            colEspecialista.setPrefWidth(150);

            // COLUMNA FECHA/HORA
            TableColumn<InterconsultaVO, String> colFechaInter = getVoStringTableColumn();

            // COLUMNA ESTADO
            TableColumn<InterconsultaVO, String> colEstadoInter = new TableColumn<>("Estado");
            colEstadoInter.setCellValueFactory(new PropertyValueFactory<>("estado"));
            colEstadoInter.setPrefWidth(90);

            tablaInterconsultas.getColumns().addAll(asList(colNumeroInter, colSintomasInter, colEspecialista, colFechaInter, colEstadoInter));
        }
    }

    private static TableColumn<InterconsultaVO, String> getVoStringTableColumn() {
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
        return colFechaInter;
    }

    private TableColumn<InterconsultaVO, String> getInterconsultaVOStringTableColumn() {
        TableColumn<InterconsultaVO, String> colSintomasInter = new TableColumn<>("Síntomas");
        colSintomasInter.setCellFactory(tc -> crearCeldaGenericaConTooltip());
        colSintomasInter.setCellValueFactory(cellData -> {
            String sintomas = obtenerSintomasInterconsulta(cellData.getValue().getIdInterconsulta());
            return new SimpleStringProperty(sintomas);
        });
        colSintomasInter.setPrefWidth(220);
        return colSintomasInter;
    }

    // ==================== MÉTODOS AUXILIARES PARA TABLAS ====================

    private String obtenerSintomasNota(int idNota) {
        try (Connection conn = ConexionBD.conectar()) {
            var nd = new NotasData();
            return nd.obtenerSintomasPorNota(conn, idNota);
        } catch (SQLException e) {
            log.error(" Error obteniendo síntomas: {}", e.getMessage());
        }
        return "Error al cargar";
    }

    private String obtenerSintomasInterconsulta(int idInterconsulta) {
        try (Connection conn = ConexionBD.conectar()) {
            var id = new InterData();
            return id.obtenerSintomasPorInterconsulta(conn, idInterconsulta);
        } catch (SQLException e) {
            log.error(" Error obteniendo síntomas de interconsulta: {}", e.getMessage());
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

    // ==================== SISTEMA DE PERMISOS Y EDICIÓN CORREGIDO ====================
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
        sesion.puedeEditarNota(notaSeleccionada.getMedicoAutor());
        boolean esDuenoNota = notaSeleccionada.getMedicoAutor().equals(sesion.getNombreMedico());
        boolean notaEsTemporal = "TEMPORAL".equals(notaSeleccionada.getEstado());
        boolean tienePermiso = notaSeleccionada.isEditablePorMedico();

        // Mostrar botón de editar solo si:
        // 1. Es dueño de la nota Y es temporal
        // 2. O tiene permiso de edición otorgado
        boolean mostrarEditar = (esDuenoNota && notaEsTemporal) || tienePermiso;

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
        boolean mostrarEditar = isMostrarEditar(interconsultaSeleccionada);

        btnEditarInterconsulta.setVisible(mostrarEditar);
        btnEditarInterconsulta.setDisable(!mostrarEditar);
        btnOtorgarPermisoInterconsulta.setVisible(false);
    }

    private boolean isMostrarEditar(InterconsultaVO interconsultaSeleccionada) {
        boolean esDuenoInterconsulta = interconsultaSeleccionada.getEspecialista().equals(sesion.getNombreMedico());
        boolean interconsultaEsTemporal = "TEMPORAL".equals(interconsultaSeleccionada.getEstado());
        boolean tienePermiso = interconsultaSeleccionada.isEditablePorMedico();

        // Mostrar botón de editar solo si:
        // 1. Es dueño de la interconsulta Y es temporal
        // 2. O tiene permiso de edición otorgado
        return (esDuenoInterconsulta && interconsultaEsTemporal) || tienePermiso;
    }

    private boolean otorgarPermisoInterconsultaEnBD(int idInterconsulta) {
        try (Connection conn = ConexionBD.conectar()) {
            var id = new InterData();
            if (id.tienePermisoInterconsulta(conn, usuarioLogueado, idInterconsulta)) {
                registrarEnHistorialPermisosInterconsulta(idInterconsulta);
                return true;
            } else {
                log.warn(" No se pudo otorgar permiso para interconsulta");
                return false;
            }
        } catch (SQLException e) {
            log.error(" Error otorgando permiso para interconsulta: {}", e.getMessage());
            return false;
        }
    }

    @FXML
    private void editarNotaMedica() {
        if (tablaNotasMedicas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una nota primero", Alert.AlertType.WARNING);
            log.warn("No se a seleccionado ninguna nota para editar");
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
            log.warn("No tiene permisos para editar nota - ID: {}", notaSeleccionada.getIdNota());
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
                log.debug("Actualizando lista después de edición...");
                forzarActualizacionCompleta();
            });

            stage.show();

        } catch (Exception e) {
            log.error("Error abriendo editor de nota: {}", e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir el editor", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void editarInterconsulta() {
        if (tablaInterconsultas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una interconsulta primero", Alert.AlertType.WARNING);
            log.warn("No se a seleccionado ninguna interconsulta para editar");
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
            log.warn("No tiene permisos para editar interconsulta - ID: {}", interconsultaSeleccionada.getIdInterconsulta());
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
            log.error("Error abriendo editor de interconsulta: {}", e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir el editor de interconsulta", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void guardarGeneral() {
        if (!validarCampos()) return;

        if (guardarCapturaCompleta()) {
            String estado = obtenerEstadoSeleccionado();

            if ("OBSERVACION".equals(estado)) {
                actualizarEstadoPaciente();
                log.debug("Actualizando estado de paciente a OBSERVADO");
                mostrarAlerta("Éxito",
                        "Paciente movido a OBSERVACIÓN",
                        Alert.AlertType.INFORMATION);
                deshabilitarCamposNuevaInformacion();
                configurarBotonesSegunEstadoPaciente();
            } else if ("EGRESADO".equals(estado)) {
                abrirVentanaEgreso();
            }
        }
    }

    private boolean guardarCapturaCompleta() {
        Connection conn = null;
        try {
            conn = ConexionBD.conectar();
            conn.setAutoCommit(false);

            var ud = new UrgenciasData();
            var dto = new ActualizarCapturaPrincipalDTO(
                    obtenerClaveDesdeDescripcion("tblt_cveurg", cmbTipoUrgencia.getValue()),
                    obtenerClaveDesdeDescripcion("tblt_cvemotatn", cmbMotivoUrgencia.getValue()),
                    obtenerClaveDesdeDescripcion("tblt_cvecama", cmbTipoCama.getValue()),
                    obtenerClaveMedico(cmbMedicoActual.getValue()),
                    cmbMedicoActual.getValue(),
                    folioPaciente
            );

            if (ud.actualizarCapturaPrincipal(conn, dto)) {
                conn.commit();
                deshabilitarCamposNuevaInformacion();
                capturaGuardada = true;
                configurarBotonesSegunEstadoPaciente();
                log.debug("Captura completa guardada - Folio: {}", folioPaciente);
                return true;
            }
        } catch (SQLException e) {
            log.error("Error guardando captura: {}", e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                log.error("Error en rollback: {}", ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                log.error("Error cerrando conexión: {}", e.getMessage());
            }
        }
        return false;
    }

    private void verificarNotasTemporalesPropias() {
        try (Connection conn = ConexionBD.conectar()) {
            String nombreMedicoActual = sesion.getNombreMedico();
            var nd = new NotasData();
            var id = new InterData();
            if (nd.obtenerConteoNotasPorMedico(conn, folioPaciente, nombreMedicoActual) > 0) {
                cargarDatosNuevaInfoDesdeBD();
                deshabilitarCamposNuevaInformacion();
                habilitarBotonesNotas();
                capturaGuardada = true;
                log.debug("Notas temporales PROPIAS detectadas - Usuario: {}", nombreMedicoActual);
            }

            // VERIFICAR INTERCONSULTAS TEMPORALES PROPIAS
            if (id.obtenerConteoInterconsultasPorMedico(conn, folioPaciente, nombreMedicoActual) > 0) {
                if (!capturaGuardada) {
                    cargarDatosNuevaInfoDesdeBD();
                    deshabilitarCamposNuevaInformacion();
                    habilitarBotonesNotas();
                    capturaGuardada = true;
                }
                log.debug(" Interconsultas temporales PROPIAS detectadas - Usuario: {}", nombreMedicoActual);
            }
        } catch (SQLException e) {
            log.error(" Error verificando notas temporales propias: {}", e.getMessage());
        }
    }

    // ==================== MÉTODOS DE CARGA DESDE BD ====================
    private void cargarNotasDelPaciente() {
        try (Connection conn = ConexionBD.conectar()) {
            var nd = new NotasData();
            notasData.clear();
            notasData.addAll(nd.obtenerNotasPaciente(conn, folioPaciente));
            Platform.runLater(() -> {
                tablaNotasMedicas.setItems(notasData);
                tablaNotasMedicas.refresh();
                log.debug("si {} notas cargadas para folio: {}", notasData.size(), folioPaciente);
            });
        } catch (SQLException e) {
            log.error(" Error cargando notas: {}", e.getMessage());
        }
    }

    private void cargarInterconsultasDelPaciente() {
        try (Connection conn = ConexionBD.conectar()) {
            var id = new InterData();
            interconsultasData.clear();
            interconsultasData.addAll(id.obtenerInterconsultasPorPaciente(conn, folioPaciente));
            Platform.runLater(() -> {
                if (tablaInterconsultas != null) {
                    tablaInterconsultas.setItems(interconsultasData);
                    tablaInterconsultas.refresh();
                }
                log.debug("si {} interconsultas cargadas para folio: {}", interconsultasData.size(), folioPaciente);
            });

        } catch (SQLException e) {
            log.error(" Error cargando interconsultas: {}", e.getMessage());
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
            log.error(" Error cargando nota existente: {}", e.getMessage());
        }
    }

    private void cargarDatosNuevaInfoDesdeBD() {
        try (Connection conn = ConexionBD.conectar()) {
            var ud = new UrgenciasData();
            var dto = ud.cargarNuevaInformacion(folioPaciente, conn);
            if (dto != null) {
                if (dto.getTipoUrg() > 0) {
                    String tipoUrgencia = obtenerDescripcionDesdeClave("tblt_cveurg", "Cve_urg", dto.getTipoUrg());
                    cmbTipoUrgencia.setValue(tipoUrgencia);
                }
                if (dto.getMotivoUrg() > 0) {
                    String motivoUrgencia = obtenerDescripcionDesdeClave("tblt_cvemotatn", "Cve_motatn", dto.getMotivoUrg());
                    cmbMotivoUrgencia.setValue(motivoUrgencia);
                }
                if (dto.getTipoCama() > 0) {
                    String tipoCama = obtenerDescripcionDesdeClave("tblt_cvecama", "Cve_cama", dto.getTipoCama());
                    cmbTipoCama.setValue(tipoCama);
                }
                if (dto.getNombreMedico() != null) {
                    cmbMedicoActual.setValue(dto.getNombreMedico());
                    actualizarCedulaMedico();
                }
                if (dto.getEstadoPaciente() == 2) { // Observación
                    rbObservacion.setSelected(true);
                } else if (dto.getEstadoPaciente() == 3) { // Egresado
                    rbAltaMedica.setSelected(true);
                }
                validarCamposRequeridosYActivarUI();
            }
        } catch (SQLException e) {
            log.error("Error cargando datos de Nueva Info: {}", e.getMessage());
        }
    }

    private String obtenerDescripcionDesdeClave(String tabla, String columnaClave, int clave) {
        if (clave <= 0) return null;

        String sql = "SELECT " + "Descripcion" + " FROM " + tabla + " WHERE " + columnaClave + " = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clave);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("Descripcion");
            }
        } catch (SQLException e) {
            log.error(" Error obteniendo descripción para clave {}: {}", clave, e.getMessage());
        }
        return null;
    }

    private void deshabilitarCamposNuevaInformacion() {
        if (cmbTipoUrgencia.getValue() != null &&
            cmbMotivoUrgencia.getValue() != null &&
            cmbTipoCama.getValue() != null &&
            cmbMedicoActual.getValue() != null) {
            
            cmbTipoUrgencia.setDisable(true);
            cmbMotivoUrgencia.setDisable(true);
            cmbTipoCama.setDisable(true);
            cmbMedicoActual.setDisable(true);
            rbObservacion.setDisable(true);

            String estiloGris = "-fx-opacity: 0.7; -fx-background-color: #f5f5f5; -fx-text-fill: #666666;";
            cmbTipoUrgencia.setStyle(estiloGris);
            cmbMotivoUrgencia.setStyle(estiloGris);
            cmbTipoCama.setStyle(estiloGris);
            cmbMedicoActual.setStyle(estiloGris);
        }
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

    private int obtenerClaveDesdeDescripcion(String tabla, String descripcion) {
        if (descripcion == null) return -1;

        String sql = "SELECT * FROM " + tabla + " WHERE " + "Descripcion" + " = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, descripcion);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return switch (tabla) {
                    case "tblt_cveurg" -> rs.getInt("Cve_urg");
                    case "tblt_cvemotatn" -> rs.getInt("Cve_motatn");
                    case "tblt_cvecama" -> rs.getInt("Cve_cama");
                    default -> rs.getInt(1);
                };
            }
        } catch (SQLException e) {
            log.error(" Error obteniendo clave para {}: {}", descripcion, e.getMessage());
        }
        return -1;
    }

    private int obtenerClaveMedico(String nombreMedico) {
        if (nombreMedico == null) return -1;
        try (Connection conn = ConexionBD.conectar()) {
            var md = new MedicoData();
            return md.obtenerClavePorNombre(conn, nombreMedico);
        } catch (SQLException e) {
            log.error(" Error obteniendo clave médico: {}", e.getMessage());
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
        try (Connection conn = ConexionBD.conectar()) {
            var ud = new UrgenciasData();
            return ud.obtenerEstadoPaciente(conn, folioPaciente);
        } catch (SQLException e) {
            log.error("Error obteniendo estado del paciente: {}", e.getMessage());
        }
        return 1;
    }

    private boolean pacienteEnObservacion() {
        return obtenerEstadoPacienteActual() == 2;
    }

    private boolean pacienteEgresado() {
        return obtenerEstadoPacienteActual() == 3;
    }

    private void actualizarEstadoPaciente() {
        try (Connection conn = ConexionBD.conectar()) {
            var ud = new UrgenciasData();
            ud.actualizarEstadoPaciente(conn, folioPaciente, 2);
        } catch (SQLException e) {
            log.error("Error actualizando estado del paciente: {}", e.getMessage());
        }
    }

    private void configurarVisibilidadSegunRol() {
        if (esMedicoInterconsulta()) {
            ocultarSeccionNuevaInformacion();
        } else if (pacienteEgresado()) {
            ocultarSeccionParaEgresado();
        } else if (pacienteEnObservacion()) {
            rbAltaMedica.setVisible(true);
            rbAltaMedica.setDisable(false);
            rbObservacion.setVisible(true);
            rbObservacion.setDisable(true);
            rbObservacion.setSelected(true);
        }

        configurarBotonesSegunEstadoPaciente();

        if (pacienteEnObservacion() && !esMedicoInterconsulta()) {
            cargarDatosNuevaInfoDesdeBD();
            deshabilitarCamposNuevaInformacion();
            rbAltaMedica.setDisable(false);
            rbAltaMedica.setVisible(true);
        }
    }

    private void ocultarSeccionesComunes() {
        txtFechaAtencion.setVisible(false);
        txtHoraAtencion.setVisible(false);
        txtCedulaMedico.setVisible(false);
        cmbTipoUrgencia.setVisible(false);
        cmbMotivoUrgencia.setVisible(false);
        cmbTipoCama.setVisible(false);
        cmbMedicoActual.setVisible(false);
        rbObservacion.setVisible(false);
    }

    private void ocultarSeccionNuevaInformacion() {
        Platform.runLater(() -> {
            ocultarSeccionesComunes();
            rbAltaMedica.setVisible(false);
            btnGuardarGeneral.setVisible(false);
        });
    }

    private void configurarBotonesSegunEstadoPaciente() {
        boolean pacienteEgresado = pacienteEgresado();

        Platform.runLater(() -> {
            if (pacienteEgresado || !capturaGuardada) {
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
            log.warn("No hay nota seleccionada para visualizar");
            return;
        }

        NotaMedicaVO nota = tablaNotasMedicas.getSelectionModel().getSelectedItem();
        log.debug(" Visualizando nota #{}", nota.getNumeroNota());

        try {
            // Llamar al PDFGenerator NUEVO (solo necesita folio y número de nota)
            boolean exito = PDFGenerator.generarNotaMedicaPDF(
                    folioPaciente,          // int folioPaciente
                    nota.getNumeroNota()    // int numeroNota
            );

            if (exito) {
                log.info(" PDF generado correctamente");
            } else {
                mostrarAlerta("Error", "No se pudo generar el PDF", Alert.AlertType.ERROR);
                log.error("No se pudo generar el PDF");
            }

        } catch (Exception e) {
            log.error(" Error visualizando nota: {}", e.getMessage());
            mostrarAlerta("Error", "Error al generar PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void visualizarInterconsulta() {
        if (tablaInterconsultas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una interconsulta primero", Alert.AlertType.WARNING);
            log.warn("No hay interconsulta seleccionada para visualizar");
            return;
        }

        InterconsultaVO interconsulta = tablaInterconsultas.getSelectionModel().getSelectedItem();
        log.debug("Visualizando interconsulta #{}", interconsulta.getNumeroInterconsulta());

        try {
            // Llamar al PDFGenerator NUEVO (solo necesita folio y número de interconsulta)
            boolean exito = PDFGenerator.generarInterconsultaPDF(
                    folioPaciente,                      // int folioPaciente
                    interconsulta.getNumeroInterconsulta() // int numeroInterconsulta
            );

            if (exito) {
                log.info(" PDF de interconsulta generado correctamente");
            } else {
                mostrarAlerta("Error", "No se pudo generar el PDF de interconsulta", Alert.AlertType.ERROR);
                log.error("No se pudo generar el PDF de interconsulta");
            }

        } catch (Exception e) {
            log.error(" Error visualizando interconsulta: {}", e.getMessage());
            mostrarAlerta("Error", "Error al generar PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void otorgarPermisoInterconsulta() {
        if (tablaInterconsultas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una interconsulta primero", Alert.AlertType.WARNING);
            log.warn("No hay interconsulta seleccionada para otorgar permiso");
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
            log.warn("La interconsulta ya tiene permiso de edición.");
            return;
        }

        Optional<ButtonType> resultado = getButtonType(interconsultaSeleccionada);
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            if (otorgarPermisoInterconsultaEnBD(interconsultaSeleccionada.getIdInterconsulta())) {
                mostrarAlerta("Éxito", "Permiso de edición otorgado al especialista\n\n" +
                        "El especialista " + interconsultaSeleccionada.getEspecialista() +
                        " ahora puede editar esta interconsulta UNA sola vez", Alert.AlertType.INFORMATION);
                forzarActualizacionCompleta();
                log.info("Permiso otorgado correctamente");
            } else {
                mostrarAlerta("Error", "No se pudo otorgar el permiso", Alert.AlertType.ERROR);
                log.error("No se pudo otorgar el permiso");
            }
        }
    }

    private static Optional<ButtonType> getButtonType(InterconsultaVO interconsultaSeleccionada) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Otorgar Permiso de Edición");
        confirmacion.setHeaderText("¿Otorgar permiso de edición al especialista?");
        confirmacion.setContentText("Especialista: " + interconsultaSeleccionada.getEspecialista() +
                "\nInterconsulta #: " + interconsultaSeleccionada.getNumeroInterconsulta() +
                "\nFolio: " + interconsultaSeleccionada.getFolioPaciente() +
                "\n\n¿El especialista podrá editar esta interconsulta? (Solo una vez)");

        return confirmacion.showAndWait();
    }

    private void registrarEnHistorialPermisosInterconsulta(int idInterconsulta) {
        try (Connection conn = ConexionBD.conectar()) {
            var hd = new HistorialPermisosData();
            hd.otorgarPermisoInter(conn, new InsertarPermisoDTO(idInterconsulta, "INTERCONSULTA", "OTORGAR", usuarioLogueado, rolUsuarioLogueado));
        } catch (SQLException e) {
            log.error(" Error registrando en historial de interconsulta: {}", e.getMessage());
        }
    }

    private boolean otorgarPermisoEnBD(int idNota) {
        try (Connection conn = ConexionBD.conectar()) {
            var nd = new NotasData();
            if (nd.otorgarPermisoEdicion(conn, usuarioLogueado, idNota)) {
                registrarEnHistorialPermisos(idNota);
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            log.error(" Error otorgando permiso: {}", e.getMessage());
            return false;
        }
    }

    private void registrarEnHistorialPermisos(int idNota) {
        try (Connection conn = ConexionBD.conectar()) {
            var hd = new HistorialPermisosData();
            hd.otorgarPermisoNotas(conn, new InsertarPermisoDTO(idNota, "MEDICA", "OTORGAR", usuarioLogueado, rolUsuarioLogueado));
        } catch (SQLException e) {
            log.error(" Error registrando en historial: {}", e.getMessage());
        }
    }

    @FXML
    private void otorgarPermisoEdicion() {
        if (tablaNotasMedicas.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta("Error", "Seleccione una nota primero", Alert.AlertType.WARNING);
            log.warn("No hay nota seleccionada para otorgar permiso");
            return;
        }

        NotaMedicaVO notaSeleccionada = tablaNotasMedicas.getSelectionModel().getSelectedItem();

        log.debug(" VERIFICANDO NOTA PARA PERMISO:");
        log.debug("   ID: {}", notaSeleccionada.getIdNota());
        log.debug("   Médico: {}", notaSeleccionada.getMedicoAutor());
        log.debug("   ¿Ya editable? {}", notaSeleccionada.isEditablePorMedico());

        //  VERIFICAR SI YA TIENE PERMISO
        if (notaSeleccionada.isEditablePorMedico()) {
            mostrarAlerta("Permiso Ya Otorgado",
                    "Esta nota YA tiene permiso de edición.\n\n" +
                            "Médico: " + notaSeleccionada.getMedicoAutor() +
                            "\nNota #: " + notaSeleccionada.getNumeroNota() +
                            "\n\nEl médico solo puede usar el permiso UNA vez.",
                    Alert.AlertType.WARNING);
            log.warn("La nota ya tiene permiso de edición");
            return;
        }

        //  VERIFICAR QUE NO SE OTORGUE PERMISO A SÍ MISMO
        if (notaSeleccionada.getMedicoAutor().equals(sesion.getNombreMedico())) {
            mostrarAlerta("Error", "No puede otorgarse permiso a sí mismo", Alert.AlertType.WARNING);
            log.warn("Intento de otorgarse permiso a sí mismo");
            return;
        }

        Optional<ButtonType> resultado = getButtonType(notaSeleccionada);
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            if (otorgarPermisoEnBD(notaSeleccionada.getIdNota())) {
                mostrarAlerta("Éxito", " Permiso de edición otorgado al médico\n\n" +
                                "Médico: " + notaSeleccionada.getMedicoAutor() +
                                "\nNota #: " + notaSeleccionada.getNumeroNota() +
                                "\n\nEl médico ahora puede editar esta nota UNA sola vez",
                        Alert.AlertType.INFORMATION);
                forzarActualizacionCompleta();
                log.info("Permiso otorgado correctamente de edición");
            } else {
                mostrarAlerta("Error", "No se pudo otorgar el permiso", Alert.AlertType.ERROR);
                log.error("No se pudo otorgar el permiso de edición");
            }
        }
    }

    private static Optional<ButtonType> getButtonType(NotaMedicaVO notaSeleccionada) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Otorgar Permiso de Edición");
        confirmacion.setHeaderText("¿Otorgar permiso de edición al médico?");
        confirmacion.setContentText("Médico: " + notaSeleccionada.getMedicoAutor() +
                "\nNota #: " + notaSeleccionada.getNumeroNota() +
                "\nFolio: " + notaSeleccionada.getFolioPaciente() +
                "\n\n¿El médico podrá editar esta nota? (Solo una vez)");

        return confirmacion.showAndWait();
    }

    private void abrirVentanaEgreso() {
        try {
            log.debug(" Abriendo ventana de egreso...");

            URL fxmlUrl = getClass().getResource("/views/egreso_paciente.fxml");
            if (fxmlUrl == null) {
                mostrarAlerta("Error", "No se encontró ventana de egreso", Alert.AlertType.ERROR);
                log.error("No se encontró ventana de egreso");
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

            log.info("Ventana de egreso abierta. El egreso se hará DENTRO de esa ventana.");

        } catch (Exception e) {
            log.error(" Error abriendo ventana de egreso: {}", e.getMessage());
            mostrarAlerta("Error", "Error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // cosas de prueba borrar todo lo que est despues de estp
    private void ocultarSeccionParaEgresado() {
        Platform.runLater(() -> {
            // Ocultar todo excepto RadioButton para Egreso
            ocultarSeccionesComunes();
            btnGuardarGeneral.setVisible(false);

            // Solo mostrar RadioButton de Egreso (pero deshabilitado)
            rbAltaMedica.setVisible(true);
            rbAltaMedica.setDisable(true);
            rbAltaMedica.setSelected(false);
        });
    }

    private void mostrarSoloRadioButtonsParaEgreso() {
        Platform.runLater(() -> {
            ocultarSeccionesComunes();
            // Mostrar ambos RadioButtons
            rbObservacion.setVisible(true);
            rbObservacion.setDisable(true); // No puede cambiar de Observación
            rbObservacion.setSelected(true); // Ya está en observación

            rbAltaMedica.setVisible(true);
            rbAltaMedica.setDisable(false); // Puede seleccionar para egresar

            btnGuardarGeneral.setVisible(true);
        });
    }

    private void habilitarElementosUI() {
        txtFolio.setDisable(false);
        txtFechaRegistro.setDisable(false);
        txtHoraRegistro.setDisable(false);
        txtTriage.setDisable(false);
        txtNombre.setDisable(false);
        txtEdad.setDisable(false);
        txtSexo.setDisable(false);
        txtMunicipio.setDisable(false);
        txtEntidad.setDisable(false);
        txtDerechohabiencia.setDisable(false);
        txtReferencia.setDisable(false);
        txtMedicoIngreso.setDisable(false);
        txtDomicilio.setDisable(false);
        txtSintomas.setDisable(false);
        btnColorTriage.setDisable(false);
        txtFechaAtencion.setDisable(false);
        txtHoraAtencion.setDisable(false);
        txtCedulaMedico.setDisable(false);
        tablaNotasMedicas.setDisable(false);
        tablaInterconsultas.setDisable(false);
        btnVisualizarNotaMedica.setDisable(false);
        btnEditarNotaMedica.setDisable(false);
        btnImprimirNotaMedica.setDisable(false);
        btnVisualizarInterconsulta.setDisable(false);
        btnEditarInterconsulta.setDisable(false);
        btnImprimirInterconsulta.setDisable(false);
        btnNuevaNotaMedica.setDisable(false);
        btnNuevaInterconsulta.setDisable(false);
        btnOtorgarPermiso.setDisable(false);
        btnOtorgarPermisoInterconsulta.setDisable(false);
        btnGuardarGeneral.setDisable(false);
    }

    private void validarCamposRequeridosYActivarUI() {
        boolean camposCompletos = cmbTipoUrgencia.getValue() != null &&
                cmbMotivoUrgencia.getValue() != null &&
                cmbTipoCama.getValue() != null &&
                cmbMedicoActual.getValue() != null &&
                (rbObservacion.isSelected() || rbAltaMedica.isSelected());

        if (camposCompletos) {
            habilitarElementosUI();
        } else {
            deshabilitarElementosUI();
        }
    }

    private void deshabilitarElementosUI() {
        txtFolio.setDisable(true);
        txtFechaRegistro.setDisable(true);
        txtHoraRegistro.setDisable(true);
        txtTriage.setDisable(true);
        txtNombre.setDisable(true);
        txtEdad.setDisable(true);
        txtSexo.setDisable(true);
        txtMunicipio.setDisable(true);
        txtEntidad.setDisable(true);
        txtDerechohabiencia.setDisable(true);
        txtReferencia.setDisable(true);
        txtMedicoIngreso.setDisable(true);
        txtDomicilio.setDisable(true);
        txtSintomas.setDisable(true);
        btnColorTriage.setDisable(true);
        txtFechaAtencion.setDisable(true);
        txtHoraAtencion.setDisable(true);
        txtCedulaMedico.setDisable(true);
        tablaNotasMedicas.setDisable(true);
        tablaInterconsultas.setDisable(true);
        btnVisualizarNotaMedica.setDisable(true);
        btnEditarNotaMedica.setDisable(true);
        btnImprimirNotaMedica.setDisable(true);
        btnVisualizarInterconsulta.setDisable(true);
        btnEditarInterconsulta.setDisable(true);
        btnImprimirInterconsulta.setDisable(true);
        btnNuevaNotaMedica.setDisable(true);
        btnNuevaInterconsulta.setDisable(true);
        btnOtorgarPermiso.setDisable(true);
        btnOtorgarPermisoInterconsulta.setDisable(true);
        btnGuardarGeneral.setDisable(true);
    }
}