/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;


/** Factory for predefined reference frames.
 *
 * <h5> FramesFactory Presentation </h5>
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
 * <h5> Reference Frames </h5>
 * <p>
 * The user can retrieve those reference frames using various static methods
 * ({@link FramesFactory#getFrame(Predefined)},
 * {@link #getGCRF()}, {@link #getCIRF(IERSConventions)}
 * {@link #getTIRF(IERSConventions, boolean)}, {@link #getTIRF(IERSConventions)},
 * {@link #getITRF93(boolean)}, {@link #getITRF93()},
 * {@link #getITRF97(boolean)}, {@link #getITRF97()},
 * {@link #getITRF2000(boolean)}, {@link #getITRF2000()},
 * {@link #getITRF2005(boolean)}, {@link #getITRF2005()},
 * {@link #getITRF2008(boolean)}, {@link #getITRF2008()},
 * {@link #getEME2000()}, {@link #getMOD(IERSConventions)}, {@link #getTOD(IERSConventions)},
 * {@link #getGTOD(IERSConventions)}, {@link #getITRFEquinox(IERSConventions)}, {@link #getTEME()}
 * and {@link #getVeis1950()}).
 * </p>
 * <h5> International Terrestrial Reference Frame 2008 </h5>
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
 * provided by {@link org.orekit.data.DataProvidersManager IERS data}.
 * Its pole axis is the IERS Reference Pole (IRP).
 * </p>
 * <p>
 * Previous realizations of the ITRS are available and linked together using
 * {@link HelmertTransformation Helmert transformations}. Parameters for all
 * ITRS realizations since 1988 are available from the ITRF site <a
 * href="ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP"> ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP</a>).
 * Orekit provides only a few of them: ITRF93, ITRF97, ITRF2000, ITRF2005 and
 * ITRF2008. The three first ones are mandatory to support CCSDS Orbit Data Messages,
 * and the two last ones correspond to the more up to date ones used by recent IERS
 * conventions. Other realizations can be added by users if they build themselves the
 * {@link HelmertTransformation Helmert transformations}.
 * </p>
 * <p>
 * OREKIT proposes all the intermediate frames used to build this specific frame.
 * This implementation follows the new non-rotating origin paradigm
 * mandated by IAU 2000 resolution B1.8. It is therefore based on
 * Celestial Ephemeris Origin (CEO-based) and Earth Rotating Angle. I is based on the
 * 2010 conventions.
 * </p>
 * <p>
 * Orekit also provides frames corresponding to IERS 2003 conventions (from GCRF to
 * TIRF 2000, but <em>not</em> ITRF (which are linked to 2010 conventions now). The
 * reason for this is that IERS does not provide Earth Orientation Data anymore for
 * these older conventions, they provide data only for the new conventions.
 * </p>
 * <h5> Classical paradigm: equinox-based transformations </h5>
 * <p>
 * The classical paradigm used prior to IERS conventions 2010 (and 2003) is equinox based and
 * uses more intermediate frames. Only some of these frames are supported in Orekit.
 * </p>
 * <h5> Earth Orientation Parameters </h5>
 * <p>
 * The Earth Orientation Parameters (EOP) needed for accurate transformations
 * between inertial and Earth fixed frames are loaded from the
 * {@link org.orekit.data.DataProvidersManager}. When EOP should be applied,
 * but EOP data are not available, then a null (0.0) correction is used. This
 * can occur when no EOP data is loaded, or when the requested date is beyond
 * the time span of the loaded EOP data. Using a null correction can result in
 * coarse accuracy. To check the time span covered by EOP data use
 * {@link #getEOPHistory(IERSConventions)}, {@link EOPHistory#getStartDate()},
 * and {@link EOPHistory#getEndDate()}.
 * <p>
 * For more on configuring the EOP data Orekit uses see
 * <a href="https://www.orekit.org/forge/projects/orekit/wiki/Configuration">
 * https://www.orekit.org/forge/projects/orekit/wiki/Configuration</a>.
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
    public static final String EOPC04_1980_FILENAME = "^eopc04_08\\.(\\d\\d)$";

    /** Default regular expression for the BulletinB files (IAU1980 compatibles). */
    public static final String BULLETINB_1980_FILENAME = "^bulletinb((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** Default regular expression for the Rapid Data and Prediction EOP columns files (IAU2000 compatibles). */
    public static final String RAPID_DATA_PREDICITON_COLUMNS_2000_FILENAME = "^finals2000A\\.[^.]*$";

    /** Default regular expression for the Rapid Data and Prediction EOP XML files (IAU2000 compatibles). */
    public static final String RAPID_DATA_PREDICITON_XML_2000_FILENAME = "^finals2000A\\..*\\.xml$";

    /** Default regular expression for the EOPC04 files (IAU2000 compatibles). */
    public static final String EOPC04_2000_FILENAME = "^eopc04_08_IAU2000\\.(\\d\\d)$";

    /** Default regular expression for the BulletinB files (IAU2000 compatibles). */
    public static final String BULLETINB_2000_FILENAME = "^bulletinb_IAU2000((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** Predefined frames. */
    private static transient Map<Predefined, FactoryManagedFrame> FRAMES =
        new HashMap<Predefined, FactoryManagedFrame>();

    /** Loaders for Earth Orientation parameters. */
    private static final Map<IERSConventions, List<EOPHistoryLoader>> EOP_HISTORY_LOADERS =
        new HashMap<IERSConventions, List<EOPHistoryLoader>>();

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private FramesFactory() {
    }

    /** Add the default loaders EOP history (IAU 1980 precession/nutation).
     * <p>
     * The default loaders look for IERS EOP 08 C04 and bulletins B files. They
     * correspond to {@link IERSConventions#IERS_1996 IERS 1996} conventions.
     * </p>
     * @param rapidDataColumnsSupportedNames regular expression for supported
     * rapid data columns EOP files names
     * (may be null if the default IERS file names are used)
     * @param rapidDataXMLSupportedNames regular expression for supported
     * rapid data XML EOP files names
     * (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP 08 C04 files names
     * (may be null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names
     * (may be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">IERS EOP 08 C04 files</a>
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP2000HistoryLoaders(String, String, String, String)
     */
    public static void addDefaultEOP1980HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames) {
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
                            new EOP08C04FilesLoader(eopcNames));
        final String bulBNames =
            (bulletinBSupportedNames == null) ? BULLETINB_1980_FILENAME : bulletinBSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                            new BulletinBFilesLoader(bulBNames));
    }

    /** Add the default loaders for EOP history (IAU 2000/2006 precession/nutation).
     * <p>
     * The default loaders look for IERS EOP 08 C04 and bulletins B files. They
     * correspond to both {@link IERSConventions#IERS_2003 IERS 2003} and {@link
     * IERSConventions#IERS_2010 IERS 2010} conventions.
     * </p>
     * @param rapidDataColumnsSupportedNames regular expression for supported
     * rapid data columns EOP files names
     * (may be null if the default IERS file names are used)
     * @param rapidDataXMLSupportedNames regular expression for supported
     * rapid data XML EOP files names
     * (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP 08 C04 files names
     * (may be null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names
     * (may be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">IERS EOP 08 C04 files</a>
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String)
     */
    public static void addDefaultEOP2000HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames) {
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
                            new EOP08C04FilesLoader(eopcNames));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                            new EOP08C04FilesLoader(eopcNames));
        final String bulBNames =
            (bulletinBSupportedNames == null) ? BULLETINB_2000_FILENAME : bulletinBSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                            new BulletinBFilesLoader(bulBNames));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                            new BulletinBFilesLoader(bulBNames));
    }

    /** Add a loader for Earth Orientation Parameters history.
     * @param conventions IERS conventions to which EOP history applies
     * @param loader custom loader to add for the EOP history
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String)
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
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String)
     */
    public static void clearEOPHistoryLoaders() {
        synchronized (EOP_HISTORY_LOADERS) {
            EOP_HISTORY_LOADERS.clear();
        }
    }

    /** Get Earth Orientation Parameters history.
     * <p>
     * If no {@link EOPHistoryLoader} has been added by calling {@link
     * #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader) addEOPHistoryLoader}
     * or if {@link #clearEOPHistoryLoaders() clearEOPHistoryLoaders} has been
     * called afterwards, the {@link #addDefaultEOP1980HistoryLoaders(String, String,
     * String, String)} and {@link #addDefaultEOP2000HistoryLoaders(String, String,
     * String, String)} methods will be called automatically with supported file names
     * parameters all set to null, in order to get the default loaders configuration.
     * </p>
     * @param conventions conventions for which EOP history is requested
     * @return Earth Orientation Parameters history
     * @exception OrekitException if the data cannot be loaded
     */
    public static EOPHistory getEOPHistory(final IERSConventions conventions)
        throws OrekitException {

        synchronized (EOP_HISTORY_LOADERS) {

            //TimeStamped based set needed to remove duplicates
            if (EOP_HISTORY_LOADERS.isEmpty()) {
                addDefaultEOP2000HistoryLoaders(null, null, null, null);
                addDefaultEOP1980HistoryLoaders(null, null, null, null);
            }

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

            final EOPHistory history = new EOPHistory(data);
            history.checkEOPContinuity(5 * Constants.JULIAN_DAY);
            return history;

        }

    }

    /** Get one of the predefined frames.
     * @param factoryKey key of the frame within the factory
     * @return the predefined frame
     * @exception OrekitException if frame cannot be built due to missing data
     */
    @SuppressWarnings("deprecation")
    public static Frame getFrame(final Predefined factoryKey)
        throws OrekitException {
        switch (factoryKey) {
        case GCRF :
            return getGCRF();
        case ICRF :
            return getICRF();
        case EME2000 :
            return getEME2000();
        case ITRF_2008_WITHOUT_TIDAL_EFFECTS :
            return getITRF2008(true);
        case ITRF_2008_WITH_TIDAL_EFFECTS :
            return getITRF2008(false);
        case ITRF_2005_WITHOUT_TIDAL_EFFECTS :
            return getITRF2005(true);
        case ITRF_2005_WITH_TIDAL_EFFECTS :
            return getITRF2005(false);
        case ITRF_2000_WITHOUT_TIDAL_EFFECTS :
            return getITRF2000(true);
        case ITRF_2000_WITH_TIDAL_EFFECTS :
            return getITRF2000(false);
        case ITRF_97_WITHOUT_TIDAL_EFFECTS :
            return getITRF97(true);
        case ITRF_97_WITH_TIDAL_EFFECTS :
            return getITRF97(false);
        case ITRF_93_WITHOUT_TIDAL_EFFECTS :
            return getITRF93(true);
        case ITRF_93_WITH_TIDAL_EFFECTS :
            return getITRF93(false);
        case ITRF_EQUINOX_CONV_2010 :
            return getITRFEquinox(IERSConventions.IERS_2010);
        case ITRF_EQUINOX_CONV_2003 :
            return getITRFEquinox(IERSConventions.IERS_2003);
        case ITRF_EQUINOX :
        case ITRF_EQUINOX_CONVENTIONS_1996 :
            return getITRFEquinox(IERSConventions.IERS_1996);
        case TIRF_2000_CONV_2010_WITHOUT_TIDAL_EFFECTS :
        case TIRF_CONVENTIONS_2010_WITHOUT_TIDAL_EFFECTS :
            return getTIRF(IERSConventions.IERS_2010, true);
        case TIRF_2000_CONV_2010_WITH_TIDAL_EFFECTS :
        case TIRF_CONVENTIONS_2010_WITH_TIDAL_EFFECTS :
            return getTIRF(IERSConventions.IERS_2010, false);
        case TIRF_2000_CONV_2003_WITHOUT_TIDAL_EFFECTS :
        case TIRF_CONVENTIONS_2003_WITHOUT_TIDAL_EFFECTS :
            return getTIRF(IERSConventions.IERS_2003, true);
        case TIRF_2000_CONV_2003_WITH_TIDAL_EFFECTS :
        case TIRF_CONVENTIONS_2003_WITH_TIDAL_EFFECTS :
            return getTIRF(IERSConventions.IERS_2003, false);
        case TIRF_CONVENTIONS_1996_WITHOUT_TIDAL_EFFECTS :
            return getTIRF(IERSConventions.IERS_1996, true);
        case TIRF_CONVENTIONS_1996_WITH_TIDAL_EFFECTS :
            return getTIRF(IERSConventions.IERS_1996, false);
        case CIRF_2000_CONV_2010 :
        case CIRF_CONVENTIONS_2010 :
            return getCIRF(IERSConventions.IERS_2010);
        case CIRF_2000_CONV_2003 :
        case CIRF_CONVENTIONS_2003 :
            return getCIRF(IERSConventions.IERS_2003);
        case CIRF_CONVENTIONS_1996 :
            return getCIRF(IERSConventions.IERS_1996);
        case VEIS_1950 :
            return getVeis1950();
        case GTOD_WITHOUT_EOP_CORRECTIONS :
            return getGTOD(IERSConventions.IERS_1996, false);
        case GTOD_CONVENTIONS_2010 :
            return getGTOD(IERSConventions.IERS_2010, true);
        case GTOD_CONVENTIONS_2003 :
            return getGTOD(IERSConventions.IERS_2003, true);
        case GTOD_WITH_EOP_CORRECTIONS :
        case GTOD_CONVENTIONS_1996 :
            return getGTOD(IERSConventions.IERS_1996, true);
        case TOD_WITHOUT_EOP_CORRECTIONS :
            return getTOD(IERSConventions.IERS_1996, false);
        case TOD_CONVENTIONS_2010 :
            return getTOD(IERSConventions.IERS_2010, true);
        case TOD_CONVENTIONS_2003 :
            return getTOD(IERSConventions.IERS_2003, true);
        case TOD_WITH_EOP_CORRECTIONS :
        case TOD_CONVENTIONS_1996 :
            return getTOD(IERSConventions.IERS_1996, true);
        case MOD_WITHOUT_EOP_CORRECTIONS :
            return getMOD(IERSConventions.IERS_1996, false);
        case MOD_CONVENTIONS_2010 :
            return getMOD(IERSConventions.IERS_2010, true);
        case MOD_CONVENTIONS_2003 :
            return getMOD(IERSConventions.IERS_2003, true);
        case MOD_WITH_EOP_CORRECTIONS :
        case MOD_CONVENTIONS_1996 :
            return getMOD(IERSConventions.IERS_1996, true);
        case TEME :
            return getTEME();
        default :
            throw OrekitException.createInternalError(null);
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
     * with EME2000.</p>
     * @return the unique instance of the ICRF frame
     * @exception OrekitException if solar system ephemerides cannot be loaded
     */
    public static Frame getICRF() throws OrekitException {
        return CelestialBodyFactory.getSolarSystemBarycenter().getInertiallyOrientedFrame();
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

    /** Get the ITRF2008 reference frame, using IERS 2010 conventions and ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2008() throws OrekitException {
        return getITRF2008(true);
    }

    /** Get the ITRF2008 reference frame, using IERS 2010 conventions.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2008(final boolean ignoreTidalEffects) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = ignoreTidalEffects ?
                                          Predefined.ITRF_2008_WITHOUT_TIDAL_EFFECTS :
                                          Predefined.ITRF_2008_WITH_TIDAL_EFFECTS;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tirfFrame = getTIRF(IERSConventions.IERS_2010, ignoreTidalEffects);
                final TIRFProvider tirfProvider = (TIRFProvider) tirfFrame.getTransformProvider();
                frame = new FactoryManagedFrame(tirfFrame,
                                                new ITRFProvider(tirfProvider.getEOPHistory(),
                                                                 tirfProvider.getTidalCorrection()),
                                                false, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the ITRF2005 reference frame, using IERS 2010 conventions and ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2005() throws OrekitException {
        return getITRF2005(true);
    }

    /** Get the ITRF2005 reference frame, using IERS 2010 conventions.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2005(final boolean ignoreTidalEffects) throws OrekitException {

        final Predefined factoryKey = ignoreTidalEffects ?
                                      Predefined.ITRF_2005_WITHOUT_TIDAL_EFFECTS :
                                      Predefined.ITRF_2005_WITH_TIDAL_EFFECTS;

        // Helmert transformation between ITRF2005 and ITRF 2008
        // see http://itrf.ensg.ign.fr/doc_ITRF/Transfo-ITRF2008_ITRFs.txt
        // SOLUTION         Tx       Ty       Tz        D        Rx        Ry        Rz      EPOCH
        // UNITS----------> mm       mm       mm       ppb       .001"     .001"     .001"
        //                         .        .        .         .        .         .         .
        //        RATES     Tx       Ty       Tz        D        Rx        Ry        Rz
        // UNITS----------> mm/y     mm/y     mm/y     ppb/y    .001"/y   .001"/y   .001"/y
        // -----------------------------------------------------------------------------------------
        //   ITRF2005       -2.0     -0.9     -4.7      0.94      0.00      0.00      0.00    2000.0
        //        rates      0.3      0.0      0.0      0.00      0.00      0.00      0.00
        //   ITRF2000       -1.9     -1.7    -10.5      1.34      0.00      0.00      0.00    2000.0
        //        rates      0.1      0.1     -1.8      0.08      0.00      0.00      0.00
        //   ITRF97          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF96          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF94          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF93        -24.0      2.4    -38.6      3.41     -1.71     -1.48     -0.30    2000.0
        //        rates     -2.8     -0.1     -2.4      0.09     -0.11     -0.19      0.07
        //   ITRF92         12.8      4.6    -41.2      2.21      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF91         24.8     18.6    -47.2      3.61      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF90         22.8     14.6    -63.2      3.91      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF89         27.8     38.6   -101.2      7.31      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF88         22.8      2.6   -125.2     10.41      0.10      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        // _________________________________________________________________________________________
        return getITRSRealization(factoryKey, getITRF2008(ignoreTidalEffects), 2000,
                                  -2.0, -0.9, -4.7, 0.000, 0.000, 0.000,
                                   0.3,  0.0,  0.0, 0.000, 0.000, 0.000);

    }

    /** Get the ITRF2000 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2000() throws OrekitException {
        return getITRF2000(true);
    }

    /** Get the ITRF2000 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2000(final boolean ignoreTidalEffects) throws OrekitException {

        final Predefined factoryKey = ignoreTidalEffects ?
                                      Predefined.ITRF_2000_WITHOUT_TIDAL_EFFECTS :
                                      Predefined.ITRF_2000_WITH_TIDAL_EFFECTS;

        // Helmert transformation between ITRF2000 and ITRF 2008
        // see http://itrf.ensg.ign.fr/doc_ITRF/Transfo-ITRF2008_ITRFs.txt
        // SOLUTION         Tx       Ty       Tz        D        Rx        Ry        Rz      EPOCH
        // UNITS----------> mm       mm       mm       ppb       .001"     .001"     .001"
        //                         .        .        .         .        .         .         .
        //        RATES     Tx       Ty       Tz        D        Rx        Ry        Rz
        // UNITS----------> mm/y     mm/y     mm/y     ppb/y    .001"/y   .001"/y   .001"/y
        // -----------------------------------------------------------------------------------------
        //   ITRF2005       -2.0     -0.9     -4.7      0.94      0.00      0.00      0.00    2000.0
        //        rates      0.3      0.0      0.0      0.00      0.00      0.00      0.00
        //   ITRF2000       -1.9     -1.7    -10.5      1.34      0.00      0.00      0.00    2000.0
        //        rates      0.1      0.1     -1.8      0.08      0.00      0.00      0.00
        //   ITRF97          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF96          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF94          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF93        -24.0      2.4    -38.6      3.41     -1.71     -1.48     -0.30    2000.0
        //        rates     -2.8     -0.1     -2.4      0.09     -0.11     -0.19      0.07
        //   ITRF92         12.8      4.6    -41.2      2.21      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF91         24.8     18.6    -47.2      3.61      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF90         22.8     14.6    -63.2      3.91      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF89         27.8     38.6   -101.2      7.31      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF88         22.8      2.6   -125.2     10.41      0.10      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        // _________________________________________________________________________________________
        return getITRSRealization(factoryKey, getITRF2008(ignoreTidalEffects), 2000,
                                 -1.9, -1.7, -10.5, 0.000, 0.000, 0.000,
                                  0.1,  0.1,  -1.8, 0.000, 0.000, 0.000);

    }

    /** Get the ITRF97 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF97() throws OrekitException {
        return getITRF97(true);
    }

    /** Get the ITRF97 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF97(final boolean ignoreTidalEffects) throws OrekitException {

        final Predefined factoryKey = ignoreTidalEffects ?
                                      Predefined.ITRF_97_WITHOUT_TIDAL_EFFECTS :
                                      Predefined.ITRF_97_WITH_TIDAL_EFFECTS;

        // Helmert transformation between ITRF97 and ITRF 2008
        // see http://itrf.ensg.ign.fr/doc_ITRF/Transfo-ITRF2008_ITRFs.txt
        // SOLUTION         Tx       Ty       Tz        D        Rx        Ry        Rz      EPOCH
        // UNITS----------> mm       mm       mm       ppb       .001"     .001"     .001"
        //                         .        .        .         .        .         .         .
        //        RATES     Tx       Ty       Tz        D        Rx        Ry        Rz
        // UNITS----------> mm/y     mm/y     mm/y     ppb/y    .001"/y   .001"/y   .001"/y
        // -----------------------------------------------------------------------------------------
        //   ITRF2005       -2.0     -0.9     -4.7      0.94      0.00      0.00      0.00    2000.0
        //        rates      0.3      0.0      0.0      0.00      0.00      0.00      0.00
        //   ITRF2000       -1.9     -1.7    -10.5      1.34      0.00      0.00      0.00    2000.0
        //        rates      0.1      0.1     -1.8      0.08      0.00      0.00      0.00
        //   ITRF97          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF96          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF94          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF93        -24.0      2.4    -38.6      3.41     -1.71     -1.48     -0.30    2000.0
        //        rates     -2.8     -0.1     -2.4      0.09     -0.11     -0.19      0.07
        //   ITRF92         12.8      4.6    -41.2      2.21      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF91         24.8     18.6    -47.2      3.61      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF90         22.8     14.6    -63.2      3.91      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF89         27.8     38.6   -101.2      7.31      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF88         22.8      2.6   -125.2     10.41      0.10      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        // _________________________________________________________________________________________
        return getITRSRealization(factoryKey, getITRF2008(ignoreTidalEffects), 2000,
                                  4.8,  2.6, -33.2, 0.00, 0.00, 0.00,
                                  0.1, -0.5,  -3.2, 0.00, 0.00, 0.002);

    }

    /** Get the ITRF93 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF93() throws OrekitException {
        return getITRF93(true);
    }

    /** Get the ITRF93 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF93(final boolean ignoreTidalEffects) throws OrekitException {

        final Predefined factoryKey = ignoreTidalEffects ?
                                      Predefined.ITRF_93_WITHOUT_TIDAL_EFFECTS :
                                      Predefined.ITRF_93_WITH_TIDAL_EFFECTS;

        // Helmert transformation between ITRF93 and ITRF 2008
        // see http://itrf.ensg.ign.fr/doc_ITRF/Transfo-ITRF2008_ITRFs.txt
        // SOLUTION         Tx       Ty       Tz        D        Rx        Ry        Rz      EPOCH
        // UNITS----------> mm       mm       mm       ppb       .001"     .001"     .001"
        //                         .        .        .         .        .         .         .
        //        RATES     Tx       Ty       Tz        D        Rx        Ry        Rz
        // UNITS----------> mm/y     mm/y     mm/y     ppb/y    .001"/y   .001"/y   .001"/y
        // -----------------------------------------------------------------------------------------
        //   ITRF2005       -2.0     -0.9     -4.7      0.94      0.00      0.00      0.00    2000.0
        //        rates      0.3      0.0      0.0      0.00      0.00      0.00      0.00
        //   ITRF2000       -1.9     -1.7    -10.5      1.34      0.00      0.00      0.00    2000.0
        //        rates      0.1      0.1     -1.8      0.08      0.00      0.00      0.00
        //   ITRF97          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF96          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF94          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF93        -24.0      2.4    -38.6      3.41     -1.71     -1.48     -0.30    2000.0
        //        rates     -2.8     -0.1     -2.4      0.09     -0.11     -0.19      0.07
        //   ITRF92         12.8      4.6    -41.2      2.21      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF91         24.8     18.6    -47.2      3.61      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF90         22.8     14.6    -63.2      3.91      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF89         27.8     38.6   -101.2      7.31      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF88         22.8      2.6   -125.2     10.41      0.10      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        // _________________________________________________________________________________________
        return getITRSRealization(factoryKey, getITRF2008(ignoreTidalEffects), 2000,
                                 -24.0,  2.4, -38.6, -1.71, -1.48, -0.30,
                                  -2.8, -0.1,  -2.4, -0.11, -0.19,  0.07);

    }

    /** Get an ITRS realization reference frame.
     * @param factoryKey key of the frame within the factory
     * @param parent parent frame to which the Helmert transformation should be applied
     * to define the desired realization
     * @param refYear reference year for the epoch of the transform
     * @param t1 translation parameter along X axis (BEWARE, this is in mm)
     * @param t2 translation parameter along Y axis (BEWARE, this is in mm)
     * @param t3 translation parameter along Z axis (BEWARE, this is in mm)
     * @param r1 rotation parameter around X axis (BEWARE, this is in mas)
     * @param r2 rotation parameter around Y axis (BEWARE, this is in mas)
     * @param r3 rotation parameter around Z axis (BEWARE, this is in mas)
     * @param t1Dot rate of translation parameter along X axis (BEWARE, this is in mm/y)
     * @param t2Dot rate of translation parameter along Y axis (BEWARE, this is in mm/y)
     * @param t3Dot rate of translation parameter along Z axis (BEWARE, this is in mm/y)
     * @param r1Dot rate of rotation parameter around X axis (BEWARE, this is in mas/y)
     * @param r2Dot rate of rotation parameter around Y axis (BEWARE, this is in mas/y)
     * @param r3Dot rate of rotation parameter around Z axis (BEWARE, this is in mas/y)
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    private static FactoryManagedFrame getITRSRealization(final Predefined factoryKey, final Frame parent, final int refYear,
                                                          final double t1, final double t2, final double t3,
                                                          final double r1, final double r2, final double r3,
                                                          final double t1Dot, final double t2Dot, final double t3Dot,
                                                          final double r1Dot, final double r2Dot, final double r3Dot)
        throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it

                final HelmertTransformation helmertTransformation =
                    new HelmertTransformation(new AbsoluteDate(refYear, 1, 1, 12, 0, 0, TimeScalesFactory.getTT()),
                                              t1, t2, t3, r1, r2, r3, t1Dot, t2Dot, t3Dot, r1Dot, r2Dot, r3Dot);
                frame = new FactoryManagedFrame(parent, helmertTransformation, false, factoryKey);

                FRAMES.put(factoryKey, frame);

            }

            return frame;

        }
    }

    /** Get the TIRF reference frame, ignoring tidal effects.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     * @deprecated since 6.1 replaced with {@link #getTIRF(IERSConventions)}
     */
    @Deprecated
    public static FactoryManagedFrame getTIRF2000(final IERSConventions conventions) throws OrekitException {
        return getTIRF(conventions);
    }

    /** Get the TIRF reference frame, ignoring tidal effects.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getTIRF(final IERSConventions conventions) throws OrekitException {
        return getTIRF(conventions, true);
    }

    /** Get the TIRF reference frame.
     * @param conventions IERS conventions to apply
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     * @deprecated since 6.1 replaced with {@link #getTIRF(IERSConventions, boolean)}
     */
    @Deprecated
    public static FactoryManagedFrame getTIRF2000(final IERSConventions conventions,
                                                  final boolean ignoreTidalEffects) throws OrekitException {
        return getTIRF(conventions, ignoreTidalEffects);
    }

    /** Get the TIRF reference frame.
     * @param conventions IERS conventions to apply
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     * @since 6.1
     */
    public static FactoryManagedFrame getTIRF(final IERSConventions conventions,
                                              final boolean ignoreTidalEffects) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
            case IERS_1996 :
                factoryKey = ignoreTidalEffects ?
                             Predefined.TIRF_CONVENTIONS_1996_WITHOUT_TIDAL_EFFECTS :
                             Predefined.TIRF_CONVENTIONS_1996_WITH_TIDAL_EFFECTS;
                break;
            case IERS_2003 :
                factoryKey = ignoreTidalEffects ?
                             Predefined.TIRF_CONVENTIONS_2003_WITHOUT_TIDAL_EFFECTS :
                             Predefined.TIRF_CONVENTIONS_2003_WITH_TIDAL_EFFECTS;
                break;
            case IERS_2010 :
                factoryKey = ignoreTidalEffects ?
                             Predefined.TIRF_CONVENTIONS_2010_WITHOUT_TIDAL_EFFECTS :
                             Predefined.TIRF_CONVENTIONS_2010_WITH_TIDAL_EFFECTS;
                break;
            default :
                // this should never happen
                throw OrekitException.createInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame cirf = getCIRF(conventions);
                final InterpolatingTransformProvider cirfInterpolating =
                        (InterpolatingTransformProvider) cirf.getTransformProvider();
                final CIRFProvider cirfRaw = (CIRFProvider) cirfInterpolating.getRawProvider();
                final EOPHistory eopHistory = cirfRaw.getEOPHistory();
                final TidalCorrection tidalCorrection = ignoreTidalEffects ? null : new TidalCorrection();
                frame = new FactoryManagedFrame(cirf,
                                                new TIRFProvider(conventions, eopHistory, tidalCorrection),
                                                false, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the CIRF2000 reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     * @deprecated since 6.1 repaced with {@link #getCIRF(IERSConventions)}
     */
    @Deprecated
    public static FactoryManagedFrame getCIRF2000(final IERSConventions conventions) throws OrekitException {
        return getCIRF(conventions);
    }

    /** Get the CIRF2000 reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getCIRF(final IERSConventions conventions) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
            case IERS_1996 :
                factoryKey = Predefined.CIRF_CONVENTIONS_1996;
                break;
            case IERS_2003 :
                factoryKey = Predefined.CIRF_CONVENTIONS_2003;
                break;
            case IERS_2010 :
                factoryKey = Predefined.CIRF_CONVENTIONS_2010;
                break;
            default :
                // this should never happen
                throw OrekitException.createInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final EOPHistory eopHistory = FramesFactory.getEOPHistory(conventions);
                final TransformProvider interpolating =
                        new InterpolatingTransformProvider(new CIRFProvider(conventions, eopHistory), true, false,
                                                           AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                           6, Constants.JULIAN_DAY / 24,
                                                           OrekitConfiguration.getCacheSlotsNumber(),
                                                           Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(getGCRF(), interpolating, true, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the VEIS 1950 reference frame.
     * <p>Its parent frame is the GTOD frame with IERS 1996 conventions without EOP corrections.<p>
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getVeis1950() throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.VEIS_1950;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(FramesFactory.getGTOD(IERSConventions.IERS_1996, false),
                                                new VEISProvider(), true, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the equinox-based ITRF reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     * @deprecated since 6.1 replaced with {@link #getITRFEquinox(IERSConventions)}
     */
    @Deprecated
    public static FactoryManagedFrame getITRFEquinox() throws OrekitException {
        return getITRFEquinox(IERSConventions.IERS_1996);
    }

    /** Get the equinox-based ITRF reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getITRFEquinox(final IERSConventions conventions) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
            case IERS_1996 :
                factoryKey = Predefined.ITRF_EQUINOX_CONVENTIONS_1996;
                break;
            case IERS_2003 :
                factoryKey = Predefined.ITRF_EQUINOX_CONV_2003;
                break;
            case IERS_2010 :
                factoryKey = Predefined.ITRF_EQUINOX_CONV_2010;
                break;
            default :
                // this should never happen
                throw OrekitException.createInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame gtod = getGTOD(conventions, true);
                final InterpolatingTransformProvider gtodInterpolating =
                        (InterpolatingTransformProvider) gtod.getTransformProvider();
                final GTODProvider gtodRaw    = (GTODProvider) gtodInterpolating.getRawProvider();
                final EOPHistory   eopHistory = gtodRaw.getEOPHistory();
                frame = new FactoryManagedFrame(getGTOD(conventions, true),
                                                new ITRFEquinoxProvider(eopHistory), false, factoryKey);
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
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getGTOD(final boolean applyEOPCorr) throws OrekitException {
        return getGTOD(IERSConventions.IERS_1996, applyEOPCorr);
    }

    /** Get the GTOD reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getGTOD(final IERSConventions conventions)
        throws OrekitException {
        return getGTOD(conventions, true);
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
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    private static FactoryManagedFrame getGTOD(final IERSConventions conventions, final boolean applyEOPCorr)
        throws OrekitException {

        if (conventions != IERSConventions.IERS_1996 && !applyEOPCorr) {
            // this should never happen as this method is private and called
            // only above with controlled input
            throw OrekitException.createInternalError(null);
        }

        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
            case IERS_1996 :
                factoryKey = applyEOPCorr ? Predefined.GTOD_CONVENTIONS_1996 : Predefined.GTOD_WITHOUT_EOP_CORRECTIONS;
                break;
            case IERS_2003 :
                factoryKey = Predefined.GTOD_CONVENTIONS_2003;
                break;
            case IERS_2010 :
                factoryKey = Predefined.GTOD_CONVENTIONS_2010;
                break;
            default :
                // this should never happen
                throw OrekitException.createInternalError(null);
            }
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tod = getTOD(conventions, applyEOPCorr);
                final InterpolatingTransformProvider todInterpolating =
                        (InterpolatingTransformProvider) tod.getTransformProvider();
                final TODProvider       todRaw     = (TODProvider) todInterpolating.getRawProvider();
                final EOPHistory        eopHistory = todRaw.getEOPHistory();
                final GTODProvider      gtodRaw    = new GTODProvider(conventions, eopHistory);
                final TransformProvider gtodInterpolating =
                        new InterpolatingTransformProvider(gtodRaw, true, false,
                                                           AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                           todInterpolating.getGridPoints(), todInterpolating.getStep(),
                                                           OrekitConfiguration.getCacheSlotsNumber(),
                                                           Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(tod, gtodInterpolating, false, factoryKey);
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
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (here, lod)
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     * @deprecated as of 6.0, replaced by {@link #getGTOD(IERSConventions, boolean)}
     */
    @Deprecated
    public static FactoryManagedFrame getPEF(final boolean applyEOPCorr) throws OrekitException {
        return getGTOD(applyEOPCorr);
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
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getTOD(final boolean applyEOPCorr)
        throws OrekitException {
        return getTOD(IERSConventions.IERS_1996, applyEOPCorr);
    }

    /** Get the TOD reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getTOD(final IERSConventions conventions)
        throws OrekitException {
        return getTOD(conventions, true);
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
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    private static FactoryManagedFrame getTOD(final IERSConventions conventions, final boolean applyEOPCorr)
        throws OrekitException {

        if (conventions != IERSConventions.IERS_1996 && !applyEOPCorr) {
            // this should never happen as this method is private and called
            // only above with controlled input
            throw OrekitException.createInternalError(null);
        }

        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey;
            switch (conventions) {
            case IERS_1996 :
                factoryKey = applyEOPCorr ? Predefined.TOD_CONVENTIONS_1996 : Predefined.TOD_WITHOUT_EOP_CORRECTIONS;
                break;
            case IERS_2003 :
                factoryKey = Predefined.TOD_CONVENTIONS_2003;
                break;
            case IERS_2010 :
                factoryKey = Predefined.TOD_CONVENTIONS_2010;
                break;
            default :
                // this should never happen
                throw OrekitException.createInternalError(null);
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
                final EOPHistory eopHistory = applyEOPCorr ? getEOPHistory(conventions) : null;
                final TransformProvider interpolating =
                        new InterpolatingTransformProvider(new TODProvider(conventions, eopHistory),
                                                           true, false,
                                                           AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                           interpolationPoints, Constants.JULIAN_DAY / pointsPerDay,
                                                           OrekitConfiguration.getCacheSlotsNumber(),
                                                           Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(getMOD(conventions, applyEOPCorr), interpolating, true, factoryKey);
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
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getMOD(final boolean applyEOPCorr)
        throws OrekitException {
        return getMOD(IERSConventions.IERS_1996, applyEOPCorr);
    }

    /** Get the MOD reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getMOD(final IERSConventions conventions)
        throws OrekitException {
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
     * @exception OrekitException if data embedded in the library cannot be read
     */
    private static FactoryManagedFrame getMOD(final IERSConventions conventions, final boolean applyEOPCorr)
        throws OrekitException {

        if (conventions != IERSConventions.IERS_1996 && !applyEOPCorr) {
            // this should never happen as this method is private and called
            // only above with controlled input
            throw OrekitException.createInternalError(null);
        }

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
                throw OrekitException.createInternalError(null);
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

    /** Get the MOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP correction parameters.
     * Beware that setting this parameter to {@code false} leads to crude accuracy
     * (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (EME2000/GCRF bias compensation)
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     * @deprecated as of 6.0, replaced by {@link #getMOD(IERSConventions)}
     */
    @Deprecated
    public static FactoryManagedFrame getMEME(final boolean applyEOPCorr) throws OrekitException {
        return getMOD(IERSConventions.IERS_1996, applyEOPCorr);
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
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getTEME() throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.TEME;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tod = getTOD(IERSConventions.IERS_1996, false);
                final InterpolatingTransformProvider todInterpolating =
                        (InterpolatingTransformProvider) tod.getTransformProvider();
                final TODProvider  todRaw  = (TODProvider) todInterpolating.getRawProvider();
                final TEMEProvider temeRaw = new TEMEProvider(todRaw);
                final TransformProvider temeInterpolating =
                        new InterpolatingTransformProvider(temeRaw, true, false,
                                                           AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                           todInterpolating.getGridPoints(), todInterpolating.getStep(),
                                                           OrekitConfiguration.getCacheSlotsNumber(),
                                                           Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

                frame = new FactoryManagedFrame(tod, temeInterpolating, true, factoryKey);
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
     * @throws OrekitException if transform cannot be computed at this date
     */
    public static Transform getNonInterpolatingTransform(final Frame from, final Frame to,
                                                         final AbsoluteDate date)
        throws OrekitException {

        // common ancestor to both frames in the frames tree
        Frame currentF = from.getDepth() > to.getDepth() ? from.getAncestor(from.getDepth() - to.getDepth()) : from;
        Frame currentT = from.getDepth() > to.getDepth() ? to : to.getAncestor(to.getDepth() - from.getDepth());
        while (currentF != currentT) {
            currentF = currentF.getParent();
            currentT = currentT.getParent();
        }
        final Frame common = currentF;

        // transform from common to instance
        Transform commonToInstance = Transform.IDENTITY;
        for (Frame frame = from; frame != common; frame = frame.getParent()) {
            TransformProvider provider = frame.getTransformProvider();
            while (provider instanceof InterpolatingTransformProvider) {
                provider = ((InterpolatingTransformProvider) provider).getRawProvider();
            }
            commonToInstance =
                    new Transform(date, provider.getTransform(date), commonToInstance);
        }

        // transform from destination up to common
        Transform commonToDestination = Transform.IDENTITY;
        for (Frame frame = to; frame != common; frame = frame.getParent()) {
            commonToDestination =
                    new Transform(date, frame.getTransformProvider().getTransform(date), commonToDestination);
        }

        // transform from instance to destination via common
        return new Transform(date, commonToInstance.getInverse(), commonToDestination);

    }

}
