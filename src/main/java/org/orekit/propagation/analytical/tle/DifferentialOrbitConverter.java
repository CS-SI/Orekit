/* Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;

/** Orbit converter for Two-Lines Elements using differential algorithm.
 * @author Rocca
 * @since 6.0
 */
public class DifferentialOrbitConverter extends AbstractTLEFitter {

    /** Maximum number of iterations for fitting. */
    private final int maxIterations;

    /** Simple constructor.
     * @param maxIterations maximum number of iterations for fitting
     * @param satelliteNumber satellite number
     * @param classification classification (U for unclassified)
     * @param launchYear launch year (all digits)
     * @param launchNumber launch number
     * @param launchPiece launch piece
     * @param elementNumber element number
     * @param revolutionNumberAtEpoch revolution number at epoch
     */
    public DifferentialOrbitConverter(final int maxIterations,
                                      final int satelliteNumber, final char classification,
                                      final int launchYear, final int launchNumber, final String launchPiece,
                                      final int elementNumber, final int revolutionNumberAtEpoch) {
        super(satelliteNumber, classification,
              launchYear, launchNumber, launchPiece, elementNumber, revolutionNumberAtEpoch);
        this.maxIterations = maxIterations;
    }

    /** {@inheritDoc} */
    protected double[] fit(final double[] initial) throws OrekitException, MaxCountExceededException {

        final double[] w = getWeight();
        for (int i = 0; i < w.length; ++i) {
            w[i] = FastMath.sqrt(w[i]);
        }

        final MultivariateMatrixFunction jacobian = getPVFunction().jacobian();
        final double[] result = initial.clone();

        double previousRMS = Double.NaN;
        for (int iterations = 0; iterations < maxIterations; ++iterations) {

            final RealMatrix A = new Array2DRowRealMatrix(jacobian.value(result));
            for (int i = 0; i < A.getRowDimension(); i++) {
                for (int j = 0; j < A.getColumnDimension(); j++) {
                    A.multiplyEntry(i, j, w[i]);
                }
            }

            final double[] residuals = getResiduals(result);
            final RealVector y = new ArrayRealVector(residuals.length);
            for (int i = 0; i < y.getDimension(); i++) {
                y.setEntry(i, residuals[i] * w[i]);
            }
            final RealVector dx = new  QRDecomposition(A).getSolver().solve(y);
            for (int i = 0; i < result.length; i++) {
                result[i] = result[i] + dx.getEntry(i);
            }

            final double rms = getRMS(result);
            if (iterations > 0 && FastMath.abs(rms - previousRMS) <= getPositionTolerance()) {
                return result;
            }
            previousRMS = rms;

        }

        throw new MaxCountExceededException(maxIterations);

    }

}
