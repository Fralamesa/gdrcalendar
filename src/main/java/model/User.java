package model;

/**
 * Modello dati che rappresenta un utente del sistema.
 * Include informazioni anagrafiche, credenziali di accesso,
 * ruolo nel sistema e appartenenza a una community.
 */
public class User {

    private int id;                   // Identificativo univoco dell'utente (PK nel DB)
    private String nome;             // Nome dell'utente
    private String cognome;          // Cognome dell'utente
    private String email;            // Indirizzo email (usato come identificatore per login)
    private String passwordHash;     // Hash sicuro della password (BCrypt)
    private String ruolo;            // Ruolo assegnato (es. "Giocatore" o "Master")
    private String community;        // Nome della community a cui l'utente appartiene

    /**
     * Costruttore vuoto richiesto per l'uso con framework o serializzazione (es. Gson, JSP, Hibernate).
     */
    public User() {}

    // === Getter & Setter ===

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRuolo() { return ruolo; }
    public void setRuolo(String ruolo) { this.ruolo = ruolo; }

    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
}
