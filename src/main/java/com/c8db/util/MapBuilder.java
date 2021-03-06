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

package com.c8db.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class MapBuilder {

    private final Map<String, Object> map;

    public MapBuilder() {
        super();
        map = new LinkedHashMap<String, Object>();
    }

    public MapBuilder put(final String key, final Object value) {
        map.put(key, value);
        return this;
    }

    public Map<String, Object> get() {
        return map;
    }
}
