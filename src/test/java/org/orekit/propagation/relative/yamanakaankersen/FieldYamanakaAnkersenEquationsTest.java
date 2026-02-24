/* Copyright 2002-2026 CS GROUP
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

package org.orekit.propagation.relative.yamanakaankersen;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.utils.FieldPVCoordinates;


public class FieldYamanakaAnkersenEquationsTest {

    public static final double NUMERICAL_TOLERANCE = 1e-6;
    @Test
    public void testMatrixComputation(){
        final Binary64 timeSinceEpoch = new Binary64(900.);
        final Binary64 targetA = new Binary64(18000e3);
        final Binary64 targetE = new Binary64(0.8);
        final Binary64 targetInitialTheta = new Binary64(15 * FastMath.PI /180 );
        final Binary64 targetTheta = new Binary64(45 * FastMath.PI /180);
        final Binary64 mu = new Binary64(398600e9);


        final FieldYamanakaAnkersenMatrices<Binary64> matrices = (new FieldYamanakaAnkersenEquations<Binary64>()).computeMatrices(timeSinceEpoch,targetA,targetE,targetInitialTheta,targetTheta,mu);
        TestUtils.validateFieldMatrix(MatrixUtils.createRealMatrix(new double[][] {{    1.,    5.15809929,    -2.78763231, 0.63629486},{0.,         -0.6069242,   0.90216168,  0.33622821},{ 0.,         -3.2138484 ,  2.80432336,  0.67245641},{0.,-0.32500015, -0.20424734, 0.62917896}}), matrices.getInPlaneMatrix(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldMatrix(MatrixUtils.createRealMatrix(new double[][] {{  0.8660254,  0.5         },{-0.5,        0.8660254}}), matrices.getOutPlaneMatrix(), NUMERICAL_TOLERANCE);
    }

    @Test
    public void testTransform(){
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 timeSinceEpoch = new Binary64(900.);
        final Binary64 targetA = new Binary64(18000e3);
        final Binary64 targetE = new Binary64(0.8);
        final Binary64 targetInitialTheta = new Binary64(15* FastMath.PI/180);
        final Binary64 targetTheta = new Binary64(45*FastMath.PI/180);
        final Binary64 mu = new Binary64(398600e9);
        final double[] initial_pos = {10, 20, 30};
        final double[] initial_vel = {2, 3, 4};

        final FieldPVCoordinates<Binary64> initialPV = new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(initial_pos)),new FieldVector3D<>(field, new Vector3D(initial_vel)));

        final FieldYamanakaAnkersenMatrices<Binary64> matrices = (new FieldYamanakaAnkersenEquations<Binary64>()).computeMatrices(timeSinceEpoch,targetA,targetE,targetInitialTheta,targetTheta,mu);

        final FieldPVCoordinates<Binary64> finalPV = matrices.transform(initialPV,targetInitialTheta,targetTheta,targetE,targetA,mu);

        Assertions.assertEquals(-3988.161653345807,finalPV.getPosition().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(2085.484607920448,finalPV.getPosition().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(4316.91631803429,finalPV.getPosition().getZ().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(6.331720007455974,finalPV.getVelocity().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(2.7687859925810123,finalPV.getVelocity().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(2.853050086358761,finalPV.getVelocity().getZ().getReal(),NUMERICAL_TOLERANCE);

    }
    @Test
    public void testGetTargetTrueAnomaly(){
        final Binary64 targetTheta = new Binary64(0.65);
        final FieldYamanakaAnkersenMatrices<Binary64> matrices = new FieldYamanakaAnkersenMatrices<>(new Binary64(100.), targetTheta, MatrixUtils.createFieldIdentityMatrix(Binary64Field.getInstance(),4),MatrixUtils.createFieldIdentityMatrix(Binary64Field.getInstance(),2));
        Assertions.assertEquals(targetTheta.getReal(),matrices.getTargetTrueAnomaly().getReal(),NUMERICAL_TOLERANCE);

    }
}

