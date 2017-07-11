/* Copyright 2013 Applied Defense Solutions, Inc.
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
package org.orekit.propagation.events.handlers;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;


/** Handle a detection event and choose what to do next.
 * <p>The implementation behavior is to {@link
 * EventHandler.Action#CONTINUE continue} propagation when ascending and to
 * {@link EventHandler.Action#STOP stop} propagation when descending.</p>
 *
 * @author Hank Grabowski
 *
 * @param <T> class type for the generic version
 * @since 6.1
 */
public class StopOnDecreasing <T extends EventDetector> implements EventHandler<T>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20160321L;

    /** Handle a detection event and choose what to do next.
     * <p>The implementation behavior is to {@link
     * EventHandler.Action#CONTINUE continue} propagation when ascending and to
     * {@link EventHandler.Action#STOP stop} propagation when descending.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param detector the detector object calling this method (not used in the evaluation)
     * @param increasing if true, the value of the switching function increases
     * when times increases around event
     * @return {@link EventHandler.Action#STOP} or {@link EventHandler.Action#CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public Action eventOccurred(final SpacecraftState s, final T detector, final boolean increasing)
        throws OrekitException {
        return increasing ? Action.CONTINUE : Action.STOP;
    }

}
