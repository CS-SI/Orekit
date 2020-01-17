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
package org.orekit.estimation.measurements.gnss;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testEmptyDataSetPhaseMinusCode() {
        doTestEmptyDataSet(MeasurementCombinationFactory.getPhaseMinusCodeCombination(system));
    }

    @Test
    public void testEmptyDataSetGRAPHIC() {
        doTestEmptyDataSet(MeasurementCombinationFactory.getGRAPHICCombination(system));
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
        doTestExceptionsDualFrequency(MeasurementCombinationFactory.getGeometryFreeCombination(system));
    }

    @Test
    public void testExceptionsIonoFree() {
        doTestExceptionsDualFrequency(MeasurementCombinationFactory.getIonosphereFreeCombination(system));
    }

    @Test
    public void testExceptionsWideLane() {
        doTestExceptionsDualFrequency(MeasurementCombinationFactory.getWideLaneCombination(system));
    }

    @Test
    public void testExceptionsNarrowLane() {
        doTestExceptionsDualFrequency(MeasurementCombinationFactory.getNarrowLaneCombination(system));
    }

    @Test
    public void testExceptionsPhaseMinusCode() {
        doTestExceptionsSingleFrequency(MeasurementCombinationFactory.getPhaseMinusCodeCombination(system));
    }

    @Test
    public void testExceptionsGRAPHIC() {
        doTestExceptionsSingleFrequency(MeasurementCombinationFactory.getGRAPHICCombination(system));
    }

    private void doTestExceptionsSingleFrequency(final AbstractSingleFrequencyCombination combination) {
        // Test INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS exception
        try {
            final ObservationData observation = new ObservationData(ObservationType.L5, 12345678.0, 0, 0);
            combination.combine(obs1, observation);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS, oe.getSpecifier());
        }

        // Test INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS exception
        try {
            final ObservationData observation = new ObservationData(ObservationType.L1, 12345678.0, 0, 0);
            combination.combine(obs1, observation);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS, oe.getSpecifier());
        }
    }

    /**
     * Test exceptions. 
     */
    private void doTestExceptionsDualFrequency(final AbstractDualFrequencyCombination combination) {
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
        doTestRinexDualFrequency(MeasurementCombinationFactory.getGeometryFreeCombination(system),
                     CombinationType.GEOMETRY_FREE, 6.953, 27534453.519,0.0,  Double.NaN, 2, 2);
    }

    @Test
    public void testRinex2IonoFree() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getIonosphereFreeCombination(system),
                     CombinationType.IONO_FREE, 23732467.5026, 167275826.4529, 0.0, 4658 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2WideLane() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getWideLaneCombination(system),
                     CombinationType.WIDE_LANE, 23732453.7100, 221895480.9217, 0.0, 34 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2NarrowLane() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getNarrowLaneCombination(system),
                     CombinationType.NARROW_LANE, 23732481.2951, 112656171.9842, 0.0, 274 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2MelbourneWubbena() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getMelbourneWubbenaCombination(system),
                     CombinationType.MELBOURNE_WUBBENA, 0.0, 0.0, 198162999.6266, 34 * Frequency.F0, 1, 2);
    }

    @Test
    public void testRinex2PhaseMinusCode() {
        doTestRinex2SingleFrequency(MeasurementCombinationFactory.getPhaseMinusCodeCombination(system),
                                    CombinationType.PHASE_MINUS_CODE, 100982578.487, 73448118.015, 73448118.300);
    }

    @Test
    public void testRinex2GRAPHIC() {
        doTestRinex2SingleFrequency(MeasurementCombinationFactory.getGRAPHICCombination(system),
                                    CombinationType.GRAPHIC, 74223767.4935, 60456544.2105, 60456544.068);
    }

    private void doTestRinex2SingleFrequency(final MeasurementCombination combination, final CombinationType type,
                                             final double expectedL1C1, final double expectedL2C2, final double expectedL2P2) {
        // Perform combination on the observation data set depending the Rinex version
        final CombinedObservationDataSet combinedDataSet = combination.combine(dataSetRinex2);
        checkCombinedDataSet(combinedDataSet, 3);
        Assert.assertEquals(type.getName(), combination.getName());
        // Verify the combined observation data
        final List<CombinedObservationData> data = combinedDataSet.getObservationData();
        // L1/C1
        Assert.assertEquals(expectedL1C1,       data.get(0).getValue(),                eps);
        Assert.assertEquals(154 * Frequency.F0, data.get(0).getCombinedMHzFrequency(), eps);
        // L2/C2
        Assert.assertEquals(expectedL2C2,       data.get(1).getValue(),                eps);
        Assert.assertEquals(120 * Frequency.F0, data.get(1).getCombinedMHzFrequency(), eps);
        // L2/P2
        Assert.assertEquals(expectedL2P2,       data.get(2).getValue(),                eps);
        Assert.assertEquals(120 * Frequency.F0, data.get(2).getCombinedMHzFrequency(), eps);
    }

    @Test
    public void testRinex3GeometryFree() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getGeometryFreeCombination(system),
                     CombinationType.GEOMETRY_FREE, 2.187, 3821708.096, 0.0, Double.NaN, 2, 3);
    }

    @Test
    public void testRinex3IonoFree() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getIonosphereFreeCombination(system),
                     CombinationType.IONO_FREE, 22399214.1934, 134735627.3126, 0.0, 235 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3WideLane() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getWideLaneCombination(system),
                     CombinationType.WIDE_LANE, 22399239.8790, 179620369.2060, 0.0, 5 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3NarrowLane() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getNarrowLaneCombination(system),
                    CombinationType.NARROW_LANE, 22399188.5078, 89850885.4191, 0.0, 235 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3MelbourneWubbena() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getMelbourneWubbenaCombination(system),
                     CombinationType.MELBOURNE_WUBBENA, 0.0, 0.0, 157221180.6982, 5 * Frequency.F0, 1, 3);
    }

    @Test
    public void testRinex3PhaseMinusCode() {
        doTestRinex3SingleFrequency(MeasurementCombinationFactory.getPhaseMinusCodeCombination(system),
                                    CombinationType.PHASE_MINUS_CODE, 95309391.697, 69321899.401,
                                    69321893.420, 65500187.511);
    }

    @Test
    public void testRinex3GRAPHIC() {
        doTestRinex3SingleFrequency(MeasurementCombinationFactory.getGRAPHICCombination(system),
                                    CombinationType.GRAPHIC, 70053877.7315, 57060139.2905,
                                    57060136.2880, 55149281.1465);
    }

    private void doTestRinex3SingleFrequency(final MeasurementCombination combination, final CombinationType type,
                                             final double expected1C, final double expected2W,
                                             final double expected2X, final double expected5X) {
        // Perform combination on the observation data set depending the Rinex version
        final CombinedObservationDataSet combinedDataSet = combination.combine(dataSetRinex3);
        Assert.assertEquals(type.getName(), combination.getName());
        // Verify the combined observation data
        final List<CombinedObservationData> data = combinedDataSet.getObservationData();
        // L1C/C1C
        Assert.assertEquals(expected1C,         data.get(0).getValue(),                eps);
        Assert.assertEquals(154 * Frequency.F0, data.get(0).getCombinedMHzFrequency(), eps);
        // L2W/C2W
        Assert.assertEquals(expected2W,         data.get(1).getValue(),                eps);
        Assert.assertEquals(120 * Frequency.F0, data.get(1).getCombinedMHzFrequency(), eps);
        // L2X/C2X
        Assert.assertEquals(expected2X,         data.get(2).getValue(),                eps);
        Assert.assertEquals(120 * Frequency.F0, data.get(1).getCombinedMHzFrequency(), eps);
        // L5X/C5X
        Assert.assertEquals(expected5X,         data.get(3).getValue(),                eps);
        Assert.assertEquals(115 * Frequency.F0, data.get(3).getCombinedMHzFrequency(), eps);
    }

    /**
     * Test if Rinex formats can be used for the combination of measurements
     */
    private void doTestRinexDualFrequency(final MeasurementCombination combination, final CombinationType expectedType,
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
