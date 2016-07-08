// Copyright 2016 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.trace.v1.source;

import com.google.cloud.trace.util.Label;
import com.google.cloud.trace.util.Labels;
import com.google.cloud.trace.util.SpanKind;
import com.google.cloud.trace.util.StackFrame;
import com.google.cloud.trace.util.StackTrace;
import com.google.cloud.trace.util.Timestamp;
import com.google.cloud.trace.util.TraceContext;
import com.google.cloud.trace.util.TraceId;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;

public class TraceSource {
  public Trace generateStartSpan(String projectId, TraceContext context,
      TraceContext parentContext, SpanKind spanKind, String name, Timestamp timestamp) {
    Trace.Builder traceBuilder = Trace.newBuilder()
        .setProjectId(projectId)
        .setTraceId(formatTraceId(context.getTraceId()));

    TraceSpan.Builder spanBuilder = traceBuilder.addSpansBuilder()
        .setSpanId(context.getSpanId().getSpanId())
        .setKind(toSpanKindProto(spanKind))
        .setName(name)
        .setStartTime(toTimestamp(timestamp));

    if (parentContext.getTraceId().equals(context.getTraceId())
        && parentContext.getSpanId().isValid()) {
      spanBuilder.setParentSpanId(parentContext.getSpanId().getSpanId());
    }

    return traceBuilder.build();
  }

  public Trace generateEndSpan(String projectId, TraceContext context, Timestamp timestamp) {
    Trace.Builder traceBuilder = Trace.newBuilder()
        .setProjectId(projectId)
        .setTraceId(formatTraceId(context.getTraceId()));

    TraceSpan.Builder spanBuilder = traceBuilder.addSpansBuilder()
        .setSpanId(context.getSpanId().getSpanId())
        .setEndTime(toTimestamp(timestamp));

    return traceBuilder.build();
  }

  public Trace generateAnnotateSpan(String projectId, TraceContext context, Labels labels) {
    Trace.Builder traceBuilder = Trace.newBuilder()
        .setProjectId(projectId)
        .setTraceId(formatTraceId(context.getTraceId()));

    TraceSpan.Builder spanBuilder = traceBuilder.addSpansBuilder()
        .setSpanId(context.getSpanId().getSpanId());

    for (Label label : labels.getLabels()) {
      spanBuilder.getMutableLabels().put(label.getKey(), label.getValue());
    }

    return traceBuilder.build();
  }

  public Trace generateSetStackTrace(
      String projectId, TraceContext context, StackTrace stackTrace) {
    Trace.Builder traceBuilder = Trace.newBuilder()
        .setProjectId(projectId)
        .setTraceId(formatTraceId(context.getTraceId()));

    TraceSpan.Builder spanBuilder = traceBuilder.addSpansBuilder()
        .setSpanId(context.getSpanId().getSpanId());

    StringBuffer stackTraceValue = new StringBuffer("{\"stack_frame\":[");
    for (int i = 0; i < stackTrace.getStackFrames().size(); i++) {
      if (i != 0) {
        stackTraceValue.append(",");
      }
      StackFrame stackFrame = stackTrace.getStackFrames().get(i);
      stackTraceValue.append("{\"class_name\":\"");
      stackTraceValue.append(stackFrame.getClassName());
      stackTraceValue.append("\",\"method_name\":\"");
      stackTraceValue.append(stackFrame.getMethodName());
      stackTraceValue.append("\"");
      if (stackFrame.getFileName() != null) {
        stackTraceValue.append(",\"file_name\":\"");
        stackTraceValue.append(stackFrame.getFileName());
        stackTraceValue.append("\"");
      }
      if (stackFrame.getLineNumber() != null) {
        stackTraceValue.append(",\"line_number\":");
        stackTraceValue.append(stackFrame.getLineNumber());
      }
      stackTraceValue.append("}");
    }
    stackTraceValue.append("]}");

    spanBuilder.getMutableLabels().put(
        "trace.cloud.google.com/stacktrace", stackTraceValue.toString());

    return traceBuilder.build();
  }

  private String formatTraceId(TraceId traceId) {
    return String.format("%032x", traceId.getTraceId());
  }

  private TraceSpan.SpanKind toSpanKindProto(SpanKind spanKind) {
    switch(spanKind) {
      case UNSPECIFIED:
        return TraceSpan.SpanKind.SPAN_KIND_UNSPECIFIED;
      case RPC_SERVER:
        return TraceSpan.SpanKind.RPC_SERVER;
      case RPC_CLIENT:
        return TraceSpan.SpanKind.RPC_CLIENT;
      default:
        return TraceSpan.SpanKind.SPAN_KIND_UNSPECIFIED;
    }
  }

  private com.google.protobuf.Timestamp toTimestamp(Timestamp timestamp) {
    return com.google.protobuf.Timestamp.newBuilder()
      .setSeconds(timestamp.getSeconds())
      .setNanos(timestamp.getNanos())
      .build();
  }
}
