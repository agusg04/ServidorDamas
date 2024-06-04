package damas.dao;

import damas.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public boolean comprobarUsuarioBD(String nombreUsuario, String contrasenia) {
        try (Connection connection = ConexionBD.obtenerConexion()) {

            String sql = "SELECT nombre, contrasenia FROM usuarios WHERE nombre = ? AND contrasenia = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, nombreUsuario.trim());
                preparedStatement.setString(2, contrasenia.trim());

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        String nombreUsu = resultSet.getString("nombre");
                        String contraseniaUsu = resultSet.getString("contrasenia");

                        System.out.println("Nombre : " + nombreUsu + " Contrasenia : " + contraseniaUsu);

                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al consultar el usuario: " + ex.getMessage());
        }

        return false;
    }

    @Override
    public boolean comprobarSoloNombreBD(String nombreUsuario) {
        try (Connection connection = ConexionBD.obtenerConexion()) {

            String sql = "SELECT nombre FROM usuarios WHERE nombre = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, nombreUsuario.trim());

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        String nombreUsu = resultSet.getString("nombre");

                        System.out.println("Nombre : " + nombreUsu);

                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al consultar solo el nombre: " + ex.getMessage());
        }

        return false;
    }

    @Override
    public boolean registrarUsuarioBD(String nombreUsuario, String contrasenia) {
        if (!comprobarSoloNombreBD(nombreUsuario)) {

            try (Connection connection = ConexionBD.obtenerConexion()) {

                String sql = "INSERT INTO usuarios (nombre, contrasenia) VALUES (?, ?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setString(1, nombreUsuario.trim());
                    preparedStatement.setString(2, contrasenia.trim());

                    int rowsAffected = preparedStatement.executeUpdate();

                    if (rowsAffected == 1) {
                        // La inserción fue exitosa
                        System.out.println("Operación de inserción exitosa.");
                        connection.commit();
                        return true;
                    } else {
                        // La inserción no tuvo efecto (ninguna fila afectada)
                        System.out.println("Error en la operación de inserción.");
                        return false;
                    }
                }

            } catch (SQLException e) {
                System.err.println("Error al registrar el usuario: " + e.getMessage());
            }

        } else {
            System.out.println("Error: Usuario ya registrado");
            return false;
        }

        return false;
    }

    @Override
    public int devolverIdUsuario(String nombreUsuario) {
        try (Connection connection = ConexionBD.obtenerConexion()) {
            String sql = "SELECT id_usuario FROM usuarios WHERE nombre = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, nombreUsuario.trim());
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("id_usuario");
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al consultar el ID del usuario: " + ex.getMessage());
        }
        return -1;
    }


}
