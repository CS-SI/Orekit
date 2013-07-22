/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.forces.gravity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Gravity field corresponding to tides.
 * <p>
 * This solid tides force model implementation corresponds to the method described
 * in <a href="http://www.iers.org/nn_11216/IERS/EN/Publications/TechnicalNotes/tn36.html">
 * IERS conventions (2010)</a>, chapter 6, section 6.2.
 * </p>
 * <p>
 * The computation of the spherical harmonics part is done using the algorithm implemented
 * designed by S. A. Holmes and W. E. Featherstone from Department of Spatial Sciences,
 * Curtin University of Technology, Perth, Australia and described in their 2002 paper:
 * <a href="http://cct.gfy.ku.dk/publ_others/ccta1870.pdf">A unified approach to
 * the Clenshaw summation and the recursive computation of very high degree and
 * order normalised associated Legendre functions</a> (Journal of Geodesy (2002)
 * 76: 279–299).
 * </p>
 * @see SolidTides
 * @author Luc Maisonobe
 */
class TidesField implements NormalizedSphericalHarmonicsProvider {

    /** Minimum degree for Love numbers. */
    private static final int MIN_LOVE_DEGREE = 2;

    /** Maximum degree for Love numbers. */
    private static final int MAX_LOVE_DEGREE = 3;

    /** Maximum degree for normalized Legendre functions. */
    private static final int MAX_LEGENDRE_DEGREE = 4;

    /** Encoding for IERS tables loading. */
    private static final String IERS_ENCODING = "UTF-8";

    /** Real part of the nominal Love numbers. */
    private final double[][] loveReal;

    /** Imaginary part of the nominal Love numbers. */
    private final double[][] loveImaginary;

    /** Time-dependent part of the Love numbers. */
    private final double[][] lovePlus;

    /** Frequency dependence corrections for k20. */
    private final List<FrequencyDependence> frequencyDependenceK20;

    /** Frequency dependence corrections for k21. */
    private final List<FrequencyDependence> frequencyDependenceK21;

    /** Frequency dependence corrections for k22. */
    private final List<FrequencyDependence> frequencyDependenceK22;

    /** Rotating body frame. */
    private final Frame centralBodyFrame;

    /** Central body reference radius. */
    private final double ae;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Tide system used in the central attraction model. */
    private final TideSystem centralTideSystem;

    /** Tide generating bodies (typically Sun and Moon). */
    private final CelestialBody[] bodies;

    /** Date offset of cached coefficients. */
    private double cachedOffset;

    /** Cached cnm. */
    private final double[][] cachedCnm;

    /** Cached snm. */
    private final double[][] cachedSnm;

    /** Tesseral terms. */
    private final double[][] pnm;

    /** First recursion coefficients for tesseral terms. */
    private final double[][] anm;

    /** Second recursion coefficients for tesseral terms. */
    private final double[][] bnm;

    /** Third recursion coefficients for sectorial terms. */
    private final double[] dmm;

