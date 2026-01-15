package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class EntidadData {
    private static final String OBTENER_ENTIDADES = "SELECT DESCRIP FROM tblt_entidad WHERE EDO = ?";

    public String obtenerEntidades(Connection connection, String edo) {
        try (PreparedStatement stmt = connection.prepareStatement(OBTENER_ENTIDADES)) {
            stmt.setString(1, edo);
            try (ResultSet rs = stmt.executeQuery()) {
                String descripcion = rs.getString(1);
                if (descripcion != null) {
                    log.debug("Entidades obtenidas: {}", descripcion);
                    return descripcion;
                } else {
                    log.warn("No se encontraron entidades para el EDO: {}", edo);
                    return null;
                }
            }
        } catch (SQLException e) {
            log.error("Error obteniendo entidades: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
