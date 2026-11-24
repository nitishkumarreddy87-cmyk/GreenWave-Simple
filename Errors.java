/** Custom checked exceptions (Exception handling). */
class SignalFailureException extends Exception {
    SignalFailureException(String msg) { super(msg); }
}
class RouteNotFoundException extends Exception {
    RouteNotFoundException(String msg) { super(msg); }
}
class AccessDeniedException extends Exception {
    AccessDeniedException(String msg) { super(msg); }
}
