// src/main/java/com/PruebaSimed2/controllers/AuthController.java
package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.Usuario;
import com.PruebaSimed2.utils.PasswordUtils;

import java.sql.*;

public class AuthController {

    public Usuario login(String username, String password) {

        System.out.println(" [AuthController] Intentando login para: " + username);

        String sql = "SELECT id_usuario, username, email, rol, activo, primer_login, password_hash " +
                "FROM tb_usuarios WHERE username = ? AND activo = true";
        try (Connection conn = ConexionBD.conectar();
       // try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("[AuthController] Usuario encontrado en BD: " + username);
                String storedHash = rs.getString("password_hash");
                System.out.println(" [AuthController] Hash en BD: " + storedHash);

                // Verificar contraseña
                boolean passwordOK = PasswordUtils.checkPassword(password, storedHash);
                System.out.println(" [AuthController] Contraseña correcta: " + passwordOK);

                if (passwordOK) {
                    // Login exitoso
                    Usuario usuario = new Usuario(
                            rs.getInt("id_usuario"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("rol"),
                            rs.getBoolean("activo"),
                            rs.getBoolean("primer_login")
                    );
                    System.out.println(" [AuthController] Login exitoso: " + usuario.getUsername());
                    return usuario;
                } else {
                    System.out.println(" [AuthController] Contraseña incorrecta");
                }
            } else {
                System.out.println(" [AuthController] Usuario NO encontrado en BD: " + username);
            }
        } catch (SQLException e) {
            System.err.println(" [AuthController] Error SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Login fallido
    }

    // Método para cambiar contraseña (para primer login)
    public boolean cambiarPassword(String username, String nuevaPassword) {
        String sql = "UPDATE tb_usuarios SET password_hash = ?, primer_login = false WHERE username = ?";
        try (Connection conn = ConexionBD.conectar();
        //try (Connection conn = ConexionBD.testconectar();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String nuevoHash = PasswordUtils.hashPassword(nuevaPassword);
            pstmt.setString(1, nuevoHash);
            pstmt.setString(2, username);

            int filasAfectadas = pstmt.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println(" Error cambiando password: " + e.getMessage());
            return false;
        }
    }
}