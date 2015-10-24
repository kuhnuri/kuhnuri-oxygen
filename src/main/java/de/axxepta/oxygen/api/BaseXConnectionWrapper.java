package de.axxepta.oxygen.api;

import de.axxepta.oxygen.workspace.BaseXOptionPage;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Wrapper class for connection with BaseX server, loading authentication data from Options
 */
public class BaseXConnectionWrapper {

    static Connection connection;

    public static void refreshFromOptions(){

        String host = BaseXOptionPage.getOption(BaseXOptionPage.KEY_BASEX_HOST);
        String user = BaseXOptionPage.getOption(BaseXOptionPage.KEY_BASEX_USERNAME);
        String pass = BaseXOptionPage.getOption(BaseXOptionPage.KEY_BASEX_PASSWORD);
        int port = Integer.parseInt(BaseXOptionPage.getOption(BaseXOptionPage.KEY_BASEX_HTTP_PORT));
        int tcpport = Integer.parseInt(BaseXOptionPage.getOption(BaseXOptionPage.KEY_BASEX_TCP_PORT));

        String connType;
        if (BaseXOptionPage.getOption(BaseXOptionPage.KEY_BASEX_CONNECTION).equals("HTTP")) {
            connType = "REST";
        } else {
            connType = "CLIENT";
        }

        if (connType.equals("REST")) {
            try {
                connection = new RestConnection(host, port, user, pass);
            } catch (MalformedURLException er) {
                connection = null;
            }
        } else {
            try {
                connection = new ClientConnection(host, tcpport, user, pass);
            } catch (IOException er) {
                connection = null;
            }
        }
    }

    public static Connection getConnection(){
        return connection;
    }
}
