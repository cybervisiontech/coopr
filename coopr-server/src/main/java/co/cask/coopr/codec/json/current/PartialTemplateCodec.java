package co.cask.coopr.codec.json.current;

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.template.AbstractTemplate;
import co.cask.coopr.spec.template.PartialTemplate;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

/**
 * Codec for serializing/deserializing a {@link co.cask.coopr.spec.template.PartialTemplate}.
 */
public class PartialTemplateCodec extends AbstractTemplateCodec<PartialTemplate> {

  @Override
  protected void addChildFields(AbstractTemplate template, JsonObject jsonObj, JsonSerializationContext context) {
    super.addChildFields(template, jsonObj, context);
    PartialTemplate partialTemplate = (PartialTemplate) template;
    jsonObj.add("immutable", context.serialize(partialTemplate.isImmutable()));
  }

  @Override
  protected BaseEntity.Builder<PartialTemplate> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    PartialTemplate.PartialTemplateBuilder builder = (PartialTemplate.PartialTemplateBuilder)
      super.getBuilder(jsonObj, context);
    builder.setImmutable(context.<Boolean>deserialize(jsonObj.get("immutable"), Boolean.class));
    return builder;
  }

  @Override
  protected AbstractTemplate.Builder getConcreteBuilder() {
    return PartialTemplate.builder();
  }
}
