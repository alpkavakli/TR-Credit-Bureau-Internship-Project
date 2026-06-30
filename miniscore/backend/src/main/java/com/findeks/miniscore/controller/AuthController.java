@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
 
   private final AuthService authService;
 
   @PostMapping("/register")
   public ResponseEntity<AuthResponse> register(
           @Valid @RequestBody RegisterRequest request) {
       return ResponseEntity.status(201).body(authService.register(request));
   }
 
   @PostMapping("/login")
   public ResponseEntity<AuthResponse> login(
           @Valid @RequestBody LoginRequest request) {
       return ResponseEntity.ok(authService.login(request));
   }
}