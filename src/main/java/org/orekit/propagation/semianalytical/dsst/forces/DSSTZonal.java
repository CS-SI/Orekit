/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.GHIJjsPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.LnsCoefficients;
import org.orekit.propagation.semianalytical.dsst.utilities.ShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.FieldHansenZonalLinear;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenZonalLinear;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/** Zonal contribution to the central body gravitational perturbation.
 *
 *   @author Romain Di Costanzo
 *   @author Pascal Parraud
 */
public class DSSTZonal implements DSSTForceModel {

    /** Retrograde factor I.
     *  <p>
     *  DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination
     *  is close to zero. In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial
     *  elements can be expressed in a different way, called "retrograde" orbit.
     *  This implies I = -1. <br>
     *  As Orekit doesn't implement the retrograde orbit, I is always set to +1.
     *  But for the sake of consistency with the theory, the retrograde factor
     *  has been kept in the formulas.
     *  </p>
     */
    private static final int I = 1;

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Number of points for interpolation. */
    private static final int INTERPOLATION_POINTS = 3;

    /** Provider for spherical harmonics. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegree;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegreeShortPeriodics;

    /** Highest power of the eccentricity to be used in short periodic computations. */
    private final int maxEccPowShortPeriodics;

    /** Highest power of the eccentricity. */
    private int maxEccPow;

    /** Maximum frequency in true longitude for short periodic computations. */
    private final int maxFrequencyShortPeriodics;

    /** Short period terms. */
    private ZonalShortPeriodicCoefficients zonalSPCoefs;

    /** h * k. */
    private double hk;
    /** k² - h². */
    private double k2mh2;
    /** (k² - h²) / 2. */
    private double k2mh2o2;
    /** 1 / (n² * a²). */
    private double oon2a2;
    /** 1 / (n² * a) . */
    private double oon2a;
    /** χ³ / (n² * a). */
    private double x3on2a;
    /** χ / (n² * a²). */
    private double xon2a2;
    /** (C * χ) / ( 2 * n² * a² ). */
    private double cxo2n2a2;
    /** (χ²) / (n² * a² * (χ + 1 ) ). */
    private double x2on2a2xp1;
    /** B * B.*/
    private double BB;

    /** Driver for gravitational parameter. */
    private final ParameterDriver gmParameterDriver;

    /** Hansen objects. */
    private HansenObjects hansen;

    /** Hansen objects for field elements. */
    private FieldHansenObjects fieldHansen;

    /** Simple constructor.
     * @param provider provider for spherical harmonics
     * @param maxDegreeShortPeriodics maximum degree to consider for short periodics zonal harmonics potential
     * (must be between 2 and {@code provider.getMaxDegree()})
     * @param maxEccPowShortPeriodics maximum power of the eccentricity to be used in short periodic computations
     * (must be between 0 and {@code maxDegreeShortPeriodics - 1}, but should typically not exceed 4 as higher
     * values will exceed computer capacity)
     * @param maxFrequencyShortPeriodics maximum frequency in true longitude for short periodic computations
     * (must be between 1 and {@code 2 * maxDegreeShortPeriodics + 1})
     * @exception OrekitException if degrees or powers are out of range
     * @since 7.2
     */
    public DSSTZonal(final UnnormalizedSphericalHarmonicsProvider provider,
                     final int maxDegreeShortPeriodics,
                     final int maxEccPowShortPeriodics,
                     final int maxFrequencyShortPeriodics)
        throws OrekitException {

        try {
            gmParameterDriver = new ParameterDriver(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                    provider.getMu(), MU_SCALE,
                                                    0.0, Double.POSITIVE_INFINITY);
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        }

        this.provider  = provider;
        this.maxDegree = provider.getMaxDegree();

        checkIndexRange(maxDegreeShortPeriodics, 2, provider.getMaxDegree());
        this.maxDegreeShortPeriodics = maxDegreeShortPeriodics;

        checkIndexRange(maxEccPowShortPeriodics, 0, maxDegreeShortPeriodics - 1);
        this.maxEccPowShortPeriodics = maxEccPowShortPeriodics;

        checkIndexRange(maxFrequencyShortPeriodics, 1, 2 * maxDegreeShortPeriodics + 1);
        this.maxFrequencyShortPeriodics = maxFrequencyShortPeriodics;

    }

    /** Check an index range.
     * @param index index value
     * @param min minimum value for index
     * @param max maximum value for index
     * @exception OrekitException if index is out of range
     */
    private void checkIndexRange(final int index, final int min, final int max)
        throws OrekitException {
        if (index < min || index > max) {
            throw new OrekitException(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, index, min, max);
        }
    }

    /** Initialise the HansenCoefficient generator. */
    private void initializeHansenObjects() {
        hansen = new HansenObjects();
    }

    /** Initialise the HansenCoefficient generator. */
    private void initializeFieldHansenObjects() {
        fieldHansen = new FieldHansenObjects();
    }

    /** Get the spherical harmonics provider.
     *  @return the spherical harmonics provider
     */
    public UnnormalizedSphericalHarmonicsProvider getProvider() {
        return provider;
    }

