/* Copyright 2002-2020 CS GROUP
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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.ParameterDriver;

/** Class for solving integer ambiguity problems.
 * @see LambdaMethod
 * @author Luc Maisonobe
 * @since 10.0
 */
public class AmbiguitySolver {

    /** Drivers for ambiguity drivers. */
    private final List<ParameterDriver> ambiguityDrivers;

    /** Solver for the underlying Integer Least Square problem. */
    private final IntegerLeastSquareSolver solver;

    /** Acceptance test to use. */
    private final AmbiguityAcceptance acceptance;

    /** Simple constructor.
     * @param ambiguityDrivers drivers for ambiguity parameters
     * @param solver solver for the underlying Integer Least Square problem
     * @param acceptance acceptance test to use
     * @see LambdaMethod
     */
    public AmbiguitySolver(final List<ParameterDriver> ambiguityDrivers,
                           final IntegerLeastSquareSolver solver,
                           final AmbiguityAcceptance acceptance) {
        this.ambiguityDrivers = ambiguityDrivers;
        this.solver           = solver;
        this.acceptance       = acceptance;
    }

    /** Get all the ambiguity parameters drivers.
     * @return all ambiguity parameters drivers
     */
    public List<ParameterDriver> getAllAmbiguityDrivers() {
        return Collections.unmodifiableList(ambiguityDrivers);
    }

    /** Get the ambiguity parameters drivers that have not been fixed yet.
     * @return ambiguity parameters drivers that have not been fixed yet
     */
    protected List<ParameterDriver> getFreeAmbiguityDrivers() {
        return ambiguityDrivers.
                        stream().
                        filter(d -> {
                            if (d.isSelected()) {
                                final double near   = FastMath.rint(d.getValue());
                                final double gapMin = near - d.getMinValue();
                                final double gapMax = d.getMaxValue() - near;
                                return FastMath.max(FastMath.abs(gapMin), FastMath.abs(gapMax)) > 1.0e-15;
                            } else {
                                return false;
                            }
                        }).
                        collect(Collectors.toList());
    }

    /** Get ambiguity indirection array for ambiguity parameters drivers that have not been fixed yet.
     * @param startIndex start index for measurements parameters in global covariance matrix
     * @param measurementsParametersDrivers measurements parameters drivers in global covariance matrix order
     * @return indirection array between full covariance matrix and ambiguity covariance matrix
     */
    protected int[] getFreeAmbiguityIndirection(final int startIndex,
                                                final List<ParameterDriver> measurementsParametersDrivers) {

        // set up indirection array
        final List<ParameterDriver> freeDrivers = getFreeAmbiguityDrivers();
        final int n = freeDrivers.size();
        final int[] indirection = new int[n];
        for (int i = 0; i < n; ++i) {
            indirection[i] = -1;
            final String name = freeDrivers.get(i).getName();
            for (int k = 0; k < measurementsParametersDrivers.size(); ++k) {
                if (name.equals(measurementsParametersDrivers.get(k).getName())) {
                    indirection[i] = startIndex + k;
                    break;
                }
            }
            if (indirection[i] < 0) {
                // the parameter was not found
                final StringBuilder builder = new StringBuilder();
                for (final ParameterDriver driver : measurementsParametersDrivers) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(driver.getName());
                }
                throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                         name, builder.toString());
            }
        }

        return indirection;

    }

    /** Un-fix an integer ambiguity (typically after a phase cycle slip).
     * @param ambiguityDriver driver for the ambiguity to un-fix
     */
    public void unFixAmbiguity(final ParameterDriver ambiguityDriver) {
        ambiguityDriver.setMinValue(Double.NEGATIVE_INFINITY);
        ambiguityDriver.setMaxValue(Double.POSITIVE_INFINITY);
    }

    /** Fix integer ambiguities.
     * @param startIndex start index for measurements parameters in global covariance matrix
     * @param measurementsParametersDrivers measurements parameters drivers in global covariance matrix order
     * @param covariance global covariance matrix
     * @return list of newly fixed ambiguities (ambiguities already fixed before the call are not counted)
     */
    public List<ParameterDriver> fixIntegerAmbiguities(final int startIndex,
                                                       final List<ParameterDriver> measurementsParametersDrivers,
                                                       final RealMatrix covariance) {

        // set up Integer Least Square problem
        final List<ParameterDriver> ambiguities      = getAllAmbiguityDrivers();
        final double[]              floatAmbiguities = ambiguities.stream().mapToDouble(d -> d.getValue()).toArray();
        final int[]                 indirection      = getFreeAmbiguityIndirection(startIndex, measurementsParametersDrivers);

        // solve the ILS problem
        final IntegerLeastSquareSolution[] candidates =
                        solver.solveILS(acceptance.numberOfCandidates(), floatAmbiguities, indirection, covariance);

        // FIXME A cleaner way is:
        //     1°/ Add a getName() method in IntegerLeastSquareSolver interface
        //     2°/ Add static name attribute to IntegerBootstrapping, LAMBDA and ModifiedLAMBDA classes
        if (solver instanceof IntegerBootstrapping && candidates.length == 0) {
            return Collections.emptyList();
        }

        // check number of candidates
        if (candidates.length < acceptance.numberOfCandidates()) {
            return Collections.emptyList();
        }

        // check acceptance
        final IntegerLeastSquareSolution bestCandidate = acceptance.accept(candidates);
        if (bestCandidate == null) {
            return Collections.emptyList();
        }

        // fix the ambiguities
        final long[] fixedAmbiguities = bestCandidate.getSolution();
        final List<ParameterDriver> fixedDrivers = new ArrayList<>(indirection.length);
        for (int i = 0; i < indirection.length; ++i) {
            final ParameterDriver driver = measurementsParametersDrivers.get(indirection[i] - startIndex);
            driver.setMinValue(fixedAmbiguities[i]);
            driver.setMaxValue(fixedAmbiguities[i]);
            fixedDrivers.add(driver);
        }

        // Update the others parameter drivers accordingly to the fixed integer ambiguity
        // Covariance matrix between integer ambiguity and the other parameter driver
        final RealMatrix Qab = getCovMatrix(covariance, indirection);

        final RealVector X = new QRDecomposer(1.0e-10).decompose(getAmbiguityMatrix(covariance, indirection)).solve(MatrixUtils.createRealVector(floatAmbiguities).
                                                                                                           subtract(MatrixUtils.createRealVector(toDoubleArray(fixedAmbiguities.length, fixedAmbiguities))));
        final RealVector Y =  Qab.preMultiply(X);

        for (int i = startIndex + 1; i < covariance.getColumnDimension(); i++) {
            if (!belongTo(indirection, i)) {
                final ParameterDriver driver = measurementsParametersDrivers.get(i - startIndex);
                driver.setValue(driver.getValue() - Y.getEntry(i - startIndex));
            }
        }

        return fixedDrivers;

    }

   /** Get the covariance matrix between the integer ambiguities and the other parameter driver.
    * @param cov global covariance matrix
    * @param indirection array of the position of integer ambiguity parameter driver
    * @return covariance matrix.
    */
    private RealMatrix getCovMatrix(final RealMatrix cov, final int[] indirection) {
        final RealMatrix Qab = MatrixUtils.createRealMatrix(indirection.length, cov.getColumnDimension());
        int index = 0;
        int iter  = 0;
        while (iter < indirection.length) {
            // Loop on column dimension
            for (int j = 0; j < cov.getColumnDimension(); j++) {
                if (!belongTo(indirection, j)) {
                    Qab.setEntry(index, 0, cov.getEntry(index, 0));
                }
            }
            index++;
            iter++;
        }
        return Qab;
    }

     /** Return the matrix of the ambiguity from the global covariance matrix.
      * @param cov global covariance matrix
      * @param indirection array of the position of the ambiguity within the global covariance matrix
      * @return matrix of ambiguities covariance
      */
    private RealMatrix getAmbiguityMatrix(final RealMatrix cov, final int[] indirection) {
        final RealMatrix Qa = MatrixUtils.createRealMatrix(indirection.length, indirection.length);
        for (int i = 0; i < indirection.length; i++) {
            Qa.setEntry(i, i, cov.getEntry(indirection[i], indirection[i]));
            for (int j = 0; j < i; j++) {
                Qa.setEntry(i, j, cov.getEntry(indirection[i], indirection[j]));
                Qa.setEntry(j, i, cov.getEntry(indirection[i], indirection[j]));
            }
        }
        return Qa;
    }

    /** Compute whether or not the integer pos belongs to the indirection array.
     * @param indirection array of the position of ambiguities within the global covariance matrix
     * @param pos integer for which we want to know if it belong to the indirection array.
     * @return true if it belongs.
     */
    private boolean belongTo(final int[] indirection, final int pos) {
        for (int j : indirection) {
            if (pos == j) {
                return true;
            }
        }
        return false;
    }

    /** Transform an array of long to an array of double.
     * @param size size of the destination array
     * @param longArray source array
     * @return the destination array
     */
    private double[] toDoubleArray(final int size, final long[] longArray) {
        // Initialize double array
        final double[] doubleArray = new double[size];
        // Copy the elements
        for (int index = 0; index < size; index++) {
            doubleArray[index] = longArray[index];
        }
        return doubleArray;
    }

}
