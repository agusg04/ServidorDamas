package damas.dao;

public interface UsuarioDAO {

    boolean comprobarUsuarioBD(String nombreUsuario, String contrasenia);
    boolean comprobarSoloNombreBD(String nombreUsuario);
    boolean registrarUsuarioBD(String nombreUsuario, String contrasenia);
    int devolverIdUsuario(String nombreUsuario);


}
