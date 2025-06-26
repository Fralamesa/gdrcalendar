<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Verifica che l'utente sia autenticato e abbia il ruolo "Master"
    // In caso contrario, reindirizza alla pagina di login
    if (session == null || session.getAttribute("userEmail") == null || !"Master".equals(session.getAttribute("userRuolo"))) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <title>Dashboard Master</title>

    <!-- Importazione font Inter da Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&display=swap" rel="stylesheet">

    <style>
        /* Imposta box-sizing per tutti gli elementi */
        * {
            box-sizing: border-box;
        }

        /* Stili di base per html e body: layout verticale, sfondo gradiente */
        html, body {
            margin: 0;
            padding: 0;
            height: 100%;
            font-family: 'Inter', sans-serif;
            background: linear-gradient(135deg, #74ebd5 0%, #ACB6E5 100%);
            color: #333;
            display: flex;
            flex-direction: column;
        }

        /* Stili condivisi per header e footer: sfondo traslucido e ombra */
        header, footer {
            background: rgba(255, 255, 255, 0.15);
            color: white;
            backdrop-filter: blur(12px);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
        }

        /* Header: layout con titolo e azioni utente */
        header {
            padding: 20px 40px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            position: sticky;
            top: 0;
            z-index: 100;
            border-bottom: 1px solid rgba(255,255,255,0.1);
        }

        /* Titolo nella barra superiore */
        header h1 {
            margin: 0;
            font-size: 1.6em;
            letter-spacing: 1px;
            text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.3);
        }

        /* Area utente nel header */
        header div {
            font-size: 0.95em;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        /* Link nel header (es. logout) */
        header a {
            color: #ffffff;
            text-decoration: none;
            font-weight: bold;
            padding: 8px 16px;
            background: rgba(255, 255, 255, 0.1);
            border-radius: 20px;
            transition: all 0.3s ease;
        }

        /* Effetto hover sul link del header */
        header a:hover {
            background: rgba(255, 255, 255, 0.25);
        }

        /* Contenitore principale della pagina */
        .main-content {
            flex: 1;
            padding: 30px;
            background: white;
            box-shadow: 0 0 20px rgba(0, 0, 0, 0.05);
        }

        /* Card utilizzate per contenere sezioni (es. calendario) */
        .card {
            background: rgba(255, 255, 255, 0.15);
            border-radius: 20px;
            padding: 25px;
            backdrop-filter: blur(8px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
            margin-bottom: 30px;
        }

        /* Controlli per navigare tra i mesi del calendario */
        #calendar-controls {
            display: flex;
            justify-content: space-between;
            margin-bottom: 20px;
        }

        /* Stile dei pulsanti di navigazione calendario */
        #calendar-controls button {
            padding: 10px 20px;
            background-color: #ffffff;
            color: #333;
            border: none;
            border-radius: 30px;
            font-weight: bold;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            transition: all 0.3s ease;
        }

        /* Effetto hover sui pulsanti calendario */
        #calendar-controls button:hover {
            background: #f0f0f0;
            transform: translateY(-2px);
        }

        /* Tabella del calendario */
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
            background: white;
            border-radius: 12px;
            overflow: hidden;
        }

        /* Celle della tabella */
        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: center;
            height: 80px;
        }

        /* Celle che contengono eventi */
        td.has-event {
            background-color: #d0f0fd;
            font-weight: bold;
            cursor: pointer;
            transition: background 0.2s;
        }

        /* Effetto hover sulle celle con eventi */
        td.has-event:hover {
            background-color: #b2e6fb;
        }

        /* Stile del footer */
        footer {
            padding: 30px 20px;
            text-align: center;
            border-top: 1px solid rgba(255,255,255,0.1);
            margin-top: auto;
        }

        /* Titolo delle sezioni nel footer */
        .footer-section h3 {
            margin-bottom: 15px;
            color: #fff;
        }

        /* Contenitore dei pulsanti nel footer */
        .footer-buttons {
            display: flex;
            justify-content: center;
            flex-wrap: wrap;
            gap: 15px;
            margin-top: 10px;
        }

        /* Pulsanti di azione nel footer */
        .footer-buttons button {
            padding: 10px 18px;
            background-color: #ffffff;
            color: #333;
            border: none;
            border-radius: 30px;
            font-weight: bold;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
            transition: background 0.3s, transform 0.2s;
        }

        /* Effetto hover sui pulsanti del footer */
        .footer-buttons button:hover {
            background-color: #f0f0f0;
            transform: translateY(-2px);
        }

        /* Overlay per la modale */
        .modal-overlay {
            position: fixed;
            top: 0; left: 0;
            width: 100%; height: 100%;
            background: rgba(0,0,0,0.5);
            display: none;
            justify-content: center;
            align-items: center;
            z-index: 999;
        }

        /* Contenuto della modale */
        .modal-content {
            background: #fff;
            padding: 30px;
            border-radius: 16px;
            max-width: 450px;
            width: 90%;
            box-shadow: 0 8px 24px rgba(0,0,0,0.2);
            animation: fadeIn 0.4s ease-out;
        }

        .modal-content h3 {
            margin-top: 0; /* Rimuove il margine superiore per il titolo della modale */
        }

        .modal-content form {
            display: flex;
            flex-direction: column; /* Disposizione verticale degli elementi del form */
        }

        .modal-content input,
        .modal-content select,
        .modal-content button {
            margin: 10px 0;
            padding: 12px;
            font-size: 14px;
            border-radius: 8px;
            border: 1px solid #ccc; /* Bordo sottile e chiaro per uniformità */
        }

        .modal-content button {
            background-color: #3498db; /* Colore di sfondo blu */
            color: white;
            border: none;
            cursor: pointer;
        }

        .modal-content button:hover {
            background-color: #2980b9; /* Colore più scuro al passaggio del mouse */
        }

        .close-modal {
            float: right;
            font-weight: bold;
            color: #999;
            cursor: pointer; /* Icona per chiudere la modale */
        }

        #event-details {
            display: none;
            position: fixed;
            top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            background: #fff;
            border-radius: 16px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.2);
            padding: 30px;
            z-index: 1000;
            max-width: 400px;
            width: 90%; /* Finestra modale per mostrare i dettagli di un evento */
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); } /* Animazione di comparsa modale */
        }
    </style>
