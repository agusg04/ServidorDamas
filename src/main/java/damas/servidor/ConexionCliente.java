package damas.servidor;

import damas.modelo.Partida;
import damas.modelo.Tablero;
import damas.datos.DatosUsuario;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class ConexionCliente implements Runnable{
    private final Socket socketCliente;
    private final GestorJuego gestorJuego;
    private ObjectInputStream objetoEntrada;
    private ObjectOutputStream objetoSalida;

    private DatosUsuario datosUsuario;

    public ConexionCliente(Socket socketCliente, GestorJuego gestorJuego) {
        this.socketCliente = socketCliente;
        this.gestorJuego = gestorJuego;
    }

    public int getIdUsuario() {
        return datosUsuario.getIdUsuario();
    }

    @Override
    public void run() {
        try {
            objetoEntrada = new ObjectInputStream(socketCliente.getInputStream());
            objetoSalida = new ObjectOutputStream(socketCliente.getOutputStream());
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
                        //////////////////////enviarBoolean(gestorJuego.capturarFicha());
                        datosUsuario.setPartidasNoMiTurno(gestorJuego.devolverPartidasNoMiTurno(getIdUsuario()));
                        break;

                    case 5:
                        //Enviar partidas por terminar donde sea su turno
                        //orden;
                        enviarObjeto(datosUsuario.getPartidasMiTurno());
                        break;

                    case 6:
                        //Enviar partidas terminadas
                        //orden;
                        enviarObjeto(datosUsuario.getPartidasTermiandas());
                        break;

                    default:
                        throw new AssertionError();
                }
            }





    }

    private void realizarLoginORegistro() {
        boolean exitoso = false;
        String[] partes = null;

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
    }


    public String leerTexto() {
        try {
            return objetoEntrada.readUTF();
        } catch (IOException e) {
            System.err.println("Error al leer texto del cliente");
        }
        return null;
    }

    public void enviarTexto(String texto) {
        try {
            objetoSalida.writeUTF(texto);
            objetoSalida.flush();
        } catch (Exception e) {
            System.err.println("Error al enviar texto al cliente");
        }

    }

    public void enviarEntero(int numero) {
        try {
            objetoSalida.writeInt(numero);
            objetoSalida.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar entero al cliente");
        }
    }

    public void enviarBoolean(boolean valor) {
        try {
            objetoSalida.writeBoolean(valor);
            objetoSalida.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar boolean al cliente");
        }
    }

    public void enviarObjeto(Object object) {
        try {
            objetoSalida.writeObject(object);
            objetoSalida.flush();
        } catch (Exception e) {
            System.err.println("Error al enviar objeto al cliente");
        }

    }

    public void cerrarSocket() {
        try {
            objetoEntrada.close();
            objetoSalida.close();
            objetoSalida.close();
            socketCliente.close();

        } catch (IOException e) {
            System.err.println("Error al cerrar los flujos de datos");
        }
    }
}
