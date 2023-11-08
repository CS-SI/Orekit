/* Copyright 2022-2023 Romain Serra
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

/** Enumerate on types of cost for 3D control vector (thrust as a force or acceleration, including an impulse)
 * at a given time. It is typically a norm (for a single, gimbaled thruster it would be the Euclidean one)
 * and relates to the mass flow rate.
 * See ROSS, I. Michael. Space Trajectory Optimization and L1-norm Optimal Control Problems.
 * Modern astrodynamics, 2006, vol. 1, p. 155.
 * <p>It is used widely across the {@link org.orekit.forces.maneuvers} package.</p>
 * <p>Note that norms in finite-dimensional vector spaces are all equivalent in a topological sense.</p>
 * @see org.orekit.forces.maneuvers.ImpulseManeuver
 * @see org.orekit.forces.maneuvers.FieldImpulseManeuver
 * @see org.orekit.forces.maneuvers.Maneuver
 * @author Romain Serra
 * @since 12.0
 */
public enum Control3DVectorCostType {

    /** Zero cost (free control). */
    NONE {
        @Override
        public double evaluate(final Vector3D controlVector) {
            return 0.;
        }

        @Override
        public <T extends CalculusFieldElement<T>> T evaluate(final FieldVector3D<T> controlVector) {
            return controlVector.getX().getField().getZero();
        }
    },

    /** 1-norm. */
    ONE_NORM {
        @Override
        public double evaluate(final Vector3D controlVector) {
            return controlVector.getNorm1();
        }

        @Override
        public <T extends CalculusFieldElement<T>> T evaluate(final FieldVector3D<T> controlVector) {
            return controlVector.getNorm1();
        }
    },

    /** 2-norm also known as Euclidean. */
    TWO_NORM {
        @Override
        public double evaluate(final Vector3D controlVector) {
            return controlVector.getNorm();
        }

        @Override
        public <T extends CalculusFieldElement<T>> T evaluate(final FieldVector3D<T> controlVector) {
            return controlVector.getNorm();
        }
    },

    /** Infinite norm also known as Max. */
    INF_NORM {
        @Override
        public double evaluate(final Vector3D controlVector) {
            return controlVector.getNormInf();
        }

        @Override
        public <T extends CalculusFieldElement<T>> T evaluate(final FieldVector3D<T> controlVector) {
            return controlVector.getNormInf();
        }
    };

    /** Evaluate the cost of the input seen as a 3D control vector.
     * @param controlVector vector
     * @return cost of vector
     */
    public abstract double evaluate(Vector3D controlVector);

    /** Evaluate the cost of the input seen as a 3D control vector.
     * @param <T> CalculusFieldElement used
     * @param controlVector vector
     * @return cost of vector
     */
    public abstract <T extends CalculusFieldElement<T>> T evaluate(FieldVector3D<T> controlVector);

}
