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

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.ParameterDriver;

/**
 * Second order J2-squared force model.
 * <p>
 * The force model implements a closed-form of the J2-squared perturbation.
 * The full realization of the model is based on a gaussian quadrature.
 * Even if it is very accurate, a gaussian quadrature is usually time consuming.
 * A closed-form is less accurate than a gaussian quadrature, but faster.
 * </p>
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class DSSTJ2SquaredClosedForm implements DSSTForceModel {

    /** Model for second order terms. */
    private final J2SquaredModel j2SquaredModel;

    /** Gravity field to use. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /**
     * Constructor.
     *
     * @param j2SquaredModel model for second order terms
     * @param provider       gravity field to use
     */
    public DSSTJ2SquaredClosedForm(final J2SquaredModel j2SquaredModel,
                                   final UnnormalizedSphericalHarmonicsProvider provider) {
        // Initialize fields
        this.j2SquaredModel = j2SquaredModel;
        this.provider = provider;
    }

    /** {@inheritDoc}. */
    @Override
    public double[] getMeanElementRate(final SpacecraftState state,
                                       final AuxiliaryElements auxiliaryElements,
                                       final double[] parameters) {

        // Context
        final DSSTJ2SquaredClosedFormContext context = new DSSTJ2SquaredClosedFormContext(auxiliaryElements, provider);

        // Second-order terms
        final double[] delta = j2SquaredModel.computeMeanEquinoctialSecondOrderTerms(context);

        // J2
        final double J2 = -provider.onDate(state.getDate()).getUnnormalizedCnm(2, 0);
        final double J2SquaredOver2 = 0.5 * J2 * J2;

        // Mean elements rate
        final double da = 0.0;
        final double dk = J2SquaredOver2 * delta[1];
        final double dh = J2SquaredOver2 * delta[2];
        final double dq = J2SquaredOver2 * delta[3];
        final double dp = J2SquaredOver2 * delta[4];
        final double dM = J2SquaredOver2 * delta[5];

        // Return
        return new double[] { da, dk, dh, dq, dp, dM };

    }

    /** {@inheritDoc}. */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getMeanElementRate(final FieldSpacecraftState<T> state,
                                                                      final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                      final T[] parameters) {

        // Field
        final Field<T> field = state.getDate().getField();

        // Context
        final FieldDSSTJ2SquaredClosedFormContext<T> context = new FieldDSSTJ2SquaredClosedFormContext<>(auxiliaryElements, provider);

        // Second-order terms
        final T[] delta = j2SquaredModel.computeMeanEquinoctialSecondOrderTerms(context);

        // J2
        final double J2 = -provider.onDate(state.getDate().toAbsoluteDate()).getUnnormalizedCnm(2, 0);
        final double J2SquaredOver2 = 0.5 * J2 * J2;

        // Mean elements rate
        final T da = field.getZero();
        final T dk = delta[1].multiply(J2SquaredOver2);
        final T dh = delta[2].multiply(J2SquaredOver2);
        final T dq = delta[3].multiply(J2SquaredOver2);
        final T dp = delta[4].multiply(J2SquaredOver2);
        final T dM = delta[5].multiply(J2SquaredOver2);

        // Return
        final T[] elements =  MathArrays.buildArray(field, 6);
        elements[0] = da;
        elements[1] = dk;
        elements[2] = dh;
        elements[3] = dq;
        elements[4] = dp;
        elements[5] = dM;

        return elements;

    }

    /** {@inheritDoc}. */
    @Override
    public List<ShortPeriodTerms> initializeShortPeriodTerms(final AuxiliaryElements auxiliaryElements,
                                                             final PropagationType type,
                                                             final double[] parameters) {
        // Currently, there is no short periods for J2-squared closed-form
        return Collections.emptyList();
    }

    /** {@inheritDoc}. */
    @Override
    public <T extends CalculusFieldElement<T>> List<FieldShortPeriodTerms<T>> initializeShortPeriodTerms(final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                                         final PropagationType type,
                                                                                                         final T[] parameters) {
        // Currently, there is no short periods for J2-squared closed-form
        return Collections.emptyList();
    }

    /** {@inheritDoc}. */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc}. */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider attitudeProvider) {
        // Nothing is done since this contribution is not sensitive to attitude
    }

    /** {@inheritDoc}. */
    @Override
    public void updateShortPeriodTerms(final double[] parameters, final SpacecraftState... meanStates) {
        // Currently, there is no short periods for J2-squared closed-form
    }

    /** {@inheritDoc}. */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends CalculusFieldElement<T>> void updateShortPeriodTerms(final T[] parameters, final FieldSpacecraftState<T>... meanStates) {
        // Currently, there is no short periods for J2-squared closed-form
    }

}
