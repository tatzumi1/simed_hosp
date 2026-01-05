// src/main/java/com/PruebaSimed2/models/NotaMedicaVO.java

package com.PruebaSimed2.models;

import java.time.LocalDateTime;

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
    public NotaMedicaVO() {}

    // Getters y Setters

    public String getMujerEdadFertil() { return mujerEdadFertil; }
    public void setMujerEdadFertil(String mujerEdadFertil) { this.mujerEdadFertil = mujerEdadFertil; }

    public int getIdNota() {
        return idNota;
    }

    public void setIdNota(int idNota) {
        this.idNota = idNota;
    }

    public int getFolioPaciente() {
        return folioPaciente;
    }

    public void setFolioPaciente(int folioPaciente) {
        this.folioPaciente = folioPaciente;
    }

    public int getNumeroNota() {
        return numeroNota;
    }

    public void setNumeroNota(int numeroNota) {
        this.numeroNota = numeroNota;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }

    public String getMedicoAutor() {
        return medicoAutor;
    }

    public void setMedicoAutor(String medicoAutor) {
        this.medicoAutor = medicoAutor;
    }

    public String getCedulaMedico() {
        return cedulaMedico;
    }

    public void setCedulaMedico(String cedulaMedico) {
        this.cedulaMedico = cedulaMedico;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstadoPaciente() {
        return estadoPaciente;
    }

    public void setEstadoPaciente(String estadoPaciente) {
        this.estadoPaciente = estadoPaciente;
    }

    public boolean isEditablePorMedico() {
        return editablePorMedico;
    }

    public void setEditablePorMedico(boolean editablePorMedico) {
        this.editablePorMedico = editablePorMedico;
    }

    public String getPermisoEdicionOtorgadoPor() {
        return permisoEdicionOtorgadoPor;
    }

    public void setPermisoEdicionOtorgadoPor(String permisoEdicionOtorgadoPor) {
        this.permisoEdicionOtorgadoPor = permisoEdicionOtorgadoPor;
    }

    public LocalDateTime getFechaPermisoEdicion() {
        return fechaPermisoEdicion;
    }

    public void setFechaPermisoEdicion(LocalDateTime fechaPermisoEdicion) {
        this.fechaPermisoEdicion = fechaPermisoEdicion;
    }

    public LocalDateTime getFechaEdicionRealizada() {
        return fechaEdicionRealizada;
    }

    public void setFechaEdicionRealizada(LocalDateTime fechaEdicionRealizada) {
        this.fechaEdicionRealizada = fechaEdicionRealizada;
    }

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