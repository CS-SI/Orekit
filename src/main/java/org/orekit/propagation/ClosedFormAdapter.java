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
package org.orekit.propagation;

/** Adapter from {@link StackableGenerator} to {@link AdditionalStateProvider}.
 * @since 11.1
 * @deprecated this adapter is temporary and will be removed when {@link AdditionalStateProvider} is removed
 */
public class ClosedFormAdapter implements AdditionalStateProvider {

    /** Underlying generator. */
    private final StackableGenerator generator;

    /** Simple constructor.
     * @param generator underlying generator
     */
    public ClosedFormAdapter(final StackableGenerator generator) {
        this.generator = generator;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return generator.getName();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalState(final SpacecraftState state) {
        return generator.generate(state);
    }

}
