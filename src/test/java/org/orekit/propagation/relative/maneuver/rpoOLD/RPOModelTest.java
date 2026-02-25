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
package org.orekit.propagation.relative.maneuver.rpoOLD;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;

public class RPOModelTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-12;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void getRBarDirectionTest() {
        TestUtils.validateVector3D(Vector3D.PLUS_I, RPOModel.CW.getRBarDirection(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(Vector3D.PLUS_K, RPOModel.YA.getRBarDirection(), NUMERICAL_TOLERANCE);
    }

    @Test
    public void getVBarDirectionTest() {
        TestUtils.validateVector3D(Vector3D.PLUS_J, RPOModel.CW.getVBarDirection(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(Vector3D.PLUS_I, RPOModel.YA.getVBarDirection(), NUMERICAL_TOLERANCE);
    }

    @Test
    public void getOutOfPlaneDirectionTest() {
        TestUtils.validateVector3D(Vector3D.PLUS_K, RPOModel.CW.getOutOfPlaneDirection(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(Vector3D.MINUS_J, RPOModel.YA.getOutOfPlaneDirection(), NUMERICAL_TOLERANCE);
    }
}
