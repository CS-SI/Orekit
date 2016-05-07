package org.orekit.propagation.events;

import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;

/**
 * Test event handling with a {@link NumericalPropagator} and a {@link
 * AdamsBashforthIntegrator}.
 *
 * @author Evan Ward
 */
public class CloseEventsNumericalABTest extends CloseEventsAbstractTest {

    /**
     * Create a propagator using the {@link #initialOrbit}.
     *
     * @param minStep   of integrator.
     * @param maxStep   of integrator.
     * @return a usable propagator.
     * @throws OrekitException
     */
    public Propagator getPropagator(double minStep,
                                    double maxStep) throws OrekitException {
        double[][] tol = NumericalPropagator
                .tolerances(1, initialOrbit, OrbitType.CARTESIAN);
        final NumericalPropagator propagator = new NumericalPropagator(
                new AdamsBashforthIntegrator(4, minStep, maxStep, tol[0], tol[1]));
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        return propagator;
    }

}
