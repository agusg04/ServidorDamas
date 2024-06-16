package damas.dao;

import modelo.*;

import java.util.ArrayList;

public interface PartidasDAO {

    ArrayList<MovimientosPartida> devolverPartidasActivasBD();
    ArrayList<MovimientosPartida> devolverPartidasTerminadasBD();
    int crearPartidaBD(int idUsuarioDesafiado, int idUsuarioDesafiador, int tamanio);
    void rendirseEnPartidaBD(int idPartida);
    void insertarMovimientoBD(int idPartida, int idUsuario, int posXini, int posYini, int posXfin, int posYfin);
    MovimientosPartida devolverPartidaBD(int idPartida);
    //boolean comprobarTurno(int idPartida, int idUsuario);
    //ColorPieza comprobarColor(int idPartida, int idUsuario);
}
