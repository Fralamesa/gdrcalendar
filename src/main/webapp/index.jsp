<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%> <%-- Impostazioni base per la pagina JSP --%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <title>GDRCalendar - Home</title> <%-- Titolo della pagina --%>
    
    <%-- Importazione del font Montserrat da Google Fonts --%>
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;700&display=swap" rel="stylesheet">
    
    <style>
        /* Applica box-sizing globale */
        * {
            box-sizing: border-box;
        }

        /* Stili generali */
        body {
            margin: 0;
            padding: 0;
            height: 100vh;
            font-family: 'Montserrat', sans-serif;
            background: linear-gradient(135deg, #74ebd5 0%, #ACB6E5 100%);
            overflow: hidden;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #fff;
            position: relative;
        }

        /* Contenitore centrale con effetto vetro */
        .container {
            background: rgba(255, 255, 255, 0.1);
            padding: 50px;
            border-radius: 20px;
            backdrop-filter: blur(10px);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
            text-align: center;
            animation: fadeIn 1.2s ease-out forwards;
            transform: translateY(30px);
            opacity: 0;
        }

        /* Titolo principale */
        h1 {
            font-size: 3em;
            margin-bottom: 30px;
            color: #fff;
            text-shadow: 2px 2px 6px rgba(0, 0, 0, 0.3);
        }

        /* Contenitore per i pulsanti di navigazione */
        .buttons {
            display: flex;
            gap: 20px;
            justify-content: center;
        }

        /* Stile dei link/pulsanti */
        a {
            text-decoration: none;
            padding: 15px 35px;
            background: #fff;
            color: #333;
            border-radius: 30px;
            font-weight: bold;
            box-shadow: 0 5px 15px rgba(0,0,0,0.1);
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden; /* Necessario per effetto luce interno */
        }

        /* Luce in movimento sull’hover */
        a::before {
            content: "";
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: rgba(255,255,255,0.4);
            transition: left 0.4s ease;
        }

        a:hover::before {
            left: 100%;
        }

        /*  Effetto sollevamento al passaggio */
        a:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 20px rgba(0,0,0,0.2);
        }

        /* Contenitore per le particelle decorative */
        .particles {
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            z-index: 0;
            overflow: hidden;
        }

        /* Singola particella */
        .particle {
            position: absolute;
            width: 8px;
            height: 8px;
            background: white;
            border-radius: 50%;
            opacity: 0.3;
            animation: float 8s infinite ease-in-out;
        }

        /* Animazione flottante*/
        @keyframes float {
            0% {
                transform: translateY(0) scale(1);
                opacity: 0.3;
            }
            50% {
                transform: translateY(-200px) scale(1.3);
                opacity: 0.6;
            }
            100% {
                transform: translateY(0) scale(1);
                opacity: 0.3;
            }
        }

        /* Animazione di comparsa */
        @keyframes fadeIn {
            to {
                transform: translateY(0);
                opacity: 1;
            }
        }
    </style>
</head>
<body>

    <div class="particles">
        <%-- Generazione dinamica di 20 particelle animate con posizione e ritardo casuali --%>
        <% for (int i = 0; i < 20; i++) { 
            int x = (int)(Math.random() * 100); // posizione orizzontale in %
            int y = (int)(Math.random() * 100); // posizione verticale in %
            int delay = (int)(Math.random() * 8); // ritardo in secondi
        %>
        <div class="particle" style="left: <%=x%>%; top: <%=y%>%; animation-delay: <%=delay%>s;"></div>
        <% } %>
    </div>

    <div class="container">
        <%-- Messaggio di benvenuto --%>
        <h1>Benvenuto su <strong>GDRCalendar</strong>!</h1>
        
        <%-- Link per pagine di registrazione e login --%>
        <div class="buttons">
            <a href="register.jsp">Registrati</a>
            <a href="login.jsp">Login</a>
        </div>
    </div>
</body>
</html>
