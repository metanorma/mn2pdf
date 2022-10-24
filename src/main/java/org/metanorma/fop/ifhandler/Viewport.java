package org.metanorma.fop.ifhandler;

public class Viewport {

    int x = 0;

    int y = 0;

    Viewport(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
