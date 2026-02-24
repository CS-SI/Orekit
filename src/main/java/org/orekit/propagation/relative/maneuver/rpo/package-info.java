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
 * Package rpo provides classes to compute typical maneuvers of relative proximity operations.
 * <p>
 *
 * </p>
 * <p>
 * In this package:
 * </p>
 * <ul>
 *  <li>{@link org.orekit.propagation.relative.maneuver.rpo.RPO RPO}
 *  Interface for rpo maneuvers shared by the models. Define the methods to compute the waypoints for Linear trajectory,
 *  Forced Circular trajectory, Natural elliptic circumnavigation and natural circular
 *  circumnavigation for circular orbits.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.maneuver.rpo.RPOModel RPOModel}
 *  Enum that implements RPO interface. Define the vectors of the local orbital frame relative to the desired model.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.maneuver.rpo.MultiRelativeTransfer MultiRelativeTransfer}
 *  Interface for MultiRelativeTransfers.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.maneuver.rpo.AbstractMultiRelativeTransfers AbstractMultiRelativeTransfers}
 *  Abstraction of MultiRelativeTransfers.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.maneuver.rpo.MultiRelativeTransfersCW MultiRelativeTransfersCW}
 *  Compute the relative transfers to realize the trajectory defined by the waypoints using Clohessy-Wiltshire model.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.maneuver.rpo.MultiRelativeTransfersYA MultiRelativeTransfersYA}
 *  Compute the relative transfers to realize the trajectory defined by the waypoints using Yamanaka-Ankersen model.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.maneuver.rpo.EllipticNCO EllipticNCO}
 *  This class computes and stores a co-elliptic Keplerian orbit to realize a natural circumnavigation around a target in
 *  any eccentric orbits.
 *  </li>
 *  <li>{@link org.orekit.propagation.relative.maneuver.rpo.TeardropCircularWaypointCalculator TeardropCircularWaypointCalculator}
 *  Computes the waypoints to realize a teardrop maneuver when the target is in a circular orbit.
 *  </li>
 * </ul>
 * <p>
 */
package org.orekit.propagation.relative.maneuver.rpo;
