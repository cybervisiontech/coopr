package co.cask.coopr.codec.json.current;

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.template.AbstractTemplate;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Abstract Codec for template serializing/deserializing.
 */
public abstract class AbstractTemplateCodec<T extends AbstractTemplate> extends AbstractBaseEntityCodec<T> {

  protected static final Type LINKS_TYPE = new com.google.common.reflect.TypeToken<Set<Link>>() { }.getType();

  @Override
  protected void addChildFields(AbstractTemplate template, JsonObject jsonObj, JsonSerializationContext context) {
    jsonObj.add("defaults", context.serialize(template.getClusterDefaults()));
    jsonObj.add("compatibility", context.serialize(template.getCompatibilities()));
    jsonObj.add("constraints", context.serialize(template.getConstraints()));
    jsonObj.add("administration", context.serialize(template.getAdministration()));
    jsonObj.add("links", context.serialize(template.getLinks()));
  }

  @Override
  @SuppressWarnings("unchecked")
  protected BaseEntity.Builder<T> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    return getConcreteBuilder()
      .setClusterDefaults(context.<ClusterDefaults>deserialize(jsonObj.get("defaults"), ClusterDefaults.class))
      .setCompatibilities(context.<Compatibilities>deserialize(jsonObj.get("compatibility"), Compatibilities.class))
      .setConstraints(context.<Constraints>deserialize(jsonObj.get("constraints"), Constraints.class))
      .setAdministration(context.<Administration>deserialize(jsonObj.get("administration"), Administration.class))
      .setLinks(context.<Set<Link>>deserialize(jsonObj.get("links"), LINKS_TYPE));
  }

  protected abstract AbstractTemplate.Builder getConcreteBuilder();

}
