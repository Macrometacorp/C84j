/*
 * DISCLAIMER
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.c8db.internal.http;

import com.arangodb.velocypack.VPackSlice;
import com.c8db.C8DBException;
import com.c8db.Protocol;
import com.c8db.internal.C8RequestParam;
import com.c8db.internal.net.Connection;
import com.c8db.internal.net.HostDescription;
import com.c8db.internal.util.CURLLogger;
import com.c8db.internal.util.IOUtils;
import com.c8db.internal.util.ResponseUtils;
import com.c8db.util.C8Serialization;
import com.c8db.util.C8Serializer.Options;
import com.c8db.velocystream.Request;
import com.c8db.velocystream.RequestType;
import com.c8db.velocystream.Response;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class HttpConnection implements Connection {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommunication.class);
    private static final ContentType CONTENT_TYPE_APPLICATION_JSON_UTF8 = ContentType.create("application/json",
            "utf-8");
    private static final ContentType CONTENT_TYPE_VPACK = ContentType.create("application/x-velocypack");
    private static final int INITIAL_SLEEP_TIME_SEC = 4;
    private static final int SLEEP_TIME_MULTIPLIER = 2;
    private static final int MAX_SLEEP_TIME_SEC = 128;
    private final PoolingHttpClientConnectionManager cm;
    private final CloseableHttpClient client;
    private final String user;
    private final String password;
    private final String email;
    private final Boolean jwtAuthEnabled;
    private final C8Serialization util;
    private final Boolean useSsl;
    private final Protocol contentType;
    private final HostDescription host;
    private volatile String jwt;
    private HttpConnection(final HostDescription host, final Integer timeout, final String user, final String password,
                           final String email, final Boolean jwtAuthEnabled, final Boolean useSsl, final SSLContext sslContext, final C8Serialization util,
                           final Protocol contentType, final Long ttl, final String httpCookieSpec) {
        super();
        this.host = host;
        this.user = user;
        this.password = password;
        this.email = email;
        this.jwtAuthEnabled = jwtAuthEnabled;
        this.useSsl = useSsl;
        this.util = util;
        this.contentType = contentType;
        final RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder
                .create();
        if (Boolean.TRUE == useSsl) {
            if (sslContext != null) {
                registryBuilder.register("https", new SSLConnectionSocketFactory(sslContext));
            } else {
                registryBuilder.register("https", new SSLConnectionSocketFactory(SSLContexts.createSystemDefault()));
            }
        } else {
            registryBuilder.register("http", new PlainConnectionSocketFactory());
        }
        cm = new PoolingHttpClientConnectionManager(registryBuilder.build());
        cm.setDefaultMaxPerRoute(1);
        cm.setMaxTotal(1);
        final RequestConfig.Builder requestConfig = RequestConfig.custom();
        if (timeout != null && timeout >= 0) {
            requestConfig.setConnectTimeout(timeout);
            requestConfig.setConnectionRequestTimeout(timeout);
            requestConfig.setSocketTimeout(timeout);
        }

        if (httpCookieSpec != null && httpCookieSpec.length() > 1) {
            requestConfig.setCookieSpec(httpCookieSpec);
        }

        final ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
                return HttpConnection.this.getKeepAliveDuration(response);
            }
        };
        final HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig.build())
                .setConnectionManager(cm).setKeepAliveStrategy(keepAliveStrategy)
                .setRetryHandler(new DefaultHttpRequestRetryHandler());
        if (ttl != null) {
            builder.setConnectionTimeToLive(ttl, TimeUnit.MILLISECONDS);
        }
        client = builder.build();
    }

    private static String buildUrl(final String baseUrl, final Request request) throws UnsupportedEncodingException {
        final StringBuilder sb = new StringBuilder().append(baseUrl);
        final String database = request.getDatabase();
        final String tenant = request.getTenant();
        if (tenant != null && !tenant.isEmpty()) {
            sb.append("/_tenant/").append(tenant);
        }

        if (database != null && !database.isEmpty()) {
            sb.append("/_fabric/").append(database);
        }

        sb.append(request.getRequest());
        if (!request.getQueryParam().isEmpty()) {
            if (request.getRequest().contains("?")) {
                sb.append("&");
            } else {
                sb.append("?");
            }
            final String paramString = URLEncodedUtils.format(toList(request.getQueryParam()), "utf-8");
            sb.append(paramString);
        }
        return sb.toString();
    }

    private static List<NameValuePair> toList(final Map<String, String> parameters) {
        final ArrayList<NameValuePair> paramList = new ArrayList<NameValuePair>(parameters.size());
        for (final Entry<String, String> param : parameters.entrySet()) {
            if (param.getValue() != null) {
                paramList.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }
        }
        return paramList;
    }

    private static void addHeader(final Request request, final HttpRequestBase httpRequest) {
        for (final Entry<String, String> header : request.getHeaderParam().entrySet()) {
            httpRequest.addHeader(header.getKey(), header.getValue());
        }
    }

    private long getKeepAliveDuration(final HttpResponse response) {
        final HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
        while (it.hasNext()) {
            final HeaderElement he = it.nextElement();
            final String param = he.getName();
            final String value = he.getValue();
            if (value != null && "timeout".equalsIgnoreCase(param)) {
                try {
                    return Long.parseLong(value) * 1000L;
                } catch (final NumberFormatException ignore) {
                }
            }
        }
        return 30L * 1000L;
    }

    @Override
    public void close() throws IOException {
        cm.shutdown();
        client.close();
    }

    public Response execute(final Request request) throws C8DBException, IOException {
        final String url = buildUrl(buildBaseUrl(host), request);
        final HttpRequestBase httpRequest = buildHttpRequestBase(request, url);
        httpRequest.setHeader("User-Agent", "Mozilla/5.0 (compatible; C8DB-JavaDriver/1.1; +http://mt.orz.at/)");
        if (contentType == Protocol.HTTP_VPACK) {
            httpRequest.setHeader("Accept", "application/x-velocypack");
        }
        addHeader(request, httpRequest);
        if (jwtAuthEnabled) {
            if (jwt == null) {
                addJWT();
            }
            httpRequest.addHeader("Authorization", "bearer " + jwt);
        } else {
            // basic auth instead
            final Credentials credentials = addCredentials(httpRequest);
            if (LOGGER.isDebugEnabled()) {
                CURLLogger.log(url, request, credentials, util);
            }
        }
        Response response = null;
        try {
            response = buildResponse(client.execute(httpRequest));
            checkError(response);
        } catch (C8DBException ex) {
            if (ex.getResponseCode().equals(401)) {
                LOGGER.error("C8DBException: Received HTP 401. Re-authenticating to C8DB");
                // jwt might has expired refresh it
                addJWT();
                httpRequest.removeHeaders("Authorization");
                httpRequest.addHeader("Authorization", "bearer " + jwt);
                response = buildResponse(client.execute(httpRequest));
                checkError(response);
            } else if (ex.getResponseCode() >= 500) {
                LOGGER.error(String.format("C8DBException: Received HTTP %d. Retrying C8DB Connection", ex.getResponseCode()));
                response = retryRequest(httpRequest);
            } else if (ex.getResponseCode().equals(404)) {
                // Handle HTTP Error messages here where we just want to log the info and don' want to treat it as
                // an exception
                LOGGER.info(String.format("C8DBException: HTTP %d - %s", ex.getResponseCode(), ex.getErrorMessage()));
                checkError(response);
            } else {
                LOGGER.error("C8DBException: Unable to complete C8DB Request due to ", ex);
                checkError(response);
            }
        } catch (UnknownHostException | NoHttpResponseException ex) {
            response = retryRequest(httpRequest);
        } catch (Exception ex) {
            LOGGER.error("Exception: Unable to complete C8DB Request due to ", ex);
        }
        return response;
    }

    private Response retryRequest(HttpRequestBase httpRequest) throws IOException {
        Response response = null;

        for (int currentWaitTime = INITIAL_SLEEP_TIME_SEC; currentWaitTime <= MAX_SLEEP_TIME_SEC; currentWaitTime *= SLEEP_TIME_MULTIPLIER) {
            try {
                LOGGER.info(String.format("Retrying connection to C8DB in %d seconds...", currentWaitTime));
                Thread.sleep(currentWaitTime * 1000);
                response = buildResponse(client.execute(httpRequest));
                checkError(response);

                return response;
            } catch (InterruptedException e) {
            } catch (Exception e) {
                if (e instanceof C8DBException && ((C8DBException) e).getResponseCode().equals(401)) {
                    // jwt might has expired refresh it
                    addJWT();
                    httpRequest.removeHeaders("Authorization");
                    httpRequest.addHeader("Authorization", "bearer " + jwt);
                }
            }
        }

        LOGGER.info(String.format("Unable to connect to the C8DB after %d seconds. No more retries will be made", MAX_SLEEP_TIME_SEC));
        return response;
    }

    private synchronized void addJWT() throws IOException {
        String authUrl = buildBaseUrl(host) + "/_open/auth";
        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put("username", user);
        credentials.put("password", password);
        credentials.put("email", email);
        final HttpRequestBase authHttpRequest = buildHttpRequestBase(
                new Request("_mm", C8RequestParam.SYSTEM, RequestType.POST, authUrl)
                        .setBody(util.serialize(credentials)),
                authUrl);
        authHttpRequest.setHeader("User-Agent", "Mozilla/5.0 (compatible; C8DB-JavaDriver/1.1; +http://mt.orz.at/)");
        if (contentType == Protocol.HTTP_VPACK) {
            authHttpRequest.setHeader("Accept", "application/x-velocypack");
        }
        Response authResponse = buildResponse(client.execute(authHttpRequest));
        checkError(authResponse);
        setJwt(authResponse.getBody().get("jwt").getAsString());
    }

    private HttpRequestBase buildHttpRequestBase(final Request request, final String url) {
        final HttpRequestBase httpRequest;
        switch (request.getRequestType()) {
            case POST:
                httpRequest = requestWithBody(new HttpPost(url), request);
                break;
            case PUT:
                httpRequest = requestWithBody(new HttpPut(url), request);
                break;
            case PATCH:
                httpRequest = requestWithBody(new HttpPatch(url), request);
                break;
            case DELETE:
                httpRequest = requestWithBody(new HttpDeleteWithBody(url), request);
                break;
            case HEAD:
                httpRequest = new HttpHead(url);
                break;
            case GET:
            default:
                httpRequest = new HttpGet(url);
                break;
        }
        return httpRequest;
    }

    private HttpRequestBase requestWithBody(final HttpEntityEnclosingRequestBase httpRequest, final Request request) {
        final VPackSlice body = request.getBody();
        if (body != null) {
            if (contentType == Protocol.HTTP_VPACK) {
                httpRequest.setEntity(new ByteArrayEntity(
                        Arrays.copyOfRange(body.getBuffer(), body.getStart(), body.getStart() + body.getByteSize()),
                        CONTENT_TYPE_VPACK));
            } else {
                httpRequest.setEntity(new StringEntity(body.toString(), CONTENT_TYPE_APPLICATION_JSON_UTF8));
            }
        }
        return httpRequest;
    }

    private String buildBaseUrl(final HostDescription host) {
        return (Boolean.TRUE == useSsl ? "https://" : "http://") + host.getHost() + ":" + host.getPort();
    }

    public Credentials addCredentials(final HttpRequestBase httpRequest) {
        Credentials credentials = null;
        if (user != null) {
            credentials = new UsernamePasswordCredentials(user, password != null ? password : "");
            try {
                httpRequest.addHeader(new BasicScheme().authenticate(credentials, httpRequest, null));
            } catch (final AuthenticationException e) {
                throw new C8DBException(e);
            }
        }
        return credentials;
    }

    public Response buildResponse(final CloseableHttpResponse httpResponse)
            throws UnsupportedOperationException, IOException {
        final Response response = new Response();
        response.setResponseCode(httpResponse.getStatusLine().getStatusCode());
        final HttpEntity entity = httpResponse.getEntity();
        if (entity != null && entity.getContent() != null) {
            if (contentType == Protocol.HTTP_VPACK) {
                final byte[] content = IOUtils.toByteArray(entity.getContent());
                if (content.length > 0) {
                    response.setBody(new VPackSlice(content));
                }
            } else {
                final String content = IOUtils.toString(entity.getContent());
                if (!content.isEmpty()) {
                    try {
                        response.setBody(
                                util.serialize(content, new Options().stringAsJson(true).serializeNullValues(true)));
                    } catch (C8DBException e) {
                        final byte[] contentAsByteArray = content.getBytes();
                        if (contentAsByteArray.length > 0) {
                            response.setBody(new VPackSlice(contentAsByteArray));
                        }
                    }
                }
            }
        }
        final Header[] headers = httpResponse.getAllHeaders();
        final Map<String, String> meta = response.getMeta();
        for (final Header header : headers) {
            meta.put(header.getName(), header.getValue());
        }
        return response;
    }

    protected void checkError(final Response response) throws C8DBException {
        ResponseUtils.checkError(util, response);
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public static class Builder {
        private String user;
        private String password;
        private String email;
        private Boolean jwtAuthEnabled;
        private C8Serialization util;
        private Boolean useSsl;
        private String httpCookieSpec;
        private Protocol contentType;
        private HostDescription host;
        private Long ttl;
        private SSLContext sslContext;
        private Integer timeout;

        public Builder user(final String user) {
            this.user = user;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder email(final String email) {
            this.email = email;
            return this;
        }

        public Builder serializationUtil(final C8Serialization util) {
            this.util = util;
            return this;
        }

        public Builder jwtAuthEnabled(final Boolean jwtAuth) {
            this.jwtAuthEnabled = jwtAuth;
            return this;
        }

        public Builder useSsl(final Boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        public Builder httpCookieSpec(String httpCookieSpec) {
            this.httpCookieSpec = httpCookieSpec;
            return this;
        }

        public Builder contentType(final Protocol contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder host(final HostDescription host) {
            this.host = host;
            return this;
        }

        public Builder ttl(final Long ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder sslContext(final SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder timeout(final Integer timeout) {
            this.timeout = timeout;
            return this;
        }

        public HttpConnection build() {
            return new HttpConnection(host, timeout, user, password, email, jwtAuthEnabled, useSsl, sslContext, util,
                    contentType, ttl, httpCookieSpec);
        }
    }

}
