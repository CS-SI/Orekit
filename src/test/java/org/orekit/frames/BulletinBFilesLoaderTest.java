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


import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class BulletinBFilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
         SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(converter, history);
        Assert.assertTrue(getMaxGap(history) > 5);
    }

    @Test
    public void testStartDate() throws OrekitException {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2005, 12, 5, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, history, true).getStartDate());
    }

    @Test
    public void testEndDate() throws OrekitException {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(converter, history);
        Assert.assertTrue(getMaxGap(history) < 5);
        Assert.assertEquals(new AbsoluteDate(2006, 3, 5, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, history, false).getEndDate());
    }

    @Test
    public void testNewFormatNominal() throws OrekitException {
        setRoot("new-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader("^bulletinb\\.270$").fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, data, true);
        Assert.assertEquals(new AbsoluteDate(2010, 6, 2, TimeScalesFactory.getUTC()),
                            history.getStartDate());
        Assert.assertEquals(new AbsoluteDate(2010, 7, 1, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testOldFormatContent() throws OrekitException {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, data, true);
        AbsoluteDate date = new AbsoluteDate(2006, 1, 11, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(msToS(  (-3 * 0.073    + 27 * -0.130   + 27 * -0.244   - 3 * -0.264)   / 48), history.getLOD(date), 1.0e-10);
        Assert.assertEquals(        (-3 * 0.333275 + 27 * 0.333310 + 27 * 0.333506 - 3 * 0.333768) / 48,  history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(asToRad((-3 * 0.04958  + 27 * 0.04927  + 27 * 0.04876  - 3 * 0.04854)  / 48), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(asToRad((-3 * 0.38117  + 27 * 0.38105  + 27 * 0.38071  - 3 * 0.38036)  / 48), history.getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testOldFormat1980() throws OrekitException {
        setRoot("old-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader("^bulletinb_IAU1980-220\\.txt$").fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_1996, data, true);
        Assert.assertEquals(new AbsoluteDate(2006,  4,  4, TimeScalesFactory.getUTC()),
                            history.getStartDate());
        Assert.assertEquals(new AbsoluteDate(2006,  5,  4, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testOldFormat1980RemovedFirstDates() throws OrekitException {
        setRoot("old-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader("^bulletinb_IAU1980-220-edited\\.txt$").fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_1996, data, true);
        Assert.assertEquals(new AbsoluteDate(2006,  4, 14, TimeScalesFactory.getUTC()),
                            history.getStartDate());
        Assert.assertEquals(new AbsoluteDate(2006,  5,  4, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testOldFormatTruncated() throws OrekitException {
        setRoot("old-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        try {
            new BulletinBFilesLoader("^bulletinb_IAU2000-216-truncated\\.txt$").fillHistory(converter, data);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE, oe.getSpecifier());
            Assert.assertEquals(54, ((Integer) oe.getParts()[1]).intValue());
        }
    }

    @Test
    public void testNewFormatContent() throws OrekitException {
        setRoot("new-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader("^bulletinb\\.270$").fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, data, true);
        AbsoluteDate date = new AbsoluteDate(2010, 6, 12, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(msToS((   -3 *   0.1202 + 27 *   0.0294 + 27 *   0.0682 - 3 *   0.1531) / 48), history.getLOD(date), 1.0e-10);
        Assert.assertEquals(msToS((   -3 * -57.1711 + 27 * -57.2523 + 27 * -57.3103 - 3 * -57.4101) / 48), history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(masToRad((-3 *  -1.216  + 27 *   1.658  + 27 *   4.926  - 3 *   7.789)  / 48), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(masToRad((-3 * 467.780  + 27 * 469.330  + 27 * 470.931  - 3 * 472.388)  / 48), history.getPoleCorrection(date).getYp(), 1.0e-10);
        Assert.assertEquals(masToRad((-3 *   0.097  + 27 *   0.089  + 27 *   0.050  - 3 *  -0.007)  / 48), history.getNonRotatinOriginNutationCorrection(date)[0],  1.0e-10);
        Assert.assertEquals(masToRad((-3 *   0.071  + 27 *   0.066  + 27 *   0.090  - 3 *   0.111)  / 48), history.getNonRotatinOriginNutationCorrection(date)[1],  1.0e-10);
    }

    @Test
    public void testNewFormatRemovedFirstDates() throws OrekitException {
        setRoot("new-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader("^bulletinb-edited\\.270$").fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, data, true);
        Assert.assertEquals(new AbsoluteDate(2010,  6, 11, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    private double msToS(double ms) {
        return ms / 1000.0;
    }

    private double asToRad(double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS;
    }

    private double masToRad(double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS / 1000.0;
    }

    @Test(expected=OrekitException.class)
    public void testNewFormatTruncated() throws OrekitException {
        setRoot("new-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
       SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader("^bulletinb-truncated\\.270$").fillHistory(converter, history);
    }

    @Test(expected=OrekitException.class)
    public void testNewFormatTruncatedEarly() throws OrekitException {
        setRoot("new-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
       SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader("^bulletinb-truncated-early\\.270$").fillHistory(converter, history);
    }

    @Test(expected=OrekitException.class)
    public void testNewFormatInconsistent() throws OrekitException {
        setRoot("new-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
       SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinBFilesLoader("^bulletinb-inconsistent\\.270$").fillHistory(converter, history);
    }

    @Test
    public void testNewFormatInconsistentDate() throws OrekitException {
        setRoot("new-bulletinB");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
       SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
       try {
           new BulletinBFilesLoader("bulletinb-inconsistent-date.270").fillHistory(converter, history);
           Assert.fail("an exception should have been thrown");
       } catch (OrekitException oe) {
           Assert.assertEquals(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE, oe.getSpecifier());
       }
    }

}
