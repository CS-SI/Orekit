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
package org.orekit.propagation.sampling;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This interface is a space-dynamics aware step interpolator.
 *
 * <p>It mirrors the <code>ODEStateInterpolator</code> interface from <a
 * href="https://hipparchus.org/">Hipparchus</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 */
public interface OrekitStepInterpolator {

    /**
     * Get the state at previous grid point date.
     * @return state at previous grid point date
     * @exception OrekitException if state cannot be retrieved
     */
    SpacecraftState getPreviousState() throws OrekitException;

    /**
     * Determines if the {@link #getPreviousState() previous state} is computed directly
     * by the integrator, or if it is calculated using {@link #getInterpolatedState(AbsoluteDate)
     * interpolation}.
     *
     * <p> Typically the previous state is directly computed by the integrator, but when
     * events are detected the steps are shortened so that events occur on step boundaries
     * which means the previous state may be computed by the interpolator.
     *
     * @return {@code true} if the previous state was calculated by the interpolator and
     * false if it was computed directly by the integrator.
     */
    boolean isPreviousStateInterpolated();

    /**
     * Get the state at previous grid point date.
     * @return state at previous grid point date
     * @exception OrekitException if state cannot be retrieved
     */
    SpacecraftState getCurrentState() throws OrekitException;

    /**
     * Determines if the {@link #getCurrentState() current state} is computed directly by
     * the integrator, or if it is calculated using {@link #getInterpolatedState(AbsoluteDate)
     * interpolation}.
     *
     * <p> Typically the current state is directly computed by the integrator, but when
     * events are detected the steps are shortened so that events occur on step boundaries
     * which means the current state may be computed by the interpolator.
     *
     * @return {@code true} if the current state was calculated by the interpolator and
     * false if it was computed directly by the integrator.
     */
    boolean isCurrentStateInterpolated();

    /** Get the state at interpolated date.
     * @param date date of the interpolated state
     * @return state at interpolated date
     * @exception OrekitException if underlying interpolator cannot handle
     * the date
     */
    SpacecraftState getInterpolatedState(AbsoluteDate date)
        throws OrekitException;

    /** Check is integration direction is forward in date.
     * @return true if integration is forward in date
     */
    boolean isForward();

}
