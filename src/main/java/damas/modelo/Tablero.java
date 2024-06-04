package damas.modelo;

public class Tablero {
    private int id;
    private int tamanio;
    private Pieza[][] casillas;

    public Tablero(int id, int tamanio) {
        this.id = id;
        this.tamanio = tamanio;
        this.casillas = new Pieza[tamanio][tamanio];
    }

    public Tablero(Tablero tableroCopia) {
        this.id = tableroCopia.id;
        this.tamanio = tableroCopia.tamanio;
        this.casillas = new Pieza[tamanio][tamanio];
        for (int i = 0; i < tamanio; i++) {
            for (int j = 0; j < tamanio; j++) {
                if (tableroCopia.casillas[i][j] != null) {
                    this.casillas[i][j] = new Pieza(tableroCopia.casillas[i][j].getColor(), tableroCopia.casillas[i][j].isEsDama(), i, j);
                }
            }
        }
    }

    public void inicializarTablero() {

        int filasDeFichas = 0;
        if (tamanio == 8) {
            filasDeFichas = 3;
        } else if (tamanio == 10) {
            filasDeFichas = 4;
        }

        for (int i = 0; i < tamanio; i++) {
            for (int j = 0; j < tamanio; j++) {
                if ((i + j) % 2 != 0) {
                    if (i < filasDeFichas) {
                        casillas[i][j] = new Pieza(ColorPieza.NEGRA, false, i, j);
                    } else if (i >= tamanio - filasDeFichas) {
                         casillas[i][j] = new Pieza(ColorPieza.BLANCA, false, i, j);
                    }
                }
            }

        }
    }

    public void moverFicha(int posXorigen, int posYorigen, int posXdestino, int posYdestino) {
        Pieza ficha = getPieza(posXorigen, posYorigen);
        casillas[posXorigen][posYorigen] = null;
        casillas[posXdestino][posYdestino] = ficha;
        ficha.mover(posXdestino, posYdestino);
    }

    public Pieza getPieza(int posX, int posY) {
        return casillas[posX][posY];
    }

    public void eliminarFicha(int posX, int posY) {
        casillas[posX][posY] = null;
    }

    public void mostrarTablero() {
        for (int i = 0; i < tamanio; i++) {
            for (int j = 0; j < tamanio; j++) {
                if (casillas[i][j] == null) {
                    System.out.print("- ");
                } else {
                    System.out.print(casillas[i][j].getColor() == ColorPieza.BLANCA ? "B " : "N ");
                }
            }
            System.out.println();
        }
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

    public Pieza[][] getCasillas() {
        return casillas;
    }

    public void setCasillas(Pieza[][] casillas) {
        this.casillas = casillas;
    }

}
