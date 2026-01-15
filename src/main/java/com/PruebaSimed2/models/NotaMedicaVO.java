// src/main/java/com/PruebaSimed2/models/NotaMedicaVO.java

package com.PruebaSimed2.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class NotaMedicaVO {
    private int idNota;
    private int folioPaciente;
    private int numeroNota;
    private String nota;
    private String indicaciones;
    private String medicoAutor;
    private String cedulaMedico;
    private LocalDateTime fechaCreacion;
    private String estado;
    private String estadoPaciente;
    private boolean editablePorMedico;
    private String permisoEdicionOtorgadoPor;
    private LocalDateTime fechaPermisoEdicion;
    private LocalDateTime fechaEdicionRealizada;
    private String mujerEdadFertil;

    // Constructor completo
    public NotaMedicaVO(int idNota, int folioPaciente, int numeroNota, String nota,
                        String medicoAutor, String cedulaMedico, LocalDateTime fechaCreacion,
                        String estado, String estadoPaciente, boolean editablePorMedico,
                        String permisoEdicionOtorgadoPor, LocalDateTime fechaPermisoEdicion) {
        this.idNota = idNota;
        this.folioPaciente = folioPaciente;
        this.numeroNota = numeroNota;
        this.nota = nota;
        this.medicoAutor = medicoAutor;
        this.cedulaMedico = cedulaMedico;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.estadoPaciente = estadoPaciente;
        this.editablePorMedico = editablePorMedico;
        this.permisoEdicionOtorgadoPor = permisoEdicionOtorgadoPor;
        this.fechaPermisoEdicion = fechaPermisoEdicion;
    }

    // Constructor vacío
    public NotaMedicaVO() {
    }

    // Getters y Setters

    // Métodos de compatibilidad - para que funcione con el código existente
    public String getContenido() {
        return this.nota;
    }

    public void setContenido(String contenido) {
        this.nota = contenido;
    }

    // Método toString para debugging
    @Override
    public String toString() {
        return "NotaMedicaVO{" +
                "idNota=" + idNota +
                ", folioPaciente=" + folioPaciente +
                ", numeroNota=" + numeroNota +
                ", medicoAutor='" + medicoAutor + '\'' +
                ", estadoPaciente='" + estadoPaciente + '\'' +
                ", editablePorMedico=" + editablePorMedico +
                '}';
    }

    // Agrega este método en NotaMedicaVO
    public String getEditable() {
        // "Sí" si es temporal o si tiene permiso de edición, "No" en otro caso
        if ("TEMPORAL".equals(this.estado) || this.editablePorMedico) {
            return "Sí";
        }
        return "No";
    }
}