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

import org.orekit.bodies.CelestialBodies;
import org.orekit.time.TimeScales;
import org.orekit.utils.IERSConventions;

/**
 * This class lazily loads auxiliary data when it is needed by a requested frame. It is
 * designed to match the behavior of {@link FramesFactory} in Orekit 10.0.
 *
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @author Pascal Parraud
 * @author Evan Ward
 * @see LazyLoadedEop
 * @since 10.1
 */
public class LazyLoadedFrames extends AbstractFrames {

    /** Delegate for all EOP loading. */
    private final LazyLoadedEop lazyLoadedEop;

    /**
     * Create a collection of frames from the given auxiliary data.
     *
     * @param lazyLoadedEop   loads Earth Orientation Parameters.
     * @param timeScales      defines the time scales used when computing frame
     *                        transformations. For example, the TT time scale needed for
     *                        {@link #getPZ9011(IERSConventions, boolean)}.
     * @param celestialBodies defines the celestial bodies which, for example, are used in
     *                        {@link #getICRF()}.
     */
    public LazyLoadedFrames(final LazyLoadedEop lazyLoadedEop,
                            final TimeScales timeScales,
                            final CelestialBodies celestialBodies) {
        super(timeScales, () -> celestialBodies.getSolarSystemBarycenter()
                .getInertiallyOrientedFrame());
        this.lazyLoadedEop = lazyLoadedEop;
    }

    /** Add the default loaders EOP history (IAU 1980 precession/nutation).
     * <p>
     * The default loaders look for IERS EOP C04 and bulletins B files. They
     * correspond to {@link IERSConventions#IERS_1996 IERS 1996} conventions.
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
     * @see <a href="https://datacenter.iers.org/products/eop/">IERS https data download</a>
     * @see #addEOPHistoryLoader(IERSConventions, EopHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP2000HistoryLoaders(String, String, String, String, String, String)
     * @since 12.0
     */
    public void addDefaultEOP1980HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                final String xmlSupportedNames,
                                                final String eopC04SupportedNames,
                                                final String bulletinBSupportedNames,
                                                final String bulletinASupportedNames,
                                                final String csvSupportedNames) {
        lazyLoadedEop.addDefaultEOP1980HistoryLoaders(
            rapidDataColumnsSupportedNames,
            xmlSupportedNames,
            eopC04SupportedNames,
            bulletinBSupportedNames,
            bulletinASupportedNames,
            csvSupportedNames,
            () -> getTimeScales().getUTC());
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
     * @see <a href="https://datacenter.iers.org/products/eop/">IERS https data download</a>
     * @see #addEOPHistoryLoader(IERSConventions, EopHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String, String)
     * @since 12.0
     */
    public void addDefaultEOP2000HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                final String xmlSupportedNames,
                                                final String eopC04SupportedNames,
                                                final String bulletinBSupportedNames,
                                                final String bulletinASupportedNames,
                                                final String csvSupportedNames) {
        lazyLoadedEop.addDefaultEOP2000HistoryLoaders(
            rapidDataColumnsSupportedNames,
            xmlSupportedNames,
            eopC04SupportedNames,
            bulletinBSupportedNames,
            bulletinASupportedNames,
            csvSupportedNames,
            () -> getTimeScales().getUTC());
    }

    /** Add a loader for Earth Orientation Parameters history.
     * @param conventions IERS conventions to which EOP history applies
     * @param loader custom loader to add for the EOP history
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String, String)
     * @see #clearEOPHistoryLoaders()
     */
    public void addEOPHistoryLoader(final IERSConventions conventions, final EopHistoryLoader loader) {
        lazyLoadedEop.addEOPHistoryLoader(conventions, loader);
    }

    /** Clear loaders for Earth Orientation Parameters history.
     * @see #addEOPHistoryLoader(IERSConventions, EopHistoryLoader)
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String, String)
     */
    public void clearEOPHistoryLoaders() {
        lazyLoadedEop.clearEOPHistoryLoaders();
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
    public void setEOPContinuityThreshold(final double threshold) {
        lazyLoadedEop.setEOPContinuityThreshold(threshold);
    }

    /** {@inheritDoc}
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
     */
    @Override
    public EOPHistory getEOPHistory(final IERSConventions conventions, final boolean simpleEOP) {
        return lazyLoadedEop.getEOPHistory(conventions, simpleEOP, getTimeScales());
    }

}
