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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.utils.AbstractExtendedPositionProvider;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Class for GNSS extended position provider.
 * @param <O> type of the orbital elements
 * @see ExtendedPositionProvider
 * @author Romain Serra
 * @since 14.0
 */
public class GNSSExtendedPositionProvider<O extends GNSSOrbitalElements<O>>
    extends AbstractExtendedPositionProvider<GNSSPropagator<O>> {

    /**
     * Build a new instance.
     * @param orbitalElements GNSS orbital elements
     * @param eci Earth Centered Inertial frame
     * @param ecef Earth Centered Earth Fixed frame
     * @param provider attitude provider
     * @param mass satellite mass (kg)
     */
    public GNSSExtendedPositionProvider(final O orbitalElements, final Frame eci,
                                        final Frame ecef, final AttitudeProvider provider, final double mass) {
        super(new GNSSPropagator<>(orbitalElements, eci, ecef, provider, mass));
    }

    /** {@inheritDoc} */
    @Override
    protected  <T extends CalculusFieldElement<T>> FieldGnssPropagator<T, O> getFieldProvider(final Field<T> field) {
        return new FieldGnssPropagator<>(getProvider().getOrbitalElements().toField(field),
                getProvider().getECI(), getProvider().getECEF(), getProvider().getAttitudeProvider(),
                field.getZero().newInstance(getProvider().getInitialState().getMass()));
    }
}
