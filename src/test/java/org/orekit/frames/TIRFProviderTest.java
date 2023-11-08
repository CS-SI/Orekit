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
package org.orekit.frames;

import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

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

/**
 * Unit tests for {@link TIRFProvider}.
 *
 * @author Evan Ward
 */
public class TIRFProviderTest {

    @Test
    public void testAASReferenceLEO() {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // Reference position & velocity from : "Fundamentals of Astrodynamics and Applications", Third edition, David A. Vallado
        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, ITRFVersion.ITRF_2008, new double[][] {
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
        checkP(pvTIRF.getPosition(),
                itrfA.getStaticTransformTo(tirf, t0).transformPosition(pvITRF.getPosition()),
                6.379e-5);

        Frame cirf = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvCIRF =
            new PVCoordinates(new Vector3D(5100018.4047, 6122786.3648, 6380344.5328),
                              new Vector3D(-4745.380330, 790.341453, 5531.931288));
        checkPV(pvCIRF,
                tirf.getTransformTo(cirf, t0).transformPVCoordinates(pvTIRF),
                8.59e-3, 4.65e-6);
        checkP(pvCIRF.getPosition(),
                tirf.getStaticTransformTo(cirf, t0).transformPosition(pvTIRF.getPosition()),
                8.59e-3);

    }

    @Test
    public void testAASReferenceGEO() {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, ITRFVersion.ITRF_2008, new double[][] {
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
        checkP(pvTIRF.getPosition(),
                itrfA.getStaticTransformTo(tirf, t0).transformPosition(pvITRF.getPosition()),
                5.697e-5);

        Frame cirf = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvCIRF =
            new PVCoordinates(new Vector3D(-40588158.1236, -11462167.0709, 10293.2583),
                              new Vector3D(834.787843, -2958.305669, -0.928772));
        checkPV(pvCIRF,
                tirf.getTransformTo(cirf, t0).transformPVCoordinates(pvTIRF),
                0.0505, 3.60e-6);
        checkP(pvCIRF.getPosition(),
                tirf.getStaticTransformTo(cirf, t0).transformPosition(pvTIRF.getPosition()),
                0.0505);

    }

    /** Issue #636 */
    @Test
    public void testContinuousDuringLeap() {
        // setup
        Utils.setDataRoot("regular-data");
        Frame tirf = FramesFactory.getTIRF(IERSConventions.IERS_2010, true);
        Frame cirf = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate dateA = new AbsoluteDate(2005, 12, 31, 23, 59, 0.0, utc);
        AbsoluteDate dateB = new AbsoluteDate(2006, 1, 1, 0, 1, 0.0, utc);
        double dt = 0.5;
        double expected = dt * 2 * FastMath.PI / (23 * 3600 + 56 * 60 + 4.1);
        double tol = expected * 1e-6;
        Rotation previous = cirf.getTransformTo(tirf, dateA.shiftedBy(-dt)).getRotation();

        // action + verify
        for (AbsoluteDate d = dateA; d.compareTo(dateB) < 0; d = d.shiftedBy(dt)) {
            Rotation actual = cirf.getTransformTo(tirf, d).getRotation();
            Assertions.assertEquals(expected,
                    Rotation.distance(previous, actual),
                    tol, "At " + d.toString(utc));
            Assertions.assertEquals(expected,
                    Rotation.distance(
                            previous,
                            cirf.getStaticTransformTo(tirf, d).getRotation()),
                    tol, "At " + d.toString(utc));
            previous = actual;
        }
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        TimeScale ut1 = DataContext.getDefault().getTimeScales().getUT1(eopHistory);
        TIRFProvider provider = new TIRFProvider(eopHistory, ut1);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(provider);

        Assertions.assertTrue(bos.size() > 340000);
        Assertions.assertTrue(bos.size() < 350000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        TIRFProvider deserialized  = (TIRFProvider) ois.readObject();
        for (int i = 0; i < FastMath.min(100, provider.getEOPHistory().getEntries().size()); ++i) {
            AbsoluteDate date = provider.getEOPHistory().getEntries().get(i).getDate();
            Transform expectedIdentity = new Transform(date,
                                                       provider.getTransform(date).getInverse(),
                                                       deserialized.getTransform(date));
            Assertions.assertEquals(0.0, expectedIdentity.getTranslation().getNorm(), 1.0e-15);
            Assertions.assertEquals(0.0, expectedIdentity.getRotation().getAngle(),   1.0e-15);
        }

    }

    /**
     * Checks that {@link TIRFProvider#getTransform(AbsoluteDate)} is thread safe.
     */
    @Test
    public void testConcurrentGetTransform()
        throws InterruptedException, ExecutionException {

        // subject under test
        final EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, false);
        final TimeScale ut1 = DataContext.getDefault().getTimeScales().getUT1(eopHistory);
        final TIRFProvider tirf = new TIRFProvider(eopHistory, ut1);
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
            Assertions.assertEquals(true, future.get());
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
        Assertions.assertEquals(Rotation.distance(actualRotation, expectedRotation), 0, absTol);
        Assertions.assertEquals(expectedAngular.getRotationRate(), actualAngular.getRotationRate());
        Assertions.assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        Assertions.assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference, PVCoordinates result,
                         double expectedPositionError, double expectedVelocityError) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assertions.assertEquals(expectedPositionError, dP.getNorm(), 0.01 * expectedPositionError);
        Assertions.assertEquals(expectedVelocityError, dV.getNorm(), 0.01 * expectedVelocityError);
    }

    private void checkP(Vector3D position,
                        Vector3D transformPosition,
                        double tol) {
        MatcherAssert.assertThat(
                transformPosition,
                OrekitMatchers.vectorCloseTo(position, tol));
    }

}
