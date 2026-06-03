package mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class AdditionalLineControl extends AbstractControl {

    private static final Vector3f UP = new Vector3f(1, 0, 0);
    private PointMass end11, end12, end21, end22;
    private final Vector3f p1 = new Vector3f();
    private final Vector3f p2 = new Vector3f();

    public AdditionalLineControl(PointMass end11, PointMass end12,
            PointMass end21, PointMass end22) {
        this.end11 = end11;
        this.end12 = end12;
        this.end21 = end21;
        this.end22 = end22;
    }

    @Override
    protected void controlUpdate(float tpf) {
        p1.set(end11.getPosition()).interpolateLocal(end12.getPosition(), 0.5f);
        p2.set(end21.getPosition()).interpolateLocal(end22.getPosition(), 0.5f);
        spatial.setLocalTranslation(p1);
        spatial.setLocalScale(p1.distance(p2));
        spatial.lookAt(p2, UP);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}