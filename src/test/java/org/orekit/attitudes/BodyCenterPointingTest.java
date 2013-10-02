/* Copyright 2002-2013 CS Systèmes d'Information
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


import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
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


public class BodyCenterPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Orbit
    private CircularOrbit circ;

    // Reference frame = ITRF 2008
    private Frame itrf;

    // Transform from EME2000 to ITRF2008
    private Transform eme2000ToItrf;

    // Earth center pointing attitude provider
    private BodyCenterPointing earthCenterAttitudeLaw;

    /** Test if target is body center
     */
    @Test
    public void testTarget() throws OrekitException {

        // Call get target method
        Vector3D target = earthCenterAttitudeLaw.getTargetPoint(circ, date, circ.getFrame());

        // Check that target is body center
        Assert.assertEquals(0.0, target.getNorm(), Utils.epsilonTest);

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
                                                                      zSatITRF2008C));

        // Check that the line contains earth center (distance from line to point less than 1.e-8 m)
        double distance = pointingLine.distance(Vector3D.ZERO);

        Assert.assertTrue(distance < 1.e-8);
    }

    @Test
    public void testSpin() throws OrekitException {

        Propagator propagator = new KeplerianPropagator(circ, earthCenterAttitudeLaw);

        double h = 0.01;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
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


            // Reference frame = ITRF 2008
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            // Transform from EME2000 to ITRF2008
            eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(itrf, date);

            // Create earth center pointing attitude provider */
            earthCenterAttitudeLaw = new BodyCenterPointing(itrf);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
        eme2000ToItrf = null;
        earthCenterAttitudeLaw = null;
        circ = null;
    }

}

