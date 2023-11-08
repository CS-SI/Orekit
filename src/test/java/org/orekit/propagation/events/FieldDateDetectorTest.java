/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.events;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.FieldTLEPropagator;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;

public class FieldDateDetectorTest {

    private int evtno = 0;
    private double minGap;
    private double threshold;
    private double dt;
    private double mu;
    private AbsoluteDate nodeDate;

    @Test
    public void testSimpleTimer() {
        doTestSimpleTimer(Binary64Field.getInstance());
    }

    @Test
    public void testEmbeddedTimer() {
        doTestEmbeddedTimer(Binary64Field.getInstance());
    }

    @Test
    public void testAutoEmbeddedTimer() {
        doTestAutoEmbeddedTimer(Binary64Field.getInstance());
    }

    @Test
    public void testExceptionTimer() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            doTestExceptionTimer(Binary64Field.getInstance());
        });
    }

    @Test
    public void testGenericHandler() {
        doTestGenericHandler(Binary64Field.getInstance());
    }

    @Test
    public void testIssue935() {
        doTestIssue935(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSimpleTimer(final Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add( 3492467.560), zero.add( -25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate  = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        FieldOrbit<T> iniOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                             FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(iniOrbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.addAdditionalDerivativesProvider(new FieldAdditionalDerivativesProvider<T>() {
            public String getName()                              { return "dummy"; }
            public int    getDimension()                         { return 1; }
            public FieldCombinedDerivatives<T> combinedDerivatives(FieldSpacecraftState<T> s) {
                return new FieldCombinedDerivatives<>(MathArrays.buildArray(field, 1), null);
                }
        });
        propagator.getMultiplexer().add(interpolator -> {
            FieldSpacecraftState<T> prev = interpolator.getPreviousState();
            FieldSpacecraftState<T> curr = interpolator.getCurrentState();
            T dt = curr.getDate().durationFrom(prev.getDate());
            FieldOrekitStepInterpolator<T> restricted =
                            interpolator.restrictStep(prev.shiftedBy(dt.multiply(+0.25)),
                                                      curr.shiftedBy(dt.multiply(-0.25)));
            FieldSpacecraftState<T> restrictedPrev = restricted.getPreviousState();
            FieldSpacecraftState<T> restrictedCurr = restricted.getCurrentState();
            T restrictedDt = restrictedCurr.getDate().durationFrom(restrictedPrev.getDate());
            Assertions.assertEquals(dt.multiply(0.5).getReal(), restrictedDt.getReal(), 1.0e-10);
        });
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState.addAdditionalState("dummy", MathArrays.buildArray(field, 1)));

        FieldDateDetector<T>  dateDetector = new FieldDateDetector<>(field, toArray(iniDate.shiftedBy(2.0*dt))).
                        withMinGap(minGap).withThreshold(field.getZero().newInstance(threshold));
        Assertions.assertEquals(2 * dt, dateDetector.getDate().durationFrom(iniDate).getReal(), 1.0e-10);
        propagator.addEventDetector(dateDetector);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(100.*dt));

        Assertions.assertEquals(2.0*dt, finalState.getDate().durationFrom(iniDate).getReal(), threshold);
    }


    private <T extends CalculusFieldElement<T>> void doTestEmbeddedTimer(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add( 3492467.560), zero.add( -25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate  = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        FieldOrbit<T> iniOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                             FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(iniOrbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);
        @SuppressWarnings("unchecked")
        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(field, (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 0)).
                        withMinGap(minGap).withThreshold(field.getZero().newInstance(threshold));
        Assertions.assertNull(dateDetector.getDate());
        FieldEventDetector<T> nodeDetector = new FieldNodeDetector<>(iniOrbit, iniOrbit.getFrame()).
                withHandler(new FieldContinueOnEvent<T>() {
                    public Action eventOccurred(FieldSpacecraftState<T> s, FieldEventDetector<T> nd, boolean increasing) {
                        if (increasing) {
                            nodeDate = s.getDate().toAbsoluteDate();
                            dateDetector.addEventDate(s.getDate().shiftedBy(dt));
                        }
                        return Action.CONTINUE;
                    }
                });

        propagator.addEventDetector(nodeDetector);
        propagator.addEventDetector(dateDetector);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(100.*dt));

        Assertions.assertEquals(dt, finalState.getDate().durationFrom(nodeDate).getReal(), threshold);
    }


    private <T extends CalculusFieldElement<T>> void doTestAutoEmbeddedTimer(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add( 3492467.560), zero.add( -25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate  = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        FieldOrbit<T> iniOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                             FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(iniOrbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);

        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(field, toArray(iniDate.shiftedBy(-dt))).
                        withMinGap(minGap).
                        withThreshold(field.getZero().newInstance(threshold)).
                        withHandler(new FieldContinueOnEvent<T >() {
                            public Action eventOccurred(FieldSpacecraftState<T> s, FieldEventDetector<T>  dd,  boolean increasing) {
                                FieldAbsoluteDate<T> nextDate = s.getDate().shiftedBy(-dt);
                                ((FieldDateDetector<T>) dd).addEventDate(nextDate);
                                ++evtno;
                                return Action.CONTINUE;
                            }
                        });
        propagator.addEventDetector(dateDetector);
        propagator.propagate(iniDate.shiftedBy(-100.*dt));

        Assertions.assertEquals(100, evtno);
    }

    private <T extends CalculusFieldElement<T>> void doTestExceptionTimer(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add( 3492467.560), zero.add( -25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate  = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        FieldOrbit<T> iniOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                             FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(iniOrbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);

        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(field, toArray(iniDate.shiftedBy(dt))).
                        withMinGap(minGap).
                        withThreshold(field.getZero().newInstance(threshold)).
                        withHandler(new FieldContinueOnEvent<T>() {
                            public Action eventOccurred(FieldSpacecraftState<T> s, FieldEventDetector<T>  dd, boolean increasing)
                            {
                                double step = (evtno % 2 == 0) ? 2.*minGap : minGap/2.;
                                FieldAbsoluteDate<T> nextDate = s.getDate().shiftedBy(step);
                                ((FieldDateDetector<T>) dd).addEventDate(nextDate);
                                ++evtno;
                                return Action.CONTINUE;
                            }
                        });
        propagator.addEventDetector(dateDetector);
        propagator.propagate(iniDate.shiftedBy(100.*dt));
    }

    /**
     * Check that a generic event handler can be used with an event detector.
     */

    private <T extends CalculusFieldElement<T>> void doTestGenericHandler(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add( 3492467.560), zero.add( -25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate  = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        FieldOrbit<T> iniOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                             FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(iniOrbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);

        //setup
        final FieldDateDetector<T> dateDetector = new FieldDateDetector<>(field, toArray(iniDate.shiftedBy(dt))).
                        withMinGap(minGap).withThreshold(field.getZero().newInstance(threshold));
        // generic event handler that works with all detectors.
        FieldEventHandler<T> handler = new FieldEventHandler<T>() {
            @Override
            public Action eventOccurred(FieldSpacecraftState<T> s,
                                        FieldEventDetector<T> detector,
                                        boolean increasing) {
                return Action.STOP;
            }

            @Override
            public FieldSpacecraftState<T> resetState(FieldEventDetector<T> detector,
                                              FieldSpacecraftState<T> oldState) {
                throw new RuntimeException("Should not be called");
            }
        };

        //action
        final FieldDateDetector<T> dateDetector2;

        dateDetector2 = dateDetector.withHandler(handler);

        propagator.addEventDetector(dateDetector2);
        FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(100 * dt));

        //verify
        Assertions.assertEquals(dt, finalState.getDate().durationFrom(iniDate).getReal(), threshold);
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue935(Field<T> field) {

        // startTime, endTime
        long start = 1570802400000L;
        long end = 1570838399000L;

        // Build propagator
        FieldTLE<T> tle = new FieldTLE<>(field,
                                         "1 43197U 18015F   19284.07336221  .00000533  00000-0  24811-4 0  9998",
                                         "2 43197  97.4059  50.1428 0017543 265.5429 181.0400 15.24136761 93779");
        FieldPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(tle, tle.getParameters(field));

        // Min gap to seconds
        int maxCheck = (int) ((end - start) / 2000);
        @SuppressWarnings("unchecked")
        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(field, getAbsoluteDateFromTimestamp(field, start)).
                        withMinGap(maxCheck).
                        withThreshold(field.getZero().newInstance(1.0e-6)).
                        withHandler(new FieldStopOnEvent<>());
        dateDetector.addEventDate(getAbsoluteDateFromTimestamp(field, end));

        // Add event detectors to orbit
        propagator.addEventDetector(dateDetector);

        // Propagate
        final FieldAbsoluteDate<T> startDate = getAbsoluteDateFromTimestamp(field, start);
        final FieldAbsoluteDate<T> endDate   = getAbsoluteDateFromTimestamp(field, end);
        FieldSpacecraftState<T> lastState = propagator.propagate(startDate, endDate.shiftedBy(1));
        Assertions.assertEquals(0.0, lastState.getDate().durationFrom(endDate).getReal(), 1.0e-15);

    }

    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getAbsoluteDateFromTimestamp(final Field<T> field,
                                                                                                        final long timestamp) {
        LocalDateTime utcDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                                                        ZoneId.of("UTC"));
        int year = utcDate.getYear();
        int month = utcDate.getMonthValue();
        int day = utcDate.getDayOfMonth();
        int hour = utcDate.getHour();
        int minute = utcDate.getMinute();
        double second = utcDate.getSecond();
        double millis = utcDate.getNano() / 1e9;
        return new FieldAbsoluteDate<>(field, year, month, day, hour, minute, second, TimeScalesFactory.getUTC()).shiftedBy(millis);
    }

    private <T extends CalculusFieldElement<T>> FieldTimeStamped<T>[] toArray(final FieldAbsoluteDate<T> date) {
        @SuppressWarnings("unchecked")
        final FieldTimeStamped<T>[] array = (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 1);
        array[0] = date;
        return array;
    }

    @BeforeEach
    public void setUp() {
            Utils.setDataRoot("regular-data");
            mu = 3.9860047e14;
            dt = 60.;
            minGap  = 10.;
            threshold = 10.e-7;
            evtno = 0;
    }

}
