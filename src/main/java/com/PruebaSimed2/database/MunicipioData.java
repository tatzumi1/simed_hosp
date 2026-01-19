package com.PruebaSimed2.database;

import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class MunicipioData {
    private static final String OBTENER_DESCRIPCION_MUNICIPIO = "SELECT DESCRIP FROM tblt_mpo WHERE MPO = ? AND EDO = ?";

    public String obtenerDescripcionMunicipio (Connection connection, String municipioCode, String entidadCode) {
        try (PreparedStatement stmt = connection.prepareStatement(OBTENER_DESCRIPCION_MUNICIPIO)) {
            stmt.setString(1, municipioCode);
            stmt.setString(2, entidadCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String nombre = rs.getString("DESCRIP");
                    log.debug("Descripción del municipio obtenida: {}", nombre);
                    return nombre;
                } else {
                    log.warn("No se encontró descripción para el municipio {} en la entidad {}", municipioCode, entidadCode);
                    return null;
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener descripción del municipio: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
