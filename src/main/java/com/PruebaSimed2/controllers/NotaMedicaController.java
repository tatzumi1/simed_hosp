//src/main/java/com/PruebaSimed2/controllers/NotaMedicaController.java

package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.NotaMedicaVO;
import com.PruebaSimed2.utils.PDFGenerator;
import com.PruebaSimed2.utils.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

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

public class NotaMedicaController {

    @FXML private Label lblFolio;
    @FXML private ComboBox<String> cmbMedicos;
    @FXML private TextField txtCedula;
    @FXML private TextArea txtNota, txtIndicaciones;
    @FXML private TextArea txtSintomas, txtSignosVitales, txtDiagnostico;
    @FXML private Button btnGuardarDefinitivo;
    @FXML private Button btnGuardarTemporal;

    // Constantes para límites de datos
    private static final int MAX_CHARS_SINTOMAS = 3000;
    private static final int MAX_CHARS_SIGNOS = 2000;
    private static final int MAX_CHARS_DIAGNOSTICO = 9000;
    private static final int MAX_CHARS_NOTA = 11000;
    private static final int MAX_CHARS_INDICACIONES = 7000;
    private static final int MAX_TOTAL_CHARS = 33000; // Límite total para evitar saturación

    private int folioPaciente;
    private String usuarioLogueado;
    private String rolUsuarioLogueado;
    private boolean modoEdicion = false;
    private NotaMedicaVO notaEnEdicion;
    private Integer idNotaActual = null;
    private Integer numeroNotaActual = null;
    private SesionUsuario sesion = SesionUsuario.getInstance();

    // Variables para control de tiempo
    private Date primeraFechaCreacion = null;
    private String primeraHoraCreacion = null;
    private boolean esPrimeraGuardada = true;

    public static class DatosCaptura {
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

        // Getters
        public String getTipoUrgencia() { return tipoUrgencia; }
        public String getMotivoUrgencia() { return motivoUrgencia; }
        public String getTipoCama() { return tipoCama; }
        public String getMedico() { return medico; }
    }

    private DatosCaptura datosCaptura;

    public void setDatosCaptura(String tipoUrgencia, String motivoUrgencia, String tipoCama, String medico) {
        this.datosCaptura = new DatosCaptura(tipoUrgencia, motivoUrgencia, tipoCama, medico);
        System.out.println(" Datos de captura recibidos en NotaMedicaController:");
        System.out.println("   - Tipo Urgencia: " + tipoUrgencia);
        System.out.println("   - Motivo Urgencia: " + motivoUrgencia);
        System.out.println("   - Tipo Cama: " + tipoCama);
        System.out.println("   - Médico: " + medico);
    }

    public void setFolioPaciente(int folio) {
        this.folioPaciente = folio;
        lblFolio.setText("Folio: " + folio);
        cargarMedicos();
        cargarNotaExistente();
    }

    public void setUsuarioLogueado(String usuario, String rol) {
        this.usuarioLogueado = usuario;
        this.rolUsuarioLogueado = rol;
        System.out.println(" Usuario en nota médica: " + usuario + " - Rol: " + rol);
    }

    @FXML
    public void initialize() {
        cmbMedicos.setOnAction(e -> actualizarCedula());

        // Configurar límites en los TextArea para prevenir saturación de RAM
        configurarLimitesTextAreas();
    }


