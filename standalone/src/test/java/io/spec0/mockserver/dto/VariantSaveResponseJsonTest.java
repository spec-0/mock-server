package io.spec0.mockserver.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spec0.mockserver.domain.MockResponseVariantEntity;
import io.spec0.mockserver.standalone.StandaloneMockServerApplication;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;

@JsonTest
@ContextConfiguration(classes = StandaloneMockServerApplication.class)
class VariantSaveResponseJsonTest {

  @Autowired ObjectMapper mapper;

  @Test
  void entityFieldsAreUnwrappedToTopLevel() throws Exception {
    MockResponseVariantEntity entity =
        new MockResponseVariantEntity(
            UUID.randomUUID(), "createItem", "ok", "201", "{\"id\":\"1\"}");
    VariantSaveResponse response =
        new VariantSaveResponse(new VariantSaveResult(entity, List.of()));

    String json = mapper.writeValueAsString(response);
    var node = mapper.readTree(json);

    assertThat(node.has("variantId")).as("variantId must be at top level").isTrue();
    assertThat(node.has("operationId")).as("operationId must be at top level").isTrue();
    assertThat(node.has("responseName")).as("responseName must be at top level").isTrue();
    assertThat(node.has("variant")).as("variant must NOT appear as nested object").isFalse();
    assertThat(node.has("validationWarnings")).as("validationWarnings absent when empty").isFalse();
  }

  @Test
  void validationWarningsIncludedWhenNonEmpty() throws Exception {
    MockResponseVariantEntity entity =
        new MockResponseVariantEntity(
            UUID.randomUUID(), "createItem", "bad", "201", "{\"wrong\":1}");
    VariantSaveResponse response =
        new VariantSaveResponse(
            new VariantSaveResult(entity, List.of("$.id: is missing but it is required")));

    String json = mapper.writeValueAsString(response);
    var node = mapper.readTree(json);

    assertThat(node.has("validationWarnings")).isTrue();
    assertThat(node.get("validationWarnings").isArray()).isTrue();
    assertThat(node.get("validationWarnings").get(0).asText())
        .isEqualTo("$.id: is missing but it is required");
  }
}
