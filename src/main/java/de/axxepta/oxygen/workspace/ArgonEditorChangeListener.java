package de.axxepta.oxygen.workspace;

import de.axxepta.oxygen.api.BaseXConnectionWrapper;
import de.axxepta.oxygen.api.BaseXSource;
import de.axxepta.oxygen.api.Connection;
import de.axxepta.oxygen.api.TopicHolder;
import de.axxepta.oxygen.customprotocol.ArgonEditorsWatchMap;
import de.axxepta.oxygen.customprotocol.CustomProtocolURLHandlerExtension;
import de.axxepta.oxygen.utils.URLUtils;
import de.axxepta.oxygen.versioncontrol.VersionHistoryUpdater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;


class ArgonEditorChangeListener extends WSEditorChangeListener {

    private static final Logger logger = LogManager.getLogger(ArgonEditorChangeListener.class);

    private StandalonePluginWorkspace pluginWorkspaceAccess;
    private ArgonToolbarComponentCustomizer toolbarCustomizer;

    ArgonEditorChangeListener(StandalonePluginWorkspace pluginWorkspace, ArgonToolbarComponentCustomizer toolbarCustomizer) {
        super();
        this.pluginWorkspaceAccess = pluginWorkspace;
        this.toolbarCustomizer = toolbarCustomizer;
    }

    @Override
    public void editorPageChanged(URL editorLocation) {
        toolbarCustomizer.checkEditorDependentMenuButtonStatus(pluginWorkspaceAccess);
    }

    @Override
    public void editorSelected(URL editorLocation) {
        toolbarCustomizer.checkEditorDependentMenuButtonStatus(pluginWorkspaceAccess);
        TopicHolder.changedEditorStatus.postMessage(VersionHistoryUpdater.checkVersionHistory(editorLocation));
    }

    @Override
    public void editorActivated(URL editorLocation) {
        toolbarCustomizer.checkEditorDependentMenuButtonStatus(pluginWorkspaceAccess);
        TopicHolder.changedEditorStatus.postMessage(VersionHistoryUpdater.checkVersionHistory(editorLocation));
    }

    @Override
    public void editorClosed(URL editorLocation) {
        if (editorLocation.toString().startsWith(CustomProtocolURLHandlerExtension.ARGON))
            ArgonEditorsWatchMap.removeURL(editorLocation);
        toolbarCustomizer.checkEditorDependentMenuButtonStatus(pluginWorkspaceAccess);
        try (Connection connection = BaseXConnectionWrapper.getConnection()) {
            BaseXSource source = CustomProtocolURLHandlerExtension.sourceFromURL(editorLocation);
            String path = CustomProtocolURLHandlerExtension.pathFromURL(editorLocation);
            if (connection.lockedByUser(source, path)) {
                int checkInFile = JOptionPane.showConfirmDialog(null, "You just closed a checked out file.\n" +
                        "Do you want to check it in?", "Closed checked out file", JOptionPane.YES_NO_OPTION);
                if (checkInFile == JOptionPane.YES_OPTION) {
                    connection.unlock(source, path);
                }
            }
        } catch (IOException ioe) {
            logger.debug(ioe.getMessage());
        }
    }

    @Override
    public void editorOpened(URL editorLocation) {
        logger.debug("editor opened: " + editorLocation.toString());
        if (editorLocation.toString().startsWith(CustomProtocolURLHandlerExtension.ARGON))
            ArgonEditorsWatchMap.addURL(editorLocation);
        toolbarCustomizer.checkEditorDependentMenuButtonStatus(pluginWorkspaceAccess);
        TopicHolder.changedEditorStatus.postMessage(VersionHistoryUpdater.checkVersionHistory(editorLocation));

        final WSEditor editorAccess = pluginWorkspaceAccess.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
        boolean isArgon = URLUtils.isArgon(editorLocation);

        if (isArgon)
            editorAccess.addEditorListener(new ArgonEditorListener(pluginWorkspaceAccess));

        if (isArgon && URLUtils.isQuery(editorLocation))
            editorAccess.addValidationProblemsFilter(new ArgonValidationProblemsFilter(editorAccess));
    }

}