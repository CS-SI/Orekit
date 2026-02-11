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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Event function for crossing a 2D plane in 3D space, as defined by a normal vector.
 * The plane is assumed to contain the origin of the frame where the normal's coordinates are given.
 * The event function is negative under the plane according to the normal's orientation.
 * @author Romain Serra
 * @since 14.0
 */
public class PlaneCrossingFunction implements EventFunction {

    /** Plane normal vector. */
    private final Vector3D planeNormal;

    /** Plane reference frame. */
    private final Frame planeFrame;

    /** Constructor.
     * @param normal plane normal
     * @param frame plane frame
     */
    public PlaneCrossingFunction(final Vector3D normal, final Frame frame) {
        this.planeNormal = normal.normalize();
        this.planeFrame = frame;
    }

    /**
     * Getter for the plane frame.
     * @return frame
     */
    public Frame getPlaneFrame() {
        return planeFrame;
    }

    /**
     * Getter for the plane normal vector.
     * @return normal
     */
    public Vector3D getPlaneNormal() {
        return planeNormal;
    }

    @Override
    public double value(final SpacecraftState state) {
        return state.getPosition(planeFrame).dotProduct(planeNormal);
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        return fieldState.getPosition(planeFrame).dotProduct(planeNormal);
    }

}
