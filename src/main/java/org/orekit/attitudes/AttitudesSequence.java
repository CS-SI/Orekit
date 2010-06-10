/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.attitudes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;

/** This classes manages a sequence of different attitude laws that are activated
 * in rows according to switching events.
 * <p>Only one attitude law in the sequence is in an active state. When one of
 * the switch event associated with the active law occurs, the active law becomes
 * the one specified with the event. A simple example is a law for the sun lighted part
 * of the orbit and another law for the eclipse time. When the sun lighted law is active,
 * the eclipse entry event is checked and when it occurs the eclipse law is activated.
 * When the eclipse law is active, the eclipse exit event is checked and when it occurs
 * the sun lighted law is activated again. This sequence is a simple loop.</p>
 * <p>An active attitude law may have several switch events and next law settings, leading
 * to different activation patterns depending on which events are triggered first. An example
 * of this feature is handling switches to safe mode if some contingency condition is met, in
 * addition to the nominal switches that correspond to proper operations. Another example
 * is handling of maneuver mode.<p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class AttitudesSequence implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 5140034224175180354L;

    /** Active law. */
    private AttitudeLaw active;

    /** Switching events map. */
    private final Map<AttitudeLaw, Collection<Switch>> switchingMap;

    /** Constructor for an initially empty sequence.
     */
    public AttitudesSequence() {
        active = null;
        switchingMap = new HashMap<AttitudeLaw, Collection<Switch>>();
    }

    /** Reset the active law.
     * @param law law to activate
     */
    public void resetActiveLaw(final AttitudeLaw law) {

        // add the law if not already known
        if (!switchingMap.containsKey(law)) {
            switchingMap.put(law, new ArrayList<Switch>());
        }

        active = law;

    }

    /** Register all wrapped switch events to the propagator.
     * <p>
     * This method must be called once before propagation, after the
     * switching conditions have been set up by calls to {@link
     * #addSwitchingCondition(AttitudeLaw, EventDetector, boolean, boolean, AttitudeLaw)}.
     * </p>
     * @param propagator propagator that will handle the events
     */
    public void registerSwitchEvents(final Propagator propagator) {
        for (final Collection<Switch> collection : switchingMap.values()) {
            for (final Switch s : collection) {
                propagator.addEventDetector(s);
            }
        }
    }

    /** Add a switching condition between two attitude laws.
     * <p>
     * An attitude law may have several different switch events associated to
     * it. Depending on which event is triggered, the appropriate law is
     * switched to.
     * </p>
     * <p>
     * The switch events specified here must <em>not</em> be registered to the
     * propagator directly. The proper way to register these events is to
     * call {@link #registerSwitchEvents(Propagator)} once after all switching
     * conditions have been set up. The reason for this is that the events will
     * be wrapped before being registered.
     * </p>
     * @param before attitude law before the switch event occurrence
     * @param switchEvent event triggering the attitude laws switch (may be null
     * for a law without any ending condition, in this case the after law is not
     * referenced and may be null too)
     * @param switchOnIncrease if true, switch is triggered on increasing event
     * @param switchOnDecrease if true, switch is triggered on decreasing event
     * @param after attitude law to activate after the switch event occurrence
     * (used only if switchEvent is non null)
     */
    public void addSwitchingCondition(final AttitudeLaw before,
                                      final EventDetector switchEvent,
                                      final boolean switchOnIncrease,
                                      final boolean switchOnDecrease,
                                      final AttitudeLaw after) {

        // add the before law if not already known
        if (!switchingMap.containsKey(before)) {
            switchingMap.put(before, new ArrayList<Switch>());
            if (active == null) {
                active = before;
            }
        }

        if (switchEvent != null) {

            // add the after law if not already known
            if (!switchingMap.containsKey(after)) {
                switchingMap.put(after, new ArrayList<Switch>());
            }

            // add the switching condition
            switchingMap.get(before).add(new Switch(switchEvent, switchOnIncrease, switchOnDecrease, after));

        }

    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final Orbit orbit) throws OrekitException {
        // delegate attitude computation to the active law
        return active.getAttitude(orbit);
    }

    /** Switch specification. */
    private class Switch implements EventDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -668295773303559063L;

        /** Event. */
        private final EventDetector event;

        /** Event direction triggering the switch. */
        private final boolean switchOnIncrease;

        /** Event direction triggering the switch. */
        private final boolean switchOnDecrease;

        /** Next attitude law. */
        private final AttitudeLaw next;

        /** Simple constructor.
         * @param event event
         * @param switchOnIncrease if true, switch is triggered on increasing event
         * @param switchOnDecrease if true, switch is triggered on decreasing event
         * otherwise switch is triggered on decreasing event
         * @param next next attitude law
         */
        public Switch(final EventDetector event,
                      final boolean switchOnIncrease,
                      final boolean switchOnDecrease,
                      final AttitudeLaw next) {
            this.event            = event;
            this.switchOnIncrease = switchOnIncrease;
            this.switchOnDecrease = switchOnDecrease;
            this.next             = next;
        }

        /** {@inheritDoc} */
        @Override
        public int eventOccurred(final SpacecraftState s, final boolean increasing)
            throws OrekitException {

            if ((increasing && switchOnIncrease) || (!increasing && switchOnDecrease)) {
                // switch to next attitude law
                active = next;
            }

            return event.eventOccurred(s, increasing);

        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState s)
            throws OrekitException {
            return event.g(s);
        }

        /** {@inheritDoc} */
        @Override
        public double getMaxCheckInterval() {
            return event.getMaxCheckInterval();
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxIterationCount() {
            return event.getMaxIterationCount();
        }

        /** {@inheritDoc} */
        @Override
        public double getThreshold() {
            return event.getThreshold();
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState)
            throws OrekitException {
            return event.resetState(oldState);
        }

    }

}
