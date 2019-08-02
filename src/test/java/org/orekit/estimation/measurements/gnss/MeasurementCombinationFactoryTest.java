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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.CombinedObservationData;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;

public class MeasurementCombinationFactoryTest {

    private ObservationData obs1;

    private ObservationData obs2;

    private SatelliteSystem system;

    @Before
    public void setUp() {
        // First observation data
        obs1 = new ObservationData(ObservationType.L1, 2.25E7, 0, 0);
        // Second observation data
        obs2 = new ObservationData(ObservationType.L5, 2.26E7, 0, 0);
        // Satellite system
        system = SatelliteSystem.GPS;
    }

    @Test
    public void testGeometryFree() {
        final GeometryFreeCombination combination = MeasurementCombinationFactory.getGeometryFreeCombination(system);
        final CombinedObservationData combinedData = combination.combine(obs1, obs2);
        Assert.assertEquals(0.01E7, combinedData.getValue(), 0.001E7);
        Assert.assertEquals(CombinationType.GEOMETRY_FREE, combinedData.getCombinationType());
        Assert.assertEquals(MeasurementType.CARRIER_PHASE, combinedData.getMeasurementType());
        Assert.assertEquals(Double.NaN, combinedData.getCombinedMHzFrequency(), Double.MIN_VALUE);
    }

    @Test
    public void testIonoFree() {
        final IonosphereFreeCombination combination = MeasurementCombinationFactory.getIonosphereFreeCombination(system);
        final CombinedObservationData   combinedData = combination.combine(obs1, obs2);
        Assert.assertEquals(2.237E7, combinedData.getValue(), 0.001E7);
        Assert.assertEquals(CombinationType.IONO_FREE, combinedData.getCombinationType());
        Assert.assertEquals(MeasurementType.CARRIER_PHASE, combinedData.getMeasurementType());
        Assert.assertEquals((10491 * Frequency.F0), combinedData.getCombinedMHzFrequency(), Double.MIN_VALUE);
    }

    @Test
    public void testWideLane() {
        final WideLaneCombination combination      = MeasurementCombinationFactory.getWideLaneCombination(system);
        final CombinedObservationData combinedData = combination.combine(obs1, obs2);
        Assert.assertEquals(2.221E7, combinedData.getValue(), 0.001E7);
        Assert.assertEquals(CombinationType.WIDE_LANE, combinedData.getCombinationType());
        Assert.assertEquals(MeasurementType.CARRIER_PHASE, combinedData.getMeasurementType());
        Assert.assertEquals((39 * Frequency.F0), combinedData.getCombinedMHzFrequency(), Double.MIN_VALUE);
    }

    @Test
    public void testNarrowLane() {
        final NarrowLaneCombination combination    = MeasurementCombinationFactory.getNarrowLaneCombination(system);
        final CombinedObservationData combinedData = combination.combine(obs1, obs2);
        Assert.assertEquals(2.254E7, combinedData.getValue(), 0.001E7);
        Assert.assertEquals(CombinationType.NARROW_LANE, combinedData.getCombinationType());
        Assert.assertEquals(MeasurementType.CARRIER_PHASE, combinedData.getMeasurementType());
        Assert.assertEquals((269 * Frequency.F0), combinedData.getCombinedMHzFrequency(), Double.MIN_VALUE);
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

}
