package servlet;

import dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Servlet che gestisce il reset della password
 */

@WebServlet("/ResetPasswordServlet")
public class ResetPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

 // Inizializza l'oggetto UserDAO
    
    public void init() {
        userDAO = new UserDAO();
    }

    // Gestisce le richieste GET mostrando il form per l'inserimento della nuova password
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");

        if (token == null || token.trim().isEmpty()) {
            response.getWriter().println("Token mancante o non valido.");
            return;
        }

        // Mostra form HTML per l'inserimento della nuova password
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().println("<html><body>");
        response.getWriter().println("<h2>Reimposta la tua password</h2>");
        response.getWriter().println("<form method='post' action='ResetPasswordServlet'>");
        response.getWriter().println("<input type='hidden' name='token' value='" + token + "'/>");
        response.getWriter().println("Nuova password: <input type='password' name='password' required/><br>");
        response.getWriter().println("Conferma password: <input type='password' name='confirmPassword' required/><br>");
        response.getWriter().println("<button type='submit'>Reimposta</button>");
        response.getWriter().println("</form>");
        response.getWriter().println("</body></html>");
    }

    // Gestisce le richieste POST per aggiornare la password nel db
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // Controlla la presenza del token
        if (token == null || token.trim().isEmpty()) {
            response.getWriter().println("Token mancante.");
            return;
        }

        // Verifica che le password coincidano
        if (!password.equals(confirmPassword)) {
            response.getWriter().println("Le password non coincidono. Torna indietro e riprova.");
            return;
        }

        // Genera l'hash della nuova password
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());

        try {
            // Aggiorna la password nel db  utilizzando il token
            boolean success = userDAO.updatePasswordByToken(token, hashed);
            if (success) {
                // Risposta HTML con messaggio di successo e reindirizzamento automatico al login
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("<html><head>");
                response.getWriter().println("<meta http-equiv='refresh' content='3;url=login.jsp'/>");
                response.getWriter().println("<title>Password Reimpostata</title>");
                response.getWriter().println("</head><body>");
                response.getWriter().println("<h2>Password aggiornata con successo!</h2>");
                response.getWriter().println("<p>Verrai reindirizzato alla pagina di login tra pochi secondi...</p>");
                response.getWriter().println("</body></html>");
            } else {
                // Il token è errato o già utilizzato
                response.getWriter().println("Token non valido o già usato.");
            }
        } catch (SQLException e) {         
            throw new ServletException(e); // Gestione delle eccezioni SQL
        }
    }
}
