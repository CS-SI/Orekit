/* Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.orekit.propagation.semianalytical.dsst.coefficients.ModifiedNewcombOperatorLoader;

/**
 * This abstract class represent gravitational forces and contains the {@link DSSTThirdBody} and the
 * {@link DSSTCentralBody} force model. .<br>
 * As resonant central body tesseral harmonics and third body potential expressions are using
 * Modified Newcomb Operator, we mainly use this class to define a common data loader to read the
 * Modified Newcomb Operator from an internal file.
 *
 * @author rdicosta
 */
public abstract class AbstractGravitationalForces implements DSSTForceModel {

    /** Maximum gravitational order. */
    protected static final int    MAX_GRAV_ORDER = 50;

    /** Loader for modified Newcomb operators. */
    private ModifiedNewcombOperatorLoader loader;

    /** Dummy constructor.
     */
    public AbstractGravitationalForces() {
        loader = null;
        // Dummy constructor
    }

    /** Get the Modified Newcomb Operator loader.
     * @return loader
     */
    public ModifiedNewcombOperatorLoader getLoader() {
        return loader;
    }

    /** Initialize the Modified Newcomb Operator loader.
     */
    public void initializeLoader() {
        if (loader == null) {

        }
    }
}
