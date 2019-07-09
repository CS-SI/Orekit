/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.frames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.RealFieldElement;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;


/** Factory for predefined reference frames.
 *
 * <h1> FramesFactory Presentation </h1>
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
 */
public class FramesFactory {

    /** Default regular expression for the Rapid Data and Prediction EOP columns files (IAU1980 compatibles). */
    public static final String RAPID_DATA_PREDICTION_COLUMNS_1980_FILENAME = "^finals\\.[^.]*$";

    /** Default regular expression for the Rapid Data and Prediction EOP XML files (IAU1980 compatibles). */
    public static final String RAPID_DATA_PREDICTION_XML_1980_FILENAME = "^finals\\..*\\.xml$";

    /** Default regular expression for the EOPC04 files (IAU1980 compatibles). */
    public static final String EOPC04_1980_FILENAME = "^eopc04_\\d\\d\\.(\\d\\d)$";

    /** Default regular expression for the BulletinB files (IAU1980 compatibles). */
    public static final String BULLETINB_1980_FILENAME = "^bulletinb(_IAU1980)?((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** Default regular expression for the Rapid Data and Prediction EOP columns files (IAU2000 compatibles). */
    public static final String RAPID_DATA_PREDICITON_COLUMNS_2000_FILENAME = "^finals2000A\\.[^.]*$";

    /** Default regular expression for the Rapid Data and Prediction EOP XML files (IAU2000 compatibles). */
    public static final String RAPID_DATA_PREDICITON_XML_2000_FILENAME = "^finals2000A\\..*\\.xml$";

    /** Default regular expression for the EOPC04 files (IAU2000 compatibles). */
    public static final String EOPC04_2000_FILENAME = "^eopc04_\\d\\d_IAU2000\\.(\\d\\d)$";

    /** Default regular expression for the BulletinB files (IAU2000 compatibles). */
    public static final String BULLETINB_2000_FILENAME = "^bulletinb(_IAU2000)?((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** Default regular expression for the BulletinA files (IAU1980 and IAU2000 compatibles). */
    public static final String BULLETINA_FILENAME = "^bulletina-[ivxlcdm]+-\\d\\d\\d\\.txt$";

    /** Predefined frames. */
    private static transient Map<Predefined, FactoryManagedFrame> FRAMES =
        new HashMap<Predefined, FactoryManagedFrame>();

    /** Predefined versioned ITRF frames. */
    private static transient Map<ITRFKey, VersionedITRF> VERSIONED_ITRF_FRAMES =
        new HashMap<ITRFKey, VersionedITRF>();

    /** Loaders for Earth Orientation parameters. */
    private static final Map<IERSConventions, List<EOPHistoryLoader>> EOP_HISTORY_LOADERS =
        new HashMap<IERSConventions, List<EOPHistoryLoader>>();

    /** Threshold for EOP continuity. */
    private static double EOP_CONTINUITY_THRESHOLD = 5 * Constants.JULIAN_DAY;

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private FramesFactory() {
    }

