package com.PruebaSimed2.DTO.HistorialPermisos;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class InsertarPermisoDTO {
    private int id;
    private String columna;
    private String permiso;
    private String usuario;
    private String rol;
}
