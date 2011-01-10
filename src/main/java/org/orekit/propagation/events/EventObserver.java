/* Copyright 2010 Centre National d'Études Spatiales
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
package org.orekit.propagation.events;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;

/** This interface represents an observer for occurred events.
 *
 * <p>During propagation, various event detectors can trigger events, through the
 * {@link EventDetector#eventOccurred(SpacecraftState, boolean) eventOccurred} method.
 * Each event type has its specific implementation of the {@link
 * EventDetector#eventOccurred(SpacecraftState, boolean) eventOccurred} method.
 * This observer allows to be notified when events are triggered, with the unique
 * {@link #notify(SpacecraftState, EventDetector) notify} method being used for all events.
 * </p>
 *
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 * @since 5.1
 */
public interface EventObserver extends Serializable {

    /** Notify that an event has occurred.
     * This method allows to add an occurred event to the occurred events
     * list handled by the propagator.
     * @param s the current state information: date, kinematics, attitude
     * @param detector the event detector that triggered the event
     * @exception OrekitException if some specific error occurs
     */
    void notify(SpacecraftState s, EventDetector detector) throws OrekitException;

}
