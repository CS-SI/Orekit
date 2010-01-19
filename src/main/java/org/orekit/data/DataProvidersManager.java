/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;

/** Singleton class managing all supported {@link DataProvider data providers}.

 * <p>
 * This class is the single point of access for all data loading features. It
 * is used for example to load Earth Orientation Parameters used by IERS frames,
 * to load UTC leap seconds used by time scales, to load planetary ephemerides ...
 * <p>
 *
 * </p>
 * It is user-customizable: users can add their own data providers at will. This
 * allows them for example to use a database or an existing data loading library
 * in order to embed an Orekit enabled application in a global system with its
 * own data handling mechanisms. There is no upper limitation on the number of
 * providers, but often each application will use only a few.
 * </p>
 *
 * <p>
 * If the list of providers is empty when attempting to {@link #feed(String, DataLoader)
 * feed} a file loader, the {@link #addDefaultProviders()} method is called
 * automatically to set up a default configuration. This default configuration
 * contains one {@link DataProvider data provider} for each component of the
 * path-like list specified by the java property <code>orekit.data.path</code>.
 * See the {@link #feed(String, DataLoader) feed} method documentation for further
 * details. The default providers configuration is <em>not</em> set up if the list
 * is not empty. If users want to have both the default providers and additional
 * providers, they must call explicitly the {@link #addDefaultProviders()} method.
 * </p>
 *
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 * @see DirectoryCrawler
 * @see ClasspathCrawler
 */
public class DataProvidersManager implements Serializable {

    /** Name of the property defining the root directories or zip/jar files path for default configuration. */
    public static final String OREKIT_DATA_PATH = "orekit.data.path";

    /** Serializable UID. */
    private static final long serialVersionUID = -6462388122735180273L;

    /** Error message for unknown path entries. */
    private static final String NEITHER_DIRECTORY_NOR_ZIP_ARCHIVE =
        "{0} is neither a directory nor a zip/jar archive file";

    /** Supported data providers. */
    private final List<DataProvider> providers;

    /** Build an instance with default configuration.
     * <p>
     * This is a singleton, so the constructor is private.
     * </p>
     */
    private DataProvidersManager() {
        providers = new ArrayList<DataProvider>();
    }

    /** Get the unique instance.
     * @return unique instance of the manager.
     */
    public static DataProvidersManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** Add the default providers configuration.
     * <p>
     * The default configuration contains one {@link DataProvider data provider}
     * for each component of the path-like list specified by the java property
     * <code>orekit.data.path</code>.
     * </p>
     * <p>
     * If the property is not set or is null, no data will be available to the library
     * (for example no pole corrections will be applied and only predefined UTC steps
     * will be taken into account). No errors will be triggered in this case.
     * </p>
     * <p>
     * If the property is set, it must contains a list of existing directories or zip/jar
     * archives. One {@link DirectoryCrawler} instance will be set up for each
     * directory and one {@link ZipJarCrawler} instance (configured to look for the
     * archive in the filesystem) will be set up for each zip/jar archive. The list
     * elements in the java property are separated using the standard path separator for
     * the operating system as returned by {@link System#getProperty(String)
     * System.getProperty("path.separator")}. This standard path separator is ":" on
     * Linux and Unix type systems and ";" on Windows types systems.
     * </p>
     * @exception OrekitException if an element of the list does not exist or exists but
     * is neither a directory nor a zip/jar archive
     */
    public void addDefaultProviders() throws OrekitException {

        // get the path containing all components
        final String path = System.getProperty(OREKIT_DATA_PATH);
        if ((path != null) && !"".equals(path)) {

            // extract the various components
            for (final String name : path.split(System.getProperty("path.separator"))) {
                if (!"".equals(name)) {

                    final File file = new File(name);

                    // check component
                    if (!file.exists()) {
                        if (DataProvider.ZIP_ARCHIVE_PATTERN.matcher(name).matches()) {
                            throw new OrekitException("{0} does not exist in filesystem", name);
                        } else {
                            throw new OrekitException("data root directory {0} does not exist", name);
                        }
                    }

                    if (file.isDirectory()) {
                        addProvider(new DirectoryCrawler(file));
                    } else if (DataProvider.ZIP_ARCHIVE_PATTERN.matcher(name).matches()) {
                        addProvider(new ZipJarCrawler(file));
                    } else {
                        throw new OrekitException(NEITHER_DIRECTORY_NOR_ZIP_ARCHIVE, name);
                    }

                }
            }
        }

    }

