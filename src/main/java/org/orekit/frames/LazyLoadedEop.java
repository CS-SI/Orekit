package org.orekit.frames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * Loads Earth Orientation Parameters (EOP)from a configured set of {@link
 * EOPHistoryLoader}s on demand. Methods are synchronized to it is safe for access from
 * multiple threads.
 *
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @author Pascal Parraud
 * @author Evan Ward
 * @see LazyLoadedFrames
 * @see FramesFactory
 * @since 10.1
 */
public class LazyLoadedEop {

    /** Provides access to the EOP data files. */
    private final DataProvidersManager dataProvidersManager;
    /** Loaders for Earth Orientation parameters. */
    private final Map<IERSConventions, List<EOPHistoryLoader>> eopHistoryLoaders;
    /** Threshold for EOP continuity. */
    private double eopContinuityThreshold;

    /**
     * Create a new instance for loading EOP data from multiple {@link
     * EOPHistoryLoader}s.
     *
     * @param dataProvidersManager provides access to the needed EOP data files.
     */
    public LazyLoadedEop(final DataProvidersManager dataProvidersManager) {
        this.dataProvidersManager = dataProvidersManager;
        this.eopHistoryLoaders = new HashMap<>();
        this.eopContinuityThreshold = 5 * Constants.JULIAN_DAY;
    }

    /**
     * Get the data providers manager for this instance.
     *
     * @return the provider of EOP data files.
     */
    public DataProvidersManager getDataProvidersManager() {
        return dataProvidersManager;
    }

