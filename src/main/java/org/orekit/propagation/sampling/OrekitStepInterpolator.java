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

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This interface is a space-dynamics aware step interpolator.
 *
 * <p>It mirrors the <code>StepInterpolator</code> interface from <a
 * href="http://commons.apache.org/math/"> commons-math</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public interface OrekitStepInterpolator extends Serializable {

    /** Get the current grid date.
     * @return current grid date
     */
    AbsoluteDate getCurrentDate();

    /** Get the previous grid date.
     * @return previous grid date
     */
    AbsoluteDate getPreviousDate();

    /** Get the interpolated date.
     * <p>If {@link #setInterpolatedDate(AbsoluteDate) setInterpolatedDate}
     * has not been called, the date returned is the same as  {@link
     * #getCurrentDate() getCurrentDate}.</p>
     * @return interpolated date
     * @see #setInterpolatedDate(AbsoluteDate)
     * @see #getInterpolatedState()
     */
    AbsoluteDate getInterpolatedDate();

    /** Set the interpolated date.
     * <p>It is possible to set the interpolation date outside of the current
     * step range, but accuracy will decrease as date is farther.</p>
     * @param date interpolated date to set
     * @exception PropagationException if underlying interpolator cannot handle
     * the date
     * @see #getInterpolatedDate()
     * @see #getInterpolatedState()
     */
    void setInterpolatedDate(final AbsoluteDate date)
        throws PropagationException;

    /** Get the interpolated state.
     * @return interpolated state at the current interpolation date
     * @exception OrekitException if state cannot be interpolated or converted
     * @see #getInterpolatedDate()
     * @see #setInterpolatedDate(AbsoluteDate)
     */
    SpacecraftState getInterpolatedState() throws OrekitException;

    /** Check is integration direction is forward in date.
     * @return true if integration is forward in date
     */
    boolean isForward();

}
