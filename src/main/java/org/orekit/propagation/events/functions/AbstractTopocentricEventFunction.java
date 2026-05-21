/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.events.functions;

import org.orekit.frames.TopocentricFrame;


/**
 * Abstract class for topocentric event function.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractTopocentricEventFunction implements EventFunction {

    /** Topocentric frame. */
    private final TopocentricFrame topocentricFrame;

    /** Constructor.
     * @param topo reference to a topocentric model
     */
    protected AbstractTopocentricEventFunction(final TopocentricFrame topo) {
        this.topocentricFrame = topo;
    }

    /**
     * Getter for the topocentric frame.
     * @return frame
     */
    public TopocentricFrame getTopocentricFrame() {
        return topocentricFrame;
    }

}
