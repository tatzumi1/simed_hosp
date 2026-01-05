// Ruta: src/main/java/com/tuempresa/simed2/controllers/ConfigurarLogosController.java
package com.PruebaSimed2.controllers;

import com.PruebaSimed2.database.ConexionBD;
import com.PruebaSimed2.models.LogoVO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.io.FileInputStream;
import java.sql.*;

public class ConfigurarLogosController {

    @FXML private TableView<LogoVO> tablaLogos;

    private final ObservableList<LogoVO> lista = FXCollections.observableArrayList();

    @FXML private void initialize() {
        configurarTabla();
        cargarLogos();
    }

    private void configurarTabla() {
        tablaLogos.setEditable(true);

        TableColumn<LogoVO, Integer> colOrden = (TableColumn<LogoVO, Integer>) tablaLogos.getColumns().get(0);
        colOrden.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        TableColumn<LogoVO, Boolean> colActivo = (TableColumn<LogoVO, Boolean>) tablaLogos.getColumns().get(3);
        colActivo.setCellValueFactory(cellData -> cellData.getValue().activoProperty());
        colActivo.setCellFactory(CheckBoxTableCell.forTableColumn(colActivo));

        // Botón seleccionar imagen
        TableColumn<LogoVO, Void> colImagen = (TableColumn<LogoVO, Void>) tablaLogos.getColumns().get(2);
        colImagen.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Seleccionar imagen");
            {
                btn.setOnAction(e -> {
                    LogoVO logo = getTableView().getItems().get(getIndex());
                    subirImagen(logo);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void cargarLogos() {
        lista.clear();
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, nombre_logo, orden, activo FROM tb_logos_encabezado ORDER BY orden");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new LogoVO(
                        rs.getInt("id"),
                        rs.getString("nombre_logo"),
                        rs.getInt("orden"),
                        rs.getInt("activo") == 1
                ));
            }
            tablaLogos.setItems(lista);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void subirImagen(LogoVO logo) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File archivo = fc.showOpenDialog(tablaLogos.getScene().getWindow());
        if (archivo != null) {
            try (FileInputStream fis = new FileInputStream(archivo);
                 Connection c = ConexionBD.conectar();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE tb_logos_encabezado SET imagen = ? WHERE id = ?")) {

                ps.setBinaryStream(1, fis, archivo.length());
                ps.setInt(2, logo.getId());
                ps.executeUpdate();

                new Alert(Alert.AlertType.INFORMATION, "Logo actualizado correctamente").show();

            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    @FXML private void guardarYCerrar() {
        try (Connection c = ConexionBD.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE tb_logos_encabezado SET orden = ?, activo = ? WHERE id = ?")) {

            for (LogoVO l : lista) {
                ps.setInt(1, l.getOrden());
                ps.setInt(2, l.isActivo() ? 1 : 0);
                ps.setInt(3, l.getId());
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (Exception e) { e.printStackTrace(); }

        ((Stage) tablaLogos.getScene().getWindow()).close();
    }
}