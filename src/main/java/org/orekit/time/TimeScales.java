package org.orekit.time;

import org.orekit.frames.EOPHistory;
import org.orekit.frames.Frames;
import org.orekit.utils.IERSConventions;

/**
 * A collection of {@link TimeScale}s. This interface defines methods for obtaining
 * instances of many common time scales.
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see TimeScalesFactory
 * @see TimeScale
 * @since 10.1
 */
public interface TimeScales {

    /**
     * Get the International Atomic Time scale.
     *
     * @return International Atomic Time scale
     */
    TAIScale getTAI();

    /**
     * Get the Universal Time Coordinate scale.
     *
     * @return Universal Time Coordinate scale
     */
    UTCScale getUTC();

    /**
     * Get the Universal Time 1 scale.
     *
     * @param conventions IERS conventions for which EOP parameters will provide dUT1
     * @param simpleEOP   if true, tidal effects are ignored when interpolating EOP
     * @return Universal Time 1 scale
     * @see #getUTC()
     * @see Frames#getEOPHistory(IERSConventions, boolean)
     */
    UT1Scale getUT1(IERSConventions conventions, boolean simpleEOP);

    /**
     * Get the Universal Time 1 scale.
     * <p>
     * As this method allow associating any history with the time scale, it may involve
     * large data sets. So this method does <em>not</em> cache the resulting {@link
     * UT1Scale UT1Scale} instance, a new instance will be returned each time. In order to
     * avoid wasting memory, calling {@link #getUT1(IERSConventions, boolean)} with the
     * single enumerate corresponding to the conventions may be a better solution. This
     * method is made available only for expert use.
     * </p>
     *
     * @param history EOP parameters providing dUT1 (may be null if no correction is
     *                desired)
     * @return Universal Time 1 scale
     * @see #getUT1(IERSConventions, boolean)
     */
    UT1Scale getUT1(EOPHistory history);

    /**
     * Get the Terrestrial Time scale.
     *
     * @return Terrestrial Time scale
     */
    TTScale getTT();

    /**
     * Get the Galileo System Time scale.
     *
     * @return Galileo System Time scale
     */
    GalileoScale getGST();

    /**
     * Get the GLObal NAvigation Satellite System time scale.
     *
     * @return GLObal NAvigation Satellite System time scale
     */
    GLONASSScale getGLONASS();

    /**
     * Get the Quasi-Zenith Satellite System time scale.
     *
     * @return Quasi-Zenith Satellite System time scale
     */
    QZSSScale getQZSS();

    /**
     * Get the Global Positioning System scale.
     *
     * @return Global Positioning System scale
     */
    GPSScale getGPS();

    /**
     * Get the Geocentric Coordinate Time scale.
     *
     * @return Geocentric Coordinate Time scale
     */
    TCGScale getTCG();

    /**
     * Get the Barycentric Dynamic Time scale.
     *
     * @return Barycentric Dynamic Time scale
     */
    TDBScale getTDB();

    /**
     * Get the Barycentric Coordinate Time scale.
     *
     * @return Barycentric Coordinate Time scale
     */
    TCBScale getTCB();

    /**
     * Get the Greenwich Mean Sidereal Time scale.
     *
     * @param conventions IERS conventions for which EOP parameters will provide dUT1
     * @param simpleEOP   if true, tidal effects are ignored when interpolating EOP
     * @return Greenwich Mean Sidereal Time scale
     * @since 7.0
     */
    GMSTScale getGMST(IERSConventions conventions, boolean simpleEOP);

    /**
     * Get the Indian Regional Navigation Satellite System time scale.
     *
     * @return Indian Regional Navigation Satellite System time scale
     */
    IRNSSScale getIRNSS();

    /**
     * Get the BeiDou Navigation Satellite System time scale.
     *
     * @return BeiDou Navigation Satellite System time scale
     */
    BDTScale getBDT();
}
