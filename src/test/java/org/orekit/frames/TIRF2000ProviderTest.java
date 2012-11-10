/* Licensed to CS Systèmes d'Information (CS) under one or more
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.util.Precision;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Unit tests for {@link TIRF2000Provider}.
 *
 * @author Evan Ward
 */
public class TIRF2000ProviderTest {

    /**
     * Checks that {@link TIRF2000Provider#getTransform(AbsoluteDate)} is thread safe.
     */
    @Test
    public void testConcurrentGetTransform()
        throws OrekitException, InterruptedException, ExecutionException {

        // subject under test
        final TIRF2000Provider tirf = new TIRF2000Provider(false);
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
            assertEquals(true, future.get());
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
        assertEquals(Rotation.distance(actualRotation, expectedRotation), 0, absTol);
        assertEquals(expectedAngular.getRotationRate(), actualAngular.getRotationRate());
        assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
