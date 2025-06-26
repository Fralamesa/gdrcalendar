package servlet;

import dao.UserDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Servlet per gestire la richiesta di recupero password.
 * Invia un'email contenente un link con token per il reset.
 */
@WebServlet("/RecoverPasswordServlet")
public class RecoverPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Gestisce la richiesta POST per l'invio dell'email di reset password.
     * Se l'email esiste, genera un token di recupero e invia un link via email.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");

        try {
            // Verifica se l'email è registrata nel sistema (users o pending_users)
            if (!userDAO.emailExists(email)) {
                response.getWriter().println("Errore: Nessun utente trovato con questa email.");
                return;
            }

            // Genera token univoco e lo salva sul database associato all'utente
            String resetToken = UUID.randomUUID().toString();
            userDAO.saveResetToken(email, resetToken);

            // Crea link per il reset della password e invia via email
            String link = "https://gdrcalendar.onrender.com/ResetPasswordServlet?token=" + resetToken;
            sendResetEmail(email, link);

            // Risposta generica per non rivelare informazioni sensibili
            response.getWriter().println("Se l'email è corretta, riceverai un link per reimpostare la password. Ignora se non l'hai richiesto.");

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Invia un'email con il link per il reset della password.
     * Configura i parametri SMTP e utilizza un account Gmail per l'invio.
     */
    private void sendResetEmail(String to, String link) {
        final String from = "arxdraconisgdr@gmail.com";
        final String password = "vlsuymcdihewfwuv"; // Attenzione: andrebbe protetta in configurazione esterna

        // Configurazione SMTP per Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Crea sessione autenticata per l'invio
        Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            // Costruzione del messaggio email
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Reset Password GDRCalendar");
            message.setText("Hai richiesto di reimpostare la password. Clicca questo link:\n" + link +
                    "\nSe non l'hai richiesto, ignora questo messaggio.");

            // Invio dell'email
            Transport.send(message);

        } catch (MessagingException e) {
            e.printStackTrace(); // In contesti reali andrebbe loggato in modo sicuro
        }
    }
}
