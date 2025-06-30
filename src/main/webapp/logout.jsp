<%@ page contentType="text/html;charset=UTF-8" language="java" %> <%-- Impostazioni base per la pagina JSP --%>

<%
    // Verifica se è presente una sessione attiva per l'utente
    if (session != null) {
        session.invalidate(); // Invalida la sessione corrente
    }

    // L'utente viene reindirizzato alla homepage
    response.sendRedirect("index.jsp");
%>
