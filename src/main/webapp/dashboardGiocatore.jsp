<%@ page contentType="text/html;charset=UTF-8" language="java" %> <%--  Impostazioni base per la pagina JSP --%>
<%
	// Verifica che l'utente sia autenticato e abbia il ruolo "Master"
	// In caso contrario, reindirizza alla pagina di login
    if (session == null || session.getAttribute("userEmail") == null || !"Giocatore".equals(session.getAttribute("userRuolo"))) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Dashboard Giocatore</title>

    <%-- Importazione del font Montserrat da Google Fonts --%>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&display=swap" rel="stylesheet">

    <style>
        /* Applica box-sizing globale */
        * {
            box-sizing: border-box;
        }

        /* Stili generali */
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

        /* Stili per header e footer */
        header, footer {
            background: rgba(255, 255, 255, 0.15);
            color: white;
            backdrop-filter: blur(12px);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
        }

        /* Header fisso in alto con layout flessibile */
        header {
    		padding: 20px 40px;
    		display: flex;
    		justify-content: space-between;
    		align-items: center;
    		position: sticky;
   			 top: 0;
    		z-index: 1000;
    		border-bottom: 1px solid rgba(255,255,255,0.1);
    		background: rgba(30, 30, 30, 0.4); 
   			backdrop-filter: blur(12px);
    		color: #fff; 
    	}
    		

        /* Titolo nella barra superiore */
        header h1 {
            margin: 0;
            font-size: 1.6em;
            letter-spacing: 1px;
            text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.3);
        }

        /* Link nel header (es. logout o navigazione) */
        header a {
            color: #ffffff;
            text-decoration: none;
            font-weight: bold;
            padding: 8px 16px;
            background: rgba(255, 255, 255, 0.1);
            border-radius: 20px;
            transition: all 0.3s ease;
        }

        /* Effetto hover sui link del header */
        header a:hover {
            background: rgba(255, 255, 255, 0.25);
        }

        /* Area principale della pagina */
        .main-content {
   			flex: 1;
   			padding: 30px;
   			background: white;
   			box-shadow: 0 0 20px rgba(0, 0, 0, 0.05);
  			margin-top: 100px; 
		}		

      	 /* Card per calendario) */
        .card {
            background: rgba(255, 255, 255, 0.15);
            border-radius: 20px;
            padding: 25px;
            backdrop-filter: blur(8px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
            margin-bottom: 30px;
        }

        /* pulssanti per navigare tra i mesi del calendario */
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

        /* Effetto hover sui pulsanti */
        #calendar-controls button:hover {
            background: #f0f0f0;
            transform: translateY(-2px);
        }

        /* Tabella calendario */
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

        /* Stile footer */
        footer {
            padding: 30px 20px;
            text-align: center;
            border-top: 1px solid rgba(255,255,255,0.1);
            margin-top: auto;
        }

        /* Titolo sezioni nel footer */
        .footer-section h3 {
            margin-bottom: 15px;
            color: #fff;
        }

        /* Contenitore pulsanti footer */
        .footer-buttons {
            display: flex;
            justify-content: center;
            flex-wrap: wrap;
            gap: 15px;
            margin-top: 10px;
        }

        /* Pulsanti nel footer */
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

        /* Contenitore della modale */
        .modal-content {
            background: #fff;
            padding: 30px;
            border-radius: 16px;
            max-width: 450px;
            width: 90%;
            box-shadow: 0 8px 24px rgba(0,0,0,0.2);
        }

        /* Titolo della modale */
        .modal-content h3 {
            margin-top: 0;
        }

        /* Disposizione degli elementi del form */
        .modal-content form {
            display: flex;
            flex-direction: column;
        }

        /* Stile per input, select e pulsanti all'interno della modale */
        .modal-content input,
        .modal-content select,
        .modal-content button {
            margin: 10px 0;
            padding: 12px;
            font-size: 14px;
            border-radius: 8px;
            border: 1px solid #ccc;
        }

        /* Pulsante nella modale */
        .modal-content button {
            background-color: #3498db;
            color: white;
            border: none;
            cursor: pointer;
        }

        /* Effetto hover sui pulsanti modale */
        .modal-content button:hover {
            background-color: #2980b9;
        }

        /* Icona per chiudere la modale */
        .close-modal {
            float: right;
            font-weight: bold;
            color: #999;
            cursor: pointer;
        }
    </style>
</head>
<body>


<!-- Intestazione con titolo, dati utente, link per logout -->
<header>
    <h1>Dashboard Giocatore</h1>
    <div>
        Benvenuto: <%= session.getAttribute("userEmail") %> | <a href="logout.jsp">Logout</a>
    </div>
</header>

<!-- Sezione calendario eventi -->
<div class="main-content">
    <div class="card" id="calendar-section">
        <h2>Calendario Eventi</h2>      
        <div id="calendar-controls">
            <button id="prev-month">« Mese precedente</button>
            <span id="current-month" style="line-height: 36px; flex: 0 1 100px; text-align: center;"></span>
            <button id="next-month">Mese successivo »</button>
        </div>     
        <div id="calendar"></div>
    </div>
</div>

<!-- Footer con pulsanti -->
<footer>
    <div class="footer-section">
        <h3>Impostazioni</h3>
        <div class="footer-buttons">
            <button id="btn-update-auth">Gestione Profilo</button>
        </div>
    </div>
</footer>

<!-- Struttura della modale generica  -->
<div id="modal" class="modal-overlay">
    <div class="modal-content">       
        <span class="close-modal" onclick="closeModal()">×</span>
        <div id="modal-body"></div>
    </div>
</div>

<!-- Script per logica calendario -->
<script src="js/calendar.js"></script>

<script>
//Mostra la modale e inserisce l'HTML passato come contenuto
    function openModal(html) {
        document.getElementById("modal-body").innerHTML = html;
        document.getElementById("modal").style.display = "flex";
    }

	 // Nasconde la modale
    function closeModal() {
        document.getElementById("modal").style.display = "none";
    }

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


    function bindAuthForms() {
    	// Form per aggiornare email
        document.getElementById('update-email-form').onsubmit = async (e) => {
            e.preventDefault();
            const data = new URLSearchParams(new FormData(e.target));
            data.append("action", "updateEmail");
            const res = await fetch("ProfileServlet", { method: "POST", body: data });
            alert(await res.text()); // Mostra la risposta del server
            closeModal();
        };

   	  	// Form per aggiornare password
        document.getElementById('update-password-form').onsubmit = async (e) => {
            e.preventDefault();
            const data = new URLSearchParams(new FormData(e.target));
            data.append("action", "updatePassword");
            const res = await fetch("ProfileServlet", { method: "POST", body: data });
            alert(await res.text()); // Mostra la risposta del server
            closeModal();
        };
    }

 //Gestisce invio del form per eliminare account
function bindDeleteForm() {
    document.getElementById('delete-account-form').onsubmit = async (e) => {
        e.preventDefault();
        const confirmInput = document.querySelector('#delete-account-form input[name="confirmDelete"]').value.trim();

        if (confirmInput !== "DELETE") {
            alert("Devi digitare 'DELETE' per confermare.");
            return;
        }

        const data = new URLSearchParams();
        data.append("action", "deleteAccount");
        data.append("confirmDelete", confirmInput);

        const res = await fetch("ProfileServlet", {
            method: "POST",
            body: data
        });

        const text = await res.text();
        alert(text);

        if (text.includes("Account eliminato")) {
            window.location.href = "logout.jsp";
        }
    };
    }
</script>

</body>
</html>
