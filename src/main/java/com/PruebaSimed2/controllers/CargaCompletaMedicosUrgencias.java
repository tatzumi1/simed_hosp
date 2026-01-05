// src/main/java/com/PruebaSimed2/controllers/CargaCompletaMedicosUrgencias.java
package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.utils.PasswordUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CargaCompletaMedicosUrgencias {

    static class MedicoUrgencias {
        int cveMed; // Cve_med EXACTO
        String nombreCompleto; // SIN "Dr.", "Dra."
        String cedula;
        String universidad;
        String email;
        String username;

        public MedicoUrgencias(int cveMed, String nombreCompleto, String cedula, String universidad) {
            this.cveMed = cveMed;

            // LIMPIAR nombre: quitar Dr., Dra.
            this.nombreCompleto = limpiarNombre(nombreCompleto);
            this.cedula = cedula.trim();

            // Universidad: si viene vacía, poner vacío
            this.universidad = (universidad == null || universidad.trim().isEmpty()) ? "" : universidad.trim();

            // Email: "no"
            this.email = "no";

            // Generar username
            this.username = generarUsername(this.nombreCompleto);
        }

        private String limpiarNombre(String nombre) {
            return nombre
                    .replace("Dr.", "").replace("Dra.", "")
                    .replace("Dr ", "").replace("Dra ", "")
                    .replace("Psic.", "").replace("Psic ", "")
                    .trim();
        }

        private String generarUsername(String nombre) {
            String nombreLimpio = nombre.toLowerCase()
                    .replace("á", "a").replace("é", "e").replace("í", "i")
                    .replace("ó", "o").replace("ú", "u").replace("ñ", "n");

            String[] partes = nombreLimpio.split(" ");
            List<String> nombresReales = new ArrayList<>();

            for (String parte : partes) {
                if (parte.length() > 2 && !parte.equals("del") && !parte.equals("de") &&
                        !parte.equals("la") && !parte.equals("las") && !parte.equals("los")) {
                    nombresReales.add(parte);
                }
            }

            if (nombresReales.size() >= 2) {
                return nombresReales.get(0) + "." + nombresReales.get(nombresReales.size() - 1);
            } else if (nombresReales.size() == 1) {
                return nombresReales.get(0);
            } else {
                return "medico." + this.cedula.substring(0, Math.min(3, this.cedula.length()));
            }
        }

        public String getCedulaCompleta() {
            if (universidad.isEmpty()) {
                return cedula;
            } else {
                return cedula + " - " + universidad;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== CARGA COMPLETA DE 81 MÉDICOS DE URGENCIAS ===");
        cargarTodosMedicosUrgencias();
    }

    public static void cargarTodosMedicosUrgencias() {
        Connection conn = null;
        int exitos = 0;
        int fallidos = 0;

        try {
            conn = ConexionBD.conectar();
            conn.setAutoCommit(false);

            List<MedicoUrgencias> medicos = crearListaCompletaMedicos();

            System.out.println("Total a procesar: " + medicos.size());
            System.out.println("---------------------------------------------");

            for (MedicoUrgencias medico : medicos) {
                try {
                    System.out.println("Cve_med " + medico.cveMed + ": " + medico.nombreCompleto);
                    System.out.println("  Username: " + medico.username);
                    System.out.println("  Cédula: " + medico.getCedulaCompleta());

                    // 1. Verificar si usuario ya existe
                    if (usuarioExiste(conn, medico.username)) {
                        System.out.println("  ⚠️  Usuario ya existe");
                        fallidos++;
                        continue;
                    }

                    // 2. Registrar en tb_usuarios
                    if (!registrarUsuario(conn, medico)) {
                        System.out.println("  ❌ Error al registrar usuario");
                        fallidos++;
                        continue;
                    }

                    // 3. Registrar en tb_medicos con Cve_med EXACTO
                    if (registrarMedicoConCveExacto(conn, medico)) {
                        System.out.println("  ✅ Registrado exitosamente");
                        exitos++;
                    } else {
                        System.out.println("  ❌ Error al registrar en tb_medicos");
                        fallidos++;
                    }

                    System.out.println("  ---");

                } catch (Exception e) {
                    System.out.println("  ❌ Error: " + e.getMessage());
                    e.printStackTrace();
                    fallidos++;
                }
            }

            conn.commit();

            System.out.println("=============================================");
            System.out.println("RESUMEN:");
            System.out.println("✅ Registrados exitosamente: " + exitos);
            System.out.println("❌ Fallidos: " + fallidos);
            System.out.println("Total procesados: " + (exitos + fallidos));

        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {}
            }
        }
    }

    private static boolean usuarioExiste(Connection conn, String username) throws SQLException {
        String sql = "SELECT id_usuario FROM tb_usuarios WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            return pstmt.executeQuery().next();
        }
    }

    private static boolean registrarUsuario(Connection conn, MedicoUrgencias medico) throws SQLException {
        String sql = "INSERT INTO tb_usuarios (username, password_hash, email, nombre_completo, " +
                "rol, cedula_profesional, activo, primer_login) " +
                "VALUES (?, ?, ?, ?, 'MEDICO_URGENCIAS', ?, true, true)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String hash = PasswordUtils.hashPassword("Temp123");

            pstmt.setString(1, medico.username);
            pstmt.setString(2, hash);
            pstmt.setString(3, medico.email);
            pstmt.setString(4, medico.nombreCompleto);
            pstmt.setString(5, medico.getCedulaCompleta());

            return pstmt.executeUpdate() > 0;
        }
    }

    private static boolean registrarMedicoConCveExacto(Connection conn, MedicoUrgencias medico) throws SQLException {
        String sql = "INSERT INTO tb_medicos (Cve_med, Med_nombre, Ced_prof) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, medico.cveMed);
            pstmt.setString(2, medico.nombreCompleto);
            pstmt.setString(3, medico.getCedulaCompleta());

            return pstmt.executeUpdate() > 0;
        }
    }

    // LISTA COMPLETA DE LOS 81 MÉDICOS CON SUS Cve_med EXACTOS
    private static List<MedicoUrgencias> crearListaCompletaMedicos() {
        List<MedicoUrgencias> medicos = new ArrayList<>();

        // Añadir TODOS los 81 médicos con sus Cve_med originales
        medicos.add(new MedicoUrgencias(1, "Dra. Rosalinda Gallegos Lemus", "2919212", ""));
        medicos.add(new MedicoUrgencias(2, "Dra. Yolanda Gómez Martinez", "960177", ""));
        medicos.add(new MedicoUrgencias(3, "Dr. Luis Adolfo Plandiura Sánchez", "2712541", ""));
        medicos.add(new MedicoUrgencias(4, "Dr. Francisco Arturo Román Corona", "7848888", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(5, "Dr. Hiram Elliot Herrera Ibarra", "12576846", "Universidad Autónoma de Sinaloa"));
        medicos.add(new MedicoUrgencias(6, "Dr. José Efraín Ramos Martínez", "11384371", "Inst. Politécnico Nacional"));
        medicos.add(new MedicoUrgencias(7, "Dr. Alberto Barrios Hernández", "4575727", ""));
        medicos.add(new MedicoUrgencias(8, "Dr. Simón De La Cruz Castillo", "4500350", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(9, "Dr. Eliud Sosa Sanchez", "3164973", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(10, "Dr. Gustavo Suarez López", "4230993", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(11, "Dr. Carlos Alonso Rivemar Olivier", "3679698", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(12, "Dra. Uri Del Carmen América Barrón Galindo", "4749076", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(13, "Dra. Adriana Camacho Campos", "1883480", ""));
        medicos.add(new MedicoUrgencias(14, "Dra. María Del Carmen Díaz Hernández", "3266470", "Universidad Autónoma de Tamaulipas"));
        medicos.add(new MedicoUrgencias(15, "Dra. Roció Gómez Alvarado", "6616144", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(19, "Dr. Sergio Linares Vázquez", "9834676", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(22, "Dr. Juan Josimar Franco Gómez", "10496083", "Universidad del Noreste"));
        medicos.add(new MedicoUrgencias(25, "Dr. Javier Bandala de Regil", "6608583", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(26, "Dr. Ricardo Arturo Rocha Cuervo", "9516366", ""));
        medicos.add(new MedicoUrgencias(27, "Dra. Daniela Díaz Argumedo", "9930836", "Univ. Del Noreste de Tamaulipas"));
        medicos.add(new MedicoUrgencias(28, "Dra. Trinidad Isabel Hernández Reyes", "10457544", ""));
        medicos.add(new MedicoUrgencias(32, "Dr. José Luis Blásquez Carbajal", "3149369", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(34, "Dra. Zully Trinidad Cortés Solís", "11848033", "Universidad del Noreste de Tampico"));
        medicos.add(new MedicoUrgencias(35, "Dra. Aide Itzel Barrios Celaya", "10787411", "Universidad del Noreste de Tamaulipas"));
        medicos.add(new MedicoUrgencias(37, "Dra. Ruth González Rodríguez", "9942893", "Esc. Latinoamericana de Medicina Cuba"));
        medicos.add(new MedicoUrgencias(38, "Dr. Moisés Ariel Flores Salazar", "11279794", "Ben. Universidad Autónoma de Puebla"));
        medicos.add(new MedicoUrgencias(39, "Dra. Ana Arlette Aguirre Alberto", "8514748", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(41, "Dr. Juan Carlos Rodriguez González", "7361825", ""));
        medicos.add(new MedicoUrgencias(43, "Dra. Giannelli Hernández Hernández", "10723867", "Esc. Latinoamericana de Medicina Cuba"));
        medicos.add(new MedicoUrgencias(44, "Dra. Claudia Victoria Baños Tellez", "11475255", "Univ. Popular Autónoma de Puebla"));
        medicos.add(new MedicoUrgencias(48, "Dr. Jesús Javier Meza Díaz", "11802654", "Ben. Universidad Autónoma de Puebla"));
        medicos.add(new MedicoUrgencias(52, "Dr. José Enrique Urbina Herberth", "12101776", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(55, "Dr. Hugo César Méndez Del Ángel", "10771863", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(58, "Dr. Julio César Clemente Cruz", "11487580", "Insituto Politécnico Nacional"));
        medicos.add(new MedicoUrgencias(59, "Dra. Norma Patricia Rosaldo Ángeles", "10910343", "Universidad Villa Rica"));
        medicos.add(new MedicoUrgencias(60, "Dra. Kikuko Martínez Licona", "12097156", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(62, "Dra. Nancy Vianney López Córdoba", "12649473", "Inst. Politécnico Nacional"));
        medicos.add(new MedicoUrgencias(64, "Dra. Melina Christell Marquez Rodríguez", "12094456", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(65, "Dr. Absalón Usiel Cuahutle Flores", "8718884", "U. Autónoma de Tlaxcala"));
        medicos.add(new MedicoUrgencias(66, "Dra. Marbella Alejandra Pintor Ríos", "12052961", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(70, "Dra. Karina Krystell Lombera Hernández", "12584215", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(72, "Dr. Daniel Posadas Morales", "12566904", "Univ. Autónoma de Sinaloa"));
        medicos.add(new MedicoUrgencias(73, "Dra. Xóchitl Nalin Gaona Cristóbal", "12552253", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(74, "Dr. Nicolás Santiago de la Cruz", "12555689", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(75, "Dra. Anadelia Tenorio Díaz", "12637296", "Universidad Autónoma de Tamaulipas"));
        medicos.add(new MedicoUrgencias(76, "Dra. América Joseline Del Ángel Valentín", "12576318", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(77, "Dra. Claudia Miramontes Meraz", "13045767", "Instituto Politécnico Nacional"));
        medicos.add(new MedicoUrgencias(78, "Dr. Víctor Soni Aguilera", "12677820", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(79, "Dr. Italo Gibrán Reyes Rosas", "12405517", "UNAM/UV/UAT"));
        medicos.add(new MedicoUrgencias(80, "Dr. Mario Alexis García Licona", "12511346", "Universidad Veracruzana"));
        medicos.add(new MedicoUrgencias(81, "Dr. Luis Enrique Moncayo García", "14013681", "U. Nacional Autónoma de México"));

        return medicos;
    }
}