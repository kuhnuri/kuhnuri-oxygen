package de.axxepta.oxygen.rest;

import de.axxepta.oxygen.api.*;
import de.axxepta.oxygen.tree.TreeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class for request to BaseX, connection details are "inherited" from the included connection
 */
public class BaseXRequest {

    private static final Logger logger = LogManager.getLogger(BaseXRequest.class);

    private List<String> result;
    private boolean check;

    public BaseXRequest(final String request, final BaseXSource source, final String path,
                        final String... params) throws IOException {
        try (Connection connection = BaseXConnectionWrapper.getConnection()) {
            if (connection != null) {
                switch (request) {

                    case "parse":
                        result = new ArrayList<>();
                        check = false;
                        try {
                            connection.parse(path);
                        } catch (BaseXQueryException ex) {
                            result.add(Integer.toString(ex.getLine()));
                            result.add(Integer.toString(ex.getColumn()));
                            result.add(ex.getInfo());
                            break;
                        }
                        check = true;
                        break;

                    case "look":
                        check = false;
                        StringBuilder regEx = new StringBuilder("");
                        for (int i = 0; i < params[0].length(); i++) {
                            char c = params[0].charAt(i);
                            switch (c) {
                                case '*':
                                    regEx.append(".*");
                                    break;
                                case '?':
                                    regEx.append('.');
                                    break;
                                case '.':
                                    regEx.append("\\.");
                                    break;
                                default:
                                    regEx.append(c);
                            }
                        }
                        String regExString = regEx.toString();
                        result = connection.search(source, path, regExString);
                        for (int i = result.size() - 1; i > -1; i--) {
                            String foundPath = result.get(i);
                            String foundFile = TreeUtils.fileStringFromPathString(foundPath);
                            Matcher matcher = Pattern.compile(regExString).
                                    matcher(foundFile);
                            if (!matcher.find())
                                result.remove(foundPath);
                        }
                        break;

                    default:
                        result = new ArrayList<>();
                        check = false;
                }
            }

        } catch (IOException er) {
            logger.warn("Argon connection exception", er.getMessage());
            result = new ArrayList<>();
            throw new IOException(er.getMessage());
        }
    }

    public List<String> getResult() {
        return result;
    }

    public boolean isCheck() {
        return check;
    }
}