    /** Add a data provider to the supported list.
     * @param provider data provider to add
     * @see #removeProvider(Class)
     * @see #clearProviders()
     * @see #isSupported(Class)
     * @see #getProviders()
     */
    public void addProvider(final DataProvider provider) {
        providers.add(provider);
    }

    /** Remove one provider.
     * <p>
     * The first supported provider extending the specified class (or implementing
     * the interface) is removed and returned. For example, removing the default
     * provider that loads data from files located somewhere in a directory hierarchy
     * can be done by calling:
     * <pre>
     *   DataProvidersManager.getInstance().remove(DataDirectoryCrawler.class);
     * </pre>
     * </p>
     * @param providerClass class (or one of the superclass's) of the provider to remove
     * @return instance removed (null if no provider of the given class was supported)
     * @see #addProvider(DataProvider)
     * @see #clearProviders()
     * @see #isSupported(Class)
     * @see #getProviders()
     */
    public DataProvider removeProvider(final Class<? extends DataProvider> providerClass) {
        for (final Iterator<DataProvider> iterator = providers.iterator(); iterator.hasNext();) {
            final DataProvider provider = iterator.next();
            if (providerClass.isInstance(provider)) {
                iterator.remove();
                return provider;
            }
        }
        return null;
    }

    /** Remove all data providers.
     * @see #addProvider(DataProvider)
     * @see #removeProvider(Class)
     * @see #isSupported(Class)
     * @see #getProviders()
     */
    public void clearProviders() {
        providers.clear();
    }

    /** Check if some type of provider is supported.
     * @param providerClass class (or one of the superclass's) of the provider to check
     * @return true if one provider of the given class is already in the supported list
     * @see #addProvider(DataProvider)
     * @see #removeProvider(Class)
     * @see #clearProviders()
     * @see #getProviders()
     */
    public boolean isSupported(final Class<? extends DataProvider> providerClass) {
        for (final Iterator<DataProvider> iterator = providers.iterator(); iterator.hasNext();) {
            final DataProvider provider = iterator.next();
            if (providerClass.isInstance(provider)) {
                return true;
            }
        }
        return false;
    }

    /** Get an unmodifiable view of the list of supported providers.
     * @return unmodifiable view of the list of supported providers
     * @see #addProvider(DataProvider)
     * @see #removeProvider(Class)
     * @see #clearProviders()
     * @see #isSupported(Class)
     */
    public List<DataProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    /** Feed a data file loader by browsing all data providers.
     * <p>
     * If this method is called with an empty list of providers, a default
     * providers configuration is set up. This default configuration contains
     * only one {@link DataProvider data provider}: a {@link DirectoryCrawler}
     * instance that loads data from files located somewhere in a directory hierarchy.
     * This default provider is <em>not</em> added if the list is not empty. If users
     * want to have both the default provider and other providers, they must add it
     * explicitly.
     * </p>
     * <p>
     * The providers are used in the order in which they were {@link #addProvider(DataProvider)
     * added}. As soon as one provider is able to feed the data loader, the loop is
     * stopped. If no provider is able to feed the data loader, then the last error
     * triggered is thrown.
     * </p>
     * @param supportedNames regular expression for file names supported by the visitor
     * @param loader data loader to use
     * @return true if some data has been loaded
     * @exception OrekitException if the data loader cannot be fed (read error ...)
     * or if the default configuration cannot be set up
     */
    public boolean feed(final String supportedNames, final DataLoader loader)
        throws OrekitException {

        final Pattern supported = Pattern.compile(supportedNames);

        // set up a default configuration if no providers have been set
        if (providers.isEmpty()) {
            addDefaultProviders();
        }

        // crawl the data collection
        OrekitException delayedException = null;
        for (final DataProvider provider : providers) {
            try {

                // try to feed the visitor using the current provider
                if (provider.feed(supported, loader)) {
                    return true;
                }

            } catch (OrekitException oe) {
                // remember the last error encountered
                delayedException = oe;
            }
        }

        if (delayedException != null) {
            throw delayedException;
        }

        return false;

    }

    /** Holder for the manager singleton.
     * <p>
     * We use the Initialization on demand holder idiom to store
     * the singletons, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.
     * </p>
     */
    private static class LazyHolder {

        /** Unique instance. */
        private static final DataProvidersManager INSTANCE = new DataProvidersManager();

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyHolder() {
        }

    }

}
