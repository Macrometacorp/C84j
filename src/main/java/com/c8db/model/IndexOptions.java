/*
 * DISCLAIMER
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.c8db.model;

/**
 *
 * This class is used for all index similarities
 */
public class IndexOptions {

    private Boolean inBackground;

    public IndexOptions() {
        super();
    }

    /**
     * @param inBackground create the the index in the background this is a RocksDB
     *                     only flag.
     * @return options
     */
    public IndexOptions inBackground(final Boolean inBackground) {
        this.inBackground = inBackground;
        return this;
    }

    public Boolean getInBackground() {
        return inBackground;
    }

}
