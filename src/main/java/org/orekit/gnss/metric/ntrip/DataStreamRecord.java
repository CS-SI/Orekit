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
package org.orekit.gnss.metric.ntrip;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.util.FastMath;

/** Data stream record in source table.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class DataStreamRecord extends Record {

    /** Pattern for delimiting messages. */
    private static final Pattern SEPARATOR = Pattern.compile(",");

    /** Message pattern. */
    private static final Pattern PATTERN = Pattern.compile("^([^()]+)(?:\\(([0-9]+)\\))?$");

    /** Data format. */
    private final DataFormat format;

    /** Streamed messages. */
    private final List<StreamedMessage> formatDetails;

    /** Carrier phase. */
    private final CarrierPhase carrierPhase;

    /** Navigation systems. */
    private final List<NavigationSystem> systems;

    /** Latitude (rad). */
    private final double latitude;

    /** Longitude (rad). */
    private final double longitude;

    /** Indicator for required NMEA. */
    private final boolean nmeaRequired;

    /** Indicator for networked streams. */
    private final boolean networked;

    /** Authentication method. */
    private final Authentication authentication;

    /** Indicator for required fees. */
    private final boolean fees;

    /** Bit rate. */
    private int bitRate;

    /** Build a data stream record by parsing a source table line.
     * @param line source table line
     */
    public DataStreamRecord(final String line) {
        super(line);
        this.format         = DataFormat.getDataFormat(getField(3));

        final String[] detailsFields = SEPARATOR.split(getField(4));
        this.formatDetails           = new ArrayList<>(detailsFields.length);
        for (final String field : detailsFields) {
            if (!field.isEmpty()) {
                final Matcher matcher = PATTERN.matcher(field);
                if (matcher.matches() && matcher.start(2) >= 0) {
                    formatDetails.add(new StreamedMessage(matcher.group(1),
                                                          Integer.parseInt(matcher.group(2))));
                } else {
                    formatDetails.add(new StreamedMessage(field, -1));
                }
            }
        }

        this.carrierPhase   = CarrierPhase.getCarrierPhase(getField(5));
        this.systems        = Stream.
                              of(getField(6).split("\\+")).
                              map(k -> NavigationSystem.getNavigationSystem(k)).
                              collect(Collectors.toList());
        this.latitude       = FastMath.toRadians(Double.parseDouble(getField(9)));
        this.longitude      = FastMath.toRadians(Double.parseDouble(getField(10)));
        this.nmeaRequired   = Integer.parseInt(getField(11)) != 0;
        this.networked      = Integer.parseInt(getField(12)) != 0;
        this.authentication = Authentication.getAuthentication(getField(15));
        this.fees           = getField(16).equals("Y");
        this.bitRate        = Integer.parseInt(getField(17));
    }

    /** {@inheritDoc} */
    @Override
    public RecordType getRecordType() {
        return RecordType.STR;
    }

    /** Get the mount point.
     * @return mount point
     */
    public String getMountPoint() {
        return getField(1);
    }

    /** Get the source identifier.
     * @return source identifier
     */
    public String getSourceIdentifier() {
        return getField(2);
    }

    /** Get the data format.
     * @return data format
     */
    public DataFormat getFormat() {
        return format;
    }

    /** Get the format details.
     * @return format details
     */
    public List<StreamedMessage> getFormatDetails() {
        return formatDetails;
    }

    /** Get the carrier phase.
     * @return carrier phase
     */
    public CarrierPhase getCarrierPhase() {
        return carrierPhase;
    }

    /** Get the navigation systems.
     * @return navigation systems
     */
    public List<NavigationSystem> getNavigationSystems() {
        return systems;
    }

    /** Get the network.
     * @return network
     */
    public String getNetwork() {
        return getField(7);
    }

    /** Get the country.
     * @return country
     */
    public String getCountry() {
        return getField(8);
    }

    /** Get the latitude.
     * @return latitude (rad)
     */
    public double getLatitude() {
        return latitude;
    }

    /** Get the longitude.
     * @return longitude (rad)
     */
    public double getLongitude() {
        return longitude;
    }

    /** Check if NMEA message must be sent to caster.
     * @return true if NMEA message must be sent to caster
     */
    public boolean isNMEARequired() {
        return nmeaRequired;
    }

    /** Check if the stream is generated from a network of stations.
     * @return true if stream  is generated from a network of stations
     */
    public boolean isNetworked() {
        return networked;
    }

    /** Get the hardware or software generator.
     * @return hardware or software generator
     */
    public String getGenerator() {
        return getField(13);
    }

    /** Get the compression/encryption algorithm applied.
     * @return compression/encryption algorithm applied
     */
    public String getCompressionEncryption() {
        return getField(14);
    }

    /** Get the authentication method.
     * @return authentication method
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /** Check if fees are required.
     * @return true if fees are required
     */
    public boolean areFeesRequired() {
        return fees;
    }

    /** Get the bit rate.
     * @return bit rate
     */
    public int getBitRate() {
        return bitRate;
    }

}
