/* Copyright 2002-2008 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.Rotation;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;


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
 * ({@link #getGCRF()}, {@link #getCIRF2000()}, {@link #getTIRF2000(boolean)},
 * {@link #getTIRF2000()}, {@link #getITRF2005(boolean)}, {@link #getITRF2005()},
 * {@link #getEME2000()}, {@link #getMEME(boolean)}, {@link #getTEME(boolean)},
 * {@link #getPEF(boolean)} and {@link #getVeis1950()}).
 * </p>
 * <h5> International Terrestrial Reference Frame 2005 </h5>
 * <p>
 * This frame is the current (as of 2008) reference realization of
 * the International Terrestrial Reference System produced by IERS.
 * It is described in <a
 * href="http://www.iers.org/documents/publications/tn/tn32/tn32.pdf">
 * IERS conventions (2003)</a>. It replaces the Earth Centered Earth Fixed
 * frame which is the reference frame for GPS satellites.
 * <p>
 * This frame is used to define position on solid Earth. It rotates with
 * the Earth and includes the pole motion with respect to Earth crust as
 * provided by {@link org.orekit.data.DataProvidersManager IERS data}.
 * Its pole axis is the IERS Reference Pole (IRP).
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
 *           Bias, Precession and Nutation effects |                     MEME                 MEME  (Mean Equator of Date)
 *                                                 |                       |             w/o EOP corrections
 *                                                 |                       |  Nutation effects  |
 *    (Celestial Intermediate Reference Frame) CIRF2000                    |                    |
 *                                                 |                     TEME                 TEME  (True Equator of Date)
 *                          Earth natural rotation |                       |             w/o EOP corrections
 *                                                 |-------------          |    Sidereal Time   |
 *                                                 |            |          |                    |
 *  (Terrestrial Intermediate Reference Frame) TIRF2000     TIRF2000      PEF                  PEF  (Pseudo Earth Fixed)
 *                                                 |    w/o tidal effects                w/o EOP corrections
 *                                     Pole motion |            |
 *                                                 |            |
 * (International Terrestrial Reference Frame) ITRF2005     ITRF2005
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

    /** Serialiazable UID. */
    private static final long serialVersionUID = 1720647682459923909L;

    /** EME2000 frame.*/
    private static Frame eme2000 = null;

    /** ITRF2005 without tidal effects. */
    private static Frame itrf2005WithoutTidalEffects = null;

    /** ITRF2005 with tidal effects. */
    private static Frame itrf2005WithTidalEffects = null;

    /** ITRF2005 without tidal effects. */
    private static Frame tirf2000WithoutTidalEffects = null;

    /** ITRF2005 with tidal effects. */
    private static Frame tirf2000WithTidalEffects = null;

    /** CIRF frame. */
    private static Frame cirf = null;

    /** Veis 1950 with tidal effects. */
    private static Frame veis1950 = null;

    /** PEF without EOP corrections. */
    private static Frame pefWithoutEopCorrections = null;

    /** PEF with EOP corrections. */
    private static Frame pefWithEopCorrections = null;

    /** TEME without EOP corrections. */
    private static Frame temeWithoutEopCorrections = null;

    /** TEME with EOP corrections. */
    private static Frame temeWithEopCorrections = null;

    /** MEME without EOP corrections. */
    private static Frame memeWithoutEopCorrections = null;

    /** MEME with EOP corrections. */
    private static Frame memeWithEopCorrections = null;


    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private FramesFactory() {
    }

    /** Get the unique GCRF frame.
     * <p>The GCRF frame is the root frame in the frame tree.</p>
     * @return the unique instance of the GCRF frame
     */
    public static Frame getGCRF() {
        return Frame.getRoot();
    }

    /** Get the unique EME2000 frame.
     * <p>The EME2000 frame is also called the J2000 frame.
     * The former denomination is preferred in Orekit.</p>
     * @return the unique instance of the EME2000 frame
     */
    public static Frame getEME2000() {
        synchronized (FramesFactory.class) {

            if (eme2000 == null) {
                eme2000 = new EME2000Frame("EME2000");
            }

            return eme2000;

        }
    }

    /** Get the ITRF2005 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getITRF2005() throws OrekitException {
        return getITRF2005(true);
    }

    /** Get the ITRF2005 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getITRF2005(final boolean ignoreTidalEffects) throws OrekitException {
        synchronized (FramesFactory.class) {

            if (ignoreTidalEffects) {
                if (itrf2005WithoutTidalEffects == null) {
                    itrf2005WithoutTidalEffects =
                        new ITRF2005Frame(false, AbsoluteDate.J2000_EPOCH, "ITRF2005");
                }
                return itrf2005WithoutTidalEffects;
            } else {
                if (itrf2005WithTidalEffects == null) {
                    itrf2005WithTidalEffects =
                        new ITRF2005Frame(true, AbsoluteDate.J2000_EPOCH, "ITRF2005 w/o tides");
                }
                return itrf2005WithTidalEffects;
            }
        }

    }

    /** Get the TIRF2000 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTIRF2000() throws OrekitException {
        return getTIRF2000(true);
    }

    /** Get the TIRF2000 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTIRF2000(final boolean ignoreTidalEffects) throws OrekitException {
        synchronized (FramesFactory.class) {

            if (ignoreTidalEffects) {
                if (tirf2000WithoutTidalEffects == null) {
                    tirf2000WithoutTidalEffects =
                        new TIRF2000Frame(false, AbsoluteDate.J2000_EPOCH, "TIRF2000");
                }
                return tirf2000WithoutTidalEffects;
            } else {
                if (tirf2000WithTidalEffects == null) {
                    tirf2000WithTidalEffects =
                        new TIRF2000Frame(true, AbsoluteDate.J2000_EPOCH, "TIRF2000 w/o tides");
                }
                return tirf2000WithTidalEffects;
            }

        }
    }

    /** Get the CIRF2000 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getCIRF2000() throws OrekitException {
        synchronized (FramesFactory.class) {

            if (cirf == null) {
                cirf = new CIRF2000Frame(AbsoluteDate.J2000_EPOCH, "CIRF2000");
            }

            return cirf;

        }
    }

    /** Get the VEIS 1950 reference frame.
     * @return the selected reference frame singleton.
     */
    public static Frame getVeis1950() {
        synchronized (FramesFactory.class) {

            if (veis1950 == null) {
                veis1950 = new Frame(getEME2000(),
                                     new Transform(new Rotation(0.99998141186121629647,
                                                                -2.01425201682020570e-5,
                                                                -2.43283773387856897e-3,
                                                                5.59078052583013584e-3,
                                                                true)),
                                     "VEIS1950");
            }

            return veis1950;

        }
    }

    /** Get the PEF reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP parameters. Beware
     * that setting this parameter to {@code false} leads to very crude accuracy
     * (order of magnitudes for errors are above 100m in LEO and above 1000m in GEO).
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (here dut1 and lod)
     * @return the selected reference frame singleton.
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getPEF(final boolean applyEOPCorr) throws OrekitException {
        synchronized (FramesFactory.class) {

            if (applyEOPCorr) {
                if (pefWithEopCorrections == null) {
                    pefWithEopCorrections =
                        new PEFFrame(true, AbsoluteDate.J2000_EPOCH, "PEF with EOP");
                }
                return pefWithEopCorrections;
            } else {
                if (pefWithoutEopCorrections == null) {
                    pefWithoutEopCorrections =
                        new PEFFrame(false, AbsoluteDate.J2000_EPOCH, "PEF without EOP");
                }
                return pefWithoutEopCorrections;
            }

        }
    }

    /** Get the TEME reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP parameters. Beware
     * that setting this parameter to {@code false} leads to very crude accuracy
     * (order of magnitudes for errors are about 1m in LEO and 10m in GEO).
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (here, nutation)
     * @return the selected reference frame singleton.
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTEME(final boolean applyEOPCorr) throws OrekitException {
        synchronized (FramesFactory.class) {

            if (applyEOPCorr) {
                if (temeWithEopCorrections == null) {
                    temeWithEopCorrections =
                        new TEMEFrame(true, AbsoluteDate.J2000_EPOCH, "TEME with EOP");
                }
                return temeWithEopCorrections;
            } else {
                if (temeWithoutEopCorrections == null) {
                    temeWithoutEopCorrections =
                        new TEMEFrame(false, AbsoluteDate.J2000_EPOCH, "TEME without EOP");
                }
                return temeWithoutEopCorrections;
            }

        }
    }

    /** Get the MEME reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for
     * consistency with legacy software that don't handle EOP parameters. Beware
     * that setting this parameter to {@code false} leads to very crude accuracy
     * (order of magnitudes for errors are about 1m in LEO and 10m in GEO).
     * </p>
     * @param applyEOPCorr if true, EOP corrections are applied (EME2000/GCRF bias compensation)
     * @return the selected reference frame singleton.
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    public static Frame getMEME(final boolean applyEOPCorr) throws OrekitException {
        synchronized (FramesFactory.class) {

            if (applyEOPCorr) {
                if (memeWithEopCorrections == null) {
                    memeWithEopCorrections =
                        new MEMEFrame(true, AbsoluteDate.J2000_EPOCH, "MEME with EOP");
                }
                return memeWithEopCorrections;
            } else {
                if (memeWithoutEopCorrections == null) {
                    memeWithoutEopCorrections =
                        new MEMEFrame(false, AbsoluteDate.J2000_EPOCH, "MEME without EOP");
                }
                return memeWithoutEopCorrections;
            }

        }
    }

}
