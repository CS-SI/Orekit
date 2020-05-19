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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.orekit.data.DataLoader;

/**
 * A {@link DataLoader} based on a {@link UTCTAIOffsetsLoader.Parser} that loads a single
 * file.
 *
 * @author Evan Ward
 * @since 10.1
 */
class UtcTaiOffsetLoader implements DataLoader {

    /** Leap second parser. */
    private final UTCTAIOffsetsLoader.Parser parser;

    /** UTC-TAI offsets. */
    private final List<OffsetModel> offsets;

    /**
     * Create a data loader from a lep second parser.
     *
     * @param parser leap second parser.
     */
    UtcTaiOffsetLoader(final UTCTAIOffsetsLoader.Parser parser) {
        this.parser = parser;
        this.offsets = new ArrayList<>();
    }

    /**
     * Get the parsed offsets.
     *
     * @return parsed offsets
     */
    public List<OffsetModel> getOffsets() {
        return offsets;
    }

    @Override
    public boolean stillAcceptsData() {
        return offsets.isEmpty();
    }

    @Override
    public void loadData(final InputStream input, final String name)
            throws IOException {
        offsets.clear();
        offsets.addAll(parser.parse(input, name));
    }

}