</head>
<body>

<!-- Intestazione con titolo e dati utente loggato -->
<header>
    <h1>Dashboard Master</h1>
    <div>
        Benvenuto: <%= session.getAttribute("userEmail") %> | <a href="logout.jsp">Logout</a>
    </div>
</header>

<!-- Contenuto principale: sezione calendario eventi -->
<div class="main-content">
    <div class="card" id="calendar-section">
        <h2>Calendario Eventi</h2>
        <div id="calendar-controls">
            <button id="prev-month">« Mese precedente</button>
            <span id="current-month" style="line-height: 36px; flex: 0 1 100px; text-align: center;"></span>
            <button id="next-month">Mese successivo »</button>
        </div>
        <div id="calendar"></div> <!-- Area in cui verrà generato il calendario -->
    </div>
</div>

<!-- Footer con pulsanti di gestione -->
<footer>
    <div class="footer-section">
        <h3>Impostazioni</h3>
        <div class="footer-buttons">
            <button id="btn-create-event">Crea Nuovo Evento</button>
            <button id="btn-config-giochi">Configura Giochi</button>
            <button id="btn-update-auth">Gestione Profilo</button>
            <button id="btn-update-role">Aggiorna Ruolo</button>
        </div>
    </div>
</footer>

<!-- Struttura della modale generica riutilizzabile -->
<div id="modal" class="modal-overlay">
    <div class="modal-content">
        <span class="close-modal" onclick="closeModal()">×</span> <!-- Pulsante chiusura -->
        <div id="modal-body"></div> <!-- Contenuto dinamico della modale -->
    </div>
