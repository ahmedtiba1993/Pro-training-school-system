package com.tiba.pts.auth.dto;

public record AuthData(String token, String type, long expiresIn, UserInfo user) {}
