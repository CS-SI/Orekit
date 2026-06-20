/* Copyright 2002-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field can be {@code null}.
 *
 * <p>The purpose of this annotation is to avoid using {@link java.util.Optional} on class fields and to provide
 * a clear indication that the field can be {@code null}.
 * </p>
 * <p>Using <code>Optional</code> on class fields is not recommended:
 * <ul>
 *     <li>Java architecture intent: <code>Optional</code> has been initially introduced to provide a clear
 *     return type of methods that might not return a value</li>
 *     <li>Memory Overhead: <code>Optional</code> is a wrapper, meaning that it stores both the Optional
 *     and the reference to the value are stored in memory</li>
 *     <li>Serialization: <code>Optional</code> is not serializable</li>
 * </ul>
 * An example of the annotation us is presented below:
 * </p>
 * <p>Non-compliant code</p>
 * <pre>
 * {@code
 * private Optional<String> name;
 *
 * public Optional<String> getName() {
 *     return name;
 * }
 *
 * public void setName(final String name) {
 *     this.name = Optional.ofNullable(name);
 * }
 * }
 * </pre>
 *
 * <p>Compliant code</p>
 * <pre>
 * {@code
 * private @Nullable String name;
 *
 * public Optional<String> getName() {
 *     return Optional.ofNullable(name);
 * }
 *
 * public void setName(final String name) {
 *     this.name = name;
 * }
 * }
 * </pre>
 * @author Bryan Cazabonne
 * @since 14.0
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface Nullable {
}
