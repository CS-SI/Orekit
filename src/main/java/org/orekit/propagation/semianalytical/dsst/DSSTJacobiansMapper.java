/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.errors.OrekitInternalError;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @see org.orekit.propagation.semianalytical.dsst.DSSTPartialDerivativesEquations
 * @see org.orekit.propagation.semianalytical.dsst.DSSTPropagator
 * @see SpacecraftState#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 */
public class DSSTJacobiansMapper extends AbstractJacobiansMapper {

    /** State dimension, fixed to 6.
     * @since 9.0
     */
    public static final int STATE_DIMENSION = 6;

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

    /** Name. */
    private String name;

    /** Selected parameters for Jacobian computation. */
    private final ParameterDriversList parameters;

    /** Parameters map. */
    private Map<ParameterDriver, Integer> map;

    /** Propagator computing state evolution. */
    private final DSSTPropagator propagator;

    /** Placeholder for the derivatives of the short period terms.*/
    private double[] shortPeriodDerivatives;

    /** Type of the orbit used for the propagation.*/
    private PropagationType propagationType;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     * @param propagator the propagator that will handle the orbit propagation
     * @param map parameters map
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     */
    DSSTJacobiansMapper(final String name,
                        final ParameterDriversList parameters,
                        final DSSTPropagator propagator,
                        final Map<ParameterDriver, Integer> map,
                        final PropagationType propagationType) {

        super(name, parameters);

        shortPeriodDerivatives = null;

        this.parameters      = parameters;
        this.name            = name;
        this.propagator      = propagator;
        this.map             = map;
        this.propagationType = propagationType;

    }

    /** {@inheritDoc} */
    protected double[][] getConversionJacobian(final SpacecraftState state) {

        final double[][] identity = new double[STATE_DIMENSION][STATE_DIMENSION];

        for (int i = 0; i < STATE_DIMENSION; ++i) {
            identity[i][i] = 1.0;
        }

        return identity;

    }

