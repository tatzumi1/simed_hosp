// src/main/java/com/PruebaSimed2/controllers/AuthController.java
package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.Usuario;
import com.PruebaSimed2.utils.PasswordUtils;
import lombok.extern.log4j.Log4j2;

import java.sql.*;

@Log4j2
public class AuthController {

    public Usuario login(String username, String password) {
        log.debug("[AuthController] Intentando login para: {}", username);

        String sql = "SELECT id_usuario, username, email, rol, activo, primer_login, password_hash " +
                "FROM tb_usuarios WHERE username = ? AND activo = true";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                log.debug("[AuthController] Usuario encontrado en BD: {}", username);
                String storedHash = rs.getString("password_hash");
                log.debug("[AuthController] Hash en BD: {}", storedHash);

                // Verificar contraseña
                boolean passwordOK = PasswordUtils.checkPassword(password, storedHash);
                log.debug("[AuthController] Contraseña correcta: {}", passwordOK);

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
                    log.debug("[AuthController] Login exitoso: {}", usuario.getUsername());
                    return usuario;
                } else {
                    log.warn("[AuthController] Contraseña incorrecta");
                }
            } else {
                log.warn("[AuthController] Usuario NO encontrado en BD: {}", username);
            }
        } catch (SQLException e) {
            log.error("[AuthController] Error SQL: {}", e.getMessage());
        }
        log.error("[AuthController] Login fallido");
        return null; // Login fallido
    }

    // Método para cambiar contraseña (para primer login)
    public boolean cambiarPassword(String username, String nuevaPassword) {
        String sql = "UPDATE tb_usuarios SET password_hash = ?, primer_login = false WHERE username = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String nuevoHash = PasswordUtils.hashPassword(nuevaPassword);
            pstmt.setString(1, nuevoHash);
            pstmt.setString(2, username);

            int filasAfectadas = pstmt.executeUpdate();
            log.debug("Filas afectadas: {}", filasAfectadas);
            log.info("Password cambiado correctamente");
            return filasAfectadas > 0;

        } catch (SQLException e) {
            log.error("[AuthController] Error cambiando password: {}", e.getMessage());
            return false;
        }
    }
}