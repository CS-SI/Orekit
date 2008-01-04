package fr.cs.orekit.propagation;

import fr.cs.orekit.time.AbsoluteDate;

/** This interface is intended for ephemerides valid only during a time range.
 *
 * <p>This interface provides a mean to retrieve orbital parameters at
 * any time within a given range. It should be implemented by orbit readers
 * based on external data files and by continuous models built after numerical
 * integration has been completed and dense output data as been
 * gathered.</p>
 *
 * @author L. Maisonobe
 *
 */
public interface BoundedEphemeris extends Ephemeris {

    /** Get the first date of the range.
     * @return the first date of the range
     */
    public AbsoluteDate getMinDate();

    /** Get the last date of the range.
     * @return the last date of the range
     */
    public AbsoluteDate getMaxDate();

}
