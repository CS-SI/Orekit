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

import org.orekit.bodies.BodyShape;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.handlers.EventHandler;

/** Abstract class for detectors using a body shape.
 *
 * @author Romain Serra
 * @since 14.0
 * @see BodyShape
 */
public abstract class AbstractGeographicalDetector<T extends AbstractDetector<T>> extends AbstractDetector<T> {

    /** Body shape. */
    private final BodyShape bodyShape;

    /** Protected constructor with full parameters.
     * @param eventFunction event function
     * @param detectionSettings detection settings
     * @param handler event handler to call at event occurrences
     * @param bodyShape body shape with respect to which altitude should be evaluated
     * @since 14.0
     */
    protected AbstractGeographicalDetector(final EventFunction eventFunction,
                                           final EventDetectionSettings detectionSettings,
                                           final EventHandler handler,
                                           final BodyShape bodyShape) {
        super(eventFunction, detectionSettings, handler);
        this.bodyShape = bodyShape;
    }

    /** Protected constructor.
     * @param detectionSettings detection settings
     * @param handler event handler to call at event occurrences
     * @param bodyShape body shape with respect to which altitude should be evaluated
     * @since 13.0
     */
    protected AbstractGeographicalDetector(final EventDetectionSettings detectionSettings, final EventHandler handler,
                                           final BodyShape bodyShape) {
        super(detectionSettings, handler);
        this.bodyShape = bodyShape;
    }

    /** Get the body shape.
     * @return the body shape
     */
    public BodyShape getBodyShape() {
        return bodyShape;
    }

}
