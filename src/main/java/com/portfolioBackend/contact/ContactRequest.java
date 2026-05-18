package com.portfolioBackend.contact;

/**
 * Datos enviados desde el formulario publico de contacto.
 */
public record ContactRequest(String name, String email, String message) {}
