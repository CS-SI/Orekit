package org.orekit.propagation.events;

import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.hipparchus.ode.nonstiff.AdamsMoultonIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
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
public class CloseEventsNumericalAMTest extends CloseEventsAbstractTest {

    /**
     * Create a propagator using the {@link #initialOrbit}.
     *
     * @param stepSize   of integrator.
     * @return a usable propagator.
     * @throws OrekitException
     */
    public Propagator getPropagator(double stepSize) throws OrekitException {
        double[][] tol = NumericalPropagator
                .tolerances(1, initialOrbit, OrbitType.CARTESIAN);
        final AdamsMoultonIntegrator integrator =
                new AdamsMoultonIntegrator(4, stepSize, stepSize, tol[0], tol[1]);
        final DormandPrince853Integrator starter =
                        new DormandPrince853Integrator(stepSize / 100, stepSize / 10,
                                                       tol[0], tol[1]);
        starter.setInitialStepSize(stepSize / 20);
        integrator.setStarterIntegrator(starter);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        return propagator;
    }

}
