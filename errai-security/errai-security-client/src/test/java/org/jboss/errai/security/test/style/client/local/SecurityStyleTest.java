package org.jboss.errai.security.test.style.client.local;

import static org.jboss.errai.enterprise.client.cdi.api.CDI.addPostInitTask;

import java.util.ArrayList;
import java.util.Arrays;

import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.SyncBeanManager;
import org.jboss.errai.security.client.local.identity.ActiveUserProvider;
import org.jboss.errai.security.shared.Role;
import org.jboss.errai.security.shared.User;
import org.jboss.errai.security.test.style.client.local.res.TemplatedStyleWidget;
import org.junit.Test;

public class SecurityStyleTest extends AbstractErraiCDITest {

  @Override
  public String getModuleName() {
    return "org.jboss.errai.security.test.style.StyleTest";
  }
  
  private final User nobody;
  private final User regularUser;
  private final User admin;
  
  private final Role userRole = new Role("user");
  private final Role adminRole = new Role("admin");
  
  private ActiveUserProvider userProvider;
  private SyncBeanManager bm;
  
  public SecurityStyleTest() {
    nobody = new User();
    nobody.setRoles(new ArrayList<Role>(0));

    regularUser = new User();
    regularUser.setRoles(Arrays.asList(new Role[] {
            userRole
    }));

    admin = new User();
    admin.setRoles(Arrays.asList(new Role[] {
            userRole,
            adminRole
    }));
  }
  
  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    addPostInitTask(new Runnable() {
      
      @Override
      public void run() {
        bm = IOC.getBeanManager();
        userProvider = bm.lookupBean(ActiveUserProvider.class).getInstance();
      }
    });
  }
  
  /**
   * Regression test for ERRAI-644.
   */
  @Test
  public void testTemplatedElementsHiddenWhenNotLoggedIn() throws Exception {
    asyncTest();
    addPostInitTask(new Runnable() {
      
      @Override
      public void run() {
        final TemplatedStyleWidget widget = bm.lookupBean(TemplatedStyleWidget.class).getInstance();
        // Make sure we are not logged in as anyone.
        userProvider.setActiveUser(null);
        
        assertTrue(widget.getControl().isVisible());
        assertFalse(widget.getUserAnchor().isVisible());
        assertFalse(widget.getUserAdminAnchor().isVisible());
        assertFalse(widget.getAdminAnchor().isVisible());

        finishTest();
      }
    });
  }

}
