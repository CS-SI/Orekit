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

import org.junit.Test;
import org.orekit.data.ClasspathCrawler;
import org.orekit.data.DataProvidersManager;
import org.orekit.time.LazyLoadedTimeScales;
import org.orekit.time.TimeScales;
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
        frames.addDefaultEOP1980HistoryLoaders(null, null, null, null, null);
        frames.addDefaultEOP2000HistoryLoaders(null, null, null, null, null);
        frames.addEOPHistoryLoader(IERSConventions.IERS_2010, null);

        // verify: no exceptions thrown
    }

}
