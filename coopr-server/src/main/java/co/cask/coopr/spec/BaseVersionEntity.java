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

package co.cask.coopr.spec;

import com.google.common.base.Objects;

/**
 * A base for entities that require a name, version and optionally support an icon, description, and label.
 */
public class BaseVersionEntity extends BaseEntity {

  protected int version;

  protected BaseVersionEntity(BaseEntity.Builder baseBuilder, int version) {
    super(baseBuilder);
    this.version = version;
  }

  /**
   * Retrieves the version of the entity.
   *
   * @return the version of the entity.
   */
  public int getVersion() {
    return version;
  }

  /**
   * Increases and retrieves the version of the entity.
   *
   * @return the version of the entity.
   */
  public int increaseVersion() {
    return ++version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseVersionEntity)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    BaseVersionEntity that = (BaseVersionEntity) o;

    return version == that.version;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + version;
    return result;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("version", version)
      .toString();
  }
}
