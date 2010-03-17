
package org.jboss.errai.workspaces.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;
import org.gwt.mosaic.ui.client.Caption;
import org.gwt.mosaic.ui.client.CaptionLayoutPanel;
import org.gwt.mosaic.ui.client.ImageButton;
import org.gwt.mosaic.ui.client.layout.BorderLayout;
import org.gwt.mosaic.ui.client.layout.BorderLayoutData;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.MessageCallback;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.bus.client.framework.ClientMessageBus;
import org.jboss.errai.bus.client.protocols.MessageParts;
import org.jboss.errai.bus.client.security.SecurityService;
import org.jboss.errai.workspaces.client.api.ToolSet;
import org.jboss.errai.workspaces.client.framework.Preferences;
import org.jboss.errai.workspaces.client.framework.Registry;
import org.jboss.errai.workspaces.client.framework.WorkspaceBuilder;
import org.jboss.errai.workspaces.client.framework.WorkspaceConfig;
import org.jboss.errai.workspaces.client.layout.WSLayoutPanel;
import org.jboss.errai.workspaces.client.modules.auth.AuthenticationModule;
import org.jboss.errai.workspaces.client.protocols.LayoutCommands;
import org.jboss.errai.workspaces.client.protocols.LayoutParts;
import org.jboss.errai.workspaces.client.util.WorkspaceLogger;

import java.util.List;

import static com.google.gwt.core.client.GWT.create;

/**
 * The errai workspace implementation
 */
public class Application implements EntryPoint {

  private Viewport viewport;
  private WSLayoutPanel mainLayout;
  private Menu menu;
  private Header header;
  private Workspace workspace;

  private AuthenticationModule authenticationModule;
  private static SecurityService securityService;

  protected ClientMessageBus bus = (ClientMessageBus) ErraiBus.get();

  private static String deferredToken = null;

  private boolean isInitialized = false;

  public Application()
  {
    authenticationModule = new AuthenticationModule();
    securityService = new SecurityService();

    // global service registry
    Registry.set(SecurityService.class, securityService);
    Registry.set(AuthenticationModule.class, authenticationModule);
    Registry.set(Preferences.class, GWT.create(Preferences.class));
  }

  public void onModuleLoad() {
    Log.setUncaughtExceptionHandler();

    DeferredCommand.addCommand(new Command() {
      public void execute() {
        onModuleLoad2();
      }
    });
  }

  private void onModuleLoad2()
  {       
    final ClientMessageBus bus = (ClientMessageBus)ErraiBus.get();

    // Don't do any of this until the MessageBus is fully initialized.    
    bus.addPostInitTask(
        new Runnable() {

          public void run() {
            // Declare the standard error client here.
            bus.subscribe("ClientErrorService",
                new MessageCallback() {

                  public void callback(Message message) {
                    String errorMessage = message.get(String.class, MessageParts.ErrorMessage);
                    PopupPanel popup = new PopupPanel();
                    popup.add(new HTML(errorMessage));
                    popup.center();
                  }
                }
            );

            // The main workspace listener            
            bus.subscribe(Workspace.SUBJECT, new MessageCallback() {

              public void callback(Message message)
              {
                switch(LayoutCommands.valueOf(message.getCommandType()))
                {
                  case Initialize:

                    GWT.runAsync(
                        new RunAsyncCallback()
                        {
                          public void onFailure(Throwable throwable)
                          {
                            GWT.log("Failed to load workspace", throwable);
                          }

                          public void onSuccess()
                          {
                            initializeUI();
                          }
                        }
                    );

                    break;
                }
              }
            });

            authenticationModule.start();
          }
        });


    header = new Header(); // stateful

    // initial history token
    deferredToken = History.getToken();    
  }  

