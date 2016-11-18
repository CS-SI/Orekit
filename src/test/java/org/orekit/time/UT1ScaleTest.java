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
package org.orekit.time;


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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class UT1ScaleTest {

    @Test
    public void testLeap2006() throws OrekitException {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate dateA = new AbsoluteDate(2005, 12, 30, 23, 59,  0.0, utc);
        AbsoluteDate dateB = new AbsoluteDate(2006,  1,  2,  0,  1,  0.0, utc);
        double deltaAUT1 = ut1.offsetFromTAI(dateA);
        double deltaAUTC = utc.offsetFromTAI(dateA);
        double deltaBUT1 = ut1.offsetFromTAI(dateB);
        double deltaBUTC = utc.offsetFromTAI(dateB);

        // there is a leap second between the two dates
        Assert.assertEquals(deltaAUTC - 1.0, deltaBUTC, 1.0e-15);

        // the leap second induces UTC goes from above UT1 to below UT1
        Assert.assertTrue(deltaAUTC > deltaAUT1);
        Assert.assertTrue(deltaBUTC < deltaBUT1);

        // UT1 is continuous, so change should be very small in two days
        Assert.assertEquals(deltaAUT1, deltaBUT1, 3.0e-4);

    }

    @Test
    public void testSymmetry() throws OrekitException {
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = ut1.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(ut1);
            double dt2 = ut1.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
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
            expected[i] = ut1.offsetFromTAI(date.shiftedBy(i * dt));
        }

        // build jobs for concurrent execution
        final List<Callable<Boolean>> jobs = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < threads; i++) {
            jobs.add(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    for (int j = 0; j < timesPerThread; j++) {
                        final double actual = ut1.offsetFromTAI(date.shiftedBy(j * dt));
                        Assert.assertEquals(expected[j], actual, 0);
                    }
                    return true;
                }
            });
        }

        // action
        final List<Future<Boolean>> futures = Executors.newFixedThreadPool(threads).invokeAll(jobs);

        // verify - necessary to throw AssertionErrors from the Callable
        for (Future<Boolean> future : futures) {
            Assert.assertEquals(true, future.get());
        }
    }

    @Test
    public void testAAS06134() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // Note that the dUT1 here is -0.439962, whereas it is -0.4399619 in the book
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
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
        Assert.assertEquals(2004,        components.getDate().getYear());
        Assert.assertEquals(   4,        components.getDate().getMonth());
        Assert.assertEquals(   6,        components.getDate().getDay());
        Assert.assertEquals(   7,        components.getTime().getHour());
        Assert.assertEquals(  51,        components.getTime().getMinute());
        Assert.assertEquals(  27.946047, components.getTime().getSecond(), 1.0e-10);
    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        UT1Scale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ut1);

        Assert.assertTrue(bos.size() > 145000);
        Assert.assertTrue(bos.size() < 155000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        UT1Scale deserialized  = (UT1Scale) ois.readObject();
        for (double dt = 0; dt < 7 * Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date = ut1.getEOPHistory().getStartDate().shiftedBy(dt);
            Assert.assertEquals(ut1.offsetFromTAI(date), deserialized.offsetFromTAI(date), 1.0e-15);
        }

    }

    @Test
    public void testDuringLeap() throws OrekitException {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        final AbsoluteDate before = new AbsoluteDate(new DateComponents(1983, 06, 30),
                                                     new TimeComponents(23, 59, 59),
                                                     utc);
        final AbsoluteDate during = before.shiftedBy(1.25);
        Assert.assertEquals(61, utc.minuteDuration(during));
        Assert.assertEquals(1.0, utc.getLeap(during), 1.0e-10);
        Assert.assertEquals(60, scale.minuteDuration(during));
        Assert.assertEquals(0.0, scale.getLeap(during), 1.0e-10);
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
    }

    @After
    public void tearDown() {
        ut1 = null;
    }

    private TimeScale ut1;

}
