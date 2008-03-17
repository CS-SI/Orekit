package fr.cs.orekit.maneuvers;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {

        TestSuite suite = new TestSuite("fr.cs.orekit.iers");

        suite.addTest(ConstantThrustManeuverTest.suite());
        return suite;

    }
}
