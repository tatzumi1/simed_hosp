package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class UsuarioData {
    private static final String OBTENER_USUARIO_POR_USERNAME = "SELECT id_usuario FROM tb_usuarios WHERE username = ?";
    private static final String OBTENER_NOMBRE_MEDICO_POR_USERNAME = "SELECT m.Med_nombre FROM tb_medicos m " +
            "INNER JOIN tb_usuarios u ON m.Ced_prof = u.empleado_id WHERE u.username = ? " +
            "UNION SELECT nombre_completo FROM tb_usuarios WHERE username = ? " +
            "UNION SELECT Med_nombre FROM tb_medicos WHERE Med_nombre LIKE CONCAT('%', ?, '%') LIMIT 1";

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

    public String obtenerNombreMedicoPorUsername(Connection connection, String username) {
        try (PreparedStatement statement = connection.prepareStatement(OBTENER_NOMBRE_MEDICO_POR_USERNAME)) {
            statement.setString(1, username);
            statement.setString(2, username);
            statement.setString(3, username);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                String nombreMedico = rs.getString("Med_nombre");
                log.debug("Nombre del medico obtenido: {}", nombreMedico);
                return nombreMedico;
            } else {
                log.warn("Nombre del medico no encontrado para el usuario: {}", username);
                return null;
            }
        } catch (SQLException e) {
            log.error("Error al obtener el nombre del medico: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
