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
 * This package provides an implementation of the equations of Yamanaka and Ankersen.
 * <p>
 * Yamanaka-Ankersen equations are analytical equations extending the equations of Clohessy and Wiltshire to
 * any eccentric orbits. These equations are only valid under the assumption of Keplerian dynamics.
 * </p>
 * <p>
 * In this package:
 * </p>
 * <ul>
 *  <li>{@link org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider YamanakaAnkersenProvider}
 *  implements AdditionalDataProvider to propagate the chaser state together with the target state as an AdditionalData.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenRendezVous YamanakaAnkersenRendezVous}
 *  computes a TwoImpulseTransfer to realize a rendezvous with the target in a chosen duration.
 *  </li>
 * </ul>
 * <p>
 * <p>
 * All the references for the equations in the code come from : <br>
 * NEW STATE TRANSITION MATRIX FOR RELATIVE MOTION ON AN ARBITRARY ELLIPTICAL ORBIT <br>
 * Koji Yamanaka, Finn Ankersen <br>
 * Journal of Guidance, Control, and Dynamics, Vol. 25, No. 1 <br>
 * January-February 2002.
 * </p>
 */
package org.orekit.propagation.relative.yamanakaankersen;
