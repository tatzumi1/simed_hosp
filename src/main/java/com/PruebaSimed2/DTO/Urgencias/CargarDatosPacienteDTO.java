package com.PruebaSimed2.DTO.Urgencias;

import lombok.*;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CargarDatosPacienteDTO {
    private int folio;
    private int cve_ts;
    private String nombre_ts;
    private String turno;
    private Timestamp fechaTurno;
    private Timestamp fecha;
    private Time horaRegistro;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombre;
    private int edad;
    private int cve_edad;
    private Date fNac;
    private int sexo;
    private String edoCivil;
    private String ocupacion;
    private String telefono;
    private String entidadResid;
    private String municipioResid;
    private String domicilio;
    private int derechoHabiencia;
    private String noAfiliacion;
    private String triage;
    private Timestamp fechaAtencion;
    private Time horaAtencion;
    private String referencia;
    private boolean reingreso;
    private String sintomas;
    private int cveMed;
    private String nombreMedico;
    private int estadoPac;
    private String ingreso;
    private String religion;
    private boolean hospitalizado;
    private String expediente;
    private String observacionesTs;
    private boolean tsDatosCompletos;
    private String curp;
    private int tipoUrgencia;
    private int motivoUrgencia;
    private int tipoCama;
    private int numNotas;
    private int numNotasInter;
    private String afecc01;
    private String afecc02;
    private String procedimiento01;
    private String procedimiento02;
    private String procedimiento03;
    private String interCons01;
    private String interCons02;
    private String interCons03;
    private String medicamentos01;
    private String medicamentos02;
    private String medicamentos03;
    private String ira;
    private String eda;
    private int sobresRep;
    private int altaPor;
    private String folioDefuncion;
    private String aHosp;
    private Timestamp fechaAlta;
    private Time horaAlta;
    private int cveMedAlta;
    private String nomMedAlta;
    private boolean impr03;
    private int notasVerif;
    private Timestamp fechaCreacion;
    private Timestamp fechaActualizacion;
    private String entidadCompleta;
    private String municipioCompleto;
    private String nombreMunicipio;
    private String nombreEntidad;
    private String nombreDerechoHabiencia;
}
