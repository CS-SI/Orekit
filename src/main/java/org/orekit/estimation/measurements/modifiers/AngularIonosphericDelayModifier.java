/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.modifiers;

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.Angular;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.models.earth.IonosphericModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical angular measurement with ionospheric delay.
 * The effect of ionospheric correction on the angular measurement is computed
 * through the computation of the ionospheric delay. The spacecraft state
 * is shifted by the computed delay time and elevation and azimuth are computed
 * again with the new spacecraft state.
 *
 * The ionospheric delay depends on the frequency of the signal (GNSS, VLBI, ...).
 * For optical measurements (e.g. SLR), the ray is not affected by ionosphere charged particles.
 *
 * @author Thierry Ceolin
 * @since 8.0
 */
public class AngularIonosphericDelayModifier implements EstimationModifier<Angular> {
    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Constructor.
     *
     * @param model  Ionospheric delay model appropriate for the current angular measurement method.
     */
    public AngularIonosphericDelayModifier(final IonosphericModel model) {
        ionoModel = model;
    }

    /** Compute the measurement error due to ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to ionosphere
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double angularErrorIonosphericModel(final GroundStation station,
                                                final SpacecraftState state)
        throws OrekitException {

        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation in radians
        final double elevation = station.getBaseFrame().getElevation(position,
                                                                     state.getFrame(),
                                                                     state.getDate());
        // only consider measures above the horizon
        if (elevation > 0.0) {

            // compute azimuth
            final double azimuth = station.getBaseFrame().getAzimuth(position,
                                                                     state.getFrame(),
                                                                     state.getDate());
            // delay in meters
            final double delay = ionoModel.pathDelay(state.getDate(),
                                                     station.getBaseFrame().getPoint(),
                                                     elevation, azimuth);

            // Take into account one way measurement
            return delay;
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    @Override
    public void modify(final EstimatedMeasurement<Angular> estimated)
        throws OrekitException {
        final Angular         measure = estimated.getObservedMeasurement();
        final GroundStation   station = measure.getStation();
        final SpacecraftState state   = estimated.getState();

        final double delay = angularErrorIonosphericModel(station, state);
        // Delay is taken into account to shift the spacecraft position
        final double dt = delay / Constants.SPEED_OF_LIGHT;

        // Position of the spacecraft shifted of dt
        final SpacecraftState transitState = state.shiftedBy(-dt);

        // Update estimated value taking into account the ionospheric delay.
        final AbsoluteDate date     = transitState.getDate();
        final Vector3D     position = transitState.getPVCoordinates().getPosition();
        final Frame        inertial = transitState.getFrame();

        // elevation and azimuth in radians
        final double elevation = station.getBaseFrame().getElevation(position, inertial, date);
        final double azimuth   = station.getBaseFrame().getAzimuth(position, inertial, date);
        // azimuth - elevation values
        estimated.setEstimatedValue(azimuth, elevation);

    }
}
