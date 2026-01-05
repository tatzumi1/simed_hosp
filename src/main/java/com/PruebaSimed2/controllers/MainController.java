// src/main/java/com/PruebaSimed2/controllers/MainController.java

package com.PruebaSimed2.controllers;
import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.Usuario;
import com.PruebaSimed2.utils.DatosPacienteUtils;
import com.itextpdf.io.exceptions.IOException;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import java.sql.*;
import java.util.Comparator;

public class MainController {

    @FXML private Button btnEstadistica;
    @FXML private Button btnPendientes;
    @FXML private Button btnJefatura;
    @FXML private TextField txtFolio;
    @FXML private Button btnCapturar, btnActualizar, btnBuscar;
    @FXML private Button btnTriage;
    @FXML private TabPane tabPanePacientes;

    // Tablas
    @FXML private TableView<Paciente> tablaEspera;
    @FXML private TableView<Paciente> tablaObservacion;
    @FXML private TableView<Paciente> tablaEgresados;

    // Columnas para tabla Espera
    @FXML private TableColumn<Paciente, Integer> colFolioEspera;
    @FXML private TableColumn<Paciente, String> colNombreEspera;
    @FXML private TableColumn<Paciente, Integer> colEdadEspera;
    @FXML private TableColumn<Paciente, String> colSexoEspera;
    @FXML private TableColumn<Paciente, String> colTriageEspera;
    @FXML private TableColumn<Paciente, String> colHoraEspera;

    // Columnas para tabla Observación
    @FXML private TableColumn<Paciente, Integer> colFolioObservacion;
    @FXML private TableColumn<Paciente, String> colNombreObservacion;
    @FXML private TableColumn<Paciente, Integer> colEdadObservacion;
    @FXML private TableColumn<Paciente, String> colSintomasObservacion;
    @FXML private TableColumn<Paciente, String> colMedicoObservacion;
    @FXML private TableColumn<Paciente, String> colHoraAtencionObservacion;

    // Columnas para tabla Egresados
    @FXML private TableColumn<Paciente, Integer> colFolioEgresados;
    @FXML private TableColumn<Paciente, String> colNombreEgresados;
    @FXML private TableColumn<Paciente, String> colFechaAltaEgresados;
    @FXML private TableColumn<Paciente, String> colTipoAltaEgresados;
    @FXML private TableColumn<Paciente, String> colMedicoEgresados;

    // === NUEVOS CAMPOS PARA BÚSQUEDA Y FILTROS ===
    @FXML private TextField txtBuscarGlobal;
    @FXML private ComboBox<String> cbTipoBusqueda;
    @FXML private Label lblResultadosBusqueda;
    @FXML private Label lblContadorEspera;
    @FXML private Label lblContadorObservacion;
    @FXML private Label lblContadorEgresados;
    @FXML private Label lblTotalPacientes;

    // Nuevas columnas para las tablas
    @FXML private TableColumn<Paciente, String> colCurpEspera;
    @FXML private TableColumn<Paciente, String> colPaternoEspera;
    @FXML private TableColumn<Paciente, String> colMaternoEspera;
    @FXML private TableColumn<Paciente, String> colSintomasEspera;
    @FXML private TableColumn<Paciente, String> colPersonalIngresaEspera;
    @FXML private TableColumn<Paciente, String> colMedicoAsignadoEspera;

    @FXML private TableColumn<Paciente, String> colCurpObservacion;
    @FXML private TableColumn<Paciente, String> colPaternoObservacion;
    @FXML private TableColumn<Paciente, String> colMaternoObservacion;
    @FXML private TableColumn<Paciente, String> colPersonalIngresaObservacion;
    @FXML private TableColumn<Paciente, String> colMedicoAsignadoObservacion;
    @FXML private TableColumn<Paciente, String> colTriageObservacion;

    @FXML private TableColumn<Paciente, String> colCurpEgresados;
    @FXML private TableColumn<Paciente, String> colPaternoEgresados;
    @FXML private TableColumn<Paciente, String> colMaternoEgresados;
    @FXML private TableColumn<Paciente, String> colSintomasEgresados;
    @FXML private TableColumn<Paciente, String> colPersonalIngresaEgresados;
    @FXML private TableColumn<Paciente, String> colMedicoAsignadoEgresados;

    private ObservableList<Paciente> pacientesEsperaCompleta = FXCollections.observableArrayList();
    private ObservableList<Paciente> pacientesObservacionCompleta = FXCollections.observableArrayList();
    private ObservableList<Paciente> pacientesEgresadosCompleta = FXCollections.observableArrayList();

    private boolean ordenAscendente = true;
    private Usuario usuarioLogueado;

    public static class Paciente {
        private final SimpleIntegerProperty folio;
        private final SimpleStringProperty curp;
        private final SimpleStringProperty paterno;
        private final SimpleStringProperty materno;
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty sintomas;
        private final SimpleStringProperty personalIngresa;
        private final SimpleStringProperty medicoAsignado;
        private final SimpleStringProperty triage;
        private final SimpleStringProperty horaRegistro;
        private final SimpleStringProperty horaAtencion;
        private final SimpleStringProperty fechaAlta;
        private final SimpleStringProperty tipoAlta;
        private final SimpleStringProperty medicoAlta;
        private final SimpleIntegerProperty edad;
        private final SimpleStringProperty sexo;

