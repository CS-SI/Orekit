package org.orekit.propagation.events;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class TimerDetectorTest {

    private int evtno = 0;
    private double dt;
    private AbsoluteDate iniDate;
    private TimerDetector timerDetector;
    private NumericalPropagator propagator;

    @Test
    public void testSimpleTimer() throws OrekitException {
    	AbsoluteDate triggerDate = iniDate.shiftedBy(dt);
    	TimerDetector timerDetector = new TimerDetector(triggerDate, dt) {
            private static final long serialVersionUID = 1L;
			public int eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
		        return STOP;
            }
        };

        propagator.addEventDetector(timerDetector);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(100.*dt));

        Assert.assertEquals(2.0*dt, finalState.getDate().durationFrom(iniDate), 1.0e-10);
    }

    @Test
    public void testEmbeddedTimer() throws OrekitException {

    	timerDetector = new TimerDetector(dt) {
            private static final long serialVersionUID = 1L;
			public int eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
		        return STOP;
            }
        };

        EventDetector dateDetector = new DateDetector(iniDate.shiftedBy(2.*dt)) {
            private static final long serialVersionUID = 1L;
			public int eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
  		        timerDetector.resetDate(s.getDate());
		        return CONTINUE;
            }
        };

        propagator.addEventDetector(dateDetector);
        propagator.addEventDetector(timerDetector);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(100.*dt));

        Assert.assertEquals(3.*dt, finalState.getDate().durationFrom(iniDate), 1.0e-10);
    }

    @Test
    public void testAutoEmbeddedTimer() throws OrekitException {
        AbsoluteDate triggerDate = iniDate;
    	timerDetector = new TimerDetector(triggerDate, dt) {
            private static final long serialVersionUID = 1L;
			public int eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
  		        this.resetDate(s.getDate());
  		        ++evtno;
		        return CONTINUE;
            }
        };
        propagator.addEventDetector(timerDetector);
        propagator.propagate(iniDate.shiftedBy(100.*dt));

        Assert.assertEquals(100, evtno);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        final double mu = 3.9860047e14;
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), iniDate, mu);
        SpacecraftState initialState = new SpacecraftState(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
        dt = 60.;
    }

    @After
    public void tearDown() {
        iniDate = null;
        propagator = null;
        timerDetector = null;
    }

}
