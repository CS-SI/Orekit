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
package org.orekit.estimation.iod;

import org.hipparchus.analysis.solvers.LaguerreSolver;
import org.hipparchus.complex.Complex;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * Gauss angles-only Initial Orbit Determination (IOD) algorithm.
 * <p>
 * The algorithm works best when the separation between observation is less than about 60°.
 * The method performs remarkably well when the data is separated by 10° or less.
 *
 * An orbit is determined from three lines of sight w.r.t. their respective observers
 * inertial positions vectors.
 * </p><p>
 * References:
 *   Vallado, D., Fundamentals of Astrodynamics and Applications
 *   Curtis, Orbital Mechanics for Engineering Students
 * </p>
 * @author Julien Asquier
 * @since 12.0
 */
public class IodGauss {

    /** Gravitational constant. */
    private final double mu;

    /**
     * Constructor.
     *
     * @param mu gravitational constant
     */
    public IodGauss(final double mu) {
        this.mu = mu;
    }

    /**
     * Estimate and orbit based on Gauss Intial Orbit Determination method.
     *
     * @param outputFrame inertial frame for observer coordinates and orbit estimate
     * @param azEl1 first angular observation
     * @param azEl2 second angular observation
     * @param azEl3 third angular observation
     * @return an estimate of the orbit at the central date or null if
     *         no estimate is possible with the given data
     */
    public Orbit estimate(final Frame outputFrame, final AngularAzEl azEl1,
                          final AngularAzEl azEl2, final AngularAzEl azEl3) {
        return estimate(outputFrame,
                        azEl1.getGroundStationPosition(outputFrame), azEl1.getDate(), azEl1.getObservedLineOfSight(outputFrame),
                        azEl2.getGroundStationPosition(outputFrame), azEl2.getDate(), azEl2.getObservedLineOfSight(outputFrame),
                        azEl3.getGroundStationPosition(outputFrame), azEl3.getDate(), azEl3.getObservedLineOfSight(outputFrame));
    }

    /**
     * Estimate and orbit based on Gauss Intial Orbit Determination method.
     *
     * @param outputFrame inertial frame for observer coordinates and orbit estimate
     * @param raDec1 first angular observation
     * @param raDec2 second angular observation
     * @param raDec3 third angular observation
     * @return an estimate of the orbit at the central date or null if
     *         no estimate is possible with the given data
     */
    public Orbit estimate(final Frame outputFrame, final AngularRaDec raDec1,
                          final AngularRaDec raDec2, final AngularRaDec raDec3) {
        return estimate(outputFrame,
                        raDec1.getGroundStationPosition(outputFrame), raDec1.getDate(), raDec1.getObservedLineOfSight(outputFrame),
                        raDec2.getGroundStationPosition(outputFrame), raDec2.getDate(), raDec2.getObservedLineOfSight(outputFrame),
                        raDec3.getGroundStationPosition(outputFrame), raDec3.getDate(), raDec3.getObservedLineOfSight(outputFrame));
    }

