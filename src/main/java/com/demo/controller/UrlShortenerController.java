package com.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.demo.service.RateLimitService;
import com.demo.service.UrlShortenerService;

@Controller
@RequestMapping("api")
public class UrlShortenerController {

	
	private RateLimitService rateLimitService ;
	private UrlShortenerService urlShortenerService;
	@PostMapping("/shorten")
	public ResponseEntity<?> shortenurl()
	{
		return null;
	}

}
