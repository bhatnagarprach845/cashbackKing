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
        // Inside handleRequest method
        byte[] inputBytes = inputStream.readAllBytes();
        String inputString = new String(inputBytes);

        if (inputString.contains("\"method\":\"OPTIONS\"") || inputString.contains("\"httpMethod\":\"OPTIONS\"")) {
            String response = "{\"statusCode\":200,\"headers\":{"
                    + "\"Access-Control-Allow-Origin\":\"*\", "
                    + "\"Access-Control-Allow-Methods\":\"GET,POST,PUT,DELETE,OPTIONS\","
                    + "\"Access-Control-Allow-Headers\":\"*\"}}";
            outputStream.write(response.getBytes());
            return;
        }
        handler.proxyStream(new ByteArrayInputStream(inputBytes), outputStream, context);
    }
}