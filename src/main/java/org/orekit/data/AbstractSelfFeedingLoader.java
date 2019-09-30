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
    private final String supportedNames;
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
        return manager.feed(supportedNames, loader);
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
     * Get the data provider manager.
     *
     * @return the source of auxiliary data files.
     */
    protected DataProvidersManager getDataProvidersManager() {
        return manager;
    }

}
