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

/** Network record in source table.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class NetworkRecord extends Record {

    /** Authentication method. */
    private final Authentication authentication;

    /** Indicator for required fees. */
    private final boolean fees;

    /** Build a data stream record by parsing a source table line.
     * @param line source table line
     */
    public NetworkRecord(final String line) {
        super(line);
        this.authentication = Authentication.getAuthentication(getField(3));
        this.fees           = getField(4).equals("Y");
    }

    /** {@inheritDoc} */
    @Override
    public RecordType getRecordType() {
        return RecordType.NET;
    }

    /** Get the network identifier.
     * @return network identifier
     */
    public String getNetworkIdentifier() {
        return getField(1);
    }

    /** Get the institution/agency/company operating the caster.
     * @return institution/agency/company operating the caster
     */
    public String getOperator() {
        return getField(2);
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

    /** Get the web address for network information.
     * @return web address for network information
     */
    public String getNetworkInfoAddress() {
        return getField(5);
    }

    /** Get the web address for stream information.
     * @return web address for stream information
     */
    public String getStreamInfoAddress() {
        return getField(6);
    }

    /** Get the web or mail address for registration.
     * @return web or mail address for registration
     */
    public String getRegistrationAddress() {
        return getField(7);
    }

}
