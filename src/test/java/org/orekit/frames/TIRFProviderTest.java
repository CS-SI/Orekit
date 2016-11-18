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
package org.orekit.frames;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * Unit tests for {@link TIRFProvider}.
 *
 * @author Evan Ward
 */
public class TIRFProviderTest {

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // Reference position & velocity from : "Fundamentals of Astrodynamics and Applications", Third edition, David A. Vallado
        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
                             { 53098, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53099, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53100, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53101, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53102, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53103, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53104, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53105, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 }
                         }));
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        // Positions LEO
        Frame itrfA = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(-1033479.3830, 7901295.2754, 6380356.5958),
                              new Vector3D(-3225.636520, -2872.451450, 5531.924446));

        // Reference coordinates
        Frame tirf = FramesFactory.getTIRF(IERSConventions.IERS_2010);
        PVCoordinates pvTIRF =
            new PVCoordinates(new Vector3D(-1033475.0312, 7901305.5856, 6380344.5328),
                              new Vector3D(-3225.632747, -2872.442511, 5531.931288));
        checkPV(pvTIRF,
                itrfA.getTransformTo(tirf, t0).transformPVCoordinates(pvITRF),
                6.379e-5, 3.78e-7);

        Frame cirf = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvCIRF =
            new PVCoordinates(new Vector3D(5100018.4047, 6122786.3648, 6380344.5328),
                              new Vector3D(-4745.380330, 790.341453, 5531.931288));
        checkPV(pvCIRF,
                tirf.getTransformTo(cirf, t0).transformPVCoordinates(pvTIRF),
                8.59e-3, 4.65e-6);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
                             { 53153, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53154, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53155, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53156, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53157, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53158, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53159, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53160, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 }
                         }));

        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        //  Positions GEO
        Frame itrfA = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(24796919.2915, -34115870.9234, 10226.0621),
                              new Vector3D(-0.979178, -1.476538, -0.928776));

        Frame tirf = FramesFactory.getTIRF(IERSConventions.IERS_2010);
        PVCoordinates pvTIRF =
            new PVCoordinates(new Vector3D(24796919.2953, -34115870.9004, 10293.2583),
                              new Vector3D(-0.979178, -1.476540, -0.928772));
        checkPV(pvTIRF,
                itrfA.getTransformTo(tirf, t0).transformPVCoordinates(pvITRF),
                5.697e-5, 4.69e-7);

        Frame cirf = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvCIRF =
            new PVCoordinates(new Vector3D(-40588158.1236, -11462167.0709, 10293.2583),
                              new Vector3D(834.787843, -2958.305669, -0.928772));
        checkPV(pvCIRF,
                tirf.getTransformTo(cirf, t0).transformPVCoordinates(pvTIRF),
                0.0505, 3.60e-6);

    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        TIRFProvider provider = new TIRFProvider(FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(provider);

        Assert.assertTrue(bos.size() > 280000);
        Assert.assertTrue(bos.size() < 285000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        TIRFProvider deserialized  = (TIRFProvider) ois.readObject();
        for (int i = 0; i < FastMath.min(100, provider.getEOPHistory().getEntries().size()); ++i) {
            AbsoluteDate date = provider.getEOPHistory().getEntries().get(i).getDate();
            Transform expectedIdentity = new Transform(date,
                                                       provider.getTransform(date).getInverse(),
                                                       deserialized.getTransform(date));
            Assert.assertEquals(0.0, expectedIdentity.getTranslation().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, expectedIdentity.getRotation().getAngle(),   1.0e-15);
        }

    }

    /**
     * Checks that {@link TIRFProvider#getTransform(AbsoluteDate)} is thread safe.
     */
    @Test
    public void testConcurrentGetTransform()
        throws OrekitException, InterruptedException, ExecutionException {

        // subject under test
        final TIRFProvider tirf = new TIRFProvider(FramesFactory.getEOPHistory(IERSConventions.IERS_2010, false));
        // arbitrary date
        final AbsoluteDate start = new AbsoluteDate("2009-09-19T23:59:45.000", TimeScalesFactory.getUTC());
        // in seconds = 15min
        final double timeStep = 300;
        // the number of possible concurrent threads
        final int nJobs = 24;
        // the number of calculations per thread
        final int nPerJob = 1000;
        // tolerance of comparisons
        final double absTol = Precision.EPSILON;
        // the expected result
        final List<Transform> expecteds = new ArrayList<Transform>();
        for (int j = 0; j < nPerJob; j++) {
            final AbsoluteDate date = start.shiftedBy(timeStep * j);
            // action
            final Transform expected = tirf.getTransform(date);
            // verify
            expecteds.add(expected);
        }

        // build jobs for concurrent execution
        final List<Callable<Boolean>> jobs = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < nJobs; i++) {
            jobs.add(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    for (int j = 0; j < nPerJob; j++) {
                        final AbsoluteDate date = start.shiftedBy(timeStep * j);
                        // action
                        final Transform actual = tirf.getTransform(date);
                        // verify
                        assertTransformEquals(expecteds.get(j), actual, absTol);
                    }
                    return true;
                }
            });
        }

        // run the jobs
        runConcurrentlyAndCheck(jobs, nJobs);
    }

    /**
     * Runs several jobs at concurrently in the given number of threads.
     *
     * @param jobs
     *            the jobs to execute
     * @param threads
     *            the size of the thread pool
     */
    private static void runConcurrentlyAndCheck(List<Callable<Boolean>> jobs, int threads)
        throws InterruptedException, ExecutionException {

        // action
        final List<Future<Boolean>> futures = Executors.newFixedThreadPool(threads).invokeAll(jobs);

        // verify - necessary to throw AssertionErrors from the Callable
        for (Future<Boolean> future : futures) {
            Assert.assertEquals(true, future.get());
        }

    }

    /**
     * Check the two {@link Transform}s are the same to within an absolute tolerance.
     */
    private static void assertTransformEquals(Transform expected, Transform actual, double absTol) {
        final AngularCoordinates expectedAngular = expected.getAngular();
        final AngularCoordinates actualAngular = actual.getAngular();
        final Rotation expectedRotation = expectedAngular.getRotation();
        final Rotation actualRotation = actualAngular.getRotation();
        final PVCoordinates expectedPV = expected.getCartesian();
        final PVCoordinates actualPV = actual.getCartesian();

        // transform
        Assert.assertEquals(Rotation.distance(actualRotation, expectedRotation), 0, absTol);
        Assert.assertEquals(expectedAngular.getRotationRate(), actualAngular.getRotationRate());
        Assert.assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        Assert.assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference, PVCoordinates result,
                         double expectedPositionError, double expectedVelocityError) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(expectedPositionError, dP.getNorm(), 0.01 * expectedPositionError);
        Assert.assertEquals(expectedVelocityError, dV.getNorm(), 0.01 * expectedVelocityError);
    }

}
