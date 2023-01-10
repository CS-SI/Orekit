/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * The sequential package provides an implementation of a
 * Kalman Filter engine to perform an orbit determination.
 * <p>
 * Users will typically create one instance of this object, register all
 * observation data as {@link org.orekit.estimation.measurements.ObservedMeasurement
 * measurements} with their included {@link
 * org.orekit.estimation.measurements.EstimationModifier modifiers}, and
 * run the {@link org.orekit.estimation.sequential.KalmanEstimator kalman
 * filter}. For each processed measurement, a fully configured propagator
 * will be available, as well as all estimated parameters individually.
 * </p>
 */
package org.orekit.estimation.sequential;
