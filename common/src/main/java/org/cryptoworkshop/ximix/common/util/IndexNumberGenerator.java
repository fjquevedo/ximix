/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.common.util;

/**
 * General interface for the generation of streams of index numbers.
 */
public interface IndexNumberGenerator
{
    /**
     * Return true if there is another index number available
     *
     * @return true if another index number, false otherwise.
     */
    boolean hasNext();

    /**
     * Return the next index number
     *
     * @return an index number.
     */
    int nextIndex();
}
