/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.mobile.android.http;

import com.liferay.mobile.android.http.file.InputStreamBody;
import com.liferay.mobile.android.http.file.UploadData;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

/**
 * @author Bruno Farache
 */
public class OkHttpClientImpl implements HttpClient {

	public OkHttpClientImpl() {
		client = new OkHttpClient();
	}

	@Override
	public Response send(Request request) throws Exception {
		Builder builder = new Builder();
		Method method = request.getMethod();

		if (method == Method.POST) {
			String body = (String)request.getBody();

			if (body != null) {
				MediaType type = MediaType.parse(
					"application/json; charset=utf-8");

				builder.post(RequestBody.create(type, body));
			}
		}
		else if (method == Method.HEAD) {
			builder.head();
		}

		return send(builder, request);
	}

	@Override
	public Response upload(Request request) throws Exception {
		Builder builder = new Builder();

		JSONObject body = (JSONObject)request.getBody();
		builder.post(getUploadBody(body));

		return send(builder, request);
	}

	protected OkHttpClient getClient(int connectionTimeout) {
		OkHttpClient clone = client.clone();

		clone.setConnectTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
		clone.setReadTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
		clone.setWriteTimeout(connectionTimeout, TimeUnit.MILLISECONDS);

		clone.setFollowRedirects(false);

		return clone;
	}

	protected RequestBody getUploadBody(JSONObject body) {
		MultipartBuilder builder = new MultipartBuilder()
			.type(MultipartBuilder.FORM);

		Iterator<String> it = body.keys();

		while (it.hasNext()) {
			String key = it.next();
			Object value = body.opt(key);

			if (value instanceof UploadData) {
				UploadData data = (UploadData)value;

				String filename = data.getFilename();
				String mimeType = data.getMimeType();
				InputStream is = data.getInputStream();

				RequestBody requestBody = new InputStreamBody(
					MediaType.parse(mimeType), is);

				builder.addFormDataPart(key, filename, requestBody);
			}
			else {
				builder.addFormDataPart(key, value.toString());
			}
		}

		return builder.build();
	}

	protected Response send(Builder builder, Request request)
		throws IOException {

		builder = builder.url(request.getURL());
		OkHttpClient client = getClient(request.getConnectionTimeout());
		Map<String, String> headers = request.getHeaders();

		if (headers != null) {
			for (Map.Entry<String, String> header : headers.entrySet()) {
				builder.addHeader(header.getKey(), header.getValue());
			}
		}

		com.squareup.okhttp.Response response = client
			.newCall(builder.build())
			.execute();

		return new Response(
			response.code(), _toMap(response.headers().toMultimap()),
			response.body());
	}

	protected OkHttpClient client;

	private Map<String, String> _toMap(Map<String, List<String>> headers) {
		Map<String, String> map = new HashMap<String, String>();

		for (Map.Entry<String, List<String>> header : headers.entrySet()) {
			map.put(header.getKey(), header.getValue().get(0));
		}

		return map;
	}

}