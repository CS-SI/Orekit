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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldGHmsjPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldGammaMnsFunction;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.GHmsjPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.GammaMnsFunction;
import org.orekit.propagation.semianalytical.dsst.utilities.JacobiPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.ShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.FieldHansenTesseralLinear;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenTesseralLinear;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTimeSpanMap;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/** Tesseral contribution to the central body gravitational perturbation.
 *  <p>
 *  Only resonant tesserals are considered.
 *  </p>
 *
 *  @author Romain Di Costanzo
 *  @author Pascal Parraud
 *  @author Bryan Cazabonne (field translation)
 */
public class DSSTTesseral implements DSSTForceModel {

    /**  Name of the prefix for short period coefficients keys. */
    public static final String SHORT_PERIOD_PREFIX = "DSST-central-body-tesseral-";

    /** Identifier for cMm coefficients. */
    public static final String CM_COEFFICIENTS = "cM";

    /** Identifier for sMm coefficients. */
    public static final String SM_COEFFICIENTS = "sM";

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

    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in seconds.
     */
    private static final double MIN_PERIOD_IN_SECONDS = 864000.;

    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in satellite revolutions.
     */
    private static final double MIN_PERIOD_IN_SAT_REV = 10.;

    /** Number of points for interpolation. */
    private static final int INTERPOLATION_POINTS = 3;

    /** Provider for spherical harmonics. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Central body rotating frame. */
    private final Frame bodyFrame;

    /** Central body rotation rate (rad/s). */
    private final double centralBodyRotationRate;

    /** Central body rotation period (seconds). */
    private final double bodyPeriod;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegree;

    /** Maximal degree to consider for short periodics tesseral harmonics potential (without m-daily). */
    private final int maxDegreeTesseralSP;

    /** Maximal degree to consider for short periodics m-daily tesseral harmonics potential . */
    private final int maxDegreeMdailyTesseralSP;

    /** Maximal order to consider for harmonics potential. */
    private final int maxOrder;

    /** Maximal order to consider for short periodics tesseral harmonics potential (without m-daily). */
    private final int maxOrderTesseralSP;

    /** Maximal order to consider for short periodics m-daily tesseral harmonics potential . */
    private final int maxOrderMdailyTesseralSP;

    /** Maximum power of the eccentricity to use in summation over s for
     * short periodic tesseral harmonics (without m-daily). */
    private final int maxEccPowTesseralSP;

    /** Maximum power of the eccentricity to use in summation over s for
     * m-daily tesseral harmonics. */
    private final int maxEccPowMdailyTesseralSP;

    /** Maximum value for j. */
    private final int maxFrequencyShortPeriodics;

    /** Maximum power of the eccentricity to use in summation over s. */
    private int maxEccPow;

    /** Maximum power of the eccentricity to use in Hansen coefficient Kernel expansion. */
    private int maxHansen;

    /** Maximum value between maxOrderMdailyTesseralSP and maxOrderTesseralSP. */
    private int mMax;

    /** List of non resonant orders with j != 0. */
    private final SortedMap<Integer, List<Integer> > nonResOrders;

    /** List of resonant orders. */
    private final List<Integer> resOrders;

    /** Short period terms. */
    private TesseralShortPeriodicCoefficients shortPeriodTerms;

    /** Short period terms. */
    private Map<Field<?>, FieldTesseralShortPeriodicCoefficients<?>> fieldShortPeriodTerms;

    /** Driver for gravitational parameter. */
    private final ParameterDriver gmParameterDriver;

    /** Hansen objects. */
    private HansenObjects hansen;

    /** Hansen objects for field elements. */
    private Map<Field<?>, FieldHansenObjects<?>> fieldHansen;

    /** Simple constructor with default reference values.
     * <p>
     * When this constructor is used, maximum allowed values are used
     * for the short periodic coefficients:
     * </p>
     * <ul>
     *    <li> {@link #maxDegreeTesseralSP} is set to {@code provider.getMaxDegree()} </li>
     *    <li> {@link #maxOrderTesseralSP} is set to {@code provider.getMaxOrder()}. </li>
     *    <li> {@link #maxEccPowTesseralSP} is set to {@code min(4, provider.getMaxOrder())} </li>
     *    <li> {@link #maxFrequencyShortPeriodics} is set to {@code min(provider.getMaxDegree() + 4, 12)}.
     *         This parameter should not exceed 12 as higher values will exceed computer capacity </li>
     *    <li> {@link #maxDegreeMdailyTesseralSP} is set to {@code provider.getMaxDegree()} </li>
     *    <li> {@link #maxOrderMdailyTesseralSP} is set to {@code provider.getMaxOrder()} </li>
     *    <li> {@link #maxEccPowMdailyTesseralSP} is set to min(provider.getMaxDegree() - 2, 4).
     *         This parameter should not exceed 4 as higher values will exceed computer capacity </li>
     * </ul>
     * @param centralBodyFrame rotating body frame
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param provider provider for spherical harmonics
     * @since 10.1
     */
    public DSSTTesseral(final Frame centralBodyFrame,
                        final double centralBodyRotationRate,
                        final UnnormalizedSphericalHarmonicsProvider provider) {
        this(centralBodyFrame, centralBodyRotationRate, provider, provider.getMaxDegree(),
             provider.getMaxOrder(), FastMath.min(4, provider.getMaxOrder()),  FastMath.min(12, provider.getMaxDegree() + 4),
             provider.getMaxDegree(), provider.getMaxOrder(), FastMath.min(4, provider.getMaxDegree() - 2));
    }

    /** Simple constructor.
     * @param centralBodyFrame rotating body frame
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param provider provider for spherical harmonics
     * @param maxDegreeTesseralSP maximal degree to consider for short periodics tesseral harmonics potential
     *  (must be between 2 and {@code provider.getMaxDegree()})
     * @param maxOrderTesseralSP maximal order to consider for short periodics tesseral harmonics potential
     *  (must be between 0 and {@code provider.getMaxOrder()})
     * @param maxEccPowTesseralSP maximum power of the eccentricity to use in summation over s for
     * short periodic tesseral harmonics (without m-daily), should typically not exceed 4 as higher
     * values will exceed computer capacity
     * (must be between 0 and {@code provider.getMaxOrder()} though, however if order = 0 the value can be anything
     *  since it won't be used in the code)
     * @param maxFrequencyShortPeriodics maximum frequency in mean longitude for short periodic computations
     * (typically {@code maxDegreeTesseralSP} + {@code maxEccPowTesseralSP and no more than 12})
     * @param maxDegreeMdailyTesseralSP maximal degree to consider for short periodics m-daily tesseral harmonics potential
     *  (must be between 2 and {@code provider.getMaxDegree()})
     * @param maxOrderMdailyTesseralSP maximal order to consider for short periodics m-daily tesseral harmonics potential
     *  (must be between 0 and {@code provider.getMaxOrder()})
     * @param maxEccPowMdailyTesseralSP maximum power of the eccentricity to use in summation over s for
     * m-daily tesseral harmonics, (must be between 0 and {@code maxDegreeMdailyTesseralSP - 2},
     * but should typically not exceed 4 as higher values will exceed computer capacity)
     * @since 7.2
     */
    public DSSTTesseral(final Frame centralBodyFrame,
                        final double centralBodyRotationRate,
                        final UnnormalizedSphericalHarmonicsProvider provider,
                        final int maxDegreeTesseralSP, final int maxOrderTesseralSP,
                        final int maxEccPowTesseralSP, final int maxFrequencyShortPeriodics,
                        final int maxDegreeMdailyTesseralSP, final int maxOrderMdailyTesseralSP,
                        final int maxEccPowMdailyTesseralSP) {

        gmParameterDriver = new ParameterDriver(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                provider.getMu(), MU_SCALE,
                                                0.0, Double.POSITIVE_INFINITY);

        // Central body rotating frame
        this.bodyFrame = centralBodyFrame;

        //Save the rotation rate
        this.centralBodyRotationRate = centralBodyRotationRate;

        // Central body rotation period in seconds
        this.bodyPeriod = MathUtils.TWO_PI / centralBodyRotationRate;

        // Provider for spherical harmonics
        this.provider      = provider;
        this.maxDegree     = provider.getMaxDegree();
        this.maxOrder      = provider.getMaxOrder();

        //set the maximum degree order for short periodics
        checkIndexRange(maxDegreeTesseralSP, 2, maxDegree);
        this.maxDegreeTesseralSP       = maxDegreeTesseralSP;

        checkIndexRange(maxDegreeMdailyTesseralSP, 2, maxDegree);
        this.maxDegreeMdailyTesseralSP = maxDegreeMdailyTesseralSP;

        checkIndexRange(maxOrderTesseralSP, 0, maxOrder);
        this.maxOrderTesseralSP        = maxOrderTesseralSP;

        checkIndexRange(maxOrderMdailyTesseralSP, 0, maxOrder);
        this.maxOrderMdailyTesseralSP  = maxOrderMdailyTesseralSP;

        // set the maximum value for eccentricity power
        if (maxOrder > 0) {
            // Range check can be silently ignored if order = 0
            checkIndexRange(maxEccPowTesseralSP, 0, maxOrder);
        }
        this.maxEccPowTesseralSP       = maxEccPowTesseralSP;

        checkIndexRange(maxEccPowMdailyTesseralSP, 0, maxDegreeMdailyTesseralSP - 2);
        this.maxEccPowMdailyTesseralSP = maxEccPowMdailyTesseralSP;

        // set the maximum value for frequency
        this.maxFrequencyShortPeriodics = maxFrequencyShortPeriodics;

        // Initialize default values
        this.resOrders    = new ArrayList<Integer>();
        this.nonResOrders = new TreeMap<Integer, List <Integer>>();

        // Initialize default values
        this.fieldShortPeriodTerms = new HashMap<>();
        this.fieldHansen           = new HashMap<>();
        this.maxEccPow             = 0;
        this.maxHansen             = 0;

    }

    /** Check an index range.
     * @param index index value
     * @param min minimum value for index
     * @param max maximum value for index
     */
    private void checkIndexRange(final int index, final int min, final int max) {
        if (index < min || index > max) {
            throw new OrekitException(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, index, min, max);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ShortPeriodTerms> initializeShortPeriodTerms(final AuxiliaryElements auxiliaryElements,
                                             final PropagationType type,
                                             final double[] parameters) {

        // Initializes specific parameters.

        final DSSTTesseralContext context = initializeStep(auxiliaryElements, parameters);

        // Set the highest power of the eccentricity in the analytical power
        // series expansion for the averaged high order resonant central body
        // spherical harmonic perturbation
        maxEccPow = getMaxEccPow(auxiliaryElements.getEcc());

        // Set the maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
        maxHansen = maxEccPow / 2;

        // The following terms are only used for hansen objects initialization
        final double ratio = context.getRatio();

        // Compute the non resonant tesseral harmonic terms if not set by the user
        getResonantAndNonResonantTerms(type, context.getOrbitPeriod(), ratio);

        hansen = new HansenObjects(ratio, type);

        mMax = FastMath.max(maxOrderTesseralSP, maxOrderMdailyTesseralSP);

        shortPeriodTerms = new TesseralShortPeriodicCoefficients(bodyFrame, maxOrderMdailyTesseralSP,
                                                                 maxDegreeTesseralSP < 0, nonResOrders,
                                                                 mMax, maxFrequencyShortPeriodics, INTERPOLATION_POINTS,
                                                                 new TimeSpanMap<Slot>(new Slot(mMax, maxFrequencyShortPeriodics,
                                                                                                INTERPOLATION_POINTS)));

        final List<ShortPeriodTerms> list = new ArrayList<ShortPeriodTerms>();
        list.add(shortPeriodTerms);
        return list;

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> List<FieldShortPeriodTerms<T>> initializeShortPeriodTerms(final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                     final PropagationType type,
                                                                                     final T[] parameters) {

        // Field used by default
        final Field<T> field = auxiliaryElements.getDate().getField();

        // Initializes specific parameters.
        final FieldDSSTTesseralContext<T> context = initializeStep(auxiliaryElements, parameters);

        // Set the highest power of the eccentricity in the analytical power
        // series expansion for the averaged high order resonant central body
        // spherical harmonic perturbation
        maxEccPow = getMaxEccPow(auxiliaryElements.getEcc().getReal());

        // Set the maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
        maxHansen = maxEccPow / 2;

        // The following terms are only used for hansen objects initialization
        final T ratio = context.getRatio();

        // Compute the non resonant tesseral harmonic terms if not set by the user
        // Field information is not important here
        getResonantAndNonResonantTerms(type, context.getOrbitPeriod().getReal(), ratio.getReal());

        mMax = FastMath.max(maxOrderTesseralSP, maxOrderMdailyTesseralSP);

        fieldHansen.put(field, new FieldHansenObjects<>(ratio, type));

        final FieldTesseralShortPeriodicCoefficients<T> ftspc =
                        new FieldTesseralShortPeriodicCoefficients<>(bodyFrame, maxOrderMdailyTesseralSP,
                                                                     maxDegreeTesseralSP < 0, nonResOrders,
                                                                     mMax, maxFrequencyShortPeriodics, INTERPOLATION_POINTS,
                                                                     new FieldTimeSpanMap<>(new FieldSlot<>(mMax,
                                                                                                            maxFrequencyShortPeriodics,
                                                                                                            INTERPOLATION_POINTS),
                                                                                            field));

        fieldShortPeriodTerms.put(field, ftspc);
        return Collections.singletonList(ftspc);

    }

    /**
     * Get the maximum power of the eccentricity to use in summation over s.
     * @param e eccentricity
     * @return the maximum power of the eccentricity
     */
    private int getMaxEccPow(final double e) {
        // maxEccPow depends on satellite eccentricity
        if (e <= 0.005) {
            return 3;
        } else if (e <= 0.02) {
            return 4;
        } else if (e <= 0.1) {
            return 7;
        } else if (e <= 0.2) {
            return 10;
        } else if (e <= 0.3) {
            return 12;
        } else if (e <= 0.4) {
            return 15;
        } else {
            return 20;
        }
    }

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters (only 1 value for each parameter)
     *  that is to say that the extract parameter method {@link #extractParameters(double[], AbsoluteDate)}
     *  should have be called before or the parameters list given in argument must correspond
     *  to the extraction of parameter for a precise date {@link #getParameters(AbsoluteDate)}.
     *  @return new force model context
     */
    private DSSTTesseralContext initializeStep(final AuxiliaryElements auxiliaryElements, final double[] parameters) {
        return new DSSTTesseralContext(auxiliaryElements, bodyFrame, provider, maxFrequencyShortPeriodics, bodyPeriod, parameters);
    }

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param <T> type of the elements
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters list of each estimated values for each driver of the force model parameters
         *                (each span of each driver)
     *  @return new force model context
     */
    private <T extends CalculusFieldElement<T>> FieldDSSTTesseralContext<T> initializeStep(final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                       final T[] parameters) {
        return new FieldDSSTTesseralContext<>(auxiliaryElements, bodyFrame, provider, maxFrequencyShortPeriodics, bodyPeriod, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getMeanElementRate(final SpacecraftState spacecraftState,
                                       final AuxiliaryElements auxiliaryElements, final double[] parameters) {

        // Container for attributes

        final DSSTTesseralContext context = initializeStep(auxiliaryElements, parameters);

        // Access to potential U derivatives
        final UAnddU udu = new UAnddU(spacecraftState.getDate(), context, hansen);

        // Compute the cross derivative operator :
        final double UAlphaGamma   = auxiliaryElements.getAlpha() * udu.getdUdGa() - auxiliaryElements.getGamma() * udu.getdUdAl();
        final double UAlphaBeta    = auxiliaryElements.getAlpha() * udu.getdUdBe() - auxiliaryElements.getBeta()  * udu.getdUdAl();
        final double UBetaGamma    = auxiliaryElements.getBeta() * udu.getdUdGa() - auxiliaryElements.getGamma() * udu.getdUdBe();
        final double Uhk           = auxiliaryElements.getH() * udu.getdUdk()  - auxiliaryElements.getK() * udu.getdUdh();
        final double pUagmIqUbgoAB = (auxiliaryElements.getP() * UAlphaGamma - I * auxiliaryElements.getQ() * UBetaGamma) * context.getOoAB();
        final double UhkmUabmdUdl  = Uhk - UAlphaBeta - udu.getdUdl();

        final double da =  context.getAx2oA() * udu.getdUdl();
        final double dh =  context.getBoA() * udu.getdUdk() + auxiliaryElements.getK() * pUagmIqUbgoAB - auxiliaryElements.getH() * context.getBoABpo() * udu.getdUdl();
        final double dk =  -(context.getBoA() * udu.getdUdh() + auxiliaryElements.getH() * pUagmIqUbgoAB + auxiliaryElements.getK() * context.getBoABpo() * udu.getdUdl());
        final double dp =  context.getCo2AB() * (auxiliaryElements.getP() * UhkmUabmdUdl - UBetaGamma);
        final double dq =  context.getCo2AB() * (auxiliaryElements.getQ() * UhkmUabmdUdl - I * UAlphaGamma);
        final double dM = -context.getAx2oA() * udu.getdUda() + context.getBoABpo() * (auxiliaryElements.getH() * udu.getdUdh() + auxiliaryElements.getK() * udu.getdUdk()) + pUagmIqUbgoAB;

        return new double[] {da, dk, dh, dq, dp, dM};
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getMeanElementRate(final FieldSpacecraftState<T> spacecraftState,
                                                                  final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                  final T[] parameters) {

        // Field used by default
        final Field<T> field = auxiliaryElements.getDate().getField();

        // Container for attributes

        final FieldDSSTTesseralContext<T> context = initializeStep(auxiliaryElements, parameters);

        @SuppressWarnings("unchecked")
        final FieldHansenObjects<T> fho = (FieldHansenObjects<T>) fieldHansen.get(field);
        // Access to potential U derivatives
        final FieldUAnddU<T> udu = new FieldUAnddU<>(spacecraftState.getDate(), context, fho);

        // Compute the cross derivative operator :
        final T UAlphaGamma   = udu.getdUdGa().multiply(auxiliaryElements.getAlpha()).subtract(udu.getdUdAl().multiply(auxiliaryElements.getGamma()));
        final T UAlphaBeta    = udu.getdUdBe().multiply(auxiliaryElements.getAlpha()).subtract(udu.getdUdAl().multiply(auxiliaryElements.getBeta()));
        final T UBetaGamma    = udu.getdUdGa().multiply(auxiliaryElements.getBeta()).subtract(udu.getdUdBe().multiply(auxiliaryElements.getGamma()));
        final T Uhk           = udu.getdUdk().multiply(auxiliaryElements.getH()).subtract(udu.getdUdh().multiply(auxiliaryElements.getK()));
        final T pUagmIqUbgoAB = (UAlphaGamma.multiply(auxiliaryElements.getP()).subtract(UBetaGamma.multiply(auxiliaryElements.getQ()).multiply(I))).multiply(context.getOoAB());
        final T UhkmUabmdUdl  = Uhk.subtract(UAlphaBeta).subtract(udu.getdUdl());

        final T da = udu.getdUdl().multiply(context.getAx2oA());
        final T dh = udu.getdUdk().multiply(context.getBoA()).add(pUagmIqUbgoAB.multiply(auxiliaryElements.getK())).subtract(udu.getdUdl().multiply(auxiliaryElements.getH()).multiply(context.getBoABpo()));
        final T dk = (udu.getdUdh().multiply(context.getBoA()).add(pUagmIqUbgoAB.multiply(auxiliaryElements.getH())).add(udu.getdUdl().multiply(context.getBoABpo()).multiply(auxiliaryElements.getK()))).negate();
        final T dp = context.getCo2AB().multiply(auxiliaryElements.getP().multiply(UhkmUabmdUdl).subtract(UBetaGamma));
        final T dq = context.getCo2AB().multiply(auxiliaryElements.getQ().multiply(UhkmUabmdUdl).subtract(UAlphaGamma.multiply(I)));
        final T dM = pUagmIqUbgoAB.add(udu.getdUda().multiply(context.getAx2oA()).negate()).add((udu.getdUdh().multiply(auxiliaryElements.getH()).add(udu.getdUdk().multiply(auxiliaryElements.getK()))).multiply(context.getBoABpo()));

        final T[] elements = MathArrays.buildArray(field, 6);
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
    public void updateShortPeriodTerms(final double[] parameters, final SpacecraftState... meanStates) {

        final Slot slot = shortPeriodTerms.createSlot(meanStates);

        for (final SpacecraftState meanState : meanStates) {

            final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(meanState.getOrbit(), I);

            // Extract the proper parameters valid at date from the input array
            final double[] extractedParameters = this.extractParameters(parameters, auxiliaryElements.getDate());
            final DSSTTesseralContext context = initializeStep(auxiliaryElements, extractedParameters);

            // Initialise the Hansen coefficients
            for (int s = -maxDegree; s <= maxDegree; s++) {
                // coefficients with j == 0 are always needed
                hansen.computeHansenObjectsInitValues(context, s + maxDegree, 0);
                if (maxDegreeTesseralSP >= 0) {
                    // initialize other objects only if required
                    for (int j = 1; j <= maxFrequencyShortPeriodics; j++) {
                        hansen.computeHansenObjectsInitValues(context, s + maxDegree, j);
                    }
                }
            }

            final FourierCjSjCoefficients cjsjFourier = new FourierCjSjCoefficients(maxFrequencyShortPeriodics, mMax);

            // Compute coefficients
            // Compute only if there is at least one non-resonant tesseral
            if (!nonResOrders.isEmpty() || maxDegreeTesseralSP < 0) {
                // Generate the fourrier coefficients
                cjsjFourier.generateCoefficients(meanState.getDate(), context, hansen);

                // the coefficient 3n / 2a
                final double tnota = 1.5 * context.getMeanMotion() / auxiliaryElements.getSma();

                // build the mDaily coefficients
                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    // build the coefficients
                    buildCoefficients(cjsjFourier, meanState.getDate(), slot, m, 0, tnota, context);
                }

                if (maxDegreeTesseralSP >= 0) {
                    // generate the other coefficients, if required
                    for (final Map.Entry<Integer, List<Integer>> entry : nonResOrders.entrySet()) {

                        for (int j : entry.getValue()) {
                            // build the coefficients
                            buildCoefficients(cjsjFourier, meanState.getDate(), slot, entry.getKey(), j, tnota, context);
                        }
                    }
                }
            }

        }

    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends CalculusFieldElement<T>> void updateShortPeriodTerms(final T[] parameters,
                                                                       final FieldSpacecraftState<T>... meanStates) {

        // Field used by default
        final Field<T> field = meanStates[0].getDate().getField();

        final FieldTesseralShortPeriodicCoefficients<T> ftspc = (FieldTesseralShortPeriodicCoefficients<T>) fieldShortPeriodTerms.get(field);
        final FieldSlot<T> slot = ftspc.createSlot(meanStates);

        for (final FieldSpacecraftState<T> meanState : meanStates) {

            final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(meanState.getOrbit(), I);

            // Extract the proper parameters valid at date from the input array
            final T[] extractedParameters = this.extractParameters(parameters, auxiliaryElements.getDate());
            final FieldDSSTTesseralContext<T> context = initializeStep(auxiliaryElements, extractedParameters);

            final FieldHansenObjects<T> fho = (FieldHansenObjects<T>) fieldHansen.get(field);
            // Initialise the Hansen coefficients
            for (int s = -maxDegree; s <= maxDegree; s++) {
                // coefficients with j == 0 are always needed
                fho.computeHansenObjectsInitValues(context, s + maxDegree, 0);
                if (maxDegreeTesseralSP >= 0) {
                    // initialize other objects only if required
                    for (int j = 1; j <= maxFrequencyShortPeriodics; j++) {
                        fho.computeHansenObjectsInitValues(context, s + maxDegree, j);
                    }
                }
            }

            final FieldFourierCjSjCoefficients<T> cjsjFourier = new FieldFourierCjSjCoefficients<>(maxFrequencyShortPeriodics, mMax, field);

            // Compute coefficients
            // Compute only if there is at least one non-resonant tesseral
            if (!nonResOrders.isEmpty() || maxDegreeTesseralSP < 0) {
                // Generate the fourrier coefficients
                cjsjFourier.generateCoefficients(meanState.getDate(), context, fho, field);

                // the coefficient 3n / 2a
                final T tnota = context.getMeanMotion().multiply(1.5).divide(auxiliaryElements.getSma());

                // build the mDaily coefficients
                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    // build the coefficients
                    buildCoefficients(cjsjFourier, meanState.getDate(), slot, m, 0, tnota, context, field);
                }

                if (maxDegreeTesseralSP >= 0) {
                    // generate the other coefficients, if required
                    for (final Map.Entry<Integer, List<Integer>> entry : nonResOrders.entrySet()) {

                        for (int j : entry.getValue()) {
                            // build the coefficients
                            buildCoefficients(cjsjFourier, meanState.getDate(), slot, entry.getKey(), j, tnota, context, field);
                        }
                    }
                }
            }

        }

    }

    /** {@inheritDoc} */
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(gmParameterDriver);
    }

    /** Build a set of coefficients.
     * @param cjsjFourier the fourier coefficients C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup>
     * @param date the current date
     * @param slot slot to which the coefficients belong
     * @param m m index
     * @param j j index
     * @param tnota 3n/2a
     * @param context container for attributes
     */
    private void buildCoefficients(final FourierCjSjCoefficients cjsjFourier,
                                   final AbsoluteDate date, final Slot slot,
                                   final int m, final int j, final double tnota, final DSSTTesseralContext context) {

        // Create local arrays
        final double[] currentCijm = new double[] {0., 0., 0., 0., 0., 0.};
        final double[] currentSijm = new double[] {0., 0., 0., 0., 0., 0.};

        // compute the term 1 / (jn - mθ<sup>.</sup>)
        final double oojnmt = 1. / (j * context.getMeanMotion() - m * centralBodyRotationRate);

        // initialise the coeficients
        for (int i = 0; i < 6; i++) {
            currentCijm[i] = -cjsjFourier.getSijm(i, j, m);
            currentSijm[i] = cjsjFourier.getCijm(i, j, m);
        }
        // Add the separate part for δ<sub>6i</sub>
        currentCijm[5] += tnota * oojnmt * cjsjFourier.getCijm(0, j, m);
        currentSijm[5] += tnota * oojnmt * cjsjFourier.getSijm(0, j, m);

        //Multiply by 1 / (jn - mθ<sup>.</sup>)
        for (int i = 0; i < 6; i++) {
            currentCijm[i] *= oojnmt;
            currentSijm[i] *= oojnmt;
        }

        // Add the coefficients to the interpolation grid
        slot.cijm[m][j + maxFrequencyShortPeriodics].addGridPoint(date, currentCijm);
        slot.sijm[m][j + maxFrequencyShortPeriodics].addGridPoint(date, currentSijm);

    }

     /** Build a set of coefficients.
     * @param <T> the type of the field elements
     * @param cjsjFourier the fourier coefficients C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup>
     * @param date the current date
     * @param slot slot to which the coefficients belong
     * @param m m index
     * @param j j index
     * @param tnota 3n/2a
     * @param context container for attributes
     * @param field field used by default
     */
    private <T extends CalculusFieldElement<T>> void buildCoefficients(final FieldFourierCjSjCoefficients<T> cjsjFourier,
                                                                   final FieldAbsoluteDate<T> date,
                                                                   final FieldSlot<T> slot,
                                                                   final int m, final int j, final T tnota,
                                                                   final FieldDSSTTesseralContext<T> context,
                                                                   final Field<T> field) {

        // Zero
        final T zero = field.getZero();

        // Create local arrays
        final T[] currentCijm = MathArrays.buildArray(field, 6);
        final T[] currentSijm = MathArrays.buildArray(field, 6);

        Arrays.fill(currentCijm, zero);
        Arrays.fill(currentSijm, zero);

        // compute the term 1 / (jn - mθ<sup>.</sup>)
        final T oojnmt = (context.getMeanMotion().multiply(j).subtract(m * centralBodyRotationRate)).reciprocal();

        // initialise the coeficients
        for (int i = 0; i < 6; i++) {
            currentCijm[i] = cjsjFourier.getSijm(i, j, m).negate();
            currentSijm[i] = cjsjFourier.getCijm(i, j, m);
        }
        // Add the separate part for δ<sub>6i</sub>
        currentCijm[5] = currentCijm[5].add(tnota.multiply(oojnmt).multiply(cjsjFourier.getCijm(0, j, m)));
        currentSijm[5] = currentSijm[5].add(tnota.multiply(oojnmt).multiply(cjsjFourier.getSijm(0, j, m)));

        //Multiply by 1 / (jn - mθ<sup>.</sup>)
        for (int i = 0; i < 6; i++) {
            currentCijm[i] = currentCijm[i].multiply(oojnmt);
            currentSijm[i] = currentSijm[i].multiply(oojnmt);
        }

        // Add the coefficients to the interpolation grid
        slot.cijm[m][j + maxFrequencyShortPeriodics].addGridPoint(date, currentCijm);
        slot.sijm[m][j + maxFrequencyShortPeriodics].addGridPoint(date, currentSijm);

    }

     /**
      * Get the resonant and non-resonant tesseral terms in the central body spherical harmonic field.
      *
      * @param type type of the elements used during the propagation
      * @param orbitPeriod Keplerian period
      * @param ratio ratio of satellite period to central body rotation period
      */
    private void getResonantAndNonResonantTerms(final PropagationType type, final double orbitPeriod,
                                                final double ratio) {

        // Compute natural resonant terms
        final double tolerance = 1. / FastMath.max(MIN_PERIOD_IN_SAT_REV,
                                                   MIN_PERIOD_IN_SECONDS / orbitPeriod);

        // Search the resonant orders in the tesseral harmonic field
        resOrders.clear();
        nonResOrders.clear();
        for (int m = 1; m <= maxOrder; m++) {
            final double resonance = ratio * m;
            int jRes = 0;
            final int jComputedRes = (int) FastMath.round(resonance);
            if (jComputedRes > 0 && jComputedRes <= maxFrequencyShortPeriodics && FastMath.abs(resonance - jComputedRes) <= tolerance) {
                // Store each resonant index and order
                resOrders.add(m);
                jRes = jComputedRes;
            }

            if (type == PropagationType.OSCULATING && maxDegreeTesseralSP >= 0 && m <= maxOrderTesseralSP) {
                //compute non resonant orders in the tesseral harmonic field
                final List<Integer> listJofM = new ArrayList<Integer>();
                //for the moment we take only the pairs (j,m) with |j| <= maxDegree + maxEccPow (from |s-j| <= maxEccPow and |s| <= maxDegree)
                for (int j = -maxFrequencyShortPeriodics; j <= maxFrequencyShortPeriodics; j++) {
                    if (j != 0 && j != jRes) {
                        listJofM.add(j);
                    }
                }

                nonResOrders.put(m, listJofM);
            }
        }
    }

    /** Compute the n-SUM for potential derivatives components.
     *  @param date current date
     *  @param j resonant index <i>j</i>
     *  @param m resonant order <i>m</i>
     *  @param s d'Alembert characteristic <i>s</i>
     *  @param maxN maximum possible value for <i>n</i> index
     *  @param roaPow powers of R/a up to degree <i>n</i>
     *  @param ghMSJ G<sup>j</sup><sub>m,s</sub> and H<sup>j</sup><sub>m,s</sub> polynomials
     *  @param gammaMNS &Gamma;<sup>m</sup><sub>n,s</sub>(γ) function
     *  @param context container for attributes
     *  @param hansenObjects initialization of hansen objects
     *  @return Components of U<sub>n</sub> derivatives for fixed j, m, s
     */
    private double[][] computeNSum(final AbsoluteDate date,
                                   final int j, final int m, final int s, final int maxN, final double[] roaPow,
                                   final GHmsjPolynomials ghMSJ, final GammaMnsFunction gammaMNS, final DSSTTesseralContext context,
                                   final HansenObjects hansenObjects) {

        // Auxiliary elements related to the current orbit
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

        //spherical harmonics
        final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);

        // Potential derivatives components
        double dUdaCos  = 0.;
        double dUdaSin  = 0.;
        double dUdhCos  = 0.;
        double dUdhSin  = 0.;
        double dUdkCos  = 0.;
        double dUdkSin  = 0.;
        double dUdlCos  = 0.;
        double dUdlSin  = 0.;
        double dUdAlCos = 0.;
        double dUdAlSin = 0.;
        double dUdBeCos = 0.;
        double dUdBeSin = 0.;
        double dUdGaCos = 0.;
        double dUdGaSin = 0.;

        // I^m
        @SuppressWarnings("unused")
        final int Im = I > 0 ? 1 : (m % 2 == 0 ? 1 : -1);

        // jacobi v, w, indices from 2.7.1-(15)
        final int v = FastMath.abs(m - s);
        final int w = FastMath.abs(m + s);

        // Initialise lower degree nmin = (Max(2, m, |s|)) for summation over n
        final int nmin = FastMath.max(FastMath.max(2, m), FastMath.abs(s));

        //Get the corresponding Hansen object
        final int sIndex = maxDegree + (j < 0 ? -s : s);
        final int jIndex = FastMath.abs(j);
        final HansenTesseralLinear hans = hansenObjects.getHansenObjects()[sIndex][jIndex];

        // n-SUM from nmin to N
        for (int n = nmin; n <= maxN; n++) {
            // If (n - s) is odd, the contribution is null because of Vmns
            if ((n - s) % 2 == 0) {

                // Vmns coefficient
                final double vMNS   = CoefficientsFactory.getVmns(m, n, s);

                // Inclination function Gamma and derivative
                final double gaMNS  = gammaMNS.getValue(m, n, s);
                final double dGaMNS = gammaMNS.getDerivative(m, n, s);

                // Hansen kernel value and derivative
                final double kJNS   = hans.getValue(-n - 1, context.getChi());
                final double dkJNS  = hans.getDerivative(-n - 1, context.getChi());

                // Gjms, Hjms polynomials and derivatives
                final double gMSJ   = ghMSJ.getGmsj(m, s, j);
                final double hMSJ   = ghMSJ.getHmsj(m, s, j);
                final double dGdh   = ghMSJ.getdGmsdh(m, s, j);
                final double dGdk   = ghMSJ.getdGmsdk(m, s, j);
                final double dGdA   = ghMSJ.getdGmsdAlpha(m, s, j);
                final double dGdB   = ghMSJ.getdGmsdBeta(m, s, j);
                final double dHdh   = ghMSJ.getdHmsdh(m, s, j);
                final double dHdk   = ghMSJ.getdHmsdk(m, s, j);
                final double dHdA   = ghMSJ.getdHmsdAlpha(m, s, j);
                final double dHdB   = ghMSJ.getdHmsdBeta(m, s, j);

                // Jacobi l-index from 2.7.1-(15)
                final int l = FastMath.min(n - m, n - FastMath.abs(s));
                // Jacobi polynomial and derivative
                final double[] jacobi = JacobiPolynomials.getValueAndDerivative(l, v, w, auxiliaryElements.getGamma());

                // Geopotential coefficients
                final double cnm = harmonics.getUnnormalizedCnm(n, m);
                final double snm = harmonics.getUnnormalizedSnm(n, m);

                // Common factors from expansion of equations 3.3-4
                final double cf_0      = roaPow[n] * Im * vMNS;
                final double cf_1      = cf_0 * gaMNS * jacobi[0]; //jacobi.getValue();
                final double cf_2      = cf_1 * kJNS;
                final double gcPhs     = gMSJ * cnm + hMSJ * snm;
                final double gsMhc     = gMSJ * snm - hMSJ * cnm;
                final double dKgcPhsx2 = 2. * dkJNS * gcPhs;
                final double dKgsMhcx2 = 2. * dkJNS * gsMhc;
                final double dUdaCoef  = (n + 1) * cf_2;
                final double dUdlCoef  = j * cf_2;
                //final double dUdGaCoef = cf_0 * kJNS * (jacobi.getValue() * dGaMNS + gaMNS * jacobi.getGradient()[0]);
                final double dUdGaCoef = cf_0 * kJNS * (jacobi[0] * dGaMNS + gaMNS * jacobi[1]);

                // dU / da components
                dUdaCos  += dUdaCoef * gcPhs;
                dUdaSin  += dUdaCoef * gsMhc;

                // dU / dh components
                dUdhCos  += cf_1 * (kJNS * (cnm * dGdh + snm * dHdh) + auxiliaryElements.getH() * dKgcPhsx2);
                dUdhSin  += cf_1 * (kJNS * (snm * dGdh - cnm * dHdh) + auxiliaryElements.getH() * dKgsMhcx2);

                // dU / dk components
                dUdkCos  += cf_1 * (kJNS * (cnm * dGdk + snm * dHdk) + auxiliaryElements.getK() * dKgcPhsx2);
                dUdkSin  += cf_1 * (kJNS * (snm * dGdk - cnm * dHdk) + auxiliaryElements.getK() * dKgsMhcx2);

                // dU / dLambda components
                dUdlCos  +=  dUdlCoef * gsMhc;
                dUdlSin  += -dUdlCoef * gcPhs;

                // dU / alpha components
                dUdAlCos += cf_2 * (dGdA * cnm + dHdA * snm);
                dUdAlSin += cf_2 * (dGdA * snm - dHdA * cnm);

                // dU / dBeta components
                dUdBeCos += cf_2 * (dGdB * cnm + dHdB * snm);
                dUdBeSin += cf_2 * (dGdB * snm - dHdB * cnm);

                // dU / dGamma components
                dUdGaCos += dUdGaCoef * gcPhs;
                dUdGaSin += dUdGaCoef * gsMhc;
            }
        }

        return new double[][] { { dUdaCos,  dUdaSin  },
                                { dUdhCos,  dUdhSin  },
                                { dUdkCos,  dUdkSin  },
                                { dUdlCos,  dUdlSin  },
                                { dUdAlCos, dUdAlSin },
                                { dUdBeCos, dUdBeSin },
                                { dUdGaCos, dUdGaSin } };
    }

    /** Compute the n-SUM for potential derivatives components.
     *  @param <T> the type of the field elements
     *  @param date current date
     *  @param j resonant index <i>j</i>
     *  @param m resonant order <i>m</i>
     *  @param s d'Alembert characteristic <i>s</i>
     *  @param maxN maximum possible value for <i>n</i> index
     *  @param roaPow powers of R/a up to degree <i>n</i>
     *  @param ghMSJ G<sup>j</sup><sub>m,s</sub> and H<sup>j</sup><sub>m,s</sub> polynomials
     *  @param gammaMNS &Gamma;<sup>m</sup><sub>n,s</sub>(γ) function
     *  @param context container for attributes
     *  @param hansenObjects initialization of hansen objects
     *  @return Components of U<sub>n</sub> derivatives for fixed j, m, s
     */
    private <T extends CalculusFieldElement<T>> T[][] computeNSum(final FieldAbsoluteDate<T> date,
                                                              final int j, final int m, final int s, final int maxN,
                                                              final T[] roaPow,
                                                              final FieldGHmsjPolynomials<T> ghMSJ,
                                                              final FieldGammaMnsFunction<T> gammaMNS,
                                                              final FieldDSSTTesseralContext<T> context,
                                                              final FieldHansenObjects<T> hansenObjects) {

        // Auxiliary elements related to the current orbit
        final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();
        // Zero for initialization
        final Field<T> field = date.getField();
        final T zero = field.getZero();

        //spherical harmonics
        final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date.toAbsoluteDate());

        // Potential derivatives components
        T dUdaCos  = zero;
        T dUdaSin  = zero;
        T dUdhCos  = zero;
        T dUdhSin  = zero;
        T dUdkCos  = zero;
        T dUdkSin  = zero;
        T dUdlCos  = zero;
        T dUdlSin  = zero;
        T dUdAlCos = zero;
        T dUdAlSin = zero;
        T dUdBeCos = zero;
        T dUdBeSin = zero;
        T dUdGaCos = zero;
        T dUdGaSin = zero;

        // I^m
        @SuppressWarnings("unused")
        final int Im = I > 0 ? 1 : (m % 2 == 0 ? 1 : -1);

        // jacobi v, w, indices from 2.7.1-(15)
        final int v = FastMath.abs(m - s);
        final int w = FastMath.abs(m + s);

        // Initialise lower degree nmin = (Max(2, m, |s|)) for summation over n
        final int nmin = FastMath.max(FastMath.max(2, m), FastMath.abs(s));

        //Get the corresponding Hansen object
        final int sIndex = maxDegree + (j < 0 ? -s : s);
        final int jIndex = FastMath.abs(j);
        final FieldHansenTesseralLinear<T> hans = hansenObjects.getHansenObjects()[sIndex][jIndex];

        // n-SUM from nmin to N
        for (int n = nmin; n <= maxN; n++) {
            // If (n - s) is odd, the contribution is null because of Vmns
            if ((n - s) % 2 == 0) {

                // Vmns coefficient
                final T vMNS   = zero.add(CoefficientsFactory.getVmns(m, n, s));

                // Inclination function Gamma and derivative
                final T gaMNS  = gammaMNS.getValue(m, n, s);
                final T dGaMNS = gammaMNS.getDerivative(m, n, s);

                // Hansen kernel value and derivative
                final T kJNS   = hans.getValue(-n - 1, context.getChi());
                final T dkJNS  = hans.getDerivative(-n - 1, context.getChi());

                // Gjms, Hjms polynomials and derivatives
                final T gMSJ   = ghMSJ.getGmsj(m, s, j);
                final T hMSJ   = ghMSJ.getHmsj(m, s, j);
                final T dGdh   = ghMSJ.getdGmsdh(m, s, j);
                final T dGdk   = ghMSJ.getdGmsdk(m, s, j);
                final T dGdA   = ghMSJ.getdGmsdAlpha(m, s, j);
                final T dGdB   = ghMSJ.getdGmsdBeta(m, s, j);
                final T dHdh   = ghMSJ.getdHmsdh(m, s, j);
                final T dHdk   = ghMSJ.getdHmsdk(m, s, j);
                final T dHdA   = ghMSJ.getdHmsdAlpha(m, s, j);
                final T dHdB   = ghMSJ.getdHmsdBeta(m, s, j);

                // Jacobi l-index from 2.7.1-(15)
                final int l = FastMath.min(n - m, n - FastMath.abs(s));
                // Jacobi polynomial and derivative
                final FieldGradient<T> jacobi =
                        JacobiPolynomials.getValue(l, v, w, FieldGradient.variable(1, 0, auxiliaryElements.getGamma()));

                // Geopotential coefficients
                final T cnm = zero.add(harmonics.getUnnormalizedCnm(n, m));
                final T snm = zero.add(harmonics.getUnnormalizedSnm(n, m));

                // Common factors from expansion of equations 3.3-4
                final T cf_0      = roaPow[n].multiply(Im).multiply(vMNS);
                final T cf_1      = cf_0.multiply(gaMNS).multiply(jacobi.getValue());
                final T cf_2      = cf_1.multiply(kJNS);
                final T gcPhs     = gMSJ.multiply(cnm).add(hMSJ.multiply(snm));
                final T gsMhc     = gMSJ.multiply(snm).subtract(hMSJ.multiply(cnm));
                final T dKgcPhsx2 = dkJNS.multiply(gcPhs).multiply(2.);
                final T dKgsMhcx2 = dkJNS.multiply(gsMhc).multiply(2.);
                final T dUdaCoef  = cf_2.multiply(n + 1);
                final T dUdlCoef  = cf_2.multiply(j);
                final T dUdGaCoef = cf_0.multiply(kJNS).multiply(dGaMNS.multiply(jacobi.getValue()).add(gaMNS.multiply(jacobi.getGradient()[0])));

                // dU / da components
                dUdaCos  = dUdaCos.add(dUdaCoef.multiply(gcPhs));
                dUdaSin  = dUdaSin.add(dUdaCoef.multiply(gsMhc));

                // dU / dh components
                dUdhCos  = dUdhCos.add(cf_1.multiply(kJNS.multiply(cnm.multiply(dGdh).add(snm.multiply(dHdh))).add(dKgcPhsx2.multiply(auxiliaryElements.getH()))));
                dUdhSin  = dUdhSin.add(cf_1.multiply(kJNS.multiply(snm.multiply(dGdh).subtract(cnm.multiply(dHdh))).add(dKgsMhcx2.multiply(auxiliaryElements.getH()))));

                // dU / dk components
                dUdkCos  = dUdkCos.add(cf_1.multiply(kJNS.multiply(cnm.multiply(dGdk).add(snm.multiply(dHdk))).add(dKgcPhsx2.multiply(auxiliaryElements.getK()))));
                dUdkSin  = dUdkSin.add(cf_1.multiply(kJNS.multiply(snm.multiply(dGdk).subtract(cnm.multiply(dHdk))).add(dKgsMhcx2.multiply(auxiliaryElements.getK()))));

                // dU / dLambda components
                dUdlCos  = dUdlCos.add(dUdlCoef.multiply(gsMhc));
                dUdlSin  = dUdlSin.add(dUdlCoef.multiply(gcPhs).negate());

                // dU / alpha components
                dUdAlCos = dUdAlCos.add(cf_2.multiply(dGdA.multiply(cnm).add(dHdA.multiply(snm))));
                dUdAlSin = dUdAlSin.add(cf_2.multiply(dGdA.multiply(snm).subtract(dHdA.multiply(cnm))));

                // dU / dBeta components
                dUdBeCos = dUdBeCos.add(cf_2.multiply(dGdB.multiply(cnm).add(dHdB.multiply(snm))));
                dUdBeSin = dUdBeSin.add(cf_2.multiply(dGdB.multiply(snm).subtract(dHdB.multiply(cnm))));

                // dU / dGamma components
                dUdGaCos = dUdGaCos.add(dUdGaCoef.multiply(gcPhs));
                dUdGaSin = dUdGaSin.add(dUdGaCoef.multiply(gsMhc));
            }
        }

        final T[][] derivatives = MathArrays.buildArray(field, 7, 2);
        derivatives[0][0] = dUdaCos;
        derivatives[0][1] = dUdaSin;
        derivatives[1][0] = dUdhCos;
        derivatives[1][1] = dUdhSin;
        derivatives[2][0] = dUdkCos;
        derivatives[2][1] = dUdkSin;
        derivatives[3][0] = dUdlCos;
        derivatives[3][1] = dUdlSin;
        derivatives[4][0] = dUdAlCos;
        derivatives[4][1] = dUdAlSin;
        derivatives[5][0] = dUdBeCos;
        derivatives[5][1] = dUdBeSin;
        derivatives[6][0] = dUdGaCos;
        derivatives[6][1] = dUdGaSin;

        return derivatives;

    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider attitudeProvider) {
        //nothing is done since this contribution is not sensitive to attitude
    }

    /** Compute the C<sup>j</sup> and the S<sup>j</sup> coefficients.
     *  <p>
     *  Those coefficients are given in Danielson paper by substituting the
     *  disturbing function (2.7.1-16) with m != 0 into (2.2-10)
     *  </p>
     */
    private class FourierCjSjCoefficients {

        /** Absolute limit for j ( -jMax <= j <= jMax ).  */
        private final int jMax;

        /** The C<sub>i</sub><sup>jm</sup> coefficients.
         * <p>
         * The index order is [m][j][i] <br/>
         * The i index corresponds to the C<sub>i</sub><sup>jm</sup> coefficients used to
         * compute the following: <br/>
         * - da/dt <br/>
         * - dk/dt <br/>
         * - dh/dt / dk <br/>
         * - dq/dt <br/>
         * - dp/dt / dα <br/>
         * - dλ/dt / dβ <br/>
         * </p>
         */
        private final double[][][] cCoef;

        /** The S<sub>i</sub><sup>jm</sup> coefficients.
         * <p>
         * The index order is [m][j][i] <br/>
         * The i index corresponds to the C<sub>i</sub><sup>jm</sup> coefficients used to
         * compute the following: <br/>
         * - da/dt <br/>
         * - dk/dt <br/>
         * - dh/dt / dk <br/>
         * - dq/dt <br/>
         * - dp/dt / dα <br/>
         * - dλ/dt / dβ <br/>
         * </p>
         */
        private final double[][][] sCoef;

        /** G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials. */
        private GHmsjPolynomials ghMSJ;

        /** &Gamma;<sub>ns</sub><sup>m</sup> function. */
        private GammaMnsFunction gammaMNS;

        /** R / a up to power degree. */
        private final double[] roaPow;

        /** Create a set of C<sub>i</sub><sup>jm</sup> and S<sub>i</sub><sup>jm</sup> coefficients.
         *  @param jMax absolute limit for j ( -jMax <= j <= jMax )
         *  @param mMax maximum value for m
         */
        FourierCjSjCoefficients(final int jMax, final int mMax) {
            // initialise fields
            final int rows    = mMax + 1;
            final int columns = 2 * jMax + 1;
            this.jMax         = jMax;
            this.cCoef        = new double[rows][columns][6];
            this.sCoef        = new double[rows][columns][6];
            this.roaPow       = new double[maxDegree + 1];
            roaPow[0] = 1.;
        }

        /**
         * Generate the coefficients.
         * @param date the current date
         * @param context container for attributes
         * @param hansenObjects initialization of hansen objects
         */
        public void generateCoefficients(final AbsoluteDate date, final DSSTTesseralContext context,
                                         final HansenObjects hansenObjects) {

            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            // Compute only if there is at least one non-resonant tesseral
            if (!nonResOrders.isEmpty() || maxDegreeTesseralSP < 0) {
                // Gmsj and Hmsj polynomials
                ghMSJ = new GHmsjPolynomials(auxiliaryElements.getK(), auxiliaryElements.getH(), auxiliaryElements.getAlpha(), auxiliaryElements.getBeta(), I);

                // GAMMAmns function
                gammaMNS = new GammaMnsFunction(maxDegree, auxiliaryElements.getGamma(), I);

                final int maxRoaPower = FastMath.max(maxDegreeTesseralSP, maxDegreeMdailyTesseralSP);

                // R / a up to power degree
                for (int i = 1; i <= maxRoaPower; i++) {
                    roaPow[i] = context.getRoa() * roaPow[i - 1];
                }

                //generate the m-daily coefficients
                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    buildFourierCoefficients(date, m, 0, maxDegreeMdailyTesseralSP, context, hansenObjects);
                }

                // generate the other coefficients only if required
                if (maxDegreeTesseralSP >= 0) {
                    for (int m: nonResOrders.keySet()) {
                        final List<Integer> listJ = nonResOrders.get(m);

                        for (int j: listJ) {
                            buildFourierCoefficients(date, m, j, maxDegreeTesseralSP, context, hansenObjects);
                        }
                    }
                }
            }
        }

        /** Build a set of fourier coefficients for a given m and j.
         *
         * @param date the date of the coefficients
         * @param m m index
         * @param j j index
         * @param maxN  maximum value for n index
         * @param context container for attributes
         * @param hansenObjects initialization of hansen objects
         */
        private void buildFourierCoefficients(final AbsoluteDate date,
               final int m, final int j, final int maxN, final DSSTTesseralContext context,
               final HansenObjects hansenObjects) {

            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            // Potential derivatives components for a given non-resonant pair {j,m}
            double dRdaCos  = 0.;
            double dRdaSin  = 0.;
            double dRdhCos  = 0.;
            double dRdhSin  = 0.;
            double dRdkCos  = 0.;
            double dRdkSin  = 0.;
            double dRdlCos  = 0.;
            double dRdlSin  = 0.;
            double dRdAlCos = 0.;
            double dRdAlSin = 0.;
            double dRdBeCos = 0.;
            double dRdBeSin = 0.;
            double dRdGaCos = 0.;
            double dRdGaSin = 0.;

            // s-SUM from -sMin to sMax
            final int sMin = j == 0 ? maxEccPowMdailyTesseralSP : maxEccPowTesseralSP;
            final int sMax = j == 0 ? maxEccPowMdailyTesseralSP : maxEccPowTesseralSP;
            for (int s = 0; s <= sMax; s++) {

                // n-SUM for s positive
                final double[][] nSumSpos = computeNSum(date, j, m, s, maxN,
                                                        roaPow, ghMSJ, gammaMNS, context, hansenObjects);
                dRdaCos  += nSumSpos[0][0];
                dRdaSin  += nSumSpos[0][1];
                dRdhCos  += nSumSpos[1][0];
                dRdhSin  += nSumSpos[1][1];
                dRdkCos  += nSumSpos[2][0];
                dRdkSin  += nSumSpos[2][1];
                dRdlCos  += nSumSpos[3][0];
                dRdlSin  += nSumSpos[3][1];
                dRdAlCos += nSumSpos[4][0];
                dRdAlSin += nSumSpos[4][1];
                dRdBeCos += nSumSpos[5][0];
                dRdBeSin += nSumSpos[5][1];
                dRdGaCos += nSumSpos[6][0];
                dRdGaSin += nSumSpos[6][1];

                // n-SUM for s negative
                if (s > 0 && s <= sMin) {
                    final double[][] nSumSneg = computeNSum(date, j, m, -s, maxN,
                                                            roaPow, ghMSJ, gammaMNS, context, hansenObjects);
                    dRdaCos  += nSumSneg[0][0];
                    dRdaSin  += nSumSneg[0][1];
                    dRdhCos  += nSumSneg[1][0];
                    dRdhSin  += nSumSneg[1][1];
                    dRdkCos  += nSumSneg[2][0];
                    dRdkSin  += nSumSneg[2][1];
                    dRdlCos  += nSumSneg[3][0];
                    dRdlSin  += nSumSneg[3][1];
                    dRdAlCos += nSumSneg[4][0];
                    dRdAlSin += nSumSneg[4][1];
                    dRdBeCos += nSumSneg[5][0];
                    dRdBeSin += nSumSneg[5][1];
                    dRdGaCos += nSumSneg[6][0];
                    dRdGaSin += nSumSneg[6][1];
                }
            }
            dRdaCos  *= -context.getMoa() / auxiliaryElements.getSma();
            dRdaSin  *= -context.getMoa() / auxiliaryElements.getSma();
            dRdhCos  *=  context.getMoa();
            dRdhSin  *=  context.getMoa();
            dRdkCos  *=  context.getMoa();
            dRdkSin  *=  context.getMoa();
            dRdlCos  *=  context.getMoa();
            dRdlSin  *=  context.getMoa();
            dRdAlCos *=  context.getMoa();
            dRdAlSin *=  context.getMoa();
            dRdBeCos *=  context.getMoa();
            dRdBeSin *=  context.getMoa();
            dRdGaCos *=  context.getMoa();
            dRdGaSin *=  context.getMoa();

            // Compute the cross derivative operator :
            final double RAlphaGammaCos   = auxiliaryElements.getAlpha() * dRdGaCos - auxiliaryElements.getGamma() * dRdAlCos;
            final double RAlphaGammaSin   = auxiliaryElements.getAlpha() * dRdGaSin - auxiliaryElements.getGamma() * dRdAlSin;
            final double RAlphaBetaCos    = auxiliaryElements.getAlpha() * dRdBeCos - auxiliaryElements.getBeta()  * dRdAlCos;
            final double RAlphaBetaSin    = auxiliaryElements.getAlpha() * dRdBeSin - auxiliaryElements.getBeta()  * dRdAlSin;
            final double RBetaGammaCos    =  auxiliaryElements.getBeta() * dRdGaCos - auxiliaryElements.getGamma() * dRdBeCos;
            final double RBetaGammaSin    =  auxiliaryElements.getBeta() * dRdGaSin - auxiliaryElements.getGamma() * dRdBeSin;
            final double RhkCos           =     auxiliaryElements.getH() * dRdkCos  -     auxiliaryElements.getK() * dRdhCos;
            final double RhkSin           =     auxiliaryElements.getH() * dRdkSin  -     auxiliaryElements.getK() * dRdhSin;
            final double pRagmIqRbgoABCos = (auxiliaryElements.getP() * RAlphaGammaCos - I * auxiliaryElements.getQ() * RBetaGammaCos) * context.getOoAB();
            final double pRagmIqRbgoABSin = (auxiliaryElements.getP() * RAlphaGammaSin - I * auxiliaryElements.getQ() * RBetaGammaSin) * context.getOoAB();
            final double RhkmRabmdRdlCos  =  RhkCos - RAlphaBetaCos - dRdlCos;
            final double RhkmRabmdRdlSin  =  RhkSin - RAlphaBetaSin - dRdlSin;

            // da/dt
            cCoef[m][j + jMax][0] = context.getAx2oA() * dRdlCos;
            sCoef[m][j + jMax][0] = context.getAx2oA() * dRdlSin;

            // dk/dt
            cCoef[m][j + jMax][1] = -(context.getBoA() * dRdhCos + auxiliaryElements.getH() * pRagmIqRbgoABCos + auxiliaryElements.getK() * context.getBoABpo() * dRdlCos);
            sCoef[m][j + jMax][1] = -(context.getBoA() * dRdhSin + auxiliaryElements.getH() * pRagmIqRbgoABSin + auxiliaryElements.getK() * context.getBoABpo() * dRdlSin);

            // dh/dt
            cCoef[m][j + jMax][2] = context.getBoA() * dRdkCos + auxiliaryElements.getK() * pRagmIqRbgoABCos - auxiliaryElements.getH() * context.getBoABpo() * dRdlCos;
            sCoef[m][j + jMax][2] = context.getBoA() * dRdkSin + auxiliaryElements.getK() * pRagmIqRbgoABSin - auxiliaryElements.getH() * context.getBoABpo() * dRdlSin;

            // dq/dt
            cCoef[m][j + jMax][3] = context.getCo2AB() * (auxiliaryElements.getQ() * RhkmRabmdRdlCos - I * RAlphaGammaCos);
            sCoef[m][j + jMax][3] = context.getCo2AB() * (auxiliaryElements.getQ() * RhkmRabmdRdlSin - I * RAlphaGammaSin);

            // dp/dt
            cCoef[m][j + jMax][4] = context.getCo2AB() * (auxiliaryElements.getP() * RhkmRabmdRdlCos - RBetaGammaCos);
            sCoef[m][j + jMax][4] = context.getCo2AB() * (auxiliaryElements.getP() * RhkmRabmdRdlSin - RBetaGammaSin);

            // dλ/dt
            cCoef[m][j + jMax][5] = -context.getAx2oA() * dRdaCos + context.getBoABpo() * (auxiliaryElements.getH() * dRdhCos + auxiliaryElements.getK() * dRdkCos) + pRagmIqRbgoABCos;
            sCoef[m][j + jMax][5] = -context.getAx2oA() * dRdaSin + context.getBoABpo() * (auxiliaryElements.getH() * dRdhSin + auxiliaryElements.getK() * dRdkSin) + pRagmIqRbgoABSin;
        }

        /** Get the coefficient C<sub>i</sub><sup>jm</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @param m m index
         * @return the coefficient C<sub>i</sub><sup>jm</sup>
         */
        public double getCijm(final int i, final int j, final int m) {
            return cCoef[m][j + jMax][i];
        }

        /** Get the coefficient S<sub>i</sub><sup>jm</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @param m m index
         * @return the coefficient S<sub>i</sub><sup>jm</sup>
         */
        public double getSijm(final int i, final int j, final int m) {
            return sCoef[m][j + jMax][i];
        }
    }

    /** Compute the C<sup>j</sup> and the S<sup>j</sup> coefficients.
     *  <p>
     *  Those coefficients are given in Danielson paper by substituting the
     *  disturbing function (2.7.1-16) with m != 0 into (2.2-10)
     *  </p>
     */
    private class FieldFourierCjSjCoefficients <T extends CalculusFieldElement<T>> {

        /** Absolute limit for j ( -jMax <= j <= jMax ).  */
        private final int jMax;

        /** The C<sub>i</sub><sup>jm</sup> coefficients.
         * <p>
         * The index order is [m][j][i] <br/>
         * The i index corresponds to the C<sub>i</sub><sup>jm</sup> coefficients used to
         * compute the following: <br/>
         * - da/dt <br/>
         * - dk/dt <br/>
         * - dh/dt / dk <br/>
         * - dq/dt <br/>
         * - dp/dt / dα <br/>
         * - dλ/dt / dβ <br/>
         * </p>
         */
        private final T[][][] cCoef;

        /** The S<sub>i</sub><sup>jm</sup> coefficients.
         * <p>
         * The index order is [m][j][i] <br/>
         * The i index corresponds to the C<sub>i</sub><sup>jm</sup> coefficients used to
         * compute the following: <br/>
         * - da/dt <br/>
         * - dk/dt <br/>
         * - dh/dt / dk <br/>
         * - dq/dt <br/>
         * - dp/dt / dα <br/>
         * - dλ/dt / dβ <br/>
         * </p>
         */
        private final T[][][] sCoef;

        /** G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials. */
        private FieldGHmsjPolynomials<T> ghMSJ;

        /** &Gamma;<sub>ns</sub><sup>m</sup> function. */
        private FieldGammaMnsFunction<T> gammaMNS;

        /** R / a up to power degree. */
        private final T[] roaPow;

        /** Create a set of C<sub>i</sub><sup>jm</sup> and S<sub>i</sub><sup>jm</sup> coefficients.
         *  @param jMax absolute limit for j ( -jMax <= j <= jMax )
         *  @param mMax maximum value for m
         *  @param field field used by default
         */
        FieldFourierCjSjCoefficients(final int jMax, final int mMax, final Field<T> field) {
            // initialise fields
            final T zero = field.getZero();
            final int rows    = mMax + 1;
            final int columns = 2 * jMax + 1;
            this.jMax         = jMax;
            this.cCoef        = MathArrays.buildArray(field, rows, columns, 6);
            this.sCoef        = MathArrays.buildArray(field, rows, columns, 6);
            this.roaPow       = MathArrays.buildArray(field, maxDegree + 1);
            roaPow[0] = zero.add(1.);
        }

        /**
         * Generate the coefficients.
         * @param date the current date
         * @param context container for attributes
         * @param hansenObjects initialization of hansen objects
         * @param field field used by default
         */
        public void generateCoefficients(final FieldAbsoluteDate<T> date,
                                         final FieldDSSTTesseralContext<T> context,
                                         final FieldHansenObjects<T> hansenObjects,
                                         final Field<T> field) {

            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();
            // Compute only if there is at least one non-resonant tesseral
            if (!nonResOrders.isEmpty() || maxDegreeTesseralSP < 0) {
                // Gmsj and Hmsj polynomials
                ghMSJ = new FieldGHmsjPolynomials<>(auxiliaryElements.getK(), auxiliaryElements.getH(), auxiliaryElements.getAlpha(), auxiliaryElements.getBeta(), I, field);

                // GAMMAmns function
                gammaMNS = new FieldGammaMnsFunction<>(maxDegree, auxiliaryElements.getGamma(), I, field);

                final int maxRoaPower = FastMath.max(maxDegreeTesseralSP, maxDegreeMdailyTesseralSP);

                // R / a up to power degree
                for (int i = 1; i <= maxRoaPower; i++) {
                    roaPow[i] = context.getRoa().multiply(roaPow[i - 1]);
                }

                //generate the m-daily coefficients
                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    buildFourierCoefficients(date, m, 0, maxDegreeMdailyTesseralSP, context, hansenObjects, field);
                }

                // generate the other coefficients only if required
                if (maxDegreeTesseralSP >= 0) {
                    for (int m: nonResOrders.keySet()) {
                        final List<Integer> listJ = nonResOrders.get(m);

                        for (int j: listJ) {
                            buildFourierCoefficients(date, m, j, maxDegreeTesseralSP, context, hansenObjects, field);
                        }
                    }
                }
            }
        }

        /** Build a set of fourier coefficients for a given m and j.
         *
         * @param date the date of the coefficients
         * @param m m index
         * @param j j index
         * @param maxN  maximum value for n index
         * @param context container for attributes
         * @param hansenObjects initialization of hansen objects
         * @param field field used by default
         */
        private void buildFourierCoefficients(final FieldAbsoluteDate<T> date,
                                              final int m, final int j, final int maxN,
                                              final FieldDSSTTesseralContext<T> context,
                                              final FieldHansenObjects<T> hansenObjects,
                                              final Field<T> field) {

            // Zero
            final T zero = field.getZero();
            // Common parameters
            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            // Potential derivatives components for a given non-resonant pair {j,m}
            T dRdaCos  = zero;
            T dRdaSin  = zero;
            T dRdhCos  = zero;
            T dRdhSin  = zero;
            T dRdkCos  = zero;
            T dRdkSin  = zero;
            T dRdlCos  = zero;
            T dRdlSin  = zero;
            T dRdAlCos = zero;
            T dRdAlSin = zero;
            T dRdBeCos = zero;
            T dRdBeSin = zero;
            T dRdGaCos = zero;
            T dRdGaSin = zero;

            // s-SUM from -sMin to sMax
            final int sMin = j == 0 ? maxEccPowMdailyTesseralSP : maxEccPowTesseralSP;
            final int sMax = j == 0 ? maxEccPowMdailyTesseralSP : maxEccPowTesseralSP;
            for (int s = 0; s <= sMax; s++) {

                // n-SUM for s positive
                final T[][] nSumSpos = computeNSum(date, j, m, s, maxN,
                                                        roaPow, ghMSJ, gammaMNS, context, hansenObjects);
                dRdaCos  =  dRdaCos.add(nSumSpos[0][0]);
                dRdaSin  =  dRdaSin.add(nSumSpos[0][1]);
                dRdhCos  =  dRdhCos.add(nSumSpos[1][0]);
                dRdhSin  =  dRdhSin.add(nSumSpos[1][1]);
                dRdkCos  =  dRdkCos.add(nSumSpos[2][0]);
                dRdkSin  =  dRdkSin.add(nSumSpos[2][1]);
                dRdlCos  =  dRdlCos.add(nSumSpos[3][0]);
                dRdlSin  =  dRdlSin.add(nSumSpos[3][1]);
                dRdAlCos = dRdAlCos.add(nSumSpos[4][0]);
                dRdAlSin = dRdAlSin.add(nSumSpos[4][1]);
                dRdBeCos = dRdBeCos.add(nSumSpos[5][0]);
                dRdBeSin = dRdBeSin.add(nSumSpos[5][1]);
                dRdGaCos = dRdGaCos.add(nSumSpos[6][0]);
                dRdGaSin = dRdGaSin.add(nSumSpos[6][1]);

                // n-SUM for s negative
                if (s > 0 && s <= sMin) {
                    final T[][] nSumSneg = computeNSum(date, j, m, -s, maxN,
                                                            roaPow, ghMSJ, gammaMNS, context, hansenObjects);
                    dRdaCos  =  dRdaCos.add(nSumSneg[0][0]);
                    dRdaSin  =  dRdaSin.add(nSumSneg[0][1]);
                    dRdhCos  =  dRdhCos.add(nSumSneg[1][0]);
                    dRdhSin  =  dRdhSin.add(nSumSneg[1][1]);
                    dRdkCos  =  dRdkCos.add(nSumSneg[2][0]);
                    dRdkSin  =  dRdkSin.add(nSumSneg[2][1]);
                    dRdlCos  =  dRdlCos.add(nSumSneg[3][0]);
                    dRdlSin  =  dRdlSin.add(nSumSneg[3][1]);
                    dRdAlCos = dRdAlCos.add(nSumSneg[4][0]);
                    dRdAlSin = dRdAlSin.add(nSumSneg[4][1]);
                    dRdBeCos = dRdBeCos.add(nSumSneg[5][0]);
                    dRdBeSin = dRdBeSin.add(nSumSneg[5][1]);
                    dRdGaCos = dRdGaCos.add(nSumSneg[6][0]);
                    dRdGaSin = dRdGaSin.add(nSumSneg[6][1]);
                }
            }
            dRdaCos  =  dRdaCos.multiply(context.getMoa().negate().divide(auxiliaryElements.getSma()));
            dRdaSin  =  dRdaSin.multiply(context.getMoa().negate().divide(auxiliaryElements.getSma()));
            dRdhCos  =  dRdhCos.multiply(context.getMoa());
            dRdhSin  =  dRdhSin.multiply(context.getMoa());
            dRdkCos  =  dRdkCos.multiply(context.getMoa());
            dRdkSin  =  dRdkSin.multiply(context.getMoa());
            dRdlCos  =  dRdlCos.multiply(context.getMoa());
            dRdlSin  =  dRdlSin.multiply(context.getMoa());
            dRdAlCos = dRdAlCos.multiply(context.getMoa());
            dRdAlSin = dRdAlSin.multiply(context.getMoa());
            dRdBeCos = dRdBeCos.multiply(context.getMoa());
            dRdBeSin = dRdBeSin.multiply(context.getMoa());
            dRdGaCos = dRdGaCos.multiply(context.getMoa());
            dRdGaSin = dRdGaSin.multiply(context.getMoa());

            // Compute the cross derivative operator :
            final T RAlphaGammaCos   = auxiliaryElements.getAlpha().multiply(dRdGaCos).subtract(auxiliaryElements.getGamma().multiply(dRdAlCos));
            final T RAlphaGammaSin   = auxiliaryElements.getAlpha().multiply(dRdGaSin).subtract(auxiliaryElements.getGamma().multiply(dRdAlSin));
            final T RAlphaBetaCos    = auxiliaryElements.getAlpha().multiply(dRdBeCos).subtract(auxiliaryElements.getBeta().multiply(dRdAlCos));
            final T RAlphaBetaSin    = auxiliaryElements.getAlpha().multiply(dRdBeSin).subtract(auxiliaryElements.getBeta().multiply(dRdAlSin));
            final T RBetaGammaCos    =  auxiliaryElements.getBeta().multiply(dRdGaCos).subtract(auxiliaryElements.getGamma().multiply(dRdBeCos));
            final T RBetaGammaSin    =  auxiliaryElements.getBeta().multiply(dRdGaSin).subtract(auxiliaryElements.getGamma().multiply(dRdBeSin));
            final T RhkCos           =     auxiliaryElements.getH().multiply(dRdkCos).subtract(auxiliaryElements.getK().multiply(dRdhCos));
            final T RhkSin           =     auxiliaryElements.getH().multiply(dRdkSin).subtract(auxiliaryElements.getK().multiply(dRdhSin));
            final T pRagmIqRbgoABCos = (auxiliaryElements.getP().multiply(RAlphaGammaCos).subtract(auxiliaryElements.getQ().multiply(RBetaGammaCos).multiply(I))).multiply(context.getOoAB());
            final T pRagmIqRbgoABSin = (auxiliaryElements.getP().multiply(RAlphaGammaSin).subtract(auxiliaryElements.getQ().multiply(RBetaGammaSin).multiply(I))).multiply(context.getOoAB());
            final T RhkmRabmdRdlCos  =  RhkCos.subtract(RAlphaBetaCos).subtract(dRdlCos);
            final T RhkmRabmdRdlSin  =  RhkSin.subtract(RAlphaBetaSin).subtract(dRdlSin);

            // da/dt
            cCoef[m][j + jMax][0] = context.getAx2oA().multiply(dRdlCos);
            sCoef[m][j + jMax][0] = context.getAx2oA().multiply(dRdlSin);

            // dk/dt
            cCoef[m][j + jMax][1] = (context.getBoA().multiply(dRdhCos).add(auxiliaryElements.getH().multiply(pRagmIqRbgoABCos)).add(auxiliaryElements.getK().multiply(context.getBoABpo()).multiply(dRdlCos))).negate();
            sCoef[m][j + jMax][1] = (context.getBoA().multiply(dRdhSin).add(auxiliaryElements.getH().multiply(pRagmIqRbgoABSin)).add(auxiliaryElements.getK().multiply(context.getBoABpo()).multiply(dRdlSin))).negate();

            // dh/dt
            cCoef[m][j + jMax][2] = context.getBoA().multiply(dRdkCos).add(auxiliaryElements.getK().multiply(pRagmIqRbgoABCos)).subtract(auxiliaryElements.getH().multiply(context.getBoABpo()).multiply(dRdlCos));
            sCoef[m][j + jMax][2] = context.getBoA().multiply(dRdkSin).add(auxiliaryElements.getK().multiply(pRagmIqRbgoABSin)).subtract(auxiliaryElements.getH().multiply(context.getBoABpo()).multiply(dRdlSin));

            // dq/dt
            cCoef[m][j + jMax][3] = context.getCo2AB().multiply(auxiliaryElements.getQ().multiply(RhkmRabmdRdlCos).subtract(RAlphaGammaCos.multiply(I)));
            sCoef[m][j + jMax][3] = context.getCo2AB().multiply(auxiliaryElements.getQ().multiply(RhkmRabmdRdlSin).subtract(RAlphaGammaSin.multiply(I)));

            // dp/dt
            cCoef[m][j + jMax][4] = context.getCo2AB().multiply(auxiliaryElements.getP().multiply(RhkmRabmdRdlCos).subtract(RBetaGammaCos));
            sCoef[m][j + jMax][4] = context.getCo2AB().multiply(auxiliaryElements.getP().multiply(RhkmRabmdRdlSin).subtract(RBetaGammaSin));

            // dλ/dt
            cCoef[m][j + jMax][5] = context.getAx2oA().negate().multiply(dRdaCos).add(context.getBoABpo().multiply(auxiliaryElements.getH().multiply(dRdhCos).add(auxiliaryElements.getK().multiply(dRdkCos)))).add(pRagmIqRbgoABCos);
            sCoef[m][j + jMax][5] = context.getAx2oA().negate().multiply(dRdaSin).add(context.getBoABpo().multiply(auxiliaryElements.getH().multiply(dRdhSin).add(auxiliaryElements.getK().multiply(dRdkSin)))).add(pRagmIqRbgoABSin);
        }

        /** Get the coefficient C<sub>i</sub><sup>jm</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @param m m index
         * @return the coefficient C<sub>i</sub><sup>jm</sup>
         */
        public T getCijm(final int i, final int j, final int m) {
            return cCoef[m][j + jMax][i];
        }

        /** Get the coefficient S<sub>i</sub><sup>jm</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @param m m index
         * @return the coefficient S<sub>i</sub><sup>jm</sup>
         */
        public T getSijm(final int i, final int j, final int m) {
            return sCoef[m][j + jMax][i];
        }
    }

    /** The C<sup>i</sup><sub>m</sub><sub>t</sub> and S<sup>i</sup><sub>m</sub><sub>t</sub> coefficients used to compute
     * the short-periodic zonal contribution.
     *   <p>
     *  Those coefficients are given by expression 2.5.4-5 from the Danielson paper.
     *   </p>
     *
     * @author Sorin Scortan
     *
     */
    private static class TesseralShortPeriodicCoefficients implements ShortPeriodTerms {

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

        /** Central body rotating frame. */
        private final Frame bodyFrame;

        /** Maximal order to consider for short periodics m-daily tesseral harmonics potential. */
        private final int maxOrderMdailyTesseralSP;

        /** Flag to take into account only M-dailies harmonic tesserals for short periodic perturbations.  */
        private final boolean mDailiesOnly;

        /** List of non resonant orders with j != 0. */
        private final SortedMap<Integer, List<Integer> > nonResOrders;

        /** Maximum value for m index. */
        private final int mMax;

        /** Maximum value for j index. */
        private final int jMax;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** All coefficients slots. */
        private final transient TimeSpanMap<Slot> slots;

        /** Constructor.
         * @param bodyFrame central body rotating frame
         * @param maxOrderMdailyTesseralSP maximal order to consider for short periodics m-daily tesseral harmonics potential
         * @param mDailiesOnly flag to take into account only M-dailies harmonic tesserals for short periodic perturbations
         * @param nonResOrders lst of non resonant orders with j != 0
         * @param mMax maximum value for m index
         * @param jMax maximum value for j index
         * @param interpolationPoints number of points used in the interpolation process
         * @param slots all coefficients slots
         */
        TesseralShortPeriodicCoefficients(final Frame bodyFrame, final int maxOrderMdailyTesseralSP,
                                          final boolean mDailiesOnly, final SortedMap<Integer, List<Integer> > nonResOrders,
                                          final int mMax, final int jMax, final int interpolationPoints,
                                          final TimeSpanMap<Slot> slots) {
            this.bodyFrame                = bodyFrame;
            this.maxOrderMdailyTesseralSP = maxOrderMdailyTesseralSP;
            this.mDailiesOnly             = mDailiesOnly;
            this.nonResOrders             = nonResOrders;
            this.mMax                     = mMax;
            this.jMax                     = jMax;
            this.interpolationPoints      = interpolationPoints;
            this.slots                    = slots;
        }

        /** Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        public Slot createSlot(final SpacecraftState... meanStates) {
            final Slot         slot  = new Slot(mMax, jMax, interpolationPoints);
            final AbsoluteDate first = meanStates[0].getDate();
            final AbsoluteDate last  = meanStates[meanStates.length - 1].getDate();
            final int compare = first.compareTo(last);
            if (compare < 0) {
                slots.addValidAfter(slot, first, false);
            } else if (compare > 0) {
                slots.addValidBefore(slot, first, false);
            } else {
                // single date, valid for all time
                slots.addValidAfter(slot, AbsoluteDate.PAST_INFINITY, false);
            }
            return slot;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final Orbit meanOrbit) {

            // select the coefficients slot
            final Slot slot = slots.get(meanOrbit.getDate());

            // Initialise the short periodic variations
            final double[] shortPeriodicVariation = new double[6];

            // Compute only if there is at least one non-resonant tesseral or
            // only the m-daily tesseral should be taken into account
            if (!nonResOrders.isEmpty() || mDailiesOnly) {

                //Build an auxiliary object
                final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(meanOrbit, I);

                // Central body rotation angle from equation 2.7.1-(3)(4).
                final StaticTransform t = bodyFrame.getStaticTransformTo(
                        auxiliaryElements.getFrame(),
                        auxiliaryElements.getDate());
                final Vector3D xB = t.transformVector(Vector3D.PLUS_I);
                final Vector3D yB = t.transformVector(Vector3D.PLUS_J);
                final Vector3D  f = auxiliaryElements.getVectorF();
                final Vector3D  g = auxiliaryElements.getVectorG();
                final double currentTheta = FastMath.atan2(-f.dotProduct(yB) + I * g.dotProduct(xB),
                                                            f.dotProduct(xB) + I * g.dotProduct(yB));

                //Add the m-daily contribution
                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    // Phase angle
                    final double jlMmt  = -m * currentTheta;
                    final SinCos scPhi  = FastMath.sinCos(jlMmt);
                    final double sinPhi = scPhi.sin();
                    final double cosPhi = scPhi.cos();

                    // compute contribution for each element
                    final double[] c = slot.getCijm(0, m, meanOrbit.getDate());
                    final double[] s = slot.getSijm(0, m, meanOrbit.getDate());
                    for (int i = 0; i < 6; i++) {
                        shortPeriodicVariation[i] += c[i] * cosPhi + s[i] * sinPhi;
                    }
                }

                // loop through all non-resonant (j,m) pairs
                for (final Map.Entry<Integer, List<Integer>> entry : nonResOrders.entrySet()) {
                    final int           m     = entry.getKey();
                    final List<Integer> listJ = entry.getValue();

                    for (int j : listJ) {
                        // Phase angle
                        final double jlMmt  = j * meanOrbit.getLM() - m * currentTheta;
                        final SinCos scPhi  = FastMath.sinCos(jlMmt);
                        final double sinPhi = scPhi.sin();
                        final double cosPhi = scPhi.cos();

                        // compute contribution for each element
                        final double[] c = slot.getCijm(j, m, meanOrbit.getDate());
                        final double[] s = slot.getSijm(j, m, meanOrbit.getDate());
                        for (int i = 0; i < 6; i++) {
                            shortPeriodicVariation[i] += c[i] * cosPhi + s[i] * sinPhi;
                        }

                    }
                }
            }

            return shortPeriodicVariation;

        }

        /** {@inheritDoc} */
        @Override
        public String getCoefficientsKeyPrefix() {
            return DSSTTesseral.SHORT_PERIOD_PREFIX;
        }

        /** {@inheritDoc}
         * <p>
         * For tesseral terms contributions,there are maxOrderMdailyTesseralSP
         * m-daily cMm coefficients, maxOrderMdailyTesseralSP m-daily sMm
         * coefficients, nbNonResonant cjm coefficients and nbNonResonant
         * sjm coefficients, where maxOrderMdailyTesseralSP and nbNonResonant both
         * depend on the orbit. The j index is the integer multiplier for the true
         * longitude argument and the m index is the integer multiplier for m-dailies.
         * </p>
         */
        @Override
        public Map<String, double[]> getCoefficients(final AbsoluteDate date, final Set<String> selected) {

            // select the coefficients slot
            final Slot slot = slots.get(date);

            if (!nonResOrders.isEmpty() || mDailiesOnly) {
                final Map<String, double[]> coefficients =
                                new HashMap<String, double[]>(12 * maxOrderMdailyTesseralSP +
                                                              12 * nonResOrders.size());

                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    storeIfSelected(coefficients, selected, slot.getCijm(0, m, date), DSSTTesseral.CM_COEFFICIENTS, m);
                    storeIfSelected(coefficients, selected, slot.getSijm(0, m, date), DSSTTesseral.SM_COEFFICIENTS, m);
                }

                for (final Map.Entry<Integer, List<Integer>> entry : nonResOrders.entrySet()) {
                    final int           m     = entry.getKey();
                    final List<Integer> listJ = entry.getValue();

                    for (int j : listJ) {
                        for (int i = 0; i < 6; ++i) {
                            storeIfSelected(coefficients, selected, slot.getCijm(j, m, date), "c", j, m);
                            storeIfSelected(coefficients, selected, slot.getSijm(j, m, date), "s", j, m);
                        }
                    }
                }

                return coefficients;

            } else {
                return Collections.emptyMap();
            }

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

    }

    /** The C<sup>i</sup><sub>m</sub><sub>t</sub> and S<sup>i</sup><sub>m</sub><sub>t</sub> coefficients used to compute
     * the short-periodic zonal contribution.
     *   <p>
     *  Those coefficients are given by expression 2.5.4-5 from the Danielson paper.
     *   </p>
     *
     * @author Sorin Scortan
     *
     */
    private static class FieldTesseralShortPeriodicCoefficients <T extends CalculusFieldElement<T>> implements FieldShortPeriodTerms<T> {

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

        /** Central body rotating frame. */
        private final Frame bodyFrame;

        /** Maximal order to consider for short periodics m-daily tesseral harmonics potential. */
        private final int maxOrderMdailyTesseralSP;

        /** Flag to take into account only M-dailies harmonic tesserals for short periodic perturbations.  */
        private final boolean mDailiesOnly;

        /** List of non resonant orders with j != 0. */
        private final SortedMap<Integer, List<Integer> > nonResOrders;

        /** Maximum value for m index. */
        private final int mMax;

        /** Maximum value for j index. */
        private final int jMax;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** All coefficients slots. */
        private final transient FieldTimeSpanMap<FieldSlot<T>, T> slots;

        /** Constructor.
         * @param bodyFrame central body rotating frame
         * @param maxOrderMdailyTesseralSP maximal order to consider for short periodics m-daily tesseral harmonics potential
         * @param mDailiesOnly flag to take into account only M-dailies harmonic tesserals for short periodic perturbations
         * @param nonResOrders lst of non resonant orders with j != 0
         * @param mMax maximum value for m index
         * @param jMax maximum value for j index
         * @param interpolationPoints number of points used in the interpolation process
         * @param slots all coefficients slots
         */
        FieldTesseralShortPeriodicCoefficients(final Frame bodyFrame, final int maxOrderMdailyTesseralSP,
                                               final boolean mDailiesOnly, final SortedMap<Integer, List<Integer> > nonResOrders,
                                               final int mMax, final int jMax, final int interpolationPoints,
                                               final FieldTimeSpanMap<FieldSlot<T>, T> slots) {
            this.bodyFrame                = bodyFrame;
            this.maxOrderMdailyTesseralSP = maxOrderMdailyTesseralSP;
            this.mDailiesOnly             = mDailiesOnly;
            this.nonResOrders             = nonResOrders;
            this.mMax                     = mMax;
            this.jMax                     = jMax;
            this.interpolationPoints      = interpolationPoints;
            this.slots                    = slots;
        }

        /** Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        @SuppressWarnings("unchecked")
        public FieldSlot<T> createSlot(final FieldSpacecraftState<T>... meanStates) {
            final FieldSlot<T>         slot  = new FieldSlot<>(mMax, jMax, interpolationPoints);
            final FieldAbsoluteDate<T> first = meanStates[0].getDate();
            final FieldAbsoluteDate<T> last  = meanStates[meanStates.length - 1].getDate();
            if (first.compareTo(last) <= 0) {
                slots.addValidAfter(slot, first);
            } else {
                slots.addValidBefore(slot, first);
            }
            return slot;
        }

        /** {@inheritDoc} */
        @Override
        public T[] value(final FieldOrbit<T> meanOrbit) {

            // select the coefficients slot
            final FieldSlot<T> slot = slots.get(meanOrbit.getDate());

            // Initialise the short periodic variations
            final T[] shortPeriodicVariation = MathArrays.buildArray(meanOrbit.getDate().getField(), 6);

            // Compute only if there is at least one non-resonant tesseral or
            // only the m-daily tesseral should be taken into account
            if (!nonResOrders.isEmpty() || mDailiesOnly) {

                //Build an auxiliary object
                final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(meanOrbit, I);

                // Central body rotation angle from equation 2.7.1-(3)(4).
                final FieldStaticTransform<T> t = bodyFrame.getStaticTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());
                final FieldVector3D<T> xB = t.transformVector(Vector3D.PLUS_I);
                final FieldVector3D<T> yB = t.transformVector(Vector3D.PLUS_J);
                final FieldVector3D<T>  f = auxiliaryElements.getVectorF();
                final FieldVector3D<T>  g = auxiliaryElements.getVectorG();
                final T currentTheta = FastMath.atan2(f.dotProduct(yB).negate().add(g.dotProduct(xB).multiply(I)),
                                                      f.dotProduct(xB).add(g.dotProduct(yB).multiply(I)));

                //Add the m-daily contribution
                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    // Phase angle
                    final T jlMmt              = currentTheta.multiply(-m);
                    final FieldSinCos<T> scPhi = FastMath.sinCos(jlMmt);
                    final T sinPhi             = scPhi.sin();
                    final T cosPhi             = scPhi.cos();

                    // compute contribution for each element
                    final T[] c = slot.getCijm(0, m, meanOrbit.getDate());
                    final T[] s = slot.getSijm(0, m, meanOrbit.getDate());
                    for (int i = 0; i < 6; i++) {
                        shortPeriodicVariation[i] = shortPeriodicVariation[i].add(c[i].multiply(cosPhi).add(s[i].multiply(sinPhi)));
                    }
                }

                // loop through all non-resonant (j,m) pairs
                for (final Map.Entry<Integer, List<Integer>> entry : nonResOrders.entrySet()) {
                    final int           m     = entry.getKey();
                    final List<Integer> listJ = entry.getValue();

                    for (int j : listJ) {
                        // Phase angle
                        final T jlMmt              = meanOrbit.getLM().multiply(j).subtract(currentTheta.multiply(m));
                        final FieldSinCos<T> scPhi = FastMath.sinCos(jlMmt);
                        final T sinPhi             = scPhi.sin();
                        final T cosPhi             = scPhi.cos();

                        // compute contribution for each element
                        final T[] c = slot.getCijm(j, m, meanOrbit.getDate());
                        final T[] s = slot.getSijm(j, m, meanOrbit.getDate());
                        for (int i = 0; i < 6; i++) {
                            shortPeriodicVariation[i] = shortPeriodicVariation[i].add(c[i].multiply(cosPhi).add(s[i].multiply(sinPhi)));
                        }

                    }
                }
            }

            return shortPeriodicVariation;

        }

        /** {@inheritDoc} */
        @Override
        public String getCoefficientsKeyPrefix() {
            return DSSTTesseral.SHORT_PERIOD_PREFIX;
        }

        /** {@inheritDoc}
         * <p>
         * For tesseral terms contributions,there are maxOrderMdailyTesseralSP
         * m-daily cMm coefficients, maxOrderMdailyTesseralSP m-daily sMm
         * coefficients, nbNonResonant cjm coefficients and nbNonResonant
         * sjm coefficients, where maxOrderMdailyTesseralSP and nbNonResonant both
         * depend on the orbit. The j index is the integer multiplier for the true
         * longitude argument and the m index is the integer multiplier for m-dailies.
         * </p>
         */
        @Override
        public Map<String, T[]> getCoefficients(final FieldAbsoluteDate<T> date, final Set<String> selected) {

            // select the coefficients slot
            final FieldSlot<T> slot = slots.get(date);

            if (!nonResOrders.isEmpty() || mDailiesOnly) {
                final Map<String, T[]> coefficients =
                                new HashMap<String, T[]>(12 * maxOrderMdailyTesseralSP +
                                                         12 * nonResOrders.size());

                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    storeIfSelected(coefficients, selected, slot.getCijm(0, m, date), DSSTTesseral.CM_COEFFICIENTS, m);
                    storeIfSelected(coefficients, selected, slot.getSijm(0, m, date), DSSTTesseral.SM_COEFFICIENTS, m);
                }

                for (final Map.Entry<Integer, List<Integer>> entry : nonResOrders.entrySet()) {
                    final int           m     = entry.getKey();
                    final List<Integer> listJ = entry.getValue();

                    for (int j : listJ) {
                        for (int i = 0; i < 6; ++i) {
                            storeIfSelected(coefficients, selected, slot.getCijm(j, m, date), "c", j, m);
                            storeIfSelected(coefficients, selected, slot.getSijm(j, m, date), "s", j, m);
                        }
                    }
                }

                return coefficients;

            } else {
                return Collections.emptyMap();
            }

        }

        /** Put a coefficient in a map if selected.
         * @param map map to populate
         * @param selected set of coefficients that should be put in the map
         * (empty set means all coefficients are selected)
         * @param value coefficient value
         * @param id coefficient identifier
         * @param indices list of coefficient indices
         */
        private void storeIfSelected(final Map<String, T[]> map, final Set<String> selected,
                                     final T[] value, final String id, final int... indices) {
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
    }

    /** Coefficients valid for one time slot. */
    private static class Slot {

        /** The coefficients C<sub>i</sub><sup>j</sup><sup>m</sup>.
         * <p>
         * The index order is cijm[m][j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[][] cijm;

        /** The coefficients S<sub>i</sub><sup>j</sup><sup>m</sup>.
         * <p>
         * The index order is sijm[m][j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[][] sijm;

        /** Simple constructor.
         *  @param mMax maximum value for m index
         *  @param jMax maximum value for j index
         *  @param interpolationPoints number of points used in the interpolation process
         */
        Slot(final int mMax, final int jMax, final int interpolationPoints) {

            final int rows    = mMax + 1;
            final int columns = 2 * jMax + 1;
            cijm = new ShortPeriodicsInterpolatedCoefficient[rows][columns];
            sijm = new ShortPeriodicsInterpolatedCoefficient[rows][columns];
            for (int m = 1; m <= mMax; m++) {
                for (int j = -jMax; j <= jMax; j++) {
                    cijm[m][j + jMax] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                    sijm[m][j + jMax] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                }
            }

        }

        /** Get C<sub>i</sub><sup>j</sup><sup>m</sup>.
         *
         * @param j j index
         * @param m m index
         * @param date the date
         * @return C<sub>i</sub><sup>j</sup><sup>m</sup>
         */
        double[] getCijm(final int j, final int m, final AbsoluteDate date) {
            final int jMax = (cijm[m].length - 1) / 2;
            return cijm[m][j + jMax].value(date);
        }

        /** Get S<sub>i</sub><sup>j</sup><sup>m</sup>.
         *
         * @param j j index
         * @param m m index
         * @param date the date
         * @return S<sub>i</sub><sup>j</sup><sup>m</sup>
         */
        double[] getSijm(final int j, final int m, final AbsoluteDate date) {
            final int jMax = (cijm[m].length - 1) / 2;
            return sijm[m][j + jMax].value(date);
        }

    }

    /** Coefficients valid for one time slot. */
    private static class FieldSlot <T extends CalculusFieldElement<T>> {

        /** The coefficients C<sub>i</sub><sup>j</sup><sup>m</sup>.
         * <p>
         * The index order is cijm[m][j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final FieldShortPeriodicsInterpolatedCoefficient<T>[][] cijm;

        /** The coefficients S<sub>i</sub><sup>j</sup><sup>m</sup>.
         * <p>
         * The index order is sijm[m][j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final FieldShortPeriodicsInterpolatedCoefficient<T>[][] sijm;

        /** Simple constructor.
         *  @param mMax maximum value for m index
         *  @param jMax maximum value for j index
         *  @param interpolationPoints number of points used in the interpolation process
         */
        @SuppressWarnings("unchecked")
        FieldSlot(final int mMax, final int jMax, final int interpolationPoints) {

            final int rows    = mMax + 1;
            final int columns = 2 * jMax + 1;
            cijm = (FieldShortPeriodicsInterpolatedCoefficient<T>[][]) Array.newInstance(FieldShortPeriodicsInterpolatedCoefficient.class, rows, columns);
            sijm = (FieldShortPeriodicsInterpolatedCoefficient<T>[][]) Array.newInstance(FieldShortPeriodicsInterpolatedCoefficient.class, rows, columns);
            for (int m = 1; m <= mMax; m++) {
                for (int j = -jMax; j <= jMax; j++) {
                    cijm[m][j + jMax] = new FieldShortPeriodicsInterpolatedCoefficient<>(interpolationPoints);
                    sijm[m][j + jMax] = new FieldShortPeriodicsInterpolatedCoefficient<>(interpolationPoints);
                }
            }

        }

        /** Get C<sub>i</sub><sup>j</sup><sup>m</sup>.
         *
         * @param j j index
         * @param m m index
         * @param date the date
         * @return C<sub>i</sub><sup>j</sup><sup>m</sup>
         */
        T[] getCijm(final int j, final int m, final FieldAbsoluteDate<T> date) {
            final int jMax = (cijm[m].length - 1) / 2;
            return cijm[m][j + jMax].value(date);
        }

        /** Get S<sub>i</sub><sup>j</sup><sup>m</sup>.
         *
         * @param j j index
         * @param m m index
         * @param date the date
         * @return S<sub>i</sub><sup>j</sup><sup>m</sup>
         */
        T[] getSijm(final int j, final int m, final FieldAbsoluteDate<T> date) {
            final int jMax = (cijm[m].length - 1) / 2;
            return sijm[m][j + jMax].value(date);
        }

    }

    /** Compute potential and potential derivatives with respect to orbital parameters.
     *  <p>The following elements are computed from expression 3.3 - (4).
     *  <pre>
     *  dU / da
     *  dU / dh
     *  dU / dk
     *  dU / dλ
     *  dU / dα
     *  dU / dβ
     *  dU / dγ
     *  </pre>
     *  </p>
     */
    private class UAnddU {

        /** dU / da. */
        private  double dUda;

        /** dU / dk. */
        private double dUdk;

        /** dU / dh. */
        private double dUdh;

        /** dU / dl. */
        private double dUdl;

        /** dU / dAlpha. */
        private double dUdAl;

        /** dU / dBeta. */
        private double dUdBe;

        /** dU / dGamma. */
        private double dUdGa;

        /** Simple constuctor.
         * @param date current date
         * @param context container for attributes
         * @param hansen hansen objects
         */
        UAnddU(final AbsoluteDate date, final DSSTTesseralContext context, final HansenObjects hansen) {

            // Auxiliary elements related to the current orbit
            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            // Potential derivatives
            dUda  = 0.;
            dUdh  = 0.;
            dUdk  = 0.;
            dUdl  = 0.;
            dUdAl = 0.;
            dUdBe = 0.;
            dUdGa = 0.;

            // Compute only if there is at least one resonant tesseral
            if (!resOrders.isEmpty()) {
                // Gmsj and Hmsj polynomials
                final GHmsjPolynomials ghMSJ = new GHmsjPolynomials(auxiliaryElements.getK(), auxiliaryElements.getH(), auxiliaryElements.getAlpha(), auxiliaryElements.getBeta(), I);

                // GAMMAmns function
                final GammaMnsFunction gammaMNS = new GammaMnsFunction(maxDegree, auxiliaryElements.getGamma(), I);

                // R / a up to power degree
                final double[] roaPow = new double[maxDegree + 1];
                roaPow[0] = 1.;
                for (int i = 1; i <= maxDegree; i++) {
                    roaPow[i] = context.getRoa() * roaPow[i - 1];
                }

                // SUM over resonant terms {j,m}
                for (int m : resOrders) {

                    // Resonant index for the current resonant order
                    final int j = FastMath.max(1, (int) FastMath.round(context.getRatio() * m));

                    // Phase angle
                    final double jlMmt  = j * auxiliaryElements.getLM() - m * context.getTheta();
                    final SinCos scPhi  = FastMath.sinCos(jlMmt);
                    final double sinPhi = scPhi.sin();
                    final double cosPhi = scPhi.cos();

                    // Potential derivatives components for a given resonant pair {j,m}
                    double dUdaCos  = 0.;
                    double dUdaSin  = 0.;
                    double dUdhCos  = 0.;
                    double dUdhSin  = 0.;
                    double dUdkCos  = 0.;
                    double dUdkSin  = 0.;
                    double dUdlCos  = 0.;
                    double dUdlSin  = 0.;
                    double dUdAlCos = 0.;
                    double dUdAlSin = 0.;
                    double dUdBeCos = 0.;
                    double dUdBeSin = 0.;
                    double dUdGaCos = 0.;
                    double dUdGaSin = 0.;

                    // s-SUM from -sMin to sMax
                    final int sMin = FastMath.min(maxEccPow - j, maxDegree);
                    final int sMax = FastMath.min(maxEccPow + j, maxDegree);
                    for (int s = 0; s <= sMax; s++) {

                        //Compute the initial values for Hansen coefficients using newComb operators
                        hansen.computeHansenObjectsInitValues(context, s + maxDegree, j);

                        // n-SUM for s positive
                        final double[][] nSumSpos = computeNSum(date, j, m, s, maxDegree,
                                                                roaPow, ghMSJ, gammaMNS, context, hansen);
                        dUdaCos  += nSumSpos[0][0];
                        dUdaSin  += nSumSpos[0][1];
                        dUdhCos  += nSumSpos[1][0];
                        dUdhSin  += nSumSpos[1][1];
                        dUdkCos  += nSumSpos[2][0];
                        dUdkSin  += nSumSpos[2][1];
                        dUdlCos  += nSumSpos[3][0];
                        dUdlSin  += nSumSpos[3][1];
                        dUdAlCos += nSumSpos[4][0];
                        dUdAlSin += nSumSpos[4][1];
                        dUdBeCos += nSumSpos[5][0];
                        dUdBeSin += nSumSpos[5][1];
                        dUdGaCos += nSumSpos[6][0];
                        dUdGaSin += nSumSpos[6][1];

                        // n-SUM for s negative
                        if (s > 0 && s <= sMin) {
                            //Compute the initial values for Hansen coefficients using newComb operators
                            hansen.computeHansenObjectsInitValues(context, maxDegree - s, j);

                            final double[][] nSumSneg = computeNSum(date, j, m, -s, maxDegree,
                                                                    roaPow, ghMSJ, gammaMNS, context, hansen);
                            dUdaCos  += nSumSneg[0][0];
                            dUdaSin  += nSumSneg[0][1];
                            dUdhCos  += nSumSneg[1][0];
                            dUdhSin  += nSumSneg[1][1];
                            dUdkCos  += nSumSneg[2][0];
                            dUdkSin  += nSumSneg[2][1];
                            dUdlCos  += nSumSneg[3][0];
                            dUdlSin  += nSumSneg[3][1];
                            dUdAlCos += nSumSneg[4][0];
                            dUdAlSin += nSumSneg[4][1];
                            dUdBeCos += nSumSneg[5][0];
                            dUdBeSin += nSumSneg[5][1];
                            dUdGaCos += nSumSneg[6][0];
                            dUdGaSin += nSumSneg[6][1];
                        }
                    }

                    // Assembly of potential derivatives componants
                    dUda  += cosPhi * dUdaCos  + sinPhi * dUdaSin;
                    dUdh  += cosPhi * dUdhCos  + sinPhi * dUdhSin;
                    dUdk  += cosPhi * dUdkCos  + sinPhi * dUdkSin;
                    dUdl  += cosPhi * dUdlCos  + sinPhi * dUdlSin;
                    dUdAl += cosPhi * dUdAlCos + sinPhi * dUdAlSin;
                    dUdBe += cosPhi * dUdBeCos + sinPhi * dUdBeSin;
                    dUdGa += cosPhi * dUdGaCos + sinPhi * dUdGaSin;
                }

                this.dUda  = dUda * (-context.getMoa() / auxiliaryElements.getSma());
                this.dUdh  = dUdh * context.getMoa();
                this.dUdk  = dUdk * context.getMoa();
                this.dUdl  = dUdl * context.getMoa();
                this.dUdAl = dUdAl * context.getMoa();
                this.dUdBe = dUdBe * context.getMoa();
                this.dUdGa = dUdGa * context.getMoa();
            }

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

        /** Return value of dU / dl.
         * @return dUdl
         */
        public double getdUdl() {
            return dUdl;
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

    /**  Computes the potential U derivatives.
     *  <p>The following elements are computed from expression 3.3 - (4).
     *  <pre>
     *  dU / da
     *  dU / dh
     *  dU / dk
     *  dU / dλ
     *  dU / dα
     *  dU / dβ
     *  dU / dγ
     *  </pre>
     *  </p>
     */
    private class FieldUAnddU <T extends CalculusFieldElement<T>> {

        /** dU / da. */
        private T dUda;

        /** dU / dk. */
        private T dUdk;

        /** dU / dh. */
        private T dUdh;

        /** dU / dl. */
        private T dUdl;

        /** dU / dAlpha. */
        private T dUdAl;

        /** dU / dBeta. */
        private T dUdBe;

        /** dU / dGamma. */
        private T dUdGa;

        /** Simple constuctor.
         * @param date current date
         * @param context container for attributes
         * @param hansen hansen objects
         */
        FieldUAnddU(final FieldAbsoluteDate<T> date, final FieldDSSTTesseralContext<T> context,
                    final FieldHansenObjects<T> hansen) {

            // Auxiliary elements related to the current orbit
            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            // Zero for initialization
            final Field<T> field = date.getField();
            final T zero = field.getZero();

            // Potential derivatives
            dUda  = zero;
            dUdh  = zero;
            dUdk  = zero;
            dUdl  = zero;
            dUdAl = zero;
            dUdBe = zero;
            dUdGa = zero;

            // Compute only if there is at least one resonant tesseral
            if (!resOrders.isEmpty()) {
                // Gmsj and Hmsj polynomials
                final FieldGHmsjPolynomials<T> ghMSJ = new FieldGHmsjPolynomials<>(auxiliaryElements.getK(), auxiliaryElements.getH(), auxiliaryElements.getAlpha(), auxiliaryElements.getBeta(), I, field);

                // GAMMAmns function
                final FieldGammaMnsFunction<T> gammaMNS = new FieldGammaMnsFunction<>(maxDegree, auxiliaryElements.getGamma(), I, field);

                // R / a up to power degree
                final T[] roaPow = MathArrays.buildArray(field, maxDegree + 1);
                roaPow[0] = zero.add(1.);
                for (int i = 1; i <= maxDegree; i++) {
                    roaPow[i] = roaPow[i - 1].multiply(context.getRoa());
                }

                // SUM over resonant terms {j,m}
                for (int m : resOrders) {

                    // Resonant index for the current resonant order
                    final int j = FastMath.max(1, (int) FastMath.round(context.getRatio().multiply(m)));

                    // Phase angle
                    final T jlMmt              = auxiliaryElements.getLM().multiply(j).subtract(context.getTheta().multiply(m));
                    final FieldSinCos<T> scPhi = FastMath.sinCos(jlMmt);
                    final T sinPhi             = scPhi.sin();
                    final T cosPhi             = scPhi.cos();

                    // Potential derivatives components for a given resonant pair {j,m}
                    T dUdaCos  = zero;
                    T dUdaSin  = zero;
                    T dUdhCos  = zero;
                    T dUdhSin  = zero;
                    T dUdkCos  = zero;
                    T dUdkSin  = zero;
                    T dUdlCos  = zero;
                    T dUdlSin  = zero;
                    T dUdAlCos = zero;
                    T dUdAlSin = zero;
                    T dUdBeCos = zero;
                    T dUdBeSin = zero;
                    T dUdGaCos = zero;
                    T dUdGaSin = zero;

                    // s-SUM from -sMin to sMax
                    final int sMin = FastMath.min(maxEccPow - j, maxDegree);
                    final int sMax = FastMath.min(maxEccPow + j, maxDegree);
                    for (int s = 0; s <= sMax; s++) {

                        //Compute the initial values for Hansen coefficients using newComb operators
                        hansen.computeHansenObjectsInitValues(context, s + maxDegree, j);

                        // n-SUM for s positive
                        final T[][] nSumSpos = computeNSum(date, j, m, s, maxDegree,
                                                                roaPow, ghMSJ, gammaMNS, context, hansen);
                        dUdaCos  = dUdaCos.add(nSumSpos[0][0]);
                        dUdaSin  = dUdaSin.add(nSumSpos[0][1]);
                        dUdhCos  = dUdhCos.add(nSumSpos[1][0]);
                        dUdhSin  = dUdhSin.add(nSumSpos[1][1]);
                        dUdkCos  = dUdkCos.add(nSumSpos[2][0]);
                        dUdkSin  = dUdkSin.add(nSumSpos[2][1]);
                        dUdlCos  = dUdlCos.add(nSumSpos[3][0]);
                        dUdlSin  = dUdlSin.add(nSumSpos[3][1]);
                        dUdAlCos = dUdAlCos.add(nSumSpos[4][0]);
                        dUdAlSin = dUdAlSin.add(nSumSpos[4][1]);
                        dUdBeCos = dUdBeCos.add(nSumSpos[5][0]);
                        dUdBeSin = dUdBeSin.add(nSumSpos[5][1]);
                        dUdGaCos = dUdGaCos.add(nSumSpos[6][0]);
                        dUdGaSin = dUdGaSin.add(nSumSpos[6][1]);

                        // n-SUM for s negative
                        if (s > 0 && s <= sMin) {
                            //Compute the initial values for Hansen coefficients using newComb operators
                            hansen.computeHansenObjectsInitValues(context, maxDegree - s, j);

                            final T[][] nSumSneg = computeNSum(date, j, m, -s, maxDegree,
                                                                    roaPow, ghMSJ, gammaMNS, context, hansen);
                            dUdaCos  = dUdaCos.add(nSumSneg[0][0]);
                            dUdaSin  = dUdaSin.add(nSumSneg[0][1]);
                            dUdhCos  = dUdhCos.add(nSumSneg[1][0]);
                            dUdhSin  = dUdhSin.add(nSumSneg[1][1]);
                            dUdkCos  = dUdkCos.add(nSumSneg[2][0]);
                            dUdkSin  = dUdkSin.add(nSumSneg[2][1]);
                            dUdlCos  = dUdlCos.add(nSumSneg[3][0]);
                            dUdlSin  = dUdlSin.add(nSumSneg[3][1]);
                            dUdAlCos = dUdAlCos.add(nSumSneg[4][0]);
                            dUdAlSin = dUdAlSin.add(nSumSneg[4][1]);
                            dUdBeCos = dUdBeCos.add(nSumSneg[5][0]);
                            dUdBeSin = dUdBeSin.add(nSumSneg[5][1]);
                            dUdGaCos = dUdGaCos.add(nSumSneg[6][0]);
                            dUdGaSin = dUdGaSin.add(nSumSneg[6][1]);
                        }
                    }

                    // Assembly of potential derivatives componants
                    dUda  = dUda.add(dUdaCos.multiply(cosPhi).add(dUdaSin.multiply(sinPhi)));
                    dUdh  = dUdh.add(dUdhCos.multiply(cosPhi).add(dUdhSin.multiply(sinPhi)));
                    dUdk  = dUdk.add(dUdkCos.multiply(cosPhi).add(dUdkSin.multiply(sinPhi)));
                    dUdl  = dUdl.add(dUdlCos.multiply(cosPhi).add(dUdlSin.multiply(sinPhi)));
                    dUdAl = dUdAl.add(dUdAlCos.multiply(cosPhi).add(dUdAlSin.multiply(sinPhi)));
                    dUdBe = dUdBe.add(dUdBeCos.multiply(cosPhi).add(dUdBeSin.multiply(sinPhi)));
                    dUdGa = dUdGa.add(dUdGaCos.multiply(cosPhi).add(dUdGaSin.multiply(sinPhi)));
                }

                dUda  =  dUda.multiply(context.getMoa().divide(auxiliaryElements.getSma())).negate();
                dUdh  =  dUdh.multiply(context.getMoa());
                dUdk  =  dUdk.multiply(context.getMoa());
                dUdl  =  dUdl.multiply(context.getMoa());
                dUdAl =  dUdAl.multiply(context.getMoa());
                dUdBe =  dUdBe.multiply(context.getMoa());
                dUdGa =  dUdGa.multiply(context.getMoa());

            }

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

        /** Return value of dU / dl.
         * @return dUdl
         */
        public T getdUdl() {
            return dUdl;
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

        /** A two dimensional array that contains the objects needed to build the Hansen coefficients. <br/>
         * The indexes are s + maxDegree and j */
        private HansenTesseralLinear[][] hansenObjects;

        /** Simple constructor.
         * @param ratio Ratio of satellite period to central body rotation period
         * @param type type of the elements used during the propagation
         */
        HansenObjects(final double ratio,
                      final PropagationType type) {

            //Allocate the two dimensional array
            final int rows     = 2 * maxDegree + 1;
            final int columns  = maxFrequencyShortPeriodics + 1;
            this.hansenObjects = new HansenTesseralLinear[rows][columns];

            switch (type) {
                case MEAN:
                    // loop through the resonant orders
                    for (int m : resOrders) {
                        //Compute the corresponding j term
                        final int j = FastMath.max(1, (int) FastMath.round(ratio * m));

                        //Compute the sMin and sMax values
                        final int sMin = FastMath.min(maxEccPow - j, maxDegree);
                        final int sMax = FastMath.min(maxEccPow + j, maxDegree);

                        //loop through the s values
                        for (int s = 0; s <= sMax; s++) {
                            //Compute the n0 value
                            final int n0 = FastMath.max(FastMath.max(2, m), s);

                            //Create the object for the pair j, s
                            this.hansenObjects[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, maxHansen);

                            if (s > 0 && s <= sMin) {
                                //Also create the object for the pair j, -s
                                this.hansenObjects[maxDegree - s][j] =  new HansenTesseralLinear(maxDegree, -s, j, n0, maxHansen);
                            }
                        }
                    }
                    break;

                case OSCULATING:
                    // create all objects
                    for (int j = 0; j <= maxFrequencyShortPeriodics; j++) {
                        for (int s = -maxDegree; s <= maxDegree; s++) {
                            //Compute the n0 value
                            final int n0 = FastMath.max(2, FastMath.abs(s));
                            this.hansenObjects[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, maxHansen);
                        }
                    }
                    break;

                default:
                    throw new OrekitInternalError(null);
            }

        }

        /** Compute init values for hansen objects.
         * @param context container for attributes
         * @param rows number of rows of the hansen matrix
         * @param columns columns number of columns of the hansen matrix
         */
        public void computeHansenObjectsInitValues(final DSSTTesseralContext context, final int rows, final int columns) {
            hansenObjects[rows][columns].computeInitValues(context.getE2(), context.getChi(), context.getChi2());
        }

        /** Get hansen object.
         * @return hansenObjects
         */
        public HansenTesseralLinear[][] getHansenObjects() {
            return hansenObjects;
        }

    }

    /** Computes init values of the Hansen Objects. */
    private class FieldHansenObjects<T extends CalculusFieldElement<T>> {

        /** A two dimensional array that contains the objects needed to build the Hansen coefficients. <br/>
         * The indexes are s + maxDegree and j */
        private FieldHansenTesseralLinear<T>[][] hansenObjects;

        /** Simple constructor.
         * @param ratio Ratio of satellite period to central body rotation period
         * @param type type of the elements used during the propagation
         */
        @SuppressWarnings("unchecked")
        FieldHansenObjects(final T ratio,
                           final PropagationType type) {

            // Set the maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
            maxHansen = maxEccPow / 2;

            //Allocate the two dimensional array
            final int rows     = 2 * maxDegree + 1;
            final int columns  = maxFrequencyShortPeriodics + 1;
            this.hansenObjects = (FieldHansenTesseralLinear<T>[][]) Array.newInstance(FieldHansenTesseralLinear.class, rows, columns);

            switch (type) {
                case MEAN:
                 // loop through the resonant orders
                    for (int m : resOrders) {
                        //Compute the corresponding j term
                        final int j = FastMath.max(1, (int) FastMath.round(ratio.multiply(m)));

                        //Compute the sMin and sMax values
                        final int sMin = FastMath.min(maxEccPow - j, maxDegree);
                        final int sMax = FastMath.min(maxEccPow + j, maxDegree);

                        //loop through the s values
                        for (int s = 0; s <= sMax; s++) {
                            //Compute the n0 value
                            final int n0 = FastMath.max(FastMath.max(2, m), s);

                            //Create the object for the pair j, s
                            this.hansenObjects[s + maxDegree][j] = new FieldHansenTesseralLinear<>(maxDegree, s, j, n0, maxHansen, ratio.getField());

                            if (s > 0 && s <= sMin) {
                                //Also create the object for the pair j, -s
                                this.hansenObjects[maxDegree - s][j] =  new FieldHansenTesseralLinear<>(maxDegree, -s, j, n0, maxHansen, ratio.getField());
                            }
                        }
                    }
                    break;

                case OSCULATING:
                    // create all objects
                    for (int j = 0; j <= maxFrequencyShortPeriodics; j++) {
                        for (int s = -maxDegree; s <= maxDegree; s++) {
                            //Compute the n0 value
                            final int n0 = FastMath.max(2, FastMath.abs(s));
                            this.hansenObjects[s + maxDegree][j] = new FieldHansenTesseralLinear<>(maxDegree, s, j, n0, maxHansen, ratio.getField());
                        }
                    }
                    break;

                default:
                    throw new OrekitInternalError(null);
            }

        }

        /** Compute init values for hansen objects.
         * @param context container for attributes
         * @param rows number of rows of the hansen matrix
         * @param columns columns number of columns of the hansen matrix
         */
        public void computeHansenObjectsInitValues(final FieldDSSTTesseralContext<T> context,
                                                   final int rows, final int columns) {
            hansenObjects[rows][columns].computeInitValues(context.getE2(), context.getChi(), context.getChi2());
        }

        /** Get hansen object.
         * @return hansenObjects
         */
        public FieldHansenTesseralLinear<T>[][] getHansenObjects() {
            return hansenObjects;
        }

    }

}
