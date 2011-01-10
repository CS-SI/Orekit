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
    package org.orekit.propagation.events;

    import java.io.Serializable;

import org.orekit.propagation.SpacecraftState;

/** Container for occurred event during propagation.
 * <p>This class is a container for an event occurring during propagation. 
 * It allows to store spacecraft state at event occurrence and the event detector responsible for it.</p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class OccurredEvent implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2539406732889277836L;

    /** State at event occurrence. */
    private final SpacecraftState state;

    /** Event detector that triggered the event. */
    private final EventDetector detector;

    /** Build a new occurred event.
     * @param state spacecraft state at event occurrence
     * @param detector event detector that triggered the event
     */
    public OccurredEvent(final SpacecraftState state, final EventDetector detector) {
        this.state = state;
        this.detector = detector;
    }

    /** Get the spacecraft state at event occurrence.
     * @return the spacecraft state at event occurrence
     */
    public SpacecraftState getState() {
        return state;
    }

    /** Get the event detector responsible for event occurrence.
     * @return the event detector responsible for event occurrence
     */
    public EventDetector getDetector() {
        return detector;
    }

}
