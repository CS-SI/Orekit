/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.navigation;

import org.orekit.gnss.SatelliteSystem;

/** Container for data contained in a ionosphere Klobuchar message.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class IonosphereNavICKlobucharMessage extends IonosphereBaseMessage {

    /** Issue Of Data. */
    private int iod;

    /** α (s/radⁿ). */
    private final double[] alpha;

    /** β (s/radⁿ). */
    private final double[] beta;

    /** Longitude min. */
    private double lonMin;

    /** Longitude max. */
    private double lonMax;

    /** MODIP min. */
    private double modipMin;

    /** MODIP max. */
    private double modipMax;

    /** Simple constructor.
     * @param system satellite system
     * @param prn satellite number
     * @param navigationMessageType navigation message type
     * @param subType message subtype
     */
    public IonosphereNavICKlobucharMessage(final SatelliteSystem system, final int prn,
                                           final String navigationMessageType, final String subType) {
        super(system, prn, navigationMessageType, subType);
        alpha = new double[4];
        beta  = new double[4];
    }

    /** Get Issue Of Data (IOD).
     * @return  Issue Of Data
     */
    public int getIOD() {
        return iod;
    }

    /** Set Issue Of Data.
     * @param iod Issue Of Data
     */
    public void setIOD(final double iod) {
        // The value is given as a floating number in the navigation message
        this.iod = (int) iod;
    }

    /** Get the α coefficients.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to retrieve the more traditional s/semi-circleⁿ, use
     * {@code IonosphereKlobucharMessage.S_PER_SC_N[i].fromSI(alpha[i])}
     * </p>
     * @return α coefficients (s/radⁿ)
     * @see #S_PER_SC_N
     */
    public double[] getAlpha() {
        return alpha.clone();
    }

    /** Set one α coefficient.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to use the more traditional s/semi-circleⁿ, use
     * {@code setAlphaI(i, IonosphereKlobucharMessage.S_PER_SC_N[i].toSi(alpha[i]))}
     * </p>
     * @param i index of the coefficient
     * @param alphaI α coefficient to set (s/radⁿ)
     * @see #S_PER_SC_N
     */
    public void setAlphaI(final int i, final double alphaI) {
        alpha[i] = alphaI;
    }

    /** Get the β coefficients.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to retrieve the more traditional s/semi-circleⁿ, use
     * {@code IonosphereKlobucharMessage.S_PER_SC_N[i].fromSI(beta[i])}
     * </p>
     * @return β coefficients (s/radⁿ)
     * @see #S_PER_SC_N
     */
    public double[] getBeta() {
        return beta.clone();
    }

    /** Set one β coefficient.
     * <p>
     * Beware Orekit uses SI units here.
     * In order to use the more traditional s/semi-circleⁿ, use
     * {@code setBetaI(i, IonosphereKlobucharMessage.S_PER_SC_N[i].toSi(beta[i]))}
     * </p>
     * @param i index of the coefficient
     * @param betaI β coefficient to set (s/radⁿ)
     * @see #S_PER_SC_N
     */
    public void setBetaI(final int i, final double betaI) {
        beta[i] = betaI;
    }

    /** Get longitude min.
     * @return longitude min
     */
    public double getLonMin() {
        return lonMin;
    }

    /** Set longitude min.
     * @param lonMin longitude min
     */
    public void setLonMin(final double lonMin) {
        this.lonMin = lonMin;
    }

    /** Get longitude max.
     * @return longitude max
     */
    public double getLonMax() {
        return lonMax;
    }

    /** Set longitude max.
     * @param lonMax longitude max
     */
    public void setLonMax(final double lonMax) {
        this.lonMax = lonMax;
    }

    /** Get MODIP min.
     * @return MODIP min
     */
    public double getModipMin() {
        return modipMin;
    }

    /** Set MODIP min.
     * @param modipMin MODIP min
     */
    public void setModipMin(final double modipMin) {
        this.modipMin = modipMin;
    }

    /** Get MODIP max.
     * @return MODIP max
     */
    public double getModipMax() {
        return modipMax;
    }

    /** Set MODIP max.
     * @param modipMax MODIP max
     */
    public void setModipMax(final double modipMax) {
        this.modipMax = modipMax;
    }

}
