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
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.BodyShape;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Class for longitude value-crossing event function.
 * It is negative under the critical value. It is also centered around it and bounded.
 * @author Romain Serra
 * @since 14.0
 */
public class LongitudeValueCrossingFunction extends AbstractGeodeticEventFunction {

    /** Critical longitude for crossing event. */
    private final double criticalLongitude;

    /** Constructor.
     * @param body body
     * @param criticalLongitude longitude for crossing
     */
    public LongitudeValueCrossingFunction(final BodyShape body, final double criticalLongitude) {
        super(body);
        this.criticalLongitude = criticalLongitude;
    }

    @Override
    public double value(final SpacecraftState state) {
        final double longitude = getBodyShape().getLongitude(state.getPosition(), state.getFrame(), state.getDate());
        return MathUtils.normalizeAngle(longitude, criticalLongitude) - criticalLongitude;
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        final T longitude = getBodyShape().getLongitude(fieldState.getPosition(), fieldState.getFrame(), fieldState.getDate());
        return MathUtils.normalizeAngle(longitude, longitude.newInstance(criticalLongitude)).subtract(criticalLongitude);
    }

    /**
     * Getter for critical longitude.
     * @return longitude
     */
    public double getCriticalLongitude() {
        return criticalLongitude;
    }
}
