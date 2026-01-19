package com.PruebaSimed2.DTO.Urgencias;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ActualizarCapturaPrincipalDTO {
    private int cveUrg;
    private int cveMotatn;
    private int cveCama;
    private int cveMedico;
    private String medico;
    private int folioPaciente;
}
