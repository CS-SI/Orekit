package org.orekit.propagation.sampling;

import org.hipparchus.Field;
import org.hipparchus.util.Decimal64;
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

public class FieldStepHandlerMultiplexerTest {

    FieldAbsoluteDate<Decimal64> initDate;
    FieldPropagator<Decimal64> propagator;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        Field<Decimal64> field = Decimal64Field.getInstance();
        Decimal64        zero  = field.getZero();
        initDate = new FieldAbsoluteDate<>(field, 2020, 2, 28, 16, 15, 0.0, TimeScalesFactory.getUTC());
        FieldOrbit<Decimal64> ic = new FieldKeplerianOrbit<>(zero.add(6378137 + 500e3), zero.add(1e-3), zero, zero, zero, zero,
                                                             PositionAngle.TRUE, FramesFactory.getGCRF(), initDate,
                                                             zero.add(Constants.WGS84_EARTH_MU));
        propagator = new FieldKeplerianPropagator<>(ic);
    }

    @Test
    public void testFixedStep() {

        Field<Decimal64> field = Decimal64Field.getInstance();
        Decimal64        zero  = field.getZero();

        FieldStepHandlerMultiplexer<Decimal64> multiplexer = new FieldStepHandlerMultiplexer<>();
        propagator.setMasterMode(multiplexer);

        FieldInitCheckerHandler initHandler = new FieldInitCheckerHandler(1.0);
        FieldIncrementationHandler incrementation60Handler = new FieldIncrementationHandler();
        FieldIncrementationHandler incrementation10Handler = new FieldIncrementationHandler();

        multiplexer.add(zero.newInstance(60.0), initHandler);
        multiplexer.add(zero.newInstance(60.0), incrementation60Handler);
        multiplexer.add(zero.newInstance(10.0), incrementation10Handler);

        Assert.assertFalse(initHandler.isInitialized());
        Assert.assertEquals(1.0, initHandler.getExpected(), Double.MIN_VALUE);

        propagator.propagate(initDate.shiftedBy(90.0));

        // verify
        Assert.assertTrue(initHandler.isInitialized());

        // init called once
        // handleStep called at t₀, t₀ + 60
        // finish called at t₁
        Assert.assertEquals( 4,  incrementation60Handler.getValue());

        // init called once
        // handleStep called at t₀, t₀ + 10, t₀ + 20, t₀ + 30, t₀ + 40, t₀ + 50, t₀ + 60, t₀ + 70, t₀ + 80, t₀ + 90
        // finish called at t₁
        Assert.assertEquals(12,  incrementation10Handler.getValue());

    }

    @Test
    public void testRemove() {

        FieldStepHandlerMultiplexer<Decimal64> multiplexer = new FieldStepHandlerMultiplexer<>();
        propagator.setMasterMode(multiplexer);

        Field<Decimal64> field = Decimal64Field.getInstance();
        Decimal64        zero  = field.getZero();

        FieldIncrementationHandler incrementation60Handler = new FieldIncrementationHandler();
        FieldIncrementationHandler incrementation10Handler = new FieldIncrementationHandler();

        multiplexer.add(zero.newInstance(60.0), incrementation60Handler);
        multiplexer.add(zero.newInstance(10.0), incrementation10Handler);

        // first run with both handlers
        propagator.propagate(initDate.shiftedBy(90.0));
        Assert.assertEquals( 4,  incrementation60Handler.getValue());
        Assert.assertEquals(12,  incrementation10Handler.getValue());

        // removing the handler at 10 seconds
        multiplexer.remove(incrementation10Handler);
        propagator.propagate(initDate, initDate.shiftedBy(90.0));
        Assert.assertEquals( 8,  incrementation60Handler.getValue());
        Assert.assertEquals(12,  incrementation10Handler.getValue());

        // attempting to remove a handler already removed
        multiplexer.remove(incrementation10Handler);
        propagator.propagate(initDate, initDate.shiftedBy(90.0));
        Assert.assertEquals(12,  incrementation60Handler.getValue());
        Assert.assertEquals(12,  incrementation10Handler.getValue());
        
        // removing everything
        multiplexer.clear();
        propagator.propagate(initDate, initDate.shiftedBy(90.0));
        Assert.assertEquals(12,  incrementation60Handler.getValue());
        Assert.assertEquals(12,  incrementation10Handler.getValue());
        
    }

    private class FieldInitCheckerHandler implements FieldOrekitFixedStepHandler<Decimal64> {

        private double expected;
        private boolean initialized;

        public FieldInitCheckerHandler(final double expected) {
            this.expected    = expected;
            this.initialized = false;
        }

        @Override
        public void init(FieldSpacecraftState<Decimal64> s0, FieldAbsoluteDate<Decimal64> t, Decimal64 step) {
            initialized = true;
        }

        @Override
        public void handleStep(FieldSpacecraftState<Decimal64> currentState) {
            this.expected = 2.0;
        }

        boolean isInitialized() {
            return initialized;
        }

        double getExpected() {
            return expected;
        }

    }

    private class FieldIncrementationHandler implements FieldOrekitFixedStepHandler<Decimal64> {

        private int value;

        public FieldIncrementationHandler() {
            this.value = 0;
        }

        @Override
        public void init(FieldSpacecraftState<Decimal64> s0, FieldAbsoluteDate<Decimal64> t, Decimal64 step) {
            this.value++;
        }

        @Override
        public void handleStep(FieldSpacecraftState<Decimal64> currentState) {
            this.value++;
        }

        @Override
        public void finish(FieldSpacecraftState<Decimal64> finalState) {
            this.value++;
        }

        int getValue() {
            return value;
        }

    }

}
