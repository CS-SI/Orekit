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
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class YawCompensationTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF
    private Frame itrf;

    // Satellite position
    CircularOrbit circOrbit;

    // Earth shape
    OneAxisEllipsoid earthShape;

    /** Test that pointed target remains the same with or without yaw compensation
     */
    @Test
    public void testTarget() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Target pointing attitude provider with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(circOrbit.getFrame(), nadirLaw);

        //  Check target
        // *************
        // without yaw compensation
        TimeStampedPVCoordinates noYawObserved = nadirLaw.getTargetPV(circOrbit, date, itrf);

        // with yaw compensation
        TimeStampedPVCoordinates yawObserved = yawCompensLaw.getTargetPV(circOrbit, date, itrf);

        // Check difference
        PVCoordinates observedDiff = new PVCoordinates(yawObserved, noYawObserved);

        Assert.assertEquals(0.0, observedDiff.getPosition().getNorm(), Utils.epsilonTest);
        Assert.assertEquals(0.0, observedDiff.getVelocity().getNorm(), Utils.epsilonTest);
        Assert.assertEquals(0.0, observedDiff.getAcceleration().getNorm(), Utils.epsilonTest);
        Assert.assertSame(nadirLaw, yawCompensLaw.getUnderlyingAttitudeProvider());

    }

    /** Test the derivatives of the sliding target
     */
    @Test
    public void testSlidingDerivatives() throws OrekitException {

        GroundPointing law =
                new YawCompensation(circOrbit.getFrame(), new NadirPointing(circOrbit.getFrame(), earthShape));

        List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt = -0.1; dt < 0.1; dt += 0.01) {
            Orbit o = circOrbit.shiftedBy(dt);
            sample.add(law.getTargetPV(o, o.getDate(), o.getFrame()));
        }
        TimeStampedPVCoordinates reference =
                TimeStampedPVCoordinates.interpolate(circOrbit.getDate(),
                                                     CartesianDerivativesFilter.USE_P, sample);

        TimeStampedPVCoordinates target =
                law.getTargetPV(circOrbit, circOrbit.getDate(), circOrbit.getFrame());

        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getPosition(),     target.getPosition()),
                            1.0e-15 * reference.getPosition().getNorm());
        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getVelocity(),     target.getVelocity()),
                            3.0e-11 * reference.getVelocity().getNorm());
        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getAcceleration(), target.getAcceleration()),
                            7.0e-6 * reference.getAcceleration().getNorm());

    }

    /** Test that pointed target motion is along -X sat axis
     */
    @Test
    public void testAlignment() throws OrekitException {

        GroundPointing   notCompensated = new NadirPointing(circOrbit.getFrame(), earthShape);
        YawCompensation compensated     = new YawCompensation(circOrbit.getFrame(), notCompensated);
        Attitude         att0           = compensated.getAttitude(circOrbit, circOrbit.getDate(), circOrbit.getFrame());

        // ground point in satellite Z direction
        Vector3D satInert = circOrbit.getPVCoordinates().getPosition();
        Vector3D zInert   = att0.getRotation().applyInverseTo(Vector3D.PLUS_K);
        GeodeticPoint gp  = earthShape.getIntersectionPoint(new Line(satInert,
                                                                     satInert.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, zInert),
                                                                     1.0e-10),
                                                            satInert,
                                                            circOrbit.getFrame(), circOrbit.getDate());
        PVCoordinates pEarth   = new PVCoordinates(earthShape.transform(gp), Vector3D.ZERO, Vector3D.ZERO);

        double minYWithoutCompensation    = Double.POSITIVE_INFINITY;
        double maxYWithoutCompensation    = Double.NEGATIVE_INFINITY;
        double minYDotWithoutCompensation = Double.POSITIVE_INFINITY;
        double maxYDotWithoutCompensation = Double.NEGATIVE_INFINITY;
        double minYWithCompensation       = Double.POSITIVE_INFINITY;
        double maxYWithCompensation       = Double.NEGATIVE_INFINITY;
        double minYDotWithCompensation    = Double.POSITIVE_INFINITY;
        double maxYDotWithCompensation    = Double.NEGATIVE_INFINITY;
        for (double dt = -0.2; dt < 0.2; dt += 0.002) {

            PVCoordinates withoutCompensation = toSpacecraft(pEarth, circOrbit.shiftedBy(dt), notCompensated);
            if (FastMath.abs(withoutCompensation.getPosition().getX()) <= 1000.0) {
                minYWithoutCompensation    = FastMath.min(minYWithoutCompensation,    withoutCompensation.getPosition().getY());
                maxYWithoutCompensation    = FastMath.max(maxYWithoutCompensation,    withoutCompensation.getPosition().getY());
                minYDotWithoutCompensation = FastMath.min(minYDotWithoutCompensation, withoutCompensation.getVelocity().getY());
                maxYDotWithoutCompensation = FastMath.max(maxYDotWithoutCompensation, withoutCompensation.getVelocity().getY());
            }

            PVCoordinates withCompensation    = toSpacecraft(pEarth, circOrbit.shiftedBy(dt), compensated);
            if (FastMath.abs(withCompensation.getPosition().getX()) <= 1000.0) {
                minYWithCompensation    = FastMath.min(minYWithCompensation,    withCompensation.getPosition().getY());
                maxYWithCompensation    = FastMath.max(maxYWithCompensation,    withCompensation.getPosition().getY());
                minYDotWithCompensation = FastMath.min(minYDotWithCompensation, withCompensation.getVelocity().getY());
                maxYDotWithCompensation = FastMath.max(maxYDotWithCompensation, withCompensation.getVelocity().getY());
            }

        }

        // when the ground point is close to cross the push-broom line (i.e. when Δx decreases from +1000m to -1000m)
        // it will drift along the Y axis if we don't apply compensation
        // but will remain nearly at Δy=0 if we do apply compensation
        // in fact, as the yaw compensation mode removes the linear drift,
        // what remains is a parabola Δy = a uΔx²
        Assert.assertEquals(-55.7056, minYWithoutCompensation,    0.0001);
        Assert.assertEquals(+55.7056, maxYWithoutCompensation,    0.0001);
        Assert.assertEquals(352.5667, minYDotWithoutCompensation, 0.0001);
        Assert.assertEquals(352.5677, maxYDotWithoutCompensation, 0.0001);
        Assert.assertEquals(  0.0000, minYWithCompensation,       0.0001);
        Assert.assertEquals(  0.0008, maxYWithCompensation,       0.0001);
        Assert.assertEquals( -0.0101, minYDotWithCompensation,    0.0001);
        Assert.assertEquals(  0.0102, maxYDotWithCompensation,    0.0001);

    }

    PVCoordinates toSpacecraft(PVCoordinates groundPoint, Orbit orbit, AttitudeProvider attitudeProvider)
        throws OrekitException {
        SpacecraftState state =
                new SpacecraftState(orbit, attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        Transform earthToSc =
                new Transform(orbit.getDate(),
                              earthShape.getBodyFrame().getTransformTo(orbit.getFrame(), orbit.getDate()),
                              state.toTransform());
        return earthToSc.transformPVCoordinates(groundPoint);
    }

    /** Test that maximum yaw compensation is at ascending/descending node,
     * and minimum yaw compensation is at maximum latitude.
     */
    @Test
    public void testCompensMinMax() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Target pointing attitude provider with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(circOrbit.getFrame(), nadirLaw);


        // Extrapolation over one orbital period (sec)
        double duration = circOrbit.getKeplerianPeriod();
        KeplerianPropagator extrapolator = new KeplerianPropagator(circOrbit);

        // Extrapolation initializations
        double delta_t = 15.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = date; // extrapolation start date

        // Min initialization
        double yawMin = 1.e+12;
        double latMin = 0.;

        while (extrapDate.durationFrom(date) < duration)  {
            extrapDate = extrapDate.shiftedBy(delta_t);

            // Extrapolated orbit state at date
            Orbit extrapOrbit = extrapolator.propagate(extrapDate).getOrbit();
            PVCoordinates extrapPvSatEME2000 = extrapOrbit.getPVCoordinates();

            // Satellite latitude at date
            double extrapLat =
                earthShape.transform(extrapPvSatEME2000.getPosition(), FramesFactory.getEME2000(), extrapDate).getLatitude();

            // Compute yaw compensation angle -- rotations composition
            double yawAngle = yawCompensLaw.getYawAngle(extrapOrbit, extrapDate, extrapOrbit.getFrame());

           // Update minimum yaw compensation angle
            if (FastMath.abs(yawAngle) <= yawMin) {
                yawMin = FastMath.abs(yawAngle);
                latMin = extrapLat;
            }

            //     Checks
            // ------------------

            // 1/ Check yaw values around ascending node (max yaw)
            if ((FastMath.abs(extrapLat) < FastMath.toRadians(2.)) &&
                (extrapPvSatEME2000.getVelocity().getZ() >= 0. )) {
                Assert.assertEquals(-3.206, FastMath.toDegrees(yawAngle), 0.003);
            }

            // 2/ Check yaw values around maximum positive latitude (min yaw)
            if ( extrapLat > FastMath.toRadians(50.15) ) {
                Assert.assertEquals(0, FastMath.toDegrees(yawAngle), 0.15);
            }

            // 3/ Check yaw values around descending node (max yaw)
            if ( (FastMath.abs(extrapLat) < FastMath.toRadians(2.))
                    && (extrapPvSatEME2000.getVelocity().getZ() <= 0. ) ) {
                Assert.assertEquals(3.206, FastMath.toDegrees(yawAngle), 0.003);
            }

            // 4/ Check yaw values around maximum negative latitude (min yaw)
            if ( extrapLat < FastMath.toRadians(-50.15) ) {
                Assert.assertEquals(0, FastMath.toDegrees(yawAngle), 0.15);
            }

        }

        // 5/ Check that minimum yaw compensation value is around maximum latitude
        Assert.assertEquals( 0.0, FastMath.toDegrees(yawMin), 0.004);
        Assert.assertEquals(50.0, FastMath.toDegrees(latMin), 0.22);

    }

    /** Test that compensation rotation axis is Zsat, yaw axis
     */
    @Test
    public void testCompensAxis() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Target pointing attitude provider with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(circOrbit.getFrame(), nadirLaw);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        Rotation rotNoYaw = nadirLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();
        Rotation rotYaw = yawCompensLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();

        // Compose rotations composition
        Rotation compoRot = rotYaw.compose(rotNoYaw.revert(), RotationConvention.VECTOR_OPERATOR);
        Vector3D yawAxis = compoRot.getAxis(RotationConvention.VECTOR_OPERATOR);

        // Check axis
        Assert.assertEquals(0., yawAxis.subtract(Vector3D.PLUS_K).getNorm(), Utils.epsilonTest);

    }

    @Test
    public void testSpin() throws OrekitException {

        NadirPointing nadirLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Target pointing attitude provider with yaw compensation
        YawCompensation law = new YawCompensation(circOrbit.getFrame(), nadirLaw);

        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(),
                              date.shiftedBy(-300.0), 3.986004415e14);

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
        Assert.assertEquals(0.0, errorAngleMinus, 8.5e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 2.0e-5 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 2.0e-8);

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
            final double mu = 3.9860047e14;

            // Reference frame = ITRF
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            //  Satellite position
            circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                       FastMath.toRadians(5.300), PositionAngle.MEAN,
                                       FramesFactory.getEME2000(), date, mu);

            // Elliptic earth shape */
            earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
        circOrbit = null;
        earthShape = null;
    }

}

