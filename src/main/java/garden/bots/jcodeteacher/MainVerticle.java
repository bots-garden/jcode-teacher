package garden.bots.jcodeteacher;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.output.Response;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.*;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import static dev.langchain4j.data.message.SystemMessage.systemMessage;

public class MainVerticle extends AbstractVerticle {

  private boolean cancelRequest ;


  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    //Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    var llmBaseUrl = Optional.ofNullable(System.getenv("OLLAMA_BASE_URL")).orElse("http://localhost:11434");
    var modelName = Optional.ofNullable(System.getenv("LLM")).orElse("deepseek-coder");

    var staticPath = Optional.ofNullable(System.getenv("STATIC_PATH")).orElse("/*");
    var httpPort = Optional.ofNullable(System.getenv("HTTP_PORT")).orElse("8888");

    //https://docs.langchain4j.dev/apidocs/dev/langchain4j/model/ollama/OllamaStreamingChatModel.html
    StreamingChatLanguageModel streamingModel = OllamaStreamingChatModel.builder()
      .baseUrl(llmBaseUrl)
      .modelName(modelName).temperature(0.0).repeatPenalty(1.0)
      .build();

    var memory = MessageWindowChatMemory.withMaxMessages(5);

    SystemMessage systemInstructions = systemMessage("""
      You are an expert in computer programming.
      Please make friendly answer for the noobs.
      Add source code examples if you can.
      """);

    PromptTemplate humanPromptTemplate = PromptTemplate.from("""
      I need a clear explanation regarding my {{question}}.
      And, please, be structured with bullet points.
      """);

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    // Serving static resources
    var staticHandler = StaticHandler.create();
    staticHandler.setCachingEnabled(false);
    router.route(staticPath).handler(staticHandler);

    router.delete("/clear-history").handler(ctx -> {
      memory.clear();
      ctx.response()
        .putHeader("content-type", "text/plain;charset=utf-8")
        .end("👋 conversation memory is empty");
    });

    router.get("/message-history").handler(ctx -> {
      var strList = memory.messages().stream().map(msg -> msg.toString());
      JsonArray messages = new JsonArray(strList.toList());
      ctx.response()
        .putHeader("Content-Type", "application/json;charset=utf-8")
        .end(messages.encodePrettily());
    });

    router.delete("/cancel-request").handler(ctx -> {
      cancelRequest = true;
      ctx.response()
        .putHeader("content-type", "text/plain;charset=utf-8")
        .end("👋 request aborted");
    });

    router.post("/prompt").handler(ctx -> {

      var question = ctx.body().asJsonObject().getString("question");

      Map<String, Object> variables  = new HashMap<String, Object>() {{
        put("question", question);
      }};

      var humanMessage = humanPromptTemplate.apply(variables).toUserMessage();

      List<ChatMessage> messages = new ArrayList<>();
      messages.add(systemInstructions);
      messages.addAll(memory.messages());
      messages.add(humanMessage);

      memory.add(humanMessage);

      HttpServerResponse response = ctx.response();


      response
        .putHeader("Content-Type", "application/octet-stream")
        .setChunked(true);

      streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
        @Override
        public void onNext(String token) {
          if (cancelRequest) {
            // https://github.com/langchain4j/langchain4j/issues/245
            cancelRequest = false;
            throw new RuntimeException("🤬 Shut up!");
          }

          System.out.println("New token: '" + token + "'");
          response.write(token);
        }

        @Override
        public void onComplete(Response<AiMessage> modelResponse) {
          memory.add(modelResponse.content());
          System.out.println("Streaming completed: " + modelResponse);
          response.end();

        }

        @Override
        public void onError(Throwable throwable) {
          throwable.printStackTrace();
        }
      });

    });


    // Create an HTTP server
    var server = vertx.createHttpServer();

    //! Start the HTTP server
    server.requestHandler(router).listen(Integer.parseInt(httpPort), http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("GenAI Vert-x server started on port " + httpPort);
      } else {
        startPromise.fail(http.cause());
      }
    });

  }
}
