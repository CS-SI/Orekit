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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldCjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.JacobiPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.ShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.FieldHansenThirdBodyLinear;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenThirdBodyLinear;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTimeSpanMap;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/** Third body attraction perturbation to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *
 *  @author Romain Di Costanzo
 *  @author Pascal Parraud
 *  @author Bryan Cazabonne (field translation)
 */
public class DSSTThirdBody implements DSSTForceModel {
    /**  Name of the prefix for short period coefficients keys. */
    public static final String SHORT_PERIOD_PREFIX = "DSST-3rd-body-";

    /** Name of the single parameter of this model: the attraction coefficient. */
    public static final String ATTRACTION_COEFFICIENT = " attraction coefficient";

    /** Max power for summation. */
    public static final int MAX_POWER = 22;

    /** Truncation tolerance for big, eccentric orbits. */
    public static final double BIG_TRUNCATION_TOLERANCE = 1.e-1;

    /** Truncation tolerance for small orbits. */
    public static final double SMALL_TRUNCATION_TOLERANCE = 1.9e-6;

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

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
    private static final int    I = 1;

    /** Number of points for interpolation. */
    private static final int    INTERPOLATION_POINTS = 3;

    /** Maximum power for eccentricity used in short periodic computation. */
    private static final int    MAX_ECCPOWER_SP = 4;

    /** V<sub>ns</sub> coefficients. */
    private final SortedMap<NSKey, Double> Vns;

    /** Force model static context. Initialized with short period terms. */
    private DSSTThirdBodyStaticContext staticContext;

    /** If false, the static context needs to be initialized. */
    private boolean doesStaticContextNeedsInitialization;

    /** The 3rd body to consider. */
    private final CelestialBody    body;

    /** Short period terms. */
    private ThirdBodyShortPeriodicCoefficients shortPeriods;

    /** "Field" Short period terms. */
    private Map<Field<?>, FieldThirdBodyShortPeriodicCoefficients<?>> fieldShortPeriods;

    /** Drivers for third body attraction coefficient and gravitational parameter. */
    private final List<ParameterDriver> parameterDrivers;

    /** Hansen objects. */
    private HansenObjects hansen;

    /** Hansen objects for field elements. */
    private Map<Field<?>, FieldHansenObjects<?>> fieldHansen;

    /** Complete constructor.
     *  @param body the 3rd body to consider
     *  @param mu central attraction coefficient
     *            (<b>i.e., attraction coefficient of the central body, not the one of the 3rd body</b>)
     *  @see CelestialBodies
     */
    public DSSTThirdBody(final CelestialBody body, final double mu) {
        parameterDrivers = new ArrayList<>(2);
        parameterDrivers.add(new ParameterDriver(body.getName() + DSSTThirdBody.ATTRACTION_COEFFICIENT,
                                                 body.getGM(), MU_SCALE,
                                                 0.0, Double.POSITIVE_INFINITY));
        parameterDrivers.add(new ParameterDriver(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                 mu, MU_SCALE,
                                                 0.0, Double.POSITIVE_INFINITY));

        this.body = body;
        this.Vns  = CoefficientsFactory.computeVns(MAX_POWER);
        this.doesStaticContextNeedsInitialization = true;

        fieldShortPeriods  = new HashMap<>();
        fieldHansen        = new HashMap<>();
    }

    /** Get third body.
     *  @return third body
     */
    public CelestialBody getBody() {
        return body;
    }

    /** Initializes the static 3rd body context if needed.
     * @param auxiliaryElements auxiliary elements
     * @param x DSST Chi element
     * @param r3 distance from center of mass of the central body to the 3rd body
     * @param parameters force model parameters
     */
    private void initializeStaticContextIfNeeded(final AuxiliaryElements auxiliaryElements,
                                                 final double x, final double r3,
                                                 final double[] parameters) {
        if (doesStaticContextNeedsInitialization) {
            staticContext = new DSSTThirdBodyStaticContext(auxiliaryElements, x, r3, parameters);
            doesStaticContextNeedsInitialization = false;
        }
    }

