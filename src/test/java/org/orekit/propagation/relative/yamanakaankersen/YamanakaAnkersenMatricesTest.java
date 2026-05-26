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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.utils.PVCoordinates;



public class YamanakaAnkersenMatricesTest {

    public static final double NUMERICAL_TOLERANCE = 1e-6;

    @Test

    public void testMatrixComputation(){
        final double timeSinceEpoch = 900.;
        final double targetA = 18000e3;
        final double targetE = 0.8;
        final double targetInitialTheta = 15 * FastMath.PI/180;
        final double targetTheta = 45*FastMath.PI/180;
        final double mu = 398600e9;

        final YamanakaAnkersenMatrices matrices = YamanakaAnkersenEquations.computeMatrices(timeSinceEpoch,targetA,targetE,targetInitialTheta,targetTheta,mu);
        TestUtils.validateRealMatrix(MatrixUtils.createRealMatrix(new double[][] {{    1., 91.65856555, -57.61316931, 7.03989058},{0., -31.85972392, 20.71074309, -1.97740435},{ 0., -65.71944785, 42.42148618, -3.95480869},{0.,-20.28609784, 12.4474521, -0.84853331}}), matrices.getInPlaneMatrix(), NUMERICAL_TOLERANCE);
        TestUtils.validateRealMatrix(MatrixUtils.createRealMatrix(new double[][] {{  0.8660254,  0.5         },{-0.5,        0.8660254}}), matrices.getOutPlaneMatrix(), NUMERICAL_TOLERANCE);
    }
@Test
    public void testTransform(){
        final double timeSinceEpoch = 900.;
        final double targetA = 18000e3;
        final double targetE = 0.8;
        final double targetInitialTheta = 15* FastMath.PI/180;
        final double targetTheta = 45*FastMath.PI/180;
        final double mu = 398600e9;
        final double[] initial_pos = {10, 20, 30};
        final double[] initial_vel = {2, 3, 4};

        final PVCoordinates initialPV = new PVCoordinates(new Vector3D(initial_pos),new Vector3D(initial_vel));

        final YamanakaAnkersenMatrices matrices = YamanakaAnkersenEquations.computeMatrices(timeSinceEpoch,targetA,targetE,targetInitialTheta,targetTheta,mu);

        final PVCoordinates finalPV = matrices.transform(initialPV,targetInitialTheta,targetTheta,targetE,targetA,mu);

        Assertions.assertEquals(-22744.724949364987,finalPV.getPosition().getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(464.8029487831341,finalPV.getPosition().getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(8873.951448243051,finalPV.getPosition().getZ(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(29.836822546829715,finalPV.getVelocity().getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(2.7524908430290833,finalPV.getVelocity().getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(26.418869170876388,finalPV.getVelocity().getZ(),NUMERICAL_TOLERANCE);

    }

    @Test
    public void testGetTargetTrueAnomaly(){
        final double targetTheta = 0.65;
        final YamanakaAnkersenMatrices matrices = new YamanakaAnkersenMatrices(100., targetTheta, MatrixUtils.createRealIdentityMatrix(4),MatrixUtils.createRealIdentityMatrix(2));
        Assertions.assertEquals(targetTheta,matrices.getTargetTrueAnomaly(),NUMERICAL_TOLERANCE);

    }

}
