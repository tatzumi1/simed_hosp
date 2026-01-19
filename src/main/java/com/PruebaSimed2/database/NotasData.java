package com.PruebaSimed2.database;

import com.PruebaSimed2.models.NotaMedicaVO;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.PruebaSimed2.utils.TypeConversor.convertSqlDateAndSqlTimeToDate;

@Log4j2
public class NotasData {
    private static final String CONTEO_NOTAS = "SELECT COUNT(*) as total FROM tb_notas WHERE Folio = ? AND (Estado = 'DEFINITIVA' OR Estado = 'TEMPORAL')";
    private static final String CONTEO_NOTAS_POR_MEDICO = "SELECT COUNT(*) as temp FROM tb_notas WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ?";
    private static final String OBTENER_SINTOMAS_POR_NOTA = "SELECT sintomas FROM tb_notas WHERE id_nota = ?";
    private static final String OBTENER_NOTAS_POR_PACIENTE = "SELECT id_nota, Folio, Num_nota, Nota, Indicaciones, sintomas, signos_vitales, diagnostico, " +
            "Medico, Cedula, Fecha, Hora, Estado, estado_paciente, " +
            "editable_por_medico, permiso_edicion_otorgado_por, " +
            "fecha_permiso_edicion, rol_usuario_otorga, fecha_edicion_realizada " +
            "FROM tb_notas WHERE Folio = ? ORDER BY Num_nota DESC";
    private static final String OTORGAR_PERMISO_EDICION = "UPDATE tb_notas SET editable_por_medico = TRUE, permiso_edicion_otorgado_por = ?, fecha_permiso_edicion = NOW(), fecha_edicion_realizada = NULL WHERE id_nota = ?";

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

    public int obtenerConteoNotasPorMedico(Connection connection, int folioPaciente, String medico) {
        try (PreparedStatement statement = connection.prepareStatement(CONTEO_NOTAS_POR_MEDICO)) {
            statement.setInt(1, folioPaciente);
            statement.setString(2, medico);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("temp");
                log.debug("Conteo de notas temporales para el paciente con folio {} y medico {}: {}", folioPaciente, medico, total);
                return total;
            } else {
                log.warn("No se encontraron notas temporales para el paciente con folio {} y medico {}", folioPaciente, medico);
                return 0;
            }
        } catch (SQLException e) {
            log.error("Error al obtener el conteo de notas por medico para el paciente con folio: {}", folioPaciente, e);
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

    public List<NotaMedicaVO> obtenerNotasPaciente(Connection connection, int folioPaciente) {
        try (PreparedStatement statement = connection.prepareStatement(OBTENER_NOTAS_POR_PACIENTE)) {
            statement.setInt(1, folioPaciente);
            ResultSet rs = statement.executeQuery();
            List<NotaMedicaVO> notas = new ArrayList<>();
            while (rs.next()) {
                NotaMedicaVO nota = new NotaMedicaVO(
                        rs.getInt("id_nota"),
                        rs.getInt("Folio"),
                        rs.getInt("Num_nota"),
                        rs.getString("Nota"),
                        rs.getString("Medico"),
                        rs.getString("Cedula"),
                        convertSqlDateAndSqlTimeToDate(rs.getDate("Fecha"), rs.getTime("Hora")),
                        rs.getString("Estado"),
                        rs.getString("estado_paciente"),
                        rs.getBoolean("editable_por_medico"),
                        rs.getString("permiso_edicion_otorgado_por"),
                        rs.getTimestamp("fecha_permiso_edicion") != null ?
                                rs.getTimestamp("fecha_permiso_edicion").toLocalDateTime() : null
                );
                nota.setIndicaciones(rs.getString("Indicaciones"));
                log.debug("Nota cargada: {}", nota);
                notas.add(nota);
            }
            log.debug("Se han cargado {} notas para el paciente con folio {}", notas.size(), folioPaciente);
            return notas;
        } catch (SQLException e) {
            log.error("Error al obtener notas para paciente con folio {}: {}", folioPaciente, e.getMessage(), e);
            return List.of();
        }
    }

    public boolean otorgarPermisoEdicion(Connection connection, String usuario, int idNota) {
        try (PreparedStatement ps = connection.prepareStatement(OTORGAR_PERMISO_EDICION)) {
            ps.setString(1, usuario);
            ps.setInt(2, idNota);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Permiso de edición otorgado para nota con ID {}", idNota);
                return true;
            } else {
                log.warn("No se pudo otorgar permiso de edición para nota con ID {}", idNota);
                return false;
            }
        } catch (SQLException e) {
            log.error("Error al otorgar permiso de edición para nota con ID {}: {}", idNota, e.getMessage(), e);
            return false;
        }
    }
}
