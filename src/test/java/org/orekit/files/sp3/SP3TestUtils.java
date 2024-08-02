/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.files.sp3;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SP3TestUtils {

    public static void checkEquals(final SP3 original, final SP3 rebuilt) {
        checkEquals(original.getHeader(), rebuilt.getHeader());
        assertEquals(original.getSatelliteCount(), rebuilt.getSatelliteCount());
        for (int i = 0; i < original.getSatelliteCount(); ++i) {
            checkEquals(original.getEphemeris(i), rebuilt.getEphemeris(i));
        }
    }

    public static void checkEquals(final SP3Header original, final SP3Header rebuilt) {
        assertEquals(original.getVersion(),         rebuilt.getVersion());
        assertEquals(original.getFilter(),          rebuilt.getFilter());
        assertEquals(original.getType(),            rebuilt.getType());
        assertEquals(original.getTimeSystem(),      rebuilt.getTimeSystem());
        assertEquals(original.getDataUsed().size(), rebuilt.getDataUsed().size());
        for (int i = 0 ; i < original.getDataUsed().size(); ++i) {
            assertEquals(original.getDataUsed().get(i), rebuilt.getDataUsed().get(i));
        }
        assertEquals(original.getEpoch(),             rebuilt.getEpoch());
        assertEquals(original.getGpsWeek(),           rebuilt.getGpsWeek());
        assertEquals(original.getSecondsOfWeek(),     rebuilt.getSecondsOfWeek(), 1.0e-15);
        assertEquals(original.getModifiedJulianDay(), rebuilt.getModifiedJulianDay());
        assertEquals(original.getDayFraction(),       rebuilt.getDayFraction(), 1.0e-15);
        assertEquals(original.getEpochInterval(),     rebuilt.getEpochInterval(), 1.0e-15);
        assertEquals(original.getNumberOfEpochs(),    rebuilt.getNumberOfEpochs());
        assertEquals(original.getCoordinateSystem(),  rebuilt.getCoordinateSystem());
        assertEquals(original.getOrbitType(),         rebuilt.getOrbitType());
        assertEquals(original.getOrbitTypeKey(),      rebuilt.getOrbitTypeKey());
        assertEquals(original.getAgency(),            rebuilt.getAgency());
        assertEquals(original.getPosVelBase(),        rebuilt.getPosVelBase(), 1.0e-15);
        assertEquals(original.getClockBase(),         rebuilt.getClockBase(),  1.0e-15);
        assertEquals(original.getSatIds().size(), rebuilt.getSatIds().size());
        for (int i = 0; i < original.getSatIds().size(); ++i) {
            assertEquals(original.getSatIds().get(i), rebuilt.getSatIds().get(i));
            assertEquals(original.getAccuracy(original.getSatIds().get(i)), rebuilt.getAccuracy(rebuilt.getSatIds().get(i)), 1.0e-15);
        }
        assertEquals(original.getComments().size(), rebuilt.getComments().size());
        for (int i = 0; i < original.getComments().size(); ++i) {
            assertEquals(original.getComments().get(i), rebuilt.getComments().get(i));
        }
    }

    public static void checkEquals(final SP3Ephemeris original, final SP3Ephemeris rebuilt) {
        assertEquals(original.getId(), rebuilt.getId());
        assertEquals(original.getMu(), rebuilt.getMu(), 0.001);
        assertEquals(original.getSegments().size(), rebuilt.getSegments().size());
        for (int i = 0; i < original.getSegments().size(); ++i) {
            checkEquals(original.getSegments().get(i), rebuilt.getSegments().get(i));
        }
        assertEquals(original.getStart(),                rebuilt.getStart());
        assertEquals(original.getStop(),                 rebuilt.getStop());
        assertEquals(original.getFrame().getName(),      rebuilt.getFrame().getName());
        assertEquals(original.getInterpolationSamples(), rebuilt.getInterpolationSamples());
        assertEquals(original.getAvailableDerivatives(), rebuilt.getAvailableDerivatives());
    }

    public static void checkEquals(final SP3Segment original, final SP3Segment rebuilt) {
        assertEquals(original.getMu(),                   rebuilt.getMu(), 0.001);
        assertEquals(original.getStart(),                rebuilt.getStart());
        assertEquals(original.getStop(),                 rebuilt.getStop());
        assertEquals(original.getFrame().getName(),      rebuilt.getFrame().getName());
        assertEquals(original.getInterpolationSamples(), rebuilt.getInterpolationSamples());
        assertEquals(original.getAvailableDerivatives(), rebuilt.getAvailableDerivatives());
        assertEquals(original.getCoordinates().size(),   rebuilt.getCoordinates().size());
        for (int i = 0; i < original.getCoordinates().size(); ++i) {
            checkEquals(original.getCoordinates().get(i), rebuilt.getCoordinates().get(i));
        }
    }

    public static void checkEquals(final SP3Coordinate original, final SP3Coordinate rebuilt) {
        assertEquals(original.getDate(),                rebuilt.getDate());
        assertEquals(0.0, Vector3D.distance(original.getPosition(), rebuilt.getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.distance(original.getVelocity(), rebuilt.getVelocity()), 1.0e-10);
        assertEquals(original.getClockCorrection(),     rebuilt.getClockCorrection(), 1.0e-15);
        assertEquals(original.getClockRateChange(),     rebuilt.getClockRateChange(), 1.0e-15);
        if (original.getPositionAccuracy() == null) {
            assertNull(rebuilt.getPositionAccuracy());
        } else {
            assertEquals(0.0, Vector3D.distance(original.getPositionAccuracy(), rebuilt.getPositionAccuracy()), 1.0e-10);
        }
        if (original.getVelocityAccuracy() == null) {
            assertNull(rebuilt.getVelocityAccuracy());
        } else {
            assertEquals(0.0, Vector3D.distance(original.getVelocityAccuracy(), rebuilt.getVelocityAccuracy()), 1.0e-10);
        }
        if (Double.isNaN(original.getClockAccuracy())) {
            assertTrue(Double.isNaN(rebuilt.getClockAccuracy()));
        } else {
            assertEquals(original.getClockAccuracy(), rebuilt.getClockAccuracy(), 1.0e-15);
        }
        if (Double.isNaN(original.getClockRateAccuracy())) {
            assertTrue(Double.isNaN(rebuilt.getClockRateAccuracy()));
        } else {
            assertEquals(original.getClockRateAccuracy(), rebuilt.getClockRateAccuracy(), 1.0e-15);
        }
        assertEquals(original.hasClockEvent(),         rebuilt.hasClockEvent());
        assertEquals(original.hasClockPrediction(),    rebuilt.hasClockPrediction());
        assertEquals(original.hasOrbitManeuverEvent(), rebuilt.hasOrbitManeuverEvent());
        assertEquals(original.hasOrbitPrediction(),    rebuilt.hasOrbitPrediction());
    }

}
