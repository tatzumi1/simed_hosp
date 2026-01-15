//src/main/java/com/PruebaSimed2/controllers/InterconsultaController.java

package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.InterconsultaVO;
import com.PruebaSimed2.utils.PDFGenerator;
import com.PruebaSimed2.utils.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.sql.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.PruebaSimed2.utils.NameGenerator.generateName;

@Log4j2
public class InterconsultaController {

    @FXML
    private Label lblFolio;
    @FXML
    private ComboBox<String> cmbEspecialistas;
    @FXML
    private TextField txtCedula;
    @FXML
    private TextField txtEspecialidad; // Cambiado de ComboBox a TextField
    @FXML
    private TextArea txtSintomas;
    @FXML
    private TextArea txtSignosVitales;
    @FXML
    private TextArea txtDiagnostico;
    @FXML
    private TextArea txtIndicaciones;
    @FXML
    private Button btnGuardarDefinitivo;
    @FXML
    private Button btnGuardarTemporal;

    // Constantes para límites de datos
    private static final int MAX_CHARS_ESPECIALIDAD = 50;
    private static final int MAX_CHARS_SINTOMAS = 800;
    private static final int MAX_CHARS_SIGNOS = 500;
    private static final int MAX_CHARS_DIAGNOSTICO = 10000; // 3 páginas
    private static final int MAX_CHARS_INDICACIONES = 7000; // 9,000 2 páginas
    private static final int MAX_TOTAL_CHARS = 19000; // Límite total para evitar saturación

    private String universidadEspecialista;
    private int folioPaciente;
    private String usuarioLogueado;
    private boolean modoEdicion = false;
    private InterconsultaVO interconsultaEnEdicion;
    private Integer idInterconsultaActual = null;
    private Integer numeroInterconsultaActual = null;
    private String rolUsuarioLogueado;
    private SesionUsuario sesion = SesionUsuario.getInstance();

    // Variables para control de tiempo
    private Date primeraFechaCreacion = null;
    private String primeraHoraCreacion = null;
    private boolean esPrimeraGuardada = true;

    public void setFolioPaciente(int folio) {
        this.folioPaciente = folio;
        lblFolio.setText("Folio: " + folio);
        cargarEspecialistas();
        cargarInterconsultaExistente();
    }

    public void setUsuarioLogueado(String usuario, String rol) {
        this.usuarioLogueado = usuario;
        this.rolUsuarioLogueado = rol;

        //  INICIALIZAR SESIÓN CORRECTAMENTE
        if (sesion.getUsername() == null) {
            int usuarioId = obtenerIdUsuarioDesdeBD(usuario);
            sesion.inicializar(usuario, rol, usuarioId);
        }

        log.debug("Usuario en interconsulta: {} - Rol: {}", usuario, rol);
        log.debug("Nombre médico en sesión: {}", sesion.getNombreMedico());
    }

