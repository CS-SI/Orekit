/* Copyright 2002-2023 Romain Serra
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
package org.orekit.forces.maneuvers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

/** Enumerate on types of norm for 3D control vector (thrust as a force or acceleration, including an impulse)
 * at a given time. See ROSS, I. Michael. Space Trajectory Optimization and L1-norm Optimal Control Problems.
 * Modern astrodynamics, 2006, vol. 1, p. 155. For now, it is only used by {@link org.orekit.forces.maneuvers.ImpulseManeuver}.
 * <p>Note that as norms of spaces in finite dimensions, they are all equivalent in a topological sense.</p>
 * @see org.orekit.forces.maneuvers.ImpulseManeuver
 * @author Romain Serra
 * @since 12.0
 */
public enum ControlVector3DNormType {

    /** Norm 1. */
    NORM_1 {
        @Override
        public double evaluate(final Vector3D controlVector) {
            return controlVector.getNorm1();
        }

        @Override
        public <T extends CalculusFieldElement<T>> T evaluate(final FieldVector3D<T> controlVector) {
            return controlVector.getNorm1();
        }
    },

    /** Norm 2 also known as Euclidean. */
    NORM_2 {
        @Override
        public double evaluate(final Vector3D controlVector) {
            return controlVector.getNorm();
        }

        @Override
        public <T extends CalculusFieldElement<T>> T evaluate(final FieldVector3D<T> controlVector) {
            return controlVector.getNorm();
        }
    },

    /** Norm Inf also known as Max. */
    NORM_INF {
        @Override
        public double evaluate(final Vector3D controlVector) {
            return controlVector.getNormInf();
        }

        @Override
        public <T extends CalculusFieldElement<T>> T evaluate(final FieldVector3D<T> controlVector) {
            return controlVector.getNormInf();
        }
    };

    /** Evaluate the norm on the inputted vector.
     * @param controlVector vector
     * @return norm of vector
     */
    public abstract double evaluate(Vector3D controlVector);

    /** Evaluate the norm on the inputted vector.
     * @param <T> CalculusFieldElement used
     * @param controlVector vector
     * @return norm of vector
     */
    public abstract <T extends CalculusFieldElement<T>> T evaluate(FieldVector3D<T> controlVector);

}
