package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class MedicoData {
    private static final String OBTENER_CEDULA_POR_NOMBRE = "SELECT Ced_prof FROM tb_medicos WHERE Med_nombre = ?";

    public String obtenerCedulaPorNombre(Connection connection, String nombreMedico) {
        try (PreparedStatement statement = connection.prepareStatement(OBTENER_CEDULA_POR_NOMBRE)) {
            statement.setString(1, nombreMedico);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                String cedula = rs.getString("Ced_prof");
                log.debug("Cedula de medico obtenida: {}", cedula);
                return cedula;
            } else {
                log.warn("Cedula de medico no encontrada para el nombre: {}", nombreMedico);
                return null;
            }
        } catch (SQLException e) {
            log.error("Error al obtener la cedula de un medico", e);
            throw new RuntimeException(e);
        }
    }
}
