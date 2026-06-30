@RestControllerAdvice
public class GlobalExceptionHandler {
 
   @ExceptionHandler(RuntimeException.class)
   public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
       return ResponseEntity.badRequest()
               .body(new ErrorResponse(ex.getMessage()));
   }
 
  @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<ErrorResponse> handleValidation(
           MethodArgumentNotValidException ex) {
       String message = ex.getBindingResult().getFieldErrors().stream()
               .map(e -> e.getField() + ": " + e.getDefaultMessage())
              .collect(Collectors.joining(", "));
       return ResponseEntity.badRequest()
               .body(new ErrorResponse(message));
   }
}