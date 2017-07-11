package org.orekit.propagation.events;

import org.hipparchus.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;

/**
 * Test event handling with a {@link NumericalPropagator} and a {@link
 * GraggBulirschStoerIntegrator}.
 *
 * @author Evan Ward
 */
public class CloseEventsNumericalGBSTest extends CloseEventsAbstractTest {

    /**
     * Create a propagator using the {@link #initialOrbit}.
     *
     * @param stepSize   of integrator.
     * @return a usable propagator.
     * @throws OrekitException
     */
    public Propagator getPropagator(double stepSize) throws OrekitException {
        double[][] tol = NumericalPropagator
                .tolerances(1e-1, initialOrbit, OrbitType.CARTESIAN);
        final NumericalPropagator propagator = new NumericalPropagator(
                new GraggBulirschStoerIntegrator(stepSize, stepSize, tol[0], tol[1]));
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        return propagator;
    }

}
