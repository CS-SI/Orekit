/* Copyright 2002-2023 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.analytical.tle.generation;

import java.util.function.UnaryOperator;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.linear.FieldVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresBuilder;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.MultivariateJacobianFunction;
import org.hipparchus.util.Pair;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.FieldTLEPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/**
 * Least squares method to generate a usable TLE from a spacecraft state.
 *
 * @author Mark Rutten
 * @since 12.0
 */
public class LeastSquaresTleGenerationAlgorithm implements TleGenerationAlgorithm {

    /** Default value for maximum number of iterations.*/
    public static final int DEFAULT_MAX_ITERATIONS = 1000;

    /** UTC scale. */
    private final TimeScale utc;

    /** TEME frame. */
    private final Frame teme;

    /** Maximum number of iterations. */
    private final int maxIterations;

    /** RMS. */
    private double rms;

    /**
     * Default constructor.
     * <p>
     * Uses the {@link DataContext#getDefault() default data context}  as well as
     * {@link #DEFAULT_MAX_ITERATIONS}.
     * </p>
     */
    @DefaultDataContext
    public LeastSquaresTleGenerationAlgorithm() {
        this(DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Default constructor.
     * <p>
     * Uses the {@link DataContext#getDefault() default data context}.
     * </p>
     * @param maxIterations maximum number of iterations for convergence
     */
    @DefaultDataContext
    public LeastSquaresTleGenerationAlgorithm(final int maxIterations) {
        this(maxIterations, DataContext.getDefault().getTimeScales().getUTC(),
             DataContext.getDefault().getFrames().getTEME());
    }

    /**
     * Constructor.
     * @param maxIterations maximum number of iterations for convergence
     * @param utc UTC time scale
     * @param teme TEME frame
     */
    public LeastSquaresTleGenerationAlgorithm(final int maxIterations,
                                              final TimeScale utc, final Frame teme) {
        this.maxIterations = maxIterations;
        this.utc           = utc;
        this.teme          = teme;
        this.rms           = Double.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @Override
    public TLE generate(final SpacecraftState state, final TLE templateTLE) {

        // Generation epoch
        final AbsoluteDate epoch = state.getDate();

        // State vector
        final RealVector stateVector = MatrixUtils.createRealVector(6);
        // Position/Velocity
        final Vector3D position = state.getPVCoordinates().getPosition();
        final Vector3D velocity = state.getPVCoordinates().getVelocity();

        // Fill state vector
        stateVector.setEntry(0, position.getX());
        stateVector.setEntry(1, position.getY());
        stateVector.setEntry(2, position.getZ());
        stateVector.setEntry(3, velocity.getX());
        stateVector.setEntry(4, velocity.getY());
        stateVector.setEntry(5, velocity.getZ());

        // Create the initial guess of the least squares problem
        final RealVector startState = MatrixUtils.createRealVector(7);
        startState.setSubVector(0, stateVector.getSubVector(0, 6));

        // Weights
        final double[] weights = new double[6];
        final double velocityWeight = state.getPVCoordinates().getVelocity().getNorm() * state.getPosition().getNormSq() / state.getMu();
        for (int i = 0; i < 3; i++) {
            weights[i] = 1.0;
            weights[i + 3] = velocityWeight;
        }

        // Time difference between template TLE and spacecraft state
        final double dt = state.getDate().durationFrom(templateTLE.getDate());

        // Construct least squares problem
        final LeastSquaresProblem problem =
                        new LeastSquaresBuilder().maxIterations(maxIterations)
                                                 .maxEvaluations(maxIterations)
                                                 .model(new ObjectiveFunction(templateTLE, dt))
                                                 .target(stateVector)
                                                 .weight(new DiagonalMatrix(weights))
                                                 .start(startState)
                                                 .build();

        // Solve least squares
        final LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        final LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(problem);
        rms = optimum.getRMS();

        // Create new TLE from mean state
        final Vector3D positionEstimated  = new Vector3D(optimum.getPoint().getSubVector(0, 3).toArray());
        final Vector3D velocityEstimated  = new Vector3D(optimum.getPoint().getSubVector(3, 3).toArray());
        final PVCoordinates pvCoordinates = new PVCoordinates(positionEstimated, velocityEstimated);
        final KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, teme, epoch, state.getMu());
        final TLE generated = TleGenerationUtil.newTLE(orbit, templateTLE, templateTLE.getBStar(), utc);

        // Verify if parameters are estimated
        for (final ParameterDriver templateDrivers : templateTLE.getParametersDrivers()) {
            if (templateDrivers.isSelected()) {
                // Set to selected for the new TLE
                generated.getParameterDriver(templateDrivers.getName()).setSelected(true);
            }
        }

        // Return
        return generated;

    }

    /**
     * Get the Root Mean Square of the TLE estimation.
     * <p>
     * Be careful that the RMS is updated each time the
     * {@link LeastSquaresTleGenerationAlgorithm#generate(SpacecraftState, TLE)}
     * method is called.
     * </p>
     * @return the RMS
     */
    public double getRms() {
        return rms;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTLE<T> generate(final FieldSpacecraftState<T> state,
                                                                    final FieldTLE<T> templateTLE) {
        throw new UnsupportedOperationException();
    }

    /** Least squares model. */
    private class ObjectiveFunction implements MultivariateJacobianFunction {

        /** Template TLE. */
        private final FieldTLE<Gradient> templateTLE;

        /** Time difference between template TLE and spacecraft state (s). */
        private final double dt;

        /**
         * Constructor.
         * @param templateTLE template TLE
         * @param dt time difference between template TLE and spacecraft state (s)
         */
        ObjectiveFunction(final TLE templateTLE, final double dt) {
            this.dt = dt;
            // Conversion of template TLE to a field TLE
            final Field<Gradient> field = GradientField.getField(7);
            this.templateTLE = new FieldTLE<>(field, templateTLE.getLine1(), templateTLE.getLine2(), utc);
        }

        /**  {@inheritDoc} */
        @Override
        public Pair<RealVector, RealMatrix> value(final RealVector point) {
            final RealVector objectiveOscState = MatrixUtils.createRealVector(6);
            final RealMatrix objectiveJacobian = MatrixUtils.createRealMatrix(6, 7);
            getTransformedAndJacobian(state -> meanStateToPV(state), point, objectiveOscState, objectiveJacobian);
            return new Pair<>(objectiveOscState, objectiveJacobian);
        }

        /**
         * Fill model.
         * @param operator state vector propagation
         * @param state state vector
         * @param transformed value to fill
         * @param jacobian Jacobian to fill
         */
        private void getTransformedAndJacobian(final UnaryOperator<FieldVector<Gradient>> operator,
                                               final RealVector state, final RealVector transformed,
                                               final RealMatrix jacobian) {

            // State dimension
            final int stateDim = state.getDimension();

            // Initialise the state as field to calculate the gradient
            final GradientField field = GradientField.getField(stateDim);
            final FieldVector<Gradient> fieldState = MatrixUtils.createFieldVector(field, stateDim);
            for (int i = 0; i < stateDim; ++i) {
                fieldState.setEntry(i, Gradient.variable(stateDim, i, state.getEntry(i)));
            }

            // Call operator
            final FieldVector<Gradient> fieldTransformed = operator.apply(fieldState);

            // Output dimension
            final int outDim = fieldTransformed.getDimension();

            // Extract transform and Jacobian as real values
            for (int i = 0; i < outDim; ++i) {
                transformed.setEntry(i, fieldTransformed.getEntry(i).getReal());
                jacobian.setRow(i, fieldTransformed.getEntry(i).getGradient());
            }

        }

        /**
         * Operator to propagate the state vector.
         * @param state state vector
         * @return propagated state vector
         */
        private FieldVector<Gradient> meanStateToPV(final FieldVector<Gradient> state) {
            // Epoch
            final FieldAbsoluteDate<Gradient> epoch = templateTLE.getDate();

            // B*
            final Gradient[] bStar = state.getSubVector(6, 1).toArray();

            // Field
            final Field<Gradient> field = epoch.getField();

            // Extract mean state
            final FieldVector3D<Gradient> position = new FieldVector3D<>(state.getSubVector(0, 3).toArray());
            final FieldVector3D<Gradient> velocity = new FieldVector3D<>(state.getSubVector(3, 3).toArray());
            final FieldPVCoordinates<Gradient> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
            final FieldKeplerianOrbit<Gradient> orbit = new FieldKeplerianOrbit<>(pvCoordinates, teme, epoch, field.getZero().add(TLEPropagator.getMU()));

            // Convert to TLE
            final FieldTLE<Gradient> tle = TleGenerationUtil.newTLE(orbit, templateTLE, bStar[0], utc);

            // Propagate to epoch
            final FieldPVCoordinates<Gradient> propagatedCoordinates =
                    FieldTLEPropagator.selectExtrapolator(tle, teme, bStar).getPVCoordinates(epoch.shiftedBy(dt), bStar);

            // Osculating
            final FieldVector<Gradient> osculating = MatrixUtils.createFieldVector(field, 6);
            osculating.setEntry(0, propagatedCoordinates.getPosition().getX());
            osculating.setEntry(1, propagatedCoordinates.getPosition().getY());
            osculating.setEntry(2, propagatedCoordinates.getPosition().getZ());
            osculating.setEntry(3, propagatedCoordinates.getVelocity().getX());
            osculating.setEntry(4, propagatedCoordinates.getVelocity().getY());
            osculating.setEntry(5, propagatedCoordinates.getVelocity().getZ());

            // Return
            return osculating;

        }

    }

}
