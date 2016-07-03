/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.forces.gravity;


import org.hipparchus.dfp.Dfp;
import org.hipparchus.dfp.DfpField;
import org.hipparchus.dfp.DfpMath;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.RawSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.RawSphericalHarmonicsProvider.RawSphericalHarmonics;
import org.orekit.forces.gravity.potential.SphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;


/** Implementation of gravity field model from defining formulas.
 * <p>
 * This implementation is for test purposes only! It is extremely slow.
 * </p>
 * <p>
 * The major features of this implementation are:
 * <ul>
 *   <li>its accuracy can be adjusted at will to any number of digits,</li>
 *   <li>it relies on direct defining formulas and hence is completely independent from
 *   the optimized practical recursions.</li>
 * </ul>
 * Both features are helpful for cross-checking practical optimized implementations.
 * </p>
 * <p>
 * Note that since this uses defining formulas to perform direct computation, it is
 * subject to the high instability of these formulas near poles. Tests performed
 * with the D. M. Gleason resting testing regime have shown for example that setting
 * the accuracy to 28 digits for the computation leads to field values having only about
 * 11 digits accuracy left near poles! In order to get 16 digits and be able to use this
 * class as a reference for testing other implementations, we needed to set accuracy to
 * at lest 30 digits. Further tests have shown that setting accuracy to 40 digits for
 * computation leads to about 24 digits left near poles.
 * </p>
 * <p>
 * An interesting conclusion from the above examples is that simply using better
 * arithmetic as we do here is much <em>less</em> efficient than using dedicated
 * stable algorithms (like {@link HolmesFeatherstoneAttractionModel Holmes-Featherstone}
 * algorithms.
 * </p>
 * @author Luc Maisonobe
 */
class ReferenceFieldModel {

    private final DfpField dfpField;
    private final SphericalHarmonicsProvider provider;
    private final AssociatedLegendreFunction[][] alf;

    public ReferenceFieldModel(NormalizedSphericalHarmonicsProvider provider, int digits) {
        this.dfpField = new DfpField(digits);
        this.provider = provider;
        this.alf = createAlf(provider.getMaxDegree(), provider.getMaxOrder(), true, dfpField);
    }

    public ReferenceFieldModel(UnnormalizedSphericalHarmonicsProvider provider, int digits) {
        this.dfpField = new DfpField(digits);
        this.provider = provider;
        this.alf = createAlf(provider.getMaxDegree(), provider.getMaxOrder(), false, dfpField);
    }

    private static AssociatedLegendreFunction[][] createAlf(int degree, int order, boolean isNormalized,
                                                            DfpField dfpField) {
        AssociatedLegendreFunction[][] alf = new AssociatedLegendreFunction[degree + 1][];
        for (int n = 2; n < alf.length; ++n) {
            alf[n] = new AssociatedLegendreFunction[FastMath.min(n, order) + 1];
            for (int m = 0; m < alf[n].length; ++m) {
                alf[n][m] = new AssociatedLegendreFunction(true, n, m, dfpField);
            }
        }
        return alf;
    }

    public Dfp nonCentralPart(final AbsoluteDate date, final Vector3D position)
            throws OrekitException {

        int degree = provider.getMaxDegree();
        int order  = provider.getMaxOrder();
        //use coefficients without caring if they are the correct type
        final RawSphericalHarmonics harmonics = raw(provider).onDate(date);

        Dfp x      = dfpField.newDfp(position.getX());
        Dfp y      = dfpField.newDfp(position.getY());
        Dfp z      = dfpField.newDfp(position.getZ());

        Dfp rho2     = x.multiply(x).add(y.multiply(y));
        Dfp rho      = rho2.sqrt();
        Dfp r2       = rho2.add(z.multiply(z));
        Dfp r        = r2.sqrt();
        Dfp aOr      = dfpField.newDfp(provider.getAe()).divide(r);
        Dfp lambda   = position.getX() > 0 ?
                       dfpField.getTwo().multiply(DfpMath.atan(y.divide(rho.add(x)))) :
                       dfpField.getPi().subtract(dfpField.getTwo().multiply(DfpMath.atan(y.divide(rho.subtract(x)))));
        Dfp cosTheta = z.divide(r);

        Dfp value = dfpField.getZero();
        Dfp aOrN      = aOr;
        for (int n = 2; n <= degree; ++n) {
            Dfp sum = dfpField.getZero();
            for (int m = 0; m <= FastMath.min(n, order); ++m) {
                double cnm = harmonics.getRawCnm(n, m);
                double snm = harmonics.getRawSnm(n, m);
                Dfp mLambda = lambda.multiply(m);
                Dfp c       = DfpMath.cos(mLambda).multiply(dfpField.newDfp(cnm));
                Dfp s       = DfpMath.sin(mLambda).multiply(dfpField.newDfp(snm));
                Dfp pnm     = alf[n][m].value(cosTheta);
                 sum = sum.add(pnm.multiply(c.add(s)));
            }
            aOrN = aOrN.multiply(aOr);
            value = value.add(aOrN.multiply(sum));
        }

        return value.multiply(dfpField.newDfp(provider.getMu())).divide(r);

    }

