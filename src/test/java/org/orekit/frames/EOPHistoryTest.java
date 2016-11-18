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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class EOPHistoryTest {

    @Test
    public void testRegular() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(2004, 1, 4, TimeScalesFactory.getUTC());
        double dt = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true).getUT1MinusUTC(date);
        Assert.assertEquals(-0.3906070, dt, 1.0e-10);
    }

    @Test
    public void testOutOfRange() throws OrekitException {
        EOPHistory history = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        AbsoluteDate endDate = new AbsoluteDate(2006, 3, 5, TimeScalesFactory.getUTC());
        for (double t = -1000; t < 1000 ; t += 3) {
            AbsoluteDate date = endDate.shiftedBy(t);
            double dt = history.getUT1MinusUTC(date);
            if (t <= 0) {
                Assert.assertTrue(dt < 0.29236);
                Assert.assertTrue(dt > 0.29233);
            } else {
                // no more data after end date
                Assert.assertEquals(0.0, dt, 1.0e-10);
            }
        }
    }

    @Test
    public void testContinuityThreshold() {
        try {
            FramesFactory.setEOPContinuityThreshold(0.5 * Constants.JULIAN_DAY);
            AbsoluteDate date = new AbsoluteDate(2004, 1, 4, TimeScalesFactory.getUTC());
            FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true).getUT1MinusUTC(date);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES,
                                oe.getSpecifier());
        }
    }

    @Test
    public void testUTCLeap() throws OrekitException {
        EOPHistory history = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        AbsoluteDate endLeap = new AbsoluteDate(2006, 1, 1, TimeScalesFactory.getUTC());
        for (double dt = -200; dt < 200; dt += 3) {
            final AbsoluteDate date = endLeap.shiftedBy(dt);
            double dtu1 = history.getUT1MinusUTC(date);
            if (dt <= 0) {
                Assert.assertEquals(-0.6612, dtu1, 3.0e-5);
            } else {
                Assert.assertEquals(0.3388, dtu1, 3.0e-5);
            }
        }
    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        EOPHistory history = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(history);

        Assert.assertTrue(bos.size() > 145000);
        Assert.assertTrue(bos.size() < 150000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        EOPHistory deserialized  = (EOPHistory) ois.readObject();
        Assert.assertEquals(history.getStartDate(), deserialized.getStartDate());
        Assert.assertEquals(history.getEndDate(), deserialized.getEndDate());
        Assert.assertEquals(history.getEntries().size(), deserialized.getEntries().size());
        for (int i = 0; i < history.getEntries().size(); ++i) {
            EOPEntry e1 = history.getEntries().get(i);
            EOPEntry e2 = deserialized.getEntries().get(i);
            Assert.assertEquals(e1.getMjd(),         e2.getMjd());
            Assert.assertEquals(e1.getDate(),        e2.getDate());
            Assert.assertEquals(e1.getUT1MinusUTC(), e2.getUT1MinusUTC(), 1.0e-10);
            Assert.assertEquals(e1.getLOD(),         e2.getLOD(),         1.0e-10);
            Assert.assertEquals(e1.getDdEps(),       e2.getDdEps(),       1.0e-10);
            Assert.assertEquals(e1.getDdPsi(),       e2.getDdPsi(),       1.0e-10);
            Assert.assertEquals(e1.getDx(),          e2.getDx(),          1.0e-10);
            Assert.assertEquals(e1.getDy(),          e2.getDy(),          1.0e-10);
            Assert.assertEquals(e1.getX(),           e2.getX(),           1.0e-10);
            Assert.assertEquals(e1.getY(),           e2.getY(),           1.0e-10);
        }

    }

    @Test
    public void testTidalInterpolationEffects() throws IOException, OrekitException {

        final EOPHistory h1 = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, false);
        final EOPHistory h2 = h1.getNonInterpolatingEOPHistory();
        final AbsoluteDate date0 = new AbsoluteDate(2004, 8, 16, 20, 0, 0, TimeScalesFactory.getUTC());

        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 10) {
            final AbsoluteDate date = date0.shiftedBy(dt);
            final double interpolationErrorUT1 = h1.getUT1MinusUTC(date) - h2.getUT1MinusUTC(date);
            final double interpolationErrorLOD = h1.getLOD(date)         - h2.getLOD(date);
            final PoleCorrection p1 = h1.getPoleCorrection(date);
            final PoleCorrection p2 = h2.getPoleCorrection(date);
            final double interpolationErrorXp  = (p1.getXp() - p2.getXp()) / Constants.ARC_SECONDS_TO_RADIANS;
            final double interpolationErrorYp  = (p1.getYp() - p2.getYp()) / Constants.ARC_SECONDS_TO_RADIANS;
            Assert.assertEquals(0.0, interpolationErrorUT1, 1.2e-10); // seconds
            Assert.assertEquals(0.0, interpolationErrorLOD, 1.5e-9);  // seconds
            Assert.assertEquals(0.0, interpolationErrorXp,  2.3e-9);  // arcseconds
            Assert.assertEquals(0.0, interpolationErrorYp,  1.5e-9);  // arcseconds
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
