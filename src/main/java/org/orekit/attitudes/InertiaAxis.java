/* Copyright 2023 Luc Maisonobe
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
package org.orekit.attitudes;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

/** Container for inertial axis.
 * <p>
 * Instances of this class are immutable
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class InertiaAxis {

    /** Moment of inertia. */
    private final double i;

    /** Inertia axis. */
    private final Vector3D a;

    /** Simple constructor to pair a moment of inertia with its associated axis.
     * @param i moment of inertia
     * @param a inertia axis
     */
    InertiaAxis(final double i, final Vector3D a) {
        this.i = i;
        this.a = a;
    }

    /** Reverse the inertia axis.
     * @return new container with reversed axis
     */
    public InertiaAxis negate() {
        return new InertiaAxis(i, a.negate());
    }

    /** Get the moment of inertia.
     * @return moment of inertia
     */
    public double getI() {
        return i;
    }

    /** Get the inertia axis.
     * @return inertia axis
     */
    public Vector3D getA() {
        return a;
    }

}
