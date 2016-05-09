package org.orekit.propagation.events;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;


/**
 * Test event handling on a {@link KeplerianPropagator}.
 *
 * @author Evan Ward
 */
public class CloseEventsAnalyticalKeplerianTest extends CloseEventsAbstractTest {

    @Override
    public Propagator getPropagator(double stepSize) throws OrekitException {
        return new KeplerianPropagator(initialOrbit);
    }

}
