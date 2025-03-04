/* Copyright 2022-2025 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;

/** Wrapper shifting events occurrences times.
 * <p>This class wraps an {@link FieldEventDetector event detector} to slightly
 * shift the events occurrences times. A typical use case is for handling
 * operational delays before or after some physical event really occurs.</p>
 * <p>For example, the satellite attitude mode may be switched from sun pointed
 * to spin-stabilized a few minutes before eclipse entry, and switched back
 * to sun pointed a few minutes after eclipse exit. This behavior is handled
 * by wrapping an {@link FieldEclipseDetector eclipse detector} into an instance
 * of this class with a positive times shift for increasing events (eclipse exit)
 * and a negative times shift for decreasing events (eclipse entry).</p>
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @see FieldEventDetector
 * @see EventShifter
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 13.0
 */
public class FieldEventShifter<T extends CalculusFieldElement<T>> implements FieldDetectorModifier<T> {

    /** Event detector for the raw unshifted event. */
    private final FieldEventDetector<T> detector;

    /** Indicator for using shifted or unshifted states at event occurrence. */
    private final boolean useShiftedStates;

    /** Offset to apply to find increasing events. */
    private final T increasingOffset;

    /** Offset to apply to find decreasing events. */
    private final T decreasingOffset;

    /** Specialized event handler. */
    private final LocalHandler<T> handler;

    /** Event detection settings. */
    private final FieldEventDetectionSettings<T> detectionSettings;

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
    public FieldEventShifter(final FieldEventDetector<T> detector, final boolean useShiftedStates,
                             final T increasingTimeShift, final T decreasingTimeShift) {
        this(detector.getDetectionSettings(), detector, useShiftedStates, increasingTimeShift, decreasingTimeShift);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param detectionSettings event detection settings
     * @param detector event detector for the raw unshifted event
     * @param useShiftedStates if true, the state provided to {@link
     * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean) eventOccurred} method of
     * the <code>detector</code> will remain shifted, otherwise it will
     * be <i>unshifted</i> to correspond to the underlying raw event.
     * @param increasingTimeShift increasing events time shift.
     * @param decreasingTimeShift decreasing events time shift.
     * @since 13.0
     */
    protected FieldEventShifter(final FieldEventDetectionSettings<T> detectionSettings,
                                final FieldEventDetector<T> detector, final boolean useShiftedStates,
                                final T increasingTimeShift, final T decreasingTimeShift) {
        this.detectionSettings = detectionSettings;
        this.handler          = new LocalHandler<>();
        this.detector         = detector;
        this.useShiftedStates = useShiftedStates;
        this.increasingOffset = increasingTimeShift.negate();
        this.decreasingOffset = decreasingTimeShift.negate();
    }

    @Override
    public FieldEventHandler<T> getHandler() {
        return handler;
    }

    @Override
    public FieldEventDetectionSettings<T> getDetectionSettings() {
        return detectionSettings;
    }

    /**
     * Get the detector for the raw unshifted event.
     * @return the detector for the raw unshifted event
     */
    public FieldEventDetector<T> getDetector() {
        return detector;
    }

    /** Get the increasing events time shift.
     * @return increasing events time shift
     */
    public T getIncreasingTimeShift() {
        return increasingOffset.negate();
    }

    /** Get the decreasing events time shift.
     * @return decreasing events time shift
     */
    public T getDecreasingTimeShift() {
        return decreasingOffset.negate();
    }

    /**
     * Builds a new instance from the input detection settings.
     * @param settings event detection settings to be used
     * @return a new detector
     */
    public FieldEventShifter<T> withDetectionSettings(final FieldEventDetectionSettings<T> settings) {
        return new FieldEventShifter<>(settings, detector, useShiftedStates, getIncreasingTimeShift(), getDecreasingTimeShift());
    }

    /** {@inheritDoc} */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        final T incShiftedG = detector.g(s.shiftedBy(increasingOffset));
        final T decShiftedG = detector.g(s.shiftedBy(decreasingOffset));
        return (increasingOffset.getReal() >= decreasingOffset.getReal()) ?
               FastMath.max(incShiftedG, decShiftedG) : FastMath.min(incShiftedG, decShiftedG);
    }

    /** Local class for handling events. */
    private static class LocalHandler<W extends CalculusFieldElement<W>> implements FieldEventHandler<W> {

        /** Shifted state at even occurrence. */
        private FieldSpacecraftState<W> shiftedState;

        /** {@inheritDoc} */
        public Action eventOccurred(final FieldSpacecraftState<W> s, final FieldEventDetector<W> detector,
                                    final boolean increasing) {

            final FieldEventShifter<W> shifter = (FieldEventShifter<W>) detector;
            if (shifter.useShiftedStates) {
                // the state provided by the caller already includes the time shift
                shiftedState = s;
            } else {
                // we need to "unshift" the state
                final W offset = increasing ? shifter.increasingOffset : shifter.decreasingOffset;
                shiftedState = s.shiftedBy(offset);
            }

            return shifter.detector.getHandler().eventOccurred(shiftedState, shifter.detector, increasing);

        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<W> resetState(final FieldEventDetector<W> detector,
                                                  final FieldSpacecraftState<W> oldState) {
            final FieldEventShifter<W> shifter = (FieldEventShifter<W>) detector;
            return shifter.detector.getHandler().resetState(shifter.detector, shiftedState);
        }

    }

}
