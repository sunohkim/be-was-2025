package webserver.request;

import webserver.HTTPExceptions;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequestBody {
    private final byte[] body;

    public HTTPRequestBody(byte[] body) {
        this.body = body;
    }

    public String getBodyToString() {
        if (body == null || body.length == 0) {
            throw new HTTPExceptions.Error400("Body is null");
        }
        return new String(body);
    }

    public Map<String, Object> parseMultipartFormData(String boundary) {
        if (body == null || body.length == 0) {
            throw new HTTPExceptions.Error400("Body is null");
        }

        Map<String, Object> formData = new HashMap<>();
        String bodyString = getBodyToString();
        String boundaryString = "--" + boundary;

        String[] boundaryParts = bodyString.split(boundaryString);
        for (String boundaryPart : boundaryParts) {
            System.out.println("boundaryPart = " + boundaryPart);
            if (boundaryPart.trim().isEmpty() || boundaryPart.equals(boundaryParts[boundaryParts.length - 1])) {
                continue;
            }

            String[] contents = boundaryPart.split("\r\n\r\n", 2);
            if (contents.length < 2) {
                throw new HTTPExceptions.Error400("Invalid boundary");
            }

            String header = contents[0].trim();
            String content = contents[1].trim();

            if (header.contains("Content-Disposition")) {
                String disposition = header.split("Content-Disposition: ")[1];
                String name = disposition.split("name=")[1].split(";")[0].replace("\"", "");

                if (header.contains("filename")) {
                    String filename = disposition.split("filename=\"")[1].split("\n")[0].replace("\"", "");
                    formData.put(name, new FileData(URLDecoder.decode(filename, StandardCharsets.UTF_8), content.getBytes(StandardCharsets.ISO_8859_1)));
                } else {
                    formData.put(name, content);
                }
            }
        }

        return formData;
    }

    public static class FileData {
        private final String filename;
        private final byte[] content;

        public FileData(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getContent() {
            return content;
        }
    }
}
