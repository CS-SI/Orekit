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

import org.hipparchus.util.FastMath;

/** Caster record in source table.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class CasterRecord extends Record {

    /** Port number. */
    private final int port;

    /** Fallback port number. */
    private final int fallbackPort;

    /** Indicator for NMEA reception. */
    private final boolean canReceiveNMEA;

    /** Latitude (rad). */
    private final double latitude;

    /** Longitude (rad). */
    private final double longitude;

    /** Build a caster record by parsing a source table line.
     * @param line source table line
     */
    public CasterRecord(final String line) {
        super(line);
        this.port           = Integer.parseInt(getField(2));
        this.canReceiveNMEA = Integer.parseInt(getField(5)) != 0;
        this.latitude       = FastMath.toRadians(Double.parseDouble(getField(7)));
        this.longitude      = FastMath.toRadians(Double.parseDouble(getField(8)));
        this.fallbackPort   = Integer.parseInt(getField(10));
    }

    /** {@inheritDoc} */
    @Override
    public RecordType getRecordType() {
        return RecordType.CAS;
    }

    /** Get the host or IP address.
     * @return host or IP address
     */
    public String getHostOrIPAddress() {
        return getField(1);
    }

    /** Get the port number.
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /** Get the source identifier.
     * @return source identifier
     */
    public String getSourceIdentifier() {
        return getField(3);
    }

    /** Get the institution/agency/company operating the caster.
     * @return institution/agency/company operating the caster
     */
    public String getOperator() {
        return getField(4);
    }

    /** Check if caster can receive NMEA messages.
     * @return true if caster can receive NMEA messages
     */
    public boolean canReceiveNMEA() {
        return canReceiveNMEA;
    }

    /** Get the country.
     * @return country
     */
    public String getCountry() {
        return getField(6);
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

    /** Get the fallback host or IP address.
     * @return fallback host or IP address
     */
    public String getFallbackHostOrIPAddress() {
        return getField(9);
    }

    /** Get the fallback port number.
     * @return fallback port number
     */
    public int getFallbackPort() {
        return fallbackPort;
    }

}
