package de.axxepta.oxygen.actions;

import de.axxepta.oxygen.api.BaseXConnectionWrapper;
import de.axxepta.oxygen.api.BaseXSource;
import de.axxepta.oxygen.api.Connection;
import de.axxepta.oxygen.customprotocol.ArgonEditorsWatchMap;
import de.axxepta.oxygen.customprotocol.CustomProtocolURLHandlerExtension;
import de.axxepta.oxygen.tree.TreeListener;
import de.axxepta.oxygen.tree.TreeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA;


/**
 * @author Markus on 07.06.2016
 */
public class CheckInAction extends AbstractAction {

    private static final Logger logger = LogManager.getLogger(CheckInAction.class);

    private final TreeListener treeListener;

    public CheckInAction(String name, Icon icon, TreeListener treeListener) {
        super(name, icon);
        this.treeListener = treeListener;
    }

    public CheckInAction(String name, Icon icon) {
        this(name, icon, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        if (this.urlString != null) {
//            checkOut(this.urlString);
//        } else
        if (treeListener == null) {
            final URL url = PluginWorkspaceProvider.getPluginWorkspace().
                    getCurrentEditorAccess(MAIN_EDITING_AREA).getEditorLocation();
            checkIn(url);
        } else if (!treeListener.getNode().getAllowsChildren()) {
            final String urlString = TreeUtils.urlStringFromTreePath(treeListener.getPath());
            try {
                checkIn(new URL(urlString));
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    static void checkIn(URL url) {
        final BaseXSource source = CustomProtocolURLHandlerExtension.sourceFromURL(url);
        final String path = CustomProtocolURLHandlerExtension.pathFromURL(url);
        try (Connection connection = BaseXConnectionWrapper.getConnection()) {
            if (connection.lockedByUser(source, path)) {
                ArgonEditorsWatchMap.getInstance().setAskedForCheckIn(url, true);
//                final WSEditor editorAccess = PluginWorkspaceProvider.getPluginWorkspace().
//                        getEditorAccess(url, MAIN_EDITING_AREA);
//                if (editorAccess != null) {
//                    editorAccess.close(true);
//                }
                connection.unlock(source, path);
            }
        } catch (IOException ex) {
            logger.debug(ex);
        }
    }

}
