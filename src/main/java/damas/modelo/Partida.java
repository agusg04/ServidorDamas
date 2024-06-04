package damas.modelo;

import java.util.ArrayList;

public class Partida {
    int id;
    int tamanio;
    ArrayList<Tablero> movimientos;

    public Partida(int id, int tamanio) {
        this.id = id;
        this.tamanio = tamanio;
        this.movimientos = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTamanio() {
        return tamanio;
    }

    public void setTamanio(int tamanio) {
        this.tamanio = tamanio;
    }

    public ArrayList<Tablero> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(ArrayList<Tablero> movimientos) {
        this.movimientos = movimientos;
    }
}
