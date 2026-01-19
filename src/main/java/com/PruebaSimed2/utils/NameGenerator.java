package com.PruebaSimed2.utils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class NameGenerator {
    public static String generateName(String apellidoPaterno, String apellidoMaterno, String nombre){
        StringBuilder nombreCompleto = new StringBuilder();

        if (apellidoPaterno != null && !apellidoPaterno.trim().isEmpty()) {
            nombreCompleto.append(apellidoMaterno).append(" ");
        }
        if (apellidoMaterno != null && !apellidoMaterno.trim().isEmpty()) {
            nombreCompleto.append(apellidoMaterno).append(" ");
        }
        if (nombre != null && !nombre.trim().isEmpty()) {
            nombreCompleto.append(nombre);
        }

        return !nombreCompleto.isEmpty() ? nombreCompleto.toString().trim() : "No especificado";
    }
}
