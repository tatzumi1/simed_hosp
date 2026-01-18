package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.utils.PDFGenerator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
public class JefaturaController {

    @FXML private TextField txtFolioNota;
    @FXML private TextField txtNumNotaMedica;
    @FXML private TextField txtNumInterconsulta;

    @FXML private TableView<PacienteEgresado> tablaEgresados;
    @FXML private TableColumn<PacienteEgresado, Integer> colFolio;
    @FXML private TableColumn<PacienteEgresado, String> colNombreCompleto;
    @FXML private TableColumn<PacienteEgresado, String> colFechaIngreso;
    @FXML private TableColumn<PacienteEgresado, Integer> colTotalNotasMedicas;
    @FXML private TableColumn<PacienteEgresado, Integer> colTotalInterconsultas;
    @FXML private TableColumn<PacienteEgresado, String> colDiagnostico;
    @FXML private TableColumn<PacienteEgresado, String> colAltaPor;
    @FXML private TableColumn<PacienteEgresado, String> colFolioDefuncion;
    @FXML private TableColumn<PacienteEgresado, String> colMedicoAlta;
    @FXML private TableColumn<PacienteEgresado, Void> colAcciones;

    @FXML private Label lblTotalEgresados;

    private ObservableList<PacienteEgresado> pacientesEgresados = FXCollections.observableArrayList();

    // ========== MAPA DE QUERIES ==========



