/* Copyright 2020-2025 Exotrail
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
package org.orekit.control.heuristics.lambert;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.DataDictionary;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Class implementing a differential correction for extended (meaning under arbitrary dynamics) Lambert arc
 * (fixed initial and terminal position vectors with fixed time).
 * Given boundary conditions and a guess, applies Newton-Raphson algorithm to find the solution
 * (initial and terminal velocity vectors) according to the propagator used.
 * The latter must be compatible with resetting the initial state and computing state transition matrices.
 * Note that propagation is not required to be forward in time.
 *
 * @author Romain Serra
 * @since 13.1
 * @see AbstractPropagator
 * @see LambertBoundaryConditions
 */
public class LambertDifferentialCorrector {

    /** Default value for convergence (on the position vector). */
    public static final double DEFAULT_POSITION_TOLERANCE = 1e-4;

    /** Default maximum number of iterations. */
    public static final int DEFAULT_MAX_ITER = 10;

    /** Lambert boundary conditions. */
    private final LambertBoundaryConditions lambertBoundaryConditions;

    /** State transition matrices harvester. */
    private MatricesHarvester matricesHarvester;

    /** Threshold for linear system solving. */
    private double thresholdMatrixSolver = Precision.EPSILON;

    /** Initial mass. */
    private double initialMass = SpacecraftState.DEFAULT_MASS;

    /** Position tolerance for convergence. */
    private double positionTolerance = DEFAULT_POSITION_TOLERANCE;

    /** Current iteration number. */
    private int currentIter = 0;

    /** Maximum number of iterations. */
    private int maxIter = DEFAULT_MAX_ITER;

    /** Name of state transition matrix. */
    private String stmName = "stm";

    /**
     * Constructor.
     * @param lambertBoundaryConditions boundary conditions
     */
    public LambertDifferentialCorrector(final LambertBoundaryConditions lambertBoundaryConditions) {
        this.lambertBoundaryConditions = lambertBoundaryConditions;
    }

    /**
     * Getter for the boundary conditions.
     * @return conditions
     */
    public LambertBoundaryConditions getLambertBoundaryConditions() {
        return lambertBoundaryConditions;
    }

    /**
     * Setter for the initial mass.
     * @param initialMass initial mass
     */
    public void setInitialMass(final double initialMass) {
        this.initialMass = initialMass;
    }

    /**
     * Getter for the initial mass.
     * @return initial mass
     */
    public double getInitialMass() {
        return initialMass;
    }

    /**
     * Getter for the position tolerance.
     * @return tolerance
     */
    public double getPositionTolerance() {
        return positionTolerance;
    }

    /**
     * Setter for the position tolerance.
     * @param positionTolerance tolerance
     */
    public void setPositionTolerance(final double positionTolerance) {
        this.positionTolerance = positionTolerance;
    }

    /**
     * Setter for the threshold used in linear system solving.
     * @param thresholdMatrixSolver threshold
     */
    public void setThresholdMatrixSolver(final double thresholdMatrixSolver) {
        this.thresholdMatrixSolver = thresholdMatrixSolver;
    }

    /**
     * Getter for the threshold used in linear system solving.
     * @return threshold
     */
    public double getThresholdMatrixSolver() {
        return thresholdMatrixSolver;
    }

    /**
     * Setter for the state transition matrix name.
     * @param stmName name
     */
    public void setStmName(final String stmName) {
        this.stmName = stmName;
    }

    /**
     * Getter for the state transition matrix name.
     * @return name
     */
    public String getStmName() {
        return stmName;
    }

    /**
     * Getter for the maximum number of iterations.
     * @return maximum iterations
     */
    public int getMaxIter() {
        return maxIter;
    }

    /**
     * Setter for the maximum number of iterations.
     * @param maxIter maximum iterations
     */
    public void setMaxIter(final int maxIter) {
        this.maxIter = maxIter;
    }

    /**
     * Protected setter for the current iteration number.
     * @param currentIter iteration number
     */
    protected void setCurrentIter(final int currentIter) {
        this.currentIter = currentIter;
    }

    /**
     * Getter for the current iteration number.
     * @return iteration number
     */
    public int getCurrentIter() {
        return currentIter;
    }

