/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3;

import java.net.URI;
import java.net.URL;
import java.util.List;
import okhttp3.internal.http.HttpMethod;

/**
 * An HTTP request. Instances of this class are immutable if their {@link #body}
 * is null or itself immutable.
 */
public final class Request {
	private final HttpUrl url;
	private final String method;
	private final Headers headers;
	private final RequestBody body;
	private final Object tag;

	private volatile URI javaNetUri; // Lazily initialized.
	private volatile CacheControl cacheControl; // Lazily initialized.

	/* NetProphet Class */
	public class RequestTimingANP {
		private long reqStartTimeANP;
		private long dnsStartTimeANP;
		private long dnsEndTimeANP;
		private long connSetupStartTimeANP;
		private long tlsConnSetupStartTimeANP; // Not used for now
		private long tlsConnSetupEndTimeANP; // Not used for now
		private long connSetupEndTimeANP;
		private long reqWriteStartTimeANP;
		private long reqWriteEndTimeANP;
		private long respStartTimeANP;
		private long respEndTimeANP;
		private long handshakeTimeANP;
		private boolean useCacheANP;
		private boolean isAccurateEndTimeANP;
		private boolean isSuccessfulANP; // set to false only when retry also
											// failed
		private String errorString;

		RequestTimingANP() {
			/* Initialize NetProphet Fields */
			this.reqStartTimeANP = 0;
			this.dnsEndTimeANP = 0;
			this.dnsStartTimeANP = 0;
			this.connSetupEndTimeANP = 0;
			this.connSetupStartTimeANP = 0;
			this.tlsConnSetupEndTimeANP = 0;
			this.tlsConnSetupStartTimeANP = 0;
			this.connSetupStartTimeANP = 0;
			this.reqWriteEndTimeANP = 0;
			this.reqWriteStartTimeANP = 0;
			this.respStartTimeANP = 0;
			this.respEndTimeANP = 0;
			this.handshakeTimeANP = 0;
			this.useCacheANP = false;
			this.isSuccessfulANP = true;
			this.isAccurateEndTimeANP = false;

			errorString = "";
		}

		/*
		 * Rounded by 10ms ReturnValue: -1: negative TTFB -2: handshake is zero
		 */
		public int getEstimatedServerDelay() {
			int result = 0;
			long TTFB = this.respStartTimeANP - this.reqWriteEndTimeANP;
			if (TTFB <= 0)
				return -1;
			if (this.handshakeTimeANP != 0)
				result = (int) (TTFB - this.handshakeTimeANP);
			else
				return -2;

			if (result < 0)
				return 0;
			else
				return result / 10 * 10;
		}

		public boolean isSuccessfulANP() {
			return isSuccessfulANP;
		}

		public boolean isAccurateEndTimeANP() {
			return isAccurateEndTimeANP;
		}

		public void setAccurateEndTimeANP(boolean isAccurateEndTimeANP) {
			this.isAccurateEndTimeANP = isAccurateEndTimeANP;
		}

		public long getHandshakeTimeANP() {
			return handshakeTimeANP;
		}

		public void setHandshakeTimeANP(long handshakeTimeANP) {
			this.handshakeTimeANP = handshakeTimeANP;
		}

		public void setSuccessfulANP(boolean isSuccessfulANP) {
			this.isSuccessfulANP = isSuccessfulANP;
		}

		public String getErrorString() {
			return errorString;
		}

		public void setErrorString(String errorString) {
			this.errorString = errorString;
		}

		/* NetProphet Getters and Setters */
		public long getReqStartTimeANP() {
			return reqStartTimeANP;
		}

		public void setReqStartTimeANP(long reqStartTimeANP) {
			this.reqStartTimeANP = reqStartTimeANP;
		}

		public long getDnsStartTimeANP() {
			return dnsStartTimeANP;
		}

		public void setDnsStartTimeANP(long dnsStartTimeANP) {
			this.dnsStartTimeANP = dnsStartTimeANP;
		}

		public long getDnsEndTimeANP() {
			return dnsEndTimeANP;
		}

		public void setDnsEndTimeANP(long dnsEndTimeANP) {
			this.dnsEndTimeANP = dnsEndTimeANP;
		}

		public long getConnSetupStartTimeANP() {
			return connSetupStartTimeANP;
		}

		public void setConnSetupStartTimeANP(long connSetupStartTimeANP) {
			this.connSetupStartTimeANP = connSetupStartTimeANP;
		}

		public long getTlsConnSetupStartTimeANP() {
			return tlsConnSetupStartTimeANP;
		}

		public void setTlsConnSetupStartTimeANP(long tlsConnSetupStartTimeANP) {
			this.tlsConnSetupStartTimeANP = tlsConnSetupStartTimeANP;
		}

		public long getTlsConnSetupEndTimeANP() {
			return tlsConnSetupEndTimeANP;
		}

		public void setTlsConnSetupEndTimeANP(long tlsConnSetupEndTimeANP) {
			this.tlsConnSetupEndTimeANP = tlsConnSetupEndTimeANP;
		}

		public long getConnSetupEndTimeANP() {
			return connSetupEndTimeANP;
		}

		public void setConnSetupEndTimeANP(long connSetupEndTimeANP) {
			this.connSetupEndTimeANP = connSetupEndTimeANP;
		}

		public long getReqWriteStartTimeANP() {
			return reqWriteStartTimeANP;
		}

		public void setReqWriteStartTimeANP(long reqWriteStartTimeANP) {
			this.reqWriteStartTimeANP = reqWriteStartTimeANP;
		}

		public long getReqWriteEndTimeANP() {
			return reqWriteEndTimeANP;
		}

		public void setReqWriteEndTimeANP(long reqWriteEndTimeANP) {
			this.reqWriteEndTimeANP = reqWriteEndTimeANP;
		}

		public long getRespStartTimeANP() {
			return respStartTimeANP;
		}

		public void setRespStartTimeANP(long respStartTimeANP) {
			this.respStartTimeANP = respStartTimeANP;
		}

		public long getRespEndTimeANP() {
			return respEndTimeANP;
		}

		public void setRespEndTimeANP(long respEndTimeANP) {
			this.respEndTimeANP = respEndTimeANP;
		}

		public boolean getUseCacheANP() {
			return useCacheANP;
		}

		public void setUseCacheANP(boolean useCacheANP) {
			this.useCacheANP = useCacheANP;
		}
		// End
	}
	/* End NetProphet Class*/
	
	/* NetProphet Field */
	private RequestTimingANP requestTimingANP;
	/* End NetProphet Field*/

	private Request(Builder builder) {
		this.url = builder.url;
		this.method = builder.method;
		this.headers = builder.headers.build();
		this.body = builder.body;
		this.tag = builder.tag != null ? builder.tag : this;
		
		/* NetProphet Initialization */
		requestTimingANP = new RequestTimingANP();
		/* End NetProphet Initialization*/
	}

	/* NetProphet Getter and Setter */
	public RequestTimingANP getRequestTimingANP() {
		return requestTimingANP;
	}

	public void setRequestTimingANP(RequestTimingANP requestTimingANP) {
		this.requestTimingANP = requestTimingANP;
	}
	/* End NetProphet Getter and Setter */
	
	public HttpUrl url() {
		return url;
	}

	public String method() {
		return method;
	}

	public Headers headers() {
		return headers;
	}

	public String header(String name) {
		return headers.get(name);
	}

	public List<String> headers(String name) {
		return headers.values(name);
	}

	public RequestBody body() {
		return body;
	}

	public Object tag() {
		return tag;
	}

	public Builder newBuilder() {
		return new Builder(this);
	}

	/**
	 * Returns the cache control directives for this response. This is never
	 * null, even if this response contains no {@code Cache-Control} header.
	 */
	public CacheControl cacheControl() {
		CacheControl result = cacheControl;
		return result != null ? result : (cacheControl = CacheControl
				.parse(headers));
	}

	public boolean isHttps() {
		return url.isHttps();
	}

	@Override
	public String toString() {
		return "Request{method=" + method + ", url=" + url + ", tag="
				+ (tag != this ? tag : null) + '}';
	}

	public static class Builder {
		private HttpUrl url;
		private String method;
		private Headers.Builder headers;
		private RequestBody body;
		private Object tag;

		public Builder() {
			this.method = "GET";
			this.headers = new Headers.Builder();
		}

		private Builder(Request request) {
			this.url = request.url;
			this.method = request.method;
			this.body = request.body;
			this.tag = request.tag;
			this.headers = request.headers.newBuilder();
		}

		public Builder url(HttpUrl url) {
			if (url == null)
				throw new IllegalArgumentException("url == null");
			this.url = url;
			return this;
		}

		/**
		 * Sets the URL target of this request.
		 *
		 * @throws IllegalArgumentException
		 *             if {@code url} is not a valid HTTP or HTTPS URL. Avoid
		 *             this exception by calling {@link HttpUrl#parse}; it
		 *             returns null for invalid URLs.
		 */
		public Builder url(String url) {
			if (url == null)
				throw new IllegalArgumentException("url == null");

			// Silently replace websocket URLs with HTTP URLs.
			if (url.regionMatches(true, 0, "ws:", 0, 3)) {
				url = "http:" + url.substring(3);
			} else if (url.regionMatches(true, 0, "wss:", 0, 4)) {
				url = "https:" + url.substring(4);
			}

			HttpUrl parsed = HttpUrl.parse(url);
			if (parsed == null)
				throw new IllegalArgumentException("unexpected url: " + url);
			return url(parsed);
		}

		/**
		 * Sets the URL target of this request.
		 *
		 * @throws IllegalArgumentException
		 *             if the scheme of {@code url} is not {@code http} or
		 *             {@code https}.
		 */
		public Builder url(URL url) {
			if (url == null)
				throw new IllegalArgumentException("url == null");
			HttpUrl parsed = HttpUrl.get(url);
			if (parsed == null)
				throw new IllegalArgumentException("unexpected url: " + url);
			return url(parsed);
		}

		/**
		 * Sets the header named {@code name} to {@code value}. If this request
		 * already has any headers with that name, they are all replaced.
		 */
		public Builder header(String name, String value) {
			headers.set(name, value);
			return this;
		}

		/**
		 * Adds a header with {@code name} and {@code value}. Prefer this method
		 * for multiply-valued headers like "Cookie".
		 *
		 * <p>
		 * Note that for some headers including {@code Content-Length} and
		 * {@code Content-Encoding}, OkHttp may replace {@code value} with a
		 * header derived from the request body.
		 */
		public Builder addHeader(String name, String value) {
			headers.add(name, value);
			return this;
		}

		public Builder removeHeader(String name) {
			headers.removeAll(name);
			return this;
		}

		/** Removes all headers on this builder and adds {@code headers}. */
		public Builder headers(Headers headers) {
			this.headers = headers.newBuilder();
			return this;
		}

		/**
		 * Sets this request's {@code Cache-Control} header, replacing any cache
		 * control headers already present. If {@code cacheControl} doesn't
		 * define any directives, this clears this request's cache-control
		 * headers.
		 */
		public Builder cacheControl(CacheControl cacheControl) {
			String value = cacheControl.toString();
			if (value.isEmpty())
				return removeHeader("Cache-Control");
			return header("Cache-Control", value);
		}

		public Builder get() {
			return method("GET", null);
		}

		public Builder head() {
			return method("HEAD", null);
		}

		public Builder post(RequestBody body) {
			return method("POST", body);
		}

		public Builder delete(RequestBody body) {
			return method("DELETE", body);
		}

		public Builder delete() {
			return delete(RequestBody.create(null, new byte[0]));
		}

		public Builder put(RequestBody body) {
			return method("PUT", body);
		}

		public Builder patch(RequestBody body) {
			return method("PATCH", body);
		}

		public Builder method(String method, RequestBody body) {
			if (method == null || method.length() == 0) {
				throw new IllegalArgumentException(
						"method == null || method.length() == 0");
			}
			if (body != null && !HttpMethod.permitsRequestBody(method)) {
				throw new IllegalArgumentException("method " + method
						+ " must not have a request body.");
			}
			if (body == null && HttpMethod.requiresRequestBody(method)) {
				throw new IllegalArgumentException("method " + method
						+ " must have a request body.");
			}
			this.method = method;
			this.body = body;
			return this;
		}

		/**
		 * Attaches {@code tag} to the request. It can be used later to cancel
		 * the request. If the tag is unspecified or null, the request is
		 * canceled by using the request itself as the tag.
		 */
		public Builder tag(Object tag) {
			this.tag = tag;
			return this;
		}

		public Request build() {
			if (url == null)
				throw new IllegalStateException("url == null");
			return new Request(this);
		}
	}
}
