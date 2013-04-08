package org.orekit.utils;

import java.util.List;

import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/**
 * Interface for a data structure that can provide concurrent access to
 * {@link TimeStamped} data surrounding a given date.
 * 
 * @author Luc Maisonobe
 * @author Evan Ward
 * @param <T> the type of data
 * @see GenericTimeStampedCache
 * @see ImmutableTimeStampedCache
 */
public interface TimeStampedCache<T extends TimeStamped> {

    /**
     * Get the entries surrounding a central date.
     * <p>
     * If the central date is well within covered range, the returned array will
     * be balanced with half the points before central date and half the points
     * after it (depending on n parity, of course). If the central date is near
     * the boundary, then the returned array will be unbalanced and will contain
     * only the n earliest (or latest) entries. A typical example of the later
     * case is leap seconds cache, since the number of leap seconds cannot be
     * arbitrarily increased.
     * <p>
     * This method is safe for multiple threads to execute concurrently.
     * 
     * @param central central date
     * @return list of cached entries surrounding the specified date. The size
     *         of the list is guaranteed to be {@link #getNeighborsSize()}.
     * @throws TimeStampedCacheException if {@code central} is outside the range
     *         of data this cache is capable of providing.
     */
    public List<T> getNeighbors(AbsoluteDate central)
        throws TimeStampedCacheException;

    /**
     * Get the fixed size of the lists returned by
     * {@link #getNeighbors(AbsoluteDate)}.
     * 
     * @return size of the list
     */
    public int getNeighborsSize();

    /**
     * Get the earliest entry in this cache.
     * 
     * @return earliest cached entry
     * @throws IllegalStateException if this cache is empty
     */
    public T getEarliest()
        throws IllegalStateException;

    /**
     * Get the latest entry in this cache.
     * 
     * @return latest cached entry
     * @throws IllegalStateException if this cache is empty
     */
    public T getLatest()
        throws IllegalStateException;

}
