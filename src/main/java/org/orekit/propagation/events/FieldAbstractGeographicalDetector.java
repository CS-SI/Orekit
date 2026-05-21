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

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.BodyShape;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.handlers.FieldEventHandler;

/** Abstract class for detectors using a body shape.
 *
 * @author Romain Serra
 * @since 14.0
 * @see AbstractGeographicalDetector
 * @see org.orekit.bodies.BodyShape
 */
public abstract class FieldAbstractGeographicalDetector<D extends FieldAbstractDetector<D, T>, T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<D, T> {

    /** Reference body shape. */
    private final BodyShape bodyShape;

    /** Protected constructor with event function.
     * @param eventFunction event function
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param bodyShape body shape
     */
    protected FieldAbstractGeographicalDetector(final EventFunction eventFunction,
                                                final FieldEventDetectionSettings<T> detectionSettings,
                                                final FieldEventHandler<T> handler,
                                                final BodyShape bodyShape) {
        super(eventFunction, detectionSettings, handler);
        this.bodyShape = bodyShape;
    }

    /** Protected constructor.
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param bodyShape body shape
     */
    protected FieldAbstractGeographicalDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                                final FieldEventHandler<T> handler,
                                                final BodyShape bodyShape) {
        super(detectionSettings, handler);
        this.bodyShape = bodyShape;
    }

    /**
     * Getter for the body shape.
     * @return shape
     */
    public BodyShape getBodyShape() {
        return bodyShape;
    }

}
