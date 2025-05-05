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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** This classes manages a sequence of different attitude providers that are activated
 * in turn according to switching events. Changes in attitude mode are instantaneous, so state derivatives need to be
 * reset and the {@link Action} returned by the event handler is ignored.
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 13.0
 * @see AttitudesSequence
 */
public class AttitudesSwitcher extends AbstractSwitchingAttitudeProvider {

    /** Switching events list. */
    private final List<InstantaneousSwitch> instantaneousSwitches;

    /** Constructor for an initially empty sequence.
     */
    public AttitudesSwitcher() {
        super();
        instantaneousSwitches = new ArrayList<>();
    }

    /** Add a switching condition between two attitude providers.
     * <p>
     * The {@code past} and {@code future} attitude providers are defined with regard
     * to the natural flow of time. This means that if the propagation is forward, the
     * propagator will switch from {@code past} provider to {@code future} provider at
     * event occurrence, but if the propagation is backward, the propagator will switch
     * from {@code future} provider to {@code past} provider at event occurrence.
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
     * occur <em>only</em> at eclipse entry.
     * </p>
     * @param past attitude provider applicable for times in the switch event occurrence past
     * @param future attitude provider applicable for times in the switch event occurrence future
     * @param switchEvent event triggering the attitude providers switch
     * @param switchOnIncrease if true, switch is triggered on increasing event
     * @param switchOnDecrease if true, switch is triggered on decreasing event
     * @param switchHandler handler to call for notifying when switch occurs (may be null)
     * @param <T> class type for the switch event
     * @since 13.0
     */
    public <T extends EventDetector> void addSwitchingCondition(final AttitudeProvider past,
                                                                final AttitudeProvider future,
                                                                final T switchEvent,
                                                                final boolean switchOnIncrease,
                                                                final boolean switchOnDecrease,
                                                                final AttitudeSwitchHandler switchHandler) {

        // if it is the first switching condition, assume first active law is the past one
        if (getActivated() == null) {
            resetActiveProvider(past);
        }

        // add the switching condition
        instantaneousSwitches.add(new InstantaneousSwitch(switchEvent, switchOnIncrease, switchOnDecrease,
                                past, future, switchHandler));

    }

    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.concat(instantaneousSwitches.stream().map(InstantaneousSwitch.class::cast), getEventDetectors(getParametersDrivers()));
    }

    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final Stream<FieldEventDetector<T>> switchesStream = instantaneousSwitches.stream().map(sw -> getFieldEventDetector(field, sw));
        return Stream.concat(switchesStream, getFieldEventDetectors(field, getParametersDrivers()));
    }

    /** Switch specification. Reset derivatives due to instantaneous change of attitude. */
    public class InstantaneousSwitch extends AbstractAttitudeSwitch {

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
         * @param switchHandler handler to call for notifying when switch occurs (may be null)
         */
        private InstantaneousSwitch(final EventDetector event, final boolean switchOnIncrease, final boolean switchOnDecrease,
                                    final AttitudeProvider past, final AttitudeProvider future,
                                    final AttitudeSwitchHandler switchHandler) {
            super(event, switchOnIncrease, switchOnDecrease, past, future, switchHandler);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            super.init(s0, t);

            // reset the transition parameters (this will be done once for each switch,
            //  despite doing it only once would have sufficient; it's not really a problem)
            forward = t.durationFrom(s0.getDate()) >= 0.0;
            if (getActivated().getSpansNumber() > 1) {
                // remove switches that will be overridden during upcoming propagation (use margin to avoid erasing after creation)
                if (forward) {
                    setActivated(getActivated().extractRange(AbsoluteDate.PAST_INFINITY, s0.getDate().shiftedBy(getDetectionSettings().getThreshold())));
                } else {
                    setActivated(getActivated().extractRange(s0.getDate().shiftedBy(-getDetectionSettings().getThreshold()), AbsoluteDate.FUTURE_INFINITY));
                }
            }

        }

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {

            final AbsoluteDate date = s.getDate();
            if (getActivated().get(date) == (forward ? getPast() : getFuture()) &&
                (increasing && isSwitchOnIncrease() || !increasing && isSwitchOnDecrease())) {

                if (forward) {
                    // prepare future law
                    getActivated().addValidAfter(getFuture(), date, false);

                    // notify about the switch
                    if (getSwitchHandler() != null) {
                        getSwitchHandler().switchOccurred(getPast(), getFuture(), s);
                    }

                } else {
                    // prepare past law
                    getActivated().addValidBefore(getPast(), date, false);

                    // notify about the switch
                    if (getSwitchHandler() != null) {
                        getSwitchHandler().switchOccurred(getFuture(), getPast(), s);
                    }

                }

            }
            getDetector().getHandler().eventOccurred(s, getDetector(), increasing);  // call but ignore output
            return Action.RESET_DERIVATIVES;
        }

    }

}
