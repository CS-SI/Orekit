/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


/** Factory for predefined reference frames.
 *
 * <h5> FramesFactory Presentation </h5>
 * <p>
 * Several predefined reference frames are implemented in OREKIT.
 * They are linked together in a tree with the <i>Geocentric
 * Celestial Reference Frame</i> (GCRF) as the root of the tree.
 * </p>
 * <h5> Reference Frames </h5>
 * <p>
 * The user can retrieve those reference frames using various static methods
 * ({@link FramesFactory#getFrame(Predefined)},
 * {@link #getGCRF()}, {@link #getCIRF2000()},
 * {@link #getTIRF2000(boolean)}, {@link #getTIRF2000()},
 * {@link #getITRF93(boolean)}, {@link #getITRF93()},
 * {@link #getITRF97(boolean)}, {@link #getITRF97()},
 * {@link #getITRF2000(boolean)}, {@link #getITRF2000()},
 * {@link #getITRF2005(boolean)}, {@link #getITRF2005()},
 * {@link #getITRF2008(boolean)}, {@link #getITRF2008()},
 * {@link #getEME2000()}, {@link #getMOD(boolean)}, {@link #getTOD(boolean)},
 * {@link #getGTOD(boolean)}, {@link #getTEME(boolean)} and {@link #getVeis1950()}).
 * </p>
 * <h5> International Terrestrial Reference Frame 2008 </h5>
 * <p>
 * This frame is the current (as of 2008) reference realization of
 * the International Terrestrial Reference System produced by IERS.
 * It is described in <a href="ftp://tai.bipm.org/iers/conv2003/tn32.pdf">
 * IERS conventions (2003)</a>. It replaces the Earth Centered Earth Fixed
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
 * Helmert transformations.
 * </p>
 * <p>
 * OREKIT proposes all the intermediate frames used to build this specific frame.
 * This implementation follows the new non-rotating origin paradigm
 * mandated by IAU 2000 resolution B1.8. It is therefore based on
 * Celestial Ephemeris Origin (CEO-based) and Earth Rotating Angle.
 * </p>
 * <h5> Classical paradigm: equinox-based transformations </h5>
 * <p>
 * The classical paradigm used prior to IERS conventions 2003 is equinox based and
 * uses more intermediate frames. Only some of these frames are supported in Orekit.
 * </p>
 * <p>
 * Here is a schematic representation of the predefined reference frames tree:
 * </p>
 * <pre>
 *                                                GCRF
 *                                                 |
 *                                                 |---------------------------------------------
 *                                                 |                       |     Frame bias     |
 *                                                 |                       |                 EME2000
 *                                                 |                       |                    |
 *                                                 |                       | Precession effects |
 *                                                 |                       |                    |
 *           Bias, Precession and Nutation effects |                      MOD                  MOD  (Mean Equator Of Date)
 *                                                 |                       |             w/o EOP corrections
 *                                                 |                       |  Nutation effects  |
 *    (Celestial Intermediate Reference Frame) CIRF2000                    |                    |
 *                                                 |                      TOD                  TOD  (True Equator Of Date)
 *                          Earth natural rotation |                       |             w/o EOP corrections
 *                                                 |-------------          |    Sidereal Time   |
 *                                                 |            |          |                    |
 *  (Terrestrial Intermediate Reference Frame) TIRF2000     TIRF2000     GTOD                 GTOD  (Greenwich True Of Date)
 *                                                 |    w/o tidal effects                w/o EOP corrections
 *                                     Pole motion |            |                               |
 *                                                 |            |                               |
 * (International Terrestrial Reference Frame)   ITRF         ITRF                          VEIS1950
 *                                                 |    w/o tidal effects
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
 * @version $Revision$ $Date$
 */
public class FramesFactory implements Serializable {

    /** Default regular expression for the XML EOP files (IAU1980 compatibles). */
    public static final String XML_EOP_1980_FILENAME = "^finals\\..*\\.xml$";

