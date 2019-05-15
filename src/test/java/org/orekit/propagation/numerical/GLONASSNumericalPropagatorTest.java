/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.propagation.numerical;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.gnss.GLONASSOrbitalElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.GLONASSDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class GLONASSNumericalPropagatorTest {

    private static GLONASSEphemeris ephemeris;

    @BeforeClass
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
        final GLONASSNumericalPropagator propagator = new GLONASSNumericalPropagator.Builder(integrator, ephemeris, false).build();

        // Target date
        final AbsoluteDate target = new AbsoluteDate(new DateComponents(2012, 9, 7),
                                                     new TimeComponents(12300),
                                                     TimeScalesFactory.getGLONASS());

        // Propagation
        final SpacecraftState finalState = propagator.propagate(target);
        final PVCoordinates pvFinal = propagator.getPVInPZ90(finalState);

        // Expected outputs
        final Vector3D expectedPosition = new Vector3D(7523174.819, -10506961.965, 21999239.413);
        final Vector3D expectedVelocity = new Vector3D(950.126007, 2855.687825, 1040.679862);

        // Computed outputs
        final Vector3D computedPosition = pvFinal.getPosition();
        final Vector3D computedVelocity = pvFinal.getVelocity();

        Assert.assertEquals(0.0, computedPosition.distance(expectedPosition), 1.1e-3);
        Assert.assertEquals(0.0, computedVelocity.distance(expectedVelocity), 3.3e-6);
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
        final Vector3D comPZ90 = itrf2008.getTransformTo(pz90, new AbsoluteDate(2010, 1, 1, 12, 0, 0, TimeScalesFactory.getTT())).transformPosition(itrf2008P);

        // Check
        Assert.assertEquals(refPZ90.getX(), comPZ90.getX(), 1.0e-4);
        Assert.assertEquals(refPZ90.getY(), comPZ90.getY(), 1.0e-4);
        Assert.assertEquals(refPZ90.getZ(), comPZ90.getZ(), 1.0e-4);
    }

    @Test
    public void testFromITRF2008ToPZ90Field() {
        doTestFromITRF2008ToPZ90Field(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFromITRF2008ToPZ90Field(final Field<T> field)  {
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
        final FieldVector3D<T> comPZ90 = itrf2008.getTransformTo(pz90, new AbsoluteDate(2010, 1, 1, 12, 0, 0, TimeScalesFactory.getTT())).transformPosition(itrf2008P);

        // Check
        Assert.assertEquals(refPZ90.getX().getReal(), comPZ90.getX().getReal(), 1.0e-4);
        Assert.assertEquals(refPZ90.getY().getReal(), comPZ90.getY().getReal(), 1.0e-4);
        Assert.assertEquals(refPZ90.getZ().getReal(), comPZ90.getZ().getReal(), 1.0e-4);
    }

    /** Internal class used to initialized the {@link GLONASSNumericalPropagator}. */
    private static class GLONASSEphemeris implements GLONASSOrbitalElements {

        private final int n4;
        private final int nt;
        private final double tb;
        private final double x;
        private final double xDot;
        private final double xDotDot;
        private final double y;
        private final double yDot;
        private final double yDotDot;
        private final double z;
        private final double zDot;
        private final double zDotDot;
        

        public GLONASSEphemeris(final int n4, final int nt, final double tb,
                                 final double x, final double xDot, final double xDotDot,
                                 final double y, final double yDot, final double yDotDot,
                                 final double z, final double zDot, final double zDotDot) {
            this.n4 = n4;
            this.nt = nt;
            this.tb = tb;
            this.x = x;
            this.xDot = xDot;
            this.xDotDot = xDotDot;
            this.y = y;
            this.yDot = yDot;
            this.yDotDot = yDotDot;
            this.z = z;
            this.zDot = zDot;
            this.zDotDot = zDotDot;
        }

        @Override
        public AbsoluteDate getDate() {
            return new GLONASSDate(nt, n4, tb).getDate();
        }

        @Override
        public double getTime() {
            return tb;
        }

        @Override
        public double getLambda() {
            // Not used by the Glonass numerical propagator -> set to 0 by default
            return 0;
        }

        @Override
        public double getE() {
            // Not used by the Glonass numerical propagator -> set to 0 by default
            return 0;
        }

        @Override
        public double getPa() {
            // Not used by the Glonass numerical propagator -> set to 0 by default
            return 0;
        }

        @Override
        public double getDeltaI() {
            // Not used by the Glonass numerical propagator -> set to 0 by default
            return 0;
        }

        @Override
        public double getDeltaT() {
            // Not used by the Glonass numerical propagator -> set to 0 by default
            return 0;
        }

        @Override
        public double getDeltaTDot() {
            // Not used by the Glonass numerical propagator -> set to 0 by default
            return 0;
        }

        @Override
        public double getXDot() {
            return xDot;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getXDotDot() {
            return xDotDot;
        }

        @Override
        public double getYDot() {
            return yDot;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getYDotDot() {
            return yDotDot;
        }

        @Override
        public double getZDot() {
            return zDot;
        }

        @Override
        public double getZ() {
            return z;
        }

        @Override
        public double getZDotDot() {
            return zDotDot;
        }

    }
}
