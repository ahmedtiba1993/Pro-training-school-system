package com.tiba.pts.modules.auth.dto;

public record AuthData(String token, String type, long expiresIn, UserInfo user) {}
