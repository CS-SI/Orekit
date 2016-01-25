/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.events.handlers;

import org.junit.Assert;
import org.junit.Test;

public class EventHandlerTest {

    @Test
    public void testEnums() {
        // this test is here only for test coverage ...

        Assert.assertEquals(4, EventHandler.Action.values().length);
        Assert.assertSame(EventHandler.Action.STOP,              EventHandler.Action.valueOf("STOP"));
        Assert.assertSame(EventHandler.Action.RESET_STATE,       EventHandler.Action.valueOf("RESET_STATE"));
        Assert.assertSame(EventHandler.Action.RESET_DERIVATIVES, EventHandler.Action.valueOf("RESET_DERIVATIVES"));
        Assert.assertSame(EventHandler.Action.CONTINUE,          EventHandler.Action.valueOf("CONTINUE"));

    }

}

