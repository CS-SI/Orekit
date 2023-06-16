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
package org.orekit.bodies;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;

import java.util.ArrayList;
import java.util.List;


public class EllipseTest {

    @Test
    public void testMeridianShape() {
        OneAxisEllipsoid model =
                new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Ellipse e = model.getPlaneSection(new Vector3D(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0),
                                          Vector3D.PLUS_J);
        Assertions.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            e.getA(),
                            1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assertions.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING),
                            e.getB(),
                            1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assertions.assertEquals(0.5 * FastMath.PI, Vector3D.angle(Vector3D.PLUS_J, e.getU()), 1.0e-15);
        Assertions.assertEquals(0.5 * FastMath.PI, Vector3D.angle(Vector3D.PLUS_K, e.getU()), 1.0e-15);
        Assertions.assertEquals(0.5 * FastMath.PI, Vector3D.angle(Vector3D.PLUS_I, e.getV()), 1.0e-15);
        Assertions.assertEquals(0.5 * FastMath.PI, Vector3D.angle(Vector3D.PLUS_J, e.getV()), 1.0e-15);
    }

    @Test
    public void testEquatorialShape() {
        OneAxisEllipsoid model =
                new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Ellipse e = model.getPlaneSection(new Vector3D(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0),
                                          Vector3D.PLUS_K);
        Assertions.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            e.getA(),
                            1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assertions.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            e.getB(),
                            1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
    }

    @Test
    public void testProjectionDerivatives() {
        Ellipse e = new Ellipse(Vector3D.ZERO, Vector3D.PLUS_I, Vector3D.PLUS_J,
                                6.4e6, 6.3e6, FramesFactory.getGCRF());
        TimeStampedPVCoordinates linearMotion =
                new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                             new Vector3D(7.0e6, 5.0e6, 0.0),
                                             new Vector3D(3.0e3, 4.0e3, 0.0),
                                             Vector3D.ZERO);
        TimeStampedPVCoordinates g0 = e.projectToEllipse(linearMotion);
        List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt = -0.25; dt <= 0.25; dt += 0.125) {
            sample.add(e.projectToEllipse(linearMotion.shiftedBy(dt)));
        }

        // create interpolator
        final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_P);

        TimeStampedPVCoordinates ref = interpolator.interpolate(g0.getDate(), sample);
        Assertions.assertEquals(0,
                            Vector3D.distance(g0.getPosition(), ref.getPosition()) / ref.getPosition().getNorm(),
                            1.0e-15);
        Assertions.assertEquals(0,
                            Vector3D.distance(g0.getVelocity(), ref.getVelocity()) / ref.getVelocity().getNorm(),
                            6.0e-12);
        Assertions.assertEquals(0,
                            Vector3D.distance(g0.getAcceleration(), ref.getAcceleration()) / ref.getAcceleration().getNorm(),
                            8.0e-8);

    }

    @Test
    public void testMinRadiusOfCurvature() {
        final double a = 100.0;
        final double b =  50.0;
        Ellipse e = new Ellipse(Vector3D.ZERO, Vector3D.PLUS_I, Vector3D.PLUS_J,
                                a, b, FramesFactory.getGCRF());
        Vector2D point = new Vector2D(10 * a, 0.0);
        Assertions.assertEquals(b * b / a,
                           Vector2D.distance(e.projectToEllipse(point), e.getCenterOfCurvature(point)),
                           1.0e-15);
    }

    @Test
    public void testMaxRadiusOfCurvature() {
        final double a = 100.0;
        final double b =  50.0;
        Ellipse e = new Ellipse(Vector3D.ZERO, Vector3D.PLUS_I, Vector3D.PLUS_J,
                                a, b, FramesFactory.getGCRF());
        Vector2D point = new Vector2D(0.0, 10 * b);
        Assertions.assertEquals(a * a / b,
                           Vector2D.distance(e.projectToEllipse(point), e.getCenterOfCurvature(point)),
                           1.0e-15);
    }

    @Test
    public void testFlatEllipse() {
        final double a = 0.839;
        final double b = 0.176;
        final Ellipse  ellipse   = new Ellipse(Vector3D.ZERO, Vector3D.PLUS_I, Vector3D.PLUS_J,
                                               a, b, FramesFactory.getGCRF());
        final Vector2D close = ellipse.projectToEllipse(new Vector2D(2.0, 4.0));
        Assertions.assertEquals(1.0,
                            close.getX() * close.getX() / (a * a) + close.getY() * close.getY() / (b * b),
                            1.0e-15);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

