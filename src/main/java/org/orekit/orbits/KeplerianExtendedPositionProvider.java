/* Copyright 2022-2026 Romain Serra
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
package org.orekit.orbits;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.utils.AbstractExtendedPositionProvider;
import org.orekit.utils.PVCoordinates;

/**
 * Position provider assuming pure Keplerian motion.
 * Propagation is computed with the same orbital parameters used to define the reference.
 *
 * @author Romain Serra
 * @see org.orekit.utils.ExtendedPositionProvider
 * @see Orbit
 * @see FieldOrbit
 *
 * @since 14.0
 */
public class KeplerianExtendedPositionProvider extends AbstractExtendedPositionProvider<Orbit> {

    /**
     * Constructor.
     * @param referenceOrbit reference orbit (non-Keplerian terms will be ignored if any)
     */
    public KeplerianExtendedPositionProvider(final Orbit referenceOrbit) {
        // Remove non-Keplerian rates if any
        super(referenceOrbit.getType().convertType(new CartesianOrbit(new PVCoordinates(referenceOrbit.getPosition(),
                referenceOrbit.getVelocity()), referenceOrbit.getFrame(), referenceOrbit.getDate(), referenceOrbit.getMu())));
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> FieldOrbit<T> getFieldProvider(final Field<T> field) {
        return getProvider().getType().convertToFieldOrbit(field, getProvider());
    }
}
