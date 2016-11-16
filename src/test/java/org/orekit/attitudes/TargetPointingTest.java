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


import org.hipparchus.geometry.euclidean.threed.Line;
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
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
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
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class TargetPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF
    private Frame itrf;

    // Transform from EME2000 to ITRF
    private Transform eme2000ToItrf;

    /** Test if both constructors are equivalent
     */
    @Test
    public void testConstructors() throws OrekitException {

        //  Satellite position
        // ********************
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        //  Attitude laws
        // ***************
        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Target definition as a geodetic point AND as a position/velocity vector
        GeodeticPoint geoTargetITRF = new GeodeticPoint(FastMath.toRadians(43.36), FastMath.toRadians(1.26), 600.);
        Vector3D pTargetITRF = earthShape.transform(geoTargetITRF);

        // Attitude law definition from geodetic point target
        TargetPointing geoTargetAttitudeLaw = new TargetPointing(circ.getFrame(), geoTargetITRF, earthShape);

        //  Attitude law definition from position/velocity target
        TargetPointing pvTargetAttitudeLaw = new TargetPointing(circ.getFrame(), itrf, pTargetITRF);

        // Check that both attitude are the same
        // Get satellite rotation for target pointing law
        Rotation rotPv = pvTargetAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get satellite rotation for nadir pointing law
        Rotation rotGeo = geoTargetAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Rotations composition
        Rotation rotCompo = rotGeo.composeInverse(rotPv, RotationConvention.VECTOR_OPERATOR);
        double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, Utils.epsilonAngle);

    }

    /** Test if geodetic constructor works
     */
    @Test
    public void testGeodeticConstructor() throws OrekitException {

        //  Satellite position
        // ********************
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        //  Attitude law
        // **************

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Target definition as a geodetic point
        GeodeticPoint geoTargetITRF = new GeodeticPoint(FastMath.toRadians(43.36), FastMath.toRadians(1.26), 600.);

        //  Attitude law definition
        TargetPointing geoTargetAttitudeLaw = new TargetPointing(circ.getFrame(), geoTargetITRF, earthShape);

        // Check that observed ground point is the same as defined target
        Vector3D pObservedEME2000 = geoTargetAttitudeLaw.getTargetPV(circ, date, FramesFactory.getEME2000()).getPosition();
        GeodeticPoint geoObserved = earthShape.transform(pObservedEME2000, FramesFactory.getEME2000(), date);

        Assert.assertEquals(geoObserved.getLongitude(), geoTargetITRF.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoObserved.getLatitude(), geoTargetITRF.getLatitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoObserved.getAltitude(), geoTargetITRF.getAltitude(), 1.1e-8);

    }

    @Test
    public void testIssue115() throws OrekitException {

        //  Satellite position
        // ********************
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        //  Attitude law
        // **************

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Target definition as a geodetic point
        GeodeticPoint geoTargetITRF = new GeodeticPoint(FastMath.toRadians(43.36), FastMath.toRadians(1.26), 600.);

        //  Attitude law definition
        TargetPointing geoTargetAttitudeLaw = new TargetPointing(circ.getFrame(), geoTargetITRF, earthShape);

        // Check that observed ground point is the same as defined target
        Frame cirf = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        Attitude att1 = geoTargetAttitudeLaw.getAttitude(circ, date, cirf);
        Attitude att2 = geoTargetAttitudeLaw.getAttitude(circ, date, itrf);
        Attitude att3 = att2.withReferenceFrame(cirf);
        Assert.assertEquals(0.0, Rotation.distance(att3.getRotation(), att1.getRotation()), 1.0e-15);

    }

    @Test
    public void testWrongFrame() {
        try {
            // in the following line, the frames have been intentionnally reversed
            new TargetPointing(itrf, FramesFactory.getEME2000(),
                               new Vector3D(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, oe.getSpecifier());
            Assert.assertEquals(itrf.getName(), oe.getParts()[0]);
        }
    }

    /** Test with nadir target : Check that when the target is the same as nadir target at date,
     * satellite attitude is the same as nadir attitude at the same date, but different at a different date.
     */
    @Test
    public void testNadirTarget() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Satellite on any position
        CircularOrbit circOrbit =
            new CircularOrbit(7178000.0, 1.e-5, 0., FastMath.toRadians(50.), 0.,
                                   FastMath.toRadians(90.), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        //  Target attitude provider with target under satellite nadir
        // *******************************************************
        // Definition of nadir target
        // Create nadir pointing attitude provider
        NadirPointing nadirAttitudeLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Check nadir target
        Vector3D pNadirTarget  = nadirAttitudeLaw.getTargetPV(circOrbit, date, itrf).getPosition();
        GeodeticPoint geoNadirTarget = earthShape.transform(pNadirTarget, itrf, date);

        // Create target attitude provider
        TargetPointing targetAttitudeLaw = new TargetPointing(circOrbit.getFrame(), geoNadirTarget, earthShape);

        //  1/ Test that attitudes are the same at date
        // *********************************************
        // i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity.

        // Get satellite rotation from target pointing law at date
        Rotation rotTarget = targetAttitudeLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();

        // Get satellite rotation from nadir pointing law at date
        Rotation rotNadir = nadirAttitudeLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();

        // Compose attitude rotations
        Rotation rotCompo = rotTarget.composeInverse(rotNadir, RotationConvention.VECTOR_OPERATOR);
        double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, Utils.epsilonAngle);


        //  2/ Test that attitudes are different at a different date
        // **********************************************************

        // Extrapolation one minute later
        KeplerianPropagator extrapolator = new KeplerianPropagator(circOrbit);
        double delta_t = 60.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = date.shiftedBy(delta_t);
        Orbit extrapOrbit = extrapolator.propagate(extrapDate).getOrbit();


        // Get satellite rotation from target pointing law at date + 1min
        Rotation extrapRotTarget = targetAttitudeLaw.getAttitude(extrapOrbit, extrapDate, extrapOrbit.getFrame()).getRotation();

        // Get satellite rotation from nadir pointing law at date
        Rotation extrapRotNadir = nadirAttitudeLaw.getAttitude(extrapOrbit, extrapDate, extrapOrbit.getFrame()).getRotation();

        // Compose attitude rotations
        Rotation extrapRotCompo = extrapRotTarget.composeInverse(extrapRotNadir, RotationConvention.VECTOR_OPERATOR);
        double extrapAngle = extrapRotCompo.getAngle();
        Assert.assertEquals(extrapAngle, FastMath.toRadians(24.684793905118823), Utils.epsilonAngle);

    }

    /** Test if defined target belongs to the direction pointed by the satellite
     */
    @Test
    public void testTargetInPointingDirection() throws OrekitException {

        // Create computation date
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                             TimeComponents.H00,
                                             TimeScalesFactory.getUTC());

        // Reference frame = ITRF
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create target pointing attitude provider
        GeodeticPoint geoTarget = new GeodeticPoint(FastMath.toRadians(43.36), FastMath.toRadians(1.26), 600.);
        TargetPointing targetAttitudeLaw = new TargetPointing(FramesFactory.getEME2000(), geoTarget, earthShape);

        //  Satellite position
        // ********************
        // Create satellite position as circular parameters
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 frame
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();

        //  Pointing direction
        // ********************
        // Get satellite attitude rotation, i.e rotation from EME2000 frame to satellite frame
        Rotation rotSatEME2000 = targetAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Transform Z axis from satellite frame to EME2000
        Vector3D zSatEME2000 = rotSatEME2000.applyInverseTo(Vector3D.PLUS_K);

        // Line containing satellite point and following pointing direction
        Vector3D p = eme2000ToItrf.transformPosition(pvSatEME2000.getPosition());
        Line pointingLine = new Line(p,
                                     p.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           eme2000ToItrf.transformVector(zSatEME2000)),
                                     1.0e-10);

        // Check that the line contains earth center
        double distance = pointingLine.distance(earthShape.transform(geoTarget));

        Assert.assertEquals(0, distance, 1.e-7);
    }

    /** Test the difference between pointing over two longitudes separated by 5°
     */
    @Test
    public void testSlewedTarget() throws OrekitException {

        // Spheric earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., itrf);

        //  Satellite position
        // ********************
        // Create satellite position as circular parameters
        CircularOrbit circ =
            new CircularOrbit(42164000.0, 0.5e-8, -0.5e-8, 0., 0.,
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Create nadir pointing attitude provider
        // **********************************
        NadirPointing nadirAttitudeLaw = new NadirPointing(circ.getFrame(), earthShape);

        // Get observed ground point from nadir pointing law
        Vector3D pNadirObservedEME2000 = nadirAttitudeLaw.getTargetPV(circ, date, FramesFactory.getEME2000()).getPosition();
        Vector3D pNadirObservedITRF = eme2000ToItrf.transformPosition(pNadirObservedEME2000);

        GeodeticPoint geoNadirObserved = earthShape.transform(pNadirObservedITRF, itrf, date);

        // Create target pointing attitude provider with target equal to nadir target
        // *********************************************************************
        TargetPointing targetLawRef = new TargetPointing(circ.getFrame(), itrf, pNadirObservedITRF);

        // Get attitude rotation in EME2000
        Rotation rotSatRefEME2000 = targetLawRef.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Create target pointing attitude provider with target 5° from nadir target
        // ********************************************************************
        GeodeticPoint geoTarget = new GeodeticPoint(geoNadirObserved.getLatitude(),
                                                    geoNadirObserved.getLongitude() - FastMath.toRadians(5), geoNadirObserved.getAltitude());
        Vector3D pTargetITRF = earthShape.transform(geoTarget);
        TargetPointing targetLaw = new TargetPointing(circ.getFrame(), itrf, pTargetITRF);

        // Get attitude rotation
        Rotation rotSatEME2000 = targetLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Compute difference between both attitude providers
        // *********************************************
        // Difference between attitudes
        //  expected
        double tanDeltaExpected = (6378136.460/(42164000.0-6378136.460))*FastMath.tan(FastMath.toRadians(5));
        double deltaExpected = FastMath.atan(tanDeltaExpected);

        //  real
        double deltaReal = Rotation.distance(rotSatEME2000, rotSatRefEME2000);

        Assert.assertEquals(deltaReal, deltaExpected, 1.e-4);

    }

    @Test
    public void testSpin() throws OrekitException {

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Create target pointing attitude provider
        GeodeticPoint geoTarget = new GeodeticPoint(FastMath.toRadians(43.36), FastMath.toRadians(1.26), 600.);
        AttitudeProvider law = new TargetPointing(FramesFactory.getEME2000(), geoTarget, earthShape);

        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        Propagator propagator = new KeplerianPropagator(orbit, law);

        double h = 0.01;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-5 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-5 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.1e-10);

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

            // Transform from EME2000 to ITRF
            eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(itrf, date);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
        eme2000ToItrf = null;
    }

}

