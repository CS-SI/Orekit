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
package org.orekit.control.indirect.shooting;

/**
 * Interface defining the differential correction part of the shooting method.
 * This is called at each iteration.
 *
 * @author Romain Serra
 * @since 12.2
 * @see FixedTimeCartesianSingleShooting
 */
@FunctionalInterface
public interface ShootingDifferentialCorrection {

    /**
     * Computes differential correction.
     * @param residuals residuals (differences w.r.t. target)
     * @param jacobian Jacobian matrix of residuals w.r.t. shooting variables
     * @return correction to apply
     */
    double[] computeCorrection(double[] residuals, double[][] jacobian);

}
