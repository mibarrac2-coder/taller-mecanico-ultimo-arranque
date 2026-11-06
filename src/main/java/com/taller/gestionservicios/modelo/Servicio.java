
package com.taller.gestionservicios.modelo;

import java.math.BigDecimal;

public class Servicio {

    private Integer idServicio;
    private String descripcion;
    private BigDecimal costoManoObra;

    public Servicio() {
    }

    
public Servicio(Integer idServicio, String descripcion,
                BigDecimal costoManoObra) {
    if (idServicio != null && idServicio <= 0) {
        throw new IllegalArgumentException(
            "El identificador debe ser positivo");
    }
    if (descripcion == null || descripcion.isBlank()
            || descripcion.trim().length() > 150) {
        throw new IllegalArgumentException(
            "La descripcion debe tener entre 1 y 150 caracteres");
    }
    if (costoManoObra == null
            || costoManoObra.compareTo(BigDecimal.ZERO) < 0
            || costoManoObra.scale() > 2
            || costoManoObra.compareTo(
                new BigDecimal("99999999.99")) > 0) {
        throw new IllegalArgumentException(
            "El costo de mano de obra no es valido");
    }

    this.idServicio = idServicio;
    this.descripcion = descripcion.trim();
    this.costoManoObra = costoManoObra;
}


    public Integer getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(Integer idServicio) {
        if (idServicio != null && idServicio <= 0) {
            throw new IllegalArgumentException(
                "El identificador debe ser positivo");
        }
        this.idServicio = idServicio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                "La descripcion es obligatoria");
        }
        if (descripcion.trim().length() > 150) {
            throw new IllegalArgumentException(
                "La descripcion no puede superar 150 caracteres");
        }
        this.descripcion = descripcion.trim();
    }

    public BigDecimal getCostoManoObra() {
        return costoManoObra;
    }

    public void setCostoManoObra(BigDecimal costoManoObra) {
        if (costoManoObra == null ||
            costoManoObra.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "El costo no puede ser negativo");
        }
        if (costoManoObra.scale() > 2 ||
            costoManoObra.compareTo(
                new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException(
                "El costo debe tener maximo dos decimales");
        }
        this.costoManoObra = costoManoObra;
    }
}
