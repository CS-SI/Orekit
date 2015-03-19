/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.forces.gravity.potential;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**This abstract class represents a Gravitational Potential Coefficients file reader.
 *
 * <p> As it exits many different coefficients models and containers this
 *  interface represents all the methods that should be implemented by a reader.
 *  The proper way to use this interface is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file.<p>
 *
 * @see GravityFieldFactory
 * @author Fabien Maussion
 */
public abstract class PotentialCoefficientsReader implements DataLoader {

    /** Maximal degree to parse. */
    private int maxParseDegree;

    /** Maximal order to parse. */
    private int maxParseOrder;

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Allow missing coefficients in the input data. */
    private final boolean missingCoefficientsAllowed;

    /** Indicator for complete read. */
    private boolean readComplete;

    /** Central body reference radius. */
    private double ae;

    /** Central body attraction coefficient. */
    private double mu;

    /** Raw tesseral-sectorial coefficients matrix. */
    private double[][] rawC;

    /** Raw tesseral-sectorial coefficients matrix. */
    private double[][] rawS;

    /** Indicator for normalized raw coefficients. */
    private boolean normalized;

    /** Tide system. */
    private TideSystem tideSystem;

    /** Simple constructor.
     * <p>Build an uninitialized reader.</p>
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed allow missing coefficients in the input data
     */
    protected PotentialCoefficientsReader(final String supportedNames,
                                          final boolean missingCoefficientsAllowed) {
        this.supportedNames             = supportedNames;
        this.missingCoefficientsAllowed = missingCoefficientsAllowed;
        this.maxParseDegree             = Integer.MAX_VALUE;
        this.maxParseOrder              = Integer.MAX_VALUE;
        this.readComplete               = false;
        this.ae                         = Double.NaN;
        this.mu                         = Double.NaN;
        this.rawC                       = null;
        this.rawS                       = null;
        this.normalized                 = false;
        this.tideSystem                 = TideSystem.UNKNOWN;
    }

    /** Get the regular expression for supported files names.
     * @return regular expression for supported files names
     */
    public String getSupportedNames() {
        return supportedNames;
    }

    /** Check if missing coefficients are allowed in the input data.
     * @return true if missing coefficients are allowed in the input data
     */
    public boolean missingCoefficientsAllowed() {
        return missingCoefficientsAllowed;
    }

    /** Set the degree limit for the next file parsing.
     * @param maxParseDegree maximal degree to parse (may be safely
     * set to {@link Integer#MAX_VALUE} to parse all available coefficients)
     * @since 6.0
     */
    public void setMaxParseDegree(final int maxParseDegree) {
        this.maxParseDegree = maxParseDegree;
    }

    /** Get the degree limit for the next file parsing.
     * @return degree limit for the next file parsing
     * @since 6.0
     */
    public int getMaxParseDegree() {
        return maxParseDegree;
    }

    /** Set the order limit for the next file parsing.
     * @param maxParseOrder maximal order to parse (may be safely
     * set to {@link Integer#MAX_VALUE} to parse all available coefficients)
     * @since 6.0
     */
    public void setMaxParseOrder(final int maxParseOrder) {
        this.maxParseOrder = maxParseOrder;
    }

    /** Get the order limit for the next file parsing.
     * @return order limit for the next file parsing
     * @since 6.0
     */
    public int getMaxParseOrder() {
        return maxParseOrder;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return !(readComplete &&
                 getMaxAvailableDegree() >= getMaxParseDegree() &&
                 getMaxAvailableOrder()  >= getMaxParseOrder());
    }

    /** Set the indicator for completed read.
     * @param readComplete if true, a gravity field has been completely read
     */
    protected void setReadComplete(final boolean readComplete) {
        this.readComplete = readComplete;
    }

    /** Set the central body reference radius.
     * @param ae central body reference radius
     */
    protected void setAe(final double ae) {
        this.ae = ae;
    }

    /** Get the central body reference radius.
     * @return central body reference radius
     */
    protected double getAe() {
        return ae;
    }

    /** Set the central body attraction coefficient.
     * @param mu central body attraction coefficient
     */
    protected void setMu(final double mu) {
        this.mu = mu;
    }

