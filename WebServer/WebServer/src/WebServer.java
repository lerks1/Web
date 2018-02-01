import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import com.sun.net.httpserver.*;
import java.nio.file.Files;

//Webserver class written by Dr Michael Homer. Edited by John Lerke.
public class WebServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 10);
        server.createContext("/record", new RecordInputHandler());
        server.createContext("/scoretable", new ScoreTable());
        server.createContext("/", new StaticHandler("web"));
        server.setExecutor(null);
        System.out.println("Starting server on http://localhost:8080/ ...");
        server.start();
    }
    
    //Serves an HTML page containing the database data represented in a table.
    static class ScoreTable implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Headers headers = t.getResponseHeaders();
            headers.set("Content-type", "text/html; charset=utf-8");
            String response = "<html><head></head><link rel='stylesheet' href='style2.css'><body>";
            response += "<center><H1 id='title'>Records!</H1><table style='width:30%' id = 'table'><tr><th>Name</th><th>Weight</th></tr>";
            for (Score s : new DBAccessor().pullFigures())
            {
            	response += "<tr><td class = 'data'>" + s.getName() + "</td><td class = 'data'>" + s.getWeight() + "</td></tr>";
            }
            response += "</table></form><button type='button' id ='button' class ='button'>Home</button></center></body></html><script type='text/javascript' src='scores.js'></script>";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } 
    }
    
    //Takes in a post containing records and adds them to the database.
    static class RecordInputHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException 
        {
        String name = "";
        Double weight = 0.0;
        InputStream is = t.getRequestBody();
        byte[] body = WebServer.readStream(is);
        String postData = new String(body, "UTF-8");
        Map<String, String> params = WebServer.parseQuery(postData);
        name = params.get("name");
        weight = Double.parseDouble(params.get("weight"));
        DBAccessor dbr = new DBAccessor();
        dbr.pushFigures(name, weight);
        } 
    } 
    
    // StaticHandler implements a traditional file-based web server.
    // It keeps track of a root directory to search in, and looks up
    // any files that are requested within that, then just sends them
    // as-is back to the web browser.
    static class StaticHandler implements HttpHandler {

        private boolean verbose = true;
        private File root;

        public StaticHandler(String path) {
            root = new File(path);
        }

        public void handle(HttpExchange t) throws IOException {
            log(t.getRequestMethod() + " " + t.getRequestURI());
            InputStream is = t.getRequestBody();
            // If we don't read the whole body, things can break, but we're
            // not using it at the moment.
            byte[] body = WebServer.readStream(is);
            // We want to take the request path and strip out the context's
            // prefix set up earlier, and any extra / characters, so that
            // we can use it as a filename.
            String prefix = t.getHttpContext().getPath();
            String filename = t.getRequestURI().getPath().substring(prefix.length());
            while (filename.startsWith("/"))
                filename = filename.substring(1);
            // Create a new File object representing this file inside
            // that root, and check that it exists.
            File file = new File(root, filename);
            if (!file.exists() || (filename.indexOf("..") != -1)) {
                WebServer.error(404, "Not Found", t);
                return;
            }
            if (file.isDirectory()) {
                serveDirectory(file, t);
            } else {
                serveFile(file, t);
            }
        } 

        // Serves out an individual file as read from the disk to the
        // browser, assuming that it exists.
        private void serveFile(File file, HttpExchange t) throws IOException {
            Headers headers = t.getResponseHeaders();
            headers.set("Content-type", getMimeType(file.getName()));
            log("--> serving file as " + getMimeType(file.getName()));
            byte[] response = Files.readAllBytes(file.toPath());
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }

        // Serves out directory dir as an HTML page with links to
        // each file in the directory.
        private void serveDirectory(File dir, HttpExchange t) throws IOException {
            log("--> serving directory");
            File index = new File(dir, "index.html");
            if (index.exists()) {
                log("--> found index");
                serveFile(index, t);
                return;
            }
            Headers headers = t.getResponseHeaders();
            headers.set("Content-type", "text/html; charset=utf-8");
            File[] files = dir.listFiles();
            String response = "<html><head></head><body>"
            		+ "<h1>Directory " + dir.getName() + "</h1>"
            		+ "<ul>";
            for (File f : files) {
            	String n = f.getName() + (f.isDirectory() ? "/" : "");
                response += "<li><a href=\"" + n + "\">" + n + "</a></li>";
            }
            response += "</ul></body></html>";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        // Pick a suitable content-type header for a file based on its
        // extension. This list is incomplete and you might find you
        // need to extend it.
        // See <https://en.wikipedia.org/wiki/Media_type>
        private String getMimeType(String name) {
            if (name.endsWith(".txt"))  return "text/plain";
            if (name.endsWith(".html")) return "text/html; charset=utf-8";
            if (name.endsWith(".htm"))  return "text/html; charset=utf-8";
            if (name.endsWith(".css"))  return "text/css";
            if (name.endsWith(".js"))   return "text/javascript";
            if (name.endsWith(".png"))  return "image/png";
            if (name.endsWith(".jpg"))  return "image/jpeg";
            if (name.endsWith(".jpeg")) return "image/jpeg";
            if (name.endsWith(".java")) return "text/plain";
            if (name.endsWith(".woff")) return "font/woff";
            if (name.endsWith(".json")) return "application/json";
            return "application/octet-stream";
        }

        private void log(String message) {
            if (verbose)
                System.out.println(message);
        }
    }
    
    // Consume an entire input stream and return it as an array of
    // bytes.
    public static byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int read = 0;
        while ((read = is.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        return out.toByteArray();
    }

    // Parses a query string into a map. The query string is the part of the
    // address after the "?", if any. Each parameter is separated from the
    // next with an ampersand (&) and the key is separated from the value
    // with an equals sign.
    public static Map<String, String> parseQuery(String query) {
        Map<String, String> res = new HashMap<String, String>();
        if (query == null)
            return res;
        String[] params = query.split("&");
        for (String param : params) {
            int index = param.indexOf("=");
            if (index == -1)
                continue;
            try {
                // Some characters get "URL-encoded" into %xx codes so that
                // they can pass through the network unimpeded.
                // We need to decode them to use them in our Java strings
                // as the real characters.
                res.put(URLDecoder.decode(param.substring(0, index), "UTF-8"),
                        URLDecoder.decode(param.substring(index + 1), "UTF-8"));
            } catch (UnsupportedEncodingException ignored) {}
        }
        return res;
    }

    // Report a particular HTTP error to the browser. For example,
    // error(404, "Not Found", t) or error(403, "Forbidden", t)
    // See <https://en.wikipedia.org/wiki/List_of_HTTP_status_codes>
    public static void error(int code, String desc, HttpExchange t) throws IOException {
        String response = code + " " + desc;
        t.sendResponseHeaders(code, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}