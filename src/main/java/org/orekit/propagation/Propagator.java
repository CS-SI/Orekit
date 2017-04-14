/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.propagation;

import java.util.Collection;
import java.util.List;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/** This interface provides a way to propagate an orbit at any time.
 *
 * <p>This interface is the top-level abstraction for orbit propagation.
 * It only allows propagation to a predefined date.
 * It is implemented by analytical models which have no time limit,
 * by orbit readers based on external data files, by numerical integrators
 * using rich force models and by continuous models built after numerical
 * integration has been completed and dense output data as been
 * gathered.</p>

 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 *
 */

public interface Propagator extends PVCoordinatesProvider {

    /** Default mass. */
    double DEFAULT_MASS = 1000.0;

    /** Default attitude provider. */
    AttitudeProvider DEFAULT_LAW = InertialProvider.EME2000_ALIGNED;

    /** Indicator for slave mode. */
    int SLAVE_MODE = 0;

    /** Indicator for master mode. */
    int MASTER_MODE = 1;

    /** Indicator for ephemeris generation mode. */
    int EPHEMERIS_GENERATION_MODE = 2;

    /** Get the current operating mode of the propagator.
     * @return one of {@link #SLAVE_MODE}, {@link #MASTER_MODE},
     * {@link #EPHEMERIS_GENERATION_MODE}
     * @see #setSlaveMode()
     * @see #setMasterMode(double, OrekitFixedStepHandler)
     * @see #setMasterMode(OrekitStepHandler)
     * @see #setEphemerisMode()
     */
    int getMode();

    /** Set the propagator to slave mode.
     * <p>This mode is used when the user needs only the final orbit at the target time.
     *  The (slave) propagator computes this result and return it to the calling
     *  (master) application, without any intermediate feedback.
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
     * @see #MASTER_MODE
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
     * @see #MASTER_MODE
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

    /**
     * Set the propagator to ephemeris generation mode with the specified handler for each
     * integration step.
     *
     * <p>This mode is used when the user needs random access to the orbit state at any
     * time between the initial and target times, as well as access to the steps computed
     * by the integrator as in Master Mode. A typical example is the implementation of
     * search and iterative algorithms that may navigate forward and backward inside the
     * propagation range before finding their result.</p>
     *
     * <p>Beware that since this mode stores <strong>all</strong> intermediate results, it
     * may be memory intensive for long integration ranges and high precision/short time
     * steps.</p>
     *
     * @param handler handler called at the end of each finalized step
     * @see #setEphemerisMode()
     * @see #getGeneratedEphemeris()
     * @see #setSlaveMode()
     * @see #setMasterMode(double, OrekitFixedStepHandler)
     * @see #setMasterMode(OrekitStepHandler)
     * @see #getMode()
     * @see #EPHEMERIS_GENERATION_MODE
     */
    void setEphemerisMode(OrekitStepHandler handler);

    /** Get the ephemeris generated during propagation.
     * @return generated ephemeris
     * @exception IllegalStateException if the propagator was not set in ephemeris
     * generation mode before propagation
     * @see #setEphemerisMode()
     */
    BoundedPropagator getGeneratedEphemeris() throws IllegalStateException;

    /** Get the propagator initial state.
     * @return initial state
     * @exception OrekitException if state cannot be retrieved
     */
    SpacecraftState getInitialState() throws OrekitException;

    /** Reset the propagator initial state.
     * @param state new initial state to consider
     * @exception OrekitException if initial state cannot be reset
     */
    void resetInitialState(SpacecraftState state)
        throws OrekitException;

    /** Add a set of user-specified state parameters to be computed along with the orbit propagation.
     * @param additionalStateProvider provider for additional state
     * @exception OrekitException if an additional state with the same name is already present
     */
    void addAdditionalStateProvider(AdditionalStateProvider additionalStateProvider)
        throws OrekitException;

    /** Get an unmodifiable list of providers for additional state.
     * @return providers for the additional states
     */
    List<AdditionalStateProvider> getAdditionalStateProviders();

    /** Check if an additional state is managed.
     * <p>
     * Managed states are states for which the propagators know how to compute
     * its evolution. They correspond to additional states for which an
     * {@link AdditionalStateProvider additional state provider} has been registered
     * by calling the {@link #addAdditionalStateProvider(AdditionalStateProvider)
     * addAdditionalStateProvider} method. If the propagator is an {@link
     * org.orekit.propagation.integration.AbstractIntegratedPropagator integrator-based
     * propagator}, the states for which a set of {@link
     * org.orekit.propagation.integration.AdditionalEquations additional equations} has
     * been registered by calling the {@link
     * org.orekit.propagation.integration.AbstractIntegratedPropagator#addAdditionalEquations(
     * org.orekit.propagation.integration.AdditionalEquations) addAdditionalEquations}
     * method are also counted as managed additional states.
     * </p>
     * <p>
     * Additional states that are present in the {@link #getInitialState() initial state}
     * but have no evolution method registered are <em>not</em> considered as managed states.
     * These unmanaged additional states are not lost during propagation, though. Their
     * value will simply be copied unchanged throughout propagation.
     * </p>
     * @param name name of the additional state
     * @return true if the additional state is managed
     */
    boolean isAdditionalStateManaged(String name);

    /** Get all the names of all managed states.
     * @return names of all managed states
     */
    String[] getManagedAdditionalStates();

    /** Add an event detector.
     * @param detector event detector to add
     * @see #clearEventsDetectors()
     * @see #getEventsDetectors()
     * @param <T> class type for the generic version
     */
    <T extends EventDetector> void addEventDetector(T detector);

    /** Get all the events detectors that have been added.
     * @return an unmodifiable collection of the added detectors
     * @see #addEventDetector(EventDetector)
     * @see #clearEventsDetectors()
     */
    Collection<EventDetector> getEventsDetectors();

    /** Remove all events detectors.
     * @see #addEventDetector(EventDetector)
     * @see #getEventsDetectors()
     */
    void clearEventsDetectors();

    /** Get attitude provider.
     * @return attitude provider
     */
    AttitudeProvider getAttitudeProvider();

    /** Set attitude provider.
     * @param attitudeProvider attitude provider
     */
    void setAttitudeProvider(AttitudeProvider attitudeProvider);

    /** Get the frame in which the orbit is propagated.
     * <p>
     * The propagation frame is the definition frame of the initial
     * state, so this method should be called after this state has
     * been set, otherwise it may return null.
     * </p>
     * @return frame in which the orbit is propagated
     * @see #resetInitialState(SpacecraftState)
     */
    Frame getFrame();

    /** Propagate towards a target date.
     * <p>Simple propagators use only the target date as the specification for
     * computing the propagated state. More feature rich propagators can consider
     * other information and provide different operating modes or G-stop
     * facilities to stop at pinpointed events occurrences. In these cases, the
     * target date is only a hint, not a mandatory objective.</p>
     * @param target target date towards which orbit state should be propagated
     * @return propagated state
     * @exception OrekitException if state cannot be propagated
     */
    SpacecraftState propagate(AbsoluteDate target) throws OrekitException;

    /** Propagate from a start date towards a target date.
     * <p>Those propagators use a start date and a target date to
     * compute the propagated state. For propagators using event detection mechanism,
     * if the provided start date is different from the initial state date, a first,
     * simple propagation is performed, without processing any event computation.
     * Then complete propagation is performed from start date to target date.</p>
     * @param start start date from which orbit state should be propagated
     * @param target target date to which orbit state should be propagated
     * @return propagated state
     * @exception OrekitException if state cannot be propagated
     */
    SpacecraftState propagate(AbsoluteDate start, AbsoluteDate target) throws OrekitException;

}
