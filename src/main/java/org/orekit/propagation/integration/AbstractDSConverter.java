/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;

/** Converter for states and parameters arrays
 *  for both {@link NumericalPropagator numerical} and {@link DSSTPropagator semi-analytical} propagators.
 *  @author Luc Maisonobe
 */
public abstract class AbstractDSConverter {

    /** Dimension of the state. */
    private final int freeStateParameters;

    /** Simple constructor.
     * @param freeStateParameters number of free parameters
     */
    protected AbstractDSConverter(final int freeStateParameters) {
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
     * @param factory factory for the extended derivatives
     * @return extended scalar
     */
    protected DerivativeStructure extend(final DerivativeStructure original, final DSFactory factory) {
        final double[] originalDerivatives = original.getAllDerivatives();
        final double[] extendedDerivatives = new double[factory.getCompiler().getSize()];
        System.arraycopy(originalDerivatives, 0, extendedDerivatives, 0, originalDerivatives.length);
        return factory.build(extendedDerivatives);
    }

    /** Add zero derivatives.
     * @param original original vector
     * @param factory factory for the extended derivatives
     * @return extended vector
     */
    protected FieldVector3D<DerivativeStructure> extend(final FieldVector3D<DerivativeStructure> original, final DSFactory factory) {
        return new FieldVector3D<>(extend(original.getX(), factory),
                        extend(original.getY(), factory),
                        extend(original.getZ(), factory));
    }

    /** Add zero derivatives.
     * @param original original rotation
     * @param factory factory for the extended derivatives
     * @return extended rotation
     */
    protected FieldRotation<DerivativeStructure> extend(final FieldRotation<DerivativeStructure> original, final DSFactory factory) {
        return new FieldRotation<>(extend(original.getQ0(), factory),
                        extend(original.getQ1(), factory),
                        extend(original.getQ2(), factory),
                        extend(original.getQ3(), factory),
                        false);
    }
}
