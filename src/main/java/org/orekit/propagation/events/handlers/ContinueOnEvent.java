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


/**
 * Event handler which will always return {@link EventHandler.Action#CONTINUE continue} as a state.
 * @author Hank Grabowski
 *
 * @param <T> class type for the generic version
 * @since 6.1
 */
public class ContinueOnEvent<T extends EventDetector> implements EventHandler<T>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20160321L;

    /**
     * Specific implementation of the eventOccurred interface.
     *
     * @param s SpaceCraft state to be used in the evaluation
     * @param detector object with appropriate type that can be used in determining correct return state
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @return {@link EventHandler.Action#CONTINUE continue} under all circumstances
     * @exception OrekitException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final T detector, final boolean increasing)
        throws OrekitException {
        return Action.CONTINUE;
    }

}
