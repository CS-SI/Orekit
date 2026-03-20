/* Copyright 2002-2026 CS GROUP
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
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.Observer;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.GeodeticExtendedPositionProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** Class modifying theoretical angular measurement with ionospheric delay.
 * <p>
 * The effect of ionospheric correction on the angular measurement is computed
 * through the computation of the ionospheric delay. The spacecraft state
 * is shifted by the computed delay time and elevation and azimuth are computed
 * again with the new spacecraft state.
 * </p>
 * <p>
 * The ionospheric delay depends on the frequency of the signal (GNSS, VLBI, ...).
 * For optical measurements (e.g. SLR), the ray is not affected by ionosphere charged particles.
 * </p>
 * <p>
 * Since 10.0, state derivatives and ionospheric parameters derivatives are computed
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
    public AngularIonosphericDelayModifier(final IonosphericModel model, final double freq) {
        ionoModel = model;
        frequency = freq;
    }

    /** {@inheritDoc} */
    @Override
    public String getEffectName() {
        return "ionosphere";
    }

    /** Compute the measurement error due to ionosphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to ionosphere
     */
    private double angularErrorIonosphericModel(final Observer station,
                                                final SpacecraftState state) {
        // Base frame associated with the station
        final PVCoordinatesProvider coordsProvider = station.getPVCoordinatesProvider();
        // delay in meters
        return ionoModel.pathDelay(state, coordsProvider, frequency, ionoModel.getParameters(state.getDate()));
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return ionoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<AngularAzEl> estimated) {
        final AngularAzEl     measurement = estimated.getObservedMeasurement();
        final GroundStation   station = measurement.getStation();
        final SpacecraftState state   = estimated.getStates()[0];
        final double[] azimuthElevation = computeAzimuthElevation(state, station, measurement);

        // Delay is taken into account to shift the spacecraft position
        final double delay = angularErrorIonosphericModel(station, state);
        final double dt = delay / Constants.SPEED_OF_LIGHT;
        final SpacecraftState transitState = state.shiftedBy(-dt);

        // recompute angles and use difference as increment
        final double[] azimuthElevationWithShift = computeAzimuthElevation(transitState, station, measurement);
        final double[] value = estimated.getEstimatedValue();
        estimated.modifyEstimatedValue(this, value[0] + (azimuthElevationWithShift[0] - azimuthElevation[0]),
                value[1] + (azimuthElevationWithShift[1] - azimuthElevation[1]));
    }

    /**
     * Compute azimuth and elevation angles in radians.
     * @param transitState state at signal emission
     * @param station ground station
     * @param measurement measurement object
     * @return azimuth and elevation array [rad]
     */
    private double[] computeAzimuthElevation(final SpacecraftState transitState, final GroundStation station,
                                             final AngularAzEl measurement) {
        // Take into account station position error in topocentric frame
        final BodyShape bodyShape = station.getBaseFrame().getParentShape();
        final Frame bodyFrame = bodyShape.getBodyFrame();
        final Vector3D stationPosition = station.getPVCoordinatesProvider().getPosition(transitState.getDate(), bodyFrame);
        final GeodeticPoint geodeticPoint = new GeodeticExtendedPositionProvider(bodyShape, stationPosition).getGeodeticPoint();
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, geodeticPoint, "station");

        // Elevation and azimuth in radians
        final Vector3D     position = transitState.getPosition();
        final TrackingCoordinates tc = topocentricFrame.getTrackingCoordinates(position, transitState.getFrame(), measurement.getDate());
        final double twoPiWrap   = MathUtils.normalizeAngle(tc.getAzimuth(), measurement.getObservedValue()[0]) - tc.getAzimuth();
        final double azimuth     = tc.getAzimuth() + twoPiWrap;
        return new double[] { azimuth, tc.getElevation() };
    }
}
