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
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** Class modifying theoretical angular measurement with ionospheric delay.
 * The effect of ionospheric correction on the angular measurement is computed
 * through the computation of the ionospheric delay. The spacecraft state
 * is shifted by the computed delay time and elevation and azimuth are computed
 * again with the new spacecraft state.
 *
 * The ionospheric delay depends on the frequency of the signal (GNSS, VLBI, ...).
 * For optical measurements (e.g. SLR), the ray is not affected by ionosphere charged particles.
 * <p>
 * Since 10.0, state derivatives and ionospheric parameters derivates are computed
 * using automatic differentiation.
 * </p>
 * @author Thierry Ceolin
 * @since 8.0
 */
public class AngularIonosphericDelayModifier implements EstimationModifier<AngularAzEl> {

    /** Ionospheric delay model. */
    private final IonosphericModel ionoModel;

    /** Frequency [Hz]. */
    private final double frequency;

    /** Constructor.
     *
     * @param model  Ionospheric delay model appropriate for the current angular measurement method.
     * @param freq frequency of the signal in Hz
     */
    public AngularIonosphericDelayModifier(final IonosphericModel model,
                                           final double freq) {
        ionoModel = model;
        frequency = freq;
    }

    /** Compute the measurement error due to ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to ionosphere
     */
    private double angularErrorIonosphericModel(final GroundStation station,
                                                final SpacecraftState state) {
        // Base frame associated with the station
        final TopocentricFrame baseFrame = station.getBaseFrame();
        // delay in meters
        final double delay = ionoModel.pathDelay(state, baseFrame, frequency, ionoModel.getParameters(state.getDate()));
        return delay;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<AngularAzEl> estimated) {
        final AngularAzEl     measure = estimated.getObservedMeasurement();
        final GroundStation   station = measure.getStation();
        final SpacecraftState state   = estimated.getStates()[0];

        final double delay = angularErrorIonosphericModel(station, state);
        // Delay is taken into account to shift the spacecraft position
        final double dt = delay / Constants.SPEED_OF_LIGHT;

        // Position of the spacecraft shifted of dt
        final SpacecraftState transitState = state.shiftedBy(-dt);

        // Update estimated value taking into account the ionospheric delay.
        final AbsoluteDate date     = transitState.getDate();
        final Vector3D     position = transitState.getPosition();
        final Frame        inertial = transitState.getFrame();

        // Elevation and azimuth in radians
        final TrackingCoordinates tc = station.getBaseFrame().getTrackingCoordinates(position, inertial, date);
        final double twoPiWrap   = MathUtils.normalizeAngle(tc.getAzimuth(), measure.getObservedValue()[0]) - tc.getAzimuth();
        final double azimuth     = tc.getAzimuth() + twoPiWrap;

        // Update estimated value taking into account the ionospheric delay.
        // Azimuth - elevation values
        estimated.setEstimatedValue(azimuth, tc.getElevation());
    }

}
