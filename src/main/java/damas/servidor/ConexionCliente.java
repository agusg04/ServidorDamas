package damas.servidor;
 
import datos.DatosUsuario;


import java.io.*;
import java.net.Socket;
import java.util.Random;

public class ConexionCliente implements Runnable{
    private final Socket socketCliente;
    private final GestorJuego gestorJuego;
    private ObjectInputStream flujoEntrada;
    private ObjectOutputStream flujoSalida;
    private DatosUsuario datosUsuario;
    private boolean cerrarConexion;

    public ConexionCliente(Socket socketCliente, GestorJuego gestorJuego) {
        this.socketCliente = socketCliente;
        this.gestorJuego = gestorJuego;
    }

    public int getIdUsuario() {
        return datosUsuario.getIdUsuario();
    }

    public String getNombreUsuario() {
        return datosUsuario != null ? datosUsuario.getNombre() : null;
    }

    @Override
    public void run() {
        try {
            flujoSalida = new ObjectOutputStream(socketCliente.getOutputStream());
            flujoEntrada = new ObjectInputStream(socketCliente.getInputStream());
            System.out.println("Flujos creados");
        } catch (Exception e) {
            System.err.println("Problemas al iniciar los flujos de datos");
            cerrarSocket();
            return;
        }

        realizarLoginORegistro();


        while (!cerrarConexion) {
            String orden = leerTexto();
            System.out.println(orden);
            if (orden != null) {
                String[] partes = orden.split(";");
                int numeroOrden = Integer.parseInt(partes[0]);
                switch (numeroOrden) {
                    case 0:
                        //Salir
                        //orden;
                        gestorJuego.notificarJugadores(getIdUsuario(), false);
                        gestorJuego.eliminarUsuarioConectado(this);
                        gestorJuego.notificarActualizacionPartidas();
                        cerrarSocket();
                        cerrarConexion = true;
                        break;

                    case 1:
                        // Enviamos el id de la partida creada
                        //orden;idUsuarioDesafiado;
                        int idPartida = gestorJuego.aniadirPartidaActiva(
                                            gestorJuego.empezarPartida(
                                                    Integer.parseInt(partes[1]),
                                                    getIdUsuario(),
                                                    (new Random().nextInt(2) == 0 ? 8 : 10)
                                            )
                                        );
                        gestorJuego.actualizarPartidasUsuario(getIdUsuario()); // Actualizar las partidas al jugador
                        gestorJuego.actualizarPartidasUsuario(Integer.parseInt(partes[1])); // Actualizar las partidas al adversario
                        enviarEntero(13); // Enviar al creado mensaje notificando que se ha creado
                        enviarEntero(idPartida); // Enviar id de la partida creada
                        gestorJuego.usuariosConectados.get(Integer.parseInt(partes[1])).enviarEntero(13); // Enviar al adversario mensaje notificando que se ha creado una partida
                        gestorJuego.usuariosConectados.get(Integer.parseInt(partes[1])).enviarEntero(idPartida); // Enviar id de la partida creada

                        break;

                    case 2:
                        // Rendirse en una partida
                        //orden;idPartida;idAdversario;
                        gestorJuego.rendirseEnPartida(Integer.parseInt(partes[1])); // El usuario se rinde en la partida
                        gestorJuego.eliminarPartida(Integer.parseInt(partes[1])); // Se elimina de las partidas en curso
                        gestorJuego.cargarPartidasTerminadas(); // Se actualizan las partidas terminadas para cargar esta última

                        gestorJuego.actualizarPartidasUsuario(getIdUsuario()); //Actualizar las partidas al jugador
                        gestorJuego.actualizarRepeticiones(getIdUsuario()); // Actualizar las repeticiones al jugador
                        enviarEntero(9); // Enviarle mensaje notificando que se ha rendido
                        enviarEntero(Integer.parseInt(partes[1])); // Enviarle el id de la partida en la que se ha rendido

                        int idAdversario = Integer.parseInt(partes[2]);
                        if (gestorJuego.usuariosConectados.containsKey(idAdversario)) {
                            // Si el adversario está conectado notificárselo
                            gestorJuego.actualizarPartidasUsuario(idAdversario); // Actualizar las partidas al adversario si está conectado
                            gestorJuego.actualizarRepeticiones(idAdversario); // Actualizar las repeticiones al adversario
                            gestorJuego.usuariosConectados.get(idAdversario).enviarEntero(10); // Enviarle mensaje de que su adversario se rindió
                            gestorJuego.usuariosConectados.get(idAdversario).enviarEntero(Integer.parseInt(partes[1])); // Enviarle el id de la partida en la que se ha rendido el adversario
                        }
                        break;

                    case 3:
                        //Mover ficha
                        //orden;idPartida;idAdversario;coordenadaXorigen;coordenadaYorigen;coordenadaXdestino;coordenadaYdestino;

                        boolean exitoso = gestorJuego.moverFicha( // Comprobar que el movimiento es legal
                                            gestorJuego.partidasActivas.get(Integer.parseInt(partes[1])),
                                            getIdUsuario(),
                                            Integer.parseInt(partes[3]),
                                            Integer.parseInt(partes[4]),
                                            Integer.parseInt(partes[5]),
                                            Integer.parseInt(partes[6])
                                            );
                        if (exitoso) {
                            gestorJuego.actualizarPartidaServidor(Integer.parseInt(partes[1])); // Actualizar las partidas activas del servidor
                            gestorJuego.actualizarPartidasUsuario(getIdUsuario()); // Actualizar las partidas al jugador
                            enviarEntero(11); // Enviar orden de actualizar el tablero
                            enviarEntero(Integer.parseInt(partes[1])); // Enviar id del tablero a actualizar

                            int idAdversarioMovimiento = Integer.parseInt(partes[2]);
                            if (gestorJuego.usuariosConectados.containsKey(idAdversarioMovimiento)) {
                                gestorJuego.actualizarPartidasUsuario(idAdversarioMovimiento); // Actualizar las partidas al adversario si está conectado
                                gestorJuego.usuariosConectados.get(idAdversarioMovimiento).enviarEntero(11); // Enviar orden de actualizar el tablero
                                gestorJuego.usuariosConectados.get(idAdversarioMovimiento).enviarEntero(Integer.parseInt(partes[1])); // Enviar id del tablero a actualizar
                            }
                        } else {
                            enviarEntero(12); // Si el movimiento es ilegal enviar mensaje al usuario que lo ha intentado realizar
                            enviarEntero(Integer.parseInt(partes[1])); // Enviar id en el que debe salir el mensaje
                        }
                        break;

                    case 4:
                        //Capturar ficha
                        //orden;idPartida;idAversario;coordenadaXorigen;coordenadaYorigen;coordenadaXdestino;coordenadaYdestino;
                        boolean capturaExitosa = gestorJuego.capturarFicha( // Comprobar si la captura es legal
                                                    gestorJuego.partidasActivas.get(Integer.parseInt(partes[1])),
                                                    getIdUsuario(),
                                                    Integer.parseInt(partes[3]),
                                                    Integer.parseInt(partes[4]),
                                                    Integer.parseInt(partes[5]),
                                                    Integer.parseInt(partes[6])
                                                    );
                        int idAdversarioCaptura = Integer.parseInt(partes[2]);
                        if (capturaExitosa) {
                            if (gestorJuego.comprobarVictoria( // Comprobar si no quedan más fichas del adversario y, por tanto, sería victoria
                                    gestorJuego.partidasActivas.get(Integer.parseInt(partes[1])).getTablero(), // Enviar el tablero para comprobar la victoria
                                    gestorJuego.partidasActivas.get(Integer.parseInt(partes[1])).getColorJugador(getIdUsuario()) // Enviar color del jugador
                            )) {
                                gestorJuego.rendirseEnPartida(Integer.parseInt(partes[1])); // El jugador se "rinde" en la partida para terminar la partida
                                gestorJuego.eliminarPartida(Integer.parseInt(partes[1])); // Se elimina la partida de las partidas activas
                                gestorJuego.cargarPartidasTerminadas(); // Se cargan las partidas terminadas
                                gestorJuego.actualizarPartidasUsuario(getIdUsuario()); // Actualizar las partidas al jugador
                                gestorJuego.actualizarRepeticiones(getIdUsuario()); // Actualizar las repeticiones al jugador
                                enviarEntero(11); // Actualizar el tablero
                                enviarEntero(Integer.parseInt(partes[1]));
                                enviarEntero(14); // Enviar mensaje de que ha ganado
                                enviarEntero(gestorJuego.partidasActivas.get(Integer.parseInt(partes[1])).getIdPartida());

                                if (gestorJuego.usuariosConectados.containsKey(idAdversarioCaptura)) { // Si el adversario está conectado
                                    gestorJuego.actualizarPartidasUsuario(idAdversarioCaptura); // Actualizar las partidas al adversario si está conectado
                                    gestorJuego.actualizarRepeticiones(idAdversarioCaptura); // Actualizar las repeticiones al adversario si está conectado
                                    gestorJuego.usuariosConectados.get(idAdversarioCaptura).enviarEntero(15); // Enviar mensaje de que ha perdido
                                    gestorJuego.usuariosConectados.get(idAdversarioCaptura).enviarEntero(Integer.parseInt(partes[1]));
                                }
                            }
                            // Si no ha ganado
                            gestorJuego.actualizarPartidaServidor(Integer.parseInt(partes[1])); // Actualizar las partidas del servidor
                            gestorJuego.actualizarPartidasUsuario(getIdUsuario()); // Actualizar las partidas del usuario
                            enviarEntero(11); // Actualizar el tablero
                            enviarEntero(Integer.parseInt(partes[1]));

                            if (gestorJuego.usuariosConectados.containsKey(idAdversarioCaptura)) { // Si el adversario está conectado
                                gestorJuego.actualizarPartidasUsuario(idAdversarioCaptura); // Actualizar las partidas al adversario si está conectado
                                gestorJuego.usuariosConectados.get(idAdversarioCaptura).enviarEntero(11); // Actualizar el tablero
                                gestorJuego.usuariosConectados.get(idAdversarioCaptura).enviarEntero(Integer.parseInt(partes[1]));
                            }
                        } else {
                            enviarEntero(12); // Si la captura no es válida enviar mensaje de movimiento no válido
                            enviarEntero(Integer.parseInt(partes[1]));
                        }
                        break;

                    default:
                        throw new AssertionError();
                }
            }
        }

    }

    private void realizarLoginORegistro() {
        boolean exitoso = false;
        String[] partes;

        while (!exitoso) {
            String nombreYContra = leerTexto();
            if (nombreYContra != null){
                System.out.println(nombreYContra);

                partes = nombreYContra.split(";");

                int numeroOrden = Integer.parseInt(partes[0]);
                switch (numeroOrden) {
                    case 0:
                        cerrarSocket();
                        exitoso = true;
                        cerrarConexion = true;
                        break;
                    case 1:
                        exitoso = gestorJuego.comprobarUsuario(partes[1], partes[2]) &&
                                gestorJuego.aniadirUsuarioConectado(gestorJuego.devolverIdUsuario(partes[1]), this);
                        if (exitoso) {
                            enviarEntero(1);
                            cargarDatos(partes[1], partes[2]);
                            gestorJuego.notificarJugadores(getIdUsuario(), true);
                            gestorJuego.cargarPartidasUsuario(getIdUsuario());

                        } else {
                            enviarEntero(2);
                        }
                        break;

                    case 2:
                        boolean registro = gestorJuego.registrarUsuario(partes[1], partes[2]);
                        enviarEntero(registro ? 3 : 4);
                        // No cargar datos aquí. El usuario necesita iniciar sesión después de registrarse.
                        break;

                    default:
                        throw new AssertionError();

                }
            }

        }
    }

    private void cargarDatos(String nombre, String contrasenia) {
        int id = gestorJuego.devolverIdUsuario(nombre);

        datosUsuario = new DatosUsuario(id, nombre, contrasenia);
        enviarObjeto(datosUsuario);
    }


    public String leerTexto() {
        try {
            return flujoEntrada.readUTF();
        } catch (IOException e) {
            System.err.println("Error al leer texto del cliente");
        }
        return null;
    }

    public void enviarTexto(String texto) {
        try {
            flujoSalida.writeUTF(texto);
            flujoSalida.flush();
        } catch (Exception e) {
            System.err.println("Error al enviar texto al cliente");
        }

    }

    public void enviarEntero(int numero) {
        try {
            flujoSalida.writeInt(numero);
            flujoSalida.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar entero al cliente");
        }
    }

    public void enviarBoolean(boolean valor) {
        try {
            flujoSalida.writeBoolean(valor);
            flujoSalida.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar boolean al cliente");
        }
    }

    public void enviarObjeto(Object object) {
        try {
            flujoSalida.writeObject(object);
            flujoSalida.flush();
            System.out.println(object);
        } catch (Exception e) {
            System.err.println("Error al enviar objeto al cliente");
        }

    }

    public void cerrarSocket() {
        try {
            if (flujoEntrada != null) flujoEntrada.close();
            if (flujoSalida != null) flujoSalida.close();
            if (socketCliente != null) socketCliente.close();
            String nombreUsuario = getNombreUsuario();
            System.out.println("Conexiones con el cliente " + (nombreUsuario != null ? nombreUsuario : "no identificado") + " cerradas");
        } catch (IOException e) {
            System.err.println("Error al cerrar los flujos de datos");
        }
    }





}