    /** Computes the highest power of the eccentricity and the highest power
     *  of a/R3 to appear in the truncated analytical power series expansion.
     *  <p>
     *  This method computes the upper value for the 3rd body potential and
     *  determines the maximal powers for the eccentricity and a/R3 producing
     *  potential terms bigger than a defined tolerance.
     *  </p>
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param type type of the elements used during the propagation
     *  @param parameters values of the force model parameters for state date (1 value for each parameters)
     */
    @Override
    public List<ShortPeriodTerms> initializeShortPeriodTerms(final AuxiliaryElements auxiliaryElements,
                                                             final PropagationType type,
                                                             final double[] parameters) {

        // Initializes specific parameters.
        final DSSTThirdBodyDynamicContext context = initializeStep(auxiliaryElements, parameters);

        // Static context
        initializeStaticContextIfNeeded(auxiliaryElements, context.getX(), context.getR3(), parameters);

        // Hansen objects
        hansen = new HansenObjects();

        // Initialize short period terms
        final int jMax = staticContext.getMaxAR3Pow() + 1;
        shortPeriods = new ThirdBodyShortPeriodicCoefficients(jMax, INTERPOLATION_POINTS,
                                                              staticContext.getMaxFreqF(), body.getName(),
                                                              new TimeSpanMap<Slot>(new Slot(jMax, INTERPOLATION_POINTS)));

        final List<ShortPeriodTerms> list = new ArrayList<ShortPeriodTerms>();
        list.add(shortPeriods);
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
        final FieldDSSTThirdBodyDynamicContext<T> context = initializeStep(auxiliaryElements, parameters);

        // Static context (only provide integers. So, derivatives are not taken into account and getReal() method is accepted)
        initializeStaticContextIfNeeded(auxiliaryElements.toAuxiliaryElements(), context.getX().getReal(), context.getR3().getReal(), getParameters());

        // Hansen object
        fieldHansen.put(field, new FieldHansenObjects<>(field));

        // Initialize short period terms
        final int jMax = staticContext.getMaxAR3Pow() + 1;
        final FieldThirdBodyShortPeriodicCoefficients<T> ftbspc =
                        new FieldThirdBodyShortPeriodicCoefficients<>(jMax, INTERPOLATION_POINTS,
                                        staticContext.getMaxFreqF(), body.getName(),
                                                                      new FieldTimeSpanMap<>(new FieldSlot<>(jMax,
                                                                                                             INTERPOLATION_POINTS),
                                                                                             field));
        fieldShortPeriods.put(field, ftbspc);
        return Collections.singletonList(ftbspc);
    }

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters  (only 1 values for each parameters corresponding
     * to state date) obtained by calling the extract parameter method {@link #extractParameters(double[], AbsoluteDate)}
     * to selected the right value for state date or by getting the parameters for a specific date
     * {@link #getParameters(AbsoluteDate)}.
     *  @return new force model context
     */
    private DSSTThirdBodyDynamicContext initializeStep(final AuxiliaryElements auxiliaryElements, final double[] parameters) {
        return new DSSTThirdBodyDynamicContext(auxiliaryElements, body, parameters);
    }

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param <T> type of the elements
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters at state date (1 value per parameter driver)
     *  @return new force model context
     */
    private <T extends CalculusFieldElement<T>> FieldDSSTThirdBodyDynamicContext<T> initializeStep(final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                        final T[] parameters) {
        return new FieldDSSTThirdBodyDynamicContext<>(auxiliaryElements, body, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getMeanElementRate(final SpacecraftState currentState,
                                       final AuxiliaryElements auxiliaryElements, final double[] parameters) {


        // Container for attributes
        final DSSTThirdBodyDynamicContext context = initializeStep(auxiliaryElements, parameters);

        // a / R3 up to power maxAR3Pow
        final double[] aoR3Pow = computeAoR3Pow(context);

        // Qns coefficients
        final double[][] Qns = CoefficientsFactory.computeQns(context.getGamma(), staticContext.getMaxAR3Pow(), FastMath.max(staticContext.getMaxEccPow(), MAX_ECCPOWER_SP));

        // Access to potential U derivatives
        final UAnddU udu = new UAnddU(context, hansen, aoR3Pow, Qns);

        // Compute cross derivatives [Eq. 2.2-(8)]
        // U(alpha,gamma) = alpha * dU/dgamma - gamma * dU/dalpha
        final double UAlphaGamma   = context.getAlpha() * udu.getdUdGa() - context.getGamma() * udu.getdUdAl();
        // U(beta,gamma) = beta * dU/dgamma - gamma * dU/dbeta
        final double UBetaGamma    =  context.getBeta() * udu.getdUdGa() - context.getGamma() * udu.getdUdBe();
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

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getMeanElementRate(final FieldSpacecraftState<T> currentState,
                                                                  final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                  final T[] parameters) {

        // Parameters for array building
        final Field<T> field = currentState.getDate().getField();
        final T        zero  = field.getZero();

        // Container for attributes
        final FieldDSSTThirdBodyDynamicContext<T> context = initializeStep(auxiliaryElements, parameters);

        // a / R3 up to power maxAR3Pow
        final T[] aoR3Pow = computeAoR3Pow(context);

        // Qns coefficients
        final T[][] Qns = CoefficientsFactory.computeQns(context.getGamma(), staticContext.getMaxAR3Pow(), FastMath.max(staticContext.getMaxEccPow(), MAX_ECCPOWER_SP));

        // Hansen objects
        @SuppressWarnings("unchecked")
        final FieldHansenObjects<T> fho = (FieldHansenObjects<T>) fieldHansen.get(field);

        // Access to potential U derivatives
        final FieldUAnddU<T> udu = new FieldUAnddU<>(context, fho, aoR3Pow, Qns);

        // Compute cross derivatives [Eq. 2.2-(8)]
        // U(alpha,gamma) = alpha * dU/dgamma - gamma * dU/dalpha
        final T UAlphaGamma   = udu.getdUdGa().multiply(context.getAlpha()).subtract(udu.getdUdAl().multiply(context.getGamma()));
        // U(beta,gamma) = beta * dU/dgamma - gamma * dU/dbeta
        final T UBetaGamma    = udu.getdUdGa().multiply(context.getBeta()).subtract(udu.getdUdBe().multiply(context.getGamma()));
        // Common factor
        final T pUAGmIqUBGoAB = (UAlphaGamma.multiply(auxiliaryElements.getP()).subtract(UBetaGamma.multiply(auxiliaryElements.getQ()).multiply(I))).multiply(context.getOoAB());

        // Compute mean elements rates [Eq. 3.1-(1)]
        final T da =  zero;
        final T dh =  udu.getdUdk().multiply(context.getBoA()).add(pUAGmIqUBGoAB.multiply(auxiliaryElements.getK()));
        final T dk =  ((udu.getdUdh().multiply(context.getBoA())).negate()).subtract(pUAGmIqUBGoAB.multiply(auxiliaryElements.getH()));
        final T dp =  UBetaGamma.multiply(context.getMCo2AB());
        final T dq =  UAlphaGamma.multiply(I).multiply(context.getMCo2AB());
        final T dM =  pUAGmIqUBGoAB.add(udu.getdUda().multiply(context.getM2aoA())).add((udu.getdUdh().multiply(auxiliaryElements.getH()).add(udu.getdUdk().multiply(auxiliaryElements.getK()))).multiply(context.getBoABpo()));

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

        final Slot slot = shortPeriods.createSlot(meanStates);

        for (final SpacecraftState meanState : meanStates) {

            // Auxiliary elements related to the current orbit
            final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(meanState.getOrbit(), I);

            // Extract the proper parameters valid for the corresponding meanState date from the input array
            final double[] extractedParameters = this.extractParameters(parameters, auxiliaryElements.getDate());

            // Container of attributes
            final DSSTThirdBodyDynamicContext context = initializeStep(auxiliaryElements, extractedParameters);

            // a / R3 up to power maxAR3Pow
            final double[] aoR3Pow = computeAoR3Pow(context);

            // Qns coefficients
            final double[][] Qns = CoefficientsFactory.computeQns(context.getGamma(), staticContext.getMaxAR3Pow(), FastMath.max(staticContext.getMaxEccPow(), MAX_ECCPOWER_SP));

            final GeneratingFunctionCoefficients gfCoefs =
                            new GeneratingFunctionCoefficients(staticContext.getMaxAR3Pow(), MAX_ECCPOWER_SP, staticContext.getMaxAR3Pow() + 1, context, hansen, aoR3Pow, Qns);

            //Compute additional quantities
            // 2 * a / An
            final double ax2oAn  = -context.getM2aoA() / context.getMeanMotion();
            // B / An
            final double BoAn    = context.getBoA() / context.getMeanMotion();
            // 1 / ABn
            final double ooABn   = context.getOoAB() / context.getMeanMotion();
            // C / 2ABn
            final double Co2ABn  = -context.getMCo2AB() / context.getMeanMotion();
            // B / (A * (1 + B) * n)
            final double BoABpon = context.getBoABpo() / context.getMeanMotion();
            // -3 / n²a² = -3 / nA
            final double m3onA   = -3 / (context.getA() * context.getMeanMotion());

            //Compute the C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> coefficients.
            for (int j = 1; j < slot.cij.length; j++) {
                // First compute the C<sub>i</sub><sup>j</sup> coefficients
                final double[] currentCij = new double[6];

                // Compute the cross derivatives operator :
                final double SAlphaGammaCj    = context.getAlpha() * gfCoefs.getdSdgammaCj(j) - context.getGamma() * gfCoefs.getdSdalphaCj(j);
                final double SAlphaBetaCj     = context.getAlpha() * gfCoefs.getdSdbetaCj(j)  - context.getBeta()  * gfCoefs.getdSdalphaCj(j);
                final double SBetaGammaCj     = context.getBeta() * gfCoefs.getdSdgammaCj(j) - context.getGamma() * gfCoefs.getdSdbetaCj(j);
                final double ShkCj            = auxiliaryElements.getH() * gfCoefs.getdSdkCj(j)     -  auxiliaryElements.getK()    * gfCoefs.getdSdhCj(j);
                final double pSagmIqSbgoABnCj = (auxiliaryElements.getP() * SAlphaGammaCj - I * auxiliaryElements.getQ() * SBetaGammaCj) * ooABn;
                final double ShkmSabmdSdlCj   = ShkCj - SAlphaBetaCj - gfCoefs.getdSdlambdaCj(j);

                currentCij[0] =  ax2oAn * gfCoefs.getdSdlambdaCj(j);
                currentCij[1] =  -(BoAn * gfCoefs.getdSdhCj(j) + auxiliaryElements.getH() * pSagmIqSbgoABnCj + auxiliaryElements.getK() * BoABpon * gfCoefs.getdSdlambdaCj(j));
                currentCij[2] =    BoAn * gfCoefs.getdSdkCj(j) + auxiliaryElements.getK() * pSagmIqSbgoABnCj - auxiliaryElements.getH() * BoABpon * gfCoefs.getdSdlambdaCj(j);
                currentCij[3] =  Co2ABn * (auxiliaryElements.getQ() * ShkmSabmdSdlCj - I * SAlphaGammaCj);
                currentCij[4] =  Co2ABn * (auxiliaryElements.getP() * ShkmSabmdSdlCj - SBetaGammaCj);
                currentCij[5] = -ax2oAn * gfCoefs.getdSdaCj(j) + BoABpon * (auxiliaryElements.getH() * gfCoefs.getdSdhCj(j) + auxiliaryElements.getK() * gfCoefs.getdSdkCj(j)) + pSagmIqSbgoABnCj + m3onA * gfCoefs.getSCj(j);

                // add the computed coefficients to the interpolators
                slot.cij[j].addGridPoint(meanState.getDate(), currentCij);

                // Compute the S<sub>i</sub><sup>j</sup> coefficients
                final double[] currentSij = new double[6];

                // Compute the cross derivatives operator :
                final double SAlphaGammaSj    = context.getAlpha() * gfCoefs.getdSdgammaSj(j) - context.getGamma() * gfCoefs.getdSdalphaSj(j);
                final double SAlphaBetaSj     = context.getAlpha() * gfCoefs.getdSdbetaSj(j)  - context.getBeta()  * gfCoefs.getdSdalphaSj(j);
                final double SBetaGammaSj     =  context.getBeta() * gfCoefs.getdSdgammaSj(j) - context.getGamma() * gfCoefs.getdSdbetaSj(j);
                final double ShkSj            =     auxiliaryElements.getH() * gfCoefs.getdSdkSj(j)     -  auxiliaryElements.getK()    * gfCoefs.getdSdhSj(j);
                final double pSagmIqSbgoABnSj = (auxiliaryElements.getP() * SAlphaGammaSj - I * auxiliaryElements.getQ() * SBetaGammaSj) * ooABn;
                final double ShkmSabmdSdlSj   =  ShkSj - SAlphaBetaSj - gfCoefs.getdSdlambdaSj(j);

                currentSij[0] =  ax2oAn * gfCoefs.getdSdlambdaSj(j);
                currentSij[1] =  -(BoAn * gfCoefs.getdSdhSj(j) + auxiliaryElements.getH() * pSagmIqSbgoABnSj + auxiliaryElements.getK() * BoABpon * gfCoefs.getdSdlambdaSj(j));
                currentSij[2] =    BoAn * gfCoefs.getdSdkSj(j) + auxiliaryElements.getK() * pSagmIqSbgoABnSj - auxiliaryElements.getH() * BoABpon * gfCoefs.getdSdlambdaSj(j);
                currentSij[3] =  Co2ABn * (auxiliaryElements.getQ() * ShkmSabmdSdlSj - I * SAlphaGammaSj);
                currentSij[4] =  Co2ABn * (auxiliaryElements.getP() * ShkmSabmdSdlSj - SBetaGammaSj);
                currentSij[5] = -ax2oAn * gfCoefs.getdSdaSj(j) + BoABpon * (auxiliaryElements.getH() * gfCoefs.getdSdhSj(j) + auxiliaryElements.getK() * gfCoefs.getdSdkSj(j)) + pSagmIqSbgoABnSj + m3onA * gfCoefs.getSSj(j);

                // add the computed coefficients to the interpolators
                slot.sij[j].addGridPoint(meanState.getDate(), currentSij);

                if (j == 1) {
                    //Compute the C⁰ coefficients using Danielson 2.5.2-15a.
                    final double[] value = new double[6];
                    for (int i = 0; i < 6; ++i) {
                        value[i] = currentCij[i] * auxiliaryElements.getK() / 2. + currentSij[i] * auxiliaryElements.getH() / 2.;
                    }
                    slot.cij[0].addGridPoint(meanState.getDate(), value);
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

        final FieldThirdBodyShortPeriodicCoefficients<T> ftbspc = (FieldThirdBodyShortPeriodicCoefficients<T>) fieldShortPeriods.get(field);
        final FieldSlot<T> slot = ftbspc.createSlot(meanStates);
        for (final FieldSpacecraftState<T> meanState : meanStates) {

            // Auxiliary elements related to the current orbit
            final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(meanState.getOrbit(), I);

            // Extract the proper parameters valid for the corresponding meanState date from the input array
            final T[] extractedParameters = this.extractParameters(parameters, auxiliaryElements.getDate());

            // Container of attributes
            final FieldDSSTThirdBodyDynamicContext<T> context = initializeStep(auxiliaryElements, extractedParameters);

            // a / R3 up to power maxAR3Pow
            final T[] aoR3Pow = computeAoR3Pow(context);

            // Qns coefficients
            final T[][] Qns = CoefficientsFactory.computeQns(context.getGamma(), staticContext.getMaxAR3Pow(), FastMath.max(staticContext.getMaxEccPow(), MAX_ECCPOWER_SP));

            final FieldGeneratingFunctionCoefficients<T> gfCoefs =
                            new FieldGeneratingFunctionCoefficients<>(staticContext.getMaxAR3Pow(), MAX_ECCPOWER_SP, staticContext.getMaxAR3Pow() + 1,
                                                                      context, (FieldHansenObjects<T>) fieldHansen.get(field), field, aoR3Pow, Qns);

            //Compute additional quantities
            // 2 * a / An
            final T ax2oAn  = context.getM2aoA().negate().divide(context.getMeanMotion());
            // B / An
            final T BoAn    = context.getBoA().divide(context.getMeanMotion());
            // 1 / ABn
            final T ooABn   = context.getOoAB().divide(context.getMeanMotion());
            // C / 2ABn
            final T Co2ABn  = context.getMCo2AB().negate().divide(context.getMeanMotion());
            // B / (A * (1 + B) * n)
            final T BoABpon = context.getBoABpo().divide(context.getMeanMotion());
            // -3 / n²a² = -3 / nA
            final T m3onA   = context.getA().multiply(context.getMeanMotion()).divide(-3.).reciprocal();

            //Compute the C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> coefficients.
            for (int j = 1; j < slot.cij.length; j++) {
                // First compute the C<sub>i</sub><sup>j</sup> coefficients
                final T[] currentCij = MathArrays.buildArray(field, 6);

                // Compute the cross derivatives operator :
                final T SAlphaGammaCj    = context.getAlpha().multiply(gfCoefs.getdSdgammaCj(j)).subtract(context.getGamma().multiply(gfCoefs.getdSdalphaCj(j)));
                final T SAlphaBetaCj     = context.getAlpha().multiply(gfCoefs.getdSdbetaCj(j)).subtract(context.getBeta().multiply(gfCoefs.getdSdalphaCj(j)));
                final T SBetaGammaCj     = context.getBeta().multiply(gfCoefs.getdSdgammaCj(j)).subtract(context.getGamma().multiply(gfCoefs.getdSdbetaCj(j)));
                final T ShkCj            = auxiliaryElements.getH().multiply(gfCoefs.getdSdkCj(j)).subtract(auxiliaryElements.getK().multiply(gfCoefs.getdSdhCj(j)));
                final T pSagmIqSbgoABnCj = ooABn.multiply(auxiliaryElements.getP().multiply(SAlphaGammaCj).subtract(auxiliaryElements.getQ().multiply(SBetaGammaCj).multiply(I)));
                final T ShkmSabmdSdlCj   = ShkCj.subtract(SAlphaBetaCj).subtract(gfCoefs.getdSdlambdaCj(j));

                currentCij[0] = ax2oAn.multiply(gfCoefs.getdSdlambdaCj(j));
                currentCij[1] = BoAn.multiply(gfCoefs.getdSdhCj(j)).add(auxiliaryElements.getH().multiply(pSagmIqSbgoABnCj)).add(auxiliaryElements.getK().multiply(BoABpon).multiply(gfCoefs.getdSdlambdaCj(j))).negate();
                currentCij[2] = BoAn.multiply(gfCoefs.getdSdkCj(j)).add(auxiliaryElements.getK().multiply(pSagmIqSbgoABnCj)).subtract(auxiliaryElements.getH().multiply(BoABpon).multiply(gfCoefs.getdSdlambdaCj(j)));
                currentCij[3] = Co2ABn.multiply(auxiliaryElements.getQ().multiply(ShkmSabmdSdlCj).subtract(SAlphaGammaCj.multiply(I)));
                currentCij[4] = Co2ABn.multiply(auxiliaryElements.getP().multiply(ShkmSabmdSdlCj).subtract(SBetaGammaCj));
                currentCij[5] = ax2oAn.negate().multiply(gfCoefs.getdSdaCj(j)).add(BoABpon.multiply(auxiliaryElements.getH().multiply(gfCoefs.getdSdhCj(j)).add(auxiliaryElements.getK().multiply(gfCoefs.getdSdkCj(j))))).add(pSagmIqSbgoABnCj).add(m3onA.multiply(gfCoefs.getSCj(j)));

                // add the computed coefficients to the interpolators
                slot.cij[j].addGridPoint(meanState.getDate(), currentCij);

                // Compute the S<sub>i</sub><sup>j</sup> coefficients
                final T[] currentSij = MathArrays.buildArray(field, 6);

                // Compute the cross derivatives operator :
                final T SAlphaGammaSj    = context.getAlpha().multiply(gfCoefs.getdSdgammaSj(j)).subtract(context.getGamma().multiply(gfCoefs.getdSdalphaSj(j)));
                final T SAlphaBetaSj     = context.getAlpha().multiply(gfCoefs.getdSdbetaSj(j)).subtract(context.getBeta().multiply(gfCoefs.getdSdalphaSj(j)));
                final T SBetaGammaSj     = context.getBeta().multiply(gfCoefs.getdSdgammaSj(j)).subtract(context.getGamma().multiply(gfCoefs.getdSdbetaSj(j)));
                final T ShkSj            = auxiliaryElements.getH().multiply(gfCoefs.getdSdkSj(j)).subtract(auxiliaryElements.getK().multiply(gfCoefs.getdSdhSj(j)));
                final T pSagmIqSbgoABnSj = ooABn.multiply(auxiliaryElements.getP().multiply(SAlphaGammaSj).subtract(auxiliaryElements.getQ().multiply(SBetaGammaSj).multiply(I)));
                final T ShkmSabmdSdlSj   = ShkSj.subtract(SAlphaBetaSj).subtract(gfCoefs.getdSdlambdaSj(j));

                currentSij[0] = ax2oAn.multiply(gfCoefs.getdSdlambdaSj(j));
                currentSij[1] = BoAn.multiply(gfCoefs.getdSdhSj(j)).add(auxiliaryElements.getH().multiply(pSagmIqSbgoABnSj)).add(auxiliaryElements.getK().multiply(BoABpon).multiply(gfCoefs.getdSdlambdaSj(j))).negate();
                currentSij[2] = BoAn.multiply(gfCoefs.getdSdkSj(j)).add(auxiliaryElements.getK().multiply(pSagmIqSbgoABnSj)).subtract(auxiliaryElements.getH().multiply(BoABpon).multiply(gfCoefs.getdSdlambdaSj(j)));
                currentSij[3] = Co2ABn.multiply(auxiliaryElements.getQ().multiply(ShkmSabmdSdlSj).subtract(SAlphaGammaSj.multiply(I)));
                currentSij[4] = Co2ABn.multiply(auxiliaryElements.getP().multiply(ShkmSabmdSdlSj).subtract(SBetaGammaSj));
                currentSij[5] = ax2oAn.negate().multiply(gfCoefs.getdSdaSj(j)).add(BoABpon.multiply(auxiliaryElements.getH().multiply(gfCoefs.getdSdhSj(j)).add(auxiliaryElements.getK().multiply(gfCoefs.getdSdkSj(j))))).add(pSagmIqSbgoABnSj).add(m3onA.multiply(gfCoefs.getSSj(j)));

                // add the computed coefficients to the interpolators
                slot.sij[j].addGridPoint(meanState.getDate(), currentSij);

                if (j == 1) {
                    //Compute the C⁰ coefficients using Danielson 2.5.2-15a.
                    final T[] value = MathArrays.buildArray(field, 6);
                    for (int i = 0; i < 6; ++i) {
                        value[i] = currentCij[i].multiply(auxiliaryElements.getK()).divide(2.).add(currentSij[i].multiply(auxiliaryElements.getH()).divide(2.));
                    }
                    slot.cij[0].addGridPoint(meanState.getDate(), value);
                }
            }
        }
    }

    /**
     * Computes a / R3 to the power maxAR3Pow.
     * @param context force model dynamic context
     * @return aoR3Pow array
     */
    private double[] computeAoR3Pow(final DSSTThirdBodyDynamicContext context) {
        // a / R3 up to power maxAR3Pow
        final double aoR3 = context.getAuxiliaryElements().getSma() / context.getR3();
        final double[] aoR3Pow = new double[staticContext.getMaxAR3Pow() + 1];
        aoR3Pow[0] = 1.;
        for (int i = 1; i <= staticContext.getMaxAR3Pow(); i++) {
            aoR3Pow[i] = aoR3 * aoR3Pow[i - 1];
        }
        return aoR3Pow;
    }

    /**
     * Computes a / R3 to the power maxAR3Pow.
     * @param context force model dynamic context
     * @param <T> type of the elements
     * @return aoR3Pow array
     */
    private <T extends CalculusFieldElement<T>> T[] computeAoR3Pow(final FieldDSSTThirdBodyDynamicContext<T> context) {
        final Field<T> field = context.getA().getField();
        // a / R3 up to power maxAR3Pow
        final T aoR3 = context.getFieldAuxiliaryElements().getSma().divide(context.getR3());
        final T[] aoR3Pow = MathArrays.buildArray(field, staticContext.getMaxAR3Pow() + 1);
        aoR3Pow[0] = field.getOne();
        for (int i = 1; i <= staticContext.getMaxAR3Pow(); i++) {
            aoR3Pow[i] = aoR3.multiply(aoR3Pow[i - 1]);
        }
        return aoR3Pow;
    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider provider) {
        //nothing is done since this contribution is not sensitive to attitude
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(parameterDrivers);
    }

    /** Computes the C<sup>j</sup> and S<sup>j</sup> coefficients Danielson 4.2-(15,16)
     * and their derivatives.
     *  <p>
     *  CS Mathematical Report $3.5.3.2
     *  </p>
     */
    private class FourierCjSjCoefficients {

        /** The coefficients G<sub>n, s</sub> and their derivatives. */
        private final GnsCoefficients gns;

        /** the coefficients e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and their derivatives by h and k. */
        private final WnsjEtomjmsCoefficient wnsjEtomjmsCoefficient;

        /** The terms containing the coefficients C<sub>j</sub> and S<sub>j</sub> of (α, β) or (k, h). */
        private final CjSjAlphaBetaKH ABDECoefficients;

        /** The Fourier coefficients C<sup>j</sup> and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - C<sup>j</sup> <br/>
         * - dC<sup>j</sup> / da <br/>
         * - dC<sup>j</sup> / dk <br/>
         * - dC<sup>j</sup> / dh <br/>
         * - dC<sup>j</sup> / dα <br/>
         * - dC<sup>j</sup> / dβ <br/>
         * - dC<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final double[][] cj;

        /** The S<sup>j</sup> coefficients and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - S<sup>j</sup> <br/>
         * - dS<sup>j</sup> / da <br/>
         * - dS<sup>j</sup> / dk <br/>
         * - dS<sup>j</sup> / dh <br/>
         * - dS<sup>j</sup> / dα <br/>
         * - dS<sup>j</sup> / dβ <br/>
         * - dS<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final double[][] sj;

        /** The Coefficients C<sup>j</sup><sub>,λ</sub>.
         * <p>
         * See Danielson 4.2-21
         * </p>
         */
        private final double[] cjlambda;

        /** The Coefficients S<sup>j</sup><sub>,λ</sub>.
        * <p>
        * See Danielson 4.2-21
        * </p>
        */
        private final double[] sjlambda;

        /** Maximum value for n. */
        private final int nMax;

        /** Maximum value for s. */
        private final int sMax;

        /** Maximum value for j. */
        private final int jMax;

        /**
         * Private constructor.
         *
         * @param nMax maximum value for n index
         * @param sMax maximum value for s index
         * @param jMax maximum value for j index
         * @param context container for dynamic force model attributes
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        FourierCjSjCoefficients(final int nMax, final int sMax, final int jMax,
                                final DSSTThirdBodyDynamicContext context,
                                final double[] aoR3Pow, final double[][] qns) {
            //Save parameters
            this.nMax = nMax;
            this.sMax = sMax;
            this.jMax = jMax;

            //Create objects
            wnsjEtomjmsCoefficient = new WnsjEtomjmsCoefficient(context);
            ABDECoefficients = new CjSjAlphaBetaKH(context);
            gns = new GnsCoefficients(nMax, sMax, context, aoR3Pow, qns);

            //create arays
            this.cj = new double[7][jMax + 1];
            this.sj = new double[7][jMax + 1];
            this.cjlambda = new double[jMax];
            this.sjlambda = new double[jMax];

            computeCoefficients(context);
        }

        /**
         * Compute all coefficients.
         * @param context container for attributes
         */
        private void computeCoefficients(final DSSTThirdBodyDynamicContext context) {

            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            for (int j = 1; j <= jMax; j++) {
                // initialise the coefficients
                for (int i = 0; i <= 6; i++) {
                    cj[i][j] = 0.;
                    sj[i][j] = 0.;
                }
                if (j < jMax) {
                    // initialise the C<sup>j</sup><sub>,λ</sub> and S<sup>j</sup><sub>,λ</sub> coefficients
                    cjlambda[j] = 0.;
                    sjlambda[j] = 0.;
                }
                for (int s = 0; s <= sMax; s++) {

                    // Compute the coefficients A<sub>j, s</sub>, B<sub>j, s</sub>, D<sub>j, s</sub> and E<sub>j, s</sub>
                    ABDECoefficients.computeCoefficients(j, s);

                    // compute starting value for n
                    final int minN = FastMath.max(2, FastMath.max(j - 1, s));

                    for (int n = minN; n <= nMax; n++) {
                        // check if n-s is even
                        if ((n - s) % 2 == 0) {
                            // compute the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n+1, s</sup> and its derivatives
                            final double[] wjnp1semjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(j, s, n + 1, context);

                            // compute the coefficient e<sup>-|j-s|</sup>*w<sub>-j</sub><sup>n+1, s</sup> and its derivatives
                            final double[] wmjnp1semjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(-j, s, n + 1, context);

                            // compute common factors
                            final double coef1 = -(wjnp1semjms[0] * ABDECoefficients.getCoefA() + wmjnp1semjms[0] * ABDECoefficients.getCoefB());
                            final double coef2 =   wjnp1semjms[0] * ABDECoefficients.getCoefD() + wmjnp1semjms[0] * ABDECoefficients.getCoefE();

                            //Compute C<sup>j</sup>
                            cj[0][j] += gns.getGns(n, s) * coef1;
                            //Compute dC<sup>j</sup> / da
                            cj[1][j] += gns.getdGnsda(n, s) * coef1;
                            //Compute dC<sup>j</sup> / dk
                            cj[2][j] += -gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[1] * ABDECoefficients.getCoefA() +
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefAdk() +
                                            wmjnp1semjms[1] * ABDECoefficients.getCoefB() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefBdk()
                                         );
                            //Compute dC<sup>j</sup> / dh
                            cj[3][j] += -gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[2] * ABDECoefficients.getCoefA() +
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefAdh() +
                                            wmjnp1semjms[2] * ABDECoefficients.getCoefB() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefBdh()
                                         );
                            //Compute dC<sup>j</sup> / dα
                            cj[4][j] += -gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefAdalpha() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefBdalpha()
                                        );
                            //Compute dC<sup>j</sup> / dβ
                            cj[5][j] += -gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefAdbeta() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefBdbeta()
                                        );
                            //Compute dC<sup>j</sup> / dγ
                            cj[6][j] += gns.getdGnsdgamma(n, s) * coef1;

                            //Compute S<sup>j</sup>
                            sj[0][j] += gns.getGns(n, s) * coef2;
                            //Compute dS<sup>j</sup> / da
                            sj[1][j] += gns.getdGnsda(n, s) * coef2;
                            //Compute dS<sup>j</sup> / dk
                            sj[2][j] += gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[1] * ABDECoefficients.getCoefD() +
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefDdk() +
                                            wmjnp1semjms[1] * ABDECoefficients.getCoefE() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefEdk()
                                         );
                            //Compute dS<sup>j</sup> / dh
                            sj[3][j] += gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[2] * ABDECoefficients.getCoefD() +
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefDdh() +
                                            wmjnp1semjms[2] * ABDECoefficients.getCoefE() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefEdh()
                                         );
                            //Compute dS<sup>j</sup> / dα
                            sj[4][j] += gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefDdalpha() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefEdalpha()
                                        );
                            //Compute dS<sup>j</sup> / dβ
                            sj[5][j] += gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefDdbeta() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefEdbeta()
                                        );
                            //Compute dS<sup>j</sup> / dγ
                            sj[6][j] += gns.getdGnsdgamma(n, s) * coef2;

                            //Check if n is greater or equal to j and j is at most jMax-1
                            if (n >= j && j < jMax) {
                                // compute the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives
                                final double[] wjnsemjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(j, s, n, context);

                                // compute the coefficient e<sup>-|j-s|</sup>*w<sub>-j</sub><sup>n, s</sup> and its derivatives
                                final double[] wmjnsemjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(-j, s, n, context);

                                //Compute C<sup>j</sup><sub>,λ</sub>
                                cjlambda[j] += gns.getGns(n, s) * (wjnsemjms[0] * ABDECoefficients.getCoefD() + wmjnsemjms[0] * ABDECoefficients.getCoefE());
                                //Compute S<sup>j</sup><sub>,λ</sub>
                                sjlambda[j] += gns.getGns(n, s) * (wjnsemjms[0] * ABDECoefficients.getCoefA() + wmjnsemjms[0] * ABDECoefficients.getCoefB());
                            }
                        }
                    }
                }
                // Divide by j
                for (int i = 0; i <= 6; i++) {
                    cj[i][j] /= j;
                    sj[i][j] /= j;
                }
            }
            //The C⁰ coefficients are not computed here.
            //They are evaluated at the final point.

            //C⁰<sub>,λ</sub>
            cjlambda[0] = auxiliaryElements.getK() * cjlambda[1] / 2. + auxiliaryElements.getH() * sjlambda[1] / 2.;
        }

        /** Get the Fourier coefficient C<sup>j</sup>.
         * @param j j index
         * @return C<sup>j</sup>
         */
        public double getCj(final int j) {
            return cj[0][j];
        }

        /** Get the derivative dC<sup>j</sup>/da.
         * @param j j index
         * @return dC<sup>j</sup>/da
         */
        public double getdCjda(final int j) {
            return cj[1][j];
        }

        /** Get the derivative dC<sup>j</sup>/dk.
         * @param j j index
         * @return dC<sup>j</sup>/dk
         */
        public double getdCjdk(final int j) {
            return cj[2][j];
        }

        /** Get the derivative dC<sup>j</sup>/dh.
         * @param j j index
         * @return dC<sup>j</sup>/dh
         */
        public double getdCjdh(final int j) {
            return cj[3][j];
        }

        /** Get the derivative dC<sup>j</sup>/dα.
         * @param j j index
         * @return dC<sup>j</sup>/dα
         */
        public double getdCjdalpha(final int j) {
            return cj[4][j];
        }

        /** Get the derivative dC<sup>j</sup>/dβ.
         * @param j j index
         * @return dC<sup>j</sup>/dβ
         */
        public double getdCjdbeta(final int j) {
            return cj[5][j];
        }

        /** Get the derivative dC<sup>j</sup>/dγ.
         * @param j j index
         * @return dC<sup>j</sup>/dγ
         */
        public double getdCjdgamma(final int j) {
            return cj[6][j];
        }

        /** Get the Fourier coefficient S<sup>j</sup>.
         * @param j j index
         * @return S<sup>j</sup>
         */
        public double getSj(final int j) {
            return sj[0][j];
        }

        /** Get the derivative dS<sup>j</sup>/da.
         * @param j j index
         * @return dS<sup>j</sup>/da
         */
        public double getdSjda(final int j) {
            return sj[1][j];
        }

        /** Get the derivative dS<sup>j</sup>/dk.
         * @param j j index
         * @return dS<sup>j</sup>/dk
         */
        public double getdSjdk(final int j) {
            return sj[2][j];
        }

        /** Get the derivative dS<sup>j</sup>/dh.
         * @param j j index
         * @return dS<sup>j</sup>/dh
         */
        public double getdSjdh(final int j) {
            return sj[3][j];
        }

        /** Get the derivative dS<sup>j</sup>/dα.
         * @param j j index
         * @return dS<sup>j</sup>/dα
         */
        public double getdSjdalpha(final int j) {
            return sj[4][j];
        }

        /** Get the derivative dS<sup>j</sup>/dβ.
         * @param j j index
         * @return dS<sup>j</sup>/dβ
         */
        public double getdSjdbeta(final int j) {
            return sj[5][j];
        }

        /** Get the derivative dS<sup>j</sup>/dγ.
         * @param j j index
         * @return dS<sup>j</sup>/dγ
         */
        public double getdSjdgamma(final int j) {
            return sj[6][j];
        }

        /** Get the coefficient C⁰<sub>,λ</sub>.
         * @return C⁰<sub>,λ</sub>
         */
        public double getC0Lambda() {
            return cjlambda[0];
        }

        /** Get the coefficient C<sup>j</sup><sub>,λ</sub>.
         * @param j j index
         * @return C<sup>j</sup><sub>,λ</sub>
         */
        public double getCjLambda(final int j) {
            if (j < 1 || j >= jMax) {
                return 0.;
            }
            return cjlambda[j];
        }

        /** Get the coefficient S<sup>j</sup><sub>,λ</sub>.
         * @param j j index
         * @return S<sup>j</sup><sub>,λ</sub>
         */
        public double getSjLambda(final int j) {
            if (j < 1 || j >= jMax) {
                return 0.;
            }
            return sjlambda[j];
        }
    }

