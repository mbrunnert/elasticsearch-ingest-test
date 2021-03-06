# Elasticsearch Test Ingest API

API for testing ingest pipelines against expected results. It simulates ingest pipeline, compares result against expected documents and outputs any diffs in JSON Patch format.

## Usage

The API is called using the same parameters and body as the [simulate api](https://www.elastic.co/guide/en/elasticsearch/reference/master/simulate-pipeline-api.html), with the addition of the expected_docs body parameter. Note that:

* Verbose mode is not supported.
* Simulate API always adds `"_id": "id, "_index" : "_index", "_type" : "_doc","` metadata if no values are provided. These fields need to be added at least to `expected_docs` documents to avoid test failure
* The `diff` objects describes the action to be performed by the pipeline in order for the test to pass. E.g. in the example below, the pipeline should be extended to move (rename) the field `foo` to `foo1`.

Example request
```
POST /_ingest/pipeline/_test
{
  "pipeline" :
  {
    "description": "_description",
    "processors": [
      {
        "set" : {
          "field" : "field2",
          "value" : "value"
        }
      }
    ]
  },
  "docs": [
    {
      "_index": "index",
      "_id": "id",
      "_source": {
        "foo": "bar"
      }
    },
    {
      "_index": "index",
      "_id": "id",
      "_source": {
        "foo": "rab"
      }
    }
  ],
  "expected_docs": [
    {
      "_index": "index",
      "_id": "id",
      "_type" : "_doc",
      "_source": {
        "foo1": "bar",
        "field2" : "value"
      }
    },
    {
      "_index": "index",
      "_id": "id",
       "_type" : "_doc",
      "_source": {
        "foo1": "rab",
        "field2": "value"
      }
    }
  ]
}
```
Response
```
{
  "simulate_results" : {
    "docs" : [
      {
        "doc" : {
          "_index" : "index",
          "_type" : "_doc",
          "_id" : "id",
          "_source" : {
            "field2" : "value",
            "foo" : "bar"
          },
          "_ingest" : {
            "timestamp" : "2020-03-04T03:13:29.676717Z"
          }
        }
      },
      {
        "doc" : {
          "_index" : "index",
          "_type" : "_doc",
          "_id" : "id",
          "_source" : {
            "field2" : "value",
            "foo" : "rab"
          },
          "_ingest" : {
            "timestamp" : "2020-03-04T03:13:29.676722Z"
          }
        }
      }
    ]
  },
  "diff" : [
    {
      "op" : "move",
      "from" : "/0/_source/foo",
      "path" : "/0/_source/foo1"
    },
    {
      "op" : "move",
      "from" : "/1/_source/foo",
      "path" : "/1/_source/foo1"
    }
  ]
}

```

## Setup

Prerequisites: JDK 13 for 7.6.x, JDK 12 for 7.5.x.

In order to install this plugin, you need to create a zip distribution first by running

```bash
./gradlew clean check
```

This will produce a zip file in `build/distributions`.

After building the zip file, you can install it like this

```bash
bin/elasticsearch-plugin install file:///path/to/ingest-test/build/distribution/ingest-test-version.zip
```

Note that you need to install the plugin on every Elasticsearch node that you intend to make API requests against. Nodes do not need any specific roles for the plugin to function, as long as you have at least one ingest node in your cluster.

## Acknowledgements

The JSON diff functionality is implemented using [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch)
