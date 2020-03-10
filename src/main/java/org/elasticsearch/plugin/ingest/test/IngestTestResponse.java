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

import org.elasticsearch.action.ingest.SimulatePipelineResponse;
import java.io.IOException;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class IngestTestResponse extends SimulatePipelineResponse {

    private final Object diff;

    public IngestTestResponse(SimulatePipelineResponse decorated, Object diff) {
        super(decorated.getPipelineId(), decorated.isVerbose(), decorated.getResults());
        this.diff = diff;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("simulate_results");
        super.toXContent(builder, params);
        builder.field("diff", diff);
        builder.endObject();
        return builder;
    }

}
