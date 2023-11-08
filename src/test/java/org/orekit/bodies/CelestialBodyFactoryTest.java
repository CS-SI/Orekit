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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CelestialBodyFactoryTest {

    @Test
    public void getSun() {
        Utils.setDataRoot("regular-data");

        CelestialBody sun = CelestialBodyFactory.getSun();
        Assertions.assertNotNull(sun);
    }

    @Test
    public void clearCache() {
        Utils.setDataRoot("regular-data");

        CelestialBody sun = CelestialBodyFactory.getSun();
        Assertions.assertNotNull(sun);
        CelestialBodyFactory.clearCelestialBodyCache();
        CelestialBody sun2 = CelestialBodyFactory.getSun();
        Assertions.assertNotNull(sun2);
        Assertions.assertNotSame(sun, sun2);
    }

    @Test
    public void clearLoaders() {
        Utils.setDataRoot("regular-data");

        CelestialBody sun = CelestialBodyFactory.getSun();
        Assertions.assertNotNull(sun);
        CelestialBodyFactory.clearCelestialBodyLoaders();
        CelestialBody sun2 = CelestialBodyFactory.getSun();
        Assertions.assertNotNull(sun2);
        Assertions.assertNotSame(sun, sun2);
        CelestialBodyFactory.clearCelestialBodyLoaders(CelestialBodyFactory.SUN);
        CelestialBodyFactory.clearCelestialBodyCache(CelestialBodyFactory.SUN);
        CelestialBodyFactory.addDefaultCelestialBodyLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES);
        CelestialBody sun3 = CelestialBodyFactory.getSun();
        Assertions.assertNotNull(sun3);
        Assertions.assertNotSame(sun,  sun3);
        Assertions.assertNotSame(sun2, sun3);
    }

    @Test
    public void testHorizon() {

        // The following data are an excerpt from a telnet session with JPL Horizon system
        // note that in Horizon we selected Jupiter barycenter rather than Jupiter body center
        // this seems to match better the content of the DE-431 ephemeris

        //  *******************************************************************************
        //  Ephemeris / PORT_LOGIN Mon Oct 26 04:53:43 2015 Pasadena, USA    / Horizons
        //  *******************************************************************************
        //  Target body name: Jupiter Barycenter (5)          {source: DE-0431LE-0431}
        //  Center body name: Solar System Barycenter (0)     {source: DE-0431LE-0431}
        //  Center-site name: BODY CENTER
        //  *******************************************************************************
        //  Start time      : A.D. 2000-Jan-01 00:00:00.0000 CT
        //  Stop  time      : A.D. 2003-Dec-31 23:59:00.0000 CT
        //  Step-size       : 1440 minutes
        //  *******************************************************************************
        //  Center geodetic : 0.00000000,0.00000000,0.0000000 {E-lon(deg),Lat(deg),Alt(km)}
        //  Center cylindric: 0.00000000,0.00000000,0.0000000 {E-lon(deg),Dxy(km),Dz(km)}
        //  Center radii    : (undefined)
        //  Output units    : KM-S
        //  Output format   : 02
        //  Reference frame : ICRF/J2000.0
        //  Output type     : GEOMETRIC cartesian states
        //  Coordinate systm: Earth Mean Equator and Equinox of Reference Epoch
        //  *******************************************************************************
        //  JDCT
        //     X     Y     Z
        //     VX    VY    VZ
        //  *******************************************************************************
        //  $$SOE
        //  2451544.500000000 = A.D. 2000-Jan-01 00:00:00.0000 (TDB)
        //   X = 5.978411018921824E+08 Y = 4.085508359611598E+08 Z = 1.605595308103096E+08
        //   VX=-7.892151874487445E+00 VY= 1.017751699703826E+01 VZ= 4.554715748011852E+00
        //  2451545.500000000 = A.D. 2000-Jan-02 00:00:00.0000 (TDB)
        //   X = 5.971584965869523E+08 Y = 4.094296790808872E+08 Z = 1.609528639632485E+08
        //   VX=-7.908893450088906E+00 VY= 1.016606978596496E+01 VZ= 4.550216570971850E+00
        //  2451546.500000000 = A.D. 2000-Jan-03 00:00:00.0000 (TDB)
        //   X = 5.964744456934582E+08 Y = 4.103075321378759E+08 Z = 1.613458079269412E+08
        //   VX=-7.925614558638352E+00 VY= 1.015459888397081E+01 VZ= 4.545706740033853E+00
        //  2451547.500000000 = A.D. 2000-Jan-04 00:00:00.0000 (TDB)
        //   X = 5.957889509819047E+08 Y = 4.111843930867567E+08 Z = 1.617383617815004E+08
        //   VX=-7.942315157290042E+00 VY= 1.014310432640078E+01 VZ= 4.541186269306714E+00
        //  2451548.500000000 = A.D. 2000-Jan-05 00:00:00.0000 (TDB)
        //   X = 5.951020142261952E+08 Y = 4.120602598852173E+08 Z = 1.621305246082588E+08
        //   VX=-7.958995203281466E+00 VY= 1.013158614867071E+01 VZ= 4.536655172931710E+00
        //  2451549.500000000 = A.D. 2000-Jan-06 00:00:00.0000 (TDB)
        //   X = 5.944136372039236E+08 Y = 4.129351304940084E+08 Z = 1.625222954897725E+08
        //   VX=-7.975654653933506E+00 VY= 1.012004438626713E+01 VZ= 4.532113465082445E+00
        final TimeStampedPVCoordinates[] refPV = new TimeStampedPVCoordinates[] {
            createPV(2000, 1, 1,
                     5.978411018921824E+08, 4.085508359611598E+08, 1.605595308103096E+08,
                     -7.892151874487445E+00, 1.017751699703826E+01, 4.554715748011852E+00),
            createPV(2000, 1, 2,
                     5.971584965869523E+08, 4.094296790808872E+08, 1.609528639632485E+08,
             -7.908893450088906E+00, 1.016606978596496E+01, 4.550216570971850E+00),
            createPV(2000, 1, 3,
                     5.964744456934582E+08, 4.103075321378759E+08, 1.613458079269412E+08,
             -7.925614558638352E+00, 1.015459888397081E+01, 4.545706740033853E+00),
            createPV(2000, 1, 4,
                     5.957889509819047E+08, 4.111843930867567E+08, 1.617383617815004E+08,
                     -7.942315157290042E+00, 1.014310432640078E+01, 4.541186269306714E+00),
            createPV(2000, 1, 5,
                     5.951020142261952E+08, 4.120602598852173E+08, 1.621305246082588E+08,
                     -7.958995203281466E+00, 1.013158614867071E+01, 4.536655172931710E+00),
            createPV(2000, 1, 6,
                     5.944136372039236E+08, 4.129351304940084E+08, 1.625222954897725E+08,
                     -7.975654653933506E+00, 1.012004438626713E+01, 4.532113465082445E+00)
        };

        Utils.setDataRoot("regular-data");
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();
        final Frame icrf = FramesFactory.getICRF();
        for (final TimeStampedPVCoordinates ref : refPV) {
            TimeStampedPVCoordinates testPV = jupiter.getPVCoordinates(ref.getDate(),
                    icrf);
            Assertions.assertEquals(0.0,
                                Vector3D.distance(ref.getPosition(), testPV.getPosition()),
                                4.0e-4);
            Assertions.assertEquals(0.0,
                                Vector3D.distance(ref.getVelocity(), testPV.getVelocity()),
                                1.0e-11);
            Vector3D testP = jupiter.getInertiallyOrientedFrame()
                    .getStaticTransformTo(icrf, ref.getDate())
                    .transformPosition(Vector3D.ZERO);
            Assertions.assertEquals(
                    0.0,
                    Vector3D.distance(ref.getPosition(), testP),
                    8.0e-4);
            testP = jupiter.getBodyOrientedFrame()
                    .getStaticTransformTo(icrf, ref.getDate())
                    .transformPosition(Vector3D.ZERO);
            Assertions.assertEquals(
                    0.0,
                    Vector3D.distance(ref.getPosition(), testP),
                    8.0e-4);
        }

    }

    private TimeStampedPVCoordinates createPV(int year, int month, int day,
                                              double xKm, double yKm, double zKM,
                                              double vxKmS, double vyKms, double vzKms) {
        return new TimeStampedPVCoordinates(new AbsoluteDate(year, month, day, TimeScalesFactory.getTDB()),
                                            new Vector3D(  xKm * 1000,   yKm * 1000,   zKM * 1000),
                                            new Vector3D(vxKmS * 1000, vyKms * 1000, vzKms * 1000));
    }

    @Test
    public void testSerialization()
            throws IOException, ClassNotFoundException {
        Utils.setDataRoot("regular-data");
        for (String name : new String[] {
            CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, CelestialBodyFactory.SUN, CelestialBodyFactory.MERCURY,
            CelestialBodyFactory.VENUS, CelestialBodyFactory.EARTH_MOON, CelestialBodyFactory.EARTH,
            CelestialBodyFactory.MOON, CelestialBodyFactory.MARS, CelestialBodyFactory.JUPITER,
            CelestialBodyFactory.SATURN, CelestialBodyFactory.URANUS, CelestialBodyFactory.NEPTUNE, CelestialBodyFactory.PLUTO
        }) {

            CelestialBody original = CelestialBodyFactory.getBody(name);

            ByteArrayOutputStream bosBody = new ByteArrayOutputStream();
            ObjectOutputStream    oosBody = new ObjectOutputStream(bosBody);
            oosBody.writeObject(original);
            Assertions.assertTrue(bosBody.size() > 400);
            Assertions.assertTrue(bosBody.size() < 460);

            ByteArrayInputStream  bisBody = new ByteArrayInputStream(bosBody.toByteArray());
            ObjectInputStream     oisBody = new ObjectInputStream(bisBody);
            CelestialBody deserializedBody  = (CelestialBody) oisBody.readObject();
            Assertions.assertTrue(original == deserializedBody);

            ByteArrayOutputStream bosInertialFrame = new ByteArrayOutputStream();
            ObjectOutputStream    oosInertialFrame = new ObjectOutputStream(bosInertialFrame);
            oosInertialFrame.writeObject(original.getInertiallyOrientedFrame());
            Assertions.assertTrue(bosInertialFrame.size() > 400);
            Assertions.assertTrue(bosInertialFrame.size() < 460);

            ByteArrayInputStream  bisInertialFrame = new ByteArrayInputStream(bosInertialFrame.toByteArray());
            ObjectInputStream     oisInertialFrame = new ObjectInputStream(bisInertialFrame);
            Frame deserializedInertialFrame  = (Frame) oisInertialFrame.readObject();
            Assertions.assertTrue(original.getInertiallyOrientedFrame() == deserializedInertialFrame);

            ByteArrayOutputStream bosBodyFrame = new ByteArrayOutputStream();
            ObjectOutputStream    oosBodyFrame = new ObjectOutputStream(bosBodyFrame);
            oosBodyFrame.writeObject(original.getBodyOrientedFrame());
            Assertions.assertTrue(bosBodyFrame.size() > 400);
            Assertions.assertTrue(bosBodyFrame.size() < 460);

            ByteArrayInputStream  bisBodyFrame = new ByteArrayInputStream(bosBodyFrame.toByteArray());
            ObjectInputStream     oisBodyFrame = new ObjectInputStream(bisBodyFrame);
            Frame deserializedBodyFrame  = (Frame) oisBodyFrame.readObject();
            Assertions.assertTrue(original.getBodyOrientedFrame() == deserializedBodyFrame);

        }
    }

    @Test
    public void multithreadTest() {
        Utils.setDataRoot("regular-data");
        checkMultiThread(10, 100);
    }

    private void checkMultiThread(final int threads, final int runs) {

        final AtomicReference<OrekitException> caught = new AtomicReference<OrekitException>();
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        List<Future<?>> results = new ArrayList<Future<?>>();
        for (int i = 0; i < threads; i++) {
            Future<?> result = executorService.submit(new Runnable() {
                public void run() {
                    try {
                        for (int run = 0; run < runs; run++) {
                            CelestialBody mars = CelestialBodyFactory.getBody(CelestialBodyFactory.MARS);
                            Assertions.assertNotNull(mars);
                            CelestialBodyFactory.clearCelestialBodyLoaders();
                        }
                    } catch (OrekitException oe) {
                        caught.set(oe);
                    }
                }
            });
            results.add(result);
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Assertions.fail(ie.getLocalizedMessage());
        }

        for (Future<?> result : results) {
            Assertions.assertTrue(result.isDone(),"Not all threads finished -> possible deadlock");
        }

        if (caught.get() != null) {
            throw caught.get();
        }
    }

    @Test
    public void testEarthMoonBarycenter() {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody mars = CelestialBodyFactory.getMars();
        CelestialBody earth = CelestialBodyFactory.getEarth();
        CelestialBody earthMoonBarycenter = CelestialBodyFactory.getEarthMoonBarycenter();
        List<Frame> frames = Arrays.asList(FramesFactory.getEME2000(),
                                           FramesFactory.getGCRF(),
                                           sun.getInertiallyOrientedFrame(),
                                           mars.getInertiallyOrientedFrame(),
                                           earth.getInertiallyOrientedFrame());

        AbsoluteDate date = new AbsoluteDate(1969, 7, 23, TimeScalesFactory.getTT());
        final double refDistance = bodyDistance(sun, earthMoonBarycenter, date, frames.get(0));
        for (Frame frame : frames) {
            Assertions.assertEquals(refDistance,
                                bodyDistance(sun, earthMoonBarycenter, date, frame),
                                1.0e-14 * refDistance,frame.toString());
        }
    }

    @Test
    public void testICRFAndGCRFAlignment() {
        Utils.setDataRoot("regular-data");
        final CelestialBody earthMoonBarycenter   = CelestialBodyFactory.getEarthMoonBarycenter();
        final CelestialBody solarSystemBarycenter = CelestialBodyFactory.getSolarSystemBarycenter();
        final List<Frame> frames = Arrays.asList(earthMoonBarycenter.getInertiallyOrientedFrame(),
                                                 earthMoonBarycenter.getBodyOrientedFrame(),
                                                 solarSystemBarycenter.getInertiallyOrientedFrame(),
                                                 solarSystemBarycenter.getBodyOrientedFrame());
        final Frame icrf = FramesFactory.getICRF();
        final Frame gcrf = FramesFactory.getGCRF();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            for (final Frame frame : frames) {
                Assertions.assertEquals(0.0, frame.getTransformTo(icrf, date).getRotation().getAngle(), 1.0e-15);
                Assertions.assertEquals(0.0, frame.getTransformTo(gcrf, date).getRotation().getAngle(), 1.0e-15);
                Assertions.assertEquals(0.0, frame.getStaticTransformTo(icrf, date).getRotation().getAngle(), 1.0e-15);
                Assertions.assertEquals(0.0, frame.getStaticTransformTo(gcrf, date).getRotation().getAngle(), 1.0e-15);
            }
        }
    }

    @Test
    public void testEarthInertialFrameAroundJ2000() {
        Utils.setDataRoot("regular-data");
        final Frame earthFrame = CelestialBodyFactory.getEarth().getInertiallyOrientedFrame();
        final Frame base       = FramesFactory.getGCRF();
        final Rotation reference = new Rotation(Vector3D.PLUS_K, Vector3D.PLUS_J,
                                                Vector3D.PLUS_K, Vector3D.PLUS_I);
         for (double dt = -60; dt <= 60; dt += 1.0) {
             final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
             Rotation rotation = base.getTransformTo(earthFrame, date).getRotation();
             Assertions.assertEquals(0.0, Rotation.distance(reference, rotation), 3.0e-10);
         }
    }

    @Test
    public void testEarthBodyOrientedFrameAroundJ2000() {
        Utils.setDataRoot("regular-data");
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final Frame base       = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
         for (double dt = -60; dt <= 60; dt += 1.0) {
             final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
             Rotation rotation = base.getTransformTo(earthFrame, date).getRotation();
             Assertions.assertEquals(7.9426e-4, Rotation.distance(Rotation.IDENTITY, rotation), 1.0e-8);
         }
    }

    private double bodyDistance(CelestialBody body1, CelestialBody body2, AbsoluteDate date, Frame frame)
        {
        Vector3D body1Position = body1.getPosition(date, frame);
        Vector3D body2Position = body2.getPosition(date, frame);
        Vector3D bodyPositionDifference = body1Position.subtract(body2Position);
        return bodyPositionDifference.getNorm();
    }

}