  private void initializeUI()
  {
    if(isInitialized)
    {
      // TODO: https://jira.jboss.org/jira/browse/ERRAI-39
      GWT.log("Workspace already initialized", new IllegalArgumentException("Received init call on already bootstrapped workspace"));
      return;
    }
    
    viewport = new Viewport();

    mainLayout = new WSLayoutPanel(new BorderLayout());

    menu = new Menu();
    workspace = Workspace.createInstance(menu);

    mainLayout.add(menu, new BorderLayoutData(BorderLayout.Region.WEST, "180 px", false));
    mainLayout.add(header, new BorderLayoutData(BorderLayout.Region.NORTH, "50 px"));
    mainLayout.add(workspace, new BorderLayoutData(BorderLayout.Region.CENTER, false));

    CaptionLayoutPanel logPanel = createLogPanel(mainLayout);
    mainLayout.add(logPanel, new BorderLayoutData(BorderLayout.Region.SOUTH, "210 px", true));
    mainLayout.setCollapsed(logPanel, true);

    WorkspaceBuilder builder = new WorkspaceBuilder();
    WorkspaceConfig config = create(WorkspaceConfig.class);
    config.configure(builder);

    // populate workspace
    builder.build(workspace);

    // finally attach the main layout to the viewport
    viewport.getLayoutPanel().add(mainLayout);
    RootPanel.get().add(viewport);

    // show default toolset
    DeferredCommand.addCommand(
        new Command()
        {
          public void execute()
          {
            String initialToolSet = null;
            String initialTool = null;

            // init by history token
            if(deferredToken!=null && deferredToken.startsWith("errai_"))
            {
              String[] token = Workspace.splitHistoryToken(deferredToken);

              initialToolSet = token[0];
              initialTool = token[1].equals("none") ? null : token[1];
            }

            // init by preferences
            if(null==initialToolSet)
            {
              Preferences prefs = GWT.create(Preferences.class);
              String preferedTool = prefs.has(Preferences.DEFAULT_TOOL) ?
                  Workspace.encode(prefs.get(Preferences.DEFAULT_TOOL)) : null;

              if(preferedTool!=null && workspace.hasToolSet(preferedTool))
              {
                initialToolSet = preferedTool;
              }
              else
              {
                // launch first available tool
                List<ToolSet> toolSets = workspace.getToolsets();
                if(toolSets.size()>0)
                {
                  initialToolSet = Workspace.encode(toolSets.get(0).getToolSetName());                   
                }
              }
            }

            // activate default tool
            if(initialToolSet!=null)
            {
              MessageBuilder.createMessage()
                  .toSubject(Workspace.SUBJECT)
                  .command(LayoutCommands.ActivateTool)
                  .with(LayoutParts.TOOLSET, initialToolSet)
                  .with(LayoutParts.TOOL, initialTool)
                  .noErrorHandling()
                  .sendNowWith(ErraiBus.get()
                  );
            }

            refreshView();
          }
        }
    );

    isInitialized = true;
  }

  private CaptionLayoutPanel createLogPanel(final WSLayoutPanel parent)
  {
    final CaptionLayoutPanel messagePanel = new CaptionLayoutPanel("Messages");
    //messagePanel.setPadding(0);
    
    // log panel
    // manually, otherwise it will appear on the login screen
    WorkspaceLogger divLogger = new WorkspaceLogger();
    Widget widget = divLogger.getWidget();
    widget.setVisible(true);
    Log.addLogger(divLogger);

    messagePanel.add(widget);

    final ImageButton collapseBtn = new ImageButton(Caption.IMAGES.toolCollapseDown());
    messagePanel.getHeader().add(collapseBtn, Caption.CaptionRegion.RIGHT);

    collapseBtn.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        parent.setCollapsed(messagePanel, true);
        parent.invalidate();
        parent.layout();
      }
    });

    return messagePanel;
  }

  /**
   * hack in order to correctly display widgets that have
   * been rendered hidden
   */
  public void refreshView()
  {
    viewport.getLayoutPanel().layout();
  }

  public static void forceReload() {
    reload();
  }

  private native static void reload() /*-{
       $wnd.location.reload();
     }-*/;

  private native static void _initAfterWSLoad() /*-{
         try {
             $wnd.initAfterWSLoad();
         }
         catch (e) {
         }
     }-*/;

}
