/* Copyright 2002-2024 CS GROUP
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

package org.orekit.utils;

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.TimeScalesFactory;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

class TimeStampedFieldAngularCoordinatesHermiteInterpolatorTest {
    @Test
    void testInterpolationNeedOffsetWrongRate() {
        AbsoluteDate date  = AbsoluteDate.GALILEO_EPOCH;
        double       omega = 2.0 * FastMath.PI;
        TimeStampedFieldAngularCoordinates<DerivativeStructure> reference =
                new TimeStampedFieldAngularCoordinates<>(date,
                                                         TimeStampedFieldAngularCoordinatesTest.createRotation(1, 0, 0, 0,
                                                                                                               false),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, -omega,
                                                                                                             4),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0, 4));

        List<TimeStampedFieldAngularCoordinates<DerivativeStructure>> sample = new ArrayList<>();
        for (double dt : new double[] { 0.0, 0.25, 0.5, 0.75, 1.0 }) {
            TimeStampedFieldAngularCoordinates<DerivativeStructure> shifted = reference.shiftedBy(dt);
            sample.add(new TimeStampedFieldAngularCoordinates<>(shifted.getDate(),
                                                                shifted.getRotation(),
                                                                TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0,
                                                                                                                    4),
                                                                TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0,
                                                                                                                    4)));
        }

        // Create interpolator
        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<DerivativeStructure>, DerivativeStructure>
                interpolator = new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(sample.size());

        for (TimeStampedFieldAngularCoordinates<DerivativeStructure> s : sample) {
            TimeStampedFieldAngularCoordinates<DerivativeStructure> interpolated =
                    interpolator.interpolate(s.getDate(), sample);
            FieldRotation<DerivativeStructure> r    = interpolated.getRotation();
            FieldVector3D<DerivativeStructure> rate = interpolated.getRotationRate();
            assertEquals(0.0, FieldRotation.distance(s.getRotation(), r).getReal(), 2.0e-14);
            assertEquals(0.0, FieldVector3D.distance(s.getRotationRate(), rate).getReal(), 2.0e-13);
        }

    }

    @Test
    void testInterpolationRotationOnly() {
        AbsoluteDate date   = AbsoluteDate.GALILEO_EPOCH;
        double       alpha0 = 0.5 * FastMath.PI;
        double       omega  = 0.5 * FastMath.PI;
        TimeStampedFieldAngularCoordinates<DerivativeStructure> reference =
                new TimeStampedFieldAngularCoordinates<>(date,
                                                         TimeStampedFieldAngularCoordinatesTest.createRotation(
                                                                 TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 1,
                                                                                                                     4),
                                                                 alpha0),
                                                         new FieldVector3D<>(omega,
                                                                             TimeStampedFieldAngularCoordinatesTest.createVector(
                                                                                     0, 0, -1, 4)),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0, 4));

        List<TimeStampedFieldAngularCoordinates<DerivativeStructure>> sample = new ArrayList<>();
        for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
            FieldRotation<DerivativeStructure> r = reference.shiftedBy(dt).getRotation();
            sample.add(new TimeStampedFieldAngularCoordinates<>(date.shiftedBy(dt), r,
                                                                TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0,
                                                                                                                    4),
                                                                TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0,
                                                                                                                    4)));
        }

        // Create interpolator
        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<DerivativeStructure>, DerivativeStructure>
                interpolator =
                new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(sample.size(), AngularDerivativesFilter.USE_R);

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            TimeStampedFieldAngularCoordinates<DerivativeStructure> interpolated =
                    interpolator.interpolate(date.shiftedBy(dt), sample);
            FieldRotation<DerivativeStructure> r            = interpolated.getRotation();
            FieldVector3D<DerivativeStructure> rate         = interpolated.getRotationRate();
            FieldVector3D<DerivativeStructure> acceleration = interpolated.getRotationAcceleration();
            assertEquals(0.0, FieldRotation.distance(reference.shiftedBy(dt).getRotation(), r).getReal(), 3.0e-4);
            assertEquals(0.0, FieldVector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate).getReal(),
                                    1.0e-2);
            assertEquals(0.0,
                                    FieldVector3D.distance(reference.shiftedBy(dt).getRotationAcceleration(), acceleration)
                                                 .getReal(), 1.0e-2);
        }

    }

    @Test
    void testInterpolationAroundPI() {

        DSFactory                                                     factory = new DSFactory(4, 1);
        List<TimeStampedFieldAngularCoordinates<DerivativeStructure>> sample  = new ArrayList<>();

        // add angular coordinates at t0: 179.999 degrees rotation along X axis
        AbsoluteDate t0 = new AbsoluteDate("2012-01-01T00:00:00.000", TimeScalesFactory.getTAI());
        TimeStampedFieldAngularCoordinates<DerivativeStructure> ac0 =
                new TimeStampedFieldAngularCoordinates<>(t0,
                                                         new FieldRotation<>(
                                                                 TimeStampedFieldAngularCoordinatesTest.createVector(1, 0, 0,
                                                                                                                     4),
                                                                 factory.variable(3, FastMath.toRadians(179.999)),
                                                                 RotationConvention.VECTOR_OPERATOR),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(
                                                                 FastMath.toRadians(0), 0, 0, 4),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0, 4));
        sample.add(ac0);

        // add angular coordinates at t1: -179.999 degrees rotation (= 180.001 degrees) along X axis
        AbsoluteDate t1 = new AbsoluteDate("2012-01-01T00:00:02.000", TimeScalesFactory.getTAI());
        TimeStampedFieldAngularCoordinates<DerivativeStructure> ac1 =
                new TimeStampedFieldAngularCoordinates<>(t1,
                                                         new FieldRotation<>(
                                                                 TimeStampedFieldAngularCoordinatesTest.createVector(1, 0, 0,
                                                                                                                     4),
                                                                 factory.variable(3, FastMath.toRadians(-179.999)),
                                                                 RotationConvention.VECTOR_OPERATOR),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(
                                                                 FastMath.toRadians(0), 0, 0, 4),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0, 4));
        sample.add(ac1);

        // Create interpolator
        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<DerivativeStructure>, DerivativeStructure>
                interpolator =
                new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(sample.size(), AngularDerivativesFilter.USE_R);

        // get interpolated angular coordinates at mid time between t0 and t1
        AbsoluteDate t = new AbsoluteDate("2012-01-01T00:00:01.000", TimeScalesFactory.getTAI());
        TimeStampedFieldAngularCoordinates<DerivativeStructure> interpolated =
                interpolator.interpolate(t, sample);

        assertEquals(FastMath.toRadians(180), interpolated.getRotation().getAngle().getReal(), 1.0e-12);

    }

    /**
     * Check interpolation with a small sample. This method used to check an exception was
     * thrown. Now after changing USE_R to USE_RR it produces a valid result.
     */
    @Test
    void testInterpolationSmallSample() {
        DSFactory    factory = new DSFactory(4, 1);
        AbsoluteDate date    = AbsoluteDate.GALILEO_EPOCH;
        double       alpha0  = 0.5 * FastMath.PI;
        double       omega   = 0.5 * FastMath.PI;
        TimeStampedFieldAngularCoordinates<DerivativeStructure> reference =
                new TimeStampedFieldAngularCoordinates<>(date,
                                                         new FieldRotation<>(
                                                                 TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 1,
                                                                                                                     4),
                                                                 factory.variable(3, alpha0),
                                                                 RotationConvention.VECTOR_OPERATOR),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, -omega,
                                                                                                             4),
                                                         TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0, 4));

        List<TimeStampedFieldAngularCoordinates<DerivativeStructure>> sample = new ArrayList<>();
        FieldRotation<DerivativeStructure>                            r      = reference.shiftedBy(0.2).getRotation();
        sample.add(new TimeStampedFieldAngularCoordinates<>(date.shiftedBy(0.2), r,
                                                            TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0, 4),
                                                            TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0,
                                                                                                                4)));

        // Create interpolator
        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<DerivativeStructure>, DerivativeStructure>
                interpolator =
                new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(sample.size(), AngularDerivativesFilter.USE_RR);

        // action
        final TimeStampedFieldAngularCoordinates<DerivativeStructure> actual =
                interpolator.interpolate(date.shiftedBy(0.3), sample);

        // verify
        assertThat(actual.getRotation().toRotation(),
                OrekitMatchers.distanceIs(
                        r.toRotation(),
                        OrekitMatchers.closeTo(0, FastMath.ulp(1.0))));
        assertThat(actual.getRotationRate().toVector3D(),
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, 0));
        assertThat(actual.getRotationAcceleration().toVector3D(),
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, 0));
    }

    @Test
    void testInterpolationGTODIssue() {
        AbsoluteDate t0 = new AbsoluteDate("2004-04-06T19:59:28.000", TimeScalesFactory.getTAI());
        double[][] params = new double[][] {
                { 0.0, -0.3802356750911964, -0.9248896320037013, 7.292115030462892e-5 },
                { 4.0, 0.1345716955788532, -0.990903859488413, 7.292115033301528e-5 },
                { 8.0, -0.613127541102373, 0.7899839354960061, 7.292115037371062e-5 }
        };
        List<TimeStampedFieldAngularCoordinates<DerivativeStructure>> sample =
                new ArrayList<TimeStampedFieldAngularCoordinates<DerivativeStructure>>();
        for (double[] row : params) {
            AbsoluteDate t = t0.shiftedBy(row[0] * 3600.0);
            FieldRotation<DerivativeStructure> r =
                    TimeStampedFieldAngularCoordinatesTest.createRotation(row[1], 0.0, 0.0, row[2], false);
            FieldVector3D<DerivativeStructure> o =
                    new FieldVector3D<>(row[3], TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 1, 4));
            sample.add(new TimeStampedFieldAngularCoordinates<>(t, r, o,
                                                                TimeStampedFieldAngularCoordinatesTest.createVector(0, 0, 0,
                                                                                                                    4)));
        }

        // Create interpolator
        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<DerivativeStructure>, DerivativeStructure>
                interpolator = new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(sample.size(), 200,
                                                                                           AngularDerivativesFilter.USE_RR);

        for (double dt = 0; dt < 29000; dt += 120) {
            TimeStampedFieldAngularCoordinates<DerivativeStructure> shifted = sample.get(0).shiftedBy(dt);
            TimeStampedFieldAngularCoordinates<DerivativeStructure> interpolated =
                    interpolator.interpolate(t0.shiftedBy(dt), sample);
            assertEquals(0.0, FieldRotation.distance(shifted.getRotation(), interpolated.getRotation()).getReal(),
                                    1.3e-7);
            assertEquals(0.0, FieldVector3D.distance(shifted.getRotationRate(), interpolated.getRotationRate())
                                                      .getReal(), 1.0e-11);
        }

    }

    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        // Given
        final AngularDerivativesFilter filter = AngularDerivativesFilter.USE_R;

        // When
        final TimeStampedFieldAngularCoordinatesHermiteInterpolator<Binary64> interpolator =
                new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(2, filter);

        // Then
        assertEquals(AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                interpolator.getExtrapolationThreshold());
        assertEquals(filter, interpolator.getFilter());
    }

}
