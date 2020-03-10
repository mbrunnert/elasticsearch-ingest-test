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

package org.elasticsearch.plugin.ingest.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.ingest.SimulatePipelineRequest;
import org.elasticsearch.action.ingest.SimulatePipelineResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * The test pipeline service decorates the _simulate action and returns a diff against
 * expected results provided as part of the request body
 * @author mattias
 */
public class IngestTestService extends BaseRestHandler {

    private static final String FIELD_EXPECTED_DOCS = "expected_docs";

    public IngestTestService(final RestController controller) {
        controller.registerHandler(RestRequest.Method.POST, "/_ingest/pipeline/{id}/_test", this);
        controller.registerHandler(RestRequest.Method.GET, "/_ingest/pipeline/{id}/_test", this);
        controller.registerHandler(RestRequest.Method.POST, "/_ingest/pipeline/_test", this);
        controller.registerHandler(RestRequest.Method.GET, "/_ingest/pipeline/_test", this);
    }

    @Override
    public String getName() {
        return "ingest_test_pipeline";
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        Tuple<XContentType, BytesReference> sourceTuple = restRequest.contentOrSourceParam();
        SimulatePipelineRequest request = new SimulatePipelineRequest(sourceTuple.v2(), sourceTuple.v1());
        request.setId(restRequest.param("id"));
        //Verbose simulate requests are not supported, since they return additional attributes that break the json diff
        request.setVerbose(restRequest.paramAsBoolean("verbose", false));

        XContentParser parser = restRequest.contentParser();
        Map<String, Object> jsonMap = parser.map();
        List expectedDocs = (List)jsonMap.get(FIELD_EXPECTED_DOCS);
        return channel -> {
            RestToXContentListener<SimulatePipelineResponse> ingestTestXContentListener =
                    new IngestTestXContentListener(channel, expectedDocs);
            client.admin().cluster().simulatePipeline(request, ingestTestXContentListener);
        };
    }

}
