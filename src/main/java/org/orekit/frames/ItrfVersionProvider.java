package org.orekit.frames;

import org.orekit.frames.ITRFVersionLoader.ITRFVersionConfiguration;

/**
 * Interface for retrieving the ITRF version for a given set of EOP data.
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see ITRFVersionLoader
 * @since 10.1
 */
public interface ItrfVersionProvider {

    /**
     * Get the ITRF version configuration defined by a given file at specified date.
     *
     * @param name EOP file name
     * @param mjd  date of the EOP in modified Julian day
     * @return configuration valid around specified date in the file
     */
    ITRFVersionConfiguration getConfiguration(String name, int mjd);

}
