@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {
 
   private final CreditScoreService scoreService;
 
   @GetMapping("/my-score")
   public ResponseEntity<CreditScoreResponse> getMyScore(
           @AuthenticationPrincipal UserDetails currentUser) {
       return ResponseEntity.ok(
              scoreService.queryScore(currentUser.getUsername()));
   }
 
   @GetMapping("/my-history")
   public ResponseEntity<List<CreditScoreResponse>> getHistory(
           @AuthenticationPrincipal UserDetails currentUser) {
       return ResponseEntity.ok(
              scoreService.getHistory(currentUser.getUsername()));
   }
}