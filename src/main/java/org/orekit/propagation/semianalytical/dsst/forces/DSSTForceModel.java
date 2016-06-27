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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.List;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/** This interface represents a force modifying spacecraft motion for a {@link
 *  org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *  <p>
 *  Objects implementing this interface are intended to be added to a {@link
 *  org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSST propagator}
 *  before the propagation is started.
 *  </p>
 *  <p>
 *  The propagator will call at the very beginning of a propagation the {@link
 *  #initialize(AuxiliaryElements, boolean)} method allowing preliminary computation
 *  such as truncation if needed.
 *  </p>
 *  <p>
 *  Then the propagator will call at each step:
 *  <ol>
 *  <li>the {@link #initializeStep(AuxiliaryElements)} method.
 *  The force model instance will extract all the elements needed before
 *  computing the mean element rates.</li>
 *  <li>the {@link #getMeanElementRate(SpacecraftState)} method.
 *  The force model instance will extract all the state data needed to compute
 *  the mean element rates that contribute to the mean state derivative.</li>
 *  <li>the {@link #updateShortPeriodTerms(SpacecraftState...)} method,
 *  if osculating parameters are desired, on a sample of points within the
 *  last step.</li>
 *  </ol>
 *
 * @author Romain Di Constanzo
 * @author Pascal Parraud
 */
public interface DSSTForceModel {

    /** Performs initialization prior to propagation for the current force model.
     *  <p>
     *  This method aims at being called at the very beginning of a propagation.
     *  </p>
     *  @param aux auxiliary elements related to the current orbit
     *  @param meanOnly only mean elements are used during the propagation
     *  @return a list of objects that will hold short period terms (the objects
     *  are also retained by the force model, which will update them during propagation)
     *  @throws OrekitException if some specific error occurs
     */
    List<ShortPeriodTerms> initialize(AuxiliaryElements aux, boolean meanOnly)
        throws OrekitException;

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param aux auxiliary elements related to the current orbit
     *  @throws OrekitException if some specific error occurs
     */
    void initializeStep(AuxiliaryElements aux)
        throws OrekitException;

    /** Computes the mean equinoctial elements rates da<sub>i</sub> / dt.
     *
     *  @param state current state information: date, kinematics, attitude
     *  @return the mean element rates dai/dt
     *  @throws OrekitException if some specific error occurs
     */
    double[] getMeanElementRate(SpacecraftState state) throws OrekitException;

    /** Get the discrete events related to the model.
     * @return array of events detectors or null if the model is not
     * related to any discrete events
     */
    EventDetector[] getEventsDetectors();

    /** Register an attitude provider.
     * <p>
     * Register an attitude provider that can be used by the force model.
     * </p>
     * @param provider the {@link AttitudeProvider}
     */
    void registerAttitudeProvider(AttitudeProvider provider);

    /** Update the short period terms.
     * <p>
     * The {@link ShortPeriodTerms short period terms} that will be updated
     * are the ones that were returned during the call to {@link
     * #initialize(AuxiliaryElements, boolean)}.
     * </p>
     * @param meanStates mean states information: date, kinematics, attitude
     * @throws OrekitException if some specific error occurs
     */
    void updateShortPeriodTerms(SpacecraftState ... meanStates)
        throws OrekitException;

}
