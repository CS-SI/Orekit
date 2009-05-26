/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.ConvergenceException;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

/** This class manages several {@link EventDetector event detectors} during propagation.
 *
 * <p>This class is heavily based on the {@link
 * org.apache.commons.math.ode.events.CombinedEventsManager} class from the
 * Apache commons-math library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link AbsoluteDate}, {@link SpacecraftState}).</p>
 * @see EventDetector
 * @version $Revision$ $Date$
 */
public class CombinedEventsDetectorsManager implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 3863772484857573910L;

    /** Detectors states. */
    private List<EventState> states;

    /** First active event. */
    private EventState first;

    /** Initialization indicator. */
    private boolean initialized;

    /** Simple constructor.
     * Create an empty manager
     */
    public CombinedEventsDetectorsManager() {
        states      = new ArrayList<EventState>();
        first       = null;
        initialized = false;
    }

    /** Add an event detector.
     * @param detector event detector to add
     * @see #getEventsDetectors()
     * @see #clearEventsDetectors()
     */
    public void addEventDetector(final EventDetector detector) {
        states.add(new EventState(detector));
    }

    /** Get all the events detectors that have been added to the manager.
     * @return an unmodifiable collection of the added events detectors
     * @see #addEventDetector(EventDetector)
     * @see #clearEventsDetectors()
     */
    public Collection<EventDetector> getEventsDetectors() {
        final List<EventDetector> list = new ArrayList<EventDetector>();
        for (EventState state : states) {
            list.add(state.getEventDetector());
        }
        return Collections.unmodifiableCollection(list);
    }

    /** Remove all the events detectors that have been added to the handler.
     * @see #addEventDetector(EventDetector)
     * @see #getEventsDetectors()
     */
    public void clearEventsDetectors() {
        states.clear();
    }

    /** Evaluate the impact of the proposed step on all handled
     * events detectors.
     * @param interpolator step interpolator for the proposed step
     * @return true if at least one event detector triggers an event
     * before the end of the proposed step (this implies the step should
     * be rejected)
     * @exception OrekitException if the interpolator fails to
     * compute the function somewhere within the step
     * @exception ConvergenceException if an event cannot be located
     */
    public boolean evaluateStep(final OrekitStepInterpolator interpolator)
        throws OrekitException, ConvergenceException {

        first = null;
        if (states.isEmpty()) {
            // there is nothing to do, return now to avoid setting the
            // interpolator time (and hence avoid unneeded calls to the
            // user function due to interpolator finalization)
            return false;
        }

        if (!initialized) {

            // initialize the events states
            final AbsoluteDate t0 = interpolator.getPreviousDate();
            interpolator.setInterpolatedDate(t0);
            final SpacecraftState y = interpolator.getInterpolatedState();
            for (final EventState state : states) {
                state.reinitializeBegin(y);
            }

            initialized = true;

        }

        // check events occurrence
        for (final EventState state : states) {

            if (state.evaluateStep(interpolator)) {
                if (first == null) {
                    first = state;
                } else {
                    if (interpolator.isForward()) {
                        if (state.getEventTime().compareTo(first.getEventTime()) < 0) {
                            first = state;
                        }
                    } else {
                        if (state.getEventTime().compareTo(first.getEventTime()) > 0) {
                            first = state;
                        }
                    }
                }
            }

        }

        return first != null;

    }

    /** Get the occurrence time of the first event triggered in the
     * last evaluated step.
     * @return occurrence time of the first event triggered in the last
     * evaluated step, or null if no event is triggered
     */
    public AbsoluteDate getEventTime() {
        return (first == null) ? null : first.getEventTime();
    }

    /** Inform the events detectors that the step has been accepted
     * by the propagator.
     * @param spacecraftState state value at the end of the step
     * @exception OrekitException if the value of one of the
     * events detectors cannot be evaluated
     */
    public void stepAccepted(final SpacecraftState spacecraftState)
        throws OrekitException {
        for (final EventState eventState : states) {
            eventState.stepAccepted(spacecraftState);
        }
    }

    /** Check if the propagation should be stopped at the end of the
     * current step.
     * @return true if the propagation should be stopped
     */
    public boolean stop() {
        for (final EventState state : states) {
            if (state.stop()) {
                return true;
            }
        }
        return false;
    }

    /** Let the events detectors reset the state if they want.
     * <p>If several detectors reset the state at the same time, the former
     * changes will be overriden by later changes, and only the last change
     * will be returned. A better way to handle this is to use only one
     * event detector for simultaneous changes.</p>
     * @param oldSpacecraftState value of the state vector at the beginning of the next step
     * @return new state (oldState if no reset is needed)
     * @exception OrekitException if one of the events detectors
     * that should reset the state fails to do it
     */
    public SpacecraftState reset(final SpacecraftState oldSpacecraftState)
        throws OrekitException {
        SpacecraftState newSpacecraftState = oldSpacecraftState;
        for (final EventState eventState : states) {
            newSpacecraftState = eventState.reset(newSpacecraftState);
        }
        return newSpacecraftState;
    }

}
