/* Copyright 2023 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.analytical.tle.generation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;


public class LeastSquaresTleGenerationAlgorithmTest {

    private TLE geoTLE;
    private TLE leoTLE;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        geoTLE = new TLE("1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                         "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");
        leoTLE = new TLE("1 31135U 07013A   11003.00000000  .00000816  00000+0  47577-4 0    11",
                         "2 31135   2.4656 183.9084 0021119 236.4164  60.4567 15.10546832    15");
    }

    @Test
    public void testConversionLeo() {
        checkConversion(leoTLE, 1.0e-12, 3.755238453429068E-9);
    }

    @Test
    public void testConversionGeo() {
        checkConversion(geoTLE, 1.0e-12, 3.135996497102161E-9);
    }

    /** Check the State to TLE conversion. */
    private void checkConversion(final TLE tle, final double threshold, final double rms) {

        Propagator p = TLEPropagator.selectExtrapolator(tle);
        LeastSquaresTleGenerationAlgorithm converter = new LeastSquaresTleGenerationAlgorithm();
        final TLE converted = converter.generate(p.getInitialState(), tle);

        Assertions.assertEquals(tle.getSatelliteNumber(),         converted.getSatelliteNumber());
        Assertions.assertEquals(tle.getClassification(),          converted.getClassification());
        Assertions.assertEquals(tle.getLaunchYear(),              converted.getLaunchYear());
        Assertions.assertEquals(tle.getLaunchNumber(),            converted.getLaunchNumber());
        Assertions.assertEquals(tle.getLaunchPiece(),             converted.getLaunchPiece());
        Assertions.assertEquals(tle.getElementNumber(),           converted.getElementNumber());
        Assertions.assertEquals(tle.getRevolutionNumberAtEpoch(), converted.getRevolutionNumberAtEpoch());

        Assertions.assertEquals(tle.getMeanMotion(), converted.getMeanMotion(), threshold * tle.getMeanMotion());
        Assertions.assertEquals(tle.getE(), converted.getE(), threshold * tle.getE());
        Assertions.assertEquals(tle.getI(), converted.getI(), threshold * tle.getI());
        Assertions.assertEquals(tle.getPerigeeArgument(), converted.getPerigeeArgument(), threshold * tle.getPerigeeArgument());
        Assertions.assertEquals(tle.getRaan(), converted.getRaan(), threshold * tle.getRaan());
        Assertions.assertEquals(tle.getMeanAnomaly(), converted.getMeanAnomaly(), threshold * tle.getMeanAnomaly());

        Assertions.assertEquals(converter.getRms(), rms, threshold);

    }

    @Test
    public void testIssue864() {

        // Initialize TLE
        final TLE tleISS = new TLE("1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                   "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");

        // TLE propagator
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleISS);

        // State at TLE epoch
        final SpacecraftState state = propagator.propagate(tleISS.getDate());

        // Set the BStar driver to selected
        tleISS.getParametersDrivers().forEach(driver -> driver.setSelected(true));

        // Convert to TLE
        final TLE rebuilt = new LeastSquaresTleGenerationAlgorithm().generate(state, tleISS);

        // Verify if driver is still selected
        rebuilt.getParametersDrivers().forEach(driver -> Assertions.assertTrue(driver.isSelected()));

    }

}
