package com.PruebaSimed2.DTO;

import lombok.*;

import java.sql.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
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
}
