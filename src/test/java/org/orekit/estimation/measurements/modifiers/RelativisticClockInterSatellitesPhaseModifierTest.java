/* Copyright 2002-2020 CS GROUP
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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.gnss.InterSatellitesPhase;
import org.orekit.gnss.Frequency;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class RelativisticClockInterSatellitesPhaseModifierTest {

    /** Date. */
    private static AbsoluteDate date;

    /** Spacecraft states. */
    private static SpacecraftState[] states;

    @Test
    public void testRelativisticClockCorrection() {

        // Measurement
        final double wavelength = Frequency.G01.getWavelength();
        final InterSatellitesPhase phase = new InterSatellitesPhase(new ObservableSatellite(0), new ObservableSatellite(1),
                                                                    date,
                                                                    Vector3D.distance(states[0].getPVCoordinates().getPosition(),
                                                                                      states[1].getPVCoordinates().getPosition()) / wavelength,
                                                                    wavelength, 1.0, 1.0);

        // Inter-satellites phase before applying the modifier
        final EstimatedMeasurement<InterSatellitesPhase> estimatedBefore = phase.estimate(0, 0, states);

        // Inter-satellites phase after applying the modifier
        final EstimationModifier<InterSatellitesPhase> modifier = new RelativisticClockInterSatellitesPhaseModifier();
        phase.addModifier(modifier);
        final EstimatedMeasurement<InterSatellitesPhase> estimatedAfter = phase.estimate(0, 0, states);

        // Verify
        Assert.assertEquals(10.57, (estimatedBefore.getEstimatedValue()[0] - estimatedAfter.getEstimatedValue()[0]) * wavelength, 1.0e-2);
        Assert.assertEquals(0, modifier.getParametersDrivers().size());

    }

    @Before
    public void setUp() {
        // Data root
        Utils.setDataRoot("regular-data");

        // Date
        date = new AbsoluteDate("2004-01-13T00:00:00.000", TimeScalesFactory.getUTC());

        // Spacecraft states
        states = new SpacecraftState[2];
        final TLE local = new TLE("1 27642U 03002A   04013.91734903  .00000108  00000-0  12227-4 0  3621",
                                  "2 27642  93.9970   6.8623 0003169  80.1383 280.0205 14.90871424 54508");
        final TLE remote = new TLE("1 20061U 89044A   04013.44391333  .00000095  00000-0  10000-3 0  3242",
                                   "2 20061  53.4233 172.2072 0234017 261.4179  95.8975  2.00577231106949");
        states[0] = TLEPropagator.selectExtrapolator(local).propagate(date);
        states[1] = TLEPropagator.selectExtrapolator(remote).propagate(date);
    }

}