    /**
     * Estimate and orbit based on Gauss Intial Orbit Determination method.
     *
     * @param outputFrame inertial frame for observer coordinates and orbit estimate
     * @param obsP1 observer position at obsDate1
     * @param obsDate1 date of the 1st observation
     * @param los1 line of sight unit vector at obsDate1
     * @param obsP2 observer position at obsDate2
     * @param obsDate2 date of the 2nd observation
     * @param los2 line of sight unit vector at obsDate2
     * @param obsP3 observer position at obsDate3
     * @param obsDate3 date of the 3rd observation
     * @param los3 line of sight unit vector at obsDate3
     * @return an estimate of the orbit at the central date obsDate2 or null if
     *         no estimate is possible with the given data
     */
    public Orbit estimate(final Frame outputFrame,
                          final Vector3D obsP1, final AbsoluteDate obsDate1, final Vector3D los1,
                          final Vector3D obsP2, final AbsoluteDate obsDate2, final Vector3D los2,
                          final Vector3D obsP3, final AbsoluteDate obsDate3, final Vector3D los3) {

        // Getting the difference of time between 1st and 3rd observation with 2nd observation
        final double tau1 = obsDate1.getDate().durationFrom(obsDate2.getDate());
        final double tau3 = obsDate3.getDate().durationFrom(obsDate2.getDate());

        final double diffTau3Tau1 = tau3 - tau1;

        // mathematical coefficients, see Vallado 7.3.2
        final double a1  = tau3 / diffTau3Tau1;
        final double a3  = -tau1 / diffTau3Tau1;
        final double a1u = tau3 * ((diffTau3Tau1 * diffTau3Tau1) - tau3 * tau3) / (6. * diffTau3Tau1);
        final double a3u = -tau1 * ((diffTau3Tau1 * diffTau3Tau1) - tau1 * tau1) / (6. * diffTau3Tau1);

        // Creating the line of Sight Matrix and inverting it
        final RealMatrix losMatrix = new Array2DRowRealMatrix(3, 3);
        losMatrix.setColumn(0, los1.toArray());
        losMatrix.setColumn(1, los2.toArray());
        losMatrix.setColumn(2, los3.toArray());

        final RealMatrix invertedLosMatrix = new LUDecomposition(losMatrix).getSolver().getInverse();

        // Creating the position of observations matrix
        final RealMatrix rSite = new Array2DRowRealMatrix(3, 3);
        rSite.setColumn(0, obsP1.toArray());
        rSite.setColumn(1, obsP2.toArray());
        rSite.setColumn(2, obsP3.toArray());

        // mathematical matrix and coefficients, see Vallado 7.3.2
        final RealMatrix m = invertedLosMatrix.multiply(rSite);

        final double d1 = m.getEntry(1, 0) * a1 - m.getEntry(1, 1) + m.getEntry(1, 2) * a3;
        final double d2 = m.getEntry(1, 0) * a1u + m.getEntry(1, 2) * a3u;
        final double C  = los2.dotProduct(obsP2);

        // norm of the position of the second observation
        final double r2Norm = obsP2.getNorm();

        // Coefficients of the 8th order polynomial we need to solve to determine "r"
        final double[] coeff = new double[] { -mu * mu * d2 * d2, 0., 0., -2. * mu * (C * d2 + d1 * d2), 0.,
                                              0., -(d1 * d1 + 2. * C * d1 + r2Norm * r2Norm), 0., 1.0 };
        final LaguerreSolver solver = new LaguerreSolver(1E-10,
                                                         1E-10, 1E-10);
        final Complex[] roots = solver.solveAllComplex(coeff, 5. * r2Norm);

        // Looking for the first adequate root of the equation (7-16) of Vallado
        double r2Mag = 0.0;
        for (final Complex root : roots) {
            if (root.getReal() > r2Mag &&
                    FastMath.abs(root.getImaginary()) < solver.getAbsoluteAccuracy()) {
                r2Mag = root.getReal();
            }
        }
        if (r2Mag == 0.0) {
            return null;
        }

        // mathematical matrix and coefficients, see Vallado 7.3.2
        final double u = mu / (r2Mag * r2Mag * r2Mag);

        final double c1 = -(-a1 - a1u * u);
        final double c2 = -1;
        final double c3 = -(-a3 - a3u * u);

        final RealMatrix cCoeffMatrix = new Array2DRowRealMatrix(3, 1);
        cCoeffMatrix.setEntry(0, 0, -c1);
        cCoeffMatrix.setEntry(1, 0, -c2);
        cCoeffMatrix.setEntry(2, 0, -c3);

        final RealMatrix B = m.multiply(cCoeffMatrix.scalarMultiply(1));

        final RealMatrix A = new Array2DRowRealMatrix(3, 3);
        A.setEntry(0, 0, c1);
        A.setEntry(1, 1, c2);
        A.setEntry(2, 2, c3);

        // Slant ranges matrix
        final RealMatrix slantRanges = new LUDecomposition(A).getSolver().solve(B);

        // Position Matrix of the satellite corresponding to the 1st, 2nd and 3rd observation
        final RealMatrix posMatrix = new Array2DRowRealMatrix(3, 3);
        for (int i = 0; i <= 2; i++) {
            final RealVector position = losMatrix.getColumnVector(i).
                                                  mapMultiply(slantRanges.getEntry(i, 0)).add(rSite.getColumnVector(i));
            posMatrix.setRowVector(i, position);
        }
        // At this point, the proper Gauss Initial Orbit determination is ending because we have the 3 positions of the
        // satellite from the 3 observations. However, we could also calculate the velocity using the next f and g
        // coefficients, see Vallado 7.3.2 and Vallado 2.3.1 for more details
        final double pos2Norm     = posMatrix.getRowVector(1).getNorm();
        final double pos2NormCubed = pos2Norm * pos2Norm * pos2Norm;

        // mathematical matrix and coefficients, see Curtis algorithms 5.5 and 5.6. It is still the IOD GAUSS
        final double f1 = 1. - (0.5 * mu * tau1 * tau1 / pos2NormCubed);
        final double f3 = 1. - (0.5 * mu * tau3 * tau3 / pos2NormCubed);
        final double g1 = tau1 - ((1. / 6.) * mu * tau1 * tau1 * tau1 / pos2NormCubed);
        final double g3 = tau3 - ((1. / 6.) * mu * tau3 * tau3 * tau3 / pos2NormCubed);

        final double v2EquationCoeff = 1. / (f1 * g3 - f3 * g1);
        // velocity at the central position of the satellite, corresponding to the second observation
        final RealVector v2 = (posMatrix.getRowVector(0).mapMultiply(-f3).add(
                posMatrix.getRowVector(2).mapMultiply(f1))).mapMultiply(v2EquationCoeff);

        // position at the central position of the satellite, corresponding to the second observation
        final RealVector p2 = posMatrix.getRowVector(1);

        // We can finally build the Orekit Object, PVCoordinates and Orbit from p2 and v2
        final Vector3D p2Vector3D = new Vector3D(p2.toArray());
        final Vector3D v2Vector3D = new Vector3D(v2.toArray());
        return new CartesianOrbit(new PVCoordinates(p2Vector3D, v2Vector3D), outputFrame, obsDate2, mu);
    }

}
