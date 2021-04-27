/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.propagation.FieldAbstractPropagator;
import org.orekit.propagation.integration.AbstractGradientConverter;

/**
 * Abstract class for analytical propagator GradientConverter.
 *
 * @author Nicolas Fialton
 */
public abstract class AbstractAnalyticalGradientConverter extends AbstractGradientConverter {

    /** Constructor used to set the freeStateParameters.
     * @param freeStateParameters
     */
    protected AbstractAnalyticalGradientConverter(final int freeStateParameters) {
        super(freeStateParameters);
    }

    /**
     * Get the gradient propagator.
     * <p>
     * This method uses the {@link DataContext#getDefault() default data
     * context}.
     *
     * @return a Field Propagator
     */
    @DefaultDataContext
    public abstract FieldAbstractPropagator<Gradient> getPropagator();

    /**
     * Get the model parameters.
     * <p>
     * This method uses the {@link DataContext#getDefault() default data
     * context}.
     *
     * @return no parameters
     */
    public Gradient[] getParameters() {
        // no parameters
        return new Gradient[0];
    }

}
