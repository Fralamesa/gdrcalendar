package servlet;

import dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Servlet per la gestione del profilo utente autenticato.
 * Supporta aggiornamento dell'email, della password e del ruolo (solo per Master).
 */
@WebServlet("/ProfileServlet")
public class ProfileServlet extends HttpServlet {
  private UserDAO userDAO;

  /**
   * Inizializza il DAO per l'accesso ai dati utente.
   */
  @Override
  public void init() {
    userDAO = new UserDAO();
  }

  /**
   * Gestisce operazioni di aggiornamento profilo utente tramite richieste POST.
   * Richiede sessione attiva e autenticata.
   *
   * Azioni supportate:
   * - updateEmail     → cambia l'email dell'utente corrente
   * - updatePassword  → cambia la password dell'utente corrente
   * - updateRole      → cambia il ruolo di un altro utente (solo se l'utente è Master)
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // Verifica che la sessione sia attiva e che l'utente sia autenticato
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("userEmail") == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    String action = request.getParameter("action");
    String currentEmail = (String) session.getAttribute("userEmail");

    try {
      switch (action) {

        // === Aggiornamento Email ===
        case "updateEmail":
          String newEmail = request.getParameter("newEmail");
          userDAO.updateEmail(currentEmail, newEmail);
          session.setAttribute("userEmail", newEmail); // aggiorna la sessione
          response.getWriter().write("Email aggiornata!");
          break;

        // === Aggiornamento Password ===
        case "updatePassword":
          String newPassword = request.getParameter("newPassword");
          String confirm = request.getParameter("confirmPassword");

          if (!newPassword.equals(confirm)) {
            response.getWriter().write("Le password non coincidono!");
          } else {
            userDAO.updatePassword(currentEmail, newPassword);
            response.getWriter().write("Password aggiornata!");
          }
          break;

        // === Aggiornamento Ruolo di un altro utente (solo Master) ===
        case "updateRole":
          String userRole = (String) session.getAttribute("userRuolo");
          if (!"Master".equals(userRole)) {
            response.getWriter().write("Non autorizzato!");
            return;
          }
          String targetEmail = request.getParameter("targetEmail");
          String newRole = request.getParameter("newRole");
          userDAO.updateUserRole(targetEmail, newRole);
          response.getWriter().write("Ruolo aggiornato!");
          break;
          
       // Elimina utente e prenotazioni associate
        case "deleteAccount":
          String confirmDelete = request.getParameter("confirmDelete");
          if (!"DELETE".equalsIgnoreCase(confirmDelete)) {
            response.getWriter().write("Conferma non valida. Scrivi DELETE per confermare.");
            return;
          }
          // Elimina prima tutte le prenotazioni associate
          userDAO.deletePrenotazioniByEmail(currentEmail);
          // Elimina l'account utente
          userDAO.deleteUserByEmail(currentEmail);
          session.invalidate();
          response.getWriter().write("Account eliminato con successo.");
          break;
      }

    } catch (Exception e) {
      e.printStackTrace(); // utile in fase di debug
      response.getWriter().write("Errore: " + e.getMessage());
    }
  }
}
