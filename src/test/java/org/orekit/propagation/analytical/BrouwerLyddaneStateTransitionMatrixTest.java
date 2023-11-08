package org.orekit.propagation.analytical;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class BrouwerLyddaneStateTransitionMatrixTest {

    /** Orbit propagator. */
    private UnnormalizedSphericalHarmonicsProvider provider;

    /** Drag parameter. */
    private double M2;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere:potential/icgem-format");
        provider = GravityFieldFactory.getUnnormalizedProvider(5, 0);
        M2 = BrouwerLyddanePropagator.M2;
    }

    @Test
    public void testNullStmName() {
        Assertions.assertThrows(OrekitException.class, () -> {
            // Definition of initial conditions with position and velocity
            // ------------------------------------------------------------
            // e = 0.04152500499523033   and   i = 1.705015527659039

            AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
            Vector3D position = new Vector3D(3220103., 69623., 6149822.);
            Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

            Orbit initialOrbit = new CartesianOrbit(new PVCoordinates(position, velocity),
                    FramesFactory.getEME2000(), initDate, provider.getMu());

            final BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(initialOrbit, provider, M2);

            propagator.setupMatricesComputation(null, null, null);
        });
    }

    @Test
    public void testStateJacobian() {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // e = 0.04152500499523033   and   i = 1.705015527659039

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Vector3D position = new Vector3D(3220103., 69623., 6149822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        Orbit initialOrbit = new CartesianOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(), initDate, provider.getMu());

        // compute state Jacobian using PartialDerivatives
        BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(initialOrbit, provider, M2);
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitType.CARTESIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        final AbsoluteDate target = initialState.getDate().shiftedBy(initialState.getKeplerianPeriod());
        MatricesHarvester harvester = propagator.setupMatricesComputation("stm", null, null);
        final SpacecraftState finalState = propagator.propagate(target);
        RealMatrix dYdY0 = harvester.getStateTransitionMatrix(finalState);
        Assertions.assertEquals(OrbitType.CARTESIAN, harvester.getOrbitType());
        Assertions.assertEquals(PositionAngleType.MEAN, harvester.getPositionAngleType());

        // compute reference state Jacobian using finite differences
        double[][] dYdY0Ref = new double[6][6];
        BrouwerLyddanePropagator propagator2;
        double[] steps = NumericalPropagator.tolerances(10, initialState.getOrbit(), OrbitType.CARTESIAN)[0];
        for (int i = 0; i < 6; ++i) {
            propagator2 = new BrouwerLyddanePropagator(shiftState(initialState, OrbitType.CARTESIAN, -4 * steps[i], i).getOrbit(), provider, M2);
            SpacecraftState sM4h = propagator2.propagate(target);
            propagator2 = new BrouwerLyddanePropagator(shiftState(initialState, OrbitType.CARTESIAN, -3 * steps[i], i).getOrbit(), provider, M2);
            SpacecraftState sM3h = propagator2.propagate(target);
            propagator2 = new BrouwerLyddanePropagator(shiftState(initialState, OrbitType.CARTESIAN, -2 * steps[i], i).getOrbit(), provider, M2);
            SpacecraftState sM2h = propagator2.propagate(target);
            propagator2 = new BrouwerLyddanePropagator(shiftState(initialState, OrbitType.CARTESIAN, -1 * steps[i], i).getOrbit(), provider, M2);
            SpacecraftState sM1h = propagator2.propagate(target);
            propagator2 = new BrouwerLyddanePropagator(shiftState(initialState, OrbitType.CARTESIAN, +1 * steps[i], i).getOrbit(), provider, M2);
            SpacecraftState sP1h = propagator2.propagate(target);
            propagator2 = new BrouwerLyddanePropagator(shiftState(initialState, OrbitType.CARTESIAN, +2 * steps[i], i).getOrbit(), provider, M2);
            SpacecraftState sP2h = propagator2.propagate(target);
            propagator2 = new BrouwerLyddanePropagator(shiftState(initialState, OrbitType.CARTESIAN, +3 * steps[i], i).getOrbit(), provider, M2);
            SpacecraftState sP3h = propagator2.propagate(target);
            propagator2 = new BrouwerLyddanePropagator(shiftState(initialState, OrbitType.CARTESIAN, +4 * steps[i], i).getOrbit(), provider, M2);
            SpacecraftState sP4h = propagator2.propagate(target);
            fillJacobianColumn(dYdY0Ref, i, OrbitType.CARTESIAN, steps[i],
                               sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);
        }

        // Verify
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                if (stateVector[i] != 0) {
                    double error = FastMath.abs((dYdY0.getEntry(i, j) - dYdY0Ref[i][j]) / stateVector[i]) * steps[j];
                    Assertions.assertEquals(0, error, 1.42e-13);
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
