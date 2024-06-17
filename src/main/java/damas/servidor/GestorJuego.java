package damas.servidor;

import damas.dao.PartidasDAOImpl;
import damas.dao.UsuarioDAOImpl;
import modelo.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GestorJuego {

    Map<Integer, ConexionCliente> usuariosConectados;
    Map<Integer, Partida> partidasActivas;
    Map<Integer, PartidaTerminada> partidasTerminadas;
    UsuarioDAOImpl usuarioDAO;
    PartidasDAOImpl partidasDAO;

    public GestorJuego() {
        this.usuariosConectados = new HashMap<>();
        this.partidasActivas = new HashMap<>();
        this.partidasTerminadas = new HashMap<>();
        this.usuarioDAO = new UsuarioDAOImpl();
        this.partidasDAO = new PartidasDAOImpl();
        cargarPartidasActivas();
        cargarPartidasTerminadas();
    }

    public boolean comprobarUsuario(String nombreUsuario, String contrasenia) {
        return usuarioDAO.comprobarUsuarioBD(nombreUsuario, contrasenia);
    }

    public boolean registrarUsuario(String nombreUsuario, String contrasenia) {
        return usuarioDAO.registrarUsuarioBD(nombreUsuario, contrasenia);
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

    public void notificarJugadores(int idUsuario, boolean conectado) {
        // Construir el mapa de todos los jugadores conectados
        Map<Integer, String> jugadoresConectados = new HashMap<>();
        for (Map.Entry<Integer, ConexionCliente> entry : usuariosConectados.entrySet()) {
            Integer id = entry.getKey();
            ConexionCliente cliente = entry.getValue();
            jugadoresConectados.put(id, cliente.getNombreUsuario());
        }
        // Enviar el mapa a todos los clientes conectados
        for (Map.Entry<Integer, ConexionCliente> entry : usuariosConectados.entrySet()) {
            ConexionCliente cliente = entry.getValue();
            cliente.enviarEntero(5);
            cliente.enviarObjeto(jugadoresConectados);
            if (cliente.getIdUsuario() != idUsuario) {
                cliente.enviarEntero(8);
                cliente.enviarTexto(usuariosConectados.get(idUsuario).getNombreUsuario().trim());
                cliente.enviarBoolean(conectado);
            }
        }
    }

    public int devolverIdUsuario(String nombre) {
        return usuarioDAO.devolverIdUsuario(nombre);
    }

    public Partida devolverPartida(int idPartida) {
        MovimientosPartida movimientosPartida = partidasDAO.obtenerMovimientosDeUnaPartida(idPartida);
        Tablero tablero = new Tablero(movimientosPartida.getTamanio());
        tablero.inicializarTablero();

        // Reproducir los movimientos en el tablero
        for (Movimiento movimiento : movimientosPartida.getMovimientos()) {

            reproducirMovimiento(tablero, movimiento);
        }

        return new Partida(idPartida, movimientosPartida.getIdJugadorBlanco(), movimientosPartida.getIdJugadorNegro(), movimientosPartida.getTurnoActual(), tablero);
    }

    public int aniadirPartidaActiva(int idPartida) {
        partidasActivas.put(idPartida, devolverPartida(idPartida));
        return idPartida;
    }

    public void eliminarPartida(int idPartida) {
        partidasActivas.remove(idPartida);
    }

    public int empezarPartida(int idUsuarioDesafiado, int idUsuarioDesafiador, int tamanio) {
        return  partidasDAO.crearPartidaBD(idUsuarioDesafiado, idUsuarioDesafiador, tamanio);
    }

    public void rendirseEnPartida(int idPartida) {
        partidasDAO.rendirseEnPartidaBD(idPartida);
    }

    public void actualizarPartidaServidor(int idPartida) {
        partidasActivas.replace(idPartida, obtenerPartida(partidasDAO.devolverPartidaBD(idPartida)));
    }

    public boolean moverFicha(Partida partida, int idUsuario, int posXini, int posYini, int posXfin, int posYfin) {
        if (comprobarTurno(partida, idUsuario) &&
                comprobarMovimientoLegal(partida.getTablero(), posXini, posYini, posXfin, posYfin, partida.getColorJugador(idUsuario), false)) {

            partidasDAO.insertarMovimientoBD(partida.getIdPartida(), idUsuario, posXini, posYini, posXfin, posYfin);
            return true;
        }
        return false;
    }

    public boolean capturarFicha(Partida partida, int idUsuario, int posXini, int posYini, int posXfin, int posYfin) {
        if (comprobarTurno(partida, idUsuario) &&
                comprobarMovimientoLegal(partida.getTablero(), posXini, posYini, posXfin, posYfin, partida.getColorJugador(idUsuario), true)) {
            partidasDAO.insertarMovimientoBD(partida.getIdPartida(), idUsuario, posXini, posYini, posXfin, posYfin);
            return true;
        }
        return false;
    }

    private boolean comprobarTurno(Partida partida, int idUsuario) {
        return partidasActivas.get(partida.getIdPartida()).getTurnoActual() == idUsuario;
    }

    private boolean comprobarMovimientoLegal(Tablero tablero, int posXini, int posYini, int posXfin, int posYfin, ColorPieza colorJugador, boolean captura) {
        int tamanio = tablero.getTamanio();
        Pieza pieza = tablero.getPieza(posXini, posYini);

        if (hayFichaEnPosicion(pieza)) return false;
        if (esFichaColorCorrecto(pieza, colorJugador)) return false;
        if (estaDentroDelTablero(posXfin, posYfin, tamanio)) return false;
        if (casillaDestinoLibre(tablero, posXfin, posYfin)) return false;

        boolean esDama = pieza.isEsDama();

        if (captura) {
            return esDama? esCapturaLegalDama(tablero, posXini, posYini, posXfin, posYfin) : esCapturaLegalPiezaNormal(tablero, pieza, posXini, posYini, posXfin, posYfin, tamanio);
        } else {
            return esDama? esMovimientoLegalDama(tablero, posXini, posYini, posXfin, posYfin) : esMovimientoLegalPiezaNormal(tablero, pieza, posXini, posYini, posXfin, posYfin, tamanio);
        }
    }

    private boolean hayFichaEnPosicion(Pieza pieza) {
        return pieza == null;
    }

    private boolean esFichaColorCorrecto(Pieza pieza, ColorPieza colorJugador) {
        return pieza.getColor() != colorJugador;
    }

    private boolean estaDentroDelTablero(int posX, int posY, int tamanio) {
        return posX < 0 || posX >= tamanio || posY < 0 || posY >= tamanio;
    }

    private boolean casillaDestinoLibre(Tablero tablero, int posXfin, int posYfin) {
        return tablero.getPieza(posXfin, posYfin) != null;
    }

    private boolean esMovimientoAdelante(Pieza pieza, int desplazamientoY) {
        return (pieza.getColor().equals(ColorPieza.BLANCA) && desplazamientoY == -1) ||
                (pieza.getColor().equals(ColorPieza.NEGRA) && desplazamientoY == 1);
    }
    private boolean esCapturaAdelante(Pieza pieza, int desplazamientoY) {
        return (pieza.getColor().equals(ColorPieza.BLANCA) && desplazamientoY == -2) ||
                (pieza.getColor().equals(ColorPieza.NEGRA) && desplazamientoY == 2);
    }

    private boolean hayFichasEnCasillasIntermedias(Tablero tablero, int posXini, int posYini, int posXfin, int posYfin) {
        int sentidoX = posXfin > posXini ? 1 : -1;
        int sentidoY = posYfin > posYini ? 1 : -1;

        int x = posXini + sentidoX;
        int y = posYini + sentidoY;

        // Comprobar que no hay fichas en las casillas intermedias entre la casilla de origen y la casilla de la ficha que va a ser comida o la casilla destino
        while (x != posXfin - sentidoX || y != posYfin - sentidoY) {
            // Si hay una ficha en alguna casilla intermedia, el movimiento no es legal
            if (tablero.getPieza(x, y) != null) return true;

            // Mover a la siguiente casilla intermedia
            x += sentidoX;
            y += sentidoY;
        }
        return false;
    }

    private boolean esMovimientoLegalDama(Tablero tablero, int posXini, int posYini, int posXfin, int posYfin) {
        //Comprobar que el movimiento es diagonal
        if (Math.abs(posXfin - posXini) != Math.abs(posYfin - posYini)) return false;

        if (hayFichasEnCasillasIntermedias(tablero, posXini, posYini, posXfin, posYfin)) return false;

        //Mover ficha
        tablero.moverFicha(posXini, posYini, posXfin, posYfin);

        return true;
    }

    private boolean esMovimientoLegalPiezaNormal(Tablero tablero, Pieza pieza, int posXini, int posYini, int posXfin, int posYfin, int tamanio) {
        if (Math.abs(posYfin - posYini) != 1) return false;

        int desplazamientoY = posXfin - posXini;
        if (!esMovimientoAdelante(pieza, desplazamientoY)) return false;

        if ((posYfin == tamanio - 1  && pieza.getColor().equals(ColorPieza.BLANCA) || posYfin == 0 && pieza.getColor().equals(ColorPieza.NEGRA))) {
            pieza.setEsDama(true);
        }

        //Mover ficha
        tablero.moverFicha(posXini, posYini, posXfin, posYfin);

        return true;
    }

    private boolean esCapturaLegalDama(Tablero tablero, int posXini, int posYini, int posXfin, int posYfin) {
        //Comprobar que el movimiento es diagonal
        if (Math.abs(posXfin - posXini) != Math.abs(posYfin - posYini)) return false;

        int sentidoX = posXfin > posXini ? 1 : -1;
        int sentidoY = posYfin > posYini ? 1 : -1;

        // Coordenadas de la casilla justo antes de la casilla de destino
        int posXcaptura = posXfin - sentidoX;
        int posYcaptura = posYfin - sentidoY;

        // Verificar que hay exactamente una ficha justo antes de la casilla de destino que va a ser comida
        if (tablero.getPieza(posXcaptura, posYcaptura) == null) return false;

        if(hayFichasEnCasillasIntermedias(tablero, posXini, posYini, posXcaptura, posYcaptura)) return false;

        //Eliminar ficha
        tablero.eliminarFicha(posXcaptura, posYcaptura);

        //Mover ficha
        tablero.moverFicha(posXini, posYini, posXfin, posYfin);

        return true;
    }

    private boolean esCapturaLegalPiezaNormal(Tablero tablero, Pieza pieza, int posXini, int posYini, int posXfin, int posYfin, int tamanio) {
        //Comprobar que solo avanza dos casillas en el eje x
        if (Math.abs(posXfin - posXini) != 2) return false;

        //Comprobar que solo avanza dos casillas hacia adelante
        int desplazamientoY = posYfin - posYini;
        if (!esCapturaAdelante(pieza, desplazamientoY)) return false;

        // Comprobar si hay una ficha a capturar en la posición intermedia
        int posXintermedia = (posXini + posXfin) / 2;
        int posYintermedia = (posYini + posYfin) / 2;

        Pieza fichaIntermedia = tablero.getPieza(posXintermedia, posYintermedia);
        if (fichaIntermedia == null || fichaIntermedia.getColor() == pieza.getColor()) return false;

        // Eliminar la ficha capturada
        tablero.eliminarFicha(posXintermedia, posYintermedia);

        //Mover ficha
        tablero.moverFicha(posXini, posYini, posXfin, posYfin);

        //Comprobar si ha llegado al final del tablero para hacerla dama
        if ((posYfin == tamanio - 1  && pieza.getColor().equals(ColorPieza.BLANCA) || posYfin == 0 && pieza.getColor().equals(ColorPieza.NEGRA))) {
            pieza.setEsDama(true);
        }

        return true;
    }

    public boolean comprobarVictoria(Tablero tablero, ColorPieza color) {
        for (int i = 0; i < tablero.getTamanio(); i++) {
            for (int j = 0; j < tablero.getTamanio(); j++) {
                Pieza pieza = tablero.getPieza(i, j);
                if (pieza != null && pieza.getColor() != color){
                    return false;
                }
            }
        }
        return true;
    }

    public void cambiarTurno(int idPartida) {
        partidasActivas.get(idPartida).cambiarTurno();
    }

    /*
    public void cargarPartidasEficiente(int idUsuario) {
        List<Integer> partidasConAdversarioConectado = new ArrayList<>();
        List<Integer> partidasSinAdversarioConectado = new ArrayList<>();

        for (Map.Entry<Integer, Partida> entry : partidasActivas.entrySet()) {
            Integer idPartida = entry.getKey();
            Partida partida = entry.getValue();

            if (partida.estaInvolucrado(idUsuario)) {
                Integer idAversario = partida.getIdAdversario(idUsuario);

                if (idAversario != null && usuariosConectados.containsKey(idAversario)) {
                    partidasConAdversarioConectado.add(idPartida);
                } else {
                    partidasSinAdversarioConectado.add(idPartida);
                }
            }
        }
        usuariosConectados.get(idUsuario).enviarEntero(6);
        usuariosConectados.get(idUsuario).enviarObjeto(partidasConAdversarioConectado);
        usuariosConectados.get(idUsuario).enviarObjeto(partidasSinAdversarioConectado);
    }

     */

    public void cargarPartidasUsuario(int idUsuario) {
        // Actualizar las partidas activas y terminadas para el usuario
        actualizarPartidasUsuario(idUsuario);
        actualizarRepeticiones(idUsuario);

        // Notificar a todos los clientes conectados para que actualicen sus datos
        notificarActualizacionPartidas();
    }

    public void notificarActualizacionPartidas() {
        for (ConexionCliente cliente : usuariosConectados.values()) {
            actualizarPartidasUsuario(cliente.getIdUsuario());
        }
    }

    public void actualizarPartidasUsuario(int idUsuario) {
        Map<Integer, Partida> partidasConAdversarioConectado = new HashMap<>();
        Map<Integer, Partida> partidasSinAdversarioConectado = new HashMap<>();

        for (Map.Entry<Integer, Partida> entry : partidasActivas.entrySet()) {
            Integer idPartida = entry.getKey();
            Partida partida = entry.getValue();

            if (partida.estaInvolucrado(idUsuario)) {
                Integer idAversario = partida.getIdAdversario(idUsuario);

                if (idAversario != null && usuariosConectados.containsKey(idAversario)) {
                    partidasConAdversarioConectado.put(idPartida, partida);
                } else {
                    partidasSinAdversarioConectado.put(idPartida, partida);
                }
            }
        }
        usuariosConectados.get(idUsuario).enviarEntero(6);
        usuariosConectados.get(idUsuario).enviarObjeto(partidasConAdversarioConectado);
        usuariosConectados.get(idUsuario).enviarObjeto(partidasSinAdversarioConectado);
    }

    public void actualizarRepeticiones(int idUsuario) {
        Map<Integer, PartidaTerminada> partidasTerminadasACargar = new HashMap<>();

        for (Map.Entry<Integer, PartidaTerminada> entry : partidasTerminadas.entrySet()) {
            Integer idPartida = entry.getKey();
            PartidaTerminada partidaTerminada = entry.getValue();

            if (partidaTerminada.estaInvolucrado(idUsuario)) {
                partidasTerminadasACargar.put(idPartida, partidaTerminada);
            }
        }
        usuariosConectados.get(idUsuario).enviarEntero(7);
        usuariosConectados.get(idUsuario).enviarObjeto(partidasTerminadasACargar);
    }

    private void cargarPartidasActivas() {
        ArrayList<MovimientosPartida> movimientosTodasPartidas = partidasDAO.devolverPartidasActivasBD();
        this.partidasActivas = obtenerPartidas(movimientosTodasPartidas);
    }

    public void cargarPartidasTerminadas() {
        ArrayList<MovimientosPartida> movimientosTodasPartidas = partidasDAO.devolverPartidasTerminadasBD();
        this.partidasTerminadas = obtenerRepeticiones(movimientosTodasPartidas);
    }

    private Partida obtenerPartida(MovimientosPartida movimientosPartida) {
        // Crear un nuevo tablero e inicializarlo
        Tablero tablero = new Tablero(movimientosPartida.getTamanio());
        tablero.inicializarTablero();

        // Reproducir los movimientos en el tablero
        for (Movimiento movimiento : movimientosPartida.getMovimientos()) {
            reproducirMovimiento(tablero, movimiento);
        }
        return new Partida(
                movimientosPartida.getIdPartida(),
                movimientosPartida.getIdJugadorBlanco(),
                movimientosPartida.getIdJugadorNegro(),
                movimientosPartida.getTurnoActual(),
                tablero
        );
    }

    private HashMap<Integer, Partida> obtenerPartidas(ArrayList<MovimientosPartida> movimientosPartidas) {
        HashMap<Integer, Partida> partidasDesdeMovimientos = new HashMap<>();

        for (MovimientosPartida movimientosUnaPartida : movimientosPartidas) {

            Partida partida = obtenerPartida(movimientosUnaPartida);
            partidasDesdeMovimientos.put(partida.getIdPartida(), partida);
        }
        return partidasDesdeMovimientos;
    }

    private void reproducirMovimiento(Tablero tablero, Movimiento movimiento) {
        int origenX = movimiento.getPosIniX();
        int origenY = movimiento.getPosIniY();
        int destinoX = movimiento.getPosFinX();
        int destinoY = movimiento.getPosFinY();

        // Mover la ficha en el tablero
        tablero.moverFicha(origenX, origenY, destinoX, destinoY);

        eliminarFichasIntermedias(tablero, origenX, origenY, destinoX, destinoY);
    }

    private void eliminarFichasIntermedias(Tablero tablero, int origenX, int origenY, int destinoX, int destinoY) {
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

    private HashMap<Integer, PartidaTerminada> obtenerRepeticiones(ArrayList<MovimientosPartida> movimientosPartidas) {
        //Aqui recoger los movimientos que nos da partidasDAO y crear un nuevo tablero por movimiento
        //asi conseguir un array de Tableros que será la partida
        HashMap<Integer, PartidaTerminada> repeticiones = new HashMap<>();

        for (MovimientosPartida movimientosUnaPartida : movimientosPartidas) {
            ArrayList<Tablero> partidaTerminada = new ArrayList<>();

            Tablero tableroInicial = new Tablero(movimientosUnaPartida.getTamanio());
            tableroInicial.inicializarTablero();

            partidaTerminada.add(new Tablero(tableroInicial));

            for (Movimiento movimiento : movimientosUnaPartida.getMovimientos()) {
                reproducirMovimiento(tableroInicial, movimiento);
                partidaTerminada.add(new Tablero(tableroInicial)); // Agregar copia del tablero después de cada movimiento
            }

            PartidaTerminada finalizada = new PartidaTerminada(
                    movimientosUnaPartida.getIdPartida(),
                    movimientosUnaPartida.getIdJugadorBlanco(),
                    movimientosUnaPartida.getIdJugadorNegro(),
                    partidaTerminada
            );
            repeticiones.put(movimientosUnaPartida.getIdPartida(), finalizada);
        }
        return repeticiones;
    }

    public Map<Integer, Partida> devolverMisPartidasActivas(int idUsuario) {
        HashMap<Integer, Partida> misPartidasActivas = new HashMap<>();

        for (Map.Entry<Integer, Partida> entry : partidasActivas.entrySet()) {
            Partida partida = entry.getValue();
            if (partida.getIdJugadorBlancas() == idUsuario || partida.getIdJugadorNegras() == idUsuario) {
                misPartidasActivas.put(entry.getKey(), partida);
            }
        }

        return misPartidasActivas;
    }

    public Map<Integer, PartidaTerminada> devolverMisPartidasFinalizadas(int idUsuario) {
        HashMap<Integer, PartidaTerminada> misPartidasFinalizadas = new HashMap<>();

        for (Map.Entry<Integer, PartidaTerminada> entry : partidasTerminadas.entrySet()) {
            PartidaTerminada partidaTerminada = entry.getValue();
            if (partidaTerminada.getIdJugadorBlancas() == idUsuario || partidaTerminada.getIdJugadorNegras() == idUsuario) {
                misPartidasFinalizadas.put(entry.getKey(), partidaTerminada);
            }
        }

        return misPartidasFinalizadas;
    }
}
