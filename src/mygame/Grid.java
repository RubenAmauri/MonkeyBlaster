package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Rectangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;

public class Grid {

    private Node gridNode;
    private Spring[] springs;
    private PointMass[][] points;
    private Geometry defaultLine;
    private Geometry thickLine;

    public Grid(int width, int height, Vector2f spacing,
            Node guiNode, AssetManager assetManager) {
        gridNode = new Node();
        guiNode.attachChild(gridNode);
        defaultLine = createLine(1f, assetManager);
        thickLine = createLine(3f, assetManager);

        ArrayList<Spring> springList = new ArrayList<>();
        float stiffness = 0.28f;
        float damping = 0.06f;
        int numColumns = (int) (width / spacing.x) + 2;
        int numRows = (int) (height / spacing.y) + 2;

        points = new PointMass[numColumns][numRows];
        PointMass[][] fixedPoints = new PointMass[numColumns][numRows];

        // Crear los point masses
        float xCoord = 0, yCoord = 0;
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numColumns; col++) {
                points[col][row] = new PointMass(
                        new Vector3f(xCoord, yCoord, 0), 1);
                fixedPoints[col][row] = new PointMass(
                        new Vector3f(xCoord, yCoord, 0), 0);
                xCoord += spacing.x;
            }
            yCoord += spacing.y;
            xCoord = 0;
        }

        // Conectar con springs
        for (int y = 0; y < numRows; y++) {
            for (int x = 0; x < numColumns; x++) {
                if (x == 0 || y == 0
                        || x == numColumns - 1 || y == numRows - 1) {
                    springList.add(new Spring(fixedPoints[x][y], points[x][y],
                            0.5f, 0.1f, gridNode, false, null));
                } else if (x % 3 == 0 && y % 3 == 0) {
                    springList.add(new Spring(fixedPoints[x][y], points[x][y],
                            0.005f, 0.02f, gridNode, false, null));
                }

                Geometry line;
                if (x > 0) {
                    line = (y % 3 == 0) ? thickLine : defaultLine;
                    springList.add(new Spring(points[x - 1][y], points[x][y],
                            stiffness, damping, gridNode, true, line.clone()));
                }
                if (y > 0) {
                    line = (x % 3 == 0) ? thickLine : defaultLine;
                    springList.add(new Spring(points[x][y - 1], points[x][y],
                            stiffness, damping, gridNode, true, line.clone()));
                }

                // Líneas de interpolación
                if (x > 0 && y > 0) {
                    Geometry addLine1 = defaultLine.clone();
                    addLine1.addControl(new AdditionalLineControl(
                            points[x - 1][y], points[x][y],
                            points[x - 1][y - 1], points[x][y - 1]));
                    gridNode.attachChild(addLine1);

                    Geometry addLine2 = defaultLine.clone();
                    addLine2.addControl(new AdditionalLineControl(
                            points[x][y - 1], points[x][y],
                            points[x - 1][y - 1], points[x - 1][y]));
                    gridNode.attachChild(addLine2);
                }
            }
        }

        springs = springList.toArray(new Spring[0]);
    }

    public void update(float tpf) {
        for (Spring spring : springs) {
            spring.update(tpf);
        }
        for (int x = 0; x < points.length; x++) {
            for (int y = 0; y < points[0].length; y++) {
                points[x][y].update(tpf);
            }
        }
    }

    public Node getNode() { return gridNode; }

    public void applyDirectedForce(Vector3f force,
            Vector3f position, float radius) {
        for (int x = 0; x < points.length; x++) {
            for (int y = 0; y < points[0].length; y++) {
                if (position.distanceSquared(points[x][y].getPosition())
                        < radius * radius) {
                    float ff = 10f / (10f + position.distance(
                            points[x][y].getPosition()));
                    points[x][y].applyForce(force.mult(ff));
                }
            }
        }
    }

    public void applyImplosiveForce(float force,
            Vector3f position, float radius) {
        for (int x = 0; x < points.length; x++) {
            for (int y = 0; y < points[0].length; y++) {
                float dist = position.distanceSquared(
                        points[x][y].getPosition());
                if (dist < radius * radius) {
                    Vector3f fv = position.subtract(
                            points[x][y].getPosition());
                    fv.multLocal(10f * force / (100 + dist));
                    points[x][y].applyForce(fv);
                    points[x][y].increaseDamping(0.6f);
                }
            }
        }
    }

    public void applyExplosiveForce(float force,
            Vector3f position, float radius) {
        for (int x = 0; x < points.length; x++) {
            for (int y = 0; y < points[0].length; y++) {
                float dist = position.distanceSquared(
                        points[x][y].getPosition());
                if (dist < radius * radius) {
                    Vector3f fv = position.subtract(
                            points[x][y].getPosition());
                    fv.multLocal(-100f * force / (10000 + dist));
                    points[x][y].applyForce(fv);
                    points[x][y].increaseDamping(0.6f);
                }
            }
        }
    }

    private Geometry createLine(float thickness, AssetManager assetManager) {
        Vector3f[] vertices = {
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, 1)
        };
        int[] indices = {0, 1};

        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Lines);
        lineMesh.setLineWidth(thickness);
        lineMesh.setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(vertices));
        lineMesh.setBuffer(VertexBuffer.Type.Index, 1,
                BufferUtils.createIntBuffer(indices));
        lineMesh.updateBound();

        Geometry lineGeom = new Geometry("lineMesh", lineMesh);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState()
                .setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.setColor("Color", new ColorRGBA(0.118f, 0.118f, 0.545f, 0.25f));
        mat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        lineGeom.setMaterial(mat);
        return lineGeom;
    }
}
