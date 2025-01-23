    package webserver.request;

    import java.io.*;
    import java.net.Socket;
    import java.nio.charset.StandardCharsets;
    import java.time.LocalTime;
    import java.util.*;

    import db.Database;
    import model.Cookie;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import webserver.*;
    import webserver.request.api.*;
    import webserver.request.route.*;
    import webserver.request.service.*;
    import webserver.request.staticResource.*;
    import webserver.response.HTTPResponse;
    import webserver.response.HTTPResponseBody;
    import webserver.response.HTTPResponseHeader;
    import webserver.response.ResponseHandler;

    public class RequestHandler implements Runnable {
        private static final int MAX_LOGIN_SESSION_TIME = 3600;
        private static final String DEFAULT_HTTP_VERSION = "HTTP/1.1";
        private final Map<String, RequestProcessor> handlerMap = Map.ofEntries(
                // route
                Map.entry("/", new DefaultPageHandler()),
                Map.entry("/registration", new RegistrationPageHandler()),
                Map.entry("/login", new LoginPageHandler()),
                Map.entry("/mypage", new MyPageHandler()),
                Map.entry("/write", new WritePageHandler()),
                // service
                Map.entry("/user/create", new UserCreateHandler()),
                Map.entry("/user/login", new UserLoginHandler()),
                Map.entry("/user/logout", new UserLogoutHandler()),
                Map.entry("/user/write", new UserWriteHandler()),
                // api
                Map.entry("/api/login-status", new LoginStatusApiHandler()),
                Map.entry("/api/latest-article", new LatestArticleApiHandler()),
                Map.entry("/api/next-article", new NextArticleApiHandler()),
                Map.entry("/api/previous-article", new PreviousArticleApiHandler())
        );

        public RequestHandler(Socket connectionSocket) {
            this.connection = connectionSocket;
        }
        private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

        private Socket connection;

        // Todo: timeout 발생 시 처리하는 기능 구현 필요
        public void run() {
            logger.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                    connection.getPort());

            HTTPRequestHeader requestHeader;
            HTTPRequestBody requestBody = null;

            HTTPResponseHeader responseHeader = new HTTPResponseHeader(DEFAULT_HTTP_VERSION);
            HTTPResponseBody responseBody = null;
            List<Cookie> cookieList = new ArrayList<>();

            try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
                DataOutputStream dos = new DataOutputStream(out);
                // 스트림 데이터를 읽기 위한 버퍼 생성
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] temp = new byte[1024];
                int bytesRead;

                // header 읽기
                StringBuilder headerBuilder = new StringBuilder();
                boolean headerEnd = false;

                while (!headerEnd && (bytesRead = in.read(temp)) != -1) {
                    for (int i = 0; i < bytesRead; i++) {
                        char c = (char) temp[i];
                        headerBuilder.append(c);

                        // body가 있을 경우에만 진입 - header의 끝인지 검사
                        if (headerBuilder.length() >= 4 && headerBuilder.substring(headerBuilder.length() - 4).equals("\r\n\r\n")) {
                            headerEnd = true;
                            // 남은 데이터는 body로 넘김
                            buffer.write(temp, i + 1, bytesRead - i - 1);
                            break;
                        }
                    }
                }
                String headersString = headerBuilder.toString();
                logger.debug(headersString);

                try {
                    requestHeader = new HTTPRequestHeader(headersString);

                    String method = requestHeader.getMethod();
                    String[] uri = requestHeader.getUri().split("\\?");
                    String version = requestHeader.getVersion();
                    Map<String, String> headers = requestHeader.getHeaders();

                    responseHeader.setVersion(version);

                    if (headers.containsKey("cookie")) {
                        // 새로운 쿠키 값을 처리해야 할 경우 이 부분에 추가하면 된다.
                        Map<String, String> cookies = Cookie.parseCookies(headers.get("cookie"));
                        if (cookies.containsKey("SESSIONID")) {
                            String sessionId = cookies.get("SESSIONID");

                            LocalTime time = LocalTime.now();
                            Database.updateSessionLastAccessTime(sessionId, time);
                            cookieList.add(new Cookie("SESSIONID", sessionId, Database.getSessionMaxInactiveInterval(sessionId)));
                        }
                    }
                    // Todo: transfer-encoding 헤더 관련 기능
                    // body가 있을 경우 body 읽기
                    if (headers.containsKey("content-length")) {
                        int contentLength = Integer.parseInt(headers.get("content-length"));

                        while (buffer.size() < contentLength && (bytesRead = in.read(temp)) != -1) {
                            buffer.write(temp, 0, bytesRead);
                        }

                        byte[] body = buffer.toByteArray();

                        // Content-Length와 body 길이가 다른 경우
                        if (body.length != contentLength) {
                            throw new HTTPExceptions.Error400("400 Bad Request: Content-Length header mismatch");
                        }

                        requestBody = new HTTPRequestBody(body);
                    }
                    // Todo: Content-Type에 따라 적합한 형태로 body 변환하기

                    // Request의 uri를 추출
                    String path = uri[0];
                    String queryParams = (uri.length > 1) ? uri[1] : "";
                    logger.debug("path: {}, params: {}", path, queryParams);
                    logger.debug("method: {}", method);

                    RequestProcessor handler = handlerMap.getOrDefault(path, new StaticResourceHandler());
                    HTTPResponse response = handler.handle(requestHeader, requestBody, queryParams, responseHeader, cookieList);

                    ResponseHandler.respond(dos, response);
                } catch (HTTPExceptions e) {
                    logger.error(e.getMessage());
                    responseHeader.setStatusCode(e.getStatusCode());
                    responseBody = new HTTPResponseBody(HTTPExceptions.getErrorMessageToBytes(e.getMessage()));
                    HTTPResponse response = new HTTPResponse(responseHeader, responseBody);
                    ResponseHandler.respond(dos, response);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }