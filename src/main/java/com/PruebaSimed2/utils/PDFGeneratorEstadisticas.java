package com.PruebaSimed2.utils;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.AreaBreakType;
import com.PruebaSimed2.controllers.ModuloEstadisticaController.EstadisticaMedico;
import lombok.extern.log4j.Log4j2;

import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Desktop;
import java.io.File;

@Log4j2
public class PDFGeneratorEstadisticas {

    public static boolean generarPDF(List<EstadisticaMedico> estadisticas, String mes, int año, String tipo, boolean esParaImprimir) {
        try {
            String fileName;
            String subtitulo;
            String columnaCantidad;
            String textoResumen;

            // Configurar según el tipo
            switch (tipo) {
                case "Ingresos":
                    fileName = "Relacion_Ingresos_" + mes + "_" + año;
                    subtitulo = "INGRESO DE PACIENTES REALIZADO EN EL SERVICIO";
                    columnaCantidad = "N° de Ingresos";
                    textoResumen = String.format("Resumen: %d usuarios realizaron %d ingresos en total",
                            estadisticas.size(), estadisticas.stream().mapToInt(EstadisticaMedico::getCantidad).sum());
                    break;

                case "Egresos":
                    fileName = "Relacion_Egresos_" + mes + "_" + año;
                    subtitulo = "EGRESO DE PACIENTES REALIZADO EN EL SERVICIO";
                    columnaCantidad = "N° de Egresos";
                    textoResumen = String.format("Resumen: %d médicos realizaron %d egresos en total",
                            estadisticas.size(), estadisticas.stream().mapToInt(EstadisticaMedico::getCantidad).sum());
                    break;

                case "Notas médicas":
                    fileName = "Relacion_Notas_Medicas_" + mes + "_" + año;
                    subtitulo = "NOTAS MÉDICAS ELABORADAS EN LA ATENCIÓN AL PACIENTE";
                    columnaCantidad = "N° de Notas Médicas";
                    textoResumen = String.format("Resumen: %d médicos elaboraron %d notas médicas en total",
                            estadisticas.size(), estadisticas.stream().mapToInt(EstadisticaMedico::getCantidad).sum());
                    break;

                case "Interconsultas":
                    fileName = "Relacion_Interconsultas_" + mes + "_" + año;
                    subtitulo = "NOTAS DE INTERCONSULTAS MÉDICAS ELABORADAS DURANTE LA ATENCIÓN AL PACIENTE";
                    columnaCantidad = "N° de Interconsultas";
                    textoResumen = String.format("Resumen: %d especialistas realizaron %d interconsultas en total",
                            estadisticas.size(), estadisticas.stream().mapToInt(EstadisticaMedico::getCantidad).sum());
                    break;

                default:
                    return false;
            }

            File file = File.createTempFile(fileName, ".pdf");
            file.deleteOnExit();

            // Crear PDF con iText 8
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Configurar fuentes
            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont subtitleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Título principal (SOLO en primera página)
            Paragraph title = new Paragraph("RELACIÓN ESTADÍSTICA DE ATENCIÓN MÉDICA")
                    .setFont(titleFont)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // Subtítulo
            Paragraph subtitlePara = new Paragraph(subtitulo)
                    .setFont(subtitleFont)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(subtitlePara);

            // Información del período
            Paragraph periodo = new Paragraph("Período: " + mes + " " + año)
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(periodo);

            // Fecha de generación
            String fechaGeneracion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Paragraph fecha = new Paragraph("Generado el: " + fechaGeneracion)
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(fecha);

            // Crear tabla PRINCIPAL que fluirá entre páginas
            float[] columnWidths = {3, 1};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginTop(10);
            table.setMarginBottom(10);

            // Encabezados de tabla - AZUL MARINO (#002b5c)
            // SOLO UNA VEZ, NO se repite en cada página
            Cell header1 = new Cell()
                    .add(new Paragraph("Nombre del Usuario/Médico").setFont(headerFont))
                    .setBackgroundColor(new DeviceRgb(0, 43, 92))  // Azul marino
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8);

            Cell header2 = new Cell()
                    .add(new Paragraph(columnaCantidad).setFont(headerFont))
                    .setBackgroundColor(new DeviceRgb(0, 43, 92))  // Azul marino
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8);

            table.addHeaderCell(header1);
            table.addHeaderCell(header2);

            // Agregar TODAS las filas (la tabla manejará automáticamente los saltos de página)
            int contador = 1;
            for (EstadisticaMedico medico : estadisticas) {
                String nombreMedico = medico.getMedico();
                if (nombreMedico == null || nombreMedico.trim().isEmpty()) {
                    nombreMedico = "No especificado";
                }

                // Opcional: numerar las filas
                String nombreConNumero = contador + ". " + nombreMedico;

                Cell cellNombre = new Cell()
                        .add(new Paragraph(nombreConNumero).setFont(normalFont).setFontSize(11))
                        .setPadding(6);

                Cell cellCantidad = new Cell()
                        .add(new Paragraph(String.valueOf(medico.getCantidad())).setFont(normalFont).setFontSize(11))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(6);

                table.addCell(cellNombre);
                table.addCell(cellCantidad);
                contador++;
            }

            document.add(table);

            // Pie de página con estadísticas
            Paragraph resumen = new Paragraph(textoResumen)
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            document.add(resumen);

            // Nota sobre ordenamiento
            Paragraph notaOrden = new Paragraph("* Lista ordenada de mayor a menor actividad")
                    .setFont(normalFont)
                    .setFontSize(10)
                    .setFontColor(new DeviceRgb(100, 100, 100))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5);
            document.add(notaOrden);

            document.close();

            // Abrir el archivo
            Desktop.getDesktop().open(file);

            return true;

        } catch (Exception e) {
            log.error("Error generando PDF de {}: {}", tipo, e.getMessage(), e);
            return false;
        }
    }

    public static boolean generarPDFGrafica(List<EstadisticaMedico> estadisticas, String mes, int año, String tipo, boolean esParaImprimir) {
        try {
            String fileName;
            String tituloGrafica;

            switch (tipo) {
                case "Ingresos":
                    fileName = "Grafica_Ingresos_" + mes + "_" + año;
                    tituloGrafica = "GRÁFICA DE PRODUCTIVIDAD POR INGRESOS - " + mes.toUpperCase() + " " + año;
                    break;
                case "Egresos":
                    fileName = "Grafica_Egresos_" + mes + "_" + año;
                    tituloGrafica = "GRÁFICA DE PRODUCTIVIDAD POR EGRESOS - " + mes.toUpperCase() + " " + año;
                    break;
                case "Notas médicas":
                    fileName = "Grafica_Notas_Medicas_" + mes + "_" + año;
                    tituloGrafica = "GRÁFICA DE PRODUCTIVIDAD POR NOTAS MÉDICAS - " + mes.toUpperCase() + " " + año;
                    break;
                case "Interconsultas":
                    fileName = "Grafica_Interconsultas_" + mes + "_" + año;
                    tituloGrafica = "GRÁFICA DE PRODUCTIVIDAD POR INTERCONSULTAS - " + mes.toUpperCase() + " " + año;
                    break;
                default:
                    return false;
            }

            File file = File.createTempFile(fileName, ".pdf");
            file.deleteOnExit();

            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Título principal
            Paragraph title = new Paragraph("INFORME ESTADÍSTICO GRÁFICO")
                    .setFont(titleFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            document.add(title);

            // Título específico
            Paragraph graficaTitle = new Paragraph(tituloGrafica)
                    .setFont(titleFont)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(graficaTitle);

            // Fecha de generación
            String fechaGeneracion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Paragraph fecha = new Paragraph("Generado el: " + fechaGeneracion)
                    .setFont(normalFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(fecha);

            // Para muchos datos (como interconsultas con 200 médicos), dividir en TOP 15
            boolean muchosDatos = estadisticas.size() > 30;
            List<EstadisticaMedico> datosParaGrafica;
            String notaGrafica = "";

            if (muchosDatos) {
                // Tomar solo TOP 15 para la gráfica
                datosParaGrafica = estadisticas.subList(0, Math.min(15, estadisticas.size()));
                int totalOtros = estadisticas.subList(15, estadisticas.size()).stream()
                        .mapToInt(EstadisticaMedico::getCantidad).sum();

                // Agregar "Otros" como último elemento
                if (totalOtros > 0) {
                    datosParaGrafica.add(new EstadisticaMedico(
                            "Otros (" + (estadisticas.size() - 15) + " más)",
                            totalOtros,
                            0.0
                    ));
                }
                notaGrafica = "* Gráfica muestra TOP 15. Ver lista completa en página siguiente.";
            } else {
                datosParaGrafica = estadisticas;
            }

            // GENERAR GRÁFICA DE BARRAS HORIZONTAL (mejor para muchos items)
            File imagenBarras = ChartGenerator.generarGraficaBarrasHorizontalAzulMarino(datosParaGrafica,
                    "Distribución por Usuarios/Médicos - " + tipo, tipo, mes, año);

            if (imagenBarras != null && imagenBarras.exists()) {
                com.itextpdf.layout.element.Image chartImageBarras =
                        new com.itextpdf.layout.element.Image(com.itextpdf.io.image.ImageDataFactory.create(imagenBarras.getAbsolutePath()));
                chartImageBarras.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                chartImageBarras.setMarginBottom(20);
                document.add(chartImageBarras);

                // Agregar nota si se truncó la gráfica
                if (!notaGrafica.isEmpty()) {
                    Paragraph nota = new Paragraph(notaGrafica)
                            .setFont(normalFont)
                            .setFontSize(9)
                            .setFontColor(new DeviceRgb(100, 100, 100))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(10);
                    document.add(nota);
                }
            } else {
                Paragraph sinGrafica = new Paragraph("No se pudo generar la gráfica. Mostrando solo datos tabulares.")
                        .setFont(normalFont)
                        .setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20);
                document.add(sinGrafica);
            }

            // NUEVA PÁGINA PARA LA TABLA COMPLETA
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            Paragraph tituloResumen = new Paragraph("LISTA COMPLETA - " + tipo.toUpperCase() + " (Ordenado de mayor a menor)")
                    .setFont(titleFont)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15);
            document.add(tituloResumen);

            // Crear tabla COMPLETA (todos los datos)
            float[] columnWidths = {1, 4, 2, 2}; // Agregamos columna para número
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(95));
            table.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            table.setMarginTop(20);

            PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Encabezados en azul marino
            table.addHeaderCell(new Cell().add(new Paragraph("#").setFont(headerFont))
                    .setBackgroundColor(new DeviceRgb(0, 43, 92))  // Azul marino
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setWidth(UnitValue.createPercentValue(5)));

            table.addHeaderCell(new Cell().add(new Paragraph("Usuario/Médico").setFont(headerFont))
                    .setBackgroundColor(new DeviceRgb(0, 43, 92))
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setTextAlignment(TextAlignment.LEFT));

            table.addHeaderCell(new Cell().add(new Paragraph("Cantidad").setFont(headerFont))
                    .setBackgroundColor(new DeviceRgb(0, 43, 92))
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setWidth(UnitValue.createPercentValue(10)));

            table.addHeaderCell(new Cell().add(new Paragraph("Porcentaje").setFont(headerFont))
                    .setBackgroundColor(new DeviceRgb(0, 43, 92))
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setWidth(UnitValue.createPercentValue(10)));

            int total = estadisticas.stream().mapToInt(EstadisticaMedico::getCantidad).sum();
            int numero = 1;

            for (EstadisticaMedico medico : estadisticas) {
                String nombreMedico = medico.getMedico();
                if (nombreMedico == null || nombreMedico.trim().isEmpty()) {
                    nombreMedico = "No especificado";
                }

                double porcentaje = total > 0 ? (medico.getCantidad() * 100.0) / total : 0;

                // Número
                table.addCell(new Cell().add(new Paragraph(String.valueOf(numero)).setFont(normalFont))
                        .setTextAlignment(TextAlignment.CENTER));

                // Nombre
                table.addCell(new Cell().add(new Paragraph(nombreMedico).setFont(normalFont)));

                // Cantidad
                table.addCell(new Cell().add(new Paragraph(String.valueOf(medico.getCantidad())).setFont(normalFont))
                        .setTextAlignment(TextAlignment.CENTER));

                // Porcentaje
                table.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", porcentaje)).setFont(normalFont))
                        .setTextAlignment(TextAlignment.CENTER));

                numero++;
            }

            document.add(table);

            // Resumen final
            Paragraph resumen = new Paragraph(
                    String.format("Total %s: %d | Total usuarios/médicos: %d", tipo.toLowerCase(), total, estadisticas.size()))
                    .setFont(normalFont)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            document.add(resumen);

            document.close();

            // Limpiar archivo temporal si existe
            if (imagenBarras != null && imagenBarras.exists()) {
                imagenBarras.delete();
            }

            Desktop.getDesktop().open(file);
            return true;

        } catch (Exception e) {
            log.error("Error generando PDF de gráfica de {}: {}", tipo, e.getMessage(), e);
            return false;
        }
    }

    // ========== MÉTODOS AUXILIARES ==========
    private static List<List<EstadisticaMedico>> dividirEnGrupos(List<EstadisticaMedico> lista, int tamañoGrupo) {
        List<List<EstadisticaMedico>> grupos = new ArrayList<>();
        for (int i = 0; i < lista.size(); i += tamañoGrupo) {
            grupos.add(lista.subList(i, Math.min(i + tamañoGrupo, lista.size())));
        }
        return grupos;
    }
}