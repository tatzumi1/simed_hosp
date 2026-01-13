package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.Edad;
import com.PruebaSimed2.utils.SesionUsuario;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Log4j2
public class VentanaRegistroTriageController {

    // === CAMPOS FXML ===
    @FXML
    private DatePicker dpFechaNac;
    @FXML
    private TextField txtFecha, txtHora, txtApPaterno, txtApMaterno, txtNombre, txtEdad, txtTelefono;
    @FXML
    private TextField txtDomicilio, txtNoAfiliacion, txtReferencia, txtExpediente, txtCURP;
    @FXML
    private TextField txtMunicipioSel, txtEntidadSel, txtOcupacion, txtReligion, txtEstadoCivil;
    @FXML
    private TextArea txtSintomas, txtObservaciones;
    @FXML
    private ComboBox<String> comboSexo, comboDerechohab, comboMedico, comboTriage;
    @FXML
    private Button btnRegistrar, btnSalir, btnMunicipio, btnEntidad, btnTriageColor;
    @FXML
    private CheckBox chkReingreso, chkHospitalizado;
    @FXML
    private Label lblCapturista, lblTurno, lblClave, lblTriageDescripcion;

    // === VARIABLES ===
    private String turno;
    private final Map<String, Integer> mapaDerechohab = new HashMap<>();

    // === LÍMITES DE DATOS ===
    private static final int LIMITE_NOMBRE = 100;
    private static final int LIMITE_TEXTO_CORTO = 50;
    private static final int LIMITE_TEXTO_LARGO = 255;
    private static final int LIMITE_TELEFONO = 15;
    private static final int LIMITE_CURP = 18;
    private static final int LIMITE_TEXTO_AREA = 2000;

    // === MÉTODOS PÚBLICOS ===
    public void setTurno(String turno) {
        if (turno != null && !turno.trim().isEmpty()) {
            this.turno = turno;
            lblTurno.setText(turno);
        }
    }

    @FXML
    public void initialize() {
        configurarUI();
        cargarCombos();
        configurarEventos();
        configurarValidaciones();
        configurarSesion();
        configurarLimitesDatos();
    }

    // === CONFIGURAR LÍMITES ===
    private void configurarLimitesDatos() {
        // TextFields
        configurarLimiteTextField(txtApPaterno, LIMITE_NOMBRE);
        configurarLimiteTextField(txtApMaterno, LIMITE_NOMBRE);
        configurarLimiteTextField(txtNombre, LIMITE_NOMBRE);
        configurarLimiteTextField(txtOcupacion, LIMITE_TEXTO_CORTO);
        configurarLimiteTextField(txtReligion, LIMITE_TEXTO_CORTO);
        configurarLimiteTextField(txtEstadoCivil, LIMITE_TEXTO_CORTO);
        configurarLimiteTextField(txtDomicilio, LIMITE_TEXTO_LARGO);
        configurarLimiteTextField(txtReferencia, LIMITE_TEXTO_LARGO);
        configurarLimiteTextField(txtTelefono, LIMITE_TELEFONO);
        configurarLimiteTextField(txtNoAfiliacion, 20);
        configurarLimiteTextField(txtCURP, LIMITE_CURP);
        configurarLimiteTextField(txtExpediente, 50);

        // Edad solo números
        txtEdad.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d*") && c.getControlNewText().length() <= 3 ? c : null));

        // TextAreas
        configurarLimiteTextArea(txtSintomas);
        configurarLimiteTextArea(txtObservaciones);
    }

