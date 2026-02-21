/* Copyright 2025-2026 Hawkeye 360 (HE360)
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.estimation.measurements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.time.clocks.FieldClockOffset;
import org.orekit.time.clocks.QuadraticFieldClockModel;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

/** Abstract interface that contains those methods necessary
 *  for both space and ground-based satellite observers.
 *
 * @author Brianna Aubin
 * @since 14.0
 */
public interface Observer extends MeasurementParticipant {

    /** Get the type of object being used in measurement observations.
     * @return boolean
     */
    boolean isSpaceBased();

    /** Return the PVCoordinatesProvider.
     * @return pos/vel coordinates provider
     */
    PVCoordinatesProvider getPVCoordinatesProvider();

    /** Return the FieldPVCoordinatesProvider.
     * @param freeParameters number of estimated parameters
     * @param parameterIndices indices of the estimated parameters in derivatives computations, must be driver
     * @return pos/vel coordinates provider for values with Gradient field
     */
    FieldPVCoordinatesProvider<Gradient> getFieldPVCoordinatesProvider(int freeParameters,
                                                                       Map<String, Integer> parameterIndices);

    /** Get the transform between offset frame and inertial frame.
     * <p>
     * The offset frame takes the <em>current</em> position offset,
     * polar motion and the meridian shift into account. The frame
     * returned is disconnected from later changes in the parameters.
     * When the {@link ParameterDriver parameters} managing these
     * offsets are changed, the method must be called again to retrieve
     * a new offset frame.
     * </p>
     * @param inertial inertial frame to transform to
     * @param date date of the transform
     * @param clockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the ground station clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return transform between offset frame and inertial frame, at <em>real</em> measurement
     * date (i.e. with clock, Earth and station offsets applied)
     */
    Transform getOffsetToInertial(Frame inertial, AbsoluteDate date, boolean clockOffsetAlreadyApplied);

    /** Get the transform between offset frame and inertial frame with derivatives.
     * <p>
     * As the East and North vectors are not well defined at pole, the derivatives
     * of these two vectors diverge to infinity as we get closer to the pole.
     * So this method should not be used for stations less than 0.0001 degree from
     * either poles.
     * </p>
     * @param inertial inertial frame to transform to
     * @param clockDate date of the transform, clock offset and its derivatives already compensated
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the estimated parameters in derivatives computations, must be driver
     * span name in map, not driver name or will not give right results (see {@link ParameterDriver#getValue(int, Map)})
     * @return transform between offset frame and inertial frame, at specified date
     */
    default FieldTransform<Gradient> getOffsetToInertial(final Frame inertial,
                                                         final AbsoluteDate clockDate,
                                                         final int freeParameters,
                                                         final Map<String, Integer> indices) {
        // take clock offset into account
        final Gradient offset = getClockOffsetDriver().getValue(freeParameters, indices, clockDate);
        final FieldAbsoluteDate<Gradient> offsetCompensatedDate = new FieldAbsoluteDate<>(clockDate, offset.negate());

        return getOffsetToInertial(inertial, offsetCompensatedDate, freeParameters, indices);
    }

    /** Get the transform between offset frame and inertial frame with derivatives.
     * <p>
     * As the East and North vectors are not well defined at pole, the derivatives
     * of these two vectors diverge to infinity as we get closer to the pole.
     * So this method should not be used for stations less than 0.0001 degree from
     * either poles.
     * </p>
     * @param inertial inertial frame to transform to
     * @param offsetCompensatedDate date of the transform, clock offset and its derivatives already compensated
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the estimated parameters in derivatives computations, must be driver
     * span name in map, not driver name or will not give right results (see {@link ParameterDriver#getValue(int, Map)})
     * @return transform between offset frame and inertial frame, at specified date
     */
    FieldTransform<Gradient> getOffsetToInertial(Frame inertial, FieldAbsoluteDate<Gradient> offsetCompensatedDate,
                                                 int freeParameters, Map<String, Integer> indices);

    /** Create a map of the free parameter values.
     * @param states list of ObservableSatellite measurement states
     * @param parameterDrivers list of all parameter values for the measurement
     * @return map of the free parameter values
     */
    static Map<String, Integer> getParameterIndices(final SpacecraftState[] states,
                                                    final List<ParameterDriver> parameterDrivers) {

        // measurement derivatives are computed with respect to spacecraft state in inertial frame
        // Parameters:
        //  - 6k..6k+2 - Position of spacecraft k (counting k from 0 to nbSat-1) in inertial frame
        //  - 6k+3..6k+5 - Velocity of spacecraft k (counting k from 0 to nbSat-1) in inertial frame
        //  - 6nbSat..n - measurements parameters (clock offset, etc)
        int nbParams = 6 * states.length;
        final Map<String, Integer> paramIndices = new HashMap<>();
        for (ParameterDriver measurementDriver : parameterDrivers) {
            if (measurementDriver.isSelected()) {
                for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    paramIndices.put(span.getData(), nbParams++);
                }
            }
        }
        return paramIndices;
    }

    /**
     * Compute actual date taking into account clock offset.
     * @param date date as registered by observer
     * @return corrected date
     */
    default AbsoluteDate getCorrectedReceptionDate(final AbsoluteDate date) {
        final ClockOffset localClock = getQuadraticClockModel().getOffset(date);
        return date.shiftedBy(-localClock.getOffset());
    }

    /**
     * Compute actual date taking into account clock offset.
     * @param date date as registered by observer
     * @param nbParams number of independent variables for automatic differentiation
     * @param paramIndices mapping between parameter name and variable index
     * @return corrected date
     */
    default FieldAbsoluteDate<Gradient> getCorrectedReceptionDateField(final AbsoluteDate date,
                                                                       final int nbParams,
                                                                       final Map<String, Integer> paramIndices) {
        final QuadraticFieldClockModel<Gradient> quadraticClockModel = getQuadraticFieldClock(nbParams, date, paramIndices);
        final GradientField field = GradientField.getField(nbParams);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldClockOffset<Gradient> localClock = quadraticClockModel.getOffset(fieldDate);
        return fieldDate.shiftedBy(localClock.getOffset().negate());
    }

}
