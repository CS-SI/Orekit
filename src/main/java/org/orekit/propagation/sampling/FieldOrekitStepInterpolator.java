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

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** This interface is a space-dynamics aware step interpolator.
 *
 * <p>It mirrors the <code>StepInterpolator</code> interface from <a
 * href="http://commons.apache.org/math/"> commons-math</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 */
public interface FieldOrekitStepInterpolator<T extends RealFieldElement<T>> {

    /**
     * Get the state at previous grid point date.
     * @return state at previous grid point date
     * @exception OrekitException if state cannot be retrieved
     */
    FieldSpacecraftState<T> getPreviousState() throws OrekitException;

    /**
     * Get the state at previous grid point date.
     * @return state at previous grid point date
     * @exception OrekitException if state cannot be retrieved
     */
    FieldSpacecraftState<T> getCurrentState() throws OrekitException;

    /** Get the state at interpolated date.
     * @param date date of the interpolated state
     * @return state at interpolated date
     * @exception OrekitException if underlying interpolator cannot handle
     * the date
     */
    FieldSpacecraftState<T> getInterpolatedState(FieldAbsoluteDate<T> date)
        throws OrekitException;

    /** Check is integration direction is forward in date.
     * @return true if integration is forward in date
     */
    boolean isForward();

}
