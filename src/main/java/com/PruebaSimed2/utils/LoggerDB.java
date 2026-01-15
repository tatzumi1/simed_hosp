// src/main/java/com/PruebaSimed2/utils/LoggerDB.java
package com.PruebaSimed2.utils;

import com.PruebaSimed2.database.ConexionBD;
import lombok.extern.log4j.Log4j2;

import java.sql.*;
import java.net.InetAddress;

@Log4j2
public class LoggerDB {

    public static void log(String nivel, String modulo, String usuario,
                           String rol, String accion, Integer folioPaciente,
                           String detalles) {

        new Thread(() -> {
            Connection conn = null;
            try {
                conn = ConexionBD.conectar();
                String sql = "INSERT INTO tb_logs_sistema " +
                        "(nivel, modulo, usuario, rol_usuario, accion, " +
                        "folio_paciente, detalles, ip_equipo, equipo_hostname) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, nivel);
                pstmt.setString(2, modulo);
                pstmt.setString(3, usuario);
                pstmt.setString(4, rol);
                pstmt.setString(5, accion);
                pstmt.setObject(6, folioPaciente, Types.INTEGER);
                pstmt.setString(7, detalles);

                try {
                    String ip = InetAddress.getLocalHost().getHostAddress();
                    String hostname = InetAddress.getLocalHost().getHostName();
                    pstmt.setString(8, ip);
                    pstmt.setString(9, hostname);
                } catch (Exception e) {
                    pstmt.setString(8, "N/A");
                    pstmt.setString(9, "N/A");
                }

                pstmt.executeUpdate();
                pstmt.close();

            } catch (SQLException e) {
                log.error(" [LOG FALLÓ] {} | {} | {}", nivel, usuario, accion);
            } finally {
                ConexionBD.safeClose(conn);
            }
        }).start();
    }

    public static void logLogin(String usuario, String rol) {
        log("INFO", "AUTENTICACION", usuario, rol, "LOGIN", null, "Usuario inició sesión");
    }

    public static void logAuditoria(String usuario, String accion, int folio, String detalles) {
        log("AUDIT", "CAPTURA", usuario, "MEDICO", accion, folio, detalles);
    }

    public static void logError(String modulo, String metodo, String mensaje, Exception e) {
        log("ERROR", modulo, "SISTEMA", "SISTEMA", metodo, null,
                mensaje + " | Error: " + e.getMessage());
    }

    public static void logBD(String operacion, String query, int filas) {
        String queryCorta = query.length() > 100 ? query.substring(0, 100) + "..." : query;
        log("INFO", "BD", "SISTEMA", "SISTEMA", operacion, null,
                "Filas: " + filas + " | Query: " + queryCorta);
    }
}