    /** Default regular expression for the EOPC04 files (IAU1980 compatibles). */
    public static final String EOPC04_1980_FILENAME = "^eopc04\\.(\\d\\d)$";

    /** Default regular expression for the BulletinB files (IAU1980 compatibles). */
    public static final String BULLETINB_1980_FILENAME = "^bulletinb((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** Default regular expression for the XML EOP files (IAU2000 compatibles). */
    public static final String XML_EOP_2000_FILENAME = "^finals2000A\\..*\\.xml$";

    /** Default regular expression for the EOPC04 files (IAU2000 compatibles). */
    public static final String EOPC04_2000_FILENAME = "^eopc04_IAU2000\\.(\\d\\d)$";

    /** Default regular expression for the BulletinB files (IAU2000 compatibles). */
    public static final String BULLETINB_2000_FILENAME = "^bulletinb_IAU2000((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** Serializable UID. */
    private static final long serialVersionUID = 1720647682459923909L;

    /** Predefined frames. */
    private static Map<Predefined, FactoryManagedFrame> FRAMES =
        new WeakHashMap<Predefined, FactoryManagedFrame>();

    /** EOP 1980 loaders. */
    private static final List<EOP1980HistoryLoader> EOP_1980_LOADERS =
        new ArrayList<EOP1980HistoryLoader>();

    /** EOP 2000 loaders. */
    private static final List<EOP2000HistoryLoader> EOP_2000_LOADERS =
        new ArrayList<EOP2000HistoryLoader>();

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private FramesFactory() {
    }

    /** Add a loader for EOP 1980 history.
     * @param loader custom loader to add for the EOP history
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String)
     * @see #clearEOP1980HistoryLoaders()
     * @see #addEOP2000HistoryLoader(EOP2000HistoryLoader)
     */
    public static void addEOP1980HistoryLoader(final EOP1980HistoryLoader loader) {
        synchronized (EOP_1980_LOADERS) {
            EOP_1980_LOADERS.add(loader);
        }
    }

    /** Add the default loaders for EOP 1980 history.
     * <p>
     * The default loaders look for IERS EOP 05 C04 and bulletins B files.
     * </p>
     * @param xmleopSupportedNames regular expression for supported XML EOP files names
     * (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP05 C04 files names
     * (may be null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names
     * (may be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04_05/">IERS EOP 05 C04 files</a>
     * @see <a href="http://hpiers.obspm.fr/eoppc/bul/bulb/">IERS bulletins B</a>
     * @see #addEOP1980HistoryLoader(EOP1980HistoryLoader)
     * @see #clearEOP1980HistoryLoaders()
     * @see #addDefaultEOP2000HistoryLoaders(String, String, String)
     */
    public static void addDefaultEOP1980HistoryLoaders(final String xmleopSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames) {
        final String xmlNames =
            (xmleopSupportedNames == null) ? XML_EOP_1980_FILENAME : xmleopSupportedNames;
        addEOP1980HistoryLoader(new XMLEOPFilesLoader(xmlNames));
        final String eopcNames =
            (eopC04SupportedNames == null) ? EOPC04_1980_FILENAME : eopC04SupportedNames;
        addEOP1980HistoryLoader(new EOP05C04FilesLoader(eopcNames));
        final String bulBNames =
            (bulletinBSupportedNames == null) ? BULLETINB_1980_FILENAME : bulletinBSupportedNames;
        addEOP1980HistoryLoader(new BulletinBFilesLoader(bulBNames));
    }

    /** Clear loaders for EOP 1980 history.
     * @see #addEOP1980HistoryLoader(EOP1980HistoryLoader)
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String)
     * @see #clearEOP2000HistoryLoaders()
     */
    public static void clearEOP1980HistoryLoaders() {
        synchronized (EOP_1980_LOADERS) {
            EOP_1980_LOADERS.clear();
        }
    }

