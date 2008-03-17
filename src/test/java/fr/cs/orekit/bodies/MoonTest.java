package fr.cs.orekit.bodies;


import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.models.bodies.Moon;
import fr.cs.orekit.time.AbsoluteDate;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MoonTest extends TestCase {

    public MoonTest(String name) {
        super(name);
    }

    public void testSpring() throws OrekitException {
        checkDirection(6868800.0,  -0.98153078, -0.19129129, 0.00223147);
    }

    public void testSummer() throws OrekitException {
        checkDirection(14731200.0, 0.52480279, -0.77707939, -0.34746173);
    }

    public void testAutomn() throws OrekitException {
        checkDirection(22766400.0, 0.05415817, 0.93023381, 0.36294898);
    }

    public void testWinter() throws OrekitException {
        checkDirection(30628800.0, -0.81434728, -0.55991656, -0.15274799);
    }

    public void checkDirection(double offsetJ2000, double x, double y, double z) throws OrekitException {
        Vector3D moon = new Moon().getPosition(new AbsoluteDate(AbsoluteDate.J2000Epoch, offsetJ2000), Frame.getJ2000());
        AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000Epoch, offsetJ2000);
        moon = Frame.getJ2000().getTransformTo(Frame.getReferenceFrame(Frame.VEIS1950, date), date).transformPosition(moon);
        moon = moon.normalize();
        assertEquals(x, moon.getX(), 1.0e-7);
        assertEquals(y, moon.getY(), 1.0e-7);
        assertEquals(z, moon.getZ(), 1.0e-7);
    }

    public static Test suite() {
        return new TestSuite(MoonTest.class);
    }

}

