/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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


import java.text.ParseException;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class SolarBodyTest {

    @Test
    public void geocentricPV() throws OrekitException, ParseException {
        Utils.setDataRoot("regular-data");
        AbsoluteDate date = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());
        Frame geocentricFrame = FramesFactory.getEME2000();
        checkPV(CelestialBodyFactory.getMoon(), date, geocentricFrame,
                new Vector3D(-0.0022350411591597575, -0.0010106334699928434, -5.658291803646671E-4),
                new Vector3D(3.1279236468844985E-4, -4.526815459166321E-4, -2.428841016970333E-4));
        checkPV(CelestialBodyFactory.getEarth(), date, geocentricFrame, Vector3D.ZERO, Vector3D.ZERO);
    }

    @Test
    public void heliocentricPV() throws OrekitException, ParseException {
        Utils.setDataRoot("regular-data");
        AbsoluteDate date = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());
        final Frame eme2000 = FramesFactory.getEME2000();
        Frame heliocentricFrame = new Frame(eme2000, null, "heliocentric/aligned EME2000", true) {
            private static final long serialVersionUID = 4301068133487454052L;
            protected void updateFrame(final AbsoluteDate date) throws OrekitException {
                PVCoordinates pv = CelestialBodyFactory.getSun().getPVCoordinates(date, eme2000);
                setTransform(new Transform(pv.getPosition().negate(), pv.getVelocity().negate()));
            }
        };
        checkPV(CelestialBodyFactory.getSun(), date, heliocentricFrame, Vector3D.ZERO, Vector3D.ZERO);
        checkPV(CelestialBodyFactory.getMercury(), date, heliocentricFrame,
                new Vector3D(0.3388866970713254, -0.16350851403469605, -0.12250815624343761),
                new Vector3D(0.008716751907934464, 0.02294287010530833, 0.011349219084264612));
        checkPV(CelestialBodyFactory.getVenus(), date, heliocentricFrame,
                new Vector3D(0.5733328682513444, -0.3947124128748959, -0.21383496742544283),
                new Vector3D(0.012311818929592546, 0.014756722625966128, 0.005857890214695866));
        checkPV(CelestialBodyFactory.getMars(), date, heliocentricFrame,
        new Vector3D(-0.15808000178306866, -1.3285167111540124, -0.6050478023304016),
        new Vector3D(0.014443621048367267, -1.3669889027283553E-4, -4.542404441793112E-4));
        checkPV(CelestialBodyFactory.getJupiter(), date, heliocentricFrame,
        new Vector3D(-5.387442227958154, -0.8116709870422928, -0.21662388956102652),
        new Vector3D(0.0010628473875341506, -0.006527800816267844, -0.0028242250304474767));
        checkPV(CelestialBodyFactory.getSaturn(), date, heliocentricFrame,
        new Vector3D(7.89952834654684, 4.582711147265509, 1.552649660593234),
        new Vector3D(-0.003208403682518813, 0.004335751536569781, 0.001928152129122073));
        checkPV(CelestialBodyFactory.getUranus(), date, heliocentricFrame,
        new Vector3D(-18.2705614311796, -1.151408356279009, -0.24540975062356502),
        new Vector3D(2.1887052624725852E-4, -0.0037678288699642877, -0.0016532828516810242));
        checkPV(CelestialBodyFactory.getNeptune(), date, heliocentricFrame,
        new Vector3D(-16.06747366050193, -23.938436657940095, -9.39837851302005),
        new Vector3D(0.0026425894813251684, -0.0015042632480101307, -6.815738977894145E-4));
        checkPV(CelestialBodyFactory.getPluto(), date, heliocentricFrame,
        new Vector3D(-30.488788499360652, -0.8637991387172488, 8.914537151982762),
        new Vector3D(3.21695873843002E-4, -0.0031487797507673814, -0.0010799339515148705));
    }

    @Test(expected = OrekitException.class)
    public void noMercury() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMercury();
    }

    @Test(expected = OrekitException.class)
    public void noVenus() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getVenus();
    }

    @Test(expected = OrekitException.class)
    public void noEarthMoonBarycenter() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getEarthMoonBarycenter();
    }

    @Test(expected = OrekitException.class)
    public void noMars() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMars();
    }

    @Test(expected = OrekitException.class)
    public void noJupiter() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getJupiter();
    }

    @Test(expected = OrekitException.class)
    public void noSaturn() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getSaturn();
    }

    @Test(expected = OrekitException.class)
    public void noUranus() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getUranus();
    }

    @Test(expected = OrekitException.class)
    public void noNeptune() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getNeptune();
    }

    @Test(expected = OrekitException.class)
    public void noPluto() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getPluto();
    }

    @Test(expected = OrekitException.class)
    public void noMoon() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMoon();
    }

    @Test(expected = OrekitException.class)
    public void noSun() throws OrekitException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getSun();
    }

    private void checkPV(PVCoordinatesProvider body, AbsoluteDate date, Frame frame,
                         Vector3D position, Vector3D velocity)
    throws OrekitException {

        PVCoordinates pv = body.getPVCoordinates(date, frame);

        final double posScale = 149597870691.0;
        final double velScale = posScale / Constants.JULIAN_DAY;
        PVCoordinates reference =
            new PVCoordinates(new Vector3D(posScale, position), new Vector3D(velScale, velocity));

        PVCoordinates error = new PVCoordinates(reference, pv);
        Assert.assertEquals(0, error.getPosition().getNorm(), 2.0e-3);
        Assert.assertEquals(0, error.getVelocity().getNorm(), 5.0e-10);

    }

    public void testKepler() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TimeScalesFactory.getTT());
        final double au = 149597870691.0;
        checkKepler(CelestialBodyFactory.getMoon(),    CelestialBodyFactory.getEarth(), date, 3.844e8, 0.012);
        checkKepler(CelestialBodyFactory.getMercury(), CelestialBodyFactory.getSun(),   date,  0.387 * au, 4.0e-9);
        checkKepler(CelestialBodyFactory.getVenus(),   CelestialBodyFactory.getSun(),   date,  0.723 * au, 8.0e-9);
        checkKepler(CelestialBodyFactory.getEarth(),   CelestialBodyFactory.getSun(),   date,  1.000 * au, 2.0e-5);
        checkKepler(CelestialBodyFactory.getMars(),    CelestialBodyFactory.getSun(),   date,  1.52  * au, 2.0e-7);
        checkKepler(CelestialBodyFactory.getJupiter(), CelestialBodyFactory.getSun(),   date,  5.20  * au, 2.0e-6);
        checkKepler(CelestialBodyFactory.getSaturn(),  CelestialBodyFactory.getSun(),   date,  9.58  * au, 8.0e-7);
        checkKepler(CelestialBodyFactory.getUranus(),  CelestialBodyFactory.getSun(),   date, 19.20  * au, 6.0e-7);
        checkKepler(CelestialBodyFactory.getNeptune(), CelestialBodyFactory.getSun(),   date, 30.05  * au, 4.0e-7);
        checkKepler(CelestialBodyFactory.getPluto(),   CelestialBodyFactory.getSun(),   date, 39.24  * au, 3.0e-7);
    }

    private void checkKepler(final PVCoordinatesProvider orbiting, final CelestialBody central,
                             final AbsoluteDate start, final double a, final double epsilon)
        throws OrekitException {

        // set up Keplerian orbit of orbiting body around central body
        Orbit orbit = new KeplerianOrbit(orbiting.getPVCoordinates(start, central.getInertiallyOrientedFrame()),
                                         central.getInertiallyOrientedFrame(),start, central.getGM());
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        Assert.assertEquals(a, orbit.getA(), 0.02 * a);
        double duration = FastMath.min(50 * Constants.JULIAN_DAY, 0.01 * orbit.getKeplerianPeriod());

        double max = 0;
        for (AbsoluteDate date = start; date.durationFrom(start) < duration; date = date.shiftedBy(duration / 100)) {
            PVCoordinates ephemPV = orbiting.getPVCoordinates(date, central.getInertiallyOrientedFrame());
            PVCoordinates keplerPV = propagator.propagate(date).getPVCoordinates();
            Vector3D error = keplerPV.getPosition().subtract(ephemPV.getPosition());
            max = FastMath.max(max, error.getNorm());
        }
        Assert.assertTrue(max < epsilon * a);
    }

}
