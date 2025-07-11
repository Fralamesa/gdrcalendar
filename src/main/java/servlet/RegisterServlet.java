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
import org.mindrot.jbcrypt.BCrypt;

/**
 * Gestisce la registrazione dei nuovi utenti.
 * I dati vengono salvati in attesa di conferma (pending_users)
 * e viene inviata un'email con link di attivazione.
 */


@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Elabora la richiesta POST per registrare un nuovo utente.
     * - Verifica email e password.
     * - Crea un record temporaneo in pending_users.
     * - Invia una mail di conferma con token.
     */
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String nome = request.getParameter("nome");
        String cognome = request.getParameter("cognome");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String community = request.getParameter("community");

        // Controlla che la password inserita sia uguale alla conferma
        if (!password.equals(confirmPassword)) {
            response.getWriter().println("Errore: le password non coincidono.");
            return;
        }

        try {
            // Pulisce eventuali registrazioni pendenti scadute (oltre 5 minuti)
            userDAO.deleteExpiredPendingUsers();

            // Verifica che l'email non sia già registrata (attiva o in pending)
            if (userDAO.emailExists(email)) {
                response.getWriter().println("Errore: esiste già un account con questa email.");
                return;
            }

            // Hash della password e generazione token di conferma
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            String token = UUID.randomUUID().toString();

            // Salvataggio nella tabella temporanea
            userDAO.savePendingUser(nome, cognome, email, hashed, community, token);

            // Genera e invia all’utente il link per attivare l’account via email
            String link = "https://gdrcalendar.onrender.com/ConfirmServlet?token=" + token;
            sendConfirmationEmail(email, link);

            response.getWriter().println("Registrazione ricevuta! Controlla la tua email.");

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Invia un'email all'utente con il link per completare la registrazione.
     * Il link contiene un token univoco generato durante la registrazione.
     *
     * @param to   Email del destinatario
     * @param link URL di conferma con token
     */
    
    private void sendConfirmationEmail(String to, String link) {
        final String from = "arxdraconisgdr@gmail.com";
        final String password = "vlsuymcdihewfwuv"; 

        // Parametri SMTP per Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Crea una Session SMTP autenticata con Gmail
        Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            // Composizione ed invio del messaggio
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Conferma Registrazione GDRCalendar");
            message.setText("Ciao!\n\nPer completare la registrazione, clicca su questo link:\n" + link +
                            "\n\nSe non hai richiesto la registrazione, puoi ignorare questo messaggio.");

            Transport.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