    private void configurarLimitesTextAreas() {
        // Configurar listeners para limitar caracteres
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

        txtNota.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_CHARS_NOTA) {
                txtNota.setText(oldValue);
                mostrarAlerta("Límite excedido",
                        "El campo Nota Médica no puede exceder los " + MAX_CHARS_NOTA + " caracteres",
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

    private void cargarMedicos() {
        try (Connection conn = ConexionBD.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Med_nombre, Ced_prof FROM tb_medicos ORDER BY Med_nombre")) {

            cmbMedicos.getItems().clear();
            while (rs.next()) {
                cmbMedicos.getItems().add(rs.getString("Med_nombre"));
            }
        } catch (SQLException e) {
            System.err.println(" Error cargando médicos: " + e.getMessage());
        }
    }

    private void cargarNotaExistente() {
        String sql = "SELECT id_nota, Num_nota, Nota, Indicaciones, sintomas, signos_vitales, diagnostico, " +
                "Medico, Cedula, Fecha, Hora, Estado FROM tb_notas " +
                "WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ? ORDER BY id_nota DESC LIMIT 1";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folioPaciente);
            pstmt.setString(2, sesion.getNombreMedico());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                idNotaActual = rs.getInt("id_nota");
                numeroNotaActual = rs.getInt("Num_nota");

                // OBTENER FECHA Y HORA DE LA PRIMERA CREACIÓN
                primeraFechaCreacion = rs.getDate("Fecha");
                primeraHoraCreacion = rs.getString("Hora");
                esPrimeraGuardada = false; // Ya existe una nota, no es la primera

                System.out.println(" FECHA/HORA DE CREACIÓN ORIGINAL:");
                System.out.println("   Fecha: " + primeraFechaCreacion);
                System.out.println("   Hora: " + primeraHoraCreacion);

                // CARGAR CAMPOS SEPARADOS
                txtSintomas.setText(rs.getString("sintomas"));
                txtSignosVitales.setText(rs.getString("signos_vitales"));
                txtDiagnostico.setText(rs.getString("diagnostico"));
                txtNota.setText(rs.getString("Nota"));
                txtIndicaciones.setText(rs.getString("Indicaciones"));

                cmbMedicos.setValue(rs.getString("Medico"));
                actualizarCedula();

                System.out.println(" Nota temporal existente cargada - ID: " + idNotaActual);
            }
        } catch (SQLException e) {
            System.err.println(" Error cargando nota existente: " + e.getMessage());
        }
    }

    private void actualizarCedula() {
        String medicoSeleccionado = cmbMedicos.getValue();
        if (medicoSeleccionado != null) {
            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT Ced_prof FROM tb_medicos WHERE Med_nombre = ?")) {

                pstmt.setString(1, medicoSeleccionado);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    txtCedula.setText(rs.getString("Ced_prof"));
                }
            } catch (SQLException e) {
                System.err.println(" Error obteniendo cédula: " + e.getMessage());
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

            // OBTENER CONTENIDO ACTUAL DE TODOS LOS CAMPOS
            String notaActual = txtNota.getText().trim();
            String indicacionesActual = txtIndicaciones.getText().trim();
            String sintomasActual = txtSintomas.getText().trim();
            String signosVitalesActual = txtSignosVitales.getText().trim();
            String diagnosticoActual = txtDiagnostico.getText().trim();
            String medicoActual = cmbMedicos.getValue();
            String cedulaActual = txtCedula.getText();

            System.out.println(" INICIANDO GUARDADO TEMPORAL ===================");
            System.out.println(" Nota: " + notaActual.length() + " chars");
            System.out.println(" Indicaciones: " + indicacionesActual.length() + " chars");
            System.out.println(" Síntomas: " + sintomasActual.length() + " chars");
            System.out.println(" Signos Vitales: " + signosVitalesActual.length() + " chars");
            System.out.println(" Diagnóstico: " + diagnosticoActual.length() + " chars");

            int filasAfectadas = 0;

            if (idNotaActual == null || esPrimeraGuardada) {
                // NUEVA NOTA TEMPORAL o PRIMERA GUARDADA
                numeroNotaActual = obtenerSiguienteNumNota(conn);

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

                String sql = "INSERT INTO tb_notas (Folio, Num_nota, Nota, Indicaciones, sintomas, signos_vitales, " +
                        "diagnostico, Medico, Cedula, Fecha, Hora, Estado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, " + fechaSQL + ", " + horaSQL + ", 'TEMPORAL')";

                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, folioPaciente);
                    pstmt.setInt(2, numeroNotaActual);
                    pstmt.setString(3, notaActual);
                    pstmt.setString(4, indicacionesActual);
                    pstmt.setString(5, sintomasActual);
                    pstmt.setString(6, signosVitalesActual);
                    pstmt.setString(7, diagnosticoActual);
                    pstmt.setString(8, medicoActual);
                    pstmt.setString(9, cedulaActual);

                    // Si es primera guardada con fecha/hora existente, usar esas
                    if (esPrimeraGuardada && primeraFechaCreacion != null && primeraHoraCreacion != null) {
                        pstmt.setDate(10, new java.sql.Date(primeraFechaCreacion.getTime()));
                        pstmt.setString(11, primeraHoraCreacion);
                        System.out.println("Usando fecha/hora original de primera creación");
                    }

                    filasAfectadas = pstmt.executeUpdate();

                    if (filasAfectadas > 0) {
                        ResultSet rs = pstmt.getGeneratedKeys();
                        if (rs.next()) {
                            idNotaActual = rs.getInt(1);
                            System.out.println(" NUEVA NOTA TEMPORAL CREADA - ID: " + idNotaActual);

                            // Si es primera guardada, marcar como no primera para próximas
                            if (esPrimeraGuardada) {
                                esPrimeraGuardada = false;
                            }

                            // OBTENER Y GUARDAR LA FECHA/HORA DE CREACIÓN
                            guardarFechaHoraCreacion(conn, idNotaActual);
                        }
                    }
                }
            } else {
                // ACTUALIZAR NOTA TEMPORAL EXISTENTE - NO MODIFICAR FECHA/HORA
                String sql = "UPDATE tb_notas SET Nota = ?, Indicaciones = ?, sintomas = ?, signos_vitales = ?, " +
                        "diagnostico = ?, Medico = ?, Cedula = ?, Estado = 'TEMPORAL' " +
                        "WHERE id_nota = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, notaActual);
                    pstmt.setString(2, indicacionesActual);
                    pstmt.setString(3, sintomasActual);
                    pstmt.setString(4, signosVitalesActual);
                    pstmt.setString(5, diagnosticoActual);
                    pstmt.setString(6, medicoActual);
                    pstmt.setString(7, cedulaActual);
                    pstmt.setInt(8, idNotaActual);
                    // IMPORTANTE: NO actualizamos Fecha ni Hora

                    filasAfectadas = pstmt.executeUpdate();
                    System.out.println(" NOTA TEMPORAL ACTUALIZADA - ID: " + idNotaActual);
                    System.out.println(" Fecha y hora ORIGINALES preservadas (no se modifican)");
                }
            }

            if (filasAfectadas > 0) {
                //  ACTUALIZAR ESTADO DEL PACIENTE A OBSERVACIÓN
                actualizarEstadoPacienteObservacion(conn);

                conn.commit();
                operacionExitosa = true;
                mostrarAlerta("Éxito", "Nota guardada temporalmente\nPaciente en OBSERVACIÓN", Alert.AlertType.INFORMATION);
            } else {
                conn.rollback();
                mostrarAlerta("Error", "No se pudo guardar la nota", Alert.AlertType.ERROR);
            }

        } catch (SQLException e) {
            System.err.println(" ERROR EN TRANSACCIÓN: " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println(" ERROR AL REVERTIR: " + rollbackEx.getMessage());
            }
            mostrarAlerta("Error de Base de Datos", "Error al guardar: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println(" Error cerrando conexión: " + e.getMessage());
            }
        }
    }

    private void guardarFechaHoraCreacion(Connection conn, int idNota) {
        String sql = "SELECT Fecha, Hora FROM tb_notas WHERE id_nota = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idNota);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                primeraFechaCreacion = rs.getDate("Fecha");
                primeraHoraCreacion = rs.getString("Hora");
                System.out.println(" FECHA/HORA DE CREACIÓN GUARDADA:");
                System.out.println("   Fecha: " + primeraFechaCreacion);
                System.out.println("   Hora: " + primeraHoraCreacion);
            }
        } catch (SQLException e) {
            System.err.println(" Error guardando fecha/hora creación: " + e.getMessage());
        }
    }

    @FXML
    private void guardarDefinitivo() {
        if (!validarCampos()) return;
        if (!verificarLimitesDatos()) return;

        // Deshabilitar botones
        btnGuardarDefinitivo.setDisable(true);
        btnGuardarTemporal.setDisable(true);

        // VARIABLES FINALES
        final Integer numNotaFinal = numeroNotaActual;
        final String notaFinal = txtNota.getText();
        final String indicacionesFinal = txtIndicaciones.getText();
        final String sintomasFinal = txtSintomas.getText();
        final String signosFinal = txtSignosVitales.getText();
        final String diagnosticoFinal = txtDiagnostico.getText();
        final String medicoFinal = cmbMedicos.getValue();

        // OBTENER FECHA/HORA ORIGINAL
        final String[] fechaHoraOriginal = new String[2];
        if (primeraFechaCreacion != null) {
            fechaHoraOriginal[0] = new SimpleDateFormat("yyyy-MM-dd").format(primeraFechaCreacion);
            fechaHoraOriginal[1] = primeraHoraCreacion;
        }

        new Thread(() -> {
            Connection conn = null;
            boolean exito = false;

            try {
                conn = ConexionBD.conectar();
                conn.setAutoCommit(false);

                if (idNotaActual == null) {
                    Platform.runLater(() -> guardarTemporal());
                    return;
                }

                String sqlUpdate = "UPDATE tb_notas SET Nota = ?, Indicaciones = ?, sintomas = ?, " +
                        "signos_vitales = ?, diagnostico = ?, Medico = ?, Cedula = ?, Estado = 'DEFINITIVA' " +
                        "WHERE id_nota = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                    pstmt.setString(1, notaFinal);
                    pstmt.setString(2, indicacionesFinal);
                    pstmt.setString(3, sintomasFinal);
                    pstmt.setString(4, signosFinal);
                    pstmt.setString(5, diagnosticoFinal);
                    pstmt.setString(6, medicoFinal);
                    pstmt.setString(7, txtCedula.getText());
                    pstmt.setInt(8, idNotaActual);

                    int filas = pstmt.executeUpdate();

                    if (filas > 0) {
                        if (modoEdicion && notaEnEdicion != null && notaEnEdicion.isEditablePorMedico()) {
                            revocarPermisoDespuesDeUso();
                        }

                        conn.commit();
                        exito = true;

                        Platform.runLater(() -> {
                            mostrarAlerta("Éxito", "Nota #" + numNotaFinal + " guardada DEFINITIVAMENTE",
                                    Alert.AlertType.INFORMATION);

                            // PDF CON FECHA/HORA ORIGINAL
                            generarPDFAutomatico(sintomasFinal, signosFinal, diagnosticoFinal,
                                    notaFinal, indicacionesFinal, medicoFinal,
                                    fechaHoraOriginal[0], fechaHoraOriginal[1]);
                        });
                    } else {
                        conn.rollback();
                        Platform.runLater(() ->
                                mostrarAlerta("Error", "No se pudo guardar", Alert.AlertType.ERROR));
                    }
                }

            } catch (SQLException e) {
                System.err.println("Error: " + e.getMessage());
                try {
                    if (conn != null) conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Rollback error: " + ex.getMessage());
                }
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException e) {
                    System.err.println(" Error cerrando: " + e.getMessage());
                }
            }
        }).start();
    }

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
                System.out.println(" PACIENTE ACTUALIZADO A OBSERVACIÓN - Folio: " + folioPaciente + " (ID Estado: " + idObservacion + ")");
            } else {
                System.out.println(" Paciente ya estaba en OBSERVACIÓN - Folio: " + folioPaciente);
            }

            pstmt.close();

        } catch (SQLException e) {
            System.err.println(" Error actualizando estado a observación: " + e.getMessage());
        }
    }

    private void generarPDFAutomatico(String sintomas, String signosVitales, String diagnostico,
                                      String nota, String indicaciones, String medico,
                                      String fecha, String hora) {
        try {
            System.out.println(" GENERANDO PDF AUTOMÁTICO:");
            System.out.println("   Fecha BD: " + fecha);
            System.out.println("   Hora BD: " + hora);
            System.out.println("   Folio: " + folioPaciente);

            //  LLAMAR AL PDFGenerator NUEVO (solo necesita folio y número de nota)
            boolean exito = PDFGenerator.generarNotaMedicaPDF(
                    folioPaciente,                                 // int folioPaciente
                    numeroNotaActual != null ? numeroNotaActual : 1 // int numeroNota
            );

            if (exito) {
                System.out.println(" PDF generado automáticamente con datos completos");
                abrirPDFReciente();

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
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.err.println(" Error generando PDF automático");
            }

        } catch (Exception e) {
            System.err.println(" Error generando PDF automático: " + e.getMessage());
            e.printStackTrace();
        }
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
                }
            }
        } catch (Exception e) {
            System.err.println(" Error abriendo PDF: " + e.getMessage());
        }
    }

    private int obtenerSiguienteNumNota(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(Num_nota), 0) + 1 as siguiente_num FROM tb_notas WHERE Folio = ?";
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
                // Nombre completo con apellidos
                String nombreCompleto = construirNombreCompleto(
                        rs.getString("A_paterno"),
                        rs.getString("A_materno"),
                        rs.getString("Nombre")
                );

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

                // Depuración
                System.out.println(" DATOS PACIENTE OBTENIDOS:");
                System.out.println("   Nombre: " + nombreCompleto);
                System.out.println("   CURP: " + datos.get("curp"));
                System.out.println("   Teléfono: " + datos.get("telefono"));
            }
        } catch (SQLException e) {
            System.err.println(" Error obteniendo datos paciente: " + e.getMessage());
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

    private boolean verificarLimitesDatos() {
        int totalChars = 0;
        List<String> camposExcedidos = new ArrayList<>();

        // Verificar límites individuales
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

        if (txtNota.getText().length() > MAX_CHARS_NOTA) {
            camposExcedidos.add("Nota Médica (" + txtNota.getText().length() + "/" + MAX_CHARS_NOTA + ")");
        }
        totalChars += txtNota.getText().length();

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
        // Validar médico
        if (cmbMedicos.getValue() == null) {
            mostrarAlerta("Error", "Seleccione un médico", Alert.AlertType.ERROR);
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

        if (txtNota.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "El campo NOTA MÉDICA es obligatorio", Alert.AlertType.ERROR);
            txtNota.requestFocus();
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

    public void setModoEdicion(NotaMedicaVO nota) {
        try {
            this.modoEdicion = true;
            this.notaEnEdicion = nota;
            this.idNotaActual = nota.getIdNota();
            this.numeroNotaActual = nota.getNumeroNota();

            // OBTENER FECHA/HORA ORIGINAL DE LA NOTA
            obtenerFechaHoraOriginalNota(nota.getIdNota());

            System.out.println(" CARGANDO NOTA PARA EDICIÓN:");
            System.out.println("   ID: " + nota.getIdNota());
            System.out.println("   Fecha Original: " + primeraFechaCreacion);
            System.out.println("   Hora Original: " + primeraHoraCreacion);
            System.out.println("   Médico Autor BD: " + nota.getMedicoAutor());
            System.out.println("   Nombre Sesión: " + sesion.getNombreMedico());
            System.out.println("   Estado: " + nota.getEstado());
            System.out.println("   ¿Editable? " + nota.isEditablePorMedico());

            // CARGAR MÉDICO
            cmbMedicos.setValue(nota.getMedicoAutor());

            // CARGAR CAMPOS SEPARADOS DESDE BD
            cargarCamposSeparadosDesdeBD(nota.getIdNota());

            actualizarCedula();
            btnGuardarDefinitivo.setText("Actualizar Nota");

            // IMPORTANTE: Marcar que NO es primera guardada
            esPrimeraGuardada = false;

            // ========== VERIFICACIÓN CORREGIDA ==========
            boolean esAdmin = "ADMIN".equals(rolUsuarioLogueado);
            boolean esJefatura = "JEFATURA_URGENCIAS".equals(rolUsuarioLogueado);

            // 1. ADMIN y JEFATURA siempre pueden editar CUALQUIER nota
            if (esAdmin || esJefatura) {
                System.out.println(" ✅ ADMIN/JEFATURA - PERMISOS TOTALES");
                btnGuardarDefinitivo.setDisable(false);
                btnGuardarTemporal.setDisable(false);
                return; // Salir, no necesita más verificaciones
            }

            // 2. PARA MÉDICOS NORMALES
            boolean esMedicoAutor = nota.getMedicoAutor() != null &&
                    nota.getMedicoAutor().equals(sesion.getNombreMedico());
            boolean esNotaTemporal = "TEMPORAL".equals(nota.getEstado());
            boolean tienePermiso = nota.isEditablePorMedico();

            boolean puedeEditar = (esMedicoAutor && esNotaTemporal) ||
                    (esMedicoAutor && tienePermiso);

            System.out.println(" 📋 VERIFICACIÓN (MÉDICO NORMAL):");
            System.out.println("   ¿Es Médico Autor? " + esMedicoAutor);
            System.out.println("   ¿Es Nota Temporal? " + esNotaTemporal);
            System.out.println("   ¿Tiene Permiso? " + tienePermiso);
            System.out.println("   ¿PUEDE EDITAR? " + puedeEditar);

            if (puedeEditar) {
                btnGuardarDefinitivo.setDisable(false);

                //  Si usa permiso, NO puede guardar como temporal
                if (tienePermiso && !esNotaTemporal) {
                    btnGuardarTemporal.setDisable(true);
                    btnGuardarTemporal.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666;");
                    btnGuardarTemporal.setTooltip(new Tooltip("No puede guardar como temporal con permiso de edición"));
                    System.out.println(" Permiso de edición activado - Solo puede guardar como DEFINITIVA");
                } else {
                    btnGuardarTemporal.setDisable(false);
                }

                System.out.println("  BOTONES HABILITADOS");
            } else {
                btnGuardarDefinitivo.setDisable(true);
                btnGuardarTemporal.setDisable(true);
                System.out.println("  BOTONES DESHABILITADOS - SIN PERMISOS");

                mostrarAlerta("Sin permisos",
                        "No tiene permisos para editar esta nota.\n\n" +
                                "Solo el médico autor puede modificar sus notas TEMPORALES.\n" +
                                "Para notas DEFINITIVAS necesita permiso de edición.",
                        Alert.AlertType.WARNING);
            }

        } catch (Exception e) {
            System.err.println(" Error cargando nota para edición: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void obtenerFechaHoraOriginalNota(int idNota) {
        String sql = "SELECT Fecha, Hora FROM tb_notas WHERE id_nota = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idNota);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                primeraFechaCreacion = rs.getDate("Fecha");
                primeraHoraCreacion = rs.getString("Hora");
                System.out.println(" FECHA/HORA ORIGINAL DE NOTA:");
                System.out.println("   Fecha: " + primeraFechaCreacion);
                System.out.println("   Hora: " + primeraHoraCreacion);
            }
        } catch (SQLException e) {
            System.err.println("  Error obteniendo fecha/hora original: " + e.getMessage());
        }
    }

    //  MÉTODO PARA CARGAR CAMPOS SEPARADOS DESDE BD
    private void cargarCamposSeparadosDesdeBD(int idNota) {
        String sql = "SELECT sintomas, signos_vitales, diagnostico, Nota, Indicaciones FROM tb_notas WHERE id_nota = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idNota);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                //  CARGAR EN CAMPOS INDIVIDUALES
                txtSintomas.setText(rs.getString("sintomas"));
                txtSignosVitales.setText(rs.getString("signos_vitales"));
                txtDiagnostico.setText(rs.getString("diagnostico"));
                txtNota.setText(rs.getString("Nota"));
                txtIndicaciones.setText(rs.getString("Indicaciones"));

                System.out.println(" Campos separados cargados correctamente");
            } else {
                System.out.println("No se encontraron campos separados, usando campo combinado");
                cargarDesdeCampoCombinado(notaEnEdicion.getContenido());
            }

        } catch (SQLException e) {
            System.err.println(" Error cargando campos separados: " + e.getMessage());
            // Fallback: intentar cargar desde campo combinado
            cargarDesdeCampoCombinado(notaEnEdicion.getContenido());
        }
    }

    //  FALLBACK: Si no hay campos separados, cargar desde campo combinado
    private void cargarDesdeCampoCombinado(String contenidoCombinado) {
        if (contenidoCombinado != null) {
            // Lógica simple para parsear texto combinado
            try {
                if (contenidoCombinado.contains("SÍNTOMAS:")) {
                    String[] partes = contenidoCombinado.split("SIGNOS VITALES:");
                    if (partes.length > 0) {
                        String sintomas = partes[0].replace("SÍNTOMAS:", "").trim();
                        txtSintomas.setText(sintomas);
                    }

                    if (partes.length > 1) {
                        String[] resto = partes[1].split("DIAGNÓSTICO:");
                        if (resto.length > 0) {
                            txtSignosVitales.setText(resto[0].trim());
                        }

                        if (resto.length > 1) {
                            String[] finalPartes = resto[1].split("NOTA MÉDICA:");
                            if (finalPartes.length > 0) {
                                txtDiagnostico.setText(finalPartes[0].trim());
                            }

                            if (finalPartes.length > 1) {
                                String[] notaPartes = finalPartes[1].split("INDICACIONES:");
                                if (notaPartes.length > 0) {
                                    txtNota.setText(notaPartes[0].trim());
                                }

                                if (notaPartes.length > 1) {
                                    txtIndicaciones.setText(notaPartes[1].trim());
                                }
                            }
                        }
                    }
                }
                System.out.println(" Campos cargados desde texto combinado");
            } catch (Exception e) {
                System.err.println(" Error parseando campo combinado: " + e.getMessage());
                // Si falla, cargar todo en el campo de nota
                txtNota.setText(contenidoCombinado);
            }
        }
    }

    private void revocarPermisoDespuesDeUso() {
        if (notaEnEdicion != null && notaEnEdicion.isEditablePorMedico()) {
            boolean esAdmin = "ADMIN".equals(rolUsuarioLogueado);
            boolean esJefatura = "JEFATURA_URGENCIAS".equals(rolUsuarioLogueado);

            if (!esAdmin && !esJefatura) {
                // Usar conexión DIFERENTE para evitar bloqueos
                new Thread(() -> {
                    try (Connection conn = ConexionBD.conectar()) {
                        // Configurar timeout corto
                        conn.setAutoCommit(true); // No usar transacciones para esto

                        String sql = "UPDATE tb_notas SET " +
                                "editable_por_medico = FALSE, " +
                                "fecha_edicion_realizada = NOW() " +
                                "WHERE id_nota = ?";

                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setInt(1, notaEnEdicion.getIdNota());
                            pstmt.setQueryTimeout(5); // Timeout de 5 segundos

                            int filas = pstmt.executeUpdate();

                            if (filas > 0) {
                                System.out.println("✅ Permiso revocado después del uso - ID Nota: " + notaEnEdicion.getIdNota());
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("⚠️ Error revocando permiso (seguirá funcionando): " + e.getMessage());
                        // No mostrar error al usuario, solo log
                    }
                }).start();
            } else {
                System.out.println(" Admin/Jefatura - Permiso NO se revoca");
            }
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

    public void limpiarRecursos() {
        // Limpiar referencias a objetos grandes
        txtSintomas.clear();
        txtSignosVitales.clear();
        txtDiagnostico.clear();
        txtNota.clear();
        txtIndicaciones.clear();

        // Limpiar listas
        cmbMedicos.getItems().clear();

        // Limpiar otras referencias
        notaEnEdicion = null;
        datosCaptura = null;

        System.out.println(" Recursos del controlador limpiados");
    }
}