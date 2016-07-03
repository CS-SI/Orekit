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

import org.hipparchus.fitting.PolynomialCurveFitter;
import org.hipparchus.fitting.WeightedObservedPoint;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class BodyCenterPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Orbit
    private CircularOrbit circ;

    // WGS84 Earth model
    private OneAxisEllipsoid earth;

    // Transform from EME2000 to ITRF2008
    private Transform eme2000ToItrf;

    // Earth center pointing attitude provider
    private BodyCenterPointing earthCenterAttitudeLaw;

    /** Test if target is on Earth surface
     */
    @Test
    public void testTarget() throws OrekitException {

        // Call get target method
        TimeStampedPVCoordinates target = earthCenterAttitudeLaw.getTargetPV(circ, date, circ.getFrame());

        // Check that target is on Earth surface
        GeodeticPoint gp = earth.transform(target.getPosition(), circ.getFrame(), date);
        Assert.assertEquals(0.0, gp.getAltitude(), 1.0e-10);
        Assert.assertEquals(date, target.getDate());

    }

    /** Test if body center belongs to the direction pointed by the satellite
     */
    @Test
    public void testBodyCenterInPointingDirection() throws OrekitException {

        // Transform satellite position to position/velocity parameters in EME2000 frame
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();

        //  Pointing direction
        // ********************
        // Get satellite attitude rotation, i.e rotation from EME2000 frame to satellite frame
        Rotation rotSatEME2000 = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Transform Z axis from satellite frame to EME2000
        Vector3D zSatEME2000 = rotSatEME2000.applyInverseTo(Vector3D.PLUS_K);

        // Transform Z axis from EME2000 to ITRF2008
        Vector3D zSatITRF2008C = eme2000ToItrf.transformVector(zSatEME2000);

        // Transform satellite position/velocity from EME2000 to ITRF2008
        PVCoordinates pvSatITRF2008C = eme2000ToItrf.transformPVCoordinates(pvSatEME2000);

       // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(pvSatITRF2008C.getPosition(),
                                     pvSatITRF2008C.getPosition().add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      zSatITRF2008C),
                                     2.0e-8);

        // Check that the line contains Earth center
        Assert.assertTrue(pointingLine.contains(Vector3D.ZERO));

    }

    @Test
    public void testQDot() throws OrekitException {

        Utils.setDataRoot("regular-data");
        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final CircularOrbit initialOrbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        EcksteinHechlerPropagator propagator =
                new EcksteinHechlerPropagator(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
        propagator.setAttitudeProvider(earthCenterAttitudeLaw);

        List<WeightedObservedPoint> w0 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w1 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w2 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w3 = new ArrayList<WeightedObservedPoint>();
        for (double dt = -1; dt < 1; dt += 0.01) {
            Rotation rP = propagator.propagate(date.shiftedBy(dt)).getAttitude().getRotation();
            w0.add(new WeightedObservedPoint(1, dt, rP.getQ0()));
            w1.add(new WeightedObservedPoint(1, dt, rP.getQ1()));
            w2.add(new WeightedObservedPoint(1, dt, rP.getQ2()));
            w3.add(new WeightedObservedPoint(1, dt, rP.getQ3()));
        }

        double q0DotRef = PolynomialCurveFitter.create(2).fit(w0)[1];
        double q1DotRef = PolynomialCurveFitter.create(2).fit(w1)[1];
        double q2DotRef = PolynomialCurveFitter.create(2).fit(w2)[1];
        double q3DotRef = PolynomialCurveFitter.create(2).fit(w3)[1];

        Attitude a0 = propagator.propagate(date).getAttitude();
        double   q0 = a0.getRotation().getQ0();
        double   q1 = a0.getRotation().getQ1();
        double   q2 = a0.getRotation().getQ2();
        double   q3 = a0.getRotation().getQ3();
        double   oX = a0.getSpin().getX();
        double   oY = a0.getSpin().getY();
        double   oZ = a0.getSpin().getZ();

        // first time-derivatives of the quaternion
        double q0Dot = 0.5 * MathArrays.linearCombination(-q1, oX, -q2, oY, -q3, oZ);
        double q1Dot = 0.5 * MathArrays.linearCombination( q0, oX, -q3, oY,  q2, oZ);
        double q2Dot = 0.5 * MathArrays.linearCombination( q3, oX,  q0, oY, -q1, oZ);
        double q3Dot = 0.5 * MathArrays.linearCombination(-q2, oX,  q1, oY,  q0, oZ);

        Assert.assertEquals(q0DotRef, q0Dot, 5.0e-9);
        Assert.assertEquals(q1DotRef, q1Dot, 5.0e-9);
        Assert.assertEquals(q2DotRef, q2Dot, 5.0e-9);
        Assert.assertEquals(q3DotRef, q3Dot, 5.0e-9);

    }

    @Test
    public void testSpin() throws OrekitException {

        Utils.setDataRoot("regular-data");
        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final CircularOrbit initialOrbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        EcksteinHechlerPropagator propagator =
                new EcksteinHechlerPropagator(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
        propagator.setAttitudeProvider(earthCenterAttitudeLaw);

        double h = 0.01;
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-13);

    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Satellite position as circular parameters
            final double mu = 3.9860047e14;
            final double raan = 270.;
            circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(raan),
                                       FastMath.toRadians(5.300 - raan), PositionAngle.MEAN,
                                       FramesFactory.getEME2000(), date, mu);


            // WGS84 Earth model
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            // Transform from EME2000 to ITRF2008
            eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(earth.getBodyFrame(), date);

            // Create earth center pointing attitude provider */
            earthCenterAttitudeLaw = new BodyCenterPointing(circ.getFrame(), earth);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        earth = null;
        eme2000ToItrf = null;
        earthCenterAttitudeLaw = null;
        circ = null;
    }

}

