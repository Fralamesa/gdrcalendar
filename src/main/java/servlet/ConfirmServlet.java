package servlet;

import dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Servlet per la conferma dell'email dell'utente tramite token.
 * L'utente viene spostato da `pending_users` a `users` se il token è valido.
 */
@WebServlet("/ConfirmServlet")
public class ConfirmServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    /**
     * Inizializza il DAO degli utenti al momento dell'avvio della servlet.
     */
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Gestisce la conferma dell'account utente.
     * Riceve un token tramite parametro GET e, se valido, attiva l'account.
     *
     * @param request  richiesta HTTP contenente il parametro "token"
     * @param response risposta HTTP con messaggio HTML di conferma o errore
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");

        // Verifica presenza del token
        if (token == null || token.trim().isEmpty()) {
            response.getWriter().println("Token mancante.");
            return;
        }

        try {
            // Tenta la conferma dell'utente tramite il token
            boolean confirmed = userDAO.confirmUser(token);

            if (confirmed) {
                // Token valido: mostra messaggio di conferma e redirect automatico alla login
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("<html><head>");
                response.getWriter().println("<meta http-equiv='refresh' content='3;url=login.jsp'/>");
                response.getWriter().println("<title>Registrazione Confermata</title>");
                response.getWriter().println("</head><body>");
                response.getWriter().println("<h2>Registrazione confermata!</h2>");
                response.getWriter().println("<p>Verrai reindirizzato alla pagina di login tra pochi secondi...</p>");
                response.getWriter().println("</body></html>");
            } else {
                // Token non valido o già utilizzato
                response.getWriter().println("Token non valido, già usato o scaduto.");
            }
        } catch (SQLException e) {
            // Solleva eccezione in caso di errore di accesso al database
            throw new ServletException(e);
        }
    }
}

