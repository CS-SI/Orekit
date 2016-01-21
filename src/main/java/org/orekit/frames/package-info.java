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
 * This package provides classes to handle frames and transforms between them.
 *
 * <p>
 * The {@link org.orekit.frames.Transform} class represents a full transform:
 * combined rotation and translation, and their first time derivatives to handle kinematics.
 * </p>
 *
 * <p>
 * Each {@link org.orekit.frames.Frame} is defined by a transform linking it to
 * another one, called its parent frame. The only exception is the root frame which has
 * no parent. This implies that all frames are naturally organized as a tree with a single
 * root. The predefined GCRF inertial frame was arbitrary chosen as the root for every tree.
 * </p>
 *
 * <p>
 * The {@link org.orekit.frames.FramesFactory} class implements several predefined reference
 * frames. One set correspond to the frames from the various IERS conventions (ITRF and others).
 * Other frames not belonging to the previous set are the EME2000 frame that was used prior
 * to GCRF and which is linked to GCRF by a simple bias rotation, the MOD (Mean Of Date) frame
 * which involves the IAU 1976 precession model, the TOD (True Of Date) frame which involves
 * the IAU 1980 nutation model, the GTOD (Greenwich True Of Date) which involves the IAU 1982
 * Greenwich sidereal time model and the Veis 1950 frame which involves a Veis modified sidereal
 * time model.
 * </p>
 *
 * <p>
 * Some other frames are predefined outside of this package, in the {@link
 * org.orekit.bodies.CelestialBodyFactory} class. They correspond to the Sun, Moon, planets, solar
 * system barycenter and Earth-Moon barycenter. For convenience, the very important solar system
 * barycenter frame, which is the ICRF, can also be retrieved from the factory in this package
 * even if it is really implemented in the bodies package.
 * </p>
 *
 * <p>
 * The frames can be time dependent (for example the ITRF frame depends on time due to
 * precession/nutation, Earth rotation and pole motion). In order to get a transform from one
 * frame to another one, the date must be specified, and {@link
 * org.orekit.frames.TransformProvider#getTransform(AbsoluteDate)} is called under the hood.
 * If a user wants to implement his own date synchronized frame, he has to  implement his
 * own {@link org.orekit.frames.TransformProvider} class and provide it to the frame constructor.
 * </p>
 *
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author Véronique Pommier-Maurussane
 * @author Pascal Parraud
 *
 */
package org.orekit.frames;
