package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class NotasData {
    private static final String CONTEO_NOTAS = "SELECT COUNT(*) as total FROM tb_notas WHERE Folio = ? AND (Estado = 'DEFINITIVA' OR Estado = 'TEMPORAL')";
    private static final String OBTENER_SINTOMAS_POR_NOTA = "SELECT sintomas FROM tb_notas WHERE id_nota = ?";

    public int obtenerConteoNotas(Connection connection, int folioPaciente) {
        try (PreparedStatement stmt = connection.prepareStatement(CONTEO_NOTAS)) {
            stmt.setInt(1, folioPaciente);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("total");
                log.debug("Conteo de notas para el paciente con folio {}: {}", folioPaciente, total);
                return total;
            } else {
                log.warn("No se encontraron notas para el paciente con folio {}", folioPaciente);
                return 0;
            }
        } catch (SQLException e) {
            log.error("Error al obtener el conteo de notas para el paciente con folio: {}", folioPaciente, e);
            return 0;
        }
    }

    public String obtenerSintomasPorNota(Connection connection, int idNota) {
        try (PreparedStatement stmt = connection.prepareStatement(OBTENER_SINTOMAS_POR_NOTA)) {
            stmt.setInt(1, idNota);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String sintomas = rs.getString("sintomas");
                log.debug("Sintomas de la nota con id {}: {}", idNota, sintomas);
                return sintomas;
            } else {
                log.warn("No se encontraron sintomas para la nota con id {}", idNota);
                return null;
            }
        } catch (SQLException e) {
            log.error("Error al obtener sintomas de la nota con id: {}", idNota, e);
            throw new RuntimeException(e);
        }
    }
}
