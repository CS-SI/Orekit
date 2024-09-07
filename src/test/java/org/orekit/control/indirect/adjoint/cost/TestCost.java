/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.stream.Stream;

public class TestCost implements CartesianCost {

    @Override
    public String getAdjointName() {
        return "";
    }

    @Override
    public double getMassFlowRateFactor() {
        return 10.;
    }

    @Override
    public Vector3D getThrustAccelerationVector(double[] adjointVariables, double mass) {
        return new Vector3D(1, 2, 3);
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldThrustAccelerationVector(T[] adjointVariables, T mass) {
        return new FieldVector3D<>(mass.getField(), new Vector3D(1, 2, 3));
    }

    @Override
    public void updateAdjointDerivatives(double[] adjointVariables, double mass, double[] adjointDerivatives) {

    }

    @Override
    public <T extends CalculusFieldElement<T>> void updateFieldAdjointDerivatives(T[] adjointVariables, T mass, T[] adjointDerivatives) {

    }

    @Override
    public double getHamiltonianContribution(double[] adjointVariables, double mass) {
        return 0;
    }

    @Override
    public <T extends CalculusFieldElement<T>> T getFieldHamiltonianContribution(T[] adjointVariables, T mass) {
        return mass.getField().getZero();
    }

    @Override
    public Stream<EventDetector> getEventDetectors() {
        return null;
    }

    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
        return null;
    }

}
