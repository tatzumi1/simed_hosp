package com.PruebaSimed2.utils;

import com.PruebaSimed2.controllers.MainController.Paciente;

public class DatosPacienteUtils {

    // ====================== CÁLCULO DE COMPLETITUD ======================
    public static int calcularCompletitud(Paciente paciente) {
        int puntos = 0;

        // Nombre completo
        if (tieneTexto(paciente.getNombre())) puntos += 25;

        // Al menos un apellido
        if (tieneTexto(paciente.getPaterno()) || tieneTexto(paciente.getMaterno())) puntos += 20;

        // Edad válida
        if (paciente.getEdad() > 0 && paciente.getEdad() <= 130) puntos += 15;

        // Domicilio
        if (tieneTexto(paciente.getDomicilio())) puntos += 15;

        // Teléfono (mínimo 10 dígitos)
        if (tieneTexto(paciente.getTelefono()) && paciente.getTelefono().replaceAll("\\D", "").length() >= 10)
            puntos += 10;

        // CURP completa (18 caracteres)
        if (tieneTexto(paciente.getCurp()) && paciente.getCurp().length() == 18) puntos += 10;

        // Derechohabiencia (IMSS, ISSSTE, etc.)
        if (tieneTexto(paciente.getDerechohabiencia())) puntos += 5;

        return puntos;
    }

    // ====================== COLORES EN TONOS AZULES CLARITOS ======================
    public static String obtenerColorCompletitud(int puntos) {
        if (puntos >= 80) return "AZUL_CLARO";      // Casi completo → azul muy clarito
        if (puntos >= 50) return "AZUL_MEDIO";      // Moderado → azul medio claro
        return "AZUL_OSCURO";                       // Muy incompleto → azul un poco más fuerte
    }

    public static String obtenerColorCSS(String color) {
        return switch (color) {
            case "AZUL_CLARO"   -> "#dbeafe";   // azul muy clarito (casi completos)
            case "AZUL_MEDIO"   -> "#bfdbfe";   // azul medio claro (moderados)
            case "AZUL_OSCURO"  -> "#93c5fd";   // azul más intenso (muy incompletos)
            default             -> "#e0f2fe";
        };
    }

    public static String obtenerDescripcionNivel(String color) {
        return switch (color) {
            case "AZUL_CLARO"   -> "Casi Completos";
            case "AZUL_MEDIO"   -> "Moderados";
            case "AZUL_OSCURO"  -> "Muy Incompletos";
            default             -> "Desconocido";
        };
    }


    private static boolean tieneTexto(String texto) {
        return texto != null && !texto.trim().isEmpty() &&
                !texto.trim().equalsIgnoreCase("No especificado") &&
                !texto.trim().equalsIgnoreCase("Sin dato");
    }
}