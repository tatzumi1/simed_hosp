// src/main/java/com/PruebaSimed2/DescubrirPassword.java
package com.PruebaSimed2;

import com.PruebaSimed2.utils.PasswordUtils;

public class DescubrirPassword {
    public static void main(String[] args) {
        // El hash que tienes en tu BD para doctor1
        String hashEnTuBD = "$2a$12$LQv3c1yqBWVHxkd0g8f7QuOMrL5b8gA9/Y3GyAe6g7JGg5Y5Q5lOa";

        System.out.println(" DESCUBRIENDO CONTRASEÑA PARA doctor1");
        System.out.println("Hash en BD: " + hashEnTuBD);
        System.out.println("======================================");

        // Lista de contraseñas comunes que podrías haber usado
        String[] passwordsComunes = {
                "temp123", "hospital123", "password", "123456",
                "admin123", "doctor123", "medico123", "12345678",
                "password123", "admin", "root", "test", "demo"
        };

        boolean encontrada = false;

        for (String password : passwordsComunes) {
            boolean coincide = PasswordUtils.checkPassword(password, hashEnTuBD);
            System.out.println("Probando '" + password + "': " + (coincide ? " CORRECTA" : "❌"));

            if (coincide) {
                System.out.println("\n ¡CONTRASEÑA ENCONTRADA!");
                System.out.println("Usuario: doctor1");
                System.out.println("Contraseña: " + password);
                encontrada = true;
                break;
            }
        }

        if (!encontrada) {
            System.out.println("\n No se encontró la contraseña. Probemos otra solución...");
        }
    }
}