/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces.gravity.potential;

import java.io.IOException;
import java.io.InputStream;

import org.orekit.errors.OrekitException;

/**This abstract class represents a Gravitational Potential Coefficients file reader.
 *
 * <p> As it exits many different coefficients models and containers this
 *  interface represents all the methods that should be implemented by a reader.
 *  The proper way to use this interface is to call the {@link PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file.<p>
 *
 * @see PotentialReaderFactory
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public abstract class PotentialCoefficientsReader {

    /** Error message for too large degree. */
    private static final String TOO_LARGE_DEGREE =
        "too large degree (n = {0}), potential maximal degree is {1})";

    /** Error message for too large order. */
    private static final String TOO_LARGE_ORDER =
        "too large order (m = {0}), potential maximal order is {1})";

    // CHECKSTYLE: stop VisibilityModifierCheck

    /** Central body reference radius. */
    protected double ae;

    /** Central body attraction coefficient. */
    protected double mu;

    /** fully normalized zonal coefficients array. */
    protected double[] normalizedJ;

    /** fully normalized tesseral-sectorial coefficients matrix. */
    protected double[][] normalizedC;

    /** fully normalized tesseral-sectorial coefficients matrix. */
    protected double[][] normalizedS;

    /** un-normalized zonal coefficients array. */
    private double[] unNormalizedJ;

    /** un-normalized tesseral-sectorial coefficients matrix. */
    private double[][] unNormalizedC;

    /** un-normalized tesseral-sectorial coefficients matrix. */
    private double[][] unNormalizedS;

    // CHECKSTYLE: resume VisibilityModifierCheck

    /** Simple constructor.
     * <p>Build an uninitialized reader.</p>
     */
    protected PotentialCoefficientsReader() {
        normalizedJ = null;
        normalizedC = null;
        normalizedS = null;
        unNormalizedJ = null;
        unNormalizedC = null;
        unNormalizedS = null;
    }

    /** Check the file to determine if its format is understood by the reader or not.
     * @param in the input to check
     * @return true if it is readable, false if not.
     * @exception IOException when the {@link InputStream} cannot be buffered.
     */
    public abstract boolean isFileOK(InputStream in) throws IOException;

    /** Computes the coefficients by reading the selected (and tested) file.
     * @exception OrekitException when the file has not been initialized or checked.
     * @exception IOException when the file is corrupted.
     */
    public abstract void read() throws OrekitException, IOException;

    /** Get the zonal coefficients.
     * @param normalized (true) or un-normalized (false)
     * @param n the maximal degree requested
     * @return J the zonal coefficients array.
     * @exception OrekitException if the requested maximal degree exceeds the
     * available degree
     */
    public double[] getJ(final boolean normalized, final int n)
        throws OrekitException {
        if (n >= normalizedC.length) {
            throw new OrekitException(TOO_LARGE_DEGREE,
                                      new Object[] {
                                          Integer.valueOf(n),
                                          Integer.valueOf(normalizedC.length - 1)
                                      });
        }

        final double[] completeJ = normalized ? getNormalizedJ() : getUnNormalizedJ();

        // truncate the array as per caller request
        final double[] result = new double[n + 1];
        System.arraycopy(completeJ, 0, result, 0, n + 1);

        return result;

    }

    /** Get the tesseral-sectorial and zonal coefficients.
     * @param n the degree
     * @param m the order
     * @param normalized (true) or un-normalized (false)
     * @return the cosines coefficients matrix
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order
     */
    public double[][] getC(final int n, final int m, final boolean normalized)
        throws OrekitException {
        return truncateArray(n, m, normalized ? getNormalizedC() : getUnNormalizedC());
    }

    /** Get tesseral-sectorial coefficients.
     * @param n the degree
     * @param m the order
     * @param normalized (true) or un-normalized (false)
     * @return the sines coefficients matrix
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order
     */
    public double[][] getS(final int n, final int m, final boolean normalized)
        throws OrekitException {
        return truncateArray(n, m, normalized ? getNormalizedS() : getUnNormalizedS());
    }

    /** Get the tesseral-sectorial and zonal coefficients.
     * @param n the degree
     * @param m the order
     * @param complete the complete array
     * @return the trunctated coefficients array
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order
     */
    private double[][] truncateArray(final int n, final int m, final double[][] complete)
        throws OrekitException {

        // safety checks
        if (n >= complete.length) {
            throw new OrekitException(TOO_LARGE_DEGREE,
                                      new Object[] {
                                          Integer.valueOf(n), Integer.valueOf(complete.length - 1)
                                      });
        }
        if (m >= complete[complete.length - 1].length) {
            throw new OrekitException(TOO_LARGE_ORDER,
                                      new Object[] {
                                          Integer.valueOf(m), Integer.valueOf(complete[complete.length - 1].length - 1)
                                      });
        }

        // truncate each array row in turn
        final double[][] result = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            final double[] ri = new double[Math.min(i, m) + 1];
            System.arraycopy(complete[i], 0, ri, 0, ri.length);
            result[i] = ri;
        }

        return result;

    }

    /** Get the fully normalized zonal coefficients.
     * @return J the zonal coefficients array.
     */
    private double[] getNormalizedJ() {
        if (normalizedJ == null) {
            normalizedJ = new double[normalizedC.length];
            for (int i = 0; i < normalizedC.length; i++) {
                normalizedJ[i] = -normalizedC[i][0];
            }
        }
        return normalizedJ;
    }

    /** Get the fully normalized tesseral-sectorial and zonal coefficients.
     * @return C the coefficients matrix
     */
    private double[][] getNormalizedC() {
        return normalizedC;
    }

    /** Get the fully normalized tesseral-sectorial coefficients.
     * @return S the coefficients matrix
     */
    private double[][] getNormalizedS() {
        return normalizedS;
    }

    /** Get the un-normalized  zonal coefficients.
     * @return J the zonal coefficients array.
     */
    private double[] getUnNormalizedJ() {
        if (unNormalizedJ == null) {
            final double[][] uC = getUnNormalizedC();
            unNormalizedJ = new double[uC.length];
            for (int i = 0; i < uC.length; i++) {
                unNormalizedJ[i] = -uC[i][0];
            }
        }
        return unNormalizedJ;
    }

    /** Get the un-normalized tesseral-sectorial and zonal coefficients.
     * @return C the coefficients matrix
     */
    private double[][] getUnNormalizedC() {
        // calculate only if asked
        if (unNormalizedC == null) {
            unNormalizedC = unNormalize(normalizedC);
        }
        return unNormalizedC;
    }

    /** Get the un-normalized tesseral-sectorial coefficients.
     * @return S the coefficients matrix
     */
    private double[][] getUnNormalizedS() {
        // calculate only if asked
        if (unNormalizedS == null) {
            unNormalizedS = unNormalize(normalizedS);
        }
        return unNormalizedS;
    }

    /** Unnormalize a coefficients array.
     * @param normalized normalized coefficients array
     * @return unnormalized array
     */
    private double[][] unNormalize(final double[][] normalized) {

        // allocate a triangular array
        final double[][] unNormalized = new double[normalized.length][];
        unNormalized[0] = new double[] {
            normalized[0][0]
        };

        // initialization
        double factN = 1.0;
        double mfactNMinusM = 1.0;
        double mfactNPlusM = 1.0;

        // unnormalize the coefficients
        for (int n = 1; n < normalized.length; n++) {
            final double[] uRow = new double[n + 1];
            final double[] nRow = normalized[n];
            final double coeffN = 2.0 * (2 * n + 1);
            factN *= n;
            mfactNMinusM = factN;
            mfactNPlusM = factN;
            uRow[0] = Math.sqrt(2 * n + 1) * normalized[n][0];
            for (int m = 1; m < nRow.length; m++) {
                mfactNPlusM  *= n + m;
                mfactNMinusM /= n - m + 1;
                uRow[m] = Math.sqrt((coeffN * mfactNMinusM) / mfactNPlusM) * nRow[m];
            }
            unNormalized[n] = uRow;
        }

        return unNormalized;

    }

    /** Get the central body attraction coefficient.
     * @return mu (m<sup>3</sup>/s<sup>2</sup>)
     */
    public double getMu() {
        return mu;
    }

    /** Get the value of the central body reference radius.
     * @return ae (m)
     */
    public double getAe() {
        return ae;
    }

}
