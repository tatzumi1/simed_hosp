package com.PruebaSimed2.controllers;

import com.PruebaSimed2.DTO.Urgencias.InsertarPacienteDTO;
import com.PruebaSimed2.database.AuditoriaData;
import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.database.UrgenciasData;
import com.PruebaSimed2.models.Edad;
import com.PruebaSimed2.utils.SesionUsuario;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class VentanaRegistroTriageController {

    // === CAMPOS FXML ===
    @FXML
    private DatePicker dpFechaNac;
    @FXML
    private TextField txtFecha, txtHora, txtApPaterno, txtApMaterno, txtNombre, txtTelefono;
    @FXML
    private TextField txtEdadAnos, txtEdadMeses, txtEdadDias;
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
    @FXML
    private TextField txtTalla, txtPeso;

    // === VARIABLES ===
    private String turno;
    private final ObservableList<String> listaMedicos = FXCollections.observableArrayList();
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
        txtEdadAnos.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d*") && c.getControlNewText().length() <= 3 ? c : null));
        txtEdadMeses.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d*") && c.getControlNewText().length() <= 2 ? c : null));
        txtEdadDias.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d*") && c.getControlNewText().length() <= 2 ? c : null));
        txtTalla.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d*") && c.getControlNewText().length() <= 3 ? c : null));
        txtPeso.setTextFormatter(new TextFormatter<>(c ->
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
        btnRegistrar.setDisable(true);

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
        comboTriage.getItems().addAll("Verde", "Amarillo", "Naranja", "Rojo");
        comboMedico.setEditable(true);
        comboMedico.valueProperty().addListener((ov, t, t1) -> {
        });
    }

    // TODO: Mover a clase propia.
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

    // TODO: Mover a clase propia.
    private void cargarMedicos() {
        try (Connection c = ConexionBD.conectar();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT Med_nombre FROM tb_medicos ORDER BY Med_nombre")) {

            listaMedicos.clear();
            while (rs.next()) {
                listaMedicos.add(rs.getString("Med_nombre"));
            }
            comboMedico.setItems(FXCollections.observableArrayList(listaMedicos));
            configurarFiltroMedico();
        } catch (SQLException e) {
            log.error("Error al cargar medicos", e);
        }
    }

    private void configurarFiltroMedico() {
        TextField editor = comboMedico.getEditor();

        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                javafx.application.Platform.runLater(() -> {
                    if (!comboMedico.isShowing()) {
                        comboMedico.setItems(FXCollections.observableArrayList(listaMedicos));
                        comboMedico.show();
                    }
                });
            }
        });

        editor.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo == null || nuevo.isEmpty()) {
                javafx.application.Platform.runLater(() -> comboMedico.setItems(FXCollections.observableArrayList(listaMedicos)));
                return;
            }

            // Si el texto es una selección exacta, no filtramos para evitar que el dropdown parpadee
            if (listaMedicos.contains(nuevo)) {
                return;
            }

            String filtro = nuevo.toLowerCase();
            ObservableList<String> filtrados = FXCollections.observableArrayList(
                    listaMedicos.stream()
                            .filter(item -> item.toLowerCase().contains(filtro))
                            .collect(java.util.stream.Collectors.toList())
            );

            javafx.application.Platform.runLater(() -> {
                comboMedico.setItems(filtrados);
                if (!filtrados.isEmpty()) {
                    comboMedico.show();
                } else {
                    comboMedico.hide();
                }
            });
        });
    }

    // === EVENTOS ===
    private void configurarEventos() {
        comboTriage.setOnAction(e -> actualizarColorTriage());
        btnRegistrar.setOnAction(e -> registrarPaciente());
        btnSalir.setOnAction(e -> cerrarVentana());
        btnMunicipio.setOnAction(e -> seleccionarMunicipio());
        btnEntidad.setOnAction(e -> seleccionarEntidad());
        seleccionarFecha();
    }

    private void seleccionarFecha() {
        dpFechaNac.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Edad edad = new Edad();
                edad.calcularEdad(newVal);
                txtEdadAnos.setText(String.valueOf(edad.getAnos()));
                txtEdadMeses.setText(String.valueOf(edad.getMeses()));
                txtEdadDias.setText(String.valueOf(edad.getDias()));
                log.debug("Edad calculada: {}", edad);
            } else {
                txtEdadAnos.clear();
                txtEdadMeses.clear();
                txtEdadDias.clear();
            }
        });
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
                "naranja", new String[]{"#ff8000", "Urgencia mayor"},
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
            log.error("Usuario sin sesión válida intentó registrar paciente");
            return;
        }

        boolean exito = ConexionBD.executeInTransaction(conn -> {
            conn.setAutoCommit(false);
            UrgenciasData ud = new UrgenciasData();
            int folio = ud.obtenerFolio(conn);
            if (folio <= 0) return false;
            InsertarPacienteDTO dto = formatearPacienteDTO(folio, nombreMedico);
            if (dto == null) return false;
            boolean insertado = ud.insertarPaciente(dto, conn);
            if (insertado) {
                conn.commit();
                registrarAuditoria(folio, username);
                log.debug("Paciente registrado con folio {}", folio);
                mostrarAlerta("Éxito", "Paciente registrado\nFolio: " + folio, Alert.AlertType.INFORMATION);
                limpiarCampos();
            } else {
                conn.rollback();
                log.warn("Error al registrar paciente con folio {}, realizando rollback", folio);
                mostrarAlerta("Error", "No se pudo registrar", Alert.AlertType.ERROR);
            }
            return insertado;
        });
        if (!exito) {
            mostrarAlerta("Error en la BD.", "Error en la operación.", Alert.AlertType.ERROR);
            log.error("Error al registrar paciente");
        }
    }

    private InsertarPacienteDTO formatearPacienteDTO(int folio, String nombreMedico) {
        LocalDate fechaNac = dpFechaNac.getValue();
        Edad edad = new Edad();
        if (fechaNac == null) {
            fechaNac = LocalDate.now();
            log.warn("Fecha nacimiento no especificada");
        }
        edad.calcularEdad(fechaNac);
        Integer cveDerechoHabiente = obtenerCodigoDerechoHabiente();
        if (cveDerechoHabiente == null) {
            log.warn("Derechohabiente no especificado");
            return null;
        }
        Integer codigoSexo = obtenerCodigoSexo();
        if (codigoSexo == null) {
            log.warn("Sexo no especificado");
            return null;
        }

        var dto = new InsertarPacienteDTO(
                folio,
                txtApPaterno.getText().trim(),
                txtApMaterno.getText().trim(),
                txtNombre.getText().trim(),
                edad.getAnos(),
                Date.valueOf(fechaNac),
                txtTelefono.getText().trim(),
                txtDomicilio.getText().trim(),
                cveDerechoHabiente,
                txtNoAfiliacion.getText().trim(),
                txtReferencia.getText().trim(),
                chkReingreso.isSelected(),
                chkHospitalizado.isSelected(),
                txtExpediente.getText().trim(),
                txtCURP.getText().trim(),
                txtSintomas.getText().trim(),
                comboMedico.getValue(),
                comboTriage.getValue(),
                nombreMedico,
                turno,
                txtHora.getText().trim(),
                codigoSexo,
                obtenerCodigoMunicipio(),
                obtenerCodigoEntidad(txtEntidadSel.getText().trim()),
                txtOcupacion.getText().trim(),
                txtReligion.getText().trim(),
                txtEstadoCivil.getText().trim(),
                txtObservaciones.getText().trim(),
                txtMunicipioSel.getText().trim(),
                txtEntidadSel.getText().trim()
        );
        dto.validarDto();
        return dto;
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

    // TODO: Mover operaciones de BD.
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

    // TODO: Mover operaciones de BD.
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
    // TODO: Mover operaciones de BD.
    @FXML
    private void seleccionarEntidad() {
        mostrarDialogoSeleccion("Seleccionar Entidad", "entidades",
                "SELECT DESCRIP FROM tblt_entidad WHERE DESCRIP NOT IN ('OTRO PAIS')",
                txtEntidadSel, true);
    }

    // TODO: Mover operaciones de BD.
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

    // TODO: Mover operaciones de BD.
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

    // TODO: Mover operaciones de BD.
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
            AuditoriaData auditoriaData = new AuditoriaData();
            auditoriaData.registrarAuditoriaRegistroTriage(username, folio);
        }).start();
    }

    private void limpiarCampos() {
        txtApPaterno.clear();
        txtApMaterno.clear();
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
        comboMedico.getEditor().setText("");
        comboMedico.setItems(FXCollections.observableArrayList(listaMedicos));
        comboSexo.setValue(null);
        dpFechaNac.setValue(null);
        chkReingreso.setSelected(false);
        chkHospitalizado.setSelected(false);
        btnTriageColor.setStyle("-fx-background-color:lightgray;");
        lblTriageDescripcion.setText("");
        txtNombre.clear();
        txtEdadAnos.clear();
        txtEdadMeses.clear();
        txtEdadDias.clear();
        txtPeso.clear();
        txtTalla.clear();
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

    // TODO: Mover a clase propia.
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

    // TODO: Mover a clase propia.
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

    // TODO: Mover a clase propia.
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

    // TODO: Mover a clase propia.
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

    // TODO: Mover a clase propia.
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

    // TODO: Mover a clase propia.
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