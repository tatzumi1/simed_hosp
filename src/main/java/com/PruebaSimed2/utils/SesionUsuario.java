//src/main/java/com/PruebaSimed2/utils/SesionUsuario.java
package com.PruebaSimed2.utils;

import com.PruebaSimed2.database.ConexionBD;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.sql.*;

@Getter
@Log4j2
public class SesionUsuario {
    private static SesionUsuario instance;

    // Datos del usuario logueado
    private String username;
    private String rol;
    private String nombreMedico;
    private int usuarioId;

    // Constructor privado para Singleton
    private SesionUsuario() {
    }

    public static SesionUsuario getInstance() {
        if (instance == null) {
            instance = new SesionUsuario();
        }
        return instance;
    }


    public void inicializar(String username, String rol, int usuarioId) {
        this.username = username;
        this.rol = rol;
        this.usuarioId = usuarioId;
        this.nombreMedico = obtenerNombreMedicoDesdeUsuario(username);

        log.debug(" SESIÓN INICIADA:");
        log.debug("   - Usuario: {}", username);
        log.debug("   - Rol: {}", rol);
        log.debug("   - ID: {}", usuarioId);
        log.debug("   - Nombre Médico: {}", nombreMedico);
    }

    public boolean puedeCrearNuevaNota(int folioPaciente) {
        try (Connection conn = ConexionBD.conectar()) {
            // Verificar si YA TIENE una nota temporal en ESTE paciente
            String sql = "SELECT COUNT(*) as count FROM tb_notas " +
                    "WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folioPaciente);
                pstmt.setString(2, nombreMedico);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int count = rs.getInt("count");

                    if (count >= 1) {
                        log.warn(" BLOQUEADO - Ya tiene {} nota(s) temporal(es) en este paciente", count);
                        return false;
                    }
                }
            }

            log.debug(" PERMITIDO - Puede crear nueva nota en folio: {}", folioPaciente);
            return true;

        } catch (SQLException e) {
            log.error(" Error verificando notas temporales: {}", e.getMessage());
            return false;
        }
    }

    public boolean puedeCrearNuevaInterconsulta(int folioPaciente) {
        try (Connection conn = ConexionBD.conectar()) {
            // Verificar si YA TIENE una interconsulta temporal en ESTE paciente
            String sql = "SELECT COUNT(*) as count FROM tb_inter " +
                    "WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folioPaciente);
                pstmt.setString(2, nombreMedico);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int count = rs.getInt("count");

                    if (count >= 1) {
                        log.warn(" BLOQUEADO - Ya tiene {} interconsulta(s) temporal(es) en este paciente", count);
                        return false;
                    }
                }
            }

            log.debug("PERMITIDO - Puede crear nueva interconsulta en folio: {}", folioPaciente);
            return true;

        } catch (SQLException e) {
            log.error(" Error verificando interconsultas temporales: {}", e.getMessage());
            return false;
        }
    }

    public boolean puedeEditarNota(String medicoAutor) {
        log.debug(" VERIFICANDO PERMISOS EDICIÓN:");
        log.debug("   Médico Autor: {}", medicoAutor);
        log.debug("   Nombre Sesión: {}", nombreMedico);
        log.debug("   Rol: {}", rol);

        // Admin y jefatura pueden editar CUALQUIER nota
        if ("ADMIN".equals(rol) || "JEFATURA_URGENCIAS".equals(rol)) {
            log.debug(" PERMITIDO ({}) - Puede editar cualquier nota", rol);
            return true;
        }

        // Médico normal solo puede editar SUS notas
        boolean esAutor = medicoAutor != null && medicoAutor.equals(nombreMedico);

        log.debug("   ¿Es Autor? {}", esAutor);

        if (esAutor) {
            log.debug(" PERMITIDO - Es el médico autor de la nota");
            return true;
        } else {
            log.warn(" BLOQUEADO - No es el médico autor");
            return false;
        }
    }


    public boolean puedeEditarInterconsulta(String especialistaAutor) {
        // Admin y jefatura pueden editar CUALQUIER interconsulta
        if ("ADMIN".equals(rol) || "JEFATURA_URGENCIAS".equals(rol)) {
            log.debug(" PERMITIDO ({}) - Puede editar cualquier interconsulta", rol);
            return true;
        }

        // Especialista normal solo puede editar SUS interconsultas
        boolean esAutor = especialistaAutor != null && especialistaAutor.equals(nombreMedico);

        if (esAutor) {
            log.debug(" PERMITIDO - Es el especialista autor de la interconsulta");
            return true;
        } else {
            log.warn(" BLOQUEADO - No es el especialista autor. Autor: {}, Actual: {}", especialistaAutor, nombreMedico);
            return false;
        }
    }

    private String obtenerNombreMedicoDesdeUsuario(String username) {
        log.debug(" [SesionUsuario] Buscando nombre médico para: {}", username);

        String sql = "SELECT m.Med_nombre " +
                "FROM tb_medicos m " +
                "INNER JOIN tb_usuarios u ON m.Ced_prof = u.empleado_id " +
                "WHERE u.username = ? " +
                "UNION " +
                "SELECT nombre_completo FROM tb_usuarios WHERE username = ? " +
                "UNION " +
                "SELECT Med_nombre FROM tb_medicos WHERE Med_nombre LIKE CONCAT('%', ?, '%') " +
                "LIMIT 1";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setString(3, username);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String nombreMedico = rs.getString("Med_nombre");
                log.debug(" [SesionUsuario] Médico encontrado: {}", nombreMedico);
                return nombreMedico;
            } else {
                // Fallback a nombre_completo
                String fallback = obtenerNombreCompletoFallback(username);
                log.warn(" [SesionUsuario] Usando fallback: {}", fallback);
                return fallback;
            }

        } catch (SQLException e) {
            log.error(" Error en SesionUsuario: {}", e.getMessage());
            return username;
        }
    }

    private String obtenerNombreCompletoFallback(String username) {
        String sql = "SELECT nombre_completo FROM tb_usuarios WHERE username = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String nombreCompleto = rs.getString("nombre_completo");
                return (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) ? nombreCompleto : username;
            }
        } catch (SQLException e) {
            log.error(" Error obteniendo nombre_completo: {}", e.getMessage());
        }

        return username;
    }

    // ==================== GETTERS ====================


    public Integer obtenerNotaTemporalExistente(int folioPaciente) {
        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT id_nota FROM tb_notas " +
                    "WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ? " +
                    "ORDER BY id_nota DESC LIMIT 1";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folioPaciente);
                pstmt.setString(2, nombreMedico);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int idNota = rs.getInt("id_nota");
                    log.debug(" Nota temporal existente encontrada - ID: {}", idNota);
                    return idNota;
                }
            }
        } catch (SQLException e) {
            log.error(" Error buscando nota temporal: {}", e.getMessage());
        }
        return null;
    }

    public Integer obtenerInterconsultaTemporalExistente(int folioPaciente) {
        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT id_inter FROM tb_inter " +
                    "WHERE Folio = ? AND Estado = 'TEMPORAL' AND Medico = ? " +
                    "ORDER BY id_inter DESC LIMIT 1";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folioPaciente);
                pstmt.setString(2, nombreMedico);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int idInter = rs.getInt("id_inter");
                    log.debug(" Interconsulta temporal existente encontrada - ID: {}", idInter);
                    return idInter;
                }
            }
        } catch (SQLException e) {
            log.error(" Error buscando interconsulta temporal: {}", e.getMessage());
        }
        return null;
    }

    public void cerrarSesion() {
        this.username = null;
        this.rol = null;
        this.nombreMedico = null;
        this.usuarioId = -1;
        log.info(" Sesión cerrada");
    }
}