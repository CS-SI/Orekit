/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.gnss;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.CombinedObservationData;
import org.orekit.gnss.CombinedObservationDataSet;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.RinexHeader;
import org.orekit.gnss.RinexLoader;
import org.orekit.gnss.SatelliteSystem;

public class MeasurementCombinationFactoryTest {

    /** Threshold for test acceptance. */
    private static double eps = 1.0e-4;

    /** First observation data. */
    private ObservationData obs1;

    /** Satellite system used for the tests. */
    private SatelliteSystem system;

    /** RINEX 2 Observation data set. */
    private ObservationDataSet dataSetRinex2;

    /** RINEX 3 Observation data set. */
    private ObservationDataSet dataSetRinex3;

    @Before
    public void setUp() throws NoSuchAlgorithmException, IOException {
        Utils.setDataRoot("gnss");

        // Observation data
        obs1 = new ObservationData(ObservationType.L1, 2.25E7, 0, 0);

        // RINEX 2 Observation data set
        RinexLoader loader2 = load("rinex/truncate-sbch0440.16o");
        dataSetRinex2 = loader2.getObservationDataSets().get(0);

        // RINEX 3 Observation data set
        RinexLoader loader3 = load("rinex/aaaa0000.00o");
        dataSetRinex3 = loader3.getObservationDataSets().get(1);

        // Satellite system
        system = dataSetRinex2.getSatelliteSystem();
    }

    @Test
    public void testEmptyDataSetGeometryFree() {
        doTestEmptyDataSet(MeasurementCombinationFactory.getGeometryFreeCombination(system));
    }

    @Test
    public void testEmptyDataSetIonoFree() {
        doTestEmptyDataSet(MeasurementCombinationFactory.getIonosphereFreeCombination(system));
    }

    @Test
    public void testEmptyDataSetWideLane() {
        doTestEmptyDataSet(MeasurementCombinationFactory.getWideLaneCombination(system));
    }

    @Test
    public void testEmptyDataSetNarrowLane() {
        doTestEmptyDataSet(MeasurementCombinationFactory.getNarrowLaneCombination(system));
    }

    @Test
    public void testEmptyDataSetMelbourneWubbena() {
        doTestEmptyDataSet(MeasurementCombinationFactory.getMelbourneWubbenaCombination(system));
    }

    /**
     * Test code stability if an empty observation data set is used. 
     */
    private void doTestEmptyDataSet(final MeasurementCombination combination) {
        // Build empty observation data set
        final ObservationDataSet emptyDataSet = new ObservationDataSet(dataSetRinex2.getHeader(), dataSetRinex2.getSatelliteSystem(),
                                                                       dataSetRinex2.getPrnNumber(), dataSetRinex2.getDate(), dataSetRinex2.getRcvrClkOffset(),
                                                                       new ArrayList<ObservationData>());
        // Test first method signature
        final CombinedObservationDataSet combinedData = combination.combine(emptyDataSet);
        Assert.assertEquals(0, combinedData.getObservationData().size());
    }

    @Test
    public void testExceptionsGeometryFree() {
        doTestExceptions(MeasurementCombinationFactory.getGeometryFreeCombination(system));
    }

    @Test
    public void testExceptionsIonoFree() {
        doTestExceptions(MeasurementCombinationFactory.getIonosphereFreeCombination(system));
    }

    @Test
    public void testExceptionsWideLane() {
        doTestExceptions(MeasurementCombinationFactory.getWideLaneCombination(system));
    }

    @Test
    public void testExceptionsNarrowLane() {
        doTestExceptions(MeasurementCombinationFactory.getNarrowLaneCombination(system));
    }

    /**
     * Test exceptions. 
     */
    private void doTestExceptions(final AbstractDualFrequencyCombination combination) {
        // Test INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS exception
        try {
            final ObservationData observation = new ObservationData(ObservationType.L1, 12345678.0, 0, 0);
            combination.combine(obs1, observation);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS, oe.getSpecifier());
        }

