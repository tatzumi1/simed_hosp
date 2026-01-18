// src/main/java/com/PruebaSimed2/controllers/EgresoPacienteController.java

// src/main/java/com/PruebaSimed2/controllers/EgresoPacienteController.java

package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.log4j.Log4j2;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Log4j2
public class EgresoPacienteController {

    // Campos para afecciones y códigos CIE10
    @FXML
    private TextArea tfAfecc1, tfAfecc2, tfAfecc3;
    @FXML
    private ComboBox<String> cbCodAfecc1, cbCodAfecc2, cbCodAfecc3;

    // Campos para medicamentos
    @FXML
    private TextField tfMedic1, tfMedic2, tfMedic3, tfMedic4, tfMedic5, tfMedic6;

    // Campos nuevos
    @FXML
    private TextArea taCausaExterna;
    @FXML
    private ComboBox<String> cbIndigena, cbAfromexicano, cbMigrante;
    @FXML
    private ComboBox<String> cbMujerFertil, cbSituacionEmbarazo;
    @FXML
    private TextField tfSemanasGestacion;

    // Campos existentes
    @FXML
    private TextArea taAviso;
    @FXML
    private ComboBox<String> cbAltaPor;
    @FXML
    private TextField tfFolioDef;
    @FXML
    private TextField tfProc1, tfProc2, tfProc3;
    @FXML
    private ComboBox<String> cbIra, cbEda;
    @FXML
    private Spinner<Integer> spSobres;
    @FXML
    private ComboBox<String> cbAreaHosp;
    @FXML
    private ComboBox<String> cbMedicoAlta;
    @FXML
    private Label lblFechaAlta, lblHoraAlta;
    @FXML
    private DatePicker dpFechaAlta;
    @FXML
    private TextField tfHoraAlta;
    @FXML
    private CheckBox cbSoloJefatura;
    @FXML
    private TextField tfFolio;

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Timer timer;
    private int folioPaciente;


    public void setFolioPaciente(int folio) {
        this.folioPaciente = folio;
        if (tfFolio != null) {
            tfFolio.setText(String.valueOf(folio));
            tfFolio.setEditable(false);
        }
        log.debug("Folio recibido en egreso: {}", folio);
        cargarDatosPaciente(folio);
    }

