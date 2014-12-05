package co.cask.coopr.spec.template;


import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public abstract class AbstractTemplate extends BaseEntity {

  protected final ClusterDefaults clusterDefaults;
  protected final Constraints constraints;
  protected final Compatibilities compatibilities;
  protected final Administration administration;
  protected final Set<Link> links;

  protected AbstractTemplate(BaseEntity.Builder baseBuilder, ClusterDefaults clusterDefaults,
                          Compatibilities compatibilities, Constraints constraints, Administration administration,
                          Set<Link> links) {
    super(baseBuilder);
    Preconditions.checkArgument(clusterDefaults != null, "cluster defaults must be specified");
    this.clusterDefaults = clusterDefaults;
    this.constraints = constraints;
    this.compatibilities = compatibilities;
    this.administration = administration;
    this.links = links;
  }

  /**
   * Get the {@link Compatibilities} for the cluster.
   *
   * @return {@link Compatibilities} for the cluster.
   */
  public Compatibilities getCompatibilities() {
    return compatibilities;
  }

  /**
   * Get the constraints that specify how the cluster should be laid out.
   *
   * @return {@link Constraints} specifying how the cluster should be laid out.
   */
  public Constraints getConstraints() {
    return constraints;
  }

  /**
   * Get the {@link ClusterDefaults} that will be used unless the user replaces them at create time.
   *
   * @return {@link ClusterDefaults} for the template.
   */
  public ClusterDefaults getClusterDefaults() {
    return clusterDefaults;
  }

  /**
   * Get administration settings like lease time for managing the cluster.
   *
   * @return Administration settings for managing the cluster.
   */
  public Administration getAdministration() {
    return administration;
  }

  /**
   * Get immutable set of cluster links to services on the cluster.
   *
   * @return Immutable set of cluster links to services on the cluster
   */
  public Set<Link> getLinks() {
    return links;
  }

  /**
   * Builder for creating templates.
   */
  public abstract static class Builder<T extends AbstractTemplate, V extends Builder<T, V>> extends BaseEntity.Builder<T> {
    protected ClusterDefaults clusterDefaults;
    protected Constraints constraints = Constraints.EMPTY_CONSTRAINTS;
    protected Compatibilities compatibilities = Compatibilities.EMPTY_COMPATIBILITIES;
    protected Administration administration = Administration.EMPTY_ADMINISTRATION;
    protected Set<Link> links = ImmutableSet.of();

    protected abstract V getThis();

    @Override
    public V setName(String name) {
      this.name = name;
      return getThis();
    }

    @Override
    public V setIcon(String icon) {
      this.icon = icon;
      return getThis();
    }

    @Override
    public V setDescription(String description) {
      this.description = description;
      return getThis();
    }

    public V setClusterDefaults(ClusterDefaults clusterDefaults) {
      this.clusterDefaults = clusterDefaults;
      return getThis();
    }

    public V setConstraints(Constraints constraints) {
      this.constraints = constraints == null ? Constraints.EMPTY_CONSTRAINTS : constraints;
      return getThis();
    }

    public V setCompatibilities(Compatibilities compatibilities) {
      this.compatibilities = compatibilities == null ? Compatibilities.EMPTY_COMPATIBILITIES : compatibilities;
      return getThis();
    }

    public V setAdministration(Administration administration) {
      this.administration = administration == null ? Administration.EMPTY_ADMINISTRATION : administration;
      return getThis();
    }

    public V setLinks(Set<Link> links) {
      this.links = links == null ? ImmutableSet.<Link>of() : ImmutableSet.copyOf(links);
      return getThis();
    }

    public abstract T build();

  }
}
