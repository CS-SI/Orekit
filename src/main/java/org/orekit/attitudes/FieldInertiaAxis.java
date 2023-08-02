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


/** Container for inertial axis.
 * <p>
 * Instances of this class are immutable
 * </p>
 * @param <T> type fof the field elements
 * @author Luc Maisonobe
 * @since 12.0
 */
public class FieldInertiaAxis<T extends CalculusFieldElement<T>> {

    /** Moment of inertia. */
    private final T i;

    /** Inertia axis. */
    private final FieldVector3D<T> a;

    /** Simple constructor to pair a moment of inertia with its associated axis.
     * @param i moment of inertia
     * @param a inertia axis
     */
    public FieldInertiaAxis(final T i, final FieldVector3D<T> a) {
        this.i = i;
        this.a = a;
    }

    /** Reverse the inertia axis.
     * @return new container with reversed axis
     */
    public FieldInertiaAxis<T> negate() {
        return new FieldInertiaAxis<>(i, a.negate());
    }

    /** Get the moment of inertia.
     * @return moment of inertia
     */
    public T getI() {
        return i;
    }

    /** Get the inertia axis.
     * @return inertia axis
     */
    public FieldVector3D<T> getA() {
        return a;
    }

}