    /** Get Earth Orientation Parameters history (IAU1980) data.
     * <p>
     * If no {@link EOP1980HistoryLoader} has been added by calling {@link
     * #addEOP1980HistoryLoader(EOP1980HistoryLoader) addEOP1980HistoryLoader}
     * or if {@link #clearEOP1980HistoryLoaders() clearEOP1980HistoryLoaders}
     * has been called afterwards,
     * the {@link #addDefaultEOP1980HistoryLoaders(String, String, String)} method
     * will be called automatically with two null parameters (supported file names).
     * </p>
     * @return Earth Orientation Parameters history (IAU1980) data
     * @exception OrekitException if the data cannot be loaded
     */
    public static EOP1980History getEOP1980History() throws OrekitException {
        final EOP1980History history = new EOP1980History();
        if (EOP_1980_LOADERS.isEmpty()) {
            addDefaultEOP1980HistoryLoaders(null, null, null);
        }
        for (final EOP1980HistoryLoader loader : EOP_1980_LOADERS) {
            loader.fillHistory(history);
        }
        history.checkEOPContinuity(5 * Constants.JULIAN_DAY);
        return history;
    }

    /** Add a loader for EOP 2000 history.
     * @param loader custom loader to add for the EOP history
     * @see #addDefaultEOP2000HistoryLoaders(String, String, String)
     * @see #clearEOP2000HistoryLoaders()
     * @see #addEOP1980HistoryLoader(EOP1980HistoryLoader)
     */
    public static void addEOP2000HistoryLoader(final EOP2000HistoryLoader loader) {
        synchronized (EOP_2000_LOADERS) {
            EOP_2000_LOADERS.add(loader);
        }
    }

    /** Add the default loaders for EOP 2000 history.
     * <p>
     * The default loaders look for IERS EOP 05 C04 and bulletins B files.
     * </p>
     * @param xmleopSupportedNames regular expression for supported XML EOP files names
     * (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP05 C04 files names
     * (may be null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names
     * (may be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04_05/">IERS EOP 05 C04 files</a>
     * @see <a href="http://hpiers.obspm.fr/eoppc/bul/bulb/">IERS bulletins B</a>
     * @see #addEOP2000HistoryLoader(EOP2000HistoryLoader)
     * @see #clearEOP2000HistoryLoaders()
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String)
     */
    public static void addDefaultEOP2000HistoryLoaders(final String xmleopSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames) {
        final String xmlNames =
            (xmleopSupportedNames == null) ? XML_EOP_2000_FILENAME : xmleopSupportedNames;
        addEOP2000HistoryLoader(new XMLEOPFilesLoader(xmlNames));
        final String eopcNames =
            (eopC04SupportedNames == null) ? EOPC04_2000_FILENAME : eopC04SupportedNames;
        addEOP2000HistoryLoader(new EOP05C04FilesLoader(eopcNames));
        final String bulBNames =
            (bulletinBSupportedNames == null) ? BULLETINB_2000_FILENAME : bulletinBSupportedNames;
        addEOP2000HistoryLoader(new BulletinBFilesLoader(bulBNames));
    }

    /** Clear loaders for EOP 2000 history.
     * @see #addEOP2000HistoryLoader(EOP2000HistoryLoader)
     * @see #addDefaultEOP2000HistoryLoaders(String, String, String)
     * @see #clearEOP1980HistoryLoaders()
     */
    public static void clearEOP2000HistoryLoaders() {
        synchronized (EOP_2000_LOADERS) {
            EOP_2000_LOADERS.clear();
        }
    }

