package damas;


import damas.servidor.Servidor;

public class Main {
    public static void main(String[] args) {
        Servidor servidor = new Servidor();
        servidor.iniciarServidor();
    }
}