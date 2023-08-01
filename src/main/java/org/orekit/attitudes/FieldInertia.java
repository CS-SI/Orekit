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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;

/** Container for inertia of a 3D object.
 * <p>
 * Instances of this class are immutable
 * </p>
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.0
 */
public class FieldInertia<T extends CalculusFieldElement<T>> {

    /** Inertia along first axis. */
    private final FieldInertiaAxis<T> iA1;

    /** Inertia along second axis. */
    private final FieldInertiaAxis<T> iA2;

    /** Inertia along third axis. */
    private final FieldInertiaAxis<T> iA3;

    /** Simple constructor from principal axes.
     * @param iA1 inertia along first axis
     * @param iA2 inertia along second axis
     * @param iA3 inertia along third axis
     */
    FieldInertia(final FieldInertiaAxis<T> iA1, final FieldInertiaAxis<T> iA2, final FieldInertiaAxis<T> iA3) {
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
    public FieldInertia<T> swap12() {
        return new FieldInertia<>(iA2, iA1, iA3.negate());
    }

    /** Swap axes 1 and 3.
     * <p>
     * The instance is unchanged.
     * </p>
     * @return inertia with swapped axes
     */
    public FieldInertia<T> swap13() {
        return new FieldInertia<>(iA3, iA2.negate(), iA1);
    }

    /** Swap axes 2 and 3.
     * <p>
     * The instance is unchanged.
     * </p>
     * @return inertia with swapped axes
     */
    public FieldInertia<T> swap23() {
        return new FieldInertia<>(iA1.negate(), iA3, iA2);
    }

    /** Get inertia along first axis.
     * @return inertia along first axis
     */
    public FieldInertiaAxis<T> getInertiaAxis1() {
        return iA1;
    }

    /** Get inertia along second axis.
     * @return inertia along second axis
     */
    public FieldInertiaAxis<T> getInertiaAxis2() {
        return iA2;
    }

    /** Get inertia along third axis.
     * @return inertia along third axis
     */
    public FieldInertiaAxis<T> getInertiaAxis3() {
        return iA3;
    }

    /** Compute angular momentum.
     * @param rotationRate rotation rate in body frame.
     * @return angular momentum in body frame
     */
    public FieldVector3D<T> momentum(final FieldVector3D<T> rotationRate) {
        final FieldVector3D<T> a1 = iA1.getA();
        final FieldVector3D<T> a2 = iA2.getA();
        final FieldVector3D<T> a3 = iA3.getA();
        return new FieldVector3D<>(iA1.getI().multiply(FieldVector3D.dotProduct(rotationRate, a1)), a1,
                                   iA2.getI().multiply(FieldVector3D.dotProduct(rotationRate, a2)), a2,
                                   iA3.getI().multiply(FieldVector3D.dotProduct(rotationRate, a3)), a3);
    }

}
