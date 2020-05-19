/* Contributed in the public domain.
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
package org.orekit.data;

/**
 * Holds a mutable static field since {@link DataContext} cannot.
 *
 * @author Evan Ward
 * @since 10.1
 */
class DefaultDataContextHolder {

    /** The default Orekit data context. */
    private static volatile LazyLoadedDataContext INSTANCE = new LazyLoadedDataContext();

    /** Private Constructor. */
    private DefaultDataContextHolder() {
    }

    /** Get the default Orekit data context.
     * @return Orekit's default data context.
     */
    static LazyLoadedDataContext getInstance() {
        return INSTANCE;
    }

    /** Set the default Orekit data context.
     * @param context the new data context.
     */
    static void setInstance(final LazyLoadedDataContext context) {
        INSTANCE = context;
    }

}