</div>

<!-- Contenitore modale dedicato ai dettagli evento -->
<div id="event-details">
    <div id="event-content"></div>
</div>

<!-- Script per logica calendario e interazioni -->
<script src="js/calendar.js"></script>

<script>
//Mostra la modale e inserisce l'HTML passato come contenuto
function openModal(html) {
    document.getElementById("modal-body").innerHTML = html;
    document.getElementById("modal").style.display = "flex";
}

// Nasconde la modale attualmente visibile
function closeModal() {
    document.getElementById("modal").style.display = "none";
}

// Mostra la modale per creare un nuovo evento
document.getElementById("btn-create-event").onclick = () => {
    openModal(`
        <h3>Crea Nuovo Evento</h3>
        <form id="create-event-form">
            <input type="text" name="titolo" placeholder="Titolo" required>
            <input type="text" name="descrizione" placeholder="Descrizione" maxlength="200" required>
            <select name="tipo_gioco" id="tipo_gioco_select_modal" required></select>
            <input type="text" name="master" placeholder="Nome del Master" required>
            <input type="number" name="max_giocatori" placeholder="Max Giocatori" min="1" required>
            <input type="datetime-local" name="data_inizio" required>
            <input type="datetime-local" name="data_fine" required>
            <input type="text" name="luogo" placeholder="Luogo">
            <input type="text" name="note" placeholder="Note">
            <button type="submit">Crea</button>
        </form>
    `);
    // Popola il select con i tipi di gioco disponibili
    loadGameTypes('tipo_gioco_select_modal');
    // Associa la logica di invio del form
    bindEventForm();
};

// Mostra la modale per aggiungere e visualizzare i tipi di gioco
document.getElementById("btn-config-giochi").onclick = () => {
    openModal(`
        <h3>Configura Giochi</h3>
        <form id="add-game-type-form">
            <input type="text" name="nome" placeholder="Nuovo tipo di gioco" required>
            <button type="submit">Aggiungi</button>
        </form>
        <ul id="game-type-list"></ul>
    `);
    // Carica la lista dei tipi di gioco esistenti
    loadGameTypesList();
    // Associa la funzione per aggiungere un nuovo tipo
    document.getElementById('add-game-type-form').onsubmit = addGameType;
};

// Mostra la modale per aggiornare email e password e cancellare profilo
document.getElementById("btn-update-auth").onclick = () => {
    openModal(`
        <h3>Gestione Profilo</h3>
        <form id="update-email-form">
            <input type="email" name="newEmail" placeholder="Nuova Email" required>
            <button type="submit">Aggiorna Email</button>
        </form>
        <form id="update-password-form">
            <input type="password" name="newPassword" placeholder="Nuova Password" required>
            <input type="password" name="confirmPassword" placeholder="Conferma Password" required>
            <button type="submit">Aggiorna Password</button>
        </form>
        <form id="delete-account-form">
            <input type="text" name="confirmDelete" placeholder='Scrivi "DELETE" per confermare' required>
            <button type="submit" style="background-color: #e74c3c;">Elimina Account</button>
        </form>
    `);
    // Associa i comportamenti ai form
    bindAuthForms();
    bindDeleteForm();
};

// Mostra la modale per aggiornare il ruolo di un utente
document.getElementById("btn-update-role").onclick = () => {
    openModal(`
        <h3>Aggiorna Ruolo</h3>
        <form id="update-role-form">
            <input type="email" name="targetEmail" placeholder="Email utente" required>
            <select name="newRole">
                <option value="Giocatore">Giocatore</option>
                <option value="Master">Master</option>
            </select>
            <button type="submit">Aggiorna</button>
        </form>
    `);
    // Invio asincrono del form per aggiornare il ruolo
    document.getElementById('update-role-form').onsubmit = async (e) => {
        e.preventDefault();
        const data = new URLSearchParams(new FormData(e.target));
        data.append("action", "updateRole");
        const res = await fetch("ProfileServlet", { method: "POST", body: data });
        alert(await res.text());
        closeModal();
    };
};