    /** Get the central body attraction coefficient.
     * @return central body attraction coefficient
     */
    protected double getMu() {
        return mu;
    }

    /** Set the {@link TideSystem} used in the gravity field.
     * @param tideSystem tide system used in the gravity field
     */
    protected void setTideSystem(final TideSystem tideSystem) {
        this.tideSystem = tideSystem;
    }

    /** Get the {@link TideSystem} used in the gravity field.
     * @return tide system used in the gravity field
     */
    protected TideSystem getTideSystem() {
        return tideSystem;
    }

    /** Set the tesseral-sectorial coefficients matrix.
     * @param rawNormalized if true, raw coefficients are normalized
     * @param c raw tesseral-sectorial coefficients matrix
     * (a reference to the array will be stored)
     * @param s raw tesseral-sectorial coefficients matrix
     * (a reference to the array will be stored)
     * @param name name of the file (or zip entry)
     * @exception OrekitException if a coefficient is missing
     */
    protected void setRawCoefficients(final boolean rawNormalized,
                                      final double[][] c, final double[][] s,
                                      final String name)
        throws OrekitException {

        // normalization indicator
        normalized = rawNormalized;

        // set known constant values, if they were not defined in the file.
        // See Hofmann-Wellenhof and Moritz, "Physical Geodesy",
        // section 2.6 Harmonics of Lower Degree.
        // All S_i,0 are irrelevant because they are multiplied by zero.
        // C0,0 is 1, the central part, since all coefficients are normalized by GM.
        setIfUnset(c, 0, 0, 1);
        setIfUnset(s, 0, 0, 0);
        // C1,0, C1,1, and S1,1 are the x,y,z coordinates of the center of mass,
        // which are 0 since all coefficients are given in an Earth centered frame
        setIfUnset(c, 1, 0, 0);
        setIfUnset(s, 1, 0, 0);
        setIfUnset(c, 1, 1, 0);
        setIfUnset(s, 1, 1, 0);

        // cosine part
        for (int i = 0; i < c.length; ++i) {
            for (int j = 0; j < c[i].length; ++j) {
                if (Double.isNaN(c[i][j])) {
                    throw new OrekitException(OrekitMessages.MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                              'C', i, j, name);
                }
            }
        }
        rawC = c;

        // sine part
        for (int i = 0; i < s.length; ++i) {
            for (int j = 0; j < s[i].length; ++j) {
                if (Double.isNaN(s[i][j])) {
                    throw new OrekitException(OrekitMessages.MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                              'S', i, j, name);
                }
            }
        }
        rawS = s;

    }

    /**
     * Set a coefficient if it has not been set already.
     * <p>
     * If {@code array[i][j]} is 0 or NaN this method sets it to {@code value} and returns
     * {@code true}. Otherwise the original value of {@code array[i][j]} is preserved and
     * this method return {@code false}.
     * <p>
     * If {@code array[i][j]} does not exist then this method returns {@code false}.
     *
     * @param array the coefficient array.
     * @param i     degree, the first index to {@code array}.
     * @param j     order, the second index to {@code array}.
     * @param value the new value to set.
     * @return {@code true} if the coefficient was set to {@code value}, {@code false} if
     * the coefficient was not set to {@code value}. A {@code false} return indicates the
     * coefficient has previously been set to a non-NaN, non-zero value.
     */
    private boolean setIfUnset(final double[][] array,
                               final int i,
                               final int j,
                               final double value) {
        if (array.length > i && array[i].length > j &&
                (Double.isNaN(array[i][j]) || Precision.equals(array[i][j], 0.0, 1))) {
            // the coefficient was not already initialized
            array[i][j] = value;
            return true;
        } else {
            return false;
        }
    }

    /** Get the maximal degree available in the last file parsed.
     * @return maximal degree available in the last file parsed
     * @since 6.0
     */
    public int getMaxAvailableDegree() {
        return rawC.length - 1;
    }

    /** Get the maximal order available in the last file parsed.
     * @return maximal order available in the last file parsed
     * @since 6.0
     */
    public int getMaxAvailableOrder() {
        return rawC[rawC.length - 1].length - 1;
    }

    /** {@inheritDoc} */
    public abstract void loadData(InputStream input, String name)
        throws IOException, ParseException, OrekitException;

