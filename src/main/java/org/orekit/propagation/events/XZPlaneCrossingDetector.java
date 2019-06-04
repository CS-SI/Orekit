package org.orekit.propagation.events;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

public class XZPlaneCrossingDetector
    extends
    AbstractDetector<XZPlaneCrossingDetector> {
   
    public XZPlaneCrossingDetector(final double maxCheck, final double threshold) {
        this(maxCheck, threshold, DEFAULT_MAX_ITER,
             new StopOnIncreasing<XZPlaneCrossingDetector>());
    }
    
    private XZPlaneCrossingDetector(final double maxCheck, final double threshold,
                             final int maxIter,
                             final EventHandler<? super XZPlaneCrossingDetector> handler) {
        super(maxCheck, threshold, maxIter, handler);
    }

    /**
     * Build a new instance.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval
     * to period/3
     * </p>
     * 
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     */


    /**
     * Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder API
     * with the various {@code withXxx()} methods to set up the instance in a
     * readable manner without using a huge amount of parameters.
     * </p>
     * 
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @since 6.1
     */
    
    protected XZPlaneCrossingDetector
        create(final double newMaxCheck, final double newThreshold,
               final int newMaxIter,
               final EventHandler<? super XZPlaneCrossingDetector> newHandler) {
        return new XZPlaneCrossingDetector(newMaxCheck, newThreshold, newMaxIter,
                                    newHandler	);
    }

    public double g(final SpacecraftState s) {
        return s.getPVCoordinates().getPosition().getY();

    }
}
