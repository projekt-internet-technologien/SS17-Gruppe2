# Projekt It - Gruppe 2 - Gesichtserkennung mit temperaturabhängiger Audiowiedergabe

## Pi 1

### Technischer Aufbau
PiCamera an den PiCamera-Anschluss anschließen.

LED anschließen:
- Widerstand 100 Ohm
- LED grün
![LED Schaltpaln](https://data.control.fysar.de/index.php/apps/files_sharing/ajax/publicpreview.php?x=3360&y=1224&a=true&file=led-schaltplan.png&t=sc2vACslzTmEeG0&scalingup=0)

### Installation 

Es müssen einige native Libraries und Treiber installiert werden. Diese entnehmen Sie aus der Installationsanleitung aus dem Meilenstein 3.

Pip:
- `wget https://bootstrap.pypa.io/get-pip.py`
- `sudo python get-pip.py`

PiCamera:
- Im ausgeschalteten Zustand wird die Picam eingesteckt
- `sudo raspi-config`
- unter Interfaces die Picamera auf Enable setzen
- `sudo reboot`
- `sudo apt-get install python-picamera`
- `sudo pip install –upgrade picamera[array]`

Libraries:
- `sudo apt-get install libboost-all-dev`
- `sudo pip install numpy`
- `sudo pip install scipy`
- `sudo pip install scikit-image`
- `sudo pip install –no-cache-dir dlib`
- `sudo pip install face_recognition`
- `sudo apt-get install python-rpi.gpio`

Den Ordner /opt/projekt-it erstellen und das Repo https://github.com/projekt-internet-technologien/SS17-Gruppe2-Face klonen.

Die Java Lib aus dem Repo https://github.com/projekt-internet-technologien/SS17-Gruppe2 klonen und mittels 

```
mvn clean package 
```

bauen. Das Artefakt dann auf beide Pi’s jeweils in den Ordner /opt/projekt-it kopieren. 

### Start

Jetzt können auf beiden Pi’s mittels

```
/opt/projekt-it/start.sh
```

die Dienste gestartet werden.

### Logs

Die unterschiedliche Anwendungen schreiben Ihre Log-Ausgaben in folgende Dateien:

- python_learn_face.log
- java_lib.log

Die Log-Dateien liegen im Verzeichnis aus welchem man die Start-Skripte ausgeführt hat.


## Pi 2

### Aufbau

Lautsprecher an den Klinkenanschluss des Pis anschließen.

### Installation

Den Ordner /opt/projekt-it erstellen und das Repo https://github.com/projekt-internet-technologien/SS17-Gruppe2-Sound klonen.

Die Java Lib aus dem Repo https://github.com/projekt-internet-technologien/SS17-Gruppe2 klonen und mittels 

```
mvn clean package 
```

bauen. Das Artefakt dann auf beide Pi’s jeweils in den Ordner /opt/projekt-it kopieren. 

### Start

Jetzt können auf beiden Pi’s mittels

```
/opt/projekt-it/start.sh
```

die Dienste gestartet werden.

### Logs

Die unterschiedliche Anwendungen schreiben Ihre Log-Ausgaben in folgende Dateien:


- python_server.log
- java_lib.log

Die Log-Dateien liegen im Verzeichnis aus welchem man die Start-Skripte ausgeführt hat.

## Benutzung

### Neuen Benutzer in Weboberfläche registrieren
Auf dem Pi Nr. 2 läuft ein Webserver auf Port 8080. Dieser kann mit einem Browser aufgerufen werden. Um einen neuen Benutzer anzulegen muss das Web-Formular ausgefüllt werden. 

Nach etwa einer Minute ist der Benutzer registriert und wird von der Gesichtserkennung über die Picam erkannt.




