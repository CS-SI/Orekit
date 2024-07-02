/* Contributed in the public domain.
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
package org.orekit.time;

import java.util.Collection;
import java.util.function.BiFunction;

import org.orekit.frames.EOPEntry;
import org.orekit.frames.Frames;
import org.orekit.utils.IERSConventions;

/**
 * A collection of {@link TimeScale}s. This interface defines methods for obtaining
 * instances of many common time scales.
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see TimeScalesFactory
 * @see TimeScale
 * @see LazyLoadedTimeScales
 * @see #of(Collection, BiFunction)
 * @since 10.1
 */
public interface TimeScales {

    /**
     * Get the International Atomic Time scale.
     *
     * @return International Atomic Time scale
     */
    TAIScale getTAI();

    /**
     * Get the Universal Time Coordinate scale.
     *
     * @return Universal Time Coordinate scale
     */
    UTCScale getUTC();

    /**
     * Get the Universal Time 1 scale.
     *
     * @param conventions IERS conventions for which EOP parameters will provide dUT1
     * @param simpleEOP   if true, tidal effects are ignored when interpolating EOP
     * @return Universal Time 1 scale
     * @see #getUTC()
     * @see Frames#getEOPHistory(IERSConventions, boolean)
     */
    UT1Scale getUT1(IERSConventions conventions, boolean simpleEOP);

    /**
     * Get the Terrestrial Time scale.
     *
     * @return Terrestrial Time scale
     */
    TTScale getTT();

    /**
     * Get the Galileo System Time scale.
     *
     * @return Galileo System Time scale
     */
    GalileoScale getGST();

    /**
     * Get the GLObal NAvigation Satellite System time scale.
     *
     * @return GLObal NAvigation Satellite System time scale
     */
    GLONASSScale getGLONASS();

    /**
     * Get the Quasi-Zenith Satellite System time scale.
     *
     * @return Quasi-Zenith Satellite System time scale
     */
    QZSSScale getQZSS();

    /**
     * Get the Global Positioning System scale.
     *
     * @return Global Positioning System scale
     */
    GPSScale getGPS();

    /**
     * Get the Geocentric Coordinate Time scale.
     *
     * @return Geocentric Coordinate Time scale
     */
    TCGScale getTCG();

    /**
     * Get the Barycentric Dynamic Time scale.
     *
     * @return Barycentric Dynamic Time scale
     */
    TDBScale getTDB();

    /**
     * Get the Barycentric Coordinate Time scale.
     *
     * @return Barycentric Coordinate Time scale
     */
    TCBScale getTCB();

    /**
     * Get the Greenwich Mean Sidereal Time scale.
     *
     * @param conventions IERS conventions for which EOP parameters will provide dUT1
     * @param simpleEOP   if true, tidal effects are ignored when interpolating EOP
     * @return Greenwich Mean Sidereal Time scale
     * @since 7.0
     */
    GMSTScale getGMST(IERSConventions conventions, boolean simpleEOP);

    /**
     * Get the Indian Regional Navigation Satellite System time scale.
     *
     * @return Indian Regional Navigation Satellite System time scale
     */
    IRNSSScale getIRNSS();

    /**
     * Get the BeiDou Navigation Satellite System time scale.
     *
     * @return BeiDou Navigation Satellite System time scale
     */
    BDTScale getBDT();

    /**
     * Reference epoch for julian dates: -4712-01-01T12:00:00 Terrestrial Time.
     * <p>Both <code>java.util.Date</code> and {@link DateComponents} classes
     * follow the astronomical conventions and consider a year 0 between years -1 and +1,
     * hence this reference date lies in year -4712 and not in year -4713 as can be seen
     * in other documents or programs that obey a different convention (for example the
     * <code>convcal</code> utility).</p>
     *
     * @return Julian epoch.
     */
    AbsoluteDate getJulianEpoch();

    /**
     * Reference epoch for modified julian dates: 1858-11-17T00:00:00 Terrestrial Time.
     *
     * @return Modified Julian Epoch
     */
    AbsoluteDate getModifiedJulianEpoch();

    /**
     * Reference epoch for 1950 dates: 1950-01-01T00:00:00 Terrestrial Time.
     *
     * @return Fifties Epoch
     */
    AbsoluteDate getFiftiesEpoch();

    /**
     * Reference epoch for CCSDS Time Code Format (CCSDS 301.0-B-4): 1958-01-01T00:00:00
     * International Atomic Time (<em>not</em> UTC).
     *
     * @return CCSDS Epoch
     */
    AbsoluteDate getCcsdsEpoch();

    /**
     * Reference epoch for Galileo System Time: 1999-08-22T00:00:00 GST.
     *
     * @return Galileo Epoch
     */
    AbsoluteDate getGalileoEpoch();

    /**
     * Reference epoch for GPS weeks: 1980-01-06T00:00:00 GPS time.
     *
     * @return GPS Epoch
     */
    AbsoluteDate getGpsEpoch();

