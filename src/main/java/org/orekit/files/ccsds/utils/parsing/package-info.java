/* Copyright 2002-2023 CS GROUP
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
 *
 * This package contains classes related to the processing
 * of parse tokens using the state design pattern.
 * <p>
 * The base abstract class {@link org.orekit.files.ccsds.utils.parsing.AbstractConstituentParser
 * AbstractMessageParser} implements the {@link
 * org.orekit.files.ccsds.utils.lexical.MessageParser MessageParser} interface using
 * the state design pattern, where each {@link
 * org.orekit.files.ccsds.utils.parsing.ProcessingState processing state} is devoted
 * to analyze one section or sub-section of a CCSDS message (like header, metadata,
 * data or even smaller parts like logical blocks inside data).
 * </p>
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
package org.orekit.files.ccsds.utils.parsing;
