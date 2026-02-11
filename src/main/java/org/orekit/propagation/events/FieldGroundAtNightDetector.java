/* Copyright 2002-2026 CS GROUP
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
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.functions.EventFunctionModifier;
import org.orekit.propagation.events.functions.GroundAtNightEventFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.utils.ExtendedPositionProvider;


/** Detector for ground location being at night.
 * <p>
 * This detector is mainly useful for scheduling optical measurements
 * (either passive telescope observation of satellites against the stars background
 *  or active satellite laser ranging).
 * </p>
 * <p>
 * The {@code g} function of this detector is positive when ground is at night
 * (i.e. Sun is below dawn/dusk elevation angle).
 * </p>
 * @author Luc Maisonobe
 * @author Romain Serra
 * @see GroundAtNightDetector
 * @since 13.1
 */
public class FieldGroundAtNightDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractTopocentricDetector<FieldGroundAtNightDetector<T>, T> {

    /** Provider for Sun position. */
    private final ExtendedPositionProvider sun;

    /** Sun elevation below which we consider night is dark enough. */
    private final T dawnDuskElevation;

    /** Atmospheric Model used for calculations, if defined. */
    private final AtmosphericRefractionModel refractionModel;

    /** Simple constructor.
     * @param topocentricFrame ground location to check
     * @param sun provider for Sun position
     * @param dawnDuskElevation Sun elevation below which we consider night is dark enough (rad)
     * @param refractionModel reference to refraction model (null if refraction should be ignored)
     */
    public FieldGroundAtNightDetector(final TopocentricFrame topocentricFrame, final ExtendedPositionProvider sun,
                                      final T dawnDuskElevation,
                                      final AtmosphericRefractionModel refractionModel) {
        this(topocentricFrame, sun, dawnDuskElevation, refractionModel, new FieldEventDetectionSettings<>(dawnDuskElevation.getField(),
                EventDetectionSettings.getDefaultEventDetectionSettings()), new FieldContinueOnEvent<>());
    }

    /** Private constructor.
     * @param topocentricFrame ground location from which measurement is performed
     * @param sun provider for Sun position
     * @param dawnDuskElevation Sun elevation below which we consider night is dark enough (rad)
     * @param refractionModel reference to refraction model (null if refraction should be ignored),
     * @param detectionSettings event detection settings
     * @param handler   event handler to call at event occurrences
     */
    protected FieldGroundAtNightDetector(final TopocentricFrame topocentricFrame, final ExtendedPositionProvider sun,
                                         final T dawnDuskElevation,
                                         final AtmosphericRefractionModel refractionModel,
                                         final FieldEventDetectionSettings<T> detectionSettings,
                                         final FieldEventHandler<T> handler) {
        super(EventFunctionModifier.addFieldValue(new GroundAtNightEventFunction(topocentricFrame, sun,
                        dawnDuskElevation.getReal(), refractionModel), dawnDuskElevation.getAddendum()),
                detectionSettings, handler, topocentricFrame);
        this.sun               = sun;
        this.dawnDuskElevation = dawnDuskElevation;
        this.refractionModel   = refractionModel;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldGroundAtNightDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                   final FieldEventHandler<T> newHandler) {
        return new FieldGroundAtNightDetector<>(getTopocentricFrame(), sun, dawnDuskElevation, refractionModel,
                detectionSettings, newHandler);
    }

    /** {@inheritDoc}
     * <p>
     * The {@code g} function of this detector is positive when ground is at night
     * (i.e. Sun is below dawn/dusk elevation angle).
     * </p>
     * <p>
     * This function only depends on date, not on the actual position of the spacecraft.
     * </p>
     */
    @Override
    public T g(final FieldSpacecraftState<T> state) {
        return getEventFunction().value(state);
    }

    @Override
    public GroundAtNightDetector toEventDetector(final EventHandler eventHandler) {
        return new GroundAtNightDetector(new GroundAtNightEventFunction(getTopocentricFrame(), sun, dawnDuskElevation.getReal(),
                refractionModel), getDetectionSettings().toEventDetectionSettings(), eventHandler);
    }
}
