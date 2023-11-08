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
package org.orekit.propagation.numerical;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.gnss.data.GLONASSEphemeris;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSOrbitalElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.GLONASSDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class GLONASSNumericalPropagatorTest {

    private static GLONASSEphemeris ephemeris;

    @BeforeAll
    public static void setUpBeforeClass() {
        Utils.setDataRoot("regular-data");
        // Reference values for validation are given into Glonass Interface Control Document v1.0 2016
        ephemeris = new GLONASSEphemeris(5, 251, 11700,
                                         7003008.789,
                                         783.5417,
                                         0.0,
                                         -12206626.953,
                                         2804.2530,
                                         0.0,
                                         21280765.625,
                                         1352.5150,
                                         0.0);
    }

    @Test
    public void testPerfectValues() {

        // 4th order Runge-Kutta
        final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // Initialize the propagator
        final GLONASSNumericalPropagator propagator = new GLONASSNumericalPropagatorBuilder(integrator, ephemeris, false).
                        attitudeProvider(Utils.defaultLaw()).
                        mass(1521.0).
                        eci(FramesFactory.getEME2000()).
                        build();

        // Target date
        final AbsoluteDate target = new AbsoluteDate(new DateComponents(2012, 9, 7),
                                                     new TimeComponents(12300),
                                                     TimeScalesFactory.getGLONASS());

        // Initial verifications
        final GLONASSOrbitalElements poe = propagator.getGLONASSOrbitalElements();
        Assertions.assertEquals(0.0, poe.getXDotDot(), Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, poe.getYDotDot(), Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, poe.getZDotDot(), Precision.SAFE_MIN);
        Assertions.assertEquals(5,   poe.getN4());
        Assertions.assertEquals(251, poe.getNa());

        // Propagation
        final SpacecraftState finalState = propagator.propagate(target);
        final PVCoordinates pvFinal = finalState.getPVCoordinates(FramesFactory.getPZ9011(IERSConventions.IERS_2010, true));

        // Expected outputs in PZ90.11 frame
        final Vector3D expectedPosition = new Vector3D(7523174.819, -10506961.965, 21999239.413);
        final Vector3D expectedVelocity = new Vector3D(950.126007, 2855.687825, 1040.679862);

        // Computed outputs
        final Vector3D computedPosition = pvFinal.getPosition();
        final Vector3D computedVelocity = pvFinal.getVelocity();

        Assertions.assertEquals(0.0, computedPosition.distance(expectedPosition), 1.1e-3);
        Assertions.assertEquals(0.0, computedVelocity.distance(expectedVelocity), 3.3e-6);
    }

    @Test
    public void testFromITRF2008ToPZ90() {
        // Reference for the test
        // "PARAMETRY ZEMLI 1990" (PZ-90.11) Reference Document
        //  MILITARY TOPOGRAPHIC DEPARTMENT OF THE GENERAL STAFF OF ARMED FORCES OF THE RUSSIAN FEDERATION, Moscow, 2014"

        // Position in ITRF-2008
        final Vector3D itrf2008P = new Vector3D(2845455.9753, 2160954.3073, 5265993.2656);

        // Ref position in PZ-90.11
        final Vector3D refPZ90   = new Vector3D(2845455.9772, 2160954.3078, 5265993.2664);

        // Recomputed position in PZ-90.11
        final Frame pz90 = FramesFactory.getPZ9011(IERSConventions.IERS_2010, true);
        final Frame itrf2008 = FramesFactory.getITRF(ITRFVersion.ITRF_2008, IERSConventions.IERS_2010, true);
        final Vector3D comPZ90 = itrf2008.getStaticTransformTo(pz90, new AbsoluteDate(2010, 1, 1, 12, 0, 0, TimeScalesFactory.getTT())).transformPosition(itrf2008P);

        // Check
        Assertions.assertEquals(refPZ90.getX(), comPZ90.getX(), 1.0e-4);
        Assertions.assertEquals(refPZ90.getY(), comPZ90.getY(), 1.0e-4);
        Assertions.assertEquals(refPZ90.getZ(), comPZ90.getZ(), 1.0e-4);
    }

    @Test
    public void testFromITRF2008ToPZ90Field() {
        doTestFromITRF2008ToPZ90Field(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFromITRF2008ToPZ90Field(final Field<T> field)  {
        // Reference for the test
        // "PARAMETRY ZEMLI 1990" (PZ-90.11) Reference Document
        //  MILITARY TOPOGRAPHIC DEPARTMENT OF THE GENERAL STAFF OF ARMED FORCES OF THE RUSSIAN FEDERATION, Moscow, 2014"

        // Position in ITRF-2008
        final FieldVector3D<T> itrf2008P = new FieldVector3D<>(field,
                        new Vector3D(2845455.9753, 2160954.3073, 5265993.2656));

        // Ref position in PZ-90.11
        final FieldVector3D<T> refPZ90   = new FieldVector3D<>(field,
                        new Vector3D(2845455.9772, 2160954.3078, 5265993.2664));

        // Recomputed position in PZ-90.11
        final Frame pz90 = FramesFactory.getPZ9011(IERSConventions.IERS_2010, true);
        final Frame itrf2008 = FramesFactory.getITRF(ITRFVersion.ITRF_2008, IERSConventions.IERS_2010, true);
        final FieldVector3D<T> comPZ90 = itrf2008.getStaticTransformTo(pz90, new AbsoluteDate(2010, 1, 1, 12, 0, 0, TimeScalesFactory.getTT())).transformPosition(itrf2008P);

        // Check
        Assertions.assertEquals(refPZ90.getX().getReal(), comPZ90.getX().getReal(), 1.0e-4);
        Assertions.assertEquals(refPZ90.getY().getReal(), comPZ90.getY().getReal(), 1.0e-4);
        Assertions.assertEquals(refPZ90.getZ().getReal(), comPZ90.getZ().getReal(), 1.0e-4);
    }

    @Test
    public void testPosition() {
        // Frames
        final Frame pz90 = FramesFactory.getPZ9011(IERSConventions.IERS_2010, true);
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        // Initial GLONASS orbital elements (Ref: IGS)
        final GLONASSNavigationMessage ge = new GLONASSNavigationMessage();
        ge.setDate(new GLONASSDate(1342, 4, 45900).getDate());
        ge.setX(-1.0705924E7);
        ge.setXDot(2052.252685546875);
        ge.setXDotDot(0.0);
        ge.setY(-1.5225037E7);
        ge.setYDot(1229.055419921875);
        ge.setYDotDot(-2.7939677238464355E-6);
        ge.setZ(-1.7389698E7);
        ge.setZDot(-2338.376953125);
        ge.setZDotDot(1.862645149230957E-6);
        // Date of the GLONASS orbital elements, 3 Septembre 2019 at 09:45:00 UTC
        final AbsoluteDate target = ge.getDate().shiftedBy(-18.0);
        // 4th order Runge-Kutta
        final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(1.);
        // Initialize the propagator
        final GLONASSNumericalPropagator propagator = new GLONASSNumericalPropagatorBuilder(integrator, ge, true).build();
        // Compute the PV coordinates at the date of the GLONASS orbital elements
        final Vector3D posInPZ90 = propagator.propagate(target).getPosition(pz90);
        final Vector3D computedPos = pz90.getStaticTransformTo(itrf, target).transformPosition(posInPZ90);
        // Expected position (reference from IGS file igv20692_06.sp3)
        final Vector3D expectedPos = new Vector3D(-10742801.600, -15247162.619, -17347541.633);
        // Verify
        Assertions.assertEquals(0., Vector3D.distance(expectedPos, computedPos), 2.8);
    }

    @Test
    public void testIssue544() {
        try {
            Method eMeSinEM = GLONASSNumericalPropagator.class.getDeclaredMethod("eMeSinE",
                                                                                 Double.TYPE, Double.TYPE);
            eMeSinEM.setAccessible(true);
            final double value = (double) eMeSinEM.invoke(null, Double.NaN, Double.NaN);
            // Verify that an infinite loop did not occur
            Assertions.assertTrue(Double.isNaN(value));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIssue1032() {
        final GLONASSNumericalPropagator propagator = new GLONASSNumericalPropagatorBuilder(new ClassicalRungeKuttaIntegrator(10.), ephemeris, false).build();
        Assertions.assertEquals(PropagationType.OSCULATING, propagator.getPropagationType());
    }

}
