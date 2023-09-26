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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.CalculusFieldUnivariateVectorFunction;
import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldCjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.ShortPeriodicsInterpolatedCoefficient;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTimeSpanMap;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/**
 * Common handling of {@link DSSTForceModel} methods for Gaussian contributions
 * to DSST propagation.
 * <p>
 * This abstract class allows to provide easily a subset of
 * {@link DSSTForceModel} methods for specific Gaussian contributions.
 * </p>
 * <p>
 * This class implements the notion of numerical averaging of the DSST theory.
 * Numerical averaging is mainly used for non-conservative disturbing forces
 * such as atmospheric drag and solar radiation pressure.
 * </p>
 * <p>
 * Gaussian contributions can be expressed as: da<sub>i</sub>/dt =
 * δa<sub>i</sub>/δv . q<br>
 * where:
 * <ul>
 * <li>a<sub>i</sub> are the six equinoctial elements</li>
 * <li>v is the velocity vector</li>
 * <li>q is the perturbing acceleration due to the considered force</li>
 * </ul>
 *
 * <p>
 * The averaging process and other considerations lead to integrate this
 * contribution over the true longitude L possibly taking into account some
 * limits.
 *
 * <p>
 * To create a numerically averaged contribution, one needs only to provide a
 * {@link ForceModel} and to implement in the derived class the methods:
 * {@link #getLLimits(SpacecraftState, AuxiliaryElements)} and
 * {@link #getParametersDriversWithoutMu()}.
 * </p>
 * @author Pascal Parraud
 * @author Bryan Cazabonne (field translation)
 */
public abstract class AbstractGaussianContribution implements DSSTForceModel {

    /**
     * Retrograde factor I.
     * <p>
     * DSST model needs equinoctial orbit as internal representation. Classical
     * equinoctial elements have discontinuities when inclination is close to zero.
     * In this representation, I = +1. <br>
     * To avoid this discontinuity, another representation exists and equinoctial
     * elements can be expressed in a different way, called "retrograde" orbit. This
     * implies I = -1. <br>
     * As Orekit doesn't implement the retrograde orbit, I is always set to +1. But
     * for the sake of consistency with the theory, the retrograde factor has been
     * kept in the formulas.
     * </p>
     */
    private static final int I = 1;

    /**
     * Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction in the
     * multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Available orders for Gauss quadrature. */
    private static final int[] GAUSS_ORDER = { 12, 16, 20, 24, 32, 40, 48 };

    /** Max rank in Gauss quadrature orders array. */
    private static final int MAX_ORDER_RANK = GAUSS_ORDER.length - 1;

    /** Number of points for interpolation. */
    private static final int INTERPOLATION_POINTS = 3;

    /** Maximum value for j index. */
    private static final int JMAX = 12;

    /** Contribution to be numerically averaged. */
    private final ForceModel contribution;

    /** Gauss integrator. */
    private final double threshold;

    /** Gauss integrator. */
    private GaussQuadrature integrator;

    /** Flag for Gauss order computation. */
    private boolean isDirty;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Prefix for coefficients keys. */
    private final String coefficientsKeyPrefix;

    /** Short period terms. */
    private GaussianShortPeriodicCoefficients gaussianSPCoefs;

    /** Short period terms. */
    private Map<Field<?>, FieldGaussianShortPeriodicCoefficients<?>> gaussianFieldSPCoefs;

    /** Driver for gravitational parameter. */
    private final ParameterDriver gmParameterDriver;

