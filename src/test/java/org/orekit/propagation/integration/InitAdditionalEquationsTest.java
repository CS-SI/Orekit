package org.orekit.propagation.integration;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class InitAdditionalEquationsTest {
    
    private NumericalPropagator  propagator;
    private AbsoluteDate         initDate;
    private double               mu;
    private SpacecraftState      initialState;
    
    
    /** Test for issue #401 */
    @Test
    public void testInitAdd() throws OrekitException {
        // setup
        // mutable holders
        SpacecraftState[] actualState = new SpacecraftState[1];
        AbsoluteDate[] actualDate = new AbsoluteDate[1];
        
        InitCheckerEquations checker = new InitCheckerEquations() {
            @Override
            public void init(SpacecraftState initialState, AbsoluteDate target) {
                actualState[0] = initialState;
                actualDate[0] = target;
            }
        };
        
        checker.initCheckerEquations();
        Assert.assertFalse(checker.wasCalled());
        AbsoluteDate target = initDate.shiftedBy(60);
        
        // action
        propagator.propagate(target);
        
        // verify
        Assert.assertTrue(checker.wasCalled());
    }
    
    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        mu  = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit, OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
    }

    @After
    public void tearDown() {
        initDate = null;
        initialState = null;
        propagator = null;
    }
    
    private static class InitCheckerEquations implements AdditionalEquations {
        
        private boolean called;

        @Override
        public String getName() {
            return null;
        }
        
        @Override
        public double[] computeDerivatives(SpacecraftState s, double[] pDot)
            throws OrekitException {
            return null;
        }
        
        public void initCheckerEquations() {
            boolean checker = false;            
        }
        
        public void init() {
            boolean checker = true;
        }
        
        public boolean wasCalled() {
            return called;
        }
               
    }

}
