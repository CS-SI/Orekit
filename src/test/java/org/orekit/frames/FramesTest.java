/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.LazyLoadedDataContext;
import org.orekit.frames.ITRFVersionLoader.ITRFVersionConfiguration;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.OffsetModel;
import org.orekit.time.TAIUTCDatFilesLoader.Parser;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * Unit tests for methods implemented in {@link Frames}.
 *
 * @author Evan Ward
 */
public class FramesTest {

    /** Time scales for testing. */
    private TimeScales timeScales;

    /**
     * Create {@link #timeScales}.
     *
     * @throws IOException on error.
     */
    @Before
    public void setUp() throws IOException {
        final String leapPath = "/USNO/tai-utc.dat";
        final String eopPath = "/rapid-data-columns/finals2000A.daily";
        final ITRFVersionConfiguration configuration = new ITRFVersionConfiguration(
                "", ITRFVersion.ITRF_2014, Integer.MIN_VALUE, Integer.MAX_VALUE);
        final ItrfVersionProvider itrfVersionProvider = (name, mjd) -> configuration;
        final List<OffsetModel> leapSeconds = new Parser().parse(
                this.getClass().getResourceAsStream(leapPath),
                leapPath);
        this.timeScales = TimeScales.of(
                leapSeconds,
                (conventions, timeScales) -> {
                    try {
                        return EOPHistoryLoader.Parser
                                .newFinalsColumnsParser(
                                        conventions,
                                        itrfVersionProvider,
                                        timeScales,
                                        true)
                                .parse(
                                        this.getClass().getResourceAsStream(eopPath),
                                        eopPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /** Check {@link Frames#of(TimeScales, Supplier)}. */
    @Test
    public void testOf() {
        // action
        Frames frames = Frames.of(timeScales, () -> null);
        Frame itrf = frames.getITRF(IERSConventions.IERS_2010, true);
        EOPHistory eopHistory = ((ITRFProvider) itrf.getTransformProvider()).getEOPHistory();
        Frame itrfEquinox = frames.getITRFEquinox(IERSConventions.IERS_2010, true);
        Frame itrfFull = frames.getITRF(IERSConventions.IERS_2010, false);
        EOPHistory eopFull = ((ITRFProvider) itrfFull.getTransformProvider()).getEOPHistory();

        // verify
        Assert.assertEquals(eopHistory.getConventions(), IERSConventions.IERS_2010);
        Assert.assertEquals(eopFull.getConventions(), IERSConventions.IERS_2010);
        Assert.assertEquals(eopHistory.getTimeScales(), timeScales);
        Assert.assertEquals(eopFull.getTimeScales(), timeScales);
        // share EOP history when conventions and tidal corrections are the same
        Assert.assertSame(
                timeScales.getUT1(IERSConventions.IERS_2010, true).getEOPHistory(),
                eopHistory);
        Assert.assertSame(
                eopHistory,
                ((ITRFProvider) itrfEquinox.getTransformProvider()).getEOPHistory());
        // changing tidal corrections still shares the same data.
        Assert.assertNotEquals(eopFull, eopHistory);
        final int n = 181;
        List<EOPEntry> entries = eopHistory.getEntries();
        List<EOPEntry> entriesFull = eopFull.getEntries();
        Assert.assertEquals(n, entries.size());
        Assert.assertEquals(n, entriesFull.size());
        for (int i = 0; i < n; i++) {
            Assert.assertSame(entries.get(i), entriesFull.get(i));
        }
        // ICRF
        Assert.assertEquals(null, frames.getICRF());
    }

    /** Check transforms between frames from different data contexts. */
    @Test
    public void testComparison() {
        // setup
        Frames frames = Frames.of(timeScales, () -> null);
        LazyLoadedDataContext dataContext = new LazyLoadedDataContext();
        dataContext.getDataProvidersManager().addProvider(
                new DirectoryCrawler(new File("src/test/resources/regular-data")));
        LazyLoadedFrames other = dataContext.getFrames();
        AbsoluteDate date = new AbsoluteDate(2011, 5, 1, timeScales.getUTC());

        // verify
        Assert.assertSame(frames.getGCRF(), other.getGCRF());
        Frame itrf = frames.getITRF(IERSConventions.IERS_2010, true);
        Frame otherItrf = other.getITRF(IERSConventions.IERS_2010, true);
        Transform transform = itrf.getTransformTo(otherItrf, date);
        final double angle = transform.getRotation().getAngle();
        // rough estimate based on EOP file
        final double expected = new Vector3D(
                0.2449186 / Constants.JULIAN_DAY * 2 * FastMath.PI,
                0.341136 * Constants.ARC_SECONDS_TO_RADIANS,
                0.3e-3 * Constants.ARC_SECONDS_TO_RADIANS).getNorm();
        Assert.assertEquals(expected, angle, 1e-2 * expected);
    }

}
