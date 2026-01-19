package com.PruebaSimed2.DTO.Urgencias;

import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.sql.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class InsertarPacienteDTO {
    private int folio;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombre;
    private int edad;
    private Date fechaNacimiento;
    private String telefono;
    private String domicilio;
    private int derechoHabiencia;
    private String noAfiliacion;
    private String referencia;
    private boolean reingreso;
    private boolean hospitalizado;
    private String expediente;
    private String curp;
    private String sintomas;
    private String medico;
    private String triage;
    private String nombreMedico;
    private String turno;
    private String hora;
    private int sexo;
    private String codigoMunicipio;
    private String codigoEntidad;
    private String ocupacion;
    private String religion;
    private String estadoCivil;
    private String observaciones;
    private String municipioSeleccionado;
    private String entidadSeleccionada;

    public void validarDto() {
        if (this.triage == null) {
            this.triage = "verde";
            log.warn("Triage no proporcionando, asignando verde por defecto");
        }
        if (this.folio <= 0) {
            this.folio = 1;
            log.warn("Folio no proporcionando, asignando 1 por defecto");
        }
        if (this.nombre == null) {
            this.nombre = "No especificado";
            log.warn("Nombre no proporcionando, asignando 'No especificado'");
        }
        if (this.apellidoPaterno == null) {
            this.apellidoPaterno = "No especificado";
            log.warn("Apellido paterno no proporcionando, asignando 'No especificado'");
        }
        if (this.apellidoMaterno == null) {
            this.apellidoMaterno = "No especificado";
            log.warn("Apellido materno no proporcionando, asignando 'No especificado'");
        }
        if (this.telefono == null) {
            this.telefono = "0000000000";
            log.warn("Numero no proporcionado");
        }
        if (this.domicilio == null) {
            this.domicilio = "No especificado";
            log.warn("Domicilio no proporcionado");
        }
        if (this.noAfiliacion == null) {
            this.noAfiliacion = "No especificado";
            log.warn("NoAfiliacion no proporcionado");
        }
        if (this.referencia == null) {
            this.referencia = "No especificado";
            log.warn("Referencia no proporcionado");
        }
        if (this.expediente == null) {
            this.expediente = "No especificado";
            log.warn("Expediente no proporcionado");
        }
        if (this.curp == null) {
            this.curp = "No especificado";
            log.warn("CURP no proporcionado");
        }
        if (this.nombreMedico == null) {
            this.nombreMedico = "No especificado";
            log.warn("Nombre medico no proporcionado");
        }
        if (this.hora == null) {
            this.hora = "00:00:00";
            log.warn("Hora no proporcionado");
        }
        if (this.ocupacion == null) {
            this.ocupacion = "No especificado";
            log.warn("Ocupacion no proporcionado");
        }
        if (this.religion == null) {
            this.religion = "No especificado";
            log.warn("Religion no proporcionado");
        }
        if (this.estadoCivil == null) {
            this.estadoCivil = "No especificado";
            log.warn("Estado civil no proporcionado");
        }
        if (this.municipioSeleccionado == null) {
            this.municipioSeleccionado = "No especificado";
            log.warn("Municipio seleccionado no proporcionado");
        }
        if (this.entidadSeleccionada == null) {
            this.entidadSeleccionada = "No especificado";
            log.warn("Entidad seleccionada no proporcionada");
        }
    }
}
