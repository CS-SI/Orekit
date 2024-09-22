/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.files.sinex;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser for SINEX block.
 * @param <T> type of the sinex files parse info
 * @author Luc Maisonobe
 * @since 13.0
 */
class BlockParser<T extends ParseInfo<?>> implements LineParser<T> {

    /** Start block pattern. */
    private final Pattern startPattern;

    /** End block pattern. */
    private Pattern endPattern;

    /** Allowed parsers when dealing within this block (content + end marker). */
    private final List<LineParser<T>> inBlockParsers;

    /** Allowed parsers when leaving this block. */
    private List<LineParser<T>> siblingParsers;

    /** Simple constructor.
     * @param blockId    regular expression for block name
     * @param predicates predicates for parsing block content lines
     */
    protected BlockParser(final String blockId, final List<Predicate<T>> predicates) {
        this.startPattern   = Pattern.compile("^\\+(" + blockId + ") *$");
        this.endPattern     = null;
        this.inBlockParsers = new ArrayList<>(1 + predicates.size());
        for (final Predicate<T> predicate : predicates) {
            inBlockParsers.add(new LineParser<T>() {

                /** {@inheritDoc} */
                @Override
                public boolean parseIfRecognized(final T parseInfo) {
                    return predicate.test(parseInfo);
                }

                /** {@inheritDoc} */
                @Override
                public Iterable<LineParser<T>> allowedNextParsers(final T parseInfo) {
                    return inBlockParsers;
                }

            });
        }
        this.inBlockParsers.add(this);
    }

    /** Set allowed parsers when leaving this block.
     * @param siblingParsers allowed parsers when leaving this block
     */
    public void setSiblingParsers(final List<LineParser<T>> siblingParsers) {
        this.siblingParsers = siblingParsers;
    }

    /** {@inheritDoc} */
    @Override
    public boolean parseIfRecognized(final T parseInfo) {
        return outsideBlock() ? checkEntering(parseInfo) : checkLeaving(parseInfo);
    }

    /** {@inheritDoc} */
    @Override
    public Iterable<LineParser<T>> allowedNextParsers(final T parseInfo) {
        return endPattern == null  ? siblingParsers : inBlockParsers;
    }

    /** Check if we are outside block.
     * @return true if we are at block end
     */
    protected boolean outsideBlock() {
        return endPattern == null;
    }

    /** Check if we are at the start marker.
     * @param parseInfo holder for transient data
     * @return true if we are at block start
     */
    protected boolean checkEntering(final T parseInfo) {
        final Matcher matcher = startPattern.matcher(parseInfo.getLine());
        if (matcher.matches()) {
            // we are entering the block
            endPattern = Pattern.compile("^-" + matcher.group(1) + " *$");
            return true;
        } else {
            return false;
        }
    }

    /** Check if we are at the end marker.
     * @param parseInfo holder for transient data
     * @return true if we are at block end
     */
    protected boolean checkLeaving(final T parseInfo) {
        if (endPattern != null && endPattern.matcher(parseInfo.getLine()).matches()) {
            endPattern = null;
            return true;
        } else {
            return false;
        }
    }

}