    private void cargarDatosPaciente(int folio) {
        String sql = "SELECT u.*, " +
                "CONCAT(u.A_paterno, ' ', u.A_materno, ' ', u.Nombre) as NombreCompleto, " +
                "u.Edad, u.Sexo " +
                "FROM tb_urgencias u WHERE u.Folio = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folio);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                log.debug(" Cargando datos del paciente folio: {}", folio);
                String nombreCompleto = rs.getString("NombreCompleto");
                int edad = rs.getInt("Edad");
                int sexo = rs.getInt("Sexo");

                log.debug("Paciente: {}, Edad: {}, Sexo: {}", nombreCompleto, edad, sexo);

            } else {
                log.warn("No se encontró el paciente con folio: {}", folio);
            }

        } catch (SQLException e) {
            log.error("Error cargando datos del paciente: {}", e.getMessage());
        }
    }

    private String rolUsuario;

    public void setUsuarioLogueado(String usuario, String rol) {
        this.rolUsuario = rol;
        log.debug("Usuario logueado: {}, Rol: {}", usuario, rol);
        configurarEdicionFechaHora();
    }

    private void configurarEdicionFechaHora() {
        if (rolUsuario == null) return;

        boolean puedeEditar = "ADMIN".equals(rolUsuario) ||
                "JEFATURA_URGENCIAS".equals(rolUsuario) ||
                "ADMINISTRATIVO".equals(rolUsuario);

        if (puedeEditar) {
            log.debug("Rol con permisos de edición de fecha/hora detectado.");

            // Detener el timer si existe
            if (timer != null) {
                timer.cancel();
                timer = null;
            }

            // Cambiar visibilidad de componentes
            lblFechaAlta.setVisible(false);
            lblFechaAlta.setManaged(false);
            dpFechaAlta.setVisible(true);
            dpFechaAlta.setManaged(true);

            lblHoraAlta.setVisible(false);
            lblHoraAlta.setManaged(false);
            tfHoraAlta.setVisible(true);
            tfHoraAlta.setManaged(true);

            // Inicializar valores
            dpFechaAlta.setValue(java.time.LocalDate.now());
            tfHoraAlta.setText(java.time.LocalTime.now().format(timeFmt));
        }
    }

    @FXML
    public void initialize() {
        log.debug(" Inicializando ventana de egreso...");

        // Configurar límites de texto
        configurarLimitesTexto();

        // Configurar ComboBoxes de CIE10 (SIN AUTOREEMPLAZAR TEXTO)
        configurarComboBoxesCIE10();

        // Rellenar combos
        cargarCombos();

        // Configurar spinner
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0);
        spSobres.setValueFactory(valueFactory);

        // Iniciar reloj
        startDateTimeUpdater();

        // Cargar datos existentes si los hay
        cargarDatosPacienteExistente();

        // Inicializar dpFechaAlta y tfHoraAlta con valores actuales por defecto
        if (dpFechaAlta != null) dpFechaAlta.setValue(java.time.LocalDate.now());
        if (tfHoraAlta != null) tfHoraAlta.setText(java.time.LocalTime.now().format(timeFmt));

        // Configurar listeners para mujer en edad fértil
        configurarListeners();

        log.debug(" Ventana de egreso inicializada correctamente");
    }

    private void configurarComboBoxesCIE10() {
        log.debug(" Configurando ComboBoxes CIE10...");

        // Configurar cómo se muestran los ítems en la lista
        cbCodAfecc1.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // item tiene formato: "CODIGO|DESCRIPCION"
                    String[] partes = item.split("\\|", 2);
                    if (partes.length == 2) {
                        setText(partes[0] + " - " + partes[1]); // Muestra: "A00 - Cólera"
                    } else {
                        setText(item);
                    }
                }
            }
        });

        // Configurar cómo se muestra el ítem seleccionado
        cbCodAfecc1.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String[] partes = item.split("\\|", 2);
                    if (partes.length == 2) {
                        setText(partes[0] + " - " + partes[1]);
                    } else {
                        setText(item);
                    }
                }
            }
        });

        // Hacer lo mismo para cbCodAfecc2
        cbCodAfecc2.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String[] partes = item.split("\\|", 2);
                    if (partes.length == 2) {
                        setText(partes[0] + " - " + partes[1]);
                    } else {
                        setText(item);
                    }
                }
            }
        });

        cbCodAfecc2.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String[] partes = item.split("\\|", 2);
                    if (partes.length == 2) {
                        setText(partes[0] + " - " + partes[1]);
                    } else {
                        setText(item);
                    }
                }
            }
        });

        // Hacer lo mismo para cbCodAfecc3
        cbCodAfecc3.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String[] partes = item.split("\\|", 2);
                    if (partes.length == 2) {
                        setText(partes[0] + " - " + partes[1]);
                    } else {
                        setText(item);
                    }
                }
            }
        });

        cbCodAfecc3.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String[] partes = item.split("\\|", 2);
                    if (partes.length == 2) {
                        setText(partes[0] + " - " + partes[1]);
                    } else {
                        setText(item);
                    }
                }
            }
        });

        // Cargar datos de CIE10
        cargarDatosCIE10();

        log.debug("ComboBoxes CIE10 configurados correctamente");
    }

    private void cargarDatosCIE10() {
        log.debug("Cargando códigos CIE10 desde tblt_cie10...");

        // Mantener la misma consulta
        String sql = "SELECT codigo, descripcion FROM tblt_cie10 WHERE activo = TRUE ORDER BY codigo";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int count = 0;

            // LIMPIAR los combos ANTES de cargar
            cbCodAfecc1.getItems().clear();
            cbCodAfecc2.getItems().clear();
            cbCodAfecc3.getItems().clear();

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String descripcion = rs.getString("descripcion");

                // Formato: "CODIGO|DESCRIPCION"
                String itemCompleto = codigo + "|" + descripcion;

                // Agregar a los 3 ComboBoxes
                cbCodAfecc1.getItems().add(itemCompleto);
                cbCodAfecc2.getItems().add(itemCompleto);
                cbCodAfecc3.getItems().add(itemCompleto);
                count++;
            }

            log.debug("Códigos CIE10 cargados: {} registros", count);

            if (count == 0) {
                log.warn("¡ADVERTENCIA: La tabla tblt_cie10 está VACÍA!");
                cargarValoresEjemploCIE10();
            }

        } catch (SQLException e) {
            log.error("Error cargando códigos CIE10: {}", e.getMessage());
            cargarValoresEjemploCIE10();
        }
    }

    private void cargarValoresEjemploCIE10() {
        log.debug(" Cargando valores de EJEMPLO (tblt_cie10 vacía)...");

        // Insertar en la tabla REAL para futuras veces
        String sqlInsert = "INSERT IGNORE INTO tblt_cie10 (codigo, descripcion, frecuente_urgencias) VALUES (?, ?, TRUE)";

        // Ahora es un array de Strings en formato "CODIGO|DESCRIPCION"
        String[] ejemplos = {
                "A00|Cólera",
                "A01|Fiebres tifoidea y paratifoidea",
                "A02|Otras infecciones por Salmonella",
                "J18|Neumonía, organismo no especificado",
                "I10|Hipertensión esencial (primaria)",
                "E11|Diabetes mellitus no insulinodependiente",
                "S06|Traumatismo intracraneal",
                "R50|Fiebre de origen desconocido"
        };

        // 1. Agregar a los 3 ComboBoxes
        for (String ejemplo : ejemplos) {
            cbCodAfecc1.getItems().add(ejemplo);
            cbCodAfecc2.getItems().add(ejemplo);
            cbCodAfecc3.getItems().add(ejemplo);
        }

        // 2. Insertar en base de datos (separar código y descripción)
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {

            for (String ejemplo : ejemplos) {
                // Separar "A00|Cólera" en partes
                String[] partes = ejemplo.split("\\|", 2);
                if (partes.length == 2) {
                    pstmt.setString(1, partes[0]); // Código
                    pstmt.setString(2, partes[1]); // Descripción
                    pstmt.addBatch();
                }
            }

            int[] resultados = pstmt.executeBatch();
            log.info("Datos de ejemplo insertados en tblt_cie10: {} registros", resultados.length);

        } catch (SQLException e) {
            log.error("Error insertando ejemplos en tblt_cie10: {}", e.getMessage());
        }

        log.debug("Valores de ejemplo cargados: {} registros", ejemplos.length);
    }

    private void configurarLimitesTexto() {
        // Limitar afecciones a 250 caracteres
        tfAfecc1.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 250 ? change : null));
        tfAfecc2.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 250 ? change : null));
        tfAfecc3.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 250 ? change : null));

        // Limitar procedimientos a 200 caracteres
        tfProc1.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 200 ? change : null));
        tfProc2.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 200 ? change : null));
        tfProc3.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 200 ? change : null));

        // Limitar medicamentos a 100 caracteres
        tfMedic1.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 100 ? change : null));
        tfMedic2.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 100 ? change : null));
        tfMedic3.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 100 ? change : null));
        tfMedic4.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 100 ? change : null));
        tfMedic5.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 100 ? change : null));
        tfMedic6.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 100 ? change : null));

        // Limitar causa externa a 500 caracteres
        taCausaExterna.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 500 ? change : null));

        // Limitar semanas de gestación a 2 dígitos
        tfSemanasGestacion.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,2}") ? change : null));
    }

    private void configurarListeners() {
        // Mostrar/ocultar campos de embarazo según selección de mujer fértil
        cbMujerFertil.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean visible = "SI".equals(newVal);
            cbSituacionEmbarazo.setVisible(visible);
            tfSemanasGestacion.setVisible(visible);

            if (!visible) {
                cbSituacionEmbarazo.setValue(null);
                tfSemanasGestacion.clear();
            }
        });
    }

    private void cargarCombos() {
        try (Connection conn = ConexionBD.conectar()) {
            log.debug("Cargando combos desde BD...");

            // CARGAR TIPOS DE ALTA
            cbAltaPor.getItems().clear();
            String sqlAlta = "SELECT Cve_alta, Descripcion FROM tblt_cvealta ORDER BY Cve_alta";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlAlta);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    cbAltaPor.getItems().add(rs.getString("Descripcion"));
                }
            }

            // CARGAR ÁREAS HOSPITALARIAS
            // TODO: Verificar query de SQL.
            cbAreaHosp.getItems().clear();
            try {
                String sqlAreas = "SELECT Descripcion FROM tblt_areashospitalarias WHERE activo = TRUE ORDER BY Descripcion";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlAreas);
                     ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        cbAreaHosp.getItems().add(rs.getString("Descripcion"));
                    }
                }
            } catch (SQLException e) {
                cbAreaHosp.getItems().addAll("Medicina Interna", "Cirugía General", "Pediatría", "Urgencias");
            }

            // CARGAR MÉDICOS
            cbMedicoAlta.getItems().clear();
            String sqlMedicos = "SELECT Med_nombre FROM tb_medicos ORDER BY Med_nombre";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlMedicos);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    cbMedicoAlta.getItems().add(rs.getString("Med_nombre"));
                }
                if (cbMedicoAlta.getItems().isEmpty()) {
                    cbMedicoAlta.getItems().add("Médico General");
                }
            }

            // CARGAR OPCIONES SI/NO
            cbIndigena.getItems().addAll("SI", "NO");
            cbAfromexicano.getItems().addAll("SI", "NO");
            cbMigrante.getItems().addAll("SI", "NO");
            cbMujerFertil.getItems().addAll("SI", "NO");

            // CARGAR SITUACIÓN EMBARAZO
            cbSituacionEmbarazo.getItems().addAll("1 - Embarazo", "2 - Puerperio", "3 - No estaba embarazada");

            // CARGAR OPCIONES PARA IRA Y EDA
            cbIra.getItems().addAll("No", "Con antibiótico", "Sintomático");
            cbEda.getItems().addAll("No", "Plan A", "Plan B", "Plan C");

            log.debug("Todos los combos cargados correctamente");

        } catch (SQLException e) {
            log.error("Error cargando combos: {}", e.getMessage());
            cargarValoresPorDefecto();
        }
    }

    private void cargarValoresPorDefecto() {
        cbAltaPor.getItems().addAll("Hospitalización", "Consulta Externa", "Translado a otra unidad",
                "Domicilio [Mejoría]", "Domicilio [M.B.]", "Sale por defunción",
                "Sale por fuga", "Sale por voluntad propia");
        cbAreaHosp.getItems().addAll("Medicina Interna", "Cirugía General", "Pediatría", "Urgencias");
        cbMedicoAlta.getItems().addAll("Dr. Ejemplo 1", "Dr. Ejemplo 2");
        cbIra.getItems().addAll("No", "Con antibiótico", "Sintomático");
        cbEda.getItems().addAll("No", "Plan A", "Plan B", "Plan C");

        cbIndigena.getItems().addAll("SI", "NO");
        cbAfromexicano.getItems().addAll("SI", "NO");
        cbMigrante.getItems().addAll("SI", "NO");
        cbMujerFertil.getItems().addAll("SI", "NO");
        cbSituacionEmbarazo.getItems().addAll("1 - Embarazo", "2 - Puerperio", "3 - No estaba embarazada");
    }

    private void cargarDatosPacienteExistente() {
        String sql = "SELECT * FROM Tb_egresos WHERE Folio = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, folioPaciente);
            var rs = ps.executeQuery();

            if (rs.next()) {
                log.debug("Cargando datos existentes de egreso...");

                // AHORA SÍ cargamos lo que el médico escribió
                tfAfecc1.setText(rs.getString("Afecc_principal"));
                tfAfecc2.setText(rs.getString("Afecc_secund"));
                tfAfecc3.setText(rs.getString("Afecc_terciaria"));

                // Cargar códigos CIE10 para seleccionar en ComboBox
                String codigo1 = rs.getString("codigo_cie10_afecc1");
                String codigo2 = rs.getString("codigo_cie10_afecc2");
                String codigo3 = rs.getString("codigo_cie10_afecc3");

// Seleccionar código en ComboBox usando el nuevo método
                if (codigo1 != null) seleccionarCodigoEnComboBox(cbCodAfecc1, codigo1);
                if (codigo2 != null) seleccionarCodigoEnComboBox(cbCodAfecc2, codigo2);
                if (codigo3 != null) seleccionarCodigoEnComboBox(cbCodAfecc3, codigo3);
                // Cargar otros datos...
                tfProc1.setText(rs.getString("Procedimiento_01"));
                tfProc2.setText(rs.getString("Procedimiento_02"));
                tfProc3.setText(rs.getString("Procedimiento_03"));
                tfMedic1.setText(rs.getString("Medicamentos_01"));
                tfMedic2.setText(rs.getString("Medicamentos_02"));
                tfMedic3.setText(rs.getString("Medicamentos_03"));
                tfMedic4.setText(rs.getString("Medicamentos_04"));
                tfMedic5.setText(rs.getString("Medicamentos_05"));
                tfMedic6.setText(rs.getString("Medicamentos_06"));
                cbIra.setValue(rs.getString("Ira"));
                cbEda.setValue(rs.getString("Eda"));
                taCausaExterna.setText(rs.getString("causa_externa"));

                // Cargar nuevos campos
                cbIndigena.setValue(rs.getString("considera_indigena"));
                cbAfromexicano.setValue(rs.getString("considera_afromexicano"));
                cbMigrante.setValue(rs.getString("migrante_retornado"));
                cbMujerFertil.setValue(rs.getString("mujer_edad_fertil"));

                String situacion = rs.getString("situacion_embarazo");
                if (situacion != null) {
                    cbSituacionEmbarazo.setValue(situacion + " - " +
                            ("1".equals(situacion) ? "Embarazo" :
                                    "2".equals(situacion) ? "Puerperio" : "No estaba embarazada"));
                }

                Integer semanas = rs.getInt("semanas_gestacion");
                if (!rs.wasNull()) {
                    tfSemanasGestacion.setText(String.valueOf(semanas));
                }

                Integer sobres = rs.getInt("Sobres_VSO");
                if (!rs.wasNull()) {
                    spSobres.getValueFactory().setValue(sobres);
                }

                cbAltaPor.setValue(rs.getString("Alta_por"));
                tfFolioDef.setText(rs.getString("Folio_defuncion"));
                cbAreaHosp.setValue(rs.getString("Egreso_hosp"));
                cbMedicoAlta.setValue(rs.getString("Medico_egresa"));

                log.debug("Datos existentes cargados correctamente");
            } else {
                log.warn("No hay datos previos de egreso para este folio");
            }
        } catch (Exception e) {
            log.error("Error cargando datos existentes: {}", e.getMessage());
        }
    }


    private void seleccionarCodigoEnComboBox(ComboBox<String> comboBox, String codigoBuscado) {
        if (codigoBuscado == null || codigoBuscado.trim().isEmpty()) {
            log.warn("Código a buscar es nulo o vacío");
            return;
        }

        log.debug("Buscando código: {} en ComboBox", codigoBuscado);

        for (String item : comboBox.getItems()) {
            // item tiene formato "CODIGO|DESCRIPCION"
            if (item == null) continue;

            String[] partes = item.split("\\|", 2);
            if (partes.length > 0 && partes[0].equals(codigoBuscado)) {
                comboBox.setValue(item);
                log.debug("Encontrado y seleccionado: {}", item);
                return;
            }
        }

        // Si no encontró exacto, buscar uno que empiece con ese código
        for (String item : comboBox.getItems()) {
            if (item != null && item.startsWith(codigoBuscado + "|")) {
                comboBox.setValue(item);
                log.debug("Encontrado por inicio: {}", item);
                return;
            }
        }

        log.warn("Código no encontrado: " + codigoBuscado);
    }


    private void startDateTimeUpdater() {
        // No iniciar el timer si el usuario ya tiene permisos de edición (ya se detuvo en setUsuarioLogueado)
        boolean puedeEditar = "ADMIN".equals(rolUsuario) ||
                "JEFATURA_URGENCIAS".equals(rolUsuario) ||
                "ADMINISTRATIVO".equals(rolUsuario);

        if (puedeEditar) {
            log.debug("Saltando startDateTimeUpdater por permisos de edición.");
            return;
        }

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    lblFechaAlta.setText(dateFmt.format(now));
                    lblHoraAlta.setText(timeFmt.format(now));
                });
            }
        }, 0, 1000);
    }

    @FXML
    private void onGuardarCerrar() {
        log.debug("Intentando guardar egreso para folio: {}", folioPaciente);

        if (!validarCampos()) return;

        try {
            guardarEgreso();
            actualizarEstadoPaciente();
            mostrarAlerta("Éxito", " Egreso guardado correctamente\n\nEl paciente ha sido dado de alta del sistema.", Alert.AlertType.INFORMATION);

            Stage stage = (Stage) tfFolio.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            log.error(" Error al guardar egreso: {}", e.getMessage());
            mostrarAlerta("Error", " Error al guardar egreso: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            if (timer != null) timer.cancel();
        }
    }

    private boolean validarCampos() {
        // Validar que haya al menos un código CIE10 seleccionado
        if (cbCodAfecc1.getValue() == null &&
                cbCodAfecc2.getValue() == null &&
                cbCodAfecc3.getValue() == null) {
            mostrarAlerta("Error", "Debe seleccionar al menos un código CIE10 para diagnóstico", Alert.AlertType.ERROR);
            cbCodAfecc1.requestFocus();
            return false;
        }

        // Validar que haya al menos un código CIE10 seleccionado
        if (cbCodAfecc1.getValue() == null &&
                cbCodAfecc2.getValue() == null &&
                cbCodAfecc3.getValue() == null) {
            mostrarAlerta("Error", "Debe seleccionar al menos un código CIE10 para diagnóstico", Alert.AlertType.ERROR);
            cbCodAfecc1.requestFocus();
            return false;
        }

        // Validar que haya al menos un procedimiento
        if ((tfProc1.getText() == null || tfProc1.getText().trim().isEmpty()) &&
                (tfProc2.getText() == null || tfProc2.getText().trim().isEmpty()) &&
                (tfProc3.getText() == null || tfProc3.getText().trim().isEmpty())) {
            mostrarAlerta("Error", "Debe registrar al menos un procedimiento realizado", Alert.AlertType.ERROR);
            tfProc1.requestFocus();
            return false;
        }

        // Validar que haya al menos un medicamento
        if ((tfMedic1.getText() == null || tfMedic1.getText().trim().isEmpty()) &&
                (tfMedic2.getText() == null || tfMedic2.getText().trim().isEmpty()) &&
                (tfMedic3.getText() == null || tfMedic3.getText().trim().isEmpty()) &&
                (tfMedic4.getText() == null || tfMedic4.getText().trim().isEmpty()) &&
                (tfMedic5.getText() == null || tfMedic5.getText().trim().isEmpty()) &&
                (tfMedic6.getText() == null || tfMedic6.getText().trim().isEmpty())) {
            mostrarAlerta("Error", "Debe registrar al menos un medicamento", Alert.AlertType.ERROR);
            tfMedic1.requestFocus();
            return false;
        }

        // Validar tipo de alta
        if (cbAltaPor.getValue() == null || cbAltaPor.getValue().trim().isEmpty()) {
            mostrarAlerta("Error", "Seleccione el tipo de alta", Alert.AlertType.ERROR);
            cbAltaPor.requestFocus();
            return false;
        }

        // Validar médico que realiza el alta
        String medicoAlta = cbMedicoAlta.getValue();
        if (medicoAlta == null || medicoAlta.trim().isEmpty()) {
            mostrarAlerta("Error", "Seleccione el médico que realiza el alta", Alert.AlertType.ERROR);
            cbMedicoAlta.requestFocus();
            return false;
        }

        return true;
    }

    private String extraerCodigo(String valorCompleto) {
        if (valorCompleto == null) return null;
        String[] partes = valorCompleto.split("\\|", 2);
        return partes.length > 0 ? partes[0] : valorCompleto;
    }

    private void guardarEgreso() throws Exception {
        Integer cveAlta = obtenerCveAlta(cbAltaPor.getValue());


// Obtener los valores completos del ComboBox
        String valorCompleto1 = cbCodAfecc1.getValue();
        String valorCompleto2 = cbCodAfecc2.getValue();
        String valorCompleto3 = cbCodAfecc3.getValue();

// Extraer solo los códigos CIE10
        String codigoCie10_1 = extraerCodigo(valorCompleto1);
        String codigoCie10_2 = extraerCodigo(valorCompleto2);
        String codigoCie10_3 = extraerCodigo(valorCompleto3);

// Obtener lo que el médico ESCRIBIÓ en los TextArea
        String descripcionMedico1 = tfAfecc1.getText();  // Lo que el médico escribió
        String descripcionMedico2 = tfAfecc2.getText();  // Lo que el médico escribió
        String descripcionMedico3 = tfAfecc3.getText();  // Lo que el médico escribió

        String sql = "INSERT INTO Tb_egresos (Folio, Afecc_principal, Afecc_secund, Afecc_terciaria, " +
                "codigo_cie10_afecc1, codigo_cie10_afecc2, codigo_cie10_afecc3, " +
                "Procedimiento_01, Procedimiento_02, Procedimiento_03, " +
                "Medicamentos_01, Medicamentos_02, Medicamentos_03, Medicamentos_04, Medicamentos_05, Medicamentos_06, " +
                "Ira, Eda, Sobres_VSO, Alta_por, Fecha_alta, Hora_alta, Folio_defuncion, Egreso_hosp, Medico_egresa, " +
                "causa_externa, considera_indigena, considera_afromexicano, migrante_retornado, " +
                "mujer_edad_fertil, situacion_embarazo, semanas_gestacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "Afecc_principal=VALUES(Afecc_principal), Afecc_secund=VALUES(Afecc_secund), Afecc_terciaria=VALUES(Afecc_terciaria), " +
                "codigo_cie10_afecc1=VALUES(codigo_cie10_afecc1), codigo_cie10_afecc2=VALUES(codigo_cie10_afecc2), codigo_cie10_afecc3=VALUES(codigo_cie10_afecc3), " +
                "Procedimiento_01=VALUES(Procedimiento_01), Procedimiento_02=VALUES(Procedimiento_02), Procedimiento_03=VALUES(Procedimiento_03), " +
                "Medicamentos_01=VALUES(Medicamentos_01), Medicamentos_02=VALUES(Medicamentos_02), Medicamentos_03=VALUES(Medicamentos_03), " +
                "Medicamentos_04=VALUES(Medicamentos_04), Medicamentos_05=VALUES(Medicamentos_05), Medicamentos_06=VALUES(Medicamentos_06), " +
                "Ira=VALUES(Ira), Eda=VALUES(Eda), Sobres_VSO=VALUES(Sobres_VSO), Alta_por=VALUES(Alta_por), " +
                "Fecha_alta=VALUES(Fecha_alta), Hora_alta=VALUES(Hora_alta), Folio_defuncion=VALUES(Folio_defuncion), " +
                "Egreso_hosp=VALUES(Egreso_hosp), Medico_egresa=VALUES(Medico_egresa), " +
                "causa_externa=VALUES(causa_externa), considera_indigena=VALUES(considera_indigena), " +
                "considera_afromexicano=VALUES(considera_afromexicano), migrante_retornado=VALUES(migrante_retornado), " +
                "mujer_edad_fertil=VALUES(mujer_edad_fertil), situacion_embarazo=VALUES(situacion_embarazo), " +
                "semanas_gestacion=VALUES(semanas_gestacion)";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Setear 32 parámetros exactos
            ps.setInt(1, folioPaciente);
            ps.setString(2, emptyToNull(descripcionMedico1));  // Afecc_principal = lo que escribió el médico
            ps.setString(3, emptyToNull(descripcionMedico2));  // Afecc_secund = lo que escribió el médico
            ps.setString(4, emptyToNull(descripcionMedico3));  // Afecc_terciaria = lo que escribió el médico
            ps.setString(5, codigoCie10_1);  // codigo_cie10_afecc1
            ps.setString(6, codigoCie10_2);  // codigo_cie10_afecc2
            ps.setString(7, codigoCie10_3);  // codigo_cie10_afecc3
            ps.setString(8, emptyToNull(tfProc1.getText()));
            ps.setString(9, emptyToNull(tfProc2.getText()));
            ps.setString(10, emptyToNull(tfProc3.getText()));
            ps.setString(11, emptyToNull(tfMedic1.getText()));
            ps.setString(12, emptyToNull(tfMedic2.getText()));
            ps.setString(13, emptyToNull(tfMedic3.getText()));
            ps.setString(14, emptyToNull(tfMedic4.getText()));
            ps.setString(15, emptyToNull(tfMedic5.getText()));
            ps.setString(16, emptyToNull(tfMedic6.getText()));
            ps.setString(17, cbIra.getValue() != null ? cbIra.getValue() : "No");
            ps.setString(18, cbEda.getValue() != null ? cbEda.getValue() : "No");
            ps.setInt(19, spSobres.getValue());

            if (cveAlta != null) {
                ps.setInt(20, cveAlta);
            } else {
                ps.setInt(20, 1);
            }

            // FECHA Y HORA DE ALTA
            boolean puedeEditar = "ADMIN".equals(rolUsuario) ||
                    "JEFATURA_URGENCIAS".equals(rolUsuario) ||
                    "ADMINISTRATIVO".equals(rolUsuario);

            if (puedeEditar) {
                ps.setString(21, dpFechaAlta.getValue() != null ? dpFechaAlta.getValue().toString() : java.time.LocalDate.now().toString());
                ps.setString(22, emptyToNull(tfHoraAlta.getText()) != null ? tfHoraAlta.getText() : java.time.LocalTime.now().format(timeFmt));
            } else {
                ps.setTimestamp(21, new java.sql.Timestamp(System.currentTimeMillis())); // NOW() aprox
                ps.setTime(22, new java.sql.Time(System.currentTimeMillis())); // CURTIME() aprox
            }

            ps.setString(23, emptyToNull(tfFolioDef.getText()));
            ps.setString(24, cbAreaHosp.getValue() != null ? cbAreaHosp.getValue() : null);
            ps.setString(25, cbMedicoAlta.getValue().trim());

            // Nuevos campos
            ps.setString(26, emptyToNull(taCausaExterna.getText()));
            ps.setString(27, cbIndigena.getValue());
            ps.setString(28, cbAfromexicano.getValue());
            ps.setString(29, cbMigrante.getValue());
            ps.setString(30, cbMujerFertil.getValue());

            // Situación embarazo (solo el número)
            String situacion = cbSituacionEmbarazo.getValue();
            if (situacion != null && situacion.length() > 0) {
                ps.setString(31, situacion.substring(0, 1));
            } else {
                ps.setNull(31, Types.VARCHAR);
            }

            // Semanas gestación
            String semanasText = tfSemanasGestacion.getText();
            if (semanasText != null && !semanasText.trim().isEmpty()) {
                try {
                    ps.setInt(32, Integer.parseInt(semanasText.trim()));
                } catch (NumberFormatException e) {
                    ps.setNull(32, Types.INTEGER);
                }
            } else {
                ps.setNull(32, Types.INTEGER);
            }

            int filas = ps.executeUpdate();
            log.debug("Egreso guardado para folio: {} - Filas afectadas: {}", folioPaciente, filas);

        } catch (SQLException e) {
            log.error(" Error SQL al guardar egreso: {}", e.getMessage());
            log.error(e);
            throw e;
        }
    }

    private Integer obtenerCveAlta(String descripcionAlta) {
        if (descripcionAlta == null) return null;

        String sql = "SELECT Cve_alta FROM tblt_cvealta WHERE Descripcion = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, descripcionAlta);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("Cve_alta");
            }
        } catch (SQLException e) {
            log.error("Error obteniendo Cve_alta: {}", e.getMessage());
        }
        return null;
    }

    private void actualizarEstadoPaciente() throws Exception {
        boolean puedeEditar = "ADMIN".equals(rolUsuario) ||
                "JEFATURA_URGENCIAS".equals(rolUsuario) ||
                "ADMINISTRATIVO".equals(rolUsuario);

        String sql;
        if (puedeEditar) {
            sql = "UPDATE tb_urgencias SET Estado_pac = 3, Fecha_alta = ?, Hora_alta = ? WHERE Folio = ?";
        } else {
            sql = "UPDATE tb_urgencias SET Estado_pac = 3, Fecha_alta = NOW(), Hora_alta = CURTIME() WHERE Folio = ?";
        }

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (puedeEditar) {
                ps.setString(1, dpFechaAlta.getValue() != null ? dpFechaAlta.getValue().toString() : java.time.LocalDate.now().toString());
                ps.setString(2, emptyToNull(tfHoraAlta.getText()) != null ? tfHoraAlta.getText() : java.time.LocalTime.now().format(timeFmt));
                ps.setInt(3, folioPaciente);
            } else {
                ps.setInt(1, folioPaciente);
            }

            int filas = ps.executeUpdate();
            log.debug("Estado actualizado a 'Egresado' para folio: {} - Filas afectadas: {}", folioPaciente, filas);
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}