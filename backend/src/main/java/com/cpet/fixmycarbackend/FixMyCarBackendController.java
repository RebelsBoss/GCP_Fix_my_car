package com.cpet.fixmycarbackend;

import com.google.cloud.discoveryengine.v1.SearchRequest;
import com.google.cloud.discoveryengine.v1.SearchResponse;
import com.google.cloud.discoveryengine.v1.SearchResponse.SearchResult;
import com.google.cloud.discoveryengine.v1.SearchServiceClient;
import com.google.cloud.discoveryengine.v1.SearchServiceSettings;
import com.google.cloud.discoveryengine.v1.ServingConfigName;

// 🔄 NEW Gen AI SDK imports
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
    // ⭐ Step 1 - Search
    logger.info("⭐ project Id: " + projectId);
    final String location = "global";
    final String collectionId = "default_collection";
    logger.info("⭐ Datastore ID is: " + datastoreId);
    final String servingConfigId = "default_search";
    final String searchQuery = message.getPrompt();
    logger.info("⭐ Datastore query: " + searchQuery);

    // discoveryengine endpoint (global)
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
          Struct derivedStructData = element.getDocument().getDerivedStructData();
          if (derivedStructData == null) continue;

          Map<String, Value> fields = derivedStructData.getFieldsMap();
          if (fields == null || !fields.containsKey("extractive_answers")) continue;

          Value extractiveAnswersValue = fields.get("extractive_answers");
          if (extractiveAnswersValue == null || !extractiveAnswersValue.hasListValue()) continue;

          ListValue listValue = extractiveAnswersValue.getListValue();
          if (listValue.getValuesCount() == 0) continue;

          Value firstValue = listValue.getValues(0);
          if (firstValue == null || !firstValue.hasStructValue()) continue;

          Struct structValue = firstValue.getStructValue();
          Map<String, Value> innerFields = structValue.getFieldsMap();
          if (innerFields == null || !innerFields.containsKey("content")) continue;

          Value contentValue = innerFields.get("content");
          if (contentValue != null && contentValue.hasStringValue()) {
            vectorSearchResults += contentValue.getStringValue() + "\n";
          }
        }
      }
    } catch (Exception e) {
      logger.error("⚠️ Vertex AI Search Error: " + e.getClass().getName() + ": " + e.getMessage());
    }

    // ⭐ Step 2 - Inference w/ augmented prompt (Gemini via Gen AI SDK on Vertex AI)
    logger.info("🔍 Vertex AI Search results: " + vectorSearchResults);
    String result = geminiInference(message.getPrompt(), vectorSearchResults);
    message.setResponse(result);
    return message;
  }

  // Gemini via Google Gen AI SDK (Vertex AI mode)
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

    // modelId can be overridden via env GENAI_MODEL if потрібно
    final String modelId =
        System.getenv().getOrDefault("GENAI_MODEL", "gemini-2.5-flash");

    try (Client client =
        Client.builder()
            .vertexAI(true) // важливо: працюємо через Vertex AI
            .location(System.getenv().getOrDefault("GOOGLE_CLOUD_LOCATION", "global"))
            .httpOptions(HttpOptions.builder().apiVersion("v1").build())
            .build()) {

      GenerateContentResponse resp =
          client.models.generateContent(modelId, geminiPrompt, /*tools*/ null);
      String strResp = resp.text();
      logger.info("🔮 Gemini Response: " + strResp);
      return strResp;
    } catch (Exception e) {
      logger.error("⚠️ Gemini Error: " + e.getClass().getName() + ": " + e.getMessage());
      return e.getMessage();
    }
  }
}
