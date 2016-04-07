/*
 * Copyright (C) 2014 Square, Inc.
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

import java.io.Closeable;

import static okhttp3.internal.Internal.logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.logging.Level;

import netprophet.NetProphetPropertyManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSource;
import static okhttp3.internal.Util.UTF_8;

/**
 * A one-shot stream from the origin server to the client application with the raw bytes of the
 * response body. Each response body is supported by an active connection to the webserver. This
 * imposes both obligations and limits on the client application.
 *
 * <h3>The response body must be closed.</h3>
 *
 * <p>Each response body is backed by a limited resource like a socket (live network responses) or
 * an open file (for cached responses). Failing to close the response body will leak these resources
 * and may ultimately cause the application to slow down or crash. Close the response body by
 * calling either {@link ResponseBody#close close()}, {@link InputStream#close()
 * byteStream().close()}, or {@link Reader#close() reader().close()}. The {@link #bytes()} and
 * {@link #string()} methods both close the response body automatically.
 *
 * <h3>The response body can be consumed only once.</h3>
 *
 * <p>This class may be used to stream very large responses. For example, it is possible to use this
 * class to read a response that is larger than the entire memory allocated to the current process.
 * It can even stream a response larger than the total storage on the current device, which is a
 * common requirement for video streaming applications.
 *
 * <p>Because this class does not buffer the full response in memory, the application may not
 * re-read the bytes of the response. Use this one shot to read the entire response into memory with
 * {@link #bytes()} or {@link #string()}. Or stream the response with either {@link #source()},
 * {@link #byteStream()}, or {@link #charStream()}.
 * 
 * * <p> NetProphet Timing: if user calls string() or bitmap(), the response end
 * time will be automatically updated; otherwise, if user used
 * streaming-based approach to read contents (i.e., byteStream, charStream), the
 * user has to call void informFinishedReadingResponse() 
 *  1. text: string()  <= 5M works fine
 *  2. image: bitmap() 
 *  3. binary: TODO 
 *  4. large contents: TODO
 */
public abstract class ResponseBody implements Closeable {
  /** Multiple calls to {@link #charStream()} must return the same instance. */
  private Reader reader;

  /* NetProphet */
  Request userRequest;
  public Request getUserRequest() {
    return userRequest;
  }
  
  // This method updates 
  //   1. respEndTime: 0 means currentTimeMillis
  //   2. respSize 
  //   3. user comments: if the call triggers an Exception, 
  //         clients can put error information into the comments.
  public void informFinishedReadingResponse(int size, String comments, int respEndTime, String type){
	NetProphetPropertyManager propertyManager = NetProphetPropertyManager.getInstance();
    if(userRequest != null){
      if(respEndTime == 0)
    	  userRequest.getRequestTimingANP().setRespEndTimeANP(System.currentTimeMillis());
      else
    	  userRequest.getRequestTimingANP().setRespEndTimeANP(0);
	  userRequest.getRequestTimingANP().setAccurateEndTimeANP(true);
	  userRequest.getResponseInfoANP().setSizeANP(size);
	  userRequest.getResponseInfoANP().setRespType(type);
	  if(comments != "")
		  userRequest.getResponseInfoANP().setUserComments(comments);
	  userRequest.getRequestTimingANP().setAccurateEndTimeANP(true);
	  if(userRequest.getCall() != null)
		  userRequest.getCall().storeCallStatInfo(propertyManager.canStoreToRemoteServerEveryRequest());
    }
    else
      logger.log(Level.SEVERE, "NETPROPHET userRequest has not been set to ResponseBody");
  }
	
  //this function has to run on Android
  public Bitmap bitmap(){
	Bitmap map =BitmapFactory.decodeStream(byteStream());
	 String type = "";
	 if(contentType() != null){
		 type = contentType().type().toLowerCase();
	 }
	informFinishedReadingResponse(map.getByteCount(),"",0, type);
	return map;
  }
  
  public void setUserRequest(Request userRequest) {
    this.userRequest = userRequest;
  }
  
  /* End NetProphet */
  
  public abstract MediaType contentType();

  /**
   * Returns the number of bytes in that will returned by {@link #bytes}, or {@link #byteStream}, or
   * -1 if unknown.
   */
  public abstract long contentLength();

  public final InputStream byteStream() {
    return source().inputStream();
  }

  public abstract BufferedSource source();

  public final byte[] bytes() throws IOException {
    long contentLength = contentLength();
    if (contentLength > Integer.MAX_VALUE) {
      throw new IOException("Cannot buffer entire body for content length: " + contentLength);
    }

    BufferedSource source = source();
    byte[] bytes;
    try {
      bytes = source.readByteArray();
    } finally {
      Util.closeQuietly(source);
    }
    if (contentLength != -1 && contentLength != bytes.length) {
      throw new IOException("Content-Length and stream length disagree");
    }
    return bytes;
  }

  /**
   * Returns the response as a character stream decoded with the charset of the Content-Type header.
   * If that header is either absent or lacks a charset, this will attempt to decode the response
   * body as UTF-8.
   */
  public final Reader charStream() {
    Reader r = reader;
    return r != null ? r : (reader = new InputStreamReader(byteStream(), charset()));
  }

  /**
   * Returns the response as a string decoded with the charset of the Content-Type header. If that
   * header is either absent or lacks a charset, this will attempt to decode the response body as
   * UTF-8.
   */
  public final String string() throws IOException {
	 String str = new String(bytes(), charset().name());
	/* NetProphet */
	 String type = "";
	 if(contentType() != null){
		 type = contentType().type().toLowerCase() + "/" + contentType().subtype().toLowerCase();
	 }
	 //System.err.println("NETPROPHET: CONTENTTYPE:"+type);
	informFinishedReadingResponse(str.length(),"", 0, type);
	/* End NetProphet */  
    return str;
  }

  private Charset charset() {
    MediaType contentType = contentType();
    return contentType != null ? contentType.charset(UTF_8) : UTF_8;
  }

  @Override public void close() {
    Util.closeQuietly(source());
  }

  /**
   * Returns a new response body that transmits {@code content}. If {@code contentType} is non-null
   * and lacks a charset, this will use UTF-8.
   */
  public static ResponseBody create(MediaType contentType, String content) {
    Charset charset = UTF_8;
    if (contentType != null) {
      charset = contentType.charset();
      if (charset == null) {
        charset = UTF_8;
        contentType = MediaType.parse(contentType + "; charset=utf-8");
      }
    }
    Buffer buffer = new Buffer().writeString(content, charset);
    return create(contentType, buffer.size(), buffer);
  }

  /** Returns a new response body that transmits {@code content}. */
  public static ResponseBody create(final MediaType contentType, byte[] content) {
    Buffer buffer = new Buffer().write(content);
    return create(contentType, content.length, buffer);
  }

  /** Returns a new response body that transmits {@code content}. */
  public static ResponseBody create(
      final MediaType contentType, final long contentLength, final BufferedSource content) {
    if (content == null) throw new NullPointerException("source == null");
    return new ResponseBody() {
      @Override public MediaType contentType() {
        return contentType;
      }

      @Override public long contentLength() {
        return contentLength;
      }

      @Override public BufferedSource source() {
        return content;
      }
    };
  }
}