    /** Computes the C<sup>j</sup> and S<sup>j</sup> coefficients Danielson 4.2-(15,16)
     * and their derivatives.
     *  <p>
     *  CS Mathematical Report $3.5.3.2
     *  </p>
     */
    private class FieldFourierCjSjCoefficients <T extends CalculusFieldElement<T>> {

        /** The coefficients G<sub>n, s</sub> and their derivatives. */
        private final FieldGnsCoefficients<T> gns;

        /** the coefficients e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and their derivatives by h and k. */
        private final FieldWnsjEtomjmsCoefficient<T> wnsjEtomjmsCoefficient;

        /** The terms containing the coefficients C<sub>j</sub> and S<sub>j</sub> of (α, β) or (k, h). */
        private final FieldCjSjAlphaBetaKH<T> ABDECoefficients;

        /** The Fourier coefficients C<sup>j</sup> and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - C<sup>j</sup> <br/>
         * - dC<sup>j</sup> / da <br/>
         * - dC<sup>j</sup> / dk <br/>
         * - dC<sup>j</sup> / dh <br/>
         * - dC<sup>j</sup> / dα <br/>
         * - dC<sup>j</sup> / dβ <br/>
         * - dC<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final T[][] cj;

        /** The S<sup>j</sup> coefficients and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - S<sup>j</sup> <br/>
         * - dS<sup>j</sup> / da <br/>
         * - dS<sup>j</sup> / dk <br/>
         * - dS<sup>j</sup> / dh <br/>
         * - dS<sup>j</sup> / dα <br/>
         * - dS<sup>j</sup> / dβ <br/>
         * - dS<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final T[][] sj;

        /** The Coefficients C<sup>j</sup><sub>,λ</sub>.
         * <p>
         * See Danielson 4.2-21
         * </p>
         */
        private final T[] cjlambda;

        /** The Coefficients S<sup>j</sup><sub>,λ</sub>.
        * <p>
        * See Danielson 4.2-21
        * </p>
        */
        private final T[] sjlambda;

        /** Zero. */
        private final T zero;

        /** Maximum value for n. */
        private final int nMax;

        /** Maximum value for s. */
        private final int sMax;

        /** Maximum value for j. */
        private final int jMax;

        /**
         * Private constructor.
         *
         * @param nMax maximum value for n index
         * @param sMax maximum value for s index
         * @param jMax maximum value for j index
         * @param context container for dynamic force model attributes
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         * @param field field used by default
         */
        FieldFourierCjSjCoefficients(final int nMax, final int sMax, final int jMax,
                                     final FieldDSSTThirdBodyDynamicContext<T> context,
                                     final T[] aoR3Pow, final T[][] qns,
                                     final Field<T> field) {
            //Zero
            this.zero = field.getZero();

            //Save parameters
            this.nMax = nMax;
            this.sMax = sMax;
            this.jMax = jMax;

            //Create objects
            wnsjEtomjmsCoefficient = new FieldWnsjEtomjmsCoefficient<>(context, field);
            ABDECoefficients       = new FieldCjSjAlphaBetaKH<>(context, field);
            gns                    = new FieldGnsCoefficients<>(nMax, sMax, context, aoR3Pow, qns, field);

            //create arays
            this.cj = MathArrays.buildArray(field, 7, jMax + 1);
            this.sj = MathArrays.buildArray(field, 7, jMax + 1);
            this.cjlambda = MathArrays.buildArray(field, jMax);
            this.sjlambda = MathArrays.buildArray(field, jMax);

            computeCoefficients(context, field);
        }

        /**
         * Compute all coefficients.
         * @param context container for attributes
         * @param field field used by default
         */
        private void computeCoefficients(final FieldDSSTThirdBodyDynamicContext<T> context,
                                         final Field<T> field) {

            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            for (int j = 1; j <= jMax; j++) {
                // initialise the coefficients
                for (int i = 0; i <= 6; i++) {
                    cj[i][j] = zero;
                    sj[i][j] = zero;
                }
                if (j < jMax) {
                    // initialise the C<sup>j</sup><sub>,λ</sub> and S<sup>j</sup><sub>,λ</sub> coefficients
                    cjlambda[j] = zero;
                    sjlambda[j] = zero;
                }
                for (int s = 0; s <= sMax; s++) {

                    // Compute the coefficients A<sub>j, s</sub>, B<sub>j, s</sub>, D<sub>j, s</sub> and E<sub>j, s</sub>
                    ABDECoefficients.computeCoefficients(j, s);

                    // compute starting value for n
                    final int minN = FastMath.max(2, FastMath.max(j - 1, s));

                    for (int n = minN; n <= nMax; n++) {
                        // check if n-s is even
                        if ((n - s) % 2 == 0) {
                            // compute the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n+1, s</sup> and its derivatives
                            final T[] wjnp1semjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(j, s, n + 1, context, field);

                            // compute the coefficient e<sup>-|j-s|</sup>*w<sub>-j</sub><sup>n+1, s</sup> and its derivatives
                            final T[] wmjnp1semjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(-j, s, n + 1, context, field);

                            // compute common factors
                            final T coef1 = (wjnp1semjms[0].multiply(ABDECoefficients.getCoefA()).add(wmjnp1semjms[0].multiply(ABDECoefficients.getCoefB()))).negate();
                            final T coef2 =  wjnp1semjms[0].multiply(ABDECoefficients.getCoefD()).add(wmjnp1semjms[0].multiply(ABDECoefficients.getCoefE()));

                            //Compute C<sup>j</sup>
                            cj[0][j] = cj[0][j].add(gns.getGns(n, s).multiply(coef1));
                            //Compute dC<sup>j</sup> / da
                            cj[1][j] = cj[1][j].add(gns.getdGnsda(n, s).multiply(coef1));
                            //Compute dC<sup>j</sup> / dk
                            cj[2][j] = cj[2][j].add(gns.getGns(n, s).negate().
                                       multiply(
                                            wjnp1semjms[1].multiply(ABDECoefficients.getCoefA()).
                                            add(wjnp1semjms[0].multiply(ABDECoefficients.getdCoefAdk())).
                                            add(wmjnp1semjms[1].multiply(ABDECoefficients.getCoefB())).
                                            add(wmjnp1semjms[0].multiply(ABDECoefficients.getdCoefBdk()))
                                         ));
                            //Compute dC<sup>j</sup> / dh
                            cj[3][j] = cj[3][j].add(gns.getGns(n, s).negate().
                                       multiply(
                                             wjnp1semjms[2].multiply(ABDECoefficients.getCoefA()).
                                             add(wjnp1semjms[0].multiply(ABDECoefficients.getdCoefAdh())).
                                             add(wmjnp1semjms[2].multiply(ABDECoefficients.getCoefB())).
                                             add(wmjnp1semjms[0].multiply(ABDECoefficients.getdCoefBdh()))
                                             ));
                            //Compute dC<sup>j</sup> / dα
                            cj[4][j] = cj[4][j].add(gns.getGns(n, s).negate().
                                       multiply(
                                           wjnp1semjms[0].multiply(ABDECoefficients.getdCoefAdalpha()).
                                           add(wmjnp1semjms[0].multiply(ABDECoefficients.getdCoefBdalpha()))
                                       ));
                            //Compute dC<sup>j</sup> / dβ
                            cj[5][j] = cj[5][j].add(gns.getGns(n, s).negate().
                                       multiply(
                                           wjnp1semjms[0].multiply(ABDECoefficients.getdCoefAdbeta()).
                                           add(wmjnp1semjms[0].multiply(ABDECoefficients.getdCoefBdbeta()))
                                       ));
                            //Compute dC<sup>j</sup> / dγ
                            cj[6][j] = cj[6][j].add(gns.getdGnsdgamma(n, s).multiply(coef1));

                            //Compute S<sup>j</sup>
                            sj[0][j] = sj[0][j].add(gns.getGns(n, s).multiply(coef2));
                            //Compute dS<sup>j</sup> / da
                            sj[1][j] = sj[1][j].add(gns.getdGnsda(n, s).multiply(coef2));
                            //Compute dS<sup>j</sup> / dk
                            sj[2][j] = sj[2][j].add(gns.getGns(n, s).
                                       multiply(
                                           wjnp1semjms[1].multiply(ABDECoefficients.getCoefD()).
                                           add(wjnp1semjms[0].multiply(ABDECoefficients.getdCoefDdk())).
                                           add(wmjnp1semjms[1].multiply(ABDECoefficients.getCoefE())).
                                           add(wmjnp1semjms[0].multiply(ABDECoefficients.getdCoefEdk()))
                                       ));
                            //Compute dS<sup>j</sup> / dh
                            sj[3][j] = sj[3][j].add(gns.getGns(n, s).
                                       multiply(
                                           wjnp1semjms[2].multiply(ABDECoefficients.getCoefD()).
                                           add(wjnp1semjms[0].multiply(ABDECoefficients.getdCoefDdh())).
                                           add(wmjnp1semjms[2].multiply(ABDECoefficients.getCoefE())).
                                           add(wmjnp1semjms[0].multiply(ABDECoefficients.getdCoefEdh()))
                                       ));
                            //Compute dS<sup>j</sup> / dα
                            sj[4][j] = sj[4][j].add(gns.getGns(n, s).
                                       multiply(
                                            wjnp1semjms[0].multiply(ABDECoefficients.getdCoefDdalpha()).
                                            add(wmjnp1semjms[0].multiply(ABDECoefficients.getdCoefEdalpha()))
                                       ));
                            //Compute dS<sup>j</sup> / dβ
                            sj[5][j] = sj[5][j].add(gns.getGns(n, s).
                                        multiply(
                                            wjnp1semjms[0].multiply(ABDECoefficients.getdCoefDdbeta()).
                                            add(wmjnp1semjms[0].multiply(ABDECoefficients.getdCoefEdbeta()))
                                       ));
                            //Compute dS<sup>j</sup> / dγ
                            sj[6][j] = sj[6][j].add(gns.getdGnsdgamma(n, s).multiply(coef2));

                            //Check if n is greater or equal to j and j is at most jMax-1
                            if (n >= j && j < jMax) {
                                // compute the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives
                                final T[] wjnsemjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(j, s, n, context, field);

                                // compute the coefficient e<sup>-|j-s|</sup>*w<sub>-j</sub><sup>n, s</sup> and its derivatives
                                final T[] wmjnsemjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(-j, s, n, context, field);

                                //Compute C<sup>j</sup><sub>,λ</sub>
                                cjlambda[j] = cjlambda[j].add(gns.getGns(n, s).multiply(wjnsemjms[0].multiply(ABDECoefficients.getCoefD()).add(wmjnsemjms[0].multiply(ABDECoefficients.getCoefE()))));
                                //Compute S<sup>j</sup><sub>,λ</sub>
                                sjlambda[j] = sjlambda[j].add(gns.getGns(n, s).multiply(wjnsemjms[0].multiply(ABDECoefficients.getCoefA()).add(wmjnsemjms[0].multiply(ABDECoefficients.getCoefB()))));
                            }
                        }
                    }
                }
                // Divide by j
                for (int i = 0; i <= 6; i++) {
                    cj[i][j] = cj[i][j].divide(j);
                    sj[i][j] = sj[i][j].divide(j);
                }
            }
            //The C⁰ coefficients are not computed here.
            //They are evaluated at the final point.

            //C⁰<sub>,λ</sub>
            cjlambda[0] = auxiliaryElements.getK().multiply(cjlambda[1]).divide(2.).add(auxiliaryElements.getH().multiply(sjlambda[1]).divide(2.));
        }

