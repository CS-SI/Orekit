/* Contributed in the public domain.
 * Licensed to CS Group (CS) under one or more
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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import org.hipparchus.util.Pair;
import org.orekit.frames.EOPEntry;
import org.orekit.frames.EOPHistory;
import org.orekit.utils.IERSConventions;

/**
 * A set of time scales that creates most time scales when constructed. The exceptions are
 * {@link #getUT1(IERSConventions, boolean)} and {@link #getGMST(IERSConventions,
 * boolean)} where six different instances exist based on conventions and simple EOP
 * settings.
 *
 * @author Evan Ward
 * @since 10.1
 */
class PreloadedTimeScales extends AbstractTimeScales {

    /** TAI time scale. */
    private final TAIScale tai;
    /** UTC time scale. */
    private final UTCScale utc;
    /** TT time scale. */
    private final TTScale tt;
    /** Galileo time scale. */
    private final GalileoScale gst;
    /** GLONASS time scale. */
    private final GLONASSScale glonass;
    /** QZSS time scale. */
    private final QZSSScale qzss;
    /** GPS time scale. */
    private final GPSScale gps;
    /** TCG time scale. */
    private final TCGScale tcg;
    /** Tdb time scale. */
    private final TDBScale tdb;
    /** TCB time scale. */
    private final TCBScale tcb;
    /** IRNSS time scale. */
    private final IRNSSScale irnss;
    /** BDT time scale. */
    private final BDTScale bdt;
    /** Provider of EOP data. */
    private final BiFunction<
            ? super IERSConventions,
            ? super TimeScales,
            ? extends Collection<? extends EOPEntry>> eopSupplier;
    /** Cached EOP data. */
    private final ConcurrentMap<IERSConventions, Collection<? extends EOPEntry>> eopMap;
    /** UT1 time scales. */
    private final ConcurrentMap<Pair<IERSConventions, Boolean>, UT1Scale> ut1Map;
    /** GMST time scales. */
    private final ConcurrentMap<Pair<IERSConventions, Boolean>, GMSTScale> gmstMap;

    /**
     * Create a new set of time scales from the given data.
     *
     * @param leapSeconds for UTC.
     * @param eopSupplier provides EOP for UT1.
     */
    PreloadedTimeScales(
            final Collection<? extends OffsetModel> leapSeconds,
            final BiFunction<
                    ? super IERSConventions,
                    ? super TimeScales,
                    ? extends Collection<? extends EOPEntry>> eopSupplier) {
        tai = new TAIScale();
        tt = new TTScale();
        gps = new GPSScale();
        qzss = new QZSSScale();
        gst = new GalileoScale();
        irnss = new IRNSSScale();
        bdt = new BDTScale();
        tcg = new TCGScale(tt, tai);
        utc = new UTCScale(tai, leapSeconds);
        glonass = new GLONASSScale(utc);
        tdb = new TDBScale(tt, getJ2000Epoch());
        tcb = new TCBScale(tdb, tai);
        final int n = IERSConventions.values().length;
        eopMap = new ConcurrentHashMap<>(n);
        ut1Map = new ConcurrentHashMap<>(n * 2);
        gmstMap = new ConcurrentHashMap<>(n * 2);
        this.eopSupplier = eopSupplier;
    }

    @Override
    public TAIScale getTAI() {
        return tai;
    }

    @Override
    public UTCScale getUTC() {
        return utc;
    }

    @Override
    public UT1Scale getUT1(final IERSConventions conventions, final boolean simpleEOP) {
        return ut1Map.computeIfAbsent(
                new Pair<>(conventions, simpleEOP),
            k -> getUT1(new EOPHistory(
                        conventions,
                        eopMap.computeIfAbsent(conventions, c -> eopSupplier.apply(c, this)),
                        simpleEOP,
                        this)));
    }

    @Override
    public TTScale getTT() {
        return tt;
    }

    @Override
    public GalileoScale getGST() {
        return gst;
    }

    @Override
    public GLONASSScale getGLONASS() {
        return glonass;
    }

    @Override
    public QZSSScale getQZSS() {
        return qzss;
    }

    @Override
    public GPSScale getGPS() {
        return gps;
    }

    @Override
    public TCGScale getTCG() {
        return tcg;
    }

    @Override
    public TDBScale getTDB() {
        return tdb;
    }

    @Override
    public TCBScale getTCB() {
        return tcb;
    }

    @Override
    public GMSTScale getGMST(final IERSConventions conventions, final boolean simpleEOP) {
        return gmstMap.computeIfAbsent(
            new Pair<>(conventions, simpleEOP),
            k -> new GMSTScale(getUT1(conventions, simpleEOP)));
    }

    @Override
    public IRNSSScale getIRNSS() {
        return irnss;
    }

    @Override
    public BDTScale getBDT() {
        return bdt;
    }

}

