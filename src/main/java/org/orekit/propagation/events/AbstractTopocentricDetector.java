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
package org.orekit.propagation.events;

import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.handlers.EventHandler;

/** Abstract class for detectors using a topocentric frame.
 *
 * @author Romain Serra
 * @since 13.1
 * @see TopocentricFrame
 */
public abstract class AbstractTopocentricDetector<T extends AbstractDetector<T>> extends AbstractDetector<T> {

    /** Reference topocentric frame. */
    private final TopocentricFrame topocentricFrame;

    /** Protected constructor with full parameters.
     * @param eventFunction event function
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param topocentricFrame topocentric frame
     * @since 14.0
     */
    protected AbstractTopocentricDetector(final EventFunction eventFunction,
                                          final EventDetectionSettings detectionSettings, final EventHandler handler,
                                          final TopocentricFrame topocentricFrame) {
        super(eventFunction, detectionSettings, handler);
        this.topocentricFrame = topocentricFrame;
    }

    /**
     * Getter for the topocentric frame.
     * @return frame
     */
    public TopocentricFrame getTopocentricFrame() {
        return topocentricFrame;
    }


    /** Get the elevation value.
     * @param s the current state information: date, kinematics, attitude
     * @return spacecraft elevation
     */
    public double getElevation(final SpacecraftState s) {
        return getTopocentricFrame().getElevation(s.getPosition(), s.getFrame(), s.getDate());
    }
}
