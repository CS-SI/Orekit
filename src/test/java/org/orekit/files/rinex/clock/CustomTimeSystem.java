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
package org.orekit.files.rinex.clock;

import org.orekit.gnss.TimeSystem;
import org.orekit.time.ConstantOffsetTimeScale;
import org.orekit.time.TimeOffset;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

class CustomTimeSystem implements TimeSystem {

    private final String key;

    CustomTimeSystem(final String key) {
        this.key = key;
    }

    @Override
    public boolean equals(final Object type) {
        if (type instanceof CustomTimeSystem) {
            return key.equals(((CustomTimeSystem) type).key);
        }
        return false;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getTwoLettersCode() {
        return key.substring(0, 2);
    }

    @Override
    public String getOneLetterCode() {
        return key.substring(0, 1);
    }

    @Override
    public TimeScale getTimeScale(final TimeScales timeScales) {
        return new ConstantOffsetTimeScale(key, new TimeOffset(179L, TimeOffset.MILLISECOND));
    }

}
