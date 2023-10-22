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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBodies;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScales;
import org.orekit.time.UT1Scale;
import org.orekit.time.UTCScale;
import org.orekit.utils.IERSConventions;


/** Factory for predefined reference frames.
 *
 * <h2> FramesFactory Presentation </h2>
 * <p>
 * Several predefined reference {@link Frame frames} are implemented in OREKIT.
 * They are linked together in a tree with the <i>Geocentric
 * Celestial Reference Frame</i> (GCRF) as the root of the tree.
 * This factory is designed to:
 * </p>
 * <ul>
 *   <li>build the frames tree consistently,</li>
 *   <li>avoid rebuilding some frames that may be costly to recreate all the time,</li>
 *   <li>set up interpolation/caching features for some frames that may induce costly computation</li>
 *   <li>streamline the {@link EOPHistory Earth Orientation Parameters} history loading.</li>
 * </ul>
 * <h2> Reference Frames </h2>
 * <p>
 * The user can retrieve those reference frames using various static methods, the most
 * important ones being: {@link #getFrame(Predefined)}, {@link #getGCRF()},
 * {@link #getCIRF(IERSConventions, boolean)} {@link #getTIRF(IERSConventions, boolean)},
 * {@link #getITRF(IERSConventions, boolean)}, {@link #getITRF(ITRFVersion, IERSConventions, boolean)},
 * {@link #getEME2000()}, {@link #getMOD(IERSConventions)}, {@link #getTOD(IERSConventions, boolean)},
 * {@link #getGTOD(IERSConventions, boolean)}, {@link #getITRFEquinox(IERSConventions, boolean)},
 * {@link #getTEME()} and {@link #getVeis1950()}.
 * </p>
 * <h2> International Terrestrial Reference Frame</h2>
 * <p>
 * This frame is the current (as of 2013) reference realization of
 * the International Terrestrial Reference System produced by IERS.
 * It is described in <a href="ftp://tai.bipm.org/iers/conv2010/tn36.pdf">
 * IERS conventions (2010)</a>. It replaces the Earth Centered Earth Fixed
 * frame which is the reference frame for GPS satellites.
 * </p>
 * <p>
 * This frame is used to define position on solid Earth. It rotates with
 * the Earth and includes the pole motion with respect to Earth crust as
 * provided by IERS {@link EOPHistory Earth Orientation Parameters}.
 * Its pole axis is the IERS Reference Pole (IRP).
 * </p>
 * <p>
 * Depending on the  {@link EOPHistory Earth Orientation Parameters} source,
 * different ITRS realization may be returned by {@link #getITRF(IERSConventions, boolean)},
 * and if EOP are mixed, the ITRF may even jump from one realization to another one.
 * This is not a problem for most users as different ITRS realizations are very close
 * to each other (a few millimeters at Earth surface). If however a specific ITRF version
 * (i.e. an ITRS realization) is needed for very high accuracy, Orekit provides the
 * {@link FramesFactory#getITRF(ITRFVersion, IERSConventions, boolean)} method
 * to get it and take care of jumps in EOP.
 * </p>
 * <p>
 * ITRF can be built using the new non-rotating origin paradigm
 * mandated by IAU 2000 resolution B1.8 and any supported {@link IERSConventions
 * IERS conventions} (even IERS 1996 can be used with non-rotating origin paradigm,
 * despite the resolution was not yet adopted at conventions publication time).
 * </p>
 * <p>
 * ITRF can also be built using the classical equinox paradigm used prior to IAU 2000
 * resolution B1.8 and any supported {@link IERSConventions IERS conventions} (even
 * IERS 2003 and 2010 can be used with equinox paradigm, despite the resolution is
 * in effect now). The choice of paradigm (non-rotating origin or equinox) and the
 * choice of IERS conventions (i.e. the choice of precession/nutation models) can
 * be made independently by user, Orekit provides all alternatives.
 * </p>
 * <h2>Intermediate frames</h2>
 * <p>
 * Orekit also provides all the intermediate frames that are needed to transform
 * between GCRF and ITRF, along the two paths: ITRF/TIRF/CIRF/GCRF for the
 * non-rotating origin paradigm and ITRF/GTOD/TOD/MOD/EME2000/GCRF for the equinox
 * paradigm.
 * </p>
 * <h2> Earth Orientation Parameters </h2>
 * <p>
 * This factory also handles loading of Earth Orientation Parameters (EOP) needed
 * for accurate transformations between inertial and Earth fixed frames, using
 * {@link org.orekit.data.DataProvidersManager} features. EOP are IERS conventions
 * dependent, because they correspond to correction to the precession/nutation
 * models. When EOP should be applied, but EOP data are not available, then a null
 * (0.0) correction is used. This can occur when no EOP data is loaded, or when the
 * requested date is beyond the time span of the loaded EOP data. Using a null
 * correction can result in coarse accuracy. To check the time span covered by EOP data use
 * {@link #getEOPHistory(IERSConventions, boolean)}, {@link EOPHistory#getStartDate()},
 * and {@link EOPHistory#getEndDate()}.
 * <p>
 * For more information on configuring the EOP data Orekit uses see
 * <a href="https://gitlab.orekit.org/orekit/orekit/blob/master/src/site/markdown/configuration.md">
 * https://gitlab.orekit.org/orekit/orekit/blob/master/src/site/markdown/configuration.md</a>.
 * <p>
 * Here is a schematic representation of the predefined reference frames tree:
 * </p>
 * <pre>
 *                                                                  GCRF
 *                                                                    |
 *                                                 |-----------------------------------------------
 *                                                 |                         |     Frame bias     |
 *                                                 |                         |                 EME2000
 *                                                 |                         |                    |
 *                                                 |                         | Precession effects |
 *                                                 |                         |                    |
 *           Bias, Precession and Nutation effects |                        MOD                  MOD  (Mean Equator Of Date)
 *                                                 |                         |             w/o EOP corrections
 *                                                 |                         |  Nutation effects  |
 *    (Celestial Intermediate Reference Frame)   CIRF                        |                    |
 *                                                 |                        TOD                  TOD  (True Equator Of Date)
 *                          Earth natural rotation |                         |             w/o EOP corrections
 *                                                 |-------------            |    Sidereal Time   |
 *                                                 |            |            |                    |
 *  (Terrestrial Intermediate Reference Frame)   TIRF         TIRF         GTOD                 GTOD  (Greenwich True Of Date)
 *                                                 |    w/o tidal effects                  w/o EOP corrections
 *                                     Pole motion |            |                                 |
 *                                                 |            |                                 |-------------
 *                                                 |            |                                 |            |
 * (International Terrestrial Reference Frame)   ITRF         ITRF                              ITRF        VEIS1950
 *                                                 |    w/o tidal effects                   equinox-based
 *                                                 |            |
 *                                           other ITRF     other ITRF
 *                                                      w/o tidal effects
 * </pre>
 * <p>
 * This is a utility class, so its constructor is private.
 * </p>
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @author Pascal Parraud
 * @see Frames
 */
