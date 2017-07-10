# Projekt It - Gruppe 2 - Gesichtserkennung mit temperaturabhängiger Audiowiedergabe

## Vorbereitung
Es müssen einige native Libraries und Treiber installiert werden. Diese entnehmen Sie aus der Installationsanleitung aus dem Meilenstein 3.

## Installation

Auf dem ersten Pi den Ordner /opt/projekt-it erstellen und das Repo https://github.com/projekt-internet-technologien/SS17-Gruppe2-Face klonen. Auf dem zweiten Pi den Ordner /opt/projekt-it erstellen und das Repo https://github.com/projekt-internet-technologien/SS17-Gruppe2-Sound klonen.

Die Java Lib aus dem Repo https://github.com/projekt-internet-technologien/SS17-Gruppe2 klonen und mittels 

```
mvn clean package 
```

bauen. Das Artefakt dann auf beide Pi’s jeweils in den Ordner /opt/projekt-it kopieren. 

Jetzt können auf beiden Pi’s mittels

```
/opt/projekt-it/start.sh
```

die Dienste gestartet werden.

## Logs

Die unterschiedliche Anwendungen schreiben Ihre Log-Ausgaben in folgende Dateien:

### Pi Nr. 1 - Face
- python_learn_face.log
- java_lib.log

### Pi Nr. 2 - Sound
- python_server.log
- java_lib.log

Die Log-Dateien liegen im Verzeichnis aus welchem man die Start-Skripte ausgeführt hat.

## Benutzung

### Neuen Benutzer in Weboberfläche registrieren
Auf dem Pi Nr. 2 läuft ein Webserver auf Port 8080. Dieser kann mit einem Browser aufgerufen werden. Um einen neuen Benutzer anzulegen muss das Web-Formular ausgefüllt werden. 

Nach etwa einer Minute ist der Benutzer registriert und wird von der Gesichtserkennung über die Picam erkannt.



