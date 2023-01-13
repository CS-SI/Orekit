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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;

/** Simple implementation of {@link RawSphericalHarmonicsProvider} for constant gravity fields.
 * @author Luc Maisonobe
 * @since 6.0
 */
class ConstantSphericalHarmonics implements RawSphericalHarmonicsProvider {

    /** Central body reference radius. */
    private final double ae;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Tide system. */
    private final TideSystem tideSystem;

    /** Converter from triangular to flatten array.
     * @since 11.1
     */
    private final Flattener flattener;

    /** Raw tesseral-sectorial coefficients matrix. */
    private final double[] rawC;

    /** Raw tesseral-sectorial coefficients matrix. */
    private final double[] rawS;

    /** Simple constructor.
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param tideSystem tide system
     * @param flattener flattener from triangular to flatten array
     * @param rawC raw tesseral-sectorial coefficients
     * @param rawS raw tesseral-sectorial coefficients
     * @since 11.1
     */
    ConstantSphericalHarmonics(final double ae, final double mu, final TideSystem tideSystem,
                               final Flattener flattener, final double[] rawC, final double[] rawS) {
        this.ae         = ae;
        this.mu         = mu;
        this.tideSystem = tideSystem;
        this.flattener  = flattener;
        this.rawC       = rawC;
        this.rawS       = rawS;
    }

    /** Create a constant provider by freezing a regular provider.
     * @param freezingDate freezing date
     * @param raw raw provider to freeze
     * @since 11.1
     */
    ConstantSphericalHarmonics(final AbsoluteDate freezingDate, final RawSphericalHarmonicsProvider raw) {

        this.ae         = raw.getAe();
        this.mu         = raw.getMu();
        this.tideSystem = raw.getTideSystem();
        this.flattener  = new Flattener(raw.getMaxDegree(), raw.getMaxOrder());
        this.rawC       = new double[flattener.arraySize()];
        this.rawS       = new double[flattener.arraySize()];

        // freeze the raw provider
        final RawSphericalHarmonics frozen = raw.onDate(freezingDate);
        for (int n = 0; n <= flattener.getDegree(); ++n) {
            for (int m = 0; m <= FastMath.min(n, flattener.getOrder()); ++m) {
                final int index = flattener.index(n, m);
                rawC[index] = frozen.getRawCnm(n, m);
                rawS[index] = frozen.getRawSnm(n, m);
            }
        }

    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return flattener.getDegree();
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return flattener.getOrder();
    }

    /** {@inheritDoc} */
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    public double getAe() {
        return ae;
    }

    /** {@inheritDoc}
     * <p>
     * For a constant field, null is always returned.
     * </p>
     */
    public AbsoluteDate getReferenceDate() {
        return null;
    }

    /** {@inheritDoc} */
    public TideSystem getTideSystem() {
        return tideSystem;
    }

    @Override
    public RawSphericalHarmonics onDate(final AbsoluteDate date) {
        return new RawSphericalHarmonics() {

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            public double getRawCnm(final int n, final int m) {
                return rawC[flattener.index(n, m)];
            }

            /** {@inheritDoc} */
            public double getRawSnm(final int n, final int m) {
                return rawS[flattener.index(n, m)];
            }

        };
    }

}

