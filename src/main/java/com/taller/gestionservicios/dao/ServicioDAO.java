
package com.taller.gestionservicios.dao;

import com.taller.gestionservicios.modelo.Servicio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class ServicioDAO {

    private final DataSource dataSource;

    public ServicioDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Servicio> listar() throws SQLException {
        List<Servicio> servicios = new ArrayList<>();

        String sql = "SELECT id_servicio, descripcion, "
                + "costo_mano_obra FROM servicios "
                + "ORDER BY id_servicio";

        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Servicio servicio = new Servicio(
                    rs.getInt("id_servicio"),
                    rs.getString("descripcion"),
                    rs.getBigDecimal("costo_mano_obra")
                );

                servicios.add(servicio);
            }
        }

        return servicios;
    }

    public Servicio buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_servicio, descripcion, "
                + "costo_mano_obra FROM servicios "
                + "WHERE id_servicio = ?";

        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Servicio(
                        rs.getInt("id_servicio"),
                        rs.getString("descripcion"),
                        rs.getBigDecimal("costo_mano_obra")
                    );
                }
            }
        }

        return null;
    }

    public void insertar(Servicio servicio) throws SQLException {
        String sql = "INSERT INTO servicios "
                + "(descripcion, costo_mano_obra) VALUES (?, ?)";

        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(
                 sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, servicio.getDescripcion());
            ps.setBigDecimal(2, servicio.getCostoManoObra());

            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    servicio.setIdServicio(claves.getInt(1));
                }
            }
        }
    }

    public boolean actualizar(Servicio servicio)
            throws SQLException {

        String sql = "UPDATE servicios SET descripcion = ?, "
                + "costo_mano_obra = ? WHERE id_servicio = ?";

        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, servicio.getDescripcion());
            ps.setBigDecimal(2, servicio.getCostoManoObra());
            ps.setInt(3, servicio.getIdServicio());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM servicios WHERE id_servicio = ?";

        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;
        }
    }
}
