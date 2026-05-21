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
import org.orekit.bodies.BodyShape;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** Class for longitude extremum event function.
 * @author Romain Serra
 * @since 14.0
 */
public class LongitudeExtremumEventFunction extends AbstractGeodeticExtremumEventFunction {

    /** Constructor.
     * @param body body
     */
    public LongitudeExtremumEventFunction(final BodyShape body) {
        super(body);
    }

    @Override
    public double value(final SpacecraftState state) {
        final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
        final UnivariateDerivative1 dt = new UnivariateDerivative1(0, 1);
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(field, state.getDate()).shiftedBy(dt);
        final FieldVector3D<UnivariateDerivative1> position = state.getPVCoordinates().toUnivariateDerivative1Vector();
        return getBodyShape().getLongitude(position, state.getFrame(), fieldDate).getFirstDerivative();
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        final FieldAbsoluteDate<FieldUnivariateDerivative1<T>> fud1Date = fieldState.getDate().toFUD1Field();
        final FieldVector3D<FieldUnivariateDerivative1<T>> fieldPosition = fieldState.getPVCoordinates().toUnivariateDerivative1Vector();
        return getBodyShape().getLongitude(fieldPosition, fieldState.getFrame(), fud1Date).getFirstDerivative();
    }
}
