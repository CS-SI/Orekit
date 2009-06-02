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


/** Predefined reference frames class.
 *
 * <h5> FrameFactory Presentation </h5>
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
 * {@link #getEME2000()}, {@link #getMEME(boolean)}, {@link #getMEME()},
 * {@link #getTEME(boolean)}, {@link #getTEME()}, {@link #getPEF(boolean)},
 * {@link #getPEF()} and {@link #getVeis1950()}).
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
public class FrameFactory implements Serializable {

    /** Serialiazable UID. */
    private static final long serialVersionUID = -6981146543760234087L;

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private FrameFactory() {
    };

    /** Get the unique GCRF frame.
     * <p>The GCRF frame is the root frame in the frame tree.</p>
     * @return the unique instance of the GCRF frame
     * @see Frame
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
        return LazyEME2000Holder.INSTANCE;
    }

    /** Get the ITRF2005 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getITRF2005()
        throws OrekitException {
        return getITRF2005(true);
    }

    /** Get the ITRF2005 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getITRF2005(final boolean ignoreTidalEffects)
        throws OrekitException {
        if (ignoreTidalEffects) {
            if (LazyITRF2005IgnoredTidalEffectHolder.INSTANCE == null) {
                throw LazyITRF2005IgnoredTidalEffectHolder.OREKIT_EXCEPTION;
            }
            return LazyITRF2005IgnoredTidalEffectHolder.INSTANCE;
        } else {
            if (LazyITRF2005Holder.INSTANCE == null) {
                throw LazyITRF2005Holder.OREKIT_EXCEPTION;
            }
            return LazyITRF2005Holder.INSTANCE;
        }
    }

    /** Get the TIRF2000 reference frame, ignoring tidal effects.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTIRF2000()
        throws OrekitException {
        return getTIRF2000(true);
    }

    /** Get the TIRF2000 reference frame.
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTIRF2000(final boolean ignoreTidalEffects)
        throws OrekitException {
        if (ignoreTidalEffects) {
            if (LazyTIRF2000IgnoredTidalEffectsHolder.INSTANCE == null) {
                throw LazyTIRF2000IgnoredTidalEffectsHolder.OREKIT_EXCEPTION;
            }
            return LazyTIRF2000IgnoredTidalEffectsHolder.INSTANCE;
        } else {
            if (LazyTIRF2000Holder.INSTANCE == null) {
                throw LazyTIRF2000Holder.OREKIT_EXCEPTION;
            }
            return LazyTIRF2000Holder.INSTANCE;
        }
    }

    /** Get the CIRF2000 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getCIRF2000()
        throws OrekitException {
        if (LazyCIRF2000Holder.INSTANCE == null) {
            throw LazyCIRF2000Holder.OREKIT_EXCEPTION;
        }
        return LazyCIRF2000Holder.INSTANCE;
    }

    /** Get the VEIS 1950 reference frame.
     * @return the selected reference frame singleton.
     */
    public static Frame getVeis1950() {
        return LazyVeis1950Holder.INSTANCE;
    }

    /** Get the PEF reference frame without nutation correction.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getPEF()
        throws OrekitException {
        return getPEF(false);
    }

    /** Get the PEF reference frame.
     * @param applyEOPCorr if true, nutation correction is applied
     * @return the selected reference frame singleton.
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getPEF(final boolean applyEOPCorr)
        throws OrekitException {
        if (applyEOPCorr) {
            if (LazyPEFWithEOPHolder.INSTANCE == null) {
                throw LazyPEFWithEOPHolder.OREKIT_EXCEPTION;
            }
            return LazyPEFWithEOPHolder.INSTANCE;
        } else {
            if (LazyPEFWoutEOPHolder.INSTANCE == null) {
                throw LazyPEFWoutEOPHolder.OREKIT_EXCEPTION;
            }
            return LazyPEFWoutEOPHolder.INSTANCE;
        }
    }

    /** Get the TEME reference frame without nutation correction.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTEME()
        throws OrekitException {
        return getTEME(false);
    }

    /** Get the TEME reference frame.
     * @param applyEOPCorr if true, nutation correction is applied
     * @return the selected reference frame singleton.
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTEME(final boolean applyEOPCorr)
        throws OrekitException {
        if (applyEOPCorr) {
            if (LazyTEMEWithEOPHolder.INSTANCE == null) {
                throw LazyTEMEWithEOPHolder.OREKIT_EXCEPTION;
            }
            return LazyTEMEWithEOPHolder.INSTANCE;
        } else {
            if (LazyTEMEWoutEOPHolder.INSTANCE == null) {
                throw LazyTEMEWoutEOPHolder.OREKIT_EXCEPTION;
            }
            return LazyTEMEWoutEOPHolder.INSTANCE;
        }
    }

    /** Get the MEME reference frame without nutation correction.
     * @return the selected reference frame singleton.
     */
    public static Frame getMEME() {
        return getMEME(false);
    }

    /** Get the MEME reference frame.
     * @param applyEOPCorr if true, nutation correction is applied
     * @return the selected reference frame singleton.
     */
    public static Frame getMEME(final boolean applyEOPCorr) {
        if (applyEOPCorr) {
            return LazyMEMEWithEOPHolder.INSTANCE;
        } else {
            return LazyMEMEWoutEOPHolder.INSTANCE;
        }
    }

    // We use the Initialization on demand holder idiom to store
    // the singletons, as it is both thread-safe, efficient (no
    // synchronization) and works with all versions of java.

    /** Holder for the EME2000 frame singleton. */
    private static class LazyEME2000Holder {

        /** Unique instance. */
        private static final Frame INSTANCE = new EME2000Frame("EME2000");

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyEME2000Holder() {
        }

    }

    /** Holder for the ITRF 2005 frame with tidal effects singleton. */
    private static class LazyITRF2005Holder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyTIRF2000Holder.INSTANCE == null) {
                    tmpException = LazyTIRF2000Holder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new ITRF2005Frame(false, AbsoluteDate.J2000_EPOCH,
                                                 "ITRF2005");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyITRF2005Holder() {
        }

    }

    /** Holder for the ITRF 2005 frame without tidal effects singleton. */
    private static class LazyITRF2005IgnoredTidalEffectHolder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyTIRF2000IgnoredTidalEffectsHolder.INSTANCE == null) {
                    tmpException = LazyTIRF2000IgnoredTidalEffectsHolder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new ITRF2005Frame(true, AbsoluteDate.J2000_EPOCH,
                                                 "ITRF2005 w/o tides");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyITRF2005IgnoredTidalEffectHolder() {
        }

    }

    /** Holder for the TIRF 2000 frame with tidal effects singleton. */
    private static class LazyTIRF2000Holder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyCIRF2000Holder.INSTANCE == null) {
                    tmpException = LazyCIRF2000Holder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new TIRF2000Frame(false, AbsoluteDate.J2000_EPOCH,
                                                 "TIRF2000");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyTIRF2000Holder() {
        }

    }

    /** Holder for the TIRF 2000 frame without tidal effects singleton. */
    private static class LazyTIRF2000IgnoredTidalEffectsHolder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyCIRF2000Holder.INSTANCE == null) {
                    tmpException = LazyCIRF2000Holder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new TIRF2000Frame(true, AbsoluteDate.J2000_EPOCH,
                                                 "TIRF2000 w/o tides");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyTIRF2000IgnoredTidalEffectsHolder() {
        }

    }

    /** Holder for the CIRF 2000 frame singleton. */
    private static class LazyCIRF2000Holder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                tmpFrame = new CIRF2000Frame(AbsoluteDate.J2000_EPOCH, "CIRF2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyCIRF2000Holder() {
        }

    }

    /** Holder for the Veis 1950 frame singleton. */
    private static class LazyVeis1950Holder {

        /** Unique instance. */
        private static final Frame INSTANCE =
            new Frame(getEME2000(),
                      new Transform(new Rotation(0.99998141186121629647,
                                                 -2.01425201682020570e-5,
                                                 -2.43283773387856897e-3,
                                                 5.59078052583013584e-3,
                                                 true)),
                      "VEIS1950");


        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyVeis1950Holder() {
        }

    }

    /** Holder for the PEF frame with nutation correction singleton. */
    private static class LazyPEFWithEOPHolder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyTEMEWithEOPHolder.INSTANCE == null) {
                    tmpException = LazyTEMEWithEOPHolder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new PEFFrame(true, AbsoluteDate.J2000_EPOCH, "PEF with EOP");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyPEFWithEOPHolder() {
        }

    }

    /** Holder for the PEF frame without nutation correction singleton. */
    private static class LazyPEFWoutEOPHolder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyTEMEWoutEOPHolder.INSTANCE == null) {
                    tmpException = LazyTEMEWoutEOPHolder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new PEFFrame(false, AbsoluteDate.J2000_EPOCH, "PEF without EOP");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyPEFWoutEOPHolder() {
        }

    }

    /** Holder for the TEME frame with nutation correction singleton. */
    private static class LazyTEMEWithEOPHolder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                tmpFrame = new TEMEFrame(true, AbsoluteDate.J2000_EPOCH, "TEME with EOP");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyTEMEWithEOPHolder() {
        }

    }

    /** Holder for the TEME without nutation correction frame singleton. */
    private static class LazyTEMEWoutEOPHolder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                tmpFrame = new TEMEFrame(false, AbsoluteDate.J2000_EPOCH, "TEME without EOP");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyTEMEWoutEOPHolder() {
        }

    }

    /** Holder for the MEME frame with nutation correction singleton. */
    private static class LazyMEMEWithEOPHolder {

        /** Unique instance. */
        private static final Frame INSTANCE =
            new MEMEFrame(true, AbsoluteDate.J2000_EPOCH, "MEME with EOP");

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyMEMEWithEOPHolder() {
        }

    }

    /** Holder for the MEME frame without nutation correction singleton. */
    private static class LazyMEMEWoutEOPHolder {

        /** Unique instance. */
        private static final Frame INSTANCE =
            new MEMEFrame(false, AbsoluteDate.J2000_EPOCH, "MEME without EOP");

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyMEMEWoutEOPHolder() {
        }

    }

}