    // ========== MAPA DE QUERIES - VERSIÓN FINAL CON ORDEN CORRECTO ==========
    private static final Map<String, String> TABLA_QUERIES = new LinkedHashMap<String, String>() {{
        // 1. TABLA URGENCIAS - TODOS LOS CAMPOS EN ORDEN
        put("tb_urgencias",
                "SELECT " +
                        // EN ORDEN DE TABLA
                        "u.Folio, " +
                        "COALESCE(ts.Ts_nombre, u.Cve_ts) AS 'Cve_ts', " +
                        "u.Nombre_ts, " +
                        "COALESCE(tu.Turno, u.Turno) AS 'Turno', " +
                        "DATE_FORMAT(u.Fecha_turno, '%d/%m/%Y %H:%i') AS 'Fecha_turno', " +
                        "DATE_FORMAT(u.Fecha, '%d/%m/%Y %H:%i') AS 'Fecha', " +
                        "TIME_FORMAT(u.Hora_registro, '%H:%i') AS 'Hora_registro', " +
                        "u.A_paterno, " +
                        "u.A_materno, " +
                        "u.Nombre, " +
                        "u.Edad, " +
                        "COALESCE(ed.Descripcion, u.Cve_edad) AS 'Cve_edad', " +
                        "DATE_FORMAT(u.F_nac, '%d/%m/%Y') AS 'F_nac', " +
                        "COALESCE(sx.Descripcion, u.Sexo) AS 'Sexo', " +
                        "u.Edo_civil, " +
                        "u.Ocupacion, " +
                        "u.Telefono, " +
                        "COALESCE(en.DESCRIP, u.Entidad_resid) AS 'Entidad_resid', " +
                        "u.Municipio_resid, " +
                        "u.Domicilio, " +
                        "COALESCE(dh.Derechohabiencia, u.Derechohabiencia) AS 'Derechohabiencia', " +
                        "u.No_afiliacion, " +
                        "u.TRIAGE, " +
                        "DATE_FORMAT(u.Fecha_atencion, '%d/%m/%Y %H:%i') AS 'Fecha_atencion', " +
                        "TIME_FORMAT(u.Hora_atencion, '%H:%i') AS 'Hora_atencion', " +
                        "u.Referencia, " +
                        "CASE WHEN u.Reingreso = 1 THEN 'SI' ELSE 'NO' END AS 'Reingreso', " +
                        "u.Sintomas, " +
                        "COALESCE(m.Ced_prof, u.Cve_med) AS 'Cve_med', " +
                        "COALESCE(m.Med_nombre, u.Nom_med) AS 'Nom_med', " +
                        "COALESCE(ep.Estado, u.Estado_pac) AS 'Estado_pac', " +
                        "u.Ingreso, " +
                        "u.Religion, " +
                        "CASE WHEN u.Hospitalizado = 1 THEN 'SI' ELSE 'NO' END AS 'Hospitalizado', " +
                        "u.Exp_clinico, " +
                        "u.Observaciones_ts, " +
                        "CASE WHEN u.Tsdatos_completos = 1 THEN 'SI' ELSE 'NO' END AS 'Tsdatos_completos', " +
                        "u.CURP, " +
                        "COALESCE(turg.Descripcion, u.Tipo_urg) AS 'Tipo_urg', " +
                        "COALESCE(mot.Descripcion, u.Motivo_urg) AS 'Motivo_urg', " +
                        "COALESCE(ca.Descripcion, u.Tipo_cama) AS 'Tipo_cama', " +
                        // CONTADORES EN TIEMPO REAL
                        "(SELECT COUNT(*) FROM tb_notas n WHERE n.Folio = u.Folio) AS 'Num_notas', " +
                        "(SELECT COUNT(*) FROM tb_inter i WHERE i.Folio = u.Folio) AS 'Num_notasinter', " +
                        // AFECCIONES (combinadas con egresos)
                        "COALESCE(e.Afecc_principal, u.Afecc_01) AS 'Afecc_01', " +
                        "COALESCE(e.Afecc_secund, u.Afecc_02, e.Afecc_secund) AS 'Afecc_02', " +
                        "COALESCE(e.Afecc_terciaria, e.Afecc_terciaria) AS 'Afecc_terciaria', " +
                        // PROCEDIMIENTOS (combinados)
                        "COALESCE(e.Procedimiento_01, u.Procedimiento_01) AS 'Procedimiento_01', " +
                        "COALESCE(e.Procedimiento_02, u.Procedimiento_02) AS 'Procedimiento_02', " +
                        "COALESCE(e.Procedimiento_03, u.Procedimiento_03) AS 'Procedimiento_03', " +
                        // INTERCONSULTAS (combinadas)
                        "(SELECT i1.Medico FROM tb_inter i1 WHERE i1.Folio = u.Folio ORDER BY i1.Fecha ASC LIMIT 0,1) AS 'Intercons_01', " +
                        "(SELECT i2.Medico FROM tb_inter i2 WHERE i2.Folio = u.Folio ORDER BY i2.Fecha ASC LIMIT 1,1) AS 'Intercons_02', " +
                        "(SELECT i3.Medico FROM tb_inter i3 WHERE i3.Folio = u.Folio ORDER BY i3.Fecha ASC LIMIT 2,1) AS 'Intercons_03', " +

                        // MEDICAMENTOS (combinados)
                        "COALESCE(e.Medicamentos_01, u.Medicamentos_01) AS 'Medicamentos_01', " +
                        "COALESCE(e.Medicamentos_02, u.Medicamentos_02) AS 'Medicamentos_02', " +
                        "COALESCE(e.Medicamentos_03, u.Medicamentos_03) AS 'Medicamentos_03', " +
                        "COALESCE(e.Medicamentos_04, e.Medicamentos_04) AS 'Medicamentos_04', " +
                        "COALESCE(e.Medicamentos_05, e.Medicamentos_05) AS 'Medicamentos_05', " +
                        "COALESCE(e.Medicamentos_06, e.Medicamentos_06) AS 'Medicamentos_06', " +
                        "COALESCE(e.Ira, u.Ira) AS 'Ira', " +
                        "COALESCE(e.Eda, u.Eda) AS 'Eda', " +
                        "COALESCE(e.Sobres_VSO, u.Sobres_rep) AS 'Sobres_rep', " +
                        // DATOS DE ALTA (combinados)
                        "COALESCE(a.Descripcion, e.Alta_por, u.Alta_por) AS 'Alta_por', " +
                        "COALESCE(e.Folio_defuncion, u.Folio_defuncion) AS 'Folio_defuncion', " +
                        "COALESCE(e.Egreso_hosp, u.A_hosp) AS 'A_hosp', " +
                        "COALESCE(DATE_FORMAT(e.Fecha_alta, '%d/%m/%Y %H:%i'), DATE_FORMAT(u.Fecha_alta, '%d/%m/%Y %H:%i')) AS 'Fecha_alta', " +
                        "COALESCE(TIME_FORMAT(e.Hora_alta, '%H:%i'), TIME_FORMAT(u.Hora_alta, '%H:%i')) AS 'Hora_alta', " +
                        "COALESCE(ma.Ced_prof, u.Cve_medalta) AS 'Cve_medalta', " +
                        "COALESCE(ma.Med_nombre, e.Medico_egresa, u.Nom_medalta) AS 'Nom_medalta', " +
                        "CASE WHEN u.Impr03 = 1 THEN 'SI' ELSE 'NO' END AS 'Impr03', " +
                        "u.Notas_verif, " +
                        // CAMPOS NUEVOS
                        "u.Entidad_completa, " +
                        "u.Municipio_completo, " +
                        // DATOS DEMOGRÁFICOS ESPECIALES (de egresos)
                        "COALESCE(e.considera_indigena, 'NO') AS 'Considera_indigena', " +
                        "COALESCE(e.considera_afromexicano, 'NO') AS 'Considera_afromexicano', " +
                        "COALESCE(e.migrante_retornado, 'NO') AS 'Migrante_retornado', " +
                        "COALESCE(e.mujer_edad_fertil, 'NO') AS 'Mujer_edad_fertil', " +
                        "CASE e.situacion_embarazo " +
                        "  WHEN '1' THEN 'EMBARAZADA' " +
                        "  WHEN '2' THEN 'PUERPERIO' " +
                        "  WHEN '3' THEN 'NO_EMBARAZADA' " +
                        "  ELSE 'NO_ESPECIFICADO' END AS 'Situacion_embarazo', " +
                        "e.semanas_gestacion AS 'Semanas_gestacion', " +
                        "e.causa_externa AS 'Causa_externa', " +
                        // CIE-10 (de egresos)
                        "COALESCE(c1.descripcion, e.codigo_cie10_afecc1) AS 'codigo_cie10_afecc1', " +
                        "COALESCE(c2.descripcion, e.codigo_cie10_afecc2) AS 'codigo_cie10_afecc2', " +
                        "COALESCE(c3.descripcion, e.codigo_cie10_afecc3) AS 'codigo_cie10_afecc3', " +
                        // FECHAS DE CONTROL
                        "DATE_FORMAT(u.fecha_creacion, '%d/%m/%Y %H:%i:%s') AS 'fecha_creacion', " +
                        "DATE_FORMAT(u.fecha_actualizacion, '%d/%m/%Y %H:%i:%s') AS 'fecha_actualizacion' " +
                        "FROM tb_urgencias u " +
                        "LEFT JOIN tb_egresos e ON u.Folio = e.Folio " +
                        "LEFT JOIN tb_tsociales ts ON u.Cve_ts = ts.Cve_ts " +
                        "LEFT JOIN tblt_cveturno tu ON u.Turno = tu.Turno " +
                        "LEFT JOIN tblt_cvesexo sx ON u.Sexo = sx.Cve_sexo " +
                        "LEFT JOIN tblt_cveedad ed ON u.Cve_edad = ed.Cve_edad " +
                        "LEFT JOIN tblt_entidad en ON u.Entidad_resid = en.EDO " +
                        "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                        "LEFT JOIN tb_medicos m ON u.Cve_med = m.Cve_med " +
                        "LEFT JOIN tblt_cveedopac ep ON u.Estado_pac = ep.id_cveedopac " +
                        "LEFT JOIN tblt_cveurg turg ON u.Tipo_urg = turg.Cve_urg " +
                        "LEFT JOIN tblt_cvemotatn mot ON u.Motivo_urg = mot.Cve_motatn " +
                        "LEFT JOIN tblt_cvecama ca ON u.Tipo_cama = ca.Cve_cama " +
                        "LEFT JOIN tblt_cvealta a ON COALESCE(e.Alta_por, u.Alta_por) = a.Cve_alta " +
                        "LEFT JOIN tb_medicos ma ON u.Cve_medalta = ma.Cve_med " +
                        "LEFT JOIN tblt_cie10 c1 ON e.codigo_cie10_afecc1 = c1.codigo " +
                        "LEFT JOIN tblt_cie10 c2 ON e.codigo_cie10_afecc2 = c2.codigo " +
                        "LEFT JOIN tblt_cie10 c3 ON e.codigo_cie10_afecc3 = c3.codigo " +
                        "ORDER BY u.Folio DESC"
        );

        // 2. TABLA EGRESOS - TODOS LOS CAMPOS EN ORDEN
        put("tb_egresos",
                "SELECT " +
                        "e.Folio, " +
                        "e.A_paterno, " +
                        "e.A_materno, " +
                        "e.Nombre, " +
                        "e.Edad, " +
                        "COALESCE(ed.Descripcion, e.Cve_edad) AS 'Cve_edad', " +
                        "DATE_FORMAT(e.Fecha_nac, '%d/%m/%Y') AS 'Fecha_nac', " +
                        "COALESCE(sx.Descripcion, e.Sexo) AS 'Sexo', " +
                        "e.Edo_civil, " +
                        "e.Ocupacion, " +
                        "e.Telefono, " +
                        "e.Domicilio, " +
                        "COALESCE(mp.DESCRIP, e.Municipio) AS 'Municipio', " +
                        "COALESCE(en.DESCRIP, e.Estado) AS 'Estado', " +
                        "e.Religion, " +
                        "e.CURP, " +
                        "COALESCE(dh.Derechohabiencia, e.Derechohabiencia) AS 'Derechohabiencia', " +
                        "e.No_afiliacion, " +
                        "e.Referencia, " +
                        "CASE WHEN e.Reingreso = 1 THEN 'SI' ELSE 'NO' END AS 'Reingreso', " +
                        "e.Sintomas, " +
                        "CASE WHEN e.Hospitalizado = 1 THEN 'SI' ELSE 'NO' END AS 'Hospitalizado', " +
                        "e.Exp_clinico, " +
                        "e.Observaciones_ts, " +
                        "DATE_FORMAT(e.Fecha, '%d/%m/%Y %H:%i') AS 'Fecha', " +
                        "TIME_FORMAT(e.Hora_registro, '%H:%i') AS 'Hora_registro', " +
                        "e.Total_notas_urg, " +
                        "e.Afecc_principal, " +
                        "e.Afecc_secund, " +
                        "e.Afecc_terciaria, " +
                        "e.Procedimiento_01, e.Procedimiento_02, e.Procedimiento_03, " +
                        "e.Total_notas_inter, " +
                        "e.Medico_intercons_1, e.Medico_intercons_2, e.Medico_intercons_3, " +
                        "e.Medicamentos_01, e.Medicamentos_02, e.Medicamentos_03, " +
                        "e.Medicamentos_04, e.Medicamentos_05, e.Medicamentos_06, " +
                        "e.Ira, e.Eda, " +
                        "e.Sobres_VSO, " +
                        "COALESCE(a.Descripcion, e.Alta_por) AS 'Alta_por', " +
                        "DATE_FORMAT(e.Fecha_alta, '%d/%m/%Y %H:%i') AS 'Fecha_alta', " +
                        "TIME_FORMAT(e.Hora_alta, '%H:%i') AS 'Hora_alta', " +
                        "e.Folio_defuncion, " +
                        "e.Egreso_hosp, " +
                        "e.Medico_egresa, " +
                        "COALESCE(e.considera_indigena, 'NO') AS 'considera_indigena', " +
                        "COALESCE(e.considera_afromexicano, 'NO') AS 'considera_afromexicano', " +
                        "COALESCE(e.migrante_retornado, 'NO') AS 'migrante_retornado', " +
                        "COALESCE(e.mujer_edad_fertil, 'NO') AS 'mujer_edad_fertil', " +
                        "CASE e.situacion_embarazo " +
                        "  WHEN '1' THEN 'EMBARAZADA' " +
                        "  WHEN '2' THEN 'PUERPERIO' " +
                        "  WHEN '3' THEN 'NO_EMBARAZADA' " +
                        "  ELSE 'NO_ESPECIFICADO' END AS 'situacion_embarazo', " +
                        "e.semanas_gestacion, " +
                        "e.causa_externa, " +
                        "COALESCE(c1.descripcion, e.codigo_cie10_afecc1) AS 'codigo_cie10_afecc1', " +
                        "COALESCE(c2.descripcion, e.codigo_cie10_afecc2) AS 'codigo_cie10_afecc2', " +
                        "COALESCE(c3.descripcion, e.codigo_cie10_afecc3) AS 'codigo_cie10_afecc3', " +
                        "DATE_FORMAT(e.fecha_creacion, '%d/%m/%Y %H:%i:%s') AS 'fecha_creacion', " +
                        "DATE_FORMAT(e.fecha_actualizacion, '%d/%m/%Y %H:%i:%s') AS 'fecha_actualizacion' " +
                        "FROM tb_egresos e " +
                        "LEFT JOIN tblt_cvesexo sx ON e.Sexo = sx.Cve_sexo " +
                        "LEFT JOIN tblt_cveedad ed ON e.Cve_edad = ed.Cve_edad " +
                        "LEFT JOIN tblt_entidad en ON e.Estado = en.EDO " +
                        "LEFT JOIN tblt_mpo mp ON e.Municipio = mp.MPO AND e.Estado = mp.EDO " +
                        "LEFT JOIN tblt_cvedhabiencia dh ON e.Derechohabiencia = dh.Cve_dh " +
                        "LEFT JOIN tblt_cvealta a ON e.Alta_por = a.Cve_alta " +
                        "LEFT JOIN tblt_cie10 c1 ON e.codigo_cie10_afecc1 = c1.codigo " +
                        "LEFT JOIN tblt_cie10 c2 ON e.codigo_cie10_afecc2 = c2.codigo " +
                        "LEFT JOIN tblt_cie10 c3 ON e.codigo_cie10_afecc3 = c3.codigo " +
                        "ORDER BY e.Fecha_alta DESC"
        );

        // 3. TABLA NOTAS - TODOS LOS CAMPOS EN ORDEN
        put("tb_notas",
                "SELECT " +
                        "n.id_nota, " +
                        "n.Folio, " +
                        "n.Num_nota, " +
                        "n.Nota, " +
                        "n.Indicaciones, " +
                        "n.sintomas, " +
                        "n.signos_vitales, " +
                        "n.diagnostico, " +
                        "COALESCE(n.mujer_edad_fertil, 'NO') AS 'mujer_edad_fertil', " +
                        "DATE_FORMAT(n.Fecha, '%d/%m/%Y %H:%i') AS 'Fecha', " +
                        "TIME_FORMAT(n.Hora, '%H:%i') AS 'Hora', " +
                        "n.Medico, " +
                        "n.Cedula, " +
                        "n.estado_paciente, " +
                        "CASE WHEN n.editable_por_medico = 1 THEN 'SI' ELSE 'NO' END AS 'editable_por_medico', " +
                        "n.permiso_edicion_otorgado_por, " +
                        "DATE_FORMAT(n.fecha_permiso_edicion, '%d/%m/%Y %H:%i') AS 'fecha_permiso_edicion', " +
                        "DATE_FORMAT(n.fecha_edicion_realizada, '%d/%m/%Y %H:%i') AS 'fecha_edicion_realizada', " +
                        "n.Estado, " +
                        "n.rol_usuario_otorga, " +
                        "DATE_FORMAT(n.fecha_creacion, '%d/%m/%Y %H:%i:%s') AS 'fecha_creacion', " +
                        "DATE_FORMAT(n.fecha_actualizacion, '%d/%m/%Y %H:%i:%s') AS 'fecha_actualizacion' " +
                        "FROM tb_notas n " +
                        "ORDER BY n.Fecha DESC"
        );

        // 4. TABLA INTERCONSULTAS - TODOS LOS CAMPOS EN ORDEN
        put("tb_inter",
                "SELECT " +
                        "i.id_inter, " +
                        "i.Folio, " +
                        "i.Num_inter, " +
                        "i.Nota, " +
                        "i.sintomas, " +
                        "i.signos_vitales, " +
                        "i.diagnostico, " +
                        "i.especialidad, " +
                        "COALESCE(i.mujer_edad_fertil, 'NO') AS 'mujer_edad_fertil', " +
                        "DATE_FORMAT(i.Fecha, '%d/%m/%Y %H:%i') AS 'Fecha', " +
                        "TIME_FORMAT(i.Hora, '%H:%i') AS 'Hora', " +
                        "i.Medico, " +
                        "i.Cedula, " +
                        "i.estado_paciente, " +
                        "CASE WHEN i.editable_por_medico = 1 THEN 'SI' ELSE 'NO' END AS 'editable_por_medico', " +
                        "i.permiso_edicion_otorgado_por, " +
                        "DATE_FORMAT(i.fecha_permiso_edicion, '%d/%m/%Y %H:%i') AS 'fecha_permiso_edicion', " +
                        "DATE_FORMAT(i.fecha_edicion_realizada, '%d/%m/%Y %H:%i') AS 'fecha_edicion_realizada', " +
                        "i.Estado, " +
                        "DATE_FORMAT(i.fecha_creacion, '%d/%m/%Y %H:%i:%s') AS 'fecha_creacion', " +
                        "DATE_FORMAT(i.fecha_actualizacion, '%d/%m/%Y %H:%i:%s') AS 'fecha_actualizacion' " +
                        "FROM tb_inter i " +
                        "ORDER BY i.Fecha DESC"
        );
    }};


