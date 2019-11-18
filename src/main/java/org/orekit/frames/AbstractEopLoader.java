package org.orekit.frames;

import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.time.TimeScale;

/**
 * Base class for EOP loaders.
 *
 * @author Evan Ward
 */
public class AbstractEopLoader extends AbstractSelfFeedingLoader {


    /** UTC time scale used for parsing files. */
    private final TimeScale utc;

    /**
     * Simple constructor.
     *
     * @param supportedNames regular expression for supported files names.
     * @param manager        provides access to the EOP files.
     * @param utc            UTC time scale.
     */
    public AbstractEopLoader(final String supportedNames,
                             final DataProvidersManager manager,
                             final TimeScale utc) {
        super(supportedNames, manager);
        this.utc = utc;
    }

    /**
     * Get the UTC time scale.
     *
     * @return UTC time scale.
     */
    protected TimeScale getUtc() {
        return utc;
    }

}
