package fr.cs.aerospace.orekit.propagation;

import fr.cs.aerospace.orekit.time.AbsoluteDate;

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
    
    /** Get the start date of the range.
     * @return the start date of the range
     */
    AbsoluteDate getStartDate();
    
    /** Get the end date of the range.
     * @return the end date of the range
     */
    AbsoluteDate getEndDate();
    
}
