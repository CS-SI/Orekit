/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.propagation.analytical.AbstractAnalyticalMatricesHarvester;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Harvester between two-dimensional Jacobian matrices and
 * one-dimensional {@link TLEPropagator}.
 * @author Thomas Paulet
 * @author Bryan Cazabonne
 * @since 11.1
 */
class TLEHarvester extends AbstractAnalyticalMatricesHarvester {

    /** Propagator bound to this harvester. */
    private final TLEPropagator propagator;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the
     * {@link org.orekit.orbits.OrbitType orbit type}
     * and {@link PositionAngleType position angle} that will be used by propagator
     * </p>
     * @param propagator propagator bound to this harvester
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂C/∂K₀,
     *                   if null (which is the most frequent case), assumed to be just the
     *                   conversion from Keplerian type to Cartesian type at t₀
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    TLEHarvester(final TLEPropagator propagator, final String stmName,
                 final RealMatrix initialStm, final DoubleArrayDictionary initialJacobianColumns) {
        super(propagator);
        this.propagator = propagator;
        setInitialStm(stmName, initialStm);
        setInitialJacobianColumns(initialJacobianColumns);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getInitialStateJacobianVsBuilderParameters() {

        // create gradient TLE with respect to build parameters
        final GradientField field = GradientField.getField(DEFAULT_STATE_DIMENSION);
        final TLE tle = propagator.getTLE();
        final FieldTLE<Gradient> gTLE =
            new FieldTLE<>(tle.getSatelliteNumber(), tle.getClassification(),
                           tle.getLaunchYear(), tle.getLaunchNumber(), tle.getLaunchPiece(),
                           tle.getEphemerisType(), tle.getElementNumber(),
                           new FieldAbsoluteDate<>(field, tle.getDate()),
                           Gradient.variable(DEFAULT_STATE_DIMENSION, 0, tle.getMeanMotion()),
                           Gradient.constant(DEFAULT_STATE_DIMENSION,    tle.getMeanMotionFirstDerivative()),
                           Gradient.constant(DEFAULT_STATE_DIMENSION,    tle.getMeanMotionSecondDerivative()),
                           Gradient.variable(DEFAULT_STATE_DIMENSION, 1, tle.getE()),
                           Gradient.variable(DEFAULT_STATE_DIMENSION, 2, tle.getI()),
                           Gradient.variable(DEFAULT_STATE_DIMENSION, 3, tle.getPerigeeArgument()),
                           Gradient.variable(DEFAULT_STATE_DIMENSION, 4, tle.getRaan()),
                           Gradient.variable(DEFAULT_STATE_DIMENSION, 5, tle.getMeanAnomaly()),
                           tle.getRevolutionNumberAtEpoch(),
                           Gradient.constant(DEFAULT_STATE_DIMENSION,    tle.getBStar()),
                           tle.getUtc());

        // evaluate initial Cartesian state
        final TimeStampedFieldPVCoordinates<Gradient> pv =
            FieldTLEPropagator.selectExtrapolator(gTLE).getBaseInitialState().getPVCoordinates();

        // create Jacobian matrix
        final RealMatrix jacobian =
            MatrixUtils.createRealMatrix(DEFAULT_STATE_DIMENSION, DEFAULT_STATE_DIMENSION);
        jacobian.setRow(0, pv.getPosition().getX().getGradient());
        jacobian.setRow(1, pv.getPosition().getY().getGradient());
        jacobian.setRow(2, pv.getPosition().getZ().getGradient());
        jacobian.setRow(3, pv.getVelocity().getX().getGradient());
        jacobian.setRow(4, pv.getVelocity().getY().getGradient());
        jacobian.setRow(5, pv.getVelocity().getZ().getGradient());

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    public AbstractAnalyticalGradientConverter getGradientConverter() {
        return new TLEGradientConverter(propagator);
    }

}
