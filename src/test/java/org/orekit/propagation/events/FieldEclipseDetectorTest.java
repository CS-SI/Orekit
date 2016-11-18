/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldStopOnDecreasing;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;

public class FieldEclipseDetectorTest {

    private double               mu;
    private CelestialBody        sun;
    private CelestialBody        earth;
    private double               sunRadius;
    private double               earthRadius;


    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            sun = CelestialBodyFactory.getSun();
            earth = CelestialBodyFactory.getEarth();
            sunRadius = 696000000.;
            earthRadius = 6400000.;
            mu  = 3.9860047e14;
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @Test
    public void test() throws OrekitException{
        testEclipse(Decimal64Field.getInstance());
    }

    public <T extends RealFieldElement<T>> void testEclipse(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field,1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getGCRF(), iniDate, mu);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        propagator.resetInitialState(initialState);

        FieldEclipseDetector<T> e = new FieldEclipseDetector<T>(zero.add(60.), zero.add(1.e-3),
                                                sun, sunRadius,
                                                earth, earthRadius).
                            withHandler(new FieldStopOnDecreasing<FieldEclipseDetector<T>, T>()).
                            withUmbra();
        Assert.assertEquals(60.0, e.getMaxCheckInterval().getReal(), 1.0e-15);
        Assert.assertEquals(1.0e-3, e.getThreshold().getReal(), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, e.getMaxIterationCount());
        Assert.assertSame(sun, e.getOcculted());
        Assert.assertEquals(sunRadius, e.getOccultedRadius(), 1.0);
        Assert.assertSame(earth, e.getOcculting());
        Assert.assertEquals(earthRadius, e.getOccultingRadius(), 1.0);
        Assert.assertTrue(e.getTotalEclipse());
        propagator.addEventDetector(e);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(2303.1835, finalState.getDate().durationFrom(iniDate).getReal(), 1.0e-3);
    }
