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
package org.orekit.files.rinex.navigation.writers;

import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.time.TimeStamped;

import java.io.IOException;

/** Interface for navigation messages writers.
 * @param <T> type of the navigation messages this writer handles
 * @author Luc Maisonobe
 * @since 14.0
 */
public interface NavigationMessageWriter<T extends TimeStamped> {

    /** Write a navigation message.
     * @param identifier identifier
     * @param message navigation message to write
     * @param writer global file writer
     * @throws IOException if an I/O error occurs.
     */
    void writeMessage(final String identifier, final T message, RinexNavigationWriter writer) throws IOException;

}
