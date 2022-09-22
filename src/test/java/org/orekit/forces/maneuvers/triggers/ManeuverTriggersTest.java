/* Copyright 2002-2022 CS GROUP
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
package org.orekit.forces.maneuvers.triggers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Decimal64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

import java.util.stream.Stream;

public class ManeuverTriggersTest {

    @Test
    public void testNoOpDefault() {
        // just test the default no-op implementation can be called without side effects
        ManeuverTriggers dummy = new ManeuverTriggers() {
            public <T extends CalculusFieldElement<T>> boolean isFiring(FieldAbsoluteDate<T> date, T[] parameters) {
                return false;
            }
            public boolean isFiring(AbsoluteDate date, double[] parameters) {
                return false;
            }
            public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(Field<T> field) {
                return null;
            }
            public Stream<EventDetector> getEventsDetectors() {
                 return null;
            }
        };

        SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7e6, 0.1, 0.2, 0.3, 0.4, 0.5,
                                                                       PositionAngle.MEAN, FramesFactory.getGCRF(),
                                                                       AbsoluteDate.J2000_EPOCH,
                                                                       Constants.EIGEN5C_EARTH_MU));
        dummy.init(state, state.getDate().shiftedBy(60));
        dummy.init(new FieldSpacecraftState<>(Decimal64Field.getInstance(), state),
                   new FieldAbsoluteDate<>(Decimal64Field.getInstance(), state.getDate().shiftedBy(60)));
        Assertions.assertEquals("", dummy.getName());

    }

}
