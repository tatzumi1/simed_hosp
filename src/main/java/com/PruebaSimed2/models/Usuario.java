// src/main/java/com/PruebaSimed2/models/Usuario.java
package com.PruebaSimed2.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Usuario {
    // Getters y Setters (usa Alt+Insert para generarlos)
    private int id;
    private String username;
    private String email;
    private String rol;
    private boolean activo;
    private boolean primerLogin;

    // Constructor vacío
    public Usuario() {}

    // Constructor completo
    public Usuario(int id, String username, String email, String rol, boolean activo, boolean primerLogin) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
        this.primerLogin = primerLogin;
    }

}