    /** {@inheritDoc} */
    public void setInitialJacobians(final SpacecraftState state, final double[][] dY1dY0,
                                    final double[][] dY1dP, final double[] p) {

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                p[index++] = (i == j) ? 1.0 : 0.0;
            }
        }

        if (parameters.getNbParams() != 0) {

            // map the converted parameters Jacobian to one-dimensional array
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                for (int j = 0; j < parameters.getNbParams(); ++j) {
                    p[index++] = dY1dP[i][j];
                }
            }
        }

    }

    /** {@inheritDoc} */
    public void getStateJacobian(final SpacecraftState state, final double[][] dYdY0) {

        // extract additional state
        final double[] p = state.getAdditionalState(name);

        for (int i = 0; i < STATE_DIMENSION; i++) {
            final double[] row = dYdY0[i];
            for (int j = 0; j < STATE_DIMENSION; j++) {
                row[j] = p[i * STATE_DIMENSION + j] + shortPeriodDerivatives[i * STATE_DIMENSION + j];
            }
        }

    }


    /** {@inheritDoc} */
    public void getParametersJacobian(final SpacecraftState state, final double[][] dYdP) {

        if (parameters.getNbParams() != 0) {

            // extract the additional state
            final double[] p = state.getAdditionalState(name);

            for (int i = 0; i < STATE_DIMENSION; i++) {
                final double[] row = dYdP[i];
                for (int j = 0; j < parameters.getNbParams(); j++) {
                    row[j] = p[STATE_DIMENSION * STATE_DIMENSION + (j + parameters.getNbParams() * i)] +
                             shortPeriodDerivatives[STATE_DIMENSION * STATE_DIMENSION + (j + parameters.getNbParams() * i)];
                }
            }

        }

    }

    /** {@inheritDoc} */
    @Override
    public int getAdditionalStateDimension() {
        return STATE_DIMENSION * (STATE_DIMENSION + parameters.getNbParams());
    }

    /** Compute the derivatives of the short period terms related to the additional state parameters.
    * @param s Current state information: date, kinematics, attitude, and additional state
    */
    @SuppressWarnings("unchecked")
    public void setShortPeriodJacobians(final SpacecraftState s) {

        final double[] p = s.getAdditionalState(name);
        if (shortPeriodDerivatives == null) {
            shortPeriodDerivatives = new double[p.length];
        }

        switch (propagationType) {
            case MEAN :
                break;
            case OSCULATING :
                // initialize Jacobians to zero
                final int paramDim = parameters.getNbParams();
                final int dim = 6;
                final double[][] dShortPerioddState = new double[dim][dim];
                final double[][] dShortPerioddParam = new double[dim][paramDim];
                final DSSTGradientConverter converter = new DSSTGradientConverter(s, propagator.getAttitudeProvider());

                // Compute Jacobian
                for (final DSSTForceModel forceModel : propagator.getAllForceModels()) {

                    final FieldSpacecraftState<Gradient> dsState = converter.getState(forceModel);
                    final Gradient[] dsParameters = converter.getParameters(dsState, forceModel);
                    final FieldAuxiliaryElements<Gradient> auxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), I);

                    final Gradient zero = dsState.getDate().getField().getZero();
                    final List<FieldShortPeriodTerms<Gradient>> shortPeriodTerms = new ArrayList<>();
                    shortPeriodTerms.addAll(forceModel.initialize(auxiliaryElements, propagationType, dsParameters));
                    forceModel.updateShortPeriodTerms(dsParameters, dsState);
                    final Gradient[] shortPeriod = new Gradient[6];
                    Arrays.fill(shortPeriod, zero);
                    for (final FieldShortPeriodTerms<Gradient> spt : shortPeriodTerms) {
                        final Gradient[] spVariation = spt.value(dsState.getOrbit());
                        for (int i = 0; i < spVariation .length; i++) {
                            shortPeriod[i] = shortPeriod[i].add(spVariation[i]);
                        }
                    }

                    final double[] derivativesASP  = shortPeriod[0].getGradient();
                    final double[] derivativesExSP = shortPeriod[1].getGradient();
                    final double[] derivativesEySP = shortPeriod[2].getGradient();
                    final double[] derivativesHxSP = shortPeriod[3].getGradient();
                    final double[] derivativesHySP = shortPeriod[4].getGradient();
                    final double[] derivativesLSP  = shortPeriod[5].getGradient();

                    // update Jacobian with respect to state
                    addToRow(derivativesASP,  0, dShortPerioddState);
                    addToRow(derivativesExSP, 1, dShortPerioddState);
                    addToRow(derivativesEySP, 2, dShortPerioddState);
                    addToRow(derivativesHxSP, 3, dShortPerioddState);
                    addToRow(derivativesHySP, 4, dShortPerioddState);
                    addToRow(derivativesLSP,  5, dShortPerioddState);

                    int index = converter.getFreeStateParameters();
                    for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                        if (driver.isSelected()) {
                            final int parameterIndex = map.get(driver);
                            dShortPerioddParam[0][parameterIndex] += derivativesASP[index];
                            dShortPerioddParam[1][parameterIndex] += derivativesExSP[index];
                            dShortPerioddParam[2][parameterIndex] += derivativesEySP[index];
                            dShortPerioddParam[3][parameterIndex] += derivativesHxSP[index];
                            dShortPerioddParam[4][parameterIndex] += derivativesHySP[index];
                            dShortPerioddParam[5][parameterIndex] += derivativesLSP[index];
                            ++index;
                        }
                    }
                }

                // Get orbital short period derivatives with respect orbital elements.
                for (int i = 0; i < dim; i++) {
                    for (int j = 0; j < dim; j++) {
                        shortPeriodDerivatives[j + dim * i] = dShortPerioddState[i][j];
                    }
                }

                // Get orbital short period derivatives with respect to model parameters.
                final int columnTop = dim * dim;
                for (int k = 0; k < paramDim; k++) {
                    for (int i = 0; i < dim; ++i) {
                        shortPeriodDerivatives[columnTop + (i + dim * k)] = dShortPerioddParam[i][k];
                    }
                }
                break;
            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for ex, 2 for ey, 3 for hx, 4 for hy, 5 for l)
     * @param dMeanElementRatedElement Jacobian of mean elements rate with respect to mean elements
     */
    private void addToRow(final double[] derivatives, final int index,
                          final double[][] dMeanElementRatedElement) {

        for (int i = 0; i < 6; i++) {
            dMeanElementRatedElement[index][i] += derivatives[i];
        }

    }

}
