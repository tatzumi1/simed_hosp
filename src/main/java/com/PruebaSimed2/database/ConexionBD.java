//src/main/java/com/PruebaSimed2/database/ConexionBD.java

package com.PruebaSimed2.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

public class ConexionBD {

    // allowPublicKeyRetrieval=true
    //  private static final String URL = "jdbc:mysql://192.168.99.108:3306/bdsimed2_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    //private static final String USER = "hospital_user";
    //  private static final String PASSWORD = "Hospital2025!";

    // AGREGAR allowPublicKeyRetrieval=true para solucionar el error
    // ¡Las credenciales NO deben de ir aquí, deben de ir en un archivo de propiedades o variables de entorno!
    private static final String URL = "jdbc:mysql://localhost:3306/bdsimed2_db?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "EIMI";
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        System.out.println("Conectado a: " + URL);

        dataSource = new HikariDataSource(config);
    }


    public static Connection conectar() throws SQLException {
        return dataSource.getConnection();
    }

    public static boolean testConnection() {
        try (Connection conn = conectar()) {
            System.out.println(" ¡CONEXIÓN EXITOSA! La BD está funcionando");
            return conn.isValid(2);
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
                conn.close();
            } catch (SQLException e) {
                System.err.println("️Error al liberar conexión: " + e.getMessage());
            }
        }
    }

    public static boolean executeInTransaction(TransactionOperation operation) {
        try (Connection conn = conectar()) {
            conn.setAutoCommit(false); // INICIAR TRANSACCIÓN
            System.out.println(" Iniciando transacción...");
            try {
                boolean success = operation.execute(conn);
                if (success) {
                    conn.commit();
                } else {
                    conn.rollback();
                }
                return success;
            } catch (Exception e) {
                conn.rollback();
                System.err.println("Transacción fallida: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error en la BD" + e.getMessage());
            return false;
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
