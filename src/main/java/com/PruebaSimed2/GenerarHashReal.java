// src/main/java/com/PruebaSimed2/GenerarHashReal.java
package com.PruebaSimed2;

import org.mindrot.jbcrypt.BCrypt;

public class GenerarHashReal {
    public static void main(String[] args) {
        String password = "temp123";

        // Generar hash FRESCO con BCrypt
        String hashReal = BCrypt.hashpw(password, BCrypt.gensalt());

        System.out.println("======================================");
        System.out.println("HASH REAL PARA 'temp123':");
        System.out.println("======================================");
        System.out.println("CONTRASEÑA: " + password);
        System.out.println("HASH: " + hashReal);
        System.out.println("======================================");

        // Verificar que funciona
        boolean check = BCrypt.checkpw(password, hashReal);
        System.out.println("VERIFICACIÓN: " + (check ? " FUNCIONA" : " NO FUNCIONA"));
    }
}
