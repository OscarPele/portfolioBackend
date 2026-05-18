package com.portfolioBackend.auth.mail;

/**
 * Puerto de salida para enviar correos HTML.
 */
public interface MailSenderPort {
    /**
     * Envia un correo HTML al destinatario indicado.
     */
    void send(String to, String subject, String htmlBody);
}