public class FramesFactory {

    /* These constants were left here instead of being moved to LazyLoadedFrames because
     * they are public.
     */

    /** Default regular expression for the Rapid Data and Prediction EOP columns files (IAU1980 compatibles). */
    public static final String RAPID_DATA_PREDICTION_COLUMNS_1980_FILENAME = "^finals\\.[^.]*$";

    /** Default regular expression for the EOP XML files (IAU1980 compatibles). */
    public static final String XML_1980_FILENAME = "^(:finals|eopc04_\\d\\d)\\..*\\.xml$";

    /** Default regular expression for the EOPC04 files (IAU1980 compatibles). */
    public static final String EOPC04_1980_FILENAME = "^eopc04(_\\d\\d)?\\.\\d\\d$";

    /** Default regular expression for the BulletinB files (IAU1980 compatibles). */
    public static final String BULLETINB_1980_FILENAME = "^bulletinb(_IAU1980)?((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** Default regular expression for the Rapid Data and Prediction EOP columns files (IAU2000 compatibles). */
    public static final String RAPID_DATA_PREDICTION_COLUMNS_2000_FILENAME = "^finals2000A\\.[^.]*$";

    /** Default regular expression for the EOP XML files (IAU2000 compatibles). */
    public static final String XML_2000_FILENAME = "^(:finals2000A|eopc04_\\d\\d_IAU2000)\\..*\\.xml$";

