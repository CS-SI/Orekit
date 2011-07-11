/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.apache.commons.math.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


public class RapidDataAndPredictionColumnsLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testStartDateDaily1980() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        EOP1980History history = new EOP1980History();
        new RapidDataAndPredictionColumnsLoader("^finals\\.daily$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2011, 4, 9, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    @Test
    public void testEndDateDaily1980() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        EOP1980History history = new EOP1980History();
        new RapidDataAndPredictionColumnsLoader("^finals\\.daily$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2011, 10, 6, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testStartDateDaily2000() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        EOP1980History history = new EOP1980History();
        new RapidDataAndPredictionColumnsLoader("^finals\\.daily$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2011, 10, 6, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testMissingColumnsPadding1980() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        EOP1980History history = new EOP1980History();
        new RapidDataAndPredictionColumnsLoader("^finals\\.daily$").fillHistory(history);

        // after 2011-06-01, the example daily file has no columns for Bulletin B data
        // we don't see anything since we ignore the columns from Bulletin B
        AbsoluteDate t1Inf = new AbsoluteDate(2011, 6, 1, TimeScalesFactory.getUTC());
        Assert.assertEquals(-67.724,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t1Inf).getDdpsi()), 1.0e-10);
        Assert.assertEquals(-11.807,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t1Inf).getDdeps()), 1.0e-10);
        Assert.assertEquals(-0.2778790, history.getUT1MinusUTC(t1Inf),                                       1.0e-10);
        Assert.assertEquals( 0.5773,    1000 * history.getLOD(t1Inf),                                        1.0e-10);
        AbsoluteDate t1Sup = t1Inf.shiftedBy(Constants.JULIAN_DAY);
        Assert.assertEquals(-67.800,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t1Sup).getDdpsi()), 1.0e-10);
        Assert.assertEquals(-11.810,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t1Sup).getDdeps()), 1.0e-10);
        Assert.assertEquals(-0.2784173, history.getUT1MinusUTC(t1Sup),                                       1.0e-10);
        Assert.assertEquals( 0.5055,    1000 * history.getLOD(t1Sup),                                        1.0e-10);

        // after 2011-07-06, the example daily file has no columns for LOD
        AbsoluteDate t2Inf = new AbsoluteDate(2011, 7, 6, TimeScalesFactory.getUTC());
        Assert.assertEquals(-72.717,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t2Inf).getDdpsi()), 1.0e-10);
        Assert.assertEquals(-10.620,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t2Inf).getDdeps()), 1.0e-10);
        Assert.assertEquals(-0.2915826, history.getUT1MinusUTC(t2Inf),                                       1.0e-10);
        Assert.assertEquals( 0.5020,    1000 * history.getLOD(t2Inf),                                        1.0e-10);
        AbsoluteDate t2Sup = t2Inf.shiftedBy(Constants.JULIAN_DAY);
        Assert.assertEquals(-73.194,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t2Sup).getDdpsi()), 1.0e-10);
        Assert.assertEquals(-10.535,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t2Sup).getDdeps()), 1.0e-10);
        Assert.assertEquals(-0.2920866, history.getUT1MinusUTC(t2Sup),                                       1.0e-10);
        Assert.assertEquals( 0.0,       1000 * history.getLOD(t2Sup),                                        1.0e-10);

        // after 2011-09-19, the example daily file has no columns for nutation
        AbsoluteDate t3Inf = new AbsoluteDate(2011, 9, 19, TimeScalesFactory.getUTC());
        Assert.assertEquals(-79.889,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t3Inf).getDdpsi()), 1.0e-10);
        Assert.assertEquals(-11.125,    3600000 * FastMath.toDegrees(history.getNutationCorrection(t3Inf).getDdeps()), 1.0e-10);
        Assert.assertEquals(-0.3112849, history.getUT1MinusUTC(t3Inf),                                       1.0e-10);
        Assert.assertEquals( 0.0,       1000 * history.getLOD(t3Inf),                                        1.0e-10);
        AbsoluteDate t3Sup = t3Inf.shiftedBy(Constants.JULIAN_DAY);
        Assert.assertEquals( 0.0,       3600000 * FastMath.toDegrees(history.getNutationCorrection(t3Sup).getDdpsi()), 1.0e-10);
        Assert.assertEquals( 0.0,       3600000 * FastMath.toDegrees(history.getNutationCorrection(t3Sup).getDdeps()), 1.0e-10);
        Assert.assertEquals(-0.3115675, history.getUT1MinusUTC(t3Sup),                                       1.0e-10);
        Assert.assertEquals( 0.0,       1000 * history.getLOD(t3Sup),                                        1.0e-10);

    }

    @Test
    public void testMissingColumnsPadding2000() throws OrekitException, ParseException {
        setRoot("rapid-data-columns");
        EOP2000History history = new EOP2000History();
        new RapidDataAndPredictionColumnsLoader("^finals2000A\\.daily$").fillHistory(history);

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
        EOP2000History history = new EOP2000History();
        new RapidDataAndPredictionColumnsLoader("^finals2000A\\.daily$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2011, 10, 6, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

}
