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
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Abstract class for event function related to geodetic coordinates.
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractGeodeticEventFunction implements EventFunction {

    /** Body shape. */
    private final BodyShape bodyShape;

    /** Constructor.
     * @param body body
     */
    protected AbstractGeodeticEventFunction(final BodyShape body) {
        this.bodyShape = body;
    }

    /**
     * Getter for body shape.
     * @return body
     */
    public BodyShape getBodyShape() {
        return bodyShape;
    }

    /** Compute the geodetic coordinates.
     * @param s the current state information: date, kinematics, attitude
     * @return geodetic point
     */
    protected GeodeticPoint transformToGeodeticPoint(final SpacecraftState s) {
        return getBodyShape().transform(s.getPosition(), s.getFrame(), s.getDate());
    }

    /** Compute the geodetic coordinates (Field version).
     * @param s the current state information: date, kinematics, attitude
     * @param <T> field type
     * @return geodetic point
     */
    protected <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> transformToGeodeticPoint(final FieldSpacecraftState<T> s) {
        return getBodyShape().transform(s.getPosition(), s.getFrame(), s.getDate());
    }
}
