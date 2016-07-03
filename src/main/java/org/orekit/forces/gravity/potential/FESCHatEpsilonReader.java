/* Copyright 2002-2016 CS Systèmes d'Information
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Reader for ocean tides files following the fes2004.dat format.
 * @since 6.1
 * @author Luc Maisonobe
 */
public class FESCHatEpsilonReader extends OceanTidesReader {

    /** Default pattern for fields with unknown type (non-space characters). */
    private static final String  UNKNOWN_TYPE_PATTERN = "\\S+";

    /** Pattern for fields with integer type. */
    private static final String  INTEGER_TYPE_PATTERN = "[-+]?\\p{Digit}+";

    /** Pattern for fields with real type. */
    private static final String  REAL_TYPE_PATTERN = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

    /** Pattern for fields with Doodson number. */
    private static final String  DOODSON_TYPE_PATTERN = "\\p{Digit}{2,3}[.,]\\p{Digit}{3}";

    /** Sea water fensity. */
    private static final double RHO   = 1025;

    /** Gravitational constant (from IERS 2010, chapter 1). */
    private static final double BIG_G = 6.67428e-11;

    /** Earth mean gravity AT EQUATOR (from IERS 2010, chapter 1). */
    private static final double GE    = 9.7803278;

    /** Scale of the CHat parameters. */
    private final double scaleCHat;

    /** Scale of the epsilon parameters. */
    private final double scaleEpsilon;

    /** Load deformation coefficients for ocean tides. */
    private final OceanLoadDeformationCoefficients oldc;

    /** Map for astronomical amplitudes. */
    private final Map<Integer, Double> astronomicalAmplitudes;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param scaleCHat scale of the CHat parameters
     * @param scaleEpsilon scale of the epsilon parameters
     * @param oldc load deformation coefficients for ocean tides
     * @param astronomicalAmplitudes map for astronomical amplitudes
     * @see AstronomicalAmplitudeReader#getAstronomicalAmplitudesMap()
     */
    public FESCHatEpsilonReader(final String supportedNames,
                                final double scaleCHat, final double scaleEpsilon,
                                final OceanLoadDeformationCoefficients oldc,
                                final Map<Integer, Double> astronomicalAmplitudes) {
        super(supportedNames);
        this.scaleCHat              = scaleCHat;
        this.scaleEpsilon           = scaleEpsilon;
        this.oldc                   = oldc;
        this.astronomicalAmplitudes = astronomicalAmplitudes;
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name)
        throws OrekitException, IOException {

        // FES ocean tides models have the following form:
        //   Ocean tide model: FES2004 normalized model (fev. 2004) up to (100,100) in cm
        //   (long period from FES2002 up to (50,50) + equilibrium Om1/Om2, atmospheric tide NOT included)
        //   Doodson Darw  n   m    Csin+     Ccos+       Csin-     Ccos-       C+   eps+      C-   eps-
        //    55.565 Om1   2   0 -0.540594  0.000000    0.000000  0.000000   0.5406 270.000 0.0000   0.000
        //    55.575 Om2   2   0 -0.005218  0.000000    0.000000  0.000000   0.0052 270.000 0.0000   0.000
        //    56.554 Sa    1   0  0.017233  0.000013    0.000000  0.000000   0.0172  89.957 0.0000   0.000
        //    56.554 Sa    2   0 -0.046604 -0.000903    0.000000  0.000000   0.0466 268.890 0.0000   0.000
        //    56.554 Sa    3   0 -0.000889  0.000049    0.000000  0.000000   0.0009 273.155 0.0000   0.000
        final String[] fieldsPatterns = new String[] {
            DOODSON_TYPE_PATTERN,
            UNKNOWN_TYPE_PATTERN,
            INTEGER_TYPE_PATTERN,
            INTEGER_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN
        };
        final StringBuilder builder = new StringBuilder("^\\p{Space}*");
        for (int i = 0; i < fieldsPatterns.length; ++i) {
            builder.append("(");
            builder.append(fieldsPatterns[i]);
            builder.append(")");
            builder.append((i < fieldsPatterns.length - 1) ? "\\p{Space}+" : "\\p{Space}*$");
        }
        final Pattern regularLinePattern = Pattern.compile(builder.toString());

        final double commonFactor = 4 * FastMath.PI * BIG_G * RHO / GE;
        final double[] kPrime = oldc.getCoefficients();

        // parse the file
        startParse(name);
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        int lineNumber      = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            ++lineNumber;
            final Matcher regularMatcher = regularLinePattern.matcher(line);
            if (regularMatcher.matches()) {
                // we have found a regular data line

                // parse fields
                final int doodson = Integer.parseInt(regularMatcher.group(1).replaceAll("[.,]", ""));
                final int n       = Integer.parseInt(regularMatcher.group(3));
                final int m       = Integer.parseInt(regularMatcher.group(4));

                if (canAdd(n, m)) {

                    final double cHatPlus  = scaleCHat    * Double.parseDouble(regularMatcher.group(9));
                    final double ePlus     = scaleEpsilon * Double.parseDouble(regularMatcher.group(10));
                    final double cHatMinus = scaleCHat    * Double.parseDouble(regularMatcher.group(11));
                    final double eMinus    = scaleEpsilon * Double.parseDouble(regularMatcher.group(12));

                    // compute bias from table 6.6
                    final double hf = astronomicalAmplitudes.containsKey(doodson) ? astronomicalAmplitudes.get(doodson) : 0.0;
                    final int cGamma = doodson / 100000;
                    final double chiF;
                    if (cGamma == 0) {
                        chiF = hf > 0 ? FastMath.PI : 0.0;
                    } else if (cGamma == 1) {
                        chiF = hf > 0 ? 0.5 * FastMath.PI : -0.5 * FastMath.PI;
                    } else if (cGamma == 2) {
                        chiF = hf > 0 ? 0.0 : FastMath.PI;
                    } else {
                        chiF = 0;
                    }

                    // compute reference gravity coefficients by converting height coefficients
                    // IERS conventions 2010, equation 6.21
                    if (n >= kPrime.length) {
                        throw new OrekitException(OrekitMessages.OCEAN_TIDE_LOAD_DEFORMATION_LIMITS,
                                                  kPrime.length - 1, n, name);
                    }
                    final double termFactor = (1 + kPrime[n]) / (2 * n + 1);

                    // an update on IERS conventions from 2012-08-10 states that for FES model:
                    //      Note that, for zonal terms, FES2004 takes the approach to set
                    //      the retrograde coefficients C-f,nO and S-f,n0 to zero and to double
                    //      the prograde coefficients C+f,nO and S+f,n0. Therefore, after
                    //      applying Equation (6.15), the ΔCn0 have the expected value but the
                    //      ΔSn0 must be set to zero.
                    // (see ftp://tai.bipm.org/iers/convupdt/chapter6/icc6.pdf)
                    final double cPlus  =                  commonFactor * termFactor * cHatPlus  * FastMath.sin(ePlus  + chiF);
                    final double sPlus  =                  commonFactor * termFactor * cHatPlus  * FastMath.cos(ePlus  + chiF);
                    final double cMinus = (m == 0) ? 0.0 : commonFactor * termFactor * cHatMinus * FastMath.sin(eMinus + chiF);
                    final double sMinus = (m == 0) ? 0.0 : commonFactor * termFactor * cHatMinus * FastMath.cos(eMinus + chiF);

                    // store parsed fields
                    addWaveCoefficients(doodson, n, m, cPlus,  sPlus, cMinus, sMinus, lineNumber, line);

                }

            }
        }
        endParse();

    }

}
