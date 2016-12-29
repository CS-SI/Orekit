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

/** This interface is a space-dynamics aware fixed size step handler.
 *
 * <p>It mirrors the <code>FixedStepHandler</code> interface from <a
 * href="http://commons.apache.org/math/">commons-math</a> but provides
 * a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 */
public interface FieldOrekitFixedStepHandler<T extends RealFieldElement<T>> {

    /** Initialize step handler at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It
     * may be used by the step handler to initialize some internal data
     * if needed.
     * </p>
     * @param s0 initial state
     * @param t target time for the integration
     * @param step the duration in seconds of the fixed step. This value is
     *             positive even if propagation is backwards.
     * @exception OrekitException if step handler cannot be initialized
     */
    default void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t, T step)
        throws OrekitException {
        // do nothing by default
    }

    /** Handle the current step.
     * @param currentState current state at step time
     * @param isLast if true, this is the last integration step
     * @exception OrekitException if step cannot be handled
     */
    void handleStep(FieldSpacecraftState<T> currentState, boolean isLast)
        throws OrekitException;

}
