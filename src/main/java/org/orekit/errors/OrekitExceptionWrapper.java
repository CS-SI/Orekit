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
package org.orekit.errors;


/** This class allows to wrap {@link OrekitException} instances in {@code RuntimeException}.

 * <p>Wrapping {@link OrekitException} instances is useful when a low level method throws
 * one such exception and this method must be called from another one which does not allow
 * this exception. Typical examples are propagation methods that are used inside Apache
 * Commons optimizers, integrators or solvers.</p>
 *
 * @author Luc Maisonobe

 */
public class OrekitExceptionWrapper extends RuntimeException {

    /** serializable UID. */
    private static final long serialVersionUID = -2369002825757407992L;

    /** Underlying Orekit exception. */
    private final OrekitException wrappedException;

    /** Simple constructor.
     * @param wrappedException Orekit exception to wrap
     */
    public OrekitExceptionWrapper(final OrekitException wrappedException) {
        super(wrappedException);
        this.wrappedException = wrappedException;
    }

    /** Get the wrapped exception.
     * @return wrapped exception
     */
    public OrekitException getException() {
        return wrappedException;
    }

}
