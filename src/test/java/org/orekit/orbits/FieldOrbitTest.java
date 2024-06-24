/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


class FieldOrbitTest {

    @Test
    void testGetPosition() {
        // GIVEN
        final TestFieldOrbit testFieldOrbit = new TestFieldOrbit(1.);
        final FieldAbsoluteDate<Complex> date = testFieldOrbit.getDate().shiftedBy(0.);
        final Frame frame = testFieldOrbit.getFrame();
        // WHEN
        final FieldVector3D<Complex> actualPosition = testFieldOrbit.getPosition(date, frame);
        // THEN
        final FieldVector3D<Complex> expectedPosition = testFieldOrbit.getPVCoordinates(date, frame).getPosition();
        Assertions.assertEquals(expectedPosition, actualPosition);
    }

    @Test
    void testHasNonKeplerianAccelerationDerivative() {
        // GIVEN
        final GradientField field = GradientField.getField(0);
        final Gradient mu = field.getZero().newInstance(Constants.EGM96_EARTH_MU);
        final FieldPVCoordinates<Gradient> fieldPVCoordinates = createFieldTPVWithKeplerianAcceleration(mu);
        // WHEN
        final boolean actualResult = FieldOrbit.hasNonKeplerianAcceleration(fieldPVCoordinates, mu);
        // THEN
        Assertions.assertFalse(actualResult);
    }

    @Test
    void testIssue1344() {
        // GIVEN
        final Binary64 mu = Binary64.ZERO.newInstance(Constants.EGM96_EARTH_MU);
        final FieldPVCoordinates<Binary64> fieldPVCoordinates = createFieldTPVWithKeplerianAcceleration(mu);
        // WHEN
        final boolean actualResult = FieldOrbit.hasNonKeplerianAcceleration(fieldPVCoordinates, mu);
        // THEN
        Assertions.assertFalse(actualResult);
    }

    private static <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> createFieldTPVWithKeplerianAcceleration(final T mu) {
        final Vector3D position = new Vector3D(1e6, 0, 0);
        final Vector3D keplerianAcceleration = new Vector3D(-mu.getReal() / position.getNormSq() / position.getNorm(),
                position);
        return new FieldPVCoordinates<>(mu.getField(), new PVCoordinates(position, Vector3D.ZERO, keplerianAcceleration));
    }

    @Test
    void testKeplerianMeanMotionAndPeriod() {
        // GIVEN
        final double aIn = 1.;
        final TestFieldOrbit testOrbit = new TestFieldOrbit(aIn);
        // WHEN
        final Complex meanMotion = testOrbit.getKeplerianMeanMotion();
        final Complex period = testOrbit.getKeplerianPeriod();
        final double actualValue = period.multiply(meanMotion).getReal();
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

    private void templateTestIsElliptical(final double aIn) {
        // GIVEN
        final TestFieldOrbit testOrbit = new TestFieldOrbit(aIn);
        // WHEN
        final boolean actualValue = testOrbit.isElliptical();
        // THEN
        final boolean expectedValue = aIn > 0.;
        Assertions.assertEquals(expectedValue, actualValue);
    }

    private static class TestFieldOrbit extends FieldOrbit<Complex> {

        final Complex a;

        protected TestFieldOrbit(final double aIn)
                throws IllegalArgumentException {
            super(FramesFactory.getGCRF(), FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance()), Complex.ONE);
            a = new Complex(aIn, 0.);
        }

        @Override
        public OrbitType getType() {
            return null;
        }

        @Override
        public Orbit toOrbit() {
            return null;
        }

        @Override
        public Complex getA() {
            return this.a;
        }

        @Override
        public Complex getADot() {
            return null;
        }

        @Override
        public Complex getEquinoctialEx() {
            return null;
        }

        @Override
        public Complex getEquinoctialExDot() {
            return null;
        }

        @Override
        public Complex getEquinoctialEy() {
            return null;
        }

        @Override
        public Complex getEquinoctialEyDot() {
            return null;
        }

        @Override
        public Complex getHx() {
            return null;
        }

        @Override
        public Complex getHxDot() {
            return null;
        }

        @Override
        public Complex getHy() {
            return null;
        }

        @Override
        public Complex getHyDot() {
            return null;
        }

        @Override
        public Complex getLE() {
            return null;
        }

        @Override
        public Complex getLEDot() {
            return null;
        }

        @Override
        public Complex getLv() {
            return null;
        }

        @Override
        public Complex getLvDot() {
            return null;
        }

        @Override
        public Complex getLM() {
            return null;
        }

        @Override
        public Complex getLMDot() {
            return null;
        }

        @Override
        public Complex getE() {
            return null;
        }

        @Override
        public Complex getEDot() {
            return null;
        }

        @Override
        public Complex getI() {
            return null;
        }

        @Override
        public Complex getIDot() {
            return null;
        }

        @Override
        public boolean hasDerivatives() {
            return false;
        }

        @Override
        protected FieldVector3D<Complex> initPosition() {
            return new FieldVector3D<>(getField(), new Vector3D(a.getReal(), 0., 0.));
        }

        @Override
        protected TimeStampedFieldPVCoordinates<Complex> initPVCoordinates() {
            final FieldPVCoordinates<Complex> fieldPVCoordinates = new FieldPVCoordinates<>(initPosition(),
                    FieldVector3D.getZero(getField()));
            return new TimeStampedFieldPVCoordinates<>(getDate(), fieldPVCoordinates);
        }

        @Override
        public FieldOrbit<Complex> shiftedBy(Complex dt) {
            return shiftedBy(dt.getReal());
        }

        @Override
        public FieldOrbit<Complex> shiftedBy(double dt) {
            return new TestFieldOrbit(a.getReal());
        }

        @Override
        protected Complex[][] computeJacobianMeanWrtCartesian() {
            return null;
        }

        @Override
        protected Complex[][] computeJacobianEccentricWrtCartesian() {
            return null;
        }

        @Override
        protected Complex[][] computeJacobianTrueWrtCartesian() {
            return null;
        }

        @Override
        public void addKeplerContribution(PositionAngleType type, Complex gm, Complex[] pDot) {

        }

    }
    
}
