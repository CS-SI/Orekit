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
package org.orekit.frames;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.orekit.bodies.CelestialBodies;
import org.orekit.time.TimeScales;
import org.orekit.time.UT1Scale;
import org.orekit.utils.IERSConventions;

/**
 * A collection of commonly used {@link Frame}s. This interface defines methods for
 * obtaining instances of many commonly used reference frames.
 *
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @author Pascal Parraud
 * @author Evan Ward
 * @see FramesFactory
 * @since 10.0
 */
public interface Frames {

    /** Get Earth Orientation Parameters history.
     * @param conventions conventions for which EOP history is requested
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return Earth Orientation Parameters history
     */
    EOPHistory getEOPHistory(IERSConventions conventions, boolean simpleEOP);

    /** Get one of the predefined frames.
     * @param factoryKey key of the frame within the factory
     * @return the predefined frame
     */
    Frame getFrame(Predefined factoryKey);

    /** Get the unique GCRF frame.
     * <p>The GCRF frame is the root frame in the frame tree.</p>
     * @return the unique instance of the GCRF frame
     */
    Frame getGCRF();

    /** Get the unique ICRF frame.
     * <p>The ICRF frame is centered at solar system barycenter and aligned
     * with GCRF.</p>
     * @return the unique instance of the ICRF frame
     */
    Frame getICRF();

    /** Get the ecliptic frame.
     * The IAU defines the ecliptic as "the plane perpendicular to the mean heliocentric
     * orbital angular momentum vector of the Earth-Moon barycentre in the BCRS (IAU 2006
     * Resolution B1)." The +z axis is aligned with the angular momentum vector, and the +x
     * axis is aligned with +x axis of {@link #getMOD(IERSConventions) MOD}.
     *
     * <p> This implementation agrees with the JPL 406 ephemerides to within 0.5 arc seconds.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     */
    Frame getEcliptic(IERSConventions conventions);

    /** Get the unique EME2000 frame.
     * <p>The EME2000 frame is also called the J2000 frame.
     * The former denomination is preferred in Orekit.</p>
     * @return the unique instance of the EME2000 frame
     */
    FactoryManagedFrame getEME2000();

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
    FactoryManagedFrame getITRF(IERSConventions conventions,
                                boolean simpleEOP);

    /** Get the TIRF reference frame, ignoring tidal effects.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     * library cannot be read.
     */
    FactoryManagedFrame getTIRF(IERSConventions conventions);

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
    VersionedITRF getITRF(ITRFVersion version,
                          IERSConventions conventions,
                          boolean simpleEOP);

    /** Build an uncached International Terrestrial Reference Frame with specific {@link EOPHistory EOP history}.
     * <p>
     * This frame and its parent frames (TIRF and CIRF) will <em>not</em> be cached, they are
     * rebuilt from scratch each time this method is called. This factory method is intended
     * to be used when EOP history is changed at run time. For regular ITRF use, the
     * {@link #getITRF(IERSConventions, boolean)} and {link {@link #getITRF(ITRFVersion, IERSConventions, boolean)}
     * are more suitable.
     * </p>
     * @param ut1 UT1 time scale (contains the {@link EOPHistory EOP history})
     * @return an ITRF frame with specified time scale and embedded EOP history
     * @since 12.0
     */
    Frame buildUncachedITRF(UT1Scale ut1);

    /** Get the TIRF reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     * @since 6.1
     */
    FactoryManagedFrame getTIRF(IERSConventions conventions,
                                boolean simpleEOP);

    /** Get the CIRF2000 reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    FactoryManagedFrame getCIRF(IERSConventions conventions,
                                boolean simpleEOP);

    /** Get the VEIS 1950 reference frame.
     * <p>Its parent frame is the GTOD frame with IERS 1996 conventions without EOP corrections.</p>
     * @return the selected reference frame singleton.
     */
    FactoryManagedFrame getVeis1950();

    /** Get the equinox-based ITRF reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     * @since 6.1
     */
    FactoryManagedFrame getITRFEquinox(IERSConventions conventions,
                                       boolean simpleEOP);

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
    FactoryManagedFrame getGTOD(boolean applyEOPCorr);

    /** Get the GTOD reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    FactoryManagedFrame getGTOD(IERSConventions conventions,
                                boolean simpleEOP);

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
    FactoryManagedFrame getTOD(boolean applyEOPCorr);

    /** Get the TOD reference frame.
     * @param conventions IERS conventions to apply
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return the selected reference frame singleton.
     */
    FactoryManagedFrame getTOD(IERSConventions conventions,
                               boolean simpleEOP);

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
    FactoryManagedFrame getMOD(boolean applyEOPCorr);

    /** Get the MOD reference frame.
     * @param conventions IERS conventions to apply
     * @return the selected reference frame singleton.
     */
    FactoryManagedFrame getMOD(IERSConventions conventions);

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
    FactoryManagedFrame getTEME();

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
    FactoryManagedFrame getPZ9011(IERSConventions convention,
                                  boolean simpleEOP);

    /* Helpers for creating instances */

    /**
     * Create a set of frames from the given data.
     *
     * @param timeScales      used to build the frames as well as the EOP data set.
     * @param celestialBodies used to get {@link #getICRF()} which is the inertial frame
     *                        of the solar system barycenter.
     * @return a set of reference frame constructed from the given data.
     * @see #of(TimeScales, Supplier)
     */
    static Frames of(final TimeScales timeScales,
                     final CelestialBodies celestialBodies) {
        return of(timeScales, () -> celestialBodies.getSolarSystemBarycenter()
                .getInertiallyOrientedFrame());
    }

    /**
     * Create a set of frames from the given data.
     *
     * @param timeScales   used to build the frames as well as the EOP data set.
     * @param icrfSupplier used to get {@link #getICRF()}. For example, {@code
     *                     celestialBodies.getSolarSystemBarycenter().getInertiallyOrientedFrame()}
     * @return a set of reference frame constructed from the given data.
     * @see CelestialBodies
     * @see TimeScales#of(Collection, BiFunction)
     */
    static Frames of(final TimeScales timeScales,
                     final Supplier<Frame> icrfSupplier) {
        return new AbstractFrames(timeScales, icrfSupplier) {
            @Override
            public EOPHistory getEOPHistory(final IERSConventions conventions,
                                            final boolean simpleEOP) {
                return getTimeScales().getUT1(conventions, simpleEOP).getEOPHistory();
            }
        };
    }

}
