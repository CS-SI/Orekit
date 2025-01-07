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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;

public class TestFieldCost implements FieldCartesianCost<Binary64> {

    @Override
    public String getAdjointName() {
        return "";
    }

    @Override
    public Binary64 getMassFlowRateFactor() {
        return Binary64.ONE.multiply(10);
    }

    @Override
    public FieldVector3D<Binary64> getFieldThrustAccelerationVector(Binary64[] adjointVariables, Binary64 mass) {
        return new FieldVector3D<>(mass.getField(), new Vector3D(1, 2, 3));
    }

    @Override
    public void updateFieldAdjointDerivatives(Binary64[] adjointVariables, Binary64 mass, Binary64[] adjointDerivatives) {

    }

    @Override
    public Binary64 getFieldHamiltonianContribution(Binary64[] adjointVariables, Binary64 mass) {
        return mass.getField().getZero();
    }

    @Override
    public CartesianCost toCartesianCost() {
        return new TestCost();
    }
}
