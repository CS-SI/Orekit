<!--- Copyright 2002-2023 CS GROUP
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Custom filters

As explained on the [filtering](./filtering.html) page, the `FiltersManager.applyRelevantFilters`
method works by looping over all registered `DataFilter` instances and calling their `filter` method
with the current `DataSource`. If the `filter` method returns the _exact same instance_ that was passed
to it, it means the filter does not do anything. In this case, the `FiltersManager.applyRelevantFilters`
method just continues its loop and check the next filter. If the `filter` method returns a different
`DataSource` instance that was passed to it, it means the filter does indeed act on the data stream.
In this case, the `FiltersManager.applyRelevantFilters` method sets the current `DataSource` to the
returned value and restart its loop from the beginning.

This algorithm allows the same filter to be applied several time if needed, and it also allows
the filters to be applied in any order, regardless of the order in which they have been registered
to the `FiltersManager`.

Users may benefit from this general feature to add their own filters. One example could be
a deciphering algorithm if sensitive data should be stored enciphered and should be deciphered
on the fly when data is loaded.

## Implementing a filter

As per the way the `applyRelevantFilters` method works, the `filter` method must be implemented in such
a way that it should check the `DataSource` passed to it and return its parameter if it considers
it should not filter it, or return a new `DataSource` if it considers is should filter it.

There is one important caveat to understand when implementing custom filter: filters must _never_ open
the `DataSource` by themselves, regardless of the fact they will return the original instance or a filtered
instance when their `filter` method is called. The rationale is that it is the upper layer that will decide
to open (or not) the returned value and that a `DataSource` can be opened only once; this is the core
principle of lazy-opening provided by `DataSource`.

A consequence of this caveat is that a filter cannot peek on the few bytes of the data stream that is
referenced by a `DataSource`, for example in an attempt to look for a magic number in a header. This is
the reason why for example the `GzipFilter` looks for a `.gz` suffix in the name and does _not_
look for the `0x1f8B` magic number at file start.

As `applyRelevantFilters` restarts its loop from the beginning each time a filter is added to the stack,
some care must be taken to avoid stacking an infinite number of instances of the same filter on top
of each other. This means that the filtered `DataSource` returned after filtering should be recognized
as already filtered and not matched again by the same filter. If the check is based on file names
extensions (like `.gz` for gzip-compressed files), then if the original `DataSource` has a name of
the form `base.ext.gz` than the filtered file should have a name of the form `base.ext`. Another point
is that if a filters does not act on a `DataSource`, then it _must_ return the same instance that
was passed to it, it must _not_ simply create a transparent filter that just passes names and data
stream unchanged, otherwise it would be considered as a valid filter and added again and again
until either a stack overflow or memory exhaustion exception occurs.

The filtering part itself is implemented by opening the data stream from the underlying original
`DataSource`, reading raw data from it, performing the processing on these data (uncompressing,
deciphering, ...) and returning them as another stream.

The following example shows how to do that for a dummy deciphering algorithm based on a simple
XOR (this is a toy example only, not intended to be secure at all).

    public class XorFilter implements DataFilter {

        /** Suffix for XOR ciphered files. */
        private static final String SUFFIX = ".xor";

        /** Highly secret key. */
        private static final int key = 0x3b;

        /** {@inheritDoc} */
        @Override
        public DataSource filter(final DataSource original) {
            final String            oName   = original.getName();
            final DataSource.Opener oOpener = original.getOpener();
            if (oName.endsWith(SUFFIX)) {
                final String                  fName   = oName.substring(0, oName.length() - SUFFIX.length());
                final DataSource.StreamOpener fOpener = () -> new XORInputStream(oName, oOpener.openStreamOnce());
                return new DataSource(fName, fOpener);
            } else {
                return original;
            }
        }

        /** Filtering of XOR ciphered stream. */
        private static class XORInputStream extends InputStream {

            /** File name. */
            private final String name;

            /** Underlying compressed stream. */
            private final InputStream input;

            /** Indicator for end of input. */
            private boolean endOfInput;
 
            /** Simple constructor.
             * @param name file name
             * @param input underlying compressed stream
             */
            XORInputStream(final String name, final InputStream input) {
                this.name       = name;
                this.input      = input;
                this.endOfInput = false;
            }

            /** {@inheritDoc} */
            @Override
            public int read() throws IOException {

                if (endOfInput) {
                    // we have reached end of data
                    return -1;
                }

                final int raw = input.read();
                if (raw < 0) {
                  endOfInput = true;
                  return -1;
                } else {
                  return raw ^ key;
                }

            }

        }

    }
