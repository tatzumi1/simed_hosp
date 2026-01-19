package com.PruebaSimed2.database;

import com.PruebaSimed2.DTO.Urgencias.ActualizarCapturaPrincipalDTO;
import com.PruebaSimed2.DTO.Urgencias.CargarDatosPacienteDTO;
import com.PruebaSimed2.DTO.Urgencias.CargarNuevaInformacionDTO;
import com.PruebaSimed2.DTO.Urgencias.InsertarPacienteDTO;
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
    private static final String CARGAR_DATOS_PACIENTE = "SELECT u.*, " +
            "m.DESCRIP as NombreMunicipio, e.DESCRIP as NombreEntidad, " +
            "dh.Derechohabiencia as NombreDerechohabiencia " +
            "FROM tb_urgencias u " +
            "LEFT JOIN tblt_mpo m ON u.Municipio_resid = m.MPO AND u.Entidad_resid = m.EDO " +
            "LEFT JOIN tblt_entidad e ON u.Entidad_resid = e.EDO " +
            "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
            "WHERE u.Folio = ?";
    private static final String CARGAR_NUEVA_INFORMACION = "SELECT Tipo_urg, Motivo_urg, Tipo_cama, Cve_med, Nom_med, Estado_pac FROM tb_urgencias WHERE Folio = ?";
    private static final String ACTUALIZAR_CAPTURA_PRINCIPAL = "UPDATE tb_urgencias SET Tipo_urg = ?, Motivo_urg = ?, Tipo_cama = ?, Cve_med = ?, Nom_med = ?, Fecha_atencion = NOW(), Hora_atencion = CURTIME() WHERE Folio = ?";
    private static final String ACTUALIZAR_ESTADO_PACIENTE = "UPDATE tb_urgencias SET Estado_pac = ? WHERE Folio = ?";
    private static final String OBTENER_ESTADO_PACIENTE = "SELECT Estado_pac FROM tb_urgencias WHERE Folio = ?";

    public boolean insertarPaciente(InsertarPacienteDTO dto, Connection connection) {
        log.debug("Insertando paciente en la base de datos: {}", dto);
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PACIENTE)) {
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

    public int obtenerFolio(Connection connection) {
        try (PreparedStatement stmt = connection.prepareStatement(OBTENER_ID_MAXIMO); ResultSet rs = stmt.executeQuery()) {
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

    public CargarDatosPacienteDTO cardarDatosPaciente(int folio, Connection connection) {
        try (PreparedStatement stmt = connection.prepareStatement(CARGAR_DATOS_PACIENTE)) {
            stmt.setInt(1, folio);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                log.debug("Datos del paciente con folio {} encontrado.", folio);
                var dto = new CargarDatosPacienteDTO();

                dto.setFolio(rs.getInt("Folio"));
                dto.setFecha(rs.getTimestamp("Fecha"));
                dto.setHoraRegistro(rs.getTime("Hora_registro"));
                dto.setTriage(rs.getString("TRIAGE"));
                dto.setApellidoPaterno(rs.getString("A_paterno"));
                dto.setApellidoMaterno(rs.getString("A_materno"));
                dto.setNombre(rs.getString("Nombre"));
                dto.setEdad(rs.getInt("Edad"));
                dto.setSexo(rs.getInt("Sexo"));
                dto.setDomicilio(rs.getString("Domicilio"));
                dto.setMunicipioResid(rs.getString("Municipio_resid"));
                dto.setEntidadResid(rs.getString("Entidad_resid"));
                dto.setNombreMunicipio(rs.getString("NombreMunicipio"));
                dto.setNombreEntidad(rs.getString("NombreEntidad"));
                dto.setNombreDerechoHabiencia(rs.getString("NombreDerechohabiencia"));
                dto.setReferencia(rs.getString("Referencia"));
                dto.setSintomas(rs.getString("Sintomas"));
                dto.setNombreMedico(rs.getString("Nom_med"));

                log.debug("Datos del paciente cargados: {}", dto);
                return dto;
            }
        } catch (SQLException e) {
            log.error("Error al cargar datos del paciente: {}", e.getMessage());
        }
        return null;
    }

    public CargarNuevaInformacionDTO cargarNuevaInformacion(int folio, Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(CARGAR_NUEVA_INFORMACION)) {
            statement.setInt(1, folio);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                var dto = new CargarNuevaInformacionDTO(
                        rs.getInt("Tipo_urg"),
                        rs.getInt("Motivo_urg"),
                        rs.getInt("Tipo_cama"),
                        rs.getInt("Cve_med"),
                        rs.getString("Nom_med"),
                        rs.getInt("Estado_pac"));
                log.debug("Nueva información cargada: {}", dto);
                return dto;
            } else {
                log.warn("No se encontraron datos para la urgencia con folio {}", folio);
                return null;
            }
        } catch (SQLException e) {
            log.error("Error al cargar datos de la urgencia: {}", e.getMessage());
            return null;
        }
    }

    public boolean actualizarCapturaPrincipal(Connection connection, ActualizarCapturaPrincipalDTO dto) {
        try (PreparedStatement statement = connection.prepareStatement(ACTUALIZAR_CAPTURA_PRINCIPAL)) {
            statement.setInt(1, dto.getCveUrg());
            statement.setInt(2, dto.getCveMotatn());
            statement.setInt(3, dto.getCveCama());
            statement.setInt(4, dto.getCveMedico());
            statement.setString(5, dto.getMedico());
            statement.setInt(6, dto.getFolioPaciente());
            int filas = statement.executeUpdate();
            if (filas > 0) {
                log.info("Captura principal actualizada correctamente");
                return true;
            } else {
                log.warn("No se pudo actualizar captura principal");
                return false;
            }
        } catch (SQLException e) {
            log.error("Error actualizando captura principal: {}", e.getMessage());
            return false;
        }
    }

    public void actualizarEstadoPaciente(Connection connection, int folio, int estado) {
        try (PreparedStatement statement = connection.prepareStatement(ACTUALIZAR_ESTADO_PACIENTE)) {
            statement.setInt(1, estado);
            statement.setInt(2, folio);
            statement.executeUpdate();
            log.info("Estado del paciente con folio {} actualizado a {}", folio, estado);
        } catch (SQLException e) {
            log.error("Error actualizando estado del paciente con folio: {}", folio, e);
        }
    }

    public int obtenerEstadoPaciente(Connection connection, int folio) {
        try (PreparedStatement statement = connection.prepareStatement(OBTENER_ESTADO_PACIENTE)) {
            statement.setInt(1, folio);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int estado = rs.getInt("Estado_pac");
                log.debug("Estado del paciente con folio {}: {}", folio, estado);
                return estado;
            } else {
                log.warn("Estado del paciente con folio {} no encontrado", folio);
                return 1;
            }
        } catch (SQLException e) {
            log.error("Error al obtener el estado del paciente con folio: {}", folio, e);
            return 1;
        }
    }
}