    /**
     * Build a new instance.
     * @param coefficientsKeyPrefix prefix for coefficients keys
     * @param threshold             tolerance for the choice of the Gauss quadrature
     *                              order
     * @param contribution          the {@link ForceModel} to be numerically
     *                              averaged
     * @param mu                    central attraction coefficient
     */
    protected AbstractGaussianContribution(final String coefficientsKeyPrefix, final double threshold,
            final ForceModel contribution, final double mu) {

        gmParameterDriver = new ParameterDriver(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT, mu, MU_SCALE,
                0.0, Double.POSITIVE_INFINITY);

        this.coefficientsKeyPrefix = coefficientsKeyPrefix;
        this.contribution = contribution;
        this.threshold = threshold;
        this.integrator = new GaussQuadrature(GAUSS_ORDER[MAX_ORDER_RANK]);
        this.isDirty = true;

        gaussianFieldSPCoefs = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        // Initialize the numerical force model
        contribution.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
        // Initialize the numerical force model
        contribution.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {

        // Parameter drivers
        final List<ParameterDriver> drivers = new ArrayList<>();

        // Loop on drivers (without central attraction coefficient driver)
        for (final ParameterDriver driver : getParametersDriversWithoutMu()) {
            drivers.add(driver);
        }

        // We put central attraction coefficient driver at the end of the array
        drivers.add(gmParameterDriver);
        return drivers;

    }

    /**
     * Get the drivers for force model parameters except the one for the central
     * attraction coefficient.
     * <p>
     * The driver for central attraction coefficient is automatically added at the
     * last element of the {@link ParameterDriver} array into
     * {@link #getParametersDrivers()} method.
     * </p>
     * @return drivers for force model parameters
     */
    protected abstract List<ParameterDriver> getParametersDriversWithoutMu();

    /** {@inheritDoc} */
    @Override
    public List<ShortPeriodTerms> initializeShortPeriodTerms(final AuxiliaryElements auxiliaryElements, final PropagationType type,
            final double[] parameters) {

        final List<ShortPeriodTerms> list = new ArrayList<ShortPeriodTerms>();
        gaussianSPCoefs = new GaussianShortPeriodicCoefficients(coefficientsKeyPrefix, JMAX, INTERPOLATION_POINTS,
                new TimeSpanMap<Slot>(new Slot(JMAX, INTERPOLATION_POINTS)));
        list.add(gaussianSPCoefs);
        return list;

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> List<FieldShortPeriodTerms<T>> initializeShortPeriodTerms(
            final FieldAuxiliaryElements<T> auxiliaryElements, final PropagationType type, final T[] parameters) {

        final Field<T> field = auxiliaryElements.getDate().getField();

        final FieldGaussianShortPeriodicCoefficients<T> fgspc = new FieldGaussianShortPeriodicCoefficients<>(
                coefficientsKeyPrefix, JMAX, INTERPOLATION_POINTS,
                new FieldTimeSpanMap<>(new FieldSlot<>(JMAX, INTERPOLATION_POINTS), field));
        gaussianFieldSPCoefs.put(field, fgspc);
        return Collections.singletonList(fgspc);
    }

    /**
     * Performs initialization at each integration step for the current force model.
     * <p>
     * This method aims at being called before mean elements rates computation.
     * </p>
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param parameters        parameters values of the force model parameters
     *                          only 1 value for each parameterDriver
     * @return new force model context
     */
    private AbstractGaussianContributionContext initializeStep(final AuxiliaryElements auxiliaryElements,
            final double[] parameters) {
        return new AbstractGaussianContributionContext(auxiliaryElements, parameters);
    }

    /**
     * Performs initialization at each integration step for the current force model.
     * <p>
     * This method aims at being called before mean elements rates computation.
     * </p>
     * @param <T>               type of the elements
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param parameters        parameters values of the force model parameters
     *                          (only 1 values for each parameters corresponding
     *                          to state date) by getting the parameters for a specific date.
     * @return new force model context
     */
    private <T extends CalculusFieldElement<T>> FieldAbstractGaussianContributionContext<T> initializeStep(
            final FieldAuxiliaryElements<T> auxiliaryElements, final T[] parameters) {
        return new FieldAbstractGaussianContributionContext<>(auxiliaryElements, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getMeanElementRate(final SpacecraftState state, final AuxiliaryElements auxiliaryElements,
            final double[] parameters) {

        // Container for attributes

        final AbstractGaussianContributionContext context = initializeStep(auxiliaryElements, parameters);
        double[] meanElementRate = new double[6];
        // Computes the limits for the integral
        final double[] ll = getLLimits(state, auxiliaryElements);
        // Computes integrated mean element rates if Llow < Lhigh
        if (ll[0] < ll[1]) {
            meanElementRate = getMeanElementRate(state, integrator, ll[0], ll[1], context, parameters);
            if (isDirty) {
                boolean next = true;
                for (int i = 0; i < MAX_ORDER_RANK && next; i++) {
                    final double[] meanRates = getMeanElementRate(state, new GaussQuadrature(GAUSS_ORDER[i]), ll[0],
                            ll[1], context, parameters);
                    if (getRatesDiff(meanElementRate, meanRates, context) < threshold) {
                        integrator = new GaussQuadrature(GAUSS_ORDER[i]);
                        next = false;
                    }
                }
                isDirty = false;
            }
        }
        return meanElementRate;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getMeanElementRate(final FieldSpacecraftState<T> state,
            final FieldAuxiliaryElements<T> auxiliaryElements, final T[] parameters) {

        // Container for attributes
        final FieldAbstractGaussianContributionContext<T> context = initializeStep(auxiliaryElements, parameters);
        final Field<T> field = state.getDate().getField();

        T[] meanElementRate = MathArrays.buildArray(field, 6);
        // Computes the limits for the integral
        final T[] ll = getLLimits(state, auxiliaryElements);
        // Computes integrated mean element rates if Llow < Lhigh
        if (ll[0].getReal() < ll[1].getReal()) {
            meanElementRate = getMeanElementRate(state, integrator, ll[0], ll[1], context, parameters);
            if (isDirty) {
                boolean next = true;
                for (int i = 0; i < MAX_ORDER_RANK && next; i++) {
                    final T[] meanRates = getMeanElementRate(state, new GaussQuadrature(GAUSS_ORDER[i]), ll[0], ll[1],
                            context, parameters);
                    if (getRatesDiff(meanElementRate, meanRates, context).getReal() < threshold) {
                        integrator = new GaussQuadrature(GAUSS_ORDER[i]);
                        next = false;
                    }
                }
                isDirty = false;
            }
        }

        return meanElementRate;
    }

    /**
     * Compute the limits in L, the true longitude, for integration.
     *
     * @param state             current state information: date, kinematics,
     *                          attitude
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @return the integration limits in L
     */
    protected abstract double[] getLLimits(SpacecraftState state, AuxiliaryElements auxiliaryElements);

    /**
     * Compute the limits in L, the true longitude, for integration.
     *
     * @param <T>               type of the elements
     * @param state             current state information: date, kinematics,
     *                          attitude
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @return the integration limits in L
     */
    protected abstract <T extends CalculusFieldElement<T>> T[] getLLimits(FieldSpacecraftState<T> state,
            FieldAuxiliaryElements<T> auxiliaryElements);

    /**
     * Computes the mean equinoctial elements rates da<sub>i</sub> / dt.
     *
     * @param state      current state
     * @param gauss      Gauss quadrature
     * @param low        lower bound of the integral interval
     * @param high       upper bound of the integral interval
     * @param context    container for attributes
     * @param parameters values of the force model parameters
     * at state date (1 values for each parameters)
     * @return the mean element rates
     */
    protected double[] getMeanElementRate(final SpacecraftState state, final GaussQuadrature gauss, final double low,
            final double high, final AbstractGaussianContributionContext context, final double[] parameters) {

        // Auxiliary elements related to the current orbit
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

        final double[] meanElementRate = gauss.integrate(new IntegrableFunction(state, true, 0, parameters), low, high);

        // Constant multiplier for integral
        final double coef = 1. / (2. * FastMath.PI * auxiliaryElements.getB());
        // Corrects mean element rates
        for (int i = 0; i < 6; i++) {
            meanElementRate[i] *= coef;
        }
        return meanElementRate;
    }

    /**
     * Computes the mean equinoctial elements rates da<sub>i</sub> / dt.
     *
     * @param <T>        type of the elements
     * @param state      current state
     * @param gauss      Gauss quadrature
     * @param low        lower bound of the integral interval
     * @param high       upper bound of the integral interval
     * @param context    container for attributes
     * @param parameters values of the force model parameters(1 values for each parameters)
     * @return the mean element rates
     */
    protected <T extends CalculusFieldElement<T>> T[] getMeanElementRate(final FieldSpacecraftState<T> state,
            final GaussQuadrature gauss, final T low, final T high,
            final FieldAbstractGaussianContributionContext<T> context, final T[] parameters) {

        // Field
        final Field<T> field = context.getA().getField();

        // Auxiliary elements related to the current orbit
        final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

        final T[] meanElementRate = gauss.integrate(new FieldIntegrableFunction<>(state, true, 0, parameters, field),
                low, high, field);
        // Constant multiplier for integral
        final T coef = auxiliaryElements.getB().multiply(low.getPi()).multiply(2.).reciprocal();
        // Corrects mean element rates
        for (int i = 0; i < 6; i++) {
            meanElementRate[i] = meanElementRate[i].multiply(coef);
        }
        return meanElementRate;
    }

    /**
     * Estimates the weighted magnitude of the difference between 2 sets of
     * equinoctial elements rates.
     *
     * @param meanRef reference rates
     * @param meanCur current rates
     * @param context container for attributes
     * @return estimated magnitude of weighted differences
     */
    private double getRatesDiff(final double[] meanRef, final double[] meanCur,
            final AbstractGaussianContributionContext context) {

        // Auxiliary elements related to the current orbit
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

        double maxDiff = FastMath.abs(meanRef[0] - meanCur[0]) / auxiliaryElements.getSma();
        // Corrects mean element rates
        for (int i = 1; i < meanRef.length; i++) {
            maxDiff = FastMath.max(maxDiff, FastMath.abs(meanRef[i] - meanCur[i]));
        }
        return maxDiff;
    }

    /**
     * Estimates the weighted magnitude of the difference between 2 sets of
     * equinoctial elements rates.
     *
     * @param <T>     type of the elements
     * @param meanRef reference rates
     * @param meanCur current rates
     * @param context container for attributes
     * @return estimated magnitude of weighted differences
     */
    private <T extends CalculusFieldElement<T>> T getRatesDiff(final T[] meanRef, final T[] meanCur,
            final FieldAbstractGaussianContributionContext<T> context) {

        // Auxiliary elements related to the current orbit
        final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

        T maxDiff = FastMath.abs(meanRef[0].subtract(meanCur[0])).divide(auxiliaryElements.getSma());
        ;
        // Corrects mean element rates
        for (int i = 1; i < meanRef.length; i++) {
            maxDiff = FastMath.max(maxDiff, FastMath.abs(meanRef[i].subtract(meanCur[i])));
        }
        return maxDiff;
    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider provider) {
        this.attitudeProvider = provider;
    }

    /** {@inheritDoc} */
    @Override
    public void updateShortPeriodTerms(final double[] parameters, final SpacecraftState... meanStates) {

        final Slot slot = gaussianSPCoefs.createSlot(meanStates);
        for (final SpacecraftState meanState : meanStates) {

            // Auxiliary elements related to the current orbit
            final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(meanState.getOrbit(), I);

            // Container of attributes
            // Extract the proper parameters valid for the corresponding meanState date from the input array
            final double[] extractedParameters = this.extractParameters(parameters, auxiliaryElements.getDate());
            final AbstractGaussianContributionContext context = initializeStep(auxiliaryElements, extractedParameters);

            // Compute rhoj and sigmaj
            final double[][] currentRhoSigmaj = computeRhoSigmaCoefficients(meanState.getDate(), auxiliaryElements);

            // Generate the Cij and Sij coefficients
            final FourierCjSjCoefficients fourierCjSj = new FourierCjSjCoefficients(meanState, JMAX, auxiliaryElements,
                                                                                    extractedParameters);

            // Generate the Uij and Vij coefficients
            final UijVijCoefficients uijvij = new UijVijCoefficients(currentRhoSigmaj, fourierCjSj, JMAX);

            gaussianSPCoefs.computeCoefficients(meanState, slot, fourierCjSj, uijvij, context.getMeanMotion(),
                    auxiliaryElements.getSma());

        }

    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends CalculusFieldElement<T>> void updateShortPeriodTerms(final T[] parameters,
            final FieldSpacecraftState<T>... meanStates) {

        // Field used by default
        final Field<T> field = meanStates[0].getDate().getField();

        final FieldGaussianShortPeriodicCoefficients<T> fgspc = (FieldGaussianShortPeriodicCoefficients<T>) gaussianFieldSPCoefs
                .get(field);
        final FieldSlot<T> slot = fgspc.createSlot(meanStates);
        for (final FieldSpacecraftState<T> meanState : meanStates) {

            // Auxiliary elements related to the current orbit
            final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(meanState.getOrbit(), I);

            // Container of attributes
            // Extract the proper parameters valid for the corresponding meanState date from the input array
            final T[] extractedParameters = this.extractParameters(parameters, auxiliaryElements.getDate());
            final FieldAbstractGaussianContributionContext<T> context = initializeStep(auxiliaryElements, extractedParameters);

            // Compute rhoj and sigmaj
            final T[][] currentRhoSigmaj = computeRhoSigmaCoefficients(meanState.getDate(), context, field);

            // Generate the Cij and Sij coefficients
            final FieldFourierCjSjCoefficients<T> fourierCjSj = new FieldFourierCjSjCoefficients<>(meanState, JMAX,
                    auxiliaryElements, extractedParameters, field);

            // Generate the Uij and Vij coefficients
            final FieldUijVijCoefficients<T> uijvij = new FieldUijVijCoefficients<>(currentRhoSigmaj, fourierCjSj, JMAX,
                    field);

            fgspc.computeCoefficients(meanState, slot, fourierCjSj, uijvij, context.getMeanMotion(),
                    auxiliaryElements.getSma(), field);

        }

    }

    /**
     * Compute the auxiliary quantities ρ<sub>j</sub> and σ<sub>j</sub>.
     * <p>
     * The expressions used are equations 2.5.3-(4) from the Danielson paper. <br/>
     * ρ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>C<sub>j</sub>(k, h) <br/>
     * σ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>S<sub>j</sub>(k, h) <br/>
     * </p>
     * @param date              current date
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @return computed coefficients
     */
    private double[][] computeRhoSigmaCoefficients(final AbsoluteDate date, final AuxiliaryElements auxiliaryElements) {
        final double[][] currentRhoSigmaj = new double[2][3 * JMAX + 1];
        final CjSjCoefficient cjsjKH = new CjSjCoefficient(auxiliaryElements.getK(), auxiliaryElements.getH());
        final double b = 1. / (1 + auxiliaryElements.getB());

        // (-b)<sup>j</sup>
        double mbtj = 1;

        for (int j = 1; j <= 3 * JMAX; j++) {

            // Compute current rho and sigma;
            mbtj *= -b;
            final double coef = (1 + j * auxiliaryElements.getB()) * mbtj;
            currentRhoSigmaj[0][j] = coef * cjsjKH.getCj(j);
            currentRhoSigmaj[1][j] = coef * cjsjKH.getSj(j);
        }
        return currentRhoSigmaj;
    }

    /**
     * Compute the auxiliary quantities ρ<sub>j</sub> and σ<sub>j</sub>.
     * <p>
     * The expressions used are equations 2.5.3-(4) from the Danielson paper. <br/>
     * ρ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>C<sub>j</sub>(k, h) <br/>
     * σ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>S<sub>j</sub>(k, h) <br/>
     * </p>
     * @param <T>     type of the elements
     * @param date    current date
     * @param context container for attributes
     * @param field   field used by default
     * @return computed coefficients
     */
    private <T extends CalculusFieldElement<T>> T[][] computeRhoSigmaCoefficients(final FieldAbsoluteDate<T> date,
            final FieldAbstractGaussianContributionContext<T> context, final Field<T> field) {
        // zero
        final T zero = field.getZero();

        final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();
        final T[][] currentRhoSigmaj = MathArrays.buildArray(field, 2, 3 * JMAX + 1);
        final FieldCjSjCoefficient<T> cjsjKH = new FieldCjSjCoefficient<>(auxiliaryElements.getK(),
                auxiliaryElements.getH(), field);
        final T b = auxiliaryElements.getB().add(1.).reciprocal();

        // (-b)<sup>j</sup>
        T mbtj = zero.add(1.);

        for (int j = 1; j <= 3 * JMAX; j++) {

            // Compute current rho and sigma;
            mbtj = mbtj.multiply(b.negate());
            final T coef = mbtj.multiply(auxiliaryElements.getB().multiply(j).add(1.));
            currentRhoSigmaj[0][j] = coef.multiply(cjsjKH.getCj(j));
            currentRhoSigmaj[1][j] = coef.multiply(cjsjKH.getSj(j));
        }
        return currentRhoSigmaj;
    }

    /**
     * Internal class for numerical quadrature.
     * <p>
     * This class is a rewrite of {@link IntegrableFunction} for field elements
     * </p>
     * @param <T> type of the field elements
     */
    protected class FieldIntegrableFunction<T extends CalculusFieldElement<T>>
            implements CalculusFieldUnivariateVectorFunction<T> {

        /** Current state. */
        private final FieldSpacecraftState<T> state;

        /**
         * Signal that this class is used to compute the values required by the mean
         * element variations or by the short periodic element variations.
         */
        private final boolean meanMode;

        /**
         * The j index.
         * <p>
         * Used only for short periodic variation. Ignored for mean elements variation.
         * </p>
         */
        private final int j;

        /** Container for attributes. */
        private final FieldAbstractGaussianContributionContext<T> context;

        /** Auxiliary Elements. */
        private final FieldAuxiliaryElements<T> auxiliaryElements;

        /** Drivers for solar radiation and atmospheric drag forces. */
        private final T[] parameters;

        /**
         * Build a new instance with a new field.
         * @param state      current state information: date, kinematics, attitude
         * @param meanMode   if true return the value associated to the mean elements
         *                   variation, if false return the values associated to the
         *                   short periodic elements variation
         * @param j          the j index. used only for short periodic variation.
         *                   Ignored for mean elements variation.
         * @param parameters values of the force model parameters (only 1 values
         *                   for each parameters corresponding to state date) obtained by
         *                   calling the extract parameter method {@link #extractParameters(double[], AbsoluteDate)}
         *                   to selected the right value for state date or by getting the parameters for a specific date
         * @param field      field utilized by default
         */
        public FieldIntegrableFunction(final FieldSpacecraftState<T> state, final boolean meanMode, final int j,
                final T[] parameters, final Field<T> field) {

            this.meanMode = meanMode;
            this.j = j;
            this.parameters = parameters.clone();
            this.auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), I);
            this.context = new FieldAbstractGaussianContributionContext<>(auxiliaryElements, this.parameters);
            // remove derivatives from state
            final T[] stateVector = MathArrays.buildArray(field, 6);
            OrbitType.EQUINOCTIAL.mapOrbitToArray(state.getOrbit(), PositionAngleType.TRUE, stateVector, null);
            final FieldOrbit<T> fixedOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(stateVector, null,
                    PositionAngleType.TRUE, state.getDate(), context.getMu(), state.getFrame());
            this.state = new FieldSpacecraftState<>(fixedOrbit, state.getAttitude(), state.getMass());
        }

        /** {@inheritDoc} */
        @Override
        public T[] value(final T x) {

            // Parameters for array building
            final Field<T> field = auxiliaryElements.getDate().getField();
            final int dimension = 6;

            // Compute the time difference from the true longitude difference
            final T shiftedLm = trueToMean(x);
            final T dLm = shiftedLm.subtract(auxiliaryElements.getLM());
            final T dt = dLm.divide(context.getMeanMotion());

            final FieldSinCos<T> scL = FastMath.sinCos(x);
            final T cosL = scL.cos();
            final T sinL = scL.sin();
            final T roa  = auxiliaryElements.getB().multiply(auxiliaryElements.getB()).divide(auxiliaryElements.getH().multiply(sinL).add(auxiliaryElements.getK().multiply(cosL)).add(1.));
            final T roa2 = roa.multiply(roa);
            final T r = auxiliaryElements.getSma().multiply(roa);
            final T X = r.multiply(cosL);
            final T Y = r.multiply(sinL);
            final T naob = context.getMeanMotion().multiply(auxiliaryElements.getSma())
                    .divide(auxiliaryElements.getB());
            final T Xdot = naob.multiply(auxiliaryElements.getH().add(sinL)).negate();
            final T Ydot = naob.multiply(auxiliaryElements.getK().add(cosL));
            final FieldVector3D<T> vel = new FieldVector3D<>(Xdot, auxiliaryElements.getVectorF(), Ydot,
                    auxiliaryElements.getVectorG());

            // Compute acceleration
            FieldVector3D<T> acc = FieldVector3D.getZero(field);

            // shift the orbit to dt
            final FieldOrbit<T> shiftedOrbit = state.getOrbit().shiftedBy(dt);

            // Recompose an orbit with time held fixed to be compliant with DSST theory
            final FieldOrbit<T> recomposedOrbit = new FieldEquinoctialOrbit<>(shiftedOrbit.getA(),
                    shiftedOrbit.getEquinoctialEx(), shiftedOrbit.getEquinoctialEy(), shiftedOrbit.getHx(),
                    shiftedOrbit.getHy(), shiftedOrbit.getLv(), PositionAngleType.TRUE, shiftedOrbit.getFrame(),
                    state.getDate(), context.getMu());

            // Get the corresponding attitude
            final FieldAttitude<T> recomposedAttitude = attitudeProvider.getAttitude(recomposedOrbit,
                    recomposedOrbit.getDate(), recomposedOrbit.getFrame());

            // create shifted SpacecraftState with attitude at specified time
            final FieldSpacecraftState<T> shiftedState = new FieldSpacecraftState<>(recomposedOrbit, recomposedAttitude,
                    state.getMass());

            acc = contribution.acceleration(shiftedState, parameters);

            // Compute the derivatives of the elements by the speed
            final T[] deriv = MathArrays.buildArray(field, dimension);
            // da/dv
            deriv[0] = getAoV(vel).dotProduct(acc);
            // dex/dv
            deriv[1] = getKoV(X, Y, Xdot, Ydot).dotProduct(acc);
            // dey/dv
            deriv[2] = getHoV(X, Y, Xdot, Ydot).dotProduct(acc);
            // dhx/dv
            deriv[3] = getQoV(X).dotProduct(acc);
            // dhy/dv
            deriv[4] = getPoV(Y).dotProduct(acc);
            // dλ/dv
            deriv[5] = getLoV(X, Y, Xdot, Ydot).dotProduct(acc);

            // Compute mean elements rates
            T[] val = null;
            if (meanMode) {
                val = MathArrays.buildArray(field, dimension);
                for (int i = 0; i < 6; i++) {
                    // da<sub>i</sub>/dt
                    val[i] = deriv[i].multiply(roa2);
                }
            } else {
                val = MathArrays.buildArray(field, dimension * 2);
                //Compute cos(j*L) and sin(j*L);
                final FieldSinCos<T> scjL = FastMath.sinCos(x.multiply(j));
                final T cosjL = j == 1 ? cosL : scjL.cos();
                final T sinjL = j == 1 ? sinL : scjL.sin();

                for (int i = 0; i < 6; i++) {
                    // da<sub>i</sub>/dv * cos(jL)
                    val[i] = deriv[i].multiply(cosjL);
                    // da<sub>i</sub>/dv * sin(jL)
                    val[i + 6] = deriv[i].multiply(sinjL);
                }
            }

            return val;
        }

        /**
         * Converts true longitude to mean longitude.
         * @param x True longitude
         * @return Eccentric longitude
         */
        private T trueToMean(final T x) {
            return eccentricToMean(trueToEccentric(x));
        }

        /**
         * Converts true longitude to eccentric longitude.
         * @param lv True longitude
         * @return Eccentric longitude
         */
        private T trueToEccentric (final T lv) {
            final FieldSinCos<T> sclV = FastMath.sinCos(lv);
            final T cosLv   = sclV.cos();
            final T sinLv   = sclV.sin();
            final T num     = auxiliaryElements.getH().multiply(cosLv).subtract(auxiliaryElements.getK().multiply(sinLv));
            final T den     = auxiliaryElements.getB().add(auxiliaryElements.getK().multiply(cosLv)).add(auxiliaryElements.getH().multiply(sinLv)).add(1.);
            return FastMath.atan(num.divide(den)).multiply(2.).add(lv);
        }

        /**
         * Converts eccentric longitude to mean longitude.
         * @param le Eccentric longitude
         * @return Mean longitude
         */
        private T eccentricToMean (final T le) {
            final FieldSinCos<T> scle = FastMath.sinCos(le);
            return le.subtract(auxiliaryElements.getK().multiply(scle.sin())).add(auxiliaryElements.getH().multiply(scle.cos()));
        }

        /**
         * Compute δa/δv.
         * @param vel satellite velocity
         * @return δa/δv
         */
        private FieldVector3D<T> getAoV(final FieldVector3D<T> vel) {
            return new FieldVector3D<>(context.getTon2a(), vel);
        }

        /**
         * Compute δh/δv.
         * @param X    satellite position component along f, equinoctial reference frame
         *             1st vector
         * @param Y    satellite position component along g, equinoctial reference frame
         *             2nd vector
         * @param Xdot satellite velocity component along f, equinoctial reference frame
         *             1st vector
         * @param Ydot satellite velocity component along g, equinoctial reference frame
         *             2nd vector
         * @return δh/δv
         */
        private FieldVector3D<T> getHoV(final T X, final T Y, final T Xdot, final T Ydot) {
            final T kf = (Xdot.multiply(Y).multiply(2.).subtract(X.multiply(Ydot))).multiply(context.getOoMU());
            final T kg = X.multiply(Xdot).multiply(context.getOoMU());
            final T kw = auxiliaryElements.getK().multiply(
                    auxiliaryElements.getQ().multiply(Y).multiply(I).subtract(auxiliaryElements.getP().multiply(X)))
                    .multiply(context.getOOAB());
            return new FieldVector3D<>(kf, auxiliaryElements.getVectorF(), kg.negate(), auxiliaryElements.getVectorG(),
                    kw, auxiliaryElements.getVectorW());
        }

        /**
         * Compute δk/δv.
         * @param X    satellite position component along f, equinoctial reference frame
         *             1st vector
         * @param Y    satellite position component along g, equinoctial reference frame
         *             2nd vector
         * @param Xdot satellite velocity component along f, equinoctial reference frame
         *             1st vector
         * @param Ydot satellite velocity component along g, equinoctial reference frame
         *             2nd vector
         * @return δk/δv
         */
        private FieldVector3D<T> getKoV(final T X, final T Y, final T Xdot, final T Ydot) {
            final T kf = Y.multiply(Ydot).multiply(context.getOoMU());
            final T kg = (X.multiply(Ydot).multiply(2.).subtract(Xdot.multiply(Y))).multiply(context.getOoMU());
            final T kw = auxiliaryElements.getH().multiply(
                    auxiliaryElements.getQ().multiply(Y).multiply(I).subtract(auxiliaryElements.getP().multiply(X)))
                    .multiply(context.getOOAB());
            return new FieldVector3D<>(kf.negate(), auxiliaryElements.getVectorF(), kg, auxiliaryElements.getVectorG(),
                    kw.negate(), auxiliaryElements.getVectorW());
        }

        /**
         * Compute δp/δv.
         * @param Y satellite position component along g, equinoctial reference frame
         *          2nd vector
         * @return δp/δv
         */
        private FieldVector3D<T> getPoV(final T Y) {
            return new FieldVector3D<>(context.getCo2AB().multiply(Y), auxiliaryElements.getVectorW());
        }

        /**
         * Compute δq/δv.
         * @param X satellite position component along f, equinoctial reference frame
         *          1st vector
         * @return δq/δv
         */
        private FieldVector3D<T> getQoV(final T X) {
            return new FieldVector3D<>(context.getCo2AB().multiply(X).multiply(I), auxiliaryElements.getVectorW());
        }

        /**
         * Compute δλ/δv.
         * @param X    satellite position component along f, equinoctial reference frame
         *             1st vector
         * @param Y    satellite position component along g, equinoctial reference frame
         *             2nd vector
         * @param Xdot satellite velocity component along f, equinoctial reference frame
         *             1st vector
         * @param Ydot satellite velocity component along g, equinoctial reference frame
         *             2nd vector
         * @return δλ/δv
         */
        private FieldVector3D<T> getLoV(final T X, final T Y, final T Xdot, final T Ydot) {
            final FieldVector3D<T> pos = new FieldVector3D<>(X, auxiliaryElements.getVectorF(), Y,
                    auxiliaryElements.getVectorG());
            final FieldVector3D<T> v2 = new FieldVector3D<>(auxiliaryElements.getK(), getHoV(X, Y, Xdot, Ydot),
                    auxiliaryElements.getH().negate(), getKoV(X, Y, Xdot, Ydot));
            return new FieldVector3D<>(context.getOOA().multiply(-2.), pos, context.getOoBpo(), v2,
                    context.getOOA().multiply(auxiliaryElements.getQ().multiply(Y).multiply(I)
                            .subtract(auxiliaryElements.getP().multiply(X))),
                    auxiliaryElements.getVectorW());
        }

    }

    /** Internal class for numerical quadrature. */
    protected class IntegrableFunction implements UnivariateVectorFunction {

        /** Current state. */
        private final SpacecraftState state;

        /**
         * Signal that this class is used to compute the values required by the mean
         * element variations or by the short periodic element variations.
         */
        private final boolean meanMode;

        /**
         * The j index.
         * <p>
         * Used only for short periodic variation. Ignored for mean elements variation.
         * </p>
         */
        private final int j;

        /** Container for attributes. */
        private final AbstractGaussianContributionContext context;

        /** Auxiliary Elements. */
        private final AuxiliaryElements auxiliaryElements;

        /** Drivers for solar radiation and atmospheric drag forces. */
        private final double[] parameters;

        /**
         * Build a new instance.
         * @param state      current state information: date, kinematics, attitude
         * @param meanMode   if true return the value associated to the mean elements
         *                   variation, if false return the values associated to the
         *                   short periodic elements variation
         * @param j          the j index. used only for short periodic variation.
         *                   Ignored for mean elements variation.
         * @param parameters list of the estimated values for each driver at state date of the force model parameters
         *                   only 1 value for each parameter
         */
        IntegrableFunction(final SpacecraftState state, final boolean meanMode, final int j,
                final double[] parameters) {

            this.meanMode = meanMode;
            this.j = j;
            this.parameters = parameters.clone();
            this.auxiliaryElements = new AuxiliaryElements(state.getOrbit(), I);
            this.context = new AbstractGaussianContributionContext(auxiliaryElements, this.parameters);
            // remove derivatives from state
            final double[] stateVector = new double[6];
            OrbitType.EQUINOCTIAL.mapOrbitToArray(state.getOrbit(), PositionAngleType.TRUE, stateVector, null);
            final Orbit fixedOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(stateVector, null, PositionAngleType.TRUE,
                    state.getDate(), context.getMu(), state.getFrame());
            this.state = new SpacecraftState(fixedOrbit, state.getAttitude(), state.getMass());
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final double x) {

            // Compute the time difference from the true longitude difference
            final double shiftedLm = trueToMean(x);
            final double dLm = shiftedLm - auxiliaryElements.getLM();
            final double dt = dLm / context.getMeanMotion();

            final SinCos scL  = FastMath.sinCos(x);
            final double cosL = scL.cos();
            final double sinL = scL.sin();
            final double roa  = auxiliaryElements.getB() * auxiliaryElements.getB() / (1. + auxiliaryElements.getH() * sinL + auxiliaryElements.getK() * cosL);
            final double roa2 = roa * roa;
            final double r = auxiliaryElements.getSma() * roa;
            final double X = r * cosL;
            final double Y = r * sinL;
            final double naob = context.getMeanMotion() * auxiliaryElements.getSma() / auxiliaryElements.getB();
            final double Xdot = -naob * (auxiliaryElements.getH() + sinL);
            final double Ydot = naob * (auxiliaryElements.getK() + cosL);
            final Vector3D vel = new Vector3D(Xdot, auxiliaryElements.getVectorF(), Ydot,
                    auxiliaryElements.getVectorG());

            // Compute acceleration
            Vector3D acc = Vector3D.ZERO;

            // shift the orbit to dt
            final Orbit shiftedOrbit = state.getOrbit().shiftedBy(dt);

            // Recompose an orbit with time held fixed to be compliant with DSST theory
            final Orbit recomposedOrbit = new EquinoctialOrbit(shiftedOrbit.getA(), shiftedOrbit.getEquinoctialEx(),
                    shiftedOrbit.getEquinoctialEy(), shiftedOrbit.getHx(), shiftedOrbit.getHy(), shiftedOrbit.getLv(),
                    PositionAngleType.TRUE, shiftedOrbit.getFrame(), state.getDate(), context.getMu());

            // Get the corresponding attitude
            final Attitude recomposedAttitude = attitudeProvider.getAttitude(recomposedOrbit, recomposedOrbit.getDate(),
                    recomposedOrbit.getFrame());

            // create shifted SpacecraftState with attitude at specified time
            final SpacecraftState shiftedState = new SpacecraftState(recomposedOrbit, recomposedAttitude,
                    state.getMass());

            // here parameters is a list of all span values of each parameter driver
            acc = contribution.acceleration(shiftedState, parameters);

            // Compute the derivatives of the elements by the speed
            final double[] deriv = new double[6];
            // da/dv
            deriv[0] = getAoV(vel).dotProduct(acc);
            // dex/dv
            deriv[1] = getKoV(X, Y, Xdot, Ydot).dotProduct(acc);
            // dey/dv
            deriv[2] = getHoV(X, Y, Xdot, Ydot).dotProduct(acc);
            // dhx/dv
            deriv[3] = getQoV(X).dotProduct(acc);
            // dhy/dv
            deriv[4] = getPoV(Y).dotProduct(acc);
            // dλ/dv
            deriv[5] = getLoV(X, Y, Xdot, Ydot).dotProduct(acc);

            // Compute mean elements rates
            double[] val = null;
            if (meanMode) {
                val = new double[6];
                for (int i = 0; i < 6; i++) {
                    // da<sub>i</sub>/dt
                    val[i] = roa2 * deriv[i];
                }
            } else {
                val = new double[12];
                //Compute cos(j*L) and sin(j*L);
                final SinCos scjL  = FastMath.sinCos(j * x);
                final double cosjL = j == 1 ? cosL : scjL.cos();
                final double sinjL = j == 1 ? sinL : scjL.sin();

                for (int i = 0; i < 6; i++) {
                    // da<sub>i</sub>/dv * cos(jL)
                    val[i] = cosjL * deriv[i];
                    // da<sub>i</sub>/dv * sin(jL)
                    val[i + 6] = sinjL * deriv[i];
                }
            }
            return val;
        }

        /**
         * Converts true longitude to eccentric longitude.
         * @param lv True longitude
         * @return Eccentric longitude
         */
        private double trueToEccentric (final double lv) {
            final SinCos scLv    = FastMath.sinCos(lv);
            final double num     = auxiliaryElements.getH() * scLv.cos() - auxiliaryElements.getK() * scLv.sin();
            final double den     = auxiliaryElements.getB() + 1. + auxiliaryElements.getK() * scLv.cos() + auxiliaryElements.getH() * scLv.sin();
            return lv + 2. * FastMath.atan(num / den);
        }

        /**
         * Converts eccentric longitude to mean longitude.
         * @param le Eccentric longitude
         * @return Mean longitude
         */
        private double eccentricToMean (final double le) {
            final SinCos scLe = FastMath.sinCos(le);
            return le - auxiliaryElements.getK() * scLe.sin() + auxiliaryElements.getH() * scLe.cos();
        }

        /**
         * Converts true longitude to mean longitude.
         * @param lv True longitude
         * @return Eccentric longitude
         */
        private double trueToMean(final double lv) {
            return eccentricToMean(trueToEccentric(lv));
        }

        /**
         * Compute δa/δv.
         * @param vel satellite velocity
         * @return δa/δv
         */
        private Vector3D getAoV(final Vector3D vel) {
            return new Vector3D(context.getTon2a(), vel);
        }

        /**
         * Compute δh/δv.
         * @param X    satellite position component along f, equinoctial reference frame
         *             1st vector
         * @param Y    satellite position component along g, equinoctial reference frame
         *             2nd vector
         * @param Xdot satellite velocity component along f, equinoctial reference frame
         *             1st vector
         * @param Ydot satellite velocity component along g, equinoctial reference frame
         *             2nd vector
         * @return δh/δv
         */
        private Vector3D getHoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final double kf = (2. * Xdot * Y - X * Ydot) * context.getOoMU();
            final double kg = X * Xdot * context.getOoMU();
            final double kw = auxiliaryElements.getK() *
                    (I * auxiliaryElements.getQ() * Y - auxiliaryElements.getP() * X) * context.getOOAB();
            return new Vector3D(kf, auxiliaryElements.getVectorF(), -kg, auxiliaryElements.getVectorG(), kw,
                    auxiliaryElements.getVectorW());
        }

        /**
         * Compute δk/δv.
         * @param X    satellite position component along f, equinoctial reference frame
         *             1st vector
         * @param Y    satellite position component along g, equinoctial reference frame
         *             2nd vector
         * @param Xdot satellite velocity component along f, equinoctial reference frame
         *             1st vector
         * @param Ydot satellite velocity component along g, equinoctial reference frame
         *             2nd vector
         * @return δk/δv
         */
        private Vector3D getKoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final double kf = Y * Ydot * context.getOoMU();
            final double kg = (2. * X * Ydot - Xdot * Y) * context.getOoMU();
            final double kw = auxiliaryElements.getH() *
                    (I * auxiliaryElements.getQ() * Y - auxiliaryElements.getP() * X) * context.getOOAB();
            return new Vector3D(-kf, auxiliaryElements.getVectorF(), kg, auxiliaryElements.getVectorG(), -kw,
                    auxiliaryElements.getVectorW());
        }

        /**
         * Compute δp/δv.
         * @param Y satellite position component along g, equinoctial reference frame
         *          2nd vector
         * @return δp/δv
         */
        private Vector3D getPoV(final double Y) {
            return new Vector3D(context.getCo2AB() * Y, auxiliaryElements.getVectorW());
        }

        /**
         * Compute δq/δv.
         * @param X satellite position component along f, equinoctial reference frame
         *          1st vector
         * @return δq/δv
         */
        private Vector3D getQoV(final double X) {
            return new Vector3D(I * context.getCo2AB() * X, auxiliaryElements.getVectorW());
        }

        /**
         * Compute δλ/δv.
         * @param X    satellite position component along f, equinoctial reference frame
         *             1st vector
         * @param Y    satellite position component along g, equinoctial reference frame
         *             2nd vector
         * @param Xdot satellite velocity component along f, equinoctial reference frame
         *             1st vector
         * @param Ydot satellite velocity component along g, equinoctial reference frame
         *             2nd vector
         * @return δλ/δv
         */
        private Vector3D getLoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final Vector3D pos = new Vector3D(X, auxiliaryElements.getVectorF(), Y, auxiliaryElements.getVectorG());
            final Vector3D v2 = new Vector3D(auxiliaryElements.getK(), getHoV(X, Y, Xdot, Ydot),
                    -auxiliaryElements.getH(), getKoV(X, Y, Xdot, Ydot));
            return new Vector3D(-2. * context.getOOA(), pos, context.getOoBpo(), v2,
                    (I * auxiliaryElements.getQ() * Y - auxiliaryElements.getP() * X) * context.getOOA(),
                    auxiliaryElements.getVectorW());
        }

    }

    /**
     * Class used to {@link #integrate(UnivariateVectorFunction, double, double)
     * integrate} a {@link org.hipparchus.analysis.UnivariateVectorFunction
     * function} of the orbital elements using the Gaussian quadrature rule to get
     * the acceleration.
     */
    protected static class GaussQuadrature {

        // Points and weights for the available quadrature orders

        /** Points for quadrature of order 12. */
        private static final double[] P_12 = { -0.98156063424671910000, -0.90411725637047490000,
            -0.76990267419430470000, -0.58731795428661740000, -0.36783149899818024000, -0.12523340851146890000,
            0.12523340851146890000, 0.36783149899818024000, 0.58731795428661740000, 0.76990267419430470000,
            0.90411725637047490000, 0.98156063424671910000 };

        /** Weights for quadrature of order 12. */
        private static final double[] W_12 = { 0.04717533638651220000, 0.10693932599531830000, 0.16007832854334633000,
            0.20316742672306584000, 0.23349253653835478000, 0.24914704581340286000, 0.24914704581340286000,
            0.23349253653835478000, 0.20316742672306584000, 0.16007832854334633000, 0.10693932599531830000,
            0.04717533638651220000 };

        /** Points for quadrature of order 16. */
        private static final double[] P_16 = { -0.98940093499164990000, -0.94457502307323260000,
            -0.86563120238783160000, -0.75540440835500310000, -0.61787624440264380000, -0.45801677765722737000,
            -0.28160355077925890000, -0.09501250983763745000, 0.09501250983763745000, 0.28160355077925890000,
            0.45801677765722737000, 0.61787624440264380000, 0.75540440835500310000, 0.86563120238783160000,
            0.94457502307323260000, 0.98940093499164990000 };

        /** Weights for quadrature of order 16. */
        private static final double[] W_16 = { 0.02715245941175405800, 0.06225352393864777000, 0.09515851168249283000,
            0.12462897125553388000, 0.14959598881657685000, 0.16915651939500256000, 0.18260341504492360000,
            0.18945061045506847000, 0.18945061045506847000, 0.18260341504492360000, 0.16915651939500256000,
            0.14959598881657685000, 0.12462897125553388000, 0.09515851168249283000, 0.06225352393864777000,
            0.02715245941175405800 };

        /** Points for quadrature of order 20. */
        private static final double[] P_20 = { -0.99312859918509490000, -0.96397192727791390000,
            -0.91223442825132600000, -0.83911697182221890000, -0.74633190646015080000, -0.63605368072651510000,
            -0.51086700195082700000, -0.37370608871541955000, -0.22778585114164507000, -0.07652652113349734000,
            0.07652652113349734000, 0.22778585114164507000, 0.37370608871541955000, 0.51086700195082700000,
            0.63605368072651510000, 0.74633190646015080000, 0.83911697182221890000, 0.91223442825132600000,
            0.96397192727791390000, 0.99312859918509490000 };

        /** Weights for quadrature of order 20. */
        private static final double[] W_20 = { 0.01761400713915226400, 0.04060142980038684000, 0.06267204833410904000,
            0.08327674157670477000, 0.10193011981724048000, 0.11819453196151844000, 0.13168863844917678000,
            0.14209610931838212000, 0.14917298647260380000, 0.15275338713072600000, 0.15275338713072600000,
            0.14917298647260380000, 0.14209610931838212000, 0.13168863844917678000, 0.11819453196151844000,
            0.10193011981724048000, 0.08327674157670477000, 0.06267204833410904000, 0.04060142980038684000,
            0.01761400713915226400 };

        /** Points for quadrature of order 24. */
        private static final double[] P_24 = { -0.99518721999702130000, -0.97472855597130950000,
            -0.93827455200273270000, -0.88641552700440100000, -0.82000198597390300000, -0.74012419157855440000,
            -0.64809365193697550000, -0.54542147138883950000, -0.43379350762604520000, -0.31504267969616340000,
            -0.19111886747361634000, -0.06405689286260563000, 0.06405689286260563000, 0.19111886747361634000,
            0.31504267969616340000, 0.43379350762604520000, 0.54542147138883950000, 0.64809365193697550000,
            0.74012419157855440000, 0.82000198597390300000, 0.88641552700440100000, 0.93827455200273270000,
            0.97472855597130950000, 0.99518721999702130000 };

        /** Weights for quadrature of order 24. */
        private static final double[] W_24 = { 0.01234122979998733500, 0.02853138862893380600, 0.04427743881741981000,
            0.05929858491543691500, 0.07334648141108027000, 0.08619016153195320000, 0.09761865210411391000,
            0.10744427011596558000, 0.11550566805372553000, 0.12167047292780335000, 0.12583745634682825000,
            0.12793819534675221000, 0.12793819534675221000, 0.12583745634682825000, 0.12167047292780335000,
            0.11550566805372553000, 0.10744427011596558000, 0.09761865210411391000, 0.08619016153195320000,
            0.07334648141108027000, 0.05929858491543691500, 0.04427743881741981000, 0.02853138862893380600,
            0.01234122979998733500 };

        /** Points for quadrature of order 32. */
        private static final double[] P_32 = { -0.99726386184948160000, -0.98561151154526840000,
            -0.96476225558750640000, -0.93490607593773970000, -0.89632115576605220000, -0.84936761373256990000,
            -0.79448379596794250000, -0.73218211874028970000, -0.66304426693021520000, -0.58771575724076230000,
            -0.50689990893222950000, -0.42135127613063540000, -0.33186860228212767000, -0.23928736225213710000,
            -0.14447196158279646000, -0.04830766568773831000, 0.04830766568773831000, 0.14447196158279646000,
            0.23928736225213710000, 0.33186860228212767000, 0.42135127613063540000, 0.50689990893222950000,
            0.58771575724076230000, 0.66304426693021520000, 0.73218211874028970000, 0.79448379596794250000,
            0.84936761373256990000, 0.89632115576605220000, 0.93490607593773970000, 0.96476225558750640000,
            0.98561151154526840000, 0.99726386184948160000 };

        /** Weights for quadrature of order 32. */
        private static final double[] W_32 = { 0.00701861000947013600, 0.01627439473090571200, 0.02539206530926214200,
            0.03427386291302141000, 0.04283589802222658600, 0.05099805926237621600, 0.05868409347853559000,
            0.06582222277636193000, 0.07234579410884862000, 0.07819389578707042000, 0.08331192422694673000,
            0.08765209300440380000, 0.09117387869576390000, 0.09384439908080441000, 0.09563872007927487000,
            0.09654008851472784000, 0.09654008851472784000, 0.09563872007927487000, 0.09384439908080441000,
            0.09117387869576390000, 0.08765209300440380000, 0.08331192422694673000, 0.07819389578707042000,
            0.07234579410884862000, 0.06582222277636193000, 0.05868409347853559000, 0.05099805926237621600,
            0.04283589802222658600, 0.03427386291302141000, 0.02539206530926214200, 0.01627439473090571200,
            0.00701861000947013600 };

        /** Points for quadrature of order 40. */
        private static final double[] P_40 = { -0.99823770971055930000, -0.99072623869945710000,
            -0.97725994998377420000, -0.95791681921379170000, -0.93281280827867660000, -0.90209880696887420000,
            -0.86595950321225960000, -0.82461223083331170000, -0.77830565142651940000, -0.72731825518992710000,
            -0.67195668461417960000, -0.61255388966798030000, -0.54946712509512820000, -0.48307580168617870000,
            -0.41377920437160500000, -0.34199409082575850000, -0.26815218500725370000, -0.19269758070137110000,
            -0.11608407067525522000, -0.03877241750605081600, 0.03877241750605081600, 0.11608407067525522000,
            0.19269758070137110000, 0.26815218500725370000, 0.34199409082575850000, 0.41377920437160500000,
            0.48307580168617870000, 0.54946712509512820000, 0.61255388966798030000, 0.67195668461417960000,
            0.72731825518992710000, 0.77830565142651940000, 0.82461223083331170000, 0.86595950321225960000,
            0.90209880696887420000, 0.93281280827867660000, 0.95791681921379170000, 0.97725994998377420000,
            0.99072623869945710000, 0.99823770971055930000 };

        /** Weights for quadrature of order 40. */
        private static final double[] W_40 = { 0.00452127709853309800, 0.01049828453115270400, 0.01642105838190797300,
            0.02224584919416689000, 0.02793700698002338000, 0.03346019528254786500, 0.03878216797447199000,
            0.04387090818567333000, 0.04869580763507221000, 0.05322784698393679000, 0.05743976909939157000,
            0.06130624249292891000, 0.06480401345660108000, 0.06791204581523394000, 0.07061164739128681000,
            0.07288658239580408000, 0.07472316905796833000, 0.07611036190062619000, 0.07703981816424793000,
            0.07750594797842482000, 0.07750594797842482000, 0.07703981816424793000, 0.07611036190062619000,
            0.07472316905796833000, 0.07288658239580408000, 0.07061164739128681000, 0.06791204581523394000,
            0.06480401345660108000, 0.06130624249292891000, 0.05743976909939157000, 0.05322784698393679000,
            0.04869580763507221000, 0.04387090818567333000, 0.03878216797447199000, 0.03346019528254786500,
            0.02793700698002338000, 0.02224584919416689000, 0.01642105838190797300, 0.01049828453115270400,
            0.00452127709853309800 };

        /** Points for quadrature of order 48. */
        private static final double[] P_48 = { -0.99877100725242610000, -0.99353017226635080000,
            -0.98412458372282700000, -0.97059159254624720000, -0.95298770316043080000, -0.93138669070655440000,
            -0.90587913671556960000, -0.87657202027424800000, -0.84358826162439350000, -0.80706620402944250000,
            -0.76715903251574020000, -0.72403413092381470000, -0.67787237963266400000, -0.62886739677651370000,
            -0.57722472608397270000, -0.52316097472223300000, -0.46690290475095840000, -0.40868648199071680000,
            -0.34875588629216070000, -0.28736248735545555000, -0.22476379039468908000, -0.16122235606889174000,
            -0.09700469920946270000, -0.03238017096286937000, 0.03238017096286937000, 0.09700469920946270000,
            0.16122235606889174000, 0.22476379039468908000, 0.28736248735545555000, 0.34875588629216070000,
            0.40868648199071680000, 0.46690290475095840000, 0.52316097472223300000, 0.57722472608397270000,
            0.62886739677651370000, 0.67787237963266400000, 0.72403413092381470000, 0.76715903251574020000,
            0.80706620402944250000, 0.84358826162439350000, 0.87657202027424800000, 0.90587913671556960000,
            0.93138669070655440000, 0.95298770316043080000, 0.97059159254624720000, 0.98412458372282700000,
            0.99353017226635080000, 0.99877100725242610000 };

        /** Weights for quadrature of order 48. */
        private static final double[] W_48 = { 0.00315334605230596250, 0.00732755390127620800, 0.01147723457923446900,
            0.01557931572294386600, 0.01961616045735556700, 0.02357076083932435600, 0.02742650970835688000,
            0.03116722783279807000, 0.03477722256477045000, 0.03824135106583080600, 0.04154508294346483000,
            0.04467456085669424000, 0.04761665849249054000, 0.05035903555385448000, 0.05289018948519365000,
            0.05519950369998416500, 0.05727729210040315000, 0.05911483969839566000, 0.06070443916589384000,
            0.06203942315989268000, 0.06311419228625403000, 0.06392423858464817000, 0.06446616443595010000,
            0.06473769681268386000, 0.06473769681268386000, 0.06446616443595010000, 0.06392423858464817000,
            0.06311419228625403000, 0.06203942315989268000, 0.06070443916589384000, 0.05911483969839566000,
            0.05727729210040315000, 0.05519950369998416500, 0.05289018948519365000, 0.05035903555385448000,
            0.04761665849249054000, 0.04467456085669424000, 0.04154508294346483000, 0.03824135106583080600,
            0.03477722256477045000, 0.03116722783279807000, 0.02742650970835688000, 0.02357076083932435600,
            0.01961616045735556700, 0.01557931572294386600, 0.01147723457923446900, 0.00732755390127620800,
            0.00315334605230596250 };

        /** Node points. */
        private final double[] nodePoints;

        /** Node weights. */
        private final double[] nodeWeights;

        /** Number of points. */
        private final int numberOfPoints;

        /**
         * Creates a Gauss integrator of the given order.
         *
         * @param numberOfPoints Order of the integration rule.
         */
        GaussQuadrature(final int numberOfPoints) {

            this.numberOfPoints = numberOfPoints;

            switch (numberOfPoints) {
                case 12:
                    this.nodePoints = P_12.clone();
                    this.nodeWeights = W_12.clone();
                    break;
                case 16:
                    this.nodePoints = P_16.clone();
                    this.nodeWeights = W_16.clone();
                    break;
                case 20:
                    this.nodePoints = P_20.clone();
                    this.nodeWeights = W_20.clone();
                    break;
                case 24:
                    this.nodePoints = P_24.clone();
                    this.nodeWeights = W_24.clone();
                    break;
                case 32:
                    this.nodePoints = P_32.clone();
                    this.nodeWeights = W_32.clone();
                    break;
                case 40:
                    this.nodePoints = P_40.clone();
                    this.nodeWeights = W_40.clone();
                    break;
                case 48:
                default:
                    this.nodePoints = P_48.clone();
                    this.nodeWeights = W_48.clone();
                    break;
            }

        }

        /**
         * Integrates a given function on the given interval.
         *
         * @param f          Function to integrate.
         * @param lowerBound Lower bound of the integration interval.
         * @param upperBound Upper bound of the integration interval.
         * @return the integral of the weighted function.
         */
        public double[] integrate(final UnivariateVectorFunction f, final double lowerBound, final double upperBound) {

            final double[] adaptedPoints = nodePoints.clone();
            final double[] adaptedWeights = nodeWeights.clone();
            transform(adaptedPoints, adaptedWeights, lowerBound, upperBound);
            return basicIntegrate(f, adaptedPoints, adaptedWeights);
        }

        /**
         * Integrates a given function on the given interval.
         *
         * @param <T>        the type of the field elements
         * @param f          Function to integrate.
         * @param lowerBound Lower bound of the integration interval.
         * @param upperBound Upper bound of the integration interval.
         * @param field      field utilized by default
         * @return the integral of the weighted function.
         */
        public <T extends CalculusFieldElement<T>> T[] integrate(final CalculusFieldUnivariateVectorFunction<T> f,
                final T lowerBound, final T upperBound, final Field<T> field) {

            final T zero = field.getZero();

            final T[] adaptedPoints = MathArrays.buildArray(field, numberOfPoints);
            final T[] adaptedWeights = MathArrays.buildArray(field, numberOfPoints);

            for (int i = 0; i < numberOfPoints; i++) {
                adaptedPoints[i] = zero.add(nodePoints[i]);
                adaptedWeights[i] = zero.add(nodeWeights[i]);
            }

            transform(adaptedPoints, adaptedWeights, lowerBound, upperBound);
            return basicIntegrate(f, adaptedPoints, adaptedWeights, field);
        }

        /**
         * Performs a change of variable so that the integration can be performed on an
         * arbitrary interval {@code [a, b]}.
         * <p>
         * It is assumed that the natural interval is {@code [-1, 1]}.
         * </p>
         *
         * @param points  Points to adapt to the new interval.
         * @param weights Weights to adapt to the new interval.
         * @param a       Lower bound of the integration interval.
         * @param b       Lower bound of the integration interval.
         */
        private void transform(final double[] points, final double[] weights, final double a, final double b) {
            // Scaling
            final double scale = (b - a) / 2;
            final double shift = a + scale;
            for (int i = 0; i < points.length; i++) {
                points[i] = points[i] * scale + shift;
                weights[i] *= scale;
            }
        }

        /**
         * Performs a change of variable so that the integration can be performed on an
         * arbitrary interval {@code [a, b]}.
         * <p>
         * It is assumed that the natural interval is {@code [-1, 1]}.
         * </p>
         * @param <T>     the type of the field elements
         * @param points  Points to adapt to the new interval.
         * @param weights Weights to adapt to the new interval.
         * @param a       Lower bound of the integration interval.
         * @param b       Lower bound of the integration interval
         */
        private <T extends CalculusFieldElement<T>> void transform(final T[] points, final T[] weights, final T a,
                final T b) {
            // Scaling
            final T scale = (b.subtract(a)).divide(2.);
            final T shift = a.add(scale);
            for (int i = 0; i < points.length; i++) {
                points[i] = scale.multiply(points[i]).add(shift);
                weights[i] = scale.multiply(weights[i]);
            }
        }

        /**
         * Returns an estimate of the integral of {@code f(x) * w(x)}, where {@code w}
         * is a weight function that depends on the actual flavor of the Gauss
         * integration scheme.
         *
         * @param f       Function to integrate.
         * @param points  Nodes.
         * @param weights Nodes weights.
         * @return the integral of the weighted function.
         */
        private double[] basicIntegrate(final UnivariateVectorFunction f, final double[] points,
                final double[] weights) {
            double x = points[0];
            double w = weights[0];
            double[] v = f.value(x);
            final double[] y = new double[v.length];
            for (int j = 0; j < v.length; j++) {
                y[j] = w * v[j];
            }
            final double[] t = y.clone();
            final double[] c = new double[v.length];
            final double[] s = t.clone();
            for (int i = 1; i < points.length; i++) {
                x = points[i];
                w = weights[i];
                v = f.value(x);
                for (int j = 0; j < v.length; j++) {
                    y[j] = w * v[j] - c[j];
                    t[j] = s[j] + y[j];
                    c[j] = (t[j] - s[j]) - y[j];
                    s[j] = t[j];
                }
            }
            return s;
        }

        /**
         * Returns an estimate of the integral of {@code f(x) * w(x)}, where {@code w}
         * is a weight function that depends on the actual flavor of the Gauss
         * integration scheme.
         *
         * @param <T>     the type of the field elements.
         * @param f       Function to integrate.
         * @param points  Nodes.
         * @param weights Nodes weight
         * @param field   field utilized by default
         * @return the integral of the weighted function.
         */
        private <T extends CalculusFieldElement<T>> T[] basicIntegrate(final CalculusFieldUnivariateVectorFunction<T> f,
                final T[] points, final T[] weights, final Field<T> field) {

            T x = points[0];
            T w = weights[0];
            T[] v = f.value(x);

            final T[] y = MathArrays.buildArray(field, v.length);
            for (int j = 0; j < v.length; j++) {
                y[j] = v[j].multiply(w);
            }
            final T[] t = y.clone();
            final T[] c = MathArrays.buildArray(field, v.length);
            ;
            final T[] s = t.clone();
            for (int i = 1; i < points.length; i++) {
                x = points[i];
                w = weights[i];
                v = f.value(x);
                for (int j = 0; j < v.length; j++) {
                    y[j] = v[j].multiply(w).subtract(c[j]);
                    t[j] = y[j].add(s[j]);
                    c[j] = (t[j].subtract(s[j])).subtract(y[j]);
                    s[j] = t[j];
                }
            }
            return s;
        }

    }

    /**
     * Compute the C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup>
     * coefficients.
     * <p>
     * Those coefficients are given in Danielson paper by expression 4.4-(6)
     * </p>
     * @author Petre Bazavan
     * @author Lucian Barbulescu
     */
    protected class FourierCjSjCoefficients {

        /** Maximum possible value for j. */
        private final int jMax;

        /**
         * The C<sub>i</sub><sup>j</sup> coefficients.
         * <p>
         * the index i corresponds to the following elements: <br/>
         * - 0 for a <br>
         * - 1 for k <br>
         * - 2 for h <br>
         * - 3 for q <br>
         * - 4 for p <br>
         * - 5 for λ <br>
         * </p>
         */
        private final double[][] cCoef;

        /**
         * The C<sub>i</sub><sup>j</sup> coefficients.
         * <p>
         * the index i corresponds to the following elements: <br/>
         * - 0 for a <br>
         * - 1 for k <br>
         * - 2 for h <br>
         * - 3 for q <br>
         * - 4 for p <br>
         * - 5 for λ <br>
         * </p>
         */
        private final double[][] sCoef;

        /**
         * Standard constructor.
         * @param state             the current state
         * @param jMax              maximum value for j
         * @param auxiliaryElements auxiliary elements related to the current orbit
         * @param parameters        list of parameter values at state date for each driver
         * of the force model parameters (1 value per parameter)
         */
        FourierCjSjCoefficients(final SpacecraftState state, final int jMax, final AuxiliaryElements auxiliaryElements,
                final double[] parameters) {

            // Initialise the fields
            this.jMax = jMax;

            // Allocate the arrays
            final int rows = jMax + 1;
            cCoef = new double[rows][6];
            sCoef = new double[rows][6];

            // Compute the coefficients
            computeCoefficients(state, auxiliaryElements, parameters);
        }

        /**
         * Compute the Fourrier coefficients.
         * <p>
         * Only the C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> coefficients
         * need to be computed as D<sub>i</sub><sup>m</sup> is always 0.
         * </p>
         * @param state             the current state
         * @param auxiliaryElements auxiliary elements related to the current orbit
         * @param parameters        list of parameter values at state date for each driver
         * of the force model parameters (1 value per parameter)
         */
        private void computeCoefficients(final SpacecraftState state, final AuxiliaryElements auxiliaryElements,
                final double[] parameters) {

            // Computes the limits for the integral
            final double[] ll = getLLimits(state, auxiliaryElements);
            // Computes integrated mean element rates if Llow < Lhigh
            if (ll[0] < ll[1]) {
                // Compute 1 / PI
                final double ooPI = 1 / FastMath.PI;

                // loop through all values of j
                for (int j = 0; j <= jMax; j++) {
                    final double[] curentCoefficients = integrator
                            .integrate(new IntegrableFunction(state, false, j, parameters), ll[0], ll[1]);

                    // divide by PI and set the values for the coefficients
                    for (int i = 0; i < 6; i++) {
                        cCoef[j][i] = ooPI * curentCoefficients[i];
                        sCoef[j][i] = ooPI * curentCoefficients[i + 6];
                    }
                }
            }
        }

        /**
         * Get the coefficient C<sub>i</sub><sup>j</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @return the coefficient C<sub>i</sub><sup>j</sup>
         */
        public double getCij(final int i, final int j) {
            return cCoef[j][i];
        }

        /**
         * Get the coefficient S<sub>i</sub><sup>j</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @return the coefficient S<sub>i</sub><sup>j</sup>
         */
        public double getSij(final int i, final int j) {
            return sCoef[j][i];
        }
    }

    /**
     * Compute the C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup>
     * coefficients with field elements.
     * <p>
     * Those coefficients are given in Danielson paper by expression 4.4-(6)
     * </p>
     * @author Petre Bazavan
     * @author Lucian Barbulescu
     * @param <T> type of the field elements
     */
    protected class FieldFourierCjSjCoefficients<T extends CalculusFieldElement<T>> {

        /** Maximum possible value for j. */
        private final int jMax;

        /**
         * The C<sub>i</sub><sup>j</sup> coefficients.
         * <p>
         * the index i corresponds to the following elements: <br/>
         * - 0 for a <br>
         * - 1 for k <br>
         * - 2 for h <br>
         * - 3 for q <br>
         * - 4 for p <br>
         * - 5 for λ <br>
         * </p>
         */
        private final T[][] cCoef;

        /**
         * The C<sub>i</sub><sup>j</sup> coefficients.
         * <p>
         * the index i corresponds to the following elements: <br/>
         * - 0 for a <br>
         * - 1 for k <br>
         * - 2 for h <br>
         * - 3 for q <br>
         * - 4 for p <br>
         * - 5 for λ <br>
         * </p>
         */
        private final T[][] sCoef;

        /**
         * Standard constructor.
         * @param state             the current state
         * @param jMax              maximum value for j
         * @param auxiliaryElements auxiliary elements related to the current orbit
         * @param parameters        values of the force model parameters
         * @param field             field used by default
         */
        FieldFourierCjSjCoefficients(final FieldSpacecraftState<T> state, final int jMax,
                final FieldAuxiliaryElements<T> auxiliaryElements, final T[] parameters, final Field<T> field) {
            // Initialise the fields
            this.jMax = jMax;

            // Allocate the arrays
            final int rows = jMax + 1;
            cCoef = MathArrays.buildArray(field, rows, 6);
            sCoef = MathArrays.buildArray(field, rows, 6);

            // Compute the coefficients
            computeCoefficients(state, auxiliaryElements, parameters, field);
        }

        /**
         * Compute the Fourrier coefficients.
         * <p>
         * Only the C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> coefficients
         * need to be computed as D<sub>i</sub><sup>m</sup> is always 0.
         * </p>
         * @param state             the current state
         * @param auxiliaryElements auxiliary elements related to the current orbit
         * @param parameters        values of the force model parameters
         * @param field             field used by default
         */
        private void computeCoefficients(final FieldSpacecraftState<T> state,
                final FieldAuxiliaryElements<T> auxiliaryElements, final T[] parameters, final Field<T> field) {
            // Zero
            final T zero = field.getZero();
            // Computes the limits for the integral
            final T[] ll = getLLimits(state, auxiliaryElements);
            // Computes integrated mean element rates if Llow < Lhigh
            if (ll[0].getReal() < ll[1].getReal()) {
                // Compute 1 / PI
                final T ooPI = zero.getPi().reciprocal();

                // loop through all values of j
                for (int j = 0; j <= jMax; j++) {
                    final T[] curentCoefficients = integrator.integrate(
                            new FieldIntegrableFunction<>(state, false, j, parameters, field), ll[0], ll[1], field);

                    // divide by PI and set the values for the coefficients
                    for (int i = 0; i < 6; i++) {
                        cCoef[j][i] = curentCoefficients[i].multiply(ooPI);
                        sCoef[j][i] = curentCoefficients[i + 6].multiply(ooPI);
                    }
                }
            }
        }

        /**
         * Get the coefficient C<sub>i</sub><sup>j</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @return the coefficient C<sub>i</sub><sup>j</sup>
         */
        public T getCij(final int i, final int j) {
            return cCoef[j][i];
        }

        /**
         * Get the coefficient S<sub>i</sub><sup>j</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @return the coefficient S<sub>i</sub><sup>j</sup>
         */
        public T getSij(final int i, final int j) {
            return sCoef[j][i];
        }
    }

    /**
     * This class handles the short periodic coefficients described in Danielson
     * 2.5.3-26.
     *
     * <p>
     * The value of M is 0. Also, since the values of the Fourier coefficient
     * D<sub>i</sub><sup>m</sup> is 0 then the values of the coefficients
     * D<sub>i</sub><sup>m</sup> for m &gt; 2 are also 0.
     * </p>
     * @author Petre Bazavan
     * @author Lucian Barbulescu
     *
     */
    protected static class GaussianShortPeriodicCoefficients implements ShortPeriodTerms {

        /** Maximum value for j index. */
        private final int jMax;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** Prefix for coefficients keys. */
        private final String coefficientsKeyPrefix;

        /** All coefficients slots. */
        private final transient TimeSpanMap<Slot> slots;

        /**
         * Constructor.
         * @param coefficientsKeyPrefix prefix for coefficients keys
         * @param jMax                  maximum value for j index
         * @param interpolationPoints   number of points used in the interpolation
         *                              process
         * @param slots                 all coefficients slots
         */
        GaussianShortPeriodicCoefficients(final String coefficientsKeyPrefix, final int jMax,
                final int interpolationPoints, final TimeSpanMap<Slot> slots) {
            // Initialize fields
            this.jMax = jMax;
            this.interpolationPoints = interpolationPoints;
            this.coefficientsKeyPrefix = coefficientsKeyPrefix;
            this.slots = slots;
        }

        /**
         * Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        public Slot createSlot(final SpacecraftState... meanStates) {
            final Slot slot = new Slot(jMax, interpolationPoints);
            final AbsoluteDate first = meanStates[0].getDate();
            final AbsoluteDate last = meanStates[meanStates.length - 1].getDate();
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

        /**
         * Compute the short periodic coefficients.
         *
         * @param state       current state information: date, kinematics, attitude
         * @param slot        coefficients slot
         * @param fourierCjSj Fourier coefficients
         * @param uijvij      U and V coefficients
         * @param n           Keplerian mean motion
         * @param a           semi major axis
         */
        private void computeCoefficients(final SpacecraftState state, final Slot slot,
                final FourierCjSjCoefficients fourierCjSj, final UijVijCoefficients uijvij, final double n,
                final double a) {

            // get the current date
            final AbsoluteDate date = state.getDate();

            // compute the k₂⁰ coefficient
            final double k20 = computeK20(jMax, uijvij.currentRhoSigmaj);

            // 1. / n
            final double oon = 1. / n;
            // 3. / (2 * a * n)
            final double to2an = 1.5 * oon / a;
            // 3. / (4 * a * n)
            final double to4an = to2an / 2;

            // Compute the coefficients for each element
            final int size = jMax + 1;
            final double[] di1 = new double[6];
            final double[] di2 = new double[6];
            final double[][] currentCij = new double[size][6];
            final double[][] currentSij = new double[size][6];
            for (int i = 0; i < 6; i++) {

                // compute D<sub>i</sub>¹ and D<sub>i</sub>² (all others are 0)
                di1[i] = -oon * fourierCjSj.getCij(i, 0);
                if (i == 5) {
                    di1[i] += to2an * uijvij.getU1(0, 0);
                }
                di2[i] = 0.;
                if (i == 5) {
                    di2[i] += -to4an * fourierCjSj.getCij(0, 0);
                }

                // the C<sub>i</sub>⁰ is computed based on all others
                currentCij[0][i] = -di2[i] * k20;

                for (int j = 1; j <= jMax; j++) {
                    // compute the current C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup>
                    currentCij[j][i] = oon * uijvij.getU1(j, i);
                    if (i == 5) {
                        currentCij[j][i] += -to2an * uijvij.getU2(j);
                    }
                    currentSij[j][i] = oon * uijvij.getV1(j, i);
                    if (i == 5) {
                        currentSij[j][i] += -to2an * uijvij.getV2(j);
                    }

                    // add the computed coefficients to C<sub>i</sub>⁰
                    currentCij[0][i] += -(currentCij[j][i] * uijvij.currentRhoSigmaj[0][j] +
                            currentSij[j][i] * uijvij.currentRhoSigmaj[1][j]);
                }

            }

            // add the values to the interpolators
            slot.cij[0].addGridPoint(date, currentCij[0]);
            slot.dij[1].addGridPoint(date, di1);
            slot.dij[2].addGridPoint(date, di2);
            for (int j = 1; j <= jMax; j++) {
                slot.cij[j].addGridPoint(date, currentCij[j]);
                slot.sij[j].addGridPoint(date, currentSij[j]);
            }

        }

        /**
         * Compute the coefficient k₂⁰ by using the equation 2.5.3-(9a) from Danielson.
         * <p>
         * After inserting 2.5.3-(8) into 2.5.3-(9a) the result becomes:<br>
         * k₂⁰ = &Sigma;<sub>k=1</sub><sup>kMax</sup>[(2 / k²) * (σ<sub>k</sub>² +
         * ρ<sub>k</sub>²)]
         * </p>
         * @param kMax             max value fot k index
         * @param currentRhoSigmaj the current computed values for the ρ<sub>j</sub> and
         *                         σ<sub>j</sub> coefficients
         * @return the coefficient k₂⁰
         */
        private double computeK20(final int kMax, final double[][] currentRhoSigmaj) {
            double k20 = 0.;

            for (int kIndex = 1; kIndex <= kMax; kIndex++) {
                // After inserting 2.5.3-(8) into 2.5.3-(9a) the result becomes:
                // k₂⁰ = &Sigma;<sub>k=1</sub><sup>kMax</sup>[(2 / k²) * (σ<sub>k</sub>² +
                // ρ<sub>k</sub>²)]
                double currentTerm = currentRhoSigmaj[1][kIndex] * currentRhoSigmaj[1][kIndex] +
                        currentRhoSigmaj[0][kIndex] * currentRhoSigmaj[0][kIndex];

                // multiply by 2 / k²
                currentTerm *= 2. / (kIndex * kIndex);

                // add the term to the result
                k20 += currentTerm;
            }

            return k20;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final Orbit meanOrbit) {

            // select the coefficients slot
            final Slot slot = slots.get(meanOrbit.getDate());

            // Get the True longitude L
            final double L = meanOrbit.getLv();

            // Compute the center (l - λ)
            final double center = L - meanOrbit.getLM();
            // Compute (l - λ)²
            final double center2 = center * center;

            // Initialize short periodic variations
            final double[] shortPeriodicVariation = slot.cij[0].value(meanOrbit.getDate());
            final double[] d1 = slot.dij[1].value(meanOrbit.getDate());
            final double[] d2 = slot.dij[2].value(meanOrbit.getDate());
            for (int i = 0; i < 6; i++) {
                shortPeriodicVariation[i] += center * d1[i] + center2 * d2[i];
            }

            for (int j = 1; j <= JMAX; j++) {
                final double[] c = slot.cij[j].value(meanOrbit.getDate());
                final double[] s = slot.sij[j].value(meanOrbit.getDate());
                final SinCos sc  = FastMath.sinCos(j * L);
                final double cos = sc.cos();
                final double sin = sc.sin();
                for (int i = 0; i < 6; i++) {
                    // add corresponding term to the short periodic variation
                    shortPeriodicVariation[i] += c[i] * cos;
                    shortPeriodicVariation[i] += s[i] * sin;
                }
            }

            return shortPeriodicVariation;

        }

        /** {@inheritDoc} */
        public String getCoefficientsKeyPrefix() {
            return coefficientsKeyPrefix;
        }

        /**
         * {@inheritDoc}
         * <p>
         * For Gaussian forces, there are JMAX cj coefficients, JMAX sj coefficients and
         * 3 dj coefficients. As JMAX = 12, this sums up to 27 coefficients. The j index
         * is the integer multiplier for the true longitude argument in the cj and sj
         * coefficients and to the degree in the polynomial dj coefficients.
         * </p>
         */
        @Override
        public Map<String, double[]> getCoefficients(final AbsoluteDate date, final Set<String> selected) {

            // select the coefficients slot
            final Slot slot = slots.get(date);

            final Map<String, double[]> coefficients = new HashMap<String, double[]>(2 * JMAX + 3);
            storeIfSelected(coefficients, selected, slot.cij[0].value(date), "d", 0);
            storeIfSelected(coefficients, selected, slot.dij[1].value(date), "d", 1);
            storeIfSelected(coefficients, selected, slot.dij[2].value(date), "d", 2);
            for (int j = 1; j <= JMAX; j++) {
                storeIfSelected(coefficients, selected, slot.cij[j].value(date), "c", j);
                storeIfSelected(coefficients, selected, slot.sij[j].value(date), "s", j);
            }

            return coefficients;

        }

        /**
         * Put a coefficient in a map if selected.
         * @param map      map to populate
         * @param selected set of coefficients that should be put in the map (empty set
         *                 means all coefficients are selected)
         * @param value    coefficient value
         * @param id       coefficient identifier
         * @param indices  list of coefficient indices
         */
        private void storeIfSelected(final Map<String, double[]> map, final Set<String> selected, final double[] value,
                final String id, final int... indices) {
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
     * This class handles the short periodic coefficients described in Danielson
     * 2.5.3-26.
     *
     * <p>
     * The value of M is 0. Also, since the values of the Fourier coefficient
     * D<sub>i</sub><sup>m</sup> is 0 then the values of the coefficients
     * D<sub>i</sub><sup>m</sup> for m &gt; 2 are also 0.
     * </p>
     * @author Petre Bazavan
     * @author Lucian Barbulescu
     * @param <T> type of the field elements
     */
    protected static class FieldGaussianShortPeriodicCoefficients<T extends CalculusFieldElement<T>>
            implements FieldShortPeriodTerms<T> {

        /** Maximum value for j index. */
        private final int jMax;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** Prefix for coefficients keys. */
        private final String coefficientsKeyPrefix;

        /** All coefficients slots. */
        private final transient FieldTimeSpanMap<FieldSlot<T>, T> slots;

        /**
         * Constructor.
         * @param coefficientsKeyPrefix prefix for coefficients keys
         * @param jMax                  maximum value for j index
         * @param interpolationPoints   number of points used in the interpolation
         *                              process
         * @param slots                 all coefficients slots
         */
        FieldGaussianShortPeriodicCoefficients(final String coefficientsKeyPrefix, final int jMax,
                final int interpolationPoints, final FieldTimeSpanMap<FieldSlot<T>, T> slots) {
            // Initialize fields
            this.jMax = jMax;
            this.interpolationPoints = interpolationPoints;
            this.coefficientsKeyPrefix = coefficientsKeyPrefix;
            this.slots = slots;
        }

        /**
         * Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        @SuppressWarnings("unchecked")
        public FieldSlot<T> createSlot(final FieldSpacecraftState<T>... meanStates) {
            final FieldSlot<T> slot = new FieldSlot<>(jMax, interpolationPoints);
            final FieldAbsoluteDate<T> first = meanStates[0].getDate();
            final FieldAbsoluteDate<T> last = meanStates[meanStates.length - 1].getDate();
            if (first.compareTo(last) <= 0) {
                slots.addValidAfter(slot, first);
            } else {
                slots.addValidBefore(slot, first);
            }
            return slot;
        }

        /**
         * Compute the short periodic coefficients.
         *
         * @param state       current state information: date, kinematics, attitude
         * @param slot        coefficients slot
         * @param fourierCjSj Fourier coefficients
         * @param uijvij      U and V coefficients
         * @param n           Keplerian mean motion
         * @param a           semi major axis
         * @param field       field used by default
         */
        private void computeCoefficients(final FieldSpacecraftState<T> state, final FieldSlot<T> slot,
                final FieldFourierCjSjCoefficients<T> fourierCjSj, final FieldUijVijCoefficients<T> uijvij, final T n,
                final T a, final Field<T> field) {

            // Zero
            final T zero = field.getZero();

            // get the current date
            final FieldAbsoluteDate<T> date = state.getDate();

            // compute the k₂⁰ coefficient
            final T k20 = computeK20(jMax, uijvij.currentRhoSigmaj, field);

            // 1. / n
            final T oon = n.reciprocal();
            // 3. / (2 * a * n)
            final T to2an = oon.multiply(1.5).divide(a);
            // 3. / (4 * a * n)
            final T to4an = to2an.divide(2.);

            // Compute the coefficients for each element
            final int size = jMax + 1;
            final T[] di1 = MathArrays.buildArray(field, 6);
            final T[] di2 = MathArrays.buildArray(field, 6);
            final T[][] currentCij = MathArrays.buildArray(field, size, 6);
            final T[][] currentSij = MathArrays.buildArray(field, size, 6);
            for (int i = 0; i < 6; i++) {

                // compute D<sub>i</sub>¹ and D<sub>i</sub>² (all others are 0)
                di1[i] = oon.negate().multiply(fourierCjSj.getCij(i, 0));
                if (i == 5) {
                    di1[i] = di1[i].add(to2an.multiply(uijvij.getU1(0, 0)));
                }
                di2[i] = zero;
                if (i == 5) {
                    di2[i] = di2[i].add(to4an.negate().multiply(fourierCjSj.getCij(0, 0)));
                }

                // the C<sub>i</sub>⁰ is computed based on all others
                currentCij[0][i] = di2[i].negate().multiply(k20);

                for (int j = 1; j <= jMax; j++) {
                    // compute the current C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup>
                    currentCij[j][i] = oon.multiply(uijvij.getU1(j, i));
                    if (i == 5) {
                        currentCij[j][i] = currentCij[j][i].add(to2an.negate().multiply(uijvij.getU2(j)));
                    }
                    currentSij[j][i] = oon.multiply(uijvij.getV1(j, i));
                    if (i == 5) {
                        currentSij[j][i] = currentSij[j][i].add(to2an.negate().multiply(uijvij.getV2(j)));
                    }

                    // add the computed coefficients to C<sub>i</sub>⁰
                    currentCij[0][i] = currentCij[0][i].add(currentCij[j][i].multiply(uijvij.currentRhoSigmaj[0][j])
                            .add(currentSij[j][i].multiply(uijvij.currentRhoSigmaj[1][j])).negate());
                }

            }

            // add the values to the interpolators
            slot.cij[0].addGridPoint(date, currentCij[0]);
            slot.dij[1].addGridPoint(date, di1);
            slot.dij[2].addGridPoint(date, di2);
            for (int j = 1; j <= jMax; j++) {
                slot.cij[j].addGridPoint(date, currentCij[j]);
                slot.sij[j].addGridPoint(date, currentSij[j]);
            }

        }

        /**
         * Compute the coefficient k₂⁰ by using the equation 2.5.3-(9a) from Danielson.
         * <p>
         * After inserting 2.5.3-(8) into 2.5.3-(9a) the result becomes:<br>
         * k₂⁰ = &Sigma;<sub>k=1</sub><sup>kMax</sup>[(2 / k²) * (σ<sub>k</sub>² +
         * ρ<sub>k</sub>²)]
         * </p>
         * @param kMax             max value fot k index
         * @param currentRhoSigmaj the current computed values for the ρ<sub>j</sub> and
         *                         σ<sub>j</sub> coefficients
         * @param field            field used by default
         * @return the coefficient k₂⁰
         */
        private T computeK20(final int kMax, final T[][] currentRhoSigmaj, final Field<T> field) {
            final T zero = field.getZero();
            T k20 = zero;

            for (int kIndex = 1; kIndex <= kMax; kIndex++) {
                // After inserting 2.5.3-(8) into 2.5.3-(9a) the result becomes:
                // k₂⁰ = &Sigma;<sub>k=1</sub><sup>kMax</sup>[(2 / k²) * (σ<sub>k</sub>² +
                // ρ<sub>k</sub>²)]
                T currentTerm = currentRhoSigmaj[1][kIndex].multiply(currentRhoSigmaj[1][kIndex])
                        .add(currentRhoSigmaj[0][kIndex].multiply(currentRhoSigmaj[0][kIndex]));

                // multiply by 2 / k²
                currentTerm = currentTerm.multiply(2. / (kIndex * kIndex));

                // add the term to the result
                k20 = k20.add(currentTerm);
            }

            return k20;
        }

        /** {@inheritDoc} */
        @Override
        public T[] value(final FieldOrbit<T> meanOrbit) {

            // select the coefficients slot
            final FieldSlot<T> slot = slots.get(meanOrbit.getDate());

            // Get the True longitude L
            final T L = meanOrbit.getLv();

            // Compute the center (l - λ)
            final T center = L.subtract(meanOrbit.getLM());
            // Compute (l - λ)²
            final T center2 = center.multiply(center);

            // Initialize short periodic variations
            final T[] shortPeriodicVariation = slot.cij[0].value(meanOrbit.getDate());
            final T[] d1 = slot.dij[1].value(meanOrbit.getDate());
            final T[] d2 = slot.dij[2].value(meanOrbit.getDate());
            for (int i = 0; i < 6; i++) {
                shortPeriodicVariation[i] = shortPeriodicVariation[i]
                        .add(center.multiply(d1[i]).add(center2.multiply(d2[i])));
            }

            for (int j = 1; j <= JMAX; j++) {
                final T[] c = slot.cij[j].value(meanOrbit.getDate());
                final T[] s = slot.sij[j].value(meanOrbit.getDate());
                final FieldSinCos<T> sc = FastMath.sinCos(L.multiply(j));
                final T cos = sc.cos();
                final T sin = sc.sin();
                for (int i = 0; i < 6; i++) {
                    // add corresponding term to the short periodic variation
                    shortPeriodicVariation[i] = shortPeriodicVariation[i].add(c[i].multiply(cos));
                    shortPeriodicVariation[i] = shortPeriodicVariation[i].add(s[i].multiply(sin));
                }
            }

            return shortPeriodicVariation;

        }

        /** {@inheritDoc} */
        public String getCoefficientsKeyPrefix() {
            return coefficientsKeyPrefix;
        }

        /**
         * {@inheritDoc}
         * <p>
         * For Gaussian forces, there are JMAX cj coefficients, JMAX sj coefficients and
         * 3 dj coefficients. As JMAX = 12, this sums up to 27 coefficients. The j index
         * is the integer multiplier for the true longitude argument in the cj and sj
         * coefficients and to the degree in the polynomial dj coefficients.
         * </p>
         */
        @Override
        public Map<String, T[]> getCoefficients(final FieldAbsoluteDate<T> date, final Set<String> selected) {

            // select the coefficients slot
            final FieldSlot<T> slot = slots.get(date);

            final Map<String, T[]> coefficients = new HashMap<String, T[]>(2 * JMAX + 3);
            storeIfSelected(coefficients, selected, slot.cij[0].value(date), "d", 0);
            storeIfSelected(coefficients, selected, slot.dij[1].value(date), "d", 1);
            storeIfSelected(coefficients, selected, slot.dij[2].value(date), "d", 2);
            for (int j = 1; j <= JMAX; j++) {
                storeIfSelected(coefficients, selected, slot.cij[j].value(date), "c", j);
                storeIfSelected(coefficients, selected, slot.sij[j].value(date), "s", j);
            }

            return coefficients;

        }

        /**
         * Put a coefficient in a map if selected.
         * @param map      map to populate
         * @param selected set of coefficients that should be put in the map (empty set
         *                 means all coefficients are selected)
         * @param value    coefficient value
         * @param id       coefficient identifier
         * @param indices  list of coefficient indices
         */
        private void storeIfSelected(final Map<String, T[]> map, final Set<String> selected, final T[] value,
                final String id, final int... indices) {
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
     * The U<sub>i</sub><sup>j</sup> and V<sub>i</sub><sup>j</sup> coefficients
     * described by equations 2.5.3-(21) and 2.5.3-(22) from Danielson.
     * <p>
     * The index i takes only the values 1 and 2<br>
     * For U only the index 0 for j is used.
     * </p>
     *
     * @author Petre Bazavan
     * @author Lucian Barbulescu
     */
    protected static class UijVijCoefficients {

        /**
         * The U₁<sup>j</sup> coefficients.
         * <p>
         * The first index identifies the Fourier coefficients used<br>
         * Those coefficients are computed for all Fourier C<sub>i</sub><sup>j</sup> and
         * S<sub>i</sub><sup>j</sup><br>
         * The only exception is when j = 0 when only the coefficient for fourier index
         * = 1 (i == 0) is needed.<br>
         * Also, for fourier index = 1 (i == 0), the coefficients up to 2 * jMax are
         * computed, because are required to compute the coefficients U₂<sup>j</sup>
         * </p>
         */
        private final double[][] u1ij;

        /**
         * The V₁<sup>j</sup> coefficients.
         * <p>
         * The first index identifies the Fourier coefficients used<br>
         * Those coefficients are computed for all Fourier C<sub>i</sub><sup>j</sup> and
         * S<sub>i</sub><sup>j</sup><br>
         * for fourier index = 1 (i == 0), the coefficients up to 2 * jMax are computed,
         * because are required to compute the coefficients V₂<sup>j</sup>
         * </p>
         */
        private final double[][] v1ij;

        /**
         * The U₂<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients that use the Fourier index = 1 (i == 0) are computed as
         * they are the only ones required.
         * </p>
         */
        private final double[] u2ij;

        /**
         * The V₂<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients that use the Fourier index = 1 (i == 0) are computed as
         * they are the only ones required.
         * </p>
         */
        private final double[] v2ij;

        /**
         * The current computed values for the ρ<sub>j</sub> and σ<sub>j</sub>
         * coefficients.
         */
        private final double[][] currentRhoSigmaj;

        /**
         * The C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup> Fourier
         * coefficients.
         */
        private final FourierCjSjCoefficients fourierCjSj;

        /** The maximum value for j index. */
        private final int jMax;

        /**
         * Constructor.
         * @param currentRhoSigmaj the current computed values for the ρ<sub>j</sub> and
         *                         σ<sub>j</sub> coefficients
         * @param fourierCjSj      the fourier coefficients C<sub>i</sub><sup>j</sup>
         *                         and the S<sub>i</sub><sup>j</sup>
         * @param jMax             maximum value for j index
         */
        UijVijCoefficients(final double[][] currentRhoSigmaj, final FourierCjSjCoefficients fourierCjSj,
                final int jMax) {
            this.currentRhoSigmaj = currentRhoSigmaj;
            this.fourierCjSj = fourierCjSj;
            this.jMax = jMax;

            // initialize the internal arrays.
            this.u1ij = new double[6][2 * jMax + 1];
            this.v1ij = new double[6][2 * jMax + 1];
            this.u2ij = new double[jMax + 1];
            this.v2ij = new double[jMax + 1];

            // compute the coefficients
            computeU1V1Coefficients();
            computeU2V2Coefficients();
        }

        /** Build the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients. */
        private void computeU1V1Coefficients() {
            // generate the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients
            // for j >= 1
            // also the U₁⁰ for Fourier index = 1 (i == 0) coefficient will be computed
            u1ij[0][0] = 0;
            for (int j = 1; j <= jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;

                for (int i = 0; i < 6; i++) {
                    // j is aready between 1 and J
                    u1ij[i][j] = fourierCjSj.getSij(i, j);
                    v1ij[i][j] = fourierCjSj.getCij(i, j);

                    // 1 - δ<sub>1j</sub> is 1 for all j > 1
                    if (j > 1) {
                        // k starts with 1 because j-J is less than or equal to 0
                        for (int kIndex = 1; kIndex <= j - 1; kIndex++) {
                            // C<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub> +
                            // S<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub>
                            u1ij[i][j] += fourierCjSj.getCij(i, j - kIndex) * currentRhoSigmaj[1][kIndex] +
                                    fourierCjSj.getSij(i, j - kIndex) * currentRhoSigmaj[0][kIndex];

                            // C<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub> -
                            // S<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub>
                            v1ij[i][j] += fourierCjSj.getCij(i, j - kIndex) * currentRhoSigmaj[0][kIndex] -
                                    fourierCjSj.getSij(i, j - kIndex) * currentRhoSigmaj[1][kIndex];
                        }
                    }

                    // since j must be between 1 and J-1 and is already between 1 and J
                    // the following sum is skiped only for j = jMax
                    if (j != jMax) {
                        for (int kIndex = 1; kIndex <= jMax - j; kIndex++) {
                            // -C<sub>i</sub><sup>j+k</sup> * σ<sub>k</sub> +
                            // S<sub>i</sub><sup>j+k</sup> * ρ<sub>k</sub>
                            u1ij[i][j] += -fourierCjSj.getCij(i, j + kIndex) * currentRhoSigmaj[1][kIndex] +
                                    fourierCjSj.getSij(i, j + kIndex) * currentRhoSigmaj[0][kIndex];

                            // C<sub>i</sub><sup>j+k</sup> * ρ<sub>k</sub> +
                            // S<sub>i</sub><sup>j+k</sup> * σ<sub>k</sub>
                            v1ij[i][j] += fourierCjSj.getCij(i, j + kIndex) * currentRhoSigmaj[0][kIndex] +
                                    fourierCjSj.getSij(i, j + kIndex) * currentRhoSigmaj[1][kIndex];
                        }
                    }

                    for (int kIndex = 1; kIndex <= jMax; kIndex++) {
                        // C<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub> -
                        // S<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub>
                        u1ij[i][j] += -fourierCjSj.getCij(i, kIndex) * currentRhoSigmaj[1][j + kIndex] -
                                fourierCjSj.getSij(i, kIndex) * currentRhoSigmaj[0][j + kIndex];

                        // C<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub> +
                        // S<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub>
                        v1ij[i][j] += fourierCjSj.getCij(i, kIndex) * currentRhoSigmaj[0][j + kIndex] +
                                fourierCjSj.getSij(i, kIndex) * currentRhoSigmaj[1][j + kIndex];
                    }

                    // divide by 1 / j
                    u1ij[i][j] *= -ooj;
                    v1ij[i][j] *= ooj;

                    // if index = 1 (i == 0) add the computed terms to U₁⁰
                    if (i == 0) {
                        // - (U₁<sup>j</sup> * ρ<sub>j</sub> + V₁<sup>j</sup> * σ<sub>j</sub>
                        u1ij[0][0] += -u1ij[0][j] * currentRhoSigmaj[0][j] - v1ij[0][j] * currentRhoSigmaj[1][j];
                    }
                }
            }

            // Terms with j > jMax are required only when computing the coefficients
            // U₂<sup>j</sup> and V₂<sup>j</sup>
            // and those coefficients are only required for Fourier index = 1 (i == 0).
            for (int j = jMax + 1; j <= 2 * jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;
                // the value of i is 0
                u1ij[0][j] = 0.;
                v1ij[0][j] = 0.;

                // k starts from j-J as it is always greater than or equal to 1
                for (int kIndex = j - jMax; kIndex <= j - 1; kIndex++) {
                    // C<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub> +
                    // S<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub>
                    u1ij[0][j] += fourierCjSj.getCij(0, j - kIndex) * currentRhoSigmaj[1][kIndex] +
                            fourierCjSj.getSij(0, j - kIndex) * currentRhoSigmaj[0][kIndex];

                    // C<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub> -
                    // S<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub>
                    v1ij[0][j] += fourierCjSj.getCij(0, j - kIndex) * currentRhoSigmaj[0][kIndex] -
                            fourierCjSj.getSij(0, j - kIndex) * currentRhoSigmaj[1][kIndex];
                }
                for (int kIndex = 1; kIndex <= jMax; kIndex++) {
                    // C<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub> -
                    // S<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub>
                    u1ij[0][j] += -fourierCjSj.getCij(0, kIndex) * currentRhoSigmaj[1][j + kIndex] -
                            fourierCjSj.getSij(0, kIndex) * currentRhoSigmaj[0][j + kIndex];

                    // C<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub> +
                    // S<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub>
                    v1ij[0][j] += fourierCjSj.getCij(0, kIndex) * currentRhoSigmaj[0][j + kIndex] +
                            fourierCjSj.getSij(0, kIndex) * currentRhoSigmaj[1][j + kIndex];
                }

                // divide by 1 / j
                u1ij[0][j] *= -ooj;
                v1ij[0][j] *= ooj;
            }
        }

        /**
         * Build the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients for Fourier index = 1 (i == 0) are required.
         * </p>
         */
        private void computeU2V2Coefficients() {
            for (int j = 1; j <= jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;

                // only the values for i == 0 are computed
                u2ij[j] = v1ij[0][j];
                v2ij[j] = u1ij[0][j];

                // 1 - δ<sub>1j</sub> is 1 for all j > 1
                if (j > 1) {
                    for (int l = 1; l <= j - 1; l++) {
                        // U₁<sup>j-l</sup> * σ<sub>l</sub> +
                        // V₁<sup>j-l</sup> * ρ<sub>l</sub>
                        u2ij[j] += u1ij[0][j - l] * currentRhoSigmaj[1][l] + v1ij[0][j - l] * currentRhoSigmaj[0][l];

                        // U₁<sup>j-l</sup> * ρ<sub>l</sub> -
                        // V₁<sup>j-l</sup> * σ<sub>l</sub>
                        v2ij[j] += u1ij[0][j - l] * currentRhoSigmaj[0][l] - v1ij[0][j - l] * currentRhoSigmaj[1][l];
                    }
                }

                for (int l = 1; l <= jMax; l++) {
                    // -U₁<sup>j+l</sup> * σ<sub>l</sub> +
                    // U₁<sup>l</sup> * σ<sub>j+l</sub> +
                    // V₁<sup>j+l</sup> * ρ<sub>l</sub> -
                    // V₁<sup>l</sup> * ρ<sub>j+l</sub>
                    u2ij[j] += -u1ij[0][j + l] * currentRhoSigmaj[1][l] + u1ij[0][l] * currentRhoSigmaj[1][j + l] +
                            v1ij[0][j + l] * currentRhoSigmaj[0][l] - v1ij[0][l] * currentRhoSigmaj[0][j + l];

                    // U₁<sup>j+l</sup> * ρ<sub>l</sub> +
                    // U₁<sup>l</sup> * ρ<sub>j+l</sub> +
                    // V₁<sup>j+l</sup> * σ<sub>l</sub> +
                    // V₁<sup>l</sup> * σ<sub>j+l</sub>
                    u2ij[j] += u1ij[0][j + l] * currentRhoSigmaj[0][l] + u1ij[0][l] * currentRhoSigmaj[0][j + l] +
                            v1ij[0][j + l] * currentRhoSigmaj[1][l] + v1ij[0][l] * currentRhoSigmaj[1][j + l];
                }

                // divide by 1 / j
                u2ij[j] *= -ooj;
                v2ij[j] *= ooj;
            }
        }

        /**
         * Get the coefficient U₁<sup>j</sup> for Fourier index i.
         *
         * @param j j index
         * @param i Fourier index (starts at 0)
         * @return the coefficient U₁<sup>j</sup> for the given Fourier index i
         */
        public double getU1(final int j, final int i) {
            return u1ij[i][j];
        }

        /**
         * Get the coefficient V₁<sup>j</sup> for Fourier index i.
         *
         * @param j j index
         * @param i Fourier index (starts at 0)
         * @return the coefficient V₁<sup>j</sup> for the given Fourier index i
         */
        public double getV1(final int j, final int i) {
            return v1ij[i][j];
        }

        /**
         * Get the coefficient U₂<sup>j</sup> for Fourier index = 1 (i == 0).
         *
         * @param j j index
         * @return the coefficient U₂<sup>j</sup> for Fourier index = 1 (i == 0)
         */
        public double getU2(final int j) {
            return u2ij[j];
        }

        /**
         * Get the coefficient V₂<sup>j</sup> for Fourier index = 1 (i == 0).
         *
         * @param j j index
         * @return the coefficient V₂<sup>j</sup> for Fourier index = 1 (i == 0)
         */
        public double getV2(final int j) {
            return v2ij[j];
        }
    }

    /**
     * The U<sub>i</sub><sup>j</sup> and V<sub>i</sub><sup>j</sup> coefficients
     * described by equations 2.5.3-(21) and 2.5.3-(22) from Danielson.
     * <p>
     * The index i takes only the values 1 and 2<br>
     * For U only the index 0 for j is used.
     * </p>
     *
     * @author Petre Bazavan
     * @author Lucian Barbulescu
     * @param <T> type of the field elements
     */
    protected static class FieldUijVijCoefficients<T extends CalculusFieldElement<T>> {

        /**
         * The U₁<sup>j</sup> coefficients.
         * <p>
         * The first index identifies the Fourier coefficients used<br>
         * Those coefficients are computed for all Fourier C<sub>i</sub><sup>j</sup> and
         * S<sub>i</sub><sup>j</sup><br>
         * The only exception is when j = 0 when only the coefficient for fourier index
         * = 1 (i == 0) is needed.<br>
         * Also, for fourier index = 1 (i == 0), the coefficients up to 2 * jMax are
         * computed, because are required to compute the coefficients U₂<sup>j</sup>
         * </p>
         */
        private final T[][] u1ij;

        /**
         * The V₁<sup>j</sup> coefficients.
         * <p>
         * The first index identifies the Fourier coefficients used<br>
         * Those coefficients are computed for all Fourier C<sub>i</sub><sup>j</sup> and
         * S<sub>i</sub><sup>j</sup><br>
         * for fourier index = 1 (i == 0), the coefficients up to 2 * jMax are computed,
         * because are required to compute the coefficients V₂<sup>j</sup>
         * </p>
         */
        private final T[][] v1ij;

        /**
         * The U₂<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients that use the Fourier index = 1 (i == 0) are computed as
         * they are the only ones required.
         * </p>
         */
        private final T[] u2ij;

        /**
         * The V₂<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients that use the Fourier index = 1 (i == 0) are computed as
         * they are the only ones required.
         * </p>
         */
        private final T[] v2ij;

        /**
         * The current computed values for the ρ<sub>j</sub> and σ<sub>j</sub>
         * coefficients.
         */
        private final T[][] currentRhoSigmaj;

        /**
         * The C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup> Fourier
         * coefficients.
         */
        private final FieldFourierCjSjCoefficients<T> fourierCjSj;

        /** The maximum value for j index. */
        private final int jMax;

        /**
         * Constructor.
         * @param currentRhoSigmaj the current computed values for the ρ<sub>j</sub> and
         *                         σ<sub>j</sub> coefficients
         * @param fourierCjSj      the fourier coefficients C<sub>i</sub><sup>j</sup>
         *                         and the S<sub>i</sub><sup>j</sup>
         * @param jMax             maximum value for j index
         * @param field            field used by default
         */
        FieldUijVijCoefficients(final T[][] currentRhoSigmaj, final FieldFourierCjSjCoefficients<T> fourierCjSj,
                final int jMax, final Field<T> field) {
            this.currentRhoSigmaj = currentRhoSigmaj;
            this.fourierCjSj = fourierCjSj;
            this.jMax = jMax;

            // initialize the internal arrays.
            this.u1ij = MathArrays.buildArray(field, 6, 2 * jMax + 1);
            this.v1ij = MathArrays.buildArray(field, 6, 2 * jMax + 1);
            this.u2ij = MathArrays.buildArray(field, jMax + 1);
            this.v2ij = MathArrays.buildArray(field, jMax + 1);

            // compute the coefficients
            computeU1V1Coefficients(field);
            computeU2V2Coefficients(field);
        }

        /**
         * Build the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients.
         * @param field field used by default
         */
        private void computeU1V1Coefficients(final Field<T> field) {
            // Zero
            final T zero = field.getZero();

            // generate the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients
            // for j >= 1
            // also the U₁⁰ for Fourier index = 1 (i == 0) coefficient will be computed
            u1ij[0][0] = zero;
            for (int j = 1; j <= jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;

                for (int i = 0; i < 6; i++) {
                    // j is aready between 1 and J
                    u1ij[i][j] = fourierCjSj.getSij(i, j);
                    v1ij[i][j] = fourierCjSj.getCij(i, j);

                    // 1 - δ<sub>1j</sub> is 1 for all j > 1
                    if (j > 1) {
                        // k starts with 1 because j-J is less than or equal to 0
                        for (int kIndex = 1; kIndex <= j - 1; kIndex++) {
                            // C<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub> +
                            // S<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub>
                            u1ij[i][j] = u1ij[i][j]
                                    .add(fourierCjSj.getCij(i, j - kIndex).multiply(currentRhoSigmaj[1][kIndex]).add(
                                            fourierCjSj.getSij(i, j - kIndex).multiply(currentRhoSigmaj[0][kIndex])));

                            // C<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub> -
                            // S<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub>
                            v1ij[i][j] = v1ij[i][j].add(
                                    fourierCjSj.getCij(i, j - kIndex).multiply(currentRhoSigmaj[0][kIndex]).subtract(
                                            fourierCjSj.getSij(i, j - kIndex).multiply(currentRhoSigmaj[1][kIndex])));
                        }
                    }

                    // since j must be between 1 and J-1 and is already between 1 and J
                    // the following sum is skiped only for j = jMax
                    if (j != jMax) {
                        for (int kIndex = 1; kIndex <= jMax - j; kIndex++) {
                            // -C<sub>i</sub><sup>j+k</sup> * σ<sub>k</sub> +
                            // S<sub>i</sub><sup>j+k</sup> * ρ<sub>k</sub>
                            u1ij[i][j] = u1ij[i][j].add(fourierCjSj.getCij(i, j + kIndex).negate()
                                    .multiply(currentRhoSigmaj[1][kIndex])
                                    .add(fourierCjSj.getSij(i, j + kIndex).multiply(currentRhoSigmaj[0][kIndex])));

                            // C<sub>i</sub><sup>j+k</sup> * ρ<sub>k</sub> +
                            // S<sub>i</sub><sup>j+k</sup> * σ<sub>k</sub>
                            v1ij[i][j] = v1ij[i][j]
                                    .add(fourierCjSj.getCij(i, j + kIndex).multiply(currentRhoSigmaj[0][kIndex]).add(
                                            fourierCjSj.getSij(i, j + kIndex).multiply(currentRhoSigmaj[1][kIndex])));
                        }
                    }

                    for (int kIndex = 1; kIndex <= jMax; kIndex++) {
                        // C<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub> -
                        // S<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub>
                        u1ij[i][j] = u1ij[i][j].add(fourierCjSj.getCij(i, kIndex).negate()
                                .multiply(currentRhoSigmaj[1][j + kIndex])
                                .subtract(fourierCjSj.getSij(i, kIndex).multiply(currentRhoSigmaj[0][j + kIndex])));

                        // C<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub> +
                        // S<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub>
                        v1ij[i][j] = v1ij[i][j]
                                .add(fourierCjSj.getCij(i, kIndex).multiply(currentRhoSigmaj[0][j + kIndex])
                                        .add(fourierCjSj.getSij(i, kIndex).multiply(currentRhoSigmaj[1][j + kIndex])));
                    }

                    // divide by 1 / j
                    u1ij[i][j] = u1ij[i][j].multiply(-ooj);
                    v1ij[i][j] = v1ij[i][j].multiply(ooj);

                    // if index = 1 (i == 0) add the computed terms to U₁⁰
                    if (i == 0) {
                        // - (U₁<sup>j</sup> * ρ<sub>j</sub> + V₁<sup>j</sup> * σ<sub>j</sub>
                        u1ij[0][0] = u1ij[0][0].add(u1ij[0][j].negate().multiply(currentRhoSigmaj[0][j])
                                .subtract(v1ij[0][j].multiply(currentRhoSigmaj[1][j])));
                    }
                }
            }

            // Terms with j > jMax are required only when computing the coefficients
            // U₂<sup>j</sup> and V₂<sup>j</sup>
            // and those coefficients are only required for Fourier index = 1 (i == 0).
            for (int j = jMax + 1; j <= 2 * jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;
                // the value of i is 0
                u1ij[0][j] = zero;
                v1ij[0][j] = zero;

                // k starts from j-J as it is always greater than or equal to 1
                for (int kIndex = j - jMax; kIndex <= j - 1; kIndex++) {
                    // C<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub> +
                    // S<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub>
                    u1ij[0][j] = u1ij[0][j].add(fourierCjSj.getCij(0, j - kIndex).multiply(currentRhoSigmaj[1][kIndex])
                            .add(fourierCjSj.getSij(0, j - kIndex).multiply(currentRhoSigmaj[0][kIndex])));

                    // C<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub> -
                    // S<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub>
                    v1ij[0][j] = v1ij[0][j].add(fourierCjSj.getCij(0, j - kIndex).multiply(currentRhoSigmaj[0][kIndex])
                            .subtract(fourierCjSj.getSij(0, j - kIndex).multiply(currentRhoSigmaj[1][kIndex])));
                }
                for (int kIndex = 1; kIndex <= jMax; kIndex++) {
                    // C<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub> -
                    // S<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub>
                    u1ij[0][j] = u1ij[0][j]
                            .add(fourierCjSj.getCij(0, kIndex).negate().multiply(currentRhoSigmaj[1][j + kIndex])
                                    .subtract(fourierCjSj.getSij(0, kIndex).multiply(currentRhoSigmaj[0][j + kIndex])));

                    // C<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub> +
                    // S<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub>
                    v1ij[0][j] = v1ij[0][j].add(fourierCjSj.getCij(0, kIndex).multiply(currentRhoSigmaj[0][j + kIndex])
                            .add(fourierCjSj.getSij(0, kIndex).multiply(currentRhoSigmaj[1][j + kIndex])));
                }

                // divide by 1 / j
                u1ij[0][j] = u1ij[0][j].multiply(-ooj);
                v1ij[0][j] = v1ij[0][j].multiply(ooj);
            }
        }

        /**
         * Build the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients for Fourier index = 1 (i == 0) are required.
         * </p>
         * @param field field used by default
         */
        private void computeU2V2Coefficients(final Field<T> field) {
            for (int j = 1; j <= jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;

                // only the values for i == 0 are computed
                u2ij[j] = v1ij[0][j];
                v2ij[j] = u1ij[0][j];

                // 1 - δ<sub>1j</sub> is 1 for all j > 1
                if (j > 1) {
                    for (int l = 1; l <= j - 1; l++) {
                        // U₁<sup>j-l</sup> * σ<sub>l</sub> +
                        // V₁<sup>j-l</sup> * ρ<sub>l</sub>
                        u2ij[j] = u2ij[j].add(u1ij[0][j - l].multiply(currentRhoSigmaj[1][l])
                                .add(v1ij[0][j - l].multiply(currentRhoSigmaj[0][l])));

                        // U₁<sup>j-l</sup> * ρ<sub>l</sub> -
                        // V₁<sup>j-l</sup> * σ<sub>l</sub>
                        v2ij[j] = v2ij[j].add(u1ij[0][j - l].multiply(currentRhoSigmaj[0][l])
                                .subtract(v1ij[0][j - l].multiply(currentRhoSigmaj[1][l])));
                    }
                }

                for (int l = 1; l <= jMax; l++) {
                    // -U₁<sup>j+l</sup> * σ<sub>l</sub> +
                    // U₁<sup>l</sup> * σ<sub>j+l</sub> +
                    // V₁<sup>j+l</sup> * ρ<sub>l</sub> -
                    // V₁<sup>l</sup> * ρ<sub>j+l</sub>
                    u2ij[j] = u2ij[j].add(u1ij[0][j + l].negate().multiply(currentRhoSigmaj[1][l])
                            .add(u1ij[0][l].multiply(currentRhoSigmaj[1][j + l]))
                            .add(v1ij[0][j + l].multiply(currentRhoSigmaj[0][l]))
                            .subtract(v1ij[0][l].multiply(currentRhoSigmaj[0][j + l])));

                    // U₁<sup>j+l</sup> * ρ<sub>l</sub> +
                    // U₁<sup>l</sup> * ρ<sub>j+l</sub> +
                    // V₁<sup>j+l</sup> * σ<sub>l</sub> +
                    // V₁<sup>l</sup> * σ<sub>j+l</sub>
                    u2ij[j] = u2ij[j].add(u1ij[0][j + l].multiply(currentRhoSigmaj[0][l])
                            .add(u1ij[0][l].multiply(currentRhoSigmaj[0][j + l]))
                            .add(v1ij[0][j + l].multiply(currentRhoSigmaj[1][l]))
                            .add(v1ij[0][l].multiply(currentRhoSigmaj[1][j + l])));
                }

                // divide by 1 / j
                u2ij[j] = u2ij[j].multiply(-ooj);
                v2ij[j] = v2ij[j].multiply(ooj);
            }
        }

        /**
         * Get the coefficient U₁<sup>j</sup> for Fourier index i.
         *
         * @param j j index
         * @param i Fourier index (starts at 0)
         * @return the coefficient U₁<sup>j</sup> for the given Fourier index i
         */
        public T getU1(final int j, final int i) {
            return u1ij[i][j];
        }

        /**
         * Get the coefficient V₁<sup>j</sup> for Fourier index i.
         *
         * @param j j index
         * @param i Fourier index (starts at 0)
         * @return the coefficient V₁<sup>j</sup> for the given Fourier index i
         */
        public T getV1(final int j, final int i) {
            return v1ij[i][j];
        }

        /**
         * Get the coefficient U₂<sup>j</sup> for Fourier index = 1 (i == 0).
         *
         * @param j j index
         * @return the coefficient U₂<sup>j</sup> for Fourier index = 1 (i == 0)
         */
        public T getU2(final int j) {
            return u2ij[j];
        }

        /**
         * Get the coefficient V₂<sup>j</sup> for Fourier index = 1 (i == 0).
         *
         * @param j j index
         * @return the coefficient V₂<sup>j</sup> for Fourier index = 1 (i == 0)
         */
        public T getV2(final int j) {
            return v2ij[j];
        }
    }

    /** Coefficients valid for one time slot. */
    protected static class Slot {

        /**
         * The coefficients D<sub>i</sub><sup>j</sup>.
         * <p>
         * Only for j = 1 and j = 2 the coefficients are not 0. <br>
         * i corresponds to the equinoctial element, as follows: - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] dij;

        /**
         * The coefficients C<sub>i</sub><sup>j</sup>.
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

        /**
         * The coefficients S<sub>i</sub><sup>j</sup>.
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

        /**
         * Simple constructor.
         * @param jMax                maximum value for j index
         * @param interpolationPoints number of points used in the interpolation process
         */
        Slot(final int jMax, final int interpolationPoints) {

            dij = new ShortPeriodicsInterpolatedCoefficient[3];
            cij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1];
            sij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1];

            // Initialize the C<sub>i</sub><sup>j</sup>, S<sub>i</sub><sup>j</sup> and
            // D<sub>i</sub><sup>j</sup> coefficients
            for (int j = 0; j <= jMax; j++) {
                cij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                if (j > 0) {
                    sij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                }
                // Initialize only the non-zero D<sub>i</sub><sup>j</sup> coefficients
                if (j == 1 || j == 2) {
                    dij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                }
            }

        }

    }

    /** Coefficients valid for one time slot.
     * @param <T> type of the field elements
     */
    protected static class FieldSlot<T extends CalculusFieldElement<T>> {

        /**
         * The coefficients D<sub>i</sub><sup>j</sup>.
         * <p>
         * Only for j = 1 and j = 2 the coefficients are not 0. <br>
         * i corresponds to the equinoctial element, as follows: - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final FieldShortPeriodicsInterpolatedCoefficient<T>[] dij;

        /**
         * The coefficients C<sub>i</sub><sup>j</sup>.
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

        /**
         * The coefficients S<sub>i</sub><sup>j</sup>.
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

        /**
         * Simple constructor.
         * @param jMax                maximum value for j index
         * @param interpolationPoints number of points used in the interpolation process
         */
        @SuppressWarnings("unchecked")
        FieldSlot(final int jMax, final int interpolationPoints) {

            dij = (FieldShortPeriodicsInterpolatedCoefficient<T>[]) Array
                    .newInstance(FieldShortPeriodicsInterpolatedCoefficient.class, 3);
            cij = (FieldShortPeriodicsInterpolatedCoefficient<T>[]) Array
                    .newInstance(FieldShortPeriodicsInterpolatedCoefficient.class, jMax + 1);
            sij = (FieldShortPeriodicsInterpolatedCoefficient<T>[]) Array
                    .newInstance(FieldShortPeriodicsInterpolatedCoefficient.class, jMax + 1);

            // Initialize the C<sub>i</sub><sup>j</sup>, S<sub>i</sub><sup>j</sup> and
            // D<sub>i</sub><sup>j</sup> coefficients
            for (int j = 0; j <= jMax; j++) {
                cij[j] = new FieldShortPeriodicsInterpolatedCoefficient<>(interpolationPoints);
                if (j > 0) {
                    sij[j] = new FieldShortPeriodicsInterpolatedCoefficient<>(interpolationPoints);
                }
                // Initialize only the non-zero D<sub>i</sub><sup>j</sup> coefficients
                if (j == 1 || j == 2) {
                    dij[j] = new FieldShortPeriodicsInterpolatedCoefficient<>(interpolationPoints);
                }
            }

        }

    }

}
