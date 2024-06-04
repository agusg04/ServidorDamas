package damas.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String NOMBREBDD = "damas";
    private static final String USUARIO = "damas";
    private static final String CONTRASENA = "damas";
    private static final String URL = "jdbc:mysql://localhost:3306/";


    public static Connection obtenerConexion() {
        try {
            return DriverManager.getConnection(URL + NOMBREBDD, USUARIO, CONTRASENA);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
