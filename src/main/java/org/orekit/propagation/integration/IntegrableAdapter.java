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
package org.orekit.propagation.integration;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Adapter from {@link IntegrableGenerator} to {@link AdditionalEquations}.
 * @since 11.1
 * @deprecated this adapter is temporary and will be removed when {@link AdditionalEquations} is removed
 */
public class IntegrableAdapter implements AdditionalEquations {

    /** Underlying generator. */
    private final IntegrableGenerator generator;

    /** Simple constructor.
     * @param generator underlying generator
     */
    public IntegrableAdapter(final IntegrableGenerator generator) {
        this.generator = generator;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return generator.getName();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        generator.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeDerivatives(final SpacecraftState s,  final double[] pDot) {
        System.arraycopy(generator.generate(s), 0, pDot, 0, pDot.length);
        return null;
    }

    /** Get underlying generator.
     * @return underlying generator
     */
    public IntegrableGenerator getGenerator() {
        return generator;
    }

}
