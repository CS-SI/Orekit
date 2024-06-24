/* Copyright 2022-2024 Romain Serra
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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Utility class to predict position and velocity under Keplerian motion, using lightweight routines based on Cartesian
 * coordinates. Computations do not require a reference frame or an epoch.
 *
 * @author Andrew Goetz
 * @author Romain Serra
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
        final double a = r / (2 - r * velocity.getNormSq() / mu);
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
        // preliminary computation
        final Vector3D pvM     = position.crossProduct(velocity);
        final double muA       = mu * a;

        // compute mean anomaly
        final double eSE    = position.dotProduct(velocity) / FastMath.sqrt(muA);
        final double eCE    = 1. - r / a;
        final double E0     = FastMath.atan2(eSE, eCE);
        final double M0     = E0 - eSE;

        final double e         = FastMath.sqrt(eCE * eCE + eSE * eSE);
        final double sqrt      = FastMath.sqrt((1 + e) / (1 - e));

        // find canonical 2D frame with p pointing to perigee
        final double v0     = 2 * FastMath.atan(sqrt * FastMath.tan(E0 / 2));
        final Rotation rotation = new Rotation(pvM, v0, RotationConvention.FRAME_TRANSFORM);
        final Vector3D p    = rotation.applyTo(position).normalize();
        final Vector3D q    = pvM.crossProduct(p).normalize();

        // compute shifted eccentric anomaly
        final double sqrtRatio = FastMath.sqrt(mu / a);
        final double meanMotion = sqrtRatio / a;
        final double M1     = M0 + meanMotion * dt;
        final double E1     = KeplerianAnomalyUtility.ellipticMeanToEccentric(e, M1);

        // compute shifted in-plane Cartesian coordinates
        final SinCos scE    = FastMath.sinCos(E1);
        final double cE     = scE.cos();
        final double sE     = scE.sin();
        final double sE2m1  = FastMath.sqrt((1 - e) * (1 + e));

        // coordinates of position and velocity in the orbital plane
        final double x      = a * (cE - e);
        final double y      = a * sE2m1 * sE;
        final double factor = sqrtRatio / (1 - e * cE);
        final double xDot   = -factor * sE;
        final double yDot   =  factor * sE2m1 * cE;

        final Vector3D predictedPosition = new Vector3D(x, p, y, q);
        final Vector3D predictedVelocity = new Vector3D(xDot, p, yDot, q);
        return new PVCoordinates(predictedPosition, predictedVelocity);
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics, in the case of a hyperbolic trajectory.
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
        final Vector3D pvM   = position.crossProduct(velocity);
        final double muA     = mu * a;
        final double e       = FastMath.sqrt(1 - pvM.getNormSq() / muA);
        final double sqrt    = FastMath.sqrt((e + 1) / (e - 1));

        // compute mean anomaly
        final double eSH     = position.dotProduct(velocity) / FastMath.sqrt(-muA);
        final double eCH     = 1. - r / a;
        final double H0      = FastMath.log((eCH + eSH) / (eCH - eSH)) / 2;
        final double M0      = e * FastMath.sinh(H0) - H0;

        // find canonical 2D frame with p pointing to perigee
        final double v0      = 2 * FastMath.atan(sqrt * FastMath.tanh(H0 / 2));
        final Rotation rotation = new Rotation(pvM, v0, RotationConvention.FRAME_TRANSFORM);
        final Vector3D p     = rotation.applyTo(position).normalize();
        final Vector3D q     = pvM.crossProduct(p).normalize();

        // compute shifted eccentric anomaly
        final double absA = FastMath.abs(a);
        final double sqrtRatio = FastMath.sqrt(mu / absA);
        final double meanMotion = sqrtRatio / absA;
        final double M1      = M0 + meanMotion * dt;
        final double H1      = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e, M1);

        // compute shifted in-plane Cartesian coordinates
        final double cH     = FastMath.cosh(H1);
        final double sH     = FastMath.sinh(H1);
        final double sE2m1  = FastMath.sqrt((e - 1) * (e + 1));

        // coordinates of position and velocity in the orbital plane
        final double x      = a * (cH - e);
        final double y      = -a * sE2m1 * sH;
        final double factor = sqrtRatio / (e * cH - 1);
        final double xDot   = -factor * sH;
        final double yDot   =  factor * sE2m1 * cH;

        final Vector3D predictedPosition = new Vector3D(x, p, y, q);
        final Vector3D predictedVelocity = new Vector3D(xDot, p, yDot, q);
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
        final T a = r.divide(r.multiply(velocity.getNormSq()).divide(mu).negate().add(2));
        if (a.getReal() >= 0.) {
            return predictPVElliptic(dt, position, velocity, mu, a, r);
        } else {
            return predictPVHyperbolic(dt, position, velocity, mu, a, r);
        }
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics, in the case of an elliptic trajectory.
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
        // preliminary computation0
        final FieldVector3D<T>      pvM = position.crossProduct(velocity);
        final T muA                     = mu.multiply(a);

        // compute mean anomaly
        final T eSE              = position.dotProduct(velocity).divide(muA.sqrt());
        final T eCE              = r.divide(a).negate().add(1);
        final T E0               = FastMath.atan2(eSE, eCE);
        final T M0               = E0.subtract(eSE);

        final T e                       = eCE.square().add(eSE.square()).sqrt();
        final T ePlusOne = e.add(1);
        final T oneMinusE = e.negate().add(1);
        final T sqrt                    = ePlusOne.divide(oneMinusE).sqrt();

        // find canonical 2D frame with p pointing to perigee
        final T v0               = sqrt.multiply((E0.divide(2)).tan()).atan().multiply(2);
        final FieldRotation<T> rotation = new FieldRotation<>(pvM, v0, RotationConvention.FRAME_TRANSFORM);
        final FieldVector3D<T> p = rotation.applyTo(position).normalize();
        final FieldVector3D<T> q = pvM.crossProduct(p).normalize();

        // compute shifted eccentric anomaly
        final T sqrtRatio = (a.reciprocal().multiply(mu)).sqrt();
        final T meanMotion = sqrtRatio.divide(a);
        final T M1               = M0.add(meanMotion.multiply(dt));
        final T E1               = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e, M1);

        // compute shifted in-plane Cartesian coordinates
        final FieldSinCos<T> scE = FastMath.sinCos(E1);
        final T               cE = scE.cos();
        final T               sE = scE.sin();
        final T            sE2m1 = oneMinusE.multiply(ePlusOne).sqrt();

        // coordinates of position and velocity in the orbital plane
        final T x        = a.multiply(cE.subtract(e));
        final T y        = a.multiply(sE2m1).multiply(sE);
        final T factor   = sqrtRatio.divide(e.multiply(cE).negate().add(1));
        final T xDot     = factor.multiply(sE).negate();
        final T yDot     = factor.multiply(sE2m1).multiply(cE);

        final FieldVector3D<T> predictedPosition = new FieldVector3D<>(x, p, y, q);
        final FieldVector3D<T> predictedVelocity = new FieldVector3D<>(xDot, p, yDot, q);
        return new FieldPVCoordinates<>(predictedPosition, predictedVelocity);
    }

    /**
     * Method to propagate position and velocity according to Keplerian dynamics, in the case of a hyperbolic trajectory.
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
        final FieldVector3D<T> pvM   = position.crossProduct(velocity);
        final T muA     = a.multiply(mu);
        final T e       = a.newInstance(1.).subtract(pvM.getNormSq().divide(muA)).sqrt();
        final T ePlusOne = e.add(1);
        final T eMinusOne = e.subtract(1);
        final T sqrt    = ePlusOne.divide(eMinusOne).sqrt();

        // compute mean anomaly
        final T eSH     = position.dotProduct(velocity).divide(muA.negate().sqrt());
        final T eCH     = r.divide(a).negate().add(1);
        final T H0      = eCH.add(eSH).divide(eCH.subtract(eSH)).log().divide(2);
        final T M0      = e.multiply(H0.sinh()).subtract(H0);

        // find canonical 2D frame with p pointing to perigee
        final T v0      = sqrt.multiply(H0.divide(2).tanh()).atan().multiply(2);
        final FieldRotation<T> rotation = new FieldRotation<>(pvM, v0, RotationConvention.FRAME_TRANSFORM);
        final FieldVector3D<T> p     = rotation.applyTo(position).normalize();
        final FieldVector3D<T> q     = pvM.crossProduct(p).normalize();

        // compute shifted eccentric anomaly
        final T sqrtRatio = (a.reciprocal().negate().multiply(mu)).sqrt();
        final T meanMotion = sqrtRatio.divide(a).negate();
        final T M1      = M0.add(meanMotion.multiply(dt));
        final T H1      = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e, M1);

        // compute shifted in-plane Cartesian coordinates
        final T cH     = H1.cosh();
        final T sH     = H1.sinh();
        final T sE2m1  = eMinusOne.multiply(ePlusOne).sqrt();

        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(cH.subtract(e));
        final T y      = a.negate().multiply(sE2m1).multiply(sH);
        final T factor = sqrtRatio.divide(e.multiply(cH).subtract(1));
        final T xDot   = factor.negate().multiply(sH);
        final T yDot   =  factor.multiply(sE2m1).multiply(cH);

        final FieldVector3D<T> predictedPosition = new FieldVector3D<>(x, p, y, q);
        final FieldVector3D<T> predictedVelocity = new FieldVector3D<>(xDot, p, yDot, q);
        return new FieldPVCoordinates<>(predictedPosition, predictedVelocity);
    }
}