//
//    public <T extends RealFieldElement<T>> void testPenumbra(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
//        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
//        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field,1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
//        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
//                                                 FramesFactory.getGCRF(), iniDate, mu);
//        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
//        double[] absTolerance = {
//            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
//        };
//        double[] relTolerance = {
//            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
//        };
//        AdaptiveStepsizeIntegrator integrator =
//            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
//        integrator.setInitialStepSize(60);
//        propagator = new NumericalPropagator(integrator);
//        propagator.setInitialState(initialState.toSpacecraftState());
//        sun = CelestialBodyFactory.getSun();
//        earth = CelestialBodyFactory.getEarth();
//        sunRadius = 696000000.;
//        earthRadius = 6400000.;
//
//        EclipseDetector e = new EclipseDetector(sun, sunRadius,
//                                                earth, earthRadius).
//                            withPenumbra();
//        Assert.assertFalse(e.getTotalEclipse());
//        propagator.addEventDetector(e);
//        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(6000));
//        Assert.assertEquals(4388.155852, finalState.getDate().durationFrom(iniDate), 2.0e-6);
//    }
//
//    public <T extends RealFieldElement<T>> void testWithMethods(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
//        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
//        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field,1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
//        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
//                                                 FramesFactory.getGCRF(), iniDate, mu);
//        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
//        double[] absTolerance = {
//            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
//        };
//        double[] relTolerance = {
//            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
//        };
//        AdaptiveStepsizeIntegrator integrator =
//            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
//        integrator.setInitialStepSize(60);
//        propagator = new NumericalPropagator(integrator);
//        propagator.setInitialState(initialState.toSpacecraftState());
//        sun = CelestialBodyFactory.getSun();
//        earth = CelestialBodyFactory.getEarth();
//        sunRadius = 696000000.;
//        earthRadius = 6400000.;
//
//        EclipseDetector e = new EclipseDetector(60.,
//                                                sun, sunRadius,
//                                                earth, earthRadius).
//                             withHandler(new FieldStopOnDecreasing<T><EclipseDetector>()).
//                             withMaxCheck(120.0).
//                             withThreshold(1.0e-4).
//                             withMaxIter(12);
//        Assert.assertEquals(120.0, e.getMaxCheckInterval(), 1.0e-15);
//        Assert.assertEquals(1.0e-4, e.getThreshold(), 1.0e-15);
//        Assert.assertEquals(12, e.getMaxIterationCount());
//        propagator.addEventDetector(e);
//        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(6000));
//        Assert.assertEquals(2303.1835, finalState.getDate().durationFrom(iniDate), 1.0e-3);
//
//    }
//
//    public <T extends RealFieldElement<T>> void testInsideOcculting(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
//        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
//        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field,1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
//        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
//                                                 FramesFactory.getGCRF(), iniDate, mu);
//        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
//        double[] absTolerance = {
//            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
//        };
//        double[] relTolerance = {
//            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
//        };
//        AdaptiveStepsizeIntegrator integrator =
//            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
//        integrator.setInitialStepSize(60);
//        propagator = new NumericalPropagator(integrator);
//        propagator.setInitialState(initialState.toSpacecraftState());
//        sun = CelestialBodyFactory.getSun();
//        earth = CelestialBodyFactory.getEarth();
//        sunRadius = 696000000.;
//        earthRadius = 6400000.;
//
//        EclipseDetector e = new EclipseDetector(sun, sunRadius,
//                                                earth, earthRadius);
//        FieldSpacecraftState<T> s = new FieldSpacecraftState<T>(new FieldCartesianOrbit<T>(new TimeStampedFieldPVCoordinates<T>(FieldAbsoluteDate<T>.J2000_EPOCH,
//                                                                                                new FieldVector3D<T>(1e6, 2e6, 3e6),
//                                                                                                new FieldVector3D<T>(1000, 0, 0)),
//                                                                   FramesFactory.getGCRF(),
//                                                                   mu));
//        Assert.assertEquals(-FastMath.PI, e.g(s), 1.0e-15);
//    }
//
//    public <T extends RealFieldElement<T>> void testInsideOcculted(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
//        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
//        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field,1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
//        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
//                                                 FramesFactory.getGCRF(), iniDate, mu);
//        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
//        double[] absTolerance = {
//            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
//        };
//        double[] relTolerance = {
//            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
//        };
//        AdaptiveStepsizeIntegrator integrator =
//            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
//        integrator.setInitialStepSize(60);
//        propagator = new NumericalPropagator(integrator);
//        propagator.setInitialState(initialState.toSpacecraftState());
//        sun = CelestialBodyFactory.getSun();
//        earth = CelestialBodyFactory.getEarth();
//        sunRadius = 696000000.;
//        earthRadius = 6400000.;
//
//        EclipseDetector e = new EclipseDetector(sun, sunRadius,
//                                                earth, earthRadius);
//        FieldVector3D<T> p = sun.getFieldPVCoordinates<T>(FieldAbsoluteDate<T>.J2000_EPOCH,
//                                          FramesFactory.getGCRF()).getPosition();
//        FieldSpacecraftState<T> s = new FieldSpacecraftState<T>(new FieldCartesianOrbit<T>(new TimeStampedFieldPVCoordinates<T>(FieldAbsoluteDate<T>.J2000_EPOCH,
//                                                                                                p.add(FieldVector3D<T>.PLUS_I),
//                                                                                                FieldVector3D<T>.PLUS_K),
//                                                                   FramesFactory.getGCRF(),
//                                                                   mu));
//        Assert.assertEquals(FastMath.PI, e.g(s), 1.0e-15);
//    }
//
//    public <T extends RealFieldElement<T>> void testTooSmallMaxIterationCount(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
//        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
//        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field,1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
//        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
//                                                 FramesFactory.getGCRF(), iniDate, mu);
//        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
//        double[] absTolerance = {
//            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
//        };
//        double[] relTolerance = {
//            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
//        };
//        AdaptiveStepsizeIntegrator integrator =
//            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
//        integrator.setInitialStepSize(60);
//        propagator = new NumericalPropagator(integrator);
//        propagator.setInitialState(initialState.toSpacecraftState());
//        sun = CelestialBodyFactory.getSun();
//        earth = CelestialBodyFactory.getEarth();
//        sunRadius = 696000000.;
//        earthRadius = 6400000.;
//
//        int n = 5;
//        EclipseDetector e = new EclipseDetector(60., 1.e-3,
//                                                sun, sunRadius,
//                                                earth, earthRadius).
//                             withHandler(new FieldStopOnDecreasing<T><EclipseDetector>()).
//                             withMaxCheck(120.0).
//                             withThreshold(1.0e-4).
//                             withMaxIter(n);
//       propagator.addEventDetector(e);
//        try {
//            propagator.propagate(iniDate.shiftedBy(6000));
//            Assert.fail("an exception should have been thrown");
//        } catch (OrekitException oe) {
//            Assert.assertEquals(n, ((Integer) ((MathRuntimeException) oe.getCause()).getParts()[0]).intValue());
//        }
//    }
//

}
//
