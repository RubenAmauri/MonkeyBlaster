package mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class LineControl extends AbstractControl {

    private static final Vector3f UP = new Vector3f(1, 0, 0);
    private PointMass end1, end2;

    public LineControl(PointMass end1, PointMass end2) {
        this.end1 = end1;
        this.end2 = end2;
    }

    @Override
    protected void controlUpdate(float tpf) {
        // Posición
        spatial.setLocalTranslation(end1.getPosition());
        // Escala
        Vector3f dif = end2.getPosition().subtract(end1.getPosition());
        spatial.setLocalScale(dif.length());
        // Rotación
        spatial.lookAt(end2.getPosition(), UP);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}