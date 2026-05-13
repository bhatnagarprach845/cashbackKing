package com.bhatn.cashbackking;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest; // <--- This MUST be V2
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamLambdaHandler implements RequestStreamHandler {
    // Note the generic types: <HttpApiV2ProxyRequest, AwsProxyResponse>
    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            // CRITICAL: You must use the 'getHttpApiV2ProxyHandler' method
            handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(CashbackKingApplication.class);
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        // 1. Read the input stream into a string to check for OPTIONS
        // (This is a bit more manual but guarantees no method signature errors)
        try {
            byte[] inputBytes = inputStream.readAllBytes();
            String inputString = new String(inputBytes);

            if (inputString.contains("\"httpMethod\":\"OPTIONS\"") || inputString.contains("\"method\":\"OPTIONS\"")) {
                // Manually write the 200 OK response with CORS headers
                String corsResponse = "{"
                        + "\"statusCode\": 200,"
                        + "\"headers\": {"
                        + "  \"Access-Control-Allow-Origin\": \"https://feature-initialcommit.dwp81oqt95zeu.amplifyapp.com\","
                        + "  \"Access-Control-Allow-Methods\": \"GET, POST, PUT, DELETE, OPTIONS\","
                        + "  \"Access-Control-Allow-Headers\": \"*\","
                        + "  \"Access-Control-Allow-Credentials\": \"true\""
                        + "},"
                        + "\"body\": \"\""
                        + "}";
                outputStream.write(corsResponse.getBytes());
                return; // Stop here!
            }

            // 2. If not OPTIONS, pass the original bytes to the handler
            ByteArrayInputStream bais = new ByteArrayInputStream(inputBytes);
            handler.proxyStream(bais, outputStream, context);

        } catch (Exception e) {
            // Fallback to standard handling if the manual check fails
            handler.proxyStream(inputStream, outputStream, context);
        }
    }
}