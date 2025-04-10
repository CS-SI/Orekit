/* Copyright 2020-2025 Exotrail
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
package org.orekit.propagation.conversion.averaging;

import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
 * Abstract class representing averaged orbital state based on {@link UnnormalizedSphericalHarmonicsProvider}.
 *
 * @author Romain Serra
 * @see AveragedOrbitalState
 * @since 12.1
 */
abstract class AbstractHarmonicsBasedOrbitalState extends AbstractAveragedOrbitalState {

    /** Spherical harmonics provider. */
    private final UnnormalizedSphericalHarmonicsProvider harmonicsProvider;

    /**
     * Protected constructor.
     * @param date epoch
     * @param frame reference frame
     * @param harmonicsProvider spherical harmonics provider
     */
    protected AbstractHarmonicsBasedOrbitalState(final AbsoluteDate date, final Frame frame,
                                                 final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        super(date, frame);
        this.harmonicsProvider = harmonicsProvider;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return harmonicsProvider.getMu();
    }

    /**
     * Getter for spherical harmonics provider.
     * @return harmonics provider
     */
    public UnnormalizedSphericalHarmonicsProvider getHarmonicsProvider() {
        return harmonicsProvider;
    }
}