    // ========== MÉTODO DE EXPORTACIÓN ==========
    @FXML
    private void handleExportarExcel() {
        log.debug("Exportando base de datos a Excel...");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Base de Datos");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        String fileName = "SIMED_Export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";
        fileChooser.setInitialFileName(fileName);

        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            // Mostrar mensaje de espera
            Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
            progressAlert.setTitle("Exportando");
            progressAlert.setHeaderText("Por favor espere...");
            progressAlert.setContentText("Exportando datos a Excel");
            progressAlert.show();

            // Ejecutar en hilo separado
            new Thread(() -> {
                try {
                    exportarBaseDatosExcel(file);

                    Platform.runLater(() -> {
                        progressAlert.close();
                        mostrarAlerta("Éxito",
                                "Base de datos exportada correctamente\n" +
                                        "Archivo: " + file.getName(),
                                Alert.AlertType.INFORMATION);
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        progressAlert.close();
                        mostrarAlerta("Error",
                                "Error al exportar: " + e.getMessage(),
                                Alert.AlertType.ERROR);
                        log.error("Error al exportar base de datos a Excel", e);
                    });
                }
            }).start();
        }
    }

    // ========== MÉTODO PARA EXPORTAR TODO ==========
    private void exportarBaseDatosExcel(File file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream outputStream = new FileOutputStream(file)) {

            log.debug("Exportando {} tablas...", TABLA_QUERIES.size());

            // Exportar cada tabla
            for (Map.Entry<String, String> entry : TABLA_QUERIES.entrySet()) {
                String sheetName = entry.getKey();
                String query = entry.getValue();

                log.debug("Exportando: {}", sheetName);
                exportarTabla(workbook, sheetName, query);
            }

            workbook.write(outputStream);
            log.debug("Exportación completada: {}", file.getAbsolutePath());

        } catch (Exception e) {
            log.error("Error al crear Excel", e);
            throw new Exception("Error al crear Excel: " + e.getMessage(), e);
        }
    }

    // ========== MÉTODO PARA EXPORTAR UNA TABLA ==========
    private void exportarTabla(Workbook workbook, String sheetName, String query) {
        Connection conn = null;

        try {
            conn = ConexionBD.getSafeConnection();

            // Nombre seguro para Excel
            String safeSheetName = sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName;
            Sheet sheet = workbook.createSheet(safeSheetName);

            try (PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Crear encabezados
                Row headerRow = sheet.createRow(0);
                for (int i = 1; i <= columnCount; i++) {
                    Cell cell = headerRow.createCell(i - 1);
                    cell.setCellValue(metaData.getColumnLabel(i));
                }

                // Llenar datos
                int rowNum = 1;
                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);

                    for (int col = 1; col <= columnCount; col++) {
                        Cell cell = row.createCell(col - 1);
                        Object value = rs.getObject(col);

                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }

                // Autoajustar columnas
                for (int i = 0; i < columnCount; i++) {
                    sheet.autoSizeColumn(i);
                }

                log.debug("  {}: {} filas", sheetName, rowNum - 1);

            }

        } catch (Exception e) {
            log.error("Error en tabla {}: {}", sheetName, e.getMessage());

        } finally {
            ConexionBD.safeClose(conn);
            log.debug("Conexión cerrada");
        }
    }



    // ========== TUS MÉTODOS ORIGINALES ==========

    // Clase para representar pacientes egresados
    @Getter
    public static class PacienteEgresado {
        private final int folio;
        private final String nombreCompleto;
        private final String fechaIngreso;
        private final int totalNotasMedicas;
        private final int totalInterconsultas;
        private final String diagnostico;
        private final String altaPor;
        private final String folioDefuncion;
        private final String medicoAlta;

        public PacienteEgresado(int folio, String nombreCompleto, String fechaIngreso,
                                int totalNotasMedicas, int totalInterconsultas,
                                String diagnostico, String altaPor, String folioDefuncion, String medicoAlta) {
            this.folio = folio;
            this.nombreCompleto = nombreCompleto;
            this.fechaIngreso = fechaIngreso;
            this.totalNotasMedicas = totalNotasMedicas;
            this.totalInterconsultas = totalInterconsultas;
            this.diagnostico = diagnostico;
            this.altaPor = altaPor;
            this.folioDefuncion = folioDefuncion;
            this.medicoAlta = medicoAlta;
        }

    }

    @FXML
    public void initialize() {
        log.debug("Módulo de Jefatura inicializado");
        configurarColumnas();
        cargarPacientesEgresados();
    }

    private void configurarColumnas() {
        colFolio.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getFolio()).asObject());
        colNombreCompleto.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombreCompleto()));
        colFechaIngreso.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFechaIngreso()));
        colTotalNotasMedicas.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getTotalNotasMedicas()).asObject());
        colTotalInterconsultas.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getTotalInterconsultas()).asObject());
        colDiagnostico.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDiagnostico()));
        colAltaPor.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAltaPor()));
        colFolioDefuncion.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFolioDefuncion()));
        colMedicoAlta.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMedicoAlta()));
        configurarColumnaAcciones();
    }

    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnObservacion = new Button("A Observación");

            {
                btnObservacion.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
                btnObservacion.setOnAction(event -> {
                    PacienteEgresado paciente = getTableView().getItems().get(getIndex());
                    handleRegresarAObservacion(paciente);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnObservacion);
                }
            }
        });
    }

    private void handleRegresarAObservacion(PacienteEgresado paciente) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Acción");
        confirmacion.setHeaderText("Regresar paciente a observación");
        confirmacion.setContentText("¿Está seguro de que desea regresar al paciente " + paciente.getNombreCompleto() + " a observación? " +
                "Esto eliminará los datos de su egreso actual.");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ejecutarRegresoAObservacion(paciente);
            }
        });
    }

    private void ejecutarRegresoAObservacion(PacienteEgresado paciente) {
        String updateUrgencias = "UPDATE tb_urgencias SET " +
                "Estado_pac = 2, " +
                "Alta_por = NULL, " +
                "Fecha_alta = NULL, " +
                "Hora_alta = NULL, " +
                "Cve_medalta = NULL, " +
                "Nom_medalta = NULL, " +
                "Folio_defuncion = NULL, " +
                "A_hosp = NULL " +
                "WHERE Folio = ?";

        String deleteEgreso = "DELETE FROM tb_egresos WHERE Folio = ?";

        Connection conn = null;
        try {
            conn = ConexionBD.getSafeConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psUpdate = conn.prepareStatement(updateUrgencias);
                 PreparedStatement psDelete = conn.prepareStatement(deleteEgreso)) {

                psUpdate.setInt(1, paciente.getFolio());
                psUpdate.executeUpdate();

                psDelete.setInt(1, paciente.getFolio());
                psDelete.executeUpdate();

                conn.commit();
                mostrarAlerta("Éxito", "El paciente ha sido regresado a observación correctamente.", Alert.AlertType.INFORMATION);
                cargarPacientesEgresados();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            log.error("Error al regresar paciente a observación: {}", e.getMessage());
            mostrarAlerta("Error", "No se pudo completar la acción: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            ConexionBD.safeClose(conn);
        }
    }

     // ================================================================0
    // puro expor exel arriba osea no tocar abajo
// ========================================================================
    @FXML
    private void handleVisualizarNota() {
        String folioTexto = txtFolioNota.getText().trim();
        String numNotaTexto = txtNumNotaMedica.getText().trim();
        String numInterTexto = txtNumInterconsulta.getText().trim();

        if (folioTexto.isEmpty()) {
            mostrarAlerta("Error", "Por favor ingrese el folio del paciente", Alert.AlertType.WARNING);
            return;
        }

        try {
            int folio = Integer.parseInt(folioTexto);

            // Buscar nota médica si se proporcionó número
            if (!numNotaTexto.isEmpty()) {
                int numNota = Integer.parseInt(numNotaTexto);
                buscarNotaMedica(folio, numNota);
            }

            // Buscar interconsulta si se proporcionó número
            if (!numInterTexto.isEmpty()) {
                int numInter = Integer.parseInt(numInterTexto);
                buscarInterconsulta(folio, numInter);
            }

            if (numNotaTexto.isEmpty() && numInterTexto.isEmpty()) {
                mostrarAlerta("Información", "Ingrese al menos un número de nota médica o interconsulta", Alert.AlertType.INFORMATION);
            }

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Los números deben ser valores numéricos válidos", Alert.AlertType.ERROR);
        }
    }

    private void buscarNotaMedica(int folio, int numNota) {
        String sql = "SELECT " +
                "n.*, " +
                "u.Nombre, u.A_paterno, u.A_materno, u.Edad, u.Sexo, u.F_nac, " +
                "u.Edo_civil, u.Ocupacion, u.Domicilio, u.Derechohabiencia, " +
                "u.Exp_clinico, u.Referencia, u.CURP, " +
                "s.Descripcion as SexoDesc, " +
                "dh.Derechohabiencia as DerechohabienciaDesc, " +
                "m.Med_nombre as NombreMedico, " +
                "m.Ced_prof as CedulaMedico " +
                "FROM tb_notas n " +
                "JOIN tb_urgencias u ON n.Folio = u.Folio " +
                "LEFT JOIN tblt_cvesexo s ON u.Sexo = s.Cve_sexo " +
                "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                "LEFT JOIN tb_medicos m ON n.Cedula = m.Ced_prof " +
                "WHERE n.Folio = ? AND n.Num_nota = ?";

        Connection conn = null;
        try {
            conn = ConexionBD.getSafeConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folio);
                pstmt.setInt(2, numNota);

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    generarPDFNotaMedica(rs, numNota);
                } else {
                    mostrarAlerta("No encontrado", "No se encontró la nota médica especificada", Alert.AlertType.WARNING);
                }
            }
        } catch (SQLException e) {
            log.error("Error buscando nota médica: {}", e.getMessage());
            mostrarAlerta("Error", "Error al buscar la nota médica: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            ConexionBD.safeClose(conn);
            log.debug("Conexión cerrada");
        }
    }

    private void buscarInterconsulta(int folio, int numInter) {
        String sql = "SELECT " +
                "i.*, " +
                "u.Nombre, u.A_paterno, u.A_materno, u.Edad, u.Sexo, u.F_nac, " +
                "u.Edo_civil, u.Ocupacion, u.Domicilio, u.Derechohabiencia, " +
                "u.Exp_clinico, u.Referencia, u.CURP, " +
                "s.Descripcion as SexoDesc, " +
                "dh.Derechohabiencia as DerechohabienciaDesc, " +
                "me.Nombre as NombreEspecialista, " +
                "me.Cedula as CedulaEspecialista " +
                "FROM tb_inter i " +
                "JOIN tb_urgencias u ON i.Folio = u.Folio " +
                "LEFT JOIN tblt_cvesexo s ON u.Sexo = s.Cve_sexo " +
                "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                "LEFT JOIN tb_medesp me ON i.Cedula = me.Cedula " +
                "WHERE i.Folio = ? AND i.Num_inter = ?";

        Connection conn = null;
        try {
            conn = ConexionBD.getSafeConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folio);
                pstmt.setInt(2, numInter);

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    generarPDFInterconsulta(rs, numInter);
                } else {
                    mostrarAlerta("No encontrado", "No se encontró la interconsulta especificada", Alert.AlertType.WARNING);
                }
            }
        } catch (SQLException e) {
            log.error("Error buscando interconsulta: {}", e.getMessage());
            mostrarAlerta("Error", "Error al buscar la interconsulta: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            ConexionBD.safeClose(conn);
            log.debug("Conexión cerrada");
        }
    }

    private void generarPDFNotaMedica(ResultSet rs, int numeroNota) throws SQLException {
        try {
            // Obtener el folio del paciente
            int folioPaciente = rs.getInt("Folio");

            // Llamar al PDFGenerator NUEVO (solo necesita folio y número de nota)
            boolean exito = PDFGenerator.generarNotaMedicaPDF(folioPaciente, numeroNota);

            if (!exito) {
                mostrarAlerta("Error", "No se pudo generar el PDF de la nota médica", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            log.error(" Error al generar PDF de nota médica: {}", e.getMessage());
            log.error("StackTrace: ", e);
            mostrarAlerta("Error", "Error al generar el PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void generarPDFInterconsulta(ResultSet rs, int numeroInterconsulta) throws SQLException {
        try {
            // Obtener el folio del paciente
            int folioPaciente = rs.getInt("Folio");

            // Llamar al PDFGenerator NUEVO (solo necesita folio y número de interconsulta)
            boolean exito = PDFGenerator.generarInterconsultaPDF(folioPaciente, numeroInterconsulta);

            if (!exito) {
                mostrarAlerta("Error", "No se pudo generar el PDF de la interconsulta", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            log.error(" Error al generar PDF de interconsulta: {}", e.getMessage());
            log.error("StackTrace: ", e);
            mostrarAlerta("Error", "Error al generar el PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String obtenerUniversidadEspecialista(String nombreEspecialista) {
        String universidad = "";

        if (nombreEspecialista == null || nombreEspecialista.trim().isEmpty()) {
            return universidad;
        }

        String sql = "SELECT universidad FROM tb_medesp WHERE Nombre = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombreEspecialista.trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                universidad = rs.getString("universidad");
                if (universidad != null) {
                    log.debug(" Universidad encontrada para {}: {}", nombreEspecialista, universidad);
                }
            } else {
                log.warn(" Especialista no encontrado en tb_medesp: {}", nombreEspecialista);
            }

        } catch (SQLException e) {
            log.error(" Error obteniendo universidad: {}", e.getMessage());
        }

        return universidad != null ? universidad : "";
    }

    private void cargarPacientesEgresados() {
        String sql = "SELECT " +
                "COALESCE(e.Folio, u.Folio) as Folio, " +

                // NOMBRE COMPLETO - CORREGIDO
                "CASE " +
                "  WHEN COALESCE(e.Nombre, u.Nombre) IS NOT NULL THEN " +
                "    TRIM(CONCAT_WS(' ', " +
                "      COALESCE(e.A_paterno, u.A_paterno, ''), " +
                "      COALESCE(e.A_materno, u.A_materno, ''), " +
                "      COALESCE(e.Nombre, u.Nombre, '')" +
                "    )) " +
                "  ELSE 'Nombre no disponible' " +
                "END as NombreCompleto, " +

                // FECHA INGRESO
                "DATE_FORMAT(COALESCE(u.Fecha, e.Fecha), '%d/%m/%Y %H:%i') as FechaIngreso, " +

                // CONTAR NOTAS MÉDICAS
                "(SELECT COUNT(*) FROM tb_notas n WHERE n.Folio = COALESCE(e.Folio, u.Folio)) as TotalNotasMedicas, " +

                // CONTAR INTERCONSULTAS
                "(SELECT COUNT(*) FROM tb_inter i WHERE i.Folio = COALESCE(e.Folio, u.Folio)) as TotalInterconsultas, " +

                // DIAGNÓSTICO (prioridad: egresos, luego urgencias)
                "COALESCE(" +
                "  e.Afecc_principal, " +
                "  e.Afecc_secund, " +
                "  e.Afecc_terciaria, " +
                "  u.Afecc_01, " +
                "  u.Afecc_02, " +
                "  'No especificado'" +
                ") as Diagnostico, " +

                // ALTA POR (con descripción del catálogo)
                "COALESCE(a.Descripcion, 'Sin especificar') as AltaPor, " +

                // FOLIO DEFUNCIÓN (de egresos primero)
                "COALESCE(e.Folio_defuncion, u.Folio_defuncion, 'No aplica') as FolioDefuncion, " +

                // MÉDICO ALTA (de egresos primero)
                "COALESCE(e.Medico_egresa, u.Nom_medalta, 'No especificado') as MedicoAlta " +

                "FROM tb_egresos e " +
                "LEFT JOIN tb_urgencias u ON e.Folio = u.Folio " +
                "LEFT JOIN tblt_cvealta a ON e.Alta_por = a.Cve_alta " +
                "WHERE e.Fecha_alta IS NOT NULL " +
                "ORDER BY e.Fecha_alta DESC " +
                "LIMIT 200";

        Connection conn = null;
        try {
            conn = ConexionBD.getSafeConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                pacientesEgresados.clear();

                while (rs.next()) {
                    PacienteEgresado paciente = new PacienteEgresado(
                            rs.getInt("Folio"),
                            rs.getString("NombreCompleto"),
                            rs.getString("FechaIngreso"),
                            rs.getInt("TotalNotasMedicas"),
                            rs.getInt("TotalInterconsultas"),
                            rs.getString("Diagnostico"),
                            rs.getString("AltaPor"),
                            rs.getString("FolioDefuncion"),
                            rs.getString("MedicoAlta")
                    );
                    pacientesEgresados.add(paciente);
                }

                tablaEgresados.setItems(pacientesEgresados);
                lblTotalEgresados.setText("Total de pacientes egresados: " + pacientesEgresados.size());

                log.debug("Pacientes egresados cargados: {}", pacientesEgresados.size());

                // DEBUG: Mostrar primeros 5 nombres para verificar
                log.debug("Primeros 5 nombres:");
                for (int i = 0; i < Math.min(5, pacientesEgresados.size()); i++) {
                    log.debug("{}. {}", i + 1, pacientesEgresados.get(i).getNombreCompleto());
                }

            }
        } catch (SQLException e) {
            log.error("Error cargando pacientes egresados: {}", e.getMessage());
            mostrarAlerta("Error", "No se pudieron cargar los pacientes egresados", Alert.AlertType.ERROR);
        } finally {
            ConexionBD.safeClose(conn);
            log.debug("Conexión cerrada");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}