    /** Get Earth Orientation Parameters history (IAU2000) data.
     * <p>
     * If no {@link EOP2000HistoryLoader} has been added by calling {@link
     * #addEOP2000HistoryLoader(EOP2000HistoryLoader) addEOP2000HistoryLoader}
     * or if {@link #clearEOP2000HistoryLoaders() clearEOP2000HistoryLoaders}
     * has been called afterwards,
     * the {@link #addDefaultEOP2000HistoryLoaders(String, String, String)} method
     * will be called automatically with two null parameters (supported file names).
     * </p>
     * @return Earth Orientation Parameters history (IAU2000) data
     * @exception OrekitException if the data cannot be loaded
     */
    public static EOP2000History getEOP2000History() throws OrekitException {
        final EOP2000History history = new EOP2000History();
        if (EOP_2000_LOADERS.isEmpty()) {
            addDefaultEOP2000HistoryLoaders(null, null, null);
        }
        for (final EOP2000HistoryLoader loader : EOP_2000_LOADERS) {
            loader.fillHistory(history);
        }
        history.checkEOPContinuity(5 * Constants.JULIAN_DAY);
        return history;
    }

    /** Get one of the predefined frames.
     * @param factoryKey key of the frame within the factory
     * @return the predefined frame
     * @exception OrekitException if frame cannot be built due to missing data
     */
    public static Frame getFrame(final Predefined factoryKey)
        throws OrekitException {
        switch (factoryKey) {
        case GCRF :
            return getGCRF();
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
        case TIRF_2000_WITHOUT_TIDAL_EFFECTS :
            return getTIRF2000(true);
        case TIRF_2000_WITH_TIDAL_EFFECTS :
            return getTIRF2000(false);
        case CIRF_2000 :
            return getCIRF2000();
        case VEIS_1950 :
            return getVeis1950();
        case GTOD_WITHOUT_EOP_CORRECTIONS :
            return getGTOD(false);
        case GTOD_WITH_EOP_CORRECTIONS :
            return getGTOD(true);
        case TOD_WITHOUT_EOP_CORRECTIONS :
            return getTOD(false);
        case TOD_WITH_EOP_CORRECTIONS :
            return getTOD(true);
        case MOD_WITHOUT_EOP_CORRECTIONS :
            return getMOD(false);
        case MOD_WITH_EOP_CORRECTIONS :
            return getMOD(true);
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
                frame = new EME2000Frame(Predefined.EME2000);
                FRAMES.put(Predefined.EME2000, frame);
            }

            return frame;

        }
    }

    /** Get the ITRF2008 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2008() throws OrekitException {
        return getITRF2008(true);
    }

    /** Get the ITRF2008 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2008(final boolean ignoreTidalEffects) throws OrekitException {

        final Predefined factoryKey = ignoreTidalEffects ?
                                      Predefined.ITRF_2008_WITHOUT_TIDAL_EFFECTS :
                                      Predefined.ITRF_2008_WITH_TIDAL_EFFECTS;

        // Helmert transformation between ITRF2005 and ITRF 2008
        // see http://itrf.ign.fr/ITRF_solutions/2008/tp_08-05.php
        //            T1      T2       T3       D     R1       R2     R3
        //            mm      mm       mm     10-9    mas     mas     mas
        //           -0.5    -0.9    -4.7    0.94    0.000   0.000   0.000
        //    +/-     0.2     0.2     0.2    0.03    0.008   0.008   0.008
        //     
        //    Rates   0.3     0.0     0.0    0.00    0.000   0.000   0.000
        //    +/-     0.2     0.2     0.2    0.03    0.008   0.008   0.008
        //    Table 1: Transformation parameters at epoch 2005.0 and their rates from ITRF2008 to ITRF2005
        //                         (ITRF2005 minus ITRF2008)
        return getITRSRealization(factoryKey, getITRF2005(ignoreTidalEffects), 2005,
                                  0.5, 0.9, 4.7, 0.000, 0.000, 0.000,
                                 -0.3, 0.0, 0.0, 0.000, 0.000, 0.000);

    }

    /** Get the ITRF2005 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2005() throws OrekitException {
        return getITRF2005(true);
    }

    /** Get the ITRF2005 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getITRF2005(final boolean ignoreTidalEffects) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = ignoreTidalEffects ?
                                          Predefined.ITRF_2005_WITHOUT_TIDAL_EFFECTS :
                                          Predefined.ITRF_2005_WITH_TIDAL_EFFECTS;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new ITRFFrame(ignoreTidalEffects, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
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

        // Helmert transformation between ITRF2005 and ITRF 2000
        // see http://itrf.ign.fr/ITRF_solutions/2005/tp_05-00.php
        //             T1      T2      T3      D       R1      R2      R3
        //             mm      mm      mm      10-9    mas     mas     mas
        //             0.1    -0.8    -5.8     0.40   0.000   0.000   0.000
        //    +/-      0.3     0.3     0.3     0.05   0.012   0.012   0.012
        //     
        //    Rates   -0.2     0.1    -1.8     0.08   0.000   0.000   0.000
        //    +/-      0.3     0.3     0.3     0.05   0.012   0.012   0.012
        //
        //    Table 1: Transformation parameters at epoch 2000.0 and their rates from ITRF2005 to ITRF2000
        //                         (ITRF2000 minus ITRF2005)
        return getITRSRealization(factoryKey, getITRF2005(ignoreTidalEffects), 2000,
                                  0.1, -0.8, -5.8, 0.000, 0.000, 0.000,
                                 -0.2,  0.1, -1.8, 0.000, 0.000, 0.000);

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

        // Helmert transformation between ITRF2000 and ITRF97
        // see ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP
        //-------------------------------------------------------------------------------------
        //SOLUTION         T1    T2    T3      D       R1      R2      R3    EPOCH   Ref. 
        //UNITS----------> cm    cm    cm     ppb     .001"   .001"   .001"         IERS Tech.
        //                 .     .     .       .       .       .       .            Note #
        //        RATES    T1    T2    T3      D       R1      R2      R3
        //UNITS----------> cm/y  cm/y  cm/y  ppb/y  .001"/y .001"/y .001"/y
        //-------------------------------------------------------------------------------------
        //   ITRF97       0.67  0.61 -1.85    1.55    0.00    0.00    0.00   1997.0   27
        //        rates   0.00 -0.06 -0.14    0.01    0.00    0.00    0.02     
        //   ...
        //
        // Note : These parameters are derived from those already published in the IERS
        // Technical Notes indicated in the table above. The transformation parameters 
        // should be used with the standard model (1) given below and are valid at the
        // indicated epoch.
        //
        // : XS :    : X :   : T1 :   :  D   -R3   R2 : : X :
        // :    :    :   :   :    :   :               : :   :
        // : YS :  = : Y : + : T2 : + :  R3   D   -R1 : : Y :                       (1)
        // :    :    :   :   :    :   :               : :   :
        // : ZS :    : Z :   : T3 :   : -R2   R1   D  : : Z :
        //
        // Where X,Y,Z are the coordinates in ITRF2000 and XS,YS,ZS are the coordinates in 
        // the other frames.
        return getITRSRealization(factoryKey, getITRF2000(ignoreTidalEffects), 1997,
                                  6.7,  6.1, -18.5, 0.00, 0.00, 0.00,
                                  0.0, -0.6,  -1.4, 0.00, 0.00, 0.002);

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

        // Helmert transformation between ITRF2000 and ITRF93
        // see ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP
        //-------------------------------------------------------------------------------------
        //SOLUTION         T1    T2    T3      D       R1      R2      R3    EPOCH   Ref. 
        //UNITS----------> cm    cm    cm     ppb     .001"   .001"   .001"         IERS Tech.
        //                 .     .     .       .       .       .       .            Note #
        //        RATES    T1    T2    T3      D       R1      R2      R3
        //UNITS----------> cm/y  cm/y  cm/y  ppb/y  .001"/y .001"/y .001"/y
        //-------------------------------------------------------------------------------------
        //   ...
        //   ITRF93       1.27  0.65 -2.09    1.95   -0.39    0.80   -1.14   1988.0   18
        //        rates  -0.29 -0.02 -0.06    0.01   -0.11   -0.19    0.07   
        //   ...
        //
        // Note : These parameters are derived from those already published in the IERS
        // Technical Notes indicated in the table above. The transformation parameters 
        // should be used with the standard model (1) given below and are valid at the
        // indicated epoch.
        //
        // : XS :    : X :   : T1 :   :  D   -R3   R2 : : X :
        // :    :    :   :   :    :   :               : :   :
        // : YS :  = : Y : + : T2 : + :  R3   D   -R1 : : Y :                       (1)
        // :    :    :   :   :    :   :               : :   :
        // : ZS :    : Z :   : T3 :   : -R2   R1   D  : : Z :
        //
        // Where X,Y,Z are the coordinates in ITRF2000 and XS,YS,ZS are the coordinates in 
        // the other frames.
        return getITRSRealization(factoryKey, getITRF2000(ignoreTidalEffects), 1988,
                                  12.7,  6.5, -20.9, -0.39,  0.80, -1.14,
                                  -2.9, -0.2,  -0.6, -0.11, -0.19,  0.07);

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
                frame = new DerivedITRSRealizationFrame(parent, helmertTransformation, factoryKey);

                FRAMES.put(factoryKey, frame);

            }

            return frame;

        }
    }

    /** Get the TIRF2000 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getTIRF2000() throws OrekitException {
        return getTIRF2000(true);
    }

    /** Get the TIRF2000 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getTIRF2000(final boolean ignoreTidalEffects) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = ignoreTidalEffects ?
                                          Predefined.TIRF_2000_WITHOUT_TIDAL_EFFECTS :
                                          Predefined.TIRF_2000_WITH_TIDAL_EFFECTS;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new TIRF2000Frame(ignoreTidalEffects, factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the CIRF2000 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static FactoryManagedFrame getCIRF2000() throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.CIRF_2000;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new CIRF2000Frame(factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

    /** Get the VEIS 1950 reference frame.
     * <p>Its parent frame is the GTOD frame without EOP corrections.<p>
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
                frame = new VEISFrame(factoryKey);
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
     * (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (here, lod)
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getGTOD(final boolean applyEOPCorr) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = applyEOPCorr ?
                                          Predefined.GTOD_WITH_EOP_CORRECTIONS :
                                          Predefined.GTOD_WITHOUT_EOP_CORRECTIONS;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new GTODFrame(applyEOPCorr, factoryKey);
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
     * (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (here, lod)
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     * @deprecated as of 5.1, replaced by {@link #getGTOD(boolean)}
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
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (here, nutation)
     * @return the selected reference frame singleton.
     * @exception OrekitException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getTOD(final boolean applyEOPCorr) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = applyEOPCorr ?
                                          Predefined.TOD_WITH_EOP_CORRECTIONS :
                                          Predefined.TOD_WITHOUT_EOP_CORRECTIONS;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new TODFrame(applyEOPCorr, factoryKey);
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
     */
    public static FactoryManagedFrame getMOD(final boolean applyEOPCorr) throws OrekitException {
        synchronized (FramesFactory.class) {

            // try to find an already built frame
            final Predefined factoryKey = applyEOPCorr ?
                                          Predefined.MOD_WITH_EOP_CORRECTIONS :
                                          Predefined.MOD_WITHOUT_EOP_CORRECTIONS;
            FactoryManagedFrame frame = FRAMES.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new MODFrame(applyEOPCorr, factoryKey);
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
     * @deprecated as of 5.1, replaced by {@link #getMOD(boolean)}
     */
    @Deprecated
    public static FactoryManagedFrame getMEME(final boolean applyEOPCorr) throws OrekitException {
        return getMOD(applyEOPCorr);
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
                frame = new TEMEFrame(factoryKey);
                FRAMES.put(factoryKey, frame);
            }

            return frame;

        }
    }

}
