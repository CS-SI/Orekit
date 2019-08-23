/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.errors.OrekitException;


/** Provider for data files defined in a list.
 * <p>
 * Zip archives entries are supported recursively.
 * </p>
 * @since 10.1
 * @see DataProvidersManager
 * @see NetworkCrawler
 * @see FilesListCrawler
 * @author Luc Maisonobe
 */
public abstract class ListCrawler<T> implements DataProvider {

    /** Inputs list. */
    private final List<T> inputs;

    /** Build a data classpath crawler.
     * @param inputs list of inputs (may be empty if {@link #addInput(Object) addInput} is called later)
     */
    @SafeVarargs
    protected ListCrawler(final T... inputs) {
        this.inputs = Arrays.stream(inputs).collect(Collectors.toList());
    }

    /** Add an input to the supported list.
     * @param input input to add
     */
    public void addInput(final T input) {
        inputs.add(input);
    }

    /** Get the list of inputs supported by the instance.
     * @return unmodifiable view of the list of inputs supported by the instance
     */
    public List<T> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    /** Get the complete name of a input.
     * @param input input to consider
     * @return complete name of the input
     */
    protected abstract String getCompleteName(T input);

    /** Get the base name of an input.
     * @param input input to consider
     * @return base name of the input
     */
    protected abstract String getBaseName(T input);

    /** Get a zip/jar crawler for an input.
     * @param input input to consider
     * @return zip/jar crawler for an input
     */
    protected abstract ZipJarCrawler getZipJarCrawler(T input);

    /** Get the stream to read from an input.
     * @param input input to read from
     * @return stream to read the content of the input
     * @throws IOException if the input cannot be opened for reading
     */
    protected abstract InputStream getStream(T input) throws IOException;

    /** {@inheritDoc} */
    public boolean feed(final Pattern supported, final DataLoader visitor) {

        try {
            OrekitException delayedException = null;
            boolean loaded = false;
            for (T input : inputs) {
                try {

                    if (visitor.stillAcceptsData()) {
                        final String name     = getCompleteName(input);
                        final String fileName = getBaseName(input);
                        if (ZIP_ARCHIVE_PATTERN.matcher(fileName).matches()) {

                            // browse inside the zip/jar file
                            getZipJarCrawler(input).feed(supported, visitor);
                            loaded = true;

                        } else {

                            // apply all registered filters
                            NamedData data = new NamedData(fileName, () -> getStream(input));
                            data = DataProvidersManager.getInstance().applyAllFilters(data);

                            if (supported.matcher(data.getName()).matches()) {
                                // visit the current file
                                try (InputStream is = data.getStreamOpener().openStream()) {
                                    visitor.loadData(is, name);
                                    loaded = true;
                                }
                            }

                        }
                    }

                } catch (OrekitException oe) {
                    // maybe the next path component will be able to provide data
                    // wait until all components have been tried
                    delayedException = oe;
                }
            }

            if (!loaded && delayedException != null) {
                throw delayedException;
            }

            return loaded;

        } catch (IOException | ParseException e) {
            throw new OrekitException(e, new DummyLocalizable(e.getMessage()));
        }

    }

}
