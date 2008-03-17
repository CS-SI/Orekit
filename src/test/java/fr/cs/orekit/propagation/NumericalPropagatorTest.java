package fr.cs.orekit.propagation;

import junit.framework.*;
import org.apache.commons.math.ode.DormandPrince853Integrator;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.NumericalPropagator;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

public class NumericalPropagatorTest extends TestCase {

    public NumericalPropagatorTest(String name) {
        super(name);
    }

    public void testNoExtrapolation() throws OrekitException {

        // Definition of initial conditions
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.986e14;
        Orbit initialOrbit =
            new Orbit(new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0),
                      new EquinoctialParameters(new PVCoordinates(position, velocity),Frame.getJ2000(), mu));


        // Extrapolator definition
        NumericalPropagator extrapolator =
            new NumericalPropagator(mu, new DormandPrince853Integrator(0.0, 10000.0,
                                                                       1.0e-8, 1.0e-8));

        // Extrapolation of the initial at the initial date
        SpacecraftState finalOrbit = extrapolator.propagate(new SpacecraftState(initialOrbit),
                                                            initialOrbit.getDate()
        );
        // Initial orbit definition
        Vector3D initialPosition = initialOrbit.getPVCoordinates(mu).getPosition();
        Vector3D initialVelocity = initialOrbit.getPVCoordinates(mu).getVelocity();

        // Final orbit definition
        Vector3D finalPosition   = finalOrbit.getPVCoordinates(mu).getPosition();
        Vector3D finalVelocity   = finalOrbit.getPVCoordinates(mu).getVelocity();

        // Check results
        assertEquals(initialPosition.getX(), finalPosition.getX(), 1.0e-10);
        assertEquals(initialPosition.getY(), finalPosition.getY(), 1.0e-10);
        assertEquals(initialPosition.getZ(), finalPosition.getZ(), 1.0e-10);
        assertEquals(initialVelocity.getX(), finalVelocity.getX(), 1.0e-10);
        assertEquals(initialVelocity.getY(), finalVelocity.getY(), 1.0e-10);
        assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 1.0e-10);

    }

    public void testKepler() throws OrekitException {

        // Definition of initial conditions
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.986e14;
        Orbit initialOrbit =
            new Orbit(new AbsoluteDate(AbsoluteDate.J2000Epoch, 0.0),
                      new EquinoctialParameters(new PVCoordinates(position,  velocity),Frame.getJ2000(), mu));

        // Extrapolator definition
        NumericalPropagator extrapolator =
            new NumericalPropagator(mu, new DormandPrince853Integrator(0.0, 10000.0,
                                                                       1.0e-8, 1.0e-8));
        double dt = 3200;

        // Extrapolation of the initial at t+dt
        SpacecraftState finalOrbit =  extrapolator.propagate(new SpacecraftState(initialOrbit),
                                                             new AbsoluteDate(initialOrbit.getDate(),dt));

        // Check results
        double n = Math.sqrt(mu / initialOrbit.getA()) / initialOrbit.getA();
        assertEquals(initialOrbit.getA(),    finalOrbit.getA(),    1.0e-10);
        assertEquals(initialOrbit.getEx(),    finalOrbit.getEx(),    1.0e-10);
        assertEquals(initialOrbit.getEy(),    finalOrbit.getEy(),    1.0e-10);
        assertEquals(initialOrbit.getHx(),    finalOrbit.getHx(),    1.0e-10);
        assertEquals(initialOrbit.getHy(),    finalOrbit.getHy(),    1.0e-10);
        assertEquals(initialOrbit.getLM() + n * dt, finalOrbit.getLM(), 4.0e-10);

    }

    public static Test suite() {
        return new TestSuite(NumericalPropagatorTest.class);
    }

}

