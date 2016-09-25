package de.axxepta.oxygen.actions;

import de.axxepta.oxygen.api.BaseXConnectionWrapper;
import de.axxepta.oxygen.api.BaseXSource;
import de.axxepta.oxygen.api.Connection;
import de.axxepta.oxygen.tree.ArgonTree;
import de.axxepta.oxygen.tree.TreeUtils;
import de.axxepta.oxygen.utils.ConnectionWrapper;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

/**
 * @author Markus on 24.10.2015.
 */
public class DeleteAction extends AbstractAction {

    private ArgonTree tree;
    private TreeModel treeModel;

    public DeleteAction(String name, Icon icon, ArgonTree tree){
        super(name, icon);
        this.tree = tree;
        this.treeModel = tree.getModel();
    }

    public DeleteAction(ArgonTree tree){
        super();
        this.tree = tree;
        this.treeModel = tree.getModel();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TreePath[] paths = tree.getSelectionPaths();
        boolean deleteAll = false;
        PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
        if (paths != null) {
            for (TreePath path : paths) {
                BaseXSource source = TreeUtils.sourceFromTreePath(path);
                String db_path = TreeUtils.resourceFromTreePath(path);
                if ((source != null) && (!db_path.equals(""))) {
                    if (TreeUtils.isDB(path)) {
                        try (Connection connection = BaseXConnectionWrapper.getConnection()) {
                            if (!ConnectionWrapper.pathContainsLockedResource(source, db_path)) {
                                connection.drop(db_path);
                                ((DefaultTreeModel) treeModel).removeNodeFromParent((DefaultMutableTreeNode) path.getLastPathComponent());
                            } else {
                                pluginWorkspace.showInformationMessage("Path contains locked resource(s). Check in all files prior to deleting.");
                            }
                        } catch (Exception er) {
                            pluginWorkspace.showErrorMessage("Failed to delete database: " + er.getMessage());
                        }
                    } else if ((!(source == BaseXSource.DATABASE)) || (db_path.contains("/"))) {
                        if (((DefaultMutableTreeNode) path.getLastPathComponent()).getAllowsChildren()) {
                            if (tree.isCollapsed(path)) {
                                tree.expandPath(path);
                                tree.collapsePath(path);
                            } else {
                                tree.collapsePath(path);
                                tree.expandPath(path);
                            }
                        }
                        if (treeModel.getChildCount(path.getLastPathComponent()) == 0) {

                            int dialogResult = 0;
                            if (!deleteAll) {
                                dialogResult = pluginWorkspace.showConfirmDialog(
                                        "Delete Resource(s)",
                                        "Do you really want to delete the file\n" + TreeUtils.urlStringFromTreePath(path),
                                        new String[] {"Yes", "All", "No"},
                                        new int[] {0, 1, 2}, 0);
                            }
                            if ((deleteAll) || (dialogResult == 0) || (dialogResult == 1)) {
                                if (!ConnectionWrapper.pathContainsLockedResource(source, db_path)) {
                                    deleteFile(source, db_path, path);
                                } else {
                                    pluginWorkspace.showInformationMessage("Path contains locked resource(s). Check in all files prior to deleting.");
                                }
                                if (dialogResult == 1)
                                    deleteAll = true;
                            }

/*                            int dialogResult = JOptionPane.YES_OPTION;
                            if (!deleteAll) {
                                Object[] answerOptions = {"Yes", "All", "No"};
                                dialogResult = JOptionPane.showOptionDialog(null, "Do you really want to delete the file\n" +
                                                TreeUtils.urlStringFromTreePath(path) + "?", "Delete Resource(s)",
                                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, answerOptions,
                                        answerOptions[0]);
                            }
                            if ((deleteAll) || (dialogResult == JOptionPane.YES_OPTION)) {
                                deleteFile(source, db_path, path);
                            } else if (dialogResult == JOptionPane.NO_OPTION) {
                                deleteAll = true;
                                deleteFile(source, db_path, path);
                            }*/

                        } else {
                            pluginWorkspace.showInformationMessage("You cannot delete non-empty directories!");
                        }
                    }
                }
            }
        }
    }

    private void deleteFile(BaseXSource source, String db_path, TreePath path) {
        try (Connection connection = BaseXConnectionWrapper.getConnection()) {
            connection.delete(source, db_path);
            ((DefaultTreeModel) treeModel).removeNodeFromParent((DefaultMutableTreeNode) path.getLastPathComponent());
        } catch (Exception er) {
            PluginWorkspaceProvider.getPluginWorkspace().showInformationMessage("Failed to delete resource: " + er.getMessage());
/*            JOptionPane.showMessageDialog(null, "Failed to delete resource: " + er.getMessage(),
                    "BaseX Connection Error", JOptionPane.PLAIN_MESSAGE);*/
        }
    }

}
