/* Copyright 2022-2025 Romain Serra
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
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldPVCoordinates;

/** Detector of local extrema with angular separation.
 * @author Romain Serra
 * @see FieldAngularSeparationDetector
 * @since 13.1
 */
public class FieldExtremumAngularSeparationDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldExtremumAngularSeparationDetector<T>, T> {

    /** Beacon at the center of the proximity zone. */
    private final ExtendedPositionProvider beacon;

    /** Observer for the spacecraft, that may also see the beacon at the same time if they are too close. */
    private final ExtendedPositionProvider observer;

    /** Protected constructor with full parameters.
     * @param detectionSettings detection settings
     * @param handler event handler to call at event occurrences
     * @param beacon beacon at the center of the proximity zone
     * @param observer observer for the spacecraft, that may also see
     * the beacon at the same time if they are too close to each other
     */
    public FieldExtremumAngularSeparationDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                                     final FieldEventHandler<T> handler,
                                                     final ExtendedPositionProvider beacon,
                                                     final ExtendedPositionProvider observer) {
        super(detectionSettings, handler);
        this.beacon         = beacon;
        this.observer       = observer;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldExtremumAngularSeparationDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                               final FieldEventHandler<T> newHandler) {
        return new FieldExtremumAngularSeparationDetector<>(detectionSettings, newHandler, beacon, observer);
    }

    /** Get the beacon at the center of the proximity zone.
     * @return beacon at the center of the proximity zone
     */
    public ExtendedPositionProvider getBeacon() {
        return beacon;
    }

    /** Get the observer for the spacecraft.
     * @return observer for the spacecraft
     */
    public ExtendedPositionProvider getObserver() {
        return observer;
    }

    @Override
    public T g(final FieldSpacecraftState<T> s) {
        final FieldPVCoordinates<FieldUnivariateDerivative1<T>> pv = s.getPVCoordinates().toUnivariateDerivative1PV();
        final FieldAbsoluteDate<FieldUnivariateDerivative1<T>> fieldDate = s.getDate().toFUD1Field();
        final FieldVector3D<FieldUnivariateDerivative1<T>> bP = beacon.getPosition(fieldDate, s.getFrame());
        final FieldVector3D<FieldUnivariateDerivative1<T>> oP = observer.getPosition(fieldDate, s.getFrame());
        final FieldUnivariateDerivative1<T> separation = FieldVector3D.angle(pv.getPosition().subtract(oP), bP.subtract(oP));
        return separation.getFirstDerivative();
    }

}
