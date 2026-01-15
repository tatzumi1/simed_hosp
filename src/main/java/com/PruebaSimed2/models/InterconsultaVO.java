// src/main/java/com/PruebaSimed2/models/InterconsultaVO.java

package com.PruebaSimed2.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class InterconsultaVO {
    private int idInterconsulta;
    private int folioPaciente;
    private int numeroInterconsulta;
    private String contenido;
    private String especialista;
    private String cedulaEspecialista;
    private LocalDateTime fechaCreacion;
    private String estado;
    private String estadoPaciente;
    private boolean editablePorMedico;
    private String permisoEdicionOtorgadoPor;
    private LocalDateTime fechaPermisoEdicion;
    private LocalDateTime fechaEdicionRealizada;
    // Getters y Setters (usa Alt+Insert)
    private String mujerEdadFertil;


    // Constructores, Getters y Setters (similar a NotaMedicaVO)
    public InterconsultaVO(int idInterconsulta, int folioPaciente, int numeroInterconsulta, String contenido,
                           String especialista, String cedulaEspecialista, LocalDateTime fechaCreacion,
                           String estado, String estadoPaciente, boolean editablePorMedico,
                           String permisoEdicionOtorgadoPor, LocalDateTime fechaPermisoEdicion) {
        this.idInterconsulta = idInterconsulta;
        this.folioPaciente = folioPaciente;
        this.numeroInterconsulta = numeroInterconsulta;
        this.contenido = contenido;
        this.especialista = especialista;
        this.cedulaEspecialista = cedulaEspecialista;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.estadoPaciente = estadoPaciente;
        this.editablePorMedico = editablePorMedico;
        this.permisoEdicionOtorgadoPor = permisoEdicionOtorgadoPor;
        this.fechaPermisoEdicion = fechaPermisoEdicion;
    }

    public InterconsultaVO() {
    }


    // Agrega este método en InterconsultaVO
    public String getEditable() {
        // "Sí" si es temporal o si tiene permiso de edición, "No" en otro caso
        if ("TEMPORAL".equals(this.estado) || this.editablePorMedico) {
            return "Sí";
        }
        return "No";
    }

}


