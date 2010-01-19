/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.propagation.BasicPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Implementation of the {@link OrekitStepInterpolator} interface based
 * on a {@link BasicPropagator}.
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class BasicStepInterpolator implements OrekitStepInterpolator {

    /** Serializable UID. */
    private static final long serialVersionUID = 3616520788183383307L;

    /** Underlying propagator. */
    private final BasicPropagator propagator;

    /** Previous date. */
    private AbsoluteDate previousDate;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Interpolated State. */
    private SpacecraftState interpolatedState;

    /** Forward propagation indicator. */
    private boolean forward;

    /** Build a new instance from a basic propagator.
     * @param propagator underlying propagator to use
     */
    public BasicStepInterpolator(final BasicPropagator propagator) {
        this.propagator  = propagator;
        previousDate     = AbsoluteDate.PAST_INFINITY;
        currentDate      = AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getCurrentDate() {
        return currentDate;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getInterpolatedDate() {
        return interpolatedState.getDate();
    }

    /** {@inheritDoc} */
    public SpacecraftState getInterpolatedState() throws OrekitException {
        return interpolatedState;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getPreviousDate() {
        return previousDate;
    }

    /** {@inheritDoc} */
    public boolean isForward() {
        return forward;
    }

    /** {@inheritDoc} */
    public void setInterpolatedDate(final AbsoluteDate date)
        throws PropagationException {
        interpolatedState = propagator.propagate(date);
    }

    /** Shift one step forward.
     * Copy the current date into the previous date, hence preparing the
     * interpolator for future calls to {@link #storeDate storeDate}
     */
    public void shift() {
        previousDate = currentDate;
    }

    /** Store the current step date.
     * @param date current date
     * @exception PropagationException if the state cannot be propagated at specified date
     */
    public void storeDate(final AbsoluteDate date)
        throws PropagationException {
        currentDate = date;
        forward     = currentDate.compareTo(previousDate) >= 0;
        setInterpolatedDate(currentDate);
    }

}
