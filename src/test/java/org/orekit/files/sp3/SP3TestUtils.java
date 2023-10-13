/* Copyright 2002-2023 Luc Maisonobe
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
import org.junit.jupiter.api.Assertions;

public class SP3TestUtils {

    public static void checkEquals(final SP3 original, final SP3 rebuilt) {
        checkEquals(original.getHeader(), rebuilt.getHeader());
        Assertions.assertEquals(original.getSatelliteCount(), rebuilt.getSatelliteCount());
        for (int i = 0; i < original.getSatelliteCount(); ++i) {
            checkEquals(original.getEphemeris(i), rebuilt.getEphemeris(i));
        }
    }

    public static void checkEquals(final SP3Header original, final SP3Header rebuilt) {
        Assertions.assertEquals(original.getVersion(),         rebuilt.getVersion());
        Assertions.assertEquals(original.getFilter(),          rebuilt.getFilter());
        Assertions.assertEquals(original.getType(),            rebuilt.getType());
        Assertions.assertEquals(original.getTimeSystem(),      rebuilt.getTimeSystem());
        Assertions.assertEquals(original.getDataUsed().size(), rebuilt.getDataUsed().size());
        for (int i = 0 ; i < original.getDataUsed().size(); ++i) {
            Assertions.assertEquals(original.getDataUsed().get(i), rebuilt.getDataUsed().get(i));
        }
        Assertions.assertEquals(original.getEpoch(),             rebuilt.getEpoch());
        Assertions.assertEquals(original.getGpsWeek(),           rebuilt.getGpsWeek());
        Assertions.assertEquals(original.getSecondsOfWeek(),     rebuilt.getSecondsOfWeek(), 1.0e-15);
        Assertions.assertEquals(original.getModifiedJulianDay(), rebuilt.getModifiedJulianDay());
        Assertions.assertEquals(original.getDayFraction(),       rebuilt.getDayFraction(), 1.0e-15);
        Assertions.assertEquals(original.getEpochInterval(),     rebuilt.getEpochInterval(), 1.0e-15);
        Assertions.assertEquals(original.getNumberOfEpochs(),    rebuilt.getNumberOfEpochs());
        Assertions.assertEquals(original.getCoordinateSystem(),  rebuilt.getCoordinateSystem());
        Assertions.assertEquals(original.getOrbitType(),         rebuilt.getOrbitType());
        Assertions.assertEquals(original.getOrbitTypeKey(),      rebuilt.getOrbitTypeKey());
        Assertions.assertEquals(original.getAgency(),            rebuilt.getAgency());
        Assertions.assertEquals(original.getPosVelBase(),        rebuilt.getPosVelBase(), 1.0e-15);
        Assertions.assertEquals(original.getClockBase(),         rebuilt.getClockBase(),  1.0e-15);
        Assertions.assertEquals(original.getSatIds().size(), rebuilt.getSatIds().size());
        for (int i = 0; i < original.getSatIds().size(); ++i) {
            Assertions.assertEquals(original.getSatIds().get(i), rebuilt.getSatIds().get(i));
            Assertions.assertEquals(original.getAccuracy(original.getSatIds().get(i)), rebuilt.getAccuracy(rebuilt.getSatIds().get(i)), 1.0e-15);
        }
        Assertions.assertEquals(original.getComments().size(), rebuilt.getComments().size());
        for (int i = 0; i < original.getComments().size(); ++i) {
            Assertions.assertEquals(original.getComments().get(i), rebuilt.getComments().get(i));
        }
    }

    public static void checkEquals(final SP3Ephemeris original, final SP3Ephemeris rebuilt) {
        Assertions.assertEquals(original.getId(), rebuilt.getId());
        Assertions.assertEquals(original.getMu(), rebuilt.getMu(), 0.001);
        Assertions.assertEquals(original.getSegments().size(), rebuilt.getSegments().size());
        for (int i = 0; i < original.getSegments().size(); ++i) {
            checkEquals(original.getSegments().get(i), rebuilt.getSegments().get(i));
        }
        Assertions.assertEquals(original.getStart(),                rebuilt.getStart());
        Assertions.assertEquals(original.getStop(),                 rebuilt.getStop());
        Assertions.assertEquals(original.getFrame().getName(),      rebuilt.getFrame().getName());
        Assertions.assertEquals(original.getInterpolationSamples(), rebuilt.getInterpolationSamples());
        Assertions.assertEquals(original.getAvailableDerivatives(), rebuilt.getAvailableDerivatives());
    }

    public static void checkEquals(final SP3Segment original, final SP3Segment rebuilt) {
        Assertions.assertEquals(original.getMu(),                   rebuilt.getMu(), 0.001);
        Assertions.assertEquals(original.getStart(),                rebuilt.getStart());
        Assertions.assertEquals(original.getStop(),                 rebuilt.getStop());
        Assertions.assertEquals(original.getFrame().getName(),      rebuilt.getFrame().getName());
        Assertions.assertEquals(original.getInterpolationSamples(), rebuilt.getInterpolationSamples());
        Assertions.assertEquals(original.getAvailableDerivatives(), rebuilt.getAvailableDerivatives());
        Assertions.assertEquals(original.getCoordinates().size(),   rebuilt.getCoordinates().size());
        for (int i = 0; i < original.getCoordinates().size(); ++i) {
            checkEquals(original.getCoordinates().get(i), rebuilt.getCoordinates().get(i));
        }
    }

    public static void checkEquals(final SP3Coordinate original, final SP3Coordinate rebuilt) {
        Assertions.assertEquals(original.getDate(),                rebuilt.getDate());
        Assertions.assertEquals(0.0, Vector3D.distance(original.getPosition(), rebuilt.getPosition()), 1.0e-10);
        Assertions.assertEquals(0.0, Vector3D.distance(original.getVelocity(), rebuilt.getVelocity()), 1.0e-10);
        Assertions.assertEquals(original.getClockCorrection(),     rebuilt.getClockCorrection(), 1.0e-15);
        Assertions.assertEquals(original.getClockRateChange(),     rebuilt.getClockRateChange(), 1.0e-15);
        if (original.getPositionAccuracy() == null) {
            Assertions.assertNull(rebuilt.getPositionAccuracy());
        } else {
            Assertions.assertEquals(0.0, Vector3D.distance(original.getPositionAccuracy(), rebuilt.getPositionAccuracy()), 1.0e-10);
        }
        if (original.getVelocityAccuracy() == null) {
            Assertions.assertNull(rebuilt.getVelocityAccuracy());
        } else {
            Assertions.assertEquals(0.0, Vector3D.distance(original.getVelocityAccuracy(), rebuilt.getVelocityAccuracy()), 1.0e-10);
        }
        if (Double.isNaN(original.getClockAccuracy())) {
            Assertions.assertTrue(Double.isNaN(rebuilt.getClockAccuracy()));
        } else {
            Assertions.assertEquals(original.getClockAccuracy(), rebuilt.getClockAccuracy(), 1.0e-15);
        }
        if (Double.isNaN(original.getClockRateAccuracy())) {
            Assertions.assertTrue(Double.isNaN(rebuilt.getClockRateAccuracy()));
        } else {
            Assertions.assertEquals(original.getClockRateAccuracy(), rebuilt.getClockRateAccuracy(), 1.0e-15);
        }
        Assertions.assertEquals(original.hasClockEvent(),         rebuilt.hasClockEvent());
        Assertions.assertEquals(original.hasClockPrediction(),    rebuilt.hasClockPrediction());
        Assertions.assertEquals(original.hasOrbitManeuverEvent(), rebuilt.hasOrbitManeuverEvent());
        Assertions.assertEquals(original.hasOrbitPrediction(),    rebuilt.hasOrbitPrediction());
    }

}
