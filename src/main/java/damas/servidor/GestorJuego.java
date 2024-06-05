package damas.servidor;

import damas.dao.PartidasDAOImpl;
import damas.dao.UsuarioDAOImpl;
import damas.modelo.*;

import java.util.ArrayList;
import java.util.HashMap;

public class GestorJuego {

    HashMap<Integer, ConexionCliente> usuariosConectados = new HashMap<>();
    UsuarioDAOImpl usuarioDAO = new UsuarioDAOImpl();
    PartidasDAOImpl partidasDAO = new PartidasDAOImpl();

    public boolean comprobarUsuario(String nombreUsuario, String contrasenia) {
        return usuarioDAO.comprobarUsuarioBD(nombreUsuario, contrasenia);
    }

    public boolean registrarUsuario(String nombreUsuario, String contrasenia) {
        return usuarioDAO.registrarUsuarioBD(nombreUsuario, contrasenia);
    }

    public int devolverIdUsuario(String nombre) {
        return usuarioDAO.devolverIdUsuario(nombre);
    }

    public boolean aniadirUsuarioConectado(int idUsuario, ConexionCliente cliente) {
        if (!usuariosConectados.containsKey(idUsuario)) {
            usuariosConectados.put(idUsuario, cliente);
            return true;
        }
        return false;
    }

    public void eliminarUsuarioConectado(ConexionCliente cliente) {
        usuariosConectados.remove(cliente.getIdUsuario());
    }


    public ArrayList<Tablero> devolverPartidasMiTurno(int id) {
        //Aquí recoger los movimientos que nos da partidasDAO y reproducirlos en un Tablero,
        //de esta forma conseguiremos el último tablero de la partida
        //------------------------------------------------------------
        //Reproducir los movimientos solo hay que mirar en los movimientos en que una ficha coma a otra
        //quitar esa ficha no comprobar si son legales los movimientos o si se salen del tablero
        return obtenerPartidas(id, true);
    }


    public ArrayList<Tablero> devolverPartidasNoMiTurno(int id) {
        //Aquí recoger los movimientos que nos da partidasDAO y reproducirlos en un Tablero,
        //de esta forma conseguiremos el último tablero de la partida
        return obtenerPartidas(id, false);
    }

    private ArrayList<Tablero> obtenerPartidas(int id, boolean miTurno) {
        ArrayList<Tablero> partidas = new ArrayList<>();
        ArrayList<MovimientosPartida> movimientosPartidas = miTurno ? partidasDAO.devolverPartidasMiTurnoBD(id) : partidasDAO.devolverPartidasNoMiTurnoBD(id);

        for (MovimientosPartida movimientosUnaPartida : movimientosPartidas) {
            // Crear un nuevo tablero e inicializarlo
            Tablero tablero = new Tablero(movimientosUnaPartida.getId(), movimientosUnaPartida.getTamanio());
            tablero.inicializarTablero();

            // Reproducir los movimientos en el tablero
            for (Movimiento movimiento : movimientosUnaPartida.getMovimientos()) {

                reproducirMovimiento(tablero, movimiento);
            }
            partidas.add(tablero);
        }
        return partidas;
    }

    private void reproducirMovimiento(Tablero tablero, Movimiento movimiento) {
        int origenX = movimiento.getPosIniX();
        int origenY = movimiento.getPosIniY();
        int destinoX = movimiento.getPosFinX();
        int destinoY = movimiento.getPosFinY();

        // Mover la ficha en el tablero
        tablero.moverFicha(origenX, origenY, destinoX, destinoY);

        // Eliminar las fichas que haya podido comerse en ese movimiento
        // Recorre las casillas intermedias y eliminar las fichas que pudiera haber
        int sentidoX = destinoX > origenX ? 1 : -1;
        int sentidoY = destinoY > origenY ? 1 : -1;

        int x = origenX + sentidoX;
        int y = origenY + sentidoY;

        while (x != destinoX || y != destinoY) {
            tablero.eliminarFicha(x, y);
            x += sentidoX;
            y += sentidoY;
        }
    }


    public ArrayList<Partida> devolverPartidasTerminadas(int idUsuario) {
        //Aqui recoger los movimientos que nos da partidasDAO y crear un nuevo tablero por movimiento
        //asi conseguir un array de Tableros que será la partida
        ArrayList<Partida> partidasTerminadas = new ArrayList<>();
        ArrayList<MovimientosPartida> movimientosPartidasTerminadas = partidasDAO.devolverPartidasTerminadasBD(idUsuario);

        for (MovimientosPartida movimientosUnaPartida : movimientosPartidasTerminadas) {
            Partida partida = new Partida(movimientosUnaPartida.getId(), movimientosUnaPartida.getTamanio());
            Tablero tablero = new Tablero(partida.getId(), partida.getTamanio());
            tablero.inicializarTablero();

            partida.getMovimientos().add(tablero); // Agrega el estado inicial del tablero

            for (Movimiento movimiento : movimientosUnaPartida.getMovimientos()) {

                reproducirMovimiento(tablero, movimiento);

                partida.getMovimientos().add(new Tablero(tablero)); // Agrega el estado del tablero después de cada movimiento
            }

            partidasTerminadas.add(partida);
        }

        return partidasTerminadas;

    }

