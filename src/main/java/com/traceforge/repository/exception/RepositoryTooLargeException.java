package com.traceforge.repository.exception;

public class RepositoryTooLargeException extends RuntimeException {

    public RepositoryTooLargeException(long actualSize, long maximumSize) {
        super("Repository size " + actualSize + " bytes exceeds the limit of "
                + maximumSize + " bytes.");
    }
}
