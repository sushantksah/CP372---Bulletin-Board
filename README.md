# Bulletin Board Client–Server System (CP372)

## Overview
This project implements a multithreaded client–server bulletin board using TCP sockets.  
The server maintains a shared board where clients can post notes, place pins, retrieve data, and modify the board state through a defined protocol.

The system follows a centralized architecture where one server manages all notes and pins while multiple clients connect concurrently. The behaviour and message formats are defined in the RFC document included in this repository.

---

## Goals
- Implement a TCP client–server application using Java sockets
- Follow a structured application-layer protocol
- Support concurrent clients using a thread-per-client model
- Maintain a consistent shared board state
- Provide clear error handling and input validation

This project focuses on networking concepts, concurrency, and protocol design rather than GUI aesthetics or persistent storage.

---

## Project Structure
```
BBoard = Bulletin Board **

Bulletin Board Code/
│
├── Server/
│   ├── BBoard.java          # Server entry point
│   ├── ClientHandler.java   # Handles each connected client
│   ├── Board.java           # Shared board state (notes + pins)
│   ├── Note.java            # Note data model
│   └── Pin.java             # Pin data model
│
├── Client/
│   ├── BBoardGUI.java       # Swing GUI 
│   ├── VisualPanel.java     # Visual board rendering 
│   └── BBoardClient.java    # Network Client for Bulletin Board 
│
├── RFC-A1.docx              # RFC/Protocol Specification 
└── README.md
```

---

## How It Works

### Server
- Parses startup arguments:
```
<port> <board_width> <board_height> <note_width> <note_height> <color1> ... <colorN>
```
- Opens a TCP socket and listens for connections
- Creates a new thread for each client
- Validates all commands and maintains board state

### Client
- Connects to the server over TCP
- Receives board configuration on connection
- Sends protocol commands (POST, GET, PIN, SHAKE, CLEAR, etc.)
- Displays notes visually using a Swing interface

---

## Running the Program

### Compile
From the project root:
```bash
javac Server/*.java Client/*.java
```

### Start Server
```bash
java Server.BBoard 4554 200 100 20 10 red white green yellow
```

### Start Client
```bash
java Client.BBoardGUI
```

---

## Technologies Used
- Java
- TCP Sockets
- Multithreading
- Swing (GUI)

---

## Notes
- The board exists only during runtime (no persistent storage).
- All validation is performed on the server side.
- Multiple clients can interact with the board at the same time.

---

## Authors
Sushant Sah  
Jessica Tidd (@jesssyd)
