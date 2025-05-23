/* Copyright 2002-2025 CS GROUP
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

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical angular measurement with tropospheric radio refractive index.
 * A radio ray passing through the lower (non-ionized) layer of the atmosphere undergoes bending
 * caused by the gradient of the relative index. Since the refractive index varies mainly with
 * altitude, only the vertical gradient of the refractive index is considered here.
 * The effect of tropospheric correction on the angular measurement is computed directly
 * through the computation of the apparent elevation angle.
 * Recommendation ITU-R P.453-11 (07/2015) and Recommendation ITU-R P.834-7 (10/2015)
 *
 * @author Thierry Ceolin
 * @since 8.0
 */
public class AngularRadioRefractionModifier implements EstimationModifier<AngularAzEl> {

    /** Tropospheric refraction model. */
    private final AtmosphericRefractionModel atmosModel;

    /** Constructor.
    *
    * @param model  tropospheric refraction model appropriate for the current angular measurement method.
    */
    public AngularRadioRefractionModifier(final AtmosphericRefractionModel model) {
        atmosModel = model;
    }

    /** {@inheritDoc} */
    @Override
    public String getEffectName() {
        return "refraction";
    }

    /** Compute the measurement error due to troposphere refraction.
    * @param station station
    * @param state spacecraft state
    * @return the measurement error due to troposphere
    */
    private double angularErrorRadioRefractionModel(final GroundStation station,
                                                    final SpacecraftState state) {

        final Vector3D position = state.getPosition();

        // elevation in radians
        final double elevation =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate()).
                        getElevation();

        // angle correction (rad)
        return atmosModel.getRefraction(elevation);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<AngularAzEl> estimated) {
        final AngularAzEl     measure = estimated.getObservedMeasurement();
        final GroundStation   station = measure.getStation();
        final SpacecraftState state   = estimated.getStates()[0];
        final double correction = angularErrorRadioRefractionModel(station, state);

        // update estimated value taking into account the tropospheric elevation correction.
        // The tropospheric elevation correction is directly added to the elevation.
        final double[] oldValue = estimated.getEstimatedValue();
        final double[] newValue = oldValue.clone();

        // consider only effect on elevation
        newValue[1] = newValue[1] + correction;
        estimated.modifyEstimatedValue(this, newValue[0], newValue[1]);
    }

}
