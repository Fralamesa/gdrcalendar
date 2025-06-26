<%@ page contentType="text/html;charset=UTF-8" language="java" %> <%-- Impostazioni della pagina JSP: output HTML, codifica UTF-8, linguaggio Java --%>

<%
    // Verifica se è presente una sessione attiva per l'utente
    if (session != null) {
        session.invalidate(); // Invalida la sessione corrente: utile per logout, rimuove tutti gli attributi (es. utente, ruoli, ecc.)
    }

    // Dopo l'invalidazione, l'utente viene reindirizzato alla home page principale
    response.sendRedirect("index.jsp"); // Redirect server-side immediato per evitare che l'utente resti su una pagina di logout "vuota"
%>
