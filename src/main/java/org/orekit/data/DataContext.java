package org.orekit.data;

import org.orekit.frames.Frames;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeScalesFactory;

/**
 * Provides auxiliary data for portions of the application.
 *
 * @author Evan Ward
 * @since 10.1
 */
public interface DataContext {

    /**
     * Get the default data context that is used to implement the static factories: {@link
     * TimeScalesFactory}.
     *
     * @return Orekit's default data context.
     */
    static LazyLoadedDataContext getDefault() {
        return DefaultDataContextHolder.INSTANCE;
    }

    /**
     * Set the default data context that is used to implement Orekit's static factories.
     *
     * <p> Calling this method will not modify any instances already retrieved from
     * Orekit's static factories. In general this method should only be called at
     * application start up before any of the static factories are used.
     *
     * @param context the new data context.
     * @see #getDefault()
     */
    static void setDefault(final LazyLoadedDataContext context) {
        DefaultDataContextHolder.INSTANCE = context;
    }

    /**
     * Get a factory for constructing {@link TimeScale}s based on the auxiliary data in
     * this context.
     *
     * @return the set of common time scales using this data context.
     */
    TimeScales getTimeScales();

    /**
     * Get a factory constructing {@link Frame}s based on the auxiliary data in this
     * context.
     *
     * @return the set of common reference frames using this data context.
     */
    Frames getFrames();

}
