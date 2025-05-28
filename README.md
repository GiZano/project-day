# ProjectDay - Company Energetic Efficiency Enhancement System
### <a href="#Ita">Italian Version below</a>

## Description

This project increases the energy efficiency of the working environment. Several technologies are implemented that allow the collection and analysis of data in a semi-automatic way.

## Technologies

The main implemented technologies are:
<ul>
    <li>ThingsBoard: center of data management, collection and visualization. Discover how to install it here <a href="https://thingsboard.io/docs/user-guide/install/installation-options/" target="_blank">ThingsBoard</a></li>
    <li>Arduino - creation of sensors to collect data and actuators to apply real-time solutions.</li>
    <li>Phantom sensors - creation of demo sensor to make practice with the application.</li>
    <li>AI - data analyzer based on the metric and and the time lapse.</li>
    <li>Graphic User Interface (GUI) - enables to visualize the first technology mentioned, and to connect via console to the analysis process.</li>
</ul>

## Architecture 

![bE-More architecture](https://github.com/user-attachments/assets/7b5e7096-b4c3-451a-82bb-c95ca022e853)

## Operation

### Arduino

<ul>
    <li>ThingsBoard turns on and gives access to the web app on the port 8080.</li>
    <li>The arduino sensor connects via MQTT credentials to ThingsBoard.</li>
    <li>"" reads the light value thanks to the photoresitance (pin A3).</li>
    <li>"" sends the value to the data platform.</li>
    <li>"" checks the button states:
        <ul>
            <li>If the "AUTO" button is pressed (check on pin 2), the mode is switched on/off, a led (pin 0) is turned on and a buzzer (pin 8) emits a sound until the start of the next sensor iteration.</li>
            <li>If the "LED" button is pressed (check on pin 1), the LEDs are turned on (pin 5).
        </ul>
    </li>
    <li>If the light value exceeds 450 and the auto mode is turned on, shut down LEDs.</li>
    <li>Restart the iteration of the sensor.</li>
</ul>

### Java Desktop App

<ul>
    <li>On the WebViewer, the application connects to the ThingsBoard platform on the 8080 port, creating an HTTP connection as we were using a normal Browser.</li>
    <li>On the Console component, it's possible to exchange data with the "Python AI Analyzer".</li>
    <li>There are also buttons to:
        <ul>
            <li>Access the platform directly via web browser.</li>
            <li>Change the application theme.</li>
            <li>Hide/Show the console panel.</li>
        </ul>
    </li>
</ul>

### Python AI Analyzer

<ul>
    <li>The desired device ID is requested.</li>
    <li>It's asked to select the avaiable metrics to analyze and the time lapse.</li>
    <li>Data is collected from the database of ThingsBoard.</li>
    <li>The local Ollama application is accessed.</li>
    <li>Through Ollama, a prompt is sent to the LLM model Mistral:7b. The prompt contains the data and some key aspects are requested to be analyzed.</li>
    <li>The reply is shown on the console.</li>
</ul>

## Real-life Model Circuit

![circuit](https://github.com/user-attachments/assets/bca92431-5202-4c54-b24f-d2595b3f0679)

## Requirements

<ul>
    <li>Ollama installed.</li>
    <li>Mistral:7b installed.</li>
    <li>Java 23.</li>
</ul>

# <p id="Ita">ProjectDay - Sistema di EfFicientamento Energetico Aziendale</p>

## Descrizione

Questo progetto permette di aumentare l'efficienza energetica dell'ambiente di lavoro. Vengono implementate diverse tecnologie che collaborando permettono di raccogliere e analizzare i dati in maniera semi-automatica.

## Tecnologie

Le tecnologie principali utilizzate sono:
<ul>
    <li>ThingsBoard - centro di gestione, immagazzinamento e visualizzazione dei dati. Scopri come installarlo qui <a href="https://thingsboard.io/docs/user-guide/install/installation-options/" target="_blank">ThingsBoard</a></li>
    <li>Arduino - sensori che raccolgono dati e attuatori che applicano soluzioni in tempo reale.</li>
    <li>Sensori fantasma - che permettono di creare demo per la visualizzazione e gestione di dati.</li>
    <li>IA - per analizzare i dati in base alla metrica e ad un lasso di tempo.</li>
    <li>Interfaccia grafica - permettendo la visualizzazione della prima citata e di accedere alla console per utilizzare l'IA.</li>
</ul>

## Architettura

![bE-More architecture_ita](https://github.com/user-attachments/assets/ab109e94-daf4-46e7-b08d-7575b2e39c83)

## Funzionamento generale

### Arduino

<ul>
    <li>Una volta avviato, Arduino si connette a ThingsBoard.</li>
    <li>Arduino legge il valore della luce grazie alla fotoresistenza (controllando il pin A3).</li>
    <li>Invia il valore alla piattaforma dati.</li>
    <li>Controlla i due pulsanti:
        <ul>
            <li>Se il pulsante AUTO è acceso (pin 2), viene attivata/disattivata la modalità auto, viene acceso/spento il LED di verifica e viene attivato un buzzer fino alla fine dell'iterazione.</li>
            <li>Se il pulsante LED è acceso (pin 1), vengono accesi/spenti i led (pin 5).</li>
        </ul>
    </li>
    <li>Se il valore della luce supera 450 e la modalità auto è attivata, spegne i led.</li>
    <li>L'iterazione ricomincia.</li>
</ul>

### Applicazione Desktop Java

<ul>
    <li>Sulla componente WebViewver, l'applicazione si connette alla piattaforma ThingsBoard tramite la porta 8080, stabilendo una connessione HTTP come se stessimo utilizzando un classico browser.</li>
    <li>Sulla componente console, si può comunicare con la componente "Analizzatore IA Python".</li>
    <li>Sono presenti pulsanti per:
        <ul>
            <li>Accedere alla piattaforma direttamente dal web.</li>
            <li>Cambiare il tema dell'applicazione.</li>
            <li>Nascondere/Mostrare la console</li>
        </ul>
    </li>
</ul>

### Analizzatore IA Python

<ul>
    <li>Viene chiesto l'ID del dispositivo da analizzare.</li>
    <li>Si chiede di selezionare la metrica da analizzare e per quanti giorni.</li>
    <li>Vengono prelevati dal database di ThingsBoard i dati richiesti.</li>
    <li>Viene effettuato tramite ollama l'accesso al modello LLM Mistral:7b.</li>
    <li>Al modello viene chiesto tramite prompt testuale di analizzare i dati con degli aspetti chiave.</li>
    <li>La risposta ricevuta viene inviata e mostrata sulla console dell'Applicazione Desktop Java.</li>
</ul>

## Circuito del Modello Reale

![circuit](https://github.com/user-attachments/assets/bca92431-5202-4c54-b24f-d2595b3f0679)

## Requirements
<ul>
    <li>Ollama installato.</li>
    <li>Mistral:7b installato.</li>
    <li>Java 23.</li>
</ul>

