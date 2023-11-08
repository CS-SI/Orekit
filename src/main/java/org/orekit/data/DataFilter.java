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

/** Interface for filtering data (typically uncompressing it) in {@link DataProvider data providers}
 * before passing it to {@link DataLoader data loaders}.
 * @see DataProvider
 * @see DataLoader
 * @author Luc Maisonobe
 * @since 9.2
 */
public interface DataFilter {

    /** Filter the data source.
     * <p>
     * Filtering is often based on suffix. For example a gzip compressed
     * file will have an original name of the form base.ext.gz when the
     * corresponding uncompressed file will have a filtered name base.ext.
     * </p>
     * <p>
     * A filter must <em>never</em> {@link DataSource.Opener#openStreamOnce() open}
     * the {@link DataSource} by itself, regardless of the fact it will return
     * the original instance or a filtered instance. The rationale is that it
     * is the upper layer that will decide to open (or not) the returned
     * value and that a {@link DataSource} can be opened only once; this is the
     * core principle of lazy-opening provided by {@link DataSource}.
     * </p>
     * <p>
     * Beware that as the {@link DataProvidersManager data providers manager}
     * will attempt to pile all filters in a stack as long as their implementation
     * of this method returns a value different from the {@code original} parameter.
     * This implies that the filter, <em>must</em> perform some checks to see if it must
     * be applied or not. If for example there is a need for a deciphering filter
     * to be applied once to all data, then the filter should for example check
     * for a suffix in the {@link DataSource#getName() name} and create a new
     * filtered {@link DataSource} instance <em>only</em> if the suffix is present,
     * removing the suffix from the filtered instance. Failing to do so and simply
     * creating a filtered instance with one deciphering layer without changing the
     * name would result in an infinite stack of deciphering filters being built, until
     * a stack overflow or memory exhaustion exception occurs.
     * </p>
     * @param original original data source
     * @return filtered data source, or {@code original} if this filter
     * does not apply to this data source
     * @exception IOException if filtered stream cannot be created
     */
    DataSource filter(DataSource original) throws IOException;

}
