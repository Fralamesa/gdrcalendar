package model;

/**
 * Rappresenta una prenotazione a un evento da parte di un utente.
 * Contiene l'indirizzo email dell'utente e un'eventuale nota allegata alla prenotazione.
 */
public class Prenotazione {
    private String email; // Email dell'utente che ha effettuato la prenotazione
    private String note;  // Nota opzionale associata alla prenotazione

    /**
     * Costruttore della prenotazione.
     *
     * @param email Email dell'utente
     * @param note Nota inserita dall'utente al momento della prenotazione
     */
    public Prenotazione(String email, String note) {
        this.email = email;
        this.note = note;
    }

    // Getter per l'email dell'utente
    public String getEmail() { return email; }

    // Getter per la nota associata alla prenotazione
    public String getNote() { return note; }
}
