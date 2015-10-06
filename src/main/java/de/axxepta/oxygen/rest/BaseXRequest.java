package de.axxepta.oxygen.rest;

import de.axxepta.oxygen.api.BaseXResource;
import de.axxepta.oxygen.api.BaseXSource;
import de.axxepta.oxygen.api.Connection;
import de.axxepta.oxygen.workspace.BaseXOptionPage;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Markus on 05.10.2015.
 */
public class BaseXRequest {
    private ArrayList<String> result;
    private String answer;

    public BaseXRequest(final String request, final BaseXSource source, final String path) {
        Connection connection = (new BaseXConnectionWrapper()).getConnection();
        if (connection != null) {
            switch (request) {
                case "list":
                    try {
                        result = new ArrayList<>();
                        BaseXResource[] resources = connection.list(source, path);
                        for (int i=0; i< resources.length; i++) {
                            String databaseEntry = resources[i].name;
                            result.add(databaseEntry);
                        }
                        for (int i=0; i<resources.length; i++) {
                            String type = resources[i].type.toString();
                            result.add(type);
                        }
                    } catch (IOException er) {
                        result = new ArrayList<String>();
                        er.printStackTrace();
                    }
                    answer = "";
                    break;
                case "delete":
                    try {
                        connection.delete(source, path);
                    } catch (IOException er) {
                        er.printStackTrace();
                    }
                    result = new ArrayList<String>();
                    answer = "";
                    break;
                case "parse":
                    try {
                        connection.parse(source, path);
                    } catch (IOException er) {
                        er.printStackTrace();
                    }
                    result = new ArrayList<String>();
                    answer = "";
                    break;
                case "unlock":
                    try {
                        connection.delete(source, path);
                    } catch (IOException er) {
                        er.printStackTrace();
                    }
                    result = new ArrayList<String>();
                    answer = "";
                    break;
                default: result = new ArrayList<String>();
                    answer = "";
            }
            try {
                connection.close();
            } catch (IOException er) {
                er.printStackTrace();
            }
        } else {
            result = new ArrayList<String>();
            answer = "";
        }
    }

    public ArrayList<String> getResult() {
        return result;
    }

    public String getAnswer() {
        return answer;
    }
}