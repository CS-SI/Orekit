/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.orekit.orbits.FieldKeplerianOrbit;

import java.util.function.Function;

/**
 * Container for data contained in an NavIC navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldNavicLegacyNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldLegacyNavigationMessage<T, NavICLegacyNavigationMessage, FieldNavicLegacyNavigationMessage<T>>  {

    /** Constructor from non-field instance.
     * @param orbit    orbit in the correct field
     * @param original regular non-field instance
     */
    public FieldNavicLegacyNavigationMessage(final FieldKeplerianOrbit<T> orbit, final NavICLegacyNavigationMessage original) {
        super(orbit, original);
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param orbit     orbit in the correct field
     * @param original  regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldNavicLegacyNavigationMessage(final FieldKeplerianOrbit<T> orbit,
                                                                                 final Function<V, T> converter,
                                                                                 final FieldNavicLegacyNavigationMessage<V> original) {
        super(orbit, converter, original);
    }

    /** {@inheritDoc} */
    @Override
    public NavICLegacyNavigationMessage toNonField() {
        return new NavICLegacyNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, V extends FieldGnssOrbitalElements<U, NavICLegacyNavigationMessage, V>>
        V toField(final FieldKeplerianOrbit<U> orbit, final Function<T, U> converter) {
        return (V) new FieldNavicLegacyNavigationMessage<>(orbit, converter, this);
    }

}
