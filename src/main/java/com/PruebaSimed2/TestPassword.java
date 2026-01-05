// src/main/java/com/PruebaSimed2/TestPassword.java
package com.PruebaSimed2;

import com.PruebaSimed2.utils.PasswordUtils;

public class TestPassword {
    public static void main(String[] args) {
        String password = "hospital123";
        String hash = PasswordUtils.hashPassword(password);

        System.out.println("======================================");
        System.out.println("CONTRASEÑA: " + password);
        System.out.println("HASH GENERADO: " + hash);
        System.out.println("======================================");

        // Verificar que funciona
        boolean check = PasswordUtils.checkPassword(password, hash);
        System.out.println("VERIFICACIÓN: " + (check ? " CORRECTO" : " INCORRECTO"));
    }
}