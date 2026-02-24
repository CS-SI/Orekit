/* Copyright 2002-2026 CS GROUP
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
/**
 * This package provides an implementation of the equations of Clohessy and Wiltshire.
 * <p>
 * Clohessy-Wiltshire equations are analytical equations for describing the relative motion of a chaser spacecraft
 * in regard to a target spacecraft when the target is on a circular orbit. These equations are only valid
 * under the assumption of Keplerian dynamics.
 * </p>
 * <p>
 * In this package:
 * </p>
 * <ul>
 *  <li>{@link org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireProvider ClohessyWiltshireProvider}
 *  implements AdditionalDataProvider to propagate the chaser state together with the target state as an AdditionalData.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireRendezVous ClohessyWilthireRendezVous}
 *  computes a TwoImpulseTransfer to realize a rendezvous with the target in a chosen duration.
 *  </li>
 * </ul>
 * <p>
 * All the references for the equations in the code come from : <br>
 * ORBITAL MECHANICS FOR ENGINEERING STUDENTS <br>
 * Howard. D. Curtis <br>
 * Elsevier Aerospace Engineering Series <br>
 * 2010.
 * </p>
 */
package org.orekit.propagation.relative.clohessywiltshire;
