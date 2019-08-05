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

    @Before
    public void setUp() {
        Utils.setDataRoot("gnss");

        // Observation data
        obs1 = new ObservationData(ObservationType.L1, 2.25E7, 0, 0);
        // Satellite system
        system = SatelliteSystem.GPS;

        // Observation data set
        RinexLoader loader = load("rinex/jnu10110.17o");
        dataSetRinex2 = loader.getObservationDataSets().get(0);
    }

    @Test
    public void testEmptyDataSetGeometryFree() {
        // Initialize combination of measurements
        final GeometryFreeCombination combination = MeasurementCombinationFactory.getGeometryFreeCombination(system);

        // Build empty observation data set
        final ObservationDataSet emptyDataSet = new ObservationDataSet(dataSetRinex2.getHeader(), dataSetRinex2.getSatelliteSystem(),
                                                                       dataSetRinex2.getPrnNumber(), dataSetRinex2.getDate(), dataSetRinex2.getRcvrClkOffset(),
                                                                       new ArrayList<ObservationData>());
        // Test first method signature
        final CombinedObservationDataSet combinedData = combination.combine(emptyDataSet);
        Assert.assertEquals(0, combinedData.getObservationData().size());
    }

    @Test
    public void testRinex2GeometryFree() {
        // Initialize combination of measurements
        final GeometryFreeCombination combination = MeasurementCombinationFactory.getGeometryFreeCombination(system);

        // Perform combination on the observation data set
        final CombinedObservationDataSet combinedDataSet = combination.combine(dataSetRinex2);
        checkCombinedDataSet(combinedDataSet);

        // Verify the combined observation data
        for (CombinedObservationData cod : combinedDataSet.getObservationData()) {

            if (cod.getMeasurementType() == MeasurementType.CARRIER_PHASE) {

                Assert.assertEquals(27477897.451,                  cod.getValue(),                eps);
                Assert.assertEquals(Double.NaN,                    cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(CombinationType.GEOMETRY_FREE, cod.getCombinationType());

            } else if (cod.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                Assert.assertEquals(2.732,                         cod.getValue(),                eps);
                Assert.assertEquals(Double.NaN,                    cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(CombinationType.GEOMETRY_FREE, cod.getCombinationType());

            }

        }
    }

    @Test
    public void testExceptionsGeometryFree() {
        final GeometryFreeCombination combination = MeasurementCombinationFactory.getGeometryFreeCombination(system);

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
    public void testEmptyDataSetIonoFree() {
        // Initialize combination of measurements
        final IonosphereFreeCombination combination = MeasurementCombinationFactory.getIonosphereFreeCombination(system);

        // Build empty observation data set
        final ObservationDataSet emptyDataSet = new ObservationDataSet(dataSetRinex2.getHeader(), dataSetRinex2.getSatelliteSystem(),
                                                                       dataSetRinex2.getPrnNumber(), dataSetRinex2.getDate(), dataSetRinex2.getRcvrClkOffset(),
                                                                       new ArrayList<ObservationData>());
        // Test first method signature
        final CombinedObservationDataSet combinedData = combination.combine(emptyDataSet);
        Assert.assertEquals(0, combinedData.getObservationData().size());
    }

    @Test
    public void testRinex2IonoFree() {
        // Initialize combination of measurements
        final IonosphereFreeCombination combination = MeasurementCombinationFactory.getIonosphereFreeCombination(system);

        // Perform combination on the observation data set
        final CombinedObservationDataSet combinedDataSet = combination.combine(dataSetRinex2);
        checkCombinedDataSet(combinedDataSet);

        // Verify the combined observation data
        for (CombinedObservationData cod : combinedDataSet.getObservationData()) {

            if (cod.getMeasurementType() == MeasurementType.CARRIER_PHASE) {

                Assert.assertEquals(166932002.3165,            cod.getValue(),                eps);
                Assert.assertEquals(9316 * Frequency.F0,       cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(CombinationType.IONO_FREE, cod.getCombinationType());

            } else if (cod.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                Assert.assertEquals(23683687.8991,             cod.getValue(),                eps);
                Assert.assertEquals(9316 * Frequency.F0,       cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(CombinationType.IONO_FREE, cod.getCombinationType());

            }

        }
    }

    @Test
    public void testExceptionsIonoFree() {
        final IonosphereFreeCombination combination = MeasurementCombinationFactory.getIonosphereFreeCombination(system);

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
    public void testEmptyDataSetWideLane() {
        // Initialize combination of measurements
        final WideLaneCombination combination = MeasurementCombinationFactory.getWideLaneCombination(system);

        // Build empty observation data set
        final ObservationDataSet emptyDataSet = new ObservationDataSet(dataSetRinex2.getHeader(), dataSetRinex2.getSatelliteSystem(),
                                                                       dataSetRinex2.getPrnNumber(), dataSetRinex2.getDate(), dataSetRinex2.getRcvrClkOffset(),
                                                                       new ArrayList<ObservationData>());
        // Test first method signature
        final CombinedObservationDataSet combinedData = combination.combine(emptyDataSet);
        Assert.assertEquals(0, combinedData.getObservationData().size());
    }

    @Test
    public void testRinex2WideLane() {
        // Initialize combination of measurements
        final WideLaneCombination combination = MeasurementCombinationFactory.getWideLaneCombination(system);

        // Perform combination on the observation data set
        final CombinedObservationDataSet combinedDataSet = combination.combine(dataSetRinex2);
        checkCombinedDataSet(combinedDataSet);

        // Verify the combined observation data
        for (CombinedObservationData cod : combinedDataSet.getObservationData()) {

            if (cod.getMeasurementType() == MeasurementType.CARRIER_PHASE) {

                Assert.assertEquals(221439467.4189,            cod.getValue(),                eps);
                Assert.assertEquals(34 * Frequency.F0,         cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(CombinationType.WIDE_LANE, cod.getCombinationType());

            } else if (cod.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                Assert.assertEquals(23683682.4796,             cod.getValue(),                eps);
                Assert.assertEquals(34 * Frequency.F0,         cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(CombinationType.WIDE_LANE, cod.getCombinationType());

            }

        }
    }

    @Test
    public void testExceptionsWideLane() {
        final WideLaneCombination combination = MeasurementCombinationFactory.getWideLaneCombination(system);

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
    public void testEmptyDataSetNarrowLane() {
        // Initialize combination of measurements
        final NarrowLaneCombination combination = MeasurementCombinationFactory.getNarrowLaneCombination(system);

        // Build empty observation data set
        final ObservationDataSet emptyDataSet = new ObservationDataSet(dataSetRinex2.getHeader(), dataSetRinex2.getSatelliteSystem(),
                                                                       dataSetRinex2.getPrnNumber(), dataSetRinex2.getDate(), dataSetRinex2.getRcvrClkOffset(),
                                                                       new ArrayList<ObservationData>());
        // Test first method signature
        final CombinedObservationDataSet combinedData = combination.combine(emptyDataSet);
        Assert.assertEquals(0, combinedData.getObservationData().size());
    }

    @Test
    public void testRinex2NarrowLane() {
        // Initialize combination of measurements
        final NarrowLaneCombination combination = MeasurementCombinationFactory.getNarrowLaneCombination(system);

        // Perform combination on the observation data set
        final CombinedObservationDataSet combinedDataSet = combination.combine(dataSetRinex2);
        checkCombinedDataSet(combinedDataSet);

        // Verify the combined observation data
        for (CombinedObservationData cod : combinedDataSet.getObservationData()) {

            if (cod.getMeasurementType() == MeasurementType.CARRIER_PHASE) {

                Assert.assertEquals(112424537.2140,              cod.getValue(),                eps);
                Assert.assertEquals(274 * Frequency.F0,          cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(CombinationType.NARROW_LANE, cod.getCombinationType());

            } else if (cod.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                Assert.assertEquals(23683693.3185,               cod.getValue(),                eps);
                Assert.assertEquals(274 * Frequency.F0,          cod.getCombinedMHzFrequency(), eps);
                Assert.assertEquals(CombinationType.NARROW_LANE, cod.getCombinationType());

            }

        }
    }

    @Test
    public void testExceptionsNarrowLane() {
        final NarrowLaneCombination combination = MeasurementCombinationFactory.getNarrowLaneCombination(system);

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

    private void checkCombinedDataSet(final CombinedObservationDataSet combinedDataSet) {
        // Verify the number of combined data set
        Assert.assertEquals(2, combinedDataSet.getObservationData().size());
        // Verify RINEX Header
        final RinexHeader header = combinedDataSet.getHeader();
        Assert.assertEquals(2.11, header.getRinexVersion(), eps);
        // Verify satellite data
        Assert.assertEquals(2, combinedDataSet.getPrnNumber());
        Assert.assertEquals(SatelliteSystem.GPS, combinedDataSet.getSatelliteSystem());
        // Verify receiver clock
        Assert.assertEquals(-0.03, combinedDataSet.getRcvrClkOffset(), eps);
        // Verify date
        Assert.assertEquals("2017-01-10T23:59:43.000", combinedDataSet.getDate().toString());
    }

    private RinexLoader load(final String name) {
        return new RinexLoader(Utils.class.getClassLoader().getResourceAsStream(name), name);
    }

}
