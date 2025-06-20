/* Copyright 2022-2025 Thales Alenia Space
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/** Cache for frame transforms.
 * <p>
 * This class is thread-safe.
 * </p>
 * @author Luc Maisonobe
 * @since 13.1
 */
class PeerCache {

    /** Origin frame. */
    private final Frame origin;

    /** Cache for transforms with peer frame. */
    private volatile CachedTransformProvider cache;

    /** Lock for peer frame cache. */
    private final ReentrantReadWriteLock lock;

    /** Cache for transforms with peer frame. */
    private volatile Map<Field<? extends CalculusFieldElement<?>>, FieldCachedTransformProvider<?>> fieldCaches;

    /** create an instance not associated with any peer.
     * @param origin origin frame
     */
    PeerCache(final Frame origin) {
        this.origin      = origin;
        this.cache       = null;
        this.fieldCaches = null;
        this.lock        = new ReentrantReadWriteLock();
    }

    /** Associate a cache with a peer frame, caching transforms.
     * <p>
     * The cache is a LRU cache (Least Recently Used), so entries remain in
     * the cache if they are used frequently, and only older entries
     * that have not been accessed for a while will be expunged.
     * </p>
     * <p>
     * If a peer was already associated with this frame, it will be overridden.
     * </p>
     * <p>
     * Peering is unidirectional, i.e. if frameA is peered with frameB,
     * then frameB may be peered with another frameC or no frame at all.
     * This allows several frames to be peered with a pivot one (typically
     * Earth frame and many topocentric frames all peered with one inertial frame).
     * </p>
     * @param peer peer frame (if null, cache is cleared)
     * @param cacheSize number of transforms kept in the date-based cache
     */
    public void setPeerCaching(final Frame peer, final int cacheSize) {

        lock.writeLock().lock();
        try {

            if (peer == null) {
                // clear peering
                cache       = null;
                fieldCaches = null;
            }

            // caching for regular dates
            cache = createCache(peer, cacheSize);

            // caching for field dates
            fieldCaches = new ConcurrentHashMap<>();
        } finally {
            lock.writeLock().unlock();
        }

    }

    /** Get the peer associated to this frame.
     * @return peer associated with this frame, null if not peered at all
     */
    Frame getPeer() {
        lock.readLock().lock();
        try {
            return cache == null ? null : cache.getDestination();
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Get the cached transform provider associated with this destination.
     * @param destination destination frame to which we want to transform vectors
     * @return cached transform provider, or null if destination is not the instance peer
     */
    CachedTransformProvider getCachedTransformProvider(final Frame destination) {
        lock.readLock().lock();
        try {
            if (cache == null || cache.getDestination() != destination) {
                return null;
            } else {
                return cache;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Get the cached transform provider associated with this destination.
     * @param <T> the type of the field elements
     * @param destination destination frame to which we want to transform vectors
     * @param field field elements belong to
     * @return cached transform provider, or null if destination is not the instance peer
     */
    <T extends CalculusFieldElement<T>> FieldCachedTransformProvider<T> getCachedTransformProvider(final Frame destination,
                                                                                                   final Field<T> field) {
        lock.readLock().lock();
        try {
            if (cache == null || cache.getDestination() != destination) {
                return null;
            } else {
                @SuppressWarnings("unchedked")
                final FieldCachedTransformProvider<T> tp =
                        (FieldCachedTransformProvider<T>) fieldCaches.computeIfAbsent(field,
                                                                                      f -> createCache(destination,
                                                                                                       cache.getCacheSize(),
                                                                                                       field));
                return tp;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Create cache.
     * @param peer peer frame
     * @param cacheSize number of transforms kept in the date-based cache
     * @return built cache
     * @since 13.0.3
     */
    private CachedTransformProvider createCache(final Frame peer, final int cacheSize) {
        final Function<AbsoluteDate, Transform> fullGenerator =
                date -> origin.getTransformTo(peer,
                                              Transform.IDENTITY,
                                              frame -> frame.getTransformProvider().getTransform(date),
                                              (t1, t2) -> new Transform(date, t1, t2),
                                              Transform::getInverse);
        final Function<AbsoluteDate, KinematicTransform> kinematicGenerator =
                date -> origin.getTransformTo(peer,
                                              KinematicTransform.getIdentity(),
                                              frame -> frame.getTransformProvider().getTransform(date),
                                              (t1, t2) -> KinematicTransform.compose(date, t1, t2),
                                              KinematicTransform::getInverse);
        final Function<AbsoluteDate, StaticTransform> staticGenerator =
                date -> origin.getTransformTo(peer,
                                              StaticTransform.getIdentity(),
                                              frame -> frame.getTransformProvider().getTransform(date),
                                              (t1, t2) -> StaticTransform.compose(date, t1, t2),
                                              StaticTransform::getInverse);
        return new CachedTransformProvider(origin, peer,
                                           fullGenerator, kinematicGenerator, staticGenerator,
                                           cacheSize);
    }

    /** Create field cache.
     * @param <T> type of the field elements
     * @param peer peer frame
     * @param cacheSize number of transforms kept in the date-based cache
     * @param field field elements belong to
     * @return built cache
     * @since 13.0.3
     */
    private <T extends CalculusFieldElement<T>> FieldCachedTransformProvider<T>
        createCache(final Frame peer, final int cacheSize, final Field<T> field) {
        final Function<FieldAbsoluteDate<T>, FieldTransform<T>> fullGenerator =
                d -> origin.getTransformTo(peer,
                                           FieldTransform.getIdentity(field),
                                           frame -> frame.getTransformProvider().getTransform(d),
                                           (FieldTransform<T> t1, FieldTransform<T> t2) -> new FieldTransform<>(d, t1, t2),
                                           FieldTransform::getInverse);
        final Function<FieldAbsoluteDate<T>, FieldKinematicTransform<T>> kinematicGenerator =
                d -> origin.getTransformTo(peer,
                                           FieldKinematicTransform.getIdentity(field),
                                           frame -> frame.getTransformProvider().getTransform(d),
                                           (t1, t2) -> FieldKinematicTransform.compose(d, t1, t2),
                                           FieldKinematicTransform::getInverse);
        final Function<FieldAbsoluteDate<T>, FieldStaticTransform<T>> staticGenerator =
                d -> origin.getTransformTo(peer,
                                           FieldStaticTransform.getIdentity(field),
                                           frame -> frame.getTransformProvider().getTransform(d),
                                           (t1, t2) -> FieldStaticTransform.compose(d, t1, t2),
                                           FieldStaticTransform::getInverse);
        return new FieldCachedTransformProvider<>(origin, peer,
                                                  fullGenerator, kinematicGenerator, staticGenerator,
                                                  cacheSize);
    }

}
