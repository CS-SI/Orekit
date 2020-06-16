/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.integration;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;

/** Converter for states and parameters arrays
 *  for both {@link NumericalPropagator numerical} and {@link DSSTPropagator semi-analytical} propagators.
 *  @author Luc Maisonobe
 *  @author Bryan Cazabonne
 *  @since 10.2
 */
public abstract class AbstractGradientConverter {

    /** Dimension of the state. */
    private final int freeStateParameters;

    /** Simple constructor.
     * @param freeStateParameters number of free parameters
     */
    protected AbstractGradientConverter(final int freeStateParameters) {
        this.freeStateParameters = freeStateParameters;
    }

    /** Get the number of free state parameters.
     * @return number of free state parameters
     */
    public int getFreeStateParameters() {
        return freeStateParameters;
    }

    /** Add zero derivatives.
     * @param original original scalar
     * @param freeParameters total number of free parameters in the gradient
     * @return extended scalar
     */
    protected Gradient extend(final Gradient original, final int freeParameters) {
        final double[] originalDerivatives = original.getGradient();
        final double[] extendedDerivatives = new double[freeParameters];
        System.arraycopy(originalDerivatives, 0, extendedDerivatives, 0, originalDerivatives.length);
        return new Gradient(original.getValue(), extendedDerivatives);
    }

    /** Add zero derivatives.
     * @param original original vector
     * @param freeParameters total number of free parameters in the gradient
     * @return extended vector
     */
    protected FieldVector3D<Gradient> extend(final FieldVector3D<Gradient> original, final int freeParameters) {
        return new FieldVector3D<>(extend(original.getX(), freeParameters),
                                   extend(original.getY(), freeParameters),
                                   extend(original.getZ(), freeParameters));
    }

    /** Add zero derivatives.
     * @param original original rotation
     * @param freeParameters total number of free parameters in the gradient
     * @return extended rotation
     */
    protected FieldRotation<Gradient> extend(final FieldRotation<Gradient> original, final int freeParameters) {
        return new FieldRotation<>(extend(original.getQ0(), freeParameters),
                                   extend(original.getQ1(), freeParameters),
                                   extend(original.getQ2(), freeParameters),
                                   extend(original.getQ3(), freeParameters),
                                   false);
    }
}
