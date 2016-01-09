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


import java.text.ParseException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class RapidDataAndPredictionColumnsLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testStartDateDaily1980() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(false, "^finals\\.daily$").fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2011, 4, 9, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, history, true).getStartDate());
    }

    @Test
    public void testEndDateDaily1980() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(false, "^finals\\.daily$").fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2011, 10, 6, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, history, true).getEndDate());
    }

    @Test
    public void testStartDateDaily2000() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(true, "^finals\\.daily$").fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2011, 4, 9, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2003, history, true).getStartDate());
    }

    @Test
    public void testMissingColumnsPadding1980() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(false, "^finals\\.daily$").fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_1996, data, true);

        // after 2011-06-01, the example daily file has no columns for Bulletin B data
        // we don't see anything since we ignore the columns from Bulletin B
        AbsoluteDate t1Inf = new AbsoluteDate(2011, 6, 1, TimeScalesFactory.getUTC());
        Assert.assertEquals(-67.724,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t1Inf)[0]), 1.0e-10);
        Assert.assertEquals(-11.807,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t1Inf)[1]), 1.0e-10);
        Assert.assertEquals(-0.2778790, history.getUT1MinusUTC(t1Inf),                                                1.0e-10);
        Assert.assertEquals( 0.5773,    1000 * history.getLOD(t1Inf),                                                 1.0e-10);
        AbsoluteDate t1Sup = t1Inf.shiftedBy(Constants.JULIAN_DAY);
        Assert.assertEquals(-67.800,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t1Sup)[0]), 1.0e-10);
        Assert.assertEquals(-11.810,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t1Sup)[1]), 1.0e-10);
        Assert.assertEquals(-0.2784173, history.getUT1MinusUTC(t1Sup),                                                1.0e-10);
        Assert.assertEquals( 0.5055,    1000 * history.getLOD(t1Sup),                                                 1.0e-10);

        // after 2011-07-06, the example daily file has no columns for LOD
        AbsoluteDate t2Inf = new AbsoluteDate(2011, 7, 6, TimeScalesFactory.getUTC());
        Assert.assertEquals(-72.717,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t2Inf)[0]), 1.0e-10);
        Assert.assertEquals(-10.620,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t2Inf)[1]), 1.0e-10);
        Assert.assertEquals(-0.2915826, history.getUT1MinusUTC(t2Inf),                                                1.0e-10);
        Assert.assertEquals( 0.5020,    1000 * history.getLOD(t2Inf),                                                 1.0e-10);
        AbsoluteDate t2Sup = t2Inf.shiftedBy(Constants.JULIAN_DAY);
        Assert.assertEquals(-73.194,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t2Sup)[0]), 1.0e-10);
        Assert.assertEquals(-10.535,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t2Sup)[1]), 1.0e-10);
        Assert.assertEquals(-0.2920866, history.getUT1MinusUTC(t2Sup),                                                1.0e-10);
        Assert.assertEquals( 0.0,       1000 * history.getLOD(t2Sup),                                                 1.0e-10);

        // after 2011-09-19, the example daily file has no columns for nutation
        AbsoluteDate t3Inf = new AbsoluteDate(2011, 9, 19, TimeScalesFactory.getUTC());
        Assert.assertEquals(-79.889,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t3Inf)[0]), 1.0e-10);
        Assert.assertEquals(-11.125,    3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t3Inf)[1]), 1.0e-10);
        Assert.assertEquals(-0.3112849, history.getUT1MinusUTC(t3Inf),                                                1.0e-10);
        Assert.assertEquals( 0.0,       1000 * history.getLOD(t3Inf),                                                 1.0e-10);
        AbsoluteDate t3Sup = t3Inf.shiftedBy(Constants.JULIAN_DAY);
        Assert.assertEquals( 0.0,       3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t3Sup)[0]), 1.0e-10);
        Assert.assertEquals( 0.0,       3600000 * FastMath.toDegrees(history.getEquinoxNutationCorrection(t3Sup)[1]), 1.0e-10);
        Assert.assertEquals(-0.3115675, history.getUT1MinusUTC(t3Sup),                                                1.0e-10);
        Assert.assertEquals( 0.0,       1000 * history.getLOD(t3Sup),                                                 1.0e-10);

    }

    @Test
    public void testMissingColumnsPadding2000() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(true, "^finals2000A\\.daily$").fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2003, data, true);

        // after 2011-06-01, the example daily file has no columns for Bulletin B data
        // we don't see anything since we ignore the columns from Bulletin B
        AbsoluteDate t1Inf = new AbsoluteDate(2011, 6, 1, TimeScalesFactory.getUTC());
        Assert.assertEquals(-0.015313,  3600 * FastMath.toDegrees(history.getPoleCorrection(t1Inf).getXp()), 1.0e-10);
        Assert.assertEquals( 0.403214,  3600 * FastMath.toDegrees(history.getPoleCorrection(t1Inf).getYp()), 1.0e-10);
        Assert.assertEquals(-0.2778790, history.getUT1MinusUTC(t1Inf),                                       1.0e-10);
        Assert.assertEquals( 0.5773,    1000 * history.getLOD(t1Inf),                                        1.0e-10);
        AbsoluteDate t1Sup = t1Inf.shiftedBy(Constants.JULIAN_DAY);
        Assert.assertEquals(-0.014222,  3600 * FastMath.toDegrees(history.getPoleCorrection(t1Sup).getXp()), 1.0e-10);
        Assert.assertEquals( 0.404430,  3600 * FastMath.toDegrees(history.getPoleCorrection(t1Sup).getYp()), 1.0e-10);
        Assert.assertEquals(-0.2784173, history.getUT1MinusUTC(t1Sup),                                       1.0e-10);
        Assert.assertEquals( 0.5055,    1000 * history.getLOD(t1Sup),                                        1.0e-10);

        // after 2011-07-06, the example daily file has no columns for LOD
        AbsoluteDate t2Inf = new AbsoluteDate(2011, 7, 6, TimeScalesFactory.getUTC());
        Assert.assertEquals( 0.052605,  3600 * FastMath.toDegrees(history.getPoleCorrection(t2Inf).getXp()), 1.0e-10);
        Assert.assertEquals( 0.440076,  3600 * FastMath.toDegrees(history.getPoleCorrection(t2Inf).getYp()), 1.0e-10);
        Assert.assertEquals(-0.2915826, history.getUT1MinusUTC(t2Inf),                                       1.0e-10);
        Assert.assertEquals( 0.5020,    1000 * history.getLOD(t2Inf),                                        1.0e-10);
        AbsoluteDate t2Sup = t2Inf.shiftedBy(Constants.JULIAN_DAY);
        Assert.assertEquals( 0.055115,  3600 * FastMath.toDegrees(history.getPoleCorrection(t2Sup).getXp()), 1.0e-10);
        Assert.assertEquals( 0.440848,  3600 * FastMath.toDegrees(history.getPoleCorrection(t2Sup).getYp()), 1.0e-10);
        Assert.assertEquals(-0.2920866, history.getUT1MinusUTC(t2Sup),                                       1.0e-10);
        Assert.assertEquals( 0.0,       1000 * history.getLOD(t2Sup),                                        1.0e-10);

    }

    @Test
    public void testEndDateDaily2000() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
         new RapidDataAndPredictionColumnsLoader(true, "^finals2000A\\.daily$").fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2011, 10, 6, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2003, history, true).getEndDate());
    }
}
