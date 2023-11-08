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
package org.orekit.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Manager for {@link DataFilter data filters}.
 * <p>
 * This manager holds a set of filters and applies all the relevant
 * ones by building a stack that transforms a raw {@link DataSource}
 * into a processed {@link DataSource}.
 * </p>
 * @see DataSource
 * @see DataFilter
 * @author Luc Maisonobe
 * @since 11.0
 */
public class FiltersManager {

    /** Supported filters. */
    private final List<DataFilter> filters;

    /** Build an empty manager.
     */
    public FiltersManager() {
        this.filters = new ArrayList<>();
    }

    /** Add a data filter.
     * @param filter filter to add
     * @see #applyRelevantFilters(DataSource)
     * @see #clearFilters()
     */
    public void addFilter(final DataFilter filter) {
        filters.add(filter);
    }

    /** Remove all data filters.
     * @see #addFilter(DataFilter)
     */
    public void clearFilters() {
        filters.clear();
    }

    /** Apply all the relevant data filters, taking care of layers.
     * <p>
     * If several filters can be applied, they will all be applied
     * as a stack, even recursively if required. This means that if
     * filter A applies to files with names of the form base.ext.a
     * and filter B applies to files with names of the form base.ext.b,
     * then providing base.ext.a.b.a will result in filter A being
     * applied on top of filter B which itself is applied on top of
     * another instance of filter A.
     * </p>
     * @param original original data source
     * @return fully filtered data source
     * @exception IOException if some data stream cannot be filtered
     * @see #addFilter(DataFilter)
     * @see #clearFilters()
     * @since 9.2
     */
    public DataSource applyRelevantFilters(final DataSource original)
        throws IOException {
        DataSource top = original;
        for (boolean filtering = true; filtering;) {
            filtering = false;
            for (final DataFilter filter : filters) {
                final DataSource filtered = filter.filter(top);
                if (filtered != top) {
                    // the filter has been applied, we need to restart the loop
                    top       = filtered;
                    filtering = true;
                    break;
                }
            }
        }
        return top;
    }

}
