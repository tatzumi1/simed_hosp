package com.PruebaSimed2.DTO.Urgencias;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CargarNuevaInformacionDTO {
    private int tipoUrg;
    private int motivoUrg;
    private int tipoCama;
    private int cveMed;
    private String nombreMedico;
    private int estadoPaciente;
}
