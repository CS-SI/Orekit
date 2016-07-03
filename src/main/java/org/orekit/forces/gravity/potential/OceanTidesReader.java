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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Reader for ocean tides coefficients.
 * @author Luc Maisonobe
 * @see OceanTidesWave
 * @since 6.1
 */
public abstract class OceanTidesReader implements DataLoader {

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Maximal degree to parse. */
    private int maxParseDegree;

    /** Maximal order to parse. */
    private int maxParseOrder;

    /** Loaded waves. */
    private List<OceanTidesWave> waves;

    /** Name name of the parsed file (or zip entry). */
    private String name;

    /** Triangular arrays to hold all coefficients. */
    private Map<Integer, double[][][]> coefficients;

    /** Max degree encountered up to now. */
    private int maxDegree;

    /** Max order encountered up to now. */
    private int maxOrder;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     */
    public OceanTidesReader(final String supportedNames) {
        this.supportedNames = supportedNames;
        this.maxParseDegree = Integer.MAX_VALUE;
        this.maxParseOrder  = Integer.MAX_VALUE;
        this.waves          = new ArrayList<OceanTidesWave>();
    }

    /** Get the regular expression for supported files names.
     * @return regular expression for supported files names
     */
    public String getSupportedNames() {
        return supportedNames;
    }

    /** Set the degree limit for the next file parsing.
     * @param maxParseDegree maximal degree to parse (may be safely
     * set to {@link Integer#MAX_VALUE} to parse all available coefficients)
     */
    public void setMaxParseDegree(final int maxParseDegree) {
        this.maxParseDegree = maxParseDegree;
    }

    /** Get the degree limit for the next file parsing.
     * @return degree limit for the next file parsing
     */
    public int getMaxParseDegree() {
        return maxParseDegree;
    }

    /** Set the order limit for the next file parsing.
     * @param maxParseOrder maximal order to parse (may be safely
     * set to {@link Integer#MAX_VALUE} to parse all available coefficients)
     */
    public void setMaxParseOrder(final int maxParseOrder) {
        this.maxParseOrder = maxParseOrder;
    }

    /** Get the order limit for the next file parsing.
     * @return order limit for the next file parsing
     */
    public int getMaxParseOrder() {
        return maxParseOrder;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return waves.isEmpty();
    }

    /** Start parsing.
     * <p>
     * This method must be called by subclasses when they start parsing a file
     * </p>
     * @param fileName name of the file (or zip entry)
     */
    protected void startParse(final String fileName) {

        this.name         = fileName;
        this.coefficients = new HashMap<Integer, double[][][]>();
        this.maxDegree    = -1;
        this.maxOrder     = -1;
    }

    /** Check if coefficients can be added.
     * @param n degree of the coefficients
     * @param m order of the coefficients
     * @return true if coefficients can be added
     */
    public boolean canAdd(final int n, final int m) {
        maxDegree = FastMath.max(maxDegree, n);
        maxOrder  = FastMath.max(maxOrder,  m);
        return n <= getMaxParseDegree() && m <= getMaxParseOrder();
    }

    /** Add parsed coefficients.
     * @param doodson Doodson number of the current wave
     * @param n degree of the coefficients
     * @param m order of the coefficients
     * @param cPlus  C+(n,m)
     * @param sPlus  S+(n,m)
     * @param cMinus C-(n,m)
     * @param sMinus S-(n,m)
     * @param lineNumber number of the parsed line
     * @param line text of the line
     * @exception OrekitException if coefficients for waves are interleaved
     */
    protected void addWaveCoefficients(final int doodson, final int n, final int m,
                                       final double cPlus, final double sPlus,
                                       final double cMinus, final double sMinus,
                                       final int lineNumber, final String line)
        throws OrekitException {

        if (!coefficients.containsKey(doodson)) {
            // prepare the triangular array to hold coefficients
            final double[][][] array = new double[getMaxParseDegree() + 1][][];
            for (int i = 0; i <= getMaxParseDegree(); ++i) {
                array[i] = new double[FastMath.min(i, getMaxParseOrder()) + 1][4];
                for (double[] a : array[i]) {
                    Arrays.fill(a, Double.NaN);
                }
            }
            coefficients.put(doodson, array);
        }

        // store the fields
        final double[] cs = coefficients.get(doodson)[n][m];
        cs[0] = cPlus;
        cs[1] = sPlus;
        cs[2] = cMinus;
        cs[3] = sMinus;

    }

    /** End parsing.
     * <p>
     * This method must be called by subclasses when they end parsing a file
     * </p>
     * @exception OrekitException if expected degree and order were not met
     */
    protected void endParse() throws OrekitException {

        // check requested degree and order
        if (maxDegree < getMaxParseDegree() || maxOrder < getMaxParseOrder()) {
            throw new OrekitException(OrekitMessages.OCEAN_TIDE_DATA_DEGREE_ORDER_LIMITS,
                                      name, maxDegree, maxOrder);
        }

        for (final Map.Entry<Integer, double[][][]> entry : coefficients.entrySet()) {

            // check wave degree and order
            int waveDegree = -1;
            int waveOrder  = -1;
            for (int i = 0; i < entry.getValue().length; ++i) {
                for (int j = 0; j < entry.getValue()[i].length; ++j) {
                    if (!Double.isNaN(entry.getValue()[i][j][0])) {
                        waveDegree = FastMath.max(waveDegree, i);
                        waveOrder  = FastMath.max(waveOrder,  j);
                    }
                }
            }

            // create wave
            waves.add(new OceanTidesWave(entry.getKey(), waveDegree, waveOrder, entry.getValue()));

        }

    }

    /** Get the loaded waves.
     * @return loaded waves
     */
    public List<OceanTidesWave> getWaves() {
        return waves;
    }

}
