/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.conversion.osc2mean;

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
import org.hipparchus.optim.ConvergenceChecker;
import org.hipparchus.optim.SimpleVectorValueChecker;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresBuilder;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresFactory;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.hipparchus.optim.nonlinear.vector.leastsquares.MultivariateJacobianFunction;
import org.hipparchus.util.Pair;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Class enabling conversion from osculating to mean orbit
 * for a given theory using a least-squares algorithm.
 *
 * @author Pascal Parraud
 * @since 13.0
 */
public class LeastSquaresConverter implements OsculatingToMeanConverter {

    /** Default convergence threshold. */
    public static final double DEFAULT_THRESHOLD   = 1e-4;

    /** Default maximum number of iterations. */
    public static final int DEFAULT_MAX_ITERATIONS = 1000;

    /** Mean theory used. */
    private MeanTheory theory;

    /** Convergence threshold. */
    private double threshold;

    /** Maximum number of iterations. */
    private int maxIterations;

    /** Optimizer used. */
    private LeastSquaresOptimizer optimizer;

    /** Convergence checker for optimization algorithm. */
    private ConvergenceChecker<LeastSquaresProblem.Evaluation> checker;

    /** RMS. */
    private double rms;

    /** Number of iterations performed. */
    private int iterationsNb;

    /**
     * Default constructor.
     * <p>
     * The mean theory and the optimizer must be set before converting.
     */
    public LeastSquaresConverter() {
        this(null, null, DEFAULT_THRESHOLD, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Constructor.
     * <p>
     * The optimizer must be set before converting.
     *
     * @param theory mean theory to be used
     */
    public LeastSquaresConverter(final MeanTheory theory) {
        this(theory, null, DEFAULT_THRESHOLD, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Constructor.
     * @param theory mean theory to be used
     * @param optimizer optimizer to be used
     */
    public LeastSquaresConverter(final MeanTheory theory,
                                 final LeastSquaresOptimizer optimizer) {
        this(theory, optimizer, DEFAULT_THRESHOLD, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Constructor.
     * <p>
     * The mean theory and the optimizer must be set before converting.
     *
     * @param threshold     convergence threshold
     * @param maxIterations maximum number of iterations
     */
    public LeastSquaresConverter(final double threshold,
                                 final int maxIterations) {
        this(null, null, threshold, maxIterations);
    }

    /**
     * Constructor.
     * @param theory        mean theory to be used
     * @param optimizer     optimizer to be used
     * @param threshold     convergence threshold
     * @param maxIterations maximum number of iterations
     */
    public LeastSquaresConverter(final MeanTheory theory,
                                 final LeastSquaresOptimizer optimizer,
                                 final double threshold,
                                 final int maxIterations) {
        setMeanTheory(theory);
        setOptimizer(optimizer);
        setThreshold(threshold);
        setMaxIterations(maxIterations);
    }

    /** {@inheritDoc} */
    @Override
    public MeanTheory getMeanTheory() {
        return theory;
    }

    /** {@inheritDoc} */
    @Override
    public void setMeanTheory(final MeanTheory meanTheory) {
        this.theory = meanTheory;
    }

    /**
     * Gets the optimizer.
     * @return the optimizer
     */
    public LeastSquaresOptimizer getOptimizer() {
        return optimizer;
    }

    /**
     * Sets the optimizer.
     * @param optimizer the optimizer
     */
    public void setOptimizer(final LeastSquaresOptimizer optimizer) {
        this.optimizer = optimizer;
    }

    /**
     * Gets the convergence threshold.
     * @return convergence threshold
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Sets the convergence threshold.
     * @param threshold convergence threshold
     */
    public void setThreshold(final double threshold) {
        this.threshold = threshold;
        final SimpleVectorValueChecker svvc = new SimpleVectorValueChecker(-1.0, threshold);
        this.checker = LeastSquaresFactory.evaluationChecker(svvc);
    }

    /**
     * Gets the maximum number of iterations.
     * @return maximum number of iterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Sets maximum number of iterations.
     * @param maxIterations maximum number of iterations
     */
    public void setMaxIterations(final int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Gets the RMS for the last conversion.
     * @return the RMS
     */
    public double getRMS() {
        return rms;
    }

    /**
     * Gets the number of iterations performed by the last conversion.
     * @return number of iterations
     */
    public int getIterationsNb() {
        return iterationsNb;
    }

    /** {@inheritDoc}
     *  Uses a least-square algorithm.
     */
    @Override
    public Orbit convertToMean(final Orbit osculating) {

        // Initialize conversion
        final Orbit initialized = theory.preprocessing(osculating);

        // State vector
        final RealVector stateVector = MatrixUtils.createRealVector(6);

        // Position/Velocity
        final Vector3D position = initialized.getPVCoordinates().getPosition();
        final Vector3D velocity = initialized.getVelocity();

        // Fill state vector
        stateVector.setEntry(0, position.getX());
        stateVector.setEntry(1, position.getY());
        stateVector.setEntry(2, position.getZ());
        stateVector.setEntry(3, velocity.getX());
        stateVector.setEntry(4, velocity.getY());
        stateVector.setEntry(5, velocity.getZ());

        // Create the initial guess of the least squares problem
        final RealVector startState = MatrixUtils.createRealVector(6);
        startState.setSubVector(0, stateVector.getSubVector(0, 6));

        // Weights
        final double[] weights = new double[6];
        final double velocityWeight = initialized.getVelocity().getNorm() *
                                      initialized.getPVCoordinates().getPosition().getNormSq() / initialized.getMu();
        for (int i = 0; i < 3; i++) {
            weights[i] = 1.0;
            weights[i + 3] = velocityWeight;
        }

        // Constructs the least squares problem
        final LeastSquaresProblem problem = new LeastSquaresBuilder().
                                            maxIterations(maxIterations).
                                            maxEvaluations(Integer.MAX_VALUE).
                                            checker(checker).
                                            model(new ModelFunction(initialized)).
                                            weight(new DiagonalMatrix(weights)).
                                            target(stateVector).
                                            start(startState).
                                            build();

        // Solve least squares
        final LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(problem);

        // Stores some results
        rms = optimum.getRMS();
        iterationsNb = optimum.getIterations();

        // Builds the estimated mean orbit
        final Vector3D pEstimated = new Vector3D(optimum.getPoint().getSubVector(0, 3).toArray());
        final Vector3D vEstimated = new Vector3D(optimum.getPoint().getSubVector(3, 3).toArray());
        final Orbit mean = new CartesianOrbit(new PVCoordinates(pEstimated, vEstimated),
                                              initialized.getFrame(), initialized.getDate(),
                                              initialized.getMu());

        // Returns the mean orbit
        return theory.postprocessing(osculating, mean);
    }

    /** {@inheritDoc}
     *  Uses a least-square algorithm.
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> convertToMean(final FieldOrbit<T> osculating) {
        throw new UnsupportedOperationException();
    }

    /** Model function for the least squares problem.
     * Provides the Jacobian of the function computing position/velocity at the point.
     */
    private class ModelFunction implements MultivariateJacobianFunction {

        /** Osculating orbit as Cartesian. */
        private final FieldCartesianOrbit<Gradient> fieldOsc;

        /**
         * Constructor.
         * @param osculating osculating orbit
         */
        ModelFunction(final Orbit osculating) {
            // Conversion to field orbit
            final Field<Gradient> field = GradientField.getField(6);
            this.fieldOsc = new FieldCartesianOrbit<>(field, osculating);
        }

        /**  {@inheritDoc} */
        @Override
        public Pair<RealVector, RealMatrix> value(final RealVector point) {
            final RealVector objectiveOscState = MatrixUtils.createRealVector(6);
            final RealMatrix objectiveJacobian = MatrixUtils.createRealMatrix(6, 6);
            getTransformedAndJacobian(state -> mean2Osc(state), point,
                                      objectiveOscState, objectiveJacobian);
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
         * Operator to compute an osculating state from a mean state.
         * @param mean mean state vector
         * @return osculating state vector
         */
        private FieldVector<Gradient> mean2Osc(final FieldVector<Gradient> mean) {
            // Epoch
            final FieldAbsoluteDate<Gradient> epoch = fieldOsc.getDate();

            // Field
            final Field<Gradient> field = epoch.getField();

            // Extract mean state
            final FieldVector3D<Gradient> pos = new FieldVector3D<>(mean.getSubVector(0, 3).toArray());
            final FieldVector3D<Gradient> vel = new FieldVector3D<>(mean.getSubVector(3, 3).toArray());
            final FieldPVCoordinates<Gradient> pvMean = new FieldPVCoordinates<>(pos, vel);
            final FieldKeplerianOrbit<Gradient> oMean = new FieldKeplerianOrbit<>(pvMean,
                                                                                  fieldOsc.getFrame(),
                                                                                  fieldOsc.getDate(),
                                                                                  fieldOsc.getMu());

            // Propagate to epoch
            final FieldOrbit<Gradient> oOsc = theory.meanToOsculating(oMean);
            final FieldPVCoordinates<Gradient> pvOsc = oOsc.getPVCoordinates(oMean.getFrame());

            // Osculating
            final FieldVector<Gradient> osculating = MatrixUtils.createFieldVector(field, 6);
            osculating.setEntry(0, pvOsc.getPosition().getX());
            osculating.setEntry(1, pvOsc.getPosition().getY());
            osculating.setEntry(2, pvOsc.getPosition().getZ());
            osculating.setEntry(3, pvOsc.getVelocity().getX());
            osculating.setEntry(4, pvOsc.getVelocity().getY());
            osculating.setEntry(5, pvOsc.getVelocity().getZ());

            // Return
            return osculating;

        }

    }

}
