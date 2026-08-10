/* Copyright 2022-2026 Romain Serra
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
package org.orekit.orbits;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.FieldSinhCosh;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Utility class to predict position and velocity under Keplerian motion, using lightweight routines based on Cartesian
 * coordinates. Computations do not require a reference frame or an epoch.
 *
 * @author Andrew Goetz
 * @author Romain Serra
 * @author Alberto Fossa'
 * @see org.orekit.propagation.analytical.KeplerianPropagator
 * @see org.orekit.propagation.analytical.FieldKeplerianPropagator
 * @see CartesianOrbit
 * @see FieldCartesianOrbit
 * @since 12.1
 */
public class KeplerianMotionCartesianUtility {

    private KeplerianMotionCartesianUtility() {
        // utility class
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics.
     * For long time of flights, it is preferable to use {@link org.orekit.propagation.analytical.KeplerianPropagator}.
     * @param dt time of flight
     * @param position initial position vector
     * @param velocity initial velocity vector
     * @param mu central body gravitational parameter
     * @return predicted position-velocity
     */
    public static PVCoordinates predictPositionVelocity(final double dt, final Vector3D position, final Vector3D velocity,
                                                        final double mu) {
        final double r = position.getNorm();
        final double a = r / (2 - r * velocity.getNorm2Sq() / mu);
        if (a >= 0.) {
            return predictPVElliptic(dt, position, velocity, mu, a, r);
        } else {
            return predictPVHyperbolic(dt, position, velocity, mu, a, r);
        }
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics, in the case of an elliptic trajectory.
     * @param dt time of flight
     * @param position initial position vector
     * @param velocity initial velocity vector
     * @param mu central body gravitational parameter
     * @param a semi-major axis
     * @param r initial radius
     * @return predicted position-velocity
     */
    private static PVCoordinates predictPVElliptic(final double dt, final Vector3D position, final Vector3D velocity,
                                                   final double mu, final double a, final double r) {

        // preliminary computations
        final double sigma0 = position.dotProduct(velocity) / FastMath.sqrt(mu);
        final double c0 = 1.0 - r / a;
        final double s0 = sigma0 / FastMath.sqrt(a);

        // change in elliptic mean and eccentric anomalies
        final double deltaM = FastMath.sqrt(mu / FastMath.pow(a, 3)) * dt;
        final double deltaE = KeplerianAnomalyUtility.ellipticMeanToEccentricDifference(s0, c0, deltaM);

        final double sE = FastMath.sin(deltaE);
        final double cE = FastMath.cos(deltaE);

        // Lagrange f and g coefficients
        final double f = 1.0 - a / r * (1.0 - cE);
        final double g = a * sigma0 / FastMath.sqrt(mu) * (1.0 - cE) + r * FastMath.sqrt(a / mu) * sE;

        // predicted position
        final Vector3D predictedPosition = new Vector3D(f, position, g, velocity);
        final double predictedR = predictedPosition.getNorm();

        // time derivatives of Lagrange f and g coefficients
        final double fDot = -FastMath.sqrt(mu * a) / (predictedR * r) * sE;
        final double gDot = 1.0 - a / predictedR * (1.0 - cE);

        // predicted velocity
        final Vector3D predictedVelocity = new Vector3D(fDot, position, gDot, velocity);

        return new PVCoordinates(predictedPosition, predictedVelocity);
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics, in the case of a hyperbolic trajectory.
     * <p>This method is described in Battin (1999), p. 170.</p>
     * @param dt time of flight
     * @param position initial position vector
     * @param velocity initial velocity vector
     * @param mu central body gravitational parameter
     * @param a semi-major axis
     * @param r initial radius
     * @return predicted position-velocity
     */
    private static PVCoordinates predictPVHyperbolic(final double dt, final Vector3D position, final Vector3D velocity,
                                                     final double mu, final double a, final double r) {
        // preliminary computations
        final double sigma0 = position.dotProduct(velocity) / FastMath.sqrt(mu);
        final double c0 = 1.0 - r / a;
        final double s0 = sigma0 / FastMath.sqrt(-a);

        // change in hyperbolic mean and eccentric anomalies
        final double deltaN = FastMath.sqrt(mu / FastMath.pow(-a, 3)) * dt;
        final double deltaH = KeplerianAnomalyUtility.hyperbolicMeanToEccentricDifference(s0, c0, deltaN);

        final double shH = FastMath.sinh(deltaH);
        final double chH = FastMath.cosh(deltaH);

        // Lagrange f and g coefficients
        final double f = 1.0 - a / r * (1.0 - chH);
        final double g = a * sigma0 / FastMath.sqrt(mu) * (1.0 - chH) + r * FastMath.sqrt((-a) / mu) * shH;

        // predicted position
        final Vector3D predictedPosition = new Vector3D(f, position, g, velocity);
        final double predictedR = predictedPosition.getNorm();

        // time derivatives of Lagrange f and g coefficients
        final double fDot = -FastMath.sqrt(mu * (-a)) / (predictedR * r) * shH;
        final double gDot = 1.0 - a / predictedR * (1.0 - chH);

        // predicted velocity
        final Vector3D predictedVelocity = new Vector3D(fDot, position, gDot, velocity);

        return new PVCoordinates(predictedPosition, predictedVelocity);
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics.
     * For long time of flights, it is preferable to use {@link org.orekit.propagation.analytical.KeplerianPropagator}.
     * @param <T> field type
     * @param dt time of flight
     * @param position initial position vector
     * @param velocity initial velocity vector
     * @param mu central body gravitational parameter
     * @return predicted position-velocity
     */
    public static <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> predictPositionVelocity(final T dt,
                                                                                                    final FieldVector3D<T> position,
                                                                                                    final FieldVector3D<T> velocity,
                                                                                                    final T mu) {
        final T r = position.getNorm();
        final T a = r.divide(r.multiply(velocity.getNorm2Sq()).divide(mu).negate().add(2));
        if (a.getReal() >= 0.) {
            return predictPVElliptic(dt, position, velocity, mu, a, r);
        } else {
            return predictPVHyperbolic(dt, position, velocity, mu, a, r);
        }
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics, in the case of an elliptic trajectory.
     * <p>This method is described in Battin (1999), pp. 162-164.</p>
     * @param <T> field type
     * @param dt time of flight
     * @param position initial position vector
     * @param velocity initial velocity vector
     * @param mu central body gravitational parameter
     * @param a semi-major axis
     * @param r initial radius
     * @return predicted position-velocity
     */
    private static <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> predictPVElliptic(final T dt,
                                                                                               final FieldVector3D<T> position,
                                                                                               final FieldVector3D<T> velocity,
                                                                                               final T mu, final T a,
                                                                                               final T r) {

        // preliminary computations
        final T sigma0 = position.dotProduct(velocity).divide(mu.sqrt());
        final T c0 = r.divide(a).negate().add(1.0);
        final T s0 = sigma0.divide(a.sqrt());

        // change in elliptic mean and eccentric anomalies
        final T deltaM = mu.divide(a.multiply(a).multiply(a)).sqrt().multiply(dt);
        final T deltaE = FieldKeplerianAnomalyUtility.ellipticMeanToEccentricDifference(s0, c0, deltaM);

        final FieldSinCos<T> scE = deltaE.sinCos();
        final T oneMinusCE = scE.cos().negate().add(1.0);

        // Lagrange f and g coefficients
        final T f = a.divide(r).multiply(oneMinusCE).negate().add(1.0);
        final T g = a.multiply(sigma0).divide(mu.sqrt()).multiply(oneMinusCE)
                .add(r.multiply(a.divide(mu).sqrt()).multiply(scE.sin()));

        // predicted position
        final FieldVector3D<T> predictedPosition = new FieldVector3D<>(f, position, g, velocity);
        final T predictedR = predictedPosition.getNorm();

        // time derivatives of Lagrange f and g coefficients
        final T fDot = mu.multiply(a).sqrt().divide(predictedR.multiply(r)).multiply(scE.sin()).negate();
        final T gDot = a.divide(predictedR).multiply(oneMinusCE).negate().add(1.0);

        // predicted velocity
        final FieldVector3D<T> predictedVelocity = new FieldVector3D<>(fDot, position, gDot, velocity);

        return new FieldPVCoordinates<>(predictedPosition, predictedVelocity);
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics, in the case of a hyperbolic trajectory.
     * <p>This method is described in Battin (1999), p. 170.</p>
     * @param <T> field type
     * @param dt time of flight
     * @param position initial position vector
     * @param velocity initial velocity vector
     * @param mu central body gravitational parameter
     * @param a semi-major axis
     * @param r initial radius
     * @return predicted position-velocity
     */
    private static <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> predictPVHyperbolic(final T dt,
                                                                                                 final FieldVector3D<T> position,
                                                                                                 final FieldVector3D<T> velocity,
                                                                                                 final T mu, final T a,
                                                                                                 final T r) {
        // preliminary computations
        final T minusA = a.negate();
        final T sigma0 = position.dotProduct(velocity).divide(mu.sqrt());
        final T c0 = r.divide(a).negate().add(1.0);
        final T s0 = sigma0.divide(minusA.sqrt());

        // change in hyperbolic mean and eccentric anomalies
        final T deltaN = mu.divide(minusA.multiply(minusA).multiply(minusA)).sqrt().multiply(dt);
        final T deltaH = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentricDifference(s0, c0, deltaN);

        final FieldSinhCosh<T> schH = deltaH.sinhCosh();
        final T oneMinusChH = schH.cosh().negate().add(1.0);

        // Lagrange f and g coefficients
        final T f = a.divide(r).multiply(oneMinusChH).negate().add(1.0);
        final T g = a.multiply(sigma0).divide(mu.sqrt()).multiply(oneMinusChH)
                .add(r.multiply(minusA.divide(mu).sqrt()).multiply(schH.sinh()));

        // predicted position
        final FieldVector3D<T> predictedPosition = new FieldVector3D<>(f, position, g, velocity);
        final T predictedR = predictedPosition.getNorm();

        // time derivatives of Lagrange f and g coefficients
        final T fDot = mu.multiply(minusA).sqrt().divide(predictedR.multiply(r)).multiply(schH.sinh()).negate();
        final T gDot = a.divide(predictedR).multiply(oneMinusChH).negate().add(1.0);

        // predicted velocity
        final FieldVector3D<T> predictedVelocity = new FieldVector3D<>(fDot, position, gDot, velocity);

        return new FieldPVCoordinates<>(predictedPosition, predictedVelocity);
    }
}
