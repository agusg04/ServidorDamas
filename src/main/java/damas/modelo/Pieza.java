package damas.modelo;

public class Pieza {
    private ColorPieza color;
    private boolean esDama;
    private int posX;
    private int posY;

    public Pieza(ColorPieza color, boolean esDama, int posX, int posY) {
        this.color = color;
        this.esDama = esDama;
        this.posX = posX;
        this.posY = posY;
    }

    public void mover(int posXpasada, int posYpasada) {
        this.posX = posXpasada;
        this.posY = posYpasada;
    }

    // Getters y setters

    public ColorPieza getColor() {
        return color;
    }

    public void setColor(ColorPieza color) {
        this.color = color;
    }

    public boolean isEsDama() {
        return esDama;
    }

    public void setEsDama(boolean esDama) {
        this.esDama = esDama;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }
}
