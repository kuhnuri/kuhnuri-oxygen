package de.axxepta.oxygen.api;

/**
 * @author Markus on 25.09.2016.
 */
public class ArgonConst {

    // PROTOCOL NAMES
    public static final String ARGON = "argon";
    public static final String ARGON_XQ = "argonquery";
    public static final String ARGON_REPO = "argonrepo";

    // DATABASE NAMES
    public static final String ARGON_DB = "~argon";

    public static final String BACKUP_DB_BASE = "~history_";

    public static final String BACKUP_RESTXQ_BASE = "~history_~restxq/";
    public static final String BACKUP_REPO_BASE = "~history_~repo/";

    public static final String META_DB_BASE = "~meta_";
    public static final String META_RESTXQ_BASE = "~meta_~restxq/";
    public static final String META_REPO_BASE = "~meta_~repo/";

    public static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm";

    // RESOURCE NAMES
    public static final String META_TEMPLATE = "MetaTemplate.xml";
    public static final String LOCK_FILE = "~usermanagement";
    public static final String EMPTY_FILE = ".empty.xml";

    public static final String BXERR_PERMISSION = "BASX0001";
}