    /** Default regular expression for the EOPC04 files (IAU2000 compatibles). */
    public static final String EOPC04_2000_FILENAME = "^eopc04(_\\d\\d_IAU2000)?\\.\\d\\d$";

    /** Default regular expression for the BulletinB files (IAU2000 compatibles). */
    public static final String BULLETINB_2000_FILENAME = "^bulletinb(_IAU2000)?((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** Default regular expression for the BulletinA files (IAU1980 and IAU2000 compatibles). */
    public static final String BULLETINA_FILENAME = "^bulletina-[ivxlcdm]+-\\d\\d\\d\\.txt$";

    /** Default regular expression for the csv files (IAU1980 and IAU2000 compatibles).
     * @since 12.0
     */
    public static final String CSV_FILENAME = "^(?:eopc04|bulletina|bulletinb).*\\.csv$";

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private FramesFactory() {
    }

    /**
     * Get the instance of {@link Frames} that is called by the static methods in this
     * class.
     *
     * @return the reference frames used by this factory.
     */
    @DefaultDataContext
    public static LazyLoadedFrames getFrames() {
        return DataContext.getDefault().getFrames();
    }

    /** Add the default loaders EOP history (IAU 1980 precession/nutation).
     * <p>
     * The default loaders look for IERS EOP C04 and bulletins B files. They
     * correspond to {@link IERSConventions#IERS_1996 IERS 1996} conventions.
     * </p>
     * @param rapidDataColumnsSupportedNames regular expression for supported
     * rapid data columns EOP files names
     * (may be null if the default IERS file names are used)
     * @param rapidDataXMLSupportedNames regular expression for supported XML EOP files names
     * (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP C04 files names
     * (may be null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names
     * (may be null if the default IERS file names are used)
     * @param bulletinASupportedNames regular expression for supported bulletin A files names
     * (may be null if the default IERS file names are used)
     * @param csvSupportedNames regular expression for supported csv files names
     * (may be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">IERS EOP C04 files</a>
     * @see #addEOPHistoryLoader(IERSConventions, EopHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP2000HistoryLoaders(String, String, String, String, String, String)
     * @since 12.0
     */
    @DefaultDataContext
    public static void addDefaultEOP1980HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames,
                                                       final String bulletinASupportedNames,
                                                       final String csvSupportedNames) {
        getFrames().addDefaultEOP1980HistoryLoaders(
                rapidDataColumnsSupportedNames,
                rapidDataXMLSupportedNames,
                eopC04SupportedNames,
                bulletinBSupportedNames,
                bulletinASupportedNames,
                csvSupportedNames);
    }

    /** Add the default loaders for EOP history (IAU 2000/2006 precession/nutation).
     * <p>
     * The default loaders look for IERS EOP C04 and bulletins B files. They
     * correspond to both {@link IERSConventions#IERS_2003 IERS 2003} and {@link
     * IERSConventions#IERS_2010 IERS 2010} conventions.
     * </p>
     * @param rapidDataColumnsSupportedNames regular expression for supported
     * rapid data columns EOP files names
     * (may be null if the default IERS file names are used)
     * @param xmlSupportedNames regular expression for supported XML EOP files names
     * (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP C04 files names
     * (may be null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names
     * (may be null if the default IERS file names are used)
     * @param bulletinASupportedNames regular expression for supported bulletin A files names
     * (may be null if the default IERS file names are used)
     * @param csvSupportedNames regular expression for supported csv files names
     * (may be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">IERS EOP C04 files</a>
     * @see #addEOPHistoryLoader(IERSConventions, EopHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String, String)
     * @since 12.0
     */
    @DefaultDataContext
    public static void addDefaultEOP2000HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String xmlSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames,
                                                       final String bulletinASupportedNames,
                                                       final String csvSupportedNames) {
        getFrames().addDefaultEOP2000HistoryLoaders(
                rapidDataColumnsSupportedNames,
                xmlSupportedNames,
                eopC04SupportedNames,
                bulletinBSupportedNames,
                bulletinASupportedNames,
                csvSupportedNames);
    }

    /** Add a loader for Earth Orientation Parameters history.
     * @param conventions IERS conventions to which EOP history applies
     * @param loader custom loader to add for the EOP history
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String, String)
     * @see #clearEOPHistoryLoaders()
     */
    @DefaultDataContext
    public static void addEOPHistoryLoader(final IERSConventions conventions, final EopHistoryLoader loader) {
        getFrames().addEOPHistoryLoader(conventions, loader);
    }

    /** Clear loaders for Earth Orientation Parameters history.
     * @see #addEOPHistoryLoader(IERSConventions, EopHistoryLoader)
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String, String)
     */
    @DefaultDataContext
    public static void clearEOPHistoryLoaders() {
        getFrames().clearEOPHistoryLoaders();
    }

    /** Set the threshold to check EOP continuity.
     * <p>
     * The default threshold (used if this method is never called)
     * is 5 Julian days. If after loading EOP entries some holes
     * between entries exceed this threshold, an exception will
     * be triggered.
     * </p>
     * <p>
     * One case when calling this method is really useful is for
     * applications that use a single Bulletin A, as these bulletins
     * have a roughly one month wide hole for the first bulletin of
     * each month, which contains older final data in addition to the
     * rapid data and the predicted data.
     * </p>
     * @param threshold threshold to use for checking EOP continuity (in seconds)
     */
    @DefaultDataContext
    public static void setEOPContinuityThreshold(final double threshold) {
        getFrames().setEOPContinuityThreshold(threshold);
    }

    /** Get Earth Orientation Parameters history.
     * <p>
     * If no {@link EopHistoryLoader} has been added by calling {@link
     * #addEOPHistoryLoader(IERSConventions, EopHistoryLoader) addEOPHistoryLoader}
     * or if {@link #clearEOPHistoryLoaders() clearEOPHistoryLoaders} has been
     * called afterwards, the {@link #addDefaultEOP1980HistoryLoaders(String, String,
     * String, String, String, String)} and {@link #addDefaultEOP2000HistoryLoaders(String,
     * String, String, String, String, String)} methods will be called automatically with
     * supported file names parameters all set to null, in order to get the default
     * loaders configuration.
     * </p>
     * @param conventions conventions for which EOP history is requested
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return Earth Orientation Parameters history
     */
    @DefaultDataContext
    public static EOPHistory getEOPHistory(final IERSConventions conventions, final boolean simpleEOP) {
        return getFrames().getEOPHistory(conventions, simpleEOP);
    }

    /** Get one of the predefined frames.
     * @param factoryKey key of the frame within the factory
     * @return the predefined frame
     */
    @DefaultDataContext
    public static Frame getFrame(final Predefined factoryKey) {
        return getFrames().getFrame(factoryKey);
    }

    /** Get the unique GCRF frame.
     * <p>The GCRF frame is the root frame in the frame tree.</p>
     * @return the unique instance of the GCRF frame
     */
    @DefaultDataContext
    public static Frame getGCRF() {
        return getFrames().getGCRF();
    }

    /** Get the unique ICRF frame.
     * <p>The ICRF frame is centered at solar system barycenter and aligned
     * with GCRF.</p>
     * @return the unique instance of the ICRF frame
     */
    @DefaultDataContext
    public static Frame getICRF() {
        return getFrames().getICRF();
    }

    /** Get the ecliptic frame.
     * The IAU defines the ecliptic as "the plane perpendicular to the mean heliocentric
     * orbital angular momentum vector of the Earth-Moon barycentre in the BCRS (IAU 2006
     * Resolution B1)." The +z axis is aligned with the angular momentum vector, and the +x
     * axis is aligned with +x axis of {@link FramesFactory#getMOD(IERSConventions) MOD}.
     *
     * <p> This implementation agrees with the JPL 406 ephemerides to within 0.5 arc seconds.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static Frame getEcliptic(final IERSConventions conventions) {
        return getFrames().getEcliptic(conventions);
    }

    /** Get the unique EME2000 frame.
     * <p>The EME2000 frame is also called the J2000 frame.
     * The former denomination is preferred in Orekit.</p>
     * @return the unique instance of the EME2000 frame
     */
    @DefaultDataContext
    public static FactoryManagedFrame getEME2000() {
        return getFrames().getEME2000();
    }

    /** Get an unspecified International Terrestrial Reference Frame.
     * <p>
     * The frame returned uses the {@link EOPEntry Earth Orientation Parameters}
     * blindly. So if for example one loads only EOP 14 C04 files to retrieve
     * the parameters, the frame will be an {@link ITRFVersion#ITRF_2014}. However,
     * if parameters are loaded from different files types, or even for file
     * types that changed their reference (like Bulletin A switching from
     * {@link ITRFVersion#ITRF_2008} to {@link ITRFVersion#ITRF_2014} starting
     * with Vol. XXXI No. 013 published on 2018-03-29), then the ITRF returned
     * by this method will jump from one version to another version.
     * </p>
     * <p>
     * IF a specific version of ITRF is needed, then {@link #getITRF(ITRFVersion,
     * IERSConventions, boolean)} should be used instead.
     * </p>
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
          * @see #getITRF(ITRFVersion, IERSConventions, boolean)
     * @since 6.1
     */
    @DefaultDataContext
    public static FactoryManagedFrame getITRF(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        return getFrames().getITRF(conventions, simpleEOP);
    }

    /** Get the TIRF reference frame, ignoring tidal effects.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
          * library cannot be read.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getTIRF(final IERSConventions conventions) {
        return getFrames().getTIRF(conventions);
    }

    /** Get a specific International Terrestrial Reference Frame.
     * <p>
     * Note that if a specific version of ITRF is required, then {@code simpleEOP}
     * should most probably be set to {@code false}, as ignoring tidal effects
     * has an effect of the same order of magnitude as the differences between
     * the various {@link ITRFVersion ITRF versions}.
     * </p>
     * @param version ITRF version
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
          * @since 9.2
     */
    @DefaultDataContext
    public static VersionedITRF getITRF(final ITRFVersion version,
                                        final IERSConventions conventions,
                                        final boolean simpleEOP) {
        return getFrames().getITRF(version, conventions, simpleEOP);
    }

    /** Build an uncached International Terrestrial Reference Frame with specific {@link EOPHistory EOP history}.
     * <p>
     * This frame and its parent frames (TIRF and CIRF) will <em>not</em> be cached, they are
     * rebuilt from scratch each time this method is called. This factory method is intended
     * to be used when EOP history is changed at run time. For regular ITRF use, the
     * {@link #getITRF(IERSConventions, boolean)} and {link {@link #getITRF(ITRFVersion, IERSConventions, boolean)}
     * are more suitable.
     * </p>
     * @param eopHistory EOP history
     * @param utc UTC time scale
     * @return an ITRF frame with specified EOP history
     * @since 12.0
     */
    public static Frame buildUncachedITRF(final EOPHistory eopHistory, final UTCScale utc) {
        final TimeScales timeScales = TimeScales.of(utc.getBaseOffsets(),
                                                    (conventions, timescales) -> eopHistory.getEntries());
        final UT1Scale   ut1        = timeScales.getUT1(eopHistory.getConventions(), eopHistory.isSimpleEop());
        final Frames     frames     = Frames.of(timeScales, (CelestialBodies) null);
        return frames.buildUncachedITRF(ut1);
    }

    /** Get the TIRF reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     * @since 6.1
     */
    @DefaultDataContext
    public static FactoryManagedFrame getTIRF(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        return getFrames().getTIRF(conventions, simpleEOP);
    }

    /** Get the CIRF2000 reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getCIRF(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        return getFrames().getCIRF(conventions, simpleEOP);
    }

    /** Get the VEIS 1950 reference frame.
     * <p>Its parent frame is the GTOD frame with IERS 1996 conventions without EOP corrections.</p>
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getVeis1950() {
        return getFrames().getVeis1950();
    }

    /** Get the equinox-based ITRF reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     * @since 6.1
     */
    @DefaultDataContext
    public static FactoryManagedFrame getITRFEquinox(final IERSConventions conventions,
                                                     final boolean simpleEOP) {
        return getFrames().getITRFEquinox(conventions, simpleEOP);
    }

    /** Get the GTOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP correction parameters.
     * Beware that setting this parameter to {@code false} leads to crude accuracy
     * (order of magnitudes for errors might be above 250m in LEO and 1400m in GEO).
     * For this reason, setting this parameter to false is restricted to {@link
     * IERSConventions#IERS_1996 IERS 1996} conventions, and hence the {@link
     * IERSConventions IERS conventions} cannot be freely chosen here.
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (here, dut1 and lod)
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getGTOD(final boolean applyEOPCorr) {
        return getFrames().getGTOD(applyEOPCorr);
    }

    /** Get the GTOD reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getGTOD(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        return getFrames().getGTOD(conventions, simpleEOP);
    }

    /** Get the TOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP correction parameters.
     * Beware that setting this parameter to {@code false} leads to crude accuracy
     * (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * For this reason, setting this parameter to false is restricted to {@link
     * IERSConventions#IERS_1996 IERS 1996} conventions, and hence the {@link
     * IERSConventions IERS conventions} cannot be freely chosen here.
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (here, nutation)
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getTOD(final boolean applyEOPCorr) {
        return getFrames().getTOD(applyEOPCorr);
    }

    /** Get the TOD reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getTOD(final IERSConventions conventions,
                                             final boolean simpleEOP) {
        return getFrames().getTOD(conventions, simpleEOP);
    }

    /** Get the MOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP correction parameters.
     * Beware that setting this parameter to {@code false} leads to crude accuracy
     * (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * For this reason, setting this parameter to false is restricted to {@link
     * IERSConventions#IERS_1996 IERS 1996} conventions, and hence the {@link
     * IERSConventions IERS conventions} cannot be freely chosen here.
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (EME2000/GCRF bias compensation)
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getMOD(final boolean applyEOPCorr) {
        return getFrames().getMOD(applyEOPCorr);
    }

    /** Get the MOD reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getMOD(final IERSConventions conventions) {
        return getFrames().getMOD(conventions);
    }

    /** Get the TEME reference frame.
     * <p>
     * The TEME frame is used for the SGP4 model in TLE propagation. This frame has <em>no</em>
     * official definition and there are some ambiguities about whether it should be used
     * as "of date" or "of epoch". This frame should therefore be used <em>only</em> for
     * TLE propagation and not for anything else, as recommended by the CCSDS Orbit Data Message
     * blue book.
     * </p>
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getTEME() {
        return getFrames().getTEME();
    }

    /** Get the PZ-90.11 (Parametry Zemly  – 1990.11) reference frame.
     * <p>
     * The PZ-90.11 reference system was updated on all operational
     * GLONASS satellites starting from 3:00 pm on December 31, 2013.
     * </p>
     * <p>
     * The transition between parent frame (ITRF-2008) and PZ-90.11 frame is performed using
     * a seven parameters Helmert transformation.
     * <pre>
     *    From       To      ΔX(m)   ΔY(m)   ΔZ(m)   RX(mas)   RY(mas)  RZ(mas)   Epoch
     * ITRF-2008  PZ-90.11  +0.003  +0.001  -0.000   +0.019    -0.042   +0.002     2010
     * </pre>
     * @see "Springer Handbook of Global Navigation Satellite Systems, Peter Teunissen & Oliver Montenbruck"
     *
     * @param convention IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    @DefaultDataContext
    public static FactoryManagedFrame getPZ9011(final IERSConventions convention,
                                                final boolean simpleEOP) {
        return getFrames().getPZ9011(convention, simpleEOP);
    }

    /** Get the transform between two frames, suppressing all interpolation.
     * <p>
     * This method is similar to {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * except it removes the performance enhancing interpolation features that are
     * added by the {@link FramesFactory factory} to some frames, in order to focus
     * on accuracy. The interpolation features are intended to save processing time
     * by avoiding doing some lengthy computation like nutation evaluation at each
     * time step and caching some results. This method can be used to avoid this,
     * when very high accuracy is desired, or for testing purposes. It should be
     * used with care, as doing the full computation is <em>really</em> costly for
     * some frames.
     * </p>
     * @param from frame from which transformation starts
     * @param to frame to which transformation ends
     * @param date date of the transform
     * @return transform between the two frames, avoiding interpolation
     */
    public static Transform getNonInterpolatingTransform(final Frame from, final Frame to,
                                                         final AbsoluteDate date) {

        // common ancestor to both frames in the frames tree
        Frame currentF = from.getDepth() > to.getDepth() ? from.getAncestor(from.getDepth() - to.getDepth()) : from;
        Frame currentT = from.getDepth() > to.getDepth() ? to : to.getAncestor(to.getDepth() - from.getDepth());
        while (currentF != currentT) {
            currentF = currentF.getParent();
            currentT = currentT.getParent();
        }
        final Frame common = currentF;

        // transform from common to origin
        Transform commonToOrigin = Transform.IDENTITY;
        for (Frame frame = from; frame != common; frame = frame.getParent()) {
            commonToOrigin = new Transform(date,
                                             peel(frame.getTransformProvider()).getTransform(date),
                                             commonToOrigin);
        }

        // transform from destination up to common
        Transform commonToDestination = Transform.IDENTITY;
        for (Frame frame = to; frame != common; frame = frame.getParent()) {
            commonToDestination = new Transform(date,
                                                peel(frame.getTransformProvider()).getTransform(date),
                                                commonToDestination);
        }

        // transform from origin to destination via common
        return new Transform(date, commonToOrigin.getInverse(), commonToDestination);

    }

    /* The methods below are static helper methods for Frame and TransformProvider. */

    /** Get the transform between two frames, suppressing all interpolation.
     * <p>
     * This method is similar to {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * except it removes the performance enhancing interpolation features that are
     * added by the {@link FramesFactory factory} to some frames, in order to focus
     * on accuracy. The interpolation features are intended to save processing time
     * by avoiding doing some lengthy computation like nutation evaluation at each
     * time step and caching some results. This method can be used to avoid this,
     * when very high accuracy is desired, or for testing purposes. It should be
     * used with care, as doing the full computation is <em>really</em> costly for
     * some frames.
     * </p>
     * @param from frame from which transformation starts
     * @param to frame to which transformation ends
     * @param date date of the transform
     * @param <T> type of the field elements
     * @return transform between the two frames, avoiding interpolation
     * @since 9.0
     */
    public static <T extends CalculusFieldElement<T>> FieldTransform<T> getNonInterpolatingTransform(final Frame from, final Frame to,
                                                                                                 final FieldAbsoluteDate<T> date) {

        // common ancestor to both frames in the frames tree
        Frame currentF = from.getDepth() > to.getDepth() ? from.getAncestor(from.getDepth() - to.getDepth()) : from;
        Frame currentT = from.getDepth() > to.getDepth() ? to : to.getAncestor(to.getDepth() - from.getDepth());
        while (currentF != currentT) {
            currentF = currentF.getParent();
            currentT = currentT.getParent();
        }
        final Frame common = currentF;

        // transform from common to origin
        FieldTransform<T> commonToOrigin = FieldTransform.getIdentity(date.getField());
        for (Frame frame = from; frame != common; frame = frame.getParent()) {
            commonToOrigin = new FieldTransform<>(date,
                                                   peel(frame.getTransformProvider()).getTransform(date),
                                                   commonToOrigin);
        }

        // transform from destination up to common
        FieldTransform<T> commonToDestination = FieldTransform.getIdentity(date.getField());
        for (Frame frame = to; frame != common; frame = frame.getParent()) {
            commonToDestination = new FieldTransform<>(date,
                                                       peel(frame.getTransformProvider()).getTransform(date),
                                                       commonToDestination);
        }

        // transform from origin to destination via common
        return new FieldTransform<>(date, commonToOrigin.getInverse(), commonToDestination);

    }

    /** Retrieve EOP from a frame hierarchy.
     * <p>
     * The frame hierarchy tree is walked from specified frame up to root
     * traversing parent frames, and the providers are checked to see if they
     * reference EOP history. The first EOP history found is returned.
     * </p>
     * @param start frame from which to start search, will typically be some
     * Earth related frame, like a topocentric frame or an ITRF frame
     * @return EOP history found while walking the frames tree, or null if
     * no EOP history is found
     * @since 9.1
     */
    public static EOPHistory findEOP(final Frame start) {

        for (Frame frame = start; frame != null; frame = frame.getParent()) {

            TransformProvider peeled = frame.getTransformProvider();

            boolean peeling = true;
            while (peeling) {
                if (peeled instanceof InterpolatingTransformProvider) {
                    peeled = ((InterpolatingTransformProvider) peeled).getRawProvider();
                } else if (peeled instanceof ShiftingTransformProvider) {
                    peeled = ((ShiftingTransformProvider) peeled).getRawProvider();
                } else if (peeled instanceof EOPBasedTransformProvider &&
                           ((EOPBasedTransformProvider) peeled).getEOPHistory() != null) {
                    return ((EOPBasedTransformProvider) peeled).getEOPHistory();
                } else {
                    peeling = false;
                }
            }

        }

        // no history found
        return null;

    }

    /** Peel interpolation and shifting from a transform provider.
     * @param provider transform provider to peel
     * @return peeled transform provider
     */
    private static TransformProvider peel(final TransformProvider provider) {

        TransformProvider peeled = provider;

        boolean peeling = true;
        while (peeling) {
            if (peeled instanceof InterpolatingTransformProvider) {
                peeled = ((InterpolatingTransformProvider) peeled).getRawProvider();
            } else if (peeled instanceof ShiftingTransformProvider) {
                peeled = ((ShiftingTransformProvider) peeled).getRawProvider();
            } else if (peeled instanceof EOPBasedTransformProvider &&
                       ((EOPBasedTransformProvider) peeled).getEOPHistory() != null &&
                       ((EOPBasedTransformProvider) peeled).getEOPHistory().cachesTidalCorrection()) {
                peeled = ((EOPBasedTransformProvider) peeled).getNonInterpolatingProvider();
            } else {
                peeling = false;
            }
        }

        return peeled;

    }

}