        //  NUEVAS PROPIEDADES PARA PENDIENTES
        private final SimpleStringProperty domicilio;
        private final SimpleStringProperty telefono;
        private final SimpleStringProperty derechohabiencia;

        // Constructor para ESPERA (11 parámetros)
        public Paciente(int folio, String curp, String paterno, String materno, String nombre,
                        String sintomas, String personalIngresa, String medicoAsignado,
                        String triage, String horaRegistro) {
            this.folio = new SimpleIntegerProperty(folio);
            this.curp = new SimpleStringProperty(curp != null ? curp : "");
            this.paterno = new SimpleStringProperty(paterno != null ? paterno : "");
            this.materno = new SimpleStringProperty(materno != null ? materno : "");
            this.nombre = new SimpleStringProperty(nombre != null ? nombre : "");
            this.sintomas = new SimpleStringProperty(sintomas != null ? sintomas : "");
            this.personalIngresa = new SimpleStringProperty(personalIngresa != null ? personalIngresa : "");
            this.medicoAsignado = new SimpleStringProperty(medicoAsignado != null ? medicoAsignado : "");
            this.triage = new SimpleStringProperty(triage != null ? triage : "");
            this.horaRegistro = new SimpleStringProperty(horaRegistro != null ? horaRegistro : "");
            this.horaAtencion = new SimpleStringProperty("");
            this.fechaAlta = new SimpleStringProperty("");
            this.tipoAlta = new SimpleStringProperty("");
            this.medicoAlta = new SimpleStringProperty("");
            this.edad = new SimpleIntegerProperty(0);
            this.sexo = new SimpleStringProperty("");

            //  INICIALIZAR NUEVAS PROPIEDADES
            this.domicilio = new SimpleStringProperty("");
            this.telefono = new SimpleStringProperty("");
            this.derechohabiencia = new SimpleStringProperty("");
        }

        // Constructor para OBSERVACIÓN (11 parámetros + "observacion")
        public Paciente(int folio, String curp, String paterno, String materno, String nombre,
                        String sintomas, String personalIngresa, String medicoAsignado,
                        String triage, String horaAtencion, String tipo) {
            this.folio = new SimpleIntegerProperty(folio);
            this.curp = new SimpleStringProperty(curp != null ? curp : "");
            this.paterno = new SimpleStringProperty(paterno != null ? paterno : "");
            this.materno = new SimpleStringProperty(materno != null ? materno : "");
            this.nombre = new SimpleStringProperty(nombre != null ? nombre : "");
            this.sintomas = new SimpleStringProperty(sintomas != null ? sintomas : "");
            this.personalIngresa = new SimpleStringProperty(personalIngresa != null ? personalIngresa : "");
            this.medicoAsignado = new SimpleStringProperty(medicoAsignado != null ? medicoAsignado : "");
            this.triage = new SimpleStringProperty(triage != null ? triage : "");
            this.horaAtencion = new SimpleStringProperty(horaAtencion != null ? horaAtencion : "");

            // Campos no usados
            this.horaRegistro = new SimpleStringProperty("");
            this.fechaAlta = new SimpleStringProperty("");
            this.tipoAlta = new SimpleStringProperty("");
            this.medicoAlta = new SimpleStringProperty("");
            this.edad = new SimpleIntegerProperty(0);
            this.sexo = new SimpleStringProperty("");

            //  INICIALIZAR NUEVAS PROPIEDADES
            this.domicilio = new SimpleStringProperty("");
            this.telefono = new SimpleStringProperty("");
            this.derechohabiencia = new SimpleStringProperty("");
        }

        // Constructor para EGRESADOS (12 parámetros)
        public Paciente(int folio, String tipoAlta, String fechaAlta, String medicoAlta,
                        String curp, String paterno, String materno, String nombre,
                        String sintomas, String personalIngresa, String medicoAsignado, int tipoPaciente) {
            this.folio = new SimpleIntegerProperty(folio);
            this.tipoAlta = new SimpleStringProperty(tipoAlta != null ? tipoAlta : "");
            this.fechaAlta = new SimpleStringProperty(fechaAlta != null ? fechaAlta : "");
            this.medicoAlta = new SimpleStringProperty(medicoAlta != null ? medicoAlta : "");
            this.curp = new SimpleStringProperty(curp != null ? curp : "");
            this.paterno = new SimpleStringProperty(paterno != null ? paterno : "");
            this.materno = new SimpleStringProperty(materno != null ? materno : "");
            this.nombre = new SimpleStringProperty(nombre != null ? nombre : "");
            this.sintomas = new SimpleStringProperty(sintomas != null ? sintomas : "");
            this.personalIngresa = new SimpleStringProperty(personalIngresa != null ? personalIngresa : "");
            this.medicoAsignado = new SimpleStringProperty(medicoAsignado != null ? medicoAsignado : "");

            // Campos no usados
            this.triage = new SimpleStringProperty("");
            this.horaRegistro = new SimpleStringProperty("");
            this.horaAtencion = new SimpleStringProperty("");
            this.edad = new SimpleIntegerProperty(0);
            this.sexo = new SimpleStringProperty("");

            this.domicilio = new SimpleStringProperty("");
            this.telefono = new SimpleStringProperty("");
            this.derechohabiencia = new SimpleStringProperty("");
        }

