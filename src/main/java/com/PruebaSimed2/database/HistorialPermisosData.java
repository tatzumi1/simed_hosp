package com.PruebaSimed2.database;

import com.PruebaSimed2.DTO.HistorialPermisos.InsertarPermisoDTO;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Log4j2
public class HistorialPermisosData {
    private static final String OTORGAR_PERMISO_NOTAS = "INSERT INTO tb_historial_permisos (id_nota, tipo_nota, folio_paciente, medico_autor, " +
            "accion, usuario_que_actua, rol_usuario, motivo, estado_paciente) " +
            "SELECT ?, ?, Folio, Medico, ?, ?, ?, 'Permiso de un solo uso', estado_paciente " +
            "FROM tb_inter WHERE id_inter = ?";

    public void otorgarPermisoNotas(Connection connection, InsertarPermisoDTO dto) {
        try (PreparedStatement statement = connection.prepareStatement(OTORGAR_PERMISO_NOTAS)) {
            statement.setInt(1, dto.getId());
            statement.setString(2, dto.getColumna());
            statement.setString(3, dto.getPermiso());
            statement.setString(4, dto.getUsuario());
            statement.setString(5, dto.getRol());
            statement.setInt(6, dto.getId());
            statement.executeUpdate();
            log.info("Permiso de notas otorgado correctamente para la nota: {}", dto.getId());
        } catch (SQLException e) {
            log.error("Error al otorgar permiso de notas a un paciente", e);
            throw new RuntimeException(e);
        }
    }
}