    /** Add the default loaders EOP history (IAU 1980 precession/nutation).
     * <p>
     * The default loaders look for IERS EOP C04 and bulletins B files. They
     * correspond to {@link IERSConventions#IERS_1996 IERS 1996} conventions.
     * </p>
     * @param rapidDataColumnsSupportedNames regular expression for supported
     * rapid data columns EOP files names
     * (may be null if the default IERS file names are used)
     * @param rapidDataXMLSupportedNames regular expression for supported
     * rapid data XML EOP files names
     * (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP C04 files names
     * (may be null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names
     * (may be null if the default IERS file names are used)
     * @param bulletinASupportedNames regular expression for supported bulletin A files names
     * (may be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">IERS EOP C04 files</a>
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP2000HistoryLoaders(String, String, String, String, String)
     */
    public static void addDefaultEOP1980HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames,
                                                       final String bulletinASupportedNames) {
        final String rapidColNames =
                (rapidDataColumnsSupportedNames == null) ?
                RAPID_DATA_PREDICTION_COLUMNS_1980_FILENAME : rapidDataColumnsSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                            new RapidDataAndPredictionColumnsLoader(false, rapidColNames));
        final String rapidXmlNames =
                (rapidDataXMLSupportedNames == null) ?
                RAPID_DATA_PREDICTION_XML_1980_FILENAME : rapidDataXMLSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                            new RapidDataAndPredictionXMLLoader(rapidXmlNames));
        final String eopcNames =
                (eopC04SupportedNames == null) ? EOPC04_1980_FILENAME : eopC04SupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                            new EOPC04FilesLoader(eopcNames));
        final String bulBNames =
                (bulletinBSupportedNames == null) ? BULLETINB_1980_FILENAME : bulletinBSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                            new BulletinBFilesLoader(bulBNames));
        final String bulANames =
                    (bulletinASupportedNames == null) ? BULLETINA_FILENAME : bulletinASupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                            new BulletinAFilesLoader(bulANames));
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
     * @param rapidDataXMLSupportedNames regular expression for supported
     * rapid data XML EOP files names
     * (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP C04 files names
     * (may be null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names
     * (may be null if the default IERS file names are used)
     * @param bulletinASupportedNames regular expression for supported bulletin A files names
     * (may be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">IERS EOP C04 files</a>
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String)
     */
    public static void addDefaultEOP2000HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames,
                                                       final String bulletinASupportedNames) {
        final String rapidColNames =
                (rapidDataColumnsSupportedNames == null) ?
                RAPID_DATA_PREDICITON_COLUMNS_2000_FILENAME : rapidDataColumnsSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                            new RapidDataAndPredictionColumnsLoader(true, rapidColNames));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                            new RapidDataAndPredictionColumnsLoader(true, rapidColNames));
        final String rapidXmlNames =
            (rapidDataXMLSupportedNames == null) ?
            RAPID_DATA_PREDICITON_XML_2000_FILENAME : rapidDataXMLSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                            new RapidDataAndPredictionXMLLoader(rapidXmlNames));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                            new RapidDataAndPredictionXMLLoader(rapidXmlNames));
        final String eopcNames =
            (eopC04SupportedNames == null) ? EOPC04_2000_FILENAME : eopC04SupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                            new EOPC04FilesLoader(eopcNames));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                            new EOPC04FilesLoader(eopcNames));
        final String bulBNames =
            (bulletinBSupportedNames == null) ? BULLETINB_2000_FILENAME : bulletinBSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                            new BulletinBFilesLoader(bulBNames));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                            new BulletinBFilesLoader(bulBNames));
        final String bulANames =
                (bulletinASupportedNames == null) ? BULLETINA_FILENAME : bulletinASupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                            new BulletinAFilesLoader(bulANames));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                            new BulletinAFilesLoader(bulANames));
    }

    /** Add a loader for Earth Orientation Parameters history.
     * @param conventions IERS conventions to which EOP history applies
     * @param loader custom loader to add for the EOP history
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String)
     * @see #clearEOPHistoryLoaders()
     */
    public static void addEOPHistoryLoader(final IERSConventions conventions, final EOPHistoryLoader loader) {
        synchronized (EOP_HISTORY_LOADERS) {
            if (!EOP_HISTORY_LOADERS.containsKey(conventions)) {
                EOP_HISTORY_LOADERS.put(conventions, new ArrayList<EOPHistoryLoader>());
            }
            EOP_HISTORY_LOADERS.get(conventions).add(loader);
        }
    }

    /** Clear loaders for Earth Orientation Parameters history.
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String)
     */
    public static void clearEOPHistoryLoaders() {
        synchronized (EOP_HISTORY_LOADERS) {
            EOP_HISTORY_LOADERS.clear();
        }
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
    public static void setEOPContinuityThreshold(final double threshold) {
        EOP_CONTINUITY_THRESHOLD = threshold;
    }

    /** Get Earth Orientation Parameters history.
     * <p>
     * If no {@link EOPHistoryLoader} has been added by calling {@link
     * #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader) addEOPHistoryLoader}
     * or if {@link #clearEOPHistoryLoaders() clearEOPHistoryLoaders} has been
     * called afterwards, the {@link #addDefaultEOP1980HistoryLoaders(String, String,
     * String, String, String)} and {@link #addDefaultEOP2000HistoryLoaders(String,
     * String, String, String, String)} methods will be called automatically with
     * supported file names parameters all set to null, in order to get the default
     * loaders configuration.
     * </p>
     * @param conventions conventions for which EOP history is requested
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return Earth Orientation Parameters history
     */
    public static EOPHistory getEOPHistory(final IERSConventions conventions, final boolean simpleEOP) {

        synchronized (EOP_HISTORY_LOADERS) {

            if (EOP_HISTORY_LOADERS.isEmpty()) {
                // set up using default loaders
                addDefaultEOP2000HistoryLoaders(null, null, null, null, null);
                addDefaultEOP1980HistoryLoaders(null, null, null, null, null);
            }

            // TimeStamped based set needed to remove duplicates
            OrekitException pendingException = null;
            final SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());

            // try to load canonical data if available
            if (EOP_HISTORY_LOADERS.containsKey(conventions)) {
                for (final EOPHistoryLoader loader : EOP_HISTORY_LOADERS.get(conventions)) {
                    try {
                        loader.fillHistory(conventions.getNutationCorrectionConverter(), data);
                    } catch (OrekitException oe) {
                        pendingException = oe;
                    }
                }
            }

            if (data.isEmpty() && pendingException != null) {
                throw pendingException;
            }

            final EOPHistory history = new EOPHistory(conventions, data, simpleEOP);
            history.checkEOPContinuity(EOP_CONTINUITY_THRESHOLD);
            return history;

        }

    }

    /** Get one of the predefined frames.
     * @param factoryKey key of the frame within the factory
     * @return the predefined frame
     */
    public static Frame getFrame(final Predefined factoryKey) {
        switch (factoryKey) {
            case GCRF :
                return getGCRF();
            case ICRF :
                return getICRF();
            case ECLIPTIC_CONVENTIONS_1996 :
                return getEcliptic(IERSConventions.IERS_1996);
            case ECLIPTIC_CONVENTIONS_2003 :
                return getEcliptic(IERSConventions.IERS_2003);
            case ECLIPTIC_CONVENTIONS_2010 :
                return getEcliptic(IERSConventions.IERS_2010);
            case EME2000 :
                return getEME2000();
            case ITRF_CIO_CONV_2010_SIMPLE_EOP :
                return getITRF(IERSConventions.IERS_2010, true);
            case ITRF_CIO_CONV_2010_ACCURATE_EOP :
                return getITRF(IERSConventions.IERS_2010, false);
            case ITRF_CIO_CONV_2003_SIMPLE_EOP :
                return getITRF(IERSConventions.IERS_2003, true);
            case ITRF_CIO_CONV_2003_ACCURATE_EOP :
                return getITRF(IERSConventions.IERS_2003, false);
            case ITRF_CIO_CONV_1996_SIMPLE_EOP :
                return getITRF(IERSConventions.IERS_1996, true);
            case ITRF_CIO_CONV_1996_ACCURATE_EOP :
                return getITRF(IERSConventions.IERS_1996, false);
            case ITRF_EQUINOX_CONV_2010_SIMPLE_EOP :
                return getITRFEquinox(IERSConventions.IERS_2010, true);
            case ITRF_EQUINOX_CONV_2010_ACCURATE_EOP :
                return getITRFEquinox(IERSConventions.IERS_2010, false);
            case ITRF_EQUINOX_CONV_2003_SIMPLE_EOP :
                return getITRFEquinox(IERSConventions.IERS_2003, true);
            case ITRF_EQUINOX_CONV_2003_ACCURATE_EOP :
                return getITRFEquinox(IERSConventions.IERS_2003, false);
            case ITRF_EQUINOX_CONV_1996_SIMPLE_EOP :
                return getITRFEquinox(IERSConventions.IERS_1996, true);
            case ITRF_EQUINOX_CONV_1996_ACCURATE_EOP :
                return getITRFEquinox(IERSConventions.IERS_1996, false);
            case TIRF_CONVENTIONS_2010_SIMPLE_EOP :
                return getTIRF(IERSConventions.IERS_2010, true);
            case TIRF_CONVENTIONS_2010_ACCURATE_EOP :
                return getTIRF(IERSConventions.IERS_2010, false);
            case TIRF_CONVENTIONS_2003_SIMPLE_EOP :
                return getTIRF(IERSConventions.IERS_2003, true);
            case TIRF_CONVENTIONS_2003_ACCURATE_EOP :
                return getTIRF(IERSConventions.IERS_2003, false);
            case TIRF_CONVENTIONS_1996_SIMPLE_EOP :
                return getTIRF(IERSConventions.IERS_1996, true);
            case TIRF_CONVENTIONS_1996_ACCURATE_EOP :
                return getTIRF(IERSConventions.IERS_1996, false);
            case CIRF_CONVENTIONS_2010_ACCURATE_EOP :
                return getCIRF(IERSConventions.IERS_2010, false);
            case CIRF_CONVENTIONS_2010_SIMPLE_EOP :
                return getCIRF(IERSConventions.IERS_2010, true);
            case CIRF_CONVENTIONS_2003_ACCURATE_EOP :
                return getCIRF(IERSConventions.IERS_2003, false);
            case CIRF_CONVENTIONS_2003_SIMPLE_EOP :
                return getCIRF(IERSConventions.IERS_2003, true);
            case CIRF_CONVENTIONS_1996_ACCURATE_EOP :
                return getCIRF(IERSConventions.IERS_1996, false);
            case CIRF_CONVENTIONS_1996_SIMPLE_EOP :
                return getCIRF(IERSConventions.IERS_1996, true);
            case VEIS_1950 :
                return getVeis1950();
            case GTOD_WITHOUT_EOP_CORRECTIONS :
                return getGTOD(IERSConventions.IERS_1996, false, true);
            case GTOD_CONVENTIONS_2010_ACCURATE_EOP :
                return getGTOD(IERSConventions.IERS_2010, true, false);
            case GTOD_CONVENTIONS_2010_SIMPLE_EOP :
                return getGTOD(IERSConventions.IERS_2010, true, true);
            case GTOD_CONVENTIONS_2003_ACCURATE_EOP :
                return getGTOD(IERSConventions.IERS_2003, true, false);
            case GTOD_CONVENTIONS_2003_SIMPLE_EOP :
                return getGTOD(IERSConventions.IERS_2003, true, true);
            case GTOD_CONVENTIONS_1996_ACCURATE_EOP :
                return getGTOD(IERSConventions.IERS_1996, true, false);
            case GTOD_CONVENTIONS_1996_SIMPLE_EOP :
                return getGTOD(IERSConventions.IERS_1996, true, true);
            case TOD_WITHOUT_EOP_CORRECTIONS :
                return getTOD(IERSConventions.IERS_1996, false, true);
            case TOD_CONVENTIONS_2010_ACCURATE_EOP :
                return getTOD(IERSConventions.IERS_2010, true, false);
            case TOD_CONVENTIONS_2010_SIMPLE_EOP :
                return getTOD(IERSConventions.IERS_2010, true, true);
            case TOD_CONVENTIONS_2003_ACCURATE_EOP :
                return getTOD(IERSConventions.IERS_2003, true, false);
            case TOD_CONVENTIONS_2003_SIMPLE_EOP :
                return getTOD(IERSConventions.IERS_2003, true, true);
            case TOD_CONVENTIONS_1996_ACCURATE_EOP :
                return getTOD(IERSConventions.IERS_1996, true, false);
            case TOD_CONVENTIONS_1996_SIMPLE_EOP :
                return getTOD(IERSConventions.IERS_1996, true, true);
            case MOD_WITHOUT_EOP_CORRECTIONS :
                return getMOD(IERSConventions.IERS_1996, false);
            case MOD_CONVENTIONS_2010 :
                return getMOD(IERSConventions.IERS_2010, true);
            case MOD_CONVENTIONS_2003 :
                return getMOD(IERSConventions.IERS_2003, true);
            case MOD_CONVENTIONS_1996 :
                return getMOD(IERSConventions.IERS_1996, true);
            case TEME :
                return getTEME();
            case PZ90_11 :
                return getPZ9011(IERSConventions.IERS_2010, true);
            default :
                // this should never happen
                throw new OrekitInternalError(null);
        }
    }

    /** Get the unique GCRF frame.
     * <p>The GCRF frame is the root frame in the frame tree.</p>
     * @return the unique instance of the GCRF frame
     */
    public static Frame getGCRF() {
        return Frame.getRoot();
    }

    /** Get the unique ICRF frame.
     * <p>The ICRF frame is centered at solar system barycenter and aligned
     * with GCRF.</p>
     * @return the unique instance of the ICRF frame
     */
    public static Frame getICRF() {
        return CelestialBodyFactory.getSolarSystemBarycenter().getInertiallyOrientedFrame();
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
    public static Frame getEcliptic(final IERSConventions conventions) {
        synchronized (FramesFactory.class) {

            final Predefined factoryKey;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = Predefined.ECLIPTIC_CONVENTIONS_1996;
                    break;
                case IERS_2003 :
                    factoryKey = Predefined.ECLIPTIC_CONVENTIONS_2003;
                    break;
                case IERS_2010 :
                    factoryKey = Predefined.ECLIPTIC_CONVENTIONS_2010;
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            final Frame parent = getMOD(conventions);

            // try to find an already built frame
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(parent, new EclipticProvider(conventions),
                                                true, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the unique EME2000 frame.
     * <p>The EME2000 frame is also called the J2000 frame.
     * The former denomination is preferred in Orekit.</p>
     * @return the unique instance of the EME2000 frame
     */
    public static FactoryManagedFrame getEME2000() {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            FactoryManagedFrame frame = FRAMES.get(Predefined.EME2000);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(getGCRF(), new EME2000Provider(), true, Predefined.EME2000);
                FRAMES.put(Predefined.EME2000, frame);
            }

            return frame;

        }
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
    public static FactoryManagedFrame getITRF(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = simpleEOP ?
                                 Predefined.ITRF_CIO_CONV_1996_SIMPLE_EOP :
                                 Predefined.ITRF_CIO_CONV_1996_ACCURATE_EOP;
                    break;
                case IERS_2003 :
                    factoryKey = simpleEOP ?
                                 Predefined.ITRF_CIO_CONV_2003_SIMPLE_EOP :
                                 Predefined.ITRF_CIO_CONV_2003_ACCURATE_EOP;
                    break;
                case IERS_2010 :
                    factoryKey = simpleEOP ?
                                 Predefined.ITRF_CIO_CONV_2010_SIMPLE_EOP :
                                 Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP;
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tirfFrame = getTIRF(conventions, simpleEOP);
                final TIRFProvider tirfProvider = (TIRFProvider) tirfFrame.getTransformProvider();
                frame = new FactoryManagedFrame(tirfFrame,
                                                new ITRFProvider(tirfProvider.getEOPHistory()),
                                                false, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the TIRF reference frame, ignoring tidal effects.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
          * library cannot be read.
     */
    public static FactoryManagedFrame getTIRF(final IERSConventions conventions) {
        return getTIRF(conventions, true);
    }

    /** Get an specific International Terrestrial Reference Frame.
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
    public static VersionedITRF getITRF(final ITRFVersion version,
                                        final IERSConventions conventions,
                                        final boolean simpleEOP) {
        synchronized (FramesFactory.class) {
            // try to find an already built frame
            final ITRFKey key = new ITRFKey(version, conventions, simpleEOP);
            VersionedITRF frame = VERSIONED_ITRF_FRAMES.get(key);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final FactoryManagedFrame rawITRF = getITRF(conventions, simpleEOP);
                frame = new VersionedITRF(rawITRF.getParent(), version,
                                          (ITRFProvider) rawITRF.getTransformProvider(),
                                          version.toString().replace('_', '-') +
                                          "/" +
                                          rawITRF.getName());
                VERSIONED_ITRF_FRAMES.put(key, frame);
            }

            return frame;

        }
    }

    /** Get the TIRF reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     * @since 6.1
     */
    public static FactoryManagedFrame getTIRF(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = simpleEOP ?
                                 Predefined.TIRF_CONVENTIONS_1996_SIMPLE_EOP :
                                 Predefined.TIRF_CONVENTIONS_1996_ACCURATE_EOP;
                    break;
                case IERS_2003 :
                    factoryKey = simpleEOP ?
                                 Predefined.TIRF_CONVENTIONS_2003_SIMPLE_EOP :
                                 Predefined.TIRF_CONVENTIONS_2003_ACCURATE_EOP;
                    break;
                case IERS_2010 :
                    factoryKey = simpleEOP ?
                                 Predefined.TIRF_CONVENTIONS_2010_SIMPLE_EOP :
                                 Predefined.TIRF_CONVENTIONS_2010_ACCURATE_EOP;
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame cirf = getCIRF(conventions, simpleEOP);
                final ShiftingTransformProvider cirfInterpolating =
                        (ShiftingTransformProvider) cirf.getTransformProvider();
                final CIRFProvider cirfRaw = (CIRFProvider) cirfInterpolating.getRawProvider();
                final EOPHistory eopHistory = cirfRaw.getEOPHistory();
                frame = new FactoryManagedFrame(cirf, new TIRFProvider(eopHistory), false, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the CIRF2000 reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    public static FactoryManagedFrame getCIRF(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = simpleEOP ?
                                 Predefined.CIRF_CONVENTIONS_1996_SIMPLE_EOP :
                                 Predefined.CIRF_CONVENTIONS_1996_ACCURATE_EOP;
                    break;
                case IERS_2003 :
                    factoryKey = simpleEOP ?
                                 Predefined.CIRF_CONVENTIONS_2003_SIMPLE_EOP :
                                 Predefined.CIRF_CONVENTIONS_2003_ACCURATE_EOP;
                    break;
                case IERS_2010 :
                    factoryKey = simpleEOP ?
                                 Predefined.CIRF_CONVENTIONS_2010_SIMPLE_EOP :
                                 Predefined.CIRF_CONVENTIONS_2010_ACCURATE_EOP;
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final EOPHistory eopHistory = FramesFactory.getEOPHistory(conventions, simpleEOP);
                final TransformProvider shifting =
                        new ShiftingTransformProvider(new CIRFProvider(eopHistory),
                                                      CartesianDerivativesFilter.USE_PVA,
                                                      AngularDerivativesFilter.USE_R,
                                                      6, Constants.JULIAN_DAY / 24,
                                                      OrekitConfiguration.getCacheSlotsNumber(),
                                                      Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(getGCRF(), shifting, true, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the VEIS 1950 reference frame.
     * <p>Its parent frame is the GTOD frame with IERS 1996 conventions without EOP corrections.<p>
     * @return the selected reference frame singleton.
     */
    public static FactoryManagedFrame getVeis1950() {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.VEIS_1950;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(FramesFactory.getGTOD(IERSConventions.IERS_1996, false, true),
                                                new VEISProvider(), true, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the equinox-based ITRF reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
          * @since 6.1
     */
    public static FactoryManagedFrame getITRFEquinox(final IERSConventions conventions,
                                                     final boolean simpleEOP) {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = simpleEOP ?
                                 Predefined.ITRF_EQUINOX_CONV_1996_SIMPLE_EOP :
                                 Predefined.ITRF_EQUINOX_CONV_1996_ACCURATE_EOP;
                    break;
                case IERS_2003 :
                    factoryKey = simpleEOP ?
                                 Predefined.ITRF_EQUINOX_CONV_2003_SIMPLE_EOP :
                                 Predefined.ITRF_EQUINOX_CONV_2003_ACCURATE_EOP;
                    break;
                case IERS_2010 :
                    factoryKey = simpleEOP ?
                                 Predefined.ITRF_EQUINOX_CONV_2010_SIMPLE_EOP :
                                 Predefined.ITRF_EQUINOX_CONV_2010_ACCURATE_EOP;
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame gtod = getGTOD(conventions, true, simpleEOP);
                final ShiftingTransformProvider gtodShifting =
                        (ShiftingTransformProvider) gtod.getTransformProvider();
                final GTODProvider gtodRaw    = (GTODProvider) gtodShifting.getRawProvider();
                final EOPHistory   eopHistory = gtodRaw.getEOPHistory();
                frame = new FactoryManagedFrame(gtod, new ITRFProvider(eopHistory), false, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
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
    public static FactoryManagedFrame getGTOD(final boolean applyEOPCorr) {
        return getGTOD(IERSConventions.IERS_1996, applyEOPCorr, true);
    }

    /** Get the GTOD reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    public static FactoryManagedFrame getGTOD(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        return getGTOD(conventions, true, simpleEOP);
    }

    /** Get the GTOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP correction parameters.
     * Beware that setting this parameter to {@code false} leads to crude accuracy
     * (order of magnitudes for errors might be above 250m in LEO and 1400m in GEO).
     * For this reason, setting this parameter to false is restricted to {@link
     * IERSConventions#IERS_1996 IERS 1996} conventions, and hence this method is private.
     * </p>
     * @param conventions IERS conventions to apply
     * @param applyEOPCorr if true, EOP corrections are applied (here, dut1 and lod)
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    private static FactoryManagedFrame getGTOD(final IERSConventions conventions,
                                               final boolean applyEOPCorr,
                                               final boolean simpleEOP) {

        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = applyEOPCorr ?
                                 (simpleEOP ? Predefined.GTOD_CONVENTIONS_1996_SIMPLE_EOP : Predefined.GTOD_CONVENTIONS_1996_ACCURATE_EOP) :
                                 Predefined.GTOD_WITHOUT_EOP_CORRECTIONS;
                    break;
                case IERS_2003 :
                    factoryKey = simpleEOP ?
                                 Predefined.GTOD_CONVENTIONS_2003_SIMPLE_EOP :
                                 Predefined.GTOD_CONVENTIONS_2003_ACCURATE_EOP;
                    break;
                case IERS_2010 :
                    factoryKey = simpleEOP ? Predefined.GTOD_CONVENTIONS_2010_SIMPLE_EOP :
                                             Predefined.GTOD_CONVENTIONS_2010_ACCURATE_EOP;
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tod = getTOD(conventions, applyEOPCorr, simpleEOP);
                final ShiftingTransformProvider todInterpolating =
                        (ShiftingTransformProvider) tod.getTransformProvider();
                final TODProvider       todRaw     = (TODProvider) todInterpolating.getRawProvider();
                final EOPHistory        eopHistory = todRaw.getEOPHistory();
                final GTODProvider      gtodRaw    = new GTODProvider(conventions, eopHistory);
                final TransformProvider gtodShifting =
                        new ShiftingTransformProvider(gtodRaw,
                                                      CartesianDerivativesFilter.USE_PVA,
                                                      AngularDerivativesFilter.USE_R,
                                                      todInterpolating.getGridPoints(), todInterpolating.getStep(),
                                                      OrekitConfiguration.getCacheSlotsNumber(),
                                                      Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(tod, gtodShifting, false, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
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
    public static FactoryManagedFrame getTOD(final boolean applyEOPCorr) {
        return getTOD(IERSConventions.IERS_1996, applyEOPCorr, false);
    }

    /** Get the TOD reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    public static FactoryManagedFrame getTOD(final IERSConventions conventions,
                                             final boolean simpleEOP) {
        return getTOD(conventions, true, simpleEOP);
    }

    /** Get the TOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP correction parameters.
     * Beware that setting this parameter to {@code false} leads to crude accuracy
     * (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * For this reason, setting this parameter to false is restricted to {@link
     * IERSConventions#IERS_1996 IERS 1996} conventions, and hence this method is private.
     * </p>
     * @param conventions IERS conventions to apply
     * @param applyEOPCorr if true, EOP corrections are applied (here, nutation)
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    private static FactoryManagedFrame getTOD(final IERSConventions conventions,
                                              final boolean applyEOPCorr,
                                              final boolean simpleEOP) {

        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = applyEOPCorr ?
                                 (simpleEOP ? Predefined.TOD_CONVENTIONS_1996_SIMPLE_EOP : Predefined.TOD_CONVENTIONS_1996_ACCURATE_EOP) :
                                 Predefined.TOD_WITHOUT_EOP_CORRECTIONS;
                    break;
                case IERS_2003 :
                    factoryKey = simpleEOP ?
                                 Predefined.TOD_CONVENTIONS_2003_SIMPLE_EOP :
                                     Predefined.TOD_CONVENTIONS_2003_ACCURATE_EOP;
                    break;
                case IERS_2010 :
                    factoryKey = simpleEOP ?
                                 Predefined.TOD_CONVENTIONS_2010_SIMPLE_EOP :
                                 Predefined.TOD_CONVENTIONS_2010_ACCURATE_EOP;
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            final int interpolationPoints;
            final int pointsPerDay;
            if (applyEOPCorr) {
                interpolationPoints = 6;
                pointsPerDay        = 24;
            } else {
                interpolationPoints = 6;
                pointsPerDay        = 8;
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final EOPHistory eopHistory = applyEOPCorr ? getEOPHistory(conventions, simpleEOP) : null;
                final TransformProvider shifting =
                        new ShiftingTransformProvider(new TODProvider(conventions, eopHistory),
                                                      CartesianDerivativesFilter.USE_PVA,
                                                      AngularDerivativesFilter.USE_R,
                                                      interpolationPoints, Constants.JULIAN_DAY / pointsPerDay,
                                                      OrekitConfiguration.getCacheSlotsNumber(),
                                                      Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(getMOD(conventions, applyEOPCorr), shifting, true, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
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
    public static FactoryManagedFrame getMOD(final boolean applyEOPCorr) {
        return getMOD(IERSConventions.IERS_1996, applyEOPCorr);
    }

    /** Get the MOD reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     */
    public static FactoryManagedFrame getMOD(final IERSConventions conventions) {
        return getMOD(conventions, true);
    }

    /** Get the MOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP correction parameters.
     * Beware that setting this parameter to {@code false} leads to crude accuracy
     * (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * For this reason, setting this parameter to false is restricted to {@link
     * IERSConventions#IERS_1996 IERS 1996} conventions, and hence this method is private.
     * </p>
     * @param conventions IERS conventions to apply
     * @param applyEOPCorr if true, EOP corrections are applied (EME2000/GCRF bias compensation)
     * @return the selected reference frame singleton.
     */
    private static FactoryManagedFrame getMOD(final IERSConventions conventions, final boolean applyEOPCorr) {

        synchronized (FramesFactory.class) {

            final Predefined factoryKey;
            final Frame parent;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = applyEOPCorr ? Predefined.MOD_CONVENTIONS_1996 : Predefined.MOD_WITHOUT_EOP_CORRECTIONS;
                    parent     = applyEOPCorr ? FramesFactory.getGCRF() : FramesFactory.getEME2000();
                    break;
                case IERS_2003 :
                    factoryKey = Predefined.MOD_CONVENTIONS_2003;
                    // in IERS conventions 2003, the precession angles zetaA, thetaA and zA
                    // from equation 33 are computed from EME2000, not from GCRF
                    parent     = FramesFactory.getEME2000();
                    break;
                case IERS_2010 :
                    factoryKey = Predefined.MOD_CONVENTIONS_2010;
                    // precession angles epsilon0, psiA, omegaA and chiA
                    // from equations 5.39 and 5.40 are computed from EME2000
                    parent     = FramesFactory.getEME2000();
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }

            // try to find an already built frame
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(parent, new MODProvider(conventions), true, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
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
    public static FactoryManagedFrame getTEME() {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.TEME;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tod = getTOD(IERSConventions.IERS_1996, false, true);
                final ShiftingTransformProvider todShifting =
                        (ShiftingTransformProvider) tod.getTransformProvider();
                final TEMEProvider temeRaw = new TEMEProvider(IERSConventions.IERS_1996, null);
                final TransformProvider temeShifting =
                        new ShiftingTransformProvider(temeRaw,
                                                      CartesianDerivativesFilter.USE_PVA,
                                                      AngularDerivativesFilter.USE_R,
                                                      todShifting.getGridPoints(), todShifting.getStep(),
                                                      OrekitConfiguration.getCacheSlotsNumber(),
                                                      Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

                frame = new FactoryManagedFrame(tod, temeShifting, true, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
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
    public static FactoryManagedFrame getPZ9011(final IERSConventions convention,
                                                final boolean simpleEOP) {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.PZ90_11;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame itrf = getITRF(ITRFVersion.ITRF_2008, convention, simpleEOP);
                final HelmertTransformation pz90Raw = new HelmertTransformation(new AbsoluteDate(2010, 1, 1, 12, 0, 0, TimeScalesFactory.getTT()),
                                                                                +3.0, +1.0, -0.0, +0.019, -0.042, +0.002, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
                frame = new FactoryManagedFrame(itrf, pz90Raw, false, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
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
    public static <T extends RealFieldElement<T>> FieldTransform<T> getNonInterpolatingTransform(final Frame from, final Frame to,
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
     * goind though parent frames, and the providers are checked to see if they
     * reference EOP history.the first EOP history found is returned.
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
                       ((EOPBasedTransformProvider) peeled).getEOPHistory().usesInterpolation()) {
                peeled = ((EOPBasedTransformProvider) peeled).getNonInterpolatingProvider();
            } else {
                peeling = false;
            }
        }

        return peeled;

    }

    /** Local class for different ITRF versions keys.
     * @since 9.2
     */
    private static class ITRFKey implements Serializable {

        /** Serialized UID. */
        private static final long serialVersionUID = 20180412L;

        /** ITRF version. */
        private final ITRFVersion version;

        /** IERS conventions to apply. */
        private final IERSConventions conventions;

        /** Tidal effects flag. */
        private final boolean simpleEOP;

        /** Simple constructor.
         * @param version ITRF version
         * @param conventions IERS conventions to apply
         * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
         */
        ITRFKey(final ITRFVersion version, final IERSConventions conventions, final boolean simpleEOP) {
            this.version     = version;
            this.conventions = conventions;
            this.simpleEOP   = simpleEOP;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return (version.ordinal()     << 5) +
                   (conventions.ordinal() << 1) +
                   (simpleEOP ? 0 : 1);
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object other) {

            if (this == other) {
                return true;
            }

            if (other instanceof ITRFKey) {
                final ITRFKey key = (ITRFKey) other;
                return version     == key.version     &&
                       conventions == key.conventions &&
                       simpleEOP   == key.simpleEOP;
            }

            return false;
        }

    }

}