    /**
     * Protected getter for the state transition matrices harvester.
     * @return harvester
     */
    protected MatricesHarvester getMatricesHarvester() {
        return matricesHarvester;
    }

    /**
     * Creates a linear system solver.
     * @param matrix matrix involved in system
     * @return solver
     */
    protected DecompositionSolver getDecompositionSolver(final RealMatrix matrix) {
        return new LUDecomposition(matrix, thresholdMatrixSolver).getSolver();
    }

    /**
     * Method applying differential correction on the guess (Newton-Raphson algorithm).
     *
     * @param propagator propagator to be used for differential corrections
     * @param guessInitialVelocity guess on the initial velocity vector
     * @return boundary velocities (null if not converged)
     */
    public LambertBoundaryVelocities solve(final AbstractPropagator propagator, final Vector3D guessInitialVelocity) {
        init(propagator, guessInitialVelocity);
        return iterate(propagator);
    }

    /**
     * Initialize propagator.
     *
     * @param propagator propagator to be used for differential corrections
     * @param guessInitialVelocity guess on the initial velocity vector
     */
    protected void init(final AbstractPropagator propagator, final Vector3D guessInitialVelocity) {
        propagator.clearMatricesComputation();
        final RealMatrix initialStm = MatrixUtils.createRealIdentityMatrix(propagator instanceof NumericalPropagator ? 7 : 6);
        propagator.resetInitialState(buildInitialState(propagator, propagator.getInitialState(), guessInitialVelocity));
        matricesHarvester = propagator.setupMatricesComputation(getStmName(), initialStm, null);
    }

    /**
     * Apply differential corrections until convergence (interrupted by maximum iterations count).
     * @param propagator propagator
     * @return boundary velocities (null if not converged)
     */
    protected LambertBoundaryVelocities iterate(final AbstractPropagator propagator) {
        Vector3D initialVelocity = extractVelocity(propagator.getInitialState());
        setCurrentIter(0);
        final AbsoluteDate terminalDate = getLambertBoundaryConditions().getTerminalDate();
        final Frame referenceFrame = getLambertBoundaryConditions().getReferenceFrame();
        for (int i = 0; i < getMaxIter(); i++) {
            final SpacecraftState initialState = propagator.getInitialState();
            final SpacecraftState terminalState = propagator.propagate(terminalDate);
            if (!terminalState.getDate().isEqualTo(terminalDate)) {
                // propagation must have been interrupted by event, exit
                break;
            }
            final Vector3D actualTerminalPosition = terminalState.getPosition(referenceFrame);
            final Vector3D positionDifference = actualTerminalPosition.subtract(getLambertBoundaryConditions()
                    .getTerminalPosition());
            if (hasConverged(positionDifference)) {
                return new LambertBoundaryVelocities(initialVelocity, extractVelocity(terminalState));
            }
            final RealMatrix stm = getCartesianStm(initialState, terminalState);
            final Vector3D correction = computeNewtonCorrection(positionDifference, stm);
            initialVelocity = correction.add(initialVelocity);
            propagator.resetInitialState(buildInitialState(propagator, initialState, initialVelocity));
            setCurrentIter(i + 1);
        }
        return null;  // no convergence
    }

