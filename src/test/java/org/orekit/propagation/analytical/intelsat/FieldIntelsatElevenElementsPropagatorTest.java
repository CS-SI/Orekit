/* Copyright 2002-2024 Airbus Defence and Space
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Airbus Defence and Space licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.analytical.intelsat;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FieldIntelsatElevenElementsPropagatorTest {

    private static FieldIntelsatElevenElements<Binary64> ELEMENTS;

    @Test
    void testCannotResetIntermediateState() {
        FieldIntelsatElevenElementsPropagator<Binary64> propagator = new FieldIntelsatElevenElementsPropagator<>(ELEMENTS);
        try {
            propagator.resetIntermediateState(null, false);
        }
        catch (OrekitException oe) {
            assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    @Test
    void testCannotResetInitialState() {
        FieldIntelsatElevenElementsPropagator<Binary64> propagator = new FieldIntelsatElevenElementsPropagator<>(ELEMENTS);
        try {
            propagator.resetInitialState(null);
        }
        catch (OrekitException oe) {
            assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
    }

    @Test
    void testPropagation() {
        // Reference: Intelsat calculator for spacecraft 4521 (used 2023/12/07)
        // https://www.intelsat.com/resources/tools/
        FieldIntelsatElevenElementsPropagator<Binary64> propagator = new FieldIntelsatElevenElementsPropagator<>(ELEMENTS);
        double referenceLongitude170Hours = 301.9191;
        double referenceLatitude170Hours = 0.0257;
        double tolerance = 0.0001;
        propagator.propagateInEcef(ELEMENTS.getEpoch().shiftedBy(170 * 3600.0));
        assertNotNull(propagator.getIntelsatElevenElements());
        assertEquals(referenceLongitude170Hours, propagator.getEastLongitudeDegrees().getValue().getReal(), tolerance);
        assertEquals(referenceLatitude170Hours, propagator.getGeocentricLatitudeDegrees().getValue().getReal(), tolerance);
    }

    @Test
    void testOrbitElementsAtT0() {
        // Reference use of the Intelsat's 11 elements propagator developed in STK
        FieldIntelsatElevenElementsPropagator<Binary64> propagator = new FieldIntelsatElevenElementsPropagator<>(ELEMENTS, FramesFactory.getTOD(IERSConventions.IERS_2010, false),
                                                                                                                 FramesFactory.getITRF(IERSConventions.IERS_2010, false));
        KeplerianOrbit orbit = ((FieldKeplerianOrbit<Binary64>) OrbitType.KEPLERIAN.convertType(
                propagator.propagateOrbit(ELEMENTS.getEpoch(), propagator.getParameters(ELEMENTS.getEpoch().getField())))).toOrbit();
        assertNotNull(propagator.getIntelsatElevenElements());
        assertEquals(302.0355, propagator.getEastLongitudeDegrees().getValue().getReal(), 0.0001);
        assertEquals(0.0378, propagator.getGeocentricLatitudeDegrees().getValue().getReal(), 0.0001);
        assertEquals(-1.529465e-6, propagator.getEastLongitudeDegrees().getFirstDerivative().getReal(), 1.0e-12);
        assertEquals(-1.01044e-7, propagator.getGeocentricLatitudeDegrees().getFirstDerivative().getReal(), 1.0e-12);
        assertEquals(42172456.005, propagator.getOrbitRadius().getValue().getReal(), 1.0e-3);
        assertEquals(0.797, propagator.getOrbitRadius().getFirstDerivative().getReal(), 1.0e-3);
        assertEquals(42166413.453, orbit.getA(), 4.0e-2);
        assertEquals(0.000296, orbit.getE(), 1.0e-6);
        assertEquals(0.037825, FastMath.toDegrees(orbit.getI()), 1.0e-6);
        assertEquals(282.488, FastMath.toDegrees(MathUtils.normalizeAngle(orbit.getRightAscensionOfAscendingNode(), FastMath.PI)), 4.0e-3);
        assertEquals(333.151, FastMath.toDegrees(MathUtils.normalizeAngle(orbit.getPerigeeArgument(), FastMath.PI)), 4.0e-3);
        assertEquals(118.919, FastMath.toDegrees(MathUtils.normalizeAngle(orbit.getAnomaly(PositionAngleType.MEAN), FastMath.PI)), 1.0e-3);
    }

    @BeforeAll
    static void initialize() {
        Binary64 zero = new Binary64(0.0);
        // Reference elements from Intelsat website (spacecraft 4521)
        ELEMENTS = new FieldIntelsatElevenElements<>(new FieldAbsoluteDate<>(zero.getField(), "2023-12-04T00:00:00.000", TimeScalesFactory.getUTC()), zero.add(302.0058),
                                                     zero.add(-0.0096), zero.add(-0.000629), zero.add(0.0297), zero.add(-0.0004), zero.add(-0.0194), zero.add(0.0007),
                                                     zero.add(0.0378), zero.add(-0.0018), zero.add(-0.0011), zero.add(0.0015));
    }
}
