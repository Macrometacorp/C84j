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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.arangodb.velocypack.VPackSlice;
import com.c8db.entity.EdgeDefinition;
import com.c8db.entity.Permissions;
import com.c8db.entity.UserQueryOptions;

/**
 *
 */
public class OptionsBuilder {

    private OptionsBuilder() {
        super();
    }

    public static UserCreateOptions build(final UserCreateOptions options, final String user, final String passwd) {
        return options.user(user).passwd(passwd);
    }

    public static HashIndexOptions build(final HashIndexOptions options, final Iterable<String> fields) {
        return options.fields(fields);
    }

    public static SkiplistIndexOptions build(final SkiplistIndexOptions options, final Iterable<String> fields) {
        return options.fields(fields);
    }

    public static PersistentIndexOptions build(final PersistentIndexOptions options, final Iterable<String> fields) {
        return options.fields(fields);
    }

    public static GeoIndexOptions build(final GeoIndexOptions options, final Iterable<String> fields) {
        return options.fields(fields);
    }

    public static FulltextIndexOptions build(final FulltextIndexOptions options, final Iterable<String> fields) {
        return options.fields(fields);
    }

    public static CollectionCreateOptions build(final CollectionCreateOptions options, final String name) {
        return options.name(name);
    }

    public static C8qlQueryOptions build(final C8qlQueryOptions options, final String query, final VPackSlice bindVars) {
        return options.query(query).bindVars(bindVars);
    }

    public static C8qlQueryExplainOptions build(final C8qlQueryExplainOptions options, final String query,
            final VPackSlice bindVars) {
        return options.query(query).bindVars(bindVars);
    }

    public static C8qlQueryParseOptions build(final C8qlQueryParseOptions options, final String query) {
        return options.query(query);
    }

    public static GraphCreateOptions build(final GraphCreateOptions options, final String name,
            final Collection<EdgeDefinition> edgeDefinitions) {
        return options.name(name).edgeDefinitions(edgeDefinitions);
    }

    public static C8TransactionOptions build(final C8TransactionOptions options, final String action) {
        return options.action(action);
    }

    public static CollectionRenameOptions build(final CollectionRenameOptions options, final String name) {
        return options.name(name);
    }

    public static DBCreateOptions build(final DBCreateOptions options, final String name, final String spotDc,
            final String dcList) {
        return options.name(name).options(spotDc, dcList);
    }

    public static DCListOptions build(final DCListOptions options, final String dcList) {
        return options.dcList(dcList);
    }

    public static UserAccessOptions build(final UserAccessOptions options, final Permissions grant) {
        return options.grant(grant);
    }

    public static VertexCollectionCreateOptions build(final VertexCollectionCreateOptions options,
            final String collection) {
        return options.collection(collection);
    }

    public static UserQueryOptions build(final UserQueryOptions options, final String name) {
        if (options.getParameter() == null) 
            options.parameter(new HashMap<String,Object> ());
        return options.name(name);
    }
    
    public static EventCreateOptions build(final EventCreateOptions options, final String action,
            final String description, final String entity, final String entityType, final String status,
            final String details, final Map<String, String> attributes) {
        return options.description(description).action(action).attributes(attributes).details(details)
                .entityType(entityType).status(status);
    }
}