    /** Simple constructor.
     * @param nameLove name of the Love number resource
     * @param nameK20 name of the k20 frequency dependence resource
     * @param nameK21 name of the k21 frequency dependence resource
     * @param nameK22 name of the k22 frequency dependence resource
     * @param centralBodyFrame rotating body frame
     * @param ae central body reference radius
     * @param mu central body attraction coefficient
     * @param centralTideSystem tide system used in the central attraction model
     * @param bodies tide generating bodies (typically Sun and Moon)
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     */
    public TidesField(final String nameLove, final String nameK20, final String nameK21, final String nameK22,
                      final Frame centralBodyFrame,
                      final double ae, final double mu, final TideSystem centralTideSystem,
                      final CelestialBody ... bodies)
        throws OrekitException {

        // store mode parameters
        this.centralBodyFrame  = centralBodyFrame;
        this.ae                = ae;
        this.mu                = mu;
        this.centralTideSystem = centralTideSystem;
        this.bodies            = bodies;

        // compute recursion coefficients for Legendre functions
        this.pnm               = buildTriangularArray(MAX_LOVE_DEGREE, true);
        this.anm               = buildTriangularArray(MAX_LOVE_DEGREE, false);
        this.bnm               = buildTriangularArray(MAX_LOVE_DEGREE, false);
        this.dmm               = new double[MAX_LOVE_DEGREE + 1];
        recursionCoefficients();

        // prepare coefficients caching
        this.cachedOffset      = Double.NaN;
        this.cachedCnm         = buildTriangularArray(MAX_LOVE_DEGREE, true);
        this.cachedSnm         = buildTriangularArray(MAX_LOVE_DEGREE, true);

        // load Love numbers
        this.loveReal          = buildTriangularArray(MAX_LOVE_DEGREE, true);
        this.loveImaginary     = buildTriangularArray(MAX_LOVE_DEGREE, true);
        this.lovePlus          = buildTriangularArray(MAX_LOVE_DEGREE, true);
        loadLoveNumbers(nameLove);

        // header coefficients lines have the form:
        // Scale   Amp  1.0e-12
        final String  initialBlanks            = "^\\s*";
        final String  integerRegexp            = "\\s+([-+]?\\d+)";
        final String  realRegexp               = "\\s+([-+]?(?:\\d*(?:\\.\\d*)?|\\.\\d+?)(?:[eE][-+]?\\d+)?)";
        final String  trailingBlanks           = "\\s*$";
        final Pattern scaleAmpPattern          = Pattern.compile(initialBlanks + "Scale\\s+Amp"       + realRegexp + trailingBlanks);

        final String nameRegexp                = "[^\\s]*";
        final String doodsonNumberRegexp       = "\\s+[-+]?(\\d{2,3}),(\\d{3})";
        final String doodsonMultipliersRegexp  = integerRegexp + integerRegexp + integerRegexp + integerRegexp + integerRegexp + integerRegexp;
        final String delaunayMultipliersRegexp = integerRegexp + integerRegexp + integerRegexp + integerRegexp + integerRegexp;

        // k20 model table has Doodson number column before deg/hr column,
        // both real and imaginary parts, delta and amplitude intermixed
        // Name   Doodson  deg/hr  τ s  h  p N' ps  l l'  F  D  Ω    δkfR  Amp.    δkfI   Amp.
        //          No.                                                    (ip)           (op)
        //        55,565   0.00221 0 0  0  0  1  0  0  0  0  0  1  0.01347 16.6 -0.00541 -6.7
        //        55,575   0.00441 0 0  0  0  2  0  0  0  0  0  2  0.01124 -0.1 -0.00488  0.1
        // Sa     56,554   0.04107 0 0  1  0  0 -1  0 -1  0  0  0  0.00547 -1.2 -0.00349  0.8
        // Ssa    57,555   0.08214 0 0  2  0  0  0  0  0 -2  2 -2  0.00403 -5.5 -0.00315  4.3
        this.frequencyDependenceK20 = loadFrequencyDependenceModel(nameK20, scaleAmpPattern,
                                                                   Pattern.compile(initialBlanks +
                                                                                   nameRegexp + doodsonNumberRegexp + realRegexp +
                                                                                   doodsonMultipliersRegexp + delaunayMultipliersRegexp +
                                                                                   realRegexp + realRegexp + realRegexp + realRegexp +
                                                                                   trailingBlanks),
                                                                   1, 4, 10, 16, 18);

        // k21 model table has Doodson number column after deg/hr column,,
        // both real and imaginary parts, delta and amplitude separated
        // Name   deg/hr    Doodson  τ  s  h  p  N' ps   l  l' F  D  Ω  δkfR  δkfI     Amp.    Amp.
        //                    No.                                       /10−5 /10−5    (ip)    (op)
        //   2Q1 12.85429   125,755  1 -3  0  2   0  0   2  0  2  0  2    -29     3    -0.1     0.0
        //    σ1 12.92714   127,555  1 -3  2  0   0  0   0  0  2  2  2    -30     3    -0.1     0.0
        //       13.39645   135,645  1 -2  0  1  -1  0   1  0  2  0  1    -45     5    -0.1     0.0
        //    Q1 13.39866   135,655  1 -2  0  1   0  0   1  0  2  0  2    -46     5    -0.7     0.1
        //    ρ1 13.47151   137,455  1 -2  2 -1   0  0  -1  0  2  2  2    -49     5    -0.1     0.0
        this.frequencyDependenceK21 = loadFrequencyDependenceModel(nameK21, scaleAmpPattern,
                                                                   Pattern.compile(initialBlanks +
                                                                                   nameRegexp + realRegexp + doodsonNumberRegexp +
                                                                                   doodsonMultipliersRegexp + delaunayMultipliersRegexp +
                                                                                   realRegexp + realRegexp + realRegexp + realRegexp +
                                                                                   trailingBlanks),
                                                                   2, 4, 10, 17, 18);
        // k22 model table has Doodson number column before deg/hr column,
        // only real correction and therefore neither mixing nor separation of real/imaginary
        // Name  Doodson  deg/hr  τ  s h p N' ps l l' F D Ω   δkfR    Amp.
        //          No.
        // N2    245,655 28.43973 2 -1 0 1 0   0 1 0  2 0 2 0.00006  -0.3
        // M2    255,555 28.98410 2  0 0 0 0   0 0 0  2 0 2 0.00004  -1.2
        this.frequencyDependenceK22 = loadFrequencyDependenceModel(nameK22, scaleAmpPattern,
                                                                   Pattern.compile(initialBlanks +
                                                                                   nameRegexp + doodsonNumberRegexp + realRegexp +
                                                                                   doodsonMultipliersRegexp + delaunayMultipliersRegexp +
                                                                                   realRegexp + realRegexp +
                                                                                   trailingBlanks),
                                                                   1, 4, 10, 16, -1);

    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree() {
        return MAX_LEGENDRE_DEGREE;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder() {
        return MAX_LEGENDRE_DEGREE;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return mu;
    }

    /** {@inheritDoc} */
    @Override
    public double getAe() {
        return ae;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getReferenceDate() {
        return AbsoluteDate.J2000_EPOCH;
    }

    /** {@inheritDoc} */
    @Override
    public double getOffset(final AbsoluteDate date) {
        return date.durationFrom(AbsoluteDate.J2000_EPOCH);
    }

    /** {@inheritDoc} */
    @Override
    public TideSystem getTideSystem() {
        // not really used here, but for consistency we can state that either
        // we add the permanent tide or it was already in the central attraction
        return TideSystem.ZERO_TIDE;
    }

    /** {@inheritDoc} */
    @Override
    public double getNormalizedCnm(final double dateOffset, final int n, final int m)
        throws OrekitException {
        if (!Precision.equals(dateOffset, cachedOffset, 1)) {
            fillCache(dateOffset);
        }
        return cachedCnm[n][m];
    }

    /** {@inheritDoc} */
    @Override
    public double getNormalizedSnm(final double dateOffset, final int n, final int m)
        throws OrekitException {
        if (!Precision.equals(dateOffset, cachedOffset, 1)) {
            fillCache(dateOffset);
        }
        return cachedSnm[n][m];
    }

    /** Fill the cache.
     * @param offset date offset from J2000.0
     * @exception OrekitException if coefficients cannot be computed
     */
    private void fillCache(final double offset) throws OrekitException {

        cachedOffset = offset;
        for (int i = 0; i < cachedCnm.length; ++i) {
            Arrays.fill(cachedCnm[i], 0.0);
            Arrays.fill(cachedSnm[i], 0.0);
        }

        final AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, offset);

        // step 1: frequency independent part
        // equations 6.6 (for degrees 2 and 3) and 6.7 (for degree 4) in IERS conventions 2010
        for (final CelestialBody body : bodies) {

            // compute tide generating body state
            final Vector3D position = body.getPVCoordinates(date, centralBodyFrame).getPosition();

            // compute polar coordinates
            final double x    = position.getX();
            final double y    = position.getY();
            final double z    = position.getZ();
            final double x2   = x * x;
            final double y2   = y * y;
            final double z2   = z * z;
            final double r2   = x2 + y2 + z2;
            final double r    = FastMath.sqrt (r2);
            final double rho2 = x2 + y2;
            final double rho  = FastMath.sqrt(rho2);

            // evaluate Pnm
            evaluateLegendre(z / r, rho / r);

            // update spherical harmonic coefficients
            frequencyIndependentPart(r, body.getGM(), x / rho, y / rho);

        }

        // step 2: frequency dependent corrections
        frequencyDependentPart();

        if (centralTideSystem == TideSystem.ZERO_TIDE) {
            // step 3: remove permanent tide which is already considered
            // in the central body gravity field
            removePermanentTide();
        }

    }

    /** Load the Love numbers.
     * @param nameLove name of the Love number resource
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     */
    private void loadLoveNumbers(final String nameLove) throws OrekitException {
        InputStream stream = null;
        BufferedReader reader = null;
        try {

            stream = TidesField.class.getResourceAsStream(nameLove);
            if (stream == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, nameLove);
            }

            // setup the reader
            reader = new BufferedReader(new InputStreamReader(stream, IERS_ENCODING));
            String line = reader.readLine();
            int lineNumber = 1;

            // look for the Love numbers
            while (line != null) {

                line = line.trim();
                if (!(line.isEmpty() || line.startsWith("#"))) {
                    final String[] fields = line.split("\\p{Space}+");
                    if (fields.length != 5) {
                        // this should never happen with files embedded within Orekit
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, nameLove, line);
                    }
                    final int n = Integer.parseInt(fields[0]);
                    final int m = Integer.parseInt(fields[1]);
                    if ((n < MIN_LOVE_DEGREE) || (n > MAX_LOVE_DEGREE) || (m < 0) || (m > n)) {
                        // this should never happen with files embedded within Orekit
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, nameLove, line);

                    }
                    loveReal[n][m]      = Double.parseDouble(fields[2]);
                    loveImaginary[n][m] = Double.parseDouble(fields[3]);
                    lovePlus[n][m]      = Double.parseDouble(fields[4]);
                }

                // next line
                lineNumber++;
                line = reader.readLine();

            }

        } catch (IOException ioe) {
            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, nameLove);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                // ignored here
            }
        }
    }

    /** Load the frequency dependence models.
     * @param nameKnm name of the knm frequency dependence resource
     * @param scaleAmpPattern pattern for the scale parameter of amplitude columns
     * @param wavePattern patern for the tide waves lines
     * @param doodsonNumberGroup group containing the Doodson number
     * @param doodsonMultipliersGroup group containing the first Doodson argument
     * @param delaunayMultipliersGroup group containing the first Delaunay argument
     * @param inPhaseAmplitudeGroup group containing the in-phase amplitude
     * @param outOfPhaseAmplitudeGroup group containing the out-of-phase amplitude (negative if not present)
     * @return list of corrections for each tide wave in the table
     * @exception OrekitException if the frequency dependence embedded in the
     * library cannot be read
     */
    private List<FrequencyDependence> loadFrequencyDependenceModel(final String nameKnm,
                                                                   final Pattern scaleAmpPattern,
                                                                   final Pattern wavePattern,
                                                                   final int doodsonNumberGroup,
                                                                   final int doodsonMultipliersGroup,
                                                                   final int delaunayMultipliersGroup,
                                                                   final int inPhaseAmplitudeGroup,
                                                                   final int outOfPhaseAmplitudeGroup)
        throws OrekitException {

        InputStream stream = null;
        BufferedReader reader = null;
        try {

            stream = TidesField.class.getResourceAsStream(nameKnm);
            if (stream == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, nameKnm);
            }

            final List<FrequencyDependence> corrections = new ArrayList<TidesField.FrequencyDependence>();

            // setup the reader
            reader = new BufferedReader(new InputStreamReader(stream, IERS_ENCODING));

            // parse data
            int lineNumber = 0;
            double scaleAmplitude = Double.NaN;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {

                ++lineNumber;

                Matcher matcher = scaleAmpPattern.matcher(line);
                if (matcher.matches()) {

                    // we have found the scale factor for amplitude columns
                    scaleAmplitude = Double.parseDouble(matcher.group(1));

                } else {

                    matcher = wavePattern.matcher(line);
                    if (matcher.matches()) {

                        // when we parse wave data, the scale must have been parsed already
                        if (Double.isNaN(scaleAmplitude)) {
                            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, nameKnm);
                        }

                        // get Doodson number
                        final int doodsonNumber = Integer.parseInt(matcher.group(doodsonNumberGroup) +
                                                                   matcher.group(doodsonNumberGroup + 1));

                        // reconstruct Doodson number from Doodson arguments, for checking purpose
                        final int tauFactor    = Integer.parseInt(matcher.group(doodsonMultipliersGroup));
                        final int sFactor      = Integer.parseInt(matcher.group(doodsonMultipliersGroup + 1));
                        final int hFactor      = Integer.parseInt(matcher.group(doodsonMultipliersGroup + 2));
                        final int pFactor      = Integer.parseInt(matcher.group(doodsonMultipliersGroup + 3));
                        final int nPrimeFactor = Integer.parseInt(matcher.group(doodsonMultipliersGroup + 4));
                        final int psFactor     = Integer.parseInt(matcher.group(doodsonMultipliersGroup + 5));
                        final int doodsonNumberCheck = ((((tauFactor * 10 +
                                (sFactor + 5)) * 10 +
                                (hFactor + 5)) * 10 +
                                (pFactor + 5)) * 10 +
                                (nPrimeFactor + 5)) * 10 +
                                (psFactor + 5);

                        // check consistency of Doodson and Delaunay arguments
                        final int lFactor      = Integer.parseInt(matcher.group(delaunayMultipliersGroup));
                        final int lPrimeFactor = Integer.parseInt(matcher.group(delaunayMultipliersGroup + 1));
                        final int fFactor      = Integer.parseInt(matcher.group(delaunayMultipliersGroup + 2));
                        final int dFactor      = Integer.parseInt(matcher.group(delaunayMultipliersGroup + 3));
                        final int omegaFactor  = Integer.parseInt(matcher.group(delaunayMultipliersGroup + 4));

                        // check consistency of all arguments
                        boolean ok = doodsonNumber == doodsonNumberCheck;
                        ok = ok && (lFactor      ==                                 pFactor);
                        ok = ok && (lPrimeFactor ==                                                          psFactor);
                        ok = ok && (fFactor      == tauFactor - sFactor - hFactor - pFactor                - psFactor);
                        ok = ok && (dFactor      ==                       hFactor                          + psFactor);
                        ok = ok && (omegaFactor  == tauFactor - sFactor - hFactor - pFactor + nPrimeFactor - psFactor);
                        if (!ok) {
                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber, nameKnm, line);
                        }

                        // parse the final coluns data
                        final double inPhaseAmplitude    = Double.parseDouble(matcher.group(inPhaseAmplitudeGroup));
                        final double outOfPhaseAmplitude = outOfPhaseAmplitudeGroup < 0 ? 0.0 : Double.parseDouble(matcher.group(outOfPhaseAmplitudeGroup));

                        // store the table entry
                        corrections.add(new FrequencyDependence(lFactor, lPrimeFactor, fFactor, dFactor, omegaFactor,
                                                                scaleAmplitude * inPhaseAmplitude,
                                                                scaleAmplitude * outOfPhaseAmplitude));

                    }
                }
            }


            return corrections;

        } catch (IOException ioe) {
            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, nameKnm);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                // ignored here
            }
        }

    }

    /** Compute recursion coefficients.
     */
    private void recursionCoefficients() {

        // pre-compute the recursion coefficients
        // (see equations 11 and 12 from Holmes and Featherstone paper)
        for (int n = 0; n < anm.length; ++n) {
            for (int m = 0; m < n; ++m) {
                anm[n][m] = FastMath.sqrt((2 * n - 1) * (2 * n + 1) /
                                          ((n - m) * (n + m)));
                bnm[n][m] = FastMath.sqrt((2 * n + 1) * (n + m - 1) * (n - m - 1) /
                                          ((n - m) * (n + m) * (2 * n - 3)));
            }
        }

        // sectorial terms corresponding to equation 13 in Holmes and Featherstone paper
        dmm[0] = Double.NaN; // dummy initialization for unused term
        dmm[1] = Double.NaN; // dummy initialization for unused term
        for (int m = 2; m < dmm.length; ++m) {
            dmm[m] = FastMath.sqrt((2 * m + 1) / (2.0 * m));
        }

    }

    /** Evaluate Legendre functions.
     * @param t cos(&theta;), where &theta; is the polar angle
     * @param u sin(&theta;), where &theta; is the polar angle
     */
    private void evaluateLegendre(final double t, final double u) {

        // as the degree is very low, we use the standard forward column method
        // and store everything (see equations 11 and 13 from Holmes and Featherstone paper)
        pnm[0][0] = 1;
        pnm[1][0] = anm[1][0] * t;
        pnm[1][1] = FastMath.sqrt(3) * u;
        for (int m = 2; m < dmm.length; ++m) {
            pnm[m][m - 1] = anm[m][m - 1] * t * pnm[m - 1][m - 1];
            pnm[m][m]     = dmm[m] * u * pnm[m - 1][m - 1];
        }
        for (int m = 0; m < dmm.length; ++m) {
            for (int n = m + 2; n < pnm.length; ++n) {
                pnm[n][m] = anm[n][m] * t * pnm[n - 1][m] - bnm[n][m] * pnm[n - 2][m];
            }
        }

    }

    /** Update coefficients applying frequency independent step, for one tide generating body.
     * @param r distance to tide generating body
     * @param gm tide generating body attraction coefficient
     * @param cosLambda cosine of the tide generating body longitude
     * @param sinLambda sine of the tide generating body longitude
     */
    private void frequencyIndependentPart(final double r, final double gm,
                                          final double cosLambda, final double sinLambda) {

        final double rRatio = ae / r;
        double fM           = gm / mu;
        double cosMLambda   = 1;
        double sinMLambda   = 0;
        for (int m = 0; m <= loveReal.length; ++m) {

            double fNPlus1 = fM;
            for (int n = m; n <= loveReal.length; ++n) {
                fNPlus1 *= rRatio;
                final double coeff = (fNPlus1 / (2 * n + 1)) * pnm[n][m];

                // direct effect of degree n tides on degree n coefficients
                // equation 6.6 from IERS conventions (2010)
                cachedCnm[n][m] += coeff * (loveReal[n][m] * cosMLambda + loveImaginary[n][m] * sinMLambda);
                cachedSnm[n][m] += coeff * (loveReal[n][m] * sinMLambda - loveImaginary[n][m] * cosMLambda);

                if (n == 2) {
                    // indirect effect of degree 2 tides on degree 4 coefficients
                    // equation 6.7 from IERS conventions (2010)
                    cachedCnm[4][m] += coeff * lovePlus[2][m] * cosMLambda;
                    cachedSnm[4][m] += coeff * lovePlus[2][m] * sinMLambda;
                }

            }

            // prepare next iteration on order
            final double tmp = cosMLambda * cosLambda - sinMLambda * sinLambda;
            sinMLambda = sinMLambda * cosLambda + cosMLambda * sinLambda;
            cosMLambda = tmp;
            fM        *= rRatio;

        }

    }

    /** Update coefficients applying frequency dependent step.
     */
    private void frequencyDependentPart() {
        // TODO implement equations 6.8a and 6.8b in IERS conventions 2010
    }

    /** Remove the permanent tide already counted in zero-tide central gravity fields.
     */
    private void removePermanentTide() {
        // TODO implement equations 6.13 and 6.14 in IERS conventions 2010
    }

    /** Create a triangular array.
     * @param maxDegree maximum degree
     * @param withDiagonal if true, the array contains a[p][p] terms, otherwise each
     * row ends up at a[p][p-1]
     * @return new triangular array
     */
    private double[][] buildTriangularArray(final int maxDegree, final boolean withDiagonal) {
        final double[][] array = new double[maxDegree + 1][];
        for (int i = 0; i < array.length; ++i) {
            array[i] = new double[withDiagonal ? i + 1 : i];
        }
        return array;
    }

    /** Local class for frequency-dependent corrections. */
    private static class FrequencyDependence {

        /** Angular factor for mean anomaly of the Moon. */
        private final int lFactor;

        /** Angular factor for mean anomaly of the Sun. */
        private final int lPrimeFactor;

        /** Angular factor for L - &Omega; where L is the mean longitude of the Moon. */
        private final int fFactor;

        /** Angular factor for mean elongation of the Moon from the Sun. */
        private final int dFactor;

        /** Angular factor for mean longitude of the ascending node of the Moon. */
        private final int omegaFactor;

        /** In-phase amplitude. */
        private final double inPhaseAmplitude;

        /** Out-of-phase amplitude. */
        private final double outOfPhaseAmplitude;

        /** Simple constructor.
         * @param lFactor angular factor for mean anomaly of the Moon
         * @param lPrimeFactor angular factor for mean anomaly of the Sun
         * @param fFactor angular factor for L - &Omega; where L is the mean longitude of the Moon
         * @param dFactor angular factor for mean elongation of the Moon from the Sun
         * @param omegaFactor angular factor for mean longitude of the ascending node of the Moon
         * @param inPhaseAmplitude in-phase amplitude
         * @param outOfPhaseAmplitude out-of-phase amplitude
         */
        public FrequencyDependence(final int lFactor, final int lPrimeFactor, final int fFactor,
                                   final int dFactor, final int omegaFactor,
                                   final double inPhaseAmplitude, final double outOfPhaseAmplitude) {
            this.lFactor             = lFactor;
            this.lPrimeFactor        = lPrimeFactor;
            this.fFactor             = fFactor;
            this.dFactor             = dFactor;
            this.omegaFactor         = omegaFactor;
            this.inPhaseAmplitude    = inPhaseAmplitude;
            this.outOfPhaseAmplitude = outOfPhaseAmplitude;
        }

    }

}
