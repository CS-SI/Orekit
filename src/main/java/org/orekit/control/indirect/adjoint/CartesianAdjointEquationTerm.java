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
package org.orekit.control.indirect.adjoint;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Interface to define terms in the adjoint equations for Cartesian coordinates.
 * @author Romain Serra
 * @see CartesianAdjointDerivativesProvider
 * @since 12.2
 */
public interface CartesianAdjointEquationTerm {

    /**
     * Computes the contribution to the rates of the adjoint variables.
     *
     * @param date             date
     * @param stateVariables   state variables
     * @param adjointVariables adjoint variables
     * @param frame            propagation frame
     * @return contribution to the adjoint derivative vector
     */
    double[] getContribution(AbsoluteDate date, double[] stateVariables, double[] adjointVariables, Frame frame);

    /**
     * Computes the contribution to the rates of the adjoint variables.
     *
     * @param <T>              field type
     * @param date             date
     * @param stateVariables   state variables
     * @param adjointVariables adjoint variables
     * @param frame            propagation frame
     * @return contribution to the adjoint derivative vector
     */
    <T extends CalculusFieldElement<T>> T[] getFieldContribution(FieldAbsoluteDate<T> date, T[] stateVariables, T[] adjointVariables, Frame frame);

}
