/* Copyright 2002-2025 CS GROUP
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
package org.orekit.time;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.ITRFVersion;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UT1ScaleTest {

    @Test
    public void testLeap2006() {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate dateA = new AbsoluteDate(2005, 12, 30, 23, 59,  0.0, utc);
        AbsoluteDate dateB = new AbsoluteDate(2006,  1,  2,  0,  1,  0.0, utc);
        TimeOffset deltaAUT1 = ut1.offsetFromTAI(dateA);
        TimeOffset deltaAUTC = utc.offsetFromTAI(dateA);
        TimeOffset deltaBUT1 = ut1.offsetFromTAI(dateB);
        TimeOffset deltaBUTC = utc.offsetFromTAI(dateB);

        // there is a leap second between the two dates
        Assertions.assertEquals(deltaAUTC.toDouble() - 1.0, deltaBUTC.toDouble(), 1.0e-15);

        // the leap second induces UTC goes from above UT1 to below UT1
        Assertions.assertTrue(deltaAUTC.compareTo(deltaAUT1) > 0);
        Assertions.assertTrue(deltaBUTC.compareTo(deltaBUT1) < 0);

        // UT1 is continuous, so change should be very small in two days
        Assertions.assertEquals(deltaAUT1.toDouble(), deltaBUT1.toDouble(), 3.0e-4);

    }

    /** Issue #636 */
    @Test
    public void testContinuousDuringLeap() {
        // setup
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate dateA = new AbsoluteDate(2005, 12, 31, 23, 59, 0.0, utc);
        AbsoluteDate dateB = new AbsoluteDate(2006, 1, 1, 0, 1, 0.0, utc);
        EOPHistory eopHistory = ut1.getEOPHistory();

        // verify
        // there is a leap second and jump in UT1-UTC
        Assertions.assertEquals(eopHistory.getUT1MinusUTC(new AbsoluteDate(2005, 12, 31, utc)), -0.6611333, 0);
        Assertions.assertEquals(eopHistory.getUT1MinusUTC(new AbsoluteDate(2006, 1, 1, utc)), 0.338829, 1e-16);

        // check UT1-TAI is still smooth
        double dt = 0.5;
        double tol = 0.001 * dt / Constants.JULIAN_DAY;
        double previous = ut1.offsetFromTAI(dateA.shiftedBy(-dt)).toDouble();
        for (AbsoluteDate d = dateA; d.compareTo(dateB) < 0; d = d.shiftedBy(dt)) {
            double actual = ut1.offsetFromTAI(d).toDouble();
            Assertions.assertEquals(0, actual - previous, tol, "at " + d.toString(utc));
            previous = actual;
        }
    }

    @Test
    public void testSymmetry() {
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = ut1.offsetFromTAI(date).toDouble();
            DateTimeComponents components = date.getComponents(ut1);
            double dt2 = ut1.offsetToTAI(components.getDate(), components.getTime()).toDouble();
            Assertions.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testConcurrent() throws InterruptedException, ExecutionException {
        // set up
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final int threads = 10;
        final int timesPerThread = 100;
        final double dt = 123.456789 * Constants.JULIAN_DAY;

        // calculate expected values in single-thread mode
        final double[] expected = new double[timesPerThread];
        for (int i = 0; i < timesPerThread; i++) {
            expected[i] = ut1.offsetFromTAI(date.shiftedBy(i * dt)).toDouble();
        }

        // build jobs for concurrent execution
        final List<Callable<Boolean>> jobs = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            jobs.add(() -> {
                for (int j = 0; j < timesPerThread; j++) {
                    final double actual = ut1.offsetFromTAI(date.shiftedBy(j * dt)).toDouble();
                    Assertions.assertEquals(expected[j], actual, 0);
                }
                return true;
            });
        }

        // action
        final List<Future<Boolean>> futures = Executors.newFixedThreadPool(threads).invokeAll(jobs);

        // verify - necessary to throw AssertionErrors from the Callable
        for (Future<Boolean> future : futures) {
            Assertions.assertEquals(true, future.get());
        }
    }

    @Test
    public void testAAS06134() {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // Note that the dUT1 here is -0.439962, whereas it is -0.4399619 in the book
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, ITRFVersion.ITRF_2008, new double[][] {
                             { 53098, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53099, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53100, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53101, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53102, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53103, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53104, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53105, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN }
                         }));
        AbsoluteDate date =
                new AbsoluteDate(2004, 4, 6, 7, 51, 28.386009, TimeScalesFactory.getUTC());
        DateTimeComponents components = date.getComponents(TimeScalesFactory.getUT1(IERSConventions.IERS_1996, true));
        Assertions.assertEquals(2004,        components.getDate().getYear());
        Assertions.assertEquals(   4,        components.getDate().getMonth());
        Assertions.assertEquals(   6,        components.getDate().getDay());
        Assertions.assertEquals(   7,        components.getTime().getHour());
        Assertions.assertEquals(  51,        components.getTime().getMinute());
        Assertions.assertEquals(  27.946047, components.getTime().getSecond(), 1.0e-10);
    }

    @Test
    public void testDuringLeap() {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        final AbsoluteDate before = new AbsoluteDate(new DateComponents(1983, 6, 30),
                                                     new TimeComponents(23, 59, 59),
                                                     utc);
        final AbsoluteDate during = before.shiftedBy(1.25);
        Assertions.assertEquals(61, utc.minuteDuration(during));
        Assertions.assertEquals(1.0, utc.getLeap(during).toDouble(), 1.0e-10);
        Assertions.assertEquals(60, scale.minuteDuration(during));
        Assertions.assertEquals(0.0, scale.getLeap(during).toDouble(), 1.0e-10);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
    }

    @AfterEach
    public void tearDown() {
        ut1 = null;
    }

    private UT1Scale ut1;

}
