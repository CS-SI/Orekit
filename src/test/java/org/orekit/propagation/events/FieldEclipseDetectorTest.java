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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.LocalizedODEFormats;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldStopOnDecreasing;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

public class FieldEclipseDetectorTest {

    private double               mu;
    private CelestialBody        sun;
    private OneAxisEllipsoid     earth;
    private double               sunRadius;


    @BeforeEach
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            sun = CelestialBodyFactory.getSun();
            earth = new OneAxisEllipsoid(6400000., 0.0, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
            sunRadius = 696000000.;
            mu  = 3.9860047e14;
        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @Test
    public void testEclipse() {
        doTestEclipse(Binary64Field.getInstance());
    }
    @Test
    public void testPenumbra() {
        doTestPenumbra(Binary64Field.getInstance());
    }
    @Test
    public void testWithMethods() {
        doTestWithMethods(Binary64Field.getInstance());
    }

    @Test
    public void testInsideOcculting() {
        doTestInsideOcculting(Binary64Field.getInstance());
    }
    @Test
    public void testInsideOcculted() {
        doTestInsideOcculted(Binary64Field.getInstance());
    }
    @Test
    public void testTooSmallMaxIterationCount() {
        testTooSmallMaxIterationCount(Binary64Field.getInstance());
    }



    private <T extends CalculusFieldElement<T>> void doTestEclipse(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getGCRF(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        propagator.resetInitialState(initialState);

        FieldEclipseDetector<T> e = new FieldEclipseDetector<>(field, sun, sunRadius, earth).
                                    withMaxCheck(60.0).
                                    withThreshold(zero.newInstance(1e-3)).
                                    withHandler(new FieldStopOnDecreasing<T>()).
                                    withUmbra();
        Assertions.assertEquals(60.0, e.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-3, e.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, e.getMaxIterationCount());
        Assertions.assertEquals(0.0, e.getMargin().getReal(), 1.0e-15);
        Assertions.assertSame(sun, e.getOccultationEngine().getOcculted());
        Assertions.assertEquals(sunRadius, e.getOccultationEngine().getOccultedRadius(), 1.0);
        Assertions.assertSame(earth, e.getOccultationEngine().getOcculting());
        Assertions.assertTrue(e.getTotalEclipse());
        propagator.addEventDetector(e);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(2303.1835, finalState.getDate().durationFrom(iniDate).getReal(), 1.0e-3);
    }

    private <T extends CalculusFieldElement<T>> void doTestPenumbra(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getGCRF(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60.);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);

        FieldEclipseDetector<T> e = new FieldEclipseDetector<>(field, sun, sunRadius, earth).
                                    withMaxCheck(60.0).
                                    withThreshold(zero.newInstance(1e-3)).
                                    withPenumbra();
        Assertions.assertFalse(e.getTotalEclipse());
        propagator.addEventDetector(e);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(4388.155852, finalState.getDate().durationFrom(iniDate).getReal(), 2.0e-6);
    }

    private <T extends CalculusFieldElement<T>> void doTestWithMethods(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getGCRF(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
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

        FieldEclipseDetector<T> e = new FieldEclipseDetector<>(field, sun, sunRadius, earth).
                                    withMaxCheck(120.0).
                                    withThreshold(zero.newInstance(1e-4)).
                                    withHandler(new FieldStopOnDecreasing<T>()).
                                    withMaxIter(12).
                                    withMargin(zero.newInstance(0.001));
        Assertions.assertEquals(120.0, e.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-4, e.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(12, e.getMaxIterationCount());
        propagator.addEventDetector(e);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(2304.188978, finalState.getDate().durationFrom(iniDate).getReal(), 1.0e-4);

    }

    private <T extends CalculusFieldElement<T>> void doTestInsideOcculting(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                 FramesFactory.getGCRF(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
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

        FieldEclipseDetector<T> e = new FieldEclipseDetector<>(field, sun, sunRadius, earth).
                                    withMaxCheck(60.0).
                                    withThreshold(zero.newInstance(1e-3));
        FieldSpacecraftState<T> s = new FieldSpacecraftState<>(new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                                                                                             new FieldPVCoordinates<>(new FieldVector3D<>(zero.newInstance(1e6),
                                                                                                                                                                          zero.newInstance(2e6),
                                                                                                                                                                          zero.newInstance(3e6)),
                                                                                                                                                      new FieldVector3D<>(zero.newInstance(1000),
                                                                                                                                                                          zero.newInstance(0),
                                                                                                                                                                          zero.newInstance(0)))),
                                                                                         FramesFactory.getGCRF(),
                                                                                         zero.add(mu)));
        try {
            e.g(s);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.POINT_INSIDE_ELLIPSOID, oe.getSpecifier());
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestInsideOcculted(Field<T> field) {
        T zero = field.getZero();
        T one  = field.getOne();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getGCRF(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
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

        FieldEclipseDetector<T> e = new FieldEclipseDetector<>(field, sun, sunRadius, earth).
                                    withMaxCheck(60.0).
                                    withThreshold(zero.newInstance(1e-3));
        Vector3D p = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                          FramesFactory.getGCRF()).getPosition();
        FieldSpacecraftState<T> s = new FieldSpacecraftState<>(new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                                                                                             new FieldPVCoordinates<>(new FieldVector3D<>(one,
                                                                                                                                                                          zero,
                                                                                                                                                                          zero).add(p),
                                                                                                                                                      new FieldVector3D<>(zero,
                                                                                                                                                                          zero,
                                                                                                                                                                          one))),
                                                                                         FramesFactory.getGCRF(),
                                                                                         zero.add(mu)));
        Assertions.assertEquals(FastMath.PI, e.g(s).getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void testTooSmallMaxIterationCount(Field<T> field) {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getGCRF(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
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

        int n = 5;
        FieldEclipseDetector<T> e = new FieldEclipseDetector<>(field, sun, sunRadius, earth).
                                    withMaxCheck(120.0).
                                    withThreshold(zero.newInstance(1e-4)).
                                    withHandler(new FieldStopOnDecreasing<T>()).
                                    withMaxIter(n);
       propagator.addEventDetector(e);
        try {
            propagator.propagate(iniDate.shiftedBy(6000));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedODEFormats.FIND_ROOT,
                                    ((MathRuntimeException) oe.getCause()).getSpecifier());
        }
    }

}
//
