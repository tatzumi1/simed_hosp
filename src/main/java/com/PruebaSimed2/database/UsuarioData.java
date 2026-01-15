package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class UsuarioData {
    private static final String OBTENER_USUARIO_POR_USERNAME = "SELECT id_usuario FROM tb_usuarios WHERE username = ?";

    public int obtenerIdUsuarioPorUsername(Connection connection, String username) {
        try (PreparedStatement stmt = connection.prepareStatement(OBTENER_USUARIO_POR_USERNAME)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id_usuario");
                log.debug("ID de usuario obtenido: {}", id);
                return id;
            } else {
                log.warn("ID no encontrado para el usuario: {}", username);
                return 0;
            }
        } catch (SQLException e) {
            log.error("Error al obtener el ID de usuario: {}", e.getMessage());
            return 0;
        }
    }
}
