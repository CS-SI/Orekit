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
import org.orekit.bodies.BodyShape;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Class for geodetic altitude value-crossing event function.
 * It is negative under the critical value.
 * @author Romain Serra
 * @since 14.0
 */
public class AltitudeEventFunction extends AbstractGeodeticEventFunction {

    /** Critical altitude for crossing event. */
    private final double criticalAltitude;

    /** Constructor.
     * @param body body
     * @param criticalAltitude altitude for crossing
     */
    public AltitudeEventFunction(final BodyShape body, final double criticalAltitude) {
        super(body);
        this.criticalAltitude = criticalAltitude;
    }

    @Override
    public double value(final SpacecraftState state) {
        return transformToGeodeticPoint(state).getAltitude() - criticalAltitude;
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        return transformToGeodeticPoint(fieldState).getAltitude().subtract(criticalAltitude);
    }
}
