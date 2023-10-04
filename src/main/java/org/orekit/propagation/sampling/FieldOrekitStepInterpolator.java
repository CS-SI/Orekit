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
package org.orekit.propagation.sampling;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** This interface is a space-dynamics aware step interpolator.
 *
 * <p>It mirrors the <code>StepInterpolator</code> interface from <a
 * href="http://commons.apache.org/math/"> commons-math</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 */
public interface FieldOrekitStepInterpolator<T extends CalculusFieldElement<T>> extends FieldPVCoordinatesProvider<T> {

    /**
     * Get the state at previous grid point date.
     * @return state at previous grid point date
     */
    FieldSpacecraftState<T> getPreviousState();

    /**
     * Get the state at previous grid point date.
     * @return state at previous grid point date
     */
    FieldSpacecraftState<T> getCurrentState();

    /** Get the state at interpolated date.
     * @param date date of the interpolated state
     * @return state at interpolated date
          * the date
     */
    FieldSpacecraftState<T> getInterpolatedState(FieldAbsoluteDate<T> date);

    /** Check is integration direction is forward in date.
     * @return true if integration is forward in date
     */
    boolean isForward();

    /** Create a new restricted version of the instance.
     * <p>
     * The instance is not changed at all.
     * </p>
     * @param newPreviousState start of the restricted step
     * @param newCurrentState end of the restricted step
     * @return restricted version of the instance
     * @see #getPreviousState()
     * @see #getCurrentState()
     * @since 11.0
     */
    FieldOrekitStepInterpolator<T> restrictStep(FieldSpacecraftState<T> newPreviousState, FieldSpacecraftState<T> newCurrentState);

    /** {@inheritDoc}
     * @since 12.0
     */
    @Override
    default TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame) {
        return getInterpolatedState(date).getPVCoordinates(frame);
    }

}
