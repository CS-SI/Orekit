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
package org.orekit.bodies;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;

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

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream    oos = new ObjectOutputStream(bos);
            oos.writeObject(original);
            Assert.assertTrue(bos.size() > 400);
            Assert.assertTrue(bos.size() < 450);

            ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream     ois = new ObjectInputStream(bis);
            CelestialBody deserialized  = (CelestialBody) ois.readObject();
            Assert.assertTrue(original == deserialized);

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

}
