package de.axxepta.oxygen.actions;

import de.axxepta.oxygen.api.BaseXConnectionWrapper;
import de.axxepta.oxygen.api.BaseXSource;
import de.axxepta.oxygen.api.Connection;
import de.axxepta.oxygen.customprotocol.CustomProtocolURLHandlerExtension;
import de.axxepta.oxygen.tree.TreeListener;
import de.axxepta.oxygen.tree.TreeUtils;
import de.axxepta.oxygen.utils.WorkspaceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.sync.exml.plugin.lock.LockException;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Markus on 04.12.2015.
 * This action should be called from a context menu in the database tree.
 * It initiates an update of the version (and revision) number in BaseX.
 * If the selected file is opened in an editor window (not necessarily the current one), the file will be saved
 */
public class NewVersionContextAction extends AbstractAction {

    private static final Logger logger = LogManager.getLogger(NewVersionContextAction.class);
    private StandalonePluginWorkspace pluginWorkspaceAccess;

    final private TreeListener treeListener;

    public NewVersionContextAction(String name, Icon icon, TreeListener treeListener,
                                   final StandalonePluginWorkspace pluginWorkspaceAccess){
        super(name, icon);

        this.treeListener = treeListener;
        this.pluginWorkspaceAccess = pluginWorkspaceAccess;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // ToDo: proper exception handling
        TreePath path = treeListener.getPath();
        String urlString = TreeUtils.urlStringFromTreePath(path);

        BaseXSource source = TreeUtils.sourceFromTreePath(path);
        String protocol = CustomProtocolURLHandlerExtension.protocolFromSource(source);
        CustomProtocolURLHandlerExtension handlerExtension = new CustomProtocolURLHandlerExtension();
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e1) {
            logger.error(e1);
        }

        if (handlerExtension.canCheckReadOnly(protocol) && !handlerExtension.isReadOnly(url)) {
            boolean urlOpenedInEditor = false;
            byte[] isByte;

            WSEditor editorAccess =                      // might change active editor (expected behavior?!)
                    pluginWorkspaceAccess.getEditorAccess(url, StandalonePluginWorkspace.MAIN_EDITING_AREA);
            if (editorAccess == null) {     // get data from file
                try {
                    handlerExtension.getLockHandler().updateLock(url, 1000);
                } catch (LockException lEx) {
                    logger.error(lEx);
                }
                try (Connection connection = BaseXConnectionWrapper.getConnection()) {
                    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(connection.get(source,
                            CustomProtocolURLHandlerExtension.pathFromURL(url)))) {
                        int l = inputStream.available();

                        isByte = new byte[l];
                        //noinspection ResultOfMethodCallIgnored
                        inputStream.read(isByte);
                    } catch (IOException er) {
                        logger.error(er);
                        isByte = new byte[0];
                    }
                } catch (IOException ex) {
                    logger.error(ex);
                    isByte = new byte[0];
                }
            } else {                        // get data from editor window
                urlOpenedInEditor = true;
                isByte = WorkspaceUtils.getEditorByteContent(editorAccess);
            }

            NewVersionButtonAction.updateFile(source, url, isByte);

            if (!urlOpenedInEditor) {
                try {
                    handlerExtension.getLockHandler().unlock(url);
                } catch (LockException lEx) {
                    logger.error(lEx);
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Couldn't update version of file\n" + urlString +
                    ".\n File is locked by other user.", "Update Version Message", JOptionPane.PLAIN_MESSAGE);
        }

    }

}
