package org.orekit.frames;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.orekit.bodies.CelestialBodies;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;

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
public class LazyLoadedFrames implements Frames {

    /** Delegate for all EOP loading. */
    private final LazyLoadedEop lazyLoadedEop;
    /** Provider of common time scales. */
    private final TimeScales timeScales;
    /** Provider of the common celestial bodies. */
    private final CelestialBodies celestialBodies;
    /** Predefined frames. */
    private transient Map<Predefined, FactoryManagedFrame> frames;
    /** Predefined versioned ITRF frames. */
    private transient Map<ITRFKey, VersionedITRF> versionedItrfFrames;

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
        this.lazyLoadedEop = lazyLoadedEop;
        this.timeScales = timeScales;
        this.celestialBodies = celestialBodies;
        this.frames = new HashMap<>();
        this.versionedItrfFrames = new HashMap<>();
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
    public void addDefaultEOP1980HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames,
                                                       final String bulletinASupportedNames) {
        lazyLoadedEop.addDefaultEOP1980HistoryLoaders(
                rapidDataColumnsSupportedNames,
                rapidDataXMLSupportedNames,
                eopC04SupportedNames,
                bulletinBSupportedNames,
                bulletinASupportedNames,
                timeScales.getUTC());
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
    public void addDefaultEOP2000HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames,
                                                       final String bulletinASupportedNames) {
        lazyLoadedEop.addDefaultEOP2000HistoryLoaders(
                rapidDataColumnsSupportedNames,
                rapidDataXMLSupportedNames,
                eopC04SupportedNames,
                bulletinBSupportedNames,
                bulletinASupportedNames,
                timeScales.getUTC());
    }

    /** Add a loader for Earth Orientation Parameters history.
     * @param conventions IERS conventions to which EOP history applies
     * @param loader custom loader to add for the EOP history
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String)
     * @see #clearEOPHistoryLoaders()
     */
    public void addEOPHistoryLoader(final IERSConventions conventions, final EOPHistoryLoader loader) {
        lazyLoadedEop.addEOPHistoryLoader(conventions, loader);
    }

    /** Clear loaders for Earth Orientation Parameters history.
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String)
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
     * If no {@link EOPHistoryLoader} has been added by calling {@link
     * #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader) addEOPHistoryLoader}
     * or if {@link #clearEOPHistoryLoaders() clearEOPHistoryLoaders} has been
     * called afterwards, the {@link #addDefaultEOP1980HistoryLoaders(String, String,
     * String, String, String)} and {@link #addDefaultEOP2000HistoryLoaders(String,
     * String, String, String, String)} methods will be called automatically with
     * supported file names parameters all set to null, in order to get the default
     * loaders configuration.
     * </p>
     */
    @Override
    public EOPHistory getEOPHistory(final IERSConventions conventions, final boolean simpleEOP) {

        return lazyLoadedEop.getEOPHistory(conventions, simpleEOP, timeScales);
    }

    @Override
    public Frame getFrame(final Predefined factoryKey) {
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

    @Override
    public Frame getGCRF() {
        return Frame.getRoot();
    }

    @Override
    public Frame getICRF() {
        return celestialBodies.getSolarSystemBarycenter().getInertiallyOrientedFrame();
    }

    @Override
    public Frame getEcliptic(final IERSConventions conventions) {
        synchronized (this) {

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
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(parent, new EclipticProvider(conventions),
                        true, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getEME2000() {
        synchronized (this) {

            // try to find an already built frame
            FactoryManagedFrame frame = frames.get(Predefined.EME2000);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(getGCRF(), new EME2000Provider(), true, Predefined.EME2000);
                frames.put(Predefined.EME2000, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getITRF(final IERSConventions conventions,
                                       final boolean simpleEOP) {
        synchronized (this) {

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
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tirfFrame = getTIRF(conventions, simpleEOP);
                final TIRFProvider tirfProvider = (TIRFProvider) tirfFrame.getTransformProvider();
                frame = new FactoryManagedFrame(tirfFrame,
                        new ITRFProvider(tirfProvider.getEOPHistory()),
                        false, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getTIRF(final IERSConventions conventions) {
        return getTIRF(conventions, true);
    }

    @Override
    public VersionedITRF getITRF(final ITRFVersion version,
                                 final IERSConventions conventions,
                                 final boolean simpleEOP) {
        synchronized (this) {
            // try to find an already built frame
            final ITRFKey key = new ITRFKey(version, conventions, simpleEOP);
            VersionedITRF frame = versionedItrfFrames.get(key);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final FactoryManagedFrame rawITRF = getITRF(conventions, simpleEOP);
                frame = new VersionedITRF(rawITRF.getParent(), version,
                        (ITRFProvider) rawITRF.getTransformProvider(),
                        version.toString().replace('_', '-') +
                                "/" +
                                rawITRF.getName());
                versionedItrfFrames.put(key, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getTIRF(final IERSConventions conventions,
                                       final boolean simpleEOP) {
        synchronized (this) {

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
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame cirf = getCIRF(conventions, simpleEOP);
                final ShiftingTransformProvider cirfInterpolating =
                        (ShiftingTransformProvider) cirf.getTransformProvider();
                final CIRFProvider cirfRaw = (CIRFProvider) cirfInterpolating.getRawProvider();
                final EOPHistory eopHistory = cirfRaw.getEOPHistory();
                final TIRFProvider provider =
                        new TIRFProvider(eopHistory, timeScales.getUT1(eopHistory));
                frame = new FactoryManagedFrame(cirf, provider, false, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getCIRF(final IERSConventions conventions,
                                       final boolean simpleEOP) {
        synchronized (this) {

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
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final EOPHistory eopHistory =
                        lazyLoadedEop.getEOPHistory(conventions, simpleEOP, timeScales);
                final TransformProvider shifting =
                        new ShiftingTransformProvider(new CIRFProvider(eopHistory),
                                CartesianDerivativesFilter.USE_PVA,
                                AngularDerivativesFilter.USE_R,
                                6, Constants.JULIAN_DAY / 24,
                                OrekitConfiguration.getCacheSlotsNumber(),
                                Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(getGCRF(), shifting, true, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getVeis1950() {
        synchronized (this) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.VEIS_1950;
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(getGTOD(IERSConventions.IERS_1996, false, true),
                        new VEISProvider(timeScales), true, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getITRFEquinox(final IERSConventions conventions,
                                              final boolean simpleEOP) {
        synchronized (this) {

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
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame gtod = getGTOD(conventions, true, simpleEOP);
                final ShiftingTransformProvider gtodShifting =
                        (ShiftingTransformProvider) gtod.getTransformProvider();
                final GTODProvider gtodRaw    = (GTODProvider) gtodShifting.getRawProvider();
                final EOPHistory   eopHistory = gtodRaw.getEOPHistory();
                frame = new FactoryManagedFrame(gtod, new ITRFProvider(eopHistory), false, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getGTOD(final boolean applyEOPCorr) {
        return getGTOD(IERSConventions.IERS_1996, applyEOPCorr, true);
    }

    @Override
    public FactoryManagedFrame getGTOD(final IERSConventions conventions,
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
    private FactoryManagedFrame getGTOD(final IERSConventions conventions,
                                               final boolean applyEOPCorr,
                                               final boolean simpleEOP) {

        synchronized (this) {

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
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tod = getTOD(conventions, applyEOPCorr, simpleEOP);
                final ShiftingTransformProvider todInterpolating =
                        (ShiftingTransformProvider) tod.getTransformProvider();
                final TODProvider       todRaw     = (TODProvider) todInterpolating.getRawProvider();
                final EOPHistory        eopHistory = todRaw.getEOPHistory();
                final GTODProvider      gtodRaw    =
                        new GTODProvider(conventions, eopHistory, timeScales);
                final TransformProvider gtodShifting =
                        new ShiftingTransformProvider(gtodRaw,
                                CartesianDerivativesFilter.USE_PVA,
                                AngularDerivativesFilter.USE_R,
                                todInterpolating.getGridPoints(), todInterpolating.getStep(),
                                OrekitConfiguration.getCacheSlotsNumber(),
                                Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(tod, gtodShifting, false, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getTOD(final boolean applyEOPCorr) {
        return getTOD(IERSConventions.IERS_1996, applyEOPCorr, false);
    }

    @Override
    public FactoryManagedFrame getTOD(final IERSConventions conventions,
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
    private FactoryManagedFrame getTOD(final IERSConventions conventions,
                                              final boolean applyEOPCorr,
                                              final boolean simpleEOP) {

        synchronized (this) {

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
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final EOPHistory eopHistory = applyEOPCorr ?
                        lazyLoadedEop.getEOPHistory(conventions, simpleEOP, timeScales) :
                        null;
                final TransformProvider shifting =
                        new ShiftingTransformProvider(
                                new TODProvider(conventions, eopHistory, timeScales.getTAI()),
                                CartesianDerivativesFilter.USE_PVA,
                                AngularDerivativesFilter.USE_R,
                                interpolationPoints, Constants.JULIAN_DAY / pointsPerDay,
                                OrekitConfiguration.getCacheSlotsNumber(),
                                Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
                frame = new FactoryManagedFrame(getMOD(conventions, applyEOPCorr), shifting, true, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getMOD(final boolean applyEOPCorr) {
        return getMOD(IERSConventions.IERS_1996, applyEOPCorr);
    }

    @Override
    public FactoryManagedFrame getMOD(final IERSConventions conventions) {
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
    private FactoryManagedFrame getMOD(final IERSConventions conventions, final boolean applyEOPCorr) {

        synchronized (this) {

            final Predefined factoryKey;
            final Frame parent;
            switch (conventions) {
                case IERS_1996 :
                    factoryKey = applyEOPCorr ? Predefined.MOD_CONVENTIONS_1996 : Predefined.MOD_WITHOUT_EOP_CORRECTIONS;
                    parent     = applyEOPCorr ? getGCRF() : getEME2000();
                    break;
                case IERS_2003 :
                    factoryKey = Predefined.MOD_CONVENTIONS_2003;
                    // in IERS conventions 2003, the precession angles zetaA, thetaA and zA
                    // from equation 33 are computed from EME2000, not from GCRF
                    parent     = getEME2000();
                    break;
                case IERS_2010 :
                    factoryKey = Predefined.MOD_CONVENTIONS_2010;
                    // precession angles epsilon0, psiA, omegaA and chiA
                    // from equations 5.39 and 5.40 are computed from EME2000
                    parent     = getEME2000();
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }

            // try to find an already built frame
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                frame = new FactoryManagedFrame(parent, new MODProvider(conventions), true, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getTEME() {
        synchronized (this) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.TEME;
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame tod = getTOD(IERSConventions.IERS_1996, false, true);
                final ShiftingTransformProvider todShifting =
                        (ShiftingTransformProvider) tod.getTransformProvider();
                final TEMEProvider temeRaw =
                        new TEMEProvider(IERSConventions.IERS_1996, null, timeScales.getTAI());
                final TransformProvider temeShifting =
                        new ShiftingTransformProvider(temeRaw,
                                CartesianDerivativesFilter.USE_PVA,
                                AngularDerivativesFilter.USE_R,
                                todShifting.getGridPoints(), todShifting.getStep(),
                                OrekitConfiguration.getCacheSlotsNumber(),
                                Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

                frame = new FactoryManagedFrame(tod, temeShifting, true, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
    }

    @Override
    public FactoryManagedFrame getPZ9011(final IERSConventions convention,
                                         final boolean simpleEOP) {
        synchronized (this) {

            // try to find an already built frame
            final Predefined factoryKey = Predefined.PZ90_11;
            FactoryManagedFrame frame = frames.get(factoryKey);

            if (frame == null) {
                // it's the first time we need this frame, build it and store it
                final Frame itrf = getITRF(ITRFVersion.ITRF_2008, convention, simpleEOP);
                final HelmertTransformation pz90Raw = new HelmertTransformation(new AbsoluteDate(2010, 1, 1, 12, 0, 0, timeScales.getTT()),
                        +3.0, +1.0, -0.0, +0.019, -0.042, +0.002, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
                frame = new FactoryManagedFrame(itrf, pz90Raw, false, factoryKey);
                frames.put(factoryKey, frame);
            }

            return frame;

        }
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
