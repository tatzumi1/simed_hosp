// src/main/java/com/PruebaSimed2/GenerarHashesParaTodos.java
package com.PruebaSimed2;

import org.mindrot.jbcrypt.BCrypt;

public class GenerarHashesParaTodos {
    public static void main(String[] args) {
        String tempPassword = "Temp123";
        String adminPassword = "hospital123";

        System.out.println("=== HASHES ACTUALIZADOS ===");
        System.out.println("Para TODOS los roles NO admin (Temp123):");
        String hashTemp = BCrypt.hashpw(tempPassword, BCrypt.gensalt());
        System.out.println("Hash: " + hashTemp);

        System.out.println("\nPara TODOS los admin (hospital123):");
        String hashAdmin = BCrypt.hashpw(adminPassword, BCrypt.gensalt());
        System.out.println("Hash: " + hashAdmin);

        // Verificar que funcionan
        System.out.println("\n=== VERIFICACIÓN ===");
        System.out.println("Temp123 verifica: " + BCrypt.checkpw("Temp123", hashTemp));
        System.out.println("hospital123 verifica: " + BCrypt.checkpw("hospital123", hashAdmin));

        System.out.println("\n=== COMANDOS SQL ===");
        System.out.println("-- Para roles NO admin:");
        System.out.println("UPDATE tb_usuarios SET password_hash = '" + hashTemp + "', primer_login = 1 WHERE rol != 'ADMIN' AND activo = TRUE;");

        System.out.println("\n-- Para admin:");
        System.out.println("UPDATE tb_usuarios SET password_hash = '" + hashAdmin + "', primer_login = 1 WHERE rol = 'ADMIN' AND activo = TRUE;");
    }
}