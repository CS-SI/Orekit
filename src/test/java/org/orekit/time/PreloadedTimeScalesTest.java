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
package org.orekit.time;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.LazyLoadedDataContext;
import org.orekit.frames.EopHistoryLoader;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.ITRFVersionLoader.ITRFVersionConfiguration;
import org.orekit.frames.ItrfVersionProvider;
import org.orekit.time.TAIUTCDatFilesLoader.Parser;
import org.orekit.utils.IERSConventions;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Unit tests for {@link PreloadedTimeScales}
 *
 * @author Evan Ward
 */
public class PreloadedTimeScalesTest {

    /** Subject under test. */
    private TimeScales timeScales;

    /**
     * Create the time scales under test.
     *
     * @throws IOException on error.
     */
    @BeforeEach
    public void setUp() throws IOException {
        final String leapPath = "/USNO/tai-utc.dat";
        final String eopPath =
                "/regular-data/Earth-orientation-parameters/yearly/finals2000A.2002.xml";
        final ITRFVersionConfiguration configuration = new ITRFVersionConfiguration(
                "", ITRFVersion.ITRF_2014, Integer.MIN_VALUE, Integer.MAX_VALUE);
        final ItrfVersionProvider itrfVersionProvider = (name, mjd) -> configuration;
        final List<OffsetModel> leapSeconds = new Parser().parse(
                PreloadedTimeScales.class.getResourceAsStream(leapPath),
                leapPath);
        this.timeScales = TimeScales.of(
                leapSeconds,
                (conventions, timeScales) -> {
                    try {
                        return EopHistoryLoader.Parser
                                .newFinalsXmlParser(conventions, itrfVersionProvider, timeScales)
                                .parse(PreloadedTimeScales.class.getResourceAsStream(eopPath), eopPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /** Rough check that the time scales work as expected. */
    @Test
    public void testTime() {
        // actions
        AbsoluteDate javaEpoch = timeScales.getJavaEpoch();
        AbsoluteDate j2000Epoch = timeScales.getJ2000Epoch();
        TAIScale tai = timeScales.getTAI();
        UTCScale utc = timeScales.getUTC();
        UT1Scale ut1 = timeScales.getUT1(IERSConventions.IERS_2010, true);
        AbsoluteDate date = new AbsoluteDate(2002, 12, 31, utc);
        AbsoluteDate date2 = new AbsoluteDate(1977, 1, 1, tai);
        AbsoluteDate date3 = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, ut1);

        // verify
        Assertions.assertEquals("1970-01-01T00:00:00.000", javaEpoch.toString(utc));
        Assertions.assertEquals(-32, utc.offsetFromTAI(date), 0);
        Assertions.assertEquals(-32.2889120, ut1.offsetFromTAI(date), 1e-14);
        Assertions.assertEquals(0, tai.offsetFromTAI(date), 0);
        Assertions.assertEquals(-32, timeScales.getUTC().offsetFromTAI(date), 0);
        Assertions.assertEquals(-32 + 24110.54841, timeScales.getGMST(IERSConventions.IERS_2010, true).offsetFromTAI(date3), 0);
        Assertions.assertEquals(-32 + 3 * 3600, timeScales.getGLONASS().offsetFromTAI(date), 0);
        Assertions.assertEquals(-19, timeScales.getGPS().offsetFromTAI(date), 0);
        Assertions.assertEquals(-19, timeScales.getQZSS().offsetFromTAI(date), 0);
        Assertions.assertEquals(-19, timeScales.getGST().offsetFromTAI(date), 0);
        Assertions.assertEquals(-19, timeScales.getIRNSS().offsetFromTAI(date), 0);
        Assertions.assertEquals(32.184, timeScales.getTT().offsetFromTAI(date), 0);
        Assertions.assertEquals(32.184, timeScales.getTDB().offsetFromTAI(j2000Epoch.shiftedBy(216525.908119)), 0);
        Assertions.assertEquals(32.184, timeScales.getTCB().offsetFromTAI(date2), 1e-4);
        Assertions.assertEquals(32.184, timeScales.getTCG().offsetFromTAI(date2), 0);
    }

    /** Check the UT1 creation logic. */
    @Test
    public void testUt1() {
        // setup
        UT1Scale ut12010Simple = timeScales.getUT1(IERSConventions.IERS_2010, true);
        UT1Scale ut12010Full = timeScales.getUT1(IERSConventions.IERS_2010, false);
        UT1Scale ut12003Simple = timeScales.getUT1(IERSConventions.IERS_2003, true);
        UT1Scale ut12003Full = timeScales.getUT1(IERSConventions.IERS_2003, false);
        UT1Scale ut11996Simple = timeScales.getUT1(IERSConventions.IERS_1996, true);
        UT1Scale ut11996Full = timeScales.getUT1(IERSConventions.IERS_1996, false);
        UTCScale utc = timeScales.getUTC();
        AbsoluteDate date = new AbsoluteDate(2002, 12, 31, utc);


        // verify
        Assertions.assertSame(ut12010Simple, timeScales.getUT1(IERSConventions.IERS_2010, true));
        Assertions.assertSame(ut12010Full, timeScales.getUT1(IERSConventions.IERS_2010, false));
        Assertions.assertSame(ut12003Simple, timeScales.getUT1(IERSConventions.IERS_2003, true));
        Assertions.assertSame(ut12003Full, timeScales.getUT1(IERSConventions.IERS_2003, false));
        Assertions.assertSame(ut11996Simple, timeScales.getUT1(IERSConventions.IERS_1996, true));
        Assertions.assertSame(ut11996Full, timeScales.getUT1(IERSConventions.IERS_1996, false));


        Assertions.assertEquals(-32.2889120, ut12010Simple.offsetFromTAI(date), 1e-14);
        Assertions.assertEquals(-32.2889120, ut12003Simple.offsetFromTAI(date), 1e-14);
        Assertions.assertEquals(-32.2889120, ut11996Simple.offsetFromTAI(date), 1e-14);
        Assertions.assertEquals(-2e-5, -32.2889120 - ut12010Full.offsetFromTAI(date), 1e-5);
        Assertions.assertEquals(-2e-5, -32.2889120 - ut12003Full.offsetFromTAI(date), 1e-5);
        Assertions.assertEquals(-2e-5, -32.2889120 - ut11996Full.offsetFromTAI(date), 1e-5);

    }

    /** Compare with another data context. */
    @Test
    public void testComparison() {
        // setup
        LazyLoadedDataContext dataContext = new LazyLoadedDataContext();
        dataContext.getDataProvidersManager().addProvider(
                new DirectoryCrawler(new File("src/test/resources/zero-EOP")));
        LazyLoadedTimeScales other = dataContext.getTimeScales();
        DateTimeComponents date = new DateTimeComponents(2002, 12, 30);

        // action
        final UT1Scale ut1 = timeScales.getUT1(IERSConventions.IERS_2010, true);
        final UT1Scale otherUt1 = other.getUT1(IERSConventions.IERS_2010, true);
        double actual = new AbsoluteDate(date, ut1)
                .durationFrom(new AbsoluteDate(date, otherUt1));

        // verify
        Assertions.assertEquals(0.2881680, actual, FastMath.ulp(32));
    }

}