        // Test INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS exception
        try {
            final ObservationData observation = new ObservationData(ObservationType.D2, 12345678.0, 0, 0);
            combination.combine(obs1, observation);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2GeometryFree() {
        doTestRinex(MeasurementCombinationFactory.getGeometryFreeCombination(system),
                     CombinationType.GEOMETRY_FREE, 6.953, 27534453.519,0.0,  Double.NaN, 2, 2);
    }

    @Test
    public void testRinex2IonoFree() {
        doTestRinex(MeasurementCombinationFactory.getIonosphereFreeCombination(system),
                     CombinationType.IONO_FREE, 23732467.5026, 167275826.4529, 0.0, 9316 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2WideLane() {
        doTestRinex(MeasurementCombinationFactory.getWideLaneCombination(system),
                     CombinationType.WIDE_LANE, 23732453.7100, 221895480.9217, 0.0, 34 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2NarrowLane() {
        doTestRinex(MeasurementCombinationFactory.getNarrowLaneCombination(system),
                     CombinationType.NARROW_LANE, 23732481.2951, 112656171.9842, 0.0, 274 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2MelbourneWubbena() {
        doTestRinex(MeasurementCombinationFactory.getMelbourneWubbenaCombination(system),
                     CombinationType.MELBOURNE_WUBBENA, 0.0, 0.0, 198162999.6266, 34 * Frequency.F0, 1, 2);
    }

    @Test
    public void testRinex3GeometryFree() {
        doTestRinex(MeasurementCombinationFactory.getGeometryFreeCombination(system),
                     CombinationType.GEOMETRY_FREE, 2.187, 3821708.096, 0.0, Double.NaN, 2, 3);
    }

    @Test
    public void testRinex3IonoFree() {
        doTestRinex(MeasurementCombinationFactory.getIonosphereFreeCombination(system),
                     CombinationType.IONO_FREE, 22399214.1934, 134735627.3126, 0.0, 1175 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3WideLane() {
        doTestRinex(MeasurementCombinationFactory.getWideLaneCombination(system),
                     CombinationType.WIDE_LANE, 22399239.8790, 179620369.2060, 0.0, 5 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3NarrowLane() {
        doTestRinex(MeasurementCombinationFactory.getNarrowLaneCombination(system),
                    CombinationType.NARROW_LANE, 22399188.5078, 89850885.4191, 0.0, 235 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3MelbourneWubbena() {
        doTestRinex(MeasurementCombinationFactory.getMelbourneWubbenaCombination(system),
                     CombinationType.MELBOURNE_WUBBENA, 0.0, 0.0, 157221180.6982, 5 * Frequency.F0, 1, 3);
    }

    /**
     * Test if Rinex formats can be used for the combination of measurements
     */
    private void doTestRinex(final MeasurementCombination combination, final CombinationType expectedType,
                             final double expectedRangeValue, final double expectedPhaseValue, final double expectedRangePhase,
                             final double expectedFrequency, final int expectedSize, final int rinexVersion) {

        // Perform combination on the observation data set depending the Rinex version
        final CombinedObservationDataSet combinedDataSet;
        if (rinexVersion == 2) {
            combinedDataSet = combination.combine(dataSetRinex2);
            checkCombinedDataSet(combinedDataSet, expectedSize);
        } else {
            combinedDataSet = combination.combine(dataSetRinex3);
            Assert.assertEquals(expectedSize, combinedDataSet.getObservationData().size());
        }

        Assert.assertEquals(expectedType.getName(), combination.getName());

        // Verify the combined observation data
        for (CombinedObservationData cod : combinedDataSet.getObservationData()) {

            if (cod.getMeasurementType() == MeasurementType.CARRIER_PHASE) {

                Assert.assertEquals(expectedPhaseValue, cod.getValue(),                eps);
                Assert.assertEquals(expectedFrequency,  cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(expectedType,       cod.getCombinationType());

            } else if (cod.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                Assert.assertEquals(expectedRangeValue, cod.getValue(),                eps);
                Assert.assertEquals(expectedFrequency,  cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(expectedType,       cod.getCombinationType());

            } else if (cod.getMeasurementType() == MeasurementType.COMBINED_RANGE_PHASE) {

                Assert.assertEquals(expectedRangePhase, cod.getValue(),                eps);
                Assert.assertEquals(expectedFrequency,  cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(expectedType,       cod.getCombinationType());

            }

        }
    }

    private void checkCombinedDataSet(final CombinedObservationDataSet combinedDataSet,
                                      final int expectedSize) {
        // Verify the number of combined data set
        Assert.assertEquals(expectedSize, combinedDataSet.getObservationData().size());
        // Verify RINEX Header
        final RinexHeader header = combinedDataSet.getHeader();
        Assert.assertEquals(2.11, header.getRinexVersion(), eps);
        // Verify satellite data
        Assert.assertEquals(30, combinedDataSet.getPrnNumber());
        Assert.assertEquals(SatelliteSystem.GPS, combinedDataSet.getSatelliteSystem());
        // Verify receiver clock
        Assert.assertEquals(0.0, combinedDataSet.getRcvrClkOffset(), eps);
        // Verify date
        Assert.assertEquals("2016-02-13T00:49:43.000", combinedDataSet.getDate().toString());
    }

    private RinexLoader load(final String name) {
        return new RinexLoader(Utils.class.getClassLoader().getResourceAsStream(name), name);
    }

}
