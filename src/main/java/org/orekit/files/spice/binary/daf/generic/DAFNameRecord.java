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
package org.orekit.files.spice.binary.daf.generic;

/**
 * Represents a name record of a {@link DAF} file, containing the names for
 * multiple arrays.
 *
 * @author Rafael Ayala
 * @since 14.0
 */
public class DAFNameRecord {

    /**
     * Number of array names in this name record.
     */
    private final int numberNamesThisRec;

    /**
     * Raw data for all the names in this summary record.
     */
    private final byte[] namesRaw;

    /**
     * Basic constructor.
     *
     * @param numberNamesThisRec number of array names in this name record
     * @param namesRaw raw data for all the array names in this name record
     */
    public DAFNameRecord(final int numberNamesThisRec, final byte[] namesRaw) {
        this.numberNamesThisRec = numberNamesThisRec;
        this.namesRaw = namesRaw.clone();
    }

    /**
     * Get the number of array names in this name record.
     *
     * @return number of array names in this name record
     */
    public int getNumberNamesThisRec() {
        return numberNamesThisRec;
    }

    /**
     * Get the raw data for all the array names in this name record.
     *
     * @return raw data for all the array names in this name record
     */
    public byte[] getNamesRaw() {
        return namesRaw.clone();
    }
}
