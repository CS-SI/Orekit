/* Copyright 2002-2023 Luc Maisonobe
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
package org.orekit.bodies;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinatesHermiteInterpolator;

public class FieldEllipseTest {

    @Test
    public void testMeridianShape() {
        doTestMeridianShape(Binary64Field.getInstance());
    }

    @Test
    public void testEquatorialShape() {
        doTestEquatorialShape(Binary64Field.getInstance());
    }

    @Test
    public void testProjectionDerivatives() {
        doTestProjectionDerivatives(Binary64Field.getInstance());
    }

    @Test
    public void testMinRadiusOfCurvature() {
        doTestMinRadiusOfCurvature(Binary64Field.getInstance());
    }

    @Test
    public void testMaxRadiusOfCurvature() {
        doTestMaxRadiusOfCurvature(Binary64Field.getInstance());
    }

    @Test
    public void testFlatEllipse() {
        doTestFlatEllipse(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestMeridianShape(final Field<T> field) {
        OneAxisEllipsoid model =
                new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        FieldEllipse<T> e = model.getPlaneSection(new FieldVector3D<>(field, new Vector3D(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0)),
                                                  FieldVector3D.getPlusJ(field));
        Assertions.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                e.getA().getReal(),
                                1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assertions.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING),
                                e.getB().getReal(),
                                1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assertions.assertEquals(0.5 * FastMath.PI, FieldVector3D.angle(Vector3D.PLUS_J, e.getU()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.5 * FastMath.PI, FieldVector3D.angle(Vector3D.PLUS_K, e.getU()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.5 * FastMath.PI, FieldVector3D.angle(Vector3D.PLUS_I, e.getV()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.5 * FastMath.PI, FieldVector3D.angle(Vector3D.PLUS_J, e.getV()).getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestEquatorialShape(final Field<T> field) {
        OneAxisEllipsoid model =
                new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        FieldEllipse<T> e = model.getPlaneSection(new FieldVector3D<>(field, new Vector3D(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0)),
                                                  FieldVector3D.getPlusK(field));
        Assertions.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                e.getA().getReal(),
                                1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assertions.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                e.getB().getReal(),
                                1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
    }

    private <T extends CalculusFieldElement<T>> void doTestProjectionDerivatives(final Field<T> field) {
        final T zero = field.getZero();
        FieldEllipse<T> e = new FieldEllipse<>(FieldVector3D.getZero(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusJ(field),
                                               zero.newInstance(6.4e6), zero.newInstance(6.3e6),
                                               FramesFactory.getGCRF());
        TimeStampedFieldPVCoordinates<T> linearMotion =
                new TimeStampedFieldPVCoordinates<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                    new FieldVector3D<>(zero.newInstance(7.0e6), zero.newInstance(5.0e6), zero),
                                                    new FieldVector3D<>(zero.newInstance(3.0e3), zero.newInstance(4.0e3), zero),
                                                    FieldVector3D.getZero(field));
        TimeStampedFieldPVCoordinates<T> g0 = e.projectToEllipse(linearMotion);
        List<TimeStampedFieldPVCoordinates<T>> sample = new ArrayList<>();
        for (double dt = -0.25; dt <= 0.25; dt += 0.125) {
            sample.add(e.projectToEllipse(linearMotion.shiftedBy(dt)));
        }

        // create interpolator
        final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<T>, T> interpolator =
                new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(sample.size(), CartesianDerivativesFilter.USE_P);

        TimeStampedFieldPVCoordinates<T> ref = interpolator.interpolate(g0.getDate(), sample);
        Assertions.assertEquals(0,
                                FieldVector3D.distance(g0.getPosition(), ref.getPosition()).divide(ref.getPosition().getNorm()).getReal(),
                                1.0e-15);
        Assertions.assertEquals(0,
                                FieldVector3D.distance(g0.getVelocity(), ref.getVelocity()).divide(ref.getVelocity().getNorm()).getReal(),
                                6.0e-12);
        Assertions.assertEquals(0,
                                FieldVector3D.distance(g0.getAcceleration(), ref.getAcceleration()).divide(ref.getAcceleration().getNorm()).getReal(),
                                8.0e-8);

    }

    private <T extends CalculusFieldElement<T>> void doTestMinRadiusOfCurvature(final Field<T> field) {
        final T zero = field.getZero();
        final double a = 100.0;
        final double b =  50.0;
        FieldEllipse<T> e = new FieldEllipse<>(FieldVector3D.getZero(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusJ(field),
                                               zero.newInstance(a), zero.newInstance(b), FramesFactory.getGCRF());
        FieldVector2D<T> point = new FieldVector2D<>(zero.newInstance(10 * a), zero);
        Assertions.assertEquals(b * b / a,
                                FieldVector2D.distance(e.projectToEllipse(point), e.getCenterOfCurvature(point)).getReal(),
                                1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestMaxRadiusOfCurvature(final Field<T> field) {
        final T zero = field.getZero();
        final double a = 100.0;
        final double b =  50.0;
        FieldEllipse<T> e = new FieldEllipse<>(FieldVector3D.getZero(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusJ(field),
                                               zero.newInstance(a), zero.newInstance(b), FramesFactory.getGCRF());
        FieldVector2D<T> point = new FieldVector2D<>(zero, zero.newInstance(10 * b));
        Assertions.assertEquals(a * a / b,
                                FieldVector2D.distance(e.projectToEllipse(point), e.getCenterOfCurvature(point)).getReal(),
                                1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestFlatEllipse(final Field<T> field) {
        final T zero = field.getZero();
        final double a = 0.839;
        final double b = 0.176;
        FieldEllipse<T> e = new FieldEllipse<>(FieldVector3D.getZero(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusJ(field),
                        zero.newInstance(a), zero.newInstance(b), FramesFactory.getGCRF());
        final FieldVector2D<T> close = e.projectToEllipse(new FieldVector2D<>(zero.newInstance(2.0), zero.newInstance(4.0)));
        Assertions.assertEquals(1.0,
                                close.getX().multiply(close.getX()).divide(a * a).add(close.getY().multiply(close.getY()).divide(b * b)).getReal(),
                                1.0e-15);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

