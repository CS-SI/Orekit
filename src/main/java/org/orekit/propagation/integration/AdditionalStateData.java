/* Copyright 2010-2011 Centre National d'Études Spatiales
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
package org.orekit.propagation.integration;

import java.io.Serializable;

import org.orekit.propagation.numerical.NumericalPropagator;

/** This class is a container for additional state parameters and the name of their associated evolution equation.
*
* <p>
* This object is a container allowing the propagator to keep constant consistency between additional
* states and the corresponding equations. It allows to set additional state values, get current
* additional state value and by reference on the associated additional state name.
* </p>
* @see NumericalPropagator
* @see AdditionalEquations
* @author Luc Maisonobe
*/
public class AdditionalStateData implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 5993627266168523777L;

    /** Additional state name. */
    private String name;

    /** Current additional state. */
    private double[] addState;

    /** Current additional state derivatives. */
    private double[] addStateDot;

    /** Simple constructor.
     * @param name additional state name.
     */
    public AdditionalStateData(final String name) {
        this.name = name;
    }

    /** Get the name of the additional state name.
     * @return additional state name
     */
    public String getName() {
        return name;
    }

    /** Get a reference to the current value of the additional state.
     * <p>The array returned is a true reference to the state array, so it may be used
     * to store data into it.</>
     * @return a reference current value of the additional state.
     */
    public double[] getAdditionalState() {
        return addState;
    }

    /** Get a reference to the current value of the additional state derivatives.
     * <p>The array returned is a true reference to the state array, so it may be used
     * to store data into it.</>
     * @return a reference current value of the additional state derivatives.
     */
    public double[] getAdditionalStateDot() {
        return addStateDot;
    }

    /** Sets a value to additional state.
     * @param state additional state value.
     */
    public void setAdditionalState(final double[] state) {
        this.addState    = state.clone();
        this.addStateDot = new double[state.length];
    }
}
