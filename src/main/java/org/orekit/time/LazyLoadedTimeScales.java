package org.orekit.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.LazyLoadedEop;
import org.orekit.utils.IERSConventions;

/**
 * An implementation of {@link TimeScales} that loads auxiliary data, leap seconds and
 * UT1-UTC, when it is first accessed. The list of loaders may be modified before the
 * first data access.
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see TimeScalesFactory
 * @since 10.1
 */
public class LazyLoadedTimeScales implements TimeScales {

    /** Source of EOP data. */
    private final LazyLoadedEop lazyLoadedEop;

    /** International Atomic Time scale. */
    private TAIScale tai = null;

    /** Universal Time Coordinate scale. */
    private UTCScale utc = null;

    /** Universal Time 1 scale (tidal effects ignored). */
    private Map<IERSConventions, UT1Scale> ut1MapSimpleEOP = new HashMap<>();

    /** Universal Time 1 scale (tidal effects considered). */
    private Map<IERSConventions, UT1Scale> ut1MapCompleteEOP = new HashMap<>();

    /** Terrestrial Time scale. */
    private TTScale tt = null;

    /** Galileo System Time scale. */
    private GalileoScale gst = null;

    /** GLObal NAvigation Satellite System scale. */
    private GLONASSScale glonass = null;

    /** Quasi-Zenith Satellite System scale. */
    private QZSSScale qzss = null;

    /** Global Positioning System scale. */
    private GPSScale gps = null;

    /** Geocentric Coordinate Time scale. */
    private TCGScale tcg = null;

    /** Barycentric Dynamic Time scale. */
    private TDBScale tdb = null;

    /** Barycentric Coordinate Time scale. */
    private TCBScale tcb = null;

    /** Greenwich Mean Sidereal Time scale. */
    private GMSTScale gmst = null;

    /** UTCTAI offsets loaders. */
    private List<UTCTAIOffsetsLoader> loaders = new ArrayList<>();

    /** IRNSS System Time scale. */
    private IRNSSScale irnss = null;

    /** BDS System Time scale. */
    private BDTScale bds = null;

    /**
     * Create a new set of time scales with the given sources of auxiliary data.
     *
     * @param lazyLoadedEop loads Earth Orientation Parameters for {@link
     *                      #getUT1(IERSConventions, boolean)}.
     */
    public LazyLoadedTimeScales(final LazyLoadedEop lazyLoadedEop) {
        this.lazyLoadedEop = lazyLoadedEop;
    }

    /**
     * Add a loader for UTC-TAI offsets history files.
     *
     * @param loader custom loader to add
     * @see TAIUTCDatFilesLoader
     * @see UTCTAIHistoryFilesLoader
     * @see UTCTAIBulletinAFilesLoader
     * @see #getUTC()
     * @see #clearUTCTAIOffsetsLoaders()
     * @since 7.1
     */
    public void addUTCTAIOffsetsLoader(final UTCTAIOffsetsLoader loader) {
        loaders.add(loader);
    }

    /**
     * Add the default loaders for UTC-TAI offsets history files (both IERS and USNO).
     * <p>
     * The default loaders are {@link TAIUTCDatFilesLoader} that looks for a file named
     * {@code tai-utc.dat} that must be in USNO format and {@link
     * UTCTAIHistoryFilesLoader} that looks fir a file named {@code UTC-TAI.history} that
     * must be in the IERS format. The {@link UTCTAIBulletinAFilesLoader} is
     * <em>not</em> added by default as it is not recommended. USNO warned us that
     * the TAI-UTC data present in bulletin A was for convenience only and was not
     * reliable, there have been errors in several bulletins regarding these data.
     * </p>
     *
     * @see <a href="http://maia.usno.navy.mil/ser7/tai-utc.dat">USNO tai-utc.dat
     * file</a>
     * @see <a href="http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history">IERS
     * UTC-TAI.history file</a>
     * @see TAIUTCDatFilesLoader
     * @see UTCTAIHistoryFilesLoader
     * @see #getUTC()
     * @see #clearUTCTAIOffsetsLoaders()
     * @since 7.1
     */
    public void addDefaultUTCTAIOffsetsLoaders() {
        addUTCTAIOffsetsLoader(new TAIUTCDatFilesLoader(TAIUTCDatFilesLoader.DEFAULT_SUPPORTED_NAMES));
        addUTCTAIOffsetsLoader(new UTCTAIHistoryFilesLoader());
    }

