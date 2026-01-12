package com.PruebaSimed2.utils;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.pdf.*;
import com.PruebaSimed2.database.ConexionBD;
import com.itextpdf.kernel.pdf.*;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Log4j2
public class PDFGeneratorDiario {
    private static final String TEMPLATE_PATH = "pdf_templates/SEUL-16-P_2024.pdf";
    private static final String TEMP_DIR = "temp_pdfs/";

    // CLUES fijo
    private static final String CLUES_FIJO = "VZIMB003766";

    public static InputStream generarPDFDiario(String fechaStr, boolean imprimir) {
        try {
            // Validar fecha
            LocalDate fecha = LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            // Obtener folios egresados
            List<Integer> folios = obtenerFoliosEgresadosPorFecha(fecha);

            if (folios.isEmpty()) {
                log.info(" No hay egresos para la fecha: {}", fechaStr);
                return null;
            }

            log.debug(" Encontrados {} egresos para {}", folios.size(), fechaStr);

            // Crear PDF final en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (PdfWriter writer = new PdfWriter(baos);
                 PdfDocument pdfDoc = new PdfDocument(writer)) {

                // Procesar cada folio
                for (int i = 0; i < folios.size(); i++) {
                    int folio = folios.get(i);
                    log.debug("\n Procesando folio {}/{}: {}", i + 1, folios.size(), folio);

                    // Generar página individual
                    byte[] paginaIndividual = generarPaginaIndividual(folio);

                    if (paginaIndividual != null) {
                        // Usar PdfPageFormCopier
                        try (PdfReader paginaReader = new PdfReader(new ByteArrayInputStream(paginaIndividual));
                             PdfDocument paginaDoc = new PdfDocument(paginaReader)) {

                            // Crear el copiador
                            PdfPageFormCopier copier = new PdfPageFormCopier();

                            // Copiar la página
                            paginaDoc.copyPagesTo(1, paginaDoc.getNumberOfPages(), pdfDoc, copier);
                        }
                    }
                }

                log.debug("\n PDF generado en memoria!");
                log.debug(" Total de páginas: {}", pdfDoc.getNumberOfPages());

                PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
                if (form != null) {
                    form.flattenFields();
                    log.debug(" Campos del formulario convertidos a texto");
                }
            }
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            log.error(" Error al generar PDF: {}", e.getMessage(), e);
            return null;
        }
    }

    private static class PdfPageFormCopier implements IPdfPageExtraCopier {
        @Override
        public void copy(PdfPage fromPage, PdfPage toPage) {
        }
    }

    private static byte[] generarPaginaIndividual(int folio) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 1. Obtener datos del paciente
            Map<String, String> datos = obtenerDatosPacienteCompletos(folio);

            if (datos.isEmpty()) {
                log.warn("   No hay datos para el folio: {}", folio);
                return null;
            }

            log.debug("   Obtenidos {} campos para folio {}", datos.size(), folio);

            // 2. Crear documento
            InputStream templateStream = PDFGeneratorDiario.class.getClassLoader()
                    .getResourceAsStream(TEMPLATE_PATH);

            if (templateStream == null) {
                log.error("No se encontró el template PDF: " + TEMPLATE_PATH);
                throw new RuntimeException("No se encontró el template PDF: " + TEMPLATE_PATH);
            }

            PdfDocument pdfDoc = new PdfDocument(
                    new PdfReader(templateStream),
                    new PdfWriter(baos)
            );

            // 3. Obtener formulario
            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);

            // 4. Rellenar campos
            int camposLlenados = 0;
            int camposNoEncontrados = 0;

            for (Map.Entry<String, String> entry : datos.entrySet()) {
                String nombreCampo = entry.getKey();
                String valor = entry.getValue();

                if (valor == null || valor.trim().isEmpty()) {
                    continue;
                }

                PdfFormField campo = form.getField(nombreCampo);
                if (campo != null) {
                    campo.setValue(valor);
                    camposLlenados++;
                } else {
                    camposNoEncontrados++;
                }
            }

            // 5. Cerrar documento
            pdfDoc.close();

            log.debug("   Campos llenados: {}", camposLlenados);
            if (camposNoEncontrados > 0) {
                log.debug("   Campos no encontrados: {}", camposNoEncontrados);
            }

            // En lugar de verificar campos no encontrados complejo, haz esto:
// Agrega esta línea después de "Campos no encontrados: X"
            log.debug("   Ejemplos de campos mapeados (primeros 5):");
            int count = 0;
            for (Map.Entry<String, String> entry : datos.entrySet()) {
                if (count < 5) {
                    log.debug("    - PDF: \"{}\" → Valor: \"{}\"", entry.getKey(), entry.getValue());
                    count++;
                }
            }
            return baos.toByteArray();

        } catch (Exception e) {
            log.error(" Error al generar página para folio {}: {}", folio, e.getMessage(), e);
            return null;
        }
    }


    private static Map<String, String> obtenerDatosPacienteCompletos(int folio) {
        Map<String, String> datos = new HashMap<>();

        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT " +
                    "u.Folio, u.Nombre, u.A_paterno, u.A_materno, u.CURP, u.F_nac, u.Edad, u.Cve_edad, " +
                    "u.Sexo, sexo.Descripcion AS sexo_desc, " +
                    "u.Telefono, u.Domicilio, u.Entidad_completa, u.Municipio_completo, " +
                    "u.Derechohabiencia, dh.Derechohabiencia AS dh_desc, u.No_afiliacion, " +
                    "u.Fecha AS fecha_ingreso, u.Hora_registro, " +
                    "u.Tipo_urg, u.Motivo_urg, u.Tipo_cama, " +
                    "e.Procedimiento_01, e.Procedimiento_02, e.Procedimiento_03, " +
                    "e.Afecc_principal, e.Afecc_secund, e.Afecc_terciaria, " +
                    "e.codigo_cie10_afecc1, e.codigo_cie10_afecc2, e.codigo_cie10_afecc3, " +
                    "e.Medicamentos_01, e.Medicamentos_02, e.Medicamentos_03, " +
                    "e.Medicamentos_04, e.Medicamentos_05, e.Medicamentos_06, " +
                    "e.Ira, e.Eda, e.Sobres_VSO, " +
                    "e.Alta_por, e.Fecha_alta, e.Hora_alta, e.Folio_defuncion, " +
                    "e.Medico_egresa, " +
                    "e.considera_indigena, e.considera_afromexicano, e.migrante_retornado, " +
                    "e.mujer_edad_fertil, e.situacion_embarazo, e.semanas_gestacion, e.causa_externa " +
                    "FROM tb_urgencias u " +
                    "LEFT JOIN tb_egresos e ON u.Folio = e.Folio " +
                    "LEFT JOIN tblt_cvesexo sexo ON u.Sexo = sexo.Cve_sexo " +
                    "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                    "LEFT JOIN tblt_cveedad edad ON u.Cve_edad = edad.Cve_edad " +
                    "WHERE u.Folio = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folio);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {

                        // LIMPIEZA DE CHECKBOXES
                        String[] checkboxes = {
                                "SEXO: hombre", "sexo:mujer",
                                "GRATUITIDAD: si", "gratuitidad:no",
                                "SE CONSIDERA AFROMEXICANO: si",
                                "TIPO DE URGENCIA: urgencia calificada", "urgencia no calificada",
                                "MOTIVO DE ATENCION: accidente, envenenamiento y violencia", "medica", "gineco-obstetrica", "pediatrica",
                                "TIPO DE CAMA: cama de observacion", "cama de choque", "sin cama",
                                "MUJER EN EDAD FERTIL: embarazo", "mujer en edad fertil:puerperio", "mujer en edad fertil:no estaba embarazada ni en puerperio",
                                "se considera indigena: si", "se considera indigena:no",
                                "ES MIGRANTE RETORNADO: si", "ES MIGRANTE RETORNADO: no",
                                "IRAS: 1 sintomatico", "IRAS: 2 con antibiotico", "antivirales",
                                "EDAS: plan A"
                        };
                        for (String cb : checkboxes) {
                            datos.put(cb, "");
                        }

                        // Limpieza de campos de texto
                        for (int i = 1; i <= 6; i++) {
                            datos.put("AFECCIONES TRATADAS,COMORBILIDADES:" + i, "");
                            datos.put("código CIE " + i, "");
                            datos.put("PROCEDIMIENTOS:" + i, "");
                            datos.put("MEDICAMENTOS SUMINISTRADOS: " + i, "");
                        }

                        // DATOS BÁSICOS
                        datos.put("folio", String.valueOf(folio));

                        String nombreCompleto = String.format("%s %s %s",
                                getValorSeguro(rs.getString("A_paterno")),
                                getValorSeguro(rs.getString("A_materno")),
                                getValorSeguro(rs.getString("Nombre"))).trim();
                        datos.put("NOMBRE: nombre, primer apellido,segundo apellido", nombreCompleto);

                        datos.put("curp", getValorSeguro(rs.getString("CURP")));

                        if (rs.getDate("F_nac") != null) {
                            datos.put("FECHA DE NACIMIENTO", new SimpleDateFormat("dd/MM/yyyy").format(rs.getDate("F_nac")));
                        }

                        // EDAD - VOLVEMOS A TU VERSIÓN ORIGINAL QUE FUNCIONABA
                        String edadStr = getValorSeguro(rs.getString("Edad"));
                        if (edadStr != null && !edadStr.isEmpty()) {
                            datos.put("edad cumplida: años ( niños y adultos normales)", edadStr);
                            // Si usabas otros campos, aquí los puedes poner, pero en tu original solo usabas el de años
                        }

                        // SEXO
                        String sexoDesc = getValorSeguro(rs.getString("sexo_desc"));
                        if ("Masculino".equalsIgnoreCase(sexoDesc)) {
                            datos.put("SEXO: hombre", "/Yes");
                        } else if ("Femenino".equalsIgnoreCase(sexoDesc)) {
                            datos.put("sexo:mujer", "/Yes");
                        }

                        datos.put("TELEFONO:", getValorSeguro(rs.getString("Telefono")));

                        datos.put("entidad o pais de nacimiento", getValorSeguro(rs.getString("Entidad_completa")));
                        datos.put("ENTIDAD FEDERATIVA,PAIS:", getValorSeguro(rs.getString("Entidad_completa")));
                        datos.put("MUNICIPIO O ALCALDIA:", getValorSeguro(rs.getString("Municipio_completo")));
                        datos.put("LOCALIDAD:", getValorSeguro(rs.getString("Domicilio")));

                        // AFILIACIÓN
                        String dhDesc = getValorSeguro(rs.getString("dh_desc"));
                        if (dhDesc.contains("IMSS")) datos.put("AfiliacionServiciosSalud:imss", "/Yes");
                        else if (dhDesc.contains("ISSSTE")) datos.put("afiliacion a los servicios de salud: isste", "/Yes");
                        else if (dhDesc.contains("PEMEX")) datos.put("afiliacion a los servicios de salud: pemex", "/Yes");
                        else if (dhDesc.contains("SEDENA")) datos.put("AfiliacionServiciosSalud:sedena", "/Yes");
                        else if (dhDesc.contains("SEMAR")) datos.put("afiliacion a los servicios de salud:semar", "/Yes");
                        else if (dhDesc.contains("BIENESTAR") || dhDesc.contains("Seguro Popular")) datos.put("afiliacion a los servicios de salud:imss bienestar", "/Yes");
                        else if (dhDesc.contains("SIN") || dhDesc.contains("Sin seguridad")) datos.put("AfiliacionServiciosSalud:ninguna", "/Yes");
                        else datos.put("afiliacion a los servicios de salud:otra", "/Yes");

                        datos.put("NUMERO AFILIACION:", getValorSeguro(rs.getString("No_afiliacion")));

                        if (dhDesc.contains("SIN") || dhDesc.contains("Sin seguridad")) {
                            datos.put("GRATUITIDAD: si", "/Yes");
                        } else {
                            datos.put("gratuitidad:no", "/Yes");
                        }

                        // FECHA Y HORA DE INGRESO
                        if (rs.getTimestamp("fecha_ingreso") != null) {
                            LocalDateTime ingreso = rs.getTimestamp("fecha_ingreso").toLocalDateTime();
                            datos.put("FECHA Y HORA DE INGRESO", ingreso.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                            datos.put("HORA DE INGRESO HH", String.format("%02d", ingreso.getHour()));
                        }

                        // FECHA Y HORA DE ALTA TAL CUAL
                        if (rs.getTimestamp("Fecha_alta") != null) {
                            String fechaAlta = new SimpleDateFormat("dd/MM/yyyy").format(rs.getTimestamp("Fecha_alta"));
                            String horaAlta = getValorSeguro(rs.getString("Hora_alta"));
                            if (horaAlta.length() >= 5) horaAlta = horaAlta.substring(0, 5);
                            datos.put("FECHA Y HORA DE ALTA ", fechaAlta + " " + horaAlta);
                        }

                        // TIPO URGENCIA, MOTIVO, CAMA
                        int tipoUrg = rs.getInt("Tipo_urg");
                        if (tipoUrg == 1) datos.put("TIPO DE URGENCIA: urgencia calificada", "/Yes");
                        else if (tipoUrg == 2) datos.put("urgencia no calificada", "/Yes");

                        int motivo = rs.getInt("Motivo_urg");
                        if (motivo == 1) datos.put("MOTIVO DE ATENCION: accidente, envenenamiento y violencia", "/Yes");
                        else if (motivo == 2) datos.put("medica", "/Yes");
                        else if (motivo == 3) datos.put("gineco-obstetrica", "/Yes");
                        else if (motivo == 4) datos.put("pediatrica", "/Yes");

                        int cama = rs.getInt("Tipo_cama");
                        if (cama == 1) datos.put("TIPO DE CAMA: cama de observacion", "/Yes");
                        else if (cama == 2) datos.put("cama de choque", "/Yes");
                        else if (cama == 3) datos.put("sin cama", "/Yes");

                        // AFECCIONES, CIE, PROCEDIMIENTOS, MEDICAMENTOS
                        datos.put("AFECCIONES TRATADAS,COMORBILIDADES:1", getValorSeguro(rs.getString("Afecc_principal")));
                        datos.put("AFECCIONES TRATADAS,COMORBILIDADES:2", getValorSeguro(rs.getString("Afecc_secund")));
                        datos.put("AFECCIONES TRATADAS,COMORBILIDADES:3", getValorSeguro(rs.getString("Afecc_terciaria")));

                        datos.put("código CIE 1", getValorSeguro(rs.getString("codigo_cie10_afecc1")));
                        datos.put("código CIE 2", getValorSeguro(rs.getString("codigo_cie10_afecc2")));
                        datos.put("código CIE 3", getValorSeguro(rs.getString("codigo_cie10_afecc3")));

                        datos.put("PROCEDIMIENTOS:1", getValorSeguro(rs.getString("Procedimiento_01")));
                        datos.put("PROCEDIMIENTOS:2", getValorSeguro(rs.getString("Procedimiento_02")));
                        datos.put("PROCEDIMIENTOS:3", getValorSeguro(rs.getString("Procedimiento_03")));

                        datos.put("MEDICAMENTOS SUMINISTRADOS: 1", getValorSeguro(rs.getString("Medicamentos_01")));
                        datos.put("MEDICAMENTOS SUMINISTRADOS: 2", getValorSeguro(rs.getString("Medicamentos_02")));
                        datos.put("MEDICAMENTOS SUMINISTRADOS: 3", getValorSeguro(rs.getString("Medicamentos_03")));
                        datos.put("MEDICAMENTOS SUMINISTRADOS: 4", getValorSeguro(rs.getString("Medicamentos_04")));
                        datos.put("MEDICAMENTOS SUMINISTRADOS: 5", getValorSeguro(rs.getString("Medicamentos_05")));
                        datos.put("MEDICAMENTOS SUMINISTRADOS: 6", getValorSeguro(rs.getString("Medicamentos_06")));

                        // IRAs
                        String ira = getValorSeguro(rs.getString("Ira"));
                        if (ira.toLowerCase().contains("sintomatico") || ira.toLowerCase().contains("sintomático")) {
                            datos.put("IRAS: 1 sintomatico", "/Yes");
                        } else if (ira.toLowerCase().contains("antibiótico") || ira.toLowerCase().contains("antibiotico")) {
                            datos.put("IRAS: 2 con antibiotico", "/Yes");
                        } else if (ira.toLowerCase().contains("antiviral")) {
                            datos.put("antivirales", "/Yes");
                        }

                        // EDAs - solo hay checkbox para Plan A
                        String eda = getValorSeguro(rs.getString("Eda"));
                        if (eda != null && eda.trim().equalsIgnoreCase("Plan A")) {
                            datos.put("EDAS: plan A", "/Yes");
                        }

                        datos.put("NUMERO DE SOBRES sueros", getValorSeguro(rs.getString("Sobres_VSO")));

                        // TIPO DE ALTA
                        int altaPor = rs.getInt("Alta_por");
                        if (!rs.wasNull()) {
                            switch (altaPor) {
                                case 1 -> datos.put("ALTA POR(ENVIADO A): hospitalizacion", "/Yes");
                                case 2 -> datos.put("ALTA POR(ENVIADO A):consulta externa", "/Yes");
                                case 3 -> datos.put("ALTA POR(ENVIADO A):traslado a otra unidad", "/Yes");
                                case 4, 5 -> datos.put("ALTA POR(ENVIADO A):domicilio", "/Yes");
                                case 6 -> datos.put("ALTA POR(ENVIADO A): defuncion", "/Yes");
                                case 7 -> datos.put("ALTA POR(ENVIADO A): fuga", "/Yes");
                                case 8 -> datos.put("ALTA POR(ENVIADO A): voluntad propia", "/Yes");
                            }
                        }

                        datos.put("folio del certificadode defuncion", getValorSeguro(rs.getString("Folio_defuncion")));

                        // MÉDICO
                        String medico = getValorSeguro(rs.getString("Medico_egresa"));
                        datos.put("profesional de la salud responsable: NOMBRE: nombre(s), primer apellido, segundo apellido", medico);

                        String cedula = obtenerCedulaMedico(medico, conn);
                        datos.put("cedula profecional del prpfesional de la salud responsable", cedula);

                        // IDENTIDAD ÉTNICA
                        String indigena = getValorSeguro(rs.getString("considera_indigena"));
                        if ("SI".equalsIgnoreCase(indigena)) {
                            datos.put("se considera indigena: si", "/Yes");
                        } else {
                            datos.put("se considera indigena:no", "/Yes");
                        }

                        String afromex = getValorSeguro(rs.getString("considera_afromexicano"));
                        if ("SI".equalsIgnoreCase(afromex)) {
                            datos.put("SE CONSIDERA AFROMEXICANO: si", "/Yes");
                        }

                        String migrante = getValorSeguro(rs.getString("migrante_retornado"));
                        if ("SI".equalsIgnoreCase(migrante)) {
                            datos.put("ES MIGRANTE RETORNADO: si", "/Yes");
                        } else {
                            datos.put("ES MIGRANTE RETORNADO: no", "/Yes");
                        }

                        // MUJER EDAD FÉRTIL
                        if ("SI".equalsIgnoreCase(getValorSeguro(rs.getString("mujer_edad_fertil")))) {
                            int situacion = rs.getInt("situacion_embarazo");
                            if (situacion == 1) datos.put("MUJER EN EDAD FERTIL: embarazo", "/Yes");
                            else if (situacion == 2) datos.put("mujer en edad fertil:puerperio", "/Yes");
                            else if (situacion == 3) datos.put("mujer en edad fertil:no estaba embarazada ni en puerperio", "/Yes");

                            datos.put("semanas de gestacion", getValorSeguro(rs.getString("semanas_gestacion")));
                        }

                        // CAUSA EXTERNA
                        String causa = getValorSeguro(rs.getString("causa_externa"));
                        datos.put("text_163pfix", causa);
                        datos.put("CAUSA EXTERNA (Especifique los acontecimientos)", causa);

                        // INTERCONSULTAS
                        List<Map<String, String>> inters = obtenerInterconsultas(folio, conn);
                        for (int i = 0; i < Math.min(inters.size(), 3); i++) {
                            Map<String, String> inter = inters.get(i);
                            int num = i + 1;
                            datos.put("INTERCONSULTA, ESPECIALIDAD " + num, getValorSeguro(inter.get("especialidad")));
                            datos.put("INTERCONSULTA, MEDICO INTERCONSULTANTE " + num + ": nombre,primer apellido,segundo apellido", getValorSeguro(inter.get("medico")));
                            if (num == 1) {
                                datos.put("INTERCONSULTA, cedula 1:", getValorSeguro(inter.get("cedula")));
                            } else if (num == 2) {
                                datos.put("INTERCONSULTA, cedula 2:", getValorSeguro(inter.get("cedula")));
                            } else if (num == 3) {
                                datos.put("INTERCONSULTA, Cedula 2:", getValorSeguro(inter.get("cedula")));
                            }
                        }

                        // CLUES
                        datos.put("CLUES:Edo del hospital", "VZIMB003766");

                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al obtener datos para folio {}: {}", folio, e.getMessage(), e);
        }

        return datos;
    }



    private static String obtenerDescripcionAlta(int cveAlta, Connection conn) {
        // Ya la tenías, solo por si acaso
        try (PreparedStatement ps = conn.prepareStatement("SELECT Descripcion FROM tblt_cvealta WHERE Cve_alta = ?")) {
            ps.setInt(1, cveAlta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("Descripcion");
            }
        } catch (Exception e) { log.error("Stacktrace: ", e); }
        return "";
    }



    private static String obtenerTipoAlta(Integer cveAlta, Connection conn) {
        if (cveAlta == null) {
            return "";
        }

        try {
            String sql = "SELECT Descripcion FROM tblt_cvealta " +
                    "WHERE Cve_alta = ? " +
                    "LIMIT 1";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, cveAlta);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return getValorSeguro(rs.getString("Descripcion"));
                    }
                }
            }
        } catch (Exception e) {
            log.error(" Error al obtener tipo de alta: {}", e.getMessage());
        }

        return "";
    }

    private static List<Map<String, String>> obtenerInterconsultas(int folio, Connection conn) {
        List<Map<String, String>> interconsultas = new ArrayList<>();

        try {
            String sql = "SELECT i.Medico, i.Cedula, i.especialidad " +
                    "FROM tb_inter i " +
                    "WHERE i.Folio = ? " +
                    "ORDER BY i.Fecha, i.Hora " +
                    "LIMIT 3";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folio);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> inter = new HashMap<>();
                        inter.put("medico", getValorSeguro(rs.getString("Medico")));
                        inter.put("cedula", getValorSeguro(rs.getString("Cedula")));
                        inter.put("especialidad", getValorSeguro(rs.getString("especialidad")));
                        interconsultas.add(inter);
                    }
                }
            }

            log.debug("   Interconsultas encontradas: {}", interconsultas.size());

        } catch (Exception e) {
            log.error(" Error al obtener interconsultas: {}", e.getMessage());
        }

        return interconsultas;
    }

    private static String obtenerCedulaMedico(String nombreMedico, Connection conn) {
        if (nombreMedico == null || nombreMedico.trim().isEmpty()) {
            return "";
        }

        try {
            // Buscar en tb_medicos
            String sql = "SELECT Ced_prof FROM tb_medicos " +
                    "WHERE Med_nombre LIKE ? " +
                    "LIMIT 1";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + nombreMedico + "%");

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return getValorSeguro(rs.getString("Ced_prof"));
                    }
                }
            }
        } catch (Exception e) {
            log.error(" Error al obtener cédula: {}", e.getMessage());
        }

        return "";
    }


    private static List<Integer> obtenerFoliosEgresadosPorFecha(LocalDate fecha) {
        List<Integer> folios = new ArrayList<>();

        try {
            String sql = "SELECT DISTINCT e.Folio, e.Fecha_alta " +
                    "FROM tb_egresos e " +
                    "WHERE DATE(e.Fecha_alta) = ? " +
                    "ORDER BY e.Fecha_alta";

            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setDate(1, java.sql.Date.valueOf(fecha));

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        folios.add(rs.getInt("Folio"));
                    }
                }
            }

            log.debug(" Folios encontrados para {}: {}", fecha, folios.size());

        } catch (Exception e) {
            log.error(" Error al obtener folios: {}", e.getMessage(), e);
        }

        return folios;
    }

    private static String getValorSeguro(Object valor) {
        if (valor == null) return "";
        String str = valor.toString().trim();
        return str.equals("null") ? "" : str;
    }

    public static boolean abrirPDF(InputStream pdfStream) {
        try {
            // Guardar temporalmente para abrir
            File tempFile = File.createTempFile("egresos_", ".pdf");
            tempFile.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = pdfStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            // Reiniciar el stream
            pdfStream.reset();
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                desktop.open(tempFile);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error(" Error al abrir PDF: {}", e.getMessage());
            return false;
        }
    }

    public static File guardarPDF(InputStream pdfStream, String nombreArchivo) {
        try {
            File archivo = new File(nombreArchivo);
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = pdfStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            log.debug(" PDF guardado en: {}", archivo.getAbsolutePath());
            return archivo;
        } catch (Exception e) {
            log.error("Error al guardar PDF: {}", e.getMessage());
            return null;
        }
    }

    // ========== NUEVOS MÉTODOS PARA IMPRIMIR POR FOLIOS ==========

    public static List<Integer> obtenerFoliosEgresadosFecha(String fechaStr) {
        List<Integer> folios = new ArrayList<>();

        try {
            LocalDate fecha = LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            folios = obtenerFoliosEgresadosPorFecha(fecha);

            // Ordenar por hora de alta
            if (!folios.isEmpty()) {
                folios = ordenarFoliosPorHoraAlta(folios, fecha);
            }

        } catch (Exception e) {
            log.error(" Error al obtener folios: {}", e.getMessage(), e);
        }

        return folios;
    }

    private static List<Integer> ordenarFoliosPorHoraAlta(List<Integer> folios, LocalDate fecha) {
        try (Connection conn = ConexionBD.conectar()) {
            // Consulta para obtener folios con hora de alta
            String sql = "SELECT e.Folio, e.Hora_alta " +
                    "FROM tb_egresos e " +
                    "WHERE DATE(e.Fecha_alta) = ? " +
                    "AND e.Folio IN (" +
                    String.join(",", Collections.nCopies(folios.size(), "?")) +
                    ") " +
                    "ORDER BY e.Hora_alta ASC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, java.sql.Date.valueOf(fecha));

                for (int i = 0; i < folios.size(); i++) {
                    pstmt.setInt(i + 2, folios.get(i));
                }

                List<Integer> foliosOrdenados = new ArrayList<>();
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        foliosOrdenados.add(rs.getInt("Folio"));
                    }
                }

                return foliosOrdenados;
            }
        } catch (Exception e) {
            log.error(" Error al ordenar folios: {}", e.getMessage());
            return folios; // Devuelve la lista original si hay error
        }
    }

    public static InputStream generarPDFPorFolios(List<Integer> folios, String fechaStr, boolean paraImprimir) {
        try {
            if (folios.isEmpty()) {
                log.warn(" No hay folios para generar PDF");
                return null;
            }

            log.debug(" Generando PDF para {} folios: {}", folios.size(), folios);

            // Crear PDF final en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (PdfWriter writer = new PdfWriter(baos);
                 PdfDocument pdfDoc = new PdfDocument(writer)) {

                // Procesar cada folio
                for (int i = 0; i < folios.size(); i++) {
                    int folio = folios.get(i);
                    log.debug("\n Procesando folio {}/{}: {}", i + 1, folios.size(), folio);

                    // Generar página individual
                    byte[] paginaIndividual = generarPaginaIndividual(folio);

                    if (paginaIndividual != null) {
                        // Usar PdfPageFormCopier
                        try (PdfReader paginaReader = new PdfReader(new ByteArrayInputStream(paginaIndividual));
                             PdfDocument paginaDoc = new PdfDocument(paginaReader)) {

                            // Crear el copiador
                            PdfPageFormCopier copier = new PdfPageFormCopier();

                            // Copiar la página
                            paginaDoc.copyPagesTo(1, paginaDoc.getNumberOfPages(), pdfDoc, copier);
                        }
                    }
                }

                log.debug("\n PDF generado en memoria!");
                log.debug("   Total de páginas: {}", pdfDoc.getNumberOfPages());

                PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
                if (form != null) {
                    form.flattenFields();
                    log.debug(" Campos del formulario convertidos a texto");
                }
            }
            return new ByteArrayInputStream(baos.toByteArray());

        } catch (Exception e) {
            log.error(" Error al generar PDF por folios: {}", e.getMessage(), e);
            return null;
        }
    }

    public static InputStream generarPDFPorRango(List<Integer> todosFoliosDelDia, int desde, int hasta, String fechaStr, boolean paraImprimir) {
        try {
            // Validar índices
            if (desde < 1 || hasta > todosFoliosDelDia.size() || desde > hasta) {
                log.error(" Rango inválido: desde={}, hasta={}, total={}", desde, hasta, todosFoliosDelDia.size());
                return null;
            }

            // Convertir índices (1-based) a índices de lista (0-based)
            List<Integer> foliosRango = new ArrayList<>();
            for (int i = desde - 1; i < hasta; i++) {
                foliosRango.add(todosFoliosDelDia.get(i));
            }

            log.debug(" Generando PDF para rango [{}-{}]", desde, hasta);
            log.debug("   Folios en rango: {}", foliosRango);

            return generarPDFPorFolios(foliosRango, fechaStr, paraImprimir);

        } catch (Exception e) {
            log.error(" Error al generar PDF por rango: {}", e.getMessage(), e);
            return null;
        }
    }

    public static InputStream generarPDFParaUnFolio(int folioEspecifico, String fechaStr, boolean paraImprimir) {
        try {
            // Verificar que el folio exista y sea de ese día
            boolean existe = verificarFolioDelDia(folioEspecifico, fechaStr);

            if (!existe) {
                log.debug(" Folio {} no encontrado para la fecha {}", folioEspecifico, fechaStr);
                return null;
            }

            List<Integer> folioList = new ArrayList<>();
            folioList.add(folioEspecifico);

            log.debug(" Generando PDF para folio único: {}", folioEspecifico);

            return generarPDFPorFolios(folioList, fechaStr, paraImprimir);

        } catch (Exception e) {
            log.error(" Error al generar PDF para folio único: {}", e.getMessage(), e);
            return null;
        }
    }

    private static boolean verificarFolioDelDia(int folio, String fechaStr) {
        try {
            LocalDate fecha = LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            String sql = "SELECT COUNT(*) as existe FROM tb_egresos " +
                    "WHERE Folio = ? " +
                    "AND DATE(Fecha_alta) = ?";

            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, folio);
                pstmt.setDate(2, java.sql.Date.valueOf(fecha));

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("existe") > 0;
                    }
                }
            }
        } catch (Exception e) {
            log.error(" Error al verificar folio: {}", e.getMessage());
        }

        return false;
    }
}