    public int empezarPartida(int idUsuarioDesafiado, int idUsuarioDesafiador, int tamanio) {
        return partidasDAO.crearPartidaBD(idUsuarioDesafiado, idUsuarioDesafiador, tamanio);
    }

    public void rendirseEnPartida(int idPartida, int idUsuario) {
        if (partidasDAO.comprobarTurno(idPartida, idUsuario)) {
            partidasDAO.rendirseEnPartidaBD(idPartida);
        }
    }

    public void moverFicha(int idPartida, int idUsuario, Tablero tablero, int posXini, int posYini, int posXfin, int posYfin) {
        if (partidasDAO.comprobarTurno(idPartida, idUsuario) && comprobarMovimientoLegal(tablero, posXini, posYini, posXfin, posYfin)) {
            partidasDAO.insertarMovimientoBD(idPartida, idUsuario, posXini, posYini, posXfin, posYfin);
        }

    }

    private boolean comprobarMovimientoLegal(Tablero tablero, int posXini, int posYini, int posXfin, int posYfin) {
        int tamanio = tablero.getTamanio();
        Pieza pieza = tablero.getPieza(posXini, posYini);

        if (!hayFichaEnPosicion(pieza)) return false;
        if (!estaDentroDelTablero(posXfin, posYfin, tamanio)) return false;
        if (!casillaDestinoLibre(tablero, posXfin, posYfin)) return false;

        if (pieza.isEsDama()) {
            return esMovimientoLegalDama(tablero, posXini, posYini, posXfin, posYfin);
        } else {
            return esMovimientoLegalPiezaNormal(tablero, pieza, posXini, posYini, posXfin, posYfin, tamanio);
        }
    }

    private boolean hayFichaEnPosicion(Pieza pieza) {
        return pieza != null;
    }

    private boolean estaDentroDelTablero(int posX, int posY, int tamanio) {
        return posX >= 0 && posX < tamanio && posY >= 0 && posY < tamanio;
    }

    private boolean casillaDestinoLibre(Tablero tablero, int posXfin, int posYfin) {
        return tablero.getPieza(posXfin, posYfin) == null;
    }

    private boolean esMovimientoLegalDama(Tablero tablero, int posXini, int posYini, int posXfin, int posYfin) {
        if (Math.abs(posXfin - posXini) != Math.abs(posYfin - posYini)) return false;

        int sentidoX = posXfin > posXini ? 1 : -1;
        int sentidoY = posYfin > posYini ? 1 : -1;

        int x = posXini + sentidoX;
        int y = posYini + sentidoY;

        while (x != posXfin || y != posYfin) {
            if (tablero.getPieza(x, y) != null) return false;
            x += sentidoX;
            y += sentidoY;
        }

        return true;
    }

    private boolean esMovimientoLegalPiezaNormal(Tablero tablero, Pieza pieza, int posXini, int posYini, int posXfin, int posYfin, int tamanio) {
        if (Math.abs(posXfin - posXini) != 1) return false;

        int desplazamientoY = posYfin - posYini;
        if (!esMovimientoAdelante(pieza, desplazamientoY)) return false;

        if (tablero.getPieza(posXfin, posYfin) != null) return false;

        if ((posYfin == tamanio - 1  && pieza.getColor().equals(ColorPieza.BLANCA) || posYfin == 0 && pieza.getColor().equals(ColorPieza.NEGRA))) {
            pieza.setEsDama(true);
        }

        return true;
    }

    private boolean esMovimientoAdelante(Pieza pieza, int desplazamientoY) {
        return (pieza.getColor().equals(ColorPieza.BLANCA) && desplazamientoY == 1) ||
                (pieza.getColor().equals(ColorPieza.NEGRA) && desplazamientoY == -1);
    }


    /*
    public boolean capturarFicha(int idPartida, int idUsuario, Tablero tablero, int posXini, int posYini, int posXfin, int posYfin) {
        if (partidasDAO.comprobarTurno(idPartida, idUsuario) && comprobarCapturaLegal(tablero, posXini, posYini, posXfin, posYfin)) {
            partidasDAO.insertarMovimientoBD(idPartida, idUsuario, posXini, posYini, posXfin, posYfin);
            return comprobarVictoria(tablero, tablero.getPieza(posXfin, posYfin).getColor());
        }
        return false;
    }

     */


}
