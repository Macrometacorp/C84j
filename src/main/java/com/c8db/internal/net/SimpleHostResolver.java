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

package com.c8db.internal.net;

import java.util.List;

import com.c8db.internal.C8ExecutorSync;
import com.c8db.util.C8Serialization;

/**
 *
 */
public class SimpleHostResolver implements HostResolver {

    private final List<Host> hosts;

    public SimpleHostResolver(final List<Host> hosts) {
        super();
        this.hosts = hosts;
    }

    @Override
    public void init(C8ExecutorSync executor, C8Serialization arangoSerialization) {

    }

    @Override
    public HostSet resolve(final boolean initial, final boolean closeConnections) {
        return new HostSet(hosts);
    }

}