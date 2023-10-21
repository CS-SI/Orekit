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
package org.orekit.files.sinex;

import org.orekit.frames.EOPEntry;
import org.orekit.frames.ITRFVersion;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.IERSConventions;

/**
 * Container for EOP entry read in a Sinex file.
 *
 * @author Bryan Cazabonne
 * @since 11.2
 */
public class SinexEopEntry implements TimeStamped {

    /** Length of day (seconds). */
    private double lod;

    /** UT1-UTC (seconds). */
    private double ut1MinusUtc;

    /** X polar motion (radians). */
    private double xPo;

    /** Y polar motion (radians). */
    private double yPo;

    /** Nutation correction in longitude (radians). */
    private double nutLn;

    /** Nutation correction in obliquity (radians). */
    private double nutOb;

    /** Nutation correction X (radians). */
    private double nutX;

    /** Nutation correction Y (radians). */
    private double nutY;

    /** EOP entry reference epoch. */
    private final AbsoluteDate epoch;

    /**
     * Constructor.
     * @param epoch epoch of the data
     */
    public SinexEopEntry(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return epoch;
    }

    /**
     * Get the length of day.
     * @return the length of day in seconds
     */
    public double getLod() {
        return lod;
    }

    /**
     * Set the length of day.
     * @param lod the length of day to set in seconds
     */
    public void setLod(final double lod) {
        this.lod = lod;
    }

    /**
     * Get the UT1-UTC offset.
     * @return the UT1-UTC offset in seconds
     */
    public double getUt1MinusUtc() {
        return ut1MinusUtc;
    }

    /**
     * Set the UT1-UTC offset.
     * @param ut1MinusUtc the value to set in seconds
     */
    public void setUt1MinusUtc(final double ut1MinusUtc) {
        this.ut1MinusUtc = ut1MinusUtc;
    }

    /**
     * Get the X polar motion.
     * @return the X polar motion in radians
     */
    public double getXPo() {
        return xPo;
    }

    /**
     * Set the X polar motion.
     * @param xPo the X polar motion to set in radians
     */
    public void setxPo(final double xPo) {
        this.xPo = xPo;
    }

    /**
     * Get the Y polar motion.
     * @return the Y polar motion in radians
     */
    public double getYPo() {
        return yPo;
    }

    /**
     * Set the Y polar motion.
     * @param yPo the Y polar motion to set in radians
     */
    public void setyPo(final double yPo) {
        this.yPo = yPo;
    }

    /**
     * Get the nutation correction in longitude.
     * @return the nutation correction in longitude in radians
     */
    public double getNutLn() {
        return nutLn;
    }

    /**
     * Set the nutation correction in longitude.
     * @param nutLn the nutation correction in longitude to set in radians
     */
    public void setNutLn(final double nutLn) {
        this.nutLn = nutLn;
    }

    /**
     * Get the nutation correction in obliquity.
     * @return the nutation correction in obliquity in radians
     */
    public double getNutOb() {
        return nutOb;
    }

    /**
     * Set the nutation correction in obliquity.
     * @param nutOb the nutation correction in obliquity to set in radians
     */
    public void setNutOb(final double nutOb) {
        this.nutOb = nutOb;
    }

    /**
     * Get the nutation correction X.
     * @return the nutation correction X in radians
     */
    public double getNutX() {
        return nutX;
    }

    /**
     * Set the nutation correction X.
     * @param nutX the nutation correction X to set in radians
     */
    public void setNutX(final double nutX) {
        this.nutX = nutX;
    }

    /**
     * Get the nutation correction Y.
     * @return the nutation correction Y in radians
     */
    public double getNutY() {
        return nutY;
    }

    /**
     * Set the nutation correction Y.
     * @param nutY the nutation correction Y to set in radians
     */
    public void setNutY(final double nutY) {
        this.nutY = nutY;
    }

    /**
     * Converts to an {@link EOPEntry}.
     * @param converter converter to use for nutation corrections
     * @param version ITRF version
     * @param scale time scale for epochs
     * @return an {@code EOPEntry}
     */
    public EOPEntry toEopEntry(final IERSConventions.NutationCorrectionConverter converter,
                               final ITRFVersion version, final TimeScale scale) {

        // Modified Julian Day
        final int mjd = epoch.getComponents(scale).getDate().getMJD();

        // Array for equinox and non rotating origin
        final double[] nro     = (nutX != 0  && nutY != 0)  ? new double[] {nutX, nutY}   : converter.toNonRotating(epoch, nutLn, nutOb);
        final double[] equinox = (nutLn != 0 && nutOb != 0) ? new double[] {nutLn, nutOb} : converter.toEquinox(epoch, nutX, nutY);

        // Create a new EOPEntry object storing the extracted data, then add it to the list of EOPEntries.
        return new EOPEntry(mjd, ut1MinusUtc, lod,
                            xPo, yPo, Double.NaN, Double.NaN,
                            equinox[0], equinox[1],
                            nro[0], nro[1],
                            version, epoch);

    }

}
