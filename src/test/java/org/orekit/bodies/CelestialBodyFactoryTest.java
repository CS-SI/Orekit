/* Copyright 2002-2015 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class CelestialBodyFactoryTest {

    @Test
    public void getSun() throws OrekitException {
        Utils.setDataRoot("regular-data");

        CelestialBody sun = CelestialBodyFactory.getSun();
        Assert.assertNotNull(sun);
    }

    @Test
    public void clearCache() throws OrekitException {
        Utils.setDataRoot("regular-data");

        CelestialBody sun = CelestialBodyFactory.getSun();
        Assert.assertNotNull(sun);
        CelestialBodyFactory.clearCelestialBodyCache();
        CelestialBody sun2 = CelestialBodyFactory.getSun();
        Assert.assertNotNull(sun2);
        Assert.assertNotSame(sun, sun2);
    }

    @Test
    public void clearLoaders() throws OrekitException {
        Utils.setDataRoot("regular-data");

        CelestialBody sun = CelestialBodyFactory.getSun();
        Assert.assertNotNull(sun);
        CelestialBodyFactory.clearCelestialBodyLoaders();
        CelestialBody sun2 = CelestialBodyFactory.getSun();
        Assert.assertNotNull(sun2);
        Assert.assertNotSame(sun, sun2);
    }

    @Test
    public void testSerialization()
            throws OrekitException, IOException, ClassNotFoundException {
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
            Assert.assertTrue(bosBody.size() > 400);
            Assert.assertTrue(bosBody.size() < 460);

            ByteArrayInputStream  bisBody = new ByteArrayInputStream(bosBody.toByteArray());
            ObjectInputStream     oisBody = new ObjectInputStream(bisBody);
            CelestialBody deserializedBody  = (CelestialBody) oisBody.readObject();
            Assert.assertTrue(original == deserializedBody);

            ByteArrayOutputStream bosInertialFrame = new ByteArrayOutputStream();
            ObjectOutputStream    oosInertialFrame = new ObjectOutputStream(bosInertialFrame);
            oosInertialFrame.writeObject(original.getInertiallyOrientedFrame());
            Assert.assertTrue(bosInertialFrame.size() > 400);
            Assert.assertTrue(bosInertialFrame.size() < 460);

            ByteArrayInputStream  bisInertialFrame = new ByteArrayInputStream(bosInertialFrame.toByteArray());
            ObjectInputStream     oisInertialFrame = new ObjectInputStream(bisInertialFrame);
            Frame deserializedInertialFrame  = (Frame) oisInertialFrame.readObject();
            Assert.assertTrue(original.getInertiallyOrientedFrame() == deserializedInertialFrame);

            ByteArrayOutputStream bosBodyFrame = new ByteArrayOutputStream();
            ObjectOutputStream    oosBodyFrame = new ObjectOutputStream(bosBodyFrame);
            oosBodyFrame.writeObject(original.getBodyOrientedFrame());
            Assert.assertTrue(bosBodyFrame.size() > 400);
            Assert.assertTrue(bosBodyFrame.size() < 460);

            ByteArrayInputStream  bisBodyFrame = new ByteArrayInputStream(bosBodyFrame.toByteArray());
            ObjectInputStream     oisBodyFrame = new ObjectInputStream(bisBodyFrame);
            Frame deserializedBodyFrame  = (Frame) oisBodyFrame.readObject();
            Assert.assertTrue(original.getBodyOrientedFrame() == deserializedBodyFrame);

        }
    }

    @Test
    public void multithreadTest() throws OrekitException {
        Utils.setDataRoot("regular-data");
        checkMultiThread(10, 100);
    }

    private void checkMultiThread(final int threads, final int runs) throws OrekitException {

        final AtomicReference<OrekitException> caught = new AtomicReference<OrekitException>();
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        List<Future<?>> results = new ArrayList<Future<?>>();
        for (int i = 0; i < threads; i++) {
            Future<?> result = executorService.submit(new Runnable() {
                public void run() {
                    try {
                        for (int run = 0; run < runs; run++) {
                            CelestialBody mars = CelestialBodyFactory.getBody(CelestialBodyFactory.MARS);
                            Assert.assertNotNull(mars);
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
            Assert.fail(ie.getLocalizedMessage());
        }

        for (Future<?> result : results) {
            Assert.assertTrue("Not all threads finished -> possible deadlock", result.isDone());
        }

        if (caught.get() != null) {
            throw caught.get();
        }
    }

    @Test
    public void testEarthMoonBarycenter() throws OrekitException {
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
            Assert.assertEquals(frame.toString(), refDistance,
                                bodyDistance(sun, earthMoonBarycenter, date, frame),
                                1.0e-14 * refDistance);
        }
    }

    private double bodyDistance(CelestialBody body1, CelestialBody body2, AbsoluteDate date, Frame frame)
        throws OrekitException {
        Vector3D body1Position = body1.getPVCoordinates(date, frame).getPosition();
        Vector3D body2Position = body2.getPVCoordinates(date, frame).getPosition();
        Vector3D bodyPositionDifference = body1Position.subtract(body2Position);
        return bodyPositionDifference.getNorm();
    }

}
