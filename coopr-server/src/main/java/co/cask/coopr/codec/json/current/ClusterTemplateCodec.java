/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.cask.coopr.codec.json.current;

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.template.AbstractTemplate;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.spec.template.Include;
import co.cask.coopr.spec.template.Parent;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link ClusterTemplate}.
 */
public class ClusterTemplateCodec extends AbstractTemplateCodec<ClusterTemplate> {

  private static final Type INCLUDES_TYPE = new com.google.common.reflect.TypeToken<Set<Include>>() { }.getType();

  @Override
  protected void addChildFields(ClusterTemplate template, JsonObject jsonObj, JsonSerializationContext context) {
    jsonObj.add("defaults", context.serialize(template.getClusterDefaults()));
    jsonObj.add("compatibility", context.serialize(template.getCompatibilities()));
    jsonObj.add("constraints", context.serialize(template.getConstraints()));
    jsonObj.add("administration", context.serialize(template.getAdministration()));
    jsonObj.add("links", context.serialize(template.getLinks()));
    jsonObj.add("extends", context.serialize(template.getParent()));
    jsonObj.add("includes", context.serialize(template.getIncludes()));
  }

  @Override
  protected BaseEntity.Builder<ClusterTemplate> getBuilder(JsonObject jsonObj,
                                                                JsonDeserializationContext context) {
    return ClusterTemplate.builder()
      .setClusterDefaults(context.<ClusterDefaults>deserialize(jsonObj.get("defaults"), ClusterDefaults.class))
      .setCompatibilities(context.<Compatibilities>deserialize(jsonObj.get("compatibility"), Compatibilities.class))
      .setConstraints(context.<Constraints>deserialize(jsonObj.get("constraints"), Constraints.class))
      .setAdministration(context.<Administration>deserialize(jsonObj.get("administration"), Administration.class))
      .setLinks(context.<Set<Link>>deserialize(jsonObj.get("links"), LINKS_TYPE))
      .setParent(context.<Parent>deserialize(jsonObj.get("extends"), Parent.class))
      .setIncludes(context.<Set<Include>>deserialize(jsonObj.get("includes"), INCLUDES_TYPE));
  }
}
