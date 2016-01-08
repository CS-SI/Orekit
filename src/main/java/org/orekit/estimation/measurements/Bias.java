/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import java.util.Arrays;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;

/** Class modeling a measurement bias.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 7.1
 */
public class Bias<T extends Measurement<T>> implements EvaluationModifier<T> {

    /** Parameter holding the bias value. */
    private final Parameter parameter;

    /** Identity matrix, for partial derivatives. */
    private final double[][] derivatives;


    /** Simple constructor.
     * @param name name of the bias
     * @param bias initial value of the bias
     * @exception OrekitException if initial value cannot be set
     */
    public Bias(final String name, final double ... bias)
        throws OrekitException {

        parameter = new Parameter(name) {
            /** {@inheritDoc} */
            @Override
            public void valueChanged(final double[] newValue) {
            }
        };

        derivatives = new double[bias.length][bias.length];
        for (int i = 0; i < bias.length; ++i) {
            derivatives[i][i] = 1.0;
        }

        setBias(bias);

    }

    /** Get the bias.
     * @return bias
     */
    public double[] getBias() {
        return parameter.getValue();
    }

    /** Set the bias.
     * @param bias new value for the bias
     * @exception OrekitException if underlying {@link Parameter} cannot be set
     */
    public void setBias(final double ... bias)
        throws OrekitException {
        parameter.setValue(bias);
    }

    /** {@inheritDoc} */
    @Override
    public List<Parameter> getSupportedParameters() {
        return Arrays.asList(parameter);
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final Evaluation<T> evaluation) {

        // apply the bias to the measurement value
        final double[] measurementValue = evaluation.getValue();
        final double[] biasValue        = parameter.getValue();
        for (int i = 0; i < parameter.getDimension(); ++i) {
            measurementValue[i] += biasValue[i];
        }
        evaluation.setValue(measurementValue);

        if (parameter.isEstimated()) {
            // add the partial derivatives
            evaluation.setParameterDerivatives(parameter.getName(), derivatives);
        }

    }

}
