/* Copyright 2022-2023 Romain Serra
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.*;


class GroundPointingTest {
    
    private static Frame INERTIAL_FRAME;
    
    private static Frame OTHER_INERTIAL_FRAME;
    
    private static Frame EARTH_FIXED_FRAME;

    @BeforeAll
    public static void setUp() {

        Utils.setDataRoot("regular-data");

        INERTIAL_FRAME = FramesFactory.getEME2000();
        OTHER_INERTIAL_FRAME = FramesFactory.getGCRF();
        EARTH_FIXED_FRAME = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

    }

    private static class TestGroundPointing extends GroundPointing {
        
        TestGroundPointing(Frame inertialFrame, Frame bodyFrame) {
            super(inertialFrame, bodyFrame);
        }
        
        @Override
        public TimeStampedPVCoordinates getTargetPV(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
            return new TimeStampedPVCoordinates(date, PVCoordinates.ZERO);
        }

        @Override
        public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getTargetPV(FieldPVCoordinatesProvider<T> pvProv, FieldAbsoluteDate<T> date, Frame frame) {
            return new TimeStampedFieldPVCoordinates<T>(date, FieldPVCoordinates.getZero(date.getField()));
        }
        
    }

    @Test
    void templateTestGetRotationSameFrame() {
        templateTestGetRotation(INERTIAL_FRAME);
    }

    @Test
    void templateTestGetRotationDifferentFrame() {
        templateTestGetRotation(OTHER_INERTIAL_FRAME);
    }

    private void templateTestGetRotation(final Frame frame) {
        // GIVEN
        final TestGroundPointing groundPointing = new TestGroundPointing(INERTIAL_FRAME, EARTH_FIXED_FRAME);
        final EquinoctialOrbit orbit = createPVCoordinatesProvider();
        // WHEN
        final Rotation actualRotation = groundPointing.getAttitudeRotation(orbit, orbit.getDate(), frame);
        // THEN
        final Attitude attitude = groundPointing.getAttitude(orbit, orbit.getDate(), frame);
        final Rotation expectedRotation = attitude.getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }
    
    @Test
    void testGetAttitudeRotationFieldSameFrame() {
        templateTestGetRotationField(ComplexField.getInstance(), INERTIAL_FRAME);
    }

    @Test
    void testGetAttitudeRotationFieldDifferentFrame() {
        templateTestGetRotationField(new UnivariateDerivative1(0., 0.).getField(), OTHER_INERTIAL_FRAME);
    }
    
    private <T extends CalculusFieldElement<T>> void templateTestGetRotationField(final Field<T> field,
                                                                                  final Frame frame) {
        // GIVEN
        final TestGroundPointing groundPointing = new TestGroundPointing(INERTIAL_FRAME, EARTH_FIXED_FRAME);
        final EquinoctialOrbit orbit = createPVCoordinatesProvider();
        final FieldEquinoctialOrbit<T> fieldOrbit = convertToField(field, orbit);
        // WHEN
        final FieldRotation<T> actualRotation = groundPointing.getAttitudeRotation(fieldOrbit, fieldOrbit.getDate(), frame);
        // THEN
        final FieldAttitude<T> attitude = groundPointing.getAttitude(fieldOrbit, fieldOrbit.getDate(), frame);
        final FieldRotation<T> expectedRotation = attitude.getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation.toRotation(), actualRotation.toRotation()));
    }

    @Test
    void testGetAttitudeFieldGradient() {
        templateTestGetAttitudeField(GradientField.getField(1));
    }

    @Test
    void testGetAttitudeFieldUnivariateDerivative2() {
        templateTestGetAttitudeField(new UnivariateDerivative2(0., 0., 0.).getField());
    }

    private <T extends CalculusFieldElement<T>> void templateTestGetAttitudeField(final Field<T> field) {
        // GIVEN
        final TestGroundPointing groundPointing = new TestGroundPointing(INERTIAL_FRAME, EARTH_FIXED_FRAME);
        final EquinoctialOrbit orbit = createPVCoordinatesProvider();
        final FieldEquinoctialOrbit<T> fieldOrbit = convertToField(field, orbit);
        // WHEN
        final Attitude actualAttitude = groundPointing.getAttitude(fieldOrbit, fieldOrbit.getDate(), OTHER_INERTIAL_FRAME).toAttitude();
        // THEN
        final Attitude expectedAttitude = groundPointing.getAttitude(orbit, orbit.getDate(), OTHER_INERTIAL_FRAME);
        Assertions.assertEquals(0., Rotation.distance(expectedAttitude.getRotation(), actualAttitude.getRotation()));
        Assertions.assertEquals(expectedAttitude.getSpin(), actualAttitude.getSpin());
        Assertions.assertEquals(expectedAttitude.getRotationAcceleration(), actualAttitude.getRotationAcceleration());
    }

    @Test
    void testGetTargetPosition() {
        // GIVEN
        final TestGroundPointing groundPointing = new TestGroundPointing(INERTIAL_FRAME, EARTH_FIXED_FRAME);
        final EquinoctialOrbit orbit = createPVCoordinatesProvider();
        // WHEN
        final Vector3D actualPosition = groundPointing.getTargetPosition(orbit, orbit.getDate(), INERTIAL_FRAME);
        // THEN
        final Vector3D expectedPosition = groundPointing.getTargetPV(orbit, orbit.getDate(), INERTIAL_FRAME).
                getPosition();
        Assertions.assertEquals(expectedPosition, actualPosition);
    }

    @Test
    void testGetTargetPositionFieldBinary64() {
        templateTestGetTargetPositionField(Binary64Field.getInstance());
    }

    @Test
    void testGetTargetPositionFieldComplex() {
        templateTestGetTargetPositionField(ComplexField.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void templateTestGetTargetPositionField(final Field<T> field) {
        // GIVEN
        final TestGroundPointing groundPointing = new TestGroundPointing(INERTIAL_FRAME, EARTH_FIXED_FRAME);
        final EquinoctialOrbit orbit = createPVCoordinatesProvider();
        final FieldEquinoctialOrbit<T> fieldOrbit = convertToField(field, orbit);
        // WHEN
        final FieldVector3D<T> actualPosition = groundPointing.getTargetPosition(fieldOrbit, fieldOrbit.getDate(), INERTIAL_FRAME);
        // THEN
        final FieldVector3D<T> expectedPosition = groundPointing.getTargetPV(fieldOrbit, fieldOrbit.getDate(), INERTIAL_FRAME).
                getPosition();
        Assertions.assertEquals(expectedPosition, actualPosition);
    }

    private EquinoctialOrbit createPVCoordinatesProvider() {
        final AbsoluteDate epoch = AbsoluteDate.ARBITRARY_EPOCH;
        final double semiMajorAxis = 45000.e3;
        final double mu = Constants.EGM96_EARTH_MU;
        return new EquinoctialOrbit(semiMajorAxis, 0., 0., 0., 0., 0., PositionAngleType.ECCENTRIC, INERTIAL_FRAME, epoch, mu);
    }

    private <T extends CalculusFieldElement<T>> FieldEquinoctialOrbit<T> convertToField(final Field<T> field,
                                                                                        final EquinoctialOrbit orbit) {
        final T zero = field.getZero();
        final T fieldSemiMajorAxis = zero.add(orbit.getA());
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<T>(field, orbit.getDate());
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final T fieldAngle = zero.add(orbit.getL(positionAngleType));
        return new FieldEquinoctialOrbit<>(fieldSemiMajorAxis, zero, zero, zero, zero, fieldAngle,
                positionAngleType, orbit.getFrame(), fieldDate, zero.add(orbit.getMu()));
    }

}