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
package org.orekit.propagation;

import java.util.Collection;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.linear.RealMatrix;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.StepHandlerMultiplexer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
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
 * <p>Note that one single propagator cannot be called from multiple threads.
 * Its configuration can be changed as there is at least a {@link
 * #resetInitialState(SpacecraftState)} method, and even propagators that do
 * not support resetting state (like the {@link
 * org.orekit.propagation.analytical.tle.TLEPropagator TLEPropagator} do
 * cache some internal data during computation. However, as long as they
 * are configured with independent building blocks (mainly event handlers
 * and step handlers that may preserve some internal state), and as long
 * as they are called from one thread only, they <em>can</em> be used in
 * multi-threaded applications. Synchronizing several propagators to run in
 * parallel is also possible using {@link PropagatorsParallelizer}.</p>
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 *
 */

public interface Propagator extends PVCoordinatesProvider {

    /** Default mass. */
    double DEFAULT_MASS = 1000.0;

    /**
     * Get a default law using the given frames.
     *
     * @param frames the set of frames to use.
     * @return attitude law.
     */
    static AttitudeProvider getDefaultLaw(final Frames frames) {
        return new FrameAlignedProvider(Rotation.IDENTITY, frames.getEME2000());
    }

    /** Get the multiplexer holding all step handlers.
     * @return multiplexer holding all step handlers
     * @since 11.0
     */
    StepHandlerMultiplexer getMultiplexer();

    /** Remove all step handlers.
     * <p>This convenience method is equivalent to call {@code getMultiplexer().clear()}</p>
     * @see #getMultiplexer()
     * @see StepHandlerMultiplexer#clear()
     * @since 11.0
     */
    default void clearStepHandlers() {
        getMultiplexer().clear();
    }

    /** Set a single handler for fixed stepsizes.
     * <p>This convenience method is equivalent to call {@code getMultiplexer().clear()}
     * followed by {@code getMultiplexer().add(h, handler)}</p>
     * @param h fixed stepsize (s)
     * @param handler handler called at the end of each finalized step
     * @see #getMultiplexer()
     * @see StepHandlerMultiplexer#add(double, OrekitFixedStepHandler)
     * @since 11.0
     */
    default void setStepHandler(final double h, final OrekitFixedStepHandler handler) {
        getMultiplexer().clear();
        getMultiplexer().add(h, handler);
    }

    /** Set a single handler for variable stepsizes.
     * <p>This convenience method is equivalent to call {@code getMultiplexer().clear()}
     * followed by {@code getMultiplexer().add(handler)}</p>
     * @param handler handler called at the end of each finalized step
     * @see #getMultiplexer()
     * @see StepHandlerMultiplexer#add(OrekitStepHandler)
     * @since 11.0
     */
    default void setStepHandler(final OrekitStepHandler handler) {
        getMultiplexer().clear();
        getMultiplexer().add(handler);
    }

    /**
     * Set up an ephemeris generator that will monitor the propagation for building
     * an ephemeris from it once completed.
     *
     * <p>
     * This generator can be used when the user needs fast random access to the orbit
     * state at any time between the initial and target times. A typical example is the
     * implementation of search and iterative algorithms that may navigate forward and
     * backward inside the propagation range before finding their result even if the
     * propagator used is integration-based and only goes from one initial time to one
     * target time.
     * </p>
     * <p>
     * Beware that when used with integration-based propagators, the generator will
     * store <strong>all</strong> intermediate results. It is therefore memory intensive
     * for long integration-based ranges and high precision/short time steps. When
     * used with analytical propagators, the generator only stores start/stop time
     * and a reference to the analytical propagator itself to call it back as needed,
     * so it is less memory intensive.
     * </p>
     * <p>
     * The returned ephemeris generator will be initially empty, it will be filled
     * with propagation data when a subsequent call to either {@link #propagate(AbsoluteDate)
     * propagate(target)} or {@link #propagate(AbsoluteDate, AbsoluteDate)
     * propagate(start, target)} is called. The proper way to use this method is
     * therefore to do:
     * </p>
     * <pre>
     *   EphemerisGenerator generator = propagator.getEphemerisGenerator();
     *   propagator.propagate(target);
     *   BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
     * </pre>
     * @return ephemeris generator
     */
    EphemerisGenerator getEphemerisGenerator();

    /** Get the propagator initial state.
     * @return initial state
     */
    SpacecraftState getInitialState();

    /** Reset the propagator initial state.
     * @param state new initial state to consider
     */
    void resetInitialState(SpacecraftState state);

    /** Add a set of user-specified state parameters to be computed along with the orbit propagation.
     * @param additionalStateProvider provider for additional state
     */
    void addAdditionalStateProvider(AdditionalStateProvider additionalStateProvider);

    /** Get an unmodifiable list of providers for additional state.
     * @return providers for the additional states
     */
    List<AdditionalStateProvider> getAdditionalStateProviders();

    /** Check if an additional state is managed.
     * <p>
     * Managed states are states for which the propagators know how to compute
     * its evolution. They correspond to additional states for which a
     * {@link AdditionalStateProvider provider} has been registered by calling the
     * {@link #addAdditionalStateProvider(AdditionalStateProvider) addAdditionalStateProvider} method.
     * </p>
     * <p>
     * Additional states that are present in the {@link #getInitialState() initial state}
     * but have no evolution method registered are <em>not</em> considered as managed states.
     * These unmanaged additional states are not lost during propagation, though. Their
     * value are piecewise constant between state resets that may change them if some
     * event handler {@link
     * org.orekit.propagation.events.handlers.EventHandler#resetState(EventDetector,
     * SpacecraftState) resetState} method is called at an event occurrence and happens
     * to change the unmanaged additional state.
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

    /** Set up computation of State Transition Matrix and Jacobians matrix with respect to parameters.
     * <p>
     * If this method is called, both State Transition Matrix and Jacobians with respect to the
     * force models parameters that will be selected when propagation starts will be automatically
     * computed, and the harvester will allow to retrieve them.
     * </p>
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the {@link org.orekit.orbits.OrbitType
     * orbit type} and {@link PositionAngleType position angle} that will be used by the propagator.
     * </p>
     * <p>
     * The default implementation throws an exception as the method is not supported by all propagators.
     * </p>
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     * @return harvester to retrieve computed matrices during and after propagation
     * @since 11.1
     */
    default MatricesHarvester setupMatricesComputation(final String stmName, final RealMatrix initialStm,
                                                       final DoubleArrayDictionary initialJacobianColumns) {
        throw new UnsupportedOperationException();
    }

    /** Propagate towards a target date.
     * <p>Simple propagators use only the target date as the specification for
     * computing the propagated state. More feature rich propagators can consider
     * other information and provide different operating modes or G-stop
     * facilities to stop at pinpointed events occurrences. In these cases, the
     * target date is only a hint, not a mandatory objective.</p>
     * @param target target date towards which orbit state should be propagated
     * @return propagated state
     */
    SpacecraftState propagate(AbsoluteDate target);

    /** Propagate from a start date towards a target date.
     * <p>Those propagators use a start date and a target date to
     * compute the propagated state. For propagators using event detection mechanism,
     * if the provided start date is different from the initial state date, a first,
     * simple propagation is performed, without processing any event computation.
     * Then complete propagation is performed from start date to target date.</p>
     * @param start start date from which orbit state should be propagated
     * @param target target date to which orbit state should be propagated
     * @return propagated state
     */
    SpacecraftState propagate(AbsoluteDate start, AbsoluteDate target);

}
