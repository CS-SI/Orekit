/* Copyright 2002-2025 CS GROUP
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
package org.orekit.attitudes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinatesHermiteInterpolator;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinatesHermiteInterpolator;

/** This classes manages a sequence of different attitude providers that are activated
 * in turn according to switching events. It includes non-zero transition durations between subsequent modes.
 * @author Luc Maisonobe
 * @since 5.1
 * @see AttitudesSwitcher
 */
public class AttitudesSequence extends AbstractSwitchingAttitudeProvider {

    /** Switching events list. */
    private final List<Switch> switches;

    /** Constructor for an initially empty sequence.
     */
    public AttitudesSequence() {
        super();
        switches = new ArrayList<>();
    }

    /** Add a switching condition between two attitude providers.
     * <p>
     * The {@code past} and {@code future} attitude providers are defined with regard
     * to the natural flow of time. This means that if the propagation is forward, the
     * propagator will switch from {@code past} provider to {@code future} provider at
     * event occurrence, but if the propagation is backward, the propagator will switch
     * from {@code future} provider to {@code past} provider at event occurrence. The
     * transition between the two attitude laws is not instantaneous, the switch event
     * defines the start of the transition (i.e. when leaving the {@code past} attitude
     * law and entering the interpolated transition law). The end of the transition
     * (i.e. when leaving the interpolating transition law and entering the {@code future}
     * attitude law) occurs at switch time plus {@code transitionTime}.
     * </p>
     * <p>
     * An attitude provider may have several different switch events associated to
     * it. Depending on which event is triggered, the appropriate provider is
     * switched to.
     * </p>
     * <p>
     * If the underlying detector has an event handler associated to it, this handler
     * will be triggered (i.e. its {@link org.orekit.propagation.events.handlers.EventHandler#eventOccurred(SpacecraftState,
     * EventDetector, boolean) eventOccurred} method will be called), <em>regardless</em>
     * of the event really triggering an attitude switch or not. As an example, if an
     * eclipse detector is used to switch from day to night attitude mode when entering
     * eclipse, with {@code switchOnIncrease} set to {@code false} and {@code switchOnDecrease}
     * set to {@code true}. Then a handler set directly at eclipse detector level would
     * be triggered at both eclipse entry and eclipse exit, but attitude switch would
     * occur <em>only</em> at eclipse entry. Note that for the sake of symmetry, the
     * transition start and end dates should match for both forward and backward propagation.
     * This implies that for backward propagation, we have to compensate for the {@code
     * transitionTime} when looking for the event. An unfortunate consequence is that the
     * {@link org.orekit.propagation.events.handlers.EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
     * eventOccurred} method may appear to be called out of sync with respect to the
     * propagation (it will be called when propagator reaches transition end, despite it
     * refers to transition start, as per {@code transitionTime} compensation), and if the
     * method returns {@link Action#STOP}, it will stop at the end of the
     * transition instead of at the start. For these reasons, it is not recommended to
     * set up an event handler for events that are used to switch attitude. If an event
     * handler is needed for other purposes, a second handler should be registered to
     * the propagator rather than relying on the side effects of attitude switches.
     * </p>
     * <p>
     * The smoothness of the transition between past and future attitude laws can be tuned
     * using the {@code transitionTime} and {@code transitionFilter} parameters. The {@code
     * transitionTime} parameter specifies how much time is spent to switch from one law to
     * the other law. It should be larger than the event {@link EventDetector#getThreshold()
     * convergence threshold} in order to ensure attitude continuity. The {@code
     * transitionFilter} parameter specifies the attitude time derivatives that should match
     * at the boundaries between past attitude law and transition law on one side, and
     * between transition law and future law on the other side.
     * {@link AngularDerivativesFilter#USE_R} means only the rotation should be identical,
     * {@link AngularDerivativesFilter#USE_RR} means both rotation and rotation rate
     * should be identical, {@link AngularDerivativesFilter#USE_RRA} means both rotation,
     * rotation rate and rotation acceleration should be identical. During the transition,
     * the attitude law is computed by interpolating between past attitude law at switch time
     * and future attitude law at current intermediate time.
     * </p>
     * @param past attitude provider applicable for times in the switch event occurrence past
     * @param future attitude provider applicable for times in the switch event occurrence future
     * @param switchEvent event triggering the attitude providers switch
     * @param switchOnIncrease if true, switch is triggered on increasing event
     * @param switchOnDecrease if true, switch is triggered on decreasing event
     * @param transitionTime duration of the transition between the past and future attitude laws
     * @param transitionFilter specification of transition law time derivatives that
     * should match past and future attitude laws
     * @param switchHandler handler to call for notifying when switch occurs (may be null)
     * @param <T> class type for the switch event
     * @since 13.0
     */
    public <T extends EventDetector> void addSwitchingCondition(final AttitudeProvider past,
                                                                final AttitudeProvider future,
                                                                final T switchEvent,
                                                                final boolean switchOnIncrease,
                                                                final boolean switchOnDecrease,
                                                                final double transitionTime,
                                                                final AngularDerivativesFilter transitionFilter,
                                                                final AttitudeSwitchHandler switchHandler) {

        // safety check, for ensuring attitude continuity
        if (transitionTime < switchEvent.getThreshold()) {
            throw new OrekitException(OrekitMessages.TOO_SHORT_TRANSITION_TIME_FOR_ATTITUDES_SWITCH,
                                      transitionTime, switchEvent.getThreshold());
        }

        // if it is the first switching condition, assume first active law is the past one
        if (getActivated() == null) {
            resetActiveProvider(past);
        }

        // add the switching condition
        switches.add(new Switch(switchEvent, switchOnIncrease, switchOnDecrease,
                                past, future, transitionTime, transitionFilter, switchHandler));

    }

    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.concat(switches.stream().map(Switch.class::cast), getEventDetectors(getParametersDrivers()));
    }

    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final Stream<FieldEventDetector<T>> switchesStream = switches.stream().map(sw -> getFieldEventDetector(field, sw));
        return Stream.concat(switchesStream, getFieldEventDetectors(field, getParametersDrivers()));
    }

    /**
     * Gets a deep copy of the switches stored in this instance.
     *
     * @return deep copy of the switches stored in this instance
     */
    public List<Switch> getSwitches() {
        return new ArrayList<>(switches);
    }

    /** Switch specification. Handles the transition. */
    public class Switch extends AbstractAttitudeSwitch {

        /** Duration of the transition between the past and future attitude laws. */
        private final double transitionTime;

        /** Order at which the transition law time derivatives should match past and future attitude laws. */
        private final AngularDerivativesFilter transitionFilter;

        /** Propagation direction. */
        private boolean forward;

        /**
         * Simple constructor.
         *
         * @param event event
         * @param switchOnIncrease if true, switch is triggered on increasing event
         * @param switchOnDecrease if true, switch is triggered on decreasing event otherwise switch is triggered on
         * decreasing event
         * @param past attitude provider applicable for times in the switch event occurrence past
         * @param future attitude provider applicable for times in the switch event occurrence future
         * @param transitionTime duration of the transition between the past and future attitude laws
         * @param transitionFilter order at which the transition law time derivatives should match past and future attitude
         * laws
         * @param switchHandler handler to call for notifying when switch occurs (may be null)
         */
        private Switch(final EventDetector event, final boolean switchOnIncrease, final boolean switchOnDecrease,
                       final AttitudeProvider past, final AttitudeProvider future, final double transitionTime,
                       final AngularDerivativesFilter transitionFilter, final AttitudeSwitchHandler switchHandler) {
            super(event, switchOnIncrease, switchOnDecrease, past, future, switchHandler);
            this.transitionTime   = transitionTime;
            this.transitionFilter = transitionFilter;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            super.init(s0, t);

            // reset the transition parameters (this will be done once for each switch,
            //  despite doing it only once would have sufficient; it's not really a problem)
            forward = t.durationFrom(s0.getDate()) >= 0.0;
            if (getActivated().getSpansNumber() > 1) {
                // remove transitions that will be overridden during upcoming propagation
                if (forward) {
                    setActivated(getActivated().extractRange(AbsoluteDate.PAST_INFINITY, s0.getDate().shiftedBy(transitionTime)));
                } else {
                    setActivated(getActivated().extractRange(s0.getDate().shiftedBy(-transitionTime), AbsoluteDate.FUTURE_INFINITY));
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState s) {
            return getDetector().g(forward ? s : s.shiftedBy(-transitionTime));
        }

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {

            final AbsoluteDate date = s.getDate();
            if (getActivated().get(date) == (forward ? getPast() : getFuture()) &&
                (increasing && isSwitchOnIncrease() || !increasing && isSwitchOnDecrease())) {

                if (forward) {

                    // prepare transition
                    final AbsoluteDate transitionEnd = date.shiftedBy(transitionTime);
                    getActivated().addValidAfter(new TransitionProvider(s.getAttitude(), transitionEnd), date, false);

                    // prepare future law after transition
                    getActivated().addValidAfter(getFuture(), transitionEnd, false);

                    // notify about the switch
                    if (getSwitchHandler() != null) {
                        getSwitchHandler().switchOccurred(getPast(), getFuture(), s);
                    }

                    return getDetector().getHandler().eventOccurred(s, getDetector(), increasing);

                } else {

                    // estimate state at transition start, according to the past attitude law
                    final double dt = -transitionTime;
                    final AbsoluteDate shiftedDate = date.shiftedBy(dt);
                    SpacecraftState sState;
                    if (s.isOrbitDefined()) {
                        final Orbit     sOrbit    = s.getOrbit().shiftedBy(dt);
                        final Attitude  sAttitude = getPast().getAttitude(sOrbit, shiftedDate, s.getFrame());
                        sState    = new SpacecraftState(sOrbit, sAttitude, s.getMass());
                    } else {
                        final AbsolutePVCoordinates sAPV    = s.getAbsPVA().shiftedBy(dt);
                        final Attitude  sAttitude = getPast().getAttitude(sAPV, shiftedDate, s.getFrame());
                        sState    = new SpacecraftState(sAPV, sAttitude, s.getMass());
                    }
                    for (final DoubleArrayDictionary.Entry entry : s.getAdditionalStatesValues().getData()) {
                        sState = sState.addAdditionalState(entry.getKey(), entry.getValue());
                    }

                    // prepare transition
                    getActivated().addValidBefore(new TransitionProvider(sState.getAttitude(), date), date, false);

                    // prepare past law before transition
                    getActivated().addValidBefore(getPast(), shiftedDate, false);

                    // notify about the switch
                    if (getSwitchHandler() != null) {
                        getSwitchHandler().switchOccurred(getFuture(), getPast(), sState);
                    }

                    return getDetector().getHandler().eventOccurred(sState, getDetector(), increasing);

                }

            } else {
                // trigger the underlying event despite no attitude switch occurred
                return getDetector().getHandler().eventOccurred(s, getDetector(), increasing);
            }

        }

        /** Provider for transition phases.
         * @since 9.2
         */
        private class TransitionProvider implements AttitudeProvider {

            /** Attitude at preceding transition. */
            private final Attitude transitionPreceding;

            /** Date of final switch to following attitude law. */
            private final AbsoluteDate transitionEnd;

            /** Simple constructor.
             * @param transitionPreceding attitude at preceding transition
             * @param transitionEnd date of final switch to following attitude law
             */
            TransitionProvider(final Attitude transitionPreceding, final AbsoluteDate transitionEnd) {
                this.transitionPreceding = transitionPreceding;
                this.transitionEnd       = transitionEnd;
            }

            /** {@inheritDoc} */
            public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                        final AbsoluteDate date, final Frame frame) {

                // Create sample
                final TimeStampedAngularCoordinates start =
                        transitionPreceding.withReferenceFrame(frame).getOrientation();
                final TimeStampedAngularCoordinates end =
                        getFuture().getAttitude(pvProv, transitionEnd, frame).getOrientation();
                final List<TimeStampedAngularCoordinates> sample =  Arrays.asList(start, end);

                // Create interpolator
                final TimeInterpolator<TimeStampedAngularCoordinates> interpolator =
                        new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), transitionFilter);

                // interpolate between the two boundary attitudes
                final TimeStampedAngularCoordinates interpolated = interpolator.interpolate(date, sample);

                return new Attitude(frame, interpolated);

            }

            /** {@inheritDoc} */
            public <S extends CalculusFieldElement<S>> FieldAttitude<S> getAttitude(final FieldPVCoordinatesProvider<S> pvProv,
                                                                                    final FieldAbsoluteDate<S> date,
                                                                                    final Frame frame) {

                // create sample
                final TimeStampedFieldAngularCoordinates<S> start =
                        new TimeStampedFieldAngularCoordinates<>(date.getField(),
                                                                 transitionPreceding.withReferenceFrame(frame).getOrientation());
                final TimeStampedFieldAngularCoordinates<S> end =
                        getFuture().getAttitude(pvProv,
                                           new FieldAbsoluteDate<>(date.getField(), transitionEnd),
                                           frame).getOrientation();
                final List<TimeStampedFieldAngularCoordinates<S>> sample = Arrays.asList(start, end);

                // create interpolator
                final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<S>, S> interpolator =
                        new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(sample.size(), transitionFilter);

                // interpolate between the two boundary attitudes
                final TimeStampedFieldAngularCoordinates<S> interpolated = interpolator.interpolate(date, sample);

                return new FieldAttitude<>(frame, interpolated);
            }

        }

    }

}
