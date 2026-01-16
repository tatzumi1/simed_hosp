package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class InterData {
    private static final String CONTEO_INTERCONSULTAS = "SELECT COUNT(*) as total FROM tb_inter WHERE Folio = ? AND (Estado = 'DEFINITIVA' OR Estado = 'TEMPORAL')";

    public int obtenerConteoInterconsultas(Connection connection, int folioPaciente) {
        try (PreparedStatement stmt = connection.prepareStatement(CONTEO_INTERCONSULTAS)) {
            stmt.setInt(1, folioPaciente);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("total");
                log.debug("Conteo de interconsultas para el paciente con folio {}: {}", folioPaciente, total);
                return total;
            } else {
                log.warn("No se encontraron interconsultas para el paciente con folio {}", folioPaciente);
                return 0;
            }
        } catch (SQLException e) {
            log.error("Error al obtener el conteo de interconsultas para el paciente con folio: {}", folioPaciente, e);
            return 0;
        }
    }
}
