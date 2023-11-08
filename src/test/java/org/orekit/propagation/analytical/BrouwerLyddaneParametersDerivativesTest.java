package org.orekit.propagation.analytical;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class BrouwerLyddaneParametersDerivativesTest {

    /** Orbit propagator. */
    private UnnormalizedSphericalHarmonicsProvider provider;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere:potential/icgem-format");
        provider = GravityFieldFactory.getUnnormalizedProvider(5, 0);
    }

    @Test
    public void testNoEstimatedParameters() {
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // e = 0.04152500499523033   and   i = 1.705015527659039

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Vector3D position = new Vector3D(3220103., 69623., 6149822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        Orbit initialOrbit = new CartesianOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(), initDate, provider.getMu());
        // compute state Jacobian using PartialDerivatives
        final BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(initialOrbit, provider, 1.0e-14);
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitType.CARTESIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        BrouwerLyddaneHarvester harvester = (BrouwerLyddaneHarvester) propagator.setupMatricesComputation("stm", null, null);
        harvester.freezeColumnsNames();
        RealMatrix dYdP = harvester.getParametersJacobian(initialState);
        Assertions.assertNull(dYdP);
    }

    @Test
    public void testParametersDerivatives() {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // e = 0.04152500499523033   and   i = 1.705015527659039

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Vector3D position = new Vector3D(3220103., 69623., 6149822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        Orbit initialOrbit = new CartesianOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(), initDate, provider.getMu());

        // Brouwer-Lyddane orbit propagator
        final BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(initialOrbit, provider, 1.0e-14);

        // compute state Jacobian using PartialDerivatives
        ParameterDriversList bound = new ParameterDriversList();
        final ParameterDriver M2 = propagator.getParametersDrivers().get(0);
        M2.setSelected(true);
        bound.add(M2);

        // Compute parameter Jacobian using Harvester
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitType.CARTESIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        final AbsoluteDate target = initialState.getDate().shiftedBy(initialState.getKeplerianPeriod());
        BrouwerLyddaneHarvester harvester = (BrouwerLyddaneHarvester) propagator.setupMatricesComputation("stm", null, null);
        harvester.freezeColumnsNames();
        final SpacecraftState finalState = propagator.propagate(target);
        RealMatrix dYdP = harvester.getParametersJacobian(finalState);

        // compute reference Jacobian using finite differences
        OrbitType orbitType = OrbitType.CARTESIAN;
        BrouwerLyddanePropagator propagator2;
        double[][] dYdPRef = new double[6][1];

        ParameterDriver selected = bound.getDrivers().get(0);
        double p0 = selected.getReferenceValue();
        double h  = selected.getScale();
        selected.setValue(p0 - 4 * h);
        propagator2 = new BrouwerLyddanePropagator(initialOrbit, provider, selected.getValue());
        SpacecraftState sM4h = propagator2.propagate(target);
        selected.setValue(p0 - 3 * h);
        propagator2 = new BrouwerLyddanePropagator(initialOrbit, provider, selected.getValue());
        SpacecraftState sM3h = propagator2.propagate(target);
        selected.setValue(p0 - 2 * h);
        propagator2 = new BrouwerLyddanePropagator(initialOrbit, provider, selected.getValue());
        SpacecraftState sM2h = propagator2.propagate(target);
        selected.setValue(p0 - 1 * h);
        propagator2 = new BrouwerLyddanePropagator(initialOrbit, provider, selected.getValue());
        SpacecraftState sM1h = propagator2.propagate(target);
        selected.setValue(p0 + 1 * h);
        propagator2 = new BrouwerLyddanePropagator(initialOrbit, provider, selected.getValue());
        SpacecraftState sP1h = propagator2.propagate(target);
        selected.setValue(p0 + 2 * h);
        propagator2 = new BrouwerLyddanePropagator(initialOrbit, provider, selected.getValue());
        SpacecraftState sP2h = propagator2.propagate(target);
        selected.setValue(p0 + 3 * h);
        propagator2 = new BrouwerLyddanePropagator(initialOrbit, provider, selected.getValue());
        SpacecraftState sP3h = propagator2.propagate(target);
        selected.setValue(p0 + 4 * h);
        propagator2 = new BrouwerLyddanePropagator(initialOrbit, provider, selected.getValue());
        SpacecraftState sP4h = propagator2.propagate(target);
        fillJacobianColumn(dYdPRef, 0, orbitType, h,
                           sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);

        for (int i = 0; i < 6; ++i) {
            Assertions.assertEquals(0.0, (dYdPRef[i][0] - dYdP.getEntry(i, 0)) / dYdPRef[i][0], 1.06e-12);
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

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType) {
          double[][] array = new double[2][6];

          orbitType.mapOrbitToArray(state.getOrbit(), PositionAngleType.MEAN, array[0], array[1]);
          return array;
      }

}
