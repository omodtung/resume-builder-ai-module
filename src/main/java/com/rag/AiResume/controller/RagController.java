package com.rag.AiResume.controller;

import com.rag.AiResume.model.Query;
import com.rag.AiResume.model.QueryRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {

  private final ChatClient chatClient;

  private final EmbeddingModel embeddingModel;

  private SimpleVectorStore vectorStore;

  private String template;

  @Value("${base.document.directory}")
  private String baseDocumentDirectory;

  // private Resource resource;

  public RagController(
    EmbeddingModel embeddingModel,
    ChatClient.Builder chatClientBuilder
  ) {
    this.embeddingModel = embeddingModel;
    this.chatClient = chatClientBuilder.build();
  }

  // @PostConstruct
  // public void init() {
  //   vectorStore = SimpleVectorStore.builder(embeddingModel).build();
  // String text = null;
  // try (PDDocument document = PDDocument.load(resource.getFile())) {
  //   PDFTextStripper pdfStripper = new PDFTextStripper();
  //   text = pdfStripper.getText(document);
  //   System.out.println(text);
  // } catch (IOException e) {
  //   e.printStackTrace();
  // }
  // System.out.println("resource loading start...............");

  // Document document = new Document(text);
  // List<Document> documentList = List.of(document);

  // vectorStore.accept(documentList);

  // // call the funticion loadDocument

  //   System.out.println("resource loading done...............");
  //   template =
  //     """
  // 			Answer the questions only using the information in the provided knowledge base.
  // 			If you do not know the answer, please response with "I don't know."

  // 			KNOWLEDGE BASE
  // 			---
  // 			{documents}
  // 			""";
  // }

  public void BuildVectorStore(String userId) {
    vectorStore = SimpleVectorStore.builder(embeddingModel).build();
    String userDocument = userId + "_";
    loadDocumentWithPrefixes(Set.of(userDocument));
    System.out.println("Resource loading done...............");
    template =
      """
            Answer the questions only using the information in the provided knowledge base.
            If you do not know the answer, please response with "I don't know."

            KNOWLEDGE BASE
            ---
            {documents}
            """;
  }

  public void loadDocumentWithPrefixes(Set<String> prefixesToLoad) {
    List<Document> documentList = new ArrayList<>();
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] allPdfResources = resolver.getResources(
        baseDocumentDirectory + "*.pdf"
      );

      if (allPdfResources.length == 0) {
        System.out.println(
          "No PDF files found in base directory: " + baseDocumentDirectory
        );
        return;
      }
      for (Resource res : allPdfResources) {
        String filename = res.getFilename();
        if (filename == null) continue;

        // Kiểm tra xem tên file có bắt đầu bằng một trong các tiền tố được chỉ định không
        boolean matchesPrefix = false;
        for (String prefix : prefixesToLoad) {
          if (filename.startsWith(prefix)) {
            matchesPrefix = true;
            break;
          }
        }

        if (matchesPrefix) {
          System.out.println("Processing file matching prefix: " + filename);
          String text = null;
          try (PDDocument document = PDDocument.load(res.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            text = pdfStripper.getText(document);
            if (text != null && !text.isBlank()) {
              documentList.add(
                new Document(text, Map.of("filename", filename))
              );
            } else {
              System.out.println(
                "File " + filename + " is empty or could not be read."
              );
            }
          } catch (IOException e) {
            System.err.println(
              "Error processing file " + filename + ": " + e.getMessage()
            );
            // e.printStackTrace(); // Cân nhắc log chi tiết hơn
          }
        }

        if (!documentList.isEmpty()) {
          vectorStore.add(documentList); // Hoặc vectorStore.accept(documentList);
          System.out.println(
            documentList.size() +
            " documents matching prefixes loaded into vector store."
          );
        } else {
          System.out.println(
            "No documents matching the specified prefixes were found or loaded."
          );
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @RabbitListener(queues = "ragQueue")
  public Object handleRagRequest(@RequestBody Query input) {
    System.err.println(
      "-userid-" + input.getUserId() + "Query from Server1" + input.getQuery()
    );
    BuildVectorStore(input.getUserId());
    String relevantDocs = vectorStore
      .similaritySearch(input.getQuery())
      .stream()
      .map(Document::getText)
      .collect(Collectors.joining());

    Message systemMessage = new SystemPromptTemplate(template)
      .createMessage(Map.of("documents", relevantDocs));
    Message userMessage = new UserMessage(input.getQuery());
    Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

    ChatClient.CallResponseSpec res = chatClient.prompt(prompt).call();
    String responseContent = res.content();

    QueryRequest chatResponse = new QueryRequest();
    chatResponse.setQuery(responseContent);
    chatResponse.setUserId(input.getUserId());
    // String chatResponse = responseContent;
    // System.out.println(" Chat Response " + chatResponse);
    // return chatResponse;
    return chatResponse;
  }
}
