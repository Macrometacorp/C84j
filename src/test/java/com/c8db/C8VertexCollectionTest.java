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

package com.c8db;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import com.c8db.C8DB.Builder;
import com.c8db.entity.BaseDocument;
import com.c8db.entity.VertexEntity;
import com.c8db.entity.VertexUpdateEntity;
import com.c8db.model.GraphCreateOptions;
import com.c8db.model.GraphDocumentReadOptions;
import com.c8db.model.SkiplistIndexOptions;
import com.c8db.model.VertexDeleteOptions;
import com.c8db.model.VertexReplaceOptions;
import com.c8db.model.VertexUpdateOptions;

/**
 * 
 */
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class C8VertexCollectionTest extends BaseTest {

    private static final String GRAPH_NAME = "db_collection_test";
    private static final String COLLECTION_NAME = "db_vertex_collection_test";

    public C8VertexCollectionTest(final Builder builder) {
        super(builder);
        setup();
    }

    public void setup() {
        try {
            db.createCollection(COLLECTION_NAME, null);
        } catch (final C8DBException e) {
        }
        final GraphCreateOptions options = new GraphCreateOptions().orphanCollections(COLLECTION_NAME);
        try {
            db.createGraph(GRAPH_NAME, null, options);
        } catch (final C8DBException e) {
        }

    }

    @After
    public void teardown() {
        try {
            db.graph(GRAPH_NAME).drop();
        } catch (final C8DBException e) {
        }
        try {
            db.collection(COLLECTION_NAME).drop();
        } catch (final C8DBException e) {
        }

    }

    @Test
    public void dropVertexCollection() {
        db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).drop();
        final Collection<String> vertexCollections = db.graph(GRAPH_NAME).getVertexCollections();
        assertThat(vertexCollections, not(hasItem(COLLECTION_NAME)));
    }

    @Test
    public void insertVertex() {
        final VertexEntity vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .insertVertex(new BaseDocument(), null);
        assertThat(vertex, is(notNullValue()));
        final BaseDocument document = db.collection(COLLECTION_NAME).getDocument(vertex.getKey(), BaseDocument.class,
                null);
        assertThat(document, is(notNullValue()));
        assertThat(document.getKey(), is(vertex.getKey()));
    }

    @Test
    public void insertVertexViolatingUniqueConstraint() {
        // FIXME: remove once fix is backported to 3.4
        assumeTrue(requireVersion(3, 5));

        db.collection(COLLECTION_NAME).ensureSkiplistIndex(Collections.singletonList("properties"),
                new SkiplistIndexOptions().unique(true));

        db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(new BaseDocument(), null);

        try {
            db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(new BaseDocument(), null);
        } catch (C8DBException e) {
            assertThat(e.getResponseCode(), is(409));
            assertThat(e.getErrorNum(), is(1210));
        }
    }

    @Test
    public void duplicateInsertSameObjectVertex() {

        final C8VertexCollection vertexCollection = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME);

        // #########################################################
        // Create a new BaseDocument
        // #########################################################

        UUID uuid = UUID.randomUUID();
        BaseDocument bd = new BaseDocument();
        bd.setKey(uuid.toString());
        bd.addAttribute("name", "Paul");

        vertexCollection.insertVertex(bd);

        UUID uuid2 = UUID.randomUUID();
        BaseDocument bd2 = new BaseDocument();
        bd2.setKey(uuid2.toString());
        bd2.addAttribute("name", "Paul");

        vertexCollection.insertVertex(bd2);
    }

    @Test
    public void insertVertexUpdateRev() {
        final BaseDocument doc = new BaseDocument();
        final VertexEntity vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc, null);
        assertThat(doc.getRevision(), is(vertex.getRev()));
    }

    @Test
    public void getVertex() {
        final VertexEntity vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .insertVertex(new BaseDocument(), null);
        final BaseDocument document = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).getVertex(vertex.getKey(),
                BaseDocument.class, null);
        assertThat(document, is(notNullValue()));
        assertThat(document.getKey(), is(vertex.getKey()));
    }

    @Test
    public void getVertexIfMatch() {
        final VertexEntity vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .insertVertex(new BaseDocument(), null);
        final GraphDocumentReadOptions options = new GraphDocumentReadOptions().ifMatch(vertex.getRev());
        final BaseDocument document = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).getVertex(vertex.getKey(),
                BaseDocument.class, options);
        assertThat(document, is(notNullValue()));
        assertThat(document.getKey(), is(vertex.getKey()));
    }

    @Test
    public void getVertexIfMatchFail() {
        final VertexEntity vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .insertVertex(new BaseDocument(), null);
        final GraphDocumentReadOptions options = new GraphDocumentReadOptions().ifMatch("no");
        final BaseDocument vertex2 = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).getVertex(vertex.getKey(),
                BaseDocument.class, options);
        assertThat(vertex2, is(nullValue()));
    }

    @Test
    public void getVertexIfNoneMatch() {
        final VertexEntity vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .insertVertex(new BaseDocument(), null);
        final GraphDocumentReadOptions options = new GraphDocumentReadOptions().ifNoneMatch("no");
        final BaseDocument document = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).getVertex(vertex.getKey(),
                BaseDocument.class, options);
        assertThat(document, is(notNullValue()));
        assertThat(document.getKey(), is(vertex.getKey()));
    }

    @Test
    public void getVertexIfNoneMatchFail() {
        final VertexEntity vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .insertVertex(new BaseDocument(), null);
        final GraphDocumentReadOptions options = new GraphDocumentReadOptions().ifNoneMatch(vertex.getRev());
        final BaseDocument vertex2 = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).getVertex(vertex.getKey(),
                BaseDocument.class, options);
        assertThat(vertex2, is(nullValue()));
    }

    @Test
    public void replaceVertex() {
        final BaseDocument doc = new BaseDocument();
        doc.addAttribute("a", "test");
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        doc.getProperties().clear();
        doc.addAttribute("b", "test");
        final VertexUpdateEntity replaceResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .replaceVertex(createResult.getKey(), doc, null);
        assertThat(replaceResult, is(notNullValue()));
        assertThat(replaceResult.getId(), is(createResult.getId()));
        assertThat(replaceResult.getRev(), is(not(replaceResult.getOldRev())));
        assertThat(replaceResult.getOldRev(), is(createResult.getRev()));

        final BaseDocument readResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .getVertex(createResult.getKey(), BaseDocument.class, null);
        assertThat(readResult.getKey(), is(createResult.getKey()));
        assertThat(readResult.getRevision(), is(replaceResult.getRev()));
        assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
        assertThat(readResult.getAttribute("b"), is(notNullValue()));
        assertThat(String.valueOf(readResult.getAttribute("b")), is("test"));
    }

    @Test
    public void replaceVertexUpdateRev() {
        final BaseDocument doc = new BaseDocument();
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        assertThat(doc.getRevision(), is(createResult.getRev()));
        final VertexUpdateEntity replaceResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .replaceVertex(createResult.getKey(), doc, null);
        assertThat(doc.getRevision(), is(replaceResult.getRev()));
    }

    @Test
    public void replaceVertexIfMatch() {
        final BaseDocument doc = new BaseDocument();
        doc.addAttribute("a", "test");
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        doc.getProperties().clear();
        doc.addAttribute("b", "test");
        final VertexReplaceOptions options = new VertexReplaceOptions().ifMatch(createResult.getRev());
        final VertexUpdateEntity replaceResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .replaceVertex(createResult.getKey(), doc, options);
        assertThat(replaceResult, is(notNullValue()));
        assertThat(replaceResult.getId(), is(createResult.getId()));
        assertThat(replaceResult.getRev(), is(not(replaceResult.getOldRev())));
        assertThat(replaceResult.getOldRev(), is(createResult.getRev()));

        final BaseDocument readResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .getVertex(createResult.getKey(), BaseDocument.class, null);
        assertThat(readResult.getKey(), is(createResult.getKey()));
        assertThat(readResult.getRevision(), is(replaceResult.getRev()));
        assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
        assertThat(readResult.getAttribute("b"), is(notNullValue()));
        assertThat(String.valueOf(readResult.getAttribute("b")), is("test"));
    }

    @Test
    public void replaceVertexIfMatchFail() {
        final BaseDocument doc = new BaseDocument();
        doc.addAttribute("a", "test");
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        doc.getProperties().clear();
        doc.addAttribute("b", "test");
        try {
            final VertexReplaceOptions options = new VertexReplaceOptions().ifMatch("no");
            db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).replaceVertex(createResult.getKey(), doc, options);
            fail();
        } catch (final C8DBException e) {
        }
    }

    @Test
    public void updateVertex() {
        final BaseDocument doc = new BaseDocument();
        doc.addAttribute("a", "test");
        doc.addAttribute("c", "test");
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        doc.updateAttribute("a", "test1");
        doc.addAttribute("b", "test");
        doc.updateAttribute("c", null);
        final VertexUpdateEntity updateResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .updateVertex(createResult.getKey(), doc, null);
        assertThat(updateResult, is(notNullValue()));
        assertThat(updateResult.getId(), is(createResult.getId()));
        assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
        assertThat(updateResult.getOldRev(), is(createResult.getRev()));

        final BaseDocument readResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .getVertex(createResult.getKey(), BaseDocument.class, null);
        assertThat(readResult.getKey(), is(createResult.getKey()));
        assertThat(readResult.getAttribute("a"), is(notNullValue()));
        assertThat(String.valueOf(readResult.getAttribute("a")), is("test1"));
        assertThat(readResult.getAttribute("b"), is(notNullValue()));
        assertThat(String.valueOf(readResult.getAttribute("b")), is("test"));
        assertThat(readResult.getRevision(), is(updateResult.getRev()));
        assertThat(readResult.getProperties().keySet(), hasItem("c"));
    }

    @Test
    public void updateVertexUpdateRev() {
        final BaseDocument doc = new BaseDocument();
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        assertThat(doc.getRevision(), is(createResult.getRev()));
        final VertexUpdateEntity updateResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .updateVertex(createResult.getKey(), doc, null);
        assertThat(doc.getRevision(), is(updateResult.getRev()));
    }

    @Test
    public void updateVertexIfMatch() {
        final BaseDocument doc = new BaseDocument();
        doc.addAttribute("a", "test");
        doc.addAttribute("c", "test");
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        doc.updateAttribute("a", "test1");
        doc.addAttribute("b", "test");
        doc.updateAttribute("c", null);
        final VertexUpdateOptions options = new VertexUpdateOptions().ifMatch(createResult.getRev());
        final VertexUpdateEntity updateResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .updateVertex(createResult.getKey(), doc, options);
        assertThat(updateResult, is(notNullValue()));
        assertThat(updateResult.getId(), is(createResult.getId()));
        assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
        assertThat(updateResult.getOldRev(), is(createResult.getRev()));

        final BaseDocument readResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .getVertex(createResult.getKey(), BaseDocument.class, null);
        assertThat(readResult.getKey(), is(createResult.getKey()));
        assertThat(readResult.getAttribute("a"), is(notNullValue()));
        assertThat(String.valueOf(readResult.getAttribute("a")), is("test1"));
        assertThat(readResult.getAttribute("b"), is(notNullValue()));
        assertThat(String.valueOf(readResult.getAttribute("b")), is("test"));
        assertThat(readResult.getRevision(), is(updateResult.getRev()));
        assertThat(readResult.getProperties().keySet(), hasItem("c"));
    }

    @Test
    public void updateVertexIfMatchFail() {
        final BaseDocument doc = new BaseDocument();
        doc.addAttribute("a", "test");
        doc.addAttribute("c", "test");
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        doc.updateAttribute("a", "test1");
        doc.addAttribute("b", "test");
        doc.updateAttribute("c", null);
        try {
            final VertexUpdateOptions options = new VertexUpdateOptions().ifMatch("no");
            db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).updateVertex(createResult.getKey(), doc, options);
            fail();
        } catch (final C8DBException e) {
        }
    }

    @Test
    public void updateVertexKeepNullTrue() {
        final BaseDocument doc = new BaseDocument();
        doc.addAttribute("a", "test");
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        doc.updateAttribute("a", null);
        final VertexUpdateOptions options = new VertexUpdateOptions().keepNull(true);
        final VertexUpdateEntity updateResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .updateVertex(createResult.getKey(), doc, options);
        assertThat(updateResult, is(notNullValue()));
        assertThat(updateResult.getId(), is(createResult.getId()));
        assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
        assertThat(updateResult.getOldRev(), is(createResult.getRev()));

        final BaseDocument readResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .getVertex(createResult.getKey(), BaseDocument.class, null);
        assertThat(readResult.getKey(), is(createResult.getKey()));
        assertThat(readResult.getProperties().keySet().size(), is(1));
        assertThat(readResult.getProperties().keySet(), hasItem("a"));
    }

    @Test
    public void updateVertexKeepNullFalse() {
        final BaseDocument doc = new BaseDocument();
        doc.addAttribute("a", "test");
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        doc.updateAttribute("a", null);
        final VertexUpdateOptions options = new VertexUpdateOptions().keepNull(false);
        final VertexUpdateEntity updateResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .updateVertex(createResult.getKey(), doc, options);
        assertThat(updateResult, is(notNullValue()));
        assertThat(updateResult.getId(), is(createResult.getId()));
        assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
        assertThat(updateResult.getOldRev(), is(createResult.getRev()));

        final BaseDocument readResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .getVertex(createResult.getKey(), BaseDocument.class, null);
        assertThat(readResult.getKey(), is(createResult.getKey()));
        assertThat(readResult.getId(), is(createResult.getId()));
        assertThat(readResult.getRevision(), is(notNullValue()));
        assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
    }

    @Test
    public void deleteVertex() {
        final BaseDocument doc = new BaseDocument();
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).deleteVertex(createResult.getKey(), null);
        final BaseDocument vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .getVertex(createResult.getKey(), BaseDocument.class, null);
        assertThat(vertex, is(nullValue()));
    }

    @Test
    public void deleteVertexIfMatch() {
        final BaseDocument doc = new BaseDocument();
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        final VertexDeleteOptions options = new VertexDeleteOptions().ifMatch(createResult.getRev());
        db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).deleteVertex(createResult.getKey(), options);
        final BaseDocument vertex = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME)
                .getVertex(createResult.getKey(), BaseDocument.class, null);
        assertThat(vertex, is(nullValue()));
    }

    @Test
    public void deleteVertexIfMatchFail() {
        final BaseDocument doc = new BaseDocument();
        final VertexEntity createResult = db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).insertVertex(doc,
                null);
        final VertexDeleteOptions options = new VertexDeleteOptions().ifMatch("no");
        try {
            db.graph(GRAPH_NAME).vertexCollection(COLLECTION_NAME).deleteVertex(createResult.getKey(), options);
            fail();
        } catch (final C8DBException e) {
        }
    }
}
