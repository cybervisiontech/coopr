package co.cask.coopr.spec.template;


import co.cask.coopr.BaseTest;
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.ClusterService;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.store.entity.EntityStoreView;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;

/**
 *
 */
public class PartialTemplateTest extends BaseTest {

  private static ClusterService clusterService;
  private static Account account;

  private static ClusterTemplate insecureTemplate;
  private static ClusterTemplate secureTemplate;
  private static ClusterTemplate distributedTemplate;
  private static PartialTemplate sensuPartial;
  private static PartialTemplate ldapPartial;

  private static EntityStoreView entityStoreView;

  @Override
  protected boolean shouldClearDataBetweenTests() {
    return false;
  }

  @BeforeClass
  public static void setupClusterServiceTests() throws Exception {
    clusterService = injector.getInstance(ClusterService.class);
    gson = injector.getInstance(Gson.class);
    TenantProvisionerService tenantProvisionerService = injector.getInstance(TenantProvisionerService.class);
    // setup data
    tenantProvisionerService.writeProvisioner(new Provisioner("p1", "host", 50056, 100, null, null));
    tenantProvisionerService.writeTenantSpecification(new TenantSpecification("tenantX", 10, 1, 10));
    Tenant tenant = tenantStore.getTenantByName("tenantX");
    account = new Account("user9", tenant.getId());
    entityStoreView = entityStoreService.getView(new Account(Constants.ADMIN_USER, tenant.getId()));
    //load json templates
    ClassLoader classLoader = PartialTemplateTest.class.getClassLoader();
    InputStream insecureIn = classLoader.getResourceAsStream("partials/cdap-­distributed­-insecure.json");
    InputStream secureIn = classLoader.getResourceAsStream("partials/cdap­-distributed-­secure­-hadoop.json");
    InputStream distributedIn = classLoader.getResourceAsStream("partials/cdap­distributed.json");
    InputStream sensuIn = classLoader.getResourceAsStream("partials/sensu-partial.json");
    InputStream ldapIn = classLoader.getResourceAsStream("partials/ldap-partial.json");

    insecureTemplate = gson.fromJson(IOUtils.toString(insecureIn), ClusterTemplate.class);
    secureTemplate = gson.fromJson(IOUtils.toString(secureIn), ClusterTemplate.class);
    distributedTemplate = gson.fromJson(IOUtils.toString(distributedIn), ClusterTemplate.class);
    sensuPartial = gson.fromJson(IOUtils.toString(sensuIn), PartialTemplate.class);
    ldapPartial = gson.fromJson(IOUtils.toString(ldapIn), PartialTemplate.class);

    entityStoreView.writeClusterTemplate(insecureTemplate);
    entityStoreView.writeClusterTemplate(secureTemplate);
    entityStoreView.writeClusterTemplate(distributedTemplate);

    entityStoreView.writePartialTemplate(sensuPartial);
    entityStoreView.writePartialTemplate(ldapPartial);
  }

  @Test
  public void testTemplates() throws Exception {
    PartialTemplate ldapInternal = entityStoreView.getPartialTemplate("LDAPInternal");
    PartialTemplate sensuInternal = entityStoreView.getPartialTemplate("sensuInternal");

    ClusterTemplate cdapDistributedSecureHadoop = entityStoreView.getClusterTemplate("cdapDistributedSecureHadoop");
    ClusterTemplate cdapDistributedInsecure = entityStoreView.getClusterTemplate("cdapDistributedInsecure");
    ClusterTemplate cdapDistributed = entityStoreView.getClusterTemplate("cdapDistributed");

    Assert.assertNotNull(ldapInternal);
    Assert.assertNotNull(sensuInternal);

    Assert.assertNotNull(cdapDistributedSecureHadoop);
    Assert.assertNotNull(cdapDistributedInsecure);
    Assert.assertNotNull(cdapDistributed);

    Assert.assertEquals("Configure Example, Inc. LDAP services", ldapInternal.getDescription());
    Assert.assertEquals(true, ldapInternal.isImmutable());
    Assert.assertEquals("ldap­internal", ldapInternal.clusterDefaults.getServices().iterator().next());
    Assert.assertEquals("ldap­internal", ldapInternal.compatibilities.getServices().iterator().next());
    Assert.assertNotNull(ldapInternal.clusterDefaults.getConfig().get("ldap"));
    Assert.assertEquals("ldap.example.com", ldapInternal.clusterDefaults.getConfig().get("ldap")
      .getAsJsonObject().get("endpoint").getAsString());

    Assert.assertEquals("Cask DAP (CDAP) with Security and Secure Hadoop cluster with single master",
                        cdapDistributedSecureHadoop.getDescription());
    Assert.assertNotNull(cdapDistributedSecureHadoop.getParent());
    Assert.assertEquals("cdapDistributed", cdapDistributedSecureHadoop.getParent().getName());
    Assert.assertNotNull(cdapDistributedSecureHadoop.getIncludes());
    Assert.assertEquals("LDAPInternal", cdapDistributedSecureHadoop.getIncludes().iterator().next().getName());
    Assert.assertEquals(3, cdapDistributedSecureHadoop.clusterDefaults.getServices().size());
    Assert.assertNotNull(cdapDistributedSecureHadoop.getClusterDefaults().getConfig().get("hive"));
    Assert.assertEquals("kerberos­client",
                        cdapDistributedSecureHadoop.getCompatibilities().getServices().iterator().next());

  }
}
