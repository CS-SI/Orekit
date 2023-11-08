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
package org.orekit.frames;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.orekit.data.DataLoader;

/**
 * Implementation of {@link DataLoader} based on {@link EopHistoryLoader.Parser} that
 * loads all files and compiles the results into one data structure.
 *
 * @author Evan Ward
 * @since 10.1
 */
class EopParserLoader implements DataLoader {

    /** Parser for EOP data files. */
    private final EopHistoryLoader.Parser parser;

    /** History entries. */
    private final List<EOPEntry> history;

    /**
     * Create a {@link DataLoader} based on a {@link EopHistoryLoader.Parser}. Loads
     * all EOP data into a single collection.
     *
     * @param parser for the EOP data files.
     */
    EopParserLoader(final EopHistoryLoader.Parser parser) {
        this.parser = parser;
        this.history = new ArrayList<>();
    }

    /**
     * Get the parsed EOP entries.
     *
     * @return the parsed EOP data. The returned collection is a reference, not a
     * copy. It is not guaranteed to be sorted.
     */
    public Collection<EOPEntry> getEop() {
        return history;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return true;
    }

    @Override
    public void loadData(final InputStream input, final String name)
            throws IOException, ParseException {
        history.addAll(parser.parse(input, name));
    }

}