    private void configurarLimiteTextField(TextField campo, int limite) {
        campo.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null && nuevo.length() > limite) {
                campo.setText(viejo);
            }
        });
    }

    private void configurarLimiteTextArea(TextArea area) {
        area.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null && nuevo.length() > VentanaRegistroTriageController.LIMITE_TEXTO_AREA) {
                area.setText(viejo);
            }
        });
    }

    // === SETUP INICIAL ===
    private void configurarUI() {
        comboSexo.getItems().addAll("Masculino", "Femenino");
        comboTriage.setPromptText("Seleccione nivel");
        comboDerechohab.setPromptText("Seleccione derechohabiencia");
        comboMedico.setPromptText("Seleccione médico");

        iniciarReloj();
        btnMunicipio.setDisable(true);
    }

    private void configurarSesion() {
        SesionUsuario sesion = SesionUsuario.getInstance();
        String nombre = sesion.getNombreMedico();
        if (nombre != null) {
            lblCapturista.setText(nombre);
            lblClave.setText(sesion.getUsername());
            if (turno == null) {
                turno = obtenerTurnoActual();
                lblTurno.setText(turno);
            }
        }
    }

    private String obtenerTurnoActual() {
        int hora = LocalDateTime.now().getHour();
        if (hora >= 7 && hora < 15) return "Matutino";
        else if (hora >= 15 && hora < 23) return "Vespertino";
        else return "Nocturno";
    }

    private void iniciarReloj() {
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime ahora = LocalDateTime.now();
            txtFecha.setText(ahora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            txtHora.setText(ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }

    // === CARGAR DATOS ===
    private void cargarCombos() {
        cargarDerechohabiencia();
        cargarMedicos();
        comboTriage.getItems().addAll("Verde", "Amarillo", "Rojo");
    }

    private void cargarDerechohabiencia() {
        try (Connection c = ConexionBD.conectar();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT Cve_dh, Derechohabiencia FROM tblt_cvedhabiencia")) {

            while (rs.next()) {
                String desc = rs.getString("Derechohabiencia");
                mapaDerechohab.put(desc, rs.getInt("Cve_dh"));
                comboDerechohab.getItems().add(desc);
            }
        } catch (SQLException e) {
            log.error("Error al cargar derechohab", e);
        }
    }

    private void cargarMedicos() {
        try (Connection c = ConexionBD.conectar();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT Med_nombre FROM tb_medicos")) {

            while (rs.next()) {
                comboMedico.getItems().add(rs.getString("Med_nombre"));
            }
        } catch (SQLException e) {
            log.error("Error al cargar medicos", e);
        }
    }

    // === EVENTOS ===
    private void configurarEventos() {
        comboTriage.setOnAction(e -> actualizarColorTriage());
        btnRegistrar.setOnAction(e -> registrarPaciente());
        btnSalir.setOnAction(e -> cerrarVentana());
        btnMunicipio.setOnAction(e -> seleccionarMunicipio());
        btnEntidad.setOnAction(e -> seleccionarEntidad());
    }

    private void configurarValidaciones() {
        Runnable validar = () -> btnRegistrar.setDisable(
                txtApPaterno.getText().trim().isEmpty() ||
                        txtNombre.getText().trim().isEmpty() ||
                        comboTriage.getValue() == null
        );
        txtApPaterno.textProperty().addListener((o, v, n) -> validar.run());
        txtNombre.textProperty().addListener((o, v, n) -> validar.run());
        comboTriage.valueProperty().addListener((o, v, n) -> validar.run());
    }

    private void actualizarColorTriage() {
        String triage = comboTriage.getValue();
        if (triage == null) return;

        Map<String, String[]> colores = Map.of(
                "verde", new String[]{"#2ecc71", "Urgencia menor - Paciente estable"},
                "amarillo", new String[]{"#f1c40f", "Urgencia - Atención prioritaria"},
                "rojo", new String[]{"#e74c3c", "Emergencia - Riesgo vital inmediato"}
        );

        String[] config = colores.get(triage.toLowerCase());
        if (config != null) {
            btnTriageColor.setStyle("-fx-background-color:" + config[0] + "; -fx-background-radius:10;");
            lblTriageDescripcion.setText(config[1]);
            lblTriageDescripcion.setStyle("-fx-text-fill:" + config[0] + ";");
        }
    }

    // === REGISTRO PACIENTE COMPLETO ===
    @FXML
    private void registrarPaciente() {
        String nombreMedico = SesionUsuario.getInstance().getNombreMedico();
        String username = SesionUsuario.getInstance().getUsername();

        if (nombreMedico == null) {
            mostrarAlerta("Sesión inválida", "Reinicie sesión", Alert.AlertType.ERROR);
            return;
        }

        try (Connection conn = ConexionBD.conectar()) {
            conn.setAutoCommit(false);
            int folio = obtenerSiguienteFolio(conn);

            if (folio > 0 && insertarPacienteCompleto(conn, folio, nombreMedico)) {
                conn.commit();
                registrarAuditoria(folio, username);
                mostrarAlerta("Éxito", "Paciente registrado\nFolio: " + folio, Alert.AlertType.INFORMATION);
                limpiarCampos();
            } else {
                conn.rollback();
                mostrarAlerta("Error", "No se pudo registrar", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            mostrarAlerta("Error BD", e.getMessage(), Alert.AlertType.ERROR);
            log.error("Error al registrar paciente", e);
        }
    }

    private int obtenerSiguienteFolio(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(Folio) FROM tb_urgencias")) {
            return rs.next() ? rs.getInt(1) + 1 : 1000;
        }
    }

    private boolean insertarPacienteCompleto(Connection conn, int folio, String nombreMedico) throws SQLException {
        String sql = "INSERT INTO tb_urgencias (" +
                "Folio, A_paterno, A_materno, Nombre, Edad, F_nac, Telefono, Domicilio, " +
                "Derechohabiencia, No_afiliacion, Referencia, Reingreso, Hospitalizado, " +
                "Exp_clinico, CURP, Sintomas, Nom_med, TRIAGE, Nombre_ts, Turno, " +
                "Fecha, Hora_registro, Estado_pac, Sexo, Municipio_resid, Entidad_resid, " +
                "Ocupacion, Religion, Edo_civil, Observaciones_ts, Municipio_completo, Entidad_completa" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "CURDATE(), ?, 1, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            LocalDate fechaNac = dpFechaNac.getValue();
            Edad edad = new Edad();
            edad.calcularEdad(fechaNac);
            if (fechaNac == null) {
                log.warn("Fecha nacimiento no especificada");
                return false;
            }
            ps.setInt(i++, folio);
            ps.setString(i++, txtApPaterno.getText().trim());
            ps.setString(i++, txtApMaterno.getText().trim());
            ps.setString(i++, txtNombre.getText().trim());
            ps.setInt(i++, edad.getAnos());
            ps.setDate(i++, Date.valueOf(fechaNac));
            ps.setString(i++, txtTelefono.getText().trim());
            ps.setString(i++, txtDomicilio.getText().trim());

            Integer cveDerechoHabiente = obtenerCodigoDerechoHabiente();
            if (cveDerechoHabiente != null) ps.setInt(i++, cveDerechoHabiente);
            else ps.setNull(i++, Types.INTEGER);
            ps.setString(i++, txtNoAfiliacion.getText().trim());
            ps.setString(i++, txtReferencia.getText().trim());
            ps.setBoolean(i++, chkReingreso.isSelected());
            ps.setBoolean(i++, chkHospitalizado.isSelected());
            ps.setString(i++, txtExpediente.getText().trim());
            ps.setString(i++, txtCURP.getText().trim());
            ps.setString(i++, txtSintomas.getText().trim());
            ps.setString(i++, comboMedico.getValue());
            ps.setString(i++, comboTriage.getValue());
            ps.setString(i++, nombreMedico);
            ps.setString(i++, turno);
            ps.setString(i++, txtHora.getText().trim());
            Integer codigoSexo = obtenerCodigoSexo();
            if (codigoSexo != null) ps.setInt(i++, codigoSexo);
            else ps.setNull(i++, Types.INTEGER);
            ps.setString(i++, obtenerCodigoMunicipio());
            ps.setString(i++, obtenerCodigoEntidad(txtEntidadSel.getText().trim()));
            ps.setString(i++, txtOcupacion.getText().trim());
            ps.setString(i++, txtReligion.getText().trim());
            ps.setString(i++, txtEstadoCivil.getText().trim());
            ps.setString(i++, txtObservaciones.getText().trim());
            ps.setString(i++, txtMunicipioSel.getText().trim());
            ps.setString(i, txtEntidadSel.getText().trim());

            return ps.executeUpdate() > 0;
        }
    }

    // === MÉTODOS AUXILIARES ===
    private Integer obtenerCodigoDerechoHabiente() {
        String valor = comboDerechohab.getValue();
        return valor != null ? mapaDerechohab.get(valor) : null;
    }

    private Integer obtenerCodigoSexo() {
        String sexo = comboSexo.getValue();
        if (sexo == null) return null;
        return sexo.equalsIgnoreCase("masculino") ? Integer.valueOf(1) :
                sexo.equalsIgnoreCase("femenino") ? 2 : null;
    }

    private String obtenerCodigoMunicipio() {
        String municipio = txtMunicipioSel.getText().trim();
        String entidad = txtEntidadSel.getText().trim();

        if (municipio.isEmpty() || entidad.isEmpty()) return "999";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT MPO FROM tblt_mpo WHERE DESCRIP = ? AND EDO = (SELECT EDO FROM tblt_entidad WHERE DESCRIP = ?)")) {

            ps.setString(1, municipio);
            ps.setString(2, entidad);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("MPO") : "999";
        } catch (SQLException e) {
            log.error("Error al obtener código municipio, usando determinado 999", e);
            return "999";
        }
    }

    private String obtenerCodigoEntidad(String nombreEntidad) {
        if (nombreEntidad == null || nombreEntidad.isEmpty()) return "97";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("SELECT EDO FROM tblt_entidad WHERE DESCRIP = ?")) {
            ps.setString(1, nombreEntidad);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("EDO") : "97";
        } catch (SQLException e) {
            log.error("Error al obtener código entidad, usando determinado 97", e);
            return "97";
        }
    }

    // === GENERADOR CURP COMPLETO ===
    @FXML
    private void generarCURPAutomatica() {
        if (!validarDatosCURP()) {
            mostrarAlerta("Faltan datos", "Complete datos para CURP", Alert.AlertType.WARNING);
            return;
        }

        String curp = generarCURP();
        if (!curp.isEmpty()) {
            txtCURP.setText(curp);
            mostrarAlerta("CURP generada", curp, Alert.AlertType.INFORMATION);
        }
    }

    private boolean validarDatosCURP() {
        return !txtApPaterno.getText().trim().isEmpty() &&
                !txtNombre.getText().trim().isEmpty() &&
                dpFechaNac.getValue() != null &&
                comboSexo.getValue() != null &&
                !txtEntidadSel.getText().isEmpty();
    }

    private static Map<String, String> initMapaEntidadesCURP() {
        Map<String, String> mapa = new HashMap<>();
        mapa.put("AGUASCALIENTES", "AS");
        mapa.put("BAJA CALIFORNIA", "BC");
        mapa.put("BAJA CALIFORNIA SUR", "BS");
        mapa.put("CAMPECHE", "CC");
        mapa.put("CHIAPAS", "CS");
        mapa.put("CHIHUAHUA", "CH");
        mapa.put("CIUDAD DE MÉXICO", "DF");
        mapa.put("COAHUILA", "CL");
        mapa.put("COLIMA", "CM");
        mapa.put("DURANGO", "DG");
        mapa.put("GUANAJUATO", "GT");
        mapa.put("GUERRERO", "GR");
        mapa.put("HIDALGO", "HG");
        mapa.put("JALISCO", "JC");
        mapa.put("MÉXICO", "MC");
        mapa.put("MICHOACÁN", "MN");
        mapa.put("MORELOS", "MS");
        mapa.put("NAYARIT", "NT");
        mapa.put("NUEVO LEÓN", "NL");
        mapa.put("OAXACA", "OC");
        mapa.put("PUEBLA", "PL");
        mapa.put("QUERÉTARO", "QT");
        mapa.put("QUINTANA ROO", "QR");
        mapa.put("SAN LUIS POTOSÍ", "SP");
        mapa.put("SINALOA", "SL");
        mapa.put("SONORA", "SR");
        mapa.put("TABASCO", "TC");
        mapa.put("TAMAULIPAS", "TS");
        mapa.put("TLAXCALA", "TL");
        mapa.put("VERACRUZ", "VZ");
        mapa.put("YUCATÁN", "YN");
        mapa.put("ZACATECAS", "ZS");
        mapa.put("EXTRANJERO", "NE");
        return mapa;
    }

    // === SELECTORES ENTIDAD/MUNICIPIO ===
    @FXML
    private void seleccionarEntidad() {
        mostrarDialogoSeleccion("Seleccionar Entidad", "entidades",
                "SELECT DESCRIP FROM tblt_entidad WHERE DESCRIP NOT IN ('OTRO PAIS')",
                txtEntidadSel, true);
    }

    @FXML
    private void seleccionarMunicipio() {
        if (txtEntidadSel.getText().isEmpty()) {
            mostrarAlerta("Primero entidad", "Seleccione entidad primero", Alert.AlertType.WARNING);
            return;
        }

        String codigoEntidad = obtenerCodigoEntidadDesdeNombre(txtEntidadSel.getText());
        if (codigoEntidad == null) return;

        mostrarDialogoSeleccion("Seleccionar Municipio", "municipios",
                "SELECT DESCRIP FROM tblt_mpo WHERE EDO = ? AND DESCRIP NOT IN ('OTRO(Escribir Manualmente)')",
                txtMunicipioSel, false, codigoEntidad);
    }

    private void mostrarDialogoSeleccion(String titulo, String tipo, String sql, TextField objetivo, boolean habilitarMunicipio, String... parametros) {
        try (Connection conn = ConexionBD.conectar()) {

            String sqlCompleto = sql;
            if (parametros.length == 0 && !sql.toUpperCase().contains("ORDER BY")) {
                sqlCompleto += " ORDER BY DESCRIP";
            }

            PreparedStatement ps = conn.prepareStatement(sqlCompleto);

            if (parametros.length > 0) {
                ps.setString(1, parametros[0]);
            }

            ResultSet rs = ps.executeQuery();

            Dialog<String> dialogo = new Dialog<>();
            dialogo.setTitle(titulo);

            ListView<String> lista = new ListView<>();
            while (rs.next()) {
                lista.getItems().add(rs.getString(1));
            }
            lista.getItems().add("ESCRIBIR MANUALMENTE");

            dialogo.getDialogPane().setContent(lista);
            dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialogo.setResultConverter(boton -> boton == ButtonType.OK ? lista.getSelectionModel().getSelectedItem() : null);

            dialogo.showAndWait().ifPresent(seleccion -> {
                if (seleccion.equals("ESCRIBIR MANUALMENTE")) {
                    mostrarDialogoTextoManual(tipo.equals("entidades") ? "Entidad" : "Municipio", objetivo);
                } else {
                    objetivo.setText(seleccion);
                    if (habilitarMunicipio) btnMunicipio.setDisable(false);
                }
            });

            rs.close();
            ps.close();

        } catch (SQLException e) {
            log.error("Error SQL en mostrarDialogoSeleccion: {}", e.getMessage());
            mostrarAlerta("Error", "Error cargando " + tipo + ": " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void mostrarDialogoTextoManual(String campo, TextField objetivo) {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Escribir " + campo);
        dialogo.setContentText(campo + ":");
        dialogo.showAndWait().ifPresent(valor -> {
            if (!valor.trim().isEmpty()) {
                objetivo.setText(valor.trim());
            }
        });
    }

    private String obtenerCodigoEntidadDesdeNombre(String entidad) {
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("SELECT EDO FROM tblt_entidad WHERE DESCRIP = ?")) {
            ps.setString(1, entidad);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : "97";
        } catch (SQLException e) {
            log.error("Error al Obtener Código Entidad desde Nombre, usando determinado 97", e);
            return "97";
        }
    }

    // === UTILIDADES ===
    private void registrarAuditoria(int folio, String username) {
        new Thread(() -> {
            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO tb_auditoria (username, accion, tabla_afectada, registro_id) VALUES (?,?,?,?)")) {
                ps.setString(1, username);
                ps.setString(2, "Registro triage");
                ps.setString(3, "tb_urgencias");
                ps.setInt(4, folio);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.error("Error al registrar auditoria", e);
            }
        }).start();
    }

    private void limpiarCampos() {
        txtApPaterno.clear();
        txtApMaterno.clear();
        txtNombre.clear();
        txtEdad.clear();
        txtTelefono.clear();
        txtDomicilio.clear();
        txtNoAfiliacion.clear();
        txtReferencia.clear();
        txtExpediente.clear();
        txtCURP.clear();
        txtSintomas.clear();
        txtObservaciones.clear();
        txtMunicipioSel.clear();
        txtEntidadSel.clear();
        txtOcupacion.clear();
        txtReligion.clear();
        txtEstadoCivil.clear();
        comboTriage.setValue(null);
        comboDerechohab.setValue(null);
        comboMedico.setValue(null);
        comboSexo.setValue(null);
        dpFechaNac.setValue(null);
        chkReingreso.setSelected(false);
        chkHospitalizado.setSelected(false);
        btnTriageColor.setStyle("-fx-background-color:lightgray;");
        lblTriageDescripcion.setText("");
    }

    @FXML
    private void cerrarVentana() {
        ((Stage) btnSalir.getScene().getWindow()).close();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }


    private String generarCURP() {
        try {
            String paterno = limpiarTextoParaCURP(txtApPaterno.getText().trim().toUpperCase());
            String materno = limpiarTextoParaCURP(txtApMaterno.getText().trim().toUpperCase());
            String nombre = limpiarTextoParaCURP(txtNombre.getText().trim().toUpperCase());

            // 1. Las primeras 4 letras
            StringBuilder curp = new StringBuilder();

            // Primera letra del primer apellido
            curp.append(paterno.charAt(0));

            // Primera vocal interna del primer apellido
            curp.append(obtenerPrimeraVocalInternaExacta(paterno));

            // Primera letra del segundo apellido (o X si no tiene)
            curp.append(materno.isEmpty() ? 'X' : materno.charAt(0));

            // Primera letra del primer nombre REAL (omitir MARIA/JOSE)
            curp.append(obtenerPrimerNombreRealExacto(nombre).charAt(0));

            // 2. Fecha de nacimiento (AAMMDD)
            curp.append(dpFechaNac.getValue().format(DateTimeFormatter.ofPattern("yyMMdd")));

            // 3. Sexo (H/M)
            curp.append(comboSexo.getValue().equals("Masculino") ? "H" : "M");

            // 4. Clave de entidad federativa
            curp.append(obtenerCodigoCURPEntidadExacto(txtEntidadSel.getText().trim()));

            // 5. Tres consonantes internas
            curp.append(obtenerPrimeraConsonanteInternaExacta(paterno));
            curp.append(obtenerPrimeraConsonanteInternaExacta(materno.isEmpty() ? "X" : materno));
            curp.append(obtenerPrimeraConsonanteInternaExacta(obtenerPrimerNombreRealExacto(nombre)));

            // 6. Dígito diferenciador (usamos 0 como placeholder)
            curp.append("0");

            // 7. Dígito verificador (simple para propósitos de demo)
            curp.append(calcularDigitoVerificadorSimple(curp.toString()));

            return curp.toString();

        } catch (Exception e) {
            log.error("Error generando CURP: {}", e.getMessage());
            return "";
        }
    }

    private String limpiarTextoParaCURP(String texto) {
        if (texto == null || texto.isEmpty()) return "";

        // Eliminar acentos y caracteres especiales
        String limpio = texto
                .replace('Á', 'A').replace('É', 'E').replace('Í', 'I')
                .replace('Ó', 'O').replace('Ú', 'U').replace('Ü', 'U')
                .replace('Ñ', 'X'); // La Ñ se convierte en X en CURP

        // Eliminar artículos, preposiciones y partículas
        String[] palabrasOmitir = {"DE", "LA", "DEL", "LAS", "LOS", "Y",
                "MC", "VON", "VAN", "DA", "DI", "EL"};

        for (String palabra : palabrasOmitir) {
            limpio = limpio.replace(" " + palabra + " ", " ");
        }

        // Eliminar espacios múltiples y trim
        return limpio.replaceAll("\\s+", " ").trim();
    }

    private String obtenerPrimerNombreRealExacto(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isEmpty()) return "X";

        String[] nombres = nombreCompleto.split(" ");

        // Si el primer nombre es "MARIA" o "JOSE", usar el SEGUNDO nombre
        if (nombres.length > 1) {
            String primerNombre = nombres[0];

            // Lista completa de variantes
            String[] nombresOmitir = {"MARIA", "MA", "MA.", "JOSE", "J", "J.", "JOSÉ",
                    "MARÍA", "JOSÉ", "MAR", "JOS"};

            for (String nombreOmitir : nombresOmitir) {
                if (primerNombre.equals(nombreOmitir)) {
                    return nombres[1]; // Usar el segundo nombre
                }
            }
        }

        // Si no es María/José, usar el primer nombre
        return nombres.length > 0 ? nombres[0] : "X";
    }

    private char obtenerPrimeraVocalInternaExacta(String texto) {
        if (texto == null || texto.length() < 2) return 'X';

        // Buscar la primera vocal DESPUÉS del primer carácter
        for (int i = 1; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U') {
                return c;
            }
        }
        return 'X'; // Si no encuentra vocal interna
    }

    private char obtenerPrimeraConsonanteInternaExacta(String texto) {
        if (texto == null || texto.length() < 2) return 'X';

        // Lista de consonantes válidas en CURP (incluye Ñ como X)
        String consonantes = "BCDFGHJKLMNÑPQRSTVWXYZ";

        // Buscar la primera consonante DESPUÉS del primer carácter
        for (int i = 1; i < texto.length(); i++) {
            char c = texto.charAt(i);
            // Convertir Ñ a X
            if (c == 'Ñ') return 'X';
            if (consonantes.indexOf(c) != -1) {
                return c;
            }
        }
        return 'X'; // Si no encuentra consonante interna
    }

    private String obtenerCodigoCURPEntidadExacto(String entidad) {
        var mapaCompleto = initMapaEntidadesCURP();

        // Buscar coincidencia exacta o parcial
        for (Map.Entry<String, String> entry : mapaCompleto.entrySet()) {
            if (entidad.toUpperCase().contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "NE"; // No especificado
    }

    private char calcularDigitoVerificadorSimple(String curp15) {
        // Para propósitos de demo, usar un dígito simple
        // En la realidad, RENAPO usa un algoritmo complejo
        int suma = 0;
        for (int i = 0; i < curp15.length(); i++) {
            suma += curp15.charAt(i);
        }
        int digito = (suma % 10);
        return (char) ('0' + digito);
    }

}