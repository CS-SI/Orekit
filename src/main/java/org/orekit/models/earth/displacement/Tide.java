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
package org.orekit.models.earth.displacement;

import org.orekit.data.BodiesElements;

/**
 * Class representing a tide.
 * @since 9.1
 * @author Luc Maisonobe
 */
public class Tide {

    /** M₂ tide. */
    public static final Tide M2 = new Tide(255555);

    /** S₂ tide. */
    public static final Tide S2 = new Tide(273555);

    /** N₂ tide. */
    public static final Tide N2 = new Tide(245655);

    /** K₂ tide. */
    public static final Tide K2 = new Tide(275555);

    /** K₁ tide. */
    public static final Tide K1 = new Tide(165555);

    /** O₁ tide. */
    public static final Tide O1 = new Tide(145555);

    /** P₁ tide. */
    public static final Tide P1 = new Tide(163555);

    /** Q₁ tide. */
    public static final Tide Q1 = new Tide(135655);

    /** Mf tide. */
    public static final Tide MF = new Tide(75555);

    /** Mm tide. */
    public static final Tide MM = new Tide(65455);

    /** Ssa tide. */
    public static final Tide SSA = new Tide(57555);

    /** Doodson number. */
    private final int doodsonNumber;

    /** Multipliers for Doodson arguments (τ, s, h, p, N', ps). */
    private final int[] doodsonMultipliers;

    /** Multipliers for Delaunay arguments (l, l', F, D, Ω). */
    private final int[] delaunayMultipliers;

    /** Simple constructor.
     * @param cTau coefficient for mean lunar time
     * @param cS coefficient for mean longitude of the Moon
     * @param cH coefficient for mean longitude of the Sun
     * @param cP coefficient for longitude of Moon mean perigee
     * @param cNprime negative of the longitude of the Moon's mean ascending node on the ecliptic
     * @param cPs coefficient for longitude of Sun mean perigee
     */
    public Tide(final int cTau, final int cS, final int cH, final int cP, final int cNprime, final int cPs) {
        doodsonNumber      = doodsonMultipliersToDoodsonNumber(cTau, cS, cH, cP, cNprime, cPs);
        doodsonMultipliers = new int[] {
            cTau, cS, cH, cP, cNprime, cPs
        };
        this.delaunayMultipliers = doodsonMultipliersToDelaunayMultipliers(doodsonMultipliers);
    }

    /** Simple constructor.
     * @param doodsonNumber Doodson Number
     */
    public Tide(final int doodsonNumber) {
        this.doodsonNumber       = doodsonNumber;
        this.doodsonMultipliers  = doodsonNumberToDoodsonMultipliers(doodsonNumber);
        this.delaunayMultipliers = doodsonMultipliersToDelaunayMultipliers(doodsonMultipliers);
    }

    /** Convert Doodson number to Doodson mutipliers.
     * @param doodsonNumber Doodson number
     * @return Doodson multipliers
     */
    private static int[] doodsonNumberToDoodsonMultipliers(final int doodsonNumber) {
        // CHECKSTYLE: stop Indentation check
        return new int[] {
             (doodsonNumber / 100000) % 10,
            ((doodsonNumber /  10000) % 10) - 5,
            ((doodsonNumber /   1000) % 10) - 5,
            ((doodsonNumber /    100) % 10) - 5,
            ((doodsonNumber /     10) % 10) - 5,
            (doodsonNumber            % 10) - 5
        };
        // CHECKSTYLE: resume Indentation check
    }

    /** Convert Doodson mutipliers to Doodson number.
     * @param cTau coefficient for mean lunar time
     * @param cS coefficient for mean longitude of the Moon
     * @param cH coefficient for mean longitude of the Sun
     * @param cP coefficient for longitude of Moon mean perigee
     * @param cNprime negative of the longitude of the Moon's mean ascending node on the ecliptic
     * @param cPs coefficient for longitude of Sun mean perigee
     * @return Doodson number
     */
    private static int doodsonMultipliersToDoodsonNumber(final int cTau, final int cS, final int cH,
                                                         final int cP, final int cNprime, final int cPs) {
        return ((((cTau * 10 + (cS + 5)) * 10 + (cH + 5)) * 10 + (cP + 5)) * 10 + (cNprime + 5)) * 10 + (cPs + 5);
    }

    /** Convert Doodson mutipliers to Delaunay multipliers.
     * @param dom Doodson multipliers
     * @return Delaunay multipliers
     */
    private static int[] doodsonMultipliersToDelaunayMultipliers(final int[] dom) {
        return new int[] {
            dom[3],
            dom[5],
            dom[0] - dom[1] - dom[2] - dom[3] - dom[5],
            dom[2] + dom[5],
            dom[0] - dom[1] - dom[2] - dom[3] + dom[4] - dom[5]
        };
    }

    /** Get the multipliers for Delaunay arguments (l, l', F, D, Ω).
     * <p>
     * Beware that for tides the multipliers for Delaunay arguments have an opposite
     * sign with respect to the convention used for nutation computation! Here, we
     * obey the tides convention.
     * </p>
     * @return multipliers for Delaunay arguments (l, l', F, D, Ω)
     */
    public int[] getDelaunayMultipliers() {
        return delaunayMultipliers.clone();
    }

    /** Get the multipliers for Doodson arguments (τ, s, h, p, N', ps).
     * @return multipliers for Doodson arguments (τ, s, h, p, N', ps)
     */
    public int[] getDoodsonMultipliers() {
        return doodsonMultipliers.clone();
    }

    /** Get the Doodson number.
     * @return Doodson number
     */
    public int getDoodsonNumber() {
        return doodsonNumber;
    }

    /** Get the multiplier for the τ Doodson argument.
     * <p>
     * This multiplier identifies semi-diurnal tides (2),
     * diurnal tides (1) and long period tides (0)
     * </p>
     * @return multiplier for the τ Doodson argument
     */
    public int getTauMultiplier() {
        return doodsonMultipliers[0];
    }

    /** Get the phase of the tide.
     * @param elements elements to use
     * @return phase of the tide (radians)
     */
    public double getPhase(final BodiesElements elements) {
        return doodsonMultipliers[0]  * elements.getGamma()  -
               delaunayMultipliers[0] * elements.getL()      -
               delaunayMultipliers[1] * elements.getLPrime() -
               delaunayMultipliers[2] * elements.getF()      -
               delaunayMultipliers[3] * elements.getD()      -
               delaunayMultipliers[4] * elements.getOmega();
    }

    /** Get the angular rate of the tide.
     * @param elements elements to use
     * @return angular rate of the tide (radians/second)
     */
    public double getRate(final BodiesElements elements) {
        return doodsonMultipliers[0]  * elements.getGammaDot()  -
               delaunayMultipliers[0] * elements.getLDot()      -
               delaunayMultipliers[1] * elements.getLPrimeDot() -
               delaunayMultipliers[2] * elements.getFDot()      -
               delaunayMultipliers[3] * elements.getDDot()      -
               delaunayMultipliers[4] * elements.getOmegaDot();
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof Tide) {
            return doodsonNumber == ((Tide) object).doodsonNumber;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return doodsonNumber;
    }

}

