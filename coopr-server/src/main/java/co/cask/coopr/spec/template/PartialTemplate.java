package co.cask.coopr.spec.template;


import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import com.google.common.base.Objects;

import java.util.Set;

public class PartialTemplate extends AbstractTemplate {

  private final boolean immutable;

  private PartialTemplate(BaseEntity.Builder baseBuilder, ClusterDefaults clusterDefaults,
                          Compatibilities compatibilities, Constraints constraints, Administration administration,
                          Set<Link> links, boolean immutable) {
    super(baseBuilder, clusterDefaults, compatibilities, constraints, administration, links);
    this.immutable = immutable;
  }

  /**
   * Get immutability of partial template. If this partial template is immutable it's attributes can't be overridden.
   *
   * @return true if template is immutable
   */
  public Boolean isImmutable() {
    return immutable;
  }

  /**
   * Get a builder for creating partial templates.
   *
   * @return Builder for creating partial templates.
   */
  public static PartialTemplateBuilder builder() {
    return new PartialTemplateBuilder();
  }

  /**
   * Builder for creating partial templates.
   */
  public static class PartialTemplateBuilder extends AbstractTemplate.Builder<PartialTemplate, PartialTemplateBuilder> {

    private boolean immutable;

    @Override
    protected PartialTemplateBuilder getThis() {
      return this;
    }

    public PartialTemplate build() {
      return new PartialTemplate(this, clusterDefaults, compatibilities, constraints, administration, links, immutable);
    }

    public PartialTemplateBuilder setImmutable(boolean immutable) {
      this.immutable = immutable;
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PartialTemplate)) {
      return false;
    }
    PartialTemplate other = (PartialTemplate) o;
    return super.equals(other) &&
      Objects.equal(compatibilities, other.compatibilities) &&
      Objects.equal(constraints, other.constraints) &&
      Objects.equal(administration, other.administration) &&
      Objects.equal(links, other.links) &&
      Objects.equal(immutable, other.immutable);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), compatibilities, constraints, administration, links);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("clusterDefaults", clusterDefaults)
      .add("constraints", constraints)
      .add("compatibilities", compatibilities)
      .add("administration", administration)
      .add("links", links)
      .add("immutable", immutable)
      .toString();
  }

}