    /**
     * Add the default loaders EOP history (IAU 1980 precession/nutation).
     * <p>
     * The default loaders look for IERS EOP C04 and bulletins B files. They correspond to
     * {@link IERSConventions#IERS_1996 IERS 1996} conventions.
     * </p>
     *
     * @param rapidDataColumnsSupportedNames regular expression for supported rapid data
     *                                       columns EOP files names (may be null if the
     *                                       default IERS file names are used)
     * @param rapidDataXMLSupportedNames     regular expression for supported rapid data
     *                                       XML EOP files names (may be null if the
     *                                       default IERS file names are used)
     * @param eopC04SupportedNames           regular expression for supported EOP C04
     *                                       files names (may be null if the default IERS
     *                                       file names are used)
     * @param bulletinBSupportedNames        regular expression for supported bulletin B
     *                                       files names (may be null if the default IERS
     *                                       file names are used)
     * @param bulletinASupportedNames        regular expression for supported bulletin A
     *                                       files names (may be null if the default IERS
     *                                       file names are used)
     * @param utc                            UTC time scale.
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">IERS EOP C04 files</a>
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP2000HistoryLoaders(String, String, String, String, String, TimeScale)
     */
    public void addDefaultEOP1980HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                final String rapidDataXMLSupportedNames,
                                                final String eopC04SupportedNames,
                                                final String bulletinBSupportedNames,
                                                final String bulletinASupportedNames,
                                                final TimeScale utc) {
        final String rapidColNames =
                (rapidDataColumnsSupportedNames == null) ?
                        FramesFactory.RAPID_DATA_PREDICTION_COLUMNS_1980_FILENAME :
                        rapidDataColumnsSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                new RapidDataAndPredictionColumnsLoader(false, rapidColNames,
                        dataProvidersManager, utc));
        final String rapidXmlNames =
                (rapidDataXMLSupportedNames == null) ?
                        FramesFactory.RAPID_DATA_PREDICTION_XML_1980_FILENAME :
                        rapidDataXMLSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                new RapidDataAndPredictionXMLLoader(rapidXmlNames, dataProvidersManager,
                        utc));
        final String eopcNames =
                (eopC04SupportedNames == null) ?
                        FramesFactory.EOPC04_1980_FILENAME : eopC04SupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                new EOPC04FilesLoader(eopcNames, dataProvidersManager, utc));
        final String bulBNames =
                (bulletinBSupportedNames == null) ?
                        FramesFactory.BULLETINB_1980_FILENAME : bulletinBSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                new BulletinBFilesLoader(bulBNames, dataProvidersManager, utc));
        final String bulANames =
                (bulletinASupportedNames == null) ?
                        FramesFactory.BULLETINA_FILENAME : bulletinASupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_1996,
                new BulletinAFilesLoader(bulANames, dataProvidersManager, utc));
    }

    /**
     * Add the default loaders for EOP history (IAU 2000/2006 precession/nutation).
     * <p>
     * The default loaders look for IERS EOP C04 and bulletins B files. They correspond to
     * both {@link IERSConventions#IERS_2003 IERS 2003} and {@link
     * IERSConventions#IERS_2010 IERS 2010} conventions.
     * </p>
     *
     * @param rapidDataColumnsSupportedNames regular expression for supported rapid data
     *                                       columns EOP files names (may be null if the
     *                                       default IERS file names are used)
     * @param rapidDataXMLSupportedNames     regular expression for supported rapid data
     *                                       XML EOP files names (may be null if the
     *                                       default IERS file names are used)
     * @param eopC04SupportedNames           regular expression for supported EOP C04
     *                                       files names (may be null if the default IERS
     *                                       file names are used)
     * @param bulletinBSupportedNames        regular expression for supported bulletin B
     *                                       files names (may be null if the default IERS
     *                                       file names are used)
     * @param bulletinASupportedNames        regular expression for supported bulletin A
     *                                       files names (may be null if the default IERS
     *                                       file names are used)
     * @param utc                            UTC time scale.
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04/">IERS EOP C04 files</a>
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #clearEOPHistoryLoaders()
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String,
     * TimeScale)
     */
    public void addDefaultEOP2000HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                final String rapidDataXMLSupportedNames,
                                                final String eopC04SupportedNames,
                                                final String bulletinBSupportedNames,
                                                final String bulletinASupportedNames,
                                                final TimeScale utc) {
        final String rapidColNames =
                (rapidDataColumnsSupportedNames == null) ?
                        FramesFactory.RAPID_DATA_PREDICITON_COLUMNS_2000_FILENAME :
                        rapidDataColumnsSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                new RapidDataAndPredictionColumnsLoader(
                        true, rapidColNames, dataProvidersManager, utc));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                new RapidDataAndPredictionColumnsLoader(
                        true, rapidColNames, dataProvidersManager, utc));
        final String rapidXmlNames =
                (rapidDataXMLSupportedNames == null) ?
                        FramesFactory.RAPID_DATA_PREDICITON_XML_2000_FILENAME :
                        rapidDataXMLSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                new RapidDataAndPredictionXMLLoader(
                        rapidXmlNames, dataProvidersManager, utc));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                new RapidDataAndPredictionXMLLoader(
                        rapidXmlNames, dataProvidersManager, utc));
        final String eopcNames =
                (eopC04SupportedNames == null) ?
                        FramesFactory.EOPC04_2000_FILENAME : eopC04SupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                new EOPC04FilesLoader(eopcNames, dataProvidersManager, utc));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                new EOPC04FilesLoader(eopcNames, dataProvidersManager, utc));
        final String bulBNames =
                (bulletinBSupportedNames == null) ?
                        FramesFactory.BULLETINB_2000_FILENAME : bulletinBSupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                new BulletinBFilesLoader(bulBNames, dataProvidersManager, utc));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                new BulletinBFilesLoader(bulBNames, dataProvidersManager, utc));
        final String bulANames =
                (bulletinASupportedNames == null) ?
                        FramesFactory.BULLETINA_FILENAME : bulletinASupportedNames;
        addEOPHistoryLoader(IERSConventions.IERS_2003,
                new BulletinAFilesLoader(bulANames, dataProvidersManager, utc));
        addEOPHistoryLoader(IERSConventions.IERS_2010,
                new BulletinAFilesLoader(bulANames, dataProvidersManager, utc));
    }

    /**
     * Add a loader for Earth Orientation Parameters history.
     *
     * @param conventions IERS conventions to which EOP history applies
     * @param loader      custom loader to add for the EOP history
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String, TimeScale)
     * @see #clearEOPHistoryLoaders()
     */
    public void addEOPHistoryLoader(final IERSConventions conventions, final EOPHistoryLoader loader) {
        synchronized (eopHistoryLoaders) {
            if (!eopHistoryLoaders.containsKey(conventions)) {
                eopHistoryLoaders.put(conventions, new ArrayList<>());
            }
            eopHistoryLoaders.get(conventions).add(loader);
        }
    }

    /**
     * Clear loaders for Earth Orientation Parameters history.
     *
     * @see #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader)
     * @see #addDefaultEOP1980HistoryLoaders(String, String, String, String, String, TimeScale)
     */
    public void clearEOPHistoryLoaders() {
        synchronized (eopHistoryLoaders) {
            eopHistoryLoaders.clear();
        }
    }

    /**
     * Set the threshold to check EOP continuity.
     * <p>
     * The default threshold (used if this method is never called) is 5 Julian days. If
     * after loading EOP entries some holes between entries exceed this threshold, an
     * exception will be triggered.
     * </p>
     * <p>
     * One case when calling this method is really useful is for applications that use a
     * single Bulletin A, as these bulletins have a roughly one month wide hole for the
     * first bulletin of each month, which contains older final data in addition to the
     * rapid data and the predicted data.
     * </p>
     *
     * @param threshold threshold to use for checking EOP continuity (in seconds)
     */
    public void setEOPContinuityThreshold(final double threshold) {
        eopContinuityThreshold = threshold;
    }

    /**
     * Get Earth Orientation Parameters history.
     * <p>
     * If no {@link EOPHistoryLoader} has been added by calling {@link
     * #addEOPHistoryLoader(IERSConventions, EOPHistoryLoader) addEOPHistoryLoader} or if
     * {@link #clearEOPHistoryLoaders() clearEOPHistoryLoaders} has been called
     * afterwards, the {@link #addDefaultEOP1980HistoryLoaders(String, String, String,
     * String, String, TimeScale)} and {@link #addDefaultEOP2000HistoryLoaders(String,
     * String, String, String, String, TimeScale)} methods will be called automatically
     * with supported file names parameters all set to null, in order to get the default
     * loaders configuration.
     * </p>
     *
     * @param conventions conventions for which EOP history is requested
     * @param simpleEOP   if true, tidal effects are ignored when interpolating EOP
     * @param timeScales  to use when loading EOP and computing corrections.
     * @return Earth Orientation Parameters history
     */
    public EOPHistory getEOPHistory(final IERSConventions conventions,
                                    final boolean simpleEOP,
                                    final TimeScales timeScales) {

        synchronized (eopHistoryLoaders) {

            if (eopHistoryLoaders.isEmpty()) {
                // set up using default loaders
                final TimeScale utc = timeScales.getUTC();
                addDefaultEOP2000HistoryLoaders(null, null, null, null, null, utc);
                addDefaultEOP1980HistoryLoaders(null, null, null, null, null, utc);
            }

            // TimeStamped based set needed to remove duplicates
            OrekitException pendingException = null;
            final SortedSet<EOPEntry> data = new TreeSet<>(new ChronologicalComparator());

            // try to load canonical data if available
            if (eopHistoryLoaders.containsKey(conventions)) {
                for (final EOPHistoryLoader loader : eopHistoryLoaders.get(conventions)) {
                    try {
                        loader.fillHistory(
                                conventions.getNutationCorrectionConverter(timeScales),
                                data);
                    } catch (OrekitException oe) {
                        pendingException = oe;
                    }
                }
            }

            if (data.isEmpty() && pendingException != null) {
                throw pendingException;
            }

            final EOPHistory history =
                    new EOPHistory(conventions, data, simpleEOP, timeScales);
            history.checkEOPContinuity(eopContinuityThreshold);
            return history;

        }

    }

}
