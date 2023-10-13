/* Copyright 2022 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/**
 * This class is a container for the common parameters used in {@link DSSTJ2SquaredClosedForm}.
 * <p>
 * It performs parameters initialization at each integration step for the second-order J2-squared
 * contribution to the central body gravitational perturbation.
 * </p>
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class DSSTJ2SquaredClosedFormContext extends ForceModelContext {

    /** Equatorial radius of the central body to the power 4. */
    private final double alpha4;

    /** Semi major axis to the power 4. */
    private final double a4;

    /** sqrt(1 - e * e). */
    private final double eta;

    /** Cosine of the inclination. */
    private final double c;

    /** Sine of the inclination to the power 2. */
    private final double s2;

    /**
     * Simple constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param provider          provider for spherical harmonics
     */
    public DSSTJ2SquaredClosedFormContext(final AuxiliaryElements auxiliaryElements,
                                          final UnnormalizedSphericalHarmonicsProvider provider) {
        super(auxiliaryElements);

        // Sine and cosine of the inclination
        final double inc = auxiliaryElements.getOrbit().getI();
        final SinCos scI = FastMath.sinCos(inc);

        // Other parameters
        this.c = scI.cos();
        this.s2 = scI.sin() * scI.sin();

        final double alpha2 = provider.getAe() * provider.getAe();
        this.alpha4 = alpha2 * alpha2;

        this.eta = FastMath.sqrt(1.0 - auxiliaryElements.getEcc() * auxiliaryElements.getEcc());

        final double a2 = auxiliaryElements.getSma() * auxiliaryElements.getSma();
        this.a4 = a2 * a2;
    }

    /**
     * Get the equatorial radius of the central body to the power 4.
     * @return the equatorial radius of the central body to the power 4
     */
    public double getAlpha4() {
        return alpha4;
    }

    /**
     * Get the semi major axis to the power 4.
     * @return the semi major axis to the power 4
     */
    public double getA4() {
        return a4;
    }

    /**
     * Get the eta value.
     * @return sqrt(1 - e * e)
     */
    public double getEta() {
        return eta;
    }

    /**
     * Get the cosine of the inclination.
     * @return the cosine of the inclination
     */
    public double getC() {
        return c;
    }

    /**
     * Get the sine of the inclination to the power 2.
     * @return the sine of the inclination to the power 2
     */
    public double getS2() {
        return s2;
    }

}
