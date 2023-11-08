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
package org.orekit.forces.gravity.potential;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;

/**This abstract class represents a Gravitational Potential Coefficients file reader.
 *
 * <p> As it exits many different coefficients models and containers this
 *  interface represents all the methods that should be implemented by a reader.
 *  The proper way to use this interface is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file.</p>
 *
 * @see GravityFields
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

    /** Time scale for parsing dates. */
    private final TimeScale timeScale;

    /** Indicator for complete read. */
    private boolean readComplete;

    /** Central body reference radius. */
    private double ae;

    /** Central body attraction coefficient. */
    private double mu;

    /** Converter from triangular to flat form. */
    private Flattener flattener;

    /** Raw tesseral-sectorial coefficients matrix. */
    private double[] rawC;

    /** Raw tesseral-sectorial coefficients matrix. */
    private double[] rawS;

    /** Indicator for normalized raw coefficients. */
    private boolean normalized;

    /** Tide system. */
    private TideSystem tideSystem;

    /** Simple constructor.
     * <p>Build an uninitialized reader.</p>
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed allow missing coefficients in the input data
     * @see #PotentialCoefficientsReader(String, boolean, TimeScale)
     */
    @DefaultDataContext
    protected PotentialCoefficientsReader(final String supportedNames,
                                          final boolean missingCoefficientsAllowed) {
        this(supportedNames, missingCoefficientsAllowed,
                DataContext.getDefault().getTimeScales().getTT());
    }

    /** Simple constructor.
     * <p>Build an uninitialized reader.</p>
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed allow missing coefficients in the input data
     * @param timeScale to use when parsing dates.
     * @since 10.1
     */
    protected PotentialCoefficientsReader(final String supportedNames,
                                          final boolean missingCoefficientsAllowed,
                                          final TimeScale timeScale) {
        this.supportedNames             = supportedNames;
        this.missingCoefficientsAllowed = missingCoefficientsAllowed;
        this.maxParseDegree             = Integer.MAX_VALUE;
        this.maxParseOrder              = Integer.MAX_VALUE;
        this.readComplete               = false;
        this.ae                         = Double.NaN;
        this.mu                         = Double.NaN;
        this.flattener                  = null;
        this.rawC                       = null;
        this.rawS                       = null;
        this.normalized                 = false;
        this.tideSystem                 = TideSystem.UNKNOWN;
        this.timeScale                  = timeScale;
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
     * @param f converter from triangular to flat form
     * @param c raw tesseral-sectorial coefficients matrix
     * @param s raw tesseral-sectorial coefficients matrix
     * @param name name of the file (or zip entry)
     * @since 11.1
     */
    protected void setRawCoefficients(final boolean rawNormalized, final Flattener f,
                                      final double[] c, final double[] s,
                                      final String name) {

        this.flattener = f;

        // normalization indicator
        normalized = rawNormalized;

        // set known constant values, if they were not defined in the file.
        // See Hofmann-Wellenhof and Moritz, "Physical Geodesy",
        // section 2.6 Harmonics of Lower Degree.
        // All S_i,0 are irrelevant because they are multiplied by zero.
        // C0,0 is 1, the central part, since all coefficients are normalized by GM.
        setIfUnset(c, flattener.index(0, 0), 1);
        setIfUnset(s, flattener.index(0, 0), 0);

        if (flattener.getDegree() >= 1) {
            // C1,0, C1,1, and S1,1 are the x,y,z coordinates of the center of mass,
            // which are 0 since all coefficients are given in an Earth centered frame
            setIfUnset(c, flattener.index(1, 0), 0);
            setIfUnset(s, flattener.index(1, 0), 0);
            if (flattener.getOrder() >= 1) {
                setIfUnset(c, flattener.index(1, 1), 0);
                setIfUnset(s, flattener.index(1, 1), 0);
            }
        }

        // cosine part
        for (int i = 0; i <= flattener.getDegree(); ++i) {
            for (int j = 0; j <= FastMath.min(i, flattener.getOrder()); ++j) {
                if (Double.isNaN(c[flattener.index(i, j)])) {
                    throw new OrekitException(OrekitMessages.MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                              'C', i, j, name);
                }
            }
        }
        rawC = c.clone();

        // sine part
        for (int i = 0; i <= flattener.getDegree(); ++i) {
            for (int j = 0; j <= FastMath.min(i, flattener.getOrder()); ++j) {
                if (Double.isNaN(s[flattener.index(i, j)])) {
                    throw new OrekitException(OrekitMessages.MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                              'S', i, j, name);
                }
            }
        }
        rawS = s.clone();

    }

    /**
     * Set a coefficient if it has not been set already.
     * <p>
     * If {@code array[i]} is 0 or NaN this method sets it to {@code value} and returns
     * {@code true}. Otherwise the original value of {@code array[i]} is preserved and
     * this method return {@code false}.
     * <p>
     * If {@code array[i]} does not exist then this method returns {@code false}.
     *
     * @param array the coefficient array.
     * @param i     index in array.
     * @param value the new value to set.
     * @return {@code true} if the coefficient was set to {@code value}, {@code false} if
     * the coefficient was not set to {@code value}. A {@code false} return indicates the
     * coefficient has previously been set to a non-NaN, non-zero value.
     */
    private boolean setIfUnset(final double[] array, final int i, final double value) {
        if (array.length > i && (Double.isNaN(array[i]) || Precision.equals(array[i], 0.0, 0))) {
            // the coefficient was not already initialized
            array[i] = value;
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
        return flattener.getDegree();
    }

    /** Get the maximal order available in the last file parsed.
     * @return maximal order available in the last file parsed
     * @since 6.0
     */
    public int getMaxAvailableOrder() {
        return flattener.getOrder();
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
     * @since 6.0
     */
    public abstract RawSphericalHarmonicsProvider getProvider(boolean wantNormalized, int degree, int order);

    /** Get a time-independent provider containing base harmonics coefficients.
     * <p>
     * Beware that some coeefficients may be missing here, if they are managed as time-dependent
     * piecewise models (as in ICGEM V2.0).
     * </p>
     * @param wantNormalized if true, the raw provider must provide normalized coefficients,
     * otherwise it will provide un-normalized coefficients
     * @param degree maximal degree
     * @param order maximal order
     * @return a new provider, with no time-dependent parts
     * @see #getProvider(boolean, int, int)
     * @since 11.1
     */
    protected ConstantSphericalHarmonics getBaseProvider(final boolean wantNormalized,
                                                         final int degree, final int order) {

        if (!readComplete) {
            throw new OrekitException(OrekitMessages.NO_GRAVITY_FIELD_DATA_LOADED);
        }

        final Flattener truncatedFlattener = new Flattener(degree, order);
        return new ConstantSphericalHarmonics(ae, mu, tideSystem, truncatedFlattener,
                                              rescale(1.0, wantNormalized, truncatedFlattener, flattener, rawC),
                                              rescale(1.0, wantNormalized, truncatedFlattener, flattener, rawS));

    }

    /** Build a coefficients array in flat form.
     * @param flattener converter from triangular to flat form
     * @param value initial value to put in array elements
     * @return built array
     * @since 11.1
     */
    protected static double[] buildFlatArray(final Flattener flattener, final double value) {
        final double[] array = new double[flattener.arraySize()];
        Arrays.fill(array, value);
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

    /** Parse a coefficient.
     * @param field text field to parse
     * @param f converter from triangular to flat form
     * @param array array where to put the coefficient
     * @param i first index in the list
     * @param j second index in the list
     * @param cName name of the coefficient
     * @param name name of the file
     * @since 11.1
     */
    protected void parseCoefficient(final String field, final Flattener f,
                                    final double[] array, final int i, final int j,
                                    final String cName, final String name) {
        final int    index    = f.index(i, j);
        final double value    = parseDouble(field);
        final double oldValue = array[index];
        if (Double.isNaN(oldValue) || Precision.equals(oldValue, 0.0, 0)) {
            // the coefficient was not already initialized
            array[index] = value;
        } else {
            throw new OrekitException(OrekitMessages.DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                      name, i, j, name);
        }
    }

    /** Rescale coefficients arrays.
     * <p>
     * The normalized/unnormalized nature of original coefficients is inherited from previous parsing.
     * </p>
     * @param scale general scaling factor to apply to all elements
     * @param wantNormalized if true, the rescaled coefficients must be normalized,
     * otherwise they must be un-normalized
     * @param rescaledFlattener converter from triangular to flat form
     * @param originalFlattener converter from triangular to flat form
     * @param original original coefficients
     * @return rescaled coefficients
     * @since 11.1
     */
    protected double[] rescale(final double scale, final boolean wantNormalized, final Flattener rescaledFlattener,
                               final Flattener originalFlattener, final double[] original) {

        if (rescaledFlattener.getDegree() > originalFlattener.getDegree()) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD,
                                      rescaledFlattener.getDegree(), flattener.getDegree());
        }

        if (rescaledFlattener.getOrder() > originalFlattener.getOrder()) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD,
                                      rescaledFlattener.getOrder(), flattener.getOrder());
        }

        // scaling and normalization factors
        final FactorsGenerator generator;
        if (wantNormalized == normalized) {
            // the parsed coefficients already match the specified normalization
            generator = (n, m) -> scale;
        } else {
            // we need to normalize/unnormalize parsed coefficients
            final double[][] unnormalizationFactors =
                            GravityFieldFactory.getUnnormalizationFactors(rescaledFlattener.getDegree(),
                                                                          rescaledFlattener.getOrder());
            generator = wantNormalized ?
                (n, m) -> scale / unnormalizationFactors[n][m] :
                (n, m) -> scale * unnormalizationFactors[n][m];
        }

        // perform rescaling
        final double[] rescaled = buildFlatArray(rescaledFlattener, 0.0);
        for (int n = 0; n <= rescaledFlattener.getDegree(); ++n) {
            for (int m = 0; m <= FastMath.min(n, rescaledFlattener.getOrder()); ++m) {
                final int    rescaledndex  = rescaledFlattener.index(n, m);
                final int    originalndex  = originalFlattener.index(n, m);
                rescaled[rescaledndex] = original[originalndex] * generator.factor(n, m);
            }
        }

        return rescaled;

    }

    /** Rescale coefficients arrays.
     * <p>
     * The normalized/unnormalized nature of original coefficients is inherited from previous parsing.
     * </p>
     * @param wantNormalized if true, the rescaled coefficients must be normalized,
     * otherwise they must be un-normalized
     * @param rescaledFlattener converter from triangular to flat form
     * @param originalFlattener converter from triangular to flat form
     * @param original original coefficients
     * @return rescaled coefficients
     * @since 11.1
     */
    protected TimeDependentHarmonic[] rescale(final boolean wantNormalized, final Flattener rescaledFlattener,
                                              final Flattener originalFlattener, final TimeDependentHarmonic[] original) {

        if (rescaledFlattener.getDegree() > originalFlattener.getDegree()) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD,
                                      rescaledFlattener.getDegree(), flattener.getDegree());
        }

        if (rescaledFlattener.getOrder() > originalFlattener.getOrder()) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD,
                                      rescaledFlattener.getOrder(), flattener.getOrder());
        }

        // scaling and normalization factors
        final FactorsGenerator generator;
        if (wantNormalized == normalized) {
            // the parsed coefficients already match the specified normalization
            generator = (n, m) -> 1.0;
        } else {
            // we need to normalize/unnormalize parsed coefficients
            final double[][] unnormalizationFactors =
                            GravityFieldFactory.getUnnormalizationFactors(rescaledFlattener.getDegree(),
                                                                          rescaledFlattener.getOrder());
            generator = wantNormalized ?
                (n, m) -> 1.0 / unnormalizationFactors[n][m] :
                (n, m) -> unnormalizationFactors[n][m];
        }

        // perform rescaling
        final TimeDependentHarmonic[] rescaled = new TimeDependentHarmonic[rescaledFlattener.arraySize()];
        for (int n = 0; n <= rescaledFlattener.getDegree(); ++n) {
            for (int m = 0; m <= FastMath.min(n, rescaledFlattener.getOrder()); ++m) {
                final int originalndex = originalFlattener.index(n, m);
                if (original[originalndex] != null) {
                    final int    rescaledndex = rescaledFlattener.index(n, m);
                    final double factor       = generator.factor(n, m);
                    rescaled[rescaledndex]    = new TimeDependentHarmonic(factor, original[originalndex]);
                }
            }
        }

        return rescaled;

    }

    /**
     * Create a date from components. Assumes the time part is noon.
     *
     * @param components year, month, day.
     * @return date.
     */
    protected AbsoluteDate toDate(final DateComponents components) {
        return toDate(components, TimeComponents.H12);
    }

    /**
     * Create a date from components.
     *
     * @param dc dates components.
     * @param tc time components
     * @return date.
     * @since 11.1
     */
    protected AbsoluteDate toDate(final DateComponents dc, final TimeComponents tc) {
        return new AbsoluteDate(dc, tc, timeScale);
    }

    /** Generator for normalization/unnormalization factors.
     * @since 11.1
     */
    private interface FactorsGenerator {
        /** Generator the normalization/unnormalization factors.
         * @param n degree of the gravity field component
         * @param m order of the gravity field component
         * @return factor to apply to term
         */
        double factor(int n, int m);
    }

}
