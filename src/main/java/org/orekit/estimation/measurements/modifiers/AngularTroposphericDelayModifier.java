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

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.EvaluationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.Angular;
import org.orekit.frames.Frame;
import org.orekit.models.earth.TroposphericModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

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
 * @since 7.1
 */
public class AngularTroposphericDelayModifier implements EvaluationModifier<Angular> {
    /** Tropospheric delay model. */
    private final TroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current angular measurement method.
     */
    public AngularTroposphericDelayModifier(final TroposphericModel model) {
        tropoModel = model;
    }

    /** Get the station height above mean sea level.
     *
     * @param station  ground station (or measuring station)
     * @return the measuring station height above sea level, m
     */
    private double getStationHeightAMSL(final GroundStation station) {
        // FIXME Il faut la hauteur par rapport au geoide WGS84+GUND = EGM2008 par exemple
        final double height = station.getBaseFrame().getPoint().getAltitude();
        return height;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double angularErrorTroposphericModel(final GroundStation station,
                                                 final SpacecraftState state) throws OrekitException
    {
        //
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation
        final double elevation = station.getBaseFrame().getElevation(position,
                                                                     state.getFrame(),
                                                                     state.getDate());

        // only consider measures above the horizon
        if (elevation > 0.0) {
            // altitude AMSL in meters
            final double height = getStationHeightAMSL(station);

            // delay in meters
            final double delay = tropoModel.pathDelay(elevation, height);

            // one-way measurement.
            return delay;
        }

        return 0;
    }

    @Override
    public List<Parameter> getSupportedParameters() {
        return null;
    }

    @Override
    public void modify(final Evaluation<Angular> evaluation)
        throws OrekitException {
        final Angular         measure = evaluation.getMeasurement();
        final GroundStation   station = measure.getStation();
        final SpacecraftState state   = evaluation.getState();

        final double delay = angularErrorTroposphericModel(station, state);
        // Delay is taken into account to shift the spacecraft position
        final double dt = delay / Constants.SPEED_OF_LIGHT;

        // Position of the spacecraft shifted of dt
        final SpacecraftState transitState = state.shiftedBy(-dt);

        // Update measurement value taking into account the ionospheric delay.
        final AbsoluteDate date      = transitState.getDate();
        final Vector3D     position  = transitState.getPVCoordinates().getPosition();
        final Frame        inertial  = transitState.getFrame();

        // elevation and azimuth in radians
        final double elevation = station.getBaseFrame().getElevation(position, inertial, date);
        final double azimuth   = station.getBaseFrame().getAzimuth(position, inertial, date);

        // update measurement value taking into account the tropospheric delay.
        // azimuth - elevation values
        evaluation.setValue(azimuth, elevation);
    }
}
