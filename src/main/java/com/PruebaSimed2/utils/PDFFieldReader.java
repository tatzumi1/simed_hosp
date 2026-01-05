package com.PruebaSimed2.utils;


import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import java.util.Map;
import java.util.TreeMap;

public class PDFFieldReader {

    public static void main(String[] args) {
        checkPDFFields();
    }

    public static void checkPDFFields() {
        String templatePath = "src/main/resources/pdf_templates/SEUL-16-p_2024.pdf";

        System.out.println(" VERIFICANDO CAMPOS DEL PDF:");
        System.out.println("Archivo: " + templatePath);
        System.out.println("=".repeat(70));

        try (PdfReader reader = new PdfReader(templatePath);
             PdfDocument pdfDoc = new PdfDocument(reader)) {

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            Map<String, PdfFormField> fields = form.getAllFormFields();

            // Ordenar alfabéticamente
            TreeMap<String, PdfFormField> camposOrdenados = new TreeMap<>(fields);

            System.out.println(" TOTAL CAMPOS: " + camposOrdenados.size());
            System.out.println("=".repeat(70));

            int i = 1;
            for (Map.Entry<String, PdfFormField> entry : camposOrdenados.entrySet()) {
                String nombre = entry.getKey();
                PdfFormField campo = entry.getValue();

                System.out.println(i + ". \"" + nombre + "\"");
                System.out.println("   Tipo: " + campo.getFormType());
                System.out.println("   Valor: \"" + campo.getValueAsString() + "\"");
                System.out.println();
                i++;
            }

            // Buscar campos relacionados con ALTA
            System.out.println("=".repeat(70));
            System.out.println(" CAMPOS CON 'ALTA':");
            for (String nombre : camposOrdenados.keySet()) {
                if (nombre.toLowerCase().contains("alta")) {
                    System.out.println("• " + nombre);
                }
            }

            // Buscar campos relacionados con FECHA
            System.out.println("\n CAMPOS CON 'FECHA':");
            for (String nombre : camposOrdenados.keySet()) {
                if (nombre.toLowerCase().contains("fecha")) {
                    System.out.println("• " + nombre);
                }
            }

            // Buscar campos relacionados con HORA
            System.out.println("\n CAMPOS CON 'HORA':");
            for (String nombre : camposOrdenados.keySet()) {
                if (nombre.toLowerCase().contains("hora")) {
                    System.out.println("• " + nombre);
                }
            }

            // Buscar campos relacionados con MINUTO
            System.out.println("\n CAMPOS CON 'MINUTO':");
            for (String nombre : camposOrdenados.keySet()) {
                if (nombre.toLowerCase().contains("minuto")) {
                    System.out.println("• " + nombre);
                }
            }

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}