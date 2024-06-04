package damas.dao;

import damas.modelo.MovimientosPartida;

import java.util.ArrayList;

public interface PartidasDAO {

    ArrayList<MovimientosPartida> devolverPartidasMiTurnoBD(int idUsuario);
    ArrayList<MovimientosPartida> devolverPartidasNoMiTurnoBD(int idUsuario);
    ArrayList<MovimientosPartida> devolverPartidasTerminadasBD(int idUsuario);
    int crearPartidaBD(int idUsuarioDesafiado, int idUsuarioDesafiador, int tamanio);
    void rendirseEnPartidaBD(int idPartida);
    void insertarMovimientoBD(int idPartida, int idUsuario, int posXini, int posYini, int posXfin, int posYfin);
    boolean comprobarTurno(int idPartida, int idUsuario);
}