    /** {@inheritDoc}
     *  <p>
     *  Computes the highest power of the eccentricity to appear in the truncated
     *  analytical power series expansion.
     *  </p>
     *  <p>
     *  This method computes the upper value for the central body potential and
     *  determines the maximal power for the eccentricity producing potential
     *  terms bigger than a defined tolerance.
     *  </p>
     */
    @Override
    public List<ShortPeriodTerms> initialize(final AuxiliaryElements auxiliaryElements, final boolean meanOnly, final double[] parameters)
        throws OrekitException {

        final DSSTZonalContext context = new DSSTZonalContext(auxiliaryElements, provider, parameters);
        if (meanOnly) {
            maxEccPow = context.getMaxEccPowMeanElements();
        } else {
            maxEccPow = FastMath.max(context.getMaxEccPowMeanElements(), maxEccPowShortPeriodics);
        }

        initializeHansenObjects();
        initializeFieldHansenObjects();

        final List<ShortPeriodTerms> list = new ArrayList<ShortPeriodTerms>();
        zonalSPCoefs = new ZonalShortPeriodicCoefficients(maxFrequencyShortPeriodics,
                                                          INTERPOLATION_POINTS,
                                                          new TimeSpanMap<Slot>(new Slot(maxFrequencyShortPeriodics,
                                                                                         INTERPOLATION_POINTS)));
        list.add(zonalSPCoefs);
        return list;

    }

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters
     *  @return new force model context
     *  @throws OrekitException if some specific error occurs
     */
    private DSSTZonalContext initializeStep(final AuxiliaryElements auxiliaryElements, final double[] parameters)
        throws OrekitException {
        return new DSSTZonalContext(auxiliaryElements, provider, parameters);
    }

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param <T> type of the elements
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters
     *  @return new force model context
     *  @throws OrekitException if some specific error occurs
     */
    private <T extends RealFieldElement<T>> FieldDSSTZonalContext<T> initializeStep(final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                    final T[] parameters)
        throws OrekitException {
        return new FieldDSSTZonalContext<>(auxiliaryElements, provider, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getMeanElementRate(final SpacecraftState spacecraftState, final AuxiliaryElements auxiliaryElements, final double[] parameters)
        throws OrekitException {
        final DSSTZonalContext context = initializeStep(auxiliaryElements, parameters);
        return computeMeanElementRates(spacecraftState.getDate(), context, hansen);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] getMeanElementRate(final FieldSpacecraftState<T> spacecraftState,
                                                                  final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                  final T[] parameters)
        throws OrekitException {
        final FieldDSSTZonalContext<T> context = initializeStep(auxiliaryElements, parameters);
        return computeMeanElementRates(spacecraftState.getDate(), context, fieldHansen);
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** Compute the mean element rates.
     * @param date current date
     * @param context container for attributes
     * @param hansenObjects initialization of hansen objects
     * @return the mean element rates
     * @throws OrekitException if an error occurs in hansen computation
     */
    private double[] computeMeanElementRates(final AbsoluteDate date,
                                             final DSSTZonalContext context,
                                             final HansenObjects hansenObjects)
        throws OrekitException {

        // Auxiliary elements related to the current orbit
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

        // Access to potential U derivatives
        final UAnddU udu = new UAnddU(date, context, hansenObjects);

        // Compute cross derivatives [Eq. 2.2-(8)]
        // U(alpha,gamma) = alpha * dU/dgamma - gamma * dU/dalpha
        final double UAlphaGamma   = auxiliaryElements.getAlpha() * udu.getdUdGa() - auxiliaryElements.getGamma() * udu.getdUdAl();
        // U(beta,gamma) = beta * dU/dgamma - gamma * dU/dbeta
        final double UBetaGamma    =  auxiliaryElements.getBeta() * udu.getdUdGa() - auxiliaryElements.getGamma() * udu.getdUdBe();
        // Common factor
        final double pUAGmIqUBGoAB = (auxiliaryElements.getP() * UAlphaGamma - I * auxiliaryElements.getQ() * UBetaGamma) * context.getOoAB();

        // Compute mean elements rates [Eq. 3.1-(1)]
        final double da =  0.;
        final double dh =  context.getBoA() * udu.getdUdk() + auxiliaryElements.getK() * pUAGmIqUBGoAB;
        final double dk = -context.getBoA() * udu.getdUdh() - auxiliaryElements.getH() * pUAGmIqUBGoAB;
        final double dp =  context.getMCo2AB() * UBetaGamma;
        final double dq =  context.getMCo2AB() * UAlphaGamma * I;
        final double dM =  context.getM2aoA() * udu.getdUda() + context.getBoABpo() * (auxiliaryElements.getH() * udu.getdUdh() + auxiliaryElements.getK() * udu.getdUdk()) + pUAGmIqUBGoAB;

        return new double[] {da, dk, dh, dq, dp, dM};
    }

    /** Compute the mean element rates.
     * @param <T> type of the elements
     * @param date current date
     * @param context container for attributes
     * @param hansenObjects initialization of hansen objects
     * @return the mean element rates
     * @throws OrekitException if an error occurs in hansen computation
     */
    private <T extends RealFieldElement<T>> T[] computeMeanElementRates(final FieldAbsoluteDate<T> date,
                                                                        final FieldDSSTZonalContext<T> context,
                                                                        final FieldHansenObjects hansenObjects)
        throws OrekitException {

        // Auxiliary elements related to the current orbit
        final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

        // Access to potential U derivatives
        final FieldUAnddU<T> udu = new FieldUAnddU<>(date, context, hansenObjects);

        // Parameter for array building
        final Field<T> field = date.getField();

        // Compute cross derivatives [Eq. 2.2-(8)]
        // U(alpha,gamma) = alpha * dU/dgamma - gamma * dU/dalpha
        final T UAlphaGamma   = udu.getdUdGa().multiply(auxiliaryElements.getAlpha()).subtract(udu.getdUdAl().multiply(auxiliaryElements.getGamma()));
        // U(beta,gamma) = beta * dU/dgamma - gamma * dU/dbeta
        final T UBetaGamma    =  udu.getdUdGa().multiply(auxiliaryElements.getBeta()).subtract(udu.getdUdBe().multiply(auxiliaryElements.getGamma()));
        // Common factor
        final T pUAGmIqUBGoAB = (UAlphaGamma.multiply(auxiliaryElements.getP()).subtract(UBetaGamma.multiply(I).multiply(auxiliaryElements.getQ()))).multiply(context.getOoAB());

        // Compute mean elements rates [Eq. 3.1-(1)]
        final T da =  field.getZero();
        final T dh =  udu.getdUdk().multiply(context.getBoA()).add(pUAGmIqUBGoAB.multiply(auxiliaryElements.getK()));
        final T dk =  (udu.getdUdh().multiply(context.getBoA()).negate()).subtract(pUAGmIqUBGoAB.multiply(auxiliaryElements.getH()));
        final T dp =  UBetaGamma.multiply(context.getMCo2AB());
        final T dq =  UAlphaGamma.multiply(context.getMCo2AB()).multiply(I);
        final T dM =  pUAGmIqUBGoAB.add(udu.getdUda().multiply(context.getM2aoA())).add((udu.getdUdh().multiply(auxiliaryElements.getH()).add(udu.getdUdk().multiply(auxiliaryElements.getK()))).multiply(context.getBoABpo()));

        final T[] elements =  MathArrays.buildArray(field, 6);
        elements[0] = da;
        elements[1] = dk;
        elements[2] = dh;
        elements[3] = dq;
        elements[4] = dp;
        elements[5] = dM;

        return elements;
    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider attitudeProvider) {
        //nothing is done since this contribution is not sensitive to attitude
    }

    /** Check if an index is within the accepted interval.
     *
     * @param index the index to check
     * @param lowerBound the lower bound of the interval
     * @param upperBound the upper bound of the interval
     * @return true if the index is between the lower and upper bounds, false otherwise
     */
    private boolean isBetween(final int index, final int lowerBound, final int upperBound) {
        return index >= lowerBound && index <= upperBound;
    }

    /** {@inheritDoc} */
    @Override
    public void updateShortPeriodTerms(final double[] parameters, final SpacecraftState... meanStates)
        throws OrekitException {

        final Slot slot = zonalSPCoefs.createSlot(meanStates);
        for (final SpacecraftState meanState : meanStates) {

            final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(meanState.getOrbit(), I);

            final DSSTZonalContext context = initializeStep(auxiliaryElements, parameters);

            // h * k.
            this.hk = auxiliaryElements.getH() * auxiliaryElements.getK();
            // k² - h².
            this.k2mh2 = auxiliaryElements.getK() * auxiliaryElements.getK() - auxiliaryElements.getH() * auxiliaryElements.getH();
            // (k² - h²) / 2.
            this.k2mh2o2 = k2mh2 / 2.;
            // 1 / (n² * a²) = 1 / (n * A)
            this.oon2a2 = 1 / (context.getA() * auxiliaryElements.getMeanMotion());
            // 1 / (n² * a) = a / (n * A)
            this.oon2a = auxiliaryElements.getSma() * oon2a2;
            // χ³ / (n² * a)
            this.x3on2a = context.getXXX() * oon2a;
            // χ / (n² * a²)
            this.xon2a2 = context.getX() * oon2a2;
            // (C * χ) / ( 2 * n² * a² )
            this.cxo2n2a2 = xon2a2 * auxiliaryElements.getC() / 2;
            // (χ²) / (n² * a² * (χ + 1 ) )
            this.x2on2a2xp1 = xon2a2 * context.getX() / (context.getX() + 1);
            // B * B
            this.BB = auxiliaryElements.getB() * auxiliaryElements.getB();

            // Compute rhoj and sigmaj
            final double[][] rhoSigma = computeRhoSigmaCoefficients(meanState.getDate(), slot, context);

            // Compute Di
            computeDiCoefficients(meanState.getDate(), slot, context, hansen);

            // generate the Cij and Sij coefficients
            final FourierCjSjCoefficients cjsj = new FourierCjSjCoefficients(meanState.getDate(),
                                                                             maxDegreeShortPeriodics,
                                                                             maxEccPowShortPeriodics,
                                                                             maxFrequencyShortPeriodics,
                                                                             context,
                                                                             hansen);
            computeCijSijCoefficients(meanState.getDate(), slot, cjsj, rhoSigma, context, hansen);
        }

    }

    /** {@inheritDoc} */
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {
            gmParameterDriver
        };
    }

    /** Generate the values for the D<sub>i</sub> coefficients.
     * @param date target date
     * @param slot slot to which the coefficients belong
     * @param context container for attributes
     * @param hansenObjects initialization for hansen objects
     * @throws OrekitException if an error occurs during the coefficient computation
     */
    private void computeDiCoefficients(final AbsoluteDate date,
                                       final Slot slot,
                                       final DSSTZonalContext context,
                                       final HansenObjects hansenObjects)
        throws OrekitException {
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();
        final double[] meanElementRates = computeMeanElementRates(date, context, hansen);

        // Access to potential U derivatives
        final UAnddU udu = new UAnddU(date, context, hansenObjects);

        final double[] currentDi = new double[6];

        // Add the coefficients to the interpolation grid
        for (int i = 0; i < 6; i++) {
            currentDi[i] = meanElementRates[i] / auxiliaryElements.getMeanMotion();

            if (i == 5) {
                currentDi[i] += -1.5 * 2 * udu.getU() * oon2a2;
            }

        }

        slot.di.addGridPoint(date, currentDi);

    }

    /**
     * Generate the values for the C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup> coefficients.
     * @param date date of computation
     * @param slot slot to which the coefficients belong
     * @param cjsj Fourier coefficients
     * @param rhoSigma ρ<sub>j</sub> and σ<sub>j</sub>
     * @param context container for attributes
     * @param hansenObjects initialization of hansen objects
     * @throws OrekitException if an error occurs
     */
    private void computeCijSijCoefficients(final AbsoluteDate date, final Slot slot,
                                           final FourierCjSjCoefficients cjsj,
                                           final double[][] rhoSigma, final DSSTZonalContext context,
                                           final HansenObjects hansenObjects)
        throws OrekitException {

        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

        // Access to potential U derivatives
        final UAnddU udu = new UAnddU(date, context, hansenObjects);

        final int nMax = maxDegreeShortPeriodics;

        // The C<sub>i</sub>⁰ coefficients
        final double[] currentCi0 = new double[] {0., 0., 0., 0., 0., 0.};
        for (int j = 1; j < slot.cij.length; j++) {

            // Create local arrays
            final double[] currentCij = new double[] {0., 0., 0., 0., 0., 0.};
            final double[] currentSij = new double[] {0., 0., 0., 0., 0., 0.};

            // j == 1
            if (j == 1) {
                final double coef1 = 4 * auxiliaryElements.getK() * udu.getU() - hk * cjsj.getCj(1) + k2mh2o2 * cjsj.getSj(1);
                final double coef2 = 4 * auxiliaryElements.getH() * udu.getU() + k2mh2o2 * cjsj.getCj(1) + hk * cjsj.getSj(1);
                final double coef3 = (auxiliaryElements.getK() * cjsj.getCj(1) + auxiliaryElements.getH() * cjsj.getSj(1)) / 4.;
                final double coef4 = (8 * udu.getU() - auxiliaryElements.getH() * cjsj.getCj(1) + auxiliaryElements.getK() * cjsj.getSj(1)) / 4.;

                //Coefficients for a
                currentCij[0] += coef1;
                currentSij[0] += coef2;

                //Coefficients for k
                currentCij[1] += coef4;
                currentSij[1] += coef3;

                //Coefficients for h
                currentCij[2] -= coef3;
                currentSij[2] += coef4;

                //Coefficients for λ
                currentCij[5] -= coef2 / 2;
                currentSij[5] += coef1 / 2;
            }

            // j == 2
            if (j == 2) {
                final double coef1 = k2mh2 * udu.getU();
                final double coef2 = 2 * hk * udu.getU();;
                final double coef3 = auxiliaryElements.getH() * udu.getU() / 2;
                final double coef4 = auxiliaryElements.getK() * udu.getU() / 2;

                //Coefficients for a
                currentCij[0] += coef1;
                currentSij[0] += coef2;

                //Coefficients for k
                currentCij[1] += coef4;
                currentSij[1] += coef3;

                //Coefficients for h
                currentCij[2] -= coef3;
                currentSij[2] += coef4;

                //Coefficients for λ
                currentCij[5] -= coef2 / 2;
                currentSij[5] += coef1 / 2;
            }

            // j between 1 and 2N-3
            if (isBetween(j, 1, 2 * nMax - 3) && j + 2 < cjsj.jMax) {
                final double coef1 = ( j + 2 ) * (-hk * cjsj.getCj(j + 2) + k2mh2o2 * cjsj.getSj(j + 2));
                final double coef2 = ( j + 2 ) * (k2mh2o2 * cjsj.getCj(j + 2) + hk * cjsj.getSj(j + 2));
                final double coef3 = ( j + 2 ) * (auxiliaryElements.getK() * cjsj.getCj(j + 2) + auxiliaryElements.getH() * cjsj.getSj(j + 2)) / 4;
                final double coef4 = ( j + 2 ) * (auxiliaryElements.getH() * cjsj.getCj(j + 2) - auxiliaryElements.getK() * cjsj.getSj(j + 2)) / 4;

                //Coefficients for a
                currentCij[0] += coef1;
                currentSij[0] -= coef2;

                //Coefficients for k
                currentCij[1] += -coef4;
                currentSij[1] -= coef3;

                //Coefficients for h
                currentCij[2] -= coef3;
                currentSij[2] += coef4;

                //Coefficients for λ
                currentCij[5] -= coef2 / 2;
                currentSij[5] += -coef1 / 2;
            }

            // j between 1 and 2N-2
            if (isBetween(j, 1, 2 * nMax - 2) && j + 1 < cjsj.jMax) {
                final double coef1 = 2 * ( j + 1 ) * (-auxiliaryElements.getH() * cjsj.getCj(j + 1) + auxiliaryElements.getK() * cjsj.getSj(j + 1));
                final double coef2 = 2 * ( j + 1 ) * (auxiliaryElements.getK() * cjsj.getCj(j + 1) + auxiliaryElements.getH() * cjsj.getSj(j + 1));
                final double coef3 = ( j + 1 ) * cjsj.getCj(j + 1);
                final double coef4 = ( j + 1 ) * cjsj.getSj(j + 1);

                //Coefficients for a
                currentCij[0] += coef1;
                currentSij[0] -= coef2;

                //Coefficients for k
                currentCij[1] += coef4;
                currentSij[1] -= coef3;

                //Coefficients for h
                currentCij[2] -= coef3;
                currentSij[2] -= coef4;

                //Coefficients for λ
                currentCij[5] -= coef2 / 2;
                currentSij[5] += -coef1 / 2;
            }

            // j between 2 and 2N
            if (isBetween(j, 2, 2 * nMax) && j - 1 < cjsj.jMax) {
                final double coef1 = 2 * ( j - 1 ) * (auxiliaryElements.getH() * cjsj.getCj(j - 1) + auxiliaryElements.getK() * cjsj.getSj(j - 1));
                final double coef2 = 2 * ( j - 1 ) * (auxiliaryElements.getK() * cjsj.getCj(j - 1) - auxiliaryElements.getH() * cjsj.getSj(j - 1));
                final double coef3 = ( j - 1 ) * cjsj.getCj(j - 1);
                final double coef4 = ( j - 1 ) * cjsj.getSj(j - 1);

                //Coefficients for a
                currentCij[0] += coef1;
                currentSij[0] -= coef2;

                //Coefficients for k
                currentCij[1] += coef4;
                currentSij[1] -= coef3;

                //Coefficients for h
                currentCij[2] += coef3;
                currentSij[2] += coef4;

                //Coefficients for λ
                currentCij[5] += coef2 / 2;
                currentSij[5] += coef1 / 2;
            }

            // j between 3 and 2N + 1
            if (isBetween(j, 3, 2 * nMax + 1) && j - 2 < cjsj.jMax) {
                final double coef1 = ( j - 2 ) * (hk * cjsj.getCj(j - 2) + k2mh2o2 * cjsj.getSj(j - 2));
                final double coef2 = ( j - 2 ) * (-k2mh2o2 * cjsj.getCj(j - 2) + hk * cjsj.getSj(j - 2));
                final double coef3 = ( j - 2 ) * (auxiliaryElements.getK() * cjsj.getCj(j - 2) - auxiliaryElements.getH() * cjsj.getSj(j - 2)) / 4;
                final double coef4 = ( j - 2 ) * (auxiliaryElements.getH() * cjsj.getCj(j - 2) + auxiliaryElements.getK() * cjsj.getSj(j - 2)) / 4;
                final double coef5 = ( j - 2 ) * (k2mh2o2 * cjsj.getCj(j - 2) - hk * cjsj.getSj(j - 2));

                //Coefficients for a
                currentCij[0] += coef1;
                currentSij[0] += coef2;

                //Coefficients for k
                currentCij[1] += coef4;
                currentSij[1] += -coef3;

                //Coefficients for h
                currentCij[2] += coef3;
                currentSij[2] += coef4;

                //Coefficients for λ
                currentCij[5] += coef5 / 2;
                currentSij[5] += coef1 / 2;
            }

            //multiply by the common factor
            //for a (i == 0) -> χ³ / (n² * a)
            currentCij[0] *= this.x3on2a;
            currentSij[0] *= this.x3on2a;
            //for k (i == 1) -> χ / (n² * a²)
            currentCij[1] *= this.xon2a2;
            currentSij[1] *= this.xon2a2;
            //for h (i == 2) -> χ / (n² * a²)
            currentCij[2] *= this.xon2a2;
            currentSij[2] *= this.xon2a2;
            //for λ (i == 5) -> (χ²) / (n² * a² * (χ + 1 ) )
            currentCij[5] *= this.x2on2a2xp1;
            currentSij[5] *= this.x2on2a2xp1;

            // j is between 1 and 2 * N - 1
            if (isBetween(j, 1, 2 * nMax - 1) && j < cjsj.jMax) {
                // Compute cross derivatives
                // Cj(alpha,gamma) = alpha * dC/dgamma - gamma * dC/dalpha
                final double CjAlphaGamma   = auxiliaryElements.getAlpha() * cjsj.getdCjdGamma(j) - auxiliaryElements.getGamma() * cjsj.getdCjdAlpha(j);
                // Cj(alpha,beta) = alpha * dC/dbeta - beta * dC/dalpha
                final double CjAlphaBeta   = auxiliaryElements.getAlpha() * cjsj.getdCjdBeta(j) - auxiliaryElements.getBeta() * cjsj.getdCjdAlpha(j);
                // Cj(beta,gamma) = beta * dC/dgamma - gamma * dC/dbeta
                final double CjBetaGamma    =  auxiliaryElements.getBeta() * cjsj.getdCjdGamma(j) - auxiliaryElements.getGamma() * cjsj.getdCjdBeta(j);
                // Cj(h,k) = h * dC/dk - k * dC/dh
                final double CjHK   = auxiliaryElements.getH() * cjsj.getdCjdK(j) - auxiliaryElements.getK() * cjsj.getdCjdH(j);
                // Sj(alpha,gamma) = alpha * dS/dgamma - gamma * dS/dalpha
                final double SjAlphaGamma   = auxiliaryElements.getAlpha() * cjsj.getdSjdGamma(j) - auxiliaryElements.getGamma() * cjsj.getdSjdAlpha(j);
                // Sj(alpha,beta) = alpha * dS/dbeta - beta * dS/dalpha
                final double SjAlphaBeta   = auxiliaryElements.getAlpha() * cjsj.getdSjdBeta(j) - auxiliaryElements.getBeta() * cjsj.getdSjdAlpha(j);
                // Sj(beta,gamma) = beta * dS/dgamma - gamma * dS/dbeta
                final double SjBetaGamma    =  auxiliaryElements.getBeta() * cjsj.getdSjdGamma(j) - auxiliaryElements.getGamma() * cjsj.getdSjdBeta(j);
                // Sj(h,k) = h * dS/dk - k * dS/dh
                final double SjHK   = auxiliaryElements.getH() * cjsj.getdSjdK(j) - auxiliaryElements.getK() * cjsj.getdSjdH(j);

                //Coefficients for a
                final double coef1 = this.x3on2a * (3 - BB) * j;
                currentCij[0] += coef1 * cjsj.getSj(j);
                currentSij[0] -= coef1 * cjsj.getCj(j);

                //Coefficients for k and h
                final double coef2 = auxiliaryElements.getP() * CjAlphaGamma - I * auxiliaryElements.getQ() * CjBetaGamma;
                final double coef3 = auxiliaryElements.getP() * SjAlphaGamma - I * auxiliaryElements.getQ() * SjBetaGamma;
                currentCij[1] -= this.xon2a2 * (auxiliaryElements.getH() * coef2 + BB * cjsj.getdCjdH(j) - 1.5 * auxiliaryElements.getK() * j * cjsj.getSj(j));
                currentSij[1] -= this.xon2a2 * (auxiliaryElements.getH() * coef3 + BB * cjsj.getdSjdH(j) + 1.5 * auxiliaryElements.getK() * j * cjsj.getCj(j));
                currentCij[2] += this.xon2a2 * (auxiliaryElements.getK() * coef2 + BB * cjsj.getdCjdK(j) + 1.5 * auxiliaryElements.getH() * j * cjsj.getSj(j));
                currentSij[2] += this.xon2a2 * (auxiliaryElements.getK() * coef3 + BB * cjsj.getdSjdK(j) - 1.5 * auxiliaryElements.getH() * j * cjsj.getCj(j));

                //Coefficients for q and p
                final double coef4 = CjHK - CjAlphaBeta - j * cjsj.getSj(j);
                final double coef5 = SjHK - SjAlphaBeta + j * cjsj.getCj(j);
                currentCij[3] = this.cxo2n2a2 * (-I * CjAlphaGamma + auxiliaryElements.getQ() * coef4);
                currentSij[3] = this.cxo2n2a2 * (-I * SjAlphaGamma + auxiliaryElements.getQ() * coef5);
                currentCij[4] = this.cxo2n2a2 * (-CjBetaGamma + auxiliaryElements.getP() * coef4);
                currentSij[4] = this.cxo2n2a2 * (-SjBetaGamma + auxiliaryElements.getP() * coef5);

                //Coefficients for λ
                final double coef6 = auxiliaryElements.getH() * cjsj.getdCjdH(j) + auxiliaryElements.getK() * cjsj.getdCjdK(j);
                final double coef7 = auxiliaryElements.getH() * cjsj.getdSjdH(j) + auxiliaryElements.getK() * cjsj.getdSjdK(j);
                currentCij[5] += this.oon2a2 * (-2 * auxiliaryElements.getSma() * cjsj.getdCjdA(j) + coef6 / (context.getX() + 1) + context.getX() * coef2 - 3 * cjsj.getCj(j));
                currentSij[5] += this.oon2a2 * (-2 * auxiliaryElements.getSma() * cjsj.getdSjdA(j) + coef7 / (context.getX() + 1) + context.getX() * coef3 - 3 * cjsj.getSj(j));
            }

            for (int i = 0; i < 6; i++) {
                //Add the current coefficients contribution to C<sub>i</sub>⁰
                currentCi0[i] -= currentCij[i] * rhoSigma[j][0] + currentSij[i] * rhoSigma[j][1];
            }

            // Add the coefficients to the interpolation grid
            slot.cij[j].addGridPoint(date, currentCij);
            slot.sij[j].addGridPoint(date, currentSij);

        }

        //Add C<sub>i</sub>⁰ to the interpolation grid
        slot.cij[0].addGridPoint(date, currentCi0);

    }

    /**
     * Compute the auxiliary quantities ρ<sub>j</sub> and σ<sub>j</sub>.
     * <p>
     * The expressions used are equations 2.5.3-(4) from the Danielson paper. <br/>
     *  ρ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>C<sub>j</sub>(k, h) <br/>
     *  σ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>S<sub>j</sub>(k, h) <br/>
     * </p>
     * @param date target date
     * @param slot slot to which the coefficients belong
     * @param context container for attributes
     * @return array containing ρ<sub>j</sub> and σ<sub>j</sub>
     */
    private double[][] computeRhoSigmaCoefficients(final AbsoluteDate date, final Slot slot, final DSSTZonalContext context) {
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();
        final CjSjCoefficient cjsjKH = new CjSjCoefficient(auxiliaryElements.getK(), auxiliaryElements.getH());
        final double b = 1. / (1 + auxiliaryElements.getB());

        // (-b)<sup>j</sup>
        double mbtj = 1;

        final double[][] rhoSigma = new double[slot.cij.length][2];
        for (int j = 1; j < rhoSigma.length; j++) {

            //Compute current rho and sigma;
            mbtj *= -b;
            final double coef  = (1 + j * auxiliaryElements.getB()) * mbtj;
            final double rho   = coef * cjsjKH.getCj(j);
            final double sigma = coef * cjsjKH.getSj(j);

            // Add the coefficients to the interpolation grid
            rhoSigma[j][0] = rho;
            rhoSigma[j][1] = sigma;
        }

        return rhoSigma;

    }

    /** The coefficients used to compute the short-periodic zonal contribution.
     *
     * <p>
     * Those coefficients are given in Danielson paper by expressions 4.1-(20) to 4.1.-(25)
     * </p>
     * <p>
     * The coefficients are: <br>
     * - C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> <br>
     * - ρ<sub>j</sub> and σ<sub>j</sub> <br>
     * - C<sub>i</sub>⁰
     * </p>
     *
     * @author Lucian Barbulescu
     */
    private static class ZonalShortPeriodicCoefficients implements ShortPeriodTerms {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151118L;

        /** Maximum value for j index. */
        private final int maxFrequencyShortPeriodics;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** All coefficients slots. */
        private final transient TimeSpanMap<Slot> slots;

        /** Constructor.
         * @param maxFrequencyShortPeriodics maximum value for j index
         * @param interpolationPoints number of points used in the interpolation process
         * @param slots all coefficients slots
         */
        ZonalShortPeriodicCoefficients(final int maxFrequencyShortPeriodics, final int interpolationPoints,
                                       final TimeSpanMap<Slot> slots) {

            // Save parameters
            this.maxFrequencyShortPeriodics = maxFrequencyShortPeriodics;
            this.interpolationPoints        = interpolationPoints;
            this.slots                      = slots;

        }

        /** Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        public Slot createSlot(final SpacecraftState... meanStates) {
            final Slot         slot  = new Slot(maxFrequencyShortPeriodics, interpolationPoints);
            final AbsoluteDate first = meanStates[0].getDate();
            final AbsoluteDate last  = meanStates[meanStates.length - 1].getDate();
            if (first.compareTo(last) <= 0) {
                slots.addValidAfter(slot, first);
            } else {
                slots.addValidBefore(slot, first);
            }
            return slot;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final Orbit meanOrbit) {

            // select the coefficients slot
            final Slot slot = slots.get(meanOrbit.getDate());

            // Get the True longitude L
            final double L = meanOrbit.getLv();

            // Compute the center
            final double center = L - meanOrbit.getLM();

            // Initialize short periodic variations
            final double[] shortPeriodicVariation = slot.cij[0].value(meanOrbit.getDate());
            final double[] d = slot.di.value(meanOrbit.getDate());
            for (int i = 0; i < 6; i++) {
                shortPeriodicVariation[i] +=  center * d[i];
            }

            for (int j = 1; j <= maxFrequencyShortPeriodics; j++) {
                final double[] c = slot.cij[j].value(meanOrbit.getDate());
                final double[] s = slot.sij[j].value(meanOrbit.getDate());
                final double cos = FastMath.cos(j * L);
                final double sin = FastMath.sin(j * L);
                for (int i = 0; i < 6; i++) {
                    // add corresponding term to the short periodic variation
                    shortPeriodicVariation[i] += c[i] * cos;
                    shortPeriodicVariation[i] += s[i] * sin;
                }
            }

            return shortPeriodicVariation;
        }

        /** {@inheritDoc} */
        @Override
        public String getCoefficientsKeyPrefix() {
            return "DSST-central-body-zonal-";
        }

        /** {@inheritDoc}
         * <p>
         * For zonal terms contributions,there are maxJ cj coefficients,
         * maxJ sj coefficients and 2 dj coefficients, where maxJ depends
         * on the orbit. The j index is the integer multiplier for the true
         * longitude argument in the cj and sj coefficients and the degree
         * in the polynomial dj coefficients.
         * </p>
         */
        @Override
        public Map<String, double[]> getCoefficients(final AbsoluteDate date, final Set<String> selected)
                throws OrekitException {

            // select the coefficients slot
            final Slot slot = slots.get(date);

            final Map<String, double[]> coefficients = new HashMap<String, double[]>(2 * maxFrequencyShortPeriodics + 2);
            storeIfSelected(coefficients, selected, slot.cij[0].value(date), "d", 0);
            storeIfSelected(coefficients, selected, slot.di.value(date), "d", 1);
            for (int j = 1; j <= maxFrequencyShortPeriodics; j++) {
                storeIfSelected(coefficients, selected, slot.cij[j].value(date), "c", j);
                storeIfSelected(coefficients, selected, slot.sij[j].value(date), "s", j);
            }
            return coefficients;

        }

        /** Put a coefficient in a map if selected.
         * @param map map to populate
         * @param selected set of coefficients that should be put in the map
         * (empty set means all coefficients are selected)
         * @param value coefficient value
         * @param id coefficient identifier
         * @param indices list of coefficient indices
         */
        private void storeIfSelected(final Map<String, double[]> map, final Set<String> selected,
                                     final double[] value, final String id, final int... indices) {
            final StringBuilder keyBuilder = new StringBuilder(getCoefficientsKeyPrefix());
            keyBuilder.append(id);
            for (int index : indices) {
                keyBuilder.append('[').append(index).append(']');
            }
            final String key = keyBuilder.toString();
            if (selected.isEmpty() || selected.contains(key)) {
                map.put(key, value);
            }
        }

        /** Replace the instance with a data transfer object for serialization.
         * @return data transfer object that will be serialized
         * @exception NotSerializableException if an additional state provider is not serializable
         */
        private Object writeReplace() throws NotSerializableException {

            // slots transitions
            final SortedSet<TimeSpanMap.Transition<Slot>> transitions     = slots.getTransitions();
            final AbsoluteDate[]                          transitionDates = new AbsoluteDate[transitions.size()];
            final Slot[]                                  allSlots        = new Slot[transitions.size() + 1];
            int i = 0;
            for (final TimeSpanMap.Transition<Slot> transition : transitions) {
                if (i == 0) {
                    // slot before the first transition
                    allSlots[i] = transition.getBefore();
                }
                if (i < transitionDates.length) {
                    transitionDates[i] = transition.getDate();
                    allSlots[++i]      = transition.getAfter();
                }
            }

            return new DataTransferObject(maxFrequencyShortPeriodics, interpolationPoints,
                                          transitionDates, allSlots);

        }


        /** Internal class used only for serialization. */
        private static class DataTransferObject implements Serializable {

            /** Serializable UID. */
            private static final long serialVersionUID = 20170420L;

            /** Maximum value for j index. */
            private final int maxFrequencyShortPeriodics;

            /** Number of points used in the interpolation process. */
            private final int interpolationPoints;

            /** Transitions dates. */
            private final AbsoluteDate[] transitionDates;

            /** All slots. */
            private final Slot[] allSlots;

            /** Simple constructor.
             * @param maxFrequencyShortPeriodics maximum value for j index
             * @param interpolationPoints number of points used in the interpolation process
             * @param transitionDates transitions dates
             * @param allSlots all slots
             */
            DataTransferObject(final int maxFrequencyShortPeriodics, final int interpolationPoints,
                               final AbsoluteDate[] transitionDates, final Slot[] allSlots) {
                this.maxFrequencyShortPeriodics = maxFrequencyShortPeriodics;
                this.interpolationPoints        = interpolationPoints;
                this.transitionDates            = transitionDates;
                this.allSlots                   = allSlots;
            }

            /** Replace the deserialized data transfer object with a {@link ZonalShortPeriodicCoefficients}.
             * @return replacement {@link ZonalShortPeriodicCoefficients}
             */
            private Object readResolve() {

                final TimeSpanMap<Slot> slots = new TimeSpanMap<Slot>(allSlots[0]);
                for (int i = 0; i < transitionDates.length; ++i) {
                    slots.addValidAfter(allSlots[i + 1], transitionDates[i]);
                }

                return new ZonalShortPeriodicCoefficients(maxFrequencyShortPeriodics,
                                                          interpolationPoints,
                                                          slots);

            }

        }

    }

    /** Compute the C<sup>j</sup> and the S<sup>j</sup> coefficients.
     *  <p>
     *  Those coefficients are given in Danielson paper by expressions 4.1-(13) to 4.1.-(16b)
     *  </p>
     */
    private class FourierCjSjCoefficients {

        /** The G<sub>js</sub>, H<sub>js</sub>, I<sub>js</sub> and J<sub>js</sub> polynomials. */
        private final GHIJjsPolynomials ghijCoef;

        /** L<sub>n</sub><sup>s</sup>(γ). */
        private final LnsCoefficients lnsCoef;

        /** Maximum possible value for n. */
        private final int nMax;

        /** Maximum possible value for s. */
        private final int sMax;

        /** Maximum possible value for j. */
        private final int jMax;

        /** The C<sup>j</sup> coefficients and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - C<sup>j</sup> <br/>
         * - dC<sup>j</sup> / da <br/>
         * - dC<sup>j</sup> / dh <br/>
         * - dC<sup>j</sup> / dk <br/>
         * - dC<sup>j</sup> / dα <br/>
         * - dC<sup>j</sup> / dβ <br/>
         * - dC<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final double[][] cCoef;

        /** The S<sup>j</sup> coefficients and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - S<sup>j</sup> <br/>
         * - dS<sup>j</sup> / da <br/>
         * - dS<sup>j</sup> / dh <br/>
         * - dS<sup>j</sup> / dk <br/>
         * - dS<sup>j</sup> / dα <br/>
         * - dS<sup>j</sup> / dβ <br/>
         * - dS<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final double[][] sCoef;

        /** h * &Chi;³. */
        private final double hXXX;
        /** k * &Chi;³. */
        private final double kXXX;

        /** Create a set of C<sup>j</sup> and the S<sup>j</sup> coefficients.
         *  @param date the current date
         *  @param nMax maximum possible value for n
         *  @param sMax maximum possible value for s
         *  @param jMax maximum possible value for j
         *  @param context container for attributes
         *  @param hansenObjects initialization of hansen objects
         * @throws OrekitException if an error occurs while generating the coefficients
         */
        FourierCjSjCoefficients(final AbsoluteDate date,
                                final int nMax, final int sMax, final int jMax, final DSSTZonalContext context,
                                final HansenObjects hansenObjects)
                throws OrekitException {

            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            this.ghijCoef = new GHIJjsPolynomials(auxiliaryElements.getK(), auxiliaryElements.getH(), auxiliaryElements.getAlpha(), auxiliaryElements.getBeta());
            // Qns coefficients
            final double[][] Qns  = CoefficientsFactory.computeQns(auxiliaryElements.getGamma(), nMax, nMax);

            this.lnsCoef = new LnsCoefficients(nMax, nMax, Qns, context.getVns(), context.getRoa());
            this.nMax = nMax;
            this.sMax = sMax;
            this.jMax = jMax;

            // compute the common factors that depends on the mean elements
            this.hXXX = auxiliaryElements.getH() * context.getXXX();
            this.kXXX = auxiliaryElements.getK() * context.getXXX();

            this.cCoef = new double[7][jMax + 1];
            this.sCoef = new double[7][jMax + 1];

            for (int s = 0; s <= sMax; s++) {
                //Initialise the Hansen roots
                hansenObjects.computeHansenObjectsInitValues(context, s);
            }
            generateCoefficients(date, context, hansenObjects);
        }

        /** Generate all coefficients.
         * @param date the current date
         * @param context container for attributes
         * @param hansenObjects initialization of hansen objects
         * @throws OrekitException if an error occurs while generating the coefficients
         */
        private void generateCoefficients(final AbsoluteDate date,
                                          final DSSTZonalContext context,
                                          final HansenObjects hansenObjects)
            throws OrekitException {
            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
            for (int j = 1; j <= jMax; j++) {

                //init arrays
                for (int i = 0; i <= 6; i++) {
                    cCoef[i][j] = 0.;
                    sCoef[i][j] = 0.;
                }

                if (isBetween(j, 1, nMax - 1)) {

                    //compute first double sum where s: j -> N-1 and n: s+1 -> N
                    for (int s = j; s <= FastMath.min(nMax - 1, sMax); s++) {
                        // j - s
                        final int jms = j - s;
                        // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                        final int d0smj = (s == j) ? 1 : 2;

                        for (int n = s + 1; n <= nMax; n++) {
                            // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                            if ((n + jms) % 2 == 0) {
                                // (2 - delta(0,s-j)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                final double lns = lnsCoef.getLns(n, -jms);
                                final double dlns = lnsCoef.getdLnsdGamma(n, -jms);

                                final double hjs = ghijCoef.getHjs(s, -jms);
                                final double dHjsdh = ghijCoef.getdHjsdh(s, -jms);
                                final double dHjsdk = ghijCoef.getdHjsdk(s, -jms);
                                final double dHjsdAlpha = ghijCoef.getdHjsdAlpha(s, -jms);
                                final double dHjsdBeta = ghijCoef.getdHjsdBeta(s, -jms);

                                final double gjs = ghijCoef.getGjs(s, -jms);
                                final double dGjsdh = ghijCoef.getdGjsdh(s, -jms);
                                final double dGjsdk = ghijCoef.getdGjsdk(s, -jms);
                                final double dGjsdAlpha = ghijCoef.getdGjsdAlpha(s, -jms);
                                final double dGjsdBeta = ghijCoef.getdGjsdBeta(s, -jms);

                                // J<sub>n</sub>
                                final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                // K₀<sup>-n-1,s</sup>
                                final double kns   = hansenObjects.getHansenObjects()[s].getValue(-n - 1, context.getX());
                                final double dkns  = hansenObjects.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                                final double coef0 = d0smj * jn;
                                final double coef1 = coef0 * lns;
                                final double coef2 = coef1 * kns;
                                final double coef3 = coef2 * hjs;
                                final double coef4 = coef2 * gjs;

                                // Add the term to the coefficients
                                cCoef[0][j] += coef3;
                                cCoef[1][j] += coef3 * (n + 1);
                                cCoef[2][j] += coef1 * (kns * dHjsdh + hjs * hXXX * dkns);
                                cCoef[3][j] += coef1 * (kns * dHjsdk + hjs * kXXX * dkns);
                                cCoef[4][j] += coef2 * dHjsdAlpha;
                                cCoef[5][j] += coef2 * dHjsdBeta;
                                cCoef[6][j] += coef0 * dlns * kns * hjs;

                                sCoef[0][j] += coef4;
                                sCoef[1][j] += coef4 * (n + 1);
                                sCoef[2][j] += coef1 * (kns * dGjsdh + gjs * hXXX * dkns);
                                sCoef[3][j] += coef1 * (kns * dGjsdk + gjs * kXXX * dkns);
                                sCoef[4][j] += coef2 * dGjsdAlpha;
                                sCoef[5][j] += coef2 * dGjsdBeta;
                                sCoef[6][j] += coef0 * dlns * kns * gjs;
                            }
                        }
                    }

                    //compute second double sum where s: 0 -> N-j and n: max(j+s, j+1) -> N
                    for (int s = 0; s <= FastMath.min(nMax - j, sMax); s++) {
                        // j + s
                        final int jps = j + s;
                        // Kronecker symbols (2 - delta(0,j+s))
                        final double d0spj = (s == -j) ? 1 : 2;

                        for (int n = FastMath.max(j + s, j + 1); n <= nMax; n++) {
                            // if n + (j+s) is odd, then the term is equal to zero due to the factor Vn,s+j
                            if ((n + jps) % 2 == 0) {
                                // (2 - delta(0,s+j)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j+s</sup>
                                final double lns = lnsCoef.getLns(n, jps);
                                final double dlns = lnsCoef.getdLnsdGamma(n, jps);

                                final double hjs = ghijCoef.getHjs(s, jps);
                                final double dHjsdh = ghijCoef.getdHjsdh(s, jps);
                                final double dHjsdk = ghijCoef.getdHjsdk(s, jps);
                                final double dHjsdAlpha = ghijCoef.getdHjsdAlpha(s, jps);
                                final double dHjsdBeta = ghijCoef.getdHjsdBeta(s, jps);

                                final double gjs = ghijCoef.getGjs(s, jps);
                                final double dGjsdh = ghijCoef.getdGjsdh(s, jps);
                                final double dGjsdk = ghijCoef.getdGjsdk(s, jps);
                                final double dGjsdAlpha = ghijCoef.getdGjsdAlpha(s, jps);
                                final double dGjsdBeta = ghijCoef.getdGjsdBeta(s, jps);

                                // J<sub>n</sub>
                                final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                // K₀<sup>-n-1,s</sup>
                                final double kns   = hansenObjects.getHansenObjects()[s].getValue(-n - 1, context.getX());
                                final double dkns  = hansenObjects.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                                final double coef0 = d0spj * jn;
                                final double coef1 = coef0 * lns;
                                final double coef2 = coef1 * kns;

                                final double coef3 = coef2 * hjs;
                                final double coef4 = coef2 * gjs;

                                // Add the term to the coefficients
                                cCoef[0][j] -= coef3;
                                cCoef[1][j] -= coef3 * (n + 1);
                                cCoef[2][j] -= coef1 * (kns * dHjsdh + hjs * hXXX * dkns);
                                cCoef[3][j] -= coef1 * (kns * dHjsdk + hjs * kXXX * dkns);
                                cCoef[4][j] -= coef2 * dHjsdAlpha;
                                cCoef[5][j] -= coef2 * dHjsdBeta;
                                cCoef[6][j] -= coef0 * dlns * kns * hjs;

                                sCoef[0][j] += coef4;
                                sCoef[1][j] += coef4 * (n + 1);
                                sCoef[2][j] += coef1 * (kns * dGjsdh + gjs * hXXX * dkns);
                                sCoef[3][j] += coef1 * (kns * dGjsdk + gjs * kXXX * dkns);
                                sCoef[4][j] += coef2 * dGjsdAlpha;
                                sCoef[5][j] += coef2 * dGjsdBeta;
                                sCoef[6][j] += coef0 * dlns * kns * gjs;
                            }
                        }
                    }

                    //compute third double sum where s: 1 -> j and  n: j+1 -> N
                    for (int s = 1; s <= FastMath.min(j, sMax); s++) {
                        // j - s
                        final int jms = j - s;
                        // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                        final int d0smj = (s == j) ? 1 : 2;

                        for (int n = j + 1; n <= nMax; n++) {
                            // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                            if ((n + jms) % 2 == 0) {
                                // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                final double lns = lnsCoef.getLns(n, jms);
                                final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                final double ijs = ghijCoef.getIjs(s, jms);
                                final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                final double jjs = ghijCoef.getJjs(s, jms);
                                final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                // J<sub>n</sub>
                                final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                // K₀<sup>-n-1,s</sup>
                                final double kns   = hansenObjects.getHansenObjects()[s].getValue(-n - 1, context.getX());
                                final double dkns  = hansenObjects.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                                final double coef0 = d0smj * jn;
                                final double coef1 = coef0 * lns;
                                final double coef2 = coef1 * kns;

                                final double coef3 = coef2 * ijs;
                                final double coef4 = coef2 * jjs;

                                // Add the term to the coefficients
                                cCoef[0][j] -= coef3;
                                cCoef[1][j] -= coef3 * (n + 1);
                                cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                cCoef[4][j] -= coef2 * dIjsdAlpha;
                                cCoef[5][j] -= coef2 * dIjsdBeta;
                                cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                sCoef[0][j] += coef4;
                                sCoef[1][j] += coef4 * (n + 1);
                                sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                sCoef[4][j] += coef2 * dJjsdAlpha;
                                sCoef[5][j] += coef2 * dJjsdBeta;
                                sCoef[6][j] += coef0 * dlns * kns * jjs;
                            }
                        }
                    }
                }

                if (isBetween(j, 2, nMax)) {
                    //add first term
                    // J<sub>j</sub>
                    final double jj = -harmonics.getUnnormalizedCnm(j, 0);
                    double kns = hansen.getHansenObjects()[0].getValue(-j - 1, context.getX());
                    double dkns = hansen.getHansenObjects()[0].getDerivative(-j - 1, context.getX());

                    double lns = lnsCoef.getLns(j, j);
                    //dlns is 0 because n == s == j

                    final double hjs = ghijCoef.getHjs(0, j);
                    final double dHjsdh = ghijCoef.getdHjsdh(0, j);
                    final double dHjsdk = ghijCoef.getdHjsdk(0, j);
                    final double dHjsdAlpha = ghijCoef.getdHjsdAlpha(0, j);
                    final double dHjsdBeta = ghijCoef.getdHjsdBeta(0, j);

                    final double gjs = ghijCoef.getGjs(0, j);
                    final double dGjsdh = ghijCoef.getdGjsdh(0, j);
                    final double dGjsdk = ghijCoef.getdGjsdk(0, j);
                    final double dGjsdAlpha = ghijCoef.getdGjsdAlpha(0, j);
                    final double dGjsdBeta = ghijCoef.getdGjsdBeta(0, j);

                    // 2 * J<sub>j</sub> * K₀<sup>-j-1,0</sup> * L<sub>j</sub><sup>j</sup>
                    double coef0 = 2 * jj;
                    double coef1 = coef0 * lns;
                    double coef2 = coef1 * kns;

                    double coef3 = coef2 * hjs;
                    double coef4 = coef2 * gjs;

                    // Add the term to the coefficients
                    cCoef[0][j] -= coef3;
                    cCoef[1][j] -= coef3 * (j + 1);
                    cCoef[2][j] -= coef1 * (kns * dHjsdh + hjs * hXXX * dkns);
                    cCoef[3][j] -= coef1 * (kns * dHjsdk + hjs * kXXX * dkns);
                    cCoef[4][j] -= coef2 * dHjsdAlpha;
                    cCoef[5][j] -= coef2 * dHjsdBeta;
                    //no contribution to cCoef[6][j] because dlns is 0

                    sCoef[0][j] += coef4;
                    sCoef[1][j] += coef4 * (j + 1);
                    sCoef[2][j] += coef1 * (kns * dGjsdh + gjs * hXXX * dkns);
                    sCoef[3][j] += coef1 * (kns * dGjsdk + gjs * kXXX * dkns);
                    sCoef[4][j] += coef2 * dGjsdAlpha;
                    sCoef[5][j] += coef2 * dGjsdBeta;
                    //no contribution to sCoef[6][j] because dlns is 0

                    //compute simple sum where s: 1 -> j-1
                    for (int s = 1; s <= FastMath.min(j - 1, sMax); s++) {
                        // j - s
                        final int jms = j - s;
                        // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                        final int d0smj = (s == j) ? 1 : 2;

                        // if s is odd, then the term is equal to zero due to the factor Vj,s-j
                        if (s % 2 == 0) {
                            // (2 - delta(0,j-s)) * J<sub>j</sub> * K₀<sup>-j-1,s</sup> * L<sub>j</sub><sup>j-s</sup>
                            kns   = hansenObjects.getHansenObjects()[s].getValue(-j - 1, context.getX());
                            dkns  = hansenObjects.getHansenObjects()[s].getDerivative(-j - 1, context.getX());

                            lns = lnsCoef.getLns(j, jms);
                            final double dlns = lnsCoef.getdLnsdGamma(j, jms);

                            final double ijs = ghijCoef.getIjs(s, jms);
                            final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                            final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                            final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                            final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                            final double jjs = ghijCoef.getJjs(s, jms);
                            final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                            final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                            final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                            final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                            coef0 = d0smj * jj;
                            coef1 = coef0 * lns;
                            coef2 = coef1 * kns;

                            coef3 = coef2 * ijs;
                            coef4 = coef2 * jjs;

                            // Add the term to the coefficients
                            cCoef[0][j] -= coef3;
                            cCoef[1][j] -= coef3 * (j + 1);
                            cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                            cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                            cCoef[4][j] -= coef2 * dIjsdAlpha;
                            cCoef[5][j] -= coef2 * dIjsdBeta;
                            cCoef[6][j] -= coef0 * dlns * kns * ijs;

                            sCoef[0][j] += coef4;
                            sCoef[1][j] += coef4 * (j + 1);
                            sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                            sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                            sCoef[4][j] += coef2 * dJjsdAlpha;
                            sCoef[5][j] += coef2 * dJjsdBeta;
                            sCoef[6][j] += coef0 * dlns * kns * jjs;
                        }
                    }
                }

                if (isBetween(j, 3, 2 * nMax - 1)) {
                    //compute uppercase sigma expressions

                    //min(j-1,N)
                    final int minjm1on = FastMath.min(j - 1, nMax);

                    //if j is even
                    if (j % 2 == 0) {
                        //compute first double sum where s: j-min(j-1,N) -> j/2-1 and n: j-s -> min(j-1,N)
                        for (int s = j - minjm1on; s <= FastMath.min(j / 2 - 1, sMax); s++) {
                            // j - s
                            final int jms = j - s;
                            // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                            final int d0smj = (s == j) ? 1 : 2;

                            for (int n = j - s; n <= minjm1on; n++) {
                                // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                                if ((n + jms) % 2 == 0) {
                                    // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                    final double lns = lnsCoef.getLns(n, jms);
                                    final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                    final double ijs = ghijCoef.getIjs(s, jms);
                                    final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                    final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                    final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                    final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                    final double jjs = ghijCoef.getJjs(s, jms);
                                    final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                    final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                    final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                    final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                    // J<sub>n</sub>
                                    final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                    // K₀<sup>-n-1,s</sup>
                                    final double kns   = hansenObjects.getHansenObjects()[s].getValue(-n - 1, context.getX());
                                    final double dkns  = hansenObjects.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                                    final double coef0 = d0smj * jn;
                                    final double coef1 = coef0 * lns;
                                    final double coef2 = coef1 * kns;

                                    final double coef3 = coef2 * ijs;
                                    final double coef4 = coef2 * jjs;

                                    // Add the term to the coefficients
                                    cCoef[0][j] -= coef3;
                                    cCoef[1][j] -= coef3 * (n + 1);
                                    cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                    cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                    cCoef[4][j] -= coef2 * dIjsdAlpha;
                                    cCoef[5][j] -= coef2 * dIjsdBeta;
                                    cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                    sCoef[0][j] += coef4;
                                    sCoef[1][j] += coef4 * (n + 1);
                                    sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                    sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                    sCoef[4][j] += coef2 * dJjsdAlpha;
                                    sCoef[5][j] += coef2 * dJjsdBeta;
                                    sCoef[6][j] += coef0 * dlns * kns * jjs;
                                }
                            }
                        }

                        //compute second double sum where s: j/2 -> min(j-1,N)-1 and n: s+1 -> min(j-1,N)
                        for (int s = j / 2; s <=  FastMath.min(minjm1on - 1, sMax); s++) {
                            // j - s
                            final int jms = j - s;
                            // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                            final int d0smj = (s == j) ? 1 : 2;

                            for (int n = s + 1; n <= minjm1on; n++) {
                                // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                                if ((n + jms) % 2 == 0) {
                                    // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                    final double lns = lnsCoef.getLns(n, jms);
                                    final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                    final double ijs = ghijCoef.getIjs(s, jms);
                                    final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                    final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                    final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                    final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                    final double jjs = ghijCoef.getJjs(s, jms);
                                    final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                    final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                    final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                    final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                    // J<sub>n</sub>
                                    final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                    // K₀<sup>-n-1,s</sup>
                                    final double kns   = hansenObjects.getHansenObjects()[s].getValue(-n - 1, context.getX());
                                    final double dkns  = hansenObjects.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                                    final double coef0 = d0smj * jn;
                                    final double coef1 = coef0 * lns;
                                    final double coef2 = coef1 * kns;

                                    final double coef3 = coef2 * ijs;
                                    final double coef4 = coef2 * jjs;

                                    // Add the term to the coefficients
                                    cCoef[0][j] -= coef3;
                                    cCoef[1][j] -= coef3 * (n + 1);
                                    cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                    cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                    cCoef[4][j] -= coef2 * dIjsdAlpha;
                                    cCoef[5][j] -= coef2 * dIjsdBeta;
                                    cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                    sCoef[0][j] += coef4;
                                    sCoef[1][j] += coef4 * (n + 1);
                                    sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                    sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                    sCoef[4][j] += coef2 * dJjsdAlpha;
                                    sCoef[5][j] += coef2 * dJjsdBeta;
                                    sCoef[6][j] += coef0 * dlns * kns * jjs;
                                }
                            }
                        }
                    }

                    //if j is odd
                    else {
                        //compute first double sum where s: (j-1)/2 -> min(j-1,N)-1 and n: s+1 -> min(j-1,N)
                        for (int s = (j - 1) / 2; s <= FastMath.min(minjm1on - 1, sMax); s++) {
                            // j - s
                            final int jms = j - s;
                            // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                            final int d0smj = (s == j) ? 1 : 2;

                            for (int n = s + 1; n <= minjm1on; n++) {
                                // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                                if ((n + jms) % 2 == 0) {
                                    // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                    final double lns = lnsCoef.getLns(n, jms);
                                    final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                    final double ijs = ghijCoef.getIjs(s, jms);
                                    final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                    final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                    final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                    final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                    final double jjs = ghijCoef.getJjs(s, jms);
                                    final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                    final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                    final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                    final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                    // J<sub>n</sub>
                                    final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                    // K₀<sup>-n-1,s</sup>

                                    final double kns = hansenObjects.getHansenObjects()[s].getValue(-n - 1, context.getX());
                                    final double dkns  = hansenObjects.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                                    final double coef0 = d0smj * jn;
                                    final double coef1 = coef0 * lns;
                                    final double coef2 = coef1 * kns;

                                    final double coef3 = coef2 * ijs;
                                    final double coef4 = coef2 * jjs;

                                    // Add the term to the coefficients
                                    cCoef[0][j] -= coef3;
                                    cCoef[1][j] -= coef3 * (n + 1);
                                    cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                    cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                    cCoef[4][j] -= coef2 * dIjsdAlpha;
                                    cCoef[5][j] -= coef2 * dIjsdBeta;
                                    cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                    sCoef[0][j] += coef4;
                                    sCoef[1][j] += coef4 * (n + 1);
                                    sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                    sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                    sCoef[4][j] += coef2 * dJjsdAlpha;
                                    sCoef[5][j] += coef2 * dJjsdBeta;
                                    sCoef[6][j] += coef0 * dlns * kns * jjs;
                                }
                            }
                        }

                        //the second double sum is added only if N >= 4 and j between 5 and 2*N-3
                        if (nMax >= 4 && isBetween(j, 5, 2 * nMax - 3)) {
                            //compute second double sum where s: j-min(j-1,N) -> (j-3)/2 and n: j-s -> min(j-1,N)
                            for (int s = j - minjm1on; s <= FastMath.min((j - 3) / 2, sMax); s++) {
                                // j - s
                                final int jms = j - s;
                                // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                                final int d0smj = (s == j) ? 1 : 2;

                                for (int n = j - s; n <= minjm1on; n++) {
                                    // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                                    if ((n + jms) % 2 == 0) {
                                        // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                        final double lns = lnsCoef.getLns(n, jms);
                                        final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                        final double ijs = ghijCoef.getIjs(s, jms);
                                        final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                        final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                        final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                        final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                        final double jjs = ghijCoef.getJjs(s, jms);
                                        final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                        final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                        final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                        final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                        // J<sub>n</sub>
                                        final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                        // K₀<sup>-n-1,s</sup>
                                        final double kns   = hansenObjects.getHansenObjects()[s].getValue(-n - 1, context.getX());
                                        final double dkns  = hansenObjects.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                                        final double coef0 = d0smj * jn;
                                        final double coef1 = coef0 * lns;
                                        final double coef2 = coef1 * kns;

                                        final double coef3 = coef2 * ijs;
                                        final double coef4 = coef2 * jjs;

                                        // Add the term to the coefficients
                                        cCoef[0][j] -= coef3;
                                        cCoef[1][j] -= coef3 * (n + 1);
                                        cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                        cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                        cCoef[4][j] -= coef2 * dIjsdAlpha;
                                        cCoef[5][j] -= coef2 * dIjsdBeta;
                                        cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                        sCoef[0][j] += coef4;
                                        sCoef[1][j] += coef4 * (n + 1);
                                        sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                        sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                        sCoef[4][j] += coef2 * dJjsdAlpha;
                                        sCoef[5][j] += coef2 * dJjsdBeta;
                                        sCoef[6][j] += coef0 * dlns * kns * jjs;
                                    }
                                }
                            }
                        }
                    }
                }

                cCoef[0][j] *= -context.getMuoa() / j;
                cCoef[1][j] *=  context.getMuoa() / ( j * auxiliaryElements.getSma() );
                cCoef[2][j] *= -context.getMuoa() / j;
                cCoef[3][j] *= -context.getMuoa() / j;
                cCoef[4][j] *= -context.getMuoa() / j;
                cCoef[5][j] *= -context.getMuoa() / j;
                cCoef[6][j] *= -context.getMuoa() / j;

                sCoef[0][j] *= -context.getMuoa() / j;
                sCoef[1][j] *=  context.getMuoa() / ( j * auxiliaryElements.getSma() );
                sCoef[2][j] *= -context.getMuoa() / j;
                sCoef[3][j] *= -context.getMuoa() / j;
                sCoef[4][j] *= -context.getMuoa() / j;
                sCoef[5][j] *= -context.getMuoa() / j;
                sCoef[6][j] *= -context.getMuoa() / j;

            }
        }

        /** Check if an index is within the accepted interval.
         *
         * @param index the index to check
         * @param lowerBound the lower bound of the interval
         * @param upperBound the upper bound of the interval
         * @return true if the index is between the lower and upper bounds, false otherwise
         */
        private boolean isBetween(final int index, final int lowerBound, final int upperBound) {
            return index >= lowerBound && index <= upperBound;
        }

        /**Get the value of C<sup>j</sup>.
         *
         * @param j j index
         * @return C<sup>j</sup>
         */
        public double getCj(final int j) {
            return cCoef[0][j];
        }

        /**Get the value of dC<sup>j</sup> / da.
         *
         * @param j j index
         * @return dC<sup>j</sup> / da
         */
        public double getdCjdA(final int j) {
            return cCoef[1][j];
        }

        /**Get the value of dC<sup>j</sup> / dh.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dh
         */
        public double getdCjdH(final int j) {
            return cCoef[2][j];
        }

        /**Get the value of dC<sup>j</sup> / dk.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dk
         */
        public double getdCjdK(final int j) {
            return cCoef[3][j];
        }

        /**Get the value of dC<sup>j</sup> / dα.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dα
         */
        public double getdCjdAlpha(final int j) {
            return cCoef[4][j];
        }

        /**Get the value of dC<sup>j</sup> / dβ.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dβ
         */
        public double getdCjdBeta(final int j) {
            return cCoef[5][j];
        }

        /**Get the value of dC<sup>j</sup> / dγ.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dγ
         */
        public double getdCjdGamma(final int j) {
            return cCoef[6][j];
        }

        /**Get the value of S<sup>j</sup>.
         *
         * @param j j index
         * @return S<sup>j</sup>
         */
        public double getSj(final int j) {
            return sCoef[0][j];
        }

        /**Get the value of dS<sup>j</sup> / da.
         *
         * @param j j index
         * @return dS<sup>j</sup> / da
         */
        public double getdSjdA(final int j) {
            return sCoef[1][j];
        }

        /**Get the value of dS<sup>j</sup> / dh.
         *
         * @param j j index
         * @return dS<sup>j</sup> / dh
         */
        public double getdSjdH(final int j) {
            return sCoef[2][j];
        }

        /**Get the value of dS<sup>j</sup> / dk.
         *
         * @param j j index
         * @return dS<sup>j</sup> / dk
         */
        public double getdSjdK(final int j) {
            return sCoef[3][j];
        }

        /**Get the value of dS<sup>j</sup> / dα.
         *
         * @param j j index
         * @return dS<sup>j</sup> / dα
         */
        public double getdSjdAlpha(final int j) {
            return sCoef[4][j];
        }

        /**Get the value of dS<sup>j</sup> / dβ.
         *
         * @param j j index
         * @return dS<sup>j</sup> / dβ
         */
        public double getdSjdBeta(final int j) {
            return sCoef[5][j];
        }

        /**Get the value of dS<sup>j</sup> /  dγ.
         *
         * @param j j index
         * @return dS<sup>j</sup> /  dγ
         */
        public double getdSjdGamma(final int j) {
            return sCoef[6][j];
        }
    }

    /** Coefficients valid for one time slot. */
    private static class Slot implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20160319L;

        /**The coefficients D<sub>i</sub>.
         * <p>
         * i corresponds to the equinoctial element, as follows:
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient di;

        /** The coefficients C<sub>i</sub><sup>j</sup>.
         * <p>
         * The constant term C<sub>i</sub>⁰ is also stored in this variable at index j = 0 <br>
         * The index order is cij[j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] cij;

        /** The coefficients S<sub>i</sub><sup>j</sup>.
         * <p>
         * The index order is sij[j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] sij;

        /** Simple constructor.
         *  @param maxFrequencyShortPeriodics maximum value for j index
         *  @param interpolationPoints number of points used in the interpolation process
         */
        Slot(final int maxFrequencyShortPeriodics, final int interpolationPoints) {

            final int rows = maxFrequencyShortPeriodics + 1;
            di  = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
            cij = new ShortPeriodicsInterpolatedCoefficient[rows];
            sij = new ShortPeriodicsInterpolatedCoefficient[rows];

            //Initialize the arrays
            for (int j = 0; j <= maxFrequencyShortPeriodics; j++) {
                cij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                sij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
            }

        }

    }

    /** Compute potential and potential derivatives with respect to orbital parameters. */
    private class UAnddU {

        /** The current value of the U function. <br/>
         * Needed for the short periodic contribution */
        private double U;

        /** dU / da. */
        private  double dUda;

        /** dU / dk. */
        private double dUdk;

        /** dU / dh. */
        private double dUdh;

        /** dU / dAlpha. */
        private double dUdAl;

        /** dU / dBeta. */
        private double dUdBe;

        /** dU / dGamma. */
        private double dUdGa;

        /** Simple constuctor.
         *  @param date current date
         *  @param context container for attributes
         *  @param hansen initialization of hansen objects
         *  @throws OrekitException if an error occurs in hansen computation
         */
        UAnddU(final AbsoluteDate date,
               final DSSTZonalContext context,
               final HansenObjects hansen)
            throws OrekitException {

            // Auxiliary elements related to the current orbit
            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);

            //Reset U
            U = 0.;

            // Gs and Hs coefficients
            final double[][] GsHs = CoefficientsFactory.computeGsHs(auxiliaryElements.getK(), auxiliaryElements.getH(), auxiliaryElements.getAlpha(), auxiliaryElements.getBeta(), context.getMaxEccPowMeanElements());
            // Qns coefficients
            final double[][] Qns  = CoefficientsFactory.computeQns(auxiliaryElements.getGamma(), context.getMaxDegree(), context.getMaxEccPowMeanElements());

            final double[] roaPow = new double[context.getMaxDegree() + 1];
            roaPow[0] = 1.;
            for (int i = 1; i <= context.getMaxDegree(); i++) {
                roaPow[i] = context.getRoa() * roaPow[i - 1];
            }

            // Potential derivatives
            dUda  = 0.;
            dUdk  = 0.;
            dUdh  = 0.;
            dUdAl = 0.;
            dUdBe = 0.;
            dUdGa = 0.;

            for (int s = 0; s <= context.getMaxEccPowMeanElements(); s++) {
                //Initialize the Hansen roots
                hansen.computeHansenObjectsInitValues(context, s);

                // Get the current Gs coefficient
                final double gs = GsHs[0][s];

                // Compute Gs partial derivatives from 3.1-(9)
                double dGsdh  = 0.;
                double dGsdk  = 0.;
                double dGsdAl = 0.;
                double dGsdBe = 0.;
                if (s > 0) {
                    // First get the G(s-1) and the H(s-1) coefficients
                    final double sxgsm1 = s * GsHs[0][s - 1];
                    final double sxhsm1 = s * GsHs[1][s - 1];
                    // Then compute derivatives
                    dGsdh  = auxiliaryElements.getBeta()  * sxgsm1 - auxiliaryElements.getAlpha() * sxhsm1;
                    dGsdk  = auxiliaryElements.getAlpha() * sxgsm1 + auxiliaryElements.getBeta()  * sxhsm1;
                    dGsdAl = auxiliaryElements.getK() * sxgsm1 - auxiliaryElements.getH() * sxhsm1;
                    dGsdBe = auxiliaryElements.getH() * sxgsm1 + auxiliaryElements.getK() * sxhsm1;
                }

                // Kronecker symbol (2 - delta(0,s))
                final double d0s = (s == 0) ? 1 : 2;

                for (int n = s + 2; n <= context.getMaxDegree(); n++) {
                    // (n - s) must be even
                    if ((n - s) % 2 == 0) {

                        //Extract data from previous computation :
                        final double kns   = hansen.getHansenObjects()[s].getValue(-n - 1, context.getX());
                        final double dkns  = hansen.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                        final double vns   = context.getVns().get(new NSKey(n, s));
                        final double coef0 = d0s * roaPow[n] * vns * -harmonics.getUnnormalizedCnm(n, 0);
                        final double coef1 = coef0 * Qns[n][s];
                        final double coef2 = coef1 * kns;
                        final double coef3 = coef2 * gs;
                        // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                        final double dqns  = Qns[n][s + 1];

                        // Compute U
                        U += coef3;
                        // Compute dU / da :
                        dUda += coef3 * (n + 1);
                        // Compute dU / dEx
                        dUdk += coef1 * (kns * dGsdk + auxiliaryElements.getK() * context.getXXX() * gs * dkns);
                        // Compute dU / dEy
                        dUdh += coef1 * (kns * dGsdh + auxiliaryElements.getH() * context.getXXX() * gs * dkns);
                        // Compute dU / dAlpha
                        dUdAl += coef2 * dGsdAl;
                        // Compute dU / dBeta
                        dUdBe += coef2 * dGsdBe;
                        // Compute dU / dGamma
                        dUdGa += coef0 * kns * dqns * gs;

                    }
                }
            }

            // Multiply by -(μ / a)
            this.U = -context.getMuoa() * U;

            this.dUda = dUda *  context.getMuoa() / auxiliaryElements.getSma();
            this.dUdk = dUdk * -context.getMuoa();
            this.dUdh = dUdh * -context.getMuoa();
            this.dUdAl = dUdAl * -context.getMuoa();
            this.dUdBe = dUdBe * -context.getMuoa();
            this.dUdGa = dUdGa * -context.getMuoa();

        }

        /** Return value of U.
         * @return U
         */
        public double getU() {
            return U;
        }

        /** Return value of dU / da.
         * @return dUda
         */
        public double getdUda() {
            return dUda;
        }

        /** Return value of dU / dk.
         * @return dUdk
         */
        public double getdUdk() {
            return dUdk;
        }

        /** Return value of dU / dh.
         * @return dUdh
         */
        public double getdUdh() {
            return dUdh;
        }

        /** Return value of dU / dAlpha.
         * @return dUdAl
         */
        public double getdUdAl() {
            return dUdAl;
        }

        /** Return value of dU / dBeta.
         * @return dUdBe
         */
        public double getdUdBe() {
            return dUdBe;
        }

        /** Return value of dU / dGamma.
         * @return dUdGa
         */
        public double getdUdGa() {
            return dUdGa;
        }

    }

    /** Compute the derivatives of the gravitational potential U [Eq. 3.1-(6)].
     *  <p>
     *  The result is the array
     *  [dU/da, dU/dk, dU/dh, dU/dα, dU/dβ, dU/dγ]
     *  </p>
     */
    private class FieldUAnddU <T extends RealFieldElement<T>> {

         /** The current value of the U function. <br/>
          * Needed for the short periodic contribution */
        private T U;

         /** dU / da. */
        private T dUda;

         /** dU / dk. */
        private T dUdk;

         /** dU / dh. */
        private T dUdh;

         /** dU / dAlpha. */
        private T dUdAl;

         /** dU / dBeta. */
        private T dUdBe;

         /** dU / dGamma. */
        private T dUdGa;

         /** Simple constuctor.
          *  @param date current date
          *  @param context container for attributes
          *  @param hansen initialization of hansen objects
          *  @throws OrekitException if an error occurs in hansen computation
          */
        @SuppressWarnings("unchecked")
        FieldUAnddU(final FieldAbsoluteDate<T> date,
                    final FieldDSSTZonalContext<T> context,
                    final FieldHansenObjects hansen)
            throws OrekitException {

            // Zero for initialization
            final Field<T> field = date.getField();
            final T zero = field.getZero();

            // Auxiliary elements related to the current orbit
            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            //spherical harmonics
            final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date.toAbsoluteDate());

            //Reset U
            U = zero;

            // Gs and Hs coefficients
            final T[][] GsHs = CoefficientsFactory.computeGsHs(auxiliaryElements.getK(), auxiliaryElements.getH(), auxiliaryElements.getAlpha(), auxiliaryElements.getBeta(), context.getMaxEccPowMeanElements(), field);
            // Qns coefficients
            final T[][] Qns  = CoefficientsFactory.computeQns(auxiliaryElements.getGamma(), context.getMaxDegree(), context.getMaxEccPowMeanElements());

            final T[] roaPow = MathArrays.buildArray(field, context.getMaxDegree() + 1);
            roaPow[0] = zero.add(1.);
            for (int i = 1; i <= context.getMaxDegree(); i++) {
                roaPow[i] = roaPow[i - 1].multiply(context.getRoa());
            }

            // Potential derivatives
            dUda  = zero;
            dUdk  = zero;
            dUdh  = zero;
            dUdAl = zero;
            dUdBe = zero;
            dUdGa = zero;

            for (int s = 0; s <= context.getMaxEccPowMeanElements(); s++) {
                //Initialize the Hansen roots
                hansen.computeHansenObjectsInitValues(context, s);

                // Get the current Gs coefficient
                final T gs = GsHs[0][s];

                // Compute Gs partial derivatives from 3.1-(9)
                T dGsdh  = zero;
                T dGsdk  = zero;
                T dGsdAl = zero;
                T dGsdBe = zero;
                if (s > 0) {
                    // First get the G(s-1) and the H(s-1) coefficients
                    final T sxgsm1 = GsHs[0][s - 1].multiply(s);
                    final T sxhsm1 = GsHs[1][s - 1].multiply(s);
                    // Then compute derivatives
                    dGsdh  = sxgsm1.multiply(auxiliaryElements.getBeta()).subtract(sxhsm1.multiply(auxiliaryElements.getAlpha()));
                    dGsdk  = sxgsm1.multiply(auxiliaryElements.getAlpha()).add(sxhsm1.multiply(auxiliaryElements.getBeta()));
                    dGsdAl = sxgsm1.multiply(auxiliaryElements.getK()).subtract(sxhsm1.multiply(auxiliaryElements.getH()));
                    dGsdBe = sxgsm1.multiply(auxiliaryElements.getH()).add(sxhsm1.multiply(auxiliaryElements.getK()));
                }

                // Kronecker symbol (2 - delta(0,s))
                final T d0s = zero.add((s == 0) ? 1 : 2);

                for (int n = s + 2; n <= context.getMaxDegree(); n++) {
                    // (n - s) must be even
                    if ((n - s) % 2 == 0) {

                        //Extract data from previous computation :
                        final T kns   = (T) hansen.getHansenObjects()[s].getValue(-n - 1, context.getX());
                        final T dkns  = (T) hansen.getHansenObjects()[s].getDerivative(-n - 1, context.getX());

                        final double vns   = context.getVns().get(new NSKey(n, s));
                        final T coef0 = d0s.multiply(roaPow[n]).multiply(vns).multiply(-harmonics.getUnnormalizedCnm(n, 0));
                        final T coef1 = coef0.multiply(Qns[n][s]);
                        final T coef2 = coef1.multiply(kns);
                        final T coef3 = coef2.multiply(gs);
                        // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                        final T dqns  = Qns[n][s + 1];

                        // Compute U
                        U = U.add(coef3);
                        // Compute dU / da :
                        dUda = dUda.add(coef3.multiply(n + 1));
                        // Compute dU / dEx
                        dUdk = dUdk.add(coef1.multiply(dGsdk.multiply(kns).add(auxiliaryElements.getK().multiply(context.getXXX()).multiply(dkns).multiply(gs))));
                        // Compute dU / dEy
                        dUdh = dUdh.add(coef1.multiply(dGsdh.multiply(kns).add(auxiliaryElements.getH().multiply(context.getXXX()).multiply(dkns).multiply(gs))));
                        // Compute dU / dAlpha
                        dUdAl = dUdAl.add(coef2.multiply(dGsdAl));
                        // Compute dU / dBeta
                        dUdBe = dUdBe.add(coef2.multiply(dGsdBe));
                        // Compute dU / dGamma
                        dUdGa = dUdGa.add(coef0.multiply(kns).multiply(dqns).multiply(gs));
                    }
                }
            }

            // Multiply by -(μ / a)
            U = U.multiply(context.getMuoa().negate());

            dUda = dUda.multiply(context.getMuoa().divide(auxiliaryElements.getSma()));
            dUdk = dUdk.multiply(context.getMuoa()).negate();
            dUdh = dUdh.multiply(context.getMuoa()).negate();
            dUdAl = dUdAl.multiply(context.getMuoa()).negate();
            dUdBe = dUdBe.multiply(context.getMuoa()).negate();
            dUdGa = dUdGa.multiply(context.getMuoa()).negate();

        }

         /** Return value of dU / da.
          * @return dUda
          */
        public T getdUda() {
            return dUda;
        }

         /** Return value of dU / dk.
          * @return dUdk
          */
        public T getdUdk() {
            return dUdk;
        }

         /** Return value of dU / dh.
          * @return dUdh
          */
        public T getdUdh() {
            return dUdh;
        }

         /** Return value of dU / dAlpha.
          * @return dUdAl
          */
        public T getdUdAl() {
            return dUdAl;
        }

         /** Return value of dU / dBeta.
          * @return dUdBe
          */
        public T getdUdBe() {
            return dUdBe;
        }

         /** Return value of dU / dGamma.
          * @return dUdGa
          */
        public T getdUdGa() {
            return dUdGa;
        }

    }

    /** Computes init values of the Hansen Objects. */
    private class HansenObjects {

        /** An array that contains the objects needed to build the Hansen coefficients. <br/>
         * The index is s*/
        private HansenZonalLinear[] hansenObjects;

        /** Simple constructor. */
        HansenObjects() {
            this.hansenObjects = new HansenZonalLinear[maxEccPow + 1];
            for (int s = 0; s <= maxEccPow; s++) {
                this.hansenObjects[s] = new HansenZonalLinear(maxDegree, s);
            }
        }

        /** Compute init values for hansen objects.
         * @param context container for attributes
         * @param element element of the array to compute the init values
         */
        public void computeHansenObjectsInitValues(final DSSTZonalContext context, final int element) {
            hansenObjects[element].computeInitValues(context.getX());
        }

        /** Get the Hansen Objects.
         * @return hansenObjects
         */
        public HansenZonalLinear[] getHansenObjects() {
            return hansenObjects;
        }

    }

    /** Computes init values of the Hansen Objects. */
    private class FieldHansenObjects {

        /** An array that contains the objects needed to build the Hansen coefficients. <br/>
         * The index is s*/
        private FieldHansenZonalLinear[] hansenObjects;

        /** Simple constructor. */
        FieldHansenObjects() {
            this.hansenObjects = new FieldHansenZonalLinear[maxEccPow + 1];
            for (int s = 0; s <= maxEccPow; s++) {
                this.hansenObjects[s] = new FieldHansenZonalLinear<>(maxDegree, s);
            }
        }

        /** Compute init values for hansen objects.
         * @param <T> type of the elements
         * @param context container for attributes
         * @param element element of the array to compute the init values
         */
        @SuppressWarnings("unchecked")
        public <T extends RealFieldElement<T>> void computeHansenObjectsInitValues(final FieldDSSTZonalContext<T> context, final int element) {
            hansenObjects[element].computeInitValues(context.getX());
        }

        /** Get the Hansen Objects.
         * @return hansenObjects
         */
        public FieldHansenZonalLinear[] getHansenObjects() {
            return hansenObjects;
        }

    }

}
