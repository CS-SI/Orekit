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
package org.orekit.estimation.measurements.gnss;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.files.rinex.observation.RinexObservationParser;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.utils.Constants;

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

    @BeforeEach
    public void setUp() throws NoSuchAlgorithmException, IOException {
        Utils.setDataRoot("gnss");
        RinexObservationParser parser = new RinexObservationParser();

        // Observation data
        obs1 = new ObservationData(ObservationType.L1, 2.25E7, 0, 0);

        // RINEX 2 Observation data set
        final String name2 = "rinex/truncate-sbch0440.16o";
        List<ObservationDataSet> parsed2 = parser.parse(new DataSource(name2,
                                                                       () -> Utils.class.getClassLoader().getResourceAsStream(name2))).
                                           getObservationDataSets();
        dataSetRinex2 = parsed2.get(0);

        // RINEX 3 Observation data set
        final String name3 = "rinex/aaaa0000.00o";
        List<ObservationDataSet> parsed3 = parser.parse(new DataSource(name3,
                                                                       () -> Utils.class.getClassLoader().getResourceAsStream(name3))).
                                           getObservationDataSets();
        dataSetRinex3 = parsed3.get(1);

        // Satellite system
        system = dataSetRinex2.getSatellite().getSystem();
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
        final ObservationDataSet emptyDataSet = new ObservationDataSet(new SatInSystem(dataSetRinex2.getSatellite().getSystem(),
                                                                                       dataSetRinex2.getSatellite()
                                                                                        .getPRN()),
                                                                       dataSetRinex2.getDate(), 0, dataSetRinex2.getRcvrClkOffset(),
                                                                       new ArrayList<ObservationData>());
        // Test first method signature
        final CombinedObservationDataSet combinedData = combination.combine(emptyDataSet);
        Assertions.assertEquals(0, combinedData.getObservationData().size());
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS, oe.getSpecifier());
        }

        // Test INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS exception
        try {
            final ObservationData observation = new ObservationData(ObservationType.L1, 12345678.0, 0, 0);
            combination.combine(obs1, observation);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS, oe.getSpecifier());
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS, oe.getSpecifier());
        }

        // Test INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS exception
        try {
            final ObservationData observation = new ObservationData(ObservationType.D2, 12345678.0, 0, 0);
            combination.combine(obs1, observation);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS, oe.getSpecifier());
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
                     CombinationType.IONO_FREE, 23732467.5026, 3772223175.669, 0.0, 4658 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2WideLane() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getWideLaneCombination(system),
                     CombinationType.WIDE_LANE, 23732453.7100, 27534453.519, 0.0, 34 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2NarrowLane() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getNarrowLaneCombination(system),
                     CombinationType.NARROW_LANE, 23732481.2951, 221895659.955, 0.0, 274 * Frequency.F0, 2, 2);
    }

    @Test
    public void testRinex2MelbourneWubbena() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getMelbourneWubbenaCombination(system),
                     CombinationType.MELBOURNE_WUBBENA, 0.0, 0.0, 3801972.2239, 34 * Frequency.F0, 1, 2);
    }

    @Test
    public void testRinex2PhaseMinusCode() {
        doTestRinex2SingleFrequency(MeasurementCombinationFactory.getPhaseMinusCodeCombination(system),
                                    CombinationType.PHASE_MINUS_CODE, 73448118.300);
    }

    @Test
    public void testRinex2GRAPHIC() {
        doTestRinex2SingleFrequency(MeasurementCombinationFactory.getGRAPHICCombination(system),
                                    CombinationType.GRAPHIC, 60456544.068);
    }

    private void doTestRinex2SingleFrequency(final MeasurementCombination combination, final CombinationType type,
                                             final double expectedL2P2) {
        // Perform combination on the observation data set depending the Rinex version
        final CombinedObservationDataSet combinedDataSet = combination.combine(dataSetRinex2);
        checkCombinedDataSet(combinedDataSet, 1);
        Assertions.assertEquals(type.getName(), combination.getName());
        // Verify the combined observation data
        final List<CombinedObservationData> data = combinedDataSet.getObservationData();
        // L2/P2
        Assertions.assertEquals(expectedL2P2,       data.get(0).getValue(),                eps);
        Assertions.assertEquals(120 * Frequency.F0, data.get(0).getCombinedMHzFrequency(), eps);
    }

    @Test
    public void testRinex3GeometryFree() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getGeometryFreeCombination(system),
                     CombinationType.GEOMETRY_FREE, 2.187, 3821708.096, 0.0, Double.NaN, 2, 3);
    }

    @Test
    public void testRinex3IonoFree() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getIonosphereFreeCombination(system),
                     CombinationType.IONO_FREE, 22399214.1934, 179620369.206, 0.0, 235 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3WideLane() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getWideLaneCombination(system),
                     CombinationType.WIDE_LANE, 22399239.8790, 3821708.096, 0.0, 5 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3NarrowLane() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getNarrowLaneCombination(system),
                    CombinationType.NARROW_LANE, 22399188.5078, 179620457.900, 0.0, 235 * Frequency.F0, 2, 3);
    }

    @Test
    public void testRinex3MelbourneWubbena() {
        doTestRinexDualFrequency(MeasurementCombinationFactory.getMelbourneWubbenaCombination(system),
                     CombinationType.MELBOURNE_WUBBENA, 0.0, 0.0, -18577480.4117, 5 * Frequency.F0, 1, 3);
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
        Assertions.assertEquals(type.getName(), combination.getName());
        // Verify the combined observation data
        final List<CombinedObservationData> data = combinedDataSet.getObservationData();
        // L1C/C1C
        Assertions.assertEquals(expected1C,         data.get(0).getValue(),                eps);
        Assertions.assertEquals(154 * Frequency.F0, data.get(0).getCombinedMHzFrequency(), eps);
        // L2W/C2W
        Assertions.assertEquals(expected2W,         data.get(1).getValue(),                eps);
        Assertions.assertEquals(120 * Frequency.F0, data.get(1).getCombinedMHzFrequency(), eps);
        // L2X/C2X
        Assertions.assertEquals(expected2X,         data.get(2).getValue(),                eps);
        Assertions.assertEquals(120 * Frequency.F0, data.get(1).getCombinedMHzFrequency(), eps);
        // L5X/C5X
        Assertions.assertEquals(expected5X,         data.get(3).getValue(),                eps);
        Assertions.assertEquals(115 * Frequency.F0, data.get(3).getCombinedMHzFrequency(), eps);
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
            Assertions.assertEquals(expectedSize, combinedDataSet.getObservationData().size());
        }

        Assertions.assertEquals(expectedType.getName(), combination.getName());

        // Verify the combined observation data
        for (CombinedObservationData cod : combinedDataSet.getObservationData()) {

            if (cod.getMeasurementType() == MeasurementType.CARRIER_PHASE) {

                Assertions.assertEquals(expectedPhaseValue, cod.getValue(),                eps);
                Assertions.assertEquals(expectedFrequency,  cod.getCombinedMHzFrequency(), eps);
                Assertions.assertEquals(expectedType,       cod.getCombinationType());

            } else if (cod.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                Assertions.assertEquals(expectedRangeValue, cod.getValue(),                eps);
                Assertions.assertEquals(expectedFrequency,  cod.getCombinedMHzFrequency(), eps);
                Assertions.assertEquals(expectedType,       cod.getCombinationType());

            } else if (cod.getMeasurementType() == MeasurementType.COMBINED_RANGE_PHASE) {

                Assertions.assertEquals(expectedRangePhase, cod.getValue(),                eps);
                Assertions.assertEquals(expectedFrequency,  cod.getCombinedMHzFrequency(), eps);
                Assertions.assertEquals(expectedType,       cod.getCombinationType());

            }

        }
    }

    private void checkCombinedDataSet(final CombinedObservationDataSet combinedDataSet,
                                      final int expectedSize) {
        // Verify the number of combined data set
        Assertions.assertEquals(expectedSize, combinedDataSet.getObservationData().size());
        // Verify satellite data
        Assertions.assertEquals(30, combinedDataSet.getPrnNumber());
        Assertions.assertEquals(SatelliteSystem.GPS, combinedDataSet.getSatelliteSystem());
        // Verify receiver clock
        Assertions.assertEquals(0.0, combinedDataSet.getRcvrClkOffset(), eps);
        // Verify date
        Assertions.assertEquals("2016-02-13T00:49:43.000Z", combinedDataSet.getDate().toString());
    }

    @Test
    public void testIssue746() {

        // This test uses the example provided by Amir Allahvirdi-Zadeh in the Orekit issue tracker
        // Source of the values: https://gitlab.orekit.org/orekit/orekit/-/issues/746

        // Build the observation data
        final ObservationData obs1 = new ObservationData(ObservationType.L1, 1.17452520667E8, 0, 0);
        final ObservationData obs2 = new ObservationData(ObservationType.L2, 9.1521434853E7, 0, 0);

        // Ionosphere-free measurement
        final IonosphereFreeCombination ionoFree = MeasurementCombinationFactory.getIonosphereFreeCombination(SatelliteSystem.GPS);
        final CombinedObservationData   combined = ionoFree.combine(obs1, obs2);

        // Combine data
        final double wavelength         = Constants.SPEED_OF_LIGHT / (combined.getCombinedMHzFrequency() * 1.0e6);
        final double combineValueMeters = combined.getValue() * wavelength;

        // Verify
        Assertions.assertEquals(22350475.245, combineValueMeters, 0.001);

    }

}
