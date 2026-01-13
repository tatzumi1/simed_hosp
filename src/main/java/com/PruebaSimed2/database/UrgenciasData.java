package com.PruebaSimed2.database;

import com.PruebaSimed2.DTO.InsertarPacienteDTO;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Log4j2
public class UrgenciasData {
    private static final String INSERT_PACIENTE = "INSERT INTO tb_urgencias (" +
            "Folio, A_paterno, A_materno, Nombre, Edad, F_nac, Telefono, Domicilio, " +
            "Derechohabiencia, No_afiliacion, Referencia, Reingreso, Hospitalizado, " +
            "Exp_clinico, CURP, Sintomas, Nom_med, TRIAGE, Nombre_ts, Turno, " +
            "Fecha, Hora_registro, Estado_pac, Sexo, Municipio_resid, Entidad_resid, " +
            "Ocupacion, Religion, Edo_civil, Observaciones_ts, Municipio_completo, Entidad_completa" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
            "CURDATE(), ?, 1, ?, ?, ?, " +
            "?, ?, ?, ?, ?, ?)";
    private static final String OBTENER_ID_MAXIMO = "SELECT COALESCE(MAX(Folio), 0) + 1 FROM tb_urgencias";

    public boolean insertarPaciente(InsertarPacienteDTO dto) {
        log.debug("Insertando paciente en la base de datos: {}", dto);
        try (Connection connection = ConexionBD.conectar(); PreparedStatement stmt = connection.prepareStatement(INSERT_PACIENTE)) {
            int i = 1;
            stmt.setInt(i++, dto.getFolio());
            stmt.setString(i++, dto.getApellidoPaterno());
            stmt.setString(i++, dto.getApellidoMaterno());
            stmt.setString(i++, dto.getNombre());
            stmt.setInt(i++, dto.getEdad());
            stmt.setDate(i++, dto.getFechaNacimiento());
            stmt.setString(i++, dto.getTelefono());
            stmt.setString(i++, dto.getDomicilio());
            stmt.setInt(i++, dto.getDerechoHabiencia());
            stmt.setString(i++, dto.getNoAfiliacion());
            stmt.setString(i++, dto.getReferencia());
            stmt.setBoolean(i++, dto.isReingreso());
            stmt.setBoolean(i++, dto.isHospitalizado());
            stmt.setString(i++, dto.getExpediente());
            stmt.setString(i++, dto.getCurp());
            stmt.setString(i++, dto.getSintomas());
            stmt.setString(i++, dto.getMedico());
            stmt.setString(i++, dto.getTriage());
            stmt.setString(i++, dto.getNombreMedico());
            stmt.setString(i++, dto.getTurno());
            stmt.setString(i++, dto.getHora());
            stmt.setInt(i++, dto.getSexo());
            stmt.setString(i++, dto.getCodigoMunicipio());
            stmt.setString(i++, dto.getCodigoEntidad());
            stmt.setString(i++, dto.getOcupacion());
            stmt.setString(i++, dto.getReligion());
            stmt.setString(i++, dto.getEstadoCivil());
            stmt.setString(i++, dto.getObservaciones());
            stmt.setString(i++, dto.getMunicipioSeleccionado());
            stmt.setString(i, dto.getEntidadSeleccionada());
            boolean resultado = stmt.executeUpdate() > 0;
            log.info("Paciente insertado correctamente: {}", resultado);
            return resultado;
        } catch (SQLException e) {
            log.error("Error al insertar paciente en la base de datos: {}", e.getMessage());
            return false;
        }
    }

    public int obtenerFolio() {
        try (Connection connection = ConexionBD.conectar(); PreparedStatement stmt = connection.prepareStatement(OBTENER_ID_MAXIMO); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int folio = rs.getInt(1);
                log.debug("Folio máximo obtenido: {}", folio);
                return folio;
            }
            log.warn("No se encontró folio máximo, asignando folio inicial 1");
            return 1;
        } catch (SQLException e) {
            log.error("Error al obtener el folio: {}", e.getMessage());
            return -1;
        }
    }
}
