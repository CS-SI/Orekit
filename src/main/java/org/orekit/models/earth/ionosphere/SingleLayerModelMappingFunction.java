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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;

/**
 * Single Layer Model (SLM) ionospheric mapping function.
 * <p>
 * The SLM mapping function assumes a single ionospheric layer with a constant height
 * for the computation of the mapping factor.
 * </p>
 * @see "N. Ya’acob, M. Abdullah and M. Ismail, Determination of the GPS
 *       total electron content using single layer model (SLM) ionospheric
 *       mapping function, in International Journal of Computer Science and
 *       Network Security, vol. 8, no. 9, pp. 154-160, 2008."
 *
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class SingleLayerModelMappingFunction implements IonosphericMappingFunction {

    /** Default value for the height of the ionospheric single layer in meters. */
    private static final double DEFAULT_HEIGHT = 450e3;

    /** Mean Earth radius in meters. */
    private static final double RE = 6371e3;

    /** Height of the ionospheric single layer in meters.*/
    private final double hIon;

    /**
     * Constructor with default value.
     * <p>
     * Using this constructor, the height of the ionospheric single
     * layer is equal to 450 kilometers as recommended by the IERS
     * Convention 2010.
     * </p>
     */
    public SingleLayerModelMappingFunction() {
        this(DEFAULT_HEIGHT);
    }

    /**
     * Constructor.
     * @param hIon height of the ionospheric single layer in meters
     */
    public SingleLayerModelMappingFunction(final double hIon) {
        this.hIon = hIon;
    }

    /** {@inheritDoc} */
    @Override
    public double mappingFactor(final double elevation) {
        // Calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI - elevation);
        // Distance ratio
        final double ratio = RE / (RE + hIon);
        // Mapping function
        final double coef = FastMath.sin(z) * ratio;
        return 1.0 / FastMath.sqrt(1.0 - coef * coef);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T mappingFactor(final T elevation) {
        // Calculate the zenith angle from the elevation
        final T z = FastMath.abs(elevation.negate().add(elevation.getPi().multiply(0.5)));
        // Distance ratio
        final double ratio = RE / (RE + hIon);
        // Mapping function
        final T coef = FastMath.sin(z).multiply(ratio);
        return FastMath.sqrt(coef.multiply(coef).negate().add(1.0)).reciprocal();
    }

}