        /** Get the Fourier coefficient C<sup>j</sup>.
         * @param j j index
         * @return C<sup>j</sup>
         */
        public T getCj(final int j) {
            return cj[0][j];
        }

        /** Get the derivative dC<sup>j</sup>/da.
         * @param j j index
         * @return dC<sup>j</sup>/da
         */
        public T getdCjda(final int j) {
            return cj[1][j];
        }

        /** Get the derivative dC<sup>j</sup>/dk.
         * @param j j index
         * @return dC<sup>j</sup>/dk
         */
        public T getdCjdk(final int j) {
            return cj[2][j];
        }

        /** Get the derivative dC<sup>j</sup>/dh.
         * @param j j index
         * @return dC<sup>j</sup>/dh
         */
        public T getdCjdh(final int j) {
            return cj[3][j];
        }

        /** Get the derivative dC<sup>j</sup>/dα.
         * @param j j index
         * @return dC<sup>j</sup>/dα
         */
        public T getdCjdalpha(final int j) {
            return cj[4][j];
        }

        /** Get the derivative dC<sup>j</sup>/dβ.
         * @param j j index
         * @return dC<sup>j</sup>/dβ
         */
        public T getdCjdbeta(final int j) {
            return cj[5][j];
        }

        /** Get the derivative dC<sup>j</sup>/dγ.
         * @param j j index
         * @return dC<sup>j</sup>/dγ
         */
        public T getdCjdgamma(final int j) {
            return cj[6][j];
        }

        /** Get the Fourier coefficient S<sup>j</sup>.
         * @param j j index
         * @return S<sup>j</sup>
         */
        public T getSj(final int j) {
            return sj[0][j];
        }

        /** Get the derivative dS<sup>j</sup>/da.
         * @param j j index
         * @return dS<sup>j</sup>/da
         */
        public T getdSjda(final int j) {
            return sj[1][j];
        }

        /** Get the derivative dS<sup>j</sup>/dk.
         * @param j j index
         * @return dS<sup>j</sup>/dk
         */
        public T getdSjdk(final int j) {
            return sj[2][j];
        }

        /** Get the derivative dS<sup>j</sup>/dh.
         * @param j j index
         * @return dS<sup>j</sup>/dh
         */
        public T getdSjdh(final int j) {
            return sj[3][j];
        }

        /** Get the derivative dS<sup>j</sup>/dα.
         * @param j j index
         * @return dS<sup>j</sup>/dα
         */
        public T getdSjdalpha(final int j) {
            return sj[4][j];
        }

        /** Get the derivative dS<sup>j</sup>/dβ.
         * @param j j index
         * @return dS<sup>j</sup>/dβ
         */
        public T getdSjdbeta(final int j) {
            return sj[5][j];
        }

        /** Get the derivative dS<sup>j</sup>/dγ.
         * @param j j index
         * @return dS<sup>j</sup>/dγ
         */
        public T getdSjdgamma(final int j) {
            return sj[6][j];
        }

        /** Get the coefficient C⁰<sub>,λ</sub>.
         * @return C⁰<sub>,λ</sub>
         */
        public T getC0Lambda() {
            return cjlambda[0];
        }

        /** Get the coefficient C<sup>j</sup><sub>,λ</sub>.
         * @param j j index
         * @return C<sup>j</sup><sub>,λ</sub>
         */
        public T getCjLambda(final int j) {
            if (j < 1 || j >= jMax) {
                return zero;
            }
            return cjlambda[j];
        }

        /** Get the coefficient S<sup>j</sup><sub>,λ</sub>.
         * @param j j index
         * @return S<sup>j</sup><sub>,λ</sub>
         */
        public T getSjLambda(final int j) {
            if (j < 1 || j >= jMax) {
                return zero;
            }
            return sjlambda[j];
        }
    }

    /** This class covers the coefficients e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and their derivatives by h and k.
     *
     * <p>
     * Starting from Danielson 4.2-9,10,11 and taking into account that fact that: <br />
     * c = e / (1 + (1 - e²)<sup>1/2</sup>) = e / (1 + B) = e * b <br/>
     * the expression e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup>
     * can be written as: <br/ >
     * - for |s| > |j| <br />
     * e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> =
     *          (((n + s)!(n - s)!)/((n + j)!(n - j)!)) *
     *          (-b)<sup>|j-s|</sup> *
     *          ((1 - c²)<sup>n-|s|</sup>/(1 + c²)<sup>n</sup>) *
     *          P<sub>n-|s|</sub><sup>|j-s|, |j+s|</sup>(χ) <br />
     * <br />
     * - for |s| <= |j| <br />
     * e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> =
     *          (-b)<sup>|j-s|</sup> *
     *          ((1 - c²)<sup>n-|j|</sup>/(1 + c²)<sup>n</sup>) *
     *          P<sub>n-|j|</sub><sup>|j-s|, |j+s|</sup>(χ)
     * </p>
     *
     * @author Lucian Barbulescu
     */
    private class WnsjEtomjmsCoefficient {

        /** The value c.
         * <p>
         *  c = e / (1 + (1 - e²)<sup>1/2</sup>) = e / (1 + B) = e * b <br/>
         * </p>
         *  */
        private final double c;

        /** db / dh. */
        private final double dbdh;

        /** db / dk. */
        private final double dbdk;

        /** dc / dh = e * db/dh + b * de/dh. */
        private final double dcdh;

        /** dc / dk = e * db/dk + b * de/dk. */
        private final double dcdk;

        /** The values (1 - c²)<sup>n</sup>. <br />
         * The maximum possible value for the power is N + 1 */
        private final double[] omc2tn;

        /** The values (1 + c²)<sup>n</sup>. <br />
         * The maximum possible value for the power is N + 1 */
        private final double[] opc2tn;

        /** The values b<sup>|j-s|</sup>. */
        private final double[] btjms;

        /**
         * Standard constructor.
         * @param context container for attributes
         */
        WnsjEtomjmsCoefficient(final DSSTThirdBodyDynamicContext context) {

            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            //initialise fields
            c = auxiliaryElements.getEcc() * context.getb();
            final double c2 = c * c;

            //b² * χ
            final double b2Chi = context.getb() * context.getb() * context.getX();
            //Compute derivatives of b
            dbdh = auxiliaryElements.getH() * b2Chi;
            dbdk = auxiliaryElements.getK() * b2Chi;

            //Compute derivatives of c
            if (auxiliaryElements.getEcc() == 0.0) {
                // we are at a perfectly circular orbit singularity here
                // we arbitrarily consider the perigee is along the X axis,
                // i.e cos(ω + Ω) = h/ecc 1 and sin(ω + Ω) = k/ecc = 0
                dcdh = auxiliaryElements.getEcc() * dbdh + context.getb();
                dcdk = auxiliaryElements.getEcc() * dbdk;
            } else {
                dcdh = auxiliaryElements.getEcc() * dbdh + context.getb() * auxiliaryElements.getH() / auxiliaryElements.getEcc();
                dcdk = auxiliaryElements.getEcc() * dbdk + context.getb() * auxiliaryElements.getK() / auxiliaryElements.getEcc();
            }

            //Compute the powers (1 - c²)<sup>n</sup> and (1 + c²)<sup>n</sup>
            omc2tn = new double[staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF() + 2];
            opc2tn = new double[staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF() + 2];
            final double omc2 = 1. - c2;
            final double opc2 = 1. + c2;
            omc2tn[0] = 1.;
            opc2tn[0] = 1.;
            for (int i = 1; i <= staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF() + 1; i++) {
                omc2tn[i] = omc2tn[i - 1] * omc2;
                opc2tn[i] = opc2tn[i - 1] * opc2;
            }

            //Compute the powers of b
            btjms = new double[staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF() + 1];
            btjms[0] = 1.;
            for (int i = 1; i <= staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF(); i++) {
                btjms[i] = btjms[i - 1] * context.getb();
            }
        }

        /** Compute the value of the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives by h and k. <br />
         *
         * @param j j index
         * @param s s index
         * @param n n index
         * @param context container for attributes
         * @return an array containing the value of the coefficient at index 0, the derivative by k at index 1 and the derivative by h at index 2
         */
        public double[] computeWjnsEmjmsAndDeriv(final int j, final int s, final int n, final DSSTThirdBodyDynamicContext context) {
            final double[] wjnsemjms = new double[] {0., 0., 0.};

            // |j|
            final int absJ = FastMath.abs(j);
            // |s|
            final int absS = FastMath.abs(s);
            // |j - s|
            final int absJmS = FastMath.abs(j - s);
            // |j + s|
            final int absJpS = FastMath.abs(j + s);

            //The lower index of P. Also the power of (1 - c²)
            final int l;
            // the factorial ratio coefficient or 1. if |s| <= |j|
            final double factCoef;
            if (absS > absJ) {
                //factCoef = (fact[n + s] / fact[n + j]) * (fact[n - s] / fact[n - j]);
                factCoef = (CombinatoricsUtils.factorialDouble(n + s) / CombinatoricsUtils.factorialDouble(n + j)) * (CombinatoricsUtils.factorialDouble(n - s) / CombinatoricsUtils.factorialDouble(n - j));
                l = n - absS;
            } else {
                factCoef = 1.;
                l = n - absJ;
            }

            // (-1)<sup>|j-s|</sup>
            final double sign = absJmS % 2 != 0 ? -1. : 1.;
            //(1 - c²)<sup>n-|s|</sup> / (1 + c²)<sup>n</sup>
            final double coef1 = omc2tn[l] / opc2tn[n];
            //-b<sup>|j-s|</sup>
            final double coef2 = sign * btjms[absJmS];
            // P<sub>l</sub><sup>|j-s|, |j+s|</sup>(χ)
            // Jacobi polynomial value (0) and first-order derivative (1)
            final double[] jac = JacobiPolynomials.getValueAndDerivative(l, absJmS, absJpS, context.getX());

            // the derivative of coef1 by c
            final double dcoef1dc = -coef1 * 2. * c * (((double) n) / opc2tn[1] + ((double) l) / omc2tn[1]);
            // the derivative of coef1 by h
            final double dcoef1dh = dcoef1dc * dcdh;
            // the derivative of coef1 by k
            final double dcoef1dk = dcoef1dc * dcdk;

            // the derivative of coef2 by b
            final double dcoef2db = absJmS == 0 ? 0 : sign * (double) absJmS * btjms[absJmS - 1];
            // the derivative of coef2 by h
            final double dcoef2dh = dcoef2db * dbdh;
            // the derivative of coef2 by k
            final double dcoef2dk = dcoef2db * dbdk;

            // the jacobi polynomial value
            // final double jacobi = jac.getValue();
            final double jacobi = jac[0];
            // the derivative of the Jacobi polynomial by h
            //final double djacobidh = jac.getGradient()[0] * context.getHXXX();
            final double djacobidh = jac[1] * context.getHXXX();
            // the derivative of the Jacobi polynomial by k
            //final double djacobidk = jac.getGradient()[0] * context.getKXXX();
            final double djacobidk = jac[1] * context.getKXXX();

            //group the above coefficients to limit the mathematical operations
            final double term1 = factCoef * coef1 * coef2;
            final double term2 = factCoef * coef1 * jacobi;
            final double term3 = factCoef * coef2 * jacobi;

            //compute e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives by k and h
            wjnsemjms[0] = term1 * jacobi;
            wjnsemjms[1] = dcoef1dk * term3 + dcoef2dk * term2 + djacobidk * term1;
            wjnsemjms[2] = dcoef1dh * term3 + dcoef2dh * term2 + djacobidh * term1;

            return wjnsemjms;
        }
    }

    /** This class covers the coefficients e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and their derivatives by h and k.
    *
    * <p>
    * Starting from Danielson 4.2-9,10,11 and taking into account that fact that: <br />
    * c = e / (1 + (1 - e²)<sup>1/2</sup>) = e / (1 + B) = e * b <br/>
    * the expression e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup>
    * can be written as: <br/ >
    * - for |s| > |j| <br />
    * e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> =
    *          (((n + s)!(n - s)!)/((n + j)!(n - j)!)) *
    *          (-b)<sup>|j-s|</sup> *
    *          ((1 - c²)<sup>n-|s|</sup>/(1 + c²)<sup>n</sup>) *
    *          P<sub>n-|s|</sub><sup>|j-s|, |j+s|</sup>(χ) <br />
    * <br />
    * - for |s| <= |j| <br />
    * e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> =
    *          (-b)<sup>|j-s|</sup> *
    *          ((1 - c²)<sup>n-|j|</sup>/(1 + c²)<sup>n</sup>) *
    *          P<sub>n-|j|</sub><sup>|j-s|, |j+s|</sup>(χ)
    * </p>
    *
    * @author Lucian Barbulescu
    */
    private class FieldWnsjEtomjmsCoefficient <T extends CalculusFieldElement<T>> {

        /** The value c.
         * <p>
         *  c = e / (1 + (1 - e²)<sup>1/2</sup>) = e / (1 + B) = e * b <br/>
         * </p>
         *  */
        private final T c;

        /** db / dh. */
        private final T dbdh;

        /** db / dk. */
        private final T dbdk;

        /** dc / dh = e * db/dh + b * de/dh. */
        private final T dcdh;

        /** dc / dk = e * db/dk + b * de/dk. */
        private final T dcdk;

        /** The values (1 - c²)<sup>n</sup>. <br />
         * The maximum possible value for the power is N + 1 */
        private final T[] omc2tn;

        /** The values (1 + c²)<sup>n</sup>. <br />
         * The maximum possible value for the power is N + 1 */
        private final T[] opc2tn;

        /** The values b<sup>|j-s|</sup>. */
        private final T[] btjms;

        /**
         * Standard constructor.
         * @param context container for attributes
         * @param field field used by default
         */
        FieldWnsjEtomjmsCoefficient(final FieldDSSTThirdBodyDynamicContext<T> context, final Field<T> field) {

            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            //Zero
            final T zero = field.getZero();

            //initialise fields
            c = auxiliaryElements.getEcc().multiply(context.getb());
            final T c2 = c.multiply(c);

            //b² * χ
            final T b2Chi = context.getb().multiply(context.getb()).multiply(context.getX());
            //Compute derivatives of b
            dbdh = auxiliaryElements.getH().multiply(b2Chi);
            dbdk = auxiliaryElements.getK().multiply(b2Chi);

            //Compute derivatives of c
            if (auxiliaryElements.getEcc().getReal() == 0.0) {
                // we are at a perfectly circular orbit singularity here
                // we arbitrarily consider the perigee is along the X axis,
                // i.e cos(ω + Ω) = h/ecc 1 and sin(ω + Ω) = k/ecc = 0
                dcdh = auxiliaryElements.getEcc().multiply(dbdh).add(context.getb());
                dcdk = auxiliaryElements.getEcc().multiply(dbdk);
            } else {
                dcdh = auxiliaryElements.getEcc().multiply(dbdh).add(context.getb().multiply(auxiliaryElements.getH()).divide(auxiliaryElements.getEcc()));
                dcdk = auxiliaryElements.getEcc().multiply(dbdk).add(context.getb().multiply(auxiliaryElements.getK()).divide(auxiliaryElements.getEcc()));
            }

            //Compute the powers (1 - c²)<sup>n</sup> and (1 + c²)<sup>n</sup>
            omc2tn = MathArrays.buildArray(field, staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF() + 2);
            opc2tn = MathArrays.buildArray(field, staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF() + 2);
            final T omc2 = c2.negate().add(1.);
            final T opc2 = c2.add(1.);
            omc2tn[0] = zero.add(1.);
            opc2tn[0] = zero.add(1.);
            for (int i = 1; i <= staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF() + 1; i++) {
                omc2tn[i] = omc2tn[i - 1].multiply(omc2);
                opc2tn[i] = opc2tn[i - 1].multiply(opc2);
            }

            //Compute the powers of b
            btjms = MathArrays.buildArray(field, staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF() + 1);
            btjms[0] = zero.add(1.);
            for (int i = 1; i <= staticContext.getMaxAR3Pow() + staticContext.getMaxFreqF(); i++) {
                btjms[i] = btjms[i - 1].multiply(context.getb());
            }
        }

