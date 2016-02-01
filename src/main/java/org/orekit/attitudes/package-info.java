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
/**
 *
 * This package provides classes to represent simple attitudes.
 *
 * <p>
 * Some force models such as the atmospheric drag or the maneuvers need to
 * know the spacecraft orientation in inertial frame. OREKIT uses a simple
 * container for {@link org.orekit.attitudes.Attitude Attitude} which
 * includes both the geometric part (i.e. rotation) and the kinematic part
 * (i.e. the instant spin axis). The components hold by this container
 * allow to convert vectors from inertial frame to spacecraft frame along
 * with their derivatives. This container is similar in spirit to the various
 * extensions of the abstract {@link org.orekit.orbits.Orbit Orbit} class:
 * it represents a state at a specific instant.
 * </p>
 *
 * <p>
 * Several classical attitude laws are already provided in this package.
 * One special implementation is the {@link
 * org.orekit.attitudes.AttitudesSequence AttitudesSequence} class which
 * handle a set of laws, only one of which being active at any time. The
 * active law changes as switch events are triggered.
 * </p>
 *
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @author Véronique Pommier-Maurussane
 *
 */
package org.orekit.attitudes;
