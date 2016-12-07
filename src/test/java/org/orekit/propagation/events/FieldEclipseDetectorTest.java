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
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldStopOnDecreasing;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

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
    public void testEclipse() throws OrekitException{
        doTestEclipse(Decimal64Field.getInstance());
    }
    @Test
    public void testPenumbra() throws OrekitException{
        doTestPenumbra(Decimal64Field.getInstance());
    }
    @Test
    public void testWithMethods() throws OrekitException{
        doTestWithMethods(Decimal64Field.getInstance());
    }
    
    @Test
    public void testInsideOcculting() throws OrekitException{
        doTestInsideOcculting(Decimal64Field.getInstance());
    }    
    @Test
    public void testInsideOcculted() throws OrekitException{
        doTestInsideOcculted(Decimal64Field.getInstance());
    }
    @Test
    public void testTooSmallMaxIterationCount() throws OrekitException{
        testTooSmallMaxIterationCount(Decimal64Field.getInstance());
    }
 
    
    
    private <T extends RealFieldElement<T>> void doTestEclipse(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685), zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field,1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getGCRF(), iniDate, mu);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
        propagator.resetInitialState(initialState);

        FieldEclipseDetector<T> e = new FieldEclipseDetector<T>(field.getZero().add(60.),field.getZero().add(1e-3),
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

    private <T extends RealFieldElement<T>> void doTestPenumbra(Field<T> field) throws OrekitException {
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
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60.));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        sun = CelestialBodyFactory.getSun();
        earth = CelestialBodyFactory.getEarth();
        sunRadius = 696000000.;
        earthRadius = 6400000.;

        FieldEclipseDetector<T> e = new FieldEclipseDetector<T>(zero.add(60.), zero.add(1.e-3), sun, sunRadius,
                                                earth, earthRadius).
                            withPenumbra();
        Assert.assertFalse(e.getTotalEclipse());
        propagator.addEventDetector(e);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(4388.155852, finalState.getDate().durationFrom(iniDate).getReal(), 2.0e-6);
    }
   
    private <T extends RealFieldElement<T>> void doTestWithMethods(Field<T> field) throws OrekitException {
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
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        sun = CelestialBodyFactory.getSun();
        earth = CelestialBodyFactory.getEarth();
        sunRadius = 696000000.;
        earthRadius = 6400000.;

        FieldEclipseDetector<T> e = new FieldEclipseDetector<T>(field.getZero().add(60.),field.getZero().add(1e-3),
                                                sun, sunRadius,
                                                earth, earthRadius).
                             withHandler(new FieldStopOnDecreasing<FieldEclipseDetector<T>, T>()).
                             withMaxCheck(field.getZero().add(120.0)).
                             withThreshold(field.getZero().add(1.0e-4)).
                             withMaxIter(12);
        Assert.assertEquals(120.0, e.getMaxCheckInterval().getReal(), 1.0e-15);
        Assert.assertEquals(1.0e-4, e.getThreshold().getReal(), 1.0e-15);
        Assert.assertEquals(12, e.getMaxIterationCount());
        propagator.addEventDetector(e);
        final FieldSpacecraftState<T> finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(2303.1835, finalState.getDate().durationFrom(iniDate).getReal(), 1.0e-3);

    }

    private <T extends RealFieldElement<T>> void doTestInsideOcculting(Field<T> field) throws OrekitException {
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
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        sun = CelestialBodyFactory.getSun();
        earth = CelestialBodyFactory.getEarth();
        sunRadius = 696000000.;
        earthRadius = 6400000.;

        FieldEclipseDetector<T> e = new FieldEclipseDetector<T>(field.getZero().add(60.), field.getZero().add(1.e-3),
                                                                sun, sunRadius,
                                                                earth, earthRadius);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<T>(new FieldCartesianOrbit<T>(new TimeStampedFieldPVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                                                           new FieldPVCoordinates<T>(
                                                                                           new FieldVector3D<T>(field.getZero().add(1e6), field.getZero().add(2e6), field.getZero().add(3e6)),
                                                                                           new FieldVector3D<T>(field.getZero().add(1000), field.getZero().add(0), field.getZero().add(0)))),
                                                                   FramesFactory.getGCRF(),
                                                                   mu));
        Assert.assertEquals(-FastMath.PI, e.g(s).getReal(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void doTestInsideOcculted(Field<T> field) throws OrekitException {
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
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        sun = CelestialBodyFactory.getSun();
        earth = CelestialBodyFactory.getEarth();
        sunRadius = 696000000.;
        earthRadius = 6400000.;

        FieldEclipseDetector<T> e = new FieldEclipseDetector<T>(field.getZero().add(60.), field.getZero().add(1.e-3),
                        sun, sunRadius,
                        earth, earthRadius);
        Vector3D p = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                          FramesFactory.getGCRF()).getPosition();
        FieldSpacecraftState<T> s = new FieldSpacecraftState<T>(new FieldCartesianOrbit<T>(new TimeStampedFieldPVCoordinates<T>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                                                                new FieldPVCoordinates<T>(
                                                                                                new FieldVector3D<T>(field.getOne(), field.getZero(), field.getZero()).add(p),
                                                                                                new FieldVector3D<T>(field.getZero(), field.getZero(), field.getOne()))),
                                                                   FramesFactory.getGCRF(),
                                                                   mu));
        Assert.assertEquals(FastMath.PI, e.g(s).getReal(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void testTooSmallMaxIterationCount(Field<T> field) throws OrekitException {
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
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        sun = CelestialBodyFactory.getSun();
        earth = CelestialBodyFactory.getEarth();
        sunRadius = 696000000.;
        earthRadius = 6400000.;

        int n = 5;
        FieldEclipseDetector<T> e = new FieldEclipseDetector<T>(field.getZero().add(60.), field.getZero().add(1.e-3),
                        sun, sunRadius,
                        earth, earthRadius).
                             withHandler(new FieldStopOnDecreasing<FieldEclipseDetector<T>, T>()).
                             withMaxCheck(field.getZero().add(120.0)).
                             withThreshold(field.getZero().add(1.0e-4)).
                             withMaxIter(n);
       propagator.addEventDetector(e);
        try {
            propagator.propagate(iniDate.shiftedBy(6000));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(n, ((Integer) ((MathRuntimeException) oe.getCause()).getParts()[0]).intValue());
        }
    }

}
//
