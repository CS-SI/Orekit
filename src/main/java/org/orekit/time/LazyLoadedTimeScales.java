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
package org.orekit.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.orekit.data.DataProvidersManager;
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
public class LazyLoadedTimeScales extends AbstractTimeScales {

    /** Source of EOP data. */
    private final LazyLoadedEop lazyLoadedEop;

    /** UTCTAI offsets loaders. */
    private final List<UTCTAIOffsetsLoader> loaders = new ArrayList<>();

    /** Universal Time Coordinate scale. */
    private AtomicReference<UTCScale> utc = new AtomicReference<>();

    /** International Atomic Time scale. */
    private AtomicReference<TAIScale> tai = new AtomicReference<>();

    /** Terrestrial Time scale. */
    private AtomicReference<TTScale> tt = new AtomicReference<>();

    /** Galileo System Time scale. */
    private AtomicReference<GalileoScale> gst = new AtomicReference<>();

    /** GLObal NAvigation Satellite System scale. */
    private AtomicReference<GLONASSScale> glonass = new AtomicReference<>();

    /** Quasi-Zenith Satellite System scale. */
    private AtomicReference<QZSSScale> qzss = new AtomicReference<>();

    /** Global Positioning System scale. */
    private AtomicReference<GPSScale> gps = new AtomicReference<>();

    /** Geocentric Coordinate Time scale. */
    private AtomicReference<TCGScale> tcg = new AtomicReference<>();

    /** Barycentric Dynamic Time scale. */
    private AtomicReference<TDBScale> tdb = new AtomicReference<>();

    /** Barycentric Coordinate Time scale. */
    private AtomicReference<TCBScale> tcb = new AtomicReference<>();

    /** IRNSS System Time scale. */
    private AtomicReference<IRNSSScale> irnss = new AtomicReference<>();

    /** BDS System Time scale. */
    private AtomicReference<BDTScale> bds = new AtomicReference<>();

    /**
     * Create a new set of time scales with the given sources of auxiliary data. This
     * constructor uses the same {@link DataProvidersManager} for the default EOP loaders
     * and the default leap second loaders.
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
        synchronized (this) {
            loaders.add(loader);
        }
    }

    /**
     * Add the default loaders for UTC-TAI offsets history files (both IERS and USNO).
     * <p>
     * The default loaders are {@link TAIUTCDatFilesLoader} that looks for a file named
     * {@code tai-utc.dat} that must be in USNO format, {@link
     * UTCTAIHistoryFilesLoader} that looks for a file named {@code UTC-TAI.history} that
     * must be in the IERS format and {@link AGILeapSecondFilesLoader} that looks for a
     * files named {@code LeapSecond.dat} that must be in AGI format. The {@link
     * UTCTAIBulletinAFilesLoader} is<em>not</em> added by default as it is not recommended.
     * USNO warned us that the TAI-UTC data present in bulletin A was for convenience only
     * and was not reliable, there have been errors in several bulletins regarding these data.
     * </p>
     *
     * @see <a href="http://maia.usno.navy.mil/ser7/tai-utc.dat">USNO tai-utc.dat
     * file</a>
     * @see <a href="http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history">IERS
     * UTC-TAI.history file</a>
     * @see TAIUTCDatFilesLoader
     * @see UTCTAIHistoryFilesLoader
     * @see AGILeapSecondFilesLoader
     * @see #getUTC()
     * @see #clearUTCTAIOffsetsLoaders()
     * @since 7.1
     */
    public void addDefaultUTCTAIOffsetsLoaders() {
        synchronized (this) {
            final DataProvidersManager dataProvidersManager =
                    lazyLoadedEop.getDataProvidersManager();
            addUTCTAIOffsetsLoader(new TAIUTCDatFilesLoader(TAIUTCDatFilesLoader.DEFAULT_SUPPORTED_NAMES, dataProvidersManager));
            addUTCTAIOffsetsLoader(new UTCTAIHistoryFilesLoader(dataProvidersManager));
            addUTCTAIOffsetsLoader(new AGILeapSecondFilesLoader(AGILeapSecondFilesLoader.DEFAULT_SUPPORTED_NAMES, dataProvidersManager));
        }
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
        synchronized (this) {
            loaders.clear();
        }
    }

