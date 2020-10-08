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

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Range;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class RelativisticClockRangeModifierTest {

    @Test
    public void testRelativisticClockCorrection() {

        // Station
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final GeodeticPoint point    = new GeodeticPoint(FastMath.toRadians(42.0), FastMath.toRadians(1.0), 100.0);
        final TopocentricFrame topo  = new TopocentricFrame(earth, point, "");
        final GroundStation station  = new GroundStation(topo);

        // Satellite (GPS orbit from TLE)
        final TLE tle = new TLE("1 28474U 04045A   20252.59334296 -.00000043  00000-0  00000-0 0  9998",
                                "2 28474  55.0265  49.5108 0200271 267.9106 149.0797  2.00552216116165");
        final TimeStampedPVCoordinates satPV = TLEPropagator.selectExtrapolator(tle).getPVCoordinates(tle.getDate(), FramesFactory.getEME2000());
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(satPV, FramesFactory.getEME2000(), Constants.WGS84_EARTH_MU));

        // Set reference date to station drivers
        for (ParameterDriver driver : Arrays.asList(station.getClockOffsetDriver(),
                                                    station.getEastOffsetDriver(),
                                                    station.getNorthOffsetDriver(),
                                                    station.getZenithOffsetDriver(),
                                                    station.getPrimeMeridianOffsetDriver(),
                                                    station.getPrimeMeridianDriftDriver(),
                                                    station.getPolarOffsetXDriver(),
                                                    station.getPolarDriftXDriver(),
                                                    station.getPolarOffsetYDriver(),
                                                    station.getPolarDriftYDriver())) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(state.getDate());
            }
        }

        // Station PV
        final Vector3D zero = Vector3D.ZERO;
        final TimeStampedPVCoordinates stationPV = station.getOffsetToInertial(state.getFrame(), state.getDate()).transformPVCoordinates(new TimeStampedPVCoordinates(state.getDate(), zero, zero, zero));

        // Range measurement
        final Range range = new Range(station, false, state.getDate(), 26584264.45, 1.0, 1.0, new ObservableSatellite(0));
        final EstimatedMeasurement<Range> estimated = new EstimatedMeasurement<Range>(range, 0, 0,
                        new SpacecraftState[] {state},
                        new TimeStampedPVCoordinates[] {state.getPVCoordinates(), stationPV});
        estimated.setEstimatedValue(range.getObservedValue()[0]);
        Assert.assertEquals(0.0, estimated.getObservedValue()[0] - estimated.getEstimatedValue()[0], 1.0e-3);

        // Measurement modifier
        final RelativisticClockRangeModifier modifier = new RelativisticClockRangeModifier();
        modifier.modify(estimated);
        Assert.assertEquals(0, modifier.getParametersDrivers().size());

        // Verify
        Assert.assertEquals(6.87, estimated.getObservedValue()[0] - estimated.getEstimatedValue()[0], 1.0e-2);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
