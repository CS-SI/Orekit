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
package org.orekit.attitudes;


import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;


public class NadirPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF
    private Frame itrf;

    /** Test in the case of a spheric earth : nadir pointing shall be
     * the same as earth center pointing
     */
    @Test
    public void testSphericEarth() throws OrekitException {

        // Spheric earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., itrf);

        // Create nadir pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        // Create earth center pointing attitude provider
        BodyCenterPointing earthCenterAttitudeLaw =
                new BodyCenterPointing(FramesFactory.getEME2000(), earthShape);

        // Create satellite position as circular parameters
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Get nadir attitude
        Rotation rotNadir = nadirAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get earth center attitude
        Rotation rotCenter = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // For a spheric earth, earth center pointing attitude and nadir pointing attitude
        // shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        Rotation rotCompo = rotCenter.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, Utils.epsilonAngle);

    }

    /** Test in the case of an elliptic earth : nadir pointing shall be :
     *   - the same as earth center pointing in case of equatorial or polar position
     *   - different from earth center pointing in any other case
     */
    @Test
    public void testNonSphericEarth() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create nadir pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        // Create earth center pointing attitude provider
        BodyCenterPointing earthCenterAttitudeLaw =
                new BodyCenterPointing(FramesFactory.getEME2000(), earthShape);

        //  Satellite on equatorial position
        // **********************************
        KeplerianOrbit kep =
            new KeplerianOrbit(7178000.0, 1.e-8, FastMath.toRadians(50.), 0., 0.,
                                    0., PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);

        // Get nadir attitude
        Rotation rotNadir = nadirAttitudeLaw.getAttitude(kep, date, kep.getFrame()).getRotation();

        // Get earth center attitude
        Rotation rotCenter = earthCenterAttitudeLaw.getAttitude(kep, date, kep.getFrame()).getRotation();

        // For a satellite at equatorial position, earth center pointing attitude and nadir pointing
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        Rotation rotCompo = rotCenter.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        double angle = rotCompo.getAngle();
        Assert.assertEquals(0.0, angle, 5.e-6);

        //  Satellite on polar position
        // *****************************
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(90.), 0.,
                                   FastMath.toRadians(90.), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

       // Get nadir attitude
        rotNadir = nadirAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // For a satellite at polar position, earth center pointing attitude and nadir pointing
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.
        rotCompo = rotCenter.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, 5.e-6);

        //  Satellite on any position
        // ***************************
        circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(50.), 0.,
                                   FastMath.toRadians(90.), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        // Get nadir attitude
        rotNadir = nadirAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // For a satellite at any position, earth center pointing attitude and nadir pointing
        // and nadir pointing attitude shall not be the same, i.e the composition of inverse earth
        // pointing rotation with nadir pointing rotation shall be different from identity.
        rotCompo = rotCenter.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        angle = rotCompo.getAngle();
        Assert.assertEquals(angle, FastMath.toRadians(0.16797386586252272), Utils.epsilonAngle);
    }

    /** Vertical test : check that Z satellite axis is collinear to local vertical axis,
        which direction is : (cos(lon)*cos(lat), sin(lon)*cos(lat), sin(lat)),
        where lon et lat stand for observed point coordinates
        (i.e satellite ones, since they are the same by construction,
        but that's what is to test.
     */
    @Test
    public void testVertical() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create earth center pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        //  Satellite on any position
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(50.), 0.,
                                   FastMath.toRadians(90.), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        //  Vertical test
        // ***************
        // Get observed ground point position/velocity
        TimeStampedPVCoordinates pvTargetItrf = nadirAttitudeLaw.getTargetPV(circ, date, itrf);

        // Convert to geodetic coordinates
        GeodeticPoint geoTarget = earthShape.transform(pvTargetItrf.getPosition(), itrf, date);

        // Compute local vertical axis
        double xVert = FastMath.cos(geoTarget.getLongitude())*FastMath.cos(geoTarget.getLatitude());
        double yVert = FastMath.sin(geoTarget.getLongitude())*FastMath.cos(geoTarget.getLatitude());
        double zVert = FastMath.sin(geoTarget.getLatitude());
        Vector3D targetVertical = new Vector3D(xVert, yVert, zVert);

        // Get attitude rotation state
        Rotation rotSatEME2000 = nadirAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get satellite Z axis in EME2000 frame
        Vector3D zSatEME2000 = rotSatEME2000.applyInverseTo(Vector3D.PLUS_K);
        Vector3D zSatItrf = FramesFactory.getEME2000().getTransformTo(itrf, date).transformVector(zSatEME2000);

        // Check that satellite Z axis is collinear to local vertical axis
        double angle= Vector3D.angle(zSatItrf, targetVertical);
        Assert.assertEquals(0.0, FastMath.sin(angle), Utils.epsilonTest);

    }

    /** Test the derivatives of the sliding target
     */
    @Test
    public void testSlidingDerivatives() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create earth center pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        //  Satellite on any position
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(50.), 0.,
                                   FastMath.toRadians(90.), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt = -0.1; dt < 0.1; dt += 0.05) {
            Orbit o = circ.shiftedBy(dt);
            sample.add(nadirAttitudeLaw.getTargetPV(o, o.getDate(), o.getFrame()));
        }
        TimeStampedPVCoordinates reference =
                TimeStampedPVCoordinates.interpolate(circ.getDate(),
                                                     CartesianDerivativesFilter.USE_P, sample);

        TimeStampedPVCoordinates target =
                nadirAttitudeLaw.getTargetPV(circ, circ.getDate(), circ.getFrame());

        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getPosition(),     target.getPosition()),
                            1.0e-15 * reference.getPosition().getNorm());
        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getVelocity(),     target.getVelocity()),
                            3.0e-11 * reference.getVelocity().getNorm());
        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getAcceleration(), target.getAcceleration()),
                            1.3e-5 * reference.getAcceleration().getNorm());

    }

    @Test
    public void testSpin() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create earth center pointing attitude provider
        NadirPointing law = new NadirPointing(FramesFactory.getEME2000(), earthShape);

        //  Satellite on any position
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, mu);

        Propagator propagator = new KeplerianPropagator(orbit, law, mu, 2500.0);

        double h = 0.1;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 5.3e-9 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 8.1e-9 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Rotation rM = sMinus.getAttitude().getRotation();
        Rotation rP = sPlus.getAttitude().getRotation();
        Vector3D reference = AngularCoordinates.estimateRate(rM, rP, 2 * h);
        Assert.assertTrue(Rotation.distance(rM, rP) > 2.0e-4);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 2.0e-6);

    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Body mu
            mu = 3.9860047e14;

            // Reference frame = ITRF
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
    }

}

