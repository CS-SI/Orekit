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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.ClasspathCrawler;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.LazyLoadedTimeScales;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * Unit tests for {@link LazyLoadedFrames}.
 *
 * @author Evan Ward
 * @see FramesFactoryTest
 */
public class LazyLoadedFramesTest {

    /**
     * Before 10.1 calling {@link FramesFactory#addDefaultEOP1980HistoryLoaders(String,
     * String, String, String, String)} before leap seconds were available worked just
     * fine. Ensure this class is capable of the same behavior.
     */
    @Test
    public void testAddLoadersWithoutUtc() {
        // setup
        DataProvidersManager manager = new DataProvidersManager();
        // prevent adding other providers
        manager.addProvider(new ClasspathCrawler("no-data/dummy.txt"));
        LazyLoadedEop eop = new LazyLoadedEop(manager);
        TimeScales timeScales = new LazyLoadedTimeScales(eop);
        LazyLoadedFrames frames = new LazyLoadedFrames(eop, timeScales, null);

        // actions
        frames.addDefaultEOP1980HistoryLoaders(null, null, null, null, null, null);
        frames.addDefaultEOP2000HistoryLoaders(null, null, null, null, null, null);
        frames.addEOPHistoryLoader(IERSConventions.IERS_2010, null);

        // verify: no exceptions thrown
    }

    @Test
    public void testInterpolationDegreeEffect() {
        DataProvidersManager manager = new DataProvidersManager();
        manager.addProvider(new ClasspathCrawler("regular-data/UTC-TAI.history"));
        manager.addProvider(new ClasspathCrawler("regular-data/Earth-orientation-parameters/yearly/eopc04_08_IAU2000.03"));
        LazyLoadedEop eop03 = new LazyLoadedEop(manager);
        eop03.setInterpolationDegree(3);
        EOPHistory history03 = eop03.getEOPHistory(IERSConventions.IERS_2010, false, new LazyLoadedTimeScales(eop03));
        LazyLoadedEop eop07 = new LazyLoadedEop(manager);
        eop07.setInterpolationDegree(7);
        EOPHistory history07 = eop07.getEOPHistory(IERSConventions.IERS_2010, false, new LazyLoadedTimeScales(eop07));
        final AbsoluteDate date = history03.getStartDate().shiftedBy(3.125 * Constants.JULIAN_DAY);
        Assertions.assertEquals(-0.290422121, history03.getUT1MinusUTC(date), 1.0e-9);
        Assertions.assertEquals(-0.290421707, history07.getUT1MinusUTC(date), 1.0e-9);
    }

    @Test
    public void testWrongInterpolationDegree() {
        DataProvidersManager manager = new DataProvidersManager();
        manager.addProvider(new ClasspathCrawler("regular-data/UTC-TAI.history"));
        LazyLoadedEop eop = new LazyLoadedEop(manager);
        eop.setInterpolationDegree(4);
        LazyLoadedTimeScales ts = new LazyLoadedTimeScales(eop);
        try {
            eop.getEOPHistory(IERSConventions.IERS_2010, false, ts);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.WRONG_EOP_INTERPOLATION_DEGREE, oe.getSpecifier());
            Assertions.assertEquals(4, ((Integer) oe.getParts()[0]).intValue());
        }
    }

}