    /**
     * Method creating an initial state.
     * @param propagator propagator
     * @param templateState template state
     * @param initialVelocity initial velocity to use
     * @return full propagation state for initialization
     */
    protected SpacecraftState buildInitialState(final AbstractPropagator propagator, final SpacecraftState templateState,
                                                final Vector3D initialVelocity) {
        final LambertBoundaryConditions boundaryValue = getLambertBoundaryConditions();
        final Frame referenceFrame = boundaryValue.getReferenceFrame();
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(boundaryValue.getInitialDate(),
                boundaryValue.getInitialPosition(), initialVelocity);
        final AttitudeProvider attitudeProvider = propagator.getAttitudeProvider();
        final SpacecraftState baseState;
        if (templateState.isOrbitDefined()) {
            final double mu = templateState.getOrbit().getMu();
            final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, mu);
            final Attitude attitude = attitudeProvider.getAttitude(orbit, orbit.getDate(), referenceFrame);
            baseState = new SpacecraftState(orbit, attitude);
        } else {
            final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(referenceFrame, pvCoordinates);
            final Attitude attitude = attitudeProvider.getAttitude(absolutePVCoordinates,
                    absolutePVCoordinates.getDate(), absolutePVCoordinates.getFrame());
            baseState = new SpacecraftState(absolutePVCoordinates, attitude);
        }
        final DataDictionary additionalData = new DataDictionary(templateState.getAdditionalDataValues());
        additionalData.remove(getStmName());
        return baseState.withMass(getInitialMass())
                .withAdditionalData(additionalData);
    }

    private Vector3D extractVelocity(final SpacecraftState state) {
        final PVCoordinates pvCoordinates = state.getPVCoordinates(getLambertBoundaryConditions().getReferenceFrame());
        return pvCoordinates.getVelocity();
    }

    /**
     * Method computing the Newton-Raphson correction.
     * @param positionDifference position residuals
     * @param cartesianStm Cartesian state transition matrix
     * @return correction
     */
    protected Vector3D computeNewtonCorrection(final Vector3D positionDifference, final RealMatrix cartesianStm) {
        final DecompositionSolver solver = getDecompositionSolver(cartesianStm.getSubMatrix(0, 2, 3, 5));
        final RealVector rhs = MatrixUtils.createRealVector(positionDifference.toArray()).mapMultiply(-1);
        final RealVector solution = solver.solve(rhs);
        return new Vector3D(solution.toArray());
    }

    /**
     * Computes the state transition matrix in Cartesian coordinates.
     * @param initialState initial state
     * @param terminalState terminal state
     * @return Cartesian STM
     */
    protected RealMatrix getCartesianStm(final SpacecraftState initialState,
                                         final SpacecraftState terminalState) {
        final RealMatrix stm = matricesHarvester.getStateTransitionMatrix(terminalState).getSubMatrix(0, 5, 0, 5);
        final OrbitType orbitType = matricesHarvester.getOrbitType();
        if (orbitType != null && orbitType != OrbitType.CARTESIAN) {
            final Orbit initialOrbit = orbitType.convertType(initialState.getOrbit());
            final RealMatrix initialJacobianCartesian = computeJacobianWrtCartesian(initialOrbit);
            final RealMatrix terminalJacobianOrbital = computeJacobianWrtOrbitalParameters(
                    terminalState.getOrbit());
            return terminalJacobianOrbital.multiply(stm).multiply(initialJacobianCartesian);
        } else {
            return stm;
        }
    }

    /**
     * Computes the Jacobian matrix w.r.t. Cartesian coordinates.
     * @param orbit orbit
     * @return Jacobian
     */
    private RealMatrix computeJacobianWrtCartesian(final Orbit orbit) {
        final double[][] jacobianOrbitalArray = create6x6Array();
        orbit.getJacobianWrtCartesian(matricesHarvester.getPositionAngleType(), jacobianOrbitalArray);
        return createRealMatrix(jacobianOrbitalArray);
    }

    /**
     * Computes the Jacobian matrix w.r.t. orbital elements.
     * @param orbit orbit
     * @return Jacobian
     */
    private RealMatrix computeJacobianWrtOrbitalParameters(final Orbit orbit) {
        final double[][] jacobianCartesianArray = create6x6Array();
        orbit.getJacobianWrtParameters(matricesHarvester.getPositionAngleType(), jacobianCartesianArray);
        return createRealMatrix(jacobianCartesianArray);
    }

    /**
     * Creates a matrix from an array.
     * @param data array
     * @return matrix
     */
    private RealMatrix createRealMatrix(final double[][] data) {
        return MatrixUtils.createRealMatrix(data);
    }

    /**
     * Creates a 6x6 array.
     * @return array
     */
    private static double[][] create6x6Array() {
        return new double[6][6];
    }

    /**
     * Checks convergence.
     * @param positionDifference position residuals
     * @return convergence flag
     */
    protected boolean hasConverged(final Vector3D positionDifference) {
        return positionDifference.getNormInf() <= getPositionTolerance();
    }
}
