package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.utils.SesionUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.poi.hpsf.Date;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EdicionPacienteController {

    @FXML private Label lblTitulo, lblInfoCapturista, lblInfoTurno, lblInfoFecha;
    @FXML private TextField txtApPaterno, txtApMaterno, txtNombre, txtEdad, txtTelefono;
    @FXML private TextField txtDomicilio, txtNoAfiliacion, txtReferencia, txtCURP;
    @FXML private TextField txtMunicipioSel, txtEntidadSel, txtOcupacion, txtReligion, txtEstadoCivil;
    @FXML private TextArea txtSintomas, txtObservaciones;
    @FXML private DatePicker dpFechaNac;
    @FXML private ComboBox<String> comboSexo, comboDerechohab;
    @FXML private CheckBox chkReingreso, chkHospitalizado;
    @FXML private Button btnGuardar, btnCancelar, btnRestaurar;

    private int folioPaciente;
    private final HashMap<String, Object> datosOriginales = new HashMap<>();
    private final HashMap<String, Integer> mapaDerechohabiencia = new HashMap<>();

    private static final HashMap<String, String> CODIGOS_ENTIDADES = new HashMap<>();
    static {
        CODIGOS_ENTIDADES.put("AGUASCALIENTES", "AS"); CODIGOS_ENTIDADES.put("BAJA CALIFORNIA", "BC");
        CODIGOS_ENTIDADES.put("BAJA CALIFORNIA SUR", "BS"); CODIGOS_ENTIDADES.put("CAMPECHE", "CC");
        CODIGOS_ENTIDADES.put("COAHUILA", "CL"); CODIGOS_ENTIDADES.put("COLIMA", "CM");
        CODIGOS_ENTIDADES.put("CHIAPAS", "CS"); CODIGOS_ENTIDADES.put("CHIHUAHUA", "CH");
        CODIGOS_ENTIDADES.put("CIUDAD DE MÉXICO", "DF"); CODIGOS_ENTIDADES.put("DURANGO", "DG");
        CODIGOS_ENTIDADES.put("GUANAJUATO", "GT"); CODIGOS_ENTIDADES.put("GUERRERO", "GR");
        CODIGOS_ENTIDADES.put("HIDALGO", "HG"); CODIGOS_ENTIDADES.put("JALISCO", "JC");
        CODIGOS_ENTIDADES.put("MÉXICO", "MC"); CODIGOS_ENTIDADES.put("MICHOACÁN", "MN");
        CODIGOS_ENTIDADES.put("MORELOS", "MS"); CODIGOS_ENTIDADES.put("NAYARIT", "NT");
        CODIGOS_ENTIDADES.put("NUEVO LEÓN", "NL"); CODIGOS_ENTIDADES.put("OAXACA", "OC");
        CODIGOS_ENTIDADES.put("PUEBLA", "PL"); CODIGOS_ENTIDADES.put("QUERÉTARO", "QT");
        CODIGOS_ENTIDADES.put("QUINTANA ROO", "QR"); CODIGOS_ENTIDADES.put("SAN LUIS POTOSÍ", "SP");
        CODIGOS_ENTIDADES.put("SINALOA", "SL"); CODIGOS_ENTIDADES.put("SONORA", "SR");
        CODIGOS_ENTIDADES.put("TABASCO", "TC"); CODIGOS_ENTIDADES.put("TAMAULIPAS", "TS");
        CODIGOS_ENTIDADES.put("TLAXCALA", "TL"); CODIGOS_ENTIDADES.put("VERACRUZ DE IGNACIO DE LA LLAVE", "VZ");
        CODIGOS_ENTIDADES.put("YUCATÁN", "YN"); CODIGOS_ENTIDADES.put("ZACATECAS", "ZS");
        CODIGOS_ENTIDADES.put("EXTRANJERO", "NE"); CODIGOS_ENTIDADES.put("OTRO PAIS", "NE");
    }

    @FXML
    public void initialize() {
        comboSexo.getItems().addAll("Masculino", "Femenino");
        cargarDerechohabiencia();
        configurarInformacionSesion();
        aplicarLimitesEntrada();
        configurarValidacionesYEdadAutomatica();
    }

    private void configurarInformacionSesion() {
        lblInfoCapturista.setText("Editando por: Usuario Actual");
        lblInfoTurno.setText("Turno: Actual");

        // Forma segura de mostrar fecha y hora (nunca falla)
        String fechaHora = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        lblInfoFecha.setText("Fecha: " + fechaHora);
    }

    private void aplicarLimitesEntrada() {
        // CADA CAMPO TIENE SU PROPIO FORMATTER (así nunca falla)
        TextFormatter<String> limit50 = crearLimitador(50);
        TextFormatter<String> limit200 = crearLimitador(200);
        TextFormatter<String> limit150 = crearLimitador(150);
        TextFormatter<String> limit18 = crearLimitador(18);
        TextFormatter<String> limit15 = crearLimitador(15);
        TextFormatter<String> limit20 = crearLimitador(20);
        TextFormatter<String> soloNum = new TextFormatter<>(c -> c.getControlNewText().matches("\\d*") ? c : null);

        txtApPaterno.setTextFormatter(limit50);
        txtApMaterno.setTextFormatter(crearLimitador(50));
        txtNombre.setTextFormatter(crearLimitador(50));
        txtOcupacion.setTextFormatter(crearLimitador(50));
        txtReligion.setTextFormatter(crearLimitador(50));
        txtEstadoCivil.setTextFormatter(crearLimitador(50));
        txtDomicilio.setTextFormatter(limit200);
        txtReferencia.setTextFormatter(limit150);
        txtTelefono.setTextFormatter(limit15);
        txtNoAfiliacion.setTextFormatter(limit20);
        txtCURP.setTextFormatter(limit18);
        txtEdad.setTextFormatter(soloNum);

        // Áreas de texto grandes
        txtSintomas.textProperty().addListener((obs, old, nuevo) -> {
            if (nuevo.length() > 2000) txtSintomas.setText(old);
        });
        txtObservaciones.textProperty().addListener((obs, old, nuevo) -> {
            if (nuevo.length() > 2000) txtObservaciones.setText(old);
        });
    }

    // MÉTODO AUXILIAR
    private TextFormatter<String> crearLimitador(int maxLength) {
        return new TextFormatter<>(c -> c.getControlNewText().length() <= maxLength ? c : null);
    }

    private void configurarValidacionesYEdadAutomatica() {
        Runnable validar = () -> btnGuardar.setDisable(
                txtApPaterno.getText().trim().isEmpty() ||
                        txtNombre.getText().trim().isEmpty() ||
                        txtEdad.getText().isEmpty() ||
                        comboSexo.getValue() == null
        );

        txtApPaterno.textProperty().addListener(o -> validar.run());
        txtNombre.textProperty().addListener(o -> validar.run());
        txtEdad.textProperty().addListener(o -> validar.run());
        comboSexo.valueProperty().addListener(o -> validar.run());

        dpFechaNac.valueProperty().addListener((obs, old, nueva) -> {
            if (nueva != null) {
                int edad = LocalDate.now().getYear() - nueva.getYear();
                if (LocalDate.now().getDayOfYear() < nueva.getDayOfYear()) edad--;
                txtEdad.setText(String.valueOf(Math.max(0, edad)));
            }
        });
    }

    public void cargarPacienteExistente(int folio) {
        this.folioPaciente = folio;
        lblTitulo.setText("EDITAR DATOS DE PACIENTE - Folio: " + folio);

        String sql = "SELECT * FROM tb_urgencias WHERE Folio = ?";
        try (Connection conn = ConexionBD.getSafeConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folio);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                guardarDatosOriginales(rs);
                cargarDatosEnInterfaz(rs);
            } else {
                mostrarAlerta("Error", "Paciente no encontrado", Alert.AlertType.ERROR);
                cancelar();
            }
        } catch (SQLException e) {
            mostrarAlerta("Error", "Error cargando paciente: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void guardarDatosOriginales(ResultSet rs) throws SQLException {
        datosOriginales.clear();
        datosOriginales.put("A_paterno", rs.getString("A_paterno"));
        datosOriginales.put("A_materno", rs.getString("A_materno"));
        datosOriginales.put("Nombre", rs.getString("Nombre"));
        datosOriginales.put("Edad", rs.getInt("Edad"));
        datosOriginales.put("F_nac", rs.getDate("F_nac"));
        datosOriginales.put("Sexo", rs.getInt("Sexo"));
        datosOriginales.put("Telefono", rs.getString("Telefono"));
        datosOriginales.put("Domicilio", rs.getString("Domicilio"));
        datosOriginales.put("Derechohabiencia", rs.getInt("Derechohabiencia"));
        datosOriginales.put("No_afiliacion", rs.getString("No_afiliacion"));
        datosOriginales.put("Referencia", rs.getString("Referencia"));
        datosOriginales.put("Reingreso", rs.getBoolean("Reingreso"));
        datosOriginales.put("Hospitalizado", rs.getBoolean("Hospitalizado"));
        datosOriginales.put("CURP", rs.getString("CURP"));
        datosOriginales.put("Sintomas", rs.getString("Sintomas"));
        datosOriginales.put("Ocupacion", rs.getString("Ocupacion"));
        datosOriginales.put("Religion", rs.getString("Religion"));
        datosOriginales.put("Edo_civil", rs.getString("Edo_civil"));
        datosOriginales.put("Observaciones_ts", rs.getString("Observaciones_ts"));
        datosOriginales.put("Entidad_resid", rs.getString("Entidad_resid"));
        datosOriginales.put("Municipio_resid", rs.getString("Municipio_resid"));
        datosOriginales.put("Entidad_completa", rs.getString("Entidad_completa"));
        datosOriginales.put("Municipio_completo", rs.getString("Municipio_completo"));
    }

    private void cargarDatosEnInterfaz(ResultSet rs) throws SQLException {
        txtApPaterno.setText(rs.getString("A_paterno"));
        txtApMaterno.setText(rs.getString("A_materno"));
        txtNombre.setText(rs.getString("Nombre"));
        txtEdad.setText(String.valueOf(rs.getInt("Edad")));

        java.sql.Date fNac = rs.getDate("F_nac");
        if (fNac != null) dpFechaNac.setValue(fNac.toLocalDate());

        int sexo = rs.getInt("Sexo");
        comboSexo.setValue(sexo == 1 ? "Masculino" : "Femenino");

        txtTelefono.setText(rs.getString("Telefono"));
        txtDomicilio.setText(rs.getString("Domicilio"));
        txtOcupacion.setText(rs.getString("Ocupacion"));
        txtReligion.setText(rs.getString("Religion"));
        txtEstadoCivil.setText(rs.getString("Edo_civil"));
        txtNoAfiliacion.setText(rs.getString("No_afiliacion"));
        txtReferencia.setText(rs.getString("Referencia"));
        txtCURP.setText(rs.getString("CURP"));
        txtSintomas.setText(rs.getString("Sintomas"));
        txtObservaciones.setText(rs.getString("Observaciones_ts"));
        txtMunicipioSel.setText(rs.getString("Municipio_completo"));  // ← Nombre completo
        txtEntidadSel.setText(rs.getString("Entidad_completa"));     // ← Nombre completo

        chkReingreso.setSelected(rs.getBoolean("Reingreso"));
        chkHospitalizado.setSelected(rs.getBoolean("Hospitalizado"));

        int dh = rs.getInt("Derechohabiencia");
        if (dh > 0) {
            String desc = obtenerDescripcionDerechohabiencia(dh);
            if (desc != null) comboDerechohab.setValue(desc);
        }
    }

    private void cargarDerechohabiencia() {
        try (Connection c = ConexionBD.getSafeConnection();
             PreparedStatement ps = c.prepareStatement("SELECT Cve_dh, Derechohabiencia FROM tblt_cvedhabiencia ORDER BY Derechohabiencia");
             ResultSet rs = ps.executeQuery()) {

            comboDerechohab.getItems().clear();
            mapaDerechohabiencia.clear();
            while (rs.next()) {
                String desc = rs.getString("Derechohabiencia");
                int clave = rs.getInt("Cve_dh");
                comboDerechohab.getItems().add(desc);
                mapaDerechohabiencia.put(desc, clave);
            }
        } catch (SQLException ignored) {}
    }

    private String obtenerDescripcionDerechohabiencia(int cve) {
        try (Connection c = ConexionBD.getSafeConnection();
             PreparedStatement ps = c.prepareStatement("SELECT Derechohabiencia FROM tblt_cvedhabiencia WHERE Cve_dh = ?")) {
            ps.setInt(1, cve);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException ignored) {}
        return null;
    }

    @FXML
    private void guardarCambios() {
        String sql = "UPDATE tb_urgencias SET " +
                "A_paterno=?, A_materno=?, Nombre=?, Edad=?, F_nac=?, Sexo=?, Telefono=?, Domicilio=?, " +
                "Ocupacion=?, Religion=?, Edo_civil=?, Derechohabiencia=?, No_afiliacion=?, Referencia=?, " +
                "Reingreso=?, Hospitalizado=?, CURP=?, Sintomas=?, Observaciones_ts=?, " +
                "Entidad_resid=?, Municipio_resid=?, Entidad_completa=?, Municipio_completo=? " + // ← CAMBIÓ (añadidos 2 campos)
                "WHERE Folio=?";

        try (Connection c = ConexionBD.getSafeConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            // === PARTE NUEVA: OBTENER CÓDIGOS ===
            String nombreEntidad = txtEntidadSel.getText().trim();
            String nombreMunicipio = txtMunicipioSel.getText().trim();

            String codigoEntidad = null;
            String codigoMunicipio = null;

            // 1. Buscar código de entidad (2 letras) en la BD
            if (!nombreEntidad.isEmpty() && !"ESCRIBIR MANUALMENTE".equals(nombreEntidad)) {
                String sqlEntidad = "SELECT EDO FROM tblt_entidad WHERE DESCRIP = ?";
                try (PreparedStatement psEnt = c.prepareStatement(sqlEntidad)) {
                    psEnt.setString(1, nombreEntidad);
                    ResultSet rs = psEnt.executeQuery();
                    if (rs.next()) {
                        codigoEntidad = rs.getString("EDO");
                    }
                }
            }

            // 2. Buscar código de municipio (3 letras) en la BD
            if (!nombreMunicipio.isEmpty() && !"ESCRIBIR MANUALMENTE".equals(nombreMunicipio) && codigoEntidad != null) {
                String sqlMunicipio = "SELECT MPO FROM tblt_mpo WHERE DESCRIP = ? AND EDO = ?";
                try (PreparedStatement psMpio = c.prepareStatement(sqlMunicipio)) {
                    psMpio.setString(1, nombreMunicipio);
                    psMpio.setString(2, codigoEntidad);
                    ResultSet rs = psMpio.executeQuery();
                    if (rs.next()) {
                        codigoMunicipio = rs.getString("MPO");
                    }
                }
            }
            // === FIN PARTE NUEVA ===

            // === SETEOS DE DATOS (los primeros 19 igual) ===
            ps.setString(1, txtApPaterno.getText().trim().toUpperCase());
            ps.setString(2, txtApMaterno.getText().trim().toUpperCase());
            ps.setString(3, txtNombre.getText().trim().toUpperCase());
            ps.setInt(4, Integer.parseInt(txtEdad.getText()));
            ps.setDate(5, dpFechaNac.getValue() != null ? java.sql.Date.valueOf(dpFechaNac.getValue()) : null);
            ps.setInt(6, comboSexo.getValue().equals("Masculino") ? 1 : 2);
            ps.setString(7, txtTelefono.getText().trim());
            ps.setString(8, txtDomicilio.getText().trim());
            ps.setString(9, txtOcupacion.getText().trim());
            ps.setString(10, txtReligion.getText().trim());
            ps.setString(11, txtEstadoCivil.getText().trim());
            ps.setInt(12, mapaDerechohabiencia.getOrDefault(comboDerechohab.getValue(), 0));
            ps.setString(13, txtNoAfiliacion.getText().trim());
            ps.setString(14, txtReferencia.getText().trim());
            ps.setBoolean(15, chkReingreso.isSelected());
            ps.setBoolean(16, chkHospitalizado.isSelected());
            ps.setString(17, txtCURP.getText().trim().toUpperCase());
            ps.setString(18, txtSintomas.getText());
            ps.setString(19, txtObservaciones.getText());

            // === CAMBIOS AQUÍ (campos 20-23) ===
            // Campo 20: Entidad_resid (código 2 letras para FK)
            if (codigoEntidad != null && !codigoEntidad.isEmpty()) {
                ps.setString(20, codigoEntidad);
            } else {
                ps.setNull(20, java.sql.Types.VARCHAR);  // NULL si no hay código
            }

            // Campo 21: Municipio_resid (código 3 letras para FK)
            if (codigoMunicipio != null && !codigoMunicipio.isEmpty()) {
                ps.setString(21, codigoMunicipio);
            } else {
                ps.setNull(21, java.sql.Types.VARCHAR);  // NULL si no hay código
            }

            // Campo 22: Entidad_completa (nombre completo para mostrar)
            if ("ESCRIBIR MANUALMENTE".equals(nombreEntidad)) {
                ps.setString(22, "");  // Vacío si escribió manualmente
            } else {
                ps.setString(22, nombreEntidad);  // Nombre normal
            }

            // Campo 23: Municipio_completo (nombre completo para mostrar)
            if ("ESCRIBIR MANUALMENTE".equals(nombreMunicipio)) {
                ps.setString(23, "");  // Vacío si escribió manualmente
            } else {
                ps.setString(23, nombreMunicipio);  // Nombre normal
            }

            // Campo 24: Folio (antes era 22, ahora es 24)
            ps.setInt(24, folioPaciente);

            if (ps.executeUpdate() > 0) {
                // ==================== AUDITORÍA  ====================
                try {
                    String nombreCompleto = SesionUsuario.getInstance().getNombreMedico();
                    String username = SesionUsuario.getInstance().getUsername();

                    if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
                        nombreCompleto = "Usuario desconocido";
                    }
                    if (username == null || username.trim().isEmpty()) {
                        username = "desconocido";
                    }

                    StringBuilder cambios = new StringBuilder();
                    String oldPaterno = (String) datosOriginales.getOrDefault("A_paterno", "");
                    String oldMaterno = (String) datosOriginales.getOrDefault("A_materno", "");
                    String oldNombre  = (String) datosOriginales.getOrDefault("Nombre", "");
                    String oldEdad    = String.valueOf(datosOriginales.getOrDefault("Edad", 0));
                    String oldCURP    = (String) datosOriginales.getOrDefault("CURP", "");
                    String oldDomicilio = (String) datosOriginales.getOrDefault("Domicilio", "");
                    String oldTelefono = (String) datosOriginales.getOrDefault("Telefono", "");

                    if (!txtApPaterno.getText().trim().equalsIgnoreCase(oldPaterno)) cambios.append("apellido paterno, ");
                    if (!txtApMaterno.getText().trim().equalsIgnoreCase(oldMaterno)) cambios.append("apellido materno, ");
                    if (!txtNombre.getText().trim().equalsIgnoreCase(oldNombre)) cambios.append("nombre, ");
                    if (!txtEdad.getText().equals(oldEdad)) cambios.append("edad, ");
                    if (!txtCURP.getText().trim().equalsIgnoreCase(oldCURP)) cambios.append("CURP, ");
                    if (!txtDomicilio.getText().trim().equalsIgnoreCase(oldDomicilio)) cambios.append("domicilio, ");
                    if (!txtTelefono.getText().trim().equalsIgnoreCase(oldTelefono)) cambios.append("teléfono, ");

                    String textoCambios = cambios.length() > 0
                            ? "Se modificó: " + cambios.substring(0, cambios.length() - 2)
                            : "Sin cambios detectados";

                    String sqlAudit = "INSERT INTO tb_auditoria_modificaciones " +
                            "(folio_paciente, capturista, username_capturista, datos_nuevos) " +
                            "VALUES (?, ?, ?, ?)";

                    try (PreparedStatement psAudit = c.prepareStatement(sqlAudit)) {
                        psAudit.setInt(1, folioPaciente);
                        psAudit.setString(2, nombreCompleto);
                        psAudit.setString(3, username);
                        psAudit.setString(4, textoCambios);
                        psAudit.executeUpdate();
                    }

                } catch (Exception ex) {
                    System.err.println("Advertencia: Falló auditoría: " + ex.getMessage());
                }
                // =====================================================================

                mostrarAlerta("Éxito", "Paciente actualizado correctamente", Alert.AlertType.INFORMATION);
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();
            }

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo guardar: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML private void cancelar() { ((Stage) btnCancelar.getScene().getWindow()).close(); }

    @FXML private void restaurarOriginal() { cargarPacienteExistente(folioPaciente); }

    @FXML
    private void seleccionarMunicipio() {
        // PRIMERO NECESITAMOS SABER LA ENTIDAD SELECCIONADA
        String entidadSeleccionada = txtEntidadSel.getText().trim();

        if (entidadSeleccionada.isEmpty() || "ESCRIBIR MANUALMENTE".equals(entidadSeleccionada)) {
            mostrarAlerta("Primero seleccione entidad", "Debe seleccionar una entidad válida antes de elegir municipio", Alert.AlertType.WARNING);
            return;
        }

        try (Connection conn = ConexionBD.getSafeConnection()) {
            // OBTENER EL CÓDIGO DE LA ENTIDAD SELECCIONADA
            String codigoEntidad = "";
            try (PreparedStatement psEnt = conn.prepareStatement(
                    "SELECT EDO FROM tblt_entidad WHERE DESCRIP = ?")) {
                psEnt.setString(1, entidadSeleccionada);
                ResultSet rsEnt = psEnt.executeQuery();
                if (rsEnt.next()) {
                    codigoEntidad = rsEnt.getString("EDO");
                }
            }

            if (codigoEntidad.isEmpty()) {
                mostrarAlerta("Error", "No se encontró código para la entidad seleccionada", Alert.AlertType.ERROR);
                return;
            }

            // AHORA FILTRAR MUNICIPIOS POR ESA ENTIDAD
            String sql = "SELECT DESCRIP FROM tblt_mpo WHERE EDO = ? AND DESCRIP NOT IN ('OTRO PAIS', 'OTRO(Escribir Manualmente)') ORDER BY DESCRIP";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, codigoEntidad);
            ResultSet rs = ps.executeQuery();

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Seleccionar Municipio");
            dialog.setHeaderText("Municipios de " + entidadSeleccionada + ":");
            ListView<String> listView = new ListView<>();
            while (rs.next()) listView.getItems().add(rs.getString(1));
            listView.getItems().add("ESCRIBIR MANUALMENTE");
            listView.setPrefHeight(400);
            dialog.getDialogPane().setContent(listView);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(btn -> btn == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);
            dialog.showAndWait().ifPresent(sel -> {
                if ("ESCRIBIR MANUALMENTE".equals(sel)) {
                    TextInputDialog tid = new TextInputDialog();
                    tid.setHeaderText("Escriba el municipio:");
                    tid.showAndWait().ifPresent(m -> txtMunicipioSel.setText(m.trim()));
                } else {
                    txtMunicipioSel.setText(sel);
                }
            });

        } catch (SQLException e) {
            mostrarAlerta("Error", "Error cargando municipios: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void seleccionarEntidad() {
        try (Connection conn = ConexionBD.getSafeConnection()) {
            String sql = "SELECT DESCRIP FROM tblt_entidad WHERE DESCRIP NOT IN ('OTRO PAIS', 'OTRO(Escribir Manualmente)') ORDER BY tipo, DESCRIP";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Seleccionar Entidad");
            dialog.setHeaderText("Elija una entidad federativa:");
            ListView<String> listView = new ListView<>();
            while (rs.next()) listView.getItems().add(rs.getString(1));
            listView.getItems().add("ESCRIBIR MANUALMENTE");
            listView.setPrefHeight(400);
            dialog.getDialogPane().setContent(listView);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(btn -> btn == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);
            dialog.showAndWait().ifPresent(sel -> {
                if ("ESCRIBIR MANUALMENTE".equals(sel)) {
                    TextInputDialog tid = new TextInputDialog();
                    tid.setHeaderText("Escriba la entidad:");
                    tid.showAndWait().ifPresent(m -> {
                        txtEntidadSel.setText(m.trim());
                        txtMunicipioSel.clear();  // Limpia municipio cuando cambia entidad
                    });
                } else {
                    txtEntidadSel.setText(sel);
                    txtMunicipioSel.clear();  // Limpia municipio cuando cambia entidad
                }
            });

        } catch (SQLException e) {
            mostrarAlerta("Error", "Error cargando entidades", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void generarCURPAutomatica() {
        if (txtApPaterno.getText().trim().isEmpty() || txtNombre.getText().trim().isEmpty()) {
            mostrarAlerta("Faltan datos", "Se requieren apellido paterno y nombre", Alert.AlertType.WARNING);
            return;
        }
        if (dpFechaNac.getValue() == null) {
            mostrarAlerta("Faltan datos", "Se requiere fecha de nacimiento", Alert.AlertType.WARNING);
            return;
        }
        if (comboSexo.getValue() == null) {
            mostrarAlerta("Faltan datos", "Se requiere sexo", Alert.AlertType.WARNING);
            return;
        }
        if (txtEntidadSel.getText().trim().isEmpty()) {
            mostrarAlerta("Faltan datos", "Se requiere entidad federativa", Alert.AlertType.WARNING);
            return;
        }

        String curp = generarCURPPropuesta();
        if (!curp.isEmpty()) {
            txtCURP.setText(curp);
            mostrarAlerta("CURP Generada", curp, Alert.AlertType.INFORMATION);
        }
    }

    private String generarCURPPropuesta() {
        StringBuilder curp = new StringBuilder();
        String paterno = limpiarTexto(txtApPaterno.getText().trim().toUpperCase());
        String materno = limpiarTexto(txtApMaterno.getText().trim().toUpperCase());
        String nombre = limpiarTexto(txtNombre.getText().trim().toUpperCase());

        if (paterno.isEmpty() || nombre.isEmpty()) return "";

        curp.append(paterno.charAt(0));
        curp.append(obtenerPrimeraVocalInterna(paterno));
        curp.append(materno.isEmpty() ? 'X' : materno.charAt(0));
        curp.append(obtenerPrimerNombreReal(nombre).charAt(0));

        curp.append(dpFechaNac.getValue().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")));

        curp.append("Masculino".equals(comboSexo.getValue()) ? "H" : "M");

        String entidad = txtEntidadSel.getText().trim().toUpperCase();
        curp.append(CODIGOS_ENTIDADES.getOrDefault(entidad, "NE"));

        curp.append(obtenerPrimeraConsonanteInterna(paterno));
        curp.append(obtenerPrimeraConsonanteInterna(materno));
        curp.append(obtenerPrimeraConsonanteInterna(obtenerPrimerNombreReal(nombre)));

        curp.append("0");
        curp.append(obtenerDigitoVerificadorSimple());

        return curp.toString();
    }

    private String limpiarTexto(String texto) {
        if (texto == null) return "";
        texto = texto.replace('Á', 'A').replace('É', 'E').replace('Í', 'I').replace('Ó', 'O').replace('Ú', 'U').replace('Ü', 'U').replace('Ñ', 'X');
        texto = texto.replaceAll("( DE | LA | DEL | LAS | LOS | Y | MC | VON | VAN | DA | DI | EL )", " ");
        return texto.replaceAll("\\s+", " ").trim();
    }

    private String obtenerPrimerNombreReal(String nombreCompleto) {
        String[] partes = nombreCompleto.split("\\s+");
        if (partes.length > 1) {
            String primero = partes[0].toUpperCase();
            if (Set.of("MARIA","MA","MA.","JOSE","J","J.","JOSÉ").contains(primero)) {
                return partes[1];
            }
        }
        return partes[0];
    }

    private char obtenerPrimeraVocalInterna(String texto) {
        for (int i = 1; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if ("AEIOU".indexOf(c) >= 0) return c;
        }
        return 'X';
    }

    private char obtenerPrimeraConsonanteInterna(String texto) {
        if (texto == null || texto.length() < 2) return 'X';
        for (int i = 1; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if ("AEIOU".indexOf(c) == -1 && c != 'Ñ') return c;
        }
        return 'X';
    }

    private String convertirEntidadACodigo(String nombreEntidad) {
        if (nombreEntidad == null || nombreEntidad.trim().isEmpty()) {
            return "";
        }
        String nombreUpper = nombreEntidad.toUpperCase();
        String codigo = CODIGOS_ENTIDADES.get(nombreUpper);
        return codigo != null ? codigo : "";
    }

    private char obtenerDigitoVerificadorSimple() {
        return (char) ('0' + new Random().nextInt(10));
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}