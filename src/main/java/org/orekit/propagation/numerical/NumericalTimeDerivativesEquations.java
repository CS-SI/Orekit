package org.orekit.propagation.numerical;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;

import java.util.Arrays;
import java.util.List;

/**
 * .
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 13.1
 */
class NumericalTimeDerivativesEquations implements TimeDerivativesEquations {

    /** Derivatives array. */
    protected final double[] yDot;

    private final OrbitType orbitType;
    private final PositionAngleType positionAngleType;
    private final List<ForceModel> forceModels;

    /** Jacobian of the orbital parameters with respect to the Cartesian parameters. */
    private double[][] coordinatesJacobian;

    /** Current state. */
    private SpacecraftState currentState;

    NumericalTimeDerivativesEquations(final OrbitType orbitType, final PositionAngleType positionAngleType,
                                      final List<ForceModel> forceModels) {
        this.orbitType = orbitType;
        this.positionAngleType = positionAngleType;
        this.forceModels = forceModels;
        this.yDot     = new double[7];
        this.coordinatesJacobian = new double[6][6];
        // default value for Jacobian is identity
        for (int i = 0; i < coordinatesJacobian.length; ++i) {
            Arrays.fill(coordinatesJacobian[i], 0.0);
            coordinatesJacobian[i][i] = 1.0;
        }
    }

    List<ForceModel> getForceModels() {
        return forceModels;
    }

    public void setCoordinatesJacobian(double[][] coordinatesJacobian) {
        this.coordinatesJacobian = coordinatesJacobian;
    }

    public void setCurrentState(SpacecraftState currentState) {
        this.currentState = currentState;
    }

    double[] computeTimeDerivatives(final SpacecraftState state) {
        currentState = state;
        Arrays.fill(yDot, 0.0);

        // compute the contributions of all perturbing forces,
        // using the Kepler contribution at the end since
        // NewtonianAttraction is always the last instance in the list
        for (final ForceModel forceModel : forceModels) {
            forceModel.addContribution(state, this);
        }

        if (orbitType == null) {
            // position derivative is velocity, and was not added above in the force models
            // (it is added when orbit type is non-null because NewtonianAttraction considers it)
            final Vector3D velocity = state.getPVCoordinates().getVelocity();
            yDot[0] += velocity.getX();
            yDot[1] += velocity.getY();
            yDot[2] += velocity.getZ();
        }

        return yDot.clone();
    }

    /** {@inheritDoc} */
    @Override
    public void addKeplerContribution(final double mu) {
        if (orbitType == null) {
            // if mu is neither 0 nor NaN, we want to include Newtonian acceleration
            if (mu > 0) {
                // velocity derivative is Newtonian acceleration
                final Vector3D position = currentState.getPosition();
                final double r2         = position.getNormSq();
                final double coeff      = -mu / (r2 * FastMath.sqrt(r2));
                yDot[3] += coeff * position.getX();
                yDot[4] += coeff * position.getY();
                yDot[5] += coeff * position.getZ();
            }

        } else {
            // propagation uses regular orbits
            orbitType.convertType(currentState.getOrbit()).addKeplerContribution(positionAngleType, mu, yDot);
        }
    }

    /** {@inheritDoc} */
    public void addNonKeplerianAcceleration(final Vector3D gamma) {
        for (int i = 0; i < 6; ++i) {
            final double[] jRow = coordinatesJacobian[i];
            yDot[i] += jRow[3] * gamma.getX() + jRow[4] * gamma.getY() + jRow[5] * gamma.getZ();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addMassDerivative(final double q) {
        if (q > 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
        }
        yDot[6] += q;
    }
}
