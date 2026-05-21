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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

/** Event function for local extrema of angular separation. It is positive when the angle increases.
 * @author Romain Serra
 * @since 14.0
 */
public class ExtremumAngularSeparationFunction implements EventFunction {

    /** Beacon at the center of the proximity zone. */
    private final ExtendedPositionProvider beacon;

    /** Observer for the spacecraft, that may also see the beacon at the same time if they are too close. */
    private final ExtendedPositionProvider observer;

    /** Protected constructor with full parameters.
     * @param beacon beacon at the center of the proximity zone
     * @param observer observer for the spacecraft, that may also see
     * the beacon at the same time if they are too close to each other
     */
    public ExtremumAngularSeparationFunction(final ExtendedPositionProvider beacon,
                                             final ExtendedPositionProvider observer) {
        this.beacon         = beacon;
        this.observer       = observer;
    }

    @Override
    public double value(final SpacecraftState s) {
        final FieldVector3D<UnivariateDerivative1> position = s.getPVCoordinates().toUnivariateDerivative1Vector();
        final UnivariateDerivative1 dt = new UnivariateDerivative1(0., 1.);
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(),
                s.getDate()).shiftedBy(dt);
        final FieldVector3D<UnivariateDerivative1> bP = beacon.getPosition(fieldDate, s.getFrame());
        final FieldVector3D<UnivariateDerivative1> oP = observer.getPosition(fieldDate, s.getFrame());
        final UnivariateDerivative1 separation = FieldVector3D.angle(position.subtract(oP), bP.subtract(oP));
        return separation.getFirstDerivative();
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> s) {
        final FieldVector3D<FieldUnivariateDerivative1<T>> position = s.getPVCoordinates().toUnivariateDerivative1Vector();
        final FieldAbsoluteDate<FieldUnivariateDerivative1<T>> fieldDate = s.getDate().toFUD1Field();
        final FieldVector3D<FieldUnivariateDerivative1<T>> bP = beacon.getPosition(fieldDate, s.getFrame());
        final FieldVector3D<FieldUnivariateDerivative1<T>> oP = observer.getPosition(fieldDate, s.getFrame());
        final FieldUnivariateDerivative1<T> separation = FieldVector3D.angle(position.subtract(oP), bP.subtract(oP));
        return separation.getFirstDerivative();
    }

}
