package org.orekit.propagation.analytical.tle;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

public class TLEStateTransitionMatrixTest {

    // build two TLEs in order to test SGP4 and SDP4 algorithms
    private TLE tleGPS;
    private TLE tleSPOT;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");

        // GPS TLE propagation will use SDP4
        String line1GPS = "1 11783U 80032A   03300.87313441  .00000062  00000-0  10000-3 0  6416";
        String line2GPS = "2 11783  62.0472 164.2367 0320924  39.0039 323.3716  2.03455768173530";
        tleGPS = new TLE(line1GPS, line2GPS);

        // SPOT TLE propagation will use SGP4
        String line1SPOT = "1 22823U 93061A   03339.49496229  .00000173  00000-0  10336-3 0   133";
        String line2SPOT = "2 22823  98.4132 359.2998 0017888 100.4310 259.8872 14.18403464527664";
        tleSPOT = new TLE(line1SPOT, line2SPOT);
    }

    @Test
    public void testPropagationSGP4() {
        doTestStateJacobian(7.65e-10, tleSPOT);
    }

    @Test
    public void testPropagationSDP4() {
        doTestStateJacobian(2.53e-9, tleGPS);
    }

    @Test
    public void testNullStmName() {
        Assertions.assertThrows(OrekitException.class, () -> {
            TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleSPOT);
            propagator.setupMatricesComputation(null, null, null);
        });
    }

    private void doTestStateJacobian(double tolerance, TLE tle) {

        // compute state Jacobian using PartialDerivatives
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitType.CARTESIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        final AbsoluteDate target = initialState.getDate().shiftedBy(initialState.getKeplerianPeriod());
        MatricesHarvester harvester = propagator.setupMatricesComputation("stm", null, null);
        RealMatrix dYdY0 = harvester.getStateTransitionMatrix(initialState);
        Assertions.assertNull(dYdY0);
        final SpacecraftState finalState = propagator.propagate(target);
        dYdY0 = harvester.getStateTransitionMatrix(finalState);

        // TLE generation algorithm
        TleGenerationAlgorithm algorithm = new FixedPointTleGenerationAlgorithm();

        // compute reference state Jacobian using finite differences
        double[][] dYdY0Ref = new double[6][6];
        TLEPropagator propagator2;
        double[] steps = NumericalPropagator.tolerances(10, initialState.getOrbit(), OrbitType.CARTESIAN)[0];
        for (int i = 0; i < 6; ++i) {
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitType.CARTESIAN, -4 * steps[i], i), tle));
            SpacecraftState sM4h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitType.CARTESIAN, -3 * steps[i], i), tle));
            SpacecraftState sM3h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitType.CARTESIAN, -2 * steps[i], i), tle));
            SpacecraftState sM2h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitType.CARTESIAN, -1 * steps[i], i), tle));
            SpacecraftState sM1h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitType.CARTESIAN, +1 * steps[i], i), tle));
            SpacecraftState sP1h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitType.CARTESIAN, +2 * steps[i], i), tle));
            SpacecraftState sP2h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitType.CARTESIAN, +3 * steps[i], i), tle));
            SpacecraftState sP3h = propagator2.propagate(target);
            propagator2 = TLEPropagator.selectExtrapolator(algorithm.generate(shiftState(initialState, OrbitType.CARTESIAN, +4 * steps[i], i), tle));
            SpacecraftState sP4h = propagator2.propagate(target);
            fillJacobianColumn(dYdY0Ref, i, OrbitType.CARTESIAN, steps[i],
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
        }

        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                if (stateVector[i] != 0) {
                    double error = FastMath.abs((dYdY0.getEntry(i, j) - dYdY0Ref[i][j]) / stateVector[i]) * steps[j];
                    Assertions.assertEquals(0, error, tolerance);
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

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngleType.MEAN, array[0], array[1]);
          return array;
      }


    private SpacecraftState arrayToState(double[][] array,
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
        CartesianOrbit orbit = (CartesianOrbit) OrbitType.CARTESIAN.mapArrayToOrbit(array[0], array[1], PositionAngleType.MEAN, date, mu, frame);
        return new SpacecraftState(orbit, attitude);
    }

}