    @Override
    public TAIScale getTAI() {

        TAIScale refTai = tai.get();
        if (refTai == null) {
            tai.compareAndSet(null, new TAIScale());
            refTai = tai.get();
        }

        return refTai;

    }

    @Override
    public UTCScale getUTC() {

        UTCScale refUtc = utc.get();
        if (refUtc == null) {
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
            utc.compareAndSet(null, new UTCScale(getTAI(), entries));
            refUtc = utc.get();
        }

        return refUtc;

    }

    @Override
    public UT1Scale getUT1(final IERSConventions conventions, final boolean simpleEOP) {
        // synchronized to maintain the same semantics as Orekit 10.0
        synchronized (this) {
            return super.getUT1(conventions, simpleEOP);
        }
    }

    @Override
    protected EOPHistory getEopHistory(final IERSConventions conventions,
                                       final boolean simpleEOP) {
        return lazyLoadedEop.getEOPHistory(conventions, simpleEOP, this);
    }

    // need to make this public for compatibility. Provides access to UT1 constructor.
    /** {@inheritDoc} */
    @Override
    public UT1Scale getUT1(final EOPHistory history) {
        return super.getUT1(history);
    }

    @Override
    public TTScale getTT() {

        TTScale refTt = tt.get();
        if (refTt == null) {
            tt.compareAndSet(null, new TTScale());
            refTt = tt.get();
        }

        return refTt;

    }

    @Override
    public GalileoScale getGST() {

        GalileoScale refGst = gst.get();
        if (refGst == null) {
            gst.compareAndSet(null, new GalileoScale());
            refGst = gst.get();
        }

        return refGst;

    }

    @Override
    public GLONASSScale getGLONASS() {

        GLONASSScale refGlonass = glonass.get();
        if (refGlonass == null) {
            glonass.compareAndSet(null, new GLONASSScale(getUTC()));
            refGlonass = glonass.get();
        }

        return refGlonass;

    }

    @Override
    public QZSSScale getQZSS() {

        QZSSScale refQzss = qzss.get();
        if (refQzss == null) {
            qzss.compareAndSet(null, new QZSSScale());
            refQzss = qzss.get();
        }

        return refQzss;

    }

    @Override
    public GPSScale getGPS() {

        GPSScale refGps = gps.get();
        if (refGps == null) {
            gps.compareAndSet(null, new GPSScale());
            refGps = gps.get();
        }

        return refGps;

    }

    @Override
    public TCGScale getTCG() {

        TCGScale refTcg = tcg.get();
        if (refTcg == null) {
            tcg.compareAndSet(null, new TCGScale(getTT(), getTAI()));
            refTcg = tcg.get();
        }

        return refTcg;

    }

    @Override
    public TDBScale getTDB() {

        TDBScale refTdb = tdb.get();
        if (refTdb == null) {
            tdb.compareAndSet(null, new TDBScale(getTT(), getJ2000Epoch()));
            refTdb = tdb.get();
        }

        return refTdb;

    }

    @Override
    public TCBScale getTCB() {

        TCBScale refTcb = tcb.get();
        if (refTcb == null) {
            tcb.compareAndSet(null, new TCBScale(getTDB(), getTAI()));
            refTcb = tcb.get();
        }

        return refTcb;

    }

    @Override
    public GMSTScale getGMST(final IERSConventions conventions, final boolean simpleEOP) {
        // synchronized to maintain the same semantics as Orekit 10.0
        synchronized (this) {
            return super.getGMST(conventions, simpleEOP);
        }
    }

    @Override
    public IRNSSScale getIRNSS() {

        IRNSSScale refIrnss = irnss.get();
        if (refIrnss == null) {
            irnss.compareAndSet(null, new IRNSSScale());
            refIrnss = irnss.get();
        }

        return refIrnss;

    }

    @Override
    public BDTScale getBDT() {

        BDTScale refBds = bds.get();
        if (refBds == null) {
            bds.compareAndSet(null, new BDTScale());
            refBds = bds.get();
        }

        return refBds;

    }

}
