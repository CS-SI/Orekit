/* Copyright 2002-2025 CS GROUP
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
package org.orekit.models.earth.displacement;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Factory for ocean loading coefficients, using Onsala Space Observatory files in BLQ format.
 * <p>
 * Files in BLQ format can be generated using the form at the
 * <a href="http://holt.oso.chalmers.se/loading/">Bos-Scherneck web site</a>,
 * selecting BLQ as the output format.
 * </p>
 * <p>
 * The sites names are extracted from the file content, not the file name, because the
 * file can contain more than one station. As we expect existing files may have been
 * stripped from headers and footers, we do not attempt to parse them. We only parse
 * the series of 7 lines blocks starting with the lines with the station names and their
 * coordinates and the 6 data lines that follows. Several such blocks may appear in the
 * file. Copy-pasting the entire mail received from OSO after completing the web site
 * form works, as intermediate lines between the 7 lines blocks are simply ignored.
 * </p>
 * @see OceanLoadingCoefficients
 * @see OceanLoading
 * @since 9.1
 * @author Luc Maisonobe
 */
public class OceanLoadingCoefficientsBLQFactory extends AbstractSelfFeedingLoader {

    /** Default supported files name pattern for Onsala Space Observatory files in BLQ format. */
    public static final String DEFAULT_BLQ_SUPPORTED_NAMES = "^.+\\.blq$";

    /** Parsed coefficients. */
    private final List<OceanLoadingCoefficients> coefficients;

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * <p>
     * Files in BLQ format can be generated using the form at the
     * <a href="http://holt.oso.chalmers.se/loading/">Bos-Scherneck web site</a>,
     * selecting BLQ as the output format.
     * </p>
     * @param supportedNames regular expression for supported files names
     * @see #DEFAULT_BLQ_SUPPORTED_NAMES
     * @see #OceanLoadingCoefficientsBLQFactory(String, DataProvidersManager)
     */
    @DefaultDataContext
    public OceanLoadingCoefficientsBLQFactory(final String supportedNames) {
        this(supportedNames, DataContext.getDefault().getDataProvidersManager());
    }

    /**
     * This constructor allows specification of the source of the BLQ auxiliary data
     * files.
     *
     * <p>
     * Files in BLQ format can be generated using the form at the
     * <a href="http://holt.oso.chalmers.se/loading/">Bos-Scherneck web site</a>,
     * selecting BLQ as the output format.
     * </p>
     * @param supportedNames regular expression for supported files names
     * @param dataProvidersManager provides access to auxiliary data files.
     * @see #DEFAULT_BLQ_SUPPORTED_NAMES
     * @since 10.1
     */
    public OceanLoadingCoefficientsBLQFactory(final String supportedNames,
                                              final DataProvidersManager dataProvidersManager) {
        super(supportedNames, dataProvidersManager);

        this.coefficients   = new ArrayList<>();

    }

    /** Lazy loading of coefficients.
     */
    private void loadsIfNeeded() {
        if (coefficients.isEmpty()) {
            feed(new DataLoader() {

                /** {@inheritDoc} */
                @Override
                public boolean stillAcceptsData() {
                    return true;
                }

                /** {@inheritDoc} */
                @Override
                public void loadData(final InputStream input, final String name) {
                    final OceanLoadingCoefficientsBlqParser parser = new OceanLoadingCoefficientsBlqParser();
                    coefficients.addAll(parser.parse(new DataSource(name, () -> input)));
                }
            });
        }
    }

    /** Get the list of sites for which we have found coefficients, in lexicographic order ignoring case.
     * @return list of sites for which we have found coefficients, in lexicographic order ignoring case
     */
    public List<String> getSites() {

        loadsIfNeeded();

        // extract sites names from the map
        return coefficients.stream()
                .map(OceanLoadingCoefficients::getSiteName)
                // sort to ensure we have a reproducible order
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

    }

    /** Get the coefficients for a given site.
     * @param site site name (as it appears in the Onsala Space Observatory files in BLQ format),
     * ignoring case
     * @return coefficients for the site
     */
    public OceanLoadingCoefficients getCoefficients(final String site) {

        loadsIfNeeded();

        final Optional<OceanLoadingCoefficients> optional =
                        coefficients.stream().filter(c -> c.getSiteName().equalsIgnoreCase(site)).findFirst();
        if (!optional.isPresent()) {
            throw new OrekitException(OrekitMessages.STATION_NOT_FOUND,
                                      site,
                                      String.join(", ", getSites()));
        }

        return optional.get();

    }

}
