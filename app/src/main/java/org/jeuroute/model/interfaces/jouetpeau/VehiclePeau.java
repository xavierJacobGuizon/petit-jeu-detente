package org.jeuroute.model.interfaces.jouetpeau;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2d;

import java.awt.Point;

import org.jeuroute.model.interfaces.Peau;

public class VehiclePeau implements Peau {

    private final float r;
    private final float g;
    private final float b;

    public VehiclePeau(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void display(Point start, Point end) {
        glColor3f(r, g, b);
        glBegin(GL_QUADS);
        glVertex2d(start.x, start.y);
        glVertex2d(end.x, start.y);
        glVertex2d(end.x, end.y);
        glVertex2d(start.x, end.y);
        glEnd();
    }
}
