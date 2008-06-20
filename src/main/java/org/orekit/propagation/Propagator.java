/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.propagation;

import org.orekit.propagation.events.OrekitFixedStepHandler;
import org.orekit.propagation.events.OrekitStepHandler;
import org.orekit.propagation.events.OrekitSwitchingFunction;

/** This interface provides a way to propagate an orbit at any time.
 *
 * <p>This interface is the top-level abstraction for orbit propagation.
 * It is implemented by analytical models which have no time limit,
 * by orbit readers based on external data files, by numerical integrators
 * using rich force models and by continuous models built after numerical
 * integration has been completed and dense output data as been
 * gathered.</p>

 * @author Luc Maisonobe
 * @author Véronique Pommier-Maurussane
 *
 * @version $Revision$ $Date$
 */

public interface Propagator extends BasicPropagator {

    /** Indicator for slave mode. */
    int SLAVE_MODE = 0;

    /** Indicator for master mode with fixed steps. */
    int MASTER_FIXED_MODE = 1;

    /** Indicator for master mode with variable steps. */
    int MASTER_VARIABLE_MODE = 2;

    /** Indicator for ephemeris generation mode. */
    int EPHEMERIS_GENERATION_MODE = 3;

    /** Get the current operating mode of the propagator.
     * @return one of {@link #SLAVE_MODE}, {@link #MASTER_FIXED_MODE},
     * {@link #MASTER_VARIABLE_MODE} or {@link #EPHEMERIS_GENERATION_MODE}
     * @see #setSlaveMode()
     * @see #setMasterMode(double, OrekitFixedStepHandler)
     * @see #setMasterMode(OrekitStepHandler)
     * @see #setEphemerisMode()
     */
    int getMode();

    /** Set the propagator to slave mode.
     * <p>This mode is used when the user needs only the final orbit at the target time.
     *  The (slave) propagator computes this result and return it to the calling
     *  (master) application, without any intermediate feedback.<p>
     * <p>This is the default mode.</p>
     * @see #setMasterMode(double, OrekitFixedStepHandler)
     * @see #setMasterMode(OrekitStepHandler)
     * @see #setEphemerisMode()
     * @see #getMode()
     * @see #SLAVE_MODE
     */
    void setSlaveMode();

    /** Set the propagator to master mode with fixed steps.
     * <p>This mode is used when the user needs to have some custom function called at the
     * end of each finalized step during integration. The (master) propagator integration
     * loop calls the (slave) application callback methods at each finalized step.</p>
     * @param h fixed stepsize (s)
     * @param handler handler called at the end of each finalized step
     * @see #setSlaveMode()
     * @see #setMasterMode(OrekitStepHandler)
     * @see #setEphemerisMode()
     * @see #getMode()
     * @see #MASTER_FIXED_MODE
     */
    void setMasterMode(double h, OrekitFixedStepHandler handler);

    /** Set the propagator to master mode with variable steps.
     * <p>This mode is used when the user needs to have some custom function called at the
     * end of each finalized step during integration. The (master) propagator integration
     * loop calls the (slave) application callback methods at each finalized step.</p>
     * @param handler handler called at the end of each finalized step
     * @see #setSlaveMode()
     * @see #setMasterMode(double, OrekitFixedStepHandler)
     * @see #setEphemerisMode()
     * @see #getMode()
     * @see #MASTER_VARIABLE_MODE
     */
    void setMasterMode(OrekitStepHandler handler);

    /** Set the propagator to ephemeris generation mode.
     *  <p>This mode is used when the user needs random access to the orbit state at any time
     *  between the initial and target times, and in no sequential order. A typical example is
     *  the implementation of search and iterative algorithms that may navigate forward and
     *  backward inside the propagation range before finding their result.</p>
     *  <p>Beware that since this mode stores <strong>all</strong> intermediate results,
     *  it may be memory intensive for long integration ranges and high precision/short
     *  time steps.</p>
     * @see #getGeneratedEphemeris()
     * @see #setSlaveMode()
     * @see #setMasterMode(double, OrekitFixedStepHandler)
     * @see #setMasterMode(OrekitStepHandler)
     * @see #getMode()
     * @see #EPHEMERIS_GENERATION_MODE
     */
    void setEphemerisMode();

    /** Get the ephemeris generated during propagation.
     * @return generated ephemeris
     * @exception IllegalStateException if the propagator was not set in ephemeris
     * generation mode before propagation
     * @see #setEphemerisMode()
     */
    BoundedPropagator getGeneratedEphemeris() throws IllegalStateException;

    /** Add a switching function.
     * @param switchingFunction switching function to add
     * @see #removeSwitchingFunctions()
     */
    void addSwitchingFunction(final OrekitSwitchingFunction switchingFunction);

    /** Remove all switching functions.
     * @see #addSwitchingFunction(OrekitSwitchingFunction)
     */
    void removeSwitchingFunctions();

}
