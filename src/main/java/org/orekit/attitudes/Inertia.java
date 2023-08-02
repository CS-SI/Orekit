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

/** Container for inertia of a 3D object.
 * <p>
 * Instances of this class are immutable
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class Inertia {

    /** Inertia along first axis. */
    private final InertiaAxis iA1;

    /** Inertia along second axis. */
    private final InertiaAxis iA2;

    /** Inertia along third axis. */
    private final InertiaAxis iA3;

    /** Simple constructor from principal axes.
     * @param iA1 inertia along first axis
     * @param iA2 inertia along second axis
     * @param iA3 inertia along third axis
     */
    Inertia(final InertiaAxis iA1, final InertiaAxis iA2, final InertiaAxis iA3) {
        this.iA1 = iA1;
        this.iA2 = iA2;
        this.iA3 = iA3;
    }

    /** Swap axes 1 and 2.
     * <p>
     * The instance is unchanged.
     * </p>
     * @return inertia with swapped axes
     */
    public Inertia swap12() {
        return new Inertia(iA2, iA1, iA3.negate());
    }

    /** Swap axes 1 and 3.
     * <p>
     * The instance is unchanged.
     * </p>
     * @return inertia with swapped axes
     */
    public Inertia swap13() {
        return new Inertia(iA3, iA2.negate(), iA1);
    }

    /** Swap axes 2 and 3.
     * <p>
     * The instance is unchanged.
     * </p>
     * @return inertia with swapped axes
     */
    public Inertia swap23() {
        return new Inertia(iA1.negate(), iA3, iA2);
    }

    /** Get inertia along first axis.
     * @return inertia along first axis
     */
    public InertiaAxis getInertiaAxis1() {
        return iA1;
    }

    /** Get inertia along second axis.
     * @return inertia along second axis
     */
    public InertiaAxis getInertiaAxis2() {
        return iA2;
    }

    /** Get inertia along third axis.
     * @return inertia along third axis
     */
    public InertiaAxis getInertiaAxis3() {
        return iA3;
    }

    /** Compute angular momentum.
     * @param rotationRate rotation rate in body frame.
     * @return angular momentum in body frame
     */
    public Vector3D momentum(final Vector3D rotationRate) {
        final Vector3D a1 = iA1.getA();
        final Vector3D a2 = iA2.getA();
        final Vector3D a3 = iA3.getA();
        return new Vector3D(iA1.getI() * Vector3D.dotProduct(rotationRate, a1), a1,
                            iA2.getI() * Vector3D.dotProduct(rotationRate, a2), a2,
                            iA3.getI() * Vector3D.dotProduct(rotationRate, a3), a3);
    }

}
