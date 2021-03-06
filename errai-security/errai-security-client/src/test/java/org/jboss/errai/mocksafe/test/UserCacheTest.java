package org.jboss.errai.mocksafe.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.enterprise.event.Event;

import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.security.client.local.identity.ActiveUserProviderImpl;
import org.jboss.errai.security.client.local.identity.LocalStorageHandler;
import org.jboss.errai.security.client.local.util.SecurityUtil;
import org.jboss.errai.security.client.local.util.SecurityUtil.SecurityModule;
import org.jboss.errai.security.shared.AuthenticationService;
import org.jboss.errai.security.shared.LoggedInEvent;
import org.jboss.errai.security.shared.LoggedOutEvent;
import org.jboss.errai.security.shared.NonCachingUserService;
import org.jboss.errai.security.shared.User;
import org.jboss.errai.security.util.GwtMockitoRunnerExtension;
import org.jboss.errai.ui.nav.client.local.Navigation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

@RunWith(GwtMockitoRunnerExtension.class)
public class UserCacheTest {

  @Mock
  private LocalStorageHandler storageHandler;
  @Mock
  private Caller<AuthenticationService> caller;
  @Mock
  private NonCachingUserService authService;
  @Mock
  private Logger logger;
  @Mock
  private Event<LoggedInEvent> loginEvent;
  @Mock
  private Event<LoggedOutEvent> logoutEvent;
  @Mock
  private Navigation nav;
  @InjectMocks
  private SecurityModule module;
  @InjectMocks
  private ActiveUserProviderImpl userProvider;

  private Method loadMethod;
  private Method rpcMethod;

  class AuthServiceAnswer implements Answer<NonCachingUserService> {
    private final User response;

    public AuthServiceAnswer(final User user) {
      response = user;
    }

    @Override
    public NonCachingUserService answer(final InvocationOnMock invocation) throws Throwable {
      when(authService.getUser()).then(new Answer<User>() {
        @Override
        public User answer(final InvocationOnMock subInvocation) throws Throwable {
          @SuppressWarnings("unchecked")
          final RemoteCallback<User> callback = (RemoteCallback<User>) invocation.getArguments()[0];
          callback.callback(response);
          return null;
        }
      });

      return authService;
    }
  }

  @Before
  public void setup() throws NoSuchMethodException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    loadMethod = userProvider.getClass().getDeclaredMethod("maybeLoadStoredCache");
    rpcMethod = userProvider.getClass().getDeclaredMethod("updateCacheFromServer");
    loadMethod.setAccessible(true);
    rpcMethod.setAccessible(true);
    
    final Field securityField = SecurityUtil.class.getDeclaredField("moduleInstance");
    securityField.setAccessible(true);
    securityField.set(null, module);
    
    final Field moduleField = SecurityModule.class.getDeclaredField("userProvider");
    moduleField.setAccessible(true);
    moduleField.set(module, userProvider);
  }

  @Test
  public void testRpcOverridesStoredUser() throws Exception {
    final User expected = new User("eve");

    when(storageHandler.getUser()).thenReturn(new User("adam"));
    when(caller.call(any(RemoteCallback.class))).then(new AuthServiceAnswer(expected));

    loadMethod.invoke(userProvider);
    // Precondition
    assertEquals("adam", userProvider.getActiveUser().getLoginName());

    // Actual test
    rpcMethod.invoke(userProvider);

    assertEquals(expected, userProvider.getActiveUser());
    verify(storageHandler).setUser(expected);
  }

  @Test
  public void testRpcHappensWithNoStoredUser() throws Exception {
    final User expected = new User("adam");

    when(storageHandler.getUser()).thenReturn(null);
    when(caller.call(any(RemoteCallback.class))).then(new AuthServiceAnswer(expected));

    loadMethod.invoke(userProvider);
    rpcMethod.invoke(userProvider);

    verify(caller).call(any(RemoteCallback.class));
    verify(storageHandler).setUser(expected);
    assertEquals(expected, userProvider.getActiveUser());
  }

  @Test
  public void testStorageDoesNotOverrideActiveUser() throws Exception {
    final User expected = new User("adam");

    when(storageHandler.getUser()).thenReturn(new User("eve"));

    userProvider.setActiveUser(expected);
    assertTrue(userProvider.isCacheValid());
    verify(storageHandler).setUser(expected);

    loadMethod.invoke(userProvider);

    assertEquals(expected, userProvider.getActiveUser());
    verify(storageHandler, new Times(0)).getUser();
    verify(storageHandler).setUser(expected);
  }

  @Test
  public void testStorageWhenActiveUserSet() throws Exception {
    final User expected = new User("adam");
    userProvider.setActiveUser(expected);

    verify(storageHandler).setUser(expected);
  }

  @Test
  public void testStorageWhenNullUserSet() throws Exception {
    userProvider.setActiveUser(null);

    verify(storageHandler).setUser(null);
  }

  @Test
  public void testStorageRemovedWhenCacheInvalidated() throws Exception {
    userProvider.invalidateCache();

    verify(storageHandler).setUser(null);
  }

}
