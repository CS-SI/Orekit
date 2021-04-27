package org.orekit.propagation.analytical;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class EcksteinHechlerPartialDerivativesTest {

  private CartesianOrbit initialOrbit;
  
  private  EcksteinHechlerPropagator propagator;
    
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");    

        // orbit
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);


        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        initialOrbit = new CartesianOrbit(new PVCoordinates(position, velocity),
                                          FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);

        // propagator
        propagator = new EcksteinHechlerPropagator(initialOrbit,
                                                   Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                   Constants.EIGEN5C_EARTH_MU,
                                                   Constants.EIGEN5C_EARTH_C20,
                                                   Constants.EIGEN5C_EARTH_C30,
                                                   Constants.EIGEN5C_EARTH_C40,
                                                   Constants.EIGEN5C_EARTH_C50,
                                                   Constants.EIGEN5C_EARTH_C60);

    }
    
    @Test
    public void testStateJacobian() throws FileNotFoundException, UnsupportedEncodingException {
        doTestStateJacobian(5.0E-2, initialOrbit);
    }

    @Test(expected=OrekitException.class)
    public void testNotInitialized() {
        
        new EcksteinHechlerPartialDerivativesEquations("partials", propagator).getMapper();
     }
    
    @Test(expected=OrekitException.class)
    public void testTooSmallDimension() {
        
        EcksteinHechlerPartialDerivativesEquations partials = new EcksteinHechlerPartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(propagator.getInitialState(),
                                     new double[5][6]);
       
     }

    @Test(expected=OrekitException.class)
    public void testTooLargeDimension() {
        
        EcksteinHechlerPartialDerivativesEquations partials = new EcksteinHechlerPartialDerivativesEquations("partials", propagator);
        partials.setInitialJacobians(propagator.getInitialState(),
                                     new double[8][6]);
       
     }
    
    
    private void doTestStateJacobian(double tolerance, CartesianOrbit initialOrbit)
                    throws FileNotFoundException, UnsupportedEncodingException {
                           
                    double dt = Constants.JULIAN_DAY;

                    // compute state Jacobian using PartialDerivatives
                    
                    // propagator
                    EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialOrbit,
                                                                                         Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                                         Constants.EIGEN5C_EARTH_MU,
                                                                                         Constants.EIGEN5C_EARTH_C20,
                                                                                         Constants.EIGEN5C_EARTH_C30,
                                                                                         Constants.EIGEN5C_EARTH_C40,
                                                                                         Constants.EIGEN5C_EARTH_C50,
                                                                                         Constants.EIGEN5C_EARTH_C60);
                    final SpacecraftState initialState = propagator.getInitialState();
                    final AbsoluteDate target = initialState.getDate().shiftedBy(dt);
                    CartesianOrbit orbit = (CartesianOrbit) OrbitType.CARTESIAN.convertType(propagator.getInitialState().getOrbit());
                    EcksteinHechlerPartialDerivativesEquations partials = new EcksteinHechlerPartialDerivativesEquations("partials", propagator);
                    final SpacecraftState initState = partials.setInitialJacobians(propagator.getInitialState());
                    final double[] stateVector = new double[6];
                    OrbitType.CARTESIAN.mapOrbitToArray(initState.getOrbit(), PositionAngle.TRUE, stateVector, null);
                    final EcksteinHechlerJacobiansMapper mapper = partials.getMapper();
                    double[][] dYdY0 =  new double[EcksteinHechlerGradientConverter.FREE_STATE_PARAMETERS][EcksteinHechlerGradientConverter.FREE_STATE_PARAMETERS];
                    propagator.resetInitialState(initState);
                    mapper.analyticalDerivatives(propagator.propagate(target));
                    mapper.getStateJacobian(initState, dYdY0);

                    // compute reference state Jacobian using finite differences
                    double[][] dYdY0Ref = new double[6][6];
                    EcksteinHechlerPropagator propagator2;
                    double[] steps = NumericalPropagator.tolerances(10, orbit, OrbitType.CARTESIAN)[0];
                    for (int i = 0; i < 6; ++i) {
                        propagator2 = new EcksteinHechlerPropagator(shiftState(initialState, OrbitType.CARTESIAN, -4 * steps[i], i).getOrbit(),
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,
                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30,
                                                                    Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50,
                                                                    Constants.EIGEN5C_EARTH_C60);
                        SpacecraftState sM4h = propagator2.propagate(target);
                        propagator2 = new EcksteinHechlerPropagator(shiftState(initialState, OrbitType.CARTESIAN, -3 * steps[i], i).getOrbit(),
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,
                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30,
                                                                    Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50,
                                                                    Constants.EIGEN5C_EARTH_C60);
                        SpacecraftState sM3h = propagator2.propagate(target);
                        propagator2 = new EcksteinHechlerPropagator(shiftState(initialState, OrbitType.CARTESIAN, -2 * steps[i], i).getOrbit(),
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,
                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30,
                                                                    Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50,
                                                                    Constants.EIGEN5C_EARTH_C60);
                        SpacecraftState sM2h = propagator2.propagate(target);
                        propagator2 = new EcksteinHechlerPropagator(shiftState(initialState, OrbitType.CARTESIAN, -1 * steps[i], i).getOrbit(),
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,
                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30,
                                                                    Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50,
                                                                    Constants.EIGEN5C_EARTH_C60);
                        SpacecraftState sM1h = propagator2.propagate(target);
                        propagator2 = new EcksteinHechlerPropagator(shiftState(initialState, OrbitType.CARTESIAN, +1 * steps[i], i).getOrbit(),
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,
                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30,
                                                                    Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50,
                                                                    Constants.EIGEN5C_EARTH_C60);
                        SpacecraftState sP1h = propagator2.propagate(target);
                        propagator2 = new EcksteinHechlerPropagator(shiftState(initialState, OrbitType.CARTESIAN, +2 * steps[i], i).getOrbit(),
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,
                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30,
                                                                    Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50,
                                                                    Constants.EIGEN5C_EARTH_C60);
                        SpacecraftState sP2h = propagator2.propagate(target);
                        propagator2 = new EcksteinHechlerPropagator(shiftState(initialState, OrbitType.CARTESIAN, +3 * steps[i], i).getOrbit(),
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,
                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30,
                                                                    Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50,
                                                                    Constants.EIGEN5C_EARTH_C60);
                        SpacecraftState sP3h = propagator2.propagate(target);
                        propagator2 = new EcksteinHechlerPropagator(shiftState(initialState, OrbitType.CARTESIAN, +4 * steps[i], i).getOrbit(),
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,
                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30,
                                                                    Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50,
                                                                    Constants.EIGEN5C_EARTH_C60);
                        SpacecraftState sP4h = propagator2.propagate(target);
                        fillJacobianColumn(dYdY0Ref, i, OrbitType.CARTESIAN, steps[i],
                                           sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
                    }

                    for (int i = 0; i < 6; ++i) {
                        for (int j = 0; j < 6; ++j) {
                            if (stateVector[i] != 0) {
                                double error = FastMath.abs((dYdY0[i][j] - dYdY0Ref[i][j]) / stateVector[i]) * steps[j];
                                Assert.assertEquals(0, error, tolerance);
                            }
                        }
                    }
                }
    
    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, double h,
                                    SpacecraftState sM4h, SpacecraftState sM3h,
                                    SpacecraftState sM2h, SpacecraftState sM1h,
                                    SpacecraftState sP1h, SpacecraftState sP2h,
                                    SpacecraftState sP3h, SpacecraftState sP4h) {
        double[] aM4h = stateToArray(sM4h, orbitType)[0];
        double[] aM3h = stateToArray(sM3h, orbitType)[0];
        double[] aM2h = stateToArray(sM2h, orbitType)[0];
        double[] aM1h = stateToArray(sM1h, orbitType)[0];
        double[] aP1h = stateToArray(sP1h, orbitType)[0];
        double[] aP2h = stateToArray(sP2h, orbitType)[0];
        double[] aP3h = stateToArray(sP3h, orbitType)[0];
        double[] aP4h = stateToArray(sP4h, orbitType)[0];
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (aP4h[i] - aM4h[i]) +
                                    32 * (aP3h[i] - aM3h[i]) -
                                   168 * (aP2h[i] - aM2h[i]) +
                                   672 * (aP1h[i] - aM1h[i])) / (840 * h);
        }
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType);
        array[0][column] += delta;

        return arrayToState(array, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }
    
    
    
    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType) {
          double[][] array = new double[2][6];

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngle.TRUE, array[0], array[1]);
          return array;
      }


    private SpacecraftState arrayToState(double[][] array, 
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
        CartesianOrbit orbit = (CartesianOrbit) OrbitType.CARTESIAN.mapArrayToOrbit(array[0], array[1], PositionAngle.TRUE, date, mu, frame);
        return new SpacecraftState(orbit, attitude);
    }
    


}
