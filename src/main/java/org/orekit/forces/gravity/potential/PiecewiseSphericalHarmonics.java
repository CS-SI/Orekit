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
import org.hipparchus.util.SinCos;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Piecewise gravity fields with time-dependent models in each interval.
 * @author Luc Maisonobe
 * @since 11.1
 */
class PiecewiseSphericalHarmonics implements RawSphericalHarmonicsProvider {

    /** Constant part of the field. */
    private final ConstantSphericalHarmonics constant;

    /** Reference dates. */
    private final AbsoluteDate[] references;

    /** Pulsations (rad/s). */
    private final double[] pulsations;

    /** Piecewise parts. */
    private final TimeSpanMap<PiecewisePart> pieces;

    /** Maximum supported degree. */
    private final int maxDegree;

    /** Maximum supported order. */
    private final int maxOrder;

    /** Simple constructor.
     * @param constant constant part of the field
     * @param references references dates
     * @param pulsations pulsations (rad/s)
     * @param pieces piecewise parts
     */
    PiecewiseSphericalHarmonics(final ConstantSphericalHarmonics constant,
                                final AbsoluteDate[] references, final double[] pulsations,
                                final TimeSpanMap<PiecewisePart> pieces) {
        this.constant   = constant;
        this.references = references.clone();
        this.pulsations = pulsations.clone();
        this.pieces     = pieces;

        // get limits
        int d = constant.getMaxDegree();
        int o = constant.getMaxOrder();
        for (TimeSpanMap.Span<PiecewisePart> span = pieces.getFirstSpan(); span != null; span = span.next()) {
            final PiecewisePart piece = span.getData();
            if (piece != null) {
                d = FastMath.max(d, piece.getMaxDegree());
                o = FastMath.max(o, piece.getMaxOrder());
            }
        }
        this.maxDegree = d;
        this.maxOrder  = o;

    }

    /** Get the constant part of the field.
     * @return constant part of the field
     */
    public ConstantSphericalHarmonics getConstant() {
        return constant;
    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return maxDegree;
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return maxOrder;
    }

    /** {@inheritDoc} */
    public double getMu() {
        return constant.getMu();
    }

    /** {@inheritDoc} */
    public double getAe() {
        return constant.getAe();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getReferenceDate() {
        AbsoluteDate last = AbsoluteDate.PAST_INFINITY;
        for (final AbsoluteDate date : references) {
            if (date.isAfter(last)) {
                last = date;
            }
        }
        return last;
    }

    /** {@inheritDoc} */
    public TideSystem getTideSystem() {
        return constant.getTideSystem();
    }

    /** Get the raw spherical harmonic coefficients on a specific date.
     * @param date to evaluate the spherical harmonics
     * @return the raw spherical harmonics on {@code date}.
     */
    public RawSphericalHarmonics onDate(final AbsoluteDate date) {

        // raw (constant) harmonics
        final RawSphericalHarmonics raw = constant.onDate(date);

        // select part of the piecewise model that is active at specified date
        final PiecewisePart piece = pieces.get(date);

        // pre-compute canonical functions
        final double[]   offsets = new double[references.length];
        final SinCos[][] sinCos  = new SinCos[references.length][pulsations.length];
        for (int i = 0; i < references.length; ++i) {
            final double offset = date.durationFrom(references[i]);
            offsets[i] = offset;
            for (int j = 0; j < pulsations.length; ++j) {
                sinCos[i][j] = FastMath.sinCos(offset * pulsations[j]);
            }
        }

        return new RawSphericalHarmonics() {

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            /** {@inheritDoc} */
            public double getRawCnm(final int n, final int m) {
                return raw.getRawCnm(n, m) + piece.computeCnm(n, m, offsets, sinCos);
            }

            /** {@inheritDoc} */
            public double getRawSnm(final int n, final int m) {
                return raw.getRawSnm(n, m) + piece.computeSnm(n, m, offsets, sinCos);
            }

        };

    }

}
