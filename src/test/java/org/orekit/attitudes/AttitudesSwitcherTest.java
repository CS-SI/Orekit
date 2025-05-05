package org.orekit.attitudes;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.*;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class AttitudesSwitcherTest {

    @Test
    void testAddSwitchingConditionUndetectable() {
        // GIVEN
        final Orbit orbit = getInitialOrbit();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final AttitudesSwitcher attitudesSwitcher = new AttitudesSwitcher();
        final AttitudeProvider pastProvider = new FrameAlignedProvider(orbit.getFrame());
        final AbsoluteDate switchingDate = orbit.getDate().shiftedBy(1e4);
        final AttitudeProvider futureProvider = new LofOffset(orbit.getFrame(), LOFType.TNW);
        final DateDetector detector = new DateDetector(switchingDate);
        // WHEN
        attitudesSwitcher.resetActiveProvider(pastProvider);
        final TestSwitchHandler switchHandler = new TestSwitchHandler();
        attitudesSwitcher.addSwitchingCondition(pastProvider, futureProvider, detector,
                false, false, switchHandler);
        propagator.setAttitudeProvider(attitudesSwitcher);
        final SpacecraftState initialState = new SpacecraftState(orbit,
                attitudesSwitcher.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        propagator.resetInitialState(initialState);
        propagator.propagate(switchingDate.shiftedBy(1e2));
        Assertions.assertEquals(0, switchHandler.count);
    }

    @ParameterizedTest
    @ValueSource(doubles = { -1e3, 1e3 })
    void testAddSwitchingCondition(final double timeShift) {
        // GIVEN
        final Orbit orbit = getInitialOrbit();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final AttitudesSwitcher attitudesSwitcher = new AttitudesSwitcher();
        final AttitudeProvider pastProvider = new FrameAlignedProvider(orbit.getFrame());
        final AbsoluteDate switchingDate = orbit.getDate().shiftedBy(timeShift);
        final AttitudeProvider futureProvider = new LofOffset(orbit.getFrame(), LOFType.TNW);
        final DateDetector detector = new DateDetector(switchingDate);
        final boolean isForward = (timeShift > 0);
        // WHEN
        if (isForward) {
            attitudesSwitcher.resetActiveProvider(pastProvider);
            attitudesSwitcher.addSwitchingCondition(pastProvider, futureProvider, detector,
                    true, true, null);
        } else {
            attitudesSwitcher.resetActiveProvider(futureProvider);
            attitudesSwitcher.addSwitchingCondition(pastProvider, futureProvider, detector,
                    true, true, null);
        }
        propagator.setAttitudeProvider(attitudesSwitcher);
        final SpacecraftState initialState = new SpacecraftState(orbit,
                attitudesSwitcher.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        propagator.resetInitialState(initialState);
        // THEN
        for (double step = 0; FastMath.abs(step) < FastMath.abs(timeShift) * 2; step += timeShift / 10) {
            final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(step));
            final Attitude attitude = state.getAttitude();
            final AttitudeProvider expectedProvider;
            if (FastMath.abs(step) < FastMath.abs(timeShift)) {
                expectedProvider = isForward ? pastProvider : futureProvider;
            } else {
                expectedProvider = isForward ? futureProvider : pastProvider;
            }
            if (FastMath.abs(step - timeShift) > 1e-1) {
                final Attitude expectedAttitude = expectedProvider.getAttitude(state.getOrbit(), state.getDate(), state.getFrame());
                Assertions.assertEquals(0., Rotation.distance(expectedAttitude.getRotation(),
                        attitude.getRotation()));
                Assertions.assertEquals(expectedAttitude.getSpin(), attitude.getSpin());
                Assertions.assertEquals(expectedAttitude.getRotationAcceleration(), attitude.getRotationAcceleration());
            }
        }
    }

    private static Orbit getInitialOrbit() {
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 1, 1, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        return new KeplerianOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), initialDate,
                Constants.EIGEN5C_EARTH_MU);
    }

    @ParameterizedTest
    @ValueSource(doubles = { -1e3, 1e3 })
    void testAddSwitchingConditionField(final double timeShift) {
        // GIVEN
        final Orbit orbit = getInitialOrbit();
        final Binary64Field field = Binary64Field.getInstance();
        final FieldOrbit<Binary64> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(fieldOrbit);
        final AttitudesSwitcher attitudesSwitcher = new AttitudesSwitcher();
        final AttitudeProvider pastProvider = new FrameAlignedProvider(orbit.getFrame());
        final AbsoluteDate switchingDate = orbit.getDate().shiftedBy(timeShift);
        final AttitudeProvider futureProvider = new LofOffset(orbit.getFrame(), LOFType.TNW);
        final DateDetector detector = new DateDetector(switchingDate);
        final boolean isForward = (timeShift > 0);
        final TestSwitchHandler switchHandler = new TestSwitchHandler();
        // WHEN
        if (!isForward) {
            attitudesSwitcher.resetActiveProvider(futureProvider);
        }
        attitudesSwitcher.addSwitchingCondition(pastProvider, futureProvider, detector,
                true, false, switchHandler);
        propagator.setAttitudeProvider(attitudesSwitcher);
        propagator.propagate(fieldOrbit.getDate().shiftedBy(timeShift * 2));
        // THEN
        Assertions.assertEquals(1, switchHandler.count);
        Assertions.assertEquals(switchingDate, switchHandler.lastSwitchDate);
    }

    private static class TestSwitchHandler implements AttitudeSwitchHandler {
        private int count = 0;
        private AbsoluteDate lastSwitchDate;

        @Override
        public void switchOccurred(AttitudeProvider preceding, AttitudeProvider following, SpacecraftState state) {
            count++;
            lastSwitchDate = state.getDate();
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }
}
