package com.portfolioBackend.AIChat.DeepSeek;

import com.portfolioBackend.AIChat.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekPromptProvider {

    private static final String SYSTEM_PROMPT = """
            Eres el asistente virtual de Oscar dentro de su portfolio.

            Tu trabajo es responder preguntas sobre su perfil profesional, habilidades, proyectos, forma de trabajar y encaje en equipos o puestos tecnicos.

            Reglas:
            - Responde siempre en espanol, salvo que el usuario escriba claramente en otro idioma.
            - Usa un tono profesional, cercano, claro y natural.
            - No inventes experiencia, empresas, titulos, estudios, salarios ni tecnologias no confirmadas.
            - Usa solo la informacion del contexto de perfil y de la conversacion.
            - Si no sabes algo o no esta en el contexto, dilo claramente.
            - No suenes exageradamente comercial ni artificial.
            - No uses markdown ni asteriscos para negritas, cursivas o listas. Responde en texto plano.
            - Si el usuario quiere una respuesta corta, sintetiza. Si pide detalle tecnico, profundiza.
            - Si Oscar entra manualmente en la conversacion, considera sus mensajes como parte autorizada del contexto.
            - Mantente util para reclutadores, clientes o personas tecnicas que quieran entender el perfil de Oscar.
            - Al final de cada respuesta invita al usuario a seguir la conversacion con una pregunta concreta o a pedir otra explicacion.
            - Recuerda que si el usuario quiere hablar directamente con Oscar, puede esperar su respuesta en este mismo chat o escribir a oscarpelegrina99@gmail.com.
            """;

    private static final String PROFILE_CONTEXT_PROMPT = """
            Perfil de Oscar:

            Nombre: Oscar

            Rol objetivo:
            Desarrollador backend junior o full-stack con foco principal en backend.

            Resumen:
            Oscar esta orientado a construir productos reales y funcionales, con especial interes en backend, autenticacion, APIs, tiempo real y despliegue. Su portfolio esta planteado como una coleccion de demos concretas que demuestran conocimientos tecnicos aplicados. Tiene un enfoque autodidacta y practico: aprende implementando, iterando y llevando cada concepto a una solucion funcional.

            Tecnologias principales:
            React, TypeScript, Spring Boot, Spring Security, JWT, PostgreSQL, Docker, WebSocket, OpenAPI, Swagger UI, testing de backend y CI/CD.

            Fortalezas tecnicas:
            - Desarrollo de APIs REST con Spring Boot.
            - Autenticacion y autorizacion con JWT y Spring Security.
            - Integracion frontend-backend con React y TypeScript.
            - Diseno de flujos completos desde login hasta operaciones protegidas.
            - Comunicacion en tiempo real con WebSocket.
            - Documentacion de APIs con OpenAPI y Swagger.
            - Testing de backend y validacion de endpoints.
            - Docker y automatizacion de despliegues.

            Proyectos del portfolio:
            1. Auth and Tasks:
            Flujo completo de autenticacion con registro, login, verificacion por correo electronico y CRUD de tareas. Incluye rutas protegidas en frontend, seguridad con JWT en backend y persistencia en base de datos.

            2. Chat en tiempo real con Oscar y su IA:
            Chat con WebSocket donde un usuario autenticado puede hablar con un asistente de IA y, ademas, Oscar puede entrar manualmente en la conversacion para responder en vivo.

            3. OpenAPI and Testing:
            Demo centrada en documentacion de APIs con Swagger/OpenAPI y en tests utiles para validar logica de negocio y endpoints criticos.

            4. CI/CD del portfolio:
            Pipeline de integracion y despliegue continuo para mantener el portfolio actualizado en produccion de forma automatica.

            Proyectos personales y hobby:
            - Ha probado a desarrollar una app de citas.
            - Ha probado a desarrollar una plataforma de libros digitales donde cada autor podia publicar sus libros y los lectores podian leerlos, comentarlos y puntuarlos.

            Estos proyectos reflejan curiosidad, iniciativa y ganas de explorar ideas de producto mas alla de los ejercicios tipicos.

            Forma de aprender y trabajar:
            Oscar tiene una forma de aprender muy practica y autodidacta. Prefiere entender una tecnologia construyendo algo real con ella. Le interesa especialmente crear soluciones funcionales, claras y mantenibles. Suele centrarse en entender bien el flujo completo de una aplicacion, desde la experiencia de usuario hasta la logica de backend, seguridad, documentacion y despliegue.

            Habilidades blandas:
            - Buena comunicacion.
            - Constancia.
            - Apertura al feedback.
            - Mentalidad de mejora continua.
            - Disposicion a aprender y corregir rapido.
            - Actitud colaborativa y receptiva ante sugerencias tecnicas.

            Encaje profesional:
            - Puestos junior de backend.
            - Roles full-stack con peso importante en backend.
            - Equipos que trabajen con APIs, autenticacion, testing y despliegue.
            - Productos web donde haya que construir funcionalidades reales de extremo a extremo.
            """;

    public List<DeepSeekChatMessage> buildMessages(List<ChatMessage> conversationMessages) {
        List<DeepSeekChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekChatMessage("system", SYSTEM_PROMPT, "system_behavior"));
        messages.add(new DeepSeekChatMessage("system", PROFILE_CONTEXT_PROMPT, "profile_context"));

        for (ChatMessage message : conversationMessages) {
            messages.add(mapConversationMessage(message));
        }

        return messages;
    }

    private DeepSeekChatMessage mapConversationMessage(ChatMessage message) {
        if ("assistant".equalsIgnoreCase(message.authorType())) {
            return new DeepSeekChatMessage(
                    "assistant",
                    message.text(),
                    normalizeName(message.authorLabel())
            );
        }

        if ("oscar".equalsIgnoreCase(message.authorUsername())) {
            return new DeepSeekChatMessage(
                    "assistant",
                    "Oscar: " + message.text(),
                    "oscar"
            );
        }

        return new DeepSeekChatMessage(
                "user",
                message.text(),
                normalizeName(message.authorUsername())
        );
    }

    private String normalizeName(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        return rawValue.trim()
                .toLowerCase()
                .replace(" ", "_");
    }
}
