// src/main/java/com/PruebaSimed2/models/Usuario.java
package com.PruebaSimed2.models;

public class Usuario {
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

    // Getters y Setters (usa Alt+Insert para generarlos)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean isPrimerLogin() { return primerLogin; }
    public void setPrimerLogin(boolean primerLogin) { this.primerLogin = primerLogin; }
}