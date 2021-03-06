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

package com.c8db.jackson.dataformat.velocypack;

import com.c8db.entity.BaseDocument;
import com.c8db.entity.BaseEdgeDocument;
import com.c8db.jackson.dataformat.velocypack.internal.VPackDeserializers;
import com.c8db.jackson.dataformat.velocypack.internal.VPackSerializers;
import com.arangodb.velocypack.VPackSlice;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 */
public class VPackMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    public VPackMapper() {
        super(new VPackFactory());
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        final SimpleModule module = new SimpleModule();
        module.addSerializer(VPackSlice.class, VPackSerializers.VPACK);
        module.addSerializer(java.util.Date.class, VPackSerializers.UTIL_DATE);
        module.addSerializer(java.sql.Date.class, VPackSerializers.SQL_DATE);
        module.addSerializer(java.sql.Timestamp.class, VPackSerializers.SQL_TIMESTAMP);
        module.addSerializer(BaseDocument.class, VPackSerializers.BASE_DOCUMENT);
        module.addSerializer(BaseEdgeDocument.class, VPackSerializers.BASE_EDGE_DOCUMENT);

        module.addDeserializer(VPackSlice.class, VPackDeserializers.VPACK);
        module.addDeserializer(java.util.Date.class, VPackDeserializers.UTIL_DATE);
        module.addDeserializer(java.sql.Date.class, VPackDeserializers.SQL_DATE);
        module.addDeserializer(java.sql.Timestamp.class, VPackDeserializers.SQL_TIMESTAMP);
        module.addDeserializer(BaseDocument.class, VPackDeserializers.BASE_DOCUMENT);
        module.addDeserializer(BaseEdgeDocument.class, VPackDeserializers.BASE_EDGE_DOCUMENT);
        registerModule(module);
    }

}
