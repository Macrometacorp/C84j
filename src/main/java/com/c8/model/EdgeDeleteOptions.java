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

package com.c8.model;

/**
 * @author Mark Vollmary
 * 
 * @see <a href="https://docs.c8db.com/current/HTTP/Gharial/Edges.html#remove-an-edge">API Documentation</a>
 */
public class EdgeDeleteOptions {

	private Boolean waitForSync;
	private String ifMatch;

	public EdgeDeleteOptions() {
		super();
	}

	public Boolean getWaitForSync() {
		return waitForSync;
	}

	/**
	 * @param waitForSync
	 *            Wait until deletion operation has been synced to disk.
	 * @return options
	 */
	public EdgeDeleteOptions waitForSync(final Boolean waitForSync) {
		this.waitForSync = waitForSync;
		return this;
	}

	public String getIfMatch() {
		return ifMatch;
	}

	/**
	 * @param ifMatch
	 *            remove a document based on a target revision
	 * @return options
	 */
	public EdgeDeleteOptions ifMatch(final String ifMatch) {
		this.ifMatch = ifMatch;
		return this;
	}
}
