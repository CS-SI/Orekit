package org.orekit.propagation.analytical.tle;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class TLEParametersDerivativesTest {

    /** Spot 5 TLE. */
    private TLE tleSPOT;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        // SPOT TLE propagation will use SGP4
        String line1SPOT = "1 22823U 93061A   03339.49496229  .00000173  00000-0  10336-3 0   133";
        String line2SPOT = "2 22823  98.4132 359.2998 0017888 100.4310 259.8872 14.18403464527664";
        tleSPOT = new TLE(line1SPOT, line2SPOT);
    }

    @Test
    public void testBStarEstimation() {
        doTestParametersDerivatives(TLE.B_STAR, 5.16e-3, tleSPOT);
    }

    @Test
    public void testNoEstimatedParameters() {
        // compute state Jacobian using PartialDerivatives
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleSPOT);
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitType.CARTESIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        TLEHarvester harvester = (TLEHarvester) propagator.setupMatricesComputation("stm", null, null);
        harvester.freezeColumnsNames();
        RealMatrix dYdP = harvester.getParametersJacobian(initialState);
        Assertions.assertNull(dYdP);
    }

    private void doTestParametersDerivatives(String parameterName, double tolerance, TLE tle) {

        // compute state Jacobian using PartialDerivatives
        ParameterDriversList bound = new ParameterDriversList();

        for (final ParameterDriver driver : tle.getParametersDrivers()) {
            if (driver.getName().equals(parameterName)) {
                driver.setSelected(true);
                bound.add(driver);
            } else {
                driver.setSelected(false);
            }
        }
        // compute state Jacobian using PartialDerivatives
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final SpacecraftState initialState = propagator.getInitialState();
        final double[] stateVector = new double[6];
        OrbitType.CARTESIAN.mapOrbitToArray(initialState.getOrbit(), PositionAngleType.MEAN, stateVector, null);
        final AbsoluteDate target = initialState.getDate().shiftedBy(initialState.getKeplerianPeriod());
        TLEHarvester harvester = (TLEHarvester) propagator.setupMatricesComputation("stm", null, null);
        harvester.freezeColumnsNames();
        RealMatrix dYdP = harvester.getParametersJacobian(initialState);
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 1; ++j) {
                Assertions.assertEquals(0.0, dYdP.getEntry(i, j), tolerance);
            }
        }
        final SpacecraftState finalState = propagator.propagate(target);
        dYdP = harvester.getParametersJacobian(finalState);

        // compute reference Jacobian using finite differences
        OrbitType orbitType = OrbitType.CARTESIAN;
        TLEPropagator propagator2 = TLEPropagator.selectExtrapolator(tle);
        double[][] dYdPRef = new double[6][1];

        ParameterDriver selected = bound.getDrivers().get(0);
        double p0 = selected.getReferenceValue();
        double h  = selected.getScale();
        selected.setValue(p0 - 4 * h);
        propagator2 = TLEPropagator.selectExtrapolator(newTLE(tle, selected.getValue()));
        SpacecraftState sM4h = propagator2.propagate(target);
        selected.setValue(p0 - 3 * h);
        propagator2 = TLEPropagator.selectExtrapolator(newTLE(tle, selected.getValue()));
        SpacecraftState sM3h = propagator2.propagate(target);
        selected.setValue(p0 - 2 * h);
        propagator2 = TLEPropagator.selectExtrapolator(newTLE(tle, selected.getValue()));
        SpacecraftState sM2h = propagator2.propagate(target);
        selected.setValue(p0 - 1 * h);
        propagator2 = TLEPropagator.selectExtrapolator(newTLE(tle, selected.getValue()));
        SpacecraftState sM1h = propagator2.propagate(target);
        selected.setValue(p0 + 1 * h);
        propagator2 = TLEPropagator.selectExtrapolator(newTLE(tle, selected.getValue()));
        SpacecraftState sP1h = propagator2.propagate(target);
        selected.setValue(p0 + 2 * h);
        propagator2 = TLEPropagator.selectExtrapolator(newTLE(tle, selected.getValue()));
        SpacecraftState sP2h = propagator2.propagate(target);
        selected.setValue(p0 + 3 * h);
        propagator2 = TLEPropagator.selectExtrapolator(newTLE(tle, selected.getValue()));
        SpacecraftState sP3h = propagator2.propagate(target);
        selected.setValue(p0 + 4 * h);
        propagator2 = TLEPropagator.selectExtrapolator(newTLE(tle, selected.getValue()));
        SpacecraftState sP4h = propagator2.propagate(target);
        fillJacobianColumn(dYdPRef, 0, orbitType, h,
                           sM4h, sM3h, sM2h, sM1h, sP1h, sP2h, sP3h, sP4h);

        for (int i = 0; i < 6; ++i) {
            Assertions.assertEquals(dYdPRef[i][0], dYdP.getEntry(i, 0), FastMath.abs(tolerance));
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

    private TLE newTLE(final TLE template, final double newBStar) {
        return new TLE(template.getSatelliteNumber(), template.getClassification(),
                       template.getLaunchYear(), template.getLaunchNumber(), template.getLaunchPiece(),
                       template.getEphemerisType(), template.getElementNumber(), template.getDate(),
                       template.getMeanMotion(), template.getMeanMotionFirstDerivative(), template.getMeanMotionSecondDerivative(),
                       template.getE(), template.getI(), template.getPerigeeArgument(),
                       template.getRaan(), template.getMeanAnomaly(), template.getRevolutionNumberAtEpoch(),
                       newBStar);
    }

}
