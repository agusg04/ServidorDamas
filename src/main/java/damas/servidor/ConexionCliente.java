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


        /*
        try {
            while (true) {
                String mensaje = flujoEntrada.readUTF();
                if (mensaje != null) {
                    System.out.println(mensaje);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

         */


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
                        //Enviamos el id de la partida creada
                        //orden;idUsuarioDesafiado;
                        gestorJuego.aniadirPartidaActiva(
                                gestorJuego.empezarPartida(
                                        Integer.parseInt(partes[1]),
                                        getIdUsuario(),
                                        (new Random().nextInt(2) == 0 ? 8 : 10)
                                )
                        );
                        gestorJuego.actualizarPartidasUsuario(getIdUsuario()); //Actualizar las partidas al jugador
                        gestorJuego.actualizarPartidasUsuario(Integer.parseInt(partes[1])); //Actualizar las partidas al adversario
                        break;

                    case 2:
                        //Rendirse en una partida
                        //orden;idPartida;idAdversario;
                        gestorJuego.rendirseEnPartida(Integer.parseInt(partes[1]));
                        gestorJuego.eliminarPartida(Integer.parseInt(partes[1]));
                        gestorJuego.cargarPartidasTerminadas();

                        gestorJuego.actualizarPartidasUsuario(getIdUsuario()); //Actualizar las partidas al jugador
                        gestorJuego.actualizarRepeticiones(getIdUsuario());
                        enviarEntero(9);
                        enviarEntero(Integer.parseInt(partes[1]));

                        if (gestorJuego.usuariosConectados.containsKey(Integer.parseInt(partes[2]))) {
                            gestorJuego.actualizarPartidasUsuario(Integer.parseInt(partes[2])); //Actualizar las partidas al adversario si esta conectado
                            gestorJuego.actualizarRepeticiones(Integer.parseInt(partes[2]));
                            enviarEntero(10);
                            enviarEntero(Integer.parseInt(partes[1]));
                        }
                        break;

                    case 3:
                        //Mover ficha
                        //orden;idPartida;idAdversario;coordenadaXorigen;coordenadaYorigen;coordenadaXdestino;coordenadaYdestino;

                        boolean exitoso = gestorJuego.moverFicha(
                                            gestorJuego.partidasActivas.get(Integer.parseInt(partes[1])),
                                            getIdUsuario(),
                                            Integer.parseInt(partes[3]),
                                            Integer.parseInt(partes[4]),
                                            Integer.parseInt(partes[5]),
                                            Integer.parseInt(partes[6])
                                            );
                        if (exitoso) {
                            gestorJuego.actualizarPartidaServidor(Integer.parseInt(partes[1]));
                            gestorJuego.actualizarPartidasUsuario(getIdUsuario());
                            enviarEntero(11);
                            enviarEntero(Integer.parseInt(partes[1]));

                            if (gestorJuego.usuariosConectados.containsKey(Integer.parseInt(partes[2]))) {
                                gestorJuego.actualizarPartidasUsuario(Integer.parseInt(partes[2])); //Actualizar las partidas al adversario si esta conectado
                                enviarEntero(11);
                                enviarEntero(Integer.parseInt(partes[1]));
                            }
                        } else {
                            enviarEntero(12);
                            enviarEntero(Integer.parseInt(partes[1]));
                        }
                        break;

                    case 4:
                        //Capturar ficha
                        //orden;idPartida;coordenadaXorigen;coordenadaYorigen;coordenadaXdestino;coordenadaYdestino;
                        /////////////////enviarBoolean(gestorJuego.capturarFicha(Integer.parseInt(partes[1]), getIdUsuario(), datosUsuario.buscarTableroMiTurno(Integer.parseInt(partes[1])), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]), Integer.parseInt(partes[4]), Integer.parseInt(partes[5])));
                        //////////////datosUsuario.setPartidasNoMiTurno(gestorJuego.devolverPartidasNoMiTurno(getIdUsuario()));
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
