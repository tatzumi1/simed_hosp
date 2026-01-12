package com.PruebaSimed2.utils;


import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.TreeMap;

@Log4j2
public class PDFFieldReader {

    public static void main(String[] args) {
        checkPDFFields();
    }

    public static void checkPDFFields() {
        String templatePath = "src/main/resources/pdf_templates/SEUL-16-p_2024.pdf";

        log.debug(" VERIFICANDO CAMPOS DEL PDF:");
        log.debug("Archivo: {}", templatePath);
        log.debug("=".repeat(70));

        try (PdfReader reader = new PdfReader(templatePath);
             PdfDocument pdfDoc = new PdfDocument(reader)) {

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            Map<String, PdfFormField> fields = form.getAllFormFields();

            // Ordenar alfabéticamente
            TreeMap<String, PdfFormField> camposOrdenados = new TreeMap<>(fields);

            log.debug(" TOTAL CAMPOS: {}", camposOrdenados.size());

            int i = 1;
            for (Map.Entry<String, PdfFormField> entry : camposOrdenados.entrySet()) {
                String nombre = entry.getKey();
                PdfFormField campo = entry.getValue();

                log.debug("{}. \"{}\"", i, nombre);
                log.debug("   Tipo: {}", campo.getFormType());
                log.debug("   Valor: \"{}\"", campo.getValueAsString());
                i++;
            }

            // Buscar campos relacionados con ALTA
            log.debug(" CAMPOS CON 'ALTA':");
            for (String nombre : camposOrdenados.keySet()) {
                if (nombre.toLowerCase().contains("alta")) {
                    log.debug("• {}", nombre);
                }
            }

            // Buscar campos relacionados con FECHA
            log.debug("\n CAMPOS CON 'FECHA':");
            for (String nombre : camposOrdenados.keySet()) {
                if (nombre.toLowerCase().contains("fecha")) {
                    log.debug("• {}", nombre);
                }
            }

            // Buscar campos relacionados con HORA
            log.debug("\n CAMPOS CON 'HORA':");
            for (String nombre : camposOrdenados.keySet()) {
                if (nombre.toLowerCase().contains("hora")) {
                    log.debug("• {}", nombre);
                }
            }

            // Buscar campos relacionados con MINUTO
            log.debug("\n CAMPOS CON 'MINUTO':");
            for (String nombre : camposOrdenados.keySet()) {
                if (nombre.toLowerCase().contains("minuto")) {
                    log.debug("• {}", nombre);
                }
            }

        } catch (Exception e) {
            log.error(" Error: {}", e.getMessage(), e);
        }
    }
}