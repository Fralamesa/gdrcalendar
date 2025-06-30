package servlet;

import dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Gestisce la conferma della registrazione utente via token.
 * Se il token è valido, l'utente viene spostato da `pending_users` a `users`
 * e riceve un messaggio di conferma con redirect alla login.
 */

@WebServlet("/ConfirmServlet")
public class ConfirmServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    
    // Inizializza il DAO degli utenti al momento dell'avvio della servlet.
     
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Gestisce la richiesta GET per confermare la registrazione di un utente.
     * L’utente clicca un link ricevuto via email contenente un token:
     * se il token è valido, l’account viene attivato e l’utente viene reindirizzato al login.
     */
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");

        // Controlla che il token sia presente nella richiesta
        if (token == null || token.trim().isEmpty()) {
            response.getWriter().println("Token mancante.");
            return;
        }

        try {
        	// Prova a confermare l’account usando il token ricevuto
            boolean confirmed = userDAO.confirmUser(token);

            if (confirmed) {
            	 // Token valido: mostra messaggio di successo e reindirizza alla pagina di login
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("<html><head>");
                response.getWriter().println("<meta http-equiv='refresh' content='3;url=login.jsp'/>");
                response.getWriter().println("<title>Registrazione Confermata</title>");
                response.getWriter().println("</head><body>");
                response.getWriter().println("<h2>Registrazione confermata!</h2>");
                response.getWriter().println("<p>Verrai reindirizzato alla pagina di login tra pochi secondi...</p>");
                response.getWriter().println("</body></html>");
            } else {
                // Token non valido
                response.getWriter().println("Token non valido, già usato o scaduto.");
            }
        } catch (SQLException e) {
            // In caso di errore di accesso al db
            throw new ServletException(e);
        }
    }
}

