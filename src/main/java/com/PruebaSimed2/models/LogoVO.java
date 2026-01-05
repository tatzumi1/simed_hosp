// Ruta: src/main/java/com/PruebaSimed2/models/LogoVO.java

package com.PruebaSimed2.models;
import javafx.beans.property.*;

public class LogoVO {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nombreLogo = new SimpleStringProperty();
    private final IntegerProperty orden = new SimpleIntegerProperty();
    private final BooleanProperty activo = new SimpleBooleanProperty();

    public LogoVO(int id, String nombreLogo, int orden, boolean activo) {
        this.id.set(id);
        this.nombreLogo.set(nombreLogo);
        this.orden.set(orden);
        this.activo.set(activo);
    }

    public int getId() { return id.get(); }
    public String getNombreLogo() { return nombreLogo.get(); }
    public int getOrden() { return orden.get(); }
    public boolean isActivo() { return activo.get(); }

    public IntegerProperty ordenProperty() { return orden; }
    public BooleanProperty activoProperty() { return activo; }
    public void setOrden(int orden) { this.orden.set(orden); }
    public void setActivo(boolean activo) { this.activo.set(activo); }
}