        /** Compute the value of the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives by h and k. <br />
         *
         * @param j j index
         * @param s s index
         * @param n n index
         * @param context container for attributes
         * @param field field used by default
         * @return an array containing the value of the coefficient at index 0, the derivative by k at index 1 and the derivative by h at index 2
         */
        public T[] computeWjnsEmjmsAndDeriv(final int j, final int s, final int n,
                                            final FieldDSSTThirdBodyDynamicContext<T> context,
                                            final Field<T> field) {
            //Zero
            final T zero = field.getZero();

            final T[] wjnsemjms = MathArrays.buildArray(field, 3);
            Arrays.fill(wjnsemjms, zero);

            // |j|
            final int absJ = FastMath.abs(j);
            // |s|
            final int absS = FastMath.abs(s);
            // |j - s|
            final int absJmS = FastMath.abs(j - s);
            // |j + s|
            final int absJpS = FastMath.abs(j + s);

            //The lower index of P. Also the power of (1 - c²)
            final int l;
            // the factorial ratio coefficient or 1. if |s| <= |j|
            final T factCoef;
            if (absS > absJ) {
                //factCoef = (fact[n + s] / fact[n + j]) * (fact[n - s] / fact[n - j]);
                factCoef = zero.add((CombinatoricsUtils.factorialDouble(n + s) / CombinatoricsUtils.factorialDouble(n + j)) * (CombinatoricsUtils.factorialDouble(n - s) / CombinatoricsUtils.factorialDouble(n - j)));
                l = n - absS;
            } else {
                factCoef = zero.add(1.);
                l = n - absJ;
            }

            // (-1)<sup>|j-s|</sup>
            final T sign = absJmS % 2 != 0 ? zero.add(-1.) : zero.add(1.);
            //(1 - c²)<sup>n-|s|</sup> / (1 + c²)<sup>n</sup>
            final T coef1 = omc2tn[l].divide(opc2tn[n]);
            //-b<sup>|j-s|</sup>
            final T coef2 = btjms[absJmS].multiply(sign);
            // P<sub>l</sub><sup>|j-s|, |j+s|</sup>(χ)
            final FieldGradient<T> jac =
                    JacobiPolynomials.getValue(l, absJmS, absJpS, FieldGradient.variable(1, 0, context.getX()));

            // the derivative of coef1 by c
            final T dcoef1dc = coef1.negate().multiply(2.).multiply(c).multiply(opc2tn[1].reciprocal().multiply(n).add(omc2tn[1].reciprocal().multiply(l)));
            // the derivative of coef1 by h
            final T dcoef1dh = dcoef1dc.multiply(dcdh);
            // the derivative of coef1 by k
            final T dcoef1dk = dcoef1dc.multiply(dcdk);

            // the derivative of coef2 by b
            final T dcoef2db = absJmS == 0 ? zero : sign.multiply(absJmS).multiply(btjms[absJmS - 1]);
            // the derivative of coef2 by h
            final T dcoef2dh = dcoef2db.multiply(dbdh);
            // the derivative of coef2 by k
            final T dcoef2dk = dcoef2db.multiply(dbdk);

            // the jacobi polynomial value
            final T jacobi = jac.getValue();
            // the derivative of the Jacobi polynomial by h
            final T djacobidh = jac.getGradient()[0].multiply(context.getHXXX());
            // the derivative of the Jacobi polynomial by k
            final T djacobidk = jac.getGradient()[0].multiply(context.getKXXX());

            //group the above coefficients to limit the mathematical operations
            final T term1 = factCoef.multiply(coef1).multiply(coef2);
            final T term2 = factCoef.multiply(coef1).multiply(jacobi);
            final T term3 = factCoef.multiply(coef2).multiply(jacobi);

            //compute e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives by k and h
            wjnsemjms[0] = term1.multiply(jacobi);
            wjnsemjms[1] = dcoef1dk.multiply(term3).add(dcoef2dk.multiply(term2)).add(djacobidk.multiply(term1));
            wjnsemjms[2] = dcoef1dh.multiply(term3).add(dcoef2dh.multiply(term2)).add(djacobidh.multiply(term1));

            return wjnsemjms;
        }
    }

    /** The G<sub>n,s</sub> coefficients and their derivatives.
     * <p>
     * See Danielson, 4.2-17
     *
     * @author Lucian Barbulescu
     */
    private class GnsCoefficients {

        /** Maximum value for n index. */
        private final int nMax;

        /** Maximum value for s index. */
        private final int sMax;

        /** The coefficients G<sub>n,s</sub>. */
        private final double gns[][];

        /** The derivatives of the coefficients G<sub>n,s</sub> by a. */
        private final double dgnsda[][];

        /** The derivatives of the coefficients G<sub>n,s</sub> by γ. */
        private final double dgnsdgamma[][];

        /** Standard constructor.
         *
         * @param nMax maximum value for n indes
         * @param sMax maximum value for s index
         * @param context container for attributes
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        GnsCoefficients(final int nMax, final int sMax,
                        final DSSTThirdBodyDynamicContext context,
                        final double[] aoR3Pow, final double[][] qns) {
            this.nMax = nMax;
            this.sMax = sMax;

            final int rows    = nMax + 1;
            final int columns = sMax + 1;
            this.gns          = new double[rows][columns];
            this.dgnsda       = new double[rows][columns];
            this.dgnsdgamma   = new double[rows][columns];

            // Generate the coefficients
            generateCoefficients(context, aoR3Pow, qns);
        }
        /**
         * Compute the coefficient G<sub>n,s</sub> and its derivatives.
         * <p>
         * Only the derivatives by a and γ are computed as all others are 0
         * </p>
         * @param context container for attributes
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        private void generateCoefficients(final DSSTThirdBodyDynamicContext context, final double[] aoR3Pow, final double[][] qns) {

            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            for (int s = 0; s <= sMax; s++) {
                // The n index is always at least the maximum between 2 and s
                final int minN = FastMath.max(2, s);
                for (int n = minN; n <= nMax; n++) {
                    // compute the coefficients only if (n - s) % 2 == 0
                    if ( (n - s) % 2 == 0 ) {
                        // Kronecker symbol (2 - delta(0,s))
                        final double delta0s = (s == 0) ? 1. : 2.;
                        final double vns   = Vns.get(new NSKey(n, s));
                        final double coef0 = delta0s * aoR3Pow[n] * vns * context.getMuoR3();
                        final double coef1 = coef0 * qns[n][s];
                        // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                        // for n = s, Q(n, n + 1) = 0. (Cefola & Broucke, 1975)
                        final double dqns = (n == s) ? 0. : qns[n][s + 1];

                        //Compute the coefficient and its derivatives.
                        this.gns[n][s] = coef1;
                        this.dgnsda[n][s] = coef1 * n / auxiliaryElements.getSma();
                        this.dgnsdgamma[n][s] = coef0 * dqns;
                    } else {
                        // the coefficient and its derivatives is 0
                        this.gns[n][s] = 0.;
                        this.dgnsda[n][s] = 0.;
                        this.dgnsdgamma[n][s] = 0.;
                    }
                }
            }
        }

        /** Get the coefficient G<sub>n,s</sub>.
         *
         * @param n n index
         * @param s s index
         * @return the coefficient G<sub>n,s</sub>
         */
        public double getGns(final int n, final int s) {
            return this.gns[n][s];
        }

        /** Get the derivative dG<sub>n,s</sub> / da.
         *
         * @param n n index
         * @param s s index
         * @return the derivative dG<sub>n,s</sub> / da
         */
        public double getdGnsda(final int n, final int s) {
            return this.dgnsda[n][s];
        }

        /** Get the derivative dG<sub>n,s</sub> / dγ.
         *
         * @param n n index
         * @param s s index
         * @return the derivative dG<sub>n,s</sub> / dγ
         */
        public double getdGnsdgamma(final int n, final int s) {
            return this.dgnsdgamma[n][s];
        }
    }

    /** The G<sub>n,s</sub> coefficients and their derivatives.
     * <p>
     * See Danielson, 4.2-17
     *
     * @author Lucian Barbulescu
     */
    private class FieldGnsCoefficients  <T extends CalculusFieldElement<T>> {

        /** Maximum value for n index. */
        private final int nMax;

        /** Maximum value for s index. */
        private final int sMax;

        /** The coefficients G<sub>n,s</sub>. */
        private final T gns[][];

        /** The derivatives of the coefficients G<sub>n,s</sub> by a. */
        private final T dgnsda[][];

        /** The derivatives of the coefficients G<sub>n,s</sub> by γ. */
        private final T dgnsdgamma[][];

        /** Standard constructor.
         *
         * @param nMax maximum value for n indes
         * @param sMax maximum value for s index
         * @param context container for attributes
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         * @param field field used by default
         */
        FieldGnsCoefficients(final int nMax, final int sMax,
                             final FieldDSSTThirdBodyDynamicContext<T> context,
                             final T[] aoR3Pow, final T[][] qns,
                             final Field<T> field) {
            this.nMax = nMax;
            this.sMax = sMax;

            final int rows    = nMax + 1;
            final int columns = sMax + 1;
            this.gns          = MathArrays.buildArray(field, rows, columns);
            this.dgnsda       = MathArrays.buildArray(field, rows, columns);
            this.dgnsdgamma   = MathArrays.buildArray(field, rows, columns);

            // Generate the coefficients
            generateCoefficients(context, aoR3Pow, qns, field);
        }
        /**
         * Compute the coefficient G<sub>n,s</sub> and its derivatives.
         * <p>
         * Only the derivatives by a and γ are computed as all others are 0
         * </p>
         * @param context container for attributes
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         * @param field field used by default
         */
        private void generateCoefficients(final FieldDSSTThirdBodyDynamicContext<T> context,
                                          final T[] aoR3Pow, final T[][] qns,
                                          final Field<T> field) {

            //Zero
            final T zero = field.getZero();

            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            for (int s = 0; s <= sMax; s++) {
                // The n index is always at least the maximum between 2 and s
                final int minN = FastMath.max(2, s);
                for (int n = minN; n <= nMax; n++) {
                    // compute the coefficients only if (n - s) % 2 == 0
                    if ( (n - s) % 2 == 0 ) {
                        // Kronecker symbol (2 - delta(0,s))
                        final T delta0s = (s == 0) ? zero.add(1.) : zero.add(2.);
                        final double vns = Vns.get(new NSKey(n, s));
                        final T coef0 = aoR3Pow[n].multiply(vns).multiply(context.getMuoR3()).multiply(delta0s);
                        final T coef1 = coef0.multiply(qns[n][s]);
                        // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                        // for n = s, Q(n, n + 1) = 0. (Cefola & Broucke, 1975)
                        final T dqns = (n == s) ? zero : qns[n][s + 1];

                        //Compute the coefficient and its derivatives.
                        this.gns[n][s] = coef1;
                        this.dgnsda[n][s] = coef1.multiply(n).divide(auxiliaryElements.getSma());
                        this.dgnsdgamma[n][s] = coef0.multiply(dqns);
                    } else {
                        // the coefficient and its derivatives is 0
                        this.gns[n][s] = zero;
                        this.dgnsda[n][s] = zero;
                        this.dgnsdgamma[n][s] = zero;
                    }
                }
            }
        }

        /** Get the coefficient G<sub>n,s</sub>.
         *
         * @param n n index
         * @param s s index
         * @return the coefficient G<sub>n,s</sub>
         */
        public T getGns(final int n, final int s) {
            return this.gns[n][s];
        }

        /** Get the derivative dG<sub>n,s</sub> / da.
         *
         * @param n n index
         * @param s s index
         * @return the derivative dG<sub>n,s</sub> / da
         */
        public T getdGnsda(final int n, final int s) {
            return this.dgnsda[n][s];
        }

        /** Get the derivative dG<sub>n,s</sub> / dγ.
         *
         * @param n n index
         * @param s s index
         * @return the derivative dG<sub>n,s</sub> / dγ
         */
        public T getdGnsdgamma(final int n, final int s) {
            return this.dgnsdgamma[n][s];
        }
    }

    /** This class computes the terms containing the coefficients C<sub>j</sub> and S<sub>j</sub> of (α, β) or (k, h).
     *
     * <p>
     * The following terms and their derivatives by k, h, alpha and beta are considered: <br/ >
     * - sign(j-s) * C<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) + S<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) - S<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) - sign(j-s) * S<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) + S<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) <br />
     * For the ease of usage the above terms are renamed A<sub>js</sub>, B<sub>js</sub>, D<sub>js</sub> and E<sub>js</sub> respectively <br />
     * See the CS Mathematical report $3.5.3.2 for more details
     * </p>
     * @author Lucian Barbulescu
     */
    private static class CjSjAlphaBetaKH {

        /** The C<sub>j</sub>(k, h) and the S<sub>j</sub>(k, h) series. */
        private final CjSjCoefficient cjsjkh;

        /** The C<sub>j</sub>(α, β) and the S<sub>j</sub>(α, β) series. */
        private final CjSjCoefficient cjsjalbe;

        /** The coeficient sign(j-s) * C<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) + S<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final double coefAandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) - S<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final double coefBandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) - sign(j-s) * S<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final double coefDandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) + S<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final double coefEandDeriv[];

        /**
         * Standard constructor.
         * @param context container for attributes
         */
        CjSjAlphaBetaKH(final DSSTThirdBodyDynamicContext context) {

            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            cjsjkh = new CjSjCoefficient(auxiliaryElements.getK(), auxiliaryElements.getH());
            cjsjalbe = new CjSjCoefficient(context.getAlpha(), context.getBeta());

            coefAandDeriv = new double[5];
            coefBandDeriv = new double[5];
            coefDandDeriv = new double[5];
            coefEandDeriv = new double[5];
        }

        /** Compute the coefficients and their derivatives for a given (j,s) pair.
         * @param j j index
         * @param s s index
         */
        public void computeCoefficients(final int j, final int s) {
            // sign of j-s
            final int sign = j < s ? -1 : 1;

            //|j-s|
            final int absJmS = FastMath.abs(j - s);

            //j+s
            final int jps = j + s;

            //Compute the coefficient A and its derivatives
            coefAandDeriv[0] = sign * cjsjalbe.getCj(s) * cjsjkh.getSj(absJmS) + cjsjalbe.getSj(s) * cjsjkh.getCj(absJmS);
            coefAandDeriv[1] = sign * cjsjalbe.getCj(s) * cjsjkh.getDsjDk(absJmS) + cjsjalbe.getSj(s) * cjsjkh.getDcjDk(absJmS);
            coefAandDeriv[2] = sign * cjsjalbe.getCj(s) * cjsjkh.getDsjDh(absJmS) + cjsjalbe.getSj(s) * cjsjkh.getDcjDh(absJmS);
            coefAandDeriv[3] = sign * cjsjalbe.getDcjDk(s) * cjsjkh.getSj(absJmS) + cjsjalbe.getDsjDk(s) * cjsjkh.getCj(absJmS);
            coefAandDeriv[4] = sign * cjsjalbe.getDcjDh(s) * cjsjkh.getSj(absJmS) + cjsjalbe.getDsjDh(s) * cjsjkh.getCj(absJmS);

            //Compute the coefficient B and its derivatives
            coefBandDeriv[0] = cjsjalbe.getCj(s) * cjsjkh.getSj(jps) - cjsjalbe.getSj(s) * cjsjkh.getCj(jps);
            coefBandDeriv[1] = cjsjalbe.getCj(s) * cjsjkh.getDsjDk(jps) - cjsjalbe.getSj(s) * cjsjkh.getDcjDk(jps);
            coefBandDeriv[2] = cjsjalbe.getCj(s) * cjsjkh.getDsjDh(jps) - cjsjalbe.getSj(s) * cjsjkh.getDcjDh(jps);
            coefBandDeriv[3] = cjsjalbe.getDcjDk(s) * cjsjkh.getSj(jps) - cjsjalbe.getDsjDk(s) * cjsjkh.getCj(jps);
            coefBandDeriv[4] = cjsjalbe.getDcjDh(s) * cjsjkh.getSj(jps) - cjsjalbe.getDsjDh(s) * cjsjkh.getCj(jps);

            //Compute the coefficient D and its derivatives
            coefDandDeriv[0] = cjsjalbe.getCj(s) * cjsjkh.getCj(absJmS) - sign * cjsjalbe.getSj(s) * cjsjkh.getSj(absJmS);
            coefDandDeriv[1] = cjsjalbe.getCj(s) * cjsjkh.getDcjDk(absJmS) - sign * cjsjalbe.getSj(s) * cjsjkh.getDsjDk(absJmS);
            coefDandDeriv[2] = cjsjalbe.getCj(s) * cjsjkh.getDcjDh(absJmS) - sign * cjsjalbe.getSj(s) * cjsjkh.getDsjDh(absJmS);
            coefDandDeriv[3] = cjsjalbe.getDcjDk(s) * cjsjkh.getCj(absJmS) - sign * cjsjalbe.getDsjDk(s) * cjsjkh.getSj(absJmS);
            coefDandDeriv[4] = cjsjalbe.getDcjDh(s) * cjsjkh.getCj(absJmS) - sign * cjsjalbe.getDsjDh(s) * cjsjkh.getSj(absJmS);

            //Compute the coefficient E and its derivatives
            coefEandDeriv[0] = cjsjalbe.getCj(s) * cjsjkh.getCj(jps) + cjsjalbe.getSj(s) * cjsjkh.getSj(jps);
            coefEandDeriv[1] = cjsjalbe.getCj(s) * cjsjkh.getDcjDk(jps) + cjsjalbe.getSj(s) * cjsjkh.getDsjDk(jps);
            coefEandDeriv[2] = cjsjalbe.getCj(s) * cjsjkh.getDcjDh(jps) + cjsjalbe.getSj(s) * cjsjkh.getDsjDh(jps);
            coefEandDeriv[3] = cjsjalbe.getDcjDk(s) * cjsjkh.getCj(jps) + cjsjalbe.getDsjDk(s) * cjsjkh.getSj(jps);
            coefEandDeriv[4] = cjsjalbe.getDcjDh(s) * cjsjkh.getCj(jps) + cjsjalbe.getDsjDh(s) * cjsjkh.getSj(jps);
        }

        /** Get the value of coefficient A<sub>j,s</sub>.
         *
         * @return the coefficient A<sub>j,s</sub>
         */
        public double getCoefA() {
            return coefAandDeriv[0];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dk.
         *
         * @return the coefficient dA<sub>j,s</sub>/dk
         */
        public double getdCoefAdk() {
            return coefAandDeriv[1];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dh.
         *
         * @return the coefficient dA<sub>j,s</sub>/dh
         */
        public double getdCoefAdh() {
            return coefAandDeriv[2];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dα.
         *
         * @return the coefficient dA<sub>j,s</sub>/dα
         */
        public double getdCoefAdalpha() {
            return coefAandDeriv[3];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dA<sub>j,s</sub>/dβ
         */
        public double getdCoefAdbeta() {
            return coefAandDeriv[4];
        }

        /** Get the value of coefficient B<sub>j,s</sub>.
         *
         * @return the coefficient B<sub>j,s</sub>
         */
        public double getCoefB() {
            return coefBandDeriv[0];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dk.
         *
         * @return the coefficient dB<sub>j,s</sub>/dk
         */
        public double getdCoefBdk() {
            return coefBandDeriv[1];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dh.
         *
         * @return the coefficient dB<sub>j,s</sub>/dh
         */
        public double getdCoefBdh() {
            return coefBandDeriv[2];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dα.
         *
         * @return the coefficient dB<sub>j,s</sub>/dα
         */
        public double getdCoefBdalpha() {
            return coefBandDeriv[3];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dB<sub>j,s</sub>/dβ
         */
        public double getdCoefBdbeta() {
            return coefBandDeriv[4];
        }

        /** Get the value of coefficient D<sub>j,s</sub>.
         *
         * @return the coefficient D<sub>j,s</sub>
         */
        public double getCoefD() {
            return coefDandDeriv[0];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dk.
         *
         * @return the coefficient dD<sub>j,s</sub>/dk
         */
        public double getdCoefDdk() {
            return coefDandDeriv[1];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dh.
         *
         * @return the coefficient dD<sub>j,s</sub>/dh
         */
        public double getdCoefDdh() {
            return coefDandDeriv[2];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dα.
         *
         * @return the coefficient dD<sub>j,s</sub>/dα
         */
        public double getdCoefDdalpha() {
            return coefDandDeriv[3];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dD<sub>j,s</sub>/dβ
         */
        public double getdCoefDdbeta() {
            return coefDandDeriv[4];
        }

        /** Get the value of coefficient E<sub>j,s</sub>.
         *
         * @return the coefficient E<sub>j,s</sub>
         */
        public double getCoefE() {
            return coefEandDeriv[0];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dk.
         *
         * @return the coefficient dE<sub>j,s</sub>/dk
         */
        public double getdCoefEdk() {
            return coefEandDeriv[1];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dh.
         *
         * @return the coefficient dE<sub>j,s</sub>/dh
         */
        public double getdCoefEdh() {
            return coefEandDeriv[2];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dα.
         *
         * @return the coefficient dE<sub>j,s</sub>/dα
         */
        public double getdCoefEdalpha() {
            return coefEandDeriv[3];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dE<sub>j,s</sub>/dβ
         */
        public double getdCoefEdbeta() {
            return coefEandDeriv[4];
        }
    }

     /** This class computes the terms containing the coefficients C<sub>j</sub> and S<sub>j</sub> of (α, β) or (k, h).
     *
     * <p>
     * The following terms and their derivatives by k, h, alpha and beta are considered: <br/ >
     * - sign(j-s) * C<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) + S<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) - S<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) - sign(j-s) * S<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) + S<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) <br />
     * For the ease of usage the above terms are renamed A<sub>js</sub>, B<sub>js</sub>, D<sub>js</sub> and E<sub>js</sub> respectively <br />
     * See the CS Mathematical report $3.5.3.2 for more details
     * </p>
     * @author Lucian Barbulescu
     */
    private static class FieldCjSjAlphaBetaKH <T extends CalculusFieldElement<T>> {

        /** The C<sub>j</sub>(k, h) and the S<sub>j</sub>(k, h) series. */
        private final FieldCjSjCoefficient<T> cjsjkh;

        /** The C<sub>j</sub>(α, β) and the S<sub>j</sub>(α, β) series. */
        private final FieldCjSjCoefficient<T> cjsjalbe;

        /** The coeficient sign(j-s) * C<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) + S<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final T coefAandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) - S<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final T coefBandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) - sign(j-s) * S<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final T coefDandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) + S<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final T coefEandDeriv[];

        /**
         * Standard constructor.
         * @param context container for attributes
         * @param field field used by default
         */
        FieldCjSjAlphaBetaKH(final FieldDSSTThirdBodyDynamicContext<T> context, final Field<T> field) {

            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            cjsjkh   = new FieldCjSjCoefficient<>(auxiliaryElements.getK(), auxiliaryElements.getH(), field);
            cjsjalbe = new FieldCjSjCoefficient<>(context.getAlpha(), context.getBeta(), field);

            coefAandDeriv = MathArrays.buildArray(field, 5);
            coefBandDeriv = MathArrays.buildArray(field, 5);
            coefDandDeriv = MathArrays.buildArray(field, 5);
            coefEandDeriv = MathArrays.buildArray(field, 5);
        }

        /** Compute the coefficients and their derivatives for a given (j,s) pair.
         * @param j j index
         * @param s s index
         */
        public void computeCoefficients(final int j, final int s) {
            // sign of j-s
            final int sign = j < s ? -1 : 1;

            //|j-s|
            final int absJmS = FastMath.abs(j - s);

            //j+s
            final int jps = j + s;

            //Compute the coefficient A and its derivatives
            coefAandDeriv[0] = cjsjalbe.getCj(s).multiply(cjsjkh.getSj(absJmS)).multiply(sign).add(cjsjalbe.getSj(s).multiply(cjsjkh.getCj(absJmS)));
            coefAandDeriv[1] = cjsjalbe.getCj(s).multiply(cjsjkh.getDsjDk(absJmS)).multiply(sign).add(cjsjalbe.getSj(s).multiply(cjsjkh.getDcjDk(absJmS)));
            coefAandDeriv[2] = cjsjalbe.getCj(s).multiply(cjsjkh.getDsjDh(absJmS)).multiply(sign).add(cjsjalbe.getSj(s).multiply(cjsjkh.getDcjDh(absJmS)));
            coefAandDeriv[3] = cjsjalbe.getDcjDk(s).multiply(cjsjkh.getSj(absJmS)).multiply(sign).add(cjsjalbe.getDsjDk(s).multiply(cjsjkh.getCj(absJmS)));
            coefAandDeriv[4] = cjsjalbe.getDcjDh(s).multiply(cjsjkh.getSj(absJmS)).multiply(sign).add(cjsjalbe.getDsjDh(s).multiply(cjsjkh.getCj(absJmS)));

            //Compute the coefficient B and its derivatives
            coefBandDeriv[0] = cjsjalbe.getCj(s).multiply(cjsjkh.getSj(jps)).subtract(cjsjalbe.getSj(s).multiply(cjsjkh.getCj(jps)));
            coefBandDeriv[1] = cjsjalbe.getCj(s).multiply(cjsjkh.getDsjDk(jps)).subtract(cjsjalbe.getSj(s).multiply(cjsjkh.getDcjDk(jps)));
            coefBandDeriv[2] = cjsjalbe.getCj(s).multiply(cjsjkh.getDsjDh(jps)).subtract(cjsjalbe.getSj(s).multiply(cjsjkh.getDcjDh(jps)));
            coefBandDeriv[3] = cjsjalbe.getDcjDk(s).multiply(cjsjkh.getSj(jps)).subtract(cjsjalbe.getDsjDk(s).multiply(cjsjkh.getCj(jps)));
            coefBandDeriv[4] = cjsjalbe.getDcjDh(s).multiply(cjsjkh.getSj(jps)).subtract(cjsjalbe.getDsjDh(s).multiply(cjsjkh.getCj(jps)));

            //Compute the coefficient D and its derivatives
            coefDandDeriv[0] = cjsjalbe.getCj(s).multiply(cjsjkh.getCj(absJmS)).subtract(cjsjalbe.getSj(s).multiply(cjsjkh.getSj(absJmS)).multiply(sign));
            coefDandDeriv[1] = cjsjalbe.getCj(s).multiply(cjsjkh.getDcjDk(absJmS)).subtract(cjsjalbe.getSj(s).multiply(cjsjkh.getDsjDk(absJmS)).multiply(sign));
            coefDandDeriv[2] = cjsjalbe.getCj(s).multiply(cjsjkh.getDcjDh(absJmS)).subtract(cjsjalbe.getSj(s).multiply(cjsjkh.getDsjDh(absJmS)).multiply(sign));
            coefDandDeriv[3] = cjsjalbe.getDcjDk(s).multiply(cjsjkh.getCj(absJmS)).subtract(cjsjalbe.getDsjDk(s).multiply(cjsjkh.getSj(absJmS)).multiply(sign));
            coefDandDeriv[4] = cjsjalbe.getDcjDh(s).multiply(cjsjkh.getCj(absJmS)).subtract(cjsjalbe.getDsjDh(s).multiply(cjsjkh.getSj(absJmS)).multiply(sign));

            //Compute the coefficient E and its derivatives
            coefEandDeriv[0] = cjsjalbe.getCj(s).multiply(cjsjkh.getCj(jps)).add(cjsjalbe.getSj(s).multiply(cjsjkh.getSj(jps)));
            coefEandDeriv[1] = cjsjalbe.getCj(s).multiply(cjsjkh.getDcjDk(jps)).add(cjsjalbe.getSj(s).multiply(cjsjkh.getDsjDk(jps)));
            coefEandDeriv[2] = cjsjalbe.getCj(s).multiply(cjsjkh.getDcjDh(jps)).add(cjsjalbe.getSj(s).multiply(cjsjkh.getDsjDh(jps)));
            coefEandDeriv[3] = cjsjalbe.getDcjDk(s).multiply(cjsjkh.getCj(jps)).add(cjsjalbe.getDsjDk(s).multiply(cjsjkh.getSj(jps)));
            coefEandDeriv[4] = cjsjalbe.getDcjDh(s).multiply(cjsjkh.getCj(jps)).add(cjsjalbe.getDsjDh(s).multiply(cjsjkh.getSj(jps)));
        }

        /** Get the value of coefficient A<sub>j,s</sub>.
         *
         * @return the coefficient A<sub>j,s</sub>
         */
        public T getCoefA() {
            return coefAandDeriv[0];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dk.
         *
         * @return the coefficient dA<sub>j,s</sub>/dk
         */
        public T getdCoefAdk() {
            return coefAandDeriv[1];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dh.
         *
         * @return the coefficient dA<sub>j,s</sub>/dh
         */
        public T getdCoefAdh() {
            return coefAandDeriv[2];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dα.
         *
         * @return the coefficient dA<sub>j,s</sub>/dα
         */
        public T getdCoefAdalpha() {
            return coefAandDeriv[3];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dA<sub>j,s</sub>/dβ
         */
        public T getdCoefAdbeta() {
            return coefAandDeriv[4];
        }

       /** Get the value of coefficient B<sub>j,s</sub>.
        *
        * @return the coefficient B<sub>j,s</sub>
        */
        public T getCoefB() {
            return coefBandDeriv[0];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dk.
         *
         * @return the coefficient dB<sub>j,s</sub>/dk
         */
        public T getdCoefBdk() {
            return coefBandDeriv[1];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dh.
         *
         * @return the coefficient dB<sub>j,s</sub>/dh
         */
        public T getdCoefBdh() {
            return coefBandDeriv[2];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dα.
         *
         * @return the coefficient dB<sub>j,s</sub>/dα
         */
        public T getdCoefBdalpha() {
            return coefBandDeriv[3];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dB<sub>j,s</sub>/dβ
         */
        public T getdCoefBdbeta() {
            return coefBandDeriv[4];
        }

        /** Get the value of coefficient D<sub>j,s</sub>.
         *
         * @return the coefficient D<sub>j,s</sub>
         */
        public T getCoefD() {
            return coefDandDeriv[0];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dk.
         *
         * @return the coefficient dD<sub>j,s</sub>/dk
         */
        public T getdCoefDdk() {
            return coefDandDeriv[1];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dh.
         *
         * @return the coefficient dD<sub>j,s</sub>/dh
         */
        public T getdCoefDdh() {
            return coefDandDeriv[2];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dα.
         *
         * @return the coefficient dD<sub>j,s</sub>/dα
         */
        public T getdCoefDdalpha() {
            return coefDandDeriv[3];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dD<sub>j,s</sub>/dβ
         */
        public T getdCoefDdbeta() {
            return coefDandDeriv[4];
        }

        /** Get the value of coefficient E<sub>j,s</sub>.
         *
         * @return the coefficient E<sub>j,s</sub>
         */
        public T getCoefE() {
            return coefEandDeriv[0];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dk.
         *
         * @return the coefficient dE<sub>j,s</sub>/dk
         */
        public T getdCoefEdk() {
            return coefEandDeriv[1];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dh.
         *
         * @return the coefficient dE<sub>j,s</sub>/dh
         */
        public T getdCoefEdh() {
            return coefEandDeriv[2];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dα.
         *
         * @return the coefficient dE<sub>j,s</sub>/dα
         */
        public T getdCoefEdalpha() {
            return coefEandDeriv[3];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dE<sub>j,s</sub>/dβ
         */
        public T getdCoefEdbeta() {
            return coefEandDeriv[4];
        }
    }

    /** This class computes the coefficients for the generating function S and its derivatives.
     * <p>
     * The form of the generating functions is: <br>
     *  S = C⁰ + &Sigma;<sub>j=1</sub><sup>N+1</sup>(C<sup>j</sup> * cos(jF) + S<sup>j</sup> * sin(jF)) <br>
     *  The coefficients C⁰, C<sup>j</sup>, S<sup>j</sup> are the Fourrier coefficients
     *  presented in Danielson 4.2-14,15 except for the case j=1 where
     *  C¹ = C¹<sub>Fourier</sub> - hU and
     *  S¹ = S¹<sub>Fourier</sub> + kU <br>
     *  Also the coefficients of the derivatives of S by a, k, h, α, β, γ and λ
     *  are computed end expressed in a similar manner. The formulas used are 4.2-19, 20, 23, 24
     * </p>
     * @author Lucian Barbulescu
     */
    private class GeneratingFunctionCoefficients {

        /** The Fourier coefficients as presented in Danielson 4.2-14,15. */
        private final FourierCjSjCoefficients cjsjFourier;

        /** Maximum value of j index. */
        private final int jMax;

        /** The coefficients C<sup>j</sup> of the function S and its derivatives.
         * <p>
         * The index j belongs to the interval [0,jMax]. The coefficient C⁰ is the free coefficient.<br>
         * Each column of the matrix contains the coefficient corresponding to the following functions: <br/>
         * - S <br/>
         * - dS / da <br/>
         * - dS / dk <br/>
         * - dS / dh <br/>
         * - dS / dα <br/>
         * - dS / dβ <br/>
         * - dS / dγ <br/>
         * - dS / dλ
         * </p>
         */
        private final double[][] cjCoefs;

        /** The coefficients S<sup>j</sup> of the function S and its derivatives.
         * <p>
         * The index j belongs to the interval [0,jMax].<br>
         * Each column of the matrix contains the coefficient corresponding to the following functions: <br/>
         * - S <br/>
         * - dS / da <br/>
         * - dS / dk <br/>
         * - dS / dh <br/>
         * - dS / dα <br/>
         * - dS / dβ <br/>
         * - dS / dγ <br/>
         * - dS / dλ
         * </p>
         */
        private final double[][] sjCoefs;

        /**
         * Standard constructor.
         *
         * @param nMax maximum value of n index
         * @param sMax maximum value of s index
         * @param jMax maximum value of j index
         * @param context container for attributes
         * @param hansen hansen objects
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        GeneratingFunctionCoefficients(final int nMax, final int sMax, final int jMax,
                                       final DSSTThirdBodyDynamicContext context,
                                       final HansenObjects hansen,
                                       final double[] aoR3Pow, final double[][] qns) {
            this.jMax = jMax;
            this.cjsjFourier = new FourierCjSjCoefficients(nMax, sMax, jMax, context, aoR3Pow, qns);
            this.cjCoefs = new double[8][jMax + 1];
            this.sjCoefs = new double[8][jMax + 1];

            computeGeneratingFunctionCoefficients(context, hansen, aoR3Pow, qns);
        }

        /**
         * Compute the coefficients for the generating function S and its derivatives.
         * @param context container for attributes
         * @param hansenObjects hansen objects
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        private void computeGeneratingFunctionCoefficients(final DSSTThirdBodyDynamicContext context, final HansenObjects hansenObjects,
                                                           final double[] aoR3Pow, final double[][] qns) {

            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            // Access to potential U derivatives
            final UAnddU udu = new UAnddU(context, hansenObjects, aoR3Pow, qns);

            //Compute the C<sup>j</sup> coefficients
            for (int j = 1; j <= jMax; j++) {
                //Compute the C<sup>j</sup> coefficients
                cjCoefs[0][j] = cjsjFourier.getCj(j);
                cjCoefs[1][j] = cjsjFourier.getdCjda(j);
                cjCoefs[2][j] = cjsjFourier.getdCjdk(j) - (cjsjFourier.getSjLambda(j - 1) - cjsjFourier.getSjLambda(j + 1)) / 2;
                cjCoefs[3][j] = cjsjFourier.getdCjdh(j) - (cjsjFourier.getCjLambda(j - 1) + cjsjFourier.getCjLambda(j + 1)) / 2;
                cjCoefs[4][j] = cjsjFourier.getdCjdalpha(j);
                cjCoefs[5][j] = cjsjFourier.getdCjdbeta(j);
                cjCoefs[6][j] = cjsjFourier.getdCjdgamma(j);
                cjCoefs[7][j] = cjsjFourier.getCjLambda(j);

                //Compute the S<sup>j</sup> coefficients
                sjCoefs[0][j] = cjsjFourier.getSj(j);
                sjCoefs[1][j] = cjsjFourier.getdSjda(j);
                sjCoefs[2][j] = cjsjFourier.getdSjdk(j) + (cjsjFourier.getCjLambda(j - 1) - cjsjFourier.getCjLambda(j + 1)) / 2;
                sjCoefs[3][j] = cjsjFourier.getdSjdh(j) - (cjsjFourier.getSjLambda(j - 1) + cjsjFourier.getSjLambda(j + 1)) / 2;
                sjCoefs[4][j] = cjsjFourier.getdSjdalpha(j);
                sjCoefs[5][j] = cjsjFourier.getdSjdbeta(j);
                sjCoefs[6][j] = cjsjFourier.getdSjdgamma(j);
                sjCoefs[7][j] = cjsjFourier.getSjLambda(j);

                //In the special case j == 1 there are some additional terms to be added
                if (j == 1) {
                    //Additional terms for C<sup>j</sup> coefficients
                    cjCoefs[0][j] += -auxiliaryElements.getH() * udu.getU();
                    cjCoefs[1][j] += -auxiliaryElements.getH() * udu.getdUda();
                    cjCoefs[2][j] += -auxiliaryElements.getH() * udu.getdUdk();
                    cjCoefs[3][j] += -(auxiliaryElements.getH() * udu.getdUdh() + udu.getU() + cjsjFourier.getC0Lambda());
                    cjCoefs[4][j] += -auxiliaryElements.getH() * udu.getdUdAl();
                    cjCoefs[5][j] += -auxiliaryElements.getH() * udu.getdUdBe();
                    cjCoefs[6][j] += -auxiliaryElements.getH() * udu.getdUdGa();

                    //Additional terms for S<sup>j</sup> coefficients
                    sjCoefs[0][j] += auxiliaryElements.getK() * udu.getU();
                    sjCoefs[1][j] += auxiliaryElements.getK() * udu.getdUda();
                    sjCoefs[2][j] += auxiliaryElements.getK() * udu.getdUdk() + udu.getU() + cjsjFourier.getC0Lambda();
                    sjCoefs[3][j] += auxiliaryElements.getK() * udu.getdUdh();
                    sjCoefs[4][j] += auxiliaryElements.getK() * udu.getdUdAl();
                    sjCoefs[5][j] += auxiliaryElements.getK() * udu.getdUdBe();
                    sjCoefs[6][j] += auxiliaryElements.getK() * udu.getdUdGa();
                }
            }
        }

        /** Get the coefficient C<sup>j</sup> for the function S.
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function S
         */
        public double getSCj(final int j) {
            return cjCoefs[0][j];
        }

        /** Get the coefficient S<sup>j</sup> for the function S.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the function S
         */
        public double getSSj(final int j) {
            return sjCoefs[0][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/da.
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/da
         */
        public double getdSdaCj(final int j) {
            return cjCoefs[1][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/da.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/da
         */
        public double getdSdaSj(final int j) {
            return sjCoefs[1][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dk
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dk
         */
        public double getdSdkCj(final int j) {
            return cjCoefs[2][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dk.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dk
         */
        public double getdSdkSj(final int j) {
            return sjCoefs[2][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dh
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dh
         */
        public double getdSdhCj(final int j) {
            return cjCoefs[3][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dh.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dh
         */
        public double getdSdhSj(final int j) {
            return sjCoefs[3][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dα
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dα
         */
        public double getdSdalphaCj(final int j) {
            return cjCoefs[4][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dα.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dα
         */
        public double getdSdalphaSj(final int j) {
            return sjCoefs[4][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dβ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dβ
         */
        public double getdSdbetaCj(final int j) {
            return cjCoefs[5][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dβ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dβ
         */
        public double getdSdbetaSj(final int j) {
            return sjCoefs[5][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dγ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dγ
         */
        public double getdSdgammaCj(final int j) {
            return cjCoefs[6][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dγ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dγ
         */
        public double getdSdgammaSj(final int j) {
            return sjCoefs[6][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dλ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dλ
         */
        public double getdSdlambdaCj(final int j) {
            return cjCoefs[7][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dλ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dλ
         */
        public double getdSdlambdaSj(final int j) {
            return sjCoefs[7][j];
        }
    }

    /** This class computes the coefficients for the generating function S and its derivatives.
     * <p>
     * The form of the generating functions is: <br>
     *  S = C⁰ + &Sigma;<sub>j=1</sub><sup>N+1</sup>(C<sup>j</sup> * cos(jF) + S<sup>j</sup> * sin(jF)) <br>
     *  The coefficients C⁰, C<sup>j</sup>, S<sup>j</sup> are the Fourrier coefficients
     *  presented in Danielson 4.2-14,15 except for the case j=1 where
     *  C¹ = C¹<sub>Fourier</sub> - hU and
     *  S¹ = S¹<sub>Fourier</sub> + kU <br>
     *  Also the coefficients of the derivatives of S by a, k, h, α, β, γ and λ
     *  are computed end expressed in a similar manner. The formulas used are 4.2-19, 20, 23, 24
     * </p>
     * @author Lucian Barbulescu
     */
    private class FieldGeneratingFunctionCoefficients <T extends CalculusFieldElement<T>> {

        /** The Fourier coefficients as presented in Danielson 4.2-14,15. */
        private final FieldFourierCjSjCoefficients<T> cjsjFourier;

        /** Maximum value of j index. */
        private final int jMax;

        /** The coefficients C<sup>j</sup> of the function S and its derivatives.
         * <p>
         * The index j belongs to the interval [0,jMax]. The coefficient C⁰ is the free coefficient.<br>
         * Each column of the matrix contains the coefficient corresponding to the following functions: <br/>
         * - S <br/>
         * - dS / da <br/>
         * - dS / dk <br/>
         * - dS / dh <br/>
         * - dS / dα <br/>
         * - dS / dβ <br/>
         * - dS / dγ <br/>
         * - dS / dλ
         * </p>
         */
        private final T[][] cjCoefs;

        /** The coefficients S<sup>j</sup> of the function S and its derivatives.
         * <p>
         * The index j belongs to the interval [0,jMax].<br>
         * Each column of the matrix contains the coefficient corresponding to the following functions: <br/>
         * - S <br/>
         * - dS / da <br/>
         * - dS / dk <br/>
         * - dS / dh <br/>
         * - dS / dα <br/>
         * - dS / dβ <br/>
         * - dS / dγ <br/>
         * - dS / dλ
         * </p>
         */
        private final T[][] sjCoefs;

        /**
         * Standard constructor.
         *
         * @param nMax maximum value of n index
         * @param sMax maximum value of s index
         * @param jMax maximum value of j index
         * @param context container for attributes
         * @param hansen hansen objects
         * @param field field used by default
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        FieldGeneratingFunctionCoefficients(final int nMax, final int sMax, final int jMax,
                                            final FieldDSSTThirdBodyDynamicContext<T> context,
                                            final FieldHansenObjects<T> hansen, final Field<T> field,
                                            final T[] aoR3Pow, final T[][] qns) {
            this.jMax = jMax;
            this.cjsjFourier = new FieldFourierCjSjCoefficients<>(nMax, sMax, jMax, context, aoR3Pow, qns, field);
            this.cjCoefs     = MathArrays.buildArray(field, 8, jMax + 1);
            this.sjCoefs     = MathArrays.buildArray(field, 8, jMax + 1);

            computeGeneratingFunctionCoefficients(context, hansen, aoR3Pow, qns);
        }

        /**
         * Compute the coefficients for the generating function S and its derivatives.
         * @param context container for attributes
         * @param hansenObjects hansen objects
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        private void computeGeneratingFunctionCoefficients(final FieldDSSTThirdBodyDynamicContext<T> context,
                                                           final FieldHansenObjects<T> hansenObjects,
                                                           final T[] aoR3Pow, final T[][] qns) {

            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            // Access to potential U derivatives
            final FieldUAnddU<T> udu = new FieldUAnddU<>(context, hansenObjects, aoR3Pow, qns);

            //Compute the C<sup>j</sup> coefficients
            for (int j = 1; j <= jMax; j++) {
                //Compute the C<sup>j</sup> coefficients
                cjCoefs[0][j] = cjsjFourier.getCj(j);
                cjCoefs[1][j] = cjsjFourier.getdCjda(j);
                cjCoefs[2][j] = cjsjFourier.getdCjdk(j).subtract((cjsjFourier.getSjLambda(j - 1).subtract(cjsjFourier.getSjLambda(j + 1))).divide(2.));
                cjCoefs[3][j] = cjsjFourier.getdCjdh(j).subtract((cjsjFourier.getCjLambda(j - 1).add(cjsjFourier.getCjLambda(j + 1))).divide(2.));
                cjCoefs[4][j] = cjsjFourier.getdCjdalpha(j);
                cjCoefs[5][j] = cjsjFourier.getdCjdbeta(j);
                cjCoefs[6][j] = cjsjFourier.getdCjdgamma(j);
                cjCoefs[7][j] = cjsjFourier.getCjLambda(j);

                //Compute the S<sup>j</sup> coefficients
                sjCoefs[0][j] = cjsjFourier.getSj(j);
                sjCoefs[1][j] = cjsjFourier.getdSjda(j);
                sjCoefs[2][j] = cjsjFourier.getdSjdk(j).add((cjsjFourier.getCjLambda(j - 1).subtract(cjsjFourier.getCjLambda(j + 1))).divide(2.));
                sjCoefs[3][j] = cjsjFourier.getdSjdh(j).subtract((cjsjFourier.getSjLambda(j - 1).add(cjsjFourier.getSjLambda(j + 1))).divide(2.));
                sjCoefs[4][j] = cjsjFourier.getdSjdalpha(j);
                sjCoefs[5][j] = cjsjFourier.getdSjdbeta(j);
                sjCoefs[6][j] = cjsjFourier.getdSjdgamma(j);
                sjCoefs[7][j] = cjsjFourier.getSjLambda(j);

                //In the special case j == 1 there are some additional terms to be added
                if (j == 1) {
                    //Additional terms for C<sup>j</sup> coefficients
                    cjCoefs[0][j] = cjCoefs[0][j].add(auxiliaryElements.getH().negate().multiply(udu.getU()));
                    cjCoefs[1][j] = cjCoefs[1][j].add(auxiliaryElements.getH().negate().multiply(udu.getdUda()));
                    cjCoefs[2][j] = cjCoefs[2][j].add(auxiliaryElements.getH().negate().multiply(udu.getdUdk()));
                    cjCoefs[3][j] = cjCoefs[3][j].add(auxiliaryElements.getH().multiply(udu.getdUdh()).add(udu.getU()).add(cjsjFourier.getC0Lambda()).negate());
                    cjCoefs[4][j] = cjCoefs[4][j].add(auxiliaryElements.getH().negate().multiply(udu.getdUdAl()));
                    cjCoefs[5][j] = cjCoefs[5][j].add(auxiliaryElements.getH().negate().multiply(udu.getdUdBe()));
                    cjCoefs[6][j] = cjCoefs[6][j].add(auxiliaryElements.getH().negate().multiply(udu.getdUdGa()));

                    //Additional terms for S<sup>j</sup> coefficients
                    sjCoefs[0][j] = sjCoefs[0][j].add(auxiliaryElements.getK().multiply(udu.getU()));
                    sjCoefs[1][j] = sjCoefs[1][j].add(auxiliaryElements.getK().multiply(udu.getdUda()));
                    sjCoefs[2][j] = sjCoefs[2][j].add(auxiliaryElements.getK().multiply(udu.getdUdk()).add(udu.getU()).add(cjsjFourier.getC0Lambda()));
                    sjCoefs[3][j] = sjCoefs[3][j].add(auxiliaryElements.getK().multiply(udu.getdUdh()));
                    sjCoefs[4][j] = sjCoefs[4][j].add(auxiliaryElements.getK().multiply(udu.getdUdAl()));
                    sjCoefs[5][j] = sjCoefs[5][j].add(auxiliaryElements.getK().multiply(udu.getdUdBe()));
                    sjCoefs[6][j] = sjCoefs[6][j].add(auxiliaryElements.getK().multiply(udu.getdUdGa()));
                }
            }
        }

        /** Get the coefficient C<sup>j</sup> for the function S.
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function S
         */
        public T getSCj(final int j) {
            return cjCoefs[0][j];
        }

        /** Get the coefficient S<sup>j</sup> for the function S.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the function S
         */
        public T getSSj(final int j) {
            return sjCoefs[0][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/da.
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/da
         */
        public T getdSdaCj(final int j) {
            return cjCoefs[1][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/da.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/da
         */
        public T getdSdaSj(final int j) {
            return sjCoefs[1][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dk
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dk
         */
        public T getdSdkCj(final int j) {
            return cjCoefs[2][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dk.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dk
         */
        public T getdSdkSj(final int j) {
            return sjCoefs[2][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dh
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dh
         */
        public T getdSdhCj(final int j) {
            return cjCoefs[3][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dh.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dh
         */
        public T getdSdhSj(final int j) {
            return sjCoefs[3][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dα
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dα
         */
        public T getdSdalphaCj(final int j) {
            return cjCoefs[4][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dα.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dα
         */
        public T getdSdalphaSj(final int j) {
            return sjCoefs[4][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dβ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dβ
         */
        public T getdSdbetaCj(final int j) {
            return cjCoefs[5][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dβ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dβ
         */
        public T getdSdbetaSj(final int j) {
            return sjCoefs[5][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dγ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dγ
         */
        public T getdSdgammaCj(final int j) {
            return cjCoefs[6][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dγ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dγ
         */
        public T getdSdgammaSj(final int j) {
            return sjCoefs[6][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dλ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dλ
         */
        public T getdSdlambdaCj(final int j) {
            return cjCoefs[7][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dλ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dλ
         */
        public T getdSdlambdaSj(final int j) {
            return sjCoefs[7][j];
        }
    }

    /**
     * The coefficients used to compute the short periodic contribution for the Third body perturbation.
     * <p>
     * The short periodic contribution for the Third Body is expressed in Danielson 4.2-25.<br>
     * The coefficients C<sub>i</sub>⁰, C<sub>i</sub><sup>j</sup>, S<sub>i</sub><sup>j</sup>
     * are computed by replacing the corresponding values in formula 2.5.5-10.
     * </p>
     * @author Lucian Barbulescu
     */
    private static class ThirdBodyShortPeriodicCoefficients implements ShortPeriodTerms {

        /** Maximal value for j. */
        private final int jMax;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** Max frequency of F. */
        private final int    maxFreqF;

        /** Coefficients prefix. */
        private final String prefix;

        /** All coefficients slots. */
        private final transient TimeSpanMap<Slot> slots;

        /**
         * Standard constructor.
         *  @param interpolationPoints number of points used in the interpolation process
         * @param jMax maximal value for j
         * @param maxFreqF Max frequency of F
         * @param bodyName third body name
         * @param slots all coefficients slots
         */
        ThirdBodyShortPeriodicCoefficients(final int jMax, final int interpolationPoints,
                                           final int maxFreqF, final String bodyName,
                                           final TimeSpanMap<Slot> slots) {
            this.jMax                = jMax;
            this.interpolationPoints = interpolationPoints;
            this.maxFreqF            = maxFreqF;
            this.prefix              = DSSTThirdBody.SHORT_PERIOD_PREFIX + bodyName + "-";
            this.slots               = slots;
        }

        /** Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        public Slot createSlot(final SpacecraftState... meanStates) {
            final Slot         slot  = new Slot(jMax, interpolationPoints);
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

            // the current eccentric longitude
            final double F = meanOrbit.getLE();

            //initialize the short periodic contribution with the corresponding C⁰ coeficient
            final double[] shortPeriodic = slot.cij[0].value(meanOrbit.getDate());

            // Add the cos and sin dependent terms
            for (int j = 1; j <= maxFreqF; j++) {
                //compute cos and sin
                final SinCos scjF  = FastMath.sinCos(j * F);

                final double[] c = slot.cij[j].value(meanOrbit.getDate());
                final double[] s = slot.sij[j].value(meanOrbit.getDate());
                for (int i = 0; i < 6; i++) {
                    shortPeriodic[i] += c[i] * scjF.cos() + s[i] * scjF.sin();
                }
            }

            return shortPeriodic;

        }

        /** {@inheritDoc} */
        @Override
        public String getCoefficientsKeyPrefix() {
            return prefix;
        }

        /** {@inheritDoc}
         * <p>
         * For third body attraction forces,there are maxFreqF + 1 cj coefficients,
         * maxFreqF sj coefficients where maxFreqF depends on the orbit.
         * The j index is the integer multiplier for the eccentric longitude argument
         * in the cj and sj coefficients.
         * </p>
         */
        @Override
        public Map<String, double[]> getCoefficients(final AbsoluteDate date, final Set<String> selected) {

            // select the coefficients slot
            final Slot slot = slots.get(date);

            final Map<String, double[]> coefficients = new HashMap<String, double[]>(2 * maxFreqF + 1);
            storeIfSelected(coefficients, selected, slot.cij[0].value(date), "c", 0);
            for (int j = 1; j <= maxFreqF; j++) {
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

    }

    /**
     * The coefficients used to compute the short periodic contribution for the Third body perturbation.
     * <p>
     * The short periodic contribution for the Third Body is expressed in Danielson 4.2-25.<br>
     * The coefficients C<sub>i</sub>⁰, C<sub>i</sub><sup>j</sup>, S<sub>i</sub><sup>j</sup>
     * are computed by replacing the corresponding values in formula 2.5.5-10.
     * </p>
     * @author Lucian Barbulescu
     */
    private static class FieldThirdBodyShortPeriodicCoefficients <T extends CalculusFieldElement<T>> implements FieldShortPeriodTerms<T> {

        /** Maximal value for j. */
        private final int jMax;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** Max frequency of F. */
        private final int    maxFreqF;

        /** Coefficients prefix. */
        private final String prefix;

        /** All coefficients slots. */
        private final transient FieldTimeSpanMap<FieldSlot<T>, T> slots;

        /**
         * Standard constructor.
         * @param interpolationPoints number of points used in the interpolation process
         * @param jMax maximal value for j
         * @param maxFreqF Max frequency of F
         * @param bodyName third body name
         * @param slots all coefficients slots
         */
        FieldThirdBodyShortPeriodicCoefficients(final int jMax, final int interpolationPoints,
                                                final int maxFreqF, final String bodyName,
                                                final FieldTimeSpanMap<FieldSlot<T>, T> slots) {
            this.jMax                = jMax;
            this.interpolationPoints = interpolationPoints;
            this.maxFreqF            = maxFreqF;
            this.prefix              = DSSTThirdBody.SHORT_PERIOD_PREFIX + bodyName + "-";
            this.slots               = slots;
        }

        /** Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        @SuppressWarnings("unchecked")
        public FieldSlot<T> createSlot(final FieldSpacecraftState<T>... meanStates) {
            final FieldSlot<T>         slot  = new FieldSlot<>(jMax, interpolationPoints);
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

            // the current eccentric longitude
            final T F = meanOrbit.getLE();

            //initialize the short periodic contribution with the corresponding C⁰ coeficient
            final T[] shortPeriodic = (T[]) slot.cij[0].value(meanOrbit.getDate());

            // Add the cos and sin dependent terms
            for (int j = 1; j <= maxFreqF; j++) {
                //compute cos and sin
                final FieldSinCos<T> scjF = FastMath.sinCos(F.multiply(j));

                final T[] c = (T[]) slot.cij[j].value(meanOrbit.getDate());
                final T[] s = (T[]) slot.sij[j].value(meanOrbit.getDate());
                for (int i = 0; i < 6; i++) {
                    shortPeriodic[i] = shortPeriodic[i].add(c[i].multiply(scjF.cos()).add(s[i].multiply(scjF.sin())));
                }
            }

            return shortPeriodic;

        }

        /** {@inheritDoc} */
        @Override
        public String getCoefficientsKeyPrefix() {
            return prefix;
        }

        /** {@inheritDoc}
         * <p>
         * For third body attraction forces,there are maxFreqF + 1 cj coefficients,
         * maxFreqF sj coefficients where maxFreqF depends on the orbit.
         * The j index is the integer multiplier for the eccentric longitude argument
         * in the cj and sj coefficients.
         * </p>
         */
        @Override
        public Map<String, T[]> getCoefficients(final FieldAbsoluteDate<T> date, final Set<String> selected) {

            // select the coefficients slot
            final FieldSlot<T> slot = slots.get(date);

            final Map<String, T[]> coefficients = new HashMap<String, T[]>(2 * maxFreqF + 1);
            storeIfSelected(coefficients, selected, slot.cij[0].value(date), "c", 0);
            for (int j = 1; j <= maxFreqF; j++) {
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

        /** The coefficients C<sub>i</sub><sup>j</sup>.
         * <p>
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
         *  @param jMax maximum value for j index
         *  @param interpolationPoints number of points used in the interpolation process
         */
        Slot(final int jMax, final int interpolationPoints) {
            // allocate the coefficients arrays
            cij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1];
            sij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1];
            for (int j = 0; j <= jMax; j++) {
                cij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                sij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
            }


        }
    }

    /** Coefficients valid for one time slot. */
    private static class FieldSlot <T extends CalculusFieldElement<T>> {

        /** The coefficients C<sub>i</sub><sup>j</sup>.
         * <p>
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
        private final FieldShortPeriodicsInterpolatedCoefficient<T>[] cij;

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
        private final FieldShortPeriodicsInterpolatedCoefficient<T>[] sij;

        /** Simple constructor.
         *  @param jMax maximum value for j index
         *  @param interpolationPoints number of points used in the interpolation process
         */
        @SuppressWarnings("unchecked")
        FieldSlot(final int jMax, final int interpolationPoints) {
            // allocate the coefficients arrays
            cij = (FieldShortPeriodicsInterpolatedCoefficient<T>[]) Array.newInstance(FieldShortPeriodicsInterpolatedCoefficient.class, jMax + 1);
            sij = (FieldShortPeriodicsInterpolatedCoefficient<T>[]) Array.newInstance(FieldShortPeriodicsInterpolatedCoefficient.class, jMax + 1);
            for (int j = 0; j <= jMax; j++) {
                cij[j] = new FieldShortPeriodicsInterpolatedCoefficient<>(interpolationPoints);
                sij[j] = new FieldShortPeriodicsInterpolatedCoefficient<>(interpolationPoints);
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
         * @param context container for attributes
         * @param hansen hansen objects
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        UAnddU(final DSSTThirdBodyDynamicContext context, final HansenObjects hansen,
               final double[] aoR3Pow, final double[][] qns) {
            // Auxiliary elements related to the current orbit
            final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

            // Gs and Hs coefficients
            final double[][] GsHs = CoefficientsFactory.computeGsHs(auxiliaryElements.getK(), auxiliaryElements.getH(), context.getAlpha(), context.getBeta(), staticContext.getMaxEccPow());

            // Initialise U.
            U = 0.;

            // Potential derivatives
            dUda  = 0.;
            dUdk  = 0.;
            dUdh  = 0.;
            dUdAl = 0.;
            dUdBe = 0.;
            dUdGa = 0.;

            for (int s = 0; s <= staticContext.getMaxEccPow(); s++) {

                // initialise the Hansen roots
                hansen.computeHansenObjectsInitValues(context, auxiliaryElements.getB(), s);

                // Get the current Gs coefficient
                final double gs = GsHs[0][s];

                // Compute Gs partial derivatives from 3.1-(9)
                double dGsdh  = 0.;
                double dGsdk  = 0.;
                double dGsdAl = 0.;
                double dGsdBe = 0.;
                if (s > 0) {
                    // First get the G(s-1) and the H(s-1) coefficients
                    final double sxGsm1 = s * GsHs[0][s - 1];
                    final double sxHsm1 = s * GsHs[1][s - 1];
                    // Then compute derivatives
                    dGsdh  = context.getBeta()  * sxGsm1 - context.getAlpha() * sxHsm1;
                    dGsdk  = context.getAlpha() * sxGsm1 + context.getBeta()  * sxHsm1;
                    dGsdAl = auxiliaryElements.getK() * sxGsm1 - auxiliaryElements.getH() * sxHsm1;
                    dGsdBe = auxiliaryElements.getH() * sxGsm1 + auxiliaryElements.getK() * sxHsm1;
                }

                // Kronecker symbol (2 - delta(0,s))
                final double delta0s = (s == 0) ? 1. : 2.;

                for (int n = FastMath.max(2, s); n <= staticContext.getMaxAR3Pow(); n++) {
                    // (n - s) must be even
                    if ((n - s) % 2 == 0) {
                        // Extract data from previous computation :
                        final double kns   = hansen.getHansenObjects()[s].getValue(n, auxiliaryElements.getB());
                        final double dkns  = hansen.getHansenObjects()[s].getDerivative(n, auxiliaryElements.getB());

                        final double vns   = Vns.get(new NSKey(n, s));
                        final double coef0 = delta0s * aoR3Pow[n] * vns;
                        final double coef1 = coef0 * qns[n][s];
                        final double coef2 = coef1 * kns;
                        // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                        // for n = s, Q(n, n + 1) = 0. (Cefola & Broucke, 1975)
                        final double dqns = (n == s) ? 0. : qns[n][s + 1];

                        //Compute U:
                        U += coef2 * gs;

                        // Compute dU / da :
                        dUda  += coef2 * n * gs;
                        // Compute dU / dh
                        dUdh  += coef1 * (kns * dGsdh + context.getHXXX() * gs * dkns);
                        // Compute dU / dk
                        dUdk  += coef1 * (kns * dGsdk + context.getKXXX() * gs * dkns);
                        // Compute dU / dAlpha
                        dUdAl += coef2 * dGsdAl;
                        // Compute dU / dBeta
                        dUdBe += coef2 * dGsdBe;
                        // Compute dU / dGamma
                        dUdGa += coef0 * kns * dqns * gs;
                    }
                }
            }

            // multiply by mu3 / R3
            this.U = U * context.getMuoR3();

            this.dUda  = dUda  * context.getMuoR3() / auxiliaryElements.getSma();
            this.dUdk  = dUdk  * context.getMuoR3();
            this.dUdh  = dUdh  * context.getMuoR3();
            this.dUdAl = dUdAl * context.getMuoR3();
            this.dUdBe = dUdBe * context.getMuoR3();
            this.dUdGa = dUdGa * context.getMuoR3();

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

    /** Compute potential and potential derivatives with respect to orbital parameters. */
    private class FieldUAnddU <T extends CalculusFieldElement<T>> {

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
         * @param context container for attributes
         * @param hansen hansen objects
         * @param aoR3Pow a / R3 up to power maxAR3Pow
         * @param qns Qns coefficients
         */
        FieldUAnddU(final FieldDSSTThirdBodyDynamicContext<T> context, final FieldHansenObjects<T> hansen,
                    final T[] aoR3Pow, final T[][] qns) {

            // Auxiliary elements related to the current orbit
            final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

            // Field for array building
            final Field<T> field = auxiliaryElements.getDate().getField();
            // Zero for initialization
            final T zero = field.getZero();

            // Gs and Hs coefficients
            final T[][] GsHs = CoefficientsFactory.computeGsHs(auxiliaryElements.getK(), auxiliaryElements.getH(), context.getAlpha(), context.getBeta(), staticContext.getMaxEccPow(), field);

            // Initialise U.
            U = zero;

            // Potential derivatives
            dUda  = zero;
            dUdk  = zero;
            dUdh  = zero;
            dUdAl = zero;
            dUdBe = zero;
            dUdGa = zero;

            for (int s = 0; s <= staticContext.getMaxEccPow(); s++) {
                // initialise the Hansen roots
                hansen.computeHansenObjectsInitValues(context, auxiliaryElements.getB(), s);

                // Get the current Gs coefficient
                final T gs = GsHs[0][s];

                // Compute Gs partial derivatives from 3.1-(9)
                T dGsdh  = zero;
                T dGsdk  = zero;
                T dGsdAl = zero;
                T dGsdBe = zero;
                if (s > 0) {
                    // First get the G(s-1) and the H(s-1) coefficients
                    final T sxGsm1 = GsHs[0][s - 1].multiply(s);
                    final T sxHsm1 = GsHs[1][s - 1].multiply(s);
                    // Then compute derivatives
                    dGsdh  = sxGsm1.multiply(context.getBeta()).subtract(sxHsm1.multiply(context.getAlpha()));
                    dGsdk  = sxGsm1.multiply(context.getAlpha()).add(sxHsm1.multiply(context.getBeta()));
                    dGsdAl = sxGsm1.multiply(auxiliaryElements.getK()).subtract(sxHsm1.multiply(auxiliaryElements.getH()));
                    dGsdBe = sxGsm1.multiply(auxiliaryElements.getH()).add(sxHsm1.multiply(auxiliaryElements.getK()));
                }

                // Kronecker symbol (2 - delta(0,s))
                final T delta0s = zero.add((s == 0) ? 1. : 2.);

                for (int n = FastMath.max(2, s); n <= staticContext.getMaxAR3Pow(); n++) {
                    // (n - s) must be even
                    if ((n - s) % 2 == 0) {
                        // Extract data from previous computation :
                        final T kns   = (T) hansen.getHansenObjects()[s].getValue(n, auxiliaryElements.getB());
                        final T dkns  = (T) hansen.getHansenObjects()[s].getDerivative(n, auxiliaryElements.getB());

                        final double vns = Vns.get(new NSKey(n, s));
                        final T coef0 = delta0s.multiply(vns).multiply(aoR3Pow[n]);
                        final T coef1 = coef0.multiply(qns[n][s]);
                        final T coef2 = coef1.multiply(kns);
                        // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                        // for n = s, Q(n, n + 1) = 0. (Cefola & Broucke, 1975)
                        final T dqns = (n == s) ? zero : qns[n][s + 1];

                        //Compute U:
                        U = U.add(coef2.multiply(gs));

                        // Compute dU / da :
                        dUda  = dUda.add(coef2.multiply(n).multiply(gs));
                        // Compute dU / dh
                        dUdh  = dUdh.add(coef1.multiply(dGsdh.multiply(kns).add(context.getHXXX().multiply(gs).multiply(dkns))));
                        // Compute dU / dk
                        dUdk  = dUdk.add(coef1.multiply(dGsdk.multiply(kns).add(context.getKXXX().multiply(gs).multiply(dkns))));
                        // Compute dU / dAlpha
                        dUdAl = dUdAl.add(coef2.multiply(dGsdAl));
                        // Compute dU / dBeta
                        dUdBe = dUdBe.add(coef2.multiply(dGsdBe));
                        // Compute dU / dGamma
                        dUdGa = dUdGa.add(coef0.multiply(kns).multiply(dqns).multiply(gs));
                    }
                }
            }

            // multiply by mu3 / R3
            this.U = U.multiply(context.getMuoR3());

            this.dUda  = dUda.multiply(context.getMuoR3().divide(auxiliaryElements.getSma()));
            this.dUdk  = dUdk.multiply(context.getMuoR3());
            this.dUdh  = dUdh.multiply(context.getMuoR3());
            this.dUdAl = dUdAl.multiply(context.getMuoR3());
            this.dUdBe = dUdBe.multiply(context.getMuoR3());
            this.dUdGa = dUdGa.multiply(context.getMuoR3());

        }

        /** Return value of U.
         * @return U
         */
        public T getU() {
            return U;
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
    private static class HansenObjects {

        /** Max power for summation. */
        private static final int    MAX_POWER = 22;

        /** An array that contains the objects needed to build the Hansen coefficients. <br/>
         * The index is s */
        private final HansenThirdBodyLinear[] hansenObjects;

        /** Simple constructor. */
        HansenObjects() {
            this.hansenObjects = new HansenThirdBodyLinear[MAX_POWER + 1];
            for (int s = 0; s <= MAX_POWER; s++) {
                this.hansenObjects[s] = new HansenThirdBodyLinear(MAX_POWER, s);
            }
        }

        /** Compute init values for hansen objects.
         * @param context container for attributes
         * @param B = sqrt(1 - e²).
         * @param element element of the array to compute the init values
         */
        public void computeHansenObjectsInitValues(final DSSTThirdBodyDynamicContext context, final double B, final int element) {
            hansenObjects[element].computeInitValues(B, context.getBB(), context.getBBB());
        }

        /** Get the Hansen Objects.
         * @return hansenObjects
         */
        public HansenThirdBodyLinear[] getHansenObjects() {
            return hansenObjects;
        }

    }

    /** Computes init values of the Hansen Objects. */
    private static class FieldHansenObjects<T extends CalculusFieldElement<T>> {

        /** Max power for summation. */
        private static final int    MAX_POWER = 22;

        /** An array that contains the objects needed to build the Hansen coefficients. <br/>
         * The index is s */
        private final FieldHansenThirdBodyLinear<T>[] hansenObjects;

        /** Simple constructor.
         * @param field field used by default
         */
        @SuppressWarnings("unchecked")
        FieldHansenObjects(final Field<T> field) {
            this.hansenObjects = (FieldHansenThirdBodyLinear<T>[]) Array.newInstance(FieldHansenThirdBodyLinear.class, MAX_POWER + 1);
            for (int s = 0; s <= MAX_POWER; s++) {
                this.hansenObjects[s] = new FieldHansenThirdBodyLinear<>(MAX_POWER, s, field);
            }
        }

        /** Initialise the Hansen roots for third body problem.
         * @param context container for attributes
         * @param B = sqrt(1 - e²).
         * @param element element of the array to compute the init values
         */
        public void computeHansenObjectsInitValues(final FieldDSSTThirdBodyDynamicContext<T> context,
                                                   final T B, final int element) {
            hansenObjects[element].computeInitValues(B, context.getBB(), context.getBBB());
        }

        /** Get the Hansen Objects.
         * @return hansenObjects
         */
        public FieldHansenThirdBodyLinear<T>[] getHansenObjects() {
            return hansenObjects;
        }

    }

}
