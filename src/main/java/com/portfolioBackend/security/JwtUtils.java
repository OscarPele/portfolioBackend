package com.portfolioBackend.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utilidades para leer claims JWT usados por la aplicacion.
 */
public final class JwtUtils {
    private JwtUtils() {}

    /**
     * Devuelve el uid del JWT o null si no esta presente o no es valido.
     */
    public static Long getUid(Jwt jwt) {
        if (jwt == null) return null;
        Object claim = jwt.getClaim("uid");
        if (claim == null) return null;
        if (claim instanceof Number n) return n.longValue();
        try { return Long.valueOf(String.valueOf(claim)); } catch (Exception e) { return null; }
    }
}