    /**
     * Reference epoch for QZSS weeks: 1980-01-06T00:00:00 QZSS time.
     *
     * @return QZSS Epoch
     */
    AbsoluteDate getQzssEpoch();

    /**
     * Reference epoch for IRNSS weeks: 1999-08-22T00:00:00 IRNSS time.
     *
     * @return IRNSS Epoch
     */
    AbsoluteDate getIrnssEpoch();

    /**
     * Reference epoch for BeiDou weeks: 2006-01-01T00:00:00 UTC.
     *
     * @return Beidou Epoch
     */
    AbsoluteDate getBeidouEpoch();

    /**
     * Reference epoch for GLONASS four-year interval number: 1996-01-01T00:00:00 GLONASS
     * time.
     * <p>By convention, TGLONASS = UTC + 3 hours.</p>
     *
     * @return GLONASS Epoch
     */
    AbsoluteDate getGlonassEpoch();

    /**
     * J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC).
     *
     * @return J2000 Epoch
     * @see AbsoluteDate#createJulianEpoch(double)
     * @see AbsoluteDate#createBesselianEpoch(double)
     */
    AbsoluteDate getJ2000Epoch();

    /**
     * Java Reference epoch: 1970-01-01T00:00:00 Universal Time Coordinate.
     * <p>
     * Between 1968-02-01 and 1972-01-01, UTC-TAI = 4.213 170 0s + (MJD - 39 126) x 0.002
     * 592s. As on 1970-01-01 MJD = 40587, UTC-TAI = 8.000082s
     * </p>
     *
     * @return Java Epoch
     */
    AbsoluteDate getJavaEpoch();

    /**
     * Dummy date at infinity in the past direction.
     *
     * @return the earliest date.
     */
    AbsoluteDate getPastInfinity();

    /**
     * Dummy date at infinity in the future direction.
     *
     * @return the latest date.
     */
    AbsoluteDate getFutureInfinity();

    /**
     * Build an instance corresponding to a Julian Epoch (JE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&amp;defaultprint=YES&amp;filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>,
     * Astronomy and Astrophysics, vol. 73, no. 3, Mar. 1979, p. 282-284, Julian Epoch is
     * related to Julian Ephemeris Date as:</p>
     * <pre>
     * JE = 2000.0 + (JED - 2451545.0) / 365.25
     * </pre>
     * <p>
     * This method reverts the formula above and computes an {@code AbsoluteDate} from the
     * Julian Epoch.
     * </p>
     *
     * @param julianEpoch Julian epoch, like 2000.0 for defining the classical reference
     *                    J2000.0
     * @return a new instant
     * @see #getJ2000Epoch()
     * @see #createBesselianEpoch(double)
     */
    AbsoluteDate createJulianEpoch(double julianEpoch);

    /**
     * Build an instance corresponding to a Besselian Epoch (BE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&amp;defaultprint=YES&amp;filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>,
     * Astronomy and Astrophysics, vol. 73, no. 3, Mar. 1979, p. 282-284, Besselian Epoch
     * is related to Julian Ephemeris Date as:</p>
     * <pre>
     * BE = 1900.0 + (JED - 2415020.31352) / 365.242198781
     * </pre>
     * <p>
     * This method reverts the formula above and computes an {@code AbsoluteDate} from the
     * Besselian Epoch.
     * </p>
     *
     * @param besselianEpoch Besselian epoch, like 1950 for defining the classical
     *                       reference B1950.0
     * @return a new instant
     * @see #createJulianEpoch(double)
     */
    AbsoluteDate createBesselianEpoch(double besselianEpoch);

    /* Helpers for creating new instances. */

    /**
     * Create a set of time scales where all the data is loaded from the given functions.
     *
     * @param utcMinusTai offsets used to compute UTC. If the pre-1972 linear offsets are
     *                    missing they will be added.
     * @param eopSupplier function to retrieve the EOP data. Since the EOP have to be
     *                    reloaded every time a different {@link IERSConventions} is
     *                    requested this function may be called multiple times. The
     *                    requested conventions and the created time scales are passed as
     *                    arguments. Attempting to call {@link #getUT1(IERSConventions,
     *                    boolean)} or {@link #getGMST(IERSConventions, boolean)} on the
     *                    time scales argument may result in unbounded recursion. To
     *                    ignore EOP corrections this function should return an empty
     *                    collection.
     * @return a set of time scales based on the given data.
     * @see UTCTAIOffsetsLoader.Parser
     * @see org.orekit.frames.EopHistoryLoader.Parser
     */
    static TimeScales of(
            final Collection<? extends OffsetModel> utcMinusTai,
            final BiFunction<
                    ? super IERSConventions,
                    ? super TimeScales,
                    ? extends Collection<? extends EOPEntry>> eopSupplier) {
        return new PreloadedTimeScales(utcMinusTai, eopSupplier);
    }

}
