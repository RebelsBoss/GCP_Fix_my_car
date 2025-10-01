package com.cpet.fixmycarbackend;

import com.google.cloud.discoveryengine.v1.SearchRequest;
import com.google.cloud.discoveryengine.v1.SearchResponse;
import com.google.cloud.discoveryengine.v1.SearchResponse.SearchResult;
import com.google.cloud.discoveryengine.v1.SearchServiceClient;
import com.google.cloud.discoveryengine.v1.SearchServiceSettings;
import com.google.cloud.discoveryengine.v1.ServingConfigName;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FixMyCarBackendController {
  private static final Logger logger = LoggerFactory.getLogger(FixMyCarBackendController.class);

  @Autowired private FixMyCarConfiguration config;
  private String projectId;
  private String datastoreId;

  @PostConstruct
  public void init() {
    projectId = config.getProjectId();
    datastoreId = config.getVertexDataStoreId();
  }

  @GetMapping("/")
  public String index() {
    logger.info("🚗 GET /");
    return "Welcome to the FixMyCar Backend API!";
  }

  @GetMapping("/health")
  public String health() {
    logger.info("✅ GET /health");
    return "ok";
  }

  @PostMapping(value = "/chat", consumes = "application/json", produces = "application/json")
  public ChatMessage message(@RequestBody ChatMessage message) {
    return ragVertexAISearch(message);
  }

  public ChatMessage ragVertexAISearch(ChatMessage message) {
    logger.info("⭐ project Id: " + projectId);
    final String location = "global";
    final String collectionId = "default_collection";
    logger.info("⭐ Datastore ID is: " + datastoreId);
    final String servingConfigId = "default_search";
    final String searchQuery = message.getPrompt();
    logger.info("⭐ Datastore query: " + searchQuery);

    final String endpoint = "discoveryengine.googleapis.com:443";
    String vectorSearchResults = "";

    try {
      SearchServiceSettings settings =
          SearchServiceSettings.newBuilder().setEndpoint(endpoint).build();

      final String servingConfig =
          ServingConfigName.formatProjectLocationCollectionDataStoreServingConfigName(
              projectId, location, collectionId, datastoreId, servingConfigId);

      logger.info("🔧 Using servingConfig: " + servingConfig);

      try (SearchServiceClient searchServiceClient = SearchServiceClient.create(settings)) {
        SearchRequest request =
            SearchRequest.newBuilder()
                .setServingConfig(servingConfig)
                .setQuery(searchQuery)
                .setPageSize(10)
                .build();

        SearchResponse response = searchServiceClient.search(request).getPage().getResponse();
        List<SearchResult> resultsList = response.getResultsList();
        logger.info("🔍 Found " + resultsList.size() + " results.");

        for (SearchResponse.SearchResult element : resultsList) {
          Struct derived = element.getDocument().getDerivedStructData();
          if (derived == null) continue;
          Map<String, Value> fields = derived.getFieldsMap();
          if (fields == null || !fields.containsKey("extractive_answers")) continue;
          Value ea = fields.get("extractive_answers");
          if (ea == null || !ea.hasListValue()) continue;
          ListValue lv = ea.getListValue();
          if (lv.getValuesCount() == 0) continue;
          Value v0 = lv.getValues(0);
          if (v0 == null || !v0.hasStructValue()) continue;
          Struct s0 = v0.getStructValue();
          Map<String, Value> inner = s0.getFieldsMap();
          if (inner == null || !inner.containsKey("content")) continue;
          Value content = inner.get("content");
          if (content != null && content.hasStringValue()) {
            vectorSearchResults += content.getStringValue() + "\n";
          }
        }
      }
    } catch (Exception e) {
      logger.error("⚠️ Vertex AI Search Error: " + e.getClass().getName() + ": " + e.getMessage());
    }

    logger.info("🔍 Vertex AI Search results: " + vectorSearchResults);
    String result = geminiInference(message.getPrompt(), vectorSearchResults);
    message.setResponse(result);
    return message;
  }

  public String geminiInference(String userPrompt, String vectorSearchResults) {
    String geminiPrompt =
        "You are a helpful car manual chatbot. Answer the car owner's question about their car."
            + " Human prompt: "
            + userPrompt
            + ",\n"
            + " Use the following grounding data as context. This came from the relevant vehicle"
            + " owner's manual: "
            + vectorSearchResults;
    logger.info("🔮 Gemini Prompt: " + geminiPrompt);

    final String modelId = System.getenv().getOrDefault("GENAI_MODEL", "gemini-2.5-flash");

    try (Client client =
        Client.builder()
            .vertexAI(true)
            .location(System.getenv().getOrDefault("GOOGLE_CLOUD_LOCATION", "global"))
            .httpOptions(HttpOptions.builder().apiVersion("v1").build())
            .build()) {

      GenerateContentResponse resp =
          client.models.generateContent(modelId, geminiPrompt, null);
      String strResp = resp.text();
      logger.info("🔮 Gemini Response: " + strResp);
      return strResp;
    } catch (Exception e) {
      logger.error("⚠️ Gemini Error: " + e.getClass().getName() + ": " + e.getMessage());
      return e.getMessage();
    }
  }
}