    /** Get a provider for read spherical harmonics coefficients.
     * @param wantNormalized if true, the provider will provide normalized coefficients,
     * otherwise it will provide un-normalized coefficients
     * @param degree maximal degree
     * @param order maximal order
     * @return a new provider
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order or if no gravity field has read yet
     * @see #getConstantProvider(boolean, int, int)
     * @since 6.0
     */
    public abstract RawSphericalHarmonicsProvider getProvider(boolean wantNormalized, int degree, int order)
        throws OrekitException;

    /** Get a time-independent provider for read spherical harmonics coefficients.
     * @param wantNormalized if true, the raw provider must provide normalized coefficients,
     * otherwise it will provide un-normalized coefficients
     * @param degree maximal degree
     * @param order maximal order
     * @return a new provider, with no time-dependent parts
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order or if no gravity field has read yet
     * @see #getProvider(boolean, int, int)
     * @since 6.0
     */
    protected ConstantSphericalHarmonics getConstantProvider(final boolean wantNormalized,
                                                             final int degree, final int order)
        throws OrekitException {

        if (!readComplete) {
            throw new OrekitException(OrekitMessages.NO_GRAVITY_FIELD_DATA_LOADED);
        }

        if (degree >= rawC.length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD,
                                      degree, rawC.length - 1);
        }

