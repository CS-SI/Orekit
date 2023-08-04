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

/** Source table for ntrip streams retrieval.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class SourceTable {

    /** Flags set by server. */
    private final String ntripFlags;

    /** Casters records. */
    private final List<CasterRecord> casters;

    /** Networks records. */
    private final List<NetworkRecord> networks;

    /** Data stream records. */
    private final List<DataStreamRecord> dataStreams;

    /** Build a source table by parsing all records.
     * @param ntripFlags flags set by the server
     */
    SourceTable(final String ntripFlags) {
        this.ntripFlags   = ntripFlags;
        this.casters      = new ArrayList<>();
        this.networks     = new ArrayList<>();
        this.dataStreams  = new ArrayList<>();
    }

    /** Add a caster record.
     * @param caster caster record to add
     */
    void addCasterRecord(final CasterRecord caster) {
        casters.add(caster);
    }

    /** Add a network record.
     * @param network network record to add
     */
    void addNetworkRecord(final NetworkRecord network) {
        networks.add(network);
    }

    /** Add a data stream record.
     * @param dataStream data stream record to add
     */
    void addDataStreamRecord(final DataStreamRecord dataStream) {
        dataStreams.add(dataStream);
    }

    /** Get the flags set by server.
     * @return flags set by server
     */
    public String getNtripFlags() {
        return ntripFlags;
    }

    /** Get the casters records.
     * @return casters records
     */
    public List<CasterRecord> getCasters() {
        return casters;
    }

    /** Get the networks records.
     * @return networks records
     */
    public List<NetworkRecord> getNetworks() {
        return networks;
    }

    /** Get the data streams records.
     * @return data streams records
     */
    public List<DataStreamRecord> getDataStreams() {
        return dataStreams;
    }

}
