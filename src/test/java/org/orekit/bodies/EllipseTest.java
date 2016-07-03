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
package org.orekit.bodies;


import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;


public class EllipseTest {

    @Test
    public void testMeridianShape() throws OrekitException {
        OneAxisEllipsoid model =
                new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Ellipse e = model.getPlaneSection(new Vector3D(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0),
                                          Vector3D.PLUS_J);
        Assert.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            e.getA(),
                            1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assert.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING),
                            e.getB(),
                            1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assert.assertEquals(0.5 * FastMath.PI, Vector3D.angle(Vector3D.PLUS_J, e.getU()), 1.0e-15);
        Assert.assertEquals(0.5 * FastMath.PI, Vector3D.angle(Vector3D.PLUS_K, e.getU()), 1.0e-15);
        Assert.assertEquals(0.5 * FastMath.PI, Vector3D.angle(Vector3D.PLUS_I, e.getV()), 1.0e-15);
        Assert.assertEquals(0.5 * FastMath.PI, Vector3D.angle(Vector3D.PLUS_J, e.getV()), 1.0e-15);
    }

    @Test
    public void testEquatorialShape() throws OrekitException {
        OneAxisEllipsoid model =
                new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Ellipse e = model.getPlaneSection(new Vector3D(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0),
                                          Vector3D.PLUS_K);
        Assert.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            e.getA(),
                            1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        Assert.assertEquals(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            e.getB(),
                            1.0e-15 * Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
    }

    @Test
    public void testProjectionDerivatives() throws OrekitException {
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
        TimeStampedPVCoordinates ref = TimeStampedPVCoordinates.interpolate(g0.getDate(),
                                                                            CartesianDerivativesFilter.USE_P,
                                                                            sample);
        Assert.assertEquals(0,
                            Vector3D.distance(g0.getPosition(), ref.getPosition()) / ref.getPosition().getNorm(),
                            1.0e-15);
        Assert.assertEquals(0,
                            Vector3D.distance(g0.getVelocity(), ref.getVelocity()) / ref.getVelocity().getNorm(),
                            6.0e-12);
        Assert.assertEquals(0,
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
        Assert.assertEquals(b * b / a,
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
        Assert.assertEquals(a * a / b,
                           Vector2D.distance(e.projectToEllipse(point), e.getCenterOfCurvature(point)),
                           1.0e-15);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

