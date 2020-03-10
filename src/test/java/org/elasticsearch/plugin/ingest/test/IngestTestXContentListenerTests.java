package org.elasticsearch.plugin.ingest.test;

/*
 * Copyright [2020] [Mattias Brunnert]
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
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.ingest.SimulateDocumentBaseResult;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.test.ESTestCase;
import org.junit.Assert;

import com.flipkart.zjsonpatch.JsonDiff;

/**
 *
 * @author mattias
 */
public class IngestTestXContentListenerTests extends ESTestCase {

    public IngestTestXContentListenerTests() {
        JsonDiff diff = null;
    }

    @SuppressWarnings("unchecked")
    public void testEmptyDiff() {

        TestBench testBench = TestBench.createSingleDocsBench();

        IngestTestXContentListener instance = new IngestTestXContentListener(null, testBench.expectedDocs);

        Assert.assertEquals(0, instance.createDiff(testBench.testDocs).size());

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testSingleFieldDiff() {
        TestBench testBench = TestBench.createSingleDocsBench();
        putToSource(testBench.expectedDocs.get(0), "title", "test");
        IngestTestXContentListener instance = new IngestTestXContentListener(null, testBench.expectedDocs);
        final List diff = instance.createDiff(testBench.testDocs);
        Assert.assertEquals(1, diff.size());
        ArrayList expectedDiff = new ArrayList();
        HashMap<String, Object> expectedDiff1 = new HashMap<>();
        expectedDiff1.put("op", "add");
        expectedDiff1.put("path", "/0/_source/title");
        expectedDiff1.put("value", "test");
        expectedDiff.add(expectedDiff1);
        Assert.assertEquals(expectedDiff, diff);
    }

    public void testReformatSimulateResult() {
        HashMap<String, Object> expected = new HashMap<>();
        putMetaAndEmptySource(expected, true);
        putToSource(expected, "title", "test");

        HashMap<String, Object> source = new HashMap<>();
        source.put("title", "test");
        IngestDocument ingestDocument = new IngestDocument("index", "_doc", "id", null, null, null, source);
        SimulateDocumentBaseResult simulateResult = new SimulateDocumentBaseResult(ingestDocument);
        Map<String, Object> reformated = IngestTestXContentListener.reformatSimulateResult(simulateResult);

        Assert.assertEquals(expected, reformated);
    }

    private static void putMetaAndEmptySource(Map<String, Object> doc, boolean includeType) {
        doc.put("_index", "index");
        doc.put("_id", "id");
        if (includeType) {
            doc.put("_type", "_doc");
        }
        doc.put("_source", new HashMap<String, Object>());
    }

    @SuppressWarnings("unchecked")
    private static void putToSource(Object document, String name, Object value) {
        Object _source = ((Map<String, Object>)document).get("_source");
        ((Map<String, Object>)_source).put(name, value);
    }


    private static class TestBench {


        @SuppressWarnings("rawtypes")
        public List testDocs;

        @SuppressWarnings("rawtypes")
        public List expectedDocs;

        @SuppressWarnings("unchecked")
        public static TestBench createSingleDocsBench() {
            TestBench testBench = new TestBench();

            Map<String, Object> testDoc = new HashMap<>();
            putMetaAndEmptySource(testDoc, false);

            testBench.testDocs.add(testDoc);

            Map<String, Object> expectedDoc = new HashMap<>();
            putMetaAndEmptySource(expectedDoc, false);
            testBench.expectedDocs.add(expectedDoc);

            return testBench;
        }

        @SuppressWarnings("rawtypes")
        private TestBench() {
            this.expectedDocs = new ArrayList();

            this.testDocs = new ArrayList();
        }
    }

}
