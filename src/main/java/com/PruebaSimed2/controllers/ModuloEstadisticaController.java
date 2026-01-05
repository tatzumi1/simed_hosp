package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.Usuario;
import com.PruebaSimed2.utils.PDFGeneratorEstadisticas;
import com.PruebaSimed2.utils.PDFGeneratorDiario;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.io.File;
import javafx.util.Pair;
import java.util.Optional;

public class ModuloEstadisticaController implements Initializable {
    @FXML private TextField txtFechaDiaria;
    @FXML private ComboBox<String> cbMes;
    @FXML private ComboBox<Integer> cbAnio;
    @FXML private ComboBox<String> cbTipoProductividad;
    @FXML private TableView<EstadisticaMedico> tablaEstadisticas;
    @FXML private VBox panelEstadisticas;

    // Nuevos botones
    @FXML private Button btnVerFoliosDia;
    @FXML private Button btnImprimirRango;
    @FXML private Button btnImprimirUnFolio;

    private Usuario usuarioLogueado;
    private List<Integer> foliosDelDiaActual = new ArrayList<>();

    // Clase para representar estadísticas de médicos
    public static class EstadisticaMedico {
        private final String medico;
        private final int cantidad;
        private final double porcentaje;

        public EstadisticaMedico(String medico, int cantidad, double porcentaje) {
            this.medico = medico;
            this.cantidad = cantidad;
            this.porcentaje = porcentaje;
        }

        // Getters
        public String getMedico() { return medico; }
        public int getCantidad() { return cantidad; }
        public double getPorcentaje() { return porcentaje; }
    }

    public void setUsuarioLogueado(Usuario usuario) {
        this.usuarioLogueado = usuario;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarCombobox();
        configurarTabla();
    }

