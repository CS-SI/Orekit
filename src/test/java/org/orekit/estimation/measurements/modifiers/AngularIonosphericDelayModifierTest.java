/* Copyright 2022-2026 Romain Serra
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.GeodeticExtendedPositionProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AngularIonosphericDelayModifierTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testGetParameters() {
        // GIVEN
        final IonosphericModel model = mock();
        final List<ParameterDriver> expectedDrivers = new ArrayList<>();
        expectedDrivers.add(mock(ParameterDriver.class));
        when(model.getParametersDrivers()).thenReturn(expectedDrivers);
        final AngularIonosphericDelayModifier modifier = new AngularIonosphericDelayModifier(model, 1.);
        // WHEN
        final List<ParameterDriver> drivers = modifier.getParametersDrivers();
        // THEN
        assertEquals(expectedDrivers, drivers);
    }

    @Test
    void testNoDelayModifyWithoutDerivatives() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0.,
                FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(0., 0., 100.);
        final GroundStation station = mock();
        when(station.getBaseFrame()).thenReturn(new TopocentricFrame(ellipsoid, point, ""));
        when(station.getPVCoordinatesProvider()).thenReturn(new GeodeticExtendedPositionProvider(ellipsoid, point));
        final AngularAzEl angularAzEl = new AngularAzEl(station, AbsoluteDate.ARBITRARY_EPOCH, new double[2], new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularAzEl> estimated = new EstimatedMeasurementBase<>(angularAzEl, 0, 0,
                new SpacecraftState[] {state}, new TimeStampedPVCoordinates[]{state.getPVCoordinates()});
        final double azimuth = 2.;
        final double elevation = 1.;
        estimated.setEstimatedValue(azimuth, elevation);
        final IonosphericModel model = mock();
        when(model.pathDelay(any(SpacecraftState.class), any(PVCoordinatesProvider.class), any(Double.class), any())).thenReturn(0.);
        final AngularIonosphericDelayModifier modifier = new AngularIonosphericDelayModifier(model, 1.);
        // WHEN
        modifier.modifyWithoutDerivatives(estimated);
        // THEN
        assertEquals(azimuth, estimated.getEstimatedValue()[0]);
        assertEquals(elevation, estimated.getEstimatedValue()[1]);
    }
}
