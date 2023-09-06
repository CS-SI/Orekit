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
package org.orekit.files.rinex.observation;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;

public class RinexObservationTest {

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testGenerateRegular() {
        final double interval = 300.0;
        final int    n        = 100;
        RinexObservation rinexObservation = generate(AbsoluteDate.ARBITRARY_EPOCH,
                                                     AbsoluteDate.ARBITRARY_EPOCH.shiftedBy((n - 1) * interval),
                                                     interval, n);
        Assertions.assertEquals(n, rinexObservation.getObservationDataSets().size());
    }

    @Test
    public void testWrongSampling() {
        final double interval = 300.0;
        final int    n        = 100;
        RinexObservation rinexObservation = generate(AbsoluteDate.ARBITRARY_EPOCH,
                                                     AbsoluteDate.ARBITRARY_EPOCH.shiftedBy((n - 1) * interval),
                                                     interval, n - 10);
        final List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        final AbsoluteDate lastGenerated = ods.get(ods.size() - 1).getDate();
        try {
            rinexObservation.addObservationDataSet(dummyMeasurement(lastGenerated.shiftedBy(0.75 * interval)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.INCONSISTENT_SAMPLING_DATE, oiae.getSpecifier());
        }
    }

    @Test
    public void testOutOfRange() {
        final double interval = 300.0;
        final int    n        = 100;
        RinexObservation rinexObservation = generate(AbsoluteDate.ARBITRARY_EPOCH,
                                                     AbsoluteDate.ARBITRARY_EPOCH.shiftedBy((n - 1) * interval),
                                                     interval, n);
        final List<ObservationDataSet> ods = rinexObservation.getObservationDataSets();
        final AbsoluteDate lastGenerated = ods.get(ods.size() - 1).getDate();
        try {
            rinexObservation.addObservationDataSet(dummyMeasurement(lastGenerated.shiftedBy(interval)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_DATE, oiae.getSpecifier());
        }
    }

    private RinexObservation generate(final AbsoluteDate first, final AbsoluteDate last,
                                      final double interval, final int n) {
        final RinexObservation rinexObservation = new RinexObservation();
        rinexObservation.getHeader().setInterval(interval);
        rinexObservation.getHeader().setTFirstObs(first);
        rinexObservation.getHeader().setTLastObs(last);
        for (int i = 0; i < n; ++i) {
            rinexObservation.addObservationDataSet(dummyMeasurement(first.shiftedBy(i * interval)));
        }
        return rinexObservation;
    }

    private ObservationDataSet dummyMeasurement(final AbsoluteDate date) {
        return new ObservationDataSet(new SatInSystem(SatelliteSystem.GALILEO, 11),
                                      date, 0, 0, new ArrayList<>());
    }

}
