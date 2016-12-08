/* Copyright 20 fiel02-2016 CS SystèmesIn>fo5rmation.00
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
package org.orekit.propagation.analytical;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.FieldAttitudeProvider;
import org.orekit.attitudes.FieldInertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTimeSpanMap;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** This class propagates a {@link org.orekit.propagation.FieldSpacecraftState}
 *  using the analytical Eckstein-Hechler model.
 * <p>The Eckstein-Hechler model is suited for near circular orbits
 * (e < 0.1, with poor accuracy between 0.005 and 0.1) and inclination
 * neither equatorial (direct or retrograde) nor critical (direct or
 * retrograde).</p>
 * @see FieldOrbit
 * @author Guylaine Prat
 */
public class FieldEcksteinHechlerPropagator<T extends RealFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

    /** Factory for the derivatives. */
    private final FDSFactory<T> factory;

    /** Initial Eckstein-Hechler model. */
    private FieldEHModel<T> initialModel;

    /** All models. */
    private transient FieldTimeSpanMap<FieldEHModel<T>, T> models;

    /** Reference radius of the central body attraction model (m). */
    private double referenceRadius;

    /** Central attraction coefficient (m³/s²). */
    private double mu;

    /** Un-normalized zonal coefficients. */
    private T[] ck0;

    /** Build a propagator from FieldOrbit<T> and potential provider.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial FieldOrbit<T>
     * @param provider for un-normalized zonal coefficients
     * @exception OrekitException if the zonal coefficients cannot be retrieved
     * or if the mean parameters cannot be computed
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                     final UnnormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {
        this(initialOrbit, new FieldInertialProvider<T>(initialOrbit.getA().getField()), initialOrbit.getA().getField().getZero().add(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()));
    }

    /**
     * Private helper constructor.
     * @param initialOrbit initial FieldOrbit<T>
     * @param attitude attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(initialOrbit.getDate())}
     * @exception OrekitException if the zonal coefficients cannot be retrieved
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                     final FieldAttitudeProvider<T> attitude,
                                     final T mass,
                                     final UnnormalizedSphericalHarmonicsProvider provider,
                                     final UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics harmonics)
        throws OrekitException {
        this(initialOrbit, attitude, mass, provider.getAe(), provider.getMu(),
             mass.getField().getZero().add(harmonics.getUnnormalizedCnm(2, 0)),
             mass.getField().getZero().add(harmonics.getUnnormalizedCnm(3, 0)),
             mass.getField().getZero().add(harmonics.getUnnormalizedCnm(4, 0)),
             mass.getField().getZero().add(harmonics.getUnnormalizedCnm(5, 0)),
             mass.getField().getZero().add(harmonics.getUnnormalizedCnm(6, 0)));
    }

    /** Build a propagator from FieldOrbit<T> and potential.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <pre>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                      <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * @param initialOrbit initial FieldOrbit<T>
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @exception OrekitException if the mean parameters cannot be computed
     * @see org.orekit.utils.Constants
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                     final double referenceRadius, final double mu,
                                     final T c20, final T c30, final T c40,
                                     final T c50, final T c60)
        throws OrekitException {
        this(initialOrbit, new FieldInertialProvider<T>(initialOrbit.getA().getField()), c20.getField().getZero().add(DEFAULT_MASS), referenceRadius, mu, c20, c30, c40, c50, c60);
    }

    /** Build a propagator from FieldOrbit<T>, mass and potential provider.
     * <p>Attitude law is set to an unspecified non-null arbitrary value.</p>
     * @param initialOrbit initial FieldOrbit<T>
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @exception OrekitException if the zonal coefficients cannot be retrieved
     * or if the mean parameters cannot be computed
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit, final T mass,
                                     final UnnormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {
        this(initialOrbit, new FieldInertialProvider<T>(initialOrbit.getA().getField()), mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()));
    }

    /** Build a propagator from FieldOrbit<T>, mass and potential.
     * <p>Attitude law is set to an unspecified non-null arbitrary value.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <pre>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                      <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * @param initialOrbit initial FieldOrbit<T>
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @exception OrekitException if the mean parameters cannot be computed
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit, final T mass,
                                     final double referenceRadius, final double mu,
                                     final T c20, final T c30, final T c40,
                                     final T c50, final T c60)
        throws OrekitException {
        this(initialOrbit, new FieldInertialProvider<T>(initialOrbit.getA().getField()), mass, referenceRadius, mu, c20, c30, c40, c50, c60);
    }

    /** Build a propagator from FieldOrbit<T>, attitude provider and potential provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialOrbit initial FieldOrbit<T>
     * @param attitudeProv attitude provider
     * @param provider for un-normalized zonal coefficients
     * @exception OrekitException if the zonal coefficients cannot be retrieved
     * or if the mean parameters cannot be computed
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                     final FieldAttitudeProvider<T> attitudeProv,
                                     final UnnormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {
        this(initialOrbit, attitudeProv, initialOrbit.getA().getField().getZero().add(DEFAULT_MASS), provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()));
    }

    /** Build a propagator from FieldOrbit<T>, attitude provider and potential.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <pre>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                     <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * @param initialOrbit initial FieldOrbit<T>
     * @param attitudeProv attitude provider
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @exception OrekitException if the mean parameters cannot be computed
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                     final FieldAttitudeProvider<T> attitudeProv,
                                     final double referenceRadius, final double mu,
                                     final T c20, final T c30, final T c40,
                                     final T c50, final T c60)
        throws OrekitException {
        this(initialOrbit, attitudeProv, c20.getField().getZero().add(DEFAULT_MASS), referenceRadius, mu, c20, c30, c40, c50, c60);
    }

    /** Build a propagator from FieldOrbit<T>, attitude provider, mass and potential provider.
     * @param initialOrbit initial FieldOrbit<T>
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @exception OrekitException if the zonal coefficients cannot be retrieved
     * or if the mean parameters cannot be computed
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                     final FieldAttitudeProvider<T> attitudeProv,
                                     final T mass,
                                     final UnnormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {
        this(initialOrbit, attitudeProv, mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()));
    }

    /** Build a propagator from FieldOrbit<T>, attitude provider, mass and potential.
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <pre>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                      <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * @param initialOrbit initial FieldOrbit<T>
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @exception OrekitException if the mean parameters cannot be computed
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                     final FieldAttitudeProvider<T> attitudeProv,
                                     final T mass,
                                     final double referenceRadius, final double mu,
                                     final T c20, final T c30, final T c40,
                                     final T c50, final T c60)
        throws OrekitException {

        super(mass.getField(), attitudeProv);
        final Field<T> field = mass.getField();
        final T zero = field.getZero();
        factory = new FDSFactory<>(field, 1, 2);
        try {

            // store model coefficients
            this.referenceRadius = referenceRadius;
            this.mu  = mu;
            this.ck0 = MathArrays.buildArray(field, 7);
            this.ck0[0] = zero;
            this.ck0[1] = zero;
            this.ck0[2] = c20;
            this.ck0[3] = c30;
            this.ck0[4] = c40;
            this.ck0[5] = c50;
            this.ck0[6] = c60;

            // compute mean parameters
            // transform into circular adapted parameters used by the Eckstein-Hechler model
            resetInitialState(new FieldSpacecraftState<T>(initialOrbit,
                                                  attitudeProv.getAttitude(initialOrbit,
                                                                           initialOrbit.getDate(),
                                                                           initialOrbit.getFrame()),
                                                  mass));

        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

    /** {@inheritDoc} */
    public void resetInitialState(final FieldSpacecraftState<T> state)
        throws OrekitException {
        super.resetInitialState(state);
        this.initialModel = computeMeanParameters((FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(state.getOrbit()),
                                                  state.getMass());
        this.models       = new FieldTimeSpanMap<FieldEHModel<T>, T>(initialModel, state.getA().getField());
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward)
        throws OrekitException {
        final FieldEHModel<T> newModel = computeMeanParameters((FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(state.getOrbit()),
                                                       state.getMass());
        if (forward) {
            models.addValidAfter(newModel, state.getDate());
        } else {
            models.addValidBefore(newModel, state.getDate());
        }
    }

    /** Compute mean parameters according to the Eckstein-Hechler analytical model.
     * @param osculating osculating FieldOrbit<T>
     * @param mass constant mass
     * @return Eckstein-Hechler mean model
     * @exception OrekitException if FieldOrbit<T> goes outside of supported range
     * (trajectory inside the Brillouin sphere, too eccentric, equatorial, critical
     * inclination) or if convergence cannot be reached
     */
    private FieldEHModel<T> computeMeanParameters(final FieldCircularOrbit<T> osculating, final T mass)
        throws OrekitException {

        // sanity check
        if (osculating.getA().getReal() < referenceRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE,
                                           osculating.getA());
        }
        final Field<T> field = mass.getField();
        final T one = field.getOne();
        final T zero = field.getZero();
        // rough initialization of the mean parameters
        FieldEHModel<T> current = new FieldEHModel<T>(factory, osculating, mass, referenceRadius, mu, ck0);
        // threshold for each parameter
        final T epsilon         = one .multiply(1.0e-13);
        final T thresholdA      = epsilon.multiply(current.mean.getA().abs().add(1.0));
        final T thresholdE      = epsilon.multiply(current.mean.getE().add(1.0));
        final T thresholdAngles = epsilon.multiply(FastMath.PI);


        int i = 0;
        while (i++ < 100) {

            // recompute the osculating parameters from the current mean parameters
            final FieldDerivativeStructure<T>[] parameters = current.propagateParameters(current.mean.getDate());
            // adapted parameters residuals
            final T deltaA      = osculating.getA()         .subtract(parameters[0].getValue());
            final T deltaEx     = osculating.getCircularEx().subtract(parameters[1].getValue());
            final T deltaEy     = osculating.getCircularEy().subtract(parameters[2].getValue());
            final T deltaI      = osculating.getI()         .subtract(parameters[3].getValue());
            final T deltaRAAN   = normalizeAngle(osculating.getRightAscensionOfAscendingNode().subtract(
                                                                parameters[4].getValue()),
                                                                zero);
            final T deltaAlphaM = normalizeAngle(osculating.getAlphaM().subtract(parameters[5].getValue()), zero);
            // update mean parameters
            current = new FieldEHModel<T>(factory,
                                          new FieldCircularOrbit<T>(current.mean.getA().add(deltaA),
                                                    current.mean.getCircularEx().add( deltaEx),
                                                    current.mean.getCircularEy().add( deltaEy),
                                                    current.mean.getI()         .add( deltaI ),
                                                    current.mean.getRightAscensionOfAscendingNode().add(deltaRAAN),
                                                    current.mean.getAlphaM().add(deltaAlphaM),
                                                    PositionAngle.MEAN,
                                                    current.mean.getFrame(),
                                                    current.mean.getDate(), mu),
                                  mass, referenceRadius, mu, ck0);
            // check convergence
            if ((FastMath.abs(deltaA.getReal())      < thresholdA.getReal()) &&
                (FastMath.abs(deltaEx.getReal())     < thresholdE.getReal()) &&
                (FastMath.abs(deltaEy.getReal())     < thresholdE.getReal()) &&
                (FastMath.abs(deltaI.getReal())      < thresholdAngles.getReal()) &&
                (FastMath.abs(deltaRAAN.getReal())   < thresholdAngles.getReal()) &&
                (FastMath.abs(deltaAlphaM.getReal()) < thresholdAngles.getReal())) {
                return current;
            }

        }

        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS, i);

    }

    /** {@inheritDoc} */
    public FieldCartesianOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date)
        throws OrekitException {
        // compute Cartesian parameters, taking derivatives into account
        // to make sure velocity and acceleration are consistent
        final FieldEHModel<T> current = models.get(date);
        return new FieldCartesianOrbit<T>(toCartesian(date, current.propagateParameters(date)),
                                          current.mean.getFrame(), mu);
    }

    /** Local class for Eckstein-Hechler model, with fixed mean parameters. */
    private static class FieldEHModel<T extends RealFieldElement<T>> {

        /** Factory for the derivatives. */
        private final FDSFactory<T> factory;

        /** Mean FieldOrbit<T>. */
        private final FieldCircularOrbit<T> mean;

        /** Constant mass. */
        private final T mass;
        // CHECKSTYLE: stop JavadocVariable check

        // preprocessed values
        private final T xnotDot;
        private final T rdpom;
        private final T rdpomp;
        private final T eps1;
        private final T eps2;
        private final T xim;
        private final T ommD;
        private final T rdl;
        private final T aMD;

        private final T kh;
        private final T kl;

        private final T ax1;
        private final T ay1;
        private final T as1;
        private final T ac2;
        private final T axy3;
        private final T as3;
        private final T ac4;
        private final T as5;
        private final T ac6;

        private final T ex1;
        private final T exx2;
        private final T exy2;
        private final T ex3;
        private final T ex4;

        private final T ey1;
        private final T eyx2;
        private final T eyy2;
        private final T ey3;
        private final T ey4;

        private final T rx1;
        private final T ry1;
        private final T r2;
        private final T r3;
        private final T rl;

        private final T iy1;
        private final T ix1;
        private final T i2;
        private final T i3;
        private final T ih;

        private final T lx1;
        private final T ly1;
        private final T l2;
        private final T l3;
        private final T ll;

        // CHECKSTYLE: resume JavadocVariable check

        /** Create a model for specified mean FieldOrbit<T>.
         * @param factory factory for the derivatives
         * @param mean mean FieldOrbit<T>
         * @param mass constant mass
         * @param referenceRadius reference radius of the central body attraction model (m)
         * @param mu central attraction coefficient (m³/s²)
         * @param ck0 un-normalized zonal coefficients
         * @exception OrekitException if mean FieldOrbit<T> is not within model supported domain
         */
        FieldEHModel(final FDSFactory<T> factory, final FieldCircularOrbit<T> mean, final T mass,
                     final double referenceRadius, final double mu, final T[] ck0)
            throws OrekitException {

            this.factory         = factory;
            this.mean            = mean;
            this.mass            = mass;
            final T zero = mass.getField().getZero();
            final T one  = mass.getField().getOne();
            // preliminary processing
            T q =  zero.add(referenceRadius).divide(mean.getA());
            T ql = q.multiply(q);
            final T g2 = ck0[2].multiply(ql);
            ql = ql.multiply(q);
            final T g3 = ck0[3].multiply(ql);
            ql = ql.multiply(q);
            final T g4 = ck0[4].multiply(ql);
            ql = ql.multiply(q);
            final T g5 = ck0[5].multiply(ql);
            ql = ql.multiply(q);
            final T g6 = ck0[6].multiply(ql);

            final T cosI1 = mean.getI().cos();
            final T sinI1 = mean.getI().sin();
            final T sinI2 = sinI1.multiply(sinI1);
            final T sinI4 = sinI2.multiply(sinI2);
            final T sinI6 = sinI2.multiply(sinI4);

            if (sinI2.getReal() < 1.0e-10) {
                throw new OrekitException(OrekitMessages.ALMOST_EQUATORIAL_ORBIT,
                                               FastMath.toDegrees(mean.getI().getReal()));
            }

            if (FastMath.abs(sinI2.getReal() - 4.0 / 5.0) < 1.0e-3) {
                throw new OrekitException(OrekitMessages.ALMOST_CRITICALLY_INCLINED_ORBIT,
                                               FastMath.toDegrees(mean.getI().getReal()));
            }

            if (mean.getE().getReal() > 0.1) {
                // if 0.005 < e < 0.1 no error is triggered, but accuracy is poor
                throw new OrekitException(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL,
                                               mean.getE());
            }

            xnotDot = zero.add(mu).divide(mean.getA()).sqrt().divide(mean.getA());

            rdpom = g2.multiply(-0.75).multiply(sinI2.multiply(-5.0).add(4.0));
            rdpomp = g4.multiply(7.5).multiply(sinI2.multiply(-31.0 / 8.0).add(1.0).add( sinI4.multiply(49.0 / 16.0))).subtract(
                    g6.multiply(13.125).multiply(one.subtract(sinI2.multiply(8.0)).add(sinI4.multiply(129.0 / 8.0)).subtract(sinI6.multiply(297.0 / 32.0)) ));


            q = zero.add(3.0).divide(rdpom.multiply(32.0));
            eps1 = q.multiply(g4).multiply(sinI2).multiply(sinI2.multiply(-35.0).add(30.0)).subtract(
                   q.multiply(175.0).multiply(g6).multiply(sinI2).multiply(sinI2.multiply(-3.0).add(sinI4.multiply(2.0625)).add(1.0)));
            q = sinI1.multiply(3.0).divide(rdpom.multiply(8.0));
            eps2 = q.multiply(g3).multiply(sinI2.multiply(-5.0).add(4.0)).subtract(q.multiply(g5).multiply(sinI2.multiply(-35.0).add(sinI4.multiply(26.25)).add(10.0)));

            xim = mean.getI();
            ommD = cosI1.multiply(g2.multiply(1.50).subtract(g2.multiply(2.25).multiply(g2).multiply(sinI2.multiply(-19.0 / 6.0).add(2.5))).add(
                            g4.multiply(0.9375).multiply(sinI2.multiply(7.0).subtract(4.0))).add(
                            g6.multiply(3.28125).multiply(sinI2.multiply(-9.0).add(2.0).add(sinI4.multiply(8.25)))));

            rdl = g2.multiply(-1.50).multiply(sinI2.multiply(-4.0).add(3.0)).add(1.0);
            aMD = rdl.add(
                    g2.multiply(2.25).multiply(g2.multiply(sinI2.multiply(-263.0 / 12.0 ).add(9.0).add(sinI4.multiply(341.0 / 24.0))))).add(
                    g4.multiply(15.0 / 16.0).multiply(sinI2.multiply(-31.0).add(8.0).add(sinI4.multiply(24.5)))).add(
                    g6.multiply(105.0 / 32.0).multiply(sinI2.multiply(25.0).add(-10.0 / 3.0).subtract(sinI4.multiply(48.75)).add(sinI6.multiply(27.5))));

            final T qq   = g2.divide(rdl).multiply(-1.5);
            final T qA   = g2.multiply(0.75).multiply(g2).multiply(sinI2);
            final T qB   = g4.multiply(0.25).multiply(sinI2);
            final T qC   = g6.multiply(105.0 / 16.0).multiply(sinI2);
            final T qD   = g3.multiply(-0.75).multiply(sinI1);
            final T qE   = g5.multiply(3.75).multiply(sinI1);
            kh = zero.add(0.375).divide(rdpom);
            kl = kh.divide(sinI1);

            ax1 = qq.multiply(sinI2.multiply(-3.5).add(2.0));
            ay1 = qq.multiply(sinI2.multiply(-2.5).add(2.0));
            as1 = qD.multiply(sinI2.multiply(-5.0).add(4.0)).add(
                  qE.multiply(sinI4.multiply(2.625).add(sinI2.multiply(-3.5)).add(1.0)));
            ac2 = qq.multiply(sinI2).add(
                  qA.multiply(7.0).multiply(sinI2.multiply(-3.0).add(2.0))).add(
                  qB.multiply(sinI2.multiply(-17.5).add(15.0))).add(
                  qC.multiply(sinI2.multiply(3.0).subtract(1.0).subtract(sinI4.multiply(33.0 / 16.0))));
            axy3 = qq.multiply(3.5).multiply(sinI2);
            as3 = qD.multiply(5.0 / 3.0).multiply(sinI2).add(
                  qE.multiply(7.0 / 6.0).multiply(sinI2).multiply(sinI2.multiply(-1.125).add(1)));
            ac4 = qA.multiply(sinI2).add(
                  qB.multiply(4.375).multiply(sinI2)).add(
                  qC.multiply(0.75).multiply(sinI4.multiply(1.1).subtract(sinI2)));

            as5 = qE.multiply(21.0 / 80.0).multiply(sinI4);

            ac6 = qC.multiply(-11.0 / 80.0).multiply(sinI4);

            ex1 = qq.multiply(sinI2.multiply(-1.25).add(1.0));
            exx2 = qq.multiply(0.5).multiply(sinI2.multiply(-5.0).add(3.0));
            exy2 = qq.multiply(sinI2.multiply(-1.5).add(2.0));
            ex3 = qq.multiply(7.0 / 12.0).multiply(sinI2);
            ex4 = qq.multiply(17.0 / 8.0).multiply(sinI2);

            ey1 = qq.multiply(sinI2.multiply(-1.75).add(1.0));
            eyx2 = qq.multiply(sinI2.multiply(-3.0).add(1.0));
            eyy2 = qq.multiply(sinI2.multiply(2.0).subtract(1.5));
            ey3 = qq.multiply(7.0 / 12.0).multiply(sinI2);
            ey4 = qq.multiply(17.0 / 8.0).multiply(sinI2);

            q  = cosI1.multiply(qq).negate();
            rx1 = q.multiply(3.5);
            ry1 = q.multiply(-2.5);
            r2 = q.multiply(-0.5);
            r3 =  q.multiply(7.0 / 6.0);
            rl = g3 .multiply( cosI1).multiply(sinI2.multiply(-15.0).add(4.0)).subtract(
                 g5.multiply(2.5).multiply(cosI1).multiply(sinI2.multiply(-42.0).add(4.0).add(sinI4.multiply(52.5))));

            q = qq.multiply(0.5).multiply(sinI1).multiply(cosI1);
            iy1 =  q;
            ix1 = q.negate();
            i2 =  q;
            i3 =  q.multiply(7.0 / 3.0);
            ih = g3.negate().multiply(cosI1).multiply(sinI2.multiply(-5.0).add(4)).add(
                 g5.multiply(2.5).multiply(cosI1).multiply(sinI2.multiply(-14.0).add(4.0).add(sinI4.multiply(10.5))));
            lx1 = qq.multiply(sinI2.multiply(-77.0 / 8.0).add(7.0));
            ly1 = qq.multiply(sinI2.multiply(55.0 / 8.0).subtract(7.50));
            l2 = qq.multiply(sinI2.multiply(1.25).subtract(0.5));
            l3 = qq.multiply(sinI2.multiply(77.0 / 24.0).subtract(7.0 / 6.0));
            ll = g3.multiply(sinI2.multiply(53.0).subtract(4.0).add(sinI4.multiply(-57.5))).add(
                 g5.multiply(2.5).multiply(sinI2.multiply(-96.0).add(4.0).add(sinI4.multiply(269.5).subtract(sinI6.multiply(183.75)))));

        }

        /** Extrapolate an FieldOrbit<T> up to a specific target date.
         * @param date target date for the FieldOrbit<T>
         * @return propagated parameters
         * @exception OrekitException if some parameters are out of bounds
         */
        public FieldDerivativeStructure<T>[] propagateParameters(final FieldAbsoluteDate<T> date)
            throws OrekitException {
            final Field<T> field = date.durationFrom(mean.getDate()).getField();
            final T one = field.getOne();
            final T zero = field.getZero();
            // keplerian evolution
            final FieldDerivativeStructure<T> dt =
                    factory.build(date.durationFrom(mean.getDate()), one, zero);
            final FieldDerivativeStructure<T> xnot = dt.multiply(xnotDot);

            // secular effects

            // eccentricity
            final FieldDerivativeStructure<T> x   = xnot.multiply(rdpom.add(rdpomp));
            final FieldDerivativeStructure<T> cx  = x.cos();
            final FieldDerivativeStructure<T> sx  = x.sin();
            final FieldDerivativeStructure<T> exm = cx.multiply(mean.getCircularEx()).
                                            add(sx.multiply(eps2.subtract(one.subtract(eps1).multiply(mean.getCircularEy()))));
            final FieldDerivativeStructure<T> eym = sx.multiply(eps1.add(1.0).multiply(mean.getCircularEx())).
                                            add(cx.multiply(mean.getCircularEy().subtract(eps2))).
                                            add(eps2);
            // no secular effect on inclination

            // right ascension of ascending node
            final FieldDerivativeStructure<T> omm =
                            factory.build(normalizeAngle(mean.getRightAscensionOfAscendingNode().add(ommD.multiply(xnot.getValue())),
                                                         zero.add(FastMath.PI)),
                                          ommD.multiply(xnotDot),
                                          zero);
            // latitude argument
            final FieldDerivativeStructure<T> xlm =
                            factory.build(normalizeAngle(mean.getAlphaM().add(aMD.multiply(xnot.getValue())), zero.add(FastMath.PI)),
                                          aMD.multiply(xnotDot),
                                          zero);

            // periodical terms
            final FieldDerivativeStructure<T> cl1 = xlm.cos();
            final FieldDerivativeStructure<T> sl1 = xlm.sin();
            final FieldDerivativeStructure<T> cl2 = cl1.multiply(cl1).subtract(sl1.multiply(sl1));
            final FieldDerivativeStructure<T> sl2 = cl1.multiply(sl1).add(sl1.multiply(cl1));
            final FieldDerivativeStructure<T> cl3 = cl2.multiply(cl1).subtract(sl2.multiply(sl1));
            final FieldDerivativeStructure<T> sl3 = cl2.multiply(sl1).add(sl2.multiply(cl1));
            final FieldDerivativeStructure<T> cl4 = cl3.multiply(cl1).subtract(sl3.multiply(sl1));
            final FieldDerivativeStructure<T> sl4 = cl3.multiply(sl1).add(sl3.multiply(cl1));
            final FieldDerivativeStructure<T> cl5 = cl4.multiply(cl1).subtract(sl4.multiply(sl1));
            final FieldDerivativeStructure<T> sl5 = cl4.multiply(sl1).add(sl4.multiply(cl1));
            final FieldDerivativeStructure<T> cl6 = cl5.multiply(cl1).subtract(sl5.multiply(sl1));

            final FieldDerivativeStructure<T> qh  = eym.subtract(eps2).multiply(kh);
            final FieldDerivativeStructure<T> ql  = exm.multiply(kl);

            final FieldDerivativeStructure<T> exmCl1 = exm.multiply(cl1);
            final FieldDerivativeStructure<T> exmSl1 = exm.multiply(sl1);
            final FieldDerivativeStructure<T> eymCl1 = eym.multiply(cl1);
            final FieldDerivativeStructure<T> eymSl1 = eym.multiply(sl1);
            final FieldDerivativeStructure<T> exmCl2 = exm.multiply(cl2);
            final FieldDerivativeStructure<T> exmSl2 = exm.multiply(sl2);
            final FieldDerivativeStructure<T> eymCl2 = eym.multiply(cl2);
            final FieldDerivativeStructure<T> eymSl2 = eym.multiply(sl2);
            final FieldDerivativeStructure<T> exmCl3 = exm.multiply(cl3);
            final FieldDerivativeStructure<T> exmSl3 = exm.multiply(sl3);
            final FieldDerivativeStructure<T> eymCl3 = eym.multiply(cl3);
            final FieldDerivativeStructure<T> eymSl3 = eym.multiply(sl3);
            final FieldDerivativeStructure<T> exmCl4 = exm.multiply(cl4);
            final FieldDerivativeStructure<T> exmSl4 = exm.multiply(sl4);
            final FieldDerivativeStructure<T> eymCl4 = eym.multiply(cl4);
            final FieldDerivativeStructure<T> eymSl4 = eym.multiply(sl4);

            // semi major axis
            final FieldDerivativeStructure<T> rda = exmCl1.multiply(ax1).
                                            add(eymSl1.multiply(ay1)).
                                            add(sl1.multiply(as1)).
                                            add(cl2.multiply(ac2)).
                                            add(exmCl3.add(eymSl3).multiply(axy3)).
                                            add(sl3.multiply(as3)).
                                            add(cl4.multiply(ac4)).
                                            add(sl5.multiply(as5)).
                                            add(cl6.multiply(ac6));

            // eccentricity
            final FieldDerivativeStructure<T> rdex = cl1.multiply(ex1).
                                             add(exmCl2.multiply(exx2)).
                                             add(eymSl2.multiply(exy2)).
                                             add(cl3.multiply(ex3)).
                                             add(exmCl4.add(eymSl4).multiply(ex4));
            final FieldDerivativeStructure<T> rdey = sl1.multiply(ey1).
                                             add(exmSl2.multiply(eyx2)).
                                             add(eymCl2.multiply(eyy2)).
                                             add(sl3.multiply(ey3)).
                                             add(exmSl4.subtract(eymCl4).multiply(ey4));

            // ascending node
            final FieldDerivativeStructure<T> rdom = exmSl1.multiply(rx1).
                                             add(eymCl1.multiply(ry1)).
                                             add(sl2.multiply(r2)).
                                             add(eymCl3.subtract(exmSl3).multiply(r3)).
                                             add(ql.multiply(rl));

            // inclination
            final FieldDerivativeStructure<T> rdxi = eymSl1.multiply(iy1).
                                             add(exmCl1.multiply(ix1)).
                                             add(cl2.multiply(i2)).
                                             add(exmCl3.add(eymSl3).multiply(i3)).
                                             add(qh.multiply(ih));

            // latitude argument
            final FieldDerivativeStructure<T> rdxl = exmSl1.multiply(lx1).
                                             add(eymCl1.multiply(ly1)).
                                             add(sl2.multiply(l2)).
                                             add(exmSl3.subtract(eymCl3).multiply(l3)).
                                             add(ql.multiply(ll));
            // osculating parameters
            final FieldDerivativeStructure<T>[] FTD = MathArrays.buildArray(rdxl.getField(), 6);

            FTD[0] = rda.add(1.0).multiply(mean.getA());
            FTD[1] = rdex.add(exm);
            FTD[2] = rdey.add(eym);
            FTD[3] = rdxi.add(xim);
            FTD[4] = rdom.add(omm);
            FTD[5] = rdxl.add(xlm);
            return FTD;

        }

    }

    /** Convert circular parameters <em>with derivatives</em> to Cartesian coordinates.
     * @param date date of the FieldOrbit<T>al parameters
     * @param parameters circular parameters (a, ex, ey, i, raan, alphaM)
     * @return Cartesian coordinates consistent with values and derivatives
     */
    private TimeStampedFieldPVCoordinates<T> toCartesian(final FieldAbsoluteDate<T> date, final FieldDerivativeStructure<T>[] parameters) {

        // evaluate coordinates in the FieldOrbit<T> canonical reference frame
        final FieldDerivativeStructure<T> cosOmega = parameters[4].cos();
        final FieldDerivativeStructure<T> sinOmega = parameters[4].sin();
        final FieldDerivativeStructure<T> cosI     = parameters[3].cos();
        final FieldDerivativeStructure<T> sinI     = parameters[3].sin();
        final FieldDerivativeStructure<T> alphaE   = meanToEccentric(parameters[5], parameters[1], parameters[2]);
        final FieldDerivativeStructure<T> cosAE    = alphaE.cos();
        final FieldDerivativeStructure<T> sinAE    = alphaE.sin();
        final FieldDerivativeStructure<T> ex2      = parameters[1].multiply(parameters[1]);
        final FieldDerivativeStructure<T> ey2      = parameters[2].multiply(parameters[2]);
        final FieldDerivativeStructure<T> exy      = parameters[1].multiply(parameters[2]);
        final FieldDerivativeStructure<T> q        = ex2.add(ey2).subtract(1).negate().sqrt();
        final FieldDerivativeStructure<T> beta     = q.add(1).reciprocal();
        final FieldDerivativeStructure<T> bx2      = beta.multiply(ex2);
        final FieldDerivativeStructure<T> by2      = beta.multiply(ey2);
        final FieldDerivativeStructure<T> bxy      = beta.multiply(exy);
        final FieldDerivativeStructure<T> u        = bxy.multiply(sinAE).subtract(parameters[1].add(by2.subtract(1).multiply(cosAE)));
        final FieldDerivativeStructure<T> v        = bxy.multiply(cosAE).subtract(parameters[2].add(bx2.subtract(1).multiply(sinAE)));
        final FieldDerivativeStructure<T> x        = parameters[0].multiply(u);
        final FieldDerivativeStructure<T> y        = parameters[0].multiply(v);

        // canonical FieldOrbit<T> reference frame
        final FieldVector3D<FieldDerivativeStructure<T>> p =
                new FieldVector3D<FieldDerivativeStructure<T>>(x.multiply(cosOmega).subtract(y.multiply(cosI.multiply(sinOmega))),
                                                       x.multiply(sinOmega).add(y.multiply(cosI.multiply(cosOmega))),
                                                       y.multiply(sinI));

        // dispatch derivatives
        final FieldVector3D<T> p0 = new FieldVector3D<T>(p.getX().getValue(),
                                                         p.getY().getValue(),
                                                         p.getZ().getValue());
        final FieldVector3D<T> p1 = new FieldVector3D<T>(p.getX().getPartialDerivative(1),
                                                         p.getY().getPartialDerivative(1),
                                                         p.getZ().getPartialDerivative(1));
        final FieldVector3D<T> p2 = new FieldVector3D<T>(p.getX().getPartialDerivative(2),
                                                         p.getY().getPartialDerivative(2),
                                                         p.getZ().getPartialDerivative(2));
        return new TimeStampedFieldPVCoordinates<T>(date, p0, p1, p2);

    }

    /** Computes the eccentric latitude argument from the mean latitude argument.
     * @param alphaM = M + Ω mean latitude argument (rad)
     * @param ex e cos(Ω), first component of circular eccentricity vector
     * @param ey e sin(Ω), second component of circular eccentricity vector
     * @return the eccentric latitude argument.
     */
    private FieldDerivativeStructure<T> meanToEccentric(final FieldDerivativeStructure<T> alphaM,
                                                final FieldDerivativeStructure<T> ex,
                                                final FieldDerivativeStructure<T> ey) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
        FieldDerivativeStructure<T> alphaE        = alphaM;
        FieldDerivativeStructure<T> shift         = alphaM.getField().getZero();
        FieldDerivativeStructure<T> alphaEMalphaM = alphaM.getField().getZero();
        FieldDerivativeStructure<T> cosAlphaE     = alphaE.cos();
        FieldDerivativeStructure<T> sinAlphaE     = alphaE.sin();
        int                 iter          = 0;
        do {
            final FieldDerivativeStructure<T> f2 = ex.multiply(sinAlphaE).subtract(ey.multiply(cosAlphaE));
            final FieldDerivativeStructure<T> f1 = alphaM.getField().getOne().subtract(ex.multiply(cosAlphaE)).subtract(ey.multiply(sinAlphaE));
            final FieldDerivativeStructure<T> f0 = alphaEMalphaM.subtract(f2);

            final FieldDerivativeStructure<T> f12 = f1.multiply(2);
            shift = f0.multiply(f12).divide(f1.multiply(f12).subtract(f0.multiply(f2)));

            alphaEMalphaM  = alphaEMalphaM.subtract(shift);
            alphaE         = alphaM.add(alphaEMalphaM);
            cosAlphaE      = alphaE.cos();
            sinAlphaE      = alphaE.sin();

        } while ((++iter < 50) && (FastMath.abs(shift.getValue().getReal()) > 1.0e-12));

        return alphaE;

    }

    /** {@inheritDoc} */
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return models.get(date).mass;
    }
    /**
     * Normalize an angle in a 2&pi; wide interval around a center value.
     * <p>This method has three main uses:</p>
     * <ul>
     *   <li>normalize an angle between 0 and 2&pi;:<br/>
     *       {@code a = MathUtils.normalizeAngle(a, FastMath.PI);}</li>
     *   <li>normalize an angle between -&pi; and +&pi;<br/>
     *       {@code a = MathUtils.normalizeAngle(a, 0.0);}</li>
     *   <li>compute the angle between two defining angular positions:<br>
     *       {@code angle = MathUtils.normalizeAngle(end, start) - start;}</li>
     * </ul>
     * <p>Note that due to numerical accuracy and since &pi; cannot be represented
     * exactly, the result interval is <em>closed</em>, it cannot be half-closed
     * as would be more satisfactory in a purely mathematical view.</p>
     * @param a angle to normalize
     * @param center center of the desired 2&pi; interval for the result
     * @param <T> the type of the field elements
     * @return a-2k&pi; with integer k and center-&pi; &lt;= a-2k&pi; &lt;= center+&pi;
     * @since 1.2
     */
    public static <T extends RealFieldElement<T>> T normalizeAngle(final T a, final T center) {
        return a.subtract(2 * FastMath.PI * FastMath.floor((a.getReal() + FastMath.PI - center.getReal()) / (2 * FastMath.PI)));
    }


}
