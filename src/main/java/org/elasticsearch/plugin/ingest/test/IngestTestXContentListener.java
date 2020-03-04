/*
 * Copyright [2018] [Mattias Brunnert]
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

package org.elasticsearch.plugin.ingest.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.elasticsearch.action.ingest.SimulateDocumentBaseResult;
import org.elasticsearch.action.ingest.SimulateDocumentResult;
import org.elasticsearch.action.ingest.SimulatePipelineResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.SpecialPermission;

public class IngestTestXContentListener extends RestToXContentListener<SimulatePipelineResponse> {

    @SuppressWarnings("rawtypes")
    private final List expectedDocs;

    private static final ObjectMapper MAPPER = buildObjectMapper();
    private static final EnumSet<DiffFlags> DIFF_FLAGS = buildDiffFlags();

    @SuppressWarnings("rawtypes")
    IngestTestXContentListener(RestChannel channel, List expectedDocs) {
        super(channel);
        this.expectedDocs = expectedDocs;
    }

    /**
     * Build object mapper
     * 
     * @return object mapper
     */
    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, false);
        return mapper;
    }

    /**
     * Makes diff object more verbose by adding original value to diff
     * 
     * @return diff flags
     */
    private static EnumSet<DiffFlags> buildDiffFlags() {
        EnumSet<DiffFlags> diffFlags = DiffFlags.defaults();
        diffFlags.add(DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE);
        return diffFlags;
    }

    /**
     * Decorates the simulate response with the diff
     *
     * @param response SimulatePipeline response
     * @param builder  XContentBuilder
     * @return rest response
     */
    @SuppressWarnings("rawtypes")
    @Override
    public RestResponse buildResponse(SimulatePipelineResponse response, XContentBuilder builder) throws Exception {
        // Reformat simulate results to match expected results json
        List<Map<String, Object>> simulateResults = response.getResults().stream()
                .map(IngestTestXContentListener::reformatSimulateResult).collect(Collectors.toList());

        List diffObject = createDiff(simulateResults);

        IngestTestResponse decoratedResponse = new IngestTestResponse(response, diffObject);
        RestResponse restResponse = super.buildResponse(decoratedResponse, builder);
        return restResponse;
    }

    @SuppressWarnings("rawtypes")
    /**
     * Create list of diff objects
     * 
     * @param simulateResults SimulatePipeline results
     * @return list of diff objects
     * @throws IllegalArgumentException
     */
    List createDiff(List<Map<String, Object>> simulateResults) throws IllegalArgumentException {
        JsonNode expectedJson = MAPPER.valueToTree(this.expectedDocs);
        JsonNode actualJson = MAPPER.valueToTree(simulateResults);
        final JsonNode diffJson = JsonDiff.asJson(actualJson, expectedJson, DIFF_FLAGS);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // unprivileged code such as scripts do not have SpecialPermission
            sm.checkPermission(new SpecialPermission());
        }
        List diffObject = AccessController
                .doPrivileged((PrivilegedAction<List>) () -> MAPPER.convertValue(diffJson, List.class));
        return diffObject;
    }

    /**
     * Reformats a simulateResult object so it matches the json structure of the
     * output object. This enables direct json comparison while the user provides
     * expected documents in the same format as results are output by the simulate
     * api call
     *
     * @param simulateResult SimulatePipeline results
     * @return simulate result Map
     */
    static Map<String, Object> reformatSimulateResult(SimulateDocumentResult simulateResult) {
        Map<String, Object> result = new HashMap<>();
        SimulateDocumentBaseResult ingestDocument = (SimulateDocumentBaseResult) simulateResult;
        Map<IngestDocument.MetaData, Object> metadataMap = ingestDocument.getIngestDocument().getMetadata();
        for (Map.Entry<IngestDocument.MetaData, Object> metadata : metadataMap.entrySet()) {
            if (metadata.getValue() != null) {
                result.put(metadata.getKey().getFieldName(), metadata.getValue().toString());
            }
        }
        Map<String, Object> source = IngestDocument
                .deepCopyMap(ingestDocument.getIngestDocument().getSourceAndMetadata());
        metadataMap.keySet().forEach(mD -> source.remove(mD.getFieldName()));
        result.put("_source", source);
        return result;
    }

}
