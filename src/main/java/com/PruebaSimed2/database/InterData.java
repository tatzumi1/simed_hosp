package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class InterData {
    private static final String CONTEO_INTERCONSULTAS = "SELECT COUNT(*) as total FROM tb_inter WHERE Folio = ? AND (Estado = 'DEFINITIVA' OR Estado = 'TEMPORAL')";
    private static final String CONTEO_INTERCONSULTAS_POR_MEDICO = "SELECT COUNT(*) as temp FROM tb_inter WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ?";
    private static final String OBTENER_SINTOMAS_POR_INTERCONSULTA = "SELECT sintomas FROM tb_inter WHERE id_inter = ?";
    private static final String TIENE_PERMISO_INTERCONSULTA = "UPDATE tb_inter SET editable_por_medico = TRUE, permiso_edicion_otorgado_por = ?, fecha_permiso_edicion = NOW(), fecha_edicion_realizada = NULL WHERE id_inter = ?";

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

    public int obtenerConteoInterconsultasPorMedico(Connection connection, int folioPaciente, String medico) {
        try (PreparedStatement statement = connection.prepareStatement(CONTEO_INTERCONSULTAS_POR_MEDICO)) {
            statement.setInt(1, folioPaciente);
            statement.setString(2, medico);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("temp");
                log.debug("Conteo de interconsultas temporales para el paciente con folio {} y medico {}: {}", folioPaciente, medico, total);
                return total;
            } else {
                log.warn("No se encontraron interconsultas temporales para el paciente con folio {} y medico {}", folioPaciente, medico);
                return 0;
            }
        } catch (SQLException e) {
            log.error("Error al obtener el conteo de interconsultas por medico para el paciente con folio: {}", folioPaciente, e);
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

    public boolean tienePermisoInterconsulta(Connection connection, String usuarioLogueado, int idInterconsulta) {
        try (PreparedStatement stmt = connection.prepareStatement(TIENE_PERMISO_INTERCONSULTA)) {
            stmt.setString(1, usuarioLogueado);
            stmt.setInt(2, idInterconsulta);
            int filas = stmt.executeUpdate();
            if (filas > 0) {
                log.debug(" Permiso otorgado para interconsulta - ID: {}", idInterconsulta);
                return true;
            } else {
                log.debug(" No se pudo otorgar permiso para interconsulta - ID: {}", idInterconsulta);
                return false;
            }
        } catch (SQLException e) {
            log.error("Error al otorgar permiso para interconsulta: {}", e.getMessage());
            return false;
        }
    }
}
