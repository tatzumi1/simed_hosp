package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class InterData {
    private static final String CONTEO_INTERCONSULTAS = "SELECT COUNT(*) as total FROM tb_inter WHERE Folio = ? AND (Estado = 'DEFINITIVA' OR Estado = 'TEMPORAL')";
    private static final String OBTENER_SINTOMAS_POR_INTERCONSULTA = "SELECT sintomas FROM tb_inter WHERE id_inter = ?";

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

    public String obtenerSintomasPorInterconsulta(Connection connection, int idInterconsulta) {
        try (PreparedStatement stmt = connection.prepareStatement(OBTENER_SINTOMAS_POR_INTERCONSULTA)) {
            stmt.setInt(1, idInterconsulta);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String sintomas = rs.getString("sintomas");
                log.debug("Sintomas de la interconsulta con id {}: {}", idInterconsulta, sintomas);
                return sintomas;
            } else {
                log.warn("No se encontraron sintomas para la interconsulta con id {}", idInterconsulta);
                return "Sin síntomas registrados";
            }
        } catch (SQLException e) {
            log.error("Error al obtener sintomas de la interconsulta con id: {}", idInterconsulta, e);
            return "Sin síntomas registrados";
        }
    }
}
