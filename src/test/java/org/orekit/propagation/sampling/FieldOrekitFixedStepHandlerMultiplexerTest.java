package org.orekit.propagation.sampling;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class FieldOrekitFixedStepHandlerMultiplexerTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testMultiplexer() {
        doTestMultiplexer(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestMultiplexer(final Field<T> field) {

        T zero = field.getZero();

        // init
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2020, 2, 28, 16, 15, 0.0, TimeScalesFactory.getUTC());

        FieldInitCheckerHandler<T> initHandler = new FieldInitCheckerHandler<>(1.0);
        FieldIncrementationHandler<T> incrementationHandler = new FieldIncrementationHandler<>();

        FieldOrekitFixedStepHandlerMultiplexer<T> handler = new FieldOrekitFixedStepHandlerMultiplexer<>();
        handler.add(initHandler);
        handler.add(incrementationHandler);

        FieldOrbit<T> ic = new FieldKeplerianOrbit<>(zero.add(6378137 + 500e3), zero.add(1e-3), zero, zero, zero, zero,
                PositionAngle.TRUE, FramesFactory.getGCRF(), initDate, zero.add(Constants.WGS84_EARTH_MU));
        FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(ic);
        propagator.setMasterMode(zero.add(60), handler);

        Assert.assertFalse(initHandler.isInitialized());
        Assert.assertEquals(1.0, initHandler.getExpected(), Double.MIN_VALUE);

        propagator.propagate(initDate.shiftedBy(90.0));

        // verify
        Assert.assertTrue(initHandler.isInitialized());
        Assert.assertEquals(2.0, initHandler.getExpected(), Double.MIN_VALUE);
        Assert.assertEquals(3, incrementationHandler.getValue());
    }

    private class FieldInitCheckerHandler<T extends RealFieldElement<T>> implements FieldOrekitFixedStepHandler<T> {

        private double expected;
        private boolean initialized;

        public FieldInitCheckerHandler(final double expected) {
            this.expected    = expected;
            this.initialized = false;
        }

        @Override
        public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t, T step) {
            initialized = true;
        }

        @Override
        public void handleStep(FieldSpacecraftState<T> currentState, boolean isLast) {
            this.expected = 2.0;
        }

        boolean isInitialized() {
            return initialized;
        }

        double getExpected() {
            return expected;
        }

    }

    private class FieldIncrementationHandler<T extends RealFieldElement<T>> implements FieldOrekitFixedStepHandler<T> {

        private int value;

        public FieldIncrementationHandler() {
            // Do nothing
        }

        @Override
        public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t, T step) {
            this.value = 1;
        }

        @Override
        public void handleStep(FieldSpacecraftState<T> currentState, boolean isLast) {
            this.value++;
        }

        int getValue() {
            return value;
        }

    }

}
