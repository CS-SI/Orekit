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
package org.orekit.files.ccsds.section;

/**
 * NDM segments are ({@link Metadata}, {@link Data}) pairs.
 * @author Luc Maisonobe
 * @since 11.0
 * @param <M> type of the metadata
 * @param <D> type of the data
 */
public class Segment<M extends Metadata, D extends Data> {

    /** Metadata. */
    private M metadata;

    /** Data. */
    private final D data;

    /**
     * Constructor.
     * @param metadata segment metadata
     * @param data segment data
     */
    public Segment(final M metadata, final D data) {
        this.metadata = metadata;
        this.data     = data;
    }

    /** Get the segment metadata.
     * @return segment metadata
     */
    public M getMetadata() {
        return metadata;
    }

    /** Set the segment metadata.
     * @param metadata the segment metadata
     */
    public void setMetadata(final M metadata) {
        this.metadata = metadata;
    }

    /** Get the segment data.
     * @return segment data
     */
    public D getData() {
        return data;
    }

}
