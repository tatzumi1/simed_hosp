package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Log4j2
public class AuditoriaData {
    private static final String REGISTRAR_AUDITORIA_REGISTRO_TRIAGE = "INSERT INTO tb_auditoria (username, accion, tabla_afectada, registro_id) VALUES (?,?,?,?)";

    public void registrarAuditoriaRegistroTriage(String username, int folio) {
        try (Connection connection = ConexionBD.conectar();
             PreparedStatement stmt = connection.prepareStatement(REGISTRAR_AUDITORIA_REGISTRO_TRIAGE)) {
            stmt.setString(1, username);
            stmt.setString(2, "Registro Triage");
            stmt.setString(3, "tb_triage");
            stmt.setInt(4, folio);
            stmt.executeUpdate();
            log.info("Auditoría de registro de triage registrada exitosamente para el folio: {}", folio);
        } catch (SQLException e) {
            log.error("Error al registrar auditoría de registro de triage", e);
        }
    }
}
