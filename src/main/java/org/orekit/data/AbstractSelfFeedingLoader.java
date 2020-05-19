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
package org.orekit.data;

/**
 * Abstract class that combines a {@link DataProvidersManager} with a supported names
 * regular expression for {@link DataProvidersManager#feed(String, DataLoader)}.
 *
 * @author Evan Ward
 * @since 10.1
 */
public abstract class AbstractSelfFeedingLoader {

    /** Regular expression for supported files names. */
    private String supportedNames;
    /** Source for auxiliary data files. */
    private final DataProvidersManager manager;

    /**
     * Create an abstract data loader that can feed itself.
     *
     * @param supportedNames regular expression. See {@link DataProvidersManager#feed(String,
     *                       DataLoader)}.
     * @param manager        the source of auxiliary data files.
     */
    public AbstractSelfFeedingLoader(final String supportedNames,
                                     final DataProvidersManager manager) {
        this.supportedNames = supportedNames;
        this.manager = manager;
    }

    /**
     * Feed the given loader with {@link #getDataProvidersManager()} and {@link
     * #getSupportedNames()}.
     *
     * @param loader to feed.
     * @return the value returned by {@link DataProvidersManager#feed(String,
     * DataLoader)}.
     */
    protected boolean feed(final DataLoader loader) {
        return getDataProvidersManager().feed(getSupportedNames(), loader);
    }

    /**
     * Get the supported names regular expression.
     *
     * @return the supported names.
     * @see DataProvidersManager#feed(String, DataLoader)
     */
    protected String getSupportedNames() {
        return supportedNames;
    }

    /**
     * Set the supported names regular expression. Using this method may create
     * concurrency issues if multiple threads can call {@link #feed(DataLoader)} and it is
     * not properly synchronized.
     *
     * @param supportedNames regular expression.
     */
    protected void setSupportedNames(final String supportedNames) {
        this.supportedNames = supportedNames;
    }

    /**
     * Get the data provider manager.
     *
     * @return the source of auxiliary data files.
     */
    protected DataProvidersManager getDataProvidersManager() {
        return manager;
    }

}
