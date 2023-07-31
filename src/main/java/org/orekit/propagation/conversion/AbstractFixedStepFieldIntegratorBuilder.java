/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.conversion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;

/**
 * Abstract class for integrator builder using fixed step size.
 *
 * @param <T> Type of the field elements
 *
 * @author Vincent Cucchietti
 */
public abstract class AbstractFixedStepFieldIntegratorBuilder<T extends CalculusFieldElement<T>>
        extends AbstractFieldIntegratorBuilder<T> {

    /** Step size (s). */
    private double step;

    /** Step size (s). */
    private T fieldStep;

    /**
     * Constructor.
     *
     * @param step step size (s)
     */
    AbstractFixedStepFieldIntegratorBuilder(final double step) {
        // Check that given step size is strictly positive
        checkStep(step);

        this.step = step;
    }

    /**
     * Constructor using a "fielded" step.
     * <p>
     * <b>WARNING : Given "fielded" step must be using the same field as the one that will be used when calling
     * {@link #buildIntegrator}</b>
     *
     * @param step step size (s)
     */
    AbstractFixedStepFieldIntegratorBuilder(final T step) {
        // Check that given step size is strictly positive
        checkStep(step.getReal());

        this.fieldStep = step;
    }

    /**
     * Check that given step size is not equal to 0.
     *
     * @param stepToCheck step size (s) to check
     */
    protected void checkStep(final double stepToCheck) {
        if (stepToCheck == 0) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.ZERO_NOT_ALLOWED, stepToCheck);
        }
    }

    /**
     * Get "fielded" step size (s).
     *
     * @param field field to which the element belong
     *
     * @return "fielded" step size (s)
     */
    protected T getFieldStep(final Field<T> field) {
        return fieldStep != null ? fieldStep : field.getOne().multiply(step);
    }
}
