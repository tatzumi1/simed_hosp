package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.utils.DatosPacienteUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SeleccionEdicionPacienteController {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cbTipoBusqueda;
    @FXML private TableView<PacienteSeleccion> tablaPacientes;
    @FXML private TableColumn<PacienteSeleccion, Integer> colFolio;
    @FXML private TableColumn<PacienteSeleccion, String> colColor;
    @FXML private TableColumn<PacienteSeleccion, String> colPaterno;
    @FXML private TableColumn<PacienteSeleccion, String> colMaterno;
    @FXML private TableColumn<PacienteSeleccion, String> colNombre;
    @FXML private TableColumn<PacienteSeleccion, Integer> colEdad;
    @FXML private TableColumn<PacienteSeleccion, String> colTriage;
    @FXML private TableColumn<PacienteSeleccion, String> colEstado;
    @FXML private TableColumn<PacienteSeleccion, String> colFechaRegistro;
    @FXML private TableColumn<PacienteSeleccion, String> colAccion;

    @FXML private ComboBox<String> cbFiltroEstado;
    @FXML private ComboBox<String> cbFiltroCompletitud;
    @FXML private Label lblContador;

    private ObservableList<PacienteSeleccion> todosLosPacientes = FXCollections.observableArrayList();
    private FilteredList<PacienteSeleccion> pacientesFiltrados;

    public static class PacienteSeleccion {
        private final SimpleIntegerProperty folio;
        private final SimpleStringProperty paterno;
        private final SimpleStringProperty materno;
        private final SimpleStringProperty nombre;
        private final SimpleIntegerProperty edad;
        private final SimpleStringProperty triage;
        private final SimpleStringProperty estado;
        private final SimpleStringProperty fechaRegistro;
        private final SimpleStringProperty colorCompletitud;
        private final SimpleStringProperty curp;

        public PacienteSeleccion(int folio, String paterno, String materno, String nombre,
                                 int edad, String triage, String estado, String fechaRegistro,
                                 String colorCompletitud, String curp) {
            this.folio = new SimpleIntegerProperty(folio);
            this.paterno = new SimpleStringProperty(paterno != null ? paterno : "");
            this.materno = new SimpleStringProperty(materno != null ? materno : "");
            this.nombre = new SimpleStringProperty(nombre != null ? nombre : "");
            this.edad = new SimpleIntegerProperty(edad);
            this.triage = new SimpleStringProperty(triage != null ? triage : "");
            this.estado = new SimpleStringProperty(estado != null ? estado : "");
            this.fechaRegistro = new SimpleStringProperty(fechaRegistro != null ? fechaRegistro : "");
            this.colorCompletitud = new SimpleStringProperty(colorCompletitud != null ? colorCompletitud : "");
            this.curp = new SimpleStringProperty(curp != null ? curp : "");
        }

        public String getCurp() { return curp.get(); }
        public int getFolio() { return folio.get(); }
        public String getPaterno() { return paterno.get(); }
        public String getMaterno() { return materno.get(); }
        public String getNombre() { return nombre.get(); }
        public int getEdad() { return edad.get(); }
        public String getTriage() { return triage.get(); }
        public String getEstado() { return estado.get(); }
        public String getFechaRegistro() { return fechaRegistro.get(); }
        public String getColorCompletitud() { return colorCompletitud.get(); }
    }

    @FXML
    public void initialize() {
        configurarCombos();
        configurarColumnas();
        configurarFiltros();
        cargarPacientes();
        aplicarColoresTabla();
    }

    private void configurarCombos() {
        // Configurar opciones de los ComboBox
        cbFiltroEstado.setItems(FXCollections.observableArrayList(
                "Todos", "En Espera", "En Observación", "Egresados"
        ));

        cbFiltroCompletitud.setItems(FXCollections.observableArrayList(
                "Todos", "Muy Incompletos", "Moderados", "Casi Completos"
        ));

        cbTipoBusqueda.setItems(FXCollections.observableArrayList(
                "Todo", "Por Folio", "Por Nombre", "Por CURP", "Por Apellido"
        ));

        cbFiltroEstado.setValue("Todos");
        cbFiltroCompletitud.setValue("Todos");
        cbTipoBusqueda.setValue("Todo");
    }

    private void configurarColumnas() {
        colFolio.setCellValueFactory(new PropertyValueFactory<>("folio"));
        colPaterno.setCellValueFactory(new PropertyValueFactory<>("paterno"));
        colMaterno.setCellValueFactory(new PropertyValueFactory<>("materno"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEdad.setCellValueFactory(new PropertyValueFactory<>("edad"));
        colTriage.setCellValueFactory(new PropertyValueFactory<>("triage"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colFechaRegistro.setCellValueFactory(new PropertyValueFactory<>("fechaRegistro"));

        // Columna de color
        colColor.setCellFactory(new Callback<TableColumn<PacienteSeleccion, String>, TableCell<PacienteSeleccion, String>>() {
            @Override
            public TableCell<PacienteSeleccion, String> call(TableColumn<PacienteSeleccion, String> param) {
                return new TableCell<PacienteSeleccion, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                            setText("");
                            setStyle("");
                        } else {
                            PacienteSeleccion paciente = getTableRow().getItem();
                            String color = paciente.getColorCompletitud();
                            String descripcion = DatosPacienteUtils.obtenerDescripcionNivel(color);
                            setText(descripcion);

                            String colorCSS = DatosPacienteUtils.obtenerColorCSS(color);
                            setStyle("-fx-background-color: " + colorCSS + "; -fx-border-color: #666; -fx-alignment: CENTER; -fx-text-fill: white; -fx-font-weight: bold;");
                        }
                    }
                };
            }
        });

        // Columna de acción - CON COLORES AZULES PROFESIONALES
        colAccion.setCellFactory(new Callback<TableColumn<PacienteSeleccion, String>, TableCell<PacienteSeleccion, String>>() {
            @Override
            public TableCell<PacienteSeleccion, String> call(TableColumn<PacienteSeleccion, String> param) {
                return new TableCell<PacienteSeleccion, String>() {
                    private final Button btnEditar = new Button("Editar");

                    {
                        // COLOR AZUL PROFESIONAL
                        btnEditar.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 4; -fx-padding: 5 8;");

                        // Efectos hover
                        btnEditar.setOnMouseEntered(e ->
                                btnEditar.setStyle("-fx-background-color: #60a5fa; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 4; -fx-padding: 5 8;")
                        );
                        btnEditar.setOnMouseExited(e ->
                                btnEditar.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 4; -fx-padding: 5 8;")
                        );

                        btnEditar.setOnAction(event -> {
                            PacienteSeleccion paciente = getTableView().getItems().get(getIndex());
                            if (paciente != null) {
                                abrirEdicionPaciente(paciente.getFolio());
                            }
                        });
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnEditar);
                        }
                    }
                };
            }
        });
    }

    private void configurarFiltros() {
        pacientesFiltrados = new FilteredList<>(todosLosPacientes);
        tablaPacientes.setItems(pacientesFiltrados);
    }

    @FXML
    private void aplicarFiltros() {
        aplicarFiltrosCombinados();
    }

    @FXML
    private void limpiarFiltros() {
        cbFiltroEstado.setValue("Todos");
        cbFiltroCompletitud.setValue("Todos");
        aplicarFiltros();
    }

    @FXML
    private void filtrarPorBusqueda() {
        aplicarFiltrosCombinados();
    }

    @FXML
    private void limpiarBusqueda() {
        txtBuscar.clear();
        cbTipoBusqueda.setValue("Todo");
        aplicarFiltrosCombinados();
    }

    private void aplicarFiltrosCombinados() {
        String textoBusqueda = txtBuscar.getText().toLowerCase().trim();
        String tipoBusqueda = cbTipoBusqueda.getValue();

        pacientesFiltrados.setPredicate(paciente -> {
            // 1. Aplicar filtros de estado y completitud
            String filtroEstado = cbFiltroEstado.getValue();
            if (!"Todos".equals(filtroEstado)) {
                String estadoPaciente = paciente.getEstado();
                if (!filtroEstado.equals(estadoPaciente)) {
                    return false;
                }
            }

            String filtroCompletitud = cbFiltroCompletitud.getValue();
            if (!"Todos".equals(filtroCompletitud)) {
                String colorPaciente = paciente.getColorCompletitud();
                switch (filtroCompletitud) {
                    case "Muy Incompletos":
                        if (!"AZUL_OSCURO".equals(colorPaciente)) return false;
                        break;
                    case "Moderados":
                        if (!"AZUL_MEDIO".equals(colorPaciente)) return false;
                        break;
                    case "Casi Completos":
                        if (!"AZUL_CLARO".equals(colorPaciente)) return false;
                        break;
                }
            }

            // 2. Aplicar búsqueda de texto
            if (!textoBusqueda.isEmpty()) {
                boolean coincideBusqueda = false;

                switch (tipoBusqueda) {
                    case "Por Folio":
                        coincideBusqueda = String.valueOf(paciente.getFolio()).contains(textoBusqueda);
                        break;
                    case "Por Nombre":
                        coincideBusqueda = paciente.getNombre().toLowerCase().contains(textoBusqueda);
                        break;
                    case "Por CURP":
                        coincideBusqueda = paciente.getCurp().toLowerCase().contains(textoBusqueda);
                        break;
                    case "Por Apellido":
                        coincideBusqueda = paciente.getPaterno().toLowerCase().contains(textoBusqueda) ||
                                paciente.getMaterno().toLowerCase().contains(textoBusqueda);
                        break;
                    default: // "Todo"
                        coincideBusqueda = String.valueOf(paciente.getFolio()).contains(textoBusqueda) ||
                                paciente.getNombre().toLowerCase().contains(textoBusqueda) ||
                                paciente.getPaterno().toLowerCase().contains(textoBusqueda) ||
                                paciente.getMaterno().toLowerCase().contains(textoBusqueda) ||
                                paciente.getCurp().toLowerCase().contains(textoBusqueda);
                        break;
                }

                if (!coincideBusqueda) {
                    return false;
                }
            }

            return true;
        });

        actualizarContador();
    }

    private void cargarPacientes() {
        String sql = "SELECT " +
                "u.Folio, u.A_paterno, u.A_materno, u.Nombre, u.Edad, " +
                "u.TRIAGE, u.Fecha, u.Hora_registro, u.Domicilio, u.Telefono, u.CURP, " +
                "CASE " +
                "   WHEN u.Estado_pac = 1 THEN 'En Espera' " +
                "   WHEN u.Estado_pac = 2 THEN 'En Observación' " +
                "   WHEN u.Estado_pac = 3 THEN 'Egresados' " +
                "   ELSE 'No especificado' " +
                "END as Estado " +
                "FROM tb_urgencias u " +
                "ORDER BY u.Fecha DESC, u.Hora_registro DESC";

        try (Connection conn = ConexionBD.getSafeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            todosLosPacientes.clear();

            while (rs.next()) {
                // OBTENER Y FORMATEAR FECHA Y HORA CORRECTAMENTE
                String fechaCompleta = rs.getString("Fecha");
                String hora = rs.getString("Hora_registro");

                // Extraer solo la parte de la fecha (sin el 00:00:00)
                String fechaSola = fechaCompleta.split(" ")[0];

                String fechaHoraCompleta;
                if (hora != null && !hora.isEmpty() && !hora.equals("00:00:00")) {
                    fechaHoraCompleta = fechaSola + " " + hora;
                } else {
                    fechaHoraCompleta = fechaSola;
                }

                // Crear paciente temporal para calcular completitud
                MainController.Paciente pacienteTemp = new MainController.Paciente(
                        rs.getInt("Folio"),
                        rs.getString("CURP"),
                        rs.getString("A_paterno"),
                        rs.getString("A_materno"),
                        rs.getString("Nombre"),
                        "", "", "", "", ""
                );
                pacienteTemp.setEdad(rs.getInt("Edad"));
                pacienteTemp.setDomicilio(rs.getString("Domicilio"));
                pacienteTemp.setTelefono(rs.getString("Telefono"));

                // Calcular completitud y color
                int puntos = DatosPacienteUtils.calcularCompletitud(pacienteTemp);
                String color = DatosPacienteUtils.obtenerColorCompletitud(puntos);

                PacienteSeleccion pacienteSeleccion = new PacienteSeleccion(
                        rs.getInt("Folio"),
                        rs.getString("A_paterno"),
                        rs.getString("A_materno"),
                        rs.getString("Nombre"),
                        rs.getInt("Edad"),
                        rs.getString("TRIAGE"),
                        rs.getString("Estado"),
                        fechaHoraCompleta,
                        color,
                        rs.getString("CURP")
                );

                todosLosPacientes.add(pacienteSeleccion);
            }

            aplicarFiltros();
            System.out.println(" Pacientes cargados para edición: " + todosLosPacientes.size());

        } catch (SQLException e) {
            System.err.println(" Error cargando pacientes: " + e.getMessage());
            mostrarAlerta("Error", "No se pudieron cargar los pacientes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void aplicarColoresTabla() {
        tablaPacientes.setRowFactory(tv -> new TableRow<PacienteSeleccion>() {
            @Override
            protected void updateItem(PacienteSeleccion paciente, boolean empty) {
                super.updateItem(paciente, empty);

                if (empty || paciente == null) {
                    setStyle("");
                } else {
                    String colorCSS = DatosPacienteUtils.obtenerColorCSS(paciente.getColorCompletitud());
                    setStyle("-fx-background-color: " + colorCSS + "; -fx-border-color: #666;");
                }
            }
        });
    }

    private void actualizarContador() {
        int total = todosLosPacientes.size();
        int mostrados = pacientesFiltrados.size();
        lblContador.setText("Mostrando: " + mostrados + " de " + total + " pacientes");
    }

    private void abrirEdicionPaciente(int folio) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("views/EdicionPaciente.fxml"));

            Parent root = loader.load();

            EdicionPacienteController controller = loader.getController();
            controller.cargarPacienteExistente(folio);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Editar Paciente - Folio: " + folio);
            stage.setResizable(false);
            stage.show();

            regresar();

        } catch (Exception e) {
            System.err.println("Error abriendo edición: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la edición:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void regresar() {
        // Volver al MainController
        Stage stage = (Stage) tablaPacientes.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void cancelar() {
        Stage stage = (Stage) tablaPacientes.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}