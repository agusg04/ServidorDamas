package damas.datos;

import modelo.*;


import java.util.ArrayList;

public class DatosUsuario {

    private int idUsuario;
    private String nombre;
    private String contrasenia;
    private ArrayList<Tablero> partidasMiTurno;
    private ArrayList<Tablero> partidasNoMiTurno;
    private ArrayList<Partida> partidasTermiandas;

    public DatosUsuario(int idUsuario, String nombre, String contrasenia, ArrayList<Tablero> partidasMiTurno, ArrayList<Tablero> partidasNoMiTurno, ArrayList<Partida> partidasTermiandas) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.contrasenia = contrasenia;
        this.partidasMiTurno = partidasMiTurno;
        this.partidasNoMiTurno = partidasNoMiTurno;
        this.partidasTermiandas = partidasTermiandas;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    public ArrayList<Tablero> getPartidasMiTurno() {
        return partidasMiTurno;
    }

    public void setPartidasMiTurno(ArrayList<Tablero> partidasMiTurno) {
        this.partidasMiTurno = partidasMiTurno;
    }

    public ArrayList<Tablero> getPartidasNoMiTurno() {
        return partidasNoMiTurno;
    }

    public void setPartidasNoMiTurno(ArrayList<Tablero> partidasNoMiTurno) {
        this.partidasNoMiTurno = partidasNoMiTurno;
    }

    public ArrayList<Partida> getPartidasTermiandas() {
        return partidasTermiandas;
    }

    public void setPartidasTermiandas(ArrayList<Partida> partidasTermiandas) {
        this.partidasTermiandas = partidasTermiandas;
    }

    public Tablero buscarTableroMiTurno(int idPartida) {
        for (Tablero tablero : partidasNoMiTurno) {
            if (tablero.getId() == idPartida) {
                return tablero;
            }
        }
        return null;
    }
}
