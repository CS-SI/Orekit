package org.orekit.data;

/**
 * Holds a mutable static field since {@link DataContext} cannot.
 *
 * @author Evan Ward
 * @since 10.1
 */
class DefaultDataContextHolder {

    /** The default Orekit data context. */
    static LazyLoadedDataContext INSTANCE = new LazyLoadedDataContext();

    /** Private Constructor. */
    private DefaultDataContextHolder() {
    }

}
