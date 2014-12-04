package co.cask.coopr.codec.json.current;

import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.template.AbstractTemplate;
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

}
