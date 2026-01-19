package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.utils.PasswordUtils;
import lombok.extern.log4j.Log4j2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class CargaMasivaMedicos {

    // Clase para representar un médico especialista
    static class MedicoEspecialista {
        String nombreCompleto;
        String cedula;
        String universidad;
        String email;
        String username;

        public MedicoEspecialista(String nombreCompleto, String cedula, String universidad) {
            this.nombreCompleto = nombreCompleto;
            this.cedula = cedula;

            // Universidad: "." si no tiene
            this.universidad = (universidad == null || universidad.trim().isEmpty() ||
                    universidad.equals("NO ESPECIFICADA")) ? "." : universidad;

            // Email: siempre "no"
            this.email = "no";

            // Generar username: nombre.apellido
            this.username = generarUsernameSimple(nombreCompleto);
        }

        private String generarUsernameSimple(String nombre) {
            // Limpiar nombre (quitar Dr., Dra., Psic.)
            String nombreLimpio = nombre
                    .replace("Dr.", "").replace("Dra.", "").replace("Psic.", "")
                    .replace("Dr ", "").replace("Dra ", "").replace("Psic ", "")
                    .trim();

            // Quitar acentos y convertir a minúsculas
            nombreLimpio = nombreLimpio.toLowerCase()
                    .replace("á", "a").replace("é", "e").replace("í", "i")
                    .replace("ó", "o").replace("ú", "u").replace("ñ", "n");

            // Dividir en partes
            String[] partes = nombreLimpio.split(" ");
            List<String> nombresReales = new ArrayList<>();

            // Filtrar palabras vacías y muy cortas
            for (String parte : partes) {
                if (parte.length() > 2 && !parte.equals("del") && !parte.equals("de") &&
                        !parte.equals("la") && !parte.equals("las") && !parte.equals("los")) {
                    nombresReales.add(parte);
                }
            }

            if (nombresReales.size() >= 2) {
                // Tomar primer nombre y primer apellido
                String nombreSimple = nombresReales.get(0);
                String apellido = nombresReales.get(nombresReales.size() - 1);
                return nombreSimple + "." + apellido;
            } else if (nombresReales.size() == 1) {
                return nombresReales.get(0);
            } else {
                // Fallback: usar las primeras letras
                return "medico." + this.cedula.substring(0, Math.min(3, this.cedula.length()));
            }
        }
    }

    // Método principal para ejecutar
    public static void main(String[] args) {
        cargarMedicosEspecialistas();
        log.info("CARGA MASIVA DE MÉDICOS ESPECIALISTAS");
    }

    public static void cargarMedicosEspecialistas() {
        Connection conn = null;
        int exitos = 0;
        int fallidos = 0;

        try {
            conn = ConexionBD.conectar();
            conn.setAutoCommit(false); // Usar transacción

            // Crear lista de médicos
            List<MedicoEspecialista> medicos = crearListaMedicos();

            log.debug("Total de médicos a procesar: {}", medicos.size());

            for (MedicoEspecialista medico : medicos) {
                try {
                    System.out.println("Procesando: " + medico.nombreCompleto);
                    System.out.println("Username: " + medico.username);
                    System.out.println("Email: " + medico.email);
                    System.out.println("Cédula: " + medico.cedula);
                    System.out.println("Universidad: " + medico.universidad);

                    // 1. Verificar si el usuario ya existe
                    if (usuarioExiste(conn, medico.username)) {
                        log.warn("⚠️  Usuario ya existe, saltando...");
                        fallidos++;
                        continue;
                    }

                    // 2. Registrar en tb_usuarios
                    int idUsuario = registrarUsuario(conn, medico);
                    if (idUsuario == -1) {
                        log.error("❌ Error al registrar usuario");
                        fallidos++;
                        continue;
                    }

                    // 3. Registrar en tb_medesp
                    if (registrarMedicoEspecialista(conn, medico)) {
                        log.debug("✅ Registrado exitosamente");
                        exitos++;
                    } else {
                        log.error("❌ Error al registrar en tb_medesp");
                        fallidos++;
                        // Revertir usuario si falla
                        revertirUsuario(conn, idUsuario);
                    }

                } catch (Exception e) {
                    log.error("❌ Error: {}", e.getMessage());
                    log.error(e);
                    fallidos++;
                }
            }

            // Confirmar transacción
            conn.commit();

            log.info("✅ Registrados exitosamente: {}", exitos);
            log.info("❌ Fallidos: {}", fallidos);
            log.info("Total procesados: {}", exitos + fallidos);

        } catch (SQLException e) {
            log.error("Error de conexión: {}", e.getMessage());
            log.error(e);
            if (conn != null) {
                try {
                    conn.rollback();
                    log.debug("Transacción revertida");
                } catch (SQLException ex) {
                    log.error("Error al revertir la transacción: {}", ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                    log.debug("Conexión cerrada");
                } catch (SQLException e) {
                    log.error("Error al cerrar la conexión: {}", e.getMessage());
                }
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

    private static int registrarUsuario(Connection conn, MedicoEspecialista medico) throws SQLException {
        String sql = "INSERT INTO tb_usuarios (username, password_hash, email, nombre_completo, " +
                "rol, cedula_profesional, activo, primer_login) " +
                "VALUES (?, ?, ?, ?, 'MEDICO_ESPECIALISTA', ?, true, true)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Contraseña temporal: Temp123
            String hash = PasswordUtils.hashPassword("Temp123");

            pstmt.setString(1, medico.username);
            pstmt.setString(2, hash);
            pstmt.setString(3, medico.email);
            pstmt.setString(4, medico.nombreCompleto);
            pstmt.setString(5, medico.cedula);

            pstmt.executeUpdate();

            // Obtener ID generado
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private static boolean registrarMedicoEspecialista(Connection conn, MedicoEspecialista medico) throws SQLException {
        // Generar nueva Cve_med
        int nuevaCveMed = generarNuevaCveMed(conn);

        String sql = "INSERT INTO tb_medesp (Cve_med, Nombre, Cedula, universidad) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, nuevaCveMed);
            pstmt.setString(2, medico.nombreCompleto);
            pstmt.setString(3, medico.cedula);
            pstmt.setString(4, medico.universidad);

            return pstmt.executeUpdate() > 0;
        }
    }

    private static int generarNuevaCveMed(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(Cve_med), 0) + 1 as nueva_clave FROM tb_medesp";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("nueva_clave");
            }
        }
        return 1;
    }

    private static void revertirUsuario(Connection conn, int idUsuario) {
        try {
            String sql = "DELETE FROM tb_usuarios WHERE id_usuario = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idUsuario);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("Error al revertir usuario: {}", e.getMessage());
        }
    }

    // LISTA COMPLETA DE MÉDICOS
    private static List<MedicoEspecialista> crearListaMedicos() {
        List<MedicoEspecialista> medicos = new ArrayList<>();

        // Añadir todos los médicos (solo los primeros 20 como ejemplo, el resto va igual)
        medicos.add(new MedicoEspecialista("Adelaida Patricia Gutiérrez Acuña", "AECEM17539", "."));
        medicos.add(new MedicoEspecialista("Adrian Ibáñez Nava", "3443653", "."));
        medicos.add(new MedicoEspecialista("Adriana Mendoza Campos", "8109714", "."));
        medicos.add(new MedicoEspecialista("Agustín Ruiz Gallardo", "3246843", "."));
        medicos.add(new MedicoEspecialista("Alba Luz López Hernández", "AECEM30998", "."));
        medicos.add(new MedicoEspecialista("Alicia Ramírez Cruz", "3820200", "."));
        medicos.add(new MedicoEspecialista("América Del Pilar Segura Tovar", "3246273", "."));
        medicos.add(new MedicoEspecialista("Ángela Rodríguez Trujillo", "4860219", "."));
        medicos.add(new MedicoEspecialista("Ángelo Barradas González", "3247709", "."));
        medicos.add(new MedicoEspecialista("Asunción González Diego", "5419405", "."));
        medicos.add(new MedicoEspecialista("Aurora García Téllez", "5928496", "."));
        medicos.add(new MedicoEspecialista("Bartolomé Cervera Pacheco", "5455650", "."));
        medicos.add(new MedicoEspecialista("Carlos Francisco Cruz Cárdenas", "3171951", "."));
        medicos.add(new MedicoEspecialista("Celerina Nazario Mendo", "5419179", "."));
        medicos.add(new MedicoEspecialista("Clementina Soni Trinidad", "AESSA28721", "."));
        medicos.add(new MedicoEspecialista("Cristóbal Aguirre Bacerot", "3411297", "."));
        medicos.add(new MedicoEspecialista("Daniel Posadas Morales", "6430097", "."));
        medicos.add(new MedicoEspecialista("Débora Emilia Mitford Taylor Del Ángel", "3247552", "."));
        medicos.add(new MedicoEspecialista("Eduardo Castillo Simbrón", "7237952", "."));
        medicos.add(new MedicoEspecialista("Eduardo Rosas Melo", "3872495", "."));
        medicos.add(new MedicoEspecialista("Edwin Cobos Abraham", "7318835", "."));
        medicos.add(new MedicoEspecialista("Edxon Ronier Lamas Rodríguez", "AEIE11417", "."));
        medicos.add(new MedicoEspecialista("Elda Yara Lagos Córdova", "6249335", "."));
        medicos.add(new MedicoEspecialista("Esperanza Hernández Cuellar", "AE10716", "."));
        medicos.add(new MedicoEspecialista("Esteban Valdescastillo Gutiérrez", "19063", "."));
        medicos.add(new MedicoEspecialista("Fabiola Sarai Muñoz Castro", "7270549", "."));
        medicos.add(new MedicoEspecialista("Francisco Espino Hernández", "286481", "."));
        medicos.add(new MedicoEspecialista("Francisco Javier González Vidal", "6249320", "."));
        medicos.add(new MedicoEspecialista("Gabriel Esteban Serratos Monrroy", "AECEM31166", "."));
        medicos.add(new MedicoEspecialista("Gabriel Gómez Genchi", "4896726", "."));
        medicos.add(new MedicoEspecialista("Guadalupe De La Hoz Sugasti", "3247489", "."));
        medicos.add(new MedicoEspecialista("Guadalupe Ramírez Calderón", "AE011858", "."));
        medicos.add(new MedicoEspecialista("Guillermo Dager Tapia", "5472352", "."));
        medicos.add(new MedicoEspecialista("Héctor Eduardo Lira De La Vega", "4111394", "."));
        medicos.add(new MedicoEspecialista("Hilda Basilio Badillo", "AE013045", "."));
        medicos.add(new MedicoEspecialista("Javier Hernández Álvarez", "5461178", "."));
        medicos.add(new MedicoEspecialista("Javier Herrera Enríquez", "4623309", "."));
        medicos.add(new MedicoEspecialista("Jesús Ulises Lomeli Rivera", "3873546", "."));
        medicos.add(new MedicoEspecialista("Joel Olmedo Méndez", "5777325", "."));
        medicos.add(new MedicoEspecialista("José Antolín Montero Alpirez", "3246836", "."));
        medicos.add(new MedicoEspecialista("José Luis Díaz Muñoz", "3445385", "."));
        medicos.add(new MedicoEspecialista("José Luis Gonzalo Cruz Serrano", "5643153", "."));
        medicos.add(new MedicoEspecialista("Juan Dávila Ledezma", "5132333", "."));
        medicos.add(new MedicoEspecialista("Juan José González Menacho", "EA07296", "."));
        medicos.add(new MedicoEspecialista("Juan Manuel Alonso Rivera", "3815476", "."));
        medicos.add(new MedicoEspecialista("Leopoldo Maldonado Azuara", "3730442", "."));
        medicos.add(new MedicoEspecialista("Leticia González Domínguez", "4512147", "."));
        medicos.add(new MedicoEspecialista("Lucia Ramírez Arellano", "8129777", "."));
        medicos.add(new MedicoEspecialista("Luz María Vargas Del Ángel", "30745", "."));
        medicos.add(new MedicoEspecialista("Manuel Iván Rodríguez Aguirre", "4110418", "."));
        medicos.add(new MedicoEspecialista("María Alejandra Barragán Hernández", "3168177", "."));
        medicos.add(new MedicoEspecialista("María Ballesteros Ramos", "3872496", "."));
        medicos.add(new MedicoEspecialista("María José Servin González", "810106", "."));
        medicos.add(new MedicoEspecialista("María Luisa Del Ángel Riveroll", "4776327", "."));
        medicos.add(new MedicoEspecialista("Mario Blásquez Díaz", "3277439", "."));
        medicos.add(new MedicoEspecialista("Martin Cadena Domínguez", "ASEA31719", "."));
        medicos.add(new MedicoEspecialista("Maulio Fabio Rivera López", "AESSA30761", "."));
        medicos.add(new MedicoEspecialista("Miguel Ángel Malfavon Perdomo", "3245984", "."));
        medicos.add(new MedicoEspecialista("Miguel Ángel Palomo Colli", "5212002", "."));
        medicos.add(new MedicoEspecialista("Oscar Alejandro Castillo Nava", "AE013458", "."));
        medicos.add(new MedicoEspecialista("Oscar René Blanco Alarcón", "3352896", "."));
        medicos.add(new MedicoEspecialista("Oscar Salas García", "AECEM19746", "."));
        medicos.add(new MedicoEspecialista("Pablo González García", "4412359", "."));
        medicos.add(new MedicoEspecialista("Pedro Francisco Valles Nahuat", "5212125", "."));
        medicos.add(new MedicoEspecialista("Rey Gaspar Sequera Hernández", "3653575", "."));
        medicos.add(new MedicoEspecialista("Roberto Peralta Juárez", "5419291", "."));
        medicos.add(new MedicoEspecialista("Rogelio Aguilar Leyva", "3341183", "."));
        medicos.add(new MedicoEspecialista("Rogelio Corona Sánchez", "3270482", "."));
        medicos.add(new MedicoEspecialista("Rossana Ramos Vargas", "4743060", "."));
        medicos.add(new MedicoEspecialista("Rubén Vázquez Núñez", "AECEM19749", "."));
        medicos.add(new MedicoEspecialista("Sabas Hernández Cruz", "5455653", "."));
        medicos.add(new MedicoEspecialista("Salatiel Cruz Vidal", "3247711", "."));
        medicos.add(new MedicoEspecialista("Sandra Luz Rubio Chavarin", "3181181", "."));
        medicos.add(new MedicoEspecialista("Santiago Ramírez Ramírez", "AECEM31158", "."));
        medicos.add(new MedicoEspecialista("Tomas Roberto Valadez Ramírez", "3277839", "."));
        medicos.add(new MedicoEspecialista("Vicente Sánchez García", "786478", "."));
        medicos.add(new MedicoEspecialista("Víctor Hugo Espinoza Román", "5928158", "."));
        medicos.add(new MedicoEspecialista("Víctor Manuel Juárez Montemayor", "AECEM29566", "."));
        medicos.add(new MedicoEspecialista("Víctor Manuel Velázquez Iglesias", "4512164", "."));
        medicos.add(new MedicoEspecialista("Virna Giuliana Acevedo Segura", "7962029", "."));
        medicos.add(new MedicoEspecialista("Yareny Martínez Salazar", "5941591", "."));
        medicos.add(new MedicoEspecialista("Yolanda Julissa Gutiérrez Cortes", "2342703", "."));
        medicos.add(new MedicoEspecialista("Gustavo Lacayo Méndez", "893193", "."));
        medicos.add(new MedicoEspecialista("Edna Berenice Sánchez Terrazas", "6677072", "."));
        medicos.add(new MedicoEspecialista("Mariana Vera Rodriguez", "6474899", "."));
        medicos.add(new MedicoEspecialista("Juan Carlos Rodríguez González", "7361825", "."));
        medicos.add(new MedicoEspecialista("Eva Arjona Gone", "4010415", "."));
        medicos.add(new MedicoEspecialista("Gamaliel Fernández Patiño", "11521969", "."));
        medicos.add(new MedicoEspecialista("Jasiel Neftalí Melchor Hidalgo", "6368467", "."));
        medicos.add(new MedicoEspecialista("Edna Berenice Sánchez Terrazas", "5097676", "."));
        medicos.add(new MedicoEspecialista("Jesús Roberto Ramírez Gómez", "11560982", "."));
        medicos.add(new MedicoEspecialista("Lucía Del Pilar Sánchez Garrido", "12451616", "."));
        medicos.add(new MedicoEspecialista("Jorge Rodríguez Martínez", "6384693", "."));
        medicos.add(new MedicoEspecialista("Lucía de Pilar Sánchez Garrido", "12451616", "."));
        medicos.add(new MedicoEspecialista("Luis Antonio Rergis Landa", "12040000", "."));
        medicos.add(new MedicoEspecialista("Eva Sánchez San Román", "12090783", "."));
        medicos.add(new MedicoEspecialista("Ángel Iván García Posadas", "12072097", "."));
        medicos.add(new MedicoEspecialista("Jaimit Cabrera Dávila", "7853476", "Universidad Autónoma de Sinaloa"));
        medicos.add(new MedicoEspecialista("Emmanuelle Rhoman Montiel Cerón", "5276910", "Universidad Veracruzana"));
        medicos.add(new MedicoEspecialista("Carlos David Vera Morales", "8697665", "Universidad Veracruzana"));
        medicos.add(new MedicoEspecialista("Lizeth Anahí Sosa Cuellar", "12197715", "Universidad Veracruzana"));
        medicos.add(new MedicoEspecialista("Héctor Eduardo Lira de la Vega", "1817006", "Universidad Veracruzana"));
        medicos.add(new MedicoEspecialista("Virna Giuliana Acevedo Segura", "5437762", "Universidad Autónoma de Nuevo León"));
        medicos.add(new MedicoEspecialista("Sergio Andrés Torres Estrada", "5927278", "Universidad del Noreste"));
        medicos.add(new MedicoEspecialista("Diana Nailea Cortes Gómez", "12474625", "Universidad del Noreste"));
        medicos.add(new MedicoEspecialista("Abel Alejandro Pastrana López Escalera", "5907122", "Universidad Nacional Autónoma de México"));
        medicos.add(new MedicoEspecialista("David Medina Jiménez", "10383957", "Universidad Nacional Autónoma de México"));
        medicos.add(new MedicoEspecialista("Mishel Iván Verdugo Hernández", "13425377", "Universidad Veracruzana"));
        medicos.add(new MedicoEspecialista("Italo Gibrán Reyes Rosas", "12405517", "UNAM/UV/UAT"));
        medicos.add(new MedicoEspecialista("Francisco Mora Cruz", "10048100", "Universidad de Monterrey"));
        medicos.add(new MedicoEspecialista("Eduardo de Jesús Enríquez Vela", "11992071", "Universidad Veracruzana"));
        medicos.add(new MedicoEspecialista("Arturo Eliseo Sigero Vazquez", "11466836", "Universidad Veracruzana"));
        medicos.add(new MedicoEspecialista("Jesús Sinue Márquez López", "6123451", "Universidad del Noreste"));
        medicos.add(new MedicoEspecialista("Héctor Alejandro Jiménez Guzmán", "12492506", "Universidad Veracruzana"));

        return medicos;
    }
}