    /**
     * Wrap the given harmonics with a {@link RawSphericalHarmonicsProvider} to ignore the
     * type of the coefficients.
     *
     * @param provider harmonics provider
     * @return a raw provider wrapping {@code provider}.
     */
    private RawSphericalHarmonicsProvider raw(final SphericalHarmonicsProvider provider) {
        if (provider instanceof RawSphericalHarmonicsProvider) {
            return (RawSphericalHarmonicsProvider) provider;
        } else if (provider instanceof NormalizedSphericalHarmonicsProvider) {
            return new RawerSphericalHarmonicsProvider(provider) {
                @Override
                public RawSphericalHarmonics onDate(final AbsoluteDate date)
                        throws OrekitException {
                    final NormalizedSphericalHarmonics normalized =
                            ((NormalizedSphericalHarmonicsProvider) provider).onDate(date);
                    return new RawSphericalHarmonics() {
                        @Override
                        public double getRawCnm(int n, int m) throws OrekitException {
                            return normalized.getNormalizedCnm(n, m);
                        }

                        @Override
                        public double getRawSnm(int n, int m) throws OrekitException {
                            return normalized.getNormalizedSnm(n, m);
                        }

                        @Override
                        public AbsoluteDate getDate() {
                            return date;
                        }
                    };
                }
            };
        } else if (provider instanceof UnnormalizedSphericalHarmonicsProvider) {
            return new RawerSphericalHarmonicsProvider(provider) {
                @Override
                public RawSphericalHarmonics onDate(final AbsoluteDate date)
                        throws OrekitException {
                    final UnnormalizedSphericalHarmonics unnormalized =
                            ((UnnormalizedSphericalHarmonicsProvider) provider).onDate(date);
                    return new RawSphericalHarmonics() {
                        @Override
                        public double getRawCnm(int n, int m) throws OrekitException {
                            return unnormalized.getUnnormalizedCnm(n, m);
                        }

                        @Override
                        public double getRawSnm(int n, int m) throws OrekitException {
                            return unnormalized.getUnnormalizedSnm(n, m);
                        }

                        @Override
                        public AbsoluteDate getDate() {
                            return date;
                        }
                    };
                }
            };
        } else {
            throw new RuntimeException("Unknown harmonics provider type: " + provider);
        }
    }

    /** Delegating Provider class */
    public static abstract class RawerSphericalHarmonicsProvider
            implements RawSphericalHarmonicsProvider {

        /** wrapped provider */
        private final SphericalHarmonicsProvider provider;

        /**
         * Wrap the given provider.
         *
         * @param provider the provider to delegate to
         */
        public RawerSphericalHarmonicsProvider(SphericalHarmonicsProvider provider) {
            this.provider = provider;
        }

        public int getMaxDegree() {
            return provider.getMaxDegree();
        }

        public int getMaxOrder() {
            return provider.getMaxOrder();
        }

        public double getMu() {
            return provider.getMu();
        }

        public double getAe() {
            return provider.getAe();
        }

        public AbsoluteDate getReferenceDate() {
            return provider.getReferenceDate();
        }

        public double getOffset(AbsoluteDate date) {
            return provider.getOffset(date);
        }

        public TideSystem getTideSystem() {
            return provider.getTideSystem();
        }
    }
}
