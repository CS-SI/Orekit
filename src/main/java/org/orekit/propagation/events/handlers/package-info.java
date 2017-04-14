/* Copyright 2002-2017 CS Systèmes d'Information
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
/**
 *
 * This package provides an interface and classes dealing with events occurrence only.
 * <p>
 * It is mainly a trimmed-down version of {@link org.orekit.propagation.events.EventDetector
 * EventDetector} that allows to separate the handling of the event once it has been
 * detected from the prior detection itself.
 * </p>
 * <p>
 * A separate interface allows a simpler use of predefined events, as in this case user only
 * wants to specialize what to do once the event occurs and often does not want to change
 * the detection code. It also allows to share a handler amon several detectors.
 * </p>
 * @author Hank Grabowski
 * @since 6.1
 */
package org.orekit.propagation.events.handlers;
