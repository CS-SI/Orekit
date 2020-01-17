/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;

public class FieldDateDetectorTest {

    private int evtno = 0;
    private double maxCheck;
    private double threshold;
    private double dt;
    private double mu;
    private AbsoluteDate nodeDate;

    @Test
    public void testSimpleTimer() {
        doTestSimpleTimer(Decimal64Field.getInstance());
    }
    @Test
    public void testEmbeddedTimer() {
        doTestEmbeddedTimer(Decimal64Field.getInstance());
    }
    @Test
    public void testAutoEmbeddedTimer() {
        doTestAutoEmbeddedTimer(Decimal64Field.getInstance());
    }
    @Test(expected=IllegalArgumentException.class)
    public void testExceptionTimer() {
        doTestExceptionTimer(Decimal64Field.getInstance());
    }
    @Test
    public void testGenericHandler() {
        doTestGenericHandler(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestSimpleTimer(Field<T> field) {
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
        integrator.setInitialStepSize(zero.add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);

        FieldDateDetector<T>  dateDetector = new FieldDateDetector<>(zero.add(maxCheck), zero.add(threshold),
                                                                     toArray(iniDate.shiftedBy(2.0*dt)));
        Assert.assertEquals(2 * dt, dateDetector.getDate().durationFrom(iniDate).getReal(), 1.0e-10);
        propagator.addEventDetector(dateDetector);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(100.*dt));

        Assert.assertEquals(2.0*dt, finalState.getDate().durationFrom(iniDate).getReal(), threshold);
    }


    private <T extends RealFieldElement<T>> void doTestEmbeddedTimer(Field<T> field) {
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
        integrator.setInitialStepSize(zero.add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);
        @SuppressWarnings("unchecked")
        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(zero.add(maxCheck), zero.add(threshold),
                                                                    (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 0));
        Assert.assertNull(dateDetector.getDate());
        FieldEventDetector<T> nodeDetector = new FieldNodeDetector<>(iniOrbit, iniOrbit.getFrame()).
                withHandler(new FieldContinueOnEvent<FieldNodeDetector<T>, T>() {
                    public Action eventOccurred(FieldSpacecraftState<T> s, FieldNodeDetector<T> nd, boolean increasing)
                        {
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

        Assert.assertEquals(dt, finalState.getDate().durationFrom(nodeDate).getReal(), threshold);
    }


    private <T extends RealFieldElement<T>> void doTestAutoEmbeddedTimer(Field<T> field) {
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
        integrator.setInitialStepSize(zero.add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);

        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(zero.add(maxCheck), zero.add(threshold),
                                                                    toArray(iniDate.shiftedBy(-dt))).
                withHandler(new FieldContinueOnEvent<FieldDateDetector<T>, T >() {
                    public Action eventOccurred(FieldSpacecraftState<T> s, FieldDateDetector<T>  dd,  boolean increasing)
                            {
                        FieldAbsoluteDate<T> nextDate = s.getDate().shiftedBy(-dt);
                        dd.addEventDate(nextDate);
                        ++evtno;
                        return Action.CONTINUE;
                    }
                });
        propagator.addEventDetector(dateDetector);
        propagator.propagate(iniDate.shiftedBy(-100.*dt));

        Assert.assertEquals(100, evtno);
    }

    private <T extends RealFieldElement<T>> void doTestExceptionTimer(Field<T> field) {
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
        integrator.setInitialStepSize(zero.add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);

        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(zero.add(maxCheck), zero.add(threshold),
                                                                    toArray(iniDate.shiftedBy(dt))).
                withHandler(new FieldContinueOnEvent<FieldDateDetector<T>, T >() {
                    public Action eventOccurred(FieldSpacecraftState<T> s, FieldDateDetector<T>  dd, boolean increasing)
                        {
                        double step = (evtno % 2 == 0) ? 2.*maxCheck : maxCheck/2.;
                        FieldAbsoluteDate<T> nextDate = s.getDate().shiftedBy(step);
                        dd.addEventDate(nextDate);
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

    private <T extends RealFieldElement<T>> void doTestGenericHandler(Field<T> field) {
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
        integrator.setInitialStepSize(zero.add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);

        //setup
        final FieldDateDetector<T> dateDetector = new FieldDateDetector<>(zero.add(maxCheck), zero.add(threshold),
                                                                          toArray(iniDate.shiftedBy(dt)));
        // generic event handler that works with all detectors.
        FieldEventHandler<FieldEventDetector<T>, T> handler = new FieldEventHandler<FieldEventDetector<T>, T>() {
            @Override
            public Action eventOccurred(FieldSpacecraftState<T> s,
                                        FieldEventDetector<T> detector,
                                        boolean increasing)
                    {
                return Action.STOP;
            }

            @Override
            public FieldSpacecraftState<T> resetState(FieldEventDetector<T> detector,
                                              FieldSpacecraftState<T> oldState)
                    {
                throw new RuntimeException("Should not be called");
            }
        };

        //action
        final FieldDateDetector<T> dateDetector2;

        dateDetector2 = dateDetector.withHandler(handler);

        propagator.addEventDetector(dateDetector2);
        FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(100 * dt));

        //verify
        Assert.assertEquals(dt, finalState.getDate().durationFrom(iniDate).getReal(), threshold);
    }

    private <T extends RealFieldElement<T>> FieldTimeStamped<T>[] toArray(final FieldAbsoluteDate<T> date) {
        @SuppressWarnings("unchecked")
        final FieldTimeStamped<T>[] array = (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 1);
        array[0] = date;
        return array;
    }

    @Before
    public void setUp() {
            Utils.setDataRoot("regular-data");
            mu = 3.9860047e14;
            dt = 60.;
            maxCheck  = 10.;
            threshold = 10.e-7;
            evtno = 0;
    }

}
