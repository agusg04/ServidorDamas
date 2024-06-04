package damas.servidor;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Servidor {
    final int PUERTO = 5000;

    public void iniciarServidor() {
        try (ServerSocket socketServidor = new ServerSocket(PUERTO);) {
            System.out.println("Servidor a la escucha en el puerto -> " + PUERTO);

            GestorJuego gestorJuego = new GestorJuego();

            while (true) {
                Socket socketCliente = socketServidor.accept();
                System.out.println("Cliente conectado desde " + socketCliente);

                new Thread(new ConexionCliente(socketCliente, gestorJuego)).start();
            }
        } catch (Exception e) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, e);
        }

    }

}
