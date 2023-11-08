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
package org.orekit.estimation.measurements.modifiers;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** Class modifying theoretical angular measurement with tropospheric delay.
 * The effect of tropospheric correction on the angular is computed
 * through the computation of the tropospheric delay.The spacecraft state
 * is shifted by the computed delay time and elevation and azimuth are computed
 * again with the new spacecraft state.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Thierry Ceolin
 * @since 8.0
 */
public class AngularTroposphericDelayModifier implements EstimationModifier<AngularAzEl> {

    /** Tropospheric delay model. */
    private final DiscreteTroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current angular measurement method.
     */
    public AngularTroposphericDelayModifier(final DiscreteTroposphericModel model) {
        tropoModel = model;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     */
    private double angularErrorTroposphericModel(final GroundStation station,
                                                 final SpacecraftState state) {
        //
        final Vector3D position = state.getPosition();

        // elevation
        final double elevation =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate()).
                        getElevation();

        // only consider measures above the horizon
        if (elevation > 0.0) {
            // delay in meters
            final double delay = tropoModel.pathDelay(elevation, station.getBaseFrame().getPoint(), tropoModel.getParameters(state.getDate()), state.getDate());

            // one-way measurement.
            return delay;
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return tropoModel.getParametersDrivers();
    }

    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<AngularAzEl> estimated) {
        final AngularAzEl     measure = estimated.getObservedMeasurement();
        final GroundStation   station = measure.getStation();
        final SpacecraftState state   = estimated.getStates()[0];

        final double delay = angularErrorTroposphericModel(station, state);
        // Delay is taken into account to shift the spacecraft position
        final double dt = delay / Constants.SPEED_OF_LIGHT;

        // Position of the spacecraft shifted of dt
        final SpacecraftState transitState = state.shiftedBy(-dt);

        // Update measurement value taking into account the ionospheric delay.
        final AbsoluteDate date      = transitState.getDate();
        final Vector3D     position  = transitState.getPosition();
        final Frame        inertial  = transitState.getFrame();

        // Elevation and azimuth in radians
        final TrackingCoordinates tc = station.getBaseFrame().getTrackingCoordinates(position, inertial, date);
        final double twoPiWrap   = MathUtils.normalizeAngle(tc.getAzimuth(), measure.getObservedValue()[0]) - tc.getAzimuth();
        final double azimuth     = tc.getAzimuth() + twoPiWrap;

        // Update estimated value taking into account the tropospheric delay.
        // Azimuth - elevation values
        estimated.setEstimatedValue(azimuth, tc.getElevation());
    }

}
