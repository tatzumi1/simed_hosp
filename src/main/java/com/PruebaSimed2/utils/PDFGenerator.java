// src/main/java/com/PruebaSimed2/PDFGenerator.java

package com.PruebaSimed2.utils;
import com.PruebaSimed2.database.ConexionBD;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.element.LineSeparator;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Log4j2
public class PDFGenerator {

    // =============== NOTA MÉDICA - CON DATOS REALES ===============
    public static boolean generarNotaMedicaPDF(int folioPaciente, int numeroNota) {
        String tempFilePath = null;
        Connection conn = null;
        try {
            conn = ConexionBD.conectar();

            // ===== DATOS PACIENTE =====
            String queryPaciente = "SELECT u.A_paterno, u.A_materno, u.Nombre, u.Edad, " +
                    "s.Descripcion as Sexo, u.F_nac, u.Edo_civil, u.Ocupacion, u.Telefono, u.Domicilio, " +
                    "dh.Derechohabiencia, u.Referencia, u.Exp_clinico, u.CURP " +
                    "FROM tb_urgencias u " +
                    "LEFT JOIN tblt_cvesexo s ON u.Sexo = s.Cve_sexo " +
                    "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                    "WHERE u.Folio = ?";
            PreparedStatement psPaciente = conn.prepareStatement(queryPaciente);
            psPaciente.setInt(1, folioPaciente);
            ResultSet rsPaciente = psPaciente.executeQuery();
            if (!rsPaciente.next()) return false;

            String nombrePaciente = rsPaciente.getString("A_paterno") + " " +
                    rsPaciente.getString("A_materno") + " " +
                    rsPaciente.getString("Nombre");

            // ===== DATOS NOTA =====
            String queryNota = "SELECT n.Nota, n.Indicaciones, n.sintomas, n.signos_vitales, n.diagnostico, " +
                    "n.Fecha, n.Hora, m.Med_nombre as Medico, m.Ced_prof as Cedula " +
                    "FROM tb_notas n LEFT JOIN tb_medicos m ON n.Cedula = m.Ced_prof " +
                    "WHERE n.Folio = ? AND n.Num_nota = ?";
            PreparedStatement psNota = conn.prepareStatement(queryNota);
            psNota.setInt(1, folioPaciente);
            psNota.setInt(2, numeroNota);
            ResultSet rsNota = psNota.executeQuery();
            if (!rsNota.next()) return false;

            // ===== CREAR PDF =====
            tempFilePath = System.getProperty("java.io.tmpdir") + "/Nota_Medica_Folio_" + folioPaciente +
                    "_Nota_" + numeroNota + "_" + System.currentTimeMillis() + ".pdf";

            PdfWriter writer = new PdfWriter(tempFilePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(75, 50, 80, 50); // Más arriba

            PdfFont normal = PdfFontFactory.createFont("Helvetica");
            PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");

            agregarEncabezadoConLogos(pdfDoc);
            agregarPiePaginaNotaMedica(pdfDoc, rsNota.getString("Medico"), rsNota.getString("Cedula"));

            // ===== DATOS DEL PACIENTE - TODAS LAS ETIQUETAS EN NEGRITA =====
            // ===== DATOS DEL PACIENTE - CON MEJOR ESPACIADO HORIZONTAL =====
            Paragraph datosPaciente = new Paragraph()
                    .setFont(normal)
                    .setFontSize(10)
                    .setMultipliedLeading(1.05f)  // Mantiene el interlineado vertical compacto que te gusta
                    .setMarginBottom(12)
                    .setTextAlignment(TextAlignment.LEFT);

// Función para agregar etiqueta en negrita + valor con buen espacio
            java.util.function.BiConsumer<String, String> add = (etiqueta, valor) -> {
                datosPaciente.add(new com.itextpdf.layout.element.Text(etiqueta).setFont(bold));
                datosPaciente.add(new com.itextpdf.layout.element.Text(valor + "    ").setFont(normal));  // 4 espacios para separar bien
            };

// Línea 1
            add.accept("MÓDULO: URGENCIAS", "");
            add.accept("                  FECHA: ", formatearFecha(rsNota.getString("Fecha")));  // o rsInter en interconsulta
            add.accept("                  HORA: ", formatearHora(rsNota.getString("Hora")) + "\n");

// Línea 2
            add.accept("DERECHOHABIENCIA: ", safeString(rsPaciente.getString("Derechohabiencia")));
            add.accept("                          EXPEDIENTE URGENCIA: ", String.valueOf(folioPaciente) + "\n");

// Línea 3
            add.accept("NOMBRE: ", safeString(nombrePaciente));
            add.accept("       EDAD: ", safeString(rsPaciente.getString("Edad")) + " Años");
            add.accept("       SEXO: ", safeString(rsPaciente.getString("Sexo")) + "\n");

// Línea 4
            add.accept("FECHA DE NACIMIENTO: ", formatearFecha(rsPaciente.getString("F_nac")));
            add.accept("                ESTADO CIVIL: ", safeString(rsPaciente.getString("Edo_civil")) + "\n");

// Línea 5
            add.accept("OCUPACIÓN: ", safeString(rsPaciente.getString("Ocupacion")));
            add.accept("                                         TELÉFONO: ", safeString(rsPaciente.getString("Telefono")) + "\n");

// Línea 6
            add.accept("DOMICILIO: ", safeString(rsPaciente.getString("Domicilio")) + "\n");

// Línea 7
            add.accept("REFERENCIA: ", safeString(rsPaciente.getString("Referencia")));
            add.accept("                             EXP. CLÍNICO No.: ", safeString(rsPaciente.getString("Exp_clinico")) + "\n");

// Línea 8
            add.accept("CURP: ", safeString(rsPaciente.getString("CURP")));
            add.accept("                          NOTA MÉDICA: #", String.valueOf(numeroNota));  // o INTERCONSULTA en el otro método
            datosPaciente.add("\n");

            document.add(datosPaciente);

            // ===== CONTENIDO DE LA NOTA =====
            Paragraph contenido = new Paragraph()
                    .setFont(normal)
                    .setFontSize(10)
                    .setMultipliedLeading(1.15f)
                    .setTextAlignment(TextAlignment.JUSTIFIED);

            java.util.function.BiConsumer<String, String> addSeccion = (titulo, texto) -> {
                if (!isEmpty(texto)) {
                    boolean negritaTotal = titulo.equals("SIGNOS VITALES:");
                    contenido.add(new com.itextpdf.layout.element.Text(titulo + " ").setFont(negritaTotal ? bold : normal));
                    contenido.add(new com.itextpdf.layout.element.Text(texto + "\n\n").setFont(negritaTotal ? bold : normal));
                }
            };

            // ORDEN de impresion del pdf
            addSeccion.accept("PRESENTACIÓN DEL PACIENTE:", rsNota.getString("Nota"));
            addSeccion.accept("SÍNTOMAS:", rsNota.getString("sintomas"));
            addSeccion.accept("SIGNOS VITALES:", rsNota.getString("signos_vitales"));
            addSeccion.accept("ANÁLISIS/DIAGNÓSTICO:", rsNota.getString("diagnostico"));
            addSeccion.accept("PLAN/INDICACIONES:", rsNota.getString("Indicaciones"));

            document.add(contenido);
            document.close();

            // Cerrar todo
            rsNota.close(); psNota.close();
            rsPaciente.close(); psPaciente.close();
            conn.close();

            abrirPDF(tempFilePath);
            return true;

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            if (tempFilePath != null) eliminarArchivoTemporal(tempFilePath);
            return false;
        }
    }

    // =============== INTERCONSULTA - CON DATOS REALES ===============
    public static boolean generarInterconsultaPDF(int folioPaciente, int numeroInterconsulta) {
        String tempFilePath = null;
        Connection conn = null;
        try {
            conn = ConexionBD.conectar();

            // ===== 1. DATOS DEL PACIENTE =====
            String queryPaciente =
                    "SELECT u.A_paterno, u.A_materno, u.Nombre, u.Edad, " +
                            " s.Descripcion as Sexo, u.F_nac, u.Edo_civil, " +
                            " u.Ocupacion, u.Telefono, u.Domicilio, " +
                            " dh.Derechohabiencia, u.No_afiliacion, u.Referencia, " +
                            " u.Exp_clinico, u.CURP " +
                            "FROM tb_urgencias u " +
                            "LEFT JOIN tblt_cvesexo s ON u.Sexo = s.Cve_sexo " +
                            "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                            "WHERE u.Folio = ?";
            PreparedStatement psPaciente = conn.prepareStatement(queryPaciente);
            psPaciente.setInt(1, folioPaciente);
            ResultSet rsPaciente = psPaciente.executeQuery();
            if (!rsPaciente.next()) {
                log.warn("No se encontró el paciente con folio: {}", folioPaciente);
                return false;
            }

            String nombrePaciente = rsPaciente.getString("A_paterno") + " " +
                    rsPaciente.getString("A_materno") + " " +
                    rsPaciente.getString("Nombre");

            // ===== 2. DATOS DE LA INTERCONSULTA =====
            String queryInter =
                    "SELECT i.Nota, i.sintomas, i.signos_vitales, i.diagnostico, i.especialidad, " +
                            " i.Fecha, i.Hora, i.Num_inter, " +
                            " m.Nombre as Especialista, m.Cedula " +
                            "FROM tb_inter i " +
                            "LEFT JOIN tb_medesp m ON i.Cedula = m.Cedula " +
                            "WHERE i.Folio = ? AND i.Num_inter = ?";
            PreparedStatement psInter = conn.prepareStatement(queryInter);
            psInter.setInt(1, folioPaciente);
            psInter.setInt(2, numeroInterconsulta);
            ResultSet rsInter = psInter.executeQuery();
            if (!rsInter.next()) {
                log.warn("No se encontró la interconsulta {} para folio: {}", numeroInterconsulta, folioPaciente);
                return false;
            }

            // ===== 3. CREAR PDF =====
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            tempFilePath = tempDir.getAbsolutePath() + "/Interconsulta_Folio_" + folioPaciente +
                    "_Inter_" + numeroInterconsulta + "_" + System.currentTimeMillis() + ".pdf";

            PdfWriter writer = new PdfWriter(tempFilePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Márgenes más compactos para subir el contenido
            document.setMargins(75, 50, 80, 50);

            PdfFont normal = PdfFontFactory.createFont("Helvetica");
            PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");

            agregarEncabezadoConLogos(pdfDoc);
            agregarPiePaginaInterconsulta(pdfDoc,
                    rsInter.getString("Especialista"),
                    rsInter.getString("Cedula"),
                    rsInter.getString("especialidad"),
                    ""); // si tienes universidad, agrégala aquí

            // ===== DATOS DEL PACIENTE - TODAS LAS ETIQUETAS EN NEGRITA =====
            Paragraph datosPaciente = new Paragraph()
                    .setFont(normal)
                    .setFontSize(10)
                    .setMultipliedLeading(1.05f)  // Interlineado muy cerrado
                    .setMarginBottom(12)
                    .setTextAlignment(TextAlignment.LEFT);

            // Función auxiliar para etiqueta en negrita + valor en normal
            java.util.function.BiConsumer<String, String> add = (etiqueta, valor) -> {
                datosPaciente.add(new com.itextpdf.layout.element.Text(etiqueta).setFont(bold));
                datosPaciente.add(new com.itextpdf.layout.element.Text(valor).setFont(normal));
            };

            // Línea 1
            add.accept("MÓDULO: URGENCIAS ",          "FECHA: " + formatearFecha(rsInter.getString("Fecha")));
            add.accept("         HORA: ", formatearHora(rsInter.getString("Hora")) + "\n");

            // Línea 2
            add.accept("DERECHOHABIENCIA: ", safeString(rsPaciente.getString("Derechohabiencia")));
            add.accept("                          EXPEDIENTE URGENCIA: ", String.valueOf(folioPaciente) + "\n");

            // Línea 3
            add.accept("NOMBRE: ", safeString(nombrePaciente));
            add.accept("       EDAD: ", safeString(rsPaciente.getString("Edad")) + " Años");
            add.accept("       SEXO: ", safeString(rsPaciente.getString("Sexo")) + "\n");

            // Línea 4
            add.accept("FECHA DE NACIMIENTO: ", formatearFecha(rsPaciente.getString("F_nac")));
            add.accept("                ESTADO CIVIL: ", safeString(rsPaciente.getString("Edo_civil")) + "\n");

            // Línea 5
            add.accept("OCUPACIÓN: ", safeString(rsPaciente.getString("Ocupacion")));
            add.accept("                                        TELÉFONO: ", safeString(rsPaciente.getString("Telefono")) + "\n");

            // Línea 6
            add.accept("DOMICILIO: ", safeString(rsPaciente.getString("Domicilio")) + "\n");

            // Línea 7
            add.accept("REFERENCIA: ", safeString(rsPaciente.getString("Referencia")));
            add.accept("                       EXP. CLÍNICO No.: ", safeString(rsPaciente.getString("Exp_clinico")) + "\n");

            // Línea 8
           // add.accept("CURP: ", safeString(rsPaciente.getString("CURP")));
         //   add.accept(" INTERCONSULTA: #", String.valueOf(numeroInterconsulta) + "\n");

            add.accept("CURP: ", safeString(rsPaciente.getString("CURP")));
            add.accept("                         INTERCONSULTA: #", String.valueOf(numeroInterconsulta));
            datosPaciente.add("\n");

            document.add(datosPaciente);

            // ===== CONTENIDO DE LA INTERCONSULTA =====
            Paragraph contenido = new Paragraph()
                    .setFont(normal)
                    .setFontSize(10)
                    .setMultipliedLeading(1.15f)
                    .setTextAlignment(TextAlignment.JUSTIFIED);

            java.util.function.BiConsumer<String, String> addSeccion = (titulo, texto) -> {
                if (!isEmpty(texto)) {
                    boolean todoNegrita = titulo.equals("SIGNOS VITALES:");
                    contenido.add(new com.itextpdf.layout.element.Text(titulo + " ").setFont(todoNegrita ? bold : normal));
                    contenido.add(new com.itextpdf.layout.element.Text(texto + "\n\n").setFont(todoNegrita ? bold : normal));
                }
            };

            addSeccion.accept("SÍNTOMAS:", rsInter.getString("sintomas"));
            addSeccion.accept("SIGNOS VITALES:", rsInter.getString("signos_vitales"));  // Todo en negrita
            addSeccion.accept("DIAGNÓSTICO:", rsInter.getString("diagnostico"));
            addSeccion.accept("INDICACIONES:", rsInter.getString("Nota"));  // En interconsulta la "Nota" son las indicaciones

            document.add(contenido);
            document.close();

            // Cerrar recursos
            rsInter.close();
            psInter.close();
            rsPaciente.close();
            psPaciente.close();
            conn.close();

            log.debug("PDF de interconsulta generado: {}", tempFilePath);
            abrirPDF(tempFilePath);
            return true;

        } catch (Exception e) {
            log.error("Error generando PDF de interconsulta: {}", e.getMessage(), e);
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            if (tempFilePath != null) eliminarArchivoTemporal(tempFilePath);
            return false;
        }
    }

    private static void agregarEncabezadoConLogos(PdfDocument pdfDoc) {
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            PdfPage page = ((PdfDocumentEvent) event).getPage();
            Rectangle pageSize = page.getPageSize();  // ← aquí estaba el error

            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT imagen FROM tb_logos_encabezado WHERE activo = 1 ORDER BY orden ASC");
                 ResultSet rs = ps.executeQuery()) {

                // Canvas de iText (no el de JavaFX)
                PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
                com.itextpdf.layout.Canvas canvas = new com.itextpdf.layout.Canvas(pdfCanvas, pageSize);

                float y = pageSize.getTop() - 68;           // altura del encabezado
                float xInicio =35;
                float separacion = 0;                    // espacio entre logos
                float anchoMax = 530;
                float altoMax = 80;

                int posicion = 0;

                while (rs.next() && posicion < 10) {
                    byte[] bytesImagen = rs.getBytes("imagen");
                    if (bytesImagen != null && bytesImagen.length > 0) {
                        ImageData imageData = ImageDataFactory.create(bytesImagen);
                        Image logo = new Image(imageData);
                        logo.scaleToFit(anchoMax, altoMax);
                        logo.setFixedPosition(xInicio + (posicion * separacion), y);
                        canvas.add(logo);
                        posicion++;
                    }
                }

                canvas.close();

            } catch (Exception e) {
                log.error("Error cargando logos desde BD: {}", e.getMessage(), e);
            }
        });
    }

    // =============== PIE NOTA MÉDICA (PERFECTO, SIN CORTES) ===============
    private static void agregarPiePaginaNotaMedica(PdfDocument pdfDoc, String medico, String cedula) {
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            PdfPage page = ((PdfDocumentEvent) event).getPage();
            Rectangle ps = page.getPageSize();
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDoc);

            String texto = "MÉDICO: " + safeString(medico) + "     |     CÉDULA PROFESIONAL: " + safeString(cedula);

            Paragraph p = new Paragraph(texto)
                    .setFontSize(9.5f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFixedPosition(50, 35, ps.getWidth() - 100); // márgenes seguros

            new Canvas(canvas, ps).add(p).close();
        });
    }

    // =============== PIE INTERCONSULTA (AHORA SÍ CABE TODO, SE VE HERMOSO) ===============
    private static void agregarPiePaginaInterconsulta(PdfDocument pdfDoc, String especialista, String cedula,
                                                      String especialidad, String universidad) {
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            PdfPage page = ((PdfDocumentEvent) event).getPage();
            Rectangle ps = page.getPageSize();
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDoc);

            StringBuilder sb = new StringBuilder();
            sb.append("ESPECIALISTA: ").append(safeString(especialista));

            if (!isEmpty(cedula))      sb.append("     |     CÉDULA: ").append(cedula);
            if (!isEmpty(especialidad)) sb.append("     |     ESPECIALIDAD: ").append(especialidad);
            if (!isEmpty(universidad))  sb.append("     |     UNIVERSIDAD: ").append(universidad);

            Paragraph p = new Paragraph(sb.toString())
                    .setFontSize(9f)  // un poquito más pequeño para que quepa todo
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFixedPosition(50, 35, ps.getWidth() - 100)
                    .setMultipliedLeading(0.9f); // reduce espacio entre líneas si se parte

            new Canvas(canvas, ps).add(p).close();
        });
    }

    // =============== CARGAR IMÁGENES ===============
    private static ImageData cargarImagenDesdeResources(String resourcePath) {
        try (InputStream is = PDFGenerator.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Warning: No encontrado: {}", resourcePath);
                return null;
            }
            return ImageDataFactory.create(is.readAllBytes());
        } catch (Exception e) {
            log.error("Error cargando imagen: {}", resourcePath);
            return null;
        }
    }

    // =============== TUS MÉTODOS AUXILIARES (intactos) ===============
    private static void abrirPDF(String filePath) {
        try {
            File pdfFile = new File(filePath);
            if (pdfFile.exists()) {
                programarEliminacionArchivo(filePath, 120000);
                java.awt.Desktop.getDesktop().open(pdfFile);
                log.debug("PDF abierto: {}", filePath);
            }
        } catch (Exception e) {
            log.error("Error abriendo PDF: {}", e.getMessage());
            eliminarArchivoTemporal(filePath);
        }
    }

    private static void programarEliminacionArchivo(String filePath, long delayMillis) {
        new Thread(() -> {
            try { Thread.sleep(delayMillis); eliminarArchivoTemporal(filePath); }
            catch (InterruptedException ignored) {}
        }).start();
    }

    private static void eliminarArchivoTemporal(String filePath) {
        try {
            File tempFile = new File(filePath);
            if (tempFile.exists() && tempFile.delete()) {
                log.debug("PDF temporal eliminado: {}", filePath);
            }
        } catch (Exception ignored) {}
    }

    private static String safeString(String texto) {
        if (texto == null || texto.trim().isEmpty()) return "No especificado";
        // SOLUCIÓN AL & : reemplazamos & por &amp; solo si no está ya escapado
        return texto.trim().replace("&", "&amp;").replace("&amp;amp;", "&amp;");
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String formatearFecha(String fechaBD) {
        if (fechaBD == null || fechaBD.trim().isEmpty()) return "No especificado";
        try {
            // Si viene con hora (2025-11-27 00:00:00), quitamos la hora
            if (fechaBD.contains(" ")) {
                fechaBD = fechaBD.split(" ")[0];
            }
            String[] partes = fechaBD.split("-");
            return partes[2] + "/" + partes[1] + "/" + partes[0]; // DD/MM/YYYY
        } catch (Exception e) {
            return fechaBD; // si falla, devuelve tal cual
        }
    }

    private static String formatearHora(String horaBD) {
        if (horaBD == null || horaBD.trim().isEmpty()) return "No especificado";
        // Si viene con milisegundos, los quitamos
        if (horaBD.contains(".")) {
            horaBD = horaBD.split("\\.")[0];
        }
        return horaBD.trim();
    }
    //  buenos pues que te digo :v

}