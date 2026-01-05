// src/main/java/com/PruebaSimed2/models/PermisoHistorialVO.java

package com.PruebaSimed2.models;

import java.time.LocalDateTime;

public class PermisoHistorialVO {
    private int idHistorial;
    private int idNota;
    private String tipoNota;
    private int folioPaciente;
    private String medicoAutor;
    private String accion;
    private String usuarioQueActua;
    private String rolUsuario;
    private LocalDateTime fechaAccion;
    private String motivo;
    private String estadoPaciente;

    // Constructor
    public PermisoHistorialVO(int idHistorial, int idNota, String tipoNota, int folioPaciente,
                              String medicoAutor, String accion, String usuarioQueActua,
                              String rolUsuario, LocalDateTime fechaAccion, String motivo,
                              String estadoPaciente) {
        this.idHistorial = idHistorial;
        this.idNota = idNota;
        this.tipoNota = tipoNota;
        this.folioPaciente = folioPaciente;
        this.medicoAutor = medicoAutor;
        this.accion = accion;
        this.usuarioQueActua = usuarioQueActua;
        this.rolUsuario = rolUsuario;
        this.fechaAccion = fechaAccion;
        this.motivo = motivo;
        this.estadoPaciente = estadoPaciente;
    }

    // Constructor vacío
    public PermisoHistorialVO() {}

    // Getters y Setters (usa Alt+Insert para generarlos rápidamente)
    public int getIdHistorial() { return idHistorial; }
    public void setIdHistorial(int idHistorial) { this.idHistorial = idHistorial; }

    public int getIdNota() { return idNota; }
    public void setIdNota(int idNota) { this.idNota = idNota; }

    public String getTipoNota() { return tipoNota; }
    public void setTipoNota(String tipoNota) { this.tipoNota = tipoNota; }

    public int getFolioPaciente() { return folioPaciente; }
    public void setFolioPaciente(int folioPaciente) { this.folioPaciente = folioPaciente; }

    public String getMedicoAutor() { return medicoAutor; }
    public void setMedicoAutor(String medicoAutor) { this.medicoAutor = medicoAutor; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getUsuarioQueActua() { return usuarioQueActua; }
    public void setUsuarioQueActua(String usuarioQueActua) { this.usuarioQueActua = usuarioQueActua; }

    public String getRolUsuario() { return rolUsuario; }
    public void setRolUsuario(String rolUsuario) { this.rolUsuario = rolUsuario; }

    public LocalDateTime getFechaAccion() { return fechaAccion; }
    public void setFechaAccion(LocalDateTime fechaAccion) { this.fechaAccion = fechaAccion; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getEstadoPaciente() { return estadoPaciente; }
    public void setEstadoPaciente(String estadoPaciente) { this.estadoPaciente = estadoPaciente; }
}