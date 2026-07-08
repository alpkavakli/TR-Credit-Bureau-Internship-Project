package com.findeks.miniscore.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.findeks.miniscore.dto.CreditScoreResponse;
import com.findeks.miniscore.service.CreditScoreService;

import lombok.RequiredArgsConstructor;


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