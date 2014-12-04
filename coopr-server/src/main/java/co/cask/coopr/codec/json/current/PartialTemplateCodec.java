package co.cask.coopr.codec.json.current;

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.spec.template.PartialTemplate;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link co.cask.coopr.spec.template.PartialTemplate}.
 */
public class PartialTemplateCodec extends AbstractTemplateCodec<PartialTemplate> {

  @Override
  protected void addChildFields(PartialTemplate template, JsonObject jsonObj, JsonSerializationContext context) {
    jsonObj.add("defaults", context.serialize(template.getClusterDefaults()));
    jsonObj.add("compatibility", context.serialize(template.getCompatibilities()));
    jsonObj.add("constraints", context.serialize(template.getConstraints()));
    jsonObj.add("administration", context.serialize(template.getAdministration()));
    jsonObj.add("links", context.serialize(template.getLinks()));
    jsonObj.add("immutable", context.serialize(template.isImmutable()));
  }

  @Override
  protected BaseEntity.Builder<PartialTemplate> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    return PartialTemplate.builder()
      .setClusterDefaults(context.<ClusterDefaults>deserialize(jsonObj.get("defaults"), ClusterDefaults.class))
      .setCompatibilities(context.<Compatibilities>deserialize(jsonObj.get("compatibility"), Compatibilities.class))
      .setConstraints(context.<Constraints>deserialize(jsonObj.get("constraints"), Constraints.class))
      .setAdministration(context.<Administration>deserialize(jsonObj.get("administration"), Administration.class))
      .setLinks(context.<Set<Link>>deserialize(jsonObj.get("links"), LINKS_TYPE))
      .setImmutable(context.<Boolean>deserialize(jsonObj.get("immutable"), Boolean.class));
  }
}