// Gestisce l'invio del form per creare un nuovo evento
function bindEventForm() {
    document.getElementById('create-event-form').onsubmit = async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);
        const inizio = new Date(formData.get("data_inizio"));
        const fine = new Date(formData.get("data_fine"));

        // Validazione: la data di fine deve essere successiva a quella di inizio
        if (fine < inizio) {
            alert("La data di fine non può essere prima della data di inizio.");
            return;
        }

        const data = new URLSearchParams(formData);
        data.append("action", "create");
        const res = await fetch("EventServlet", { method: "POST", body: data });
        alert(await res.text());
        location.reload(); // Ricarica la pagina per aggiornare il calendario
    };
}

function bindAuthForms() {
    // Form per aggiornare l'email
    document.getElementById('update-email-form').onsubmit = async (e) => {
        e.preventDefault();
        const data = new URLSearchParams(new FormData(e.target));
        data.append("action", "updateEmail");
        const res = await fetch("ProfileServlet", { method: "POST", body: data });
        alert(await res.text());
        closeModal();
    };

    // Form per aggiornare la password
    document.getElementById('update-password-form').onsubmit = async (e) => {
        e.preventDefault();
        const data = new URLSearchParams(new FormData(e.target));
        data.append("action", "updatePassword");
        const res = await fetch("ProfileServlet", { method: "POST", body: data });
        alert(await res.text());
        closeModal();
    };
}

// Spostata fuori da bindAuthForms
function bindDeleteForm() {
    document.getElementById('delete-account-form').onsubmit = async (e) => {
        e.preventDefault();
        const confirmInput = e.target.confirmDelete.value.trim();
        if (confirmInput !== "DELETE") {
            alert("Devi digitare 'DELETE' per confermare.");
            return;
        }
        const data = new URLSearchParams();
        data.append("action", "deleteAccount");
        const res = await fetch("ProfileServlet", {
            method: "POST",
            body: data
        });
        alert(await res.text());
        window.location.href = "logout.jsp";
    };
}


// Carica dinamicamente i tipi di gioco disponibili nel <select> indicato
async function loadGameTypes(selectId) {
    const res = await fetch("GameTypeServlet?action=list");
    const types = await res.json();
    const select = document.getElementById(selectId);
    if (!select) return;

    select.innerHTML = "";
    types.forEach(name => {
        const opt = document.createElement("option");
        opt.value = name;
        opt.textContent = name;
        select.appendChild(opt);
    });
}

// Carica e mostra la lista dei tipi di gioco nella configurazione
async function loadGameTypesList() {
    const res = await fetch("GameTypeServlet?action=list");
    const types = await res.json();
    const ul = document.getElementById("game-type-list");
    ul.innerHTML = "";

    types.forEach(name => {
        const li = document.createElement("li");
        li.textContent = name + " ";
        const btn = document.createElement("button");
        btn.textContent = "Elimina";
        btn.onclick = () => deleteGameType(name); // Associa la funzione di eliminazione
        li.appendChild(btn);
        ul.appendChild(li);
    });
}

// Invia la richiesta per aggiungere un nuovo tipo di gioco
async function addGameType(e) {
    e.preventDefault();
    const data = new URLSearchParams(new FormData(e.target));
    data.append("action", "add");
    const res = await fetch("GameTypeServlet", { method: "POST", body: data });
    alert(await res.text());
    e.target.reset();
    loadGameTypesList(); // Ricarica la lista aggiornata
}

// Invia la richiesta per eliminare un tipo di gioco
async function deleteGameType(name) {
    if (confirm(`Eliminare "${name}"?`)) {
        const data = new URLSearchParams({ action: "delete", nome: name });
        const res = await fetch("GameTypeServlet", { method: "POST", body: data });
        alert(await res.text());
        loadGameTypesList(); // Ricarica la lista dopo eliminazione
        }
    }
</script>

</body>
</html>
