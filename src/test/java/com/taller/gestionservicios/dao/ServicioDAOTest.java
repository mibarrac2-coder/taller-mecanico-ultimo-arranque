
package com.taller.gestionservicios.dao;

import com.taller.gestionservicios.modelo.Servicio;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ServicioDAOTest {

    @Autowired
    private ServicioDAO servicioDAO;

    @Test
    void debeConsultarServiciosEnMySQL() throws SQLException {
        List<Servicio> servicios = servicioDAO.listar();

        assertNotNull(servicios);
        assertTrue(servicios.size() >= 5,
                "Se esperaban al menos 5 servicios registrados");

        for (Servicio servicio : servicios) {
            System.out.println(
                    servicio.getIdServicio() + " | "
                    + servicio.getDescripcion() + " | Q"
                    + servicio.getCostoManoObra()
            );
        }
    }
}
