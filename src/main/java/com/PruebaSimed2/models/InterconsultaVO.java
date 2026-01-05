// src/main/java/com/PruebaSimed2/models/InterconsultaVO.java

package com.PruebaSimed2.models;

import java.time.LocalDateTime;

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

    public InterconsultaVO() {}

    // Getters y Setters (usa Alt+Insert)
    public String getMujerEdadFertil() { return mujerEdadFertil; }
    public void setMujerEdadFertil(String mujerEdadFertil) { this.mujerEdadFertil = mujerEdadFertil; }

    public int getIdInterconsulta() { return idInterconsulta; }
    public void setIdInterconsulta(int idInterconsulta) { this.idInterconsulta = idInterconsulta; }

    public int getFolioPaciente() { return folioPaciente; }
    public void setFolioPaciente(int folioPaciente) { this.folioPaciente = folioPaciente; }

    public int getNumeroInterconsulta() { return numeroInterconsulta; }
    public void setNumeroInterconsulta(int numeroInterconsulta) { this.numeroInterconsulta = numeroInterconsulta; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getEspecialista() { return especialista; }
    public void setEspecialista(String especialista) { this.especialista = especialista; }

    public String getCedulaEspecialista() { return cedulaEspecialista; }
    public void setCedulaEspecialista(String cedulaEspecialista) { this.cedulaEspecialista = cedulaEspecialista; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEstadoPaciente() { return estadoPaciente; }
    public void setEstadoPaciente(String estadoPaciente) { this.estadoPaciente = estadoPaciente; }

    public boolean isEditablePorMedico() { return editablePorMedico; }
    public void setEditablePorMedico(boolean editablePorMedico) { this.editablePorMedico = editablePorMedico; }

    public String getPermisoEdicionOtorgadoPor() { return permisoEdicionOtorgadoPor; }
    public void setPermisoEdicionOtorgadoPor(String permisoEdicionOtorgadoPor) { this.permisoEdicionOtorgadoPor = permisoEdicionOtorgadoPor; }

    public LocalDateTime getFechaPermisoEdicion() { return fechaPermisoEdicion; }
    public void setFechaPermisoEdicion(LocalDateTime fechaPermisoEdicion) { this.fechaPermisoEdicion = fechaPermisoEdicion; }

    public LocalDateTime getFechaEdicionRealizada() { return fechaEdicionRealizada; }
    public void setFechaEdicionRealizada(LocalDateTime fechaEdicionRealizada) { this.fechaEdicionRealizada = fechaEdicionRealizada; }


    // Agrega este método en InterconsultaVO
    public String getEditable() {
        // "Sí" si es temporal o si tiene permiso de edición, "No" en otro caso
        if ("TEMPORAL".equals(this.estado) || this.editablePorMedico) {
            return "Sí";
        }
        return "No";
    }

}


