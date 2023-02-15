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
package org.orekit.time;

import java.io.Serializable;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.LazyLoadedEop;
import org.orekit.utils.IERSConventions;


/** Factory for predefined time scales.
 * <p>
 * This is a utility class, so its constructor is private.
 * </p>
 * @author Luc Maisonobe
 * @see TimeScales
 * @see LazyLoadedTimeScales
 */
public class TimeScalesFactory implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20190927L;

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private TimeScalesFactory() {
    }

    /**
     * Get the instance of {@link TimeScales} that is called by all of the static methods
     * in this class.
     *
     * @return the time scales used by this factory.
     */
    @DefaultDataContext
    public static LazyLoadedTimeScales getTimeScales() {
        return DataContext.getDefault().getTimeScales();
    }

    /** Add a loader for UTC-TAI offsets history files.
     * @param loader custom loader to add
     * @see TAIUTCDatFilesLoader
     * @see UTCTAIHistoryFilesLoader
     * @see UTCTAIBulletinAFilesLoader
     * @see #getUTC()
     * @see #clearUTCTAIOffsetsLoaders()
     * @since 7.1
     */
    @DefaultDataContext
    public static void addUTCTAIOffsetsLoader(final UTCTAIOffsetsLoader loader) {
        getTimeScales().addUTCTAIOffsetsLoader(loader);
    }

    /** Add the default loaders for UTC-TAI offsets history files (both IERS and USNO).
     * <p>
     * The default loaders are {@link TAIUTCDatFilesLoader} that looks for
     * a file named {@code tai-utc.dat} that must be in USNO format and
     * {@link UTCTAIHistoryFilesLoader} that looks fir a file named
     * {@code UTC-TAI.history} that must be in the IERS format. The
     * {@link UTCTAIBulletinAFilesLoader} is <em>not</em> added by default
     * as it is not recommended. USNO warned us that the TAI-UTC data present
     * in bulletin A was for convenience only and was not reliable, there
     * have been errors in several bulletins regarding these data.
     * </p>
     * @see <a href="http://maia.usno.navy.mil/ser7/tai-utc.dat">USNO tai-utc.dat file</a>
     * @see <a href="http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history">IERS UTC-TAI.history file</a>
     * @see TAIUTCDatFilesLoader
     * @see UTCTAIHistoryFilesLoader
     * @see #getUTC()
     * @see #clearUTCTAIOffsetsLoaders()
     * @since 7.1
     */
    @DefaultDataContext
    public static void addDefaultUTCTAIOffsetsLoaders() {
        getTimeScales().addDefaultUTCTAIOffsetsLoaders();
    }

    /** Clear loaders for UTC-TAI offsets history files.
     * @see #getUTC()
     * @see #addUTCTAIOffsetsLoader(UTCTAIOffsetsLoader)
     * @see #addDefaultUTCTAIOffsetsLoaders()
     * @since 7.1
     */
    @DefaultDataContext
    public static void clearUTCTAIOffsetsLoaders() {
        getTimeScales().clearUTCTAIOffsetsLoaders();
    }

    /** Get the International Atomic Time scale.
     * @return International Atomic Time scale
     */
    @DefaultDataContext
    public static TAIScale getTAI() {
        return getTimeScales().getTAI();
    }

    /** Get the Universal Time Coordinate scale.
     * <p>
     * If no {@link UTCTAIOffsetsLoader} has been added by calling {@link
     * #addUTCTAIOffsetsLoader(UTCTAIOffsetsLoader) addUTCTAIOffsetsLoader} or if {@link
     * #clearUTCTAIOffsetsLoaders() clearUTCTAIOffsetsLoaders} has been called afterwards,
     * the {@link #addDefaultUTCTAIOffsetsLoaders() addDefaultUTCTAILoaders} method
     * will be called automatically.
     * </p>
     * @return Universal Time Coordinate scale
     * @see #addDefaultUTCTAIOffsetsLoaders()
     */
    @DefaultDataContext
    public static UTCScale getUTC() {
        return getTimeScales().getUTC();
    }

    /** Get the Universal Time 1 scale.
     * <p>
     * UT1 scale depends on both UTC scale and Earth Orientation Parameters,
     * so this method loads these data sets. See the {@link #getUTC()
     * TimeScalesFactory.getUTC()} and {@link
     * LazyLoadedEop#getEOPHistory(IERSConventions, boolean, TimeScales)} methods
     * for an explanation of how the corresponding data loaders can be configured.
     * </p>
     * @param conventions IERS conventions for which EOP parameters will provide dUT1
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return Universal Time 1 scale
     * @see #getUTC()
     * @see LazyLoadedEop#getEOPHistory(IERSConventions, boolean, TimeScales)
     */
    @DefaultDataContext
    public static UT1Scale getUT1(final IERSConventions conventions, final boolean simpleEOP) {
        return getTimeScales().getUT1(conventions, simpleEOP);
    }

    /** Get the Universal Time 1 scale.
     * <p>
     * As this method allow associating any history with the time scale,
     * it may involve large data sets. So this method does <em>not</em>
     * cache the resulting {@link UT1Scale UT1Scale} instance, a new
     * instance will be returned each time. In order to avoid wasting
     * memory, calling {@link #getUT1(IERSConventions, boolean)}
     * with the single enumerate corresponding to the conventions may be
     * a better solution. This method is made available only for expert use.
     * </p>
     * @param history EOP parameters providing dUT1
     * (may be null if no correction is desired)
     * @return Universal Time 1 scale
     * @see #getUT1(IERSConventions, boolean)
     */
    @DefaultDataContext
    public static UT1Scale getUT1(final EOPHistory history) {
        return getTimeScales().getUT1(history);
    }

    /** Get the Terrestrial Time scale.
     * @return Terrestrial Time scale
     */
    @DefaultDataContext
    public static TTScale getTT() {
        return getTimeScales().getTT();
    }

    /** Get the Galileo System Time scale.
     * @return Galileo System Time scale
     */
    @DefaultDataContext
    public static GalileoScale getGST() {
        return getTimeScales().getGST();
    }

    /** Get the GLObal NAvigation Satellite System time scale.
     * @return  GLObal NAvigation Satellite System time scale
     */
    @DefaultDataContext
    public static GLONASSScale getGLONASS() {
        return getTimeScales().getGLONASS();
    }

    /** Get the Quasi-Zenith Satellite System time scale.
     * @return  Quasi-Zenith Satellite System time scale
     */
    @DefaultDataContext
    public static QZSSScale getQZSS() {
        return getTimeScales().getQZSS();
    }

    /** Get the Global Positioning System scale.
     * @return Global Positioning System scale
     */
    @DefaultDataContext
    public static GPSScale getGPS() {
        return getTimeScales().getGPS();
    }

    /** Get the Geocentric Coordinate Time scale.
     * @return Geocentric Coordinate Time scale
     */
    @DefaultDataContext
    public static TCGScale getTCG() {
        return getTimeScales().getTCG();
    }

    /** Get the Barycentric Dynamic Time scale.
     * @return Barycentric Dynamic Time scale
     */
    @DefaultDataContext
    public static TDBScale getTDB() {
        return getTimeScales().getTDB();
    }

    /** Get the Barycentric Coordinate Time scale.
     * @return Barycentric Coordinate Time scale
     */
    @DefaultDataContext
    public static TCBScale getTCB() {
        return getTimeScales().getTCB();
    }

    /** Get the Greenwich Mean Sidereal Time scale.
     * @param conventions IERS conventions for which EOP parameters will provide dUT1
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return Greenwich Mean Sidereal Time scale
     * @since 7.0
     */
    @DefaultDataContext
    public static GMSTScale getGMST(final IERSConventions conventions, final boolean simpleEOP) {
        return getTimeScales().getGMST(conventions, simpleEOP);
    }

    /** Get the Indian Regional Navigation Satellite System time scale.
     * @return  Indian Regional Navigation Satellite System time scale
     */
    @DefaultDataContext
    public static IRNSSScale getIRNSS() {
        return getTimeScales().getIRNSS();
    }

    /** Get the BeiDou Navigation Satellite System time scale.
     * @return  BeiDou Navigation Satellite System time scale
     */
    @DefaultDataContext
    public static BDTScale getBDT() {
        return getTimeScales().getBDT();
    }


}
