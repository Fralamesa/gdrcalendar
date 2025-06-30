package servlet;

import dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Servlet  per l' autenticazione utente.
 * Verifica le credenziali inserite e, se corrette, crea una sessione con i dati dell'utente.
 */

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    // Inizializza l'oggetto UserDAO

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Gestisce la richiesta POST per il login.
     * Controlla le credenziali fornite e, se valide, avvia una sessione utente.
     */
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Estrae email e password dal form di login
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
        	// Controlla se le credenziali sono corrette confrontando l'hash della password con quella salvata
            boolean valid = userDAO.validateUser(email, password);

            if (valid) {
            	// Se l'utente è valido, recupera il ruolo (Master o Giocatore)
                String ruolo = userDAO.getRuoloByEmail(email);

                // Crea una nuova sessione e salva le info dell'utente (usate poi da tutte le altre servlet)
                HttpSession session = request.getSession();
                session.setAttribute("userEmail", email);
                session.setAttribute("userRuolo", ruolo);

                // Reindirizza alla dashboard adatta in base al ruolo
                if ("Master".equalsIgnoreCase(ruolo)) {
                    response.sendRedirect("dashboardMaster.jsp");
                } else {
                    response.sendRedirect("dashboardGiocatore.jsp");
                }

            } else {
                // Credenziali non valide: mostra messaggio di errore
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("Login fallito. Credenziali errate.");
            }

        } catch (SQLException e) {
            // Gestione eccezioni SQL come errore interno del server
            throw new ServletException("Errore durante l'autenticazione", e);
        }
    }
}


