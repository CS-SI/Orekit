/* Copyright 2002-2026 CS GROUP
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
package org.orekit.frames;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeStamped;

/** This class holds an Earth Orientation Parameters entry.
 * @author Luc Maisonobe
 */
public class EOPEntry implements TimeStamped, Serializable {

    /** Serializable UID. */
    @Serial
    private static final long serialVersionUID = 20260729L;

    /** Entry date (modified julian day, 00h00 UTC scale). */
    private final int mjd;

    /** Entry date (absolute date). */
    private final AbsoluteDate date;

    /** UT1-UTC. */
    private final double dt;

    /** Length of day. */
    private final double lod;

    /** X component of pole motion. */
    private final double x;

    /** Y component of pole motion. */
    private final double y;

    /** X component of pole motion rate.
     * @since 12.0
     */
    private final double xRate;

    /** Y component of pole motion rate.
     * @since 12.0
     */
    private final double yRate;

    /** Correction for nutation in longitude. */
    private final double ddPsi;

    /** Correction for nutation in obliquity. */
    private final double ddEps;

    /** Correction for nutation in Celestial Intermediate Pole (CIP) coordinates. */
    private final double dx;

    /** Correction for nutation in Celestial Intermediate Pole (CIP) coordinates. */
    private final double dy;

    /** ITRF version this entry defines. */
    private final ITRFVersion itrfType;

    /** EOP data type. */
    private final EopDataType eopDataType;

    /** Publication date for X, Y, UT1-UTC and LOD.
     * @since 14.0
     */
    private final int dtPub;

    /** Publication date for nutation.
     * @since 14.0
     */
    private final int nutPub;

    /** Publication date for Celestial Intermediate Pole.
     * @since 14.0
     */
    private final int cipPub;

    /** Constructor from raw elements.
     * <p>
     * This constructor assumes publication date is the same as entry date
     * </p>
     * @param mjd entry date (modified Julian day, 00h00 UTC scale)
     * @param dt UT1-UTC in seconds
     * @param lod length of day
     * @param x X component of pole motion
     * @param y Y component of pole motion
     * @param xRate X component of pole motion rate (NaN if absent)
     * @param yRate Y component of pole motion rate (NaN if absent)
     * @param ddPsi correction for nutation in longitude δΔΨ
     * @param ddEps correction for nutation in obliquity δΔε
     * @param dx correction for Celestial Intermediate Pole (CIP) coordinates
     * @param dy correction for Celestial Intermediate Pole (CIP) coordinates
     * @param itrfType ITRF version this entry defines
     * @param date corresponding to {@code mjd}
     * @param eopDataType EOP data type
     * @since 13.1.1
     */
    public EOPEntry(final int mjd, final double dt, final double lod,
                    final double x, final double y, final double xRate, final double yRate,
                    final double ddPsi, final double ddEps,
                    final double dx, final double dy,
                    final ITRFVersion itrfType, final AbsoluteDate date,
                    final EopDataType eopDataType) {
        this(mjd, dt, lod, x, y, xRate, yRate, ddPsi, ddEps, dx, dy, itrfType, date, eopDataType,
             mjd, mjd, mjd);
    }

    /** Constructor from raw elements.
     * @param mjd entry date (modified Julian day, 00h00 UTC scale)
     * @param dt UT1-UTC in seconds
     * @param lod length of day
     * @param x X component of pole motion
     * @param y Y component of pole motion
     * @param xRate X component of pole motion rate (NaN if absent)
     * @param yRate Y component of pole motion rate (NaN if absent)
     * @param ddPsi correction for nutation in longitude δΔΨ
     * @param ddEps correction for nutation in obliquity δΔε
     * @param dx correction for Celestial Intermediate Pole (CIP) coordinates
     * @param dy correction for Celestial Intermediate Pole (CIP) coordinates
     * @param itrfType ITRF version this entry defines
     * @param date corresponding to {@code mjd}
     * @param eopDataType EOP data type
     * @param dtPub publication date for X, Y, UT1-UTC and LOD
     * @param nutPub publication date for nutation
     * @param cipPub publication date for Celestial Intermediate Pole
     * @since 14.0
     */
    public EOPEntry(final int mjd, final double dt, final double lod,
                    final double x, final double y, final double xRate, final double yRate,
                    final double ddPsi, final double ddEps,
                    final double dx, final double dy,
                    final ITRFVersion itrfType, final AbsoluteDate date, final EopDataType eopDataType,
                    final int dtPub, final int nutPub, final int cipPub) {
        this.mjd         = mjd;
        this.date        = date;
        this.dt          = dt;
        this.lod         = lod;
        this.x           = x;
        this.y           = y;
        this.xRate       = xRate;
        this.yRate       = yRate;
        this.ddPsi       = ddPsi;
        this.ddEps       = ddEps;
        this.dx          = dx;
        this.dy          = dy;
        this.itrfType    = itrfType;
        this.eopDataType = eopDataType;
        this.dtPub       = dtPub;
        this.nutPub      = nutPub;
        this.cipPub      = cipPub;
    }

    /** Combination constructor.
     * <p>
     * This constructor allows to merge data from two potentially incomplete entries
     * corresponding to the same date.
     * </p>
     * <p>
     * Incomplete entries occur in particular when parsing Bulletin A data, as
     * xp, yp and UT1-UTC data are published on a weekly basis for rapid data and
     * monthly for final data with a one-month delay. The pole offsets Δδψ/Δδε and
     * x/y are published monthly with a two months delay.
     * </p>
     * <p>
     * If some fields are initialized in one entry and non-initialized (i.e. NaN) in
     * the other argument, then the initialized fields will be used for construction.
     * If some fields are initialized in both entries, then the one published later
     * will be used in construction. The publication date will be set to the latest
     * of both publication dates. This implies that entries are combined.
     * </p>
     * @param entry1 first entry
     * @param entry2 second entry
     * @since 14.0
     */
    public EOPEntry(final EOPEntry entry1, final EOPEntry entry2) {

        // safety check
        if (entry1.mjd != entry2.mjd) {
            throw new OrekitException(OrekitMessages.INCOMPATIBLE_EARTH_ORIENTATION_PARAMETERS,
                                      new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, entry1.mjd),
                                      new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, entry2.mjd));
        }

        // the dates are the same
        this.mjd   = entry1.mjd;
        this.date  = entry1.date;

        // combine fields
        this.dt    = select(entry1, entry2, entry -> entry.dt,    entry -> entry.dtPub);
        this.lod   = select(entry1, entry2, entry -> entry.lod,   entry -> entry.dtPub);
        this.x     = select(entry1, entry2, entry -> entry.x,     entry -> entry.dtPub);
        this.y     = select(entry1, entry2, entry -> entry.y,     entry -> entry.dtPub);
        this.xRate = select(entry1, entry2, entry -> entry.xRate, entry -> entry.dtPub);
        this.yRate = select(entry1, entry2, entry -> entry.yRate, entry -> entry.dtPub);
        this.ddPsi = select(entry1, entry2, entry -> entry.ddPsi, entry -> entry.nutPub);
        this.ddEps = select(entry1, entry2, entry -> entry.ddEps, entry -> entry.nutPub);
        this.dx    = select(entry1, entry2, entry -> entry.dx,    entry -> entry.cipPub);
        this.dy    = select(entry1, entry2, entry -> entry.dy,    entry -> entry.cipPub);

        if (entry1.dtPub >= entry2.dtPub) {
            this.itrfType           = entry1.itrfType;
            this.eopDataType        = entry1.eopDataType;
            this.dtPub = entry1.dtPub;
        } else {
            this.itrfType           = entry2.itrfType;
            this.eopDataType        = entry2.eopDataType;
            this.dtPub = entry2.dtPub;
        }

        this.nutPub = FastMath.max(entry1.nutPub, entry2.nutPub);
        this.cipPub = FastMath.max(entry1.cipPub, entry2.cipPub);

    }

    /** Select either initialized or published last EOP field.
     * @param entry1      first entry
     * @param entry2      second entry
     * @param field       selector for field
     * @param publication selector for publication date
     * @return selected field
     * @since 14.0
     */
    private double select(final EOPEntry entry1, final EOPEntry entry2,
                          final ToDoubleFunction<EOPEntry> field,
                          final ToIntFunction<EOPEntry> publication) {
        final double field1 = field.applyAsDouble(entry1);
        final double field2 = field.applyAsDouble(entry2);
        if (Double.isNaN(field1)) {
            // the field is not initialized in entry1, we select the field from entry2
            return field2;
        } else if (Double.isNaN(field2)) {
            // the field is not initialized in entry2, we select the field from entry1
            return field1;
        } else {
            // the field is initialized in both entries, we select the one published later
            return publication.applyAsInt(entry1) >= publication.applyAsInt(entry2) ?
                   field1 : field2;
        }
    }

    /** Get the entry date (modified julian day, 00h00 UTC scale).
     * @return entry date
     * @see #getDate()
     */
    public int getMjd() {
        return mjd;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the UT1-UTC value.
     * @return UT1-UTC in seconds
     */
    public double getUT1MinusUTC() {
        return dt;
    }

    /** Get the LoD (Length of Day) value.
     * @return LoD in seconds
     */
    public double getLOD() {
        return lod;
    }

    /** Get the X component of the pole motion.
     * @return X component of pole motion
     */
    public double getX() {
        return x;
    }

    /** Get the Y component of the pole motion.
     * @return Y component of pole motion
     */
    public double getY() {
        return y;
    }

    /** Get the X component of the pole motion rate.
     * @return X component of pole motion rate
     * @since 12.0
     */
    public double getXRate() {
        return xRate;
    }

    /** Get the Y component of the pole motion rate.
     * @return Y component of pole motion rate
     * @since 12.0
     */
    public double getYRate() {
        return yRate;
    }

    /** Get the correction for nutation in longitude δΔΨ.
     * @return correction for nutation in longitude  δΔΨ
     */
    public double getDdPsi() {
        return ddPsi;
    }

    /** Get the correction for nutation in obliquity δΔε.
     * @return correction for nutation in obliquity δΔε
     */
    public double getDdEps() {
        return ddEps;
    }

    /** Get the correction for Celestial Intermediate Pole (CIP) coordinates.
     * @return correction for Celestial Intermediate Pole (CIP) coordinates
     */
    public double getDx() {
        return dx;
    }

    /** Get the correction for Celestial Intermediate Pole (CIP) coordinates.
     * @return correction for Celestial Intermediate Pole (CIP) coordinates
     */
    public double getDy() {
        return dy;
    }

    /** Get the ITRF version this entry defines.
     * @return ITRF version this entry defines
     * @since 9.2
     */
    public ITRFVersion getITRFType() {
        return itrfType;
    }

    /** Get the EOP data type.
     * @return EOP data type
     * @since 13.1.1
     */
    public EopDataType getEopDataType() { return eopDataType; }

    /** Get the publication date for X, Y, UT1-UTC and LOD (modified Julian day).
     * @return publication date for X, Y, UT1-UTC and LOD
     * @since 14.0
     */
    public int getDtPub() {
        return dtPub;
    }

    /** Get the publication date for nutation (modified Julian day).
     * @return publication date for nutation
     * @since 14.0
     */
    public int getNutPub() {
        return nutPub;
    }

    /** Get the publication date for Celestial Intermediate Pole (modified Julian day).
     * @return publication date for Celestial Intermediate Pole
     * @since 14.0
     */
    public int getCipPub() {
        return cipPub;
    }

}