    /**
     * Clear loaders for UTC-TAI offsets history files.
     *
     * @see #getUTC()
     * @see #addUTCTAIOffsetsLoader(UTCTAIOffsetsLoader)
     * @see #addDefaultUTCTAIOffsetsLoaders()
     * @since 7.1
     */
    public void clearUTCTAIOffsetsLoaders() {
        loaders.clear();
    }

    @Override
    public TAIScale getTAI() {
        synchronized (this) {

            if (tai == null) {
                tai = new TAIScale();
            }

            return tai;

        }
    }

    @Override
    public UTCScale getUTC() {
        synchronized (this) {

            if (utc == null) {
                List<OffsetModel> entries = Collections.emptyList();
                if (loaders.isEmpty()) {
                    addDefaultUTCTAIOffsetsLoaders();
                }
                for (UTCTAIOffsetsLoader loader : loaders) {
                    entries = loader.loadOffsets();
                    if (!entries.isEmpty()) {
                        break;
                    }
                }
                if (entries.isEmpty()) {
                    throw new OrekitException(OrekitMessages.NO_IERS_UTC_TAI_HISTORY_DATA_LOADED);
                }
                utc = new UTCScale(entries);
            }

            return utc;
        }
    }

    @Override
    public UT1Scale getUT1(final IERSConventions conventions, final boolean simpleEOP) {
        synchronized (this) {

            final Map<IERSConventions, UT1Scale> map =
                    simpleEOP ? ut1MapSimpleEOP : ut1MapCompleteEOP;
            UT1Scale ut1 = map.get(conventions);
            if (ut1 == null) {
                ut1 = getUT1(lazyLoadedEop.getEOPHistory(conventions, simpleEOP));
                map.put(conventions, ut1);
            }
            return ut1;
        }
    }

    @Override
    public UT1Scale getUT1(final EOPHistory history) {
        return new UT1Scale(history, getUTC());
    }

    @Override
    public TTScale getTT() {
        synchronized (this) {

            if (tt == null) {
                tt = new TTScale();
            }

            return tt;

        }
    }

    @Override
    public GalileoScale getGST() {
        synchronized (this) {

            if (gst == null) {
                gst = new GalileoScale();
            }

            return gst;

        }
    }

    @Override
    public GLONASSScale getGLONASS() {
        synchronized (this) {

            if (glonass == null) {
                glonass = new GLONASSScale(getUTC());
            }

            return glonass;

        }
    }

    @Override
    public QZSSScale getQZSS() {
        synchronized (this) {

            if (qzss == null) {
                qzss = new QZSSScale();
            }

            return qzss;

        }
    }

    @Override
    public GPSScale getGPS() {
        synchronized (this) {

            if (gps == null) {
                gps = new GPSScale();
            }

            return gps;

        }
    }

    @Override
    public TCGScale getTCG() {
        synchronized (this) {

            if (tcg == null) {
                tcg = new TCGScale();
            }

            return tcg;

        }
    }

    @Override
    public TDBScale getTDB() {
        synchronized (this) {

            if (tdb == null) {
                tdb = new TDBScale();
            }

            return tdb;

        }
    }

    @Override
    public TCBScale getTCB() {
        synchronized (this) {

            if (tcb == null) {
                tcb = new TCBScale(getTDB());
            }

            return tcb;

        }
    }

    @Override
    public GMSTScale getGMST(final IERSConventions conventions, final boolean simpleEOP) {
        synchronized (this) {

            if (gmst == null) {
                gmst = new GMSTScale(getUT1(conventions, simpleEOP));
            }

            return gmst;

        }
    }

    @Override
    public IRNSSScale getIRNSS() {
        synchronized (this) {

            if (irnss == null) {
                irnss = new IRNSSScale();
            }

            return irnss;

        }
    }

    @Override
    public BDTScale getBDT() {
        synchronized (this) {

            if (bds == null) {
                bds = new BDTScale();
            }

            return bds;

        }
    }

}
