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

import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;


public class SatelliteClockScaleTest {

    private TimeScale          utc;
    private DateTimeComponents dtc;
    private AbsoluteDate       epoch;

    @Test
    public void testZeroAtEpoch() {
        final double    offset = 0.0;
        final double    drift  = 0.001;
        final TimeScale sclk   = new SatelliteClockScale("SCLK", epoch, utc, offset, drift);
        Assertions.assertEquals("SCLK", sclk.toString());
        Assertions.assertEquals(-25.0, sclk.offsetFromTAI(epoch), 1.0e-12);
        Assertions.assertEquals(-24.0, sclk.offsetFromTAI(epoch.shiftedBy(1000)), 1.0e-12);
    }

    @Test
    public void testNonZeroAtEpoch() {
        final double    offset = 325.0;
        final double    drift  = 0.002;
        final TimeScale sclk   = new SatelliteClockScale("SCLK", epoch, utc, offset, drift);
        Assertions.assertEquals("SCLK", sclk.toString());
        Assertions.assertEquals(300.0, sclk.offsetFromTAI(epoch), 1.0e-12);
        Assertions.assertEquals(302.0, sclk.offsetFromTAI(epoch.shiftedBy(1000)), 1.0e-12);
    }

    @Test
    public void testField() {
        final double    offset = 0.0;
        final double    drift  = 0.001;
        final TimeScale sclk   = new SatelliteClockScale("SCLK",epoch, utc,  offset, drift);
        FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(epoch, new Binary64(1000.0));
        Assertions.assertEquals(-24.0, sclk.offsetFromTAI(date).getReal(), 1.0e-12);
    }

    @Test
    public void testParseEpoch() {
        final double       offset = 325.0;
        final double       drift  = 10.002;
        final TimeScale    sclk   = new SatelliteClockScale("SCLK", epoch, utc, offset, drift);
        final AbsoluteDate date   = new AbsoluteDate(dtc.getDate(),
                                               new TimeComponents(dtc.getTime().getSecondsInUTCDay() +
                                                                  offset),
                                               sclk);
        Assertions.assertEquals(0.0, date.durationFrom(epoch), 2.0e-12);
    }

    @Test
    public void testParse() {
        final double       offset = 325.0;
        final double       drift  = 10.002;
        final TimeScale    sclk   = new SatelliteClockScale("SCLK", epoch, utc, offset, drift);
        final double       shift  = 2343.426;
        final AbsoluteDate date   = new AbsoluteDate(dtc.getDate(),
                                               new TimeComponents(dtc.getTime().getSecondsInUTCDay() +
                                                                  offset + shift),
                                               sclk);
        Assertions.assertEquals(shift / (1 + drift), date.durationFrom(epoch), 2.0e-12);
    }

    @Test
    public void testSymmetryDrift() {
        final double       offset = 325.0;
        final double       drift  = 10.002;
        final TimeScale    sclk   = new SatelliteClockScale("SCLK", epoch, utc, offset, drift);
        for (double dt = -1000; dt < 1000; dt += 0.01) {
            AbsoluteDate ref     = epoch.shiftedBy(dt);
            AbsoluteDate rebuilt = new AbsoluteDate(ref.getComponents(sclk), sclk);
            Assertions.assertEquals(0.0, rebuilt.durationFrom(ref), 6.0e-12);
        }
    }

    @Test
    public void testCountSymmetry() {
        final double              offset = 325.0;
        final double              drift  = 10.002;
        final SatelliteClockScale sclk   = new SatelliteClockScale("SCLK", epoch, utc, offset, drift);
        for (double count = -1000; count < 1000; count += 0.01) {
            double rebuilt = sclk.countAtDate(sclk.dateAtCount(count));
            Assertions.assertEquals(count, rebuilt, 2.0e-13);
        }
    }

    @Test
    public void testCount() {
        final double              offset = 325.0;
        final double              drift  = 10.002;
        final SatelliteClockScale sclk   = new SatelliteClockScale("SCLK", epoch, utc, offset, drift);
        Assertions.assertEquals(0.0,    sclk.dateAtCount(offset).durationFrom(epoch), 1.0e-15);
        Assertions.assertEquals(offset, sclk.countAtDate(epoch),                      1.0e-15);
        RandomGenerator random = new Well19937a(0xc7607abceb6835bdl);
        AbsoluteDate previous = epoch;
        for (int i = 0; i < 100; ++i) {
            AbsoluteDate current = epoch.shiftedBy((random.nextDouble() * 10000) - 5000);
            double deltaT     = current.durationFrom(previous);
            double deltaCount = sclk.countAtDate(current) - sclk.countAtDate(previous);
            Assertions.assertEquals(drift, deltaCount / deltaT - 1.0, 2.0e-12);
            previous = current;
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        utc   = TimeScalesFactory.getUTC();
        dtc   = new DateTimeComponents(1990, 6, 23, 11, 30, 45.756);
        epoch = new AbsoluteDate(dtc, utc);
    }

}