    /**
     * OBTENER ID DE USUARIO DESDE BD
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
            log.error("Error obteniendo ID de usuario: {}", e.getMessage());
        }
        return 0;
    }

    // Método para otorgar permiso (27 de nov)
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
                log.debug("Permiso otorgado para interconsulta - ID: {}", idInterconsulta);
                registrarEnHistorialPermisosInterconsulta(idInterconsulta, "INTERCONSULTA", "OTORGAR");
                return true;
            }
            return false;

        } catch (SQLException e) {
            log.error("Error otorgando permiso para interconsulta: {}", e.getMessage());
            return false;
        }
    }

    @Getter
    public static class DatosCaptura {
        // Getters
        private String tipoUrgencia;
        private String motivoUrgencia;
        private String tipoCama;
        private String medico;

        public DatosCaptura(String tipoUrgencia, String motivoUrgencia, String tipoCama, String medico) {
            this.tipoUrgencia = tipoUrgencia;
            this.motivoUrgencia = motivoUrgencia;
            this.tipoCama = tipoCama;
            this.medico = medico;
        }
    }

    private DatosCaptura datosCaptura;

    public void setDatosCaptura(String tipoUrgencia, String motivoUrgencia, String tipoCama, String medico) {
        this.datosCaptura = new DatosCaptura(tipoUrgencia, motivoUrgencia, tipoCama, medico);
        log.debug(" Datos de captura recibidos en InterconsultaController:");
        log.debug("- Tipo Urgencia: {}", tipoUrgencia);
        log.debug("- Motivo Urgencia: {}", motivoUrgencia);
        log.debug("- Tipo Cama: {}", tipoCama);
        log.debug("- Médico: {}", medico);
    }

    @FXML
    public void initialize() {
        cmbEspecialistas.setOnAction(e -> actualizarCedula());

        // Configurar límites en los TextArea y TextField para prevenir saturación de RAM
        configurarLimitesTextos();

        // Agregar sugerencias al campo de especialidad
        configurarSugerenciasEspecialidad();
    }

    private void configurarLimitesTextos() {
        // Límite para campo de especialidad (TextField)
        txtEspecialidad.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_CHARS_ESPECIALIDAD) {
                txtEspecialidad.setText(oldValue);
                mostrarAlerta("Límite excedido",
                        "El campo Especialidad no puede exceder los " + MAX_CHARS_ESPECIALIDAD + " caracteres",
                        Alert.AlertType.WARNING);
            }
        });

        // Límites para TextAreas
        txtSintomas.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_CHARS_SINTOMAS) {
                txtSintomas.setText(oldValue);
                mostrarAlerta("Límite excedido",
                        "El campo Síntomas no puede exceder los " + MAX_CHARS_SINTOMAS + " caracteres",
                        Alert.AlertType.WARNING);
            }
        });

        txtSignosVitales.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_CHARS_SIGNOS) {
                txtSignosVitales.setText(oldValue);
                mostrarAlerta("Límite excedido",
                        "El campo Signos Vitales no puede exceder los " + MAX_CHARS_SIGNOS + " caracteres",
                        Alert.AlertType.WARNING);
            }
        });

        txtDiagnostico.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_CHARS_DIAGNOSTICO) {
                txtDiagnostico.setText(oldValue);
                mostrarAlerta("Límite excedido",
                        "El campo Diagnóstico no puede exceder los " + MAX_CHARS_DIAGNOSTICO + " caracteres",
                        Alert.AlertType.WARNING);
            }
        });

        txtIndicaciones.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_CHARS_INDICACIONES) {
                txtIndicaciones.setText(oldValue);
                mostrarAlerta("Límite excedido",
                        "El campo Indicaciones no puede exceder los " + MAX_CHARS_INDICACIONES + " caracteres",
                        Alert.AlertType.WARNING);
            }
        });
    }

    private void configurarSugerenciasEspecialidad() {
        // Crear un Tooltip con sugerencias
        Tooltip tooltip = new Tooltip("Especialidades comunes:\n" +
                "• MEDICINA INTERNA\n" +
                "• CARDIOLOGÍA\n" +
                "• NEUROLOGÍA\n" +
                "• PEDIATRÍA\n" +
                "• GINECOLOGÍA\n" +
                "• TRAUMATOLOGÍA\n" +
                "• CIRUGÍA GENERAL\n" +
                "• GASTROENTEROLOGÍA\n" +
                "• NEFROLOGÍA\n" +
                "• NEUMOLOGÍA\n" +
                "\nEscriba la especialidad del médico");
        tooltip.setWrapText(true);
        tooltip.setShowDelay(javafx.util.Duration.millis(100));
        txtEspecialidad.setTooltip(tooltip);
    }

    private void cargarEspecialistas() {
        try (Connection conn = ConexionBD.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Nombre, Cedula FROM tb_medesp ORDER BY Nombre")) {

            cmbEspecialistas.getItems().clear();
            while (rs.next()) {
                cmbEspecialistas.getItems().add(rs.getString("Nombre"));
            }
        } catch (SQLException e) {
            log.error("Error cargando especialistas: {}", e.getMessage());
        }
    }

    private void cargarInterconsultaExistente() {
        String sql = "SELECT id_inter, Num_inter, Nota, sintomas, signos_vitales, diagnostico, " +
                "especialidad, Medico, Cedula, Fecha, Hora, Estado FROM tb_inter " +
                "WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ? ORDER BY id_inter DESC LIMIT 1";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folioPaciente);
            pstmt.setString(2, obtenerNombreMedicoDesdeUsuario(usuarioLogueado));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                idInterconsultaActual = rs.getInt("id_inter");
                numeroInterconsultaActual = rs.getInt("Num_inter");

                // OBTENER FECHA Y HORA DE LA PRIMERA CREACIÓN
                primeraFechaCreacion = rs.getDate("Fecha");
                primeraHoraCreacion = rs.getString("Hora");
                esPrimeraGuardada = false; // Ya existe una interconsulta, no es la primera

                log.debug(" FECHA/HORA DE CREACIÓN ORIGINAL INTERCONSULTA:");
                log.debug("Fecha: {}", primeraFechaCreacion);
                log.debug("Hora: {}", primeraHoraCreacion);

                // CARGAR CAMPOS SEPARADOS
                txtSintomas.setText(rs.getString("sintomas"));
                txtSignosVitales.setText(rs.getString("signos_vitales"));
                txtDiagnostico.setText(rs.getString("diagnostico"));
                txtIndicaciones.setText(rs.getString("Nota")); // Nota va en indicaciones para interconsulta
                txtEspecialidad.setText(rs.getString("especialidad"));

                cmbEspecialistas.setValue(rs.getString("Medico"));
                actualizarCedula();

                log.debug("Interconsulta temporal existente cargada - ID: {}", idInterconsultaActual);
            }
        } catch (SQLException e) {
            log.error("Error cargando interconsulta existente: {}", e.getMessage());
        }
    }

    private void actualizarCedula() {
        String especialistaSeleccionado = cmbEspecialistas.getValue();
        if (especialistaSeleccionado != null) {
            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT Cedula, universidad FROM tb_medesp WHERE Nombre = ?")) {

                pstmt.setString(1, especialistaSeleccionado);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    txtCedula.setText(rs.getString("Cedula"));
                    this.universidadEspecialista = rs.getString("universidad");
                    log.debug("Universidad del especialista: {}", universidadEspecialista);
                }
            } catch (SQLException e) {
                log.error("Error obteniendo cédula y universidad: {}", e.getMessage());
            }
        }
    }

    @FXML
    private void guardarTemporal() {
        if (!validarCampos()) return;
        if (!verificarLimitesDatos()) return; // Verificar límites antes de guardar

        Connection conn = null;
        boolean operacionExitosa = false;

        try {
            conn = ConexionBD.conectar();
            if (conn == null) {
                mostrarAlerta("Error", "No hay conexión a la base de datos", Alert.AlertType.ERROR);
                return;
            }

            conn.setAutoCommit(false);

            // OBTENER TODOS LOS CAMPOS
            String sintomasActual = txtSintomas.getText().trim();
            String signosVitalesActual = txtSignosVitales.getText().trim();
            String diagnosticoActual = txtDiagnostico.getText().trim();
            String indicacionesActual = txtIndicaciones.getText().trim();
            String especialistaActual = cmbEspecialistas.getValue();
            String cedulaActual = txtCedula.getText();
            String especialidadActual = txtEspecialidad.getText().trim(); // Ahora es TextField

            log.debug(" INICIANDO GUARDADO TEMPORAL INTERCONSULTA ===================");
            log.debug(" Especialidad: {}", especialidadActual);
            log.debug(" Síntomas: {} chars", sintomasActual.length());
            log.debug(" Signos Vitales: {} chars", signosVitalesActual.length());
            log.debug(" Diagnóstico: {} chars", diagnosticoActual.length());
            log.debug(" Indicaciones: {} chars", indicacionesActual.length());

            int filasAfectadas = 0;

            if (idInterconsultaActual == null || esPrimeraGuardada) {
                // NUEVA INTERCONSULTA TEMPORAL o PRIMERA GUARDADA
                numeroInterconsultaActual = obtenerSiguienteNumInterconsulta(conn);

                // Determinar si usar fecha/hora actual o mantener la existente
                String fechaSQL, horaSQL;

                if (esPrimeraGuardada && primeraFechaCreacion != null && primeraHoraCreacion != null) {
                    // Usar la fecha/hora de la primera creación
                    fechaSQL = "?";
                    horaSQL = "?";
                } else {
                    // Usar fecha/hora actual (primera vez)
                    fechaSQL = "CURDATE()";
                    horaSQL = "CURTIME()";
                }

                String sql = "INSERT INTO tb_inter (Folio, Num_inter, Nota, sintomas, signos_vitales, " +
                        "diagnostico, especialidad, Medico, Cedula, Fecha, Hora, Estado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, " + fechaSQL + ", " + horaSQL + ", 'TEMPORAL')";

                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, folioPaciente);
                    pstmt.setInt(2, numeroInterconsultaActual);
                    pstmt.setString(3, indicacionesActual);
                    pstmt.setString(4, sintomasActual);
                    pstmt.setString(5, signosVitalesActual);
                    pstmt.setString(6, diagnosticoActual);
                    pstmt.setString(7, especialidadActual);
                    pstmt.setString(8, especialistaActual);
                    pstmt.setString(9, cedulaActual);

                    // Si es primera guardada con fecha/hora existente, usar esas
                    if (esPrimeraGuardada && primeraFechaCreacion != null && primeraHoraCreacion != null) {
                        pstmt.setDate(10, new java.sql.Date(primeraFechaCreacion.getTime()));
                        pstmt.setString(11, primeraHoraCreacion);
                        log.debug(" Usando fecha/hora original de primera creación");
                    }

                    filasAfectadas = pstmt.executeUpdate();

                    if (filasAfectadas > 0) {
                        ResultSet rs = pstmt.getGeneratedKeys();
                        if (rs.next()) {
                            idInterconsultaActual = rs.getInt(1);
                            log.debug(" NUEVA INTERCONSULTA TEMPORAL CREADA - ID: {}", idInterconsultaActual);

                            // Si es primera guardada, marcar como no primera para próximas
                            if (esPrimeraGuardada) {
                                esPrimeraGuardada = false;
                            }

                            // OBTENER Y GUARDAR LA FECHA/HORA DE CREACIÓN
                            guardarFechaHoraCreacion(conn, idInterconsultaActual);
                        }
                    }
                }
            } else {
                // ACTUALIZAR INTERCONSULTA TEMPORAL EXISTENTE - NO MODIFICAR FECHA/HORA
                String sql = "UPDATE tb_inter SET Nota = ?, sintomas = ?, signos_vitales = ?, " +
                        "diagnostico = ?, especialidad = ?, Medico = ?, Cedula = ?, Estado = 'TEMPORAL' " +
                        "WHERE id_inter = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, indicacionesActual);
                    pstmt.setString(2, sintomasActual);
                    pstmt.setString(3, signosVitalesActual);
                    pstmt.setString(4, diagnosticoActual);
                    pstmt.setString(5, especialidadActual);
                    pstmt.setString(6, especialistaActual);
                    pstmt.setString(7, cedulaActual);
                    pstmt.setInt(8, idInterconsultaActual);
                    // IMPORTANTE: NO actualizamos Fecha ni Hora

                    filasAfectadas = pstmt.executeUpdate();
                    log.debug(" INTERCONSULTA TEMPORAL ACTUALIZADA - ID: {}", idInterconsultaActual);
                    log.debug(" Fecha y hora ORIGINALES preservadas (no se modifican)");
                }
            }

            if (filasAfectadas > 0) {
                //  ACTUALIZAR ESTADO DEL PACIENTE A OBSERVACIÓN
                actualizarEstadoPacienteObservacion(conn);

                conn.commit();
                operacionExitosa = true;
                mostrarAlerta("Éxito", "Interconsulta guardada temporalmente\nPaciente en OBSERVACIÓN", Alert.AlertType.INFORMATION);
            } else {
                conn.rollback();
                mostrarAlerta("Error", "No se pudo guardar la interconsulta", Alert.AlertType.ERROR);
            }

        } catch (SQLException e) {
            log.error(" ERROR EN TRANSACCIÓN: {}", e.getMessage());
            log.error(e);
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                log.error(" ERROR AL REVERTIR: {}", rollbackEx.getMessage());
            }
            mostrarAlerta("Error de Base de Datos", "Error al guardar: " + e.getMessage(), Alert.AlertType.ERROR);
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
    }

    /**
     * Guarda la fecha y hora de creación para uso futuro
     */
    private void guardarFechaHoraCreacion(Connection conn, int idInterconsulta) {
        String sql = "SELECT Fecha, Hora FROM tb_inter WHERE id_inter = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idInterconsulta);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                primeraFechaCreacion = rs.getDate("Fecha");
                primeraHoraCreacion = rs.getString("Hora");
                log.debug("FECHA/HORA DE CREACIÓN INTERCONSULTA GUARDADA:");
                log.debug("Fecha: {}", primeraFechaCreacion);
                log.debug("Hora: {}", primeraHoraCreacion);
            }
        } catch (SQLException e) {
            log.error("Error guardando fecha/hora creación interconsulta: {}", e.getMessage());
        }
    }

    @FXML
    private void guardarDefinitivo() {
        if (!validarCampos()) return;
        if (!verificarLimitesDatos()) return;

        // Deshabilitar botones inmediatamente
        btnGuardarDefinitivo.setDisable(true);
        btnGuardarTemporal.setDisable(true);

        // GUARDAR VARIABLES LOCALES COMO FINAL
        final int folioFinal = folioPaciente;
        final Integer numInterFinal = numeroInterconsultaActual;
        final String sintomasFinal = txtSintomas.getText();
        final String signosFinal = txtSignosVitales.getText();
        final String diagnosticoFinal = txtDiagnostico.getText();
        final String indicacionesFinal = txtIndicaciones.getText();
        final String especialistaFinal = cmbEspecialistas.getValue();
        final String cedulaFinal = txtCedula.getText();
        final String especialidadFinal = txtEspecialidad.getText();

        // OBTENER FECHA/HORA ORIGINAL ANTES DEL HILO
        final String[] fechaHoraOriginal = new String[2]; // [0]=fecha, [1]=hora

        try (Connection connTemp = ConexionBD.conectar()) {
            String sqlSelect = "SELECT Fecha, Hora FROM tb_inter WHERE id_inter = ?";
            try (PreparedStatement pstmtSelect = connTemp.prepareStatement(sqlSelect)) {
                pstmtSelect.setInt(1, idInterconsultaActual);
                ResultSet rs = pstmtSelect.executeQuery();
                if (rs.next()) {
                    fechaHoraOriginal[0] = rs.getString("Fecha");
                    fechaHoraOriginal[1] = rs.getString("Hora");
                    log.debug("FECHA/HORA ORIGINAL INTERCONSULTA: {} {}", fechaHoraOriginal[0], fechaHoraOriginal[1]);
                }
            }
        } catch (SQLException e) {
            log.error("Error obteniendo fecha/hora original: {}", e.getMessage());
        }

        // Ejecutar en hilo separado
        new Thread(() -> {
            Connection conn = null;
            boolean exito = false;

            try {
                conn = ConexionBD.conectar();
                if (conn == null) {
                    Platform.runLater(() ->
                            mostrarAlerta("Error", "No hay conexión a BD", Alert.AlertType.ERROR));
                    return;
                }

                conn.setAutoCommit(false);

                if (idInterconsultaActual == null) {
                    Platform.runLater(() -> {
                        guardarTemporal();
                        if (idInterconsultaActual == null) {
                            mostrarAlerta("Error", "No se pudo crear la interconsulta", Alert.AlertType.ERROR);
                        }
                    });
                    return;
                }

                //  ACTUALIZAR A DEFINITIVA SIN TOCAR FECHA/HORA
                String sqlUpdate = "UPDATE tb_inter SET Nota = ?, sintomas = ?, signos_vitales = ?, " +
                        "diagnostico = ?, especialidad = ?, Medico = ?, Cedula = ?, Estado = 'DEFINITIVA' " +
                        "WHERE id_inter = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                    pstmt.setString(1, indicacionesFinal);
                    pstmt.setString(2, sintomasFinal);
                    pstmt.setString(3, signosFinal);
                    pstmt.setString(4, diagnosticoFinal);
                    pstmt.setString(5, especialidadFinal);
                    pstmt.setString(6, especialistaFinal);
                    pstmt.setString(7, cedulaFinal);
                    pstmt.setInt(8, idInterconsultaActual);

                    int filas = pstmt.executeUpdate();

                    if (filas > 0) {

                        if (modoEdicion && interconsultaEnEdicion != null && interconsultaEnEdicion.isEditablePorMedico()) {
                            revocarPermisoInterconsultaDespuesDeUso();
                        }

                        conn.commit();
                        exito = true;

                        Platform.runLater(() -> {
                            mostrarAlerta("Éxito", "Interconsulta #" + numInterFinal +
                                    " guardada DEFINITIVAMENTE", Alert.AlertType.INFORMATION);

                            generarPDFAutomatico(sintomasFinal, signosFinal, diagnosticoFinal,
                                    indicacionesFinal, especialistaFinal, especialidadFinal,
                                    fechaHoraOriginal[0], fechaHoraOriginal[1]);
                        });

                    } else {
                        conn.rollback();
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "No se pudo guardar la interconsulta definitiva",
                                        Alert.AlertType.ERROR));
                    }
                }

            } catch (SQLException e) {
                log.error(" Error SQL en guardado definitivo: {}", e.getMessage());
                try {
                    if (conn != null) conn.rollback();
                } catch (SQLException ex) {
                    log.error(" Error en rollback: {}", ex.getMessage());
                }
                Platform.runLater(() ->
                        mostrarAlerta("Error", "Error al guardar: " + e.getMessage(), Alert.AlertType.ERROR));
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException e) {
                    log.error(" Error cerrando conexión: {}", e.getMessage());
                }

                if (!exito) {
                    Platform.runLater(() -> {
                        btnGuardarDefinitivo.setDisable(false);
                        btnGuardarTemporal.setDisable(false);
                    });
                }
            }
        }).start();
    }

    // MÉTODO VERSÁTIL CORREGIDO - usa ID 2 para Observación
    private void actualizarEstadoPacienteObservacion(Connection... conn) {
        String sql = "UPDATE tb_urgencias SET Estado_pac = ? WHERE Folio = ?";

        boolean usarConexionExterna = conn.length > 0 && conn[0] != null;

        try {
            // ID CORREGIDO: 2 = Observación (según tu tabla)
            int idObservacion = 2;

            PreparedStatement pstmt;

            if (usarConexionExterna) {
                // Usar conexión existente (para transacciones)
                pstmt = conn[0].prepareStatement(sql);
            } else {
                // Crear nueva conexión
                Connection nuevaConn = ConexionBD.conectar();
                pstmt = nuevaConn.prepareStatement(sql);
            }

            pstmt.setInt(1, idObservacion);
            pstmt.setInt(2, folioPaciente);

            int filasActualizadas = pstmt.executeUpdate();

            if (filasActualizadas > 0) {
                log.debug("PACIENTE ACTUALIZADO A OBSERVACIÓN - Folio: {} (ID Estado: {})", folioPaciente, idObservacion);
            } else {
                log.debug("Paciente ya estaba en OBSERVACIÓN - Folio: {}", folioPaciente);
            }

            pstmt.close();

        } catch (SQLException e) {
            log.error("Error actualizando estado a observación: {}", e.getMessage());
        }
    }

    private void generarPDFAutomatico(String sintomas, String signosVitales, String diagnostico,
                                      String indicaciones, String especialista, String especialidad,
                                      String fecha, String hora) {
        try {
            log.debug(" GENERANDO PDF INTERCONSULTA:");
            log.debug("   Fecha BD: {}", fecha);
            log.debug("   Hora BD: {}", hora);
            log.debug("   Especialista: {}", especialista);
            log.debug("   Especialidad: {}", especialidad);
            log.debug("   Folio: {}", folioPaciente);

            //  LLAMAR AL PDFGenerator NUEVO (solo necesita folio y número de interconsulta)
            boolean exito = PDFGenerator.generarInterconsultaPDF(
                    folioPaciente,                                          // int folioPaciente
                    numeroInterconsultaActual != null ?
                            numeroInterconsultaActual : 1                      // int numeroInterconsulta
            );

            if (exito) {
                log.debug(" PDF de interconsulta generado automáticamente");
                log.debug("   Fecha: {}", fecha);
                log.debug("   Hora: {}", hora);
                abrirPDFInterconsultaReciente();

                // Cerrar ventana después de 3 segundos
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        javafx.application.Platform.runLater(() -> {
                            Stage stage = (Stage) btnGuardarDefinitivo.getScene().getWindow();
                            if (stage != null && stage.isShowing()) {
                                stage.close();
                            }
                        });
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }).start();
            } else {
                log.error(" Error generando PDF de interconsulta");
            }

        } catch (Exception e) {
            log.error(" Error generando PDF automático de interconsulta: {}", e.getMessage());
            log.error(e);
        }
    }


    private int obtenerSiguienteNumInterconsulta(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(Num_inter), 0) + 1 as siguiente_num FROM tb_inter WHERE Folio = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, folioPaciente);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("siguiente_num");
            }
        }
        return 1;
    }

    private Map<String, String> obtenerDatosPacienteCompletos() {
        Map<String, String> datos = new HashMap<>();
        String sql = "SELECT u.*, " +
                "dh.Derechohabiencia as NombreDerechohabiencia, " +
                "s.Descripcion as SexoDesc " +
                "FROM tb_urgencias u " +
                "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                "LEFT JOIN tblt_cvesexo s ON u.Sexo = s.Cve_sexo " +
                "WHERE u.Folio = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folioPaciente);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Nombre completo con apellidos
                String nombreCompleto = generateName(rs.getString("A_paterno"), rs.getString("A_materno"), rs.getString("Nombre"));

                // Datos básicos
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
                datos.put("curp", rs.getString("CURP"));          // CURP
                datos.put("telefono", rs.getString("Telefono"));  // Teléfono

                log.debug(" DATOS PACIENTE INTERCONSULTA:");
                log.debug("   CURP: {}", datos.get("curp"));
                log.debug("   Teléfono: {}", datos.get("telefono"));
            }
        } catch (SQLException e) {
            log.error(" Error obteniendo datos paciente: {}", e.getMessage());
        }

        // Valores por defecto
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
        datos.putIfAbsent("curp", "No especificado");      // Por defecto para CURP
        datos.putIfAbsent("telefono", "No especificado");  // Por defecto para Teléfono

        return datos;
    }

    private void abrirPDFInterconsultaReciente() {
        try {
            File directorio = new File("pdfs");
            if (directorio.exists()) {
                File[] archivos = directorio.listFiles((dir, name) ->
                        name.startsWith("Interconsulta_Folio_" + folioPaciente + "_"));

                if (archivos != null && archivos.length > 0) {
                    Arrays.sort(archivos, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    File pdfMasReciente = archivos[0];
                    java.awt.Desktop.getDesktop().open(pdfMasReciente);
                    log.debug("Abriendo PDF de interconsulta: {}", pdfMasReciente.getName());
                } else {
                    mostrarAlerta("Error", "No se encontró el PDF generado", Alert.AlertType.WARNING);
                }
            } else {
                mostrarAlerta("Error", "No se encontró la carpeta de PDFs", Alert.AlertType.WARNING);
            }
        } catch (Exception e) {
            log.error(" Error abriendo PDF: {}", e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir el PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private boolean verificarLimitesDatos() {
        int totalChars = 0;
        List<String> camposExcedidos = new ArrayList<>();

        // Verificar límites individuales
        if (txtEspecialidad.getText().length() > MAX_CHARS_ESPECIALIDAD) {
            camposExcedidos.add("Especialidad (" + txtEspecialidad.getText().length() + "/" + MAX_CHARS_ESPECIALIDAD + ")");
        }
        totalChars += txtEspecialidad.getText().length();

        if (txtSintomas.getText().length() > MAX_CHARS_SINTOMAS) {
            camposExcedidos.add("Síntomas (" + txtSintomas.getText().length() + "/" + MAX_CHARS_SINTOMAS + ")");
        }
        totalChars += txtSintomas.getText().length();

        if (txtSignosVitales.getText().length() > MAX_CHARS_SIGNOS) {
            camposExcedidos.add("Signos Vitales (" + txtSignosVitales.getText().length() + "/" + MAX_CHARS_SIGNOS + ")");
        }
        totalChars += txtSignosVitales.getText().length();

        if (txtDiagnostico.getText().length() > MAX_CHARS_DIAGNOSTICO) {
            camposExcedidos.add("Diagnóstico (" + txtDiagnostico.getText().length() + "/" + MAX_CHARS_DIAGNOSTICO + ")");
        }
        totalChars += txtDiagnostico.getText().length();

        if (txtIndicaciones.getText().length() > MAX_CHARS_INDICACIONES) {
            camposExcedidos.add("Indicaciones (" + txtIndicaciones.getText().length() + "/" + MAX_CHARS_INDICACIONES + ")");
        }
        totalChars += txtIndicaciones.getText().length();

        // Verificar límite total
        if (totalChars > MAX_TOTAL_CHARS) {
            camposExcedidos.add("TOTAL (" + totalChars + "/" + MAX_TOTAL_CHARS + " caracteres)");
        }

        // Mostrar alertas si hay campos excedidos
        if (!camposExcedidos.isEmpty()) {
            StringBuilder mensaje = new StringBuilder("Límites de datos excedidos:\n\n");
            for (String campo : camposExcedidos) {
                mensaje.append("• ").append(campo).append("\n");
            }
            mensaje.append("\nPor favor, reduzca el contenido para evitar saturación de memoria.");

            mostrarAlerta("Límites Excedidos", mensaje.toString(), Alert.AlertType.ERROR);
            return false;
        }

        // Verificar uso de memoria
        if (totalChars > MAX_TOTAL_CHARS * 0.8) {
            mostrarAlerta("Advertencia",
                    "Está usando el " + (totalChars * 100 / MAX_TOTAL_CHARS) +
                            "% de la capacidad máxima. Considere reducir el contenido si planea más ediciones.",
                    Alert.AlertType.WARNING);
        }

        return true;
    }

    private boolean validarCampos() {
        // Validar especialista
        if (cmbEspecialistas.getValue() == null) {
            mostrarAlerta("Error", "Seleccione un especialista", Alert.AlertType.ERROR);
            return false;
        }

        // Validar especialidad (ahora es TextField)
        if (txtEspecialidad.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El campo ESPECIALIDAD es obligatorio", Alert.AlertType.ERROR);
            txtEspecialidad.requestFocus();
            return false;
        }

        // Validar que la especialidad no sea demasiado larga
        if (txtEspecialidad.getText().trim().length() > MAX_CHARS_ESPECIALIDAD) {
            mostrarAlerta("Error", "La especialidad es demasiado larga (máximo " + MAX_CHARS_ESPECIALIDAD + " caracteres)", Alert.AlertType.ERROR);
            txtEspecialidad.requestFocus();
            return false;
        }

        // Validar campos obligatorios
        if (txtSintomas.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El campo SÍNTOMAS es obligatorio", Alert.AlertType.ERROR);
            txtSintomas.requestFocus();
            return false;
        }

        if (txtSignosVitales.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El campo SIGNOS VITALES es obligatorio", Alert.AlertType.ERROR);
            txtSignosVitales.requestFocus();
            return false;
        }

        if (txtDiagnostico.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El campo DIAGNÓSTICO es obligatorio", Alert.AlertType.ERROR);
            txtDiagnostico.requestFocus();
            return false;
        }

        if (txtIndicaciones.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El campo INDICACIONES es obligatorio", Alert.AlertType.ERROR);
            txtIndicaciones.requestFocus();
            return false;
        }

        return true;
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public void setModoEdicion(InterconsultaVO interconsulta) {
        try {
            log.debug(" CARGANDO INTERCONSULTA PARA EDICIÓN:");
            log.debug("   ID: {}", interconsulta.getIdInterconsulta());
            log.debug("   Estado: {}", interconsulta.getEstado());

            this.modoEdicion = true;
            this.interconsultaEnEdicion = interconsulta;
            this.idInterconsultaActual = interconsulta.getIdInterconsulta();
            this.numeroInterconsultaActual = interconsulta.getNumeroInterconsulta();

            obtenerFechaHoraOriginalInterconsulta(interconsulta.getIdInterconsulta());
            cmbEspecialistas.setValue(interconsulta.getEspecialista());
            cargarCamposSeparadosDesdeBD(interconsulta.getIdInterconsulta());
            actualizarCedula();
            btnGuardarDefinitivo.setText("Actualizar Interconsulta");
            esPrimeraGuardada = false;

            // ========== VERIFICACIÓN CORREGIDA ==========
            boolean esAdmin = "ADMIN".equals(rolUsuarioLogueado);
            boolean esJefatura = "JEFATURA_URGENCIAS".equals(rolUsuarioLogueado);

            // 1. ADMIN y JEFATURA siempre pueden editar
            if (esAdmin || esJefatura) {
                log.debug(" ADMIN/JEFATURA - PERMISOS TOTALES");
                btnGuardarDefinitivo.setDisable(false);
                btnGuardarTemporal.setDisable(false);
                return;
            }

            // 2. PARA ESPECIALISTAS NORMALES
            boolean esEspecialistaAutor = interconsulta.getEspecialista() != null &&
                    interconsulta.getEspecialista().equals(obtenerNombreMedicoDesdeUsuario(usuarioLogueado));
            boolean esInterconsultaTemporal = "TEMPORAL".equals(interconsulta.getEstado());
            boolean tienePermiso = interconsulta.isEditablePorMedico();

            boolean puedeEditar = (esEspecialistaAutor && esInterconsultaTemporal) ||
                    (esEspecialistaAutor && tienePermiso);

            log.debug(" VERIFICACIÓN (ESPECIALISTA NORMAL):");
            log.debug(" ¿Es Especialista Autor? {}", esEspecialistaAutor);
            log.debug(" ¿Es Temporal? {}", esInterconsultaTemporal);
            log.debug(" ¿Tiene Permiso? {}", tienePermiso);
            log.debug(" ¿PUEDE EDITAR? {}", puedeEditar);

            if (puedeEditar) {
                btnGuardarDefinitivo.setDisable(false);

                if (tienePermiso && !esInterconsultaTemporal) {
                    btnGuardarTemporal.setDisable(true);
                    btnGuardarTemporal.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666;");
                    btnGuardarTemporal.setTooltip(new Tooltip("No puede guardar como temporal con permiso de edición"));
                    log.debug("Permiso activado - Solo puede guardar como DEFINITIVA");
                } else {
                    btnGuardarTemporal.setDisable(false);
                }

                log.debug("BOTONES HABILITADOS");
            } else {
                btnGuardarDefinitivo.setDisable(true);
                btnGuardarTemporal.setDisable(true);
                log.debug(" BOTONES DESHABILITADOS");
            }

        } catch (Exception e) {
            log.error(" Error cargando interconsulta para edición: {}", e.getMessage());
            log.error(e);
        }
    }

    private void obtenerFechaHoraOriginalInterconsulta(int idInterconsulta) {
        String sql = "SELECT Fecha, Hora FROM tb_inter WHERE id_inter = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idInterconsulta);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                primeraFechaCreacion = rs.getDate("Fecha");
                primeraHoraCreacion = rs.getString("Hora");
                log.debug(" FECHA/HORA ORIGINAL DE INTERCONSULTA:");
                log.debug("   Fecha: {}", primeraFechaCreacion);
                log.debug("   Hora: {}", primeraHoraCreacion);
            }
        } catch (SQLException e) {
            log.error(" Error obteniendo fecha/hora original interconsulta: {}", e.getMessage());
        }
    }

    // MÉTODO PARA CARGAR CAMPOS SEPARADOS DESDE BD
    private void cargarCamposSeparadosDesdeBD(int idInterconsulta) {
        String sql = "SELECT sintomas, signos_vitales, diagnostico, especialidad, Nota FROM tb_inter WHERE id_inter = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idInterconsulta);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // CARGAR EN CAMPOS INDIVIDUALES
                txtSintomas.setText(rs.getString("sintomas"));
                txtSignosVitales.setText(rs.getString("signos_vitales"));
                txtDiagnostico.setText(rs.getString("diagnostico"));
                txtIndicaciones.setText(rs.getString("Nota")); // Nota = Indicaciones en interconsulta
                txtEspecialidad.setText(rs.getString("especialidad"));

                log.debug("Campos separados cargados correctamente");
            } else {
                log.warn("️ No se encontraron campos separados, usando campo combinado");
                cargarDesdeCampoCombinado(interconsultaEnEdicion.getContenido());
            }

        } catch (SQLException e) {
            log.error(" Error cargando campos separados: {}", e.getMessage());
            // Fallback: intentar cargar desde campo combinado
            cargarDesdeCampoCombinado(interconsultaEnEdicion.getContenido());
        }
    }

    // FALLBACK: Si no hay campos separados, cargar desde campo combinado
    private void cargarDesdeCampoCombinado(String contenidoCombinado) {
        if (contenidoCombinado != null) {
            // Lógica simple para parsear texto combinado
            try {
                if (contenidoCombinado.contains("ESPECIALIDAD:")) {
                    String[] partes = contenidoCombinado.split("SÍNTOMAS:");
                    if (partes.length > 0) {
                        String especialidad = partes[0].replace("ESPECIALIDAD:", "").trim();
                        txtEspecialidad.setText(especialidad);
                    }

                    if (partes.length > 1) {
                        String[] resto = partes[1].split("SIGNOS VITALES:");
                        if (resto.length > 0) {
                            txtSintomas.setText(resto[0].trim());
                        }

                        if (resto.length > 1) {
                            String[] finalPartes = resto[1].split("DIAGNÓSTICO:");
                            if (finalPartes.length > 0) {
                                txtSignosVitales.setText(finalPartes[0].trim());
                            }

                            if (finalPartes.length > 1) {
                                String[] indicacionesPartes = finalPartes[1].split("INDICACIONES:");
                                if (indicacionesPartes.length > 0) {
                                    txtDiagnostico.setText(indicacionesPartes[0].trim());
                                }

                                if (indicacionesPartes.length > 1) {
                                    txtIndicaciones.setText(indicacionesPartes[1].trim());
                                }
                            }
                        }
                    }
                }
                log.debug(" Campos cargados desde texto combinado");
            } catch (Exception e) {
                log.error(" Error parseando campo combinado: {}", e.getMessage());
                // Si falla, cargar todo en el campo de indicaciones
                txtIndicaciones.setText(contenidoCombinado);
            }
        }
    }

    private String obtenerNombreMedicoDesdeUsuario(String username) {
        String sql = "SELECT m.Med_nombre " +
                "FROM tb_medicos m " +
                "INNER JOIN tb_usuarios u ON m.Ced_prof = u.empleado_id " +
                "WHERE u.username = ? " +
                "UNION " +
                "SELECT nombre_completo FROM tb_usuarios WHERE username = ? " +
                "LIMIT 1";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("Med_nombre");
            }
        } catch (SQLException e) {
            log.error(" Error obteniendo nombre médico: {}", e.getMessage());
        }

        return username;
    }


    private void revocarPermisoInterconsultaDespuesDeUso() {
        if (interconsultaEnEdicion != null && interconsultaEnEdicion.isEditablePorMedico()) {
            boolean esAdmin = "ADMIN".equals(rolUsuarioLogueado);
            boolean esJefatura = "JEFATURA_URGENCIAS".equals(rolUsuarioLogueado);

            if (!esAdmin && !esJefatura) {
                // Usar conexión DIFERENTE en hilo separado
                new Thread(() -> {
                    try (Connection conn = ConexionBD.conectar()) {
                        conn.setAutoCommit(true); // No usar transacciones

                        String sql = "UPDATE tb_inter SET " +
                                "editable_por_medico = FALSE, " +
                                "fecha_edicion_realizada = NOW() " +
                                "WHERE id_inter = ?";

                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setInt(1, interconsultaEnEdicion.getIdInterconsulta());
                            pstmt.setQueryTimeout(5); // Timeout de 5 segundos

                            int filas = pstmt.executeUpdate();

                            if (filas > 0) {
                                log.info(" Permiso interconsulta revocado - ID: {}", interconsultaEnEdicion.getIdInterconsulta());
                            }
                        }
                    } catch (SQLException e) {
                        log.error("Error revocando permiso interconsulta: {}", e.getMessage());
                        // No bloquear la UI, solo log
                    }
                }).start();
            } else {
                log.info(" Admin/Jefatura - Permiso interconsulta NO se revoca");
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
            log.debug(" Historial interconsulta registrado - {}", accion);

        } catch (SQLException e) {
            log.error(" Error registrando en historial de interconsulta: {}", e.getMessage());
        }
    }

    public void limpiarRecursos() {
        // Limpiar referencias a objetos grandes
        txtSintomas.clear();
        txtSignosVitales.clear();
        txtDiagnostico.clear();
        txtIndicaciones.clear();
        txtEspecialidad.clear();

        // Limpiar listas
        cmbEspecialistas.getItems().clear();

        // Limpiar otras referencias
        interconsultaEnEdicion = null;
        datosCaptura = null;

        log.debug(" Recursos del controlador de interconsulta limpiados");
    }
}