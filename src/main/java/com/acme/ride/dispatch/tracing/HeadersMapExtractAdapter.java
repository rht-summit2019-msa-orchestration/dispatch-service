/*
 * Copyright 2017-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.acme.ride.dispatch.tracing;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.opentracing.propagation.TextMap;


public class HeadersMapExtractAdapter implements TextMap {

  private final Map<String, String> map = new HashMap<>();

  HeadersMapExtractAdapter(Map<String, Object> headers, boolean second) {
    for (String key : headers.keySet()) {
      if (second) {
        if (key.startsWith("second_span")) {
          map.put(key.replaceFirst("^second_span_",""), new String((byte[])(headers.get(key)), StandardCharsets.UTF_8));
        } else {
          if (headers.get(key) instanceof byte[]) {
            map.put(key, new String((byte[])(headers.get(key)), StandardCharsets.UTF_8));
          } else {
            map.put(key, headers.get(key).toString());
          }
        }
      }
    }
  }

  @Override
  public Iterator<Entry<String, String>> iterator() {
    return map.entrySet().iterator();
  }

  @Override
  public void put(String key, String value) {
    throw new UnsupportedOperationException(
        "HeadersMapExtractAdapter should only be used with Tracer.extract()");
  }
}
