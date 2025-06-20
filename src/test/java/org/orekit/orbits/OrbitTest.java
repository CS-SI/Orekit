/* Copyright 2022-2025 Romain Serra
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
package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


class OrbitTest {

    @Test
    void testGetPosition() {
        // GIVEN
        final double aIn = 1.;
        final TestOrbit testOrbit = new TestOrbit(aIn);
        final AbsoluteDate date = testOrbit.getDate().shiftedBy(0.);
        // WHEN
        final Vector3D actualPosition = testOrbit.getPosition(date, testOrbit.getFrame());
        // THEN
        final Vector3D expectedPosition = testOrbit.getPVCoordinates(date, testOrbit.getFrame()).getPosition();
        Assertions.assertEquals(expectedPosition, actualPosition);
    }

    @Test
    void testKeplerianMeanMotionAndPeriod() {
        // GIVEN
        final double aIn = 1.;
        final TestOrbit testOrbit = new TestOrbit(aIn);
        // WHEN
        final double meanMotion = testOrbit.getKeplerianMeanMotion();
        final double period = testOrbit.getKeplerianPeriod();
        final double actualValue = period * meanMotion;
        // THEN
        final double expectedValue = MathUtils.TWO_PI;
        Assertions.assertEquals(expectedValue, actualValue, 1e-10);
    }

    @Test
    void testIsElliptical() {
        templateTestIsElliptical(1.);
    }

    @Test
    void testIsNotElliptical() {
        templateTestIsElliptical(-1.);
    }

    @Test
    public void testIssue1557() {
        // GIVEN
        final Orbit fakeOrbit = TestUtils.getFakeOrbit();

        // WHEN
        final Vector3D velocity = fakeOrbit.getVelocity();

        // THEN
        Assertions.assertEquals(fakeOrbit.getPVCoordinates().getVelocity(), velocity);
    }

    private void templateTestIsElliptical(final double aIn) {
        // GIVEN
        final TestOrbit testOrbit = new TestOrbit(aIn);
        // WHEN
        final boolean actualValue = testOrbit.isElliptical();
        // THEN
        final boolean expectedValue = aIn > 0.;
        Assertions.assertEquals(expectedValue, actualValue);
    }

    private static class TestOrbit extends Orbit {

        final double a;

        protected TestOrbit(final double aIn) throws IllegalArgumentException {
            super(FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
            this.a = aIn;
        }

        @Override
        public OrbitType getType() {
            return null;
        }

        @Override
        public double getA() {
            return this.a;
        }

        @Override
        public double getADot() {
            return 0;
        }

        @Override
        public double getEquinoctialEx() {
            return 0;
        }

        @Override
        public double getEquinoctialExDot() {
            return 0;
        }

        @Override
        public double getEquinoctialEy() {
            return 0;
        }

        @Override
        public double getEquinoctialEyDot() {
            return 0;
        }

        @Override
        public double getHx() {
            return 0;
        }

        @Override
        public double getHxDot() {
            return 0;
        }

        @Override
        public double getHy() {
            return 0;
        }

        @Override
        public double getHyDot() {
            return 0;
        }

        @Override
        public double getLE() {
            return 0;
        }

        @Override
        public double getLEDot() {
            return 0;
        }

        @Override
        public double getLv() {
            return 0;
        }

        @Override
        public double getLvDot() {
            return 0;
        }

        @Override
        public double getLM() {
            return 0;
        }

        @Override
        public double getLMDot() {
            return 0;
        }

        @Override
        public double getE() {
            return 0;
        }

        @Override
        public double getEDot() {
            return 0;
        }

        @Override
        public double getI() {
            return 0;
        }

        @Override
        public double getIDot() {
            return 0;
        }

        @Override
        protected Vector3D initPosition() {
            return new Vector3D(a, 0., 0.);
        }

        @Override
        protected TimeStampedPVCoordinates initPVCoordinates() {
            return new TimeStampedPVCoordinates(getDate(), new PVCoordinates(initPosition(), Vector3D.ZERO));
        }

        @Override
        public Orbit inFrame(Frame inertialFrame) {
            return null;
        }

        @Override
        public Orbit shiftedBy(double dt) {
            return new TestOrbit(a);
        }

        @Override
        public Orbit shiftedBy(TimeOffset dt) {
            return new TestOrbit(a);
        }

        @Override
        protected double[][] computeJacobianMeanWrtCartesian() {
            return new double[0][];
        }

        @Override
        protected double[][] computeJacobianEccentricWrtCartesian() {
            return new double[0][];
        }

        @Override
        protected double[][] computeJacobianTrueWrtCartesian() {
            return new double[0][];
        }

        @Override
        public void addKeplerContribution(PositionAngleType type, double gm, double[] pDot) {

        }
    }

}
