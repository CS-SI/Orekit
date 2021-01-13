/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm;

import org.orekit.files.ccsds.ndm.NDMData;
import org.orekit.files.ccsds.ndm.NDMSegment;

/**
 * This class stores the metadata and data for one attitude segment.
 * @param <M> type of the metadata
 * @param <D> type of the data
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ADMSegment<M extends ADMMetadata, D extends NDMData> extends NDMSegment<M, D> {

    /** Simple constructor.
     * @param metadata segment metadata
     * @param data segment data
     */
    public ADMSegment(final M metadata, final D data) {
        super(metadata, data);
    }

}
