package fr.cs.orekit;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {

        TestSuite suite = new TestSuite("fr.cs.orekit");

        suite.addTest(fr.cs.orekit.iers.AllTests.suite());
        suite.addTest(fr.cs.orekit.time.AllTests.suite());
        suite.addTest(fr.cs.orekit.frames.AllTests.suite());
        suite.addTest(fr.cs.orekit.frames.series.AllTests.suite());
        suite.addTest(fr.cs.orekit.bodies.AllTests.suite());
        suite.addTest(fr.cs.orekit.orbits.AllTests.suite());
        suite.addTest(fr.cs.orekit.perturbations.AllTests.suite());
        suite.addTest(fr.cs.orekit.propagation.AllTests.suite());
        suite.addTest(fr.cs.orekit.potential.AllTests.suite());
        suite.addTest(fr.cs.orekit.maneuvers.AllTests.suite());
        suite.addTest(fr.cs.orekit.attitudes.AllTests.suite());
        suite.addTest(fr.cs.orekit.tle.AllTests.suite());

        return suite;

    }
}
