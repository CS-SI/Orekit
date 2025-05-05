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

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldPVCoordinates;

/** Detector of local extrema with angular separation.
 * @author Romain Serra
 * @see AngularSeparationDetector
 * @since 13.1
 */
public class ExtremumAngularSeparationDetector extends AbstractDetector<ExtremumAngularSeparationDetector> {

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
    public ExtremumAngularSeparationDetector(final EventDetectionSettings detectionSettings,
                                             final EventHandler handler,
                                             final ExtendedPositionProvider beacon,
                                                final ExtendedPositionProvider observer) {
        super(detectionSettings, handler);
        this.beacon         = beacon;
        this.observer       = observer;
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
    protected ExtremumAngularSeparationDetector create(final EventDetectionSettings detectionSettings,
                                                       final EventHandler newHandler) {
        return new ExtremumAngularSeparationDetector(detectionSettings, newHandler, beacon, observer);
    }

    @Override
    public double g(final SpacecraftState s) {
        final FieldPVCoordinates<UnivariateDerivative1> pv = s.getPVCoordinates().toUnivariateDerivative1PV();
        final UnivariateDerivative1 dt = new UnivariateDerivative1(0., 1.);
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(),
                s.getDate()).shiftedBy(dt);
        final FieldVector3D<UnivariateDerivative1> bP = beacon.getPosition(fieldDate, s.getFrame());
        final FieldVector3D<UnivariateDerivative1> oP = observer.getPosition(fieldDate, s.getFrame());
        final UnivariateDerivative1 separation = FieldVector3D.angle(pv.getPosition().subtract(oP), bP.subtract(oP));
        return separation.getFirstDerivative();
    }
}
