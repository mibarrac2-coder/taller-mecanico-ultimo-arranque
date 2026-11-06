
package com.taller.gestionservicios.controlador;

import com.taller.gestionservicios.dao.ServicioDAO;
import com.taller.gestionservicios.modelo.Servicio;
import java.math.BigDecimal;
import java.sql.SQLException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/servicios")
public class ServicioControlador {

    private final ServicioDAO servicioDAO;

    public ServicioControlador(ServicioDAO servicioDAO) {
        this.servicioDAO = servicioDAO;
    }

    @GetMapping
    public String listarServicios(Model model)
            throws SQLException {

        model.addAttribute("servicios", servicioDAO.listar());
        return "servicios";
    }

    @PostMapping("/guardar")
    public String guardarServicio(
            @RequestParam String descripcion,
            @RequestParam BigDecimal costoManoObra,
            RedirectAttributes redirectAttributes)
            throws SQLException {

        try {
            Servicio servicio = new Servicio(
                    null, descripcion, costoManoObra);

            servicioDAO.insertar(servicio);

            redirectAttributes.addFlashAttribute(
                    "mensaje", "Servicio registrado correctamente");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        return "redirect:/servicios";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(
            @PathVariable int id,
            Model model,
            RedirectAttributes redirectAttributes)
            throws SQLException {

        Servicio servicio = servicioDAO.buscarPorId(id);

        if (servicio == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "El servicio solicitado no existe");

            return "redirect:/servicios";
        }

        model.addAttribute("servicio", servicio);
        return "editar-servicio";
    }

    @PostMapping("/actualizar")
    public String actualizarServicio(
            @RequestParam int idServicio,
            @RequestParam String descripcion,
            @RequestParam BigDecimal costoManoObra,
            RedirectAttributes redirectAttributes)
            throws SQLException {

        try {
            Servicio servicio = new Servicio(
                    idServicio, descripcion, costoManoObra);

            boolean actualizado = servicioDAO.actualizar(servicio);

            if (actualizado) {
                redirectAttributes.addFlashAttribute(
                        "mensaje", "Servicio actualizado correctamente");
            } else {
                redirectAttributes.addFlashAttribute(
                        "error", "No se encontro el servicio");
            }

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        return "redirect:/servicios";
    }

    @PostMapping("/eliminar")
    public String eliminarServicio(
            @RequestParam int idServicio,
            RedirectAttributes redirectAttributes) {

        try {
            if (idServicio <= 0) {
                throw new IllegalArgumentException(
                        "El identificador no es valido");
            }

            boolean eliminado = servicioDAO.eliminar(idServicio);

            if (eliminado) {
                redirectAttributes.addFlashAttribute(
                        "mensaje", "Servicio eliminado correctamente");
            } else {
                redirectAttributes.addFlashAttribute(
                        "error", "El servicio no existe");
            }

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());

        } catch (SQLException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el servicio. "
                    + "Verifique si esta asociado a otros registros.");
        }

        return "redirect:/servicios";
    }
}
