// src/main/java/com/PruebaSimed2/models/Edad.java

package com.PruebaSimed2.models;

import lombok.*;

import java.time.LocalDate;
import java.time.Period;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Edad {
    private int dias;
    private int meses;
    private int anos;

    public void calcularEdad(LocalDate fechaNacimiento) {
        LocalDate fechaActual = LocalDate.now();
        Period periodo = Period.between(fechaNacimiento, fechaActual);
        this.anos = periodo.getYears();
        this.meses = periodo.getMonths();
        this.dias = periodo.getDays();
    }
}
