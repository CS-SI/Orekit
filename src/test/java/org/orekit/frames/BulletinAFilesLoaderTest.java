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
package org.orekit.frames;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.SortedSet;
import java.util.TreeSet;


public class BulletinAFilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testStartDate() {
        setRoot("bulletinA");
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinAFilesLoader("bulletina-xxvi-\\d\\d\\d\\.txt", manager, () -> utc).fillHistory(null, history);
        Assertions.assertEquals(new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56475),
                                             TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, true).getStartDate());
    }

    @Test
    public void testEndDate() {
        setRoot("bulletinA");
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinAFilesLoader("bulletina-xxvi-\\d\\d\\d\\.txt", manager, () -> utc).fillHistory(null, history);
        Assertions.assertTrue(getMaxGap(history) < 2);
        Assertions.assertEquals(new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56968),
                                             TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, false).getEndDate());
    }

    @Test
    public void testSingleFile() {
        setRoot("bulletinA");
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinAFilesLoader("bulletina-xxvi-039.txt", manager, () -> utc).fillHistory(null, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                            data, true);

        // earliest date is for pole position, provided days 56546, 56547, 56548
        Assertions.assertEquals(new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56546),
                                             TimeScalesFactory.getUTC()),
                            history.getStartDate());

        // with this single file, there is a hole between last pole (56548) and first rapid data (56555)
        Assertions.assertEquals(56555 - 56548, getMaxGap(data));

        // latest date is for EOP prediction, corresponding to 56926
        Assertions.assertEquals(new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56926),
                                             TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testRapidDataContent() {
        setRoot("bulletinA");
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinAFilesLoader(FramesFactory.BULLETINA_FILENAME, manager, () -> utc).fillHistory(null, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                            data, true);
        AbsoluteDate date = new AbsoluteDate(2013, 10, 14, 12, 0, 0, TimeScalesFactory.getUTC());
        // the following values are from bulletina-xxvi-042.txt, rapid service section, lines 53-56
        Assertions.assertEquals(        (-3 * -0.001957 + 27 * -0.003274 + 27 * -0.004706 - 3 * -0.006211) / 48,  history.getUT1MinusUTC(date), 1.0e-10);
        Assertions.assertEquals(asToRad((-3 *  0.11518  + 27 *  0.11389  + 27 *  0.11285  - 3 *  0.11171)  / 48), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assertions.assertEquals(asToRad((-3 *  0.28484  + 27 *  0.28449  + 27 *  0.28408  - 3 *  0.28379)  / 48), history.getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testFinalValuesContent() {
        setRoot("bulletinA");
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new BulletinAFilesLoader(FramesFactory.BULLETINA_FILENAME, manager, () -> utc).fillHistory(null, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                            data, true);
        AbsoluteDate date = new AbsoluteDate(2013, 8, 26, 12, 0, 0, TimeScalesFactory.getUTC());
        // the following values are from bulletina-xxvi-040.txt, final values section, lines 79-82
        Assertions.assertEquals(        (-3 * 0.04058 + 27 * 0.04000 + 27 * 0.03953 - 3 * 0.03917) / 48,  history.getUT1MinusUTC(date), 1.0e-10);
        Assertions.assertEquals(asToRad((-3 * 0.1692  + 27 * 0.1689  + 27 * 0.1685  - 3 * 0.1684)  / 48), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assertions.assertEquals(asToRad((-3 * 0.3336  + 27 * 0.3322  + 27 * 0.3307  - 3 * 0.3294)  / 48), history.getPoleCorrection(date).getYp(), 1.0e-10);
    }

    private double asToRad(double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS;
    }

    @Test
    public void testMissingSections() {
        setRoot("bulletinA");
        checkTruncated("bulletina-missing-eop-rapid-service.txt",    OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE);
        checkTruncated("bulletina-missing-eop-prediction.txt",       OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE);
        checkTruncated("bulletina-with-1980-without-2000-rapid.txt", OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE);
        checkTruncated("bulletina-without-1980-with-2000-rapid.txt", OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE);
        checkTruncated("bulletina-with-1980-without-2000-final.txt", OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE);
        checkTruncated("bulletina-without-1980-with-2000-final.txt", OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE);
    }

    @Test
    public void testMissingData() {
        setRoot("bulletinA");
        checkTruncated("bulletina-truncated-in-eop-data.txt",  OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE);
        checkTruncated("bulletina-truncated-in-pole-data.txt", OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE);
    }

    private void checkTruncated(String name, OrekitMessages expected) {
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        try {
            new BulletinAFilesLoader(name, manager, () -> utc).fillHistory(null, history);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(expected, oe.getSpecifier());
            Assertions.assertTrue(((String) oe.getParts()[0]).endsWith(name));
        }
    }

    @Test
    public void testInconsistentDate() {
        setRoot("bulletinA");
        checkInconsistent("bulletina-inconsistent-year.txt");
        checkInconsistent("bulletina-inconsistent-month.txt");
        checkInconsistent("bulletina-inconsistent-day.txt");
    }

    private void checkInconsistent(String name) {
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        try {
            new BulletinAFilesLoader(name, manager, () -> utc).fillHistory(null, history);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE, oe.getSpecifier());
            Assertions.assertTrue(((String) oe.getParts()[0]).endsWith(name));
        }
    }

}
