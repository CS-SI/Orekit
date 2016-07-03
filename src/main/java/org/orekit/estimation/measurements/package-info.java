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
 * The measurements package defines everything that is related to orbit
 * determination measurements.
 * All measurements must implement the {@link
 * org.orekit.estimation.measurements.ObservedMeasurement} interface, which is the
 * public API that the engine will use to deal with all measurements. The
 * estimated theoretical values can be modified by registering one or several {@link
 * org.orekit.estimation.measurements.EstimationModifier} objects. These
 * objects will manage notions like tropospheric delays, biases, stations offsets ...
 * @since 8.0
 * @author Luc Maisonobe
 * @author Thierry Ceolin
 */
package org.orekit.estimation.measurements;
