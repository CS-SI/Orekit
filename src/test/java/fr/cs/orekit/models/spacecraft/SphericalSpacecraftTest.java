package fr.cs.orekit.models.spacecraft;

import org.apache.commons.math.geometry.Vector3D;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SphericalSpacecraftTest
extends TestCase {

    public SphericalSpacecraftTest(String name) {
        super(name);
    }

    public void testConstructor() {
        SphericalSpacecraft s = new SphericalSpacecraft(1.0, 2.0, 3.0, 4.0);
        Vector3D[] directions = { Vector3D.plusI, Vector3D.plusJ, Vector3D.plusK };
        for (int i = 0; i < directions.length; ++i) {
            assertEquals(1.0, s.getSurface(directions[i]), 1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getDragCoef(directions[i]),
                                      2.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getAbsCoef(directions[i]),
                                      3.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getReflectionCoef(directions[i]),
                                      4.0, directions[i]).getNorm(),
                         1.0e-15);
        }
    }

    public void testSettersGetters() {
        SphericalSpacecraft s = new SphericalSpacecraft(0, 0, 0, 0);
        s.setSurface(1.0);
        s.setDragCoef(2.0);
        s.setAbsCoef(3.0);
        s.setReflectionCoef(4.0);
        Vector3D[] directions = { Vector3D.plusI, Vector3D.plusJ, Vector3D.plusK };
        for (int i = 0; i < directions.length; ++i) {
            assertEquals(1.0, s.getSurface(directions[i]), 1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getDragCoef(directions[i]),
                                      2.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getAbsCoef(directions[i]),
                                      3.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getReflectionCoef(directions[i]),
                                      4.0, directions[i]).getNorm(),
                         1.0e-15);
        }
    }

    public static Test suite() {
        return new TestSuite(SphericalSpacecraftTest.class);
    }

}
