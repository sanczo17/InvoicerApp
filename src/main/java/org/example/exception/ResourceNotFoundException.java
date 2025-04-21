package org.example.exception;

/**
 * Wyjątek rzucany, gdy żądany zasób nie został znaleziony.
 * Używany głównie w warstwie serwisowej dla przypadków,
 * gdy nie można odnaleźć elementu o podanym identyfikatorze.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s o identyfikatorze %d nie został znaleziony", resourceType, id));
    }
}