//src/main/java/com/PruebaSimed2/database/ConexionBD.java

package com.PruebaSimed2.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

public class ConexionBD {

    // allowPublicKeyRetrieval=true
  //  private static final String URL = "jdbc:mysql://192.168.99.108:3306/bdsimed2_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    //private static final String USER = "hospital_user";
  //  private static final String PASSWORD = "Hospital2025!";

    // AGREGAR allowPublicKeyRetrieval=true para solucionar el error
    private static final String URL = "jdbc:mysql://localhost:3306/bdsimed2_db?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "EIMI";



    public static Connection conectar() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println(" Conectando a: " + URL);

            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            // CONFIGURACIONES PARA MEJOR ESTABILIDAD
            conn.setAutoCommit(true); // Por defecto auto-commit true
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            System.out.println(" Conexión establecida - AutoCommit: " + conn.getAutoCommit());
            return conn;

        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver no encontrado", e);
        } catch (SQLException e) {
            System.err.println(" Error de conexión: " + e.getMessage());
            throw e;
        }
    }

    public static boolean testConnection() {
        try (Connection conn = conectar()) {
            System.out.println(" ¡CONEXIÓN EXITOSA! La BD está funcionando");
            return true;
        } catch (SQLException e) {
            System.err.println(" ERROR DE CONEXIÓN: " + e.getMessage());
            return false;
        }
    }

    public static Connection getSafeConnection() throws SQLException {
        try {
            Connection conn = conectar();
            if (conn == null || conn.isClosed()) {
                throw new SQLException("No se pudo establecer conexión con la base de datos");
            }
            return conn;
        } catch (SQLException e) {
            System.err.println(" ERROR CRÍTICO DE CONEXIÓN: " + e.getMessage());
            throw new SQLException("El sistema no está disponible temporalmente. Contacte al administrador.", e);
        }
    }

    public static void safeClose(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    // Restaurar auto-commit a true antes de cerrar
                    conn.setAutoCommit(true);
                    conn.close();
                    System.out.println(" Conexión cerrada correctamente");
                }
            } catch (SQLException e) {
                System.err.println("️ Advertencia al cerrar conexión: " + e.getMessage());
                // NO relanzar la excepción - es solo limpieza
            }
        }
    }

    public static boolean executeInTransaction(TransactionOperation operation) {
        Connection conn = null;
        boolean success = false;

        try {
            conn = getSafeConnection();
            conn.setAutoCommit(false); // INICIAR TRANSACCIÓN

            System.out.println(" Iniciando transacción...");

            // Ejecutar la operación del usuario
            success = operation.execute(conn);

            if (success) {
                conn.commit();
                System.out.println(" Transacción COMPLETADA exitosamente");
            } else {
                conn.rollback();
                System.out.println(" Transacción CANCELADA - Rollback ejecutado");
            }

            return success;

        } catch (SQLException e) {
            System.err.println(" ERROR en transacción: " + e.getMessage());

            // Hacer ROLLBACK en caso de error
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println(" Rollback de emergencia ejecutado");
                } catch (SQLException rollbackEx) {
                    System.err.println(" ERROR haciendo rollback: " + rollbackEx.getMessage());
                }
            }
            return false;

        } finally {
            // CERRAR CONEXIÓN de forma segura
            safeClose(conn);
        }
    }

    @FunctionalInterface
    public interface TransactionOperation {
        boolean execute(Connection conn) throws SQLException;
    }

    public static boolean isConnectionAlive(Connection conn) {
        if (conn == null) return false;

        try {
            return !conn.isClosed() && conn.isValid(2); // 2 segundos de timeout
        } catch (SQLException e) {
            return false;
        }
    }
}