        public int getFolio() { return folio.get(); }
        public String getCurp() { return curp.get(); }
        public String getPaterno() { return paterno.get(); }
        public String getMaterno() { return materno.get(); }
        public String getNombre() { return nombre.get(); }
        public String getSintomas() { return sintomas.get(); }
        public String getPersonalIngresa() { return personalIngresa.get(); }
        public String getMedicoAsignado() { return medicoAsignado.get(); }
        public String getTriage() { return triage.get(); }
        public String getHoraRegistro() { return horaRegistro.get(); }
        public String getHoraAtencion() { return horaAtencion.get(); }
        public String getFechaAlta() { return fechaAlta.get(); }
        public String getTipoAlta() { return tipoAlta.get(); }
        public String getMedicoAlta() { return medicoAlta.get(); }
        public int getEdad() { return edad.get(); }
        public String getSexo() { return sexo.get(); }

        public String getDomicilio() { return domicilio.get(); }
        public String getTelefono() { return telefono.get(); }
        public String getDerechohabiencia() { return derechohabiencia.get(); }

        public void setDomicilio(String domicilio) { this.domicilio.set(domicilio); }
        public void setTelefono(String telefono) { this.telefono.set(telefono); }
        public void setDerechohabiencia(String derechohabiencia) { this.derechohabiencia.set(derechohabiencia); }
        public void setEdad(int edad) { this.edad.set(edad); }
        public void setSexo(String sexo) { this.sexo.set(sexo); }
    }

    public void setUsuarioLogueado(Usuario usuario) {
        this.usuarioLogueado = usuario;
        System.out.println(" Usuario en MainController: " + usuario.getUsername() + " - Rol: " + usuario.getRol());
        actualizarInterfazSegunRol();
    }

    private void actualizarInterfazSegunRol() {
        if (usuarioLogueado == null) return;

        String rol = usuarioLogueado.getRol();
        System.out.println(" Configurando interfaz para rol: " + rol);

        // 1. BOTÓN TRIAGE
        boolean mostrarTriage = "ADMIN".equals(rol) ||
                "TRIAGE".equals(rol) ||
                "JEFATURA_URGENCIAS".equals(rol) ||
                "ADMINISTRATIVO".equals(rol);

        if (btnTriage != null) {
            btnTriage.setVisible(mostrarTriage);
            btnTriage.setManaged(mostrarTriage);
            System.out.println("Botón TRIAGE: " + (mostrarTriage ? "VISIBLE" : "OCULTO"));
        }

        // 2. BOTÓN ESTADÍSTICA
        boolean mostrarEstadistica = "ADMIN".equals(rol) ||
                "JEFATURA_URGENCIAS".equals(rol) ||
                "ADMINISTRATIVO".equals(rol);

        if (btnEstadistica != null) {
            btnEstadistica.setVisible(mostrarEstadistica);
            btnEstadistica.setManaged(mostrarEstadistica);
            System.out.println("Botón ESTADÍSTICA: " + (mostrarEstadistica ? "VISIBLE" : "OCULTO"));
        }

        // 3. BOTÓN PENDIENTES
        boolean mostrarPendientes = "ADMIN".equals(rol) || "TRIAGE".equals(rol);

        if (btnPendientes != null) {
            btnPendientes.setVisible(mostrarPendientes);
            btnPendientes.setManaged(mostrarPendientes);
            System.out.println("Botón PENDIENTES: " + (mostrarPendientes ? "VISIBLE" : "OCULTO"));
        }

        // 4. BOTÓN JEFATURA
        boolean mostrarJefatura = "ADMIN".equals(rol) ||
                "JEFATURA_URGENCIAS".equals(rol) ||
                "ADMINISTRATIVO".equals(rol);

        if (btnJefatura != null) {
            btnJefatura.setVisible(mostrarJefatura);
            btnJefatura.setManaged(mostrarJefatura);
            System.out.println("Botón JEFATURA: " + (mostrarJefatura ? "VISIBLE" : "OCULTO"));
        }

        // 5. BOTÓN CAPTURAR
        boolean mostrarCapturar = "ADMIN".equals(rol) ||
                "JEFATURA_URGENCIAS".equals(rol) ||
                "MEDICO_URGENCIAS".equals(rol) ||
                "MEDICO_ESPECIALISTA".equals(rol) ||
                "ADMINISTRATIVO".equals(rol);

        if (btnCapturar != null) {
            btnCapturar.setVisible(mostrarCapturar);
            btnCapturar.setManaged(mostrarCapturar);
            System.out.println(" Botón CAPTURAR: " + (mostrarCapturar ? "VISIBLE" : "OCULTO"));
        }

        // 6. GESTIÓN DE USUARIOS (SOLO admin)
        boolean mostrarGestionUsuarios = "ADMIN".equals(rol);

        // 7. Los botones BUSCAR y ACTUALIZAR son visibles para TODOS
        if (btnBuscar != null) {
            btnBuscar.setVisible(true);
            btnBuscar.setManaged(true);
        }

        if (btnActualizar != null) {
            btnActualizar.setVisible(true);
            btnActualizar.setManaged(true);
        }

        // 8. Abrir gestión de usuarios automáticamente solo si es admin
        if (mostrarGestionUsuarios) {
            System.out.println(" Admin detectado - Abriendo gestión de usuarios automáticamente");
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> {
                        abrirGestionUsuarios();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            System.out.println(" Usuario sin permisos de gestión - Rol: " + rol);
        }
    }

    @FXML
    public void initialize() {
        System.out.println(" Ventana principal cargada con éxito.");
        configurarColumnas();
        inicializarBusqueda();
        cargarPacientesEnEspera();
        cargarPacientesEnObservacion();
        cargarPacientesEgresados();
        actualizarContadores();
    }

    private void abrirGestionUsuarios() {
        System.out.println(" INTENTANDO abrir gestión de usuarios...");
        System.out.println(" Usuario logueado: " + (usuarioLogueado != null ? usuarioLogueado.getUsername() : "NULL"));
        System.out.println(" Rol del usuario: " + (usuarioLogueado != null ? usuarioLogueado.getRol() : "NULL"));

        try {
            System.out.println("1. Cargando FXML...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/gestionUsuarios.fxml"));
            Parent root = loader.load();
            System.out.println("2.  FXML cargado exitosamente");

            System.out.println("3. Configurando controlador...");
            GestionUsuariosController controller = loader.getController();

            if (usuarioLogueado != null) {
                System.out.println("4. Pasando usuario al controlador: " + usuarioLogueado.getUsername());
                controller.setUsuarioAdmin(usuarioLogueado);
            } else {
                System.out.println(" ERROR: usuarioLogueado es NULL");
                return;
            }

            System.out.println("5. Creando ventana...");
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 900, 700));
            stage.setTitle("Gestión de Usuarios - Sistema SIMED");
            stage.show();

