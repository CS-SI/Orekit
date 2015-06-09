/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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
 * The leastsquares package provides an implementation of a batch least
 * squares estimator engine to perform an orbit determination.
 * Users will typically create one instance of this object, register all
 * observation data as {@link org.orekit.estimation.measurements.Measurement
 * measurements} with their included {@link
 * org.orekit.estimation.measurements.MeasurementModifier modifiers}, and
 * run the {@link org.orekit.estimation.leastsquares.BatchLSEstimator least
 * squares estimator}. At the end of the process, the orbital state and the
 * estimated parameters will be available.
 * @since 7.1
 * @author Luc Maisonobe
 * @author Thierry Ceolin
 */
package org.orekit.estimation.leastsquares;
