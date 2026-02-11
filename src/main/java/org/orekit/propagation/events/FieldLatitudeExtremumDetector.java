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
import org.hipparchus.Field;
import org.orekit.bodies.BodyShape;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.functions.LatitudeExtremumEventFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;

/** Detector for latitude extrema.
 * @author Romain Serra
 * @since 14.0
 * @see LatitudeExtremumDetector
 * @param <T> type of the field elements
 */
public class FieldLatitudeExtremumDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractGeographicalDetector<FieldLatitudeExtremumDetector<T>, T> {

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAX_CHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param field the type of numbers to use.
     * @param body body on which the latitude is defined
     */
    public FieldLatitudeExtremumDetector(final Field<T> field, final BodyShape body) {
        this(new FieldEventDetectionSettings<>(field, EventDetectionSettings.getDefaultEventDetectionSettings()),
                new FieldStopOnIncreasing<>(), body);
    }

    /** Constructor with body shape.
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @param body body on which the latitude is defined
     * @since 13.0
     */
    public FieldLatitudeExtremumDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                         final FieldEventHandler<T> handler, final BodyShape body) {
        this(new LatitudeExtremumEventFunction(body), detectionSettings, handler);
    }

    /** Constructor with full parameters.
     * @param latitudeExtremumEventFunction event function
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @since 14.0
     */
    public FieldLatitudeExtremumDetector(final LatitudeExtremumEventFunction latitudeExtremumEventFunction,
                                         final FieldEventDetectionSettings<T> detectionSettings,
                                         final FieldEventHandler<T> handler) {
        super(latitudeExtremumEventFunction, detectionSettings, handler, latitudeExtremumEventFunction.getBodyShape());
    }

    /** {@inheritDoc} */
    @Override
    protected FieldLatitudeExtremumDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                      final FieldEventHandler<T> newHandler) {
        return new FieldLatitudeExtremumDetector<>((LatitudeExtremumEventFunction) getEventFunction(),
                detectionSettings, newHandler);
    }

    /** {@inheritDoc} */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        return getEventFunction().value(s);
    }

    @Override
    public LatitudeExtremumDetector toEventDetector(final EventHandler eventHandler) {
        return new LatitudeExtremumDetector((LatitudeExtremumEventFunction) getEventFunction(),
                getDetectionSettings().toEventDetectionSettings(), eventHandler);
    }
}
