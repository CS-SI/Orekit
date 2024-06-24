/* Copyright 2002-2024 Thales Alenia Space
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

import org.hipparchus.CalculusFieldElement;

/** Abstract base class for modifying state during propagation.
 * <p>
 * This class is a specialized implementation of {@link AdditionalStateProvider}
 * with a name set to the empty string and returning a null additional state.
 * </p>
 * <p>
 * Beware that changing the state undercover from the propagator may have
 * many side effects. Using this class should therefore be done cautiously.
 * </p>
 * @see Propagator
 * @see AdditionalStateProvider
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 * @since 12.1
 */
public abstract class FieldAbstractStateModifier<T extends CalculusFieldElement<T>>
    implements FieldAdditionalStateProvider<T> {

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public T[] getAdditionalState(final FieldSpacecraftState<T> state) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public FieldSpacecraftState<T> update(final FieldSpacecraftState<T> state) {
        return change(state);
    }
    /** Change main state.
     * @param state spacecraft state to change
     * @return changed state
     */
    public abstract FieldSpacecraftState<T> change(FieldSpacecraftState<T> state);

}
