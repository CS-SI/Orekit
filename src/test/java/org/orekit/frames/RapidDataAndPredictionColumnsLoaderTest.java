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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.SortedSet;
import java.util.TreeSet;


public class RapidDataAndPredictionColumnsLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testStartDateDaily1980() {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(false, "^finals\\.daily$", manager, () -> utc).fillHistory(converter, history);
        Assertions.assertEquals(new AbsoluteDate(2011, 4, 9, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, true).getStartDate());
    }

    @Test
    public void testEndDateDaily1980() {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(false, "^finals\\.daily$", manager, () -> utc).fillHistory(converter, history);
        Assertions.assertEquals(new AbsoluteDate(2011, 10, 6, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, true).getEndDate());
    }

    @Test
    public void testStartDateDaily2000() {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(true, "^finals\\.daily$", manager, () -> utc).fillHistory(converter, history);
        Assertions.assertEquals(new AbsoluteDate(2011, 4, 9, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2003, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, true).getStartDate());
    }

    @Test
    public void testMissingColumnsPadding1980() {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(false, "^finals\\.daily$", manager, () -> utc).fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_1996, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                            data, true);

        // after 2011-05-31, the example daily file has no columns for Bulletin B data
        // we don't see anything since we fall back to bulletin A
        AbsoluteDate t1Inf = new AbsoluteDate(2011, 6, 1, TimeScalesFactory.getUTC());
        Assertions.assertEquals(-67.724,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t1Inf)[0]), 1.0e-10);
        Assertions.assertEquals(-11.807,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t1Inf)[1]), 1.0e-10);
        Assertions.assertEquals(-0.2778790, history.getUT1MinusUTC(t1Inf),                                                1.0e-10);
        Assertions.assertEquals( 0.5773,    1000 * history.getLOD(t1Inf),                                                 1.0e-10);
        AbsoluteDate t1Sup = t1Inf.shiftedBy(Constants.JULIAN_DAY);
        Assertions.assertEquals(-67.800,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t1Sup)[0]), 1.0e-10);
        Assertions.assertEquals(-11.810,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t1Sup)[1]), 1.0e-10);
        Assertions.assertEquals(-0.2784173, history.getUT1MinusUTC(t1Sup),                                                1.0e-10);
        Assertions.assertEquals( 0.5055,    1000 * history.getLOD(t1Sup),                                                 1.0e-10);

        // after 2011-07-06, the example daily file has no columns for LOD, but it is interpolated
        AbsoluteDate t2Inf = new AbsoluteDate(2011, 7, 6, TimeScalesFactory.getUTC());
        Assertions.assertEquals(-72.717,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t2Inf)[0]), 1.0e-10);
        Assertions.assertEquals(-10.620,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t2Inf)[1]), 1.0e-10);
        Assertions.assertEquals(-0.2915826, history.getUT1MinusUTC(t2Inf),                                                1.0e-10);
        Assertions.assertEquals( 0.5020,    1000 * history.getLOD(t2Inf),                                                 1.0e-10);
        AbsoluteDate t2Sup = t2Inf.shiftedBy(Constants.JULIAN_DAY);
        Assertions.assertEquals(-73.194,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t2Sup)[0]), 1.0e-10);
        Assertions.assertEquals(-10.535,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t2Sup)[1]), 1.0e-10);
        Assertions.assertEquals(-0.2920866, history.getUT1MinusUTC(t2Sup),                                                1.0e-10);
        Assertions.assertEquals( 5.3509e-6, 1000 * history.getLOD(t2Sup),                                                 1.0e-10);

        // after 2011-09-19, the example daily file has no columns for nutation
        AbsoluteDate t3Inf = new AbsoluteDate(2011, 9, 19, TimeScalesFactory.getUTC());
        Assertions.assertEquals(-79.889,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t3Inf)[0]), 1.0e-10);
        Assertions.assertEquals(-11.125,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t3Inf)[1]), 1.0e-10);
        Assertions.assertEquals(-0.3112849, history.getUT1MinusUTC(t3Inf),                                                1.0e-10);
        Assertions.assertEquals( 3.2137e-6, 1000 * history.getLOD(t3Inf),                                                 1.0e-10);
        AbsoluteDate t3Sup = t3Inf.shiftedBy(Constants.JULIAN_DAY);
        Assertions.assertEquals( 0.0,       3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t3Sup)[0]), 1.0e-10);
        Assertions.assertEquals( 0.0,       3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t3Sup)[1]), 1.0e-10);
        Assertions.assertEquals(-0.3115675, history.getUT1MinusUTC(t3Sup),                                                1.0e-10);
        Assertions.assertEquals( 3.4186e-6, 1000 * history.getLOD(t3Sup),                                                 1.0e-10);

    }

    @Test
    public void testMissingColumnsPadding2000() {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(true, "^finals2000A\\.daily$", manager, () -> utc).fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2003, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                            data, true);

        // after 2011-05-31, the example daily file has no columns for Bulletin B data
        // we don't see anything since we fall back to bulletin A
        AbsoluteDate t1Inf = new AbsoluteDate(2011, 6, 1, TimeScalesFactory.getUTC());
        Assertions.assertEquals(-0.015313,  3600 * FastMath.toDegrees(history.getPoleCorrection(t1Inf).getXp()), 1.0e-10);
        Assertions.assertEquals( 0.403214,  3600 * FastMath.toDegrees(history.getPoleCorrection(t1Inf).getYp()), 1.0e-10);
        Assertions.assertEquals(-0.2778790, history.getUT1MinusUTC(t1Inf),                                       1.0e-10);
        Assertions.assertEquals( 0.5773,    1000 * history.getLOD(t1Inf),                                        1.0e-10);
        AbsoluteDate t1Sup = t1Inf.shiftedBy(Constants.JULIAN_DAY);
        Assertions.assertEquals(-0.014222,  3600 * FastMath.toDegrees(history.getPoleCorrection(t1Sup).getXp()), 1.0e-10);
        Assertions.assertEquals( 0.404430,  3600 * FastMath.toDegrees(history.getPoleCorrection(t1Sup).getYp()), 1.0e-10);
        Assertions.assertEquals(-0.2784173, history.getUT1MinusUTC(t1Sup),                                       1.0e-10);
        Assertions.assertEquals( 0.5055,    1000 * history.getLOD(t1Sup),                                        1.0e-10);

        // after 2011-07-06, the example daily file has no columns for LOD, but it is interpolated
        AbsoluteDate t2Inf = new AbsoluteDate(2011, 7, 6, TimeScalesFactory.getUTC());
        Assertions.assertEquals( 0.052605,  3600 * FastMath.toDegrees(history.getPoleCorrection(t2Inf).getXp()), 1.0e-10);
        Assertions.assertEquals( 0.440076,  3600 * FastMath.toDegrees(history.getPoleCorrection(t2Inf).getYp()), 1.0e-10);
        Assertions.assertEquals(-0.2915826, history.getUT1MinusUTC(t2Inf),                                       1.0e-10);
        Assertions.assertEquals( 0.5020,    1000 * history.getLOD(t2Inf),                                        1.0e-10);
        AbsoluteDate t2Sup = t2Inf.shiftedBy(Constants.JULIAN_DAY);
        Assertions.assertEquals( 0.055115,  3600 * FastMath.toDegrees(history.getPoleCorrection(t2Sup).getXp()), 1.0e-10);
        Assertions.assertEquals( 0.440848,  3600 * FastMath.toDegrees(history.getPoleCorrection(t2Sup).getYp()), 1.0e-10);
        Assertions.assertEquals(-0.2920866, history.getUT1MinusUTC(t2Sup),                                       1.0e-10);
        Assertions.assertEquals( 5.3509e-6, 1000 * history.getLOD(t2Sup),                                        1.0e-10);

    }

    @Test
    public void testEndDateDaily2000() {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(true, "^finals2000A\\.daily$", manager, () -> utc).fillHistory(converter, history);
        Assertions.assertEquals(new AbsoluteDate(2011, 10, 6, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2003, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, true).getEndDate());
    }

    @Test
    public void testNoColumns() {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                        IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(true, "^finals2000A-no-columns\\.daily$", manager, () -> utc).fillHistory(converter, history);
        EOPHistory eopH = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                         history, true);
        Assertions.assertEquals(new AbsoluteDate(2011, 4, 16, TimeScalesFactory.getUTC()), eopH.getEndDate());
        AbsoluteDate testDate = eopH.getEndDate().shiftedBy(-2 * Constants.JULIAN_DAY);
        Assertions.assertEquals(0.0, eopH.getPoleCorrection(testDate).getXp(),                1.0e-15);
        Assertions.assertEquals(0.0, eopH.getPoleCorrection(testDate).getYp(),                1.0e-15);
        Assertions.assertEquals(0.0, eopH.getUT1MinusUTC(testDate),                           1.0e-15);
        Assertions.assertEquals(0.0, eopH.getLOD(testDate),                                   1.0e-15);
        Assertions.assertEquals(0.0, eopH.getNonRotatinOriginNutationCorrection(testDate)[0], 1.0e-15);
        Assertions.assertEquals(0.0, eopH.getNonRotatinOriginNutationCorrection(testDate)[1], 1.0e-15);
    }

    @Test
    public void testPost2070() {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                        IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(true, "^finals2000A-post-2070\\.daily$", manager, () -> utc).fillHistory(converter, history);
        Assertions.assertEquals(new AbsoluteDate(2075, 4, 16, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, true).getEndDate());

    }

    @Test
    public void testTruncatedLine() {
        doTestWrongFile("^finals2000A-truncated-line\\.daily$", 3);
    }

    @Test
    public void testWrongDateFormat() {
        doTestWrongFile("^finals2000A-wrong-date-format\\.daily$", 3);
    }

    @Test
    public void testWrongYear() {
        doTestWrongFile("^finals2000A-wrong-year\\.daily$", 6);
    }

    @Test
    public void testWrongMonth() {
        doTestWrongFile("^finals2000A-wrong-month\\.daily$", 5);
    }

    @Test
    public void testWrongDay() {
        doTestWrongFile("^finals2000A-wrong-day\\.daily$", 4);
    }

    @Test
    public void testWrongPoleAFormat() {
        doTestWrongFile("^finals2000A-wrong-pole-A-format\\.daily$", 7);
    }

    @Test
    public void testWrongPoleBFormat() {
        doTestWrongFile("^finals2000A-wrong-pole-B-format\\.daily$", 7);
    }

    @Test
    public void testWrongUT1UTCAFormat() {
        doTestWrongFile("^finals2000A-wrong-ut1-utc-A-format\\.daily$", 7);
    }

    @Test
    public void testWrongUT1UTCBFormat() {
        doTestWrongFile("^finals2000A-wrong-ut1-utc-B-format\\.daily$", 7);
    }

    @Test
    public void testWrongLODAFormat() {
        doTestWrongFile("^finals2000A-wrong-lod-A-format\\.daily$", 7);
    }

    @Test
    public void testWrongNutationAFormat() {
        doTestWrongFile("^finals2000A-wrong-nutation-A-format\\.daily$", 7);
    }

    @Test
    public void testWrongNutationBFormat() {
        doTestWrongFile("^finals2000A-wrong-nutation-B-format\\.daily$", 7);
    }

    private void doTestWrongFile(String fileName, int lineNumber) {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                        IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        try {
            new RapidDataAndPredictionColumnsLoader(true, fileName, manager, () -> utc).fillHistory(converter, history);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(lineNumber, ((Integer) oe.getParts()[0]).intValue());
        }
    }

}
