package webserver.request.service;

import db.Database;
import model.Article;
import model.Cookie;
import webserver.HTTPExceptions;
import webserver.request.HTTPRequestBody;
import webserver.request.HTTPRequestHeader;
import webserver.request.RequestProcessor;
import webserver.response.HTTPResponse;
import webserver.response.HTTPResponseBody;
import webserver.response.HTTPResponseHeader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserWriteHandler implements RequestProcessor {

    @Override
    public HTTPResponse handle(HTTPRequestHeader requestHeader, HTTPRequestBody requestBody, String queryParams, HTTPResponseHeader responseHeader, List<Cookie> cookieList) throws IOException {
        boolean isLoggedIn = false;
        HTTPResponseBody responseBody = null;

        try {
            String method = requestHeader.getMethod();
            if (!method.equals("POST")) {
                throw new HTTPExceptions.Error405("Method not supported " + method);
            }

            // 쿠키 정보를 바탕으로 사용자 정보 가져오기
            String userId = null, userName = null;
            for (Cookie cookie : cookieList) {
                if (cookie.getName().equals("SESSIONID")) {
                    isLoggedIn = true;
                    String sessionId = cookie.getValue();
                    userId = Database.getSessionById(sessionId).getUserId();
                    userName = Database.getUserById(userId).getName();
                    break;
                }
            }
            if (!isLoggedIn) {
                responseHeader.setStatusCode(302);
                responseHeader.addHeader("Location", "/login");
                return new HTTPResponse(responseHeader, responseBody);
            }

            Map<String, String> headers = requestHeader.getHeaders();
            String[] contentTypes = headers.get("content-type").split("; boundary=");
            String contentType = contentTypes[0];
            String boundary = contentTypes[1];

            if (!contentType.equals("multipart/form-data")) {
                throw new HTTPExceptions.Error415("Unsupported Media Type " + contentType);
            }

            // multipart/form-data 파싱
            Map<String, Object> formData = requestBody.parseMultipartFormData(boundary);

            String content = (String) formData.get("content");
            HTTPRequestBody.FileData imageFile = (HTTPRequestBody.FileData) formData.get("image");

            if (content == null || content.isEmpty()) {
                throw new HTTPExceptions.Error400("Missing required parameters");
            }

            Article article = new Article(content, userId, userName, LocalTime.now());
            Database.addArticle(article);

            if (imageFile != null) {
                System.out.println("image: " + Arrays.toString(imageFile.getContent()));
//                // 파일 저장 로직 (예: 디스크 또는 데이터베이스)
                System.out.println("FileName: " + imageFile.getFilename());
                String uploadPath = "/Users/admin/Documents/" + imageFile.getFilename();
                try (OutputStream os = new FileOutputStream(uploadPath)) {
                    os.write(imageFile.getContent());
                }
            }

            responseHeader.setStatusCode(302);
            responseHeader.addHeader("Location", "/index.html");
        } catch (HTTPExceptions e) {
            responseHeader.setStatusCode(e.getStatusCode());
            responseBody = new HTTPResponseBody(HTTPExceptions.getErrorMessageToBytes(e.getMessage()));
        }

        for (Cookie cookie : cookieList) {
            responseHeader.addHeader("Set-Cookie", cookie.toString());
        }

        return new HTTPResponse(responseHeader, responseBody);
    }
}