        if (order >= rawC[rawC.length - 1].length) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD,
                                      order, rawC[rawC.length - 1].length);
        }

        // fix normalization
        final double[][] truncatedC = buildTriangularArray(degree, order, 0.0);
        final double[][] truncatedS = buildTriangularArray(degree, order, 0.0);
        rescale(1.0, normalized, rawC, rawS, wantNormalized, truncatedC, truncatedS);

        return new ConstantSphericalHarmonics(ae, mu, tideSystem, truncatedC, truncatedS);

    }

    /** Build a coefficients triangular array.
     * @param degree array degree
     * @param order array order
     * @param value initial value to put in array elements
     * @return built array
     */
    protected static double[][] buildTriangularArray(final int degree, final int order, final double value) {
        final double[][] array = new double[degree + 1][];
        for (int k = 0; k < array.length; ++k) {
            array[k] = buildRow(k, order, value);
        }
        return array;
    }

    /**
     * Parse a double from a string. Accept the Fortran convention of using a 'D' or
     * 'd' instead of an 'E' or 'e'.
     *
     * @param string to be parsed.
     * @return the double value of {@code string}.
     */
    protected static double parseDouble(final String string) {
        return Double.parseDouble(string.toUpperCase(Locale.ENGLISH).replace('D', 'E'));
    }

    /** Build a coefficients row.
     * @param degree row degree
     * @param order row order
     * @param value initial value to put in array elements
     * @return built row
     */
    protected static double[] buildRow(final int degree, final int order, final double value) {
        final double[] row = new double[FastMath.min(order, degree) + 1];
        Arrays.fill(row, value);
        return row;
    }

    /** Extend a list of lists of coefficients if needed.
     * @param list list of lists of coefficients
     * @param degree degree required to be present
     * @param order order required to be present
     * @param value initial value to put in list elements
     */
    protected void extendListOfLists(final List<List<Double>> list, final int degree, final int order,
                                     final double value) {
        for (int i = list.size(); i <= degree; ++i) {
            list.add(new ArrayList<Double>());
        }
        final List<Double> listN = list.get(degree);
        final Double v = Double.valueOf(value);
        for (int j = listN.size(); j <= order; ++j) {
            listN.add(v);
        }
    }

    /** Convert a list of list into an array.
     * @param list list of lists of coefficients
     * @return a new array
     */
    protected double[][] toArray(final List<List<Double>> list) {
        final double[][] array = new double[list.size()][];
        for (int i = 0; i < array.length; ++i) {
            array[i] = new double[list.get(i).size()];
            for (int j = 0; j < array[i].length; ++j) {
                array[i][j] = list.get(i).get(j);
            }
        }
        return array;
    }

    /** Parse a coefficient.
     * @param field text field to parse
     * @param list list where to put the coefficient
     * @param i first index in the list
     * @param j second index in the list
     * @param cName name of the coefficient
     * @param name name of the file
     * @exception OrekitException if the coefficient is already set
     */
    protected void parseCoefficient(final String field, final List<List<Double>> list,
                                    final int i, final int j,
                                    final String cName, final String name)
        throws OrekitException {
        final double value    = parseDouble(field);
        final double oldValue = list.get(i).get(j);
        if (Double.isNaN(oldValue) || Precision.equals(oldValue, 0.0, 1)) {
            // the coefficient was not already initialized
            list.get(i).set(j, value);
        } else {
            throw new OrekitException(OrekitMessages.DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                      name, i, j, name);
        }
    }

    /** Parse a coefficient.
     * @param field text field to parse
     * @param array array where to put the coefficient
     * @param i first index in the list
     * @param j second index in the list
     * @param cName name of the coefficient
     * @param name name of the file
     * @exception OrekitException if the coefficient is already set
     */
    protected void parseCoefficient(final String field, final double[][] array,
                                    final int i, final int j,
                                    final String cName, final String name)
        throws OrekitException {
        final double value    = parseDouble(field);
        final double oldValue = array[i][j];
        if (Double.isNaN(oldValue) || Precision.equals(oldValue, 0.0, 1)) {
            // the coefficient was not already initialized
            array[i][j] = value;
        } else {
            throw new OrekitException(OrekitMessages.DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                      name, i, j, name);
        }
    }

    /** Rescale coefficients arrays.
     * @param scale general scaling factor to apply to all elements
     * @param normalizedOrigin if true, the origin coefficients are normalized
     * @param originC cosine part of the origina coefficients
     * @param originS sine part of the origin coefficients
     * @param wantNormalized if true, the rescaled coefficients must be normalized
     * @param rescaledC cosine part of the rescaled coefficients to fill in (may be the originC array)
     * @param rescaledS sine part of the rescaled coefficients to fill in (may be the originS array)
     * @exception OrekitException if normalization/unnormalization fails because of an underflow
     * due to too high degree/order
     */
    protected static void rescale(final double scale,
                                  final boolean normalizedOrigin, final double[][] originC,
                                  final double[][] originS, final boolean wantNormalized,
                                  final double[][] rescaledC, final double[][] rescaledS)
        throws OrekitException {

        if (wantNormalized == normalizedOrigin) {
            // apply only the general scaling factor
            for (int i = 0; i < rescaledC.length; ++i) {
                final double[] rCi = rescaledC[i];
                final double[] rSi = rescaledS[i];
                final double[] oCi = originC[i];
                final double[] oSi = originS[i];
                for (int j = 0; j < rCi.length; ++j) {
                    rCi[j] = oCi[j] * scale;
                    rSi[j] = oSi[j] * scale;
                }
            }
        } else {

            // we have to re-scale the coefficients
            // (we use rescaledC.length - 1 for the order instead of rescaledC[rescaledC.length - 1].length - 1
            //  because typically trend or pulsation arrays are irregular, some test cases have
            //  order 2 elements at degree 2, but only order 1 elements for higher degrees for example)
            final double[][] factors = GravityFieldFactory.getUnnormalizationFactors(rescaledC.length - 1,
                                                                                     rescaledC.length - 1);

            if (wantNormalized) {
                // normalize the coefficients
                for (int i = 0; i < rescaledC.length; ++i) {
                    final double[] rCi = rescaledC[i];
                    final double[] rSi = rescaledS[i];
                    final double[] oCi = originC[i];
                    final double[] oSi = originS[i];
                    final double[] fi  = factors[i];
                    for (int j = 0; j < rCi.length; ++j) {
                        final double factor = scale / fi[j];
                        rCi[j] = oCi[j] * factor;
                        rSi[j] = oSi[j] * factor;
                    }
                }
            } else {
                // un-normalize the coefficients
                for (int i = 0; i < rescaledC.length; ++i) {
                    final double[] rCi = rescaledC[i];
                    final double[] rSi = rescaledS[i];
                    final double[] oCi = originC[i];
                    final double[] oSi = originS[i];
                    final double[] fi  = factors[i];
                    for (int j = 0; j < rCi.length; ++j) {
                        final double factor = scale * fi[j];
                        rCi[j] = oCi[j] * factor;
                        rSi[j] = oSi[j] * factor;
                    }
                }
            }

        }
    }

}
