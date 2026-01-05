//src/main/java/com/PruebaSimed2/utils/SesionUsuario.java
package com.PruebaSimed2.utils;

import com.PruebaSimed2.database.ConexionBD;
import java.sql.*;

public class SesionUsuario {
    private static SesionUsuario instance;

    // Datos del usuario logueado
    private String username;
    private String rol;
    private String nombreMedico;
    private int usuarioId;

    // Constructor privado para Singleton
    private SesionUsuario() {}

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

        System.out.println(" SESIÓN INICIADA:");
        System.out.println("   - Usuario: " + username);
        System.out.println("   - Rol: " + rol);
        System.out.println("   - ID: " + usuarioId);
        System.out.println("   - Nombre Médico: " + nombreMedico);
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
                        System.out.println(" BLOQUEADO - Ya tiene " + count + " nota(s) temporal(es) en este paciente");
                        return false;
                    }
                }
            }

            System.out.println(" PERMITIDO - Puede crear nueva nota en folio: " + folioPaciente);
            return true;

        } catch (SQLException e) {
            System.err.println(" Error verificando notas temporales: " + e.getMessage());
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
                        System.out.println(" BLOQUEADO - Ya tiene " + count + " interconsulta(s) temporal(es) en este paciente");
                        return false;
                    }
                }
            }

            System.out.println("PERMITIDO - Puede crear nueva interconsulta en folio: " + folioPaciente);
            return true;

        } catch (SQLException e) {
            System.err.println(" Error verificando interconsultas temporales: " + e.getMessage());
            return false;
        }
    }

    public boolean puedeEditarNota(String medicoAutor) {
        System.out.println(" VERIFICANDO PERMISOS EDICIÓN:");
        System.out.println("   Médico Autor: " + medicoAutor);
        System.out.println("   Nombre Sesión: " + nombreMedico);
        System.out.println("   Rol: " + rol);

        // Admin y jefatura pueden editar CUALQUIER nota
        if ("ADMIN".equals(rol) || "JEFATURA_URGENCIAS".equals(rol)) {
            System.out.println(" PERMITIDO (" + rol + ") - Puede editar cualquier nota");
            return true;
        }

        // Médico normal solo puede editar SUS notas
        boolean esAutor = medicoAutor != null && medicoAutor.equals(nombreMedico);

        System.out.println("   ¿Es Autor? " + esAutor);

        if (esAutor) {
            System.out.println(" PERMITIDO - Es el médico autor de la nota");
            return true;
        } else {
            System.out.println(" BLOQUEADO - No es el médico autor");
            return false;
        }
    }


    public boolean puedeEditarInterconsulta(String especialistaAutor) {
        // Admin y jefatura pueden editar CUALQUIER interconsulta
        if ("ADMIN".equals(rol) || "JEFATURA_URGENCIAS".equals(rol)) {
            System.out.println(" PERMITIDO (" + rol + ") - Puede editar cualquier interconsulta");
            return true;
        }

        // Especialista normal solo puede editar SUS interconsultas
        boolean esAutor = especialistaAutor != null && especialistaAutor.equals(nombreMedico);

        if (esAutor) {
            System.out.println(" PERMITIDO - Es el especialista autor de la interconsulta");
            return true;
        } else {
            System.out.println(" BLOQUEADO - No es el especialista autor. Autor: " + especialistaAutor + ", Actual: " + nombreMedico);
            return false;
        }
    }

    private String obtenerNombreMedicoDesdeUsuario(String username) {
        System.out.println(" [SesionUsuario] Buscando nombre médico para: " + username);

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
                System.out.println(" [SesionUsuario] Médico encontrado: " + nombreMedico);
                return nombreMedico;
            } else {
                // Fallback a nombre_completo
                String fallback = obtenerNombreCompletoFallback(username);
                System.out.println(" [SesionUsuario] Usando fallback: " + fallback);
                return fallback;
            }

        } catch (SQLException e) {
            System.err.println(" Error en SesionUsuario: " + e.getMessage());
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
            System.err.println(" Error obteniendo nombre_completo: " + e.getMessage());
        }

        return username;
    }

    // ==================== GETTERS ====================

    public String getUsername() { return username; }
    public String getRol() { return rol; }
    public String getNombreMedico() { return nombreMedico; }
    public int getUsuarioId() { return usuarioId; }


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
                    System.out.println(" Nota temporal existente encontrada - ID: " + idNota);
                    return idNota;
                }
            }

        } catch (SQLException e) {
            System.err.println(" Error buscando nota temporal: " + e.getMessage());
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
                    System.out.println(" Interconsulta temporal existente encontrada - ID: " + idInter);
                    return idInter;
                }
            }

        } catch (SQLException e) {
            System.err.println(" Error buscando interconsulta temporal: " + e.getMessage());
        }

        return null;
    }

    public void cerrarSesion() {
        this.username = null;
        this.rol = null;
        this.nombreMedico = null;
        this.usuarioId = -1;
        System.out.println(" Sesión cerrada");
    }
}