            System.out.println(" VENTANA DE GESTIÓN ABIERTA EXITOSAMENTE");

        } catch (Exception e) {
            System.err.println(" ERROR CRÍTICO abriendo gestión de usuarios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarColumnas() {
        // ===== COLUMNAS PARA TABLA ESPERA =====
        colFolioEspera.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getFolio()).asObject());
        colCurpEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCurp()));
        colPaternoEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaterno()));
        colMaternoEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMaterno()));
        colNombreEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNombre()));
        colSintomasEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSintomas()));
        colPersonalIngresaEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPersonalIngresa()));
        colMedicoAsignadoEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMedicoAsignado()));
        colTriageEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTriage()));
        colHoraEspera.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getHoraRegistro()));

        // ===== COLUMNAS PARA TABLA OBSERVACIÓN =====
        colFolioObservacion.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getFolio()).asObject());
        colCurpObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCurp()));
        colPaternoObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaterno()));
        colMaternoObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMaterno()));
        colNombreObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNombre()));
        colSintomasObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSintomas()));
        colPersonalIngresaObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPersonalIngresa()));
        colMedicoAsignadoObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMedicoAsignado()));
        colTriageObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTriage()));
        colHoraAtencionObservacion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getHoraAtencion()));

        // ===== COLUMNAS PARA TABLA EGRESADOS =====
        colFolioEgresados.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getFolio()).asObject());
        colCurpEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCurp()));
        colPaternoEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaterno()));
        colMaternoEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMaterno()));
        colNombreEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNombre()));
        colSintomasEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSintomas()));
        colPersonalIngresaEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPersonalIngresa()));
        colMedicoAsignadoEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMedicoAsignado()));
        colTipoAltaEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTipoAlta()));
        colFechaAltaEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFechaAlta()));
        colMedicoEgresados.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMedicoAlta()));
    }

    @FXML
    private void onKeyReleased(KeyEvent event) {
        if (txtFolio.getText().trim().isEmpty()) {
            txtFolio.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        } else {
            txtFolio.setStyle("-fx-border-color: #cccccc;");
        }
    }

    @FXML
    private void handleCapturar() {
        String folioTexto = txtFolio.getText().trim();
        if (folioTexto.isEmpty()) {
            mostrarAlerta("Error", "Por favor ingrese un folio", Alert.AlertType.WARNING);
            return;
        }

        try {
            int folio = Integer.parseInt(folioTexto);
            System.out.println(" Buscando paciente con folio: " + folio);

            if (verificarFolioExiste(folio)) {
                abrirVentanaCaptura(folio);
            } else {
                mostrarAlerta("Folio No Encontrado",
                        "No existe paciente con folio: " + folio + "\n\n" +
                                "Verifique que:\n" +
                                "• El folio sea correcto\n" +
                                "• El paciente esté registrado en el módulo TRIAGE\n" +
                                "• El paciente esté en estado 'Espera' o 'Observación'",
                        Alert.AlertType.WARNING);
            }

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El folio debe ser un número válido", Alert.AlertType.ERROR);
        }
    }

    private boolean verificarFolioExiste(int folio) {
        String sql = "SELECT Folio, Estado_pac FROM tb_urgencias WHERE Folio = ?";
        Connection conn = null;

        try {
            conn = ConexionBD.getSafeConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, folio);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int estado = rs.getInt("Estado_pac");
                    System.out.println(" Folio encontrado - Estado: " + estado);
                    return true;
                } else {
                    System.out.println(" Folio NO encontrado: " + folio);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println(" Error verificando folio: " + e.getMessage());
            mostrarAlerta("Error de Conexión",
                    "No se pudo verificar el folio. Revise la conexión a la base de datos.",
                    Alert.AlertType.ERROR);
            return false;
        } finally {
            ConexionBD.safeClose(conn);
        }
    }

    private void abrirVentanaCaptura(int folio) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/captura_principal.fxml"));
            Parent root = loader.load();

            CapturaPrincipalController controller = loader.getController();
            controller.setFolioPaciente(folio);

            if (usuarioLogueado != null) {
                controller.setUsuarioLogueado(usuarioLogueado.getUsername(), usuarioLogueado.getRol());
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Registro Médico - Folio: " + folio);
            stage.show();

            txtFolio.clear();
            System.out.println(" Ventana de captura abierta para folio: " + folio);

        } catch (Exception e) {
            System.err.println(" Error abriendo ventana de captura: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error",
                    "No se pudo abrir el módulo de captura:\n" + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleActualizar() {
        System.out.println(" Actualizando listas de pacientes...");
        cargarPacientesEnEspera();
        cargarPacientesEnObservacion();
        cargarPacientesEgresados();
        mostrarAlerta("Actualizado", "Listas de pacientes actualizadas", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleBuscar() {
        String folioTexto = txtFolio.getText().trim();
        if (folioTexto.isEmpty()) {
            mostrarAlerta("Error", "Ingrese un folio para buscar", Alert.AlertType.WARNING);
            return;
        }

        try {
            int folio = Integer.parseInt(folioTexto);
            buscarPacientePorFolio(folio);
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Folio debe ser numérico", Alert.AlertType.ERROR);
        }
    }

    private void cargarPacientesEnEspera() {
        // ACTUALIZADA para mostrar FECHA + HORA
        String sql = "SELECT " +
                "u.Folio, u.CURP, u.A_paterno, u.A_materno, u.Nombre, " +
                "u.Sintomas, COALESCE(u.Nombre_ts, 'No asignado') as Personal_Ingresa, " +
                "COALESCE(u.Nom_med, 'No asignado') as Medico_Asignado, " +
                "u.TRIAGE, u.Fecha, u.Hora_registro, u.Edad, u.Sexo, " + // ← AGREGADO Fecha
                "u.Domicilio, u.Telefono, dh.Derechohabiencia " +
                "FROM tb_urgencias u " +
                "LEFT JOIN tblt_cvedhabiencia dh ON u.Derechohabiencia = dh.Cve_dh " +
                "WHERE u.Estado_pac = 1 OR u.Estado_pac IS NULL " +
                "ORDER BY u.Fecha DESC, u.Hora_registro DESC";

        Connection conn = null;
        try {
            conn = ConexionBD.getSafeConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                pacientesEsperaCompleta.clear();

                while (rs.next()) {
                    // COMBINAR FECHA Y HORA
                    String fecha = rs.getString("Fecha");
                    String hora = rs.getString("Hora_registro");
                    String fechaHoraCompleta = fecha.split(" ")[0] + " " + hora; // Solo fecha + hora

                    Paciente paciente = new Paciente(
                            rs.getInt("Folio"),
                            rs.getString("CURP"),
                            rs.getString("A_paterno"),
                            rs.getString("A_materno"),
                            rs.getString("Nombre"),
                            rs.getString("Sintomas"),
                            rs.getString("Personal_Ingresa"),
                            rs.getString("Medico_Asignado"),
                            rs.getString("TRIAGE"),
                            fechaHoraCompleta // ← FECHA + HORA COMBINADAS
                    );

                    // CARGAR LOS NUEVOS DATOS
                    paciente.setEdad(rs.getInt("Edad"));
                    paciente.setSexo(rs.getString("Sexo"));
                    paciente.setDomicilio(rs.getString("Domicilio"));
                    paciente.setTelefono(rs.getString("Telefono"));
                    paciente.setDerechohabiencia(rs.getString("Derechohabiencia"));

                    pacientesEsperaCompleta.add(paciente);
                }

                tablaEspera.setItems(pacientesEsperaCompleta);
                System.out.println(" Pacientes en espera cargados: " + pacientesEsperaCompleta.size());

            }
        } catch (SQLException e) {
            System.err.println(" Error cargando pacientes en espera: " + e.getMessage());
            mostrarAlerta("Error del Sistema",
                    "No se pudieron cargar los pacientes. Intente nuevamente.",
                    Alert.AlertType.ERROR);
        } finally {
            ConexionBD.safeClose(conn);
        }
    }

    private void cargarPacientesEnObservacion() {
        // ACTUALIZADA para mostrar FECHA_ATENCION + HORA_ATENCION
        String sql = "SELECT " +
                "u.Folio, u.CURP, u.A_paterno, u.A_materno, u.Nombre, " +
                "u.Sintomas, COALESCE(u.Nombre_ts, 'No asignado') as Personal_Ingresa, " +
                "COALESCE(m.Med_nombre, u.Nom_med) as Medico_Asignado, " +
                "u.TRIAGE, u.Fecha_atencion, u.Hora_atencion, u.Edad, u.Sexo " + // ← AGREGADO Fecha_atencion
                "FROM tb_urgencias u " +
                "LEFT JOIN tb_medicos m ON u.Cve_med = m.Cve_med " +
                "WHERE u.Estado_pac = 2 " +
                "ORDER BY u.Fecha_atencion DESC";

        Connection conn = null;
        try {
            conn = ConexionBD.getSafeConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                pacientesObservacionCompleta.clear();

                while (rs.next()) {
                    // COMBINAR FECHA_ATENCION Y HORA_ATENCION
                    String fechaAtencion = rs.getString("Fecha_atencion");
                    String horaAtencion = rs.getString("Hora_atencion");
                    String fechaHoraAtencion = (fechaAtencion != null ? fechaAtencion.split(" ")[0] : "Sin fecha") + " " +
                            (horaAtencion != null ? horaAtencion : "00:00:00");

                    Paciente paciente = new Paciente(
                            rs.getInt("Folio"),
                            rs.getString("CURP"),
                            rs.getString("A_paterno"),
                            rs.getString("A_materno"),
                            rs.getString("Nombre"),
                            rs.getString("Sintomas"),
                            rs.getString("Personal_Ingresa"),
                            rs.getString("Medico_Asignado"),
                            rs.getString("TRIAGE"),
                            fechaHoraAtencion, // ← FECHA + HORA ATENCIÓN
                            "observacion"
                    );

                    paciente.setEdad(rs.getInt("Edad"));
                    paciente.setSexo(rs.getString("Sexo"));

                    pacientesObservacionCompleta.add(paciente);
                }

                tablaObservacion.setItems(pacientesObservacionCompleta);
                System.out.println(" Pacientes en observación cargados: " + pacientesObservacionCompleta.size());

            }
        } catch (SQLException e) {
            System.err.println(" Error cargando pacientes en observación: " + e.getMessage());
        } finally {
            ConexionBD.safeClose(conn);
        }
    }

    private void cargarPacientesEgresados() {
        String sql = "SELECT " +
                "u.Folio, u.CURP, u.A_paterno, u.A_materno, u.Nombre, " +
                "u.Sintomas, COALESCE(u.Nombre_ts, 'No asignado') as Personal_Ingresa, " +  // ← CORREGIDO: u.Nombre_ts
                "COALESCE(m.Med_nombre, u.Nom_med) as Medico_Asignado, " +
                "COALESCE(a.Descripcion, 'Sin especificar') as TipoAlta, " +
                "u.Fecha_alta, COALESCE(u.Nom_medalta, 'No especificado') as MedicoAlta, " +
                "u.Edad, u.Sexo " +
                "FROM tb_urgencias u " +
                "LEFT JOIN tb_medicos m ON u.Cve_med = m.Cve_med " +  // ← QUITAR JOIN con tb_tsociales
                "LEFT JOIN tblt_cvealta a ON u.Alta_por = a.Cve_alta " +
                "WHERE u.Estado_pac = 3 OR u.Fecha_alta IS NOT NULL " +
                "ORDER BY u.Fecha_alta DESC " +
                "LIMIT 100";
        try (Connection conn = ConexionBD.conectar();
             //  try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            pacientesEgresadosCompleta.clear();

            while (rs.next()) {
                Paciente paciente = new Paciente(
                        rs.getInt("Folio"),
                        rs.getString("TipoAlta"),      // ← PRIMERO
                        rs.getString("Fecha_alta"),    // ← SEGUNDO
                        rs.getString("MedicoAlta"),    // ← TERCERO
                        rs.getString("CURP"),          // ← CUARTO
                        rs.getString("A_paterno"),
                        rs.getString("A_materno"),
                        rs.getString("Nombre"),
                        rs.getString("Sintomas"),
                        rs.getString("Personal_Ingresa"),
                        rs.getString("Medico_Asignado"),
                        3
                );
                pacientesEgresadosCompleta.add(paciente);
            }

            tablaEgresados.setItems(pacientesEgresadosCompleta);
            System.out.println("Pacientes egresados cargados: " + pacientesEgresadosCompleta.size());

        } catch (SQLException e) {
            System.err.println(" Error cargando pacientes egresados: " + e.getMessage());
        }
    }

    private void buscarPacientePorFolio(int folio) {
        String sql = "SELECT Folio, CONCAT(COALESCE(A_paterno, ''), ' ', COALESCE(A_materno, ''), ' ', COALESCE(Nombre, '')) as Nombre, " +
                "Estado_pac, TRIAGE, Fecha, Hora_registro " +
                "FROM tb_urgencias WHERE Folio = ?";
        try (Connection conn = ConexionBD.conectar();
             //  try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, folio);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String nombre = rs.getString("Nombre");
                String estado = rs.getInt("Estado_pac") == 1 ? "Espera" :
                        rs.getInt("Estado_pac") == 2 ? "Observación" : "Egresado";

                mostrarAlerta("Paciente Encontrado",
                        "Folio: " + folio + "\n" +
                                "Nombre: " + nombre + "\n" +
                                "Estado: " + estado + "\n" +
                                "TRIAGE: " + rs.getString("TRIAGE"),
                        Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("No Encontrado", "No se encontró paciente con folio: " + folio,
                        Alert.AlertType.WARNING);
            }

        } catch (SQLException e) {
            System.err.println(" Error buscando paciente: " + e.getMessage());
            mostrarAlerta("Error", "Error al buscar paciente: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void handleAbrirTriage() {
        System.out.println(" BOTÓN TRIAGE PRESIONADO");

        try {
            if (usuarioLogueado == null) {
                System.out.println(" No hay usuario logueado");
                return;
            }

            String rol = usuarioLogueado.getRol();
            boolean puedeAccederTriage = "ADMIN".equals(rol) ||
                    "TRIAGE".equals(rol) ||
                    "JEFATURA_URGENCIAS".equals(rol) ||
                    "ADMINISTRATIVO".equals(rol);

            if (!puedeAccederTriage) {
                System.out.println(" Rol sin permisos para TRIAGE: " + rol);
                mostrarAlerta("Acceso Denegado",
                        "No tiene permisos para acceder al módulo TRIAGE",
                        Alert.AlertType.WARNING);
                return;
            }

            System.out.println(" Abriendo módulo TRIAGE para: " + usuarioLogueado.getUsername() + " - Rol: " + rol);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/VentanaInicialTriage.fxml"));
            Parent root = loader.load();

            VentanaInicialTriageController controller = loader.getController();
            controller.setAdmin("ADMIN".equals(rol));

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Inicialización de Captura - TRIAGE");
            stage.setResizable(false);
            stage.show();

            System.out.println(" Ventana TRIAGE abierta exitosamente");

        } catch (Exception e) {
            System.err.println(" ERROR abriendo TRIAGE: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el módulo TRIAGE: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAbrirEstadistica() {
        System.out.println("BOTÓN ESTADÍSTICA PRESIONADO");

        try {
            if (usuarioLogueado == null) {
                System.out.println("No hay usuario logueado");
                return;
            }

            String rol = usuarioLogueado.getRol();
            boolean puedeAcceder = "ADMIN".equals(rol) ||
                    "JEFATURA_URGENCIAS".equals(rol) ||
                    "ADMINISTRATIVO".equals(rol);

            if (!puedeAcceder) {
                mostrarAlerta("Acceso Denegado",
                        "No tiene permisos para acceder al módulo de Estadística\n\n" +
                                "Roles permitidos: ADMIN, JEFATURA_URGENCIAS, ADMINISTRATIVO",
                        Alert.AlertType.WARNING);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ModuloEstadistica.fxml"));
            Parent root = loader.load();

            ModuloEstadisticaController controller = loader.getController();
            controller.setUsuarioLogueado(usuarioLogueado);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 900, 700));
            stage.setTitle("Módulo de Urgencias Estadística y Supervisión");
            stage.show();

            System.out.println("Ventana de Estadística abierta exitosamente");

        } catch (Exception e) {
            System.err.println("ERROR abriendo módulo de Estadística: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el módulo de Estadística: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAbrirPendientes() {
        System.out.println(" BOTÓN PENDIENTES PRESIONADO");

        try {
            if (usuarioLogueado == null) {
                System.out.println("No hay usuario logueado");
                return;
            }

            String rol = usuarioLogueado.getRol();
            boolean puedeAcceder = "ADMIN".equals(rol) || "TRIAGE".equals(rol);

            if (!puedeAcceder) {
                mostrarAlerta("Acceso Denegado",
                        "No tiene permisos para acceder al módulo de Pendientes\n\n" +
                                "Roles permitidos: ADMIN, TRIAGE",
                        Alert.AlertType.WARNING);
                return;
            }

            // Abrir ventana de selección para edición
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SeleccionEdicionPaciente.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Módulo de Pendientes - Completar Datos");
            stage.show();

            System.out.println(" Módulo de Pendientes abierto exitosamente");

        } catch (Exception e) {
            System.err.println(" ERROR abriendo módulo de Pendientes: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el módulo de Pendientes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAbrirJefatura() {
        System.out.println("BOTÓN JEFATURA PRESIONADO");

        try {
            if (usuarioLogueado == null) {
                System.out.println("No hay usuario logueado");
                return;
            }

            String rol = usuarioLogueado.getRol();
            boolean puedeAcceder = "ADMIN".equals(rol) || "JEFATURA_URGENCIAS".equals(rol);

            if (!puedeAcceder) {
                mostrarAlerta("Acceso Denegado",
                        "No tiene permisos para acceder al módulo de Jefatura\n\n" +
                                "Roles permitidos: ADMIN, JEFATURA_URGENCIAS",
                        Alert.AlertType.WARNING);
                return;
            }

            // Abrir ventana de Jefatura
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/jefatura.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("Módulo de Jefatura del Servicio de Urgencias");
            stage.show();

            System.out.println("Módulo de Jefatura abierto exitosamente");

        } catch (Exception e) {
            System.err.println("ERROR abriendo módulo de Jefatura: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el módulo de Jefatura: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void inicializarBusqueda() {
        cbTipoBusqueda.setItems(FXCollections.observableArrayList(
                "Todo", "Por Folio", "Por CURP", "Por Nombre", "Por Apellido"
        ));
        cbTipoBusqueda.setValue("Todo");

        txtBuscarGlobal.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrarPacientes();
        });

        cbTipoBusqueda.valueProperty().addListener((observable, oldValue, newValue) -> {
            filtrarPacientes();
        });
    }

    private void filtrarPacientes() {
        String textoBusqueda = txtBuscarGlobal.getText().toLowerCase();
        String tipoBusqueda = cbTipoBusqueda.getValue();

        if (textoBusqueda.isEmpty()) {
            tablaEspera.setItems(pacientesEsperaCompleta);
            tablaObservacion.setItems(pacientesObservacionCompleta);
            tablaEgresados.setItems(pacientesEgresadosCompleta);
            lblResultadosBusqueda.setText("");
        } else {
            filtrarTabla(tablaEspera, pacientesEsperaCompleta, textoBusqueda, tipoBusqueda, "espera");
            filtrarTabla(tablaObservacion, pacientesObservacionCompleta, textoBusqueda, tipoBusqueda, "observacion");
            filtrarTabla(tablaEgresados, pacientesEgresadosCompleta, textoBusqueda, tipoBusqueda, "egresados");
        }

        actualizarContadores();
    }

    private void filtrarTabla(TableView<Paciente> tabla, ObservableList<Paciente> listaCompleta,
                              String textoBusqueda, String tipoBusqueda, String tipoTabla) {
        ObservableList<Paciente> filtrada = FXCollections.observableArrayList();

        for (Paciente paciente : listaCompleta) {
            boolean coincide = false;

            switch (tipoBusqueda) {
                case "Por Folio":
                    coincide = String.valueOf(paciente.getFolio()).contains(textoBusqueda);
                    break;
                case "Por CURP":
                    coincide = paciente.getCurp() != null && paciente.getCurp().toLowerCase().contains(textoBusqueda);
                    break;
                case "Por Nombre":
                    coincide = paciente.getNombre().toLowerCase().contains(textoBusqueda);
                    break;
                case "Por Apellido":
                    coincide = (paciente.getPaterno() != null && paciente.getPaterno().toLowerCase().contains(textoBusqueda)) ||
                            (paciente.getMaterno() != null && paciente.getMaterno().toLowerCase().contains(textoBusqueda));
                    break;
                default: // "Todo"
                    coincide = String.valueOf(paciente.getFolio()).contains(textoBusqueda) ||
                            (paciente.getCurp() != null && paciente.getCurp().toLowerCase().contains(textoBusqueda)) ||
                            paciente.getNombre().toLowerCase().contains(textoBusqueda) ||
                            (paciente.getPaterno() != null && paciente.getPaterno().toLowerCase().contains(textoBusqueda)) ||
                            (paciente.getMaterno() != null && paciente.getMaterno().toLowerCase().contains(textoBusqueda));
                    break;
            }

            if (coincide) {
                filtrada.add(paciente);
            }
        }

        tabla.setItems(filtrada);

        int total = listaCompleta.size();
        int mostrados = filtrada.size();
        if (mostrados < total && !textoBusqueda.isEmpty()) {
            lblResultadosBusqueda.setText(String.format(" Mostrando %d de %d pacientes", mostrados, total));
        }
    }

    private void actualizarContadores() {
        new Thread(() -> {
            try (Connection conn = ConexionBD.conectar()) {

                //  UNA SOLA CONSULTA PARA TODOS LOS CONTADORES
                String sql = "SELECT " +
                        "SUM(CASE WHEN Estado_pac = 1 OR Estado_pac IS NULL THEN 1 ELSE 0 END) as espera, " +
                        "SUM(CASE WHEN Estado_pac = 2 THEN 1 ELSE 0 END) as observacion, " +
                        "SUM(CASE WHEN Estado_pac = 3 THEN 1 ELSE 0 END) as egresados, " +
                        "COUNT(*) as total " +
                        "FROM tb_urgencias";

                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {

                    if (rs.next()) {
                        int espera = rs.getInt("espera");
                        int observacion = rs.getInt("observacion");
                        int egresados = rs.getInt("egresados");
                        int total = rs.getInt("total");

                        Platform.runLater(() -> {
                            lblContadorEspera.setText(String.valueOf(espera));
                            lblContadorObservacion.setText(String.valueOf(observacion));
                            lblContadorEgresados.setText(String.valueOf(egresados));
                            lblTotalPacientes.setText(String.valueOf(total));

                            System.out.println(" Contadores optimizados cargados");
                        });
                    }
                }

            } catch (SQLException e) {
                System.err.println(" Error cargando contadores optimizados: " + e.getMessage());
            }
        }).start();
    }

    // === MANEJADORES DE BÚSQUEDA ===
    @FXML
    private void handleBuscarGlobal() {
        filtrarPacientes();
    }

    @FXML
    private void handleLimpiarBusqueda() {
        txtBuscarGlobal.clear();
        cbTipoBusqueda.setValue("Todo");
        filtrarPacientes();
    }

    @FXML
    private void handleOrdenar() {
        ordenAscendente = !ordenAscendente;

        Comparator<Paciente> comparador = ordenAscendente ?
                Comparator.comparing(Paciente::getNombre) :
                Comparator.comparing(Paciente::getNombre).reversed();

        FXCollections.sort(tablaEspera.getItems(), comparador);
        FXCollections.sort(tablaObservacion.getItems(), comparador);
        FXCollections.sort(tablaEgresados.getItems(), comparador);

        Button btn = (Button) txtBuscarGlobal.getScene().lookup("#btnOrdenar");
        if (btn != null) {
            btn.setText(ordenAscendente ? " Ordenar A-Z" : " Ordenar Z-A");
        }
    }


}