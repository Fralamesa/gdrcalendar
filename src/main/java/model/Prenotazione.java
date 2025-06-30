package model;

/**
 * Modello che rappresenta una prenotazione a un evento.
 * Include l'email dell'utente e una nota inserita al momento della prenotazione.
 */

public class Prenotazione {
    private String email; //  identificativoa dell'utente che ha prenotato
    private String note;  

    /**
     * Costruisce un oggetto Prenotazione a partire dai dati letti dal DB.
     */
    
    public Prenotazione(String email, String note) {
        this.email = email;
        this.note = note;
    }

 // Getters per i dati statici letti da db (tabella prenotazioni)
    
    public String getEmail() { return email; }
    public String getNote() { return note; }
}
