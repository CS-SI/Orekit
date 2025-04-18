/* Copyright 2022-2025 Romain Serra
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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.events.EventDetector;

import java.util.stream.Stream;

/**
 * Class for unbounded energy cost with Cartesian coordinates neglecting the mass consumption.
 * Under this assumption, the mass is constant and there is no need to consider the corresponding adjoint variable.
 * Here, the control vector is chosen as the acceleration given by thrusting, expressed in the propagation frame.
 * This leads to the optimal thrust force being equal to the adjoint velocity vector times the mass.
 * @author Romain Serra
 * @since 12.2
 */
public class UnboundedCartesianEnergyNeglectingMass extends AbstractCartesianCost {

    /**
     * Constructor.
     * @param name name
     */
    public UnboundedCartesianEnergyNeglectingMass(final String name) {
        super(name, 0.);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustAccelerationVector(final double[] adjointVariables, final double mass) {
        return new Vector3D(adjointVariables[3], adjointVariables[4], adjointVariables[5]);
    }

    /** {@inheritDoc} */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass,
                                         final double[] adjointDerivatives) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double getHamiltonianContribution(final double[] adjointVariables, final double mass) {
        final Vector3D thrustAcceleration = getThrustAccelerationVector(adjointVariables, mass);
        return -thrustAcceleration.getNormSq() / 2.;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.empty();
    }

}
