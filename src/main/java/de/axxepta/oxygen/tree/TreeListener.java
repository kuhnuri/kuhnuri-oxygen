package de.axxepta.oxygen.tree;

import de.axxepta.oxygen.actions.AddDatabaseAction;
import de.axxepta.oxygen.actions.AddNewFileAction;
import de.axxepta.oxygen.actions.DeleteAction;
import de.axxepta.oxygen.actions.RefreshTreeAction;
import de.axxepta.oxygen.api.BaseXResource;
import de.axxepta.oxygen.api.BaseXSource;
import de.axxepta.oxygen.api.BaseXType;
import de.axxepta.oxygen.api.Resource;
import de.axxepta.oxygen.core.ClassFactory;
import de.axxepta.oxygen.core.ObserverInterface;
import de.axxepta.oxygen.utils.ConnectionWrapper;
import de.axxepta.oxygen.utils.Lang;
import de.axxepta.oxygen.utils.WorkspaceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Listener class observing all tree-related events
 */
public class TreeListener extends MouseAdapter implements TreeSelectionListener, TreeWillExpandListener,
        KeyListener, ObserverInterface {

    private static final Logger logger = LogManager.getLogger(TreeListener.class);
    private static final PluginWorkspace workspace = PluginWorkspaceProvider.getPluginWorkspace();

    private ArgonTree tree;
    private DefaultTreeModel treeModel;
    private TreePath path;
    private TreeNode node;
    private boolean showErrorMessages = true;
    private boolean newExpandEvent;
    private boolean singleClick = true;
    private boolean doubleClickExpandEnabled = true;
    private Timer timer;
    private final ArgonPopupMenu contextMenu;

    public TreeListener(ArgonTree tree, TreeModel treeModel, ArgonPopupMenu contextMenu) {
        this.tree = tree;
        this.treeModel = (DefaultTreeModel) treeModel;
        this.newExpandEvent = true;
        this.contextMenu = contextMenu;
        final ActionListener actionListener = e -> {
            timer.stop();
            if (singleClick) {
                singleClickHandler(e);
            } else {
                try {
                    doubleClickHandler(e);
                } catch (ParseException ex) {
                    logger.error(ex);
                }
            }
        };
        final int doubleClickDelay = 300;
        timer = new javax.swing.Timer(doubleClickDelay, actionListener);
        timer.setRepeats(false);
    }

    public void setShowErrorMessages(boolean show) {
        showErrorMessages = show;
    }

    /*
     * methods of MouseAdapter
     */

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
            singleClick = true;
            timer.start();
        } else {
            singleClick = false;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        logger.info("mouseReleased " + e.toString());
        path = tree.getPathForLocation(e.getX(), e.getY());
        logger.info("  path " + path);
        try {
            if (path != null) {
                logger.info("  node " + node);
                node = (TreeNode) path.getLastPathComponent();
            }
        } catch (NullPointerException er) {
            er.printStackTrace();
        }
        if (e.isPopupTrigger()) {
            contextMenu.show(e.getComponent(), e.getX(), e.getY(), path);
        }
    }


    /*
     * methods of interface TreeSelectionListener
     */

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        path = e.getNewLeadSelectionPath();
        node = (TreeNode) tree.getLastSelectedPathComponent();
    }


    /*
     * methods of interface TreeWillExpandListener
     */

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        logger.info("treeWillExpand "/* + event*/);
        // method is called twice, if new data is loaded--prevent database check in 2nd call
        final boolean newTreeExpandEvent = this.newExpandEvent;
        logger.info("   newTreeExpandEvent " + newTreeExpandEvent);
        path = event.getPath();
        logger.info("  path " + path);
        node = (TreeNode) path.getLastPathComponent();
        logger.info("  node " + node);
        int depth = path.getPathCount();
        logger.info("  depth " + depth);
        logger.info("  " + (depth > 1) + " && " + node.getAllowsChildren() + " && " + this.newExpandEvent);
        if (depth > 1 && node.getAllowsChildren() && this.newExpandEvent) {
            final BaseXSource source = TreeUtils.sourceFromTreePath(path);
            logger.info("  source " + source);
            String db_path;
            if (depth > 2) {    // get path in source
                db_path = (((String) ((ArgonTreeNode) node).getTag()).split(":/*"))[1] + "/";
            } else {
                db_path = "";
            }
            logger.info("  db_path " + db_path);

            List<Resource> childList;
            try {
                childList = ConnectionWrapper.list(source, db_path);
            } catch (Exception er) {
                childList = new ArrayList<>();
                logger.debug(er);
                String error = er.getMessage();
                if (error == null || error.equals("")) {
                    error = "Database connection could not be established.";
                }
                if (showErrorMessages) {
                    workspace.showInformationMessage(Lang.get(Lang.Keys.warn_failedlist) + "\n" + error);
                }
            }

            if (updateExpandedNode((MutableTreeNode) node, childList)) {
                logger.info("  newExpandEvent = false");
                this.newExpandEvent = false;
                printTree(tree);
                tree.expandPath(path);
            }

        }
        if (!newTreeExpandEvent) {
            logger.info("  newExpandEvent = true");
            this.newExpandEvent = true;
        }
        logger.info("   newTreeExpandEvent " + newTreeExpandEvent);
    }

    private void printTree(ArgonTree tree) {
        logger.info("printTree");
        logger.info("  rowCount " + tree.getRowCount());
        printTreeNode((DefaultMutableTreeNode) tree.getModel().getRoot(), "  ");
    }

    private void printTreeNode(DefaultMutableTreeNode node, String indent) {
        logger.info(indent + " * " + node.toString() + " {level=" + node.getLevel() + " depth=" + node.getDepth() + " }");
        for (int i = 0; i < node.getChildCount(); i++) {
            printTreeNode((DefaultMutableTreeNode) node.getChildAt(i), indent + "  ");
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    }


    /*
     * methods for MouseAdapter
     */

    private void singleClickHandler(ActionEvent e) {
        logger.debug("-- single click --");
    }

    private void doubleClickHandler(ActionEvent e) throws ParseException {
        logger.debug("-- double click --");
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                if (((TreeNode) path.getLastPathComponent()).getAllowsChildren()) {
                    if (doubleClickExpandEnabled) {
                        try {
                            treeWillExpand(new TreeExpansionEvent(this, path));
                        } catch (ExpandVetoException eve) {
                            logger.debug("Expand Veto: ", eve.getMessage());
                        }
                    }
                } else {
                    doubleClickAction(path);
                }
            }
        }
    }

    // made public for access with AspectJ
    @SuppressWarnings("all")
    public static void doubleClickAction(TreePath path) {
        //String db_path = ((ArgonTreeNode) path.getLastPathComponent()).getTag();
        String dbPath = TreeUtils.urlStringFromTreePath(path);
        logger.info("DbPath: " + dbPath);
        WorkspaceUtils.openURLString(dbPath);
    }

    /*
     * method for interface Observer
     */

    @Override
    public void update(String type, Object... message) {
        // is notified as observer when changes have been made to the database file structure
        // updates the tree if necessary
        TreeNode currNode;
        TreePath currPath;

        logger.info("Tree needs to update: " + message[0]);

        if (type.equals("SAVE_FILE") || type.equals("NEW_DIR")) {
            String[] protocol = ((String) message[0]).split(":/*");
            String[] path = protocol[1].split("/");
            currPath = new TreePath(treeModel.getRoot());
            switch (protocol[0]) {
//                case ArgonConst.ARGON_REPO:
//                    currPath = TreeUtils.pathByAddingChildAsStr(currPath, Lang.get(Lang.Keys.tree_repo));
//                    break;
//                case ArgonConst.ARGON_XQ:
//                    currPath = TreeUtils.pathByAddingChildAsStr(currPath, Lang.get(Lang.Keys.tree_restxq));
//                    break;
                default:
                    currPath = TreeUtils.pathByAddingChildAsStr(currPath, Lang.get(Lang.Keys.tree_DB));
            }
            currNode = (TreeNode) currPath.getLastPathComponent();
            boolean expanded = false;
            Boolean isFile;
            for (int i = 0; i < path.length; i++) {
                if (tree.isExpanded(currPath)) expanded = true;
                if (expanded || (i == path.length - 1)) { // update tree now only if file is in visible path
                    if (TreeUtils.isNodeAsStrChild(currNode, path[i]) == -1) {
                        isFile = (i + 1 == path.length) && type.equals("SAVE_FILE");
                        TreeUtils.insertStrAsNodeLexi(treeModel, path[i], (DefaultMutableTreeNode) currNode, isFile);
                        treeModel.reload(currNode);
                    }
                    currPath = TreeUtils.pathByAddingChildAsStr(currPath, path[i]);
                    currNode = (DefaultMutableTreeNode) currPath.getLastPathComponent();
                } else {
                    break;
                }
            }
        }
    }

    /*
     * other methods
     */

    public void setDoubleClickExpand(boolean expand) {
        doubleClickExpandEnabled = expand;
    }

    private boolean updateExpandedNode(MutableTreeNode node, List<Resource> newChildrenList) {
        logger.info("updateExpandedNode");
        final Set<String> childrenValues = newChildrenList.stream()
                .map(child -> child.name)
                .collect(Collectors.toSet());

        DefaultMutableTreeNode newChild;
        final List<String> oldChildren = new ArrayList<>();
        String oldChild;
        boolean treeChanged = false;

        // check whether old children are in new list and vice versa
        if (node.getChildCount() > 0) {
            logger.info("  find old nodes");
            boolean[] inNewList = new boolean[node.getChildCount()];
            if (newChildrenList.size() > 0) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    final DefaultMutableTreeNode currNode = (DefaultMutableTreeNode) node.getChildAt(i);
                    oldChild = currNode.getUserObject().toString();
                    oldChildren.add(oldChild);
                    if (childrenValues.contains(oldChild)) {
                        logger.info("  found old child " + oldChild);
                        inNewList[i] = true;
                    }
                }
            }
            for (int i = node.getChildCount() - 1; i > -1; i--) {
                if (!inNewList[i]) {
                    logger.info("  remove node " + i);
                    treeModel.removeNodeFromParent((MutableTreeNode) node.getChildAt(i));
                    treeModel.nodeChanged(node);
                    treeChanged = true;
                }
            }
        }
        logger.info("updateExpandedNode node.getChildCount() == " + node.getChildCount());
        if (node.getChildCount() == 0) {  // if old list was empty skip lexicographic insert (faster)
            logger.info("  no old children");
            for (Resource newPossibleChild : newChildrenList) {
                final String url = ((ArgonTreeNode) node).getTag().toString() + "/" + newPossibleChild.name;
                logger.info("  adding " + newPossibleChild + " " + url);
                newChild = ClassFactory.getInstance().getTreeNode(newPossibleChild.name, url);
                newChild.setAllowsChildren(newPossibleChild.type == BaseXType.DIRECTORY);
                logger.info("  insert " + newChild + " " + node.getChildCount());
                treeModel.insertNodeInto(newChild, node, node.getChildCount());
                logger.info("  after " + node.getChildCount());
                treeChanged = true;
            }
        } else {
            logger.info("  has old children");
            for (Resource newPossibleChild : newChildrenList) {
                logger.info("  adding " + newPossibleChild);
                if (!oldChildren.contains(newPossibleChild.name)) {
                    TreeUtils.insertStrAsNodeLexi(treeModel, newPossibleChild.name, (DefaultMutableTreeNode) node,
                            newPossibleChild.type != BaseXType.DIRECTORY);
                    treeChanged = true;
                }
            }
        }
        return treeChanged;
    }

    public TreePath getPath() {
        return this.path;
    }

    public TreeNode getNode() {
        return this.node;
    }


    /*
     * methods for interface KeyListener
     */

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_DELETE:
                new DeleteAction(tree).actionPerformed(null);
                break;
            case KeyEvent.VK_F5:
                new RefreshTreeAction(tree).actionPerformed(null);
                break;
            case KeyEvent.VK_ENTER:
                try {
                    doubleClickHandler(null);
                } catch (ParseException pe) {
                    // if URL raises exc    eption, just ignore
                }
                break;
            case KeyEvent.VK_INSERT:
                if (TreeUtils.isDir(path) || TreeUtils.isDB(path) || TreeUtils.isFileSource(path)) {
                    new AddNewFileAction(tree).actionPerformed(null);
                    break;
                }
                if (TreeUtils.isDbSource(path)) {
                    new AddDatabaseAction().actionPerformed(null);
                    break;
                }
                break;
            default:
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
