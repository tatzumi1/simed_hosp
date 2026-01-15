package com.PruebaSimed2.utils;

import com.PruebaSimed2.controllers.ModuloEstadisticaController.EstadisticaMedico;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.awt.Color;
import java.util.List;

@Log4j2
public class ChartGenerator {

    // Método para generar gráfica de barras en AZUL MARINO
    public static File generarGraficaBarrasAzulMarino(List<EstadisticaMedico> estadisticas,
                                                      String titulo, String tipo, String mes, int año) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // Limitar a los primeros 20 médicos para que la gráfica sea legible
            int maxItems = Math.min(estadisticas.size(), 20);

            for (int i = 0; i < maxItems; i++) {
                EstadisticaMedico medico = estadisticas.get(i);
                String nombre = medico.getMedico();
                if (nombre.length() > 20) {
                    nombre = nombre.substring(0, 17) + "...";
                }
                dataset.addValue(medico.getCantidad(), "Cantidad", nombre);
            }

            String tituloCompleto = titulo + " - " + mes + " " + año;

            // Crear gráfica de barras
            JFreeChart chart = ChartFactory.createBarChart(
                    tituloCompleto,         // Título
                    "Usuario/Médico",       // Etiqueta eje X
                    "Cantidad",             // Etiqueta eje Y
                    dataset,                // Datos
                    PlotOrientation.VERTICAL,
                    true,                   // Incluir leyenda
                    true,                   // Tooltips
                    false                   // URLs
            );

            // Configurar colores - AZUL MARINO (#002b5c)
            CategoryPlot plot = chart.getCategoryPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();

            // Color azul marino para todas las barras
            Color azulMarino = new Color(0, 43, 92);
            renderer.setSeriesPaint(0, azulMarino);

            // Configurar fondo
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

            // Configurar márgenes
            plot.getDomainAxis().setLowerMargin(0.02);
            plot.getDomainAxis().setUpperMargin(0.02);

            // Crear archivo temporal
            File tempFile = File.createTempFile("grafica_barras_", ".png");
            tempFile.deleteOnExit();

            // Guardar gráfica como imagen
            ChartUtils.saveChartAsPNG(tempFile, chart, 800, 500);

            return tempFile;

        } catch (Exception e) {
            log.error("Error generando gráfica de barras: {}", e.getMessage(), e);
            return null;
        }
    }


    public static File generarGraficaBarrasHorizontalAzulMarino(List<EstadisticaMedico> estadisticas,
                                                                String titulo, String tipo, String mes, int año) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // Limitar a los primeros 15 médicos para mejor visualización
            int maxItems = Math.min(estadisticas.size(), 15);

            for (int i = 0; i < maxItems; i++) {
                EstadisticaMedico medico = estadisticas.get(i);
                String nombre = medico.getMedico();
                if (nombre.length() > 25) {
                    nombre = nombre.substring(0, 22) + "...";
                }
                dataset.addValue(medico.getCantidad(), "Cantidad", nombre);
            }

            String tituloCompleto = titulo + " - " + mes + " " + año;

            // Crear gráfica de barras HORIZONTAL
            JFreeChart chart = ChartFactory.createBarChart(
                    tituloCompleto,         // Título
                    "Cantidad",             // Etiqueta eje X (ahora en horizontal)
                    "Usuario/Médico",       // Etiqueta eje Y
                    dataset,                // Datos
                    PlotOrientation.HORIZONTAL,
                    true,                   // Incluir leyenda
                    true,                   // Tooltips
                    false                   // URLs
            );

            // Configurar colores - AZUL MARINO
            CategoryPlot plot = chart.getCategoryPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();

            Color azulMarino = new Color(0, 43, 92);
            renderer.setSeriesPaint(0, azulMarino);

            // Configurar fondo
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

            // Crear archivo temporal
            File tempFile = File.createTempFile("grafica_horizontal_", ".png");
            tempFile.deleteOnExit();

            // Guardar gráfica como imagen
            ChartUtils.saveChartAsPNG(tempFile, chart, 800, 600);

            return tempFile;

        } catch (Exception e) {
            log.error("Error generando gráfica horizontal: {}", e.getMessage(), e);
            return null;
        }
    }
}