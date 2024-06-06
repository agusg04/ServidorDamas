package damas.servidor;
 
import damas.datos.DatosUsuario;
import modelo.*;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class ConexionCliente implements Runnable{
    private final Socket socketCliente;
    private final GestorJuego gestorJuego;
    private ObjectInputStream flujoEntrada;
    private ObjectOutputStream flujoSalida;

    private DatosUsuario datosUsuario;

    public ConexionCliente(Socket socketCliente, GestorJuego gestorJuego) {
        this.socketCliente = socketCliente;
        this.gestorJuego = gestorJuego;
    }

    public int getIdUsuario() {
        return datosUsuario.getIdUsuario();
    }

    public String getNombreUsuario() {
        return datosUsuario.getNombre();
    }

    @Override
    public void run() {
        try {
            flujoEntrada = new ObjectInputStream(socketCliente.getInputStream());
            flujoSalida = new ObjectOutputStream(socketCliente.getOutputStream());
        } catch (Exception e) {
            System.err.println("Problemas al iniciar los flujos de datos");
            cerrarSocket();
            return;
        }

        /*
        try {
            String mensaje;
            //var aa=(int) objetoEntrada.readObject();
            while ((mensaje = objetoEntrada.readUTF()) != null){
                System.out.println(mensaje);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
         */

        realizarLoginORegistro();

        while (socketCliente.isConnected()) {
            String orden = leerTexto();
            if (orden != null) {
                String[] partes = orden.split(";");
                switch (Integer.parseInt(partes[0])) {
                    case 0:
                        //Salir
                        //orden;
                        gestorJuego.eliminarUsuarioConectado(this);
                        cerrarSocket();
                        break;

                    case 1:
                        //Enviamos el id de la partida creada
                        //orden;idUsuarioDesafiado;
                        enviarEntero(gestorJuego.empezarPartida(Integer.parseInt(partes[1]), getIdUsuario(), (new Random().nextInt(2) == 0 ? 8 : 10) ));
                        datosUsuario.setPartidasNoMiTurno(gestorJuego.devolverPartidasNoMiTurno(getIdUsuario()));
                        break;

                    case 2:
                        //Rendirse en una partida
                        //orden;idPartida;
                        gestorJuego.rendirseEnPartida(Integer.parseInt(partes[1]), getIdUsuario());
                        datosUsuario.setPartidasTermiandas(gestorJuego.devolverPartidasTerminadas(getIdUsuario()));
                        break;

                    case 3:
                        //Poner ficha
                        //orden;idPartida;coordenadaXorigen;coordenadaYorigen;coordenadaXdestino;coordenadaYdestino;
                        gestorJuego.moverFicha(Integer.parseInt(partes[1]), getIdUsuario(), datosUsuario.buscarTableroMiTurno(Integer.parseInt(partes[1])), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]), Integer.parseInt(partes[4]), Integer.parseInt(partes[5]));
                        datosUsuario.setPartidasNoMiTurno(gestorJuego.devolverPartidasNoMiTurno(getIdUsuario()));
                        break;

                    case 4:
                        //Capturar ficha
                        //orden;idPartida;coordenadaXorigen;coordenadaYorigen;coordenadaXdestino;coordenadaYdestino;
                        enviarBoolean(gestorJuego.capturarFicha(Integer.parseInt(partes[1]), getIdUsuario(), datosUsuario.buscarTableroMiTurno(Integer.parseInt(partes[1])), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]), Integer.parseInt(partes[4]), Integer.parseInt(partes[5])));
                        datosUsuario.setPartidasNoMiTurno(gestorJuego.devolverPartidasNoMiTurno(getIdUsuario()));
                        break;

                    case 5:
                        //Enviar partidas por terminar donde sea su turno
                        //orden;
                        enviarEntero(2);
                        enviarObjeto(datosUsuario.getPartidasMiTurno());
                        break;

                    case 6:
                        //Enviar partidas terminadas
                        //orden;
                        enviarEntero(3);
                        enviarObjeto(datosUsuario.getPartidasTermiandas());
                        break;

                    case 7:
                        //Enviar los jugadores disponibles para jugar pero sin enviar al propio jugador
                        //orden;idUsuario
                        enviarObjeto(gestorJuego.devoLverJugadoresDisponibles(Integer.parseInt(partes[1])));
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

        while (!exitoso && socketCliente.isConnected()) {
            String nombreYContra = leerTexto();
            System.out.println(nombreYContra);

            partes = nombreYContra.split(";");

            switch (partes[0]) {
                case "L":
                    exitoso = gestorJuego.comprobarUsuario(partes[1], partes[2]) &&
                            gestorJuego.aniadirUsuarioConectado(gestorJuego.devolverIdUsuario(partes[1]), this);
                    enviarBoolean(exitoso);
                    if (exitoso) {
                        cargarDatos(partes[1], partes[2]);
                    }
                    break;

                case "R":
                    boolean registro = gestorJuego.registrarUsuario(partes[1], partes[2]);
                    enviarBoolean(registro);
                    // No cargar datos aquí. El usuario necesita iniciar sesión después de registrarse.
                    break;

                default:
                    throw new AssertionError();

            }
        }
    }

    private void cargarDatos(String nombre, String contrasenia) {
        int id = gestorJuego.devolverIdUsuario(nombre);
        ArrayList<Tablero> partidasMiTurno = gestorJuego.devolverPartidasMiTurno(id);
        ArrayList<Tablero> partidasNoMiTurno = gestorJuego.devolverPartidasNoMiTurno(id);
        ArrayList<Partida> partidasTerminadas = gestorJuego.devolverPartidasTerminadas(id);

        datosUsuario = new DatosUsuario(id, nombre, contrasenia, partidasMiTurno, partidasNoMiTurno, partidasTerminadas);
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
        } catch (Exception e) {
            System.err.println("Error al enviar objeto al cliente");
        }

    }

    public void cerrarSocket() {
        try {
            flujoEntrada.close();
            flujoSalida.close();
            socketCliente.close();

        } catch (IOException e) {
            System.err.println("Error al cerrar los flujos de datos");
        }
    }
}
