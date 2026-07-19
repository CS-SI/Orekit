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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.GroundObserver;
import org.orekit.estimation.measurements.Observer;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** Base class modifying theoretical range measurements with tropospheric delay.
 * The effect of tropospheric correction on the range is directly computed
 * through the computation of the tropospheric delay.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Joris Olympio
 * @since 11.2
 */
public abstract class BaseRangeTroposphericDelayModifier {

    /** Tropospheric delay model. */
    private final TroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range measurement method.
     * @since 12.1
     */
    protected BaseRangeTroposphericDelayModifier(final TroposphericModel model) {
        tropoModel = model;
    }

    /** Get the name of the effect modifying the measurement.
     * @return name of the effect modifying the measurement
     * @since 13.0
     */
    public String getEffectName() {
        return "troposphere";
    }

    /** Get the tropospheric delay model.
     * @return tropospheric delay model
     */
    protected TroposphericModel getTropoModel() {
        return tropoModel;
    }

    /** Compute the measurement error due to Troposphere.
     * @param observer object that observes signal
     * @param state    estimated spacecraft state
     * @return the measurement error due to Troposphere
     */
    public double rangeErrorTroposphericModel(final Observer observer,
                                              final SpacecraftState state) {

        // Currently not calculating tropospheric delays for this type of observer
        if (observer instanceof GroundObserver groundObserver) {

            // spacecraft position and elevation as seen from the ground station
            final TrackingCoordinates trackingCoordinates = groundObserver.getTrackingCoordinates(state);

            // only consider measures above the horizon
            if (trackingCoordinates.getElevation() > 0) {
                // tropospheric delay in meters
                return tropoModel.
                        pathDelay(trackingCoordinates, groundObserver.getOffsetGeodeticPoint(state.getDate()),
                                tropoModel.getParameters(), state.getDate()).
                        getDelay();
            }

            return 0;
        } else {
            throw new OrekitException(OrekitMessages.WRONG_OBSERVER_TYPE);
        }
    }


    /** Compute the measurement error due to Troposphere.
     * @param <T> type of the element
     * @param observer   object that observes signal
     * @param state      estimated spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere
     */
    public <T extends CalculusFieldElement<T>> T rangeErrorTroposphericModel(final Observer observer,
                                                                             final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {

        // Currently not calculating tropospheric delays for this type of observer
        if (observer instanceof GroundObserver groundObserver) {

            // Field
            final Field<T> field = state.getDate().getField();
            final T zero = field.getZero();

            // spacecraft position and elevation as seen from the ground station
            final FieldTrackingCoordinates<T> trackingCoordinates = groundObserver.getTrackingCoordinates(state);

            // only consider measures above the horizon
            if (trackingCoordinates.getElevation().getReal() > 0) {
                // tropospheric delay in meters
                return tropoModel.
                        pathDelay(trackingCoordinates, groundObserver.getOffsetGeodeticPoint(state.getDate()),
                                parameters, state.getDate()).
                        getDelay();
            }

            return zero;
        } else {
            throw new OrekitException(OrekitMessages.WRONG_OBSERVER_TYPE);
        }
    }

    /** Get the drivers for this modifier parameters.
     * @return drivers for this modifier parameters
     */
    public List<ParameterDriver> getParametersDrivers() {
        return tropoModel.getParametersDrivers();
    }

}
