/**
 *
 */
package de.axxepta.oxygen.customprotocol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.axxepta.oxygen.api.BaseXClient;

/**
 * @author Daniel Altiparmak
 *
 *
 */
public class BaseXFilterOutputStream extends FilterOutputStream {
	
	 // Define a static logger variable so that it references the
    // Logger instance named "CustomProtocolHandler".
    private static final Logger logger = LogManager.getLogger(BaseXFilterOutputStream.class);

    //private ArrayList<Byte> bytes = new ArrayList<Byte>();
    private URL url;
    private File temp;

    /*
     * Helper method to read file content and return its string
     */
    public String readFile(String filename) {
        File f = new File(filename);
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            return new String(bytes, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Constructor
     */
    public BaseXFilterOutputStream(OutputStream out) {
        super(out);
        // TODO Auto-generated constructor stub
    }

    public BaseXFilterOutputStream(OutputStream out, URL url) {
        super(out);
        this.url = url;
    }

    public BaseXFilterOutputStream(OutputStream out, File temp, URL url) {
        super(out);
        this.temp = temp;
        this.url = url;
    }


    /**
     * @see java.io.FilterOutputStream#write(byte[])
     */
    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
    }

    /**
     * @see java.io.FilterOutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {

        //TODO use direct bytes array instead of making file operations
        /*
         * @TODO
         *
        for (int i = 0; i < b.length; i++) {
            this.bytes.add(b[i]);
        }
        */
        super.write(b, off, len);
    }

    /**
     * @see java.io.FilterOutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException {
        super.write(b);
    }

    @Override
    public void close() throws IOException {
        super.close();

        // custom BaseX Operations
        // create session
        final BaseXClient session = new BaseXClient("localhost", 1984, "admin","admin");

        // get content from temporary file
        String content = this.readFile(temp.getAbsolutePath());

        // define input stream
        InputStream bais = new ByteArrayInputStream(content.getBytes());

        // split up url string into important parts
        String argonUrlPath = this.url.getPath();
        argonUrlPath = argonUrlPath.replaceFirst("/", "");
        String[] parts = argonUrlPath.split("/", 2);
        String database = parts[0];
        String path = parts[1];

        logger.info("Database: " + database);
        logger.info("Path: " + path);

        // open BaseX database
        session.execute(MessageFormat.format("open {0}", database));
        logger.info(session.info());

        /*
         * byte[] primitive = new byte[this.bytes.size()];
         *
         * for (int i = 0; i < this.bytes.size(); i++) { Byte temp =
         * this.bytes.get(i); primitive[i] = temp.byteValue(); }
         *
         * String decoded = new String(primitive, "UTF-8");
         * System.out.println(decoded);
         *
         * InputStream bais = new ByteArrayInputStream(primitive);
         *
         * String argonUrlPath = this.url.getPath(); argonUrlPath =
         * argonUrlPath.replaceFirst("/", ""); String[] parts =
         * argonUrlPath.split("/", 2); String database = parts[0]; String path =
         * parts[1];
         *
         * System.out.println("----------------------");
         * System.out.println("Database: " + database);
         * System.out.println("Path: " + path);
         *
         *
         * session.execute(MessageFormat.format("open {0}", database));
         * System.out.println(session.info());
         */

        try {
            // replace document
            session.replace(path, bais);
            logger.info(session.info());

        } catch (Exception e1) {
            // TODO Auto-generated catch block
        	logger.error(e1);
        }

        boolean bool = false;
        try {
            // tries to delete the newly created file
            bool = temp.delete();
            // print
            logger.info("File deleted: " + bool);

        } catch (Exception e) {
            // if any error occurs
            logger.error(e);
        }
        session.close();

    }

}