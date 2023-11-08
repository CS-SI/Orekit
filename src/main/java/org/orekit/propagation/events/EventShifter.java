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
package org.orekit.propagation.events;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** Wrapper shifting events occurrences times.
 * <p>This class wraps an {@link EventDetector event detector} to slightly
 * shift the events occurrences times. A typical use case is for handling
 * operational delays before or after some physical event really occurs.</p>
 * <p>For example, the satellite attitude mode may be switched from sun pointed
 * to spin-stabilized a few minutes before eclipse entry, and switched back
 * to sun pointed a few minutes after eclipse exit. This behavior is handled
 * by wrapping an {@link EclipseDetector eclipse detector} into an instance
 * of this class with a positive times shift for increasing events (eclipse exit)
 * and a negative times shift for decreasing events (eclipse entry).</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see EventDetector
 * @author Luc Maisonobe
 */
public class EventShifter extends AbstractDetector<EventShifter> {

    /** Event detector for the raw unshifted event. */
    private final EventDetector detector;

    /** Indicator for using shifted or unshifted states at event occurrence. */
    private final boolean useShiftedStates;

    /** Offset to apply to find increasing events. */
    private final double increasingOffset;

    /** Offset to apply to find decreasing events. */
    private final double decreasingOffset;

    /** Build a new instance.
     * <p>The {@link #getMaxCheckInterval() max check interval}, the
     * {@link #getThreshold() convergence threshold} of the raw unshifted
     * events will be used for the shifted event. When an event occurs,
     * the {@link EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean) eventOccurred}
     * method of the raw unshifted events will be called (with spacecraft
     * state at either the shifted or the unshifted event date depending
     * on the <code>useShiftedStates</code> parameter).</p>
     * @param detector event detector for the raw unshifted event
     * @param useShiftedStates if true, the state provided to {@link
     * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean) eventOccurred} method of
     * the associated {@code handler} will remain shifted, otherwise it will
     * be <i>unshifted</i> to correspond to the underlying raw event.
     * @param increasingTimeShift increasing events time shift.
     * @param decreasingTimeShift decreasing events time shift.
     */
    public EventShifter(final EventDetector detector, final boolean useShiftedStates,
                        final double increasingTimeShift, final double decreasingTimeShift) {
        this(detector.getMaxCheckInterval(), detector.getThreshold(),
             detector.getMaxIterationCount(), new LocalHandler(),
             detector, useShiftedStates, increasingTimeShift, decreasingTimeShift);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param detector event detector for the raw unshifted event
     * @param useShiftedStates if true, the state provided to {@link
     * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean) eventOccurred} method of
     * the <code>detector</code> will remain shifted, otherwise it will
     * be <i>unshifted</i> to correspond to the underlying raw event.
     * @param increasingTimeShift increasing events time shift.
     * @param decreasingTimeShift decreasing events time shift.
     * @since 6.1
     */
    protected EventShifter(final AdaptableInterval maxCheck, final double threshold,
                           final int maxIter, final EventHandler handler,
                           final EventDetector detector, final boolean useShiftedStates,
                           final double increasingTimeShift, final double decreasingTimeShift) {
        super(maxCheck, threshold, maxIter, handler);
        this.detector         = detector;
        this.useShiftedStates = useShiftedStates;
        this.increasingOffset = -increasingTimeShift;
        this.decreasingOffset = -decreasingTimeShift;
    }

    /** {@inheritDoc} */
    @Override
    protected EventShifter create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                  final int newMaxIter, final EventHandler newHandler) {
        return new EventShifter(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                detector, useShiftedStates, -increasingOffset, -decreasingOffset);
    }

    /**
     * Get the detector for the raw unshifted event.
     * @return the detector for the raw unshifted event
     * @since 11.1
     */
    public EventDetector getDetector() {
        return detector;
    }

    /** Get the increasing events time shift.
     * @return increasing events time shift
     */
    public double getIncreasingTimeShift() {
        return -increasingOffset;
    }

    /** Get the decreasing events time shift.
     * @return decreasing events time shift
     */
    public double getDecreasingTimeShift() {
        return -decreasingOffset;
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0,
                     final AbsoluteDate t) {
        super.init(s0, t);
        detector.init(s0, t);
    }

    /** {@inheritDoc} */
    public double g(final SpacecraftState s) {
        final double incShiftedG = detector.g(s.shiftedBy(increasingOffset));
        final double decShiftedG = detector.g(s.shiftedBy(decreasingOffset));
        return (increasingOffset >= decreasingOffset) ?
               FastMath.max(incShiftedG, decShiftedG) : FastMath.min(incShiftedG, decShiftedG);
    }

    /** Local class for handling events. */
    private static class LocalHandler implements EventHandler {

        /** Shifted state at even occurrence. */
        private SpacecraftState shiftedState;

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {

            final EventShifter shifter = (EventShifter) detector;
            if (shifter.useShiftedStates) {
                // the state provided by the caller already includes the time shift
                shiftedState = s;
            } else {
                // we need to "unshift" the state
                final double offset = increasing ? shifter.increasingOffset : shifter.decreasingOffset;
                shiftedState = s.shiftedBy(offset);
            }

            return shifter.detector.getHandler().eventOccurred(shiftedState, shifter.detector, increasing);

        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
            final EventShifter shifter = (EventShifter) detector;
            return shifter.detector.getHandler().resetState(shifter.detector, shiftedState);
        }

    }

}
