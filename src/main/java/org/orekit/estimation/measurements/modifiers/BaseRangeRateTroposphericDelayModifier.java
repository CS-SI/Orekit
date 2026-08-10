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
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.GroundObserver;
import org.orekit.estimation.measurements.Observer;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** Baselass modifying theoretical range-rate measurements with tropospheric delay.
 * The effect of tropospheric correction on the range-rate is directly computed
 * through the computation of the tropospheric delay difference with respect to
 * time.
 * <p>
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 * </p>
 *
 * @author Joris Olympio
 * @since 11.2
 */
public abstract class BaseRangeRateTroposphericDelayModifier {

    /** Tropospheric delay model. */
    private final TroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range-rate measurement method.
     * @since 12.1
     */
    protected BaseRangeRateTroposphericDelayModifier(final TroposphericModel model) {
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
    public double rangeRateErrorTroposphericModel(final Observer observer,
                                                  final SpacecraftState state) {
        // The effect of tropospheric correction on the range rate is computed using finite differences.

        // Currently not calculating tropospheric delays for this type of observer
        if (observer instanceof GroundObserver groundObserver) {
            final double dt = 10; // s

            // tracking
            final TrackingCoordinates trackingCoordinates1 = groundObserver.getTrackingCoordinates(state);

            // only consider measures above the horizon
            if (trackingCoordinates1.getElevation() > 0) {
                // tropospheric delay in meters
                final AbsoluteDate date = state.getDate();
                final GeodeticPoint point = groundObserver.getOffsetGeodeticPoint(date);
                final double d1 = tropoModel.pathDelay(trackingCoordinates1, point,
                        tropoModel.getParameters(date), date).getDelay();

                // propagate spacecraft state forward by dt
                final SpacecraftState state2 = state.shiftedBy(dt);

                // tracking
                final TrackingCoordinates trackingCoordinates2 = groundObserver.getTrackingCoordinates(state2);

                // tropospheric delay dt after
                final double d2 = tropoModel.pathDelay(trackingCoordinates2, point,
                                tropoModel.getParameters(state2.getDate()), state2.getDate()).
                        getDelay();

                return (d2 - d1) / dt;
            }

            return 0;
        } else {
            throw new OrekitException(OrekitMessages.WRONG_OBSERVER_TYPE);
        }
    }


    /** Compute the measurement error due to Troposphere.
     * @param <T>        type of the element
     * @param observer   object that observes signal
     * @param state      estimated spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere
     */
    public <T extends CalculusFieldElement<T>> T rangeRateErrorTroposphericModel(final Observer observer,
                                                                                 final FieldSpacecraftState<T> state,
                                                                                 final T[] parameters) {

        // Check to make sure Observer is NOT space-based
        if (observer instanceof GroundObserver groundObserver) {

            // Field
            final Field<T> field = state.getDate().getField();
            final T zero = field.getZero();

            // The effect of tropospheric correction on the range rate is
            // computed using finite differences.

            final double dt = 10; // s

            // spacecraft position and elevation as seen from the ground station
            final FieldTrackingCoordinates<T> trackingCoordinates1 = groundObserver.getTrackingCoordinates(state);

            // only consider measures above the horizon
            if (trackingCoordinates1.getElevation().getReal() > 0) {
                // tropospheric delay in meters
                final FieldAbsoluteDate<T> date = state.getDate();
                final FieldGeodeticPoint<T> gp1 = groundObserver.getOffsetGeodeticPoint(date);
                final T d1 = tropoModel.pathDelay(trackingCoordinates1, gp1, parameters, date).getDelay();

                // propagate spacecraft state forward by dt
                final FieldSpacecraftState<T> state2 = state.shiftedBy(dt);

                // elevation
                final FieldTrackingCoordinates<T> trackingCoordinates2 = groundObserver.getTrackingCoordinates(state2);

                // tropospheric delay dt after
                final T d2 = tropoModel.pathDelay(trackingCoordinates2, gp1, parameters, state2.getDate()).getDelay();

                return d2.subtract(d1).divide(dt);
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
