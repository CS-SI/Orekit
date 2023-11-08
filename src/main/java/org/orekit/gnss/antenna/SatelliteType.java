/* Copyright 2002-2023 CS GROUP
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
package org.orekit.gnss.antenna;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.gnss.attitude.BeidouGeo;
import org.orekit.gnss.attitude.BeidouIGSO;
import org.orekit.gnss.attitude.BeidouMeo;
import org.orekit.gnss.attitude.GNSSAttitudeProvider;
import org.orekit.gnss.attitude.GPSBlockIIA;
import org.orekit.gnss.attitude.GPSBlockIIF;
import org.orekit.gnss.attitude.GPSBlockIIR;
import org.orekit.gnss.attitude.Galileo;
import org.orekit.gnss.attitude.GenericGNSS;
import org.orekit.gnss.attitude.Glonass;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;

/**
 * Enumerate for satellite types.
 *
 * @author Luc Maisonobe
 * @since 9.3
 */
public enum SatelliteType {

    /** BeiDou-2 GEO. */
    BEIDOU_2G("BEIDOU-2G") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new BeidouGeo(validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-2 IGSO. */
    BEIDOU_2I("BEIDOU-2I") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new BeidouIGSO(validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-2 MEO. */
    BEIDOU_2M("BEIDOU-2M") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new BeidouMeo(validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-3 IGSO. */
    BEIDOU_3I("BEIDOU-3I") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // it seems Beidou III satellites use Galileo mode
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-3. */
    BEIDOU_3SI_SECM("BEIDOU-3SI-SECM") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // it seems Beidou III satellites use Galileo mode
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-3. */
    BEIDOU_3SI_CAST("BEIDOU-3SI-CAST") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // it seems Beidou III satellites use Galileo mode
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-3. */
    BEIDOU_3M_CAST("BEIDOU-3M-CAST") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // it seems Beidou III satellites use Galileo mode
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-3. */
    BEIDOU_3SM_CAST("BEIDOU-3SM-CAST") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // it seems Beidou III satellites use Galileo mode
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-3. */
    BEIDOU_3M_SECM("BEIDOU-3M-SECM") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // it seems Beidou III satellites use Galileo mode
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** BeiDou-3. */
    BEIDOU_3G_CAST("BEIDOU-3G-CAST") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // it seems Beidou III satellites use Galileo mode
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GPS Block I     : SVN 01-11. */
    BLOCK_I("BLOCK I") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new GPSBlockIIA(GPSBlockIIA.getDefaultYawRate(prnNumber),
                                   GPSBlockIIA.DEFAULT_YAW_BIAS,
                                   validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GPS Block II    : SVN 13-21. */
    BLOCK_II("BLOCK II") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new GPSBlockIIA(GPSBlockIIA.getDefaultYawRate(prnNumber),
                                   GPSBlockIIA.DEFAULT_YAW_BIAS,
                                   validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GPS Block IIA   : SVN 22-40. */
    BLOCK_IIA("BLOCK IIA") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new GPSBlockIIA(GPSBlockIIA.getDefaultYawRate(prnNumber),
                                   GPSBlockIIA.DEFAULT_YAW_BIAS,
                                   validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GPS Block IIR   : SVN 41, 43-46, 51, 54, 56. */
    BLOCK_IIR_A("BLOCK IIR-A") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new GPSBlockIIR(GPSBlockIIR.DEFAULT_YAW_RATE,
                                   validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GPS Block IIR   : SVN 47, 59-61. */
    BLOCK_IIR_B("BLOCK IIR-B") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new GPSBlockIIR(GPSBlockIIR.DEFAULT_YAW_RATE,
                                   validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GPS Block IIR-M : SVN 48-50, 52-53, 55, 57-58. */
    BLOCK_IIR_M("BLOCK IIR-M") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new GPSBlockIIR(GPSBlockIIR.DEFAULT_YAW_RATE,
                                   validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GPS Block IIF   : SVN 62-73. */
    BLOCK_IIF("BLOCK IIF") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new GPSBlockIIF(GPSBlockIIF.DEFAULT_YAW_RATE,
                                   GPSBlockIIF.DEFAULT_YAW_BIAS,
                                   validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GPS Block IIIA  : SVN 74-81. */
    BLOCK_IIIA("BLOCK IIIA") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // we don't have yet a specific mode for block IIIA, we reuse block IIF
            return new GPSBlockIIF(GPSBlockIIF.DEFAULT_YAW_RATE,
                                   GPSBlockIIF.DEFAULT_YAW_BIAS,
                                   validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** Galileo In-Orbit Validation Element A (GIOVE-A). */
    GALILEO_0A("GALILEO-0A") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** Galileo In-Orbit Validation Element B (GIOVE-B). */
    GALILEO_0B("GALILEO-0B") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** Galileo IOV     : GSAT 0101-0104. */
    GALILEO_1("GALILEO-1") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** Galileo FOC     : GSAT 0201-0222. */
    GALILEO_2("GALILEO-2") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new Galileo(Galileo.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GLONASS         : GLONASS no. 201-249, 750-798. */
    GLONASS("GLONASS") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new Glonass(Glonass.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GLONASS-M       : GLONASS no. 701-749, IGS SVN R850-R861 (GLO no. + 100). */
    GLONASS_M("GLONASS-M") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new Glonass(Glonass.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GLONASS-K1      : IGS SVN R801-R802 (GLO no. + 100). */
    GLONASS_K1("GLONASS-K1") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new Glonass(Glonass.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** GLONASS-K2. */
    GLONASS_K2("GLONASS-K2") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            return new Glonass(Glonass.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** IRNSS-1 GEO. */
    IRNSS_1GEO("IRNSS-1GEO") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // we don't have yet a specific mode for IRNSS, we use generic GNSS (simple yaw steering)
            return new GenericGNSS(validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** IRNSS-1 IGSO. */
    IRNSS_1IGSO("IRNSS-1IGSO") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // we don't have yet a specific mode for IRNSS, we use generic GNSS (simple yaw steering)
            return new GenericGNSS(validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** QZSS Block I (Michibiki-1). */
    QZSS("QZSS") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // we don't have yet a specific mode for QZSS, we use generic GNSS (simple yaw steering)
            return new GenericGNSS(validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** QZSS Block II (Michibiki-2). */
    QZSS_2A("QZSS-2A") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // we don't have yet a specific mode for QZSS, we use generic GNSS (simple yaw steering)
            return new GenericGNSS(validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** QZSS Block II IGSO (Michibiki-2,4). */
    QZSS_2I("QZSS-2I") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // we don't have yet a specific mode for QZSS, we use generic GNSS (simple yaw steering)
            return new GenericGNSS(validityStart, validityEnd, sun, inertialFrame);
        }
    },

    /** QZSS Block II GEO (Michibiki-3). */
    QZSS_2G("QZSS-2G") {
        /** {@inheritDoc} */
        @Override
        public GNSSAttitudeProvider buildAttitudeProvider(final AbsoluteDate validityStart,
                                                          final AbsoluteDate validityEnd,
                                                          final ExtendedPVCoordinatesProvider sun,
                                                          final Frame inertialFrame, final int prnNumber) {
            // we don't have yet a specific mode for QZSS, we use generic GNSS (simple yaw steering)
            return new GenericGNSS(validityStart, validityEnd, sun, inertialFrame);
        }
    };

    /** Pattern for satellite antenna code. */
    private static final Pattern PATTERN = Pattern.compile("[-_ ]");

    /** Parsing map. */
    private static final Map<String, SatelliteType> NAMES_MAP = new HashMap<>();
    static {
        for (final SatelliteType satelliteAntennaCode : values()) {
            NAMES_MAP.put(satelliteAntennaCode.getName(), satelliteAntennaCode);
            NAMES_MAP.put(PATTERN.matcher(satelliteAntennaCode.getName()).replaceAll(""), satelliteAntennaCode);
        }
    }

    /** IGS name for the antenna code. */
    private final String name;

    /** Simple constructor.
     * @param name IGS name for the antenna code
     */
    SatelliteType(final String name) {
        this.name = name;
    }

    /** Get the IGS name for the antenna code.
     * @return IGS name for the antenna code
     */
    public String getName() {
        return name;
    }

    /** Build an attitude provider suitable for this satellite type.
     * <p>
     * Apart from the caller-provided validity interval, Sun provider,
     * frame and PRN number, all construction parameters required for
     * the {@link GNSSAttitudeProvider} (for example yaw rates and biases)
     * will be the default ones. If non-default values are needed, the
     * constructor of the appropriate {@link GNSSAttitudeProvider} must be
     * called explicitly instead of relying on this general purpose factory
     * method.
     * </p>
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param inertialFrame inertial frame where velocity are computed
     * @param prnNumber number within the satellite system
     * @return an attitude provider suitable for this satellite type
     */
    public abstract GNSSAttitudeProvider buildAttitudeProvider(AbsoluteDate validityStart,
                                                               AbsoluteDate validityEnd,
                                                               ExtendedPVCoordinatesProvider sun,
                                                               Frame inertialFrame, int prnNumber);

    /** Parse a string to get the satellite type.
     * <p>
     * The name must be either a strict IGS name (like "BLOCK IIR-B") or
     * an IGS name canonicalized by removing all spaces, hypen and underscore
     * characters (like BLOCKIIRB").
     * </p>
     * @param s string to parse (must be a strict IGS name)
     * @return the satellite type
     * @exception OrekitIllegalArgumentException if the string does not correspond to a satellite antenna code
     */
    public static SatelliteType parseSatelliteType(final String s)
        throws OrekitIllegalArgumentException {
        final SatelliteType satelliteAntennaCode = NAMES_MAP.get(s);
        if (satelliteAntennaCode == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_SATELLITE_ANTENNA_CODE, s);
        }
        return satelliteAntennaCode;
    }

}
