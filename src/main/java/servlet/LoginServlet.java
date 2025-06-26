package servlet;

import dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Servlet responsabile dell'autenticazione utente.
 * Verifica le credenziali inserite e, se corrette, crea una sessione con i dati dell'utente.
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    /**
     * Inizializza la servlet creando un'istanza di UserDAO per l'accesso al database.
     */
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

        // Estrae parametri email e password dalla richiesta
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            // Verifica la validità delle credenziali (password hashata confrontata via BCrypt)
            boolean valid = userDAO.validateUser(email, password);

            if (valid) {
                // Recupera il ruolo associato all'utente (es. Giocatore, Master)
                String ruolo = userDAO.getRuoloByEmail(email);

                // Inizializza la sessione e imposta attributi utente
                HttpSession session = request.getSession();
                session.setAttribute("userEmail", email);
                session.setAttribute("userRuolo", ruolo);

                // Reindirizza alla dashboard appropriata in base al ruolo
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


