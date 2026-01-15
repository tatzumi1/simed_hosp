// src/main/java/com/PruebaSimed2/models/PermisoHistorialVO.java

package com.PruebaSimed2.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class PermisoHistorialVO {
    // Getters y Setters (usa Alt+Insert para generarlos rápidamente)
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
    public PermisoHistorialVO() {
    }
}