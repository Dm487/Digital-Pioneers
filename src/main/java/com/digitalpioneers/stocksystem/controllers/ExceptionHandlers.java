package com.digitalpioneers.stocksystem.controllers;

import com.digitalpioneers.stocksystem.exceptions.InsufficientStockException;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExceptionHandlers {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlers.class);

    private ExceptionHandlers() {}

    public static void register(Javalin app) {
        app.exception(NotFoundResponse.class, (exception, context) ->
                context.status(404).json(Map.of("error", "The requested resource was not found.")));
        app.exception(BadRequestResponse.class, (exception, context) ->
                context.status(400).json(Map.of("error", safeMessage(exception))));
        app.exception(IllegalArgumentException.class, (exception, context) ->
                context.status(400).json(Map.of("error", safeMessage(exception))));
        app.exception(InsufficientStockException.class, (exception, context) ->
                context.status(409).json(Map.of("error", safeMessage(exception))));
        app.exception(Exception.class, (exception, context) -> {
            SQLException sqlException = findSqlException(exception);
            if (sqlException != null && ("23505".equals(sqlException.getSQLState())
                    || "23503".equals(sqlException.getSQLState()) || sqlException.getErrorCode() == 19)) {
                context.status(409).json(Map.of("error", "The request conflicts with existing data."));
                return;
            }
            LOGGER.error("Request failed: {} {}", context.method(), context.path(), exception);
            context.status(500).json(Map.of("error", "An unexpected server error occurred."));
        });
    }

    private static SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) return sqlException;
            current = current.getCause();
        }
        return null;
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "The request is invalid." : exception.getMessage();
    }
}