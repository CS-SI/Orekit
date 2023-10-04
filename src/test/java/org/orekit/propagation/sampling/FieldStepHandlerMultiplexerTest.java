package org.orekit.propagation.sampling;

import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class FieldStepHandlerMultiplexerTest {

    FieldAbsoluteDate<Binary64> initDate;
    FieldPropagator<Binary64> propagator;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        Field<Binary64> field = Binary64Field.getInstance();
        Binary64        zero  = field.getZero();
        initDate = new FieldAbsoluteDate<>(field, 2020, 2, 28, 16, 15, 0.0, TimeScalesFactory.getUTC());
        FieldOrbit<Binary64> ic = new FieldKeplerianOrbit<>(zero.add(6378137 + 500e3), zero.add(1e-3), zero, zero, zero, zero,
                                                             PositionAngleType.TRUE, FramesFactory.getGCRF(), initDate,
                                                             zero.add(Constants.WGS84_EARTH_MU));
        propagator = new FieldKeplerianPropagator<>(ic);
    }

    @AfterEach
    public void tearDown() {
        initDate   = null;
        propagator = null;
    }

    @Test
    public void testMixedSteps() {

        Field<Binary64> field = Binary64Field.getInstance();
        Binary64        zero  = field.getZero();

        FieldStepHandlerMultiplexer<Binary64> multiplexer = propagator.getMultiplexer();

        FieldInitCheckerHandler initHandler = new FieldInitCheckerHandler(1.0);
        FieldFixedCounter    counter60  = new FieldFixedCounter();
        FieldVariableCounter counterVar = new FieldVariableCounter();
        FieldFixedCounter    counter10  = new FieldFixedCounter();

        multiplexer.add(zero.newInstance(60.0), initHandler);
        multiplexer.add(zero.newInstance(60.0), counter60);
        multiplexer.add(counterVar);
        multiplexer.add(zero.newInstance(10.0), counter10);
        Assertions.assertEquals(4, multiplexer.getHandlers().size());

        Assertions.assertFalse(initHandler.isInitialized());
        Assertions.assertEquals(1.0, initHandler.getExpected(), Double.MIN_VALUE);

        propagator.propagate(initDate.shiftedBy(90.0));

        // verify
        Assertions.assertTrue(initHandler.isInitialized());
        Assertions.assertEquals(2.0, initHandler.getExpected(), Double.MIN_VALUE);

        Assertions.assertEquals( 1,  counter60.initCount);
        Assertions.assertEquals( 2,  counter60.handleCount);
        Assertions.assertEquals( 1,  counter60.finishCount);

        Assertions.assertEquals( 1,  counterVar.initCount);
        Assertions.assertEquals( 1,  counterVar.handleCount);
        Assertions.assertEquals( 1,  counterVar.finishCount);

        Assertions.assertEquals( 1,  counter10.initCount);
        Assertions.assertEquals(10,  counter10.handleCount);
        Assertions.assertEquals( 1,  counter10.finishCount);

    }

    @Test
    public void testRemove() {

        FieldStepHandlerMultiplexer<Binary64> multiplexer = propagator.getMultiplexer();

        Field<Binary64> field = Binary64Field.getInstance();
        Binary64        zero  = field.getZero();

        FieldFixedCounter    counter60  = new FieldFixedCounter();
        FieldVariableCounter counterVar = new FieldVariableCounter();
        FieldFixedCounter    counter10  = new FieldFixedCounter();

        multiplexer.add(zero.newInstance(60.0), counter60);
        multiplexer.add(counterVar);
        multiplexer.add(zero.newInstance(10.0), counter10);
        Assertions.assertEquals(3, multiplexer.getHandlers().size());
        Assertions.assertTrue(((FieldOrekitStepNormalizer<Binary64>) multiplexer.getHandlers().get(0)).getFixedStepHandler() instanceof FieldFixedCounter);
        Assertions.assertEquals(60.0, ((FieldOrekitStepNormalizer<Binary64>) multiplexer.getHandlers().get(0)).getFixedTimeStep().getReal(), 1.0e-15);
        Assertions.assertTrue(((FieldOrekitStepNormalizer<Binary64>) multiplexer.getHandlers().get(2)).getFixedStepHandler() instanceof FieldFixedCounter);
        Assertions.assertEquals(10.0, ((FieldOrekitStepNormalizer<Binary64>) multiplexer.getHandlers().get(2)).getFixedTimeStep().getReal(), 1.0e-15);

        // first run with all handlers
        propagator.propagate(initDate.shiftedBy(90.0));
        Assertions.assertEquals( 1,    counter60.initCount);
        Assertions.assertEquals( 2,    counter60.handleCount);
        Assertions.assertEquals( 1,    counter60.finishCount);
        Assertions.assertEquals(  0.0, counter60.start, 1.0e-15);
        Assertions.assertEquals( 90.0, counter60.stop, 1.0e-15);
        Assertions.assertEquals( 1,    counterVar.initCount);
        Assertions.assertEquals( 1,    counterVar.handleCount);
        Assertions.assertEquals( 1,    counterVar.finishCount);
        Assertions.assertEquals(  0.0, counterVar.start, 1.0e-15);
        Assertions.assertEquals( 90.0, counterVar.stop, 1.0e-15);
        Assertions.assertEquals( 1,    counter10.initCount);
        Assertions.assertEquals(10,    counter10.handleCount);
        Assertions.assertEquals( 1,    counter10.finishCount);
        Assertions.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assertions.assertEquals( 90.0, counter10.stop, 1.0e-15);

        // removing the handler at 10 seconds
        multiplexer.remove(counter10);
        Assertions.assertEquals(2, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(100.0), initDate.shiftedBy(190.0));
        Assertions.assertEquals( 2,    counter60.initCount);
        Assertions.assertEquals( 4,    counter60.handleCount);
        Assertions.assertEquals( 2,    counter60.initCount);
        Assertions.assertEquals(100.0, counter60.start, 1.0e-15);
        Assertions.assertEquals(190.0, counter60.stop, 1.0e-15);
        Assertions.assertEquals( 2,    counterVar.initCount);
        Assertions.assertEquals( 2,    counterVar.handleCount);
        Assertions.assertEquals( 2,    counterVar.finishCount);
        Assertions.assertEquals(100.0, counterVar.start, 1.0e-15);
        Assertions.assertEquals(190.0, counterVar.stop, 1.0e-15);
        Assertions.assertEquals( 1,    counter10.initCount);
        Assertions.assertEquals(10,    counter10.handleCount);
        Assertions.assertEquals( 1,    counter10.initCount);
        Assertions.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assertions.assertEquals( 90.0, counter10.stop, 1.0e-15);

        // attempting to remove a handler already removed
        multiplexer.remove(counter10);
        Assertions.assertEquals(2, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(200.0), initDate.shiftedBy(290.0));
        Assertions.assertEquals( 3,    counter60.initCount);
        Assertions.assertEquals( 6,    counter60.handleCount);
        Assertions.assertEquals( 3,    counter60.finishCount);
        Assertions.assertEquals(200.0, counter60.start, 1.0e-15);
        Assertions.assertEquals(290.0, counter60.stop, 1.0e-15);
        Assertions.assertEquals( 3,    counterVar.initCount);
        Assertions.assertEquals( 3,    counterVar.handleCount);
        Assertions.assertEquals( 3,    counterVar.finishCount);
        Assertions.assertEquals(200.0, counterVar.start, 1.0e-15);
        Assertions.assertEquals(290.0, counterVar.stop, 1.0e-15);
        Assertions.assertEquals( 1,    counter10.initCount);
        Assertions.assertEquals(10,    counter10.handleCount);
        Assertions.assertEquals( 1,    counter10.finishCount);
        Assertions.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assertions.assertEquals( 90.0, counter10.stop, 1.0e-15);

        // removing the handler with variable stepsize
        multiplexer.remove(counterVar);
        Assertions.assertEquals(1, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(300.0), initDate.shiftedBy(390.0));
        Assertions.assertEquals( 4,    counter60.initCount);
        Assertions.assertEquals( 8,    counter60.handleCount);
        Assertions.assertEquals( 4,    counter60.initCount);
        Assertions.assertEquals(300.0, counter60.start, 1.0e-15);
        Assertions.assertEquals(390.0, counter60.stop, 1.0e-15);
        Assertions.assertEquals( 3,    counterVar.initCount);
        Assertions.assertEquals( 3,    counterVar.handleCount);
        Assertions.assertEquals( 3,    counterVar.finishCount);
        Assertions.assertEquals(200.0, counterVar.start, 1.0e-15);
        Assertions.assertEquals(290.0, counterVar.stop, 1.0e-15);
        Assertions.assertEquals( 1,    counter10.initCount);
        Assertions.assertEquals(10,    counter10.handleCount);
        Assertions.assertEquals( 1,    counter10.initCount);
        Assertions.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assertions.assertEquals( 90.0, counter10.stop, 1.0e-15);

        // attempting to remove a handler already removed
        multiplexer.remove(counterVar);
        Assertions.assertEquals(1, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(400.0), initDate.shiftedBy(490.0));
        Assertions.assertEquals( 5,    counter60.initCount);
        Assertions.assertEquals(10,    counter60.handleCount);
        Assertions.assertEquals( 5,    counter60.finishCount);
        Assertions.assertEquals(400.0, counter60.start, 1.0e-15);
        Assertions.assertEquals(490.0, counter60.stop, 1.0e-15);
        Assertions.assertEquals( 3,    counterVar.initCount);
        Assertions.assertEquals( 3,    counterVar.handleCount);
        Assertions.assertEquals( 3,    counterVar.finishCount);
        Assertions.assertEquals(200.0, counterVar.start, 1.0e-15);
        Assertions.assertEquals(290.0, counterVar.stop, 1.0e-15);
        Assertions.assertEquals( 1,    counter10.initCount);
        Assertions.assertEquals(10,    counter10.handleCount);
        Assertions.assertEquals( 1,    counter10.finishCount);
        Assertions.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assertions.assertEquals( 90.0, counter10.stop, 1.0e-15);

        // removing everything
        multiplexer.clear();
        Assertions.assertEquals(0, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(500.0), initDate.shiftedBy(590.0));
        Assertions.assertEquals( 5,    counter60.initCount);
        Assertions.assertEquals(10,    counter60.handleCount);
        Assertions.assertEquals( 5,    counter60.finishCount);
        Assertions.assertEquals(400.0, counter60.start, 1.0e-15);
        Assertions.assertEquals(490.0, counter60.stop, 1.0e-15);
        Assertions.assertEquals( 3,    counterVar.initCount);
        Assertions.assertEquals( 3,    counterVar.handleCount);
        Assertions.assertEquals( 3,    counterVar.finishCount);
        Assertions.assertEquals(200.0, counterVar.start, 1.0e-15);
        Assertions.assertEquals(290.0, counterVar.stop, 1.0e-15);
        Assertions.assertEquals( 1,    counter10.initCount);
        Assertions.assertEquals(10,    counter10.handleCount);
        Assertions.assertEquals( 1,    counter10.finishCount);
        Assertions.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assertions.assertEquals( 90.0, counter10.stop, 1.0e-15);

    }

    @Test
    public void testOnTheFlyChanges() {

        final FieldStepHandlerMultiplexer<Binary64> multiplexer = propagator.getMultiplexer();

        Field<Binary64> field = Binary64Field.getInstance();
        Binary64        zero  = field.getZero();

        double               add60      =  3.0;
        double               rem60      = 78.0;
        FieldFixedCounter    counter60  = new FieldFixedCounter();
        FieldDateDetector<Binary64> d1 = new FieldDateDetector<>(field, initDate.shiftedBy(add60)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.add(zero.newInstance(60.0), counter60);
                                            return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d1);
        FieldDateDetector<Binary64> d2 = new FieldDateDetector<>(field, initDate.shiftedBy(rem60)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.remove(counter60);
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d2);

        double               addVar     =  5.0;
        double               remVar     =  7.0;
        FieldVariableCounter counterVar = new FieldVariableCounter();
        FieldDateDetector<Binary64> d3 = new FieldDateDetector<>(field, initDate.shiftedBy(addVar)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.add(counterVar);
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d3);
        FieldDateDetector<Binary64> d4 = new FieldDateDetector<>(field, initDate.shiftedBy(remVar)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.remove(counterVar);
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d4);

        double               add10      =  6.0;
        double               rem10      = 82.0;
        FieldFixedCounter    counter10  = new FieldFixedCounter();
        FieldDateDetector<Binary64> d5 = new FieldDateDetector<>(field, initDate.shiftedBy(add10)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.add(zero.newInstance(10.0), counter10);
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d5);
        FieldDateDetector<Binary64> d6 = new FieldDateDetector<>(field, initDate.shiftedBy(rem10)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.clear();
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d6);

        // full run, which will add and remove step handlers on the fly
        propagator.propagate(initDate.shiftedBy(90.0));
        Assertions.assertEquals( 1,     counter60.initCount);
        Assertions.assertEquals( 2,     counter60.handleCount);
        Assertions.assertEquals( 1,     counter60.finishCount);
        Assertions.assertEquals(add60,  counter60.start, 1.0e-15);
        Assertions.assertEquals(rem60,  counter60.stop, 1.0e-15);
        Assertions.assertEquals( 1,     counterVar.initCount);
        Assertions.assertEquals( 2,     counterVar.handleCount); // event at add10 splits the variable step in two parts
        Assertions.assertEquals( 1,     counterVar.finishCount);
        Assertions.assertEquals(addVar, counterVar.start, 1.0e-15);
        Assertions.assertEquals(remVar, counterVar.stop, 1.0e-15);
        Assertions.assertEquals( 1,     counter10.initCount);
        Assertions.assertEquals( 8,     counter10.handleCount);
        Assertions.assertEquals( 1,     counter10.finishCount);
        Assertions.assertEquals(add10,  counter10.start, 1.0e-15);
        Assertions.assertEquals(rem10,  counter10.stop, 1.0e-15);

    }

    private class FieldInitCheckerHandler implements FieldOrekitFixedStepHandler<Binary64> {

        private double expected;
        private boolean initialized;

        public FieldInitCheckerHandler(final double expected) {
            this.expected    = expected;
            this.initialized = false;
        }

        @Override
        public void init(FieldSpacecraftState<Binary64> s0, FieldAbsoluteDate<Binary64> t, Binary64 step) {
            initialized = true;
        }

        @Override
        public void handleStep(FieldSpacecraftState<Binary64> currentState) {
            this.expected = 2.0;
        }

        boolean isInitialized() {
            return initialized;
        }

        double getExpected() {
            return expected;
        }

    }

    private class FieldFixedCounter implements FieldOrekitFixedStepHandler<Binary64> {

        private int    initCount;
        private int    handleCount;
        private int    finishCount;
        private double start;
        private double stop;

        @Override
        public void init(FieldSpacecraftState<Binary64> s0, FieldAbsoluteDate<Binary64> t, Binary64 step) {
            ++initCount;
            start = s0.getDate().durationFrom(initDate).getReal();
        }

        @Override
        public void handleStep(FieldSpacecraftState<Binary64> currentState) {
            ++handleCount;
        }

        @Override
        public void finish(FieldSpacecraftState<Binary64> finalState) {
            ++finishCount;
            stop = finalState.getDate().durationFrom(initDate).getReal();
        }

    }

    private class FieldVariableCounter implements FieldOrekitStepHandler<Binary64> {

        private int    initCount;
        private int    handleCount;
        private int    finishCount;
        private double start;
        private double stop;

        @Override
        public void init(FieldSpacecraftState<Binary64> s0, FieldAbsoluteDate<Binary64> t) {
            ++initCount;
            start = s0.getDate().durationFrom(initDate).getReal();
        }

        @Override
        public void handleStep(FieldOrekitStepInterpolator<Binary64> interpolator) {
            Assertions.assertNotNull(interpolator.getPosition(interpolator.getCurrentState().getDate(),
                                                              interpolator.getCurrentState().getFrame()));
            ++handleCount;
        }

        @Override
        public void finish(FieldSpacecraftState<Binary64> finalState) {
            ++finishCount;
            stop = finalState.getDate().durationFrom(initDate).getReal();
        }

    }

}