    private void configurarCombobox() {
        // Configurar meses
        cbMes.setItems(FXCollections.observableArrayList(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        ));

        // Configurar años
        ObservableList<Integer> años = FXCollections.observableArrayList();
        int añoActual = Year.now().getValue();
        for (int i = 2020; i <= 2030; i++) {
            años.add(i);
        }
        cbAnio.setItems(años);
        cbAnio.setValue(añoActual);

        // Configurar tipos de productividad
        cbTipoProductividad.setItems(FXCollections.observableArrayList(
                "Ingresos", "Egresos", "Notas médicas", "Interconsultas"
        ));

        // Configurar fecha actual
        txtFechaDiaria.setText(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private void configurarTabla() {
        // Configurar las columnas de la tabla
        TableColumn<EstadisticaMedico, String> colMedico = new TableColumn<>("Médico");
        colMedico.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMedico()));

        TableColumn<EstadisticaMedico, Integer> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCantidad()).asObject());

        TableColumn<EstadisticaMedico, String> colPorcentaje = new TableColumn<>("Porcentaje");
        colPorcentaje.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.2f%%", cellData.getValue().getPorcentaje())));

        tablaEstadisticas.getColumns().clear();
        tablaEstadisticas.getColumns().addAll(colMedico, colCantidad, colPorcentaje);
    }

    // ========== NUEVOS MÉTODOS PARA GESTIÓN DE FOLIOS ==========

    /**
     * Obtiene y muestra todos los folios egresados del día
     */
    @FXML
    private void handleVerFoliosDia() {
        String fecha = txtFechaDiaria.getText().trim();
        if (fecha.isEmpty()) {
            mostrarAlerta("Error", "Por favor ingrese una fecha", Alert.AlertType.WARNING);
            return;
        }

        if (!validarFormatoFecha(fecha)) {
            mostrarAlerta("Error", "Formato de fecha inválido. Use dd/MM/aaaa", Alert.AlertType.ERROR);
            return;
        }

        try {
            // Obtener folios del día ordenados cronológicamente
            foliosDelDiaActual = PDFGeneratorDiario.obtenerFoliosEgresadosFecha(fecha);

            if (foliosDelDiaActual.isEmpty()) {
                mostrarAlerta("Sin folios", "No hay folios egresados para el día " + fecha, Alert.AlertType.INFORMATION);
                return;
            }

            // Crear ventana emergente para mostrar los folios
            crearVentanaFoliosDia(fecha, foliosDelDiaActual);

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudieron obtener los folios: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void crearVentanaFoliosDia(String fecha, List<Integer> folios) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Folios Egresados - " + fecha);
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 1;");

        // Título
        Label titulo = new Label(" Folios egresados el día " + fecha);
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #002b5c;");

        // Lista de folios
        TextArea areaFolios = new TextArea();
        areaFolios.setEditable(false);
        areaFolios.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        areaFolios.setPrefRowCount(Math.min(folios.size() + 2, 20));

        StringBuilder foliosText = new StringBuilder();
        for (int i = 0; i < folios.size(); i++) {
            foliosText.append(String.format("%3d. Folio: %d\n", i + 1, folios.get(i)));
        }
        areaFolios.setText(foliosText.toString());

        // Información
        Label info = new Label(String.format("Total: %d folios egresados (ordenados por hora de alta)", folios.size()));
        info.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        // Botones de acción
        HBox botonesBox = new HBox(10);
        botonesBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnCopiar = new Button(" Copiar Lista");
        btnCopiar.setOnAction(e -> copiarListaFolios(foliosText.toString()));
        btnCopiar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> dialogStage.close());
        btnCerrar.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 5;");

        botonesBox.getChildren().addAll(btnCopiar, btnCerrar);

        vbox.getChildren().addAll(titulo, areaFolios, info, botonesBox);

        Scene scene = new Scene(vbox, 350, 400);
        dialogStage.setScene(scene);
        dialogStage.show();
    }

    private void copiarListaFolios(String texto) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(texto);
        clipboard.setContent(content);

        mostrarAlerta("Copiado", "Lista de folios copiada al portapapeles", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleImprimirRangoFolios() {
        String fecha = txtFechaDiaria.getText().trim();
        if (fecha.isEmpty()) {
            mostrarAlerta("Error", "Por favor ingrese una fecha", Alert.AlertType.WARNING);
            return;
        }

        if (foliosDelDiaActual.isEmpty()) {
            foliosDelDiaActual = PDFGeneratorDiario.obtenerFoliosEgresadosFecha(fecha);
        }

        if (foliosDelDiaActual.isEmpty()) {
            mostrarAlerta("Sin folios", "No hay folios egresados para esta fecha", Alert.AlertType.WARNING);
            return;
        }

        // Crear diálogo para ingresar rango
        Dialog<Pair<Integer, Integer>> dialog = new Dialog<>();
        dialog.setTitle(" Imprimir Rango de Folios");
        dialog.setHeaderText("Seleccione el rango de folios a imprimir\n(Total disponible: " + foliosDelDiaActual.size() + " folios)");

        // Botones
        ButtonType imprimirButtonType = new ButtonType("Imprimir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(imprimirButtonType, ButtonType.CANCEL);

        // Crear campos
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Spinner<Integer> desdeSpinner = new Spinner<>(1, foliosDelDiaActual.size(), 1);
        desdeSpinner.setEditable(true);

        Spinner<Integer> hastaSpinner = new Spinner<>(1, foliosDelDiaActual.size(), foliosDelDiaActual.size());
        hastaSpinner.setEditable(true);

        // Labels informativos
        Label labelDesde = new Label("Desde (posición):");
        Label labelHasta = new Label("Hasta (posición):");
        Label labelInfo = new Label(String.format("Folios reales: %d - %d",
                foliosDelDiaActual.get(0),
                foliosDelDiaActual.get(foliosDelDiaActual.size()-1)));
        labelInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        grid.add(labelDesde, 0, 0);
        grid.add(desdeSpinner, 1, 0);
        grid.add(labelHasta, 0, 1);
        grid.add(hastaSpinner, 1, 1);
        grid.add(labelInfo, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == imprimirButtonType) {
                int desde = desdeSpinner.getValue();
                int hasta = hastaSpinner.getValue();

                if (desde > hasta) {
                    mostrarAlerta("Error", "El valor 'Desde' no puede ser mayor que 'Hasta'", Alert.AlertType.ERROR);
                    return null;
                }

                return new Pair<>(desde, hasta);
            }
            return null;
        });

        Optional<Pair<Integer, Integer>> result = dialog.showAndWait();
        result.ifPresent(rango -> {
            int desde = rango.getKey();
            int hasta = rango.getValue();

            // Confirmar
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Confirmar Impresión");
            confirmacion.setHeaderText("Imprimir folios del rango " + desde + " al " + hasta);
            confirmacion.setContentText(String.format(
                    "Se imprimirán %d folios.\n" +
                            "Folios reales: %d al %d\n\n" +
                            "¿Continuar?",
                    (hasta - desde + 1),
                    foliosDelDiaActual.get(desde-1),
                    foliosDelDiaActual.get(hasta-1)
            ));

            Optional<ButtonType> confirmResult = confirmacion.showAndWait();
            if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                imprimirRangoFolios(fecha, desde, hasta);
            }
        });
    }

    private void imprimirRangoFolios(String fecha, int desde, int hasta) {
        try {
            // Generar PDF del rango
            InputStream pdfStream = PDFGeneratorDiario.generarPDFPorRango(foliosDelDiaActual, desde, hasta, fecha, true);

            if (pdfStream == null) {
                mostrarAlerta("Error", "No se pudo generar el PDF para el rango seleccionado", Alert.AlertType.ERROR);
                return;
            }

            // Guardar el PDF
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PDF del Rango");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

            // Nombre del archivo
            String[] partesFecha = fecha.split("/");
            String nombreArchivo = String.format("Egresos_%s_%s_%s_rango_%d-%d.pdf",
                    partesFecha[2], partesFecha[1], partesFecha[0], desde, hasta);
            fileChooser.setInitialFileName(nombreArchivo);

            File archivo = fileChooser.showSaveDialog(txtFechaDiaria.getScene().getWindow());

            if (archivo != null) {
                // Guardar PDF
                PDFGeneratorDiario.guardarPDF(pdfStream, archivo.getAbsolutePath());

                // Preguntar si quiere imprimir
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("PDF Generado");
                alert.setHeaderText("PDF guardado exitosamente");
                alert.setContentText("¿Desea abrir el PDF para imprimir ahora?");

                Optional<ButtonType> resultado = alert.showAndWait();
                if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                    // Abrir PDF
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                        desktop.open(archivo);

                        mostrarAlerta("PDF Listo",
                                "PDF abierto en el visor.\n" +
                                        "Ubicación: " + archivo.getAbsolutePath() + "\n" +
                                        "Por favor imprima desde el visor de PDF.",
                                Alert.AlertType.INFORMATION);
                    }
                }
            }

        } catch (Exception e) {
            mostrarAlerta("Error", "Error al procesar el rango de folios: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleImprimirUnFolio() {
        String fecha = txtFechaDiaria.getText().trim();
        if (fecha.isEmpty()) {
            mostrarAlerta("Error", "Por favor ingrese una fecha", Alert.AlertType.WARNING);
            return;
        }

        // Crear diálogo para ingresar folio
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle(" Imprimir Folio Individual");
        dialog.setHeaderText("Ingrese el número de folio a imprimir");

        // Botones
        ButtonType imprimirButtonType = new ButtonType("Buscar e Imprimir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(imprimirButtonType, ButtonType.CANCEL);

        // Crear campos
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtFolio = new TextField();
        txtFolio.setPromptText("Ej: 12345");

        // Label informativo
        Label labelInfo = new Label("Ingrese el número de folio (ej: 12345)");
        labelInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        grid.add(new Label("Número de Folio:"), 0, 0);
        grid.add(txtFolio, 1, 0);
        grid.add(labelInfo, 0, 1, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == imprimirButtonType) {
                try {
                    int folio = Integer.parseInt(txtFolio.getText().trim());
                    return folio;
                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "Ingrese un número de folio válido", Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(folio -> {
            imprimirFolioIndividual(fecha, folio);
        });
    }

    private void imprimirFolioIndividual(String fecha, int folio) {
        try {
            // Verificar que el folio exista
            boolean existe = verificarFolioExiste(folio, fecha);

            if (!existe) {
                mostrarAlerta("Folio no encontrado",
                        String.format("El folio %d no existe o no corresponde a la fecha %s", folio, fecha),
                        Alert.AlertType.WARNING);
                return;
            }

            // Generar PDF para el folio individual
            InputStream pdfStream = PDFGeneratorDiario.generarPDFParaUnFolio(folio, fecha, true);

            if (pdfStream == null) {
                mostrarAlerta("Error", "No se pudo generar el PDF para el folio " + folio, Alert.AlertType.ERROR);
                return;
            }

            // Guardar el PDF
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PDF del Folio");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

            // Nombre del archivo
            String[] partesFecha = fecha.split("/");
            String nombreArchivo = String.format("Egreso_%s_%s_%s_folio_%d.pdf",
                    partesFecha[2], partesFecha[1], partesFecha[0], folio);
            fileChooser.setInitialFileName(nombreArchivo);

            File archivo = fileChooser.showSaveDialog(txtFechaDiaria.getScene().getWindow());

            if (archivo != null) {
                // Guardar PDF
                PDFGeneratorDiario.guardarPDF(pdfStream, archivo.getAbsolutePath());

                // Preguntar si quiere imprimir
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("PDF Generado");
                alert.setHeaderText("PDF guardado exitosamente");
                alert.setContentText("¿Desea abrir el PDF para imprimir ahora?");

                Optional<ButtonType> resultado = alert.showAndWait();
                if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                    // Abrir PDF
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                        desktop.open(archivo);

                        mostrarAlerta("PDF Listo",
                                String.format("PDF del folio %d abierto en el visor.\n" +
                                                "Ubicación: %s\n" +
                                                "Por favor imprima desde el visor de PDF.",
                                        folio, archivo.getAbsolutePath()),
                                Alert.AlertType.INFORMATION);
                    }
                }
            }

        } catch (Exception e) {
            mostrarAlerta("Error", "Error al procesar el folio individual: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private boolean verificarFolioExiste(int folio, String fechaStr) {
        try {
            LocalDate fecha = LocalDate.parse(fechaStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            String sql = "SELECT COUNT(*) as existe FROM tb_egresos " +
                    "WHERE Folio = ? " +
                    "AND DATE(Fecha_alta) = ?";

            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, folio);
                pstmt.setDate(2, java.sql.Date.valueOf(fecha));

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("existe") > 0;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al verificar folio: " + e.getMessage());
        }

        return false;
    }
    // de aqui arriba no mover -------------------------------------------




    // ========== MÉTODOS PARA GRÁFICAS ==========
    @FXML
    private void handleVisualizarGrafica() {
        if (validarSeleccionMensual()) {
            String mes = cbMes.getValue();
            Integer año = cbAnio.getValue();
            String tipo = cbTipoProductividad.getValue();

            List<EstadisticaMedico> estadisticas = obtenerEstadisticasDesdeBD(mes, año, tipo);

            if (estadisticas != null && !estadisticas.isEmpty()) {
                boolean exito = PDFGeneratorEstadisticas.generarPDFGrafica(estadisticas, mes, año, tipo, false);

                if (exito) {
                    mostrarAlerta("Gráfica Visualizada",
                            "Se abrió la gráfica PDF de " + tipo.toLowerCase() + " para visualización",
                            Alert.AlertType.INFORMATION);
                } else {
                    mostrarAlerta("Error",
                            "No se pudo generar la gráfica PDF de " + tipo.toLowerCase(),
                            Alert.AlertType.ERROR);
                }
            } else {
                mostrarAlerta("Sin Datos",
                        "No hay datos suficientes para generar la gráfica de " + tipo.toLowerCase(),
                        Alert.AlertType.WARNING);
            }
        }
    }


    @FXML
    private void handleVisualizarRelacion() {
        if (validarSeleccionMensual()) {
            String mes = cbMes.getValue();
            Integer año = cbAnio.getValue();
            String tipo = cbTipoProductividad.getValue();

            List<EstadisticaMedico> estadisticas = obtenerEstadisticasDesdeBD(mes, año, tipo);

            if (estadisticas != null && !estadisticas.isEmpty()) {
                boolean exito = PDFGeneratorEstadisticas.generarPDF(estadisticas, mes, año, tipo, false);

                if (exito) {
                    mostrarAlerta("PDF Visualizado",
                            "Se abrió el PDF de relación de " + tipo.toLowerCase() + " para visualización",
                            Alert.AlertType.INFORMATION);
                } else {
                    mostrarAlerta("Error",
                            "No se pudo generar el PDF de relación",
                            Alert.AlertType.ERROR);
                }
            }
        }
    }


    private List<EstadisticaMedico> obtenerEstadisticasDesdeBD(String mes, int año, String tipo) {
        try {
            // 1. Obtener TODAS las personas para este tipo
            List<String> todasPersonas = obtenerTodasLasPersonasPorTipo(tipo);

            if (todasPersonas.isEmpty()) {
                mostrarAlerta("Sin Personas", "No se encontraron personas para: " + tipo, Alert.AlertType.WARNING);
                return new ArrayList<>();
            }

            System.out.println("=== PASO 1: Personas encontradas para " + tipo + " ===");
            System.out.println("Total: " + todasPersonas.size());
            for (int i = 0; i < Math.min(5, todasPersonas.size()); i++) {
                System.out.println("  " + (i+1) + ". " + todasPersonas.get(i));
            }
            if (todasPersonas.size() > 5) {
                System.out.println("  ... y " + (todasPersonas.size() - 5) + " más");
            }

            // 2. Obtener estadísticas REALES de quienes sí trabajaron
            Map<String, Integer> estadisticasReales = obtenerEstadisticasReales(mes, año, tipo);

            System.out.println("=== PASO 2: Estadísticas reales ===");
            System.out.println("Con actividad: " + estadisticasReales.size());
            estadisticasReales.forEach((nombre, cantidad) -> {
                System.out.println("  " + nombre + ": " + cantidad);
            });

            // 3. Crear lista combinada (TODOS, incluso con 0)
            ObservableList<EstadisticaMedico> datos = FXCollections.observableArrayList();
            int totalGeneral = 0;

            for (String persona : todasPersonas) {
                int cantidad = estadisticasReales.getOrDefault(persona, 0);
                datos.add(new EstadisticaMedico(persona, cantidad, 0.0));
                totalGeneral += cantidad;
            }

            // 4. ORDENAR de MAYOR a MENOR cantidad
            datos.sort((a, b) -> Integer.compare(b.getCantidad(), a.getCantidad()));

            // 5. Calcular porcentajes SOLO si hay total
            if (totalGeneral > 0) {
                for (int i = 0; i < datos.size(); i++) {
                    EstadisticaMedico em = datos.get(i);
                    double porcentaje = (em.getCantidad() * 100.0) / totalGeneral;
                    datos.set(i, new EstadisticaMedico(em.getMedico(), em.getCantidad(), porcentaje));
                }
            }

            // 6. Mostrar en tabla
            tablaEstadisticas.setItems(datos);
            panelEstadisticas.setVisible(true);

            System.out.println("=== RESULTADO FINAL ===");
            System.out.println("Tipo: " + tipo);
            System.out.println("Total personas en lista: " + datos.size());
            System.out.println("Total registros trabajados: " + totalGeneral);
            System.out.println("Orden: " + datos.get(0).getMedico() + " (" + datos.get(0).getCantidad() + ") → " +
                    datos.get(datos.size()-1).getMedico() + " (" + datos.get(datos.size()-1).getCantidad() + ")");

            return datos;

        } catch (SQLException e) {
            System.err.println("Error obteniendo estadísticas: " + e.getMessage());
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron obtener las estadísticas: " + e.getMessage(), Alert.AlertType.ERROR);
            return new ArrayList<>();
        }
    }



    private List<String> obtenerTodasLasPersonasPorTipo(String tipo) throws SQLException {
        List<String> personas = new ArrayList<>();
        String sql = "";

        switch (tipo) {
            case "Ingresos":
                // TODOS los que pueden hacer ingresos: TRIAGE, JEFATURA, ADMINISTRATIVO
                sql = "SELECT DISTINCT COALESCE(NULLIF(nombre_completo, ''), username) as nombre " +
                        "FROM tb_usuarios WHERE activo = 1 " +
                        "AND rol IN ('TRIAGE', 'JEFATURA_URGENCIAS', 'ADMINISTRATIVO') " +
                        "ORDER BY COALESCE(NULLIF(nombre_completo, ''), username)";
                break;

            case "Notas médicas":
                // TODOS los médicos generales de tb_medicos - CORREGIDO
                sql = "SELECT DISTINCT TRIM(Med_nombre) as nombre " +
                        "FROM tb_medicos " +
                        "WHERE Med_nombre IS NOT NULL AND TRIM(Med_nombre) != '' " +
                        "ORDER BY TRIM(Med_nombre)";
                break;

            case "Interconsultas":
                // TODOS los médicos especialistas de tb_medesp - CORREGIDO
                sql = "SELECT DISTINCT TRIM(Nombre) as nombre " +
                        "FROM tb_medesp " +
                        "WHERE Nombre IS NOT NULL AND TRIM(Nombre) != '' " +
                        "ORDER BY TRIM(Nombre)";
                break;

            case "Egresos":
                // TODOS los médicos generales (mismos que notas)
                sql = "SELECT DISTINCT TRIM(Med_nombre) as nombre " +
                        "FROM tb_medicos " +
                        "WHERE Med_nombre IS NOT NULL AND TRIM(Med_nombre) != '' " +
                        "ORDER BY TRIM(Med_nombre)";
                break;

            default:
                return personas;
        }

        System.out.println("Ejecutando SQL para " + tipo + ": " + sql);

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                if (nombre != null && !nombre.trim().isEmpty()) {
                    personas.add(nombre.trim());
                }
            }
        }

        return personas;
    }


    private Map<String, Integer> obtenerEstadisticasReales(String mes, int año, String tipo) throws SQLException {
        Map<String, Integer> estadisticas = new HashMap<>();
        int numeroMes = obtenerNumeroMes(mes);

        String sql = "";

        switch (tipo) {
            case "Ingresos":
                // Buscar en tb_urgencias.Nombre_ts (enfermeros triage)
                sql = "SELECT TRIM(Nombre_ts) as nombre, COUNT(*) as cantidad " +
                        "FROM tb_urgencias " +
                        "WHERE Nombre_ts IS NOT NULL AND TRIM(Nombre_ts) != '' " +
                        "AND TRIM(Nombre_ts) != 'null' " +
                        "AND Fecha IS NOT NULL " +
                        "AND MONTH(Fecha) = ? AND YEAR(Fecha) = ? " +
                        "GROUP BY TRIM(Nombre_ts)";
                break;

            case "Notas médicas":
                // Buscar en tb_notas.Medico
                sql = "SELECT TRIM(Medico) as nombre, COUNT(*) as cantidad " +
                        "FROM tb_notas " +
                        "WHERE Medico IS NOT NULL AND TRIM(Medico) != '' " +
                        "AND TRIM(Medico) != 'null' " +
                        "AND Fecha IS NOT NULL " +
                        "AND MONTH(Fecha) = ? AND YEAR(Fecha) = ? " +
                        "GROUP BY TRIM(Medico)";
                break;

            case "Interconsultas":
                // Buscar en tb_inter.Medico
                sql = "SELECT TRIM(Medico) as nombre, COUNT(*) as cantidad " +
                        "FROM tb_inter " +
                        "WHERE Medico IS NOT NULL AND TRIM(Medico) != '' " +
                        "AND TRIM(Medico) != 'null' " +
                        "AND Fecha IS NOT NULL " +
                        "AND MONTH(Fecha) = ? AND YEAR(Fecha) = ? " +
                        "GROUP BY TRIM(Medico)";
                break;

            case "Egresos":
                // Buscar en tb_urgencias.Nom_medalta
                sql = "SELECT TRIM(Nom_medalta) as nombre, COUNT(*) as cantidad " +
                        "FROM tb_urgencias " +
                        "WHERE Nom_medalta IS NOT NULL AND TRIM(Nom_medalta) != '' " +
                        "AND TRIM(Nom_medalta) != 'null' " +
                        "AND Estado_pac = 3 " +  // SOLO EGRESADOS
                        "AND Fecha_alta IS NOT NULL " +
                        "AND MONTH(Fecha_alta) = ? AND YEAR(Fecha_alta) = ? " +
                        "GROUP BY TRIM(Nom_medalta)";
                break;
        }

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, numeroMes);
            pstmt.setInt(2, año);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nombre = rs.getString("nombre");
                    int cantidad = rs.getInt("cantidad");
                    if (nombre != null && !nombre.trim().isEmpty()) {
                        estadisticas.put(nombre.trim(), cantidad);
                    }
                }
            }
        }

        return estadisticas;
    }


    // nuevos no mover


    // MÉTODOS PARA PRODUCTIVIDAD DIARIA
    @FXML
    private void handleVisualizarDiaria() {
        String fecha = txtFechaDiaria.getText().trim();
        if (fecha.isEmpty()) {
            mostrarAlerta("Error", "Por favor ingrese una fecha", Alert.AlertType.WARNING);
            return;
        }

        if (!validarFormatoFecha(fecha)) {
            mostrarAlerta("Error", "Formato de fecha inválido. Use dd/mm/aaaa", Alert.AlertType.ERROR);
            return;
        }

        System.out.println("Generando vista previa para fecha: " + fecha);

        // Generar PDF en memoria
        InputStream pdfStream = PDFGeneratorDiario.generarPDFDiario(fecha, false);

        if (pdfStream != null) {
            // Abrir el PDF para visualización
            boolean abierto = PDFGeneratorDiario.abrirPDF(pdfStream);
            if (abierto) {
                mostrarAlerta("Éxito",
                        "PDF generado y abierto exitosamente.",
                        Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("Error",
                        "No se pudo abrir el PDF automáticamente.",
                        Alert.AlertType.WARNING);
            }
        } else {
            mostrarAlerta("Sin datos",
                    "No hay egresos para la fecha seleccionada.",
                    Alert.AlertType.WARNING);
        }
    }


    // MÉTODOS AUXILIARES
    private int obtenerNumeroMes(String mes) {
        switch (mes.toLowerCase()) {
            case "enero": return 1;
            case "febrero": return 2;
            case "marzo": return 3;
            case "abril": return 4;
            case "mayo": return 5;
            case "junio": return 6;
            case "julio": return 7;
            case "agosto": return 8;
            case "septiembre": return 9;
            case "octubre": return 10;
            case "noviembre": return 11;
            case "diciembre": return 12;
            default: return 1;
        }
    }

    private boolean validarSeleccionMensual() {
        if (cbMes.getValue() == null) {
            mostrarAlerta("Error", "Seleccione un mes", Alert.AlertType.WARNING);
            return false;
        }
        if (cbAnio.getValue() == null) {
            mostrarAlerta("Error", "Seleccione un año", Alert.AlertType.WARNING);
            return false;
        }
        if (cbTipoProductividad.getValue() == null) {
            mostrarAlerta("Error", "Seleccione un tipo de productividad", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    @FXML
    private void handleLimpiar() {
        txtFechaDiaria.clear();
        cbMes.setValue(null);
        cbAnio.setValue(Year.now().getValue());
        cbTipoProductividad.setValue(null);
        panelEstadisticas.setVisible(false);
        foliosDelDiaActual.clear();
    }

    @FXML
    private void handleCerrar() {
        Stage stage = (Stage) txtFechaDiaria.getScene().getWindow();
        stage.close();
    }

    private boolean validarFormatoFecha(String fecha) {
        return fecha.matches("\\d{2}/\\d{2}/\\d